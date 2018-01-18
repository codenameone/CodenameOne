package com.codename1.ui.layouts.mig;

import java.util.ArrayList;
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

/** A simple value holder for one component's constraint.
 */
public final class CC 
{
	private static final BoundSize DEF_GAP = BoundSize.NULL_SIZE;    // Only used to denote default wrap/newline gap.

	static final String[] DOCK_SIDES = {"north", "west", "south", "east"};

	// See the getters and setters for information about the properties below.

	private int dock = -1;

	private UnitValue[] pos = null; // [x1, y1, x2, y2]

	private UnitValue[] padding = null;   // top, left, bottom, right

	private UnitValue[] visualPadding = null;   // top, left, bottom, right

	private Boolean flowX = null;

	private int skip = 0;

	private int split = 1;

	private int spanX = 1, spanY = 1;

	private int cellX = -1, cellY = 0; // If cellX is -1 then cellY is also considered -1. cellY is never negative.

	private String tag = null;

	private String id = null;

	private int hideMode = -1;

	private DimConstraint hor = new DimConstraint();

	private DimConstraint ver = new DimConstraint();

	private BoundSize newline = null;

	private BoundSize wrap = null;

	private boolean boundsInGrid = true;

	private boolean external = false;

	private Float pushX = null, pushY = null;


	// ***** Tmp cache field

	private static final String[] EMPTY_ARR = new String[0];

	private transient String[] linkTargets = null;

	/** Empty constructor.
	 */
	public CC()
	{
	}

	String[] getLinkTargets()
	{
		if (linkTargets == null) {
			final ArrayList<String> targets = new ArrayList<String>(2);

			if (pos != null) {
				for (int i = 0; i < pos.length ; i++)
					addLinkTargetIDs(targets, pos[i]);
			}

			linkTargets = targets.size() == 0 ? EMPTY_ARR : targets.toArray(new String[targets.size()]);
		}
		return linkTargets;
	}

	private void addLinkTargetIDs(ArrayList<String> targets, UnitValue uv)
	{
		if (uv != null) {
			String linkId = uv.getLinkTargetId();
			if (linkId != null) {
				targets.add(linkId);
			} else {
				for (int i = uv.getSubUnitCount() - 1; i >= 0; i--) {
					UnitValue subUv = uv.getSubUnitValue(i);
					if (subUv.isLinkedDeep())
						addLinkTargetIDs(targets, subUv);
				}
			}
		}
	}

	// **********************************************************
	// Chaining constraint setters
	// **********************************************************

	/** Specifies that the component should be put in the end group <code>s</code> and will thus share the same ending
	 * coordinate as them within the group.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param s A name to associate on the group that should be the same for other rows/columns in the same group.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC endGroupX(String s)
	{
		hor.setEndGroup(s);
		return this;
	}

	/** Specifies that the component should be put in the size group <code>s</code> and will thus share the same size
	 * as them within the group.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param s A name to associate on the group that should be the same for other rows/columns in the same group.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC sizeGroupX(String s)
	{
		hor.setSizeGroup(s);
		return this;
	}

	/** The minimum size for the component. The value will override any value that is set on the component itself.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The size expressed as a <code>UnitValue</code>. E.g. "100px" or "200mm".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC minWidth(String size)
	{
		hor.setSize(LayoutUtil.derive(hor.getSize(), ConstraintParser.parseUnitValue(size, true), null, null));
		return this;
	}

	/** The size for the component as a min and/or preferred and/or maximum size. The value will override any value that is set on
	 * the component itself.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The size expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC width(String size)
	{
		hor.setSize(ConstraintParser.parseBoundSize(size, false, true));
		return this;
	}

	/** The maximum size for the component. The value will override any value that is set on the component itself.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The size expressed as a <code>UnitValue</code>. E.g. "100px" or "200mm".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC maxWidth(String size)
	{
		hor.setSize(LayoutUtil.derive(hor.getSize(), null, null, ConstraintParser.parseUnitValue(size, true)));
		return this;
	}


	/** The horizontal gap before and/or after the component. The gap is towards cell bounds and/or other component bounds.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param before The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @param after The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC gapX(String before, String after)
	{
		if (before != null)
			hor.setGapBefore(ConstraintParser.parseBoundSize(before, true, true));

		if (after != null)
			hor.setGapAfter(ConstraintParser.parseBoundSize(after, true, true));

		return this;
	}

	/** Same functionality as <code>getHorizontal().setAlign(ConstraintParser.parseUnitValue(unitValue, true))</code> only this method
	 * returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param align The align keyword or for instance "100px". E.g "left", "right", "leading" or "trailing".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC alignX(String align)
	{
		hor.setAlign(ConstraintParser.parseUnitValueOrAlign(align, true, null));
		return this;
	}

	/** The grow priority compared to other components in the same cell.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param p The grow priority.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC growPrioX(int p)
	{
		hor.setGrowPriority(p);
		return this;
	}

	/** Grow priority for the component horizontally and optionally vertically.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param widthHeight The new shrink weight and height. 1-2 arguments, never null.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC growPrio(int ... widthHeight)
	{
		switch (widthHeight.length) {
			default:
				throw new IllegalArgumentException("Illegal argument count: " + widthHeight.length);
			case 2:
				growPrioY(widthHeight[1]);
			case 1:
				growPrioX(widthHeight[0]);
		}
		return this;
	}

	/** Grow weight for the component horizontally. It default to weight <code>100</code>.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #growX(float)
	 */
	public final CC growX()
	{
		hor.setGrow(ResizeConstraint.WEIGHT_100);
		return this;
	}

