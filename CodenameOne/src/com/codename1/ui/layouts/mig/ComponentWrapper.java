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

/** A class that wraps the important parts of a Component.
 * <p>
 * <b>NOTE!</b>.equals() and .hashcode() should be forwarded to the wrapped component. E.g.
 * <pre>
 * 	public int hashCode()
	{
		return getComponent().hashCode();
	}

	public final boolean equals(Object o)
	{
		 if (o instanceof ComponentWrapper == false)
			 return false;

		 return getComponent().equals(((ComponentWrapper) o).getComponent());
	}
 * </pre>
 */
public interface ComponentWrapper
{
	static final int TYPE_UNSET = -1;
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_CONTAINER = 1;
	public static final int TYPE_LABEL = 2;
	public static final int TYPE_TEXT_FIELD = 3;
	public static final int TYPE_TEXT_AREA = 4;
	public static final int TYPE_BUTTON = 5;
	public static final int TYPE_LIST = 6;
	public static final int TYPE_TABLE = 7;
	public static final int TYPE_SCROLL_PANE = 8;
	public static final int TYPE_IMAGE = 9;
	public static final int TYPE_PANEL = 10;
	public static final int TYPE_COMBO_BOX = 11;
	public static final int TYPE_SLIDER = 12;
	public static final int TYPE_SPINNER = 13;
	public static final int TYPE_PROGRESS_BAR = 14;
	public static final int TYPE_TREE = 15;
	public static final int TYPE_CHECK_BOX = 16;
	public static final int TYPE_SCROLL_BAR = 17;
	public static final int TYPE_SEPARATOR = 18;
	public static final int TYPE_TABBED_PANE = 19;

	/** Returns the actual object that this wrapper is aggregating. This might be needed for getting
	 * information about the object that the wrapper interface does not provide.
	 * <p>
	 * If this is a container the container should be returned instead.
	 * @return The actual object that this wrapper is aggregating. Not <code>null</code>.
	 */
	public abstract Object getComponent();

	/** Returns the current x coordinate for this component.
	 * @return The current x coordinate for this component.
	 */
	public abstract int getX();

	/** Returns the current y coordinate for this component.
	 * @return The current y coordinate for this component.
	 */
	public abstract int getY();

	/** Returns the current width for this component.
	 * @return The current width for this component.
	 */
	public abstract int getWidth();

	/** Returns the current height for this component.
	 * @return The current height for this component.
	 */
	public abstract int getHeight();

	/** Returns the screen x-coordinate for the upper left coordinate of the component layout-able bounds.
	 * @return The screen x-coordinate for the upper left coordinate of the component layout-able bounds.
	 */
	public abstract int getScreenLocationX();

	/** Returns the screen y-coordinate for the upper left coordinate of the component layout-able bounds.
	 * @return The screen y-coordinate for the upper left coordinate of the component layout-able bounds.
	 */
	public abstract int getScreenLocationY();

	/** Returns the minimum width of the component.
	 * @param hHint The Size hint for the other dimension. An implementation can use this value or the
	 * current size for the widget in this dimension, or a combination of both, to calculate the correct size.<br>
	 * Use -1 to denote that there is no hint. This corresponds with SWT.DEFAULT.
	 * @return The minimum width of the component.
	 * @since 3.5. Added the hint as a parameter knowing that a correction and recompilation is necessary for
	 * any implementing classes. This change was worth it though.
	 */
	public abstract int getMinimumWidth(int hHint);

	/** Returns the minimum height of the component.
	 * @param wHint The Size hint for the other dimension. An implementation can use this value or the
	 * current size for the widget in this dimension, or a combination of both, to calculate the correct size.<br>
	 * Use -1 to denote that there is no hint. This corresponds with SWT.DEFAULT.
	 * @return The minimum height of the component.
	 * @since 3.5. Added the hint as a parameter knowing that a correction and recompilation is necessary for
	 * any implementing classes. This change was worth it though.
	 */
	public abstract int getMinimumHeight(int wHint);

