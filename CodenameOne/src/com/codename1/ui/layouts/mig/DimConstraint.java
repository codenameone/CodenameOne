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

/** A simple value holder for a constraint for one dimension.
 */
public final class DimConstraint
{
	/** How this entity can be resized in the dimension that this constraint represents.
	 */
	final ResizeConstraint resize = new ResizeConstraint();

	// Look at the properties' getter/setter methods for explanation

	private String sizeGroup = null;            // A "context" compared with equals.

	private BoundSize size = BoundSize.NULL_SIZE;     // Min, pref, max. Never null, but sizes can be null.

	private BoundSize gapBefore = null, gapAfter = null;

	private UnitValue align = null;


	// **************  Only applicable on components! *******************

	private String endGroup = null;            // A "context" compared with equals.


	// **************  Only applicable on rows/columns! *******************

	private boolean fill = false;

	private boolean noGrid = false;

	/** Empty constructor.
	 */
	public DimConstraint()
	{
	}

	/** Returns the grow priority. Relative priority is used for determining which entities gets the extra space first.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The grow priority.
	 */
	public int getGrowPriority()
	{
		return resize.growPrio;
	}

	/** Sets the grow priority. Relative priority is used for determining which entities gets the extra space first.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param p The new grow priority.
	 */
	public void setGrowPriority(int p)
	{
		resize.growPrio = p;
	}

	/** Returns the grow weight.<p>
	 * Grow weight is how flexible the entity should be, relative to other entities, when it comes to growing. <code>null</code> or
	 * zero mean it will never grow. An entity that has twice the grow weight compared to another entity will get twice
	 * as much of available space.
	 * <p>
	 * GrowWeight are only compared within the same GrowPrio.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current grow weight.
	 */
	public Float getGrow()
	{
		return resize.grow;
	}

	/** Sets the grow weight.<p>
	 * Grow weight is how flexible the entity should be, relative to other entities, when it comes to growing. <code>null</code> or
	 * zero mean it will never grow. An entity that has twice the grow weight compared to another entity will get twice
	 * as much of available space.
	 * <p>
	 * GrowWeight are only compared within the same GrowPrio.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param weight The new grow weight.
	 */
	public void setGrow(Float weight)
	{
		resize.grow = weight;
	}

	/** Returns the shrink priority. Relative priority is used for determining which entities gets smaller first when space is scarce.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The shrink priority.
	 */
	public int getShrinkPriority()
	{
		return resize.shrinkPrio;
	}

	/** Sets the shrink priority. Relative priority is used for determining which entities gets smaller first when space is scarce.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param p The new shrink priority.
	 */
	public void setShrinkPriority(int p)
	{
		resize.shrinkPrio = p;
	}

	/** Returns the shrink priority. Relative priority is used for determining which entities gets smaller first when space is scarce.
	 * Shrink weight is how flexible the entity should be, relative to other entities, when it comes to shrinking. <code>null</code> or
	 * zero mean it will never shrink (default). An entity that has twice the shrink weight compared to another entity will get twice
	 * as much of available space.
	 * <p>
	 * Shrink(Weight) are only compared within the same ShrinkPrio.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current shrink weight.
	 */
	public Float getShrink()
	{
		return resize.shrink;
	}

	/** Sets the shrink priority. Relative priority is used for determining which entities gets smaller first when space is scarce.
	 * Shrink weight is how flexible the entity should be, relative to other entities, when it comes to shrinking. <code>null</code> or
	 * zero mean it will never shrink (default). An entity that has twice the shrink weight compared to another entity will get twice
	 * as much of available space.
	 * <p>
	 * Shrink(Weight) are only compared within the same ShrinkPrio.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param weight The new shrink weight.
	 */
	public void setShrink(Float weight)
	{
		resize.shrink = weight;
	}

	public UnitValue getAlignOrDefault(boolean isCols)
	{
		if (align != null)
			return align;

		if (isCols)
			return UnitValue.LEADING;

		return fill || PlatformDefaults.getDefaultRowAlignmentBaseline() == false ? UnitValue.CENTER : UnitValue.BASELINE_IDENTITY;
	}

	/** Returns the alignment used either as a default value for sub-entities or for this entity.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The alignment.
	 */
	public UnitValue getAlign()
	{
		return align;
	}