	/** Grow weight for the component horizontally.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param w The new grow weight.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC growX(float w)
	{
		hor.setGrow(new Float(w));
		return this;
	}

	/** grow weight for the component horizontally and optionally vertically.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param widthHeight The new shrink weight and height. 1-2 arguments, never null.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC grow(float ... widthHeight)
	{
		switch (widthHeight.length) {
			default:
				throw new IllegalArgumentException("Illegal argument count: " + widthHeight.length);
			case 2:
				growY(new Float(widthHeight[1]));
			case 1:
				growX(new Float(widthHeight[0]));
		}
		return this;
	}

	/** The shrink priority compared to other components in the same cell.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param p The shrink priority.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC shrinkPrioX(int p)
	{
		hor.setShrinkPriority(p);
		return this;
	}

	/** Shrink priority for the component horizontally and optionally vertically.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param widthHeight The new shrink weight and height. 1-2 arguments, never null.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC shrinkPrio(int ... widthHeight)
	{
		switch (widthHeight.length) {
			default:
				throw new IllegalArgumentException("Illegal argument count: " + widthHeight.length);
			case 2:
				shrinkPrioY(widthHeight[1]);
			case 1:
				shrinkPrioX(widthHeight[0]);
		}
		return this;
	}

	/** Shrink weight for the component horizontally.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param w The new shrink weight.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC shrinkX(float w)
	{
		hor.setShrink(new Float(w));
		return this;
	}

	/** Shrink weight for the component horizontally and optionally vertically.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param widthHeight The new shrink weight and height. 1-2 arguments, never null.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC shrink(float ... widthHeight)
	{
		switch (widthHeight.length) {
			default:
				throw new IllegalArgumentException("Illegal argument count: " + widthHeight.length);
			case 2:
				shrinkY(widthHeight[1]);
			case 1:
				shrinkX(widthHeight[0]);
		}
		return this;
	}

	/** The end group that this component should be placed in.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param s The name of the group. If <code>null</code> that means no group (default)
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC endGroupY(String s)
	{
		ver.setEndGroup(s);
		return this;
	}

	/** The end group(s) that this component should be placed in.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param xy The end group for x and y respectively. 1-2 arguments, not null.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC endGroup(String ... xy)
	{
		switch (xy.length) {
			default:
				throw new IllegalArgumentException("Illegal argument count: " + xy.length);
			case 2:
				endGroupY(xy[1]);
			case 1:
				endGroupX(xy[0]);
		}
		return this;
	}

	/** The size group that this component should be placed in.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param s The name of the group. If <code>null</code> that means no group (default)
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC sizeGroupY(String s)
	{
		ver.setSizeGroup(s);
		return this;
	}

	/** The size group(s) that this component should be placed in.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param xy The size group for x and y respectively. 1-2 arguments, not null.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC sizeGroup(String ... xy)
	{
		switch (xy.length) {
			default:
				throw new IllegalArgumentException("Illegal argument count: " + xy.length);
			case 2:
				sizeGroupY(xy[1]);
			case 1:
				sizeGroupX(xy[0]);
		}
		return this;
	}

	/** The minimum size for the component. The value will override any value that is set on the component itself.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The size expressed as a <code>UnitValue</code>. E.g. "100px" or "200mm".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC minHeight(String size)
	{
		ver.setSize(LayoutUtil.derive(ver.getSize(), ConstraintParser.parseUnitValue(size, false), null, null));
		return this;
	}

	/** The size for the component as a min and/or preferred and/or maximum size. The value will override any value that is set on
	 * the component itself.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The size expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC height(String size)
	{
		ver.setSize(ConstraintParser.parseBoundSize(size, false, false));
		return this;
	}

	/** The maximum size for the component. The value will override any value that is set on the component itself.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The size expressed as a <code>UnitValue</code>. E.g. "100px" or "200mm".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC maxHeight(String size)
	{
		ver.setSize(LayoutUtil.derive(ver.getSize(), null, null, ConstraintParser.parseUnitValue(size, false)));
		return this;
	}

	/** The vertical gap before (normally above) and/or after (normally below) the component. The gap is towards cell bounds and/or other component bounds.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param before The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @param after The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC gapY(String before, String after)
	{
		if (before != null)
			ver.setGapBefore(ConstraintParser.parseBoundSize(before, true, false));

		if (after != null)
			ver.setGapAfter(ConstraintParser.parseBoundSize(after, true, false));

		return this;
	}

	/** Same functionality as <code>getVertical().setAlign(ConstraintParser.parseUnitValue(unitValue, true))</code> only this method
	 * returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param align The align keyword or for instance "100px". E.g "top" or "bottom".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC alignY(String align)
	{
		ver.setAlign(ConstraintParser.parseUnitValueOrAlign(align, false, null));
		return this;
	}

	/** The grow priority compared to other components in the same cell.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param p The grow priority.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC growPrioY(int p)
	{
		ver.setGrowPriority(p);
		return this;
	}

	/** Grow weight for the component vertically. Defaults to <code>100</code>.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #growY(Float)
	 */
	public final CC growY()
	{
		ver.setGrow(ResizeConstraint.WEIGHT_100);
		return this;
	}

	/** Grow weight for the component vertically.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param w The new grow weight.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC growY(Float w)
	{
		ver.setGrow(w);
		return this;
	}

	/** The shrink priority compared to other components in the same cell.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param p The shrink priority.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC shrinkPrioY(int p)
	{
		ver.setShrinkPriority(p);
		return this;
	}

	/** Shrink weight for the component horizontally.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param w The new shrink weight.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC shrinkY(float w)
	{
		ver.setShrink(new Float(w));
		return this;
	}

	/** How this component, if hidden (not visible), should be treated.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param mode The mode. Default to the mode in the {@link net.miginfocom.layout.LC}.
	 * 0 == Normal. Bounds will be calculated as if the component was visible.<br>
	 * 1 == If hidden the size will be 0, 0 but the gaps remain.<br>
	 * 2 == If hidden the size will be 0, 0 and gaps set to zero.<br>
	 * 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC hideMode(int mode)
	{
		setHideMode(mode);
		return this;
	}

	/** The id used to reference this component in some constraints.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param s The id or <code>null</code>. May consist of a groupID and a componentID which are separated by a dot: ".". E.g. "grp1.id1".
	 * The dot should never be first or last if present.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 */
	public final CC id(String s)
	{
		setId(s);
		return this;
	}

