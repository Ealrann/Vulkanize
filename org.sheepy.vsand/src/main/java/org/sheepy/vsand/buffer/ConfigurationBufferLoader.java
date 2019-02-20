package org.sheepy.vsand.buffer;

import java.nio.ByteBuffer;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.lwjgl.system.MemoryUtil;
import org.sheepy.lily.vulkan.model.resource.Buffer;
import org.sheepy.vsand.logic.MaterialUtil;
import org.sheepy.vsand.model.Material;
import org.sheepy.vsand.model.VSandApplication;

public class ConfigurationBufferLoader
{
	private static final int BYTE_SIZE = MaterialUtil.MAX_MATERIAL_NUMBER * 4 * Integer.BYTES;

	public static final void load(Buffer buffer)
	{
		var application = (VSandApplication) EcoreUtil.getRootContainer(buffer);

		buffer.setSize(BYTE_SIZE);

		ByteBuffer bBuffer = MemoryUtil.memAlloc(BYTE_SIZE);
		for (Material material : application.getMaterials().getMaterials())
		{
			bBuffer.putInt(material.isIsStatic() ? 1 : 0);
			bBuffer.putInt(material.getDensity());
			bBuffer.putInt(material.getRunoff());

			int rgb = material.getR() << 16
					| material.getG() << 8
					| material.getB();

			bBuffer.putInt(rgb);
		}
		bBuffer.flip();

		buffer.setData(bBuffer);
	}
}
