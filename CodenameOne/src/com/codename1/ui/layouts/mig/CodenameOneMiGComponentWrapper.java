package com.codename1.ui.layouts.mig;
        
/*
 * License (BSD):
 * ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (miglayout (at) miginfocom (dot) com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 * Neither the name of the MiG InfoCom AB nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 * @version 1.0
 * @author Mikael Grev, MiG InfoCom AB
 *         Date: 2006-sep-08
 */

import com.codename1.components.InfiniteProgress;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Font;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Slider;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.mig.ComponentWrapper;
import com.codename1.ui.layouts.mig.ContainerWrapper;
import com.codename1.ui.layouts.mig.LayoutUtil;
import com.codename1.ui.layouts.mig.PlatformDefaults;
import com.codename1.ui.spinner.BaseSpinner;
import com.codename1.ui.table.Table;

import java.util.IdentityHashMap;

/**
 */
class CodenameOneMiGComponentWrapper implements ComponentWrapper
{
	private static boolean maxSet = false;

	private static boolean vp = true;

	/** Debug color for component bounds outline.
	 */
	//private static final Color DB_COMP_OUTLINE = new Color(0, 0, 200);

	/** Property to use in LAF settings and as JComponent client property
	 * to specify the visual padding.
	 * <p>
	 */
	private static final String VISUAL_PADDING_PROPERTY = com.codename1.ui.layouts.mig.PlatformDefaults.VISUAL_PADDING_PROPERTY;

	private final Component c;
	private int compType = TYPE_UNSET;
	private Boolean bl = null;
	private boolean prefCalled = false;

	public CodenameOneMiGComponentWrapper(Component c)
	{
		this.c = c;
	}

	public final int getBaseline(int width, int height)
	{
		int baseLine = c.getBaseline(width < 0 ? c.getWidth() : width, height < 0 ? c.getHeight() : height);
		if (baseLine != -1) {
			int[] visPad = getVisualPadding();
			if (visPad != null)
				baseLine += (visPad[2] - visPad[0] + 1) / 2;
		}
		return baseLine;
	}

	public final Object getComponent()
	{
		return c;
	}

	/** Cache.
	 */
	//private final static IdentityHashMap<FontMetrics, Point.Float> FM_MAP = new IdentityHashMap<FontMetrics, Point.Float>(4);
	private final static Font SUBST_FONT = Font.getDefaultFont();

	public final float getPixelUnitFactor(boolean isHor)
	{
		switch (PlatformDefaults.getLogicalPixelBase()) {
			case PlatformDefaults.BASE_FONT_SIZE:
				/*Font font = c.getFont();
				FontMetrics fm = c.getFontMetrics(font != null ? font : SUBST_FONT);
				Point.Float p = FM_MAP.get(fm);
				if (p == null) {
					Rectangle2D r = fm.getStringBounds("X", c.getGraphics());
					p = new Point.Float(((float) r.getWidth()) / 6f, ((float) r.getHeight()) / 13.27734375f);
					FM_MAP.put(fm, p);
				}
				return isHor ? p.x : p.y;*/
                            return isHor ? ((float)SUBST_FONT.charWidth('X')) /6f : ((float)SUBST_FONT.getHeight() / 13.27734375f);

			case PlatformDefaults.BASE_SCALE_FACTOR:

				Float s = isHor ? PlatformDefaults.getHorizontalScaleFactor() : PlatformDefaults.getVerticalScaleFactor();
				if (s == null)
					s = new Float(1.0f);
				return s * (isHor ? getHorizontalScreenDPI() : getVerticalScreenDPI()) / (float) PlatformDefaults.getDefaultDPI();

			default:
				return 1f;
		}
	}


	public final int getX()
	{
		return c.getX();
	}

	public final int getY()
	{
		return c.getY();
	}

	public final int getHeight()
	{
		return c.getHeight();
	}

	public final int getWidth()
	{
		return c.getWidth();
	}

	public final int getScreenLocationX()
	{
            return 0;
	}

	public final int getScreenLocationY()
	{
            return 0;
	}

	public final int getMinimumHeight(int sz)
	{
		return c.getPreferredH();
	}

	public final int getMinimumWidth(int sz)
	{
		return c.getPreferredW();
	}
	public final int getPreferredHeight(int sz)
	{
		return c.getPreferredH();
	}

	public final int getPreferredWidth(int sz)
	{
		return c.getPreferredW();
	}

	public final int getMaximumHeight(int sz)
	{
            Container p = c.getParent();
            if(p != null) {
                int w = p.getHeight();
                if(w > 10) {
                    return w;
                }
            }
            return Display.getInstance().getDisplayHeight();
	}

	public final int getMaximumWidth(int sz)
	{
            Container p = c.getParent();
            if(p != null) {
                int w = p.getWidth();
                if(w > 10) {
                    return w;
                }
            }
            return Display.getInstance().getDisplayWidth();
	}