	/** Same functionality as {@link #setTag(String tag)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param tag The new tag. May be <code>null</code>.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setTag(String)
	 */
	public final CC tag(String tag)
	{
		setTag(tag);
		return this;
	}

	/** Set the cell(s) that the component should occupy in the grid. Same functionality as {@link #setCellX(int col)} and
	 * {@link #setCellY(int row)} together with {@link #setSpanX(int width)} and {@link #setSpanY(int height)}. This method
	 * returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param colRowWidthHeight cellX, cellY, spanX, spanY repectively. 1-4 arguments, not null.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setCellX(int)
	 * @see #setCellY(int)
	 * @see #setSpanX(int)
	 * @see #setSpanY(int)
	 * @since 3.7.2. Replacing cell(int, int) and cell(int, int, int, int)
	 */
	public final CC cell(int ... colRowWidthHeight)
	{
		switch (colRowWidthHeight.length) {
			default:
				throw new IllegalArgumentException("Illegal argument count: " + colRowWidthHeight.length);
			case 4:
				setSpanY(colRowWidthHeight[3]);
			case 3:
				setSpanX(colRowWidthHeight[2]);
			case 2:
				setCellY(colRowWidthHeight[1]);
			case 1:
				setCellX(colRowWidthHeight[0]);
		}
		return this;
	}

	/** Same functionality as <code>spanX(cellsX).spanY(cellsY)</code> which means this cell will span cells in both x and y.
	 * This method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * Since 3.7.2 this takes an array/vararg whereas it previously only took two specific values, xSpan and ySpan.
	 * @param cells spanX and spanY, when present, and in that order.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setSpanY(int)
	 * @see #setSpanX(int)
	 * @see #spanY()
	 * @see #spanX()
	 * @since 3.7.2 Replaces span(int, int).
	 */
	public final CC span(int ... cells)
	{
		if (cells == null || cells.length == 0) {
			setSpanX(LayoutUtil.INF);
			setSpanY(1);
		} else if (cells.length == 1) {
			setSpanX(cells[0]);
			setSpanY(1);
		} else {
			setSpanX(cells[0]);
			setSpanY(cells[1]);
		}
		return this;
	}

	/** Corresponds exactly to the "gap left right top bottom" keyword.
	 * @param args Same as for the "gap" keyword. Length 1-4, never null buf elements can be null.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC gap(String ... args)
	{
		switch (args.length) {
			default:
				throw new IllegalArgumentException("Illegal argument count: " + args.length);
			case 4:
				gapBottom(args[3]);
			case 3:
				gapTop(args[2]);
			case 2:
				gapRight(args[1]);
			case 1:
				gapLeft(args[0]);
		}
		return this;
	}

	/** Sets the horizontal gap before the component.
	 * <p>
	 * Note! This is currently same as gapLeft(). This might change in 4.x.
	 * @param boundsSize The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC gapBefore(String boundsSize)
	{
		hor.setGapBefore(ConstraintParser.parseBoundSize(boundsSize, true, true));
		return this;
	}

	/** Sets the horizontal gap after the component.
	 * <p>
	 * Note! This is currently same as gapLeft(). This might change in 4.x.
	 * @param boundsSize The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC gapAfter(String boundsSize)
	{
		hor.setGapAfter(ConstraintParser.parseBoundSize(boundsSize, true, true));
		return this;
	}

	/** Sets the gap above the component.
	 * @param boundsSize The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC gapTop(String boundsSize)
	{
		ver.setGapBefore(ConstraintParser.parseBoundSize(boundsSize, true, false));
		return this;
	}

	/** Sets the gap to the left the component.
	 * @param boundsSize The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC gapLeft(String boundsSize)
	{
		hor.setGapBefore(ConstraintParser.parseBoundSize(boundsSize, true, true));
		return this;
	}

	/** Sets the gap below the component.
	 * @param boundsSize The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC gapBottom(String boundsSize)
	{
		ver.setGapAfter(ConstraintParser.parseBoundSize(boundsSize, true, false));
		return this;
	}

	/** Sets the gap to the right of the component.
	 * @param boundsSize The size of the gap expressed as a <code>BoundSize</code>. E.g. "50:100px:200mm" or "100px!".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @since 3.7.2
	 */
	public final CC gapRight(String boundsSize)
	{
		hor.setGapAfter(ConstraintParser.parseBoundSize(boundsSize, true, true));
		return this;
	}

	/** Same functionality as calling {@link #setSpanY(int)} with <code>LayoutUtil.INF</code> which means this cell will span the rest of the column.
	 * This method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setSpanY(int)
	 * @see #spanY()
	 */
	public final CC spanY()
	{
		return spanY(LayoutUtil.INF);
	}

	/** Same functionality as {@link #setSpanY(int)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param cells The number of cells to span (i.e. merge).
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setSpanY(int)
	 */
	public final CC spanY(int cells)
	{
		setSpanY(cells);
		return this;
	}

	/** Same functionality as {@link #setSpanX(int)} which means this cell will span the rest of the row.
	 * This method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setSpanX(int)
	 * @see #spanX()
	 */
	public final CC spanX()
	{
		return spanX(LayoutUtil.INF);
	}

	/** Same functionality as {@link #setSpanX(int)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param cells The number of cells to span (i.e. merge).
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setSpanY(int)
	 */
	public final CC spanX(int cells)
	{
		setSpanX(cells);
		return this;
	}

	/** Same functionality as <code>pushX().pushY()</code> which means this cell will push in both x and y dimensions.
	 * This method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setPushX(Float)
	 * @see #setPushX(Float)
	 * @see #pushY()
	 * @see #pushX()
	 */
	public final CC push()
	{
		return pushX().pushY();
	}

