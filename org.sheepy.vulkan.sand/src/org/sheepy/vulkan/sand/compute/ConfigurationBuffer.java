package org.sheepy.vulkan.sand.compute;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.sheepy.vulkan.buffer.Buffer;
import org.sheepy.vulkan.common.IAllocable;
import org.sheepy.vulkan.device.LogicalDevice;
import org.sheepy.vulkan.sand.board.EMaterial;

public class ConfigurationBuffer implements IAllocable
{
	private static final int MAX_MATERIAL_NUMBER = 16;
	
	private Buffer configBuffer;

	public ConfigurationBuffer(LogicalDevice logicalDevice)
	{
		int size = (MAX_MATERIAL_NUMBER * 8);
		int byteSize = size * Integer.BYTES;

		configBuffer = new Buffer(logicalDevice, byteSize,
				VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
				VK_MEMORY_PROPERTY_HOST_COHERENT_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
		configBuffer.configureDescriptor(VK_SHADER_STAGE_COMPUTE_BIT,
				VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
	}

	@Override
	public void allocate(MemoryStack stack)
	{
		configBuffer.allocate();
		
		ByteBuffer bBuffer = MemoryUtil.memAlloc((int) configBuffer.getSize());
//		bBuffer.putInt(width);
//		bBuffer.putInt(height);
		for (EMaterial material : EMaterial.values())
		{
			bBuffer.putInt(material.isStatic ? 1 : 0);
			bBuffer.putInt(material.density);
			bBuffer.putInt(material.runoff);
			bBuffer.putFloat(material.viscosity);
			bBuffer.putFloat(material.r);
			bBuffer.putFloat(material.g);
			bBuffer.putFloat(material.b);
			
			// Padding1
			bBuffer.putFloat(0);
		}
		bBuffer.flip();

		configBuffer.fillWithBuffer(bBuffer);
		MemoryUtil.memFree(bBuffer);
	}

	public Buffer getBuffer()
	{
		return configBuffer;
	}

	@Override
	public void free()
	{
		configBuffer.free();
	}
}