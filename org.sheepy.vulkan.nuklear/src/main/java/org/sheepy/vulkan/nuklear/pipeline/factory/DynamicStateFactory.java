package org.sheepy.vulkan.nuklear.pipeline.factory;

import org.sheepy.vulkan.model.enumeration.EDynamicState;
import org.sheepy.vulkan.model.process.graphic.DynamicState;
import org.sheepy.vulkan.model.process.graphic.impl.DynamicStateImpl;

public class DynamicStateFactory
{

	public static final DynamicState create()
	{
		var dynamicState = new DynamicStateImpl();
		dynamicState.getStates().add(EDynamicState.VIEWPORT);
		dynamicState.getStates().add(EDynamicState.SCISSOR);

		return dynamicState;
	}
}