	/** Same functionality as <code>pushX(weightX).pushY(weightY)</code> which means this cell will push in both x and y dimensions.
	 * This method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param weightX The weight used in the push.
	 * @param weightY The weight used in the push.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setPushY(Float)
	 * @see #setPushX(Float)
	 * @see #pushY()
	 * @see #pushX()
	 */
	public final CC push(Float weightX, Float weightY)
	{
		return pushX(weightX).pushY(weightY);
	}

	/** Same functionality as {@link #setPushY(Float)} which means this cell will push the rest of the column.
	 * This method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setPushY(Float)
	 */
	public final CC pushY()
	{
		return pushY(ResizeConstraint.WEIGHT_100);
	}

	/** Same functionality as {@link #setPushY(Float weight)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param weight The weight used in the push.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setPushY(Float)
	 */
	public final CC pushY(Float weight)
	{
		setPushY(weight);
		return this;
	}

	/** Same functionality as {@link #setPushX(Float)} which means this cell will push the rest of the row.
	 * This method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setPushX(Float)
	 */
	public final CC pushX()
	{
		return pushX(ResizeConstraint.WEIGHT_100);
	}

	/** Same functionality as {@link #setPushX(Float weight)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param weight The weight used in the push.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new LayoutConstraint().noGrid().gap().fill()</code>.
	 * @see #setPushY(Float)
	 */
	public final CC pushX(Float weight)
	{
		setPushX(weight);
		return this;
	}

	/** Same functionality as {@link #setSplit(int parts)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param parts The number of parts (i.e. component slots) the cell should be divided into.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setSplit(int)
	 */
	public final CC split(int parts)
	{
		setSplit(parts);
		return this;
	}

	/** Same functionality as split(LayoutUtil.INF), which means split until one of the keywords that breaks the split is found for
	 * a component after this one (e.g. wrap, newline and skip).
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setSplit(int)
	 * @since 3.7.2
	 */
	public final CC split()
	{
		setSplit(LayoutUtil.INF);
		return this;
	}

	/** Same functionality as {@link #setSkip(int)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param cells How many cells in the grid that should be skipped <b>before</b> the component that this constraint belongs to
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setSkip(int)
	 */
	public final CC skip(int cells)
	{
		setSkip(cells);
		return this;
	}

	/** Same functionality as skip(1).
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setSkip(int)
	 * @since 3.7.2
	 */
	public final CC skip()
	{
		setSkip(1);
		return this;
	}

	/** Same functionality as calling {@link #setExternal(boolean)} with <code>true</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setExternal(boolean)
	 */
	public final CC external()
	{
		setExternal(true);
		return this;
	}

	/** Same functionality as calling {@link #setFlowX(Boolean)} with <code>Boolean.TRUE</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setFlowX(Boolean)
	 */
	public final CC flowX()
	{
		setFlowX(Boolean.TRUE);
		return this;
	}

	/** Same functionality as calling {@link #setFlowX(Boolean)} with <code>Boolean.FALSE</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setFlowX(Boolean)
	 */
	public final CC flowY()
	{
		setFlowX(Boolean.FALSE);
		return this;
	}


	/** Same functionality as {@link #growX()} and {@link #growY()}.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #growX()
	 * @see #growY()
	 */
	public final CC grow()
	{
		growX();
		growY();
		return this;
	}

	/** Same functionality as calling {@link #setNewline(boolean)} with <code>true</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setNewline(boolean)
	 */
	public final CC newline()
	{
		setNewline(true);
		return this;
	}

	/** Same functionality as {@link #setNewlineGapSize(BoundSize)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param gapSize The gap size that will override the gap size in the row/colum constraints if <code>!= null</code>. E.g. "5px" or "unrel".
	 * If <code>null</code> or <code>""</code> the newline size will be set to the default size and turned on. This is different compared to
	 * {@link #setNewlineGapSize(BoundSize)}.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setNewlineGapSize(BoundSize)
	 */
	public final CC newline(String gapSize)
	{
		BoundSize bs = ConstraintParser.parseBoundSize(gapSize, true, (flowX != null && flowX == false));
		if (bs != null) {
			setNewlineGapSize(bs);
		} else {
			setNewline(true);
		}
		return this;
	}

	/** Same functionality as calling {@link #setWrap(boolean)} with <code>true</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setWrap(boolean)
	 */
	public final CC wrap()
	{
		setWrap(true);
		return this;
	}

	/** Same functionality as {@link #setWrapGapSize(BoundSize)} only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param gapSize The gap size that will override the gap size in the row/colum constraints if <code>!= null</code>. E.g. "5px" or "unrel".
	 * If <code>null</code> or <code>""</code> the wrap size will be set to the default size and turned on. This is different compared to
	 * {@link #setWrapGapSize(BoundSize)}.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setWrapGapSize(BoundSize)
	 */
	public final CC wrap(String gapSize)
	{
		BoundSize bs = ConstraintParser.parseBoundSize(gapSize, true, (flowX != null && flowX == false));
		if (bs != null) {
			setWrapGapSize(bs);
		} else {
			setWrap(true);
		}
		return this;
	}

	/** Same functionality as calling {@link #setDockSide(int)} with <code>0</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setDockSide(int)
	 */
	public final CC dockNorth()
	{
		setDockSide(0);
		return this;
	}

	/** Same functionality as calling {@link #setDockSide(int)} with <code>1</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setDockSide(int)
	 */
	public final CC dockWest()
	{
		setDockSide(1);
		return this;
	}

	/** Same functionality as calling {@link #setDockSide(int)} with <code>2</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setDockSide(int)
	 */
	public final CC dockSouth()
	{
		setDockSide(2);
		return this;
	}

	/** Same functionality as calling {@link #setDockSide(int)} with <code>3</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setDockSide(int)
	 */
	public final CC dockEast()
	{
		setDockSide(3);
		return this;
	}

	/** Sets the x-coordinate for the component. This is used to set the x coordinate position to a specific value. The component
	 * bounds is still precalculated to the grid cell and this method should be seen as a way to correct the x position.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param x The x position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setPos(UnitValue[])
	 * @see #setBoundsInGrid(boolean)
	 */
	public final CC x(String x)
	{
		return corrPos(x, 0);
	}

