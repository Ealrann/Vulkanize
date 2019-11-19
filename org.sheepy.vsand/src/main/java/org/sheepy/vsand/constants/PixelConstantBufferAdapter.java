package org.sheepy.vsand.constants;

import java.nio.ByteBuffer;

import org.joml.Vector2fc;
import org.joml.Vector2i;
import org.lwjgl.system.MemoryUtil;
import org.sheepy.lily.core.api.adapter.annotation.Adapter;
import org.sheepy.lily.core.api.adapter.annotation.Dispose;
import org.sheepy.lily.core.api.adapter.annotation.Load;
import org.sheepy.lily.core.api.adapter.annotation.Statefull;
import org.sheepy.lily.core.api.adapter.annotation.Tick;
import org.sheepy.lily.core.api.util.ModelUtil;
import org.sheepy.lily.vulkan.api.engine.IVulkanEngineAdapter;
import org.sheepy.lily.vulkan.api.input.IVulkanInputManager;
import org.sheepy.lily.vulkan.api.resource.buffer.IConstantBufferUpdater;
import org.sheepy.lily.vulkan.model.VulkanEngine;
import org.sheepy.lily.vulkan.model.resource.ConstantBuffer;
import org.sheepy.vsand.model.Material;
import org.sheepy.vsand.model.PixelConstantBuffer;
import org.sheepy.vsand.model.VSandApplication;
import org.sheepy.vsand.util.EShapeSize;

@Statefull
@Adapter(scope = PixelConstantBuffer.class, lazy = false)
public final class PixelConstantBufferAdapter implements IConstantBufferUpdater
{
	private static final int BYTE_SIZE = 7 * Integer.BYTES;
	private static final int BOARD_INDEX_POSITION = 6 * Integer.BYTES;

	private final PixelConstantBuffer constantBuffer;
	private final VSandApplication application;
	private final Vector2i boardSize;

	private ByteBuffer buffer = null;

	private IVulkanInputManager inputManager;

	private PixelConstantBufferAdapter(PixelConstantBuffer constantBuffer)
	{
		this.constantBuffer = constantBuffer;

		application = (VSandApplication) ModelUtil.getApplication(constantBuffer);
		boardSize = new Vector2i(application.getSize());
	}

	@Load
	private void load()
	{
		buffer = MemoryUtil.memAlloc(BYTE_SIZE);

		final var vulkanEngine = (VulkanEngine) application.getEngines().get(0);
		final var engineAdapter = vulkanEngine.adapt(IVulkanEngineAdapter.class);
		inputManager = engineAdapter.getInputManager();

		updateBuffer();
	}

	@Dispose
	private void dispose()
	{
		MemoryUtil.memFree(buffer);
	}

	@Override
	public void beforePush(ConstantBuffer b)
	{
		buffer.putInt(	BOARD_INDEX_POSITION,
						constantBuffer.getBoardConstantBuffer().getCurrentBoardBuffer());
	}

	@Tick
	private void updateBuffer()
	{
		int forceClear = 0;
		if (application.isForceClear())
		{
			forceClear = 1;
		}

		final var size = EShapeSize.values()[application.getBrushSize() - 1];

		buffer.putInt(forceClear);
		buffer.putInt(application.isShowSleepZones() ? 1 : 0);

		final Material mainMaterial = application.getMainMaterial();
		final int index = application.getMaterials().getMaterials().indexOf(mainMaterial);
		buffer.putInt(index);

		buffer.putInt(size.getSize() >> 1);

		if (inputManager != null)
		{
			final var cursorPosition = convertToBoardPosition(inputManager.getCursorPosition());
			buffer.putInt(cursorPosition.x);
			buffer.putInt(cursorPosition.y);
		}
		else
		{
			buffer.putInt(0);
			buffer.putInt(0);
		}

		buffer.putInt(0);

		buffer.flip();

		constantBuffer.setData(buffer);
	}

	private Vector2i convertToBoardPosition(Vector2fc mousePos)
	{
		final Vector2i res = new Vector2i((int) mousePos.x(), (int) mousePos.y());

		final int boardWidth = boardSize.x;
		final int boardHeight = boardSize.y;

		final int width = application.getSize().x();
		final int height = application.getSize().y();
		if (width != boardWidth || height != boardHeight)
		{
			res.x *= (float) boardWidth / width;
			res.y *= (float) boardHeight / height;
		}

		return res;
	}
}