	private boolean isMaxSet(Component c)
	{
		return false;
	}

	public final ContainerWrapper getParent()
	{
                Container p = c.getParent();
		return p != null ? new CodenameOneMiGContainerWrapper(p) : null;
	}

        public final int getHorizontalScreenDPI() {
            return PlatformDefaults.getDefaultDPI();
        }

	public final int getVerticalScreenDPI()
	{
            return PlatformDefaults.getDefaultDPI();
	}

	public final int getScreenWidth()
	{
            return Display.getInstance().getDisplayWidth();
	}

	public final int getScreenHeight()
	{
            return Display.getInstance().getDisplayHeight();
	}

	public final boolean hasBaseline()
	{
		return true;
	}

	public final String getLinkId()
	{
		return c.getName();
	}

	public final void setBounds(int x, int y, int width, int height)
	{
		c.setX(x);
		c.setY(y);
		c.setWidth(width);
		c.setHeight(height);
	}

	public boolean isVisible()
	{
		return c.isVisible();
	}

	public final int[] getVisualPadding()
	{
                // TOOD, optimize this
                        int[] padding = new int[] {c.getStyle().getMarginTop(), c.getStyle().getMarginLeftNoRTL(), 
                            c.getStyle().getMarginBottom(), c.getStyle().getMarginRightNoRTL()};
                        return padding;
	}

	/**
	 * @deprecated Java 1.4 is not supported anymore
	 */
	public static boolean isMaxSizeSetOn1_4()
	{
		return maxSet;
	}

	/**
	 * @deprecated Java 1.4 is not supported anymore
	 */
	public static void setMaxSizeSetOn1_4(boolean b)
	{
		maxSet = b;
	}

	public static boolean isVisualPaddingEnabled()
	{
		return vp;
	}

	public static void setVisualPaddingEnabled(boolean b)
	{
		vp = b;
	}

	public final void paintDebugOutline(boolean showVisualPadding)
	{
		/*if (c.isShowing() == false)
			return;

		Graphics2D g = (Graphics2D) c.getGraphics();
		if (g == null)
			return;

		g.setPaint(DB_COMP_OUTLINE);
		g.setStroke(new BasicStroke(1f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10f, new float[] {2f, 4f}, 0));
		g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

		if (showVisualPadding && isVisualPaddingEnabled()) {
			int[] padding = getVisualPadding();
			if (padding != null) {
				g.setColor(Color.GREEN);
				g.drawRect(padding[1], padding[0], (getWidth() - 1) - (padding[1] + padding[3]), (getHeight() - 1) - (padding[0] + padding[2]));
			}
		}*/
	}

	public int getComponentType(boolean disregardScrollPane)
	{
		if (compType == TYPE_UNSET)
			compType = checkType(disregardScrollPane);

		return compType;
	}

	public int getLayoutHashCode()
	{
		Dimension d = c.getPreferredSize();
		int hash = (d.getWidth() << 10) + (d.getHeight() << 15);

		if (c.isVisible())
			hash += 1324511;

		String id = getLinkId();
		if (id != null)
			hash += id.hashCode();

		return hash;
	}

	private int checkType(boolean disregardScrollPane)
	{
		Component c = this.c;

		if (c instanceof TextField) {
			return TYPE_TEXT_FIELD;
		} else if (c instanceof Label) {
			return TYPE_LABEL;
		} else if (c instanceof RadioButton || c instanceof CheckBox) {
			return TYPE_CHECK_BOX;
		} else if (c instanceof Button) {
			return TYPE_BUTTON;
		} else if (c instanceof ComboBox) {
			return TYPE_COMBO_BOX;
		} else if (c instanceof TextArea) {
			return TYPE_TEXT_AREA;
		} else if (c instanceof Container) {
			return TYPE_PANEL;
		} else if (c instanceof List) {
			return TYPE_LIST;
		} else if (c instanceof Table) {
			return TYPE_TABLE;
		} else if (c instanceof BaseSpinner) {
			return TYPE_SPINNER;
		} else if (c instanceof Tabs) {
			return TYPE_TABBED_PANE;
		} else if (c instanceof InfiniteProgress) {
			return TYPE_PROGRESS_BAR;
		} else if (c instanceof Slider) {
			return TYPE_SLIDER;
		} 
                return TYPE_UNKNOWN;
	}

	public final int hashCode()
	{
		return getComponent().hashCode();
	}

	public final boolean equals(Object o)
	{
		if (o instanceof ComponentWrapper == false)
			return false;

		return c.equals(((ComponentWrapper) o).getComponent());
	}

	public int getContentBias()
	{
		return c instanceof TextArea || (Boolean.TRUE.equals(((Component)c).getClientProperty("migLayout.dynamicAspectRatio"))) ? LayoutUtil.HORIZONTAL : -1;
	}
}

