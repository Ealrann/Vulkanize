package org.sheepy.vsand.input;

import org.eclipse.emf.common.util.EList;
import org.logoce.adapter.api.Adapter;
import org.sheepy.lily.core.api.cadence.AutoLoad;
import org.logoce.extender.api.IAdapter;
import org.logoce.extender.api.ModelExtender;
import org.sheepy.lily.core.api.input.IInputManager;
import org.sheepy.lily.core.api.input.event.KeyEvent;
import org.sheepy.lily.core.api.input.event.ScrollEvent;
import org.sheepy.lily.core.api.notification.observatory.IObservatoryBuilder;
import org.sheepy.lily.core.model.types.EKeyState;
import org.sheepy.vsand.model.Material;
import org.sheepy.vsand.model.VSandApplication;

@ModelExtender(scope = VSandApplication.class)
@Adapter
@AutoLoad
public final class InputManager implements IAdapter
{
	private final VSandApplication application;
	private final IInputManager inputManager;

	private boolean shiftPressed = false;

	private InputManager(final VSandApplication application, final IObservatoryBuilder observatory)
	{
		this.application = application;

		inputManager = IInputManager.get(application);
		inputManager.showCursor(inputManager.isMouseOnUI());

		observatory.focus(inputManager)
				   .listen(this::onMouseOverUI, IInputManager.Features.MouseOverUIEvent)
				   .listen(this::onKeyEvent, IInputManager.Features.KeyEvent)
				   .listen(this::onScrollEvent, IInputManager.Features.ScrollEvent);
	}

	private void onMouseOverUI(Boolean overUI)
	{
		inputManager.showCursor(overUI);
	}

	private void onKeyEvent(KeyEvent event)
	{
		switch (event.key)
		{
			case 340 -> shiftKeyEvent(event.state); // Shift
		}

		// Pressed specific
		if (event.state == EKeyState.PRESSED)
		{
			switch (event.key)
			{
				case 32 -> application.setPaused(!application.isPaused()); // space bar
				case 331 -> doubleSpeed(); // *
				case 332 -> halfSpeed(); // /
				case 333 -> smallerBrush(); // -
				case 334 -> biggerBrush(); // +
				case 'n' - 32 -> application.setNextMode(true); // n
				case 'f' - 32 -> application.getScene().setFullscreen(!application.getScene().isFullscreen());
				case 256 -> application.setRun(false); // Escape
				case 's' - 32 -> showDebug();
			}
		}
		else
		{
			switch (event.key)
			{
				case 's' - 32 -> application.setForceClear(false);
			}
		}
	}

	private void shiftKeyEvent(final EKeyState state)
	{
		if (state == EKeyState.PRESSED)
		{
			shiftPressed = true;
		}
		else if (state == EKeyState.RELEASED)
		{
			shiftPressed = false;
		}
	}

	private void showDebug()
	{
		application.setForceClear(true);
		application.setShowSleepZones(!application.isShowSleepZones());
	}

	private void biggerBrush()
	{
		final int current = application.getBrushSize();
		final int brushPlus = Math.min(8, current + 1);
		application.setBrushSize(brushPlus);
	}

	private void smallerBrush()
	{
		final int current = application.getBrushSize();
		final int brushMinus = Math.max(1, current - 1);
		application.setBrushSize(brushMinus);
	}

	private void halfSpeed()
	{
		final int current = application.getSpeed();
		final int speedPlus = Math.min(16, current * 2);
		application.setSpeed(speedPlus);
	}

	private void doubleSpeed()
	{
		final int current = application.getSpeed();
		final int speedMinus = Math.max(1, current / 2);
		application.setSpeed(speedMinus);
	}

	private void onScrollEvent(ScrollEvent event)
	{
		final var mainMaterial = shiftPressed ? application.getSecondaryMaterial() : application.getMainMaterial();
		final boolean goDown = event.yOffset > 0f;
		final var next = findMaterial(mainMaterial, goDown);

		if (next != null)
		{
			if (shiftPressed)
			{
				application.setSecondaryMaterial(next);
			}
			else
			{
				application.setMainMaterial(next);
			}
		}
	}

	private Material findMaterial(Material mainMaterial, boolean needNext)
	{
		final var materials = application.getMaterials().getMaterials();
		final int index = materials.indexOf(mainMaterial);
		if (needNext)
		{
			return findNextUserFriendlyMaterial(materials, index, -1);
		}
		else
		{
			return findNextUserFriendlyMaterial(materials, index, 1);
		}
	}

	private static Material findNextUserFriendlyMaterial(EList<Material> materials, int index, int direction)
	{
		Material next;
		do
		{
			index += direction;
			if (index < 0)
			{
				index = materials.size() - 1;
			}
			else if (index == materials.size())
			{
				index = 0;
			}
			next = materials.get(index);
		} while (next.isUserFriendly() == false);

		return next;
	}
}