	/** Returns the preferred width of the component.
	 * @param hHint The Size hint for the other dimension. An implementation can use this value or the
	 * current size for the widget in this dimension, or a combination of both, to calculate the correct size.<br>
	 * Use -1 to denote that there is no hint. This corresponds with SWT.DEFAULT.
	 * @return The preferred width of the component.
	 * @since 3.5. Added the hint as a parameter knowing that a correction and recompilation is necessary for
	 * any implementing classes. This change was worth it though.
	 */
	public abstract int getPreferredWidth(int hHint);

	/** Returns the preferred height of the component.
	 * @param wHint The Size hint for the other dimension. An implementation can use this value or the
	 * current size for the widget in this dimension, or a combination of both, to calculate the correct size.<br>
	 * Use -1 to denote that there is no hint. This corresponds with SWT.DEFAULT.
	 * @return The preferred height of the component.
	 * @since 3.5. Added the hint as a parameter knowing that a correction and recompilation is necessary for
	 * any implementing classes. This change was worth it though.
	 */
	public abstract int getPreferredHeight(int wHint);

	/** Returns the maximum width of the component.
	 * @param hHint The Size hint for the other dimension. An implementation can use this value or the
	 * current size for the widget in this dimension, or a combination of both, to calculate the correct size.<br>
	 * Use -1 to denote that there is no hint. This corresponds with SWT.DEFAULT.
	 * @return The maximum width of the component.
	 * @since 3.5. Added the hint as a parameter knowing that a correction and recompilation is necessary for
	 * any implementing classes. This change was worth it though.
	 */
	public abstract int getMaximumWidth(int hHint);

	/** Returns the maximum height of the component.
	 * @param wHint The Size hint for the other dimension. An implementation can use this value or the
	 * current size for the widget in this dimension, or a combination of both, to calculate the correct size.<br>
	 * Use -1 to denote that there is no hint. This corresponds with SWT.DEFAULT.
	 * @return The maximum height of the component.
	 * @since 3.5. Added the hint as a parameter knowing that a correction and recompilation is necessary for
	 * any implementing classes. This change was worth it though.
	 */
	public abstract int getMaximumHeight(int wHint);

	/** Sets the component's bounds.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param width The width.
	 * @param height The height.
	 */
	public abstract void setBounds(int x, int y, int width, int height);

	/** Returns if the component's visibility is set to <code>true</code>. This should not return if the component is
	 * actually visible, but if the visibility is set to true or not.
	 * @return <code>true</code> means visible.
	 */
	public abstract boolean isVisible();

	/** Returns the baseline for the component given the suggested height.
	 * @param width The width to calculate for if other than the current. If <code>-1</code> the current size should be used.
	 * @param height The height to calculate for if other than the current. If <code>-1</code> the current size should be used.
	 * @return The baseline from the top or -1 if not applicable.
	 */
	public abstract int getBaseline(int width, int height);

	/** Returns if the component has a baseline and if it can be retrieved. Should for instance return
	 * <code>false</code> for Swing before mustang.
	 * @return If the component has a baseline and if it can be retrieved.
	 */
	public abstract boolean hasBaseline();

	/** Returns the container for this component.
	 * @return The container for this component. Will return <code>null</code> if the component has no parent.
	 */
	public abstract ContainerWrapper getParent();

	/** Returns the pixel unit factor for the horizontal or vertical dimension.
	 * <p>
	 * The factor is 1 for both dimensions on the normal font in a JPanel on Windows. The factor should increase with a bigger "X".
	 * <p>
	 * This is the Swing version:
	 * <pre>
	 * Rectangle2D r = fm.getStringBounds("X", parent.getGraphics());
	 * wFactor = r.getWidth() / 6;
	 * hFactor = r.getHeight() / 13.27734375f;
	 * </pre>
	 * @param isHor If it is the horizontal factor that should be returned.
	 * @return The factor.
	 */
	public abstract float getPixelUnitFactor(boolean isHor);

