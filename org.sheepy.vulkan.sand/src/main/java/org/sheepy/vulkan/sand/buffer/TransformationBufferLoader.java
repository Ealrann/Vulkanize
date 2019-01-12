package org.sheepy.vulkan.sand.buffer;

import java.nio.ByteBuffer;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.lwjgl.system.MemoryUtil;
import org.sheepy.vulkan.model.enumeration.EBufferUsage;
import org.sheepy.vulkan.model.enumeration.EDescriptorType;
import org.sheepy.vulkan.model.enumeration.EMemoryProperty;
import org.sheepy.vulkan.model.enumeration.EShaderStage;
import org.sheepy.vulkan.model.resource.Buffer;
import org.sheepy.vulkan.sand.logic.MaterialUtil;
import org.sheepy.vulkan.sand.logic.TransformationUtil;
import org.sheepy.vulkan.sand.model.VSandApplication;

public class TransformationBufferLoader
{
	private static final int BYTE_SIZE = MaterialUtil.MAX_MATERIAL_NUMBER
			* MaterialUtil.MAX_MATERIAL_NUMBER
			* Integer.BYTES;

	public static final void load(Buffer buffer)
	{
		var application = (VSandApplication) EcoreUtil.getRootContainer(buffer);

		buffer.setSize(BYTE_SIZE);
		buffer.getUsages().add(EBufferUsage.STORAGE_BUFFER_BIT);
		buffer.getUsages().add(EBufferUsage.TRANSFER_DST_BIT);
		buffer.getProperties().add(EMemoryProperty.DEVICE_LOCAL_BIT);
		buffer.setDescriptorType(EDescriptorType.STORAGE_BUFFER);
		buffer.getShaderStages().add(EShaderStage.COMPUTE_BIT);

		ByteBuffer bBuffer = MemoryUtil.memAlloc(BYTE_SIZE);
		int[] transfoArray = TransformationUtil.toArray(application);
		for (int i = 0; i < transfoArray.length; i++)
		{
			bBuffer.putInt(transfoArray[i]);
		}
		bBuffer.flip();
		buffer.setData(bBuffer);
	}
}