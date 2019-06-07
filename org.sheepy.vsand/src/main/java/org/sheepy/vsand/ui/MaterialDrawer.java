package org.sheepy.vsand.ui;

import static org.lwjgl.nuklear.Nuklear.*;

import java.nio.ByteBuffer;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.sheepy.vsand.model.Material;
import org.sheepy.vsand.ui.MaterialSelectorPanelAdapter.LineData;

public final class MaterialDrawer
{
	private static final int STYLE = NK_WINDOW_NO_SCROLLBAR | NK_WINDOW_BORDER;

	private final int lineHeight;
	private final NkColor defaultBorderColor;
	private final NkColor selectedBorderColor;

	private float defaultPaddingX;
	private float defaultPaddingY;

	private NkColor borderColor;
	private NkVec2 padding;

	public MaterialDrawer(int lineHeight, int r, int g, int b)
	{
		this.lineHeight = lineHeight;

		defaultBorderColor = NkColor.create();
		selectedBorderColor = NkColor.create();
		selectedBorderColor.set((byte) r, (byte) g, (byte) b, (byte) 255);
	}

	public void prepare(NkContext nkContext)
	{
		borderColor = nkContext.style().window().border_color();
		defaultBorderColor.set(borderColor);

		padding = nkContext.style().window().padding();

		defaultPaddingX = padding.x();
		defaultPaddingY = padding.y();
	}

	public boolean draw(NkContext nkContext,
						LineData data,
						Material main,
						ByteBuffer id,
						NkRect rect)
	{
		boolean res = false;
		padding.x(0);
		padding.y(0);

		if (main == data.material)
		{
			borderColor.set(selectedBorderColor);
		}
		else
		{
			borderColor.a((byte) 0);
		}

		if (nk_begin(nkContext, id, rect, STYLE))
		{
			final var color = data.color;
			nk_layout_row_dynamic(nkContext, lineHeight - 5, 1);
			if (nk_button_color(nkContext, color))
			{
				res = true;
			}
		}
		nk_end(nkContext);

		if (main == data.material)
		{
			borderColor.set(defaultBorderColor);
			borderColor.a((byte) 0);
		}

		padding.x(defaultPaddingX);
		padding.y(defaultPaddingY);

		return res;
	}

	public void finish()
	{
		borderColor.a(defaultBorderColor.a());
	}
}