	/** Sets the y-coordinate for the component. This is used to set the y coordinate position to a specific value. The component
	 * bounds is still precalculated to the grid cell and this method should be seen as a way to correct the y position.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param y The y position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setPos(UnitValue[])
	 * @see #setBoundsInGrid(boolean)
	 */
	public final CC y(String y)
	{
		return corrPos(y, 1);
	}

	/** Sets the x2-coordinate for the component (right side). This is used to set the x2 coordinate position to a specific value. The component
	 * bounds is still precalculated to the grid cell and this method should be seen as a way to correct the x position.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param x2 The x2 side's position as a UnitValue. E.g. "10" or "40mm" or "container.x2 - 10".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setPos(UnitValue[])
	 * @see #setBoundsInGrid(boolean)
	 */
	public final CC x2(String x2)
	{
		return corrPos(x2, 2);
	}

	/** Sets the y2-coordinate for the component (bottom side). This is used to set the y2 coordinate position to a specific value. The component
	 * bounds is still precalculated to the grid cell and this method should be seen as a way to correct the y position.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param y2 The y2 side's position as a UnitValue. E.g. "10" or "40mm" or "container.x2 - 10".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setPos(UnitValue[])
	 * @see #setBoundsInGrid(boolean)
	 */
	public final CC y2(String y2)
	{
		return corrPos(y2, 3);
	}

	private final CC corrPos(String uv, int ix)
	{
		UnitValue[] b = getPos();
		if (b == null)
			b = new UnitValue[4];

		b[ix] = ConstraintParser.parseUnitValue(uv, (ix % 2 == 0));
		setPos(b);

		setBoundsInGrid(true);
		return this;
	}

	/** Same functionality as {@link #x(String x)} and {@link #y(String y)} toghether.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param x The x position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
	 * @param y The y position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setPos(UnitValue[])
	 */
	public final CC pos(String x, String y)
	{
		UnitValue[] b = getPos();
		if (b == null)
			b = new UnitValue[4];

		b[0] = ConstraintParser.parseUnitValue(x, true);
		b[1] = ConstraintParser.parseUnitValue(y, false);
		setPos(b);

		setBoundsInGrid(false);
		return this;
	}

	/** Same functionality as {@link #x(String x)}, {@link #y(String y)}, {@link #y2(String y)} and {@link #y2(String y)} toghether.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param x The x position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
	 * @param y The y position as a UnitValue. E.g. "10" or "40mm" or "container.x+10".
	 * @param x2 The x2 side's position as a UnitValue. E.g. "10" or "40mm" or "container.x2 - 10".
	 * @param y2 The y2 side's position as a UnitValue. E.g. "10" or "40mm" or "container.x2 - 10".
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setPos(UnitValue[])
	 */
	public final CC pos(String x, String y, String x2, String y2)
	{
		setPos(new UnitValue[] {
				ConstraintParser.parseUnitValue(x, true),
				ConstraintParser.parseUnitValue(y, false),
				ConstraintParser.parseUnitValue(x2, true),
				ConstraintParser.parseUnitValue(y2, false),
		});
		setBoundsInGrid(false);
		return this;
	}

	/** Same functionality as {@link #setPadding(UnitValue[])} but the unit values as absolute pixels. This method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param top The top padding that will be added to the y coordinate at the last stage in the layout.
	 * @param left The top padding that will be added to the x coordinate at the last stage in the layout.
	 * @param bottom The top padding that will be added to the y2 coordinate at the last stage in the layout.
	 * @param right The top padding that will be added to the x2 coordinate at the last stage in the layout.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setTag(String)
	 */
	public final CC pad(int top, int left, int bottom, int right)
	{
		setPadding(new UnitValue[] {
				new UnitValue(top),	new UnitValue(left), new UnitValue(bottom), new UnitValue(right)
		});
		return this;
	}

	/** Same functionality as <code>setPadding(ConstraintParser.parseInsets(pad, false))}</code> only this method returns <code>this</code> for chaining multiple calls.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param pad The string to parse. E.g. "10 10 10 10" or "20". If less than 4 groups the last will be used for the missing.
	 * @return <code>this</code> so it is possible to chain calls. E.g. <code>new ComponentConstraint().noGrid().gap().fill()</code>.
	 * @see #setTag(String)
	 */
	public final CC pad(String pad)
	{
		setPadding(pad != null ? ConstraintParser.parseInsets(pad, false) : null);
		return this;
	}

	// **********************************************************
	// Bean properties
	// **********************************************************

	/** Returns the horizontal dimension constraint for this component constraint. It has constraints for the horizontal size
	 * and grow/shink priorities and weights.
	 * <p>
	 * Note! If any changes is to be made it must be made direct when the object is returned. It is not allowed to save the
	 * constraint for later use.
	 * @return The current dimension constraint. Never <code>null</code>.
	 */
	public DimConstraint getHorizontal()
	{
		return hor;
	}

	/** Sets the horizontal dimension constraint for this component constraint. It has constraints for the horizontal size
	 * and grow/shrink priorities and weights.
	 * @param h The new dimension constraint. If <code>null</code> it will be reset to <code>new DimConstraint();</code>
	 */
	public void setHorizontal(DimConstraint h)
	{
		hor = h != null ? h : new DimConstraint();
	}

	/** Returns the vertical dimension constraint for this component constraint. It has constraints for the vertical size
	 * and grow/shrink priorities and weights.
	 * <p>
	 * Note! If any changes is to be made it must be made direct when the object is returned. It is not allowed to save the
	 * constraint for later use.
	 * @return The current dimension constraint. Never <code>null</code>.
	 */
	public DimConstraint getVertical()
	{
		return ver;
	}

	/** Sets the vertical dimension constraint for this component constraint. It has constraints for the vertical size
	 * and grow/shrink priorities and weights.
	 * @param v The new dimension constraint. If <code>null</code> it will be reset to <code>new DimConstraint();</code>
	 */
	public void setVertical(DimConstraint v)
	{
		ver = v != null ? v : new DimConstraint();
	}