	/** Sets the alignment used wither as a default value for sub-entities or for this entity.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param uv The new shrink priority. E.g. {@link UnitValue#CENTER} or {@link net.miginfocom.layout.UnitValue#LEADING}.
	 */
	public void setAlign(UnitValue uv)
	{
		this.align = uv;
	}

	/** Returns the gap after this entity. The gap is an empty space and can have a min/preferred/maximum size so that it can shrink and
	 * grow depending on available space. Gaps are against other entities' edges and not against other entities' gaps.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The gap after this entity
	 */
	public BoundSize getGapAfter()
	{
		return gapAfter;
	}

	/** Sets the gap after this entity. The gap is an empty space and can have a min/preferred/maximum size so that it can shrink and
	 * grow depending on available space. Gaps are against other entities' edges and not against other entities' gaps.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The new gap.
	 * @see net.miginfocom.layout.ConstraintParser#parseBoundSize(String, boolean, boolean) 
	 */
	public void setGapAfter(BoundSize size)
	{
		this.gapAfter = size;
	}

	boolean hasGapAfter()
	{
		return gapAfter != null && gapAfter.isUnset() == false;
	}

	boolean isGapAfterPush()
	{
		return gapAfter != null && gapAfter.getGapPush();
	}

	/** Returns the gap before this entity. The gap is an empty space and can have a min/preferred/maximum size so that it can shrink and
	 * grow depending on available space. Gaps are against other entities' edges and not against other entities' gaps.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The gap before this entity
	 */
	public BoundSize getGapBefore()
	{
		return gapBefore;
	}

	/** Sets the gap before this entity. The gap is an empty space and can have a min/preferred/maximum size so that it can shrink and
	 * grow depending on available space. Gaps are against other entities' edges and not against other entities' gaps.
	 * <p>
	 * See also {@link net.miginfocom.layout.ConstraintParser#parseBoundSize(String, boolean, boolean)}.
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The new gap.
	 */
	public void setGapBefore(BoundSize size)
	{
		this.gapBefore = size;
	}

	boolean hasGapBefore()
	{
		return gapBefore != null && gapBefore.isUnset() == false;
	}

	boolean isGapBeforePush()
	{
		return gapBefore != null && gapBefore.getGapPush();
	}

	/** Returns the min/preferred/max size for the entity in the dimension that this object describes.
	 * <p>
     * See also {@link net.miginfocom.layout.ConstraintParser#parseBoundSize(String, boolean, boolean)}.
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current size. Never <code>null</code> since v3.5.
	 */
	public BoundSize getSize()
	{
		return size;
	}

	/** Sets the min/preferred/max size for the entity in the dimension that this object describes.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param size The new size. May be <code>null</code>.
	 */
	public void setSize(BoundSize size)
	{
		if (size != null)
			size.checkNotLinked();
		this.size = size;
	}

	/** Returns the size group that this entity should be in for the dimension that this object is describing.
	 * If this constraint is in a size group that is specified here. <code>null</code> means no size group
	 * and all other values are legal. Comparison with .equals(). Components/columnss/rows in the same size group
	 * will have the same min/preferred/max size; that of the largest in the group for the first two and the
	 * smallest for max.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current size group. May be <code>null</code>.
	 */
	public String getSizeGroup()
	{
		return sizeGroup;
	}

	/** Sets the size group that this entity should be in for the dimension that this object is describing.
	 * If this constraint is in a size group that is specified here. <code>null</code> means no size group
	 * and all other values are legal. Comparison with .equals(). Components/columnss/rows in the same size group
	 * will have the same min/preferred/max size; that of the largest in the group for the first two and the
	 * smallest for max.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param s The new size group. <code>null</code> disables size grouping.
	 */
	public void setSizeGroup(String s)
	{
		sizeGroup = s;
	}

	// **************  Only applicable on components ! *******************

	/** Returns the end group that this entity should be in for the demension that this object is describing.
	 * If this constraint is in an end group that is specified here. <code>null</code> means no end group
	 * and all other values are legal. Comparison with .equals(). Components in the same end group
	 * will have the same end coordinate.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return The current end group. <code>null</code> may be returned.
	 */
	public String getEndGroup()
	{
		return endGroup;
	}

	/** Sets the end group that this entity should be in for the demension that this object is describing.
	 * If this constraint is in an end group that is specified here. <code>null</code> means no end group
	 * and all other values are legal. Comparison with .equals(). Components in the same end group
	 * will have the same end coordinate.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param s The new end group. <code>null</code> disables end grouping.
	 */
	public void setEndGroup(String s)
	{
		endGroup = s;
	}

