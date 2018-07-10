package org.sheepy.vulkan.sand.pipelinepool;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.sheepy.vulkan.buffer.Buffer;
import org.sheepy.vulkan.command.CommandPool;
import org.sheepy.vulkan.command.SingleTimeCommand;
import org.sheepy.vulkan.descriptor.IDescriptor;
import org.sheepy.vulkan.device.LogicalDevice;
import org.sheepy.vulkan.pipeline.IPipelinePool;
import org.sheepy.vulkan.pipeline.compute.ComputeProcess;
import org.sheepy.vulkan.pipeline.compute.ComputeProcessPool;
import org.sheepy.vulkan.pipeline.compute.Computer;
import org.sheepy.vulkan.sand.board.BoardModifications;
import org.sheepy.vulkan.sand.compute.BoardImage;
import org.sheepy.vulkan.sand.compute.ConfigurationBuffer;
import org.sheepy.vulkan.sand.compute.PixelComputer;

public class BoardPipelinePool implements IPipelinePool
{
	private static final String SHADER_VERT = "org/sheepy/vulkan/sand/game_step_vert.comp.spv";

	private static final String SHADER_DRAW = "org/sheepy/vulkan/sand/draw.comp.spv";

	private LogicalDevice logicalDevice;

	private BoardModifications boardModifications;
	private BoardImage boardImage;

	private CommandPool commandPool;

	private ComputeProcessPool boardProcesses;
	private ComputeProcessPool mergeBoardProcesses;

	private ConfigurationBuffer configBuffer;
	private Buffer boardBuffer;

	public BoardPipelinePool(LogicalDevice logicalDevice, BoardModifications boardModifications,
			BoardImage boardImage)
	{
		this.logicalDevice = logicalDevice;
		this.boardModifications = boardModifications;
		this.boardImage = boardImage;

		try (MemoryStack stack = MemoryStack.stackPush())
		{
			commandPool = CommandPool.alloc(stack, logicalDevice,
					logicalDevice.getQueueManager().getComputeQueueIndex());
		}

		createBuffers();
		buildPipelines();
	}

	private void createBuffers()
	{
		int width = boardImage.getWidth();
		int height = boardImage.getHeight();

		// configBuffer
		{
			configBuffer = new ConfigurationBuffer(logicalDevice);
		}

		// boardBuffer
		{
			int boardSizeByte = width * height * Integer.BYTES;
			boardBuffer = Buffer.alloc(logicalDevice, boardSizeByte,
					VK_BUFFER_USAGE_STORAGE_BUFFER_BIT
							| VK_BUFFER_USAGE_TRANSFER_SRC_BIT
							| VK_BUFFER_USAGE_TRANSFER_DST_BIT,
					VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
			boardBuffer.configureDescriptor(VK_SHADER_STAGE_COMPUTE_BIT,
					VK_DESCRIPTOR_TYPE_STORAGE_BUFFER);
		}
	}

	private void buildPipelines()
	{
		int width = boardImage.getWidth();
		int height = boardImage.getHeight();

		Computer modificationComputer = new Computer(logicalDevice, SHADER_DRAW, width, height,
				Arrays.asList(boardBuffer, boardModifications));

		List<IDescriptor> stepDescriptors = new ArrayList<>();
		stepDescriptors.add(configBuffer.getBuffer());
		stepDescriptors.add(boardBuffer);

		Computer stepVert = new Computer(logicalDevice, SHADER_VERT, width,
				1, stepDescriptors);
		stepVert.setWorkgroupSize(16);
		
		
		PixelComputer pixelComputer = new PixelComputer(logicalDevice, configBuffer, boardBuffer,
				boardImage);

		ComputeProcess process = new ComputeProcess(logicalDevice);
		process.addNewPipeline(stepVert);
		process.addNewPipeline(pixelComputer);

		boardProcesses = new ComputeProcessPool(logicalDevice, commandPool);
		boardProcesses.addProcess(process);

		ComputeProcess modificationProcess = new ComputeProcess(logicalDevice);
		modificationProcess.addNewPipeline(modificationComputer);
		modificationProcess.addNewPipeline(stepVert);
		modificationProcess.addNewPipeline(pixelComputer);

		mergeBoardProcesses = new ComputeProcessPool(logicalDevice, commandPool);
		mergeBoardProcesses.addProcess(modificationProcess);
	}

	@Override
	public void load(MemoryStack stack, long surface, int width, int height)
	{
		{
			SingleTimeCommand stc = new SingleTimeCommand(commandPool,
					logicalDevice.getQueueManager().getGraphicQueue())
			{
				@Override
				protected void doExecute(MemoryStack stack, VkCommandBuffer commandBuffer)
				{
					boardImage.getImage().transitionImageLayout(commandBuffer,
							VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_GENERAL, 1,
							VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, 0,
							VK_ACCESS_SHADER_WRITE_BIT);
				}
			};
			stc.execute();
		}

		// Fill the configurationBuffer
		{
			configBuffer.load(width, height);
		}

		// Fill the board buffer with void (0)
		{
			ByteBuffer bBuffer = MemoryUtil.memCalloc((int) boardBuffer.getSize());
			boardBuffer.fillWithBuffer(bBuffer);
			MemoryUtil.memFree(bBuffer);
		}

		mergeBoardProcesses.allocateNode(stack);
		boardProcesses.allocateNode(stack);
	}

	@Override
	public void execute()
	{
		if (boardModifications.isDirty())
		{
			boardModifications.updateVkBuffer();

			mergeBoardProcesses.exectue(logicalDevice.getQueueManager().getComputeQueue(), 0);
		}
		else
		{
			boardProcesses.exectue(logicalDevice.getQueueManager().getComputeQueue(), 0);
		}
	}

	@Override
	public void resize(long surface, int width, int height)
	{}

	@Override
	public void free()
	{
		mergeBoardProcesses.freeNode();
		boardProcesses.freeNode();

		boardBuffer.free();
		configBuffer.free();

		commandPool.free();
	}

	@Override
	public CommandPool getCommandPool()
	{
		return commandPool;
	}
}