	/** Returns the vertical or horizontal dim constraint.
	 * <p>
	 * Note! If any changes is to be made it must be made direct when the object is returned. It is not allowed to save the
	 * constraint for later use.
	 * @param isHor If the horizontal constraint should be returned.
	 * @return The dim constraint. Never <code>null</code>.
	 */
	public DimConstraint getDimConstraint(boolean isHor)
	{
		return isHor ? hor : ver;
	}

	/** Returns the absolute positioning of one or more of the edges. This will be applied last in the layout cycle and will not
	 * affect the flow or grid positions. The positioning is relative to the parent and can not (as padding) be used
	 * to adjust the edges relative to the old value. May be <code>null</code> and elements may be <code>null</code>.
	 * <code>null</code> value(s) for the x2 and y2 will be interpreted as to keep the preferred size and thus the x1
	 * and x2 will just absolutely positions the component.
	 * <p>
	 * Note that {@link #setBoundsInGrid(boolean)} changes the interpretation of thisproperty slightly.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value as a new array, free to modify.
	 */
	public UnitValue[] getPos()
	{
		return pos != null ? new UnitValue[] {pos[0], pos[1], pos[2], pos[3]} : null;
	}

	/** Sets absolute positioning of one or more of the edges. This will be applied last in the layout cycle and will not
	 * affect the flow or grid positions. The positioning is relative to the parent and can not (as padding) be used
	 * to adjust the edges relative to the old value. May be <code>null</code> and elements may be <code>null</code>.
	 * <code>null</code> value(s) for the x2 and y2 will be interpreted as to keep the preferred size and thus the x1
	 * and x2 will just absolutely positions the component.
	 * <p>
	 * Note that {@link #setBoundsInGrid(boolean)} changes the interpretation of thisproperty slightly.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param pos <code>UnitValue[] {x, y, x2, y2}</code>. Must be <code>null</code> or of length 4. Elements can be <code>null</code>.
	 */
	public void setPos(UnitValue[] pos)
	{
		this.pos = pos != null ? new UnitValue[] {pos[0], pos[1], pos[2], pos[3]} : null;
		linkTargets = null;
	}

	/** Returns if the absolute <code>pos</code> value should be corrections to the component that is in a normal cell. If <code>false</code>
	 * the value of <code>pos</code> is truly absolute in that it will not affect the grid or have a default bounds in the grid.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 * @see #getPos()
	 */
	public boolean isBoundsInGrid()
	{
		return boundsInGrid;
	}

	/** Sets if the absolute <code>pos</code> value should be corrections to the component that is in a normal cell. If <code>false</code>
	 * the value of <code>pos</code> is truly absolute in that it will not affect the grid or have a default bounds in the grid.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param b <code>true</code> for bounds taken from the grid position. <code>false</code> is default.
	 * @see #setPos(UnitValue[])
	 */
	void setBoundsInGrid(boolean b)
	{
		this.boundsInGrid = b;
	}

	/** Returns the absolute cell position in the grid or <code>-1</code> if cell positioning is not used.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public int getCellX()
	{
		return cellX;
	}

	/** Set an absolute cell x-position in the grid. If &gt;= 0 this point points to the absolute cell that this constaint's component should occupy.
	 * If there's already a component in that cell they will split the cell. The flow will then continue after this cell.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param x The x-position or <code>-1</code> to disable cell positioning.
	 */
	public void setCellX(int x)
	{
		cellX = x;
	}

	/** Returns the absolute cell position in the grid or <code>-1</code> if cell positioning is not used.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public int getCellY()
	{
		return cellX < 0 ? -1 : cellY;
	}

	/** Set an absolute cell x-position in the grid. If &gt;= 0 this point points to the absolute cell that this constaint's component should occupy.
	 * If there's already a component in that cell they will split the cell. The flow will then continue after this cell.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param y The y-position or <code>-1</code> to disable cell positioning.
	 */
	public void setCellY(int y)
	{
		if (y < 0)
			cellX = -1;
		cellY = y < 0 ? 0 : y;
	}

	/** Sets the docking side. -1 means no docking.<br>
	 * Valid sides are: <code> north = 0, west = 1, south = 2, east = 3</code>.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current side.
	 */
	public int getDockSide()
	{
		return dock;
	}

	/** Sets the docking side. -1 means no docking.<br>
	 * Valid sides are: <code> north = 0, west = 1, south = 2, east = 3</code>.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param side -1 or 0-3.
	 */
	public void setDockSide(int side)
	{
		if (side < -1 || side > 3)
			throw new IllegalArgumentException("Illegal dock side: " + side);
		dock = side;
	}

	/** Returns if this component should have its bounds handled by an external source and not this layout manager.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public boolean isExternal()
	{
		return external;
	}

	/** If this boolean is true this component is not handled in any way by the layout manager and the component can have its bounds set by an external
	 * handler which is normally by the use of some <code>component.setBounds(x, y, width, height)</code> directly (for Swing).
	 * <p>
	 * The bounds <b>will not</b> affect the minimum and preferred size of the container.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param b <code>true</code> means that the bounds are not changed.
	 */
	public void setExternal(boolean b)
	{
		this.external = b;
	}

	/** Returns if the flow in the <b>cell</b> is in the horizontal dimension. Vertical if <code>false</code>. Only the first
	 * component is a cell can set the flow.
	 * <p>
	 * If <code>null</code> the flow direction is inherited by from the {@link net.miginfocom.layout.LC}.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public Boolean getFlowX()
	{
		return flowX;
	}

	/** Sets if the flow in the <b>cell</b> is in the horizontal dimension. Vertical if <code>false</code>. Only the first
	 * component is a cell can set the flow.
	 * <p>
	 * If <code>null</code> the flow direction is inherited by from the {@link net.miginfocom.layout.LC}.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param b <code>Boolean.TRUE</code> means horizontal flow in the cell.
	 */
	public void setFlowX(Boolean b)
	{
		this.flowX = b;
	}