	// **************  Not applicable on components below ! *******************

	/** Returns if the component in the row/column that this constraint should default be grown in the same dimension that
	 * this constraint represents (width for column and height for a row).
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>true</code> means that components should grow.
	 */
	public boolean isFill()
	{
		return fill;
	}

	/** Sets if the component in the row/column that this constraint should default be grown in the same dimension that
	 * this constraint represents (width for column and height for a row).
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param b <code>true</code> means that components should grow.
	 */
	public void setFill(boolean b)
	{
		fill = b;
	}

	/** Returns if the row/column should default to flow and not to grid behaviour. This means that the whole row/column
	 * will be one cell and all components will end up in that cell.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @return <code>true</code> means that the whole row/column should be one cell.
	 */
	public boolean isNoGrid()
	{
		return noGrid;
	}

	/** Sets if the row/column should default to flow and not to grid behaviour. This means that the whole row/column
	 * will be one cell and all components will end up in that cell.
	 * <p>
	 * For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
	 * @param b <code>true</code> means that the whole row/column should be one cell.
	 */
	public void setNoGrid(boolean b)
	{
		this.noGrid = b;
	}

	/** Returns the gaps as pixel values.
	 * @param parent The parent. Used to get the pixel values.
	 * @param defGap The default gap to use if there is no gap set on this object (i.e. it is null).
	 * @param refSize The reference size used to get the pixel sizes.
	 * @param before IF it is the gap before rather than the gap after to return.
	 * @return The [min,preferred,max] sizes for the specified gap. Uses {@link net.miginfocom.layout.LayoutUtil#NOT_SET}
	 * for gap sizes that are <code>null</code>. Returns <code>null</code> if there was no gap specified. A new and free to use array.
	 */
	int[] getRowGaps(ContainerWrapper parent, BoundSize defGap, int refSize, boolean before)
	{
		BoundSize gap = before ? gapBefore : gapAfter;
		if (gap == null || gap.isUnset())
			gap = defGap;

		if (gap == null || gap.isUnset())
			return null;

		int[] ret = new int[3];
		for (int i = LayoutUtil.MIN; i <= LayoutUtil.MAX; i++) {
			UnitValue uv = gap.getSize(i);
			ret[i] = uv != null ? uv.getPixels(refSize, parent, null) : LayoutUtil.NOT_SET;
		}
		return ret;
	}

	/** Returns the gaps as pixel values.
	 * @param parent The parent. Used to get the pixel values.
	 * @param comp The component that the gap is for. If not for a component it is <code>null</code>.
	 * @param adjGap The gap that the adjacent component, if any, has towards <code>comp</code>.
	 * @param adjacentComp The adjacent component if any. May be <code>null</code>.
	 * @param refSize The reference size used to get the pixel sizes.
	 * @param adjacentSide What side the <code>adjacentComp</code> is on. 0 = top, 1 = left, 2 = bottom, 3 = right.
	 * @param tag The tag string that the component might be tagged with in the component constraints. May be <code>null</code>.
	 * @param isLTR If it is left-to-right.
	 * @return The [min,preferred,max] sizes for the specified gap. Uses {@link net.miginfocom.layout.LayoutUtil#NOT_SET}
	 * for gap sizes that are <code>null</code>. Returns <code>null</code> if there was no gap specified. A new and free to use array.
	 */
	int[] getComponentGaps(ContainerWrapper parent, ComponentWrapper comp, BoundSize adjGap, ComponentWrapper adjacentComp, String tag, int refSize, int adjacentSide, boolean isLTR)
	{
		BoundSize gap = adjacentSide < 2 ? gapBefore : gapAfter;

		boolean hasGap = gap != null && gap.getGapPush();
		if ((gap == null || gap.isUnset()) && (adjGap == null || adjGap.isUnset()) && comp != null)
			gap = PlatformDefaults.getDefaultComponentGap(comp, adjacentComp, adjacentSide + 1, tag, isLTR);

		if (gap == null)
			return hasGap ? new int[] {0, 0, LayoutUtil.NOT_SET} : null;

		int[] ret = new int[3];
		for (int i = LayoutUtil.MIN; i <= LayoutUtil.MAX; i++) {
			UnitValue uv = gap.getSize(i);
			ret[i] = uv != null ? uv.getPixels(refSize, parent, null) : LayoutUtil.NOT_SET;
		}
		return ret;
	}
}