	/** Returns the DPI (Dots Per Inch) of the screen the component is currently in or for the default
	 * screen if the component is not visible.
	 * <p>
	 * If headless mode {@link net.miginfocom.layout.PlatformDefaults#getDefaultDPI} will be returned.
	 * @return The DPI.
	 */
	public abstract int getHorizontalScreenDPI();

	/** Returns the DPI (Dots Per Inch) of the screen the component is currently in or for the default
	 * screen if the component is not visible.
	 * <p>
	 * If headless mode {@link net.miginfocom.layout.PlatformDefaults#getDefaultDPI} will be returned.
	 * @return The DPI.
	 */
	public abstract int getVerticalScreenDPI();

	/** Returns the pixel size of the screen that the component is currently in or for the default
	 * screen if the component is not visible or <code>null</code>.
	 * <p>
	 * If in headless mode <code>1024</code> is returned.
	 * @return The screen size. E.g. <code>1280</code>.
	 */
	public abstract int getScreenWidth();

	/** Returns the pixel size of the screen that the component is currently in or for the default
	 * screen if the component is not visible or <code>null</code>.
	 * <p>
	 * If in headless mode <code>768</code> is returned.
	 * @return The screen size. E.g. <code>1024</code>.
	 */
	public abstract int getScreenHeight();

	/** Returns a String id that can be used to reference the component in link constraints. This value should
	 * return the default id for the component. The id can be set for a component in the constraints and if
	 * so the value returned by this method will never be used. If there are no sensible id for the component
	 * <code>null</code> should be returned.
	 * <p>
	 * For instance the Swing implementation returns the string returned from <code>Component.getName()</code>.
	 * @return The string link id or <code>null</code>.
	 */
	public abstract String getLinkId();

	/** Returns a hash code that should be reasonably different for anything that might change the layout. This value is used to
	 *  know if the component layout needs to clear any caches.
	 * @return A hash code that should be reasonably different for anything that might change the layout. Returns -1 if the widget is
	 * disposed.
	 */
	public abstract int getLayoutHashCode();

	/** Returns the padding on a component by component basis. This method can be overridden to return padding to compensate for example for
	 * borders that have shadows or where the outer most pixel is not the visual "edge" to align to.
	 * <p>
	 * Default implementation returns <code>null</code> for all components except for Windows XP's JTabbedPane which will return new Insets(0, 0, 2, 2).
	 * <p>
	 * <b>NOTE!</B> To reduce generated garbage the returned padding should never be changed so that the same insets can be returned many times.
	 * @return <code>null</code> if no padding. <b>NOTE!</B> To reduce generated garbage the returned padding should never be changed so that
	 * the same insets can be returned many times. [top, left, bottom, right]
	 */
	public int[] getVisualPadding();

	/** Paints component outline to indicate where it is.
	 * @param showVisualPadding If the visual padding should be shown in the debug drawing.
	 */
	public abstract void paintDebugOutline(boolean showVisualPadding);

	/** Returns the type of component that this wrapper is wrapping.
	 * <p>
	 * This method can be invoked often so the result should be cached.
	 * <p>
	 * @param disregardScrollPane Is <code>true</code> any wrapping scroll pane should be disregarded and the type
	 * of the scrolled component should be returned.
	 * @return The type of component that this wrapper is wrapping. E.g. {@link #TYPE_LABEL}.
	 */
	public abstract int getComponentType(boolean disregardScrollPane);

	/** Returns in what way the min/pref/max sizes relates to it's height or width for the current settings of the component (like wrapText).
	 * If the min/pref/max height depends on it's width return {@link net.miginfocom.layout.LayoutUtil#HORIZONTAL}
	 * If the min/pref/max width depends on it's height (not common) return {@link net.miginfocom.layout.LayoutUtil#VERTICAL}
	 * If there is no connection between the preferred min/pref/max and the size of the component return -1.
	 * @since 5.0
	 */
	public abstract int getContentBias();
}