	/** Sets how a component that is hidden (not visible) should be treated by default.
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The mode:<br>
	 * 0 == Normal. Bounds will be calculated as if the component was visible.<br>
	 * 1 == If hidden the size will be 0, 0 but the gaps remain.<br>
	 * 2 == If hidden the size will be 0, 0 and gaps set to zero.<br>
	 * 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
	 */
	public int getHideMode()
	{
		return hideMode;
	}

	/** Sets how a component that is hidden (not visible) should be treated by default.
	 * @param mode The mode:<br>
	 * 0 == Normal. Bounds will be calculated as if the component was visible.<br>
	 * 1 == If hidden the size will be 0, 0 but the gaps remain.<br>
	 * 2 == If hidden the size will be 0, 0 and gaps set to zero.<br>
	 * 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
	 */
	public void setHideMode(int mode)
	{
		if (mode < -1 || mode > 3)
			throw new IllegalArgumentException("Wrong hideMode: " + mode);

		hideMode = mode;
	}

	/** Returns the id used to reference this component in some constraints.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The id or <code>null</code>. May consist of a groupID and a componentID which are separated by a dot: ".". E.g. "grp1.id1".
	 * The dot should never be first or last if present.
	 */
	public String getId()
	{
		return id;
	}

	/** Sets the id used to reference this component in some constraints.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param id The id or <code>null</code>. May consist of a groupID and a componentID which are separated by a dot: ".". E.g. "grp1.id1".
	 * The dot should never be first or last if present.
	 */
	public void setId(String id)
	{
		this.id = id;
	}

	/** Returns the absolute resizing in the last stage of the layout cycle. May be <code>null</code> and elements may be <code>null</code>.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value. <code>null</code> or of length 4.
	 */
	public UnitValue[] getPadding()
	{
		return padding != null ? new UnitValue[] {padding[0], padding[1], padding[2], padding[3]} : null;
	}

	/** Sets the absolute resizing in the last stage of the layout cycle. These values are added to the edges and can thus for
	 * instance be used to grow or reduce the size or move the component an absolute number of pixels. May be <code>null</code>
	 * and elements may be <code>null</code>.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param sides top, left, bottom right. Must be <code>null</code> or of length 4.
	 */
	public void setPadding(UnitValue[] sides)
	{
		this.padding = sides != null ? new UnitValue[] {sides[0], sides[1], sides[2], sides[3]} : null;
	}

	/** Returns the visual padding used when laying out this Component. May be <code>null</code> and elements may be <code>null</code>.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value. <code>null</code> or of length 4.
	 */
	public UnitValue[] getVisualPadding()
	{
		return visualPadding != null ? new UnitValue[] {visualPadding[0], visualPadding[1], visualPadding[2], visualPadding[3]} : null;
	}

	/** Sets the visual padding used when laying out this Component.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param sides top, left, bottom right. Must be <code>null</code> or of length 4.
	 */
	public void setVisualPadding(UnitValue[] sides)
	{
		this.visualPadding = sides != null ? new UnitValue[] {sides[0], sides[1], sides[2], sides[3]} : null;
	}

	/** Returns how many cells in the grid that should be skipped <b>before</b> the component that this constraint belongs to.
	 * <p>
	 * Note that only the first component will be checked for this property.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value. 0 if no skip.
	 */
	public int getSkip()
	{
		return skip;
	}

	/** Sets how many cells in the grid that should be skipped <b>before</b> the component that this constraint belongs to.
	 * <p>
	 * Note that only the first component will be checked for this property.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param cells How many cells in the grid that should be skipped <b>before</b> the component that this constraint belongs to
	 */
	public void setSkip(int cells)
	{
		this.skip = cells;
	}

	/** Returns the number of cells the cell that this constraint's component will span in the indicated dimension. <code>1</code> is default and
	 * means that it only spans the current cell. <code>LayoutUtil.INF</code> is used to indicate a span to the end of the column/row.
	 * <p>
	 * Note that only the first component will be checked for this property.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public int getSpanX()
	{
		return spanX;
	}

	/** Sets the number of cells the cell that this constraint's component will span in the indicated dimension. <code>1</code> is default and
	 * means that it only spans the current cell. <code>LayoutUtil.INF</code> is used to indicate a span to the end of the column/row.
	 * <p>
	 * Note that only the first component will be checked for this property.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param cells The number of cells to span (i.e. merge).
	 */
	public void setSpanX(int cells)
	{
		this.spanX = cells;
	}

	/** Returns the number of cells the cell that this constraint's component will span in the indicated dimension. <code>1</code> is default and
	 * means that it only spans the current cell. <code>LayoutUtil.INF</code> is used to indicate a span to the end of the column/row.
	 * <p>
	 * Note that only the first component will be checked for this property.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public int getSpanY()
	{
		return spanY;
	}

	/** Sets the number of cells the cell that this constraint's component will span in the indicated dimension. <code>1</code> is default and
	 * means that it only spans the current cell. <code>LayoutUtil.INF</code> is used to indicate a span to the end of the column/row.
	 * <p>
	 * Note that only the first component will be checked for this property.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param cells The number of cells to span (i.e. merge).
	 */
	public void setSpanY(int cells)
	{
		this.spanY = cells;
	}

	/** "pushx" indicates that the column that this component is in (this first if the component spans) should default to growing.
	 * If any other column has been set to grow this push value on the component does nothing as the column's explicit grow weight
	 * will take precedence. Push is normally used when the grid has not been defined in the layout.
	 * <p>
	 * If multiple components in a column has push weights set the largest one will be used for the column.
	 * @return The current push value. Default is <code>null</code>.
	 */
	public Float getPushX()
	{
		return pushX;
	}

