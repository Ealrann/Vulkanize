package org.sheepy.vsand;

import org.eclipse.emf.common.util.EList;
import org.joml.Vector2i;
import org.sheepy.lily.core.api.cadence.IMainLoop;
import org.sheepy.lily.core.api.input.IInputManager;
import org.sheepy.lily.core.api.util.DebugUtil;
import org.sheepy.lily.core.model.application.Application;
import org.sheepy.lily.vulkan.api.engine.IVulkanEngineAdapter;
import org.sheepy.lily.vulkan.api.process.IProcessAdapter;
import org.sheepy.lily.vulkan.model.IProcess;
import org.sheepy.lily.vulkan.model.VulkanEngine;
import org.sheepy.lily.vulkan.model.process.CompositeTask;
import org.sheepy.lily.vulkan.model.process.compute.ComputePipeline;
import org.sheepy.lily.vulkan.model.process.compute.ComputeProcess;
import org.sheepy.lily.vulkan.model.process.graphic.GraphicProcess;
import org.sheepy.lily.vulkan.model.resource.Buffer;
import org.sheepy.vsand.buffer.ModificationsManager;
import org.sheepy.vsand.input.VSandInputManager;
import org.sheepy.vsand.model.VSandApplication;
import org.sheepy.vsand.util.FPSCounter;
import org.sheepy.vulkan.window.Window;

public class VSandMainLoop implements IMainLoop
{
	private IVulkanEngineAdapter engineAdapter;

	private DrawManager mainDrawManager;
	private DrawManager secondaryDrawManager;

	private final FPSCounter fpsCounter = new FPSCounter();

	private IProcessAdapter boardProcessAdapter;
	private IProcessAdapter renderProcessAdapter;

	private ModificationsManager modificationsManager;
	private ComputePipeline stepPipeline;
	private ComputePipeline drawPipeline;
	private CompositeTask stepTasks;
	private IInputManager inputManager;
	private VSandApplication application;

	private VSandInputManager vsandInputManager;

	private ComputeProcess boardProcess;

	@Override
	public void load(Application _application)
	{
		application = (VSandApplication) _application;

		final var vulkanEngine = (VulkanEngine) application.getEngines().get(0);
		engineAdapter = IVulkanEngineAdapter.adapt(vulkanEngine);
		final Window window = engineAdapter.getWindow();
		inputManager = engineAdapter.getInputManager();

		gatherProcesses(vulkanEngine);

		vsandInputManager = new VSandInputManager(window, application, stepTasks);
		inputManager.addListener(vsandInputManager);

		final Vector2i boardSize = new Vector2i(stepPipeline.getWidth(), stepPipeline.getHeight());
		mainDrawManager = new DrawManager(application, inputManager, modificationsManager,
				boardSize);
		secondaryDrawManager = new DrawManager(application, inputManager, modificationsManager,
				boardSize);
	}

	private void gatherProcesses(VulkanEngine vulkanEngine)
	{
		final EList<IProcess> processes = vulkanEngine.getProcesses();
		for (final IProcess process : processes)
		{
			if (process instanceof ComputeProcess)
			{
				boardProcess = (ComputeProcess) process;
				boardProcessAdapter = IProcessAdapter.adapt(process);

				final var parts = boardProcess.getPartPkg().getParts();
				drawPipeline = (ComputePipeline) parts.get(0);
				stepPipeline = (ComputePipeline) parts.get(1);
				stepTasks = (CompositeTask) stepPipeline.getTaskPkg().getTasks().get(2);

				final var resources = boardProcess.getResourcePkg().getResources();

				final var modificationBuffer = (Buffer) resources.get(4);
				modificationsManager = new ModificationsManager(modificationBuffer);
			}
			else if (process instanceof GraphicProcess)
			{
				renderProcessAdapter = IProcessAdapter.adapt(process);
			}
		}
	}

	@Override
	public void step(Application _application)
	{
		if (DebugUtil.DEBUG_ENABLED) fpsCounter.step();

		final var next = boardProcessAdapter.prepareNext();
		updateDrawManager();
		boardProcessAdapter.execute(next);

		if (application.isNextMode() == true)
		{
			application.setNextMode(false);
			stepPipeline.setEnabled(false);
		}

		renderProcessAdapter.prepareNextAndExecute();
	}

	@Override
	public void free(Application application)
	{}

	private void updateDrawManager()
	{
		// Main draw
		mainDrawManager.manage(application.getMainMaterial(),
				vsandInputManager.isLeftClicPressed());

		// Secondary draw
		secondaryDrawManager.manage(application.getSecondaryMaterial(),
				vsandInputManager.isRightClicPressed());

		// Enable drawManager
		if (modificationsManager.isEmpty() == false)
		{
			if (drawPipeline.isEnabled() == false)
			{
				drawPipeline.setEnabled(true);
			}

			modificationsManager.update();
		}
		// disable drawManager
		else if (drawPipeline.isEnabled())
		{
			drawPipeline.setEnabled(false);
		}
	}
}