	/** "pushx" indicates that the column that this component is in (this first if the component spans) should default to growing.
	 * If any other column has been set to grow this push value on the component does nothing as the column's explicit grow weight
	 * will take precedence. Push is normally used when the grid has not been defined in the layout.
	 * <p>
	 * If multiple components in a column has push weights set the largest one will be used for the column.
	 * @param weight The new push value. Default is <code>null</code>.
	 */
	public void setPushX(Float weight)
	{
		this.pushX = weight;
	}

	/** "pushx" indicates that the row that this component is in (this first if the component spans) should default to growing.
	 * If any other row has been set to grow this push value on the component does nothing as the row's explicit grow weight
	 * will take precedence. Push is normally used when the grid has not been defined in the layout.
	 * <p>
	 * If multiple components in a row has push weights set the largest one will be used for the row.
	 * @return The current push value. Default is <code>null</code>.
	 */
	public Float getPushY()
	{
		return pushY;
	}

	/** "pushx" indicates that the row that this component is in (this first if the component spans) should default to growing.
	 * If any other row has been set to grow this push value on the component does nothing as the row's explicit grow weight
	 * will take precedence. Push is normally used when the grid has not been defined in the layout.
	 * <p>
	 * If multiple components in a row has push weights set the largest one will be used for the row.
	 * @param weight The new push value. Default is <code>null</code>.
	 */
	public void setPushY(Float weight)
	{
		this.pushY = weight;
	}

	/** Returns in how many parts the current cell (that this constraint's component will be in) should be split in. If for instance
	 * it is split in two, the next component will also share the same cell. Note that the cell can also span a number of
	 * cells, which means that you can for instance span three cells and split that big cell for two components. Split can be
	 * set to a very high value to make all components in the same row/column share the same cell (e.g. <code>LayoutUtil.INF</code>).
	 * <p>
	 * Note that only the first component will be checked for this property.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public int getSplit()
	{
		return split;
	}

	/** Sets in how many parts the current cell (that this constraint's component will be in) should be split in. If for instance
	 * it is split in two, the next component will also share the same cell. Note that the cell can also span a number of
	 * cells, which means that you can for instance span three cells and split that big cell for two components. Split can be
	 * set to a very high value to make all components in the same row/column share the same cell (e.g. <code>LayoutUtil.INF</code>).
	 * <p>
	 * Note that only the first component will be checked for this property.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param parts The number of parts (i.e. component slots) the cell should be divided into.
	 */
	public void setSplit(int parts)
	{
		this.split = parts;
	}

	/** Tags the component with metadata. Currently only used to tag buttons with for instance "cancel" or "ok" to make them
	 * show up in the correct order depending on platform. See {@link PlatformDefaults#setButtonOrder(String)} for information.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value. May be <code>null</code>.
	 */
	public String getTag()
	{
		return tag;
	}

	/** Optinal tag that gives more context to this constraint's component. It is for instance used to tag buttons in a
	 * button bar with the button type such as "ok", "help" or "cancel".
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param tag The new tag. May be <code>null</code>.
	 */
	public void setTag(String tag)
	{
		this.tag = tag;
	}

	/** Returns if the flow should wrap to the next line/column <b>after</b> the component that this constraint belongs to.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public boolean isWrap()
	{
		return wrap != null;
	}

	/** Sets if the flow should wrap to the next line/column <b>after</b> the component that this constraint belongs to.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param b <code>true</code> means wrap after.
	 */
	public void setWrap(boolean b)
	{
		wrap = b ? (wrap == null ? DEF_GAP : wrap) : null;
	}

	/** Returns the wrap size if it is a custom size. If wrap was set to true with {@link #setWrap(boolean)} then this method will
	 * return <code>null</code> since that means that the gap size should be the default one as defined in the rows spec.
	 * @return The custom gap size. NOTE! Will return <code>null</code> for both no wrap <b>and</b> default wrap.
	 * @see #isWrap()
	 * @see #setWrap(boolean)
	 * @since 2.4.2
	 */
	public BoundSize getWrapGapSize()
	{
		return wrap == DEF_GAP ? null : wrap;
	}

	/** Set the wrap size and turns wrap on if <code>!= null</code>.
	 * @param s The custom gap size. NOTE! <code>null</code> will not turn on or off wrap, it will only set the wrap gap size to "default".
	 * A non-null value will turn on wrap though.
	 * @see #isWrap()
	 * @see #setWrap(boolean)
	 * @since 2.4.2
	 */
	public void setWrapGapSize(BoundSize s)
	{
		wrap = s == null ? (wrap != null ? DEF_GAP : null) : s;
	}

	/** Returns if the flow should wrap to the next line/column <b>before</b> the component that this constraint belongs to.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current value.
	 */
	public boolean isNewline()
	{
		return newline != null;
	}

	/** Sets if the flow should wrap to the next line/column <b>before</b> the component that this constraint belongs to.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param b <code>true</code> means wrap before.
	 */
	public void setNewline(boolean b)
	{
		newline = b ? (newline == null ? DEF_GAP : newline) : null;
	}

	/** Returns the newline size if it is a custom size. If newline was set to true with {@link #setNewline(boolean)} then this method will
	 * return <code>null</code> since that means that the gap size should be the default one as defined in the rows spec.
	 * @return The custom gap size. NOTE! Will return <code>null</code> for both no newline <b>and</b> default newline.
	 * @see #isNewline()
	 * @see #setNewline(boolean)
	 * @since 2.4.2
	 */
	public BoundSize getNewlineGapSize()
	{
		return newline == DEF_GAP ? null : newline;
	}

	/** Set the newline size and turns newline on if <code>!= null</code>.
	 * @param s The custom gap size. NOTE! <code>null</code> will not turn on or off newline, it will only set the newline gap size to "default".
	 * A non-null value will turn on newline though.
	 * @see #isNewline()
	 * @see #setNewline(boolean)
	 * @since 2.4.2
	 */
	public void setNewlineGapSize(BoundSize s)
	{
		newline = s == null ? (newline != null ? DEF_GAP : null) : s;
	}
}