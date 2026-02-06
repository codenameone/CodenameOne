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

/// Contains the constraints for an instance of the `LC` layout manager.
public final class LC {
    // See the corresponding set/get method for documentation of the property!

    private int wrapAfter = LayoutUtil.INF;

    private Boolean leftToRight = null;

    private UnitValue[] insets = null;    // Never null elememts but if unset array is null

    private UnitValue alignX = null;
    private UnitValue alignY = null;

    private BoundSize gridGapX = null;
    private BoundSize gridGapY = null;

    private BoundSize width = BoundSize.NULL_SIZE;
    private BoundSize height = BoundSize.NULL_SIZE;

    private BoundSize packW = BoundSize.NULL_SIZE;
    private BoundSize packH = BoundSize.NULL_SIZE;

    private float pwAlign = 0.5f;
    private float phAlign = 1.0f;

    private int debugMillis = 0;

    private int hideMode = 0;

    private boolean noCache = false;

    private boolean flowX = true;

    private boolean fillX = false;
    private boolean fillY = false;

    private boolean topToBottom = true;

    private boolean noGrid = false;

    private boolean visualPadding = true;

    // ************************************************************************
    // * JavaBean get/set methods.
    // ************************************************************************


    /// If components have sizes or positions linked to the bounds of the parent in some way (as for instance the `"%"` unit has) the cache
    /// must be turned off for the panel. If components does not get the correct or expected size or position try to set this property to `true`.
    ///
    /// #### Returns
    ///
    /// `true` means no cache and slightly slower layout.
    public boolean isNoCache() {
        return noCache;
    }

    /// If components have sizes or positions linked to the bounds of the parent in some way (as for instance the `"%"` unit has) the cache
    /// must be turned off for the panel. If components does not get the correct or expected size or position try to set this property to `true`.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` means no cache and slightly slower layout.
    public void setNoCache(boolean b) {
        this.noCache = b;
    }

    /// If the laid out components' bounds in total is less than the final size of the container these align values will be used to align the components
    /// in the parent. `null` is default and that means top/left alignment. The relative distances between the components will not be affected
    /// by this property.
    ///
    /// #### Returns
    ///
    /// The current alignment.
    public UnitValue getAlignX() {
        return alignX;
    }

    /// If the laid out components' bounds in total is less than the final size of the container these align values will be used to align the components
    /// in the parent. `null` is default and that means top/left alignment. The relative distances between the components will not be affected
    /// by this property.
    ///
    /// #### Parameters
    ///
    /// - `uv`: The new alignment. Use `boolean)` to create the `UnitValue`. May be `null`.
    public void setAlignX(UnitValue uv) {
        this.alignX = uv;
    }

    /// If the laid out components' bounds in total is less than the final size of the container these align values will be used to align the components
    /// in the parent. `null` is default and that means top/left alignment. The relative distances between the components will not be affected
    /// by this property.
    ///
    /// #### Returns
    ///
    /// The current alignment.
    public UnitValue getAlignY() {
        return alignY;
    }

    /// If the laid out components' bounds in total is less than the final size of the container these align values will be used to align the components
    /// in the parent. `null` is default and that means top/left alignment. The relative distances between the components will not be affected
    /// by this property.
    ///
    /// #### Parameters
    ///
    /// - `uv`: The new alignment. Use `boolean)` to create the `UnitValue`. May be `null`.
    public void setAlignY(UnitValue uv) {
        this.alignY = uv;
    }

    /// If `> 0` the debug decorations will be repainted every `millis`. No debug information if `<= 0` (default).
    ///
    /// #### Returns
    ///
    /// The current debug repaint interval.
    public int getDebugMillis() {
        return debugMillis;
    }

    /// If `> 0` the debug decorations will be repainted every `millis`. No debug information if `<= 0` (default).
    ///
    /// #### Parameters
    ///
    /// - `millis`: The new debug repaint interval.
    public void setDebugMillis(int millis) {
        this.debugMillis = millis;
    }

    /// If the layout should always claim the whole bounds of the laid out container even if the preferred size is smaller.
    ///
    /// #### Returns
    ///
    /// `true` means fill. `false` is default.
    public boolean isFillX() {
        return fillX;
    }

    /// If the layout should always claim the whole bounds of the laid out container even if the preferred size is smaller.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` means fill. `false` is default.
    public void setFillX(boolean b) {
        this.fillX = b;
    }

    /// If the layout should always claim the whole bounds of the laid out container even if the preferred size is smaller.
    ///
    /// #### Returns
    ///
    /// `true` means fill. `false` is default.
    public boolean isFillY() {
        return fillY;
    }

    /// If the layout should always claim the whole bounds of the laid out container even if the preferred size is smaller.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` means fill. `false` is default.
    public void setFillY(boolean b) {
        this.fillY = b;
    }

    /// The default flow direction. Normally (which is `true`) this is horizontal and that means that the "next" component
    /// will be put in the cell to the right (or to the left if left-to-right is false).
    ///
    /// #### Returns
    ///
    /// `true` is the default flow horizontally.
    ///
    /// #### See also
    ///
    /// - #setLeftToRight(Boolean)
    public boolean isFlowX() {
        return flowX;
    }

    /// The default flow direction. Normally (which is `true`) this is horizontal and that means that the "next" component
    /// will be put in the cell to the right (or to the left if left-to-right is false).
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` is the default flow horizontally.
    ///
    /// #### See also
    ///
    /// - #setLeftToRight(Boolean)
    public void setFlowX(boolean b) {
        this.flowX = b;
    }

    /// If non-`null` (`null` is default) these value will be used as the default gaps between the columns in the grid.
    ///
    /// #### Returns
    ///
    /// The default grid gap between columns in the grid. `null` if the platform default is used.
    public BoundSize getGridGapX() {
        return gridGapX;
    }

    /// If non-`null` (`null` is default) these value will be used as the default gaps between the columns in the grid.
    ///
    /// #### Parameters
    ///
    /// - `x`: The default grid gap between columns in the grid. If `null` the platform default is used.
    public void setGridGapX(BoundSize x) {
        this.gridGapX = x;
    }

    /// If non-`null` (`null` is default) these value will be used as the default gaps between the rows in the grid.
    ///
    /// #### Returns
    ///
    /// The default grid gap between rows in the grid. `null` if the platform default is used.
    public BoundSize getGridGapY() {
        return gridGapY;
    }

    /// If non-`null` (`null` is default) these value will be used as the default gaps between the rows in the grid.
    ///
    /// #### Parameters
    ///
    /// - `y`: The default grid gap between rows in the grid. If `null` the platform default is used.
    public void setGridGapY(BoundSize y) {
        this.gridGapY = y;
    }

    /// How a component that is hidden (not visible) should be treated by default.
    ///
    /// #### Returns
    ///
    /// @return The mode:
    ///
    /// 0 == Normal. Bounds will be calculated as if the component was visible.
    ///
    /// 1 == If hidden the size will be 0, 0 but the gaps remain.
    ///
    /// 2 == If hidden the size will be 0, 0 and gaps set to zero.
    ///
    /// 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
    public int getHideMode() {
        return hideMode;
    }

    /// How a component that is hidden (not visible) should be treated.
    ///
    /// #### Parameters
    ///
    /// - `mode`: @param mode The mode:
    ///
    /// 0 == Normal. Bounds will be calculated as if the component was visible.
    ///
    /// 1 == If hidden the size will be 0, 0 but the gaps remain.
    ///
    /// 2 == If hidden the size will be 0, 0 and gaps set to zero.
    ///
    /// 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
    public void setHideMode(int mode) {
        if (mode < 0 || mode > 3) {
            throw new IllegalArgumentException("Wrong hideMode: " + mode);
        }

        this.hideMode = mode;
    }

    /// The insets for the layed out panel. The insets will be an empty space around the components in the panel. `null` values
    /// means that the default panel insets for the platform is used. See `net.miginfocom.layout.UnitValue, net.miginfocom.layout.UnitValue, net.miginfocom.layout.UnitValue)`.
    ///
    /// #### Returns
    ///
    /// The insets. Of length 4 (top, left, bottom, right) or `null`. The elements (1 to 4) may be `null`. The array is a copy and can be used freely.
    ///
    /// #### See also
    ///
    /// - net.miginfocom.layout.ConstraintParser#parseInsets(String, boolean)
    public UnitValue[] getInsets() {
        return insets != null ? new UnitValue[]{insets[0], insets[1], insets[2], insets[3]} : null;
    }

    /// The insets for the layed out panel. The insets will be an empty space around the components in the panel. `null` values
    /// means that the default panel insets for the platform is used. See `net.miginfocom.layout.UnitValue, net.miginfocom.layout.UnitValue, net.miginfocom.layout.UnitValue)`.
    ///
    /// #### Parameters
    ///
    /// - `ins`: @param ins The new insets. Must be of length 4 (top, left, bottom, right) or `null`. The elements (1 to 4) may be `null` to use
    /// the platform default for that side. The array is copied for storage.
    ///
    /// #### See also
    ///
    /// - net.miginfocom.layout.ConstraintParser#parseInsets(String, boolean)
    public void setInsets(UnitValue[] ins) {
        this.insets = ins != null ? new UnitValue[]{ins[0], ins[1], ins[2], ins[3]} : null;
    }

    /// If the layout should be forced to be left-to-right or right-to-left. A value of `null` is default and
    /// means that this will be picked up from the `java.util.Locale` that the container being layed out is reporting.
    ///
    /// #### Returns
    ///
    /// @return `Boolean.TRUE` if force left-to-right. `Boolean.FALSE` if force tight-to-left. `null`
    /// for the default "let the current Locale decide".
    public Boolean getLeftToRight() {
        return leftToRight;
    }

    /// If the layout should be forced to be left-to-right or right-to-left. A value of `null` is default and
    /// means that this will be picked up from the `java.util.Locale` that the container being layed out is reporting.
    ///
    /// #### Parameters
    ///
    /// - `b`: @param b `Boolean.TRUE` to force left-to-right. `Boolean.FALSE` to force tight-to-left. `null`
    /// for the default "let the current Locale decide".
    public void setLeftToRight(Boolean b) {
        this.leftToRight = b;
    }

    /// If the whole layout should be non grid based. It is the same as setting the "nogrid" property on every row/column in the grid.
    ///
    /// #### Returns
    ///
    /// `true` means not grid based. `false` is default.
    public boolean isNoGrid() {
        return noGrid;
    }

    /// If the whole layout should be non grid based. It is the same as setting the "nogrid" property on every row/column in the grid.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` means no grid. `false` is default.
    public void setNoGrid(boolean b) {
        this.noGrid = b;
    }

    /// If the layout should go from the default top-to-bottom in the grid instead of the optinal bottom-to-top.
    ///
    /// #### Returns
    ///
    /// `true` for the default top-to-bottom.
    public boolean isTopToBottom() {
        return topToBottom;
    }

    /// If the layout should go from the default top-to-bottom in the grid instead of the optinal bottom-to-top.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` for the default top-to-bottom.
    public void setTopToBottom(boolean b) {
        this.topToBottom = b;
    }

    /// If visual padding should be automatically used and compensated for by this layout instance.
    ///
    /// #### Returns
    ///
    /// `true` if visual padding.
    public boolean isVisualPadding() {
        return visualPadding;
    }

    /// If visual padding should be automatically used and compensated for by this layout instance.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` turns on visual padding.
    public void setVisualPadding(boolean b) {
        this.visualPadding = b;
    }

    /// Returns after what cell the grid should always auto wrap.
    ///
    /// #### Returns
    ///
    /// @return After what cell the grid should always auto wrap. If `0` the number of columns/rows in the
    /// `net.miginfocom.layout.AC` is used. `LayoutUtil.INF` is used for no auto wrap.
    public int getWrapAfter() {
        return wrapAfter;
    }

    /// Sets after what cell the grid should always auto wrap.
    ///
    /// #### Parameters
    ///
    /// - `count`: @param count After what cell the grid should always auto wrap. If `0` the number of columns/rows in the
    /// `net.miginfocom.layout.AC` is used. `LayoutUtil.INF` is used for no auto wrap.
    public void setWrapAfter(int count) {
        this.wrapAfter = count;
    }

    /// Returns the "pack width" for the **window** that this container is located in. When the size of this container changes
    /// the size of the window will be corrected to be within this BoundsSize. It can be used to set the minimum and/or maximum size of the window
    /// as well as the size window should optimally get. This optimal size is normally its "preferred" size which is why "preferred"
    /// is the normal value to set here.
    ///
    /// ":push" can be appended to the bound size to only push the size bigger and never shrink it if the preferred size gets smaller.
    ///
    /// E.g. "pref", "100:pref", "pref:700", "300::700", "pref:push"
    ///
    /// #### Returns
    ///
    /// The current value. Never `null`. Check if not set with `.isUnset()`.
    ///
    /// #### Since
    ///
    /// 3.5
    public BoundSize getPackWidth() {
        return packW;
    }

    /// Sets the "pack width" for the **window** that this container is located in. When the size of this container changes
    /// the size of the window will be corrected to be within this BoundsSize. It can be used to set the minimum and/or maximum size of the window
    /// as well as the size window should optimally get. This optimal size is normally its "preferred" size which is why "preferred"
    /// is the normal value to set here.
    ///
    /// ":push" can be appended to the bound size to only push the size bigger and never shrink it if the preferred size gets smaller.
    ///
    /// E.g. "pref", "100:pref", "pref:700", "300::700", "pref:push"
    ///
    /// #### Parameters
    ///
    /// - `size`: The new pack size. If `null` it will be corrected to an "unset" BoundSize.
    ///
    /// #### Since
    ///
    /// 3.5
    public void setPackWidth(BoundSize size) {
        packW = size != null ? size : BoundSize.NULL_SIZE;
    }

    /// Returns the "pack height" for the **window** that this container is located in. When the size of this container changes
    /// the size of the window will be corrected to be within this BoundsSize. It can be used to set the minimum and/or maximum size of the window
    /// as well as the size window should optimally get. This optimal size is normally its "preferred" size which is why "preferred"
    /// is the normal value to set here.
    ///
    /// ":push" can be appended to the bound size to only push the size bigger and never shrink it if the preferred size gets smaller.
    ///
    /// E.g. "pref", "100:pref", "pref:700", "300::700", "pref:push"
    ///
    /// #### Returns
    ///
    /// The current value. Never `null`. Check if not set with `.isUnset()`.
    ///
    /// #### Since
    ///
    /// 3.5
    public BoundSize getPackHeight() {
        return packH;
    }

    /// Sets the "pack height" for the **window** that this container is located in. When the size of this container changes
    /// the size of the window will be corrected to be within this BoundsSize. It can be used to set the minimum and/or maximum size of the window
    /// as well as the size window should optimally get. This optimal size is normally its "preferred" size which is why "preferred"
    /// is the normal value to set here.
    ///
    /// ":push" can be appended to the bound size to only push the size bigger and never shrink it if the preferred size gets smaller.
    ///
    /// E.g. "pref", "100:pref", "pref:700", "300::700", "pref:push"
    ///
    /// #### Parameters
    ///
    /// - `size`: The new pack size. If `null` it will be corrected to an "unset" BoundSize.
    ///
    /// #### Since
    ///
    /// 3.5
    public void setPackHeight(BoundSize size) {
        packH = size != null ? size : BoundSize.NULL_SIZE;
    }


    /// If there is a resize of the window due to packing (see `#setPackHeight(BoundSize)` this value, which is between 0f and 1f,
    /// decides where the extra/superfluous size is placed. 0f means that the window will resize so that the upper part moves up and the
    /// lower side stays in the same place. 0.5f will expand/reduce the window equally upwards and downwards. 1f will do the opposite of 0f
    /// of course.
    ///
    /// #### Returns
    ///
    /// The pack alignment. Always between 0f and 1f, inclusive.
    ///
    /// #### Since
    ///
    /// 3.5
    public float getPackHeightAlign() {
        return phAlign;
    }

    /// If there is a resize of the window due to packing (see `#setPackHeight(BoundSize)` this value, which is between 0f and 1f,
    /// decides where the extra/superfluous size is placed. 0f means that the window will resize so that the upper part moves up and the
    /// lower side stays in the same place. 0.5f will expand/reduce the window equally upwards and downwards. 1f will do the opposite of 0f
    /// of course.
    ///
    /// #### Parameters
    ///
    /// - `align`: The pack alignment. Always between 0f and 1f, inclusive. Values outside this will be truncated.
    ///
    /// #### Since
    ///
    /// 3.5
    public void setPackHeightAlign(float align) {
        phAlign = Math.max(0f, Math.min(1f, align));
    }

    /// If there is a resize of the window due to packing (see `#setPackHeight(BoundSize)` this value, which is between 0f and 1f,
    /// decides where the extra/superfluous size is placed. 0f means that the window will resize so that the left part moves left and the
    /// right side stays in the same place. 0.5f will expand/reduce the window equally to the right and lefts. 1f will do the opposite of 0f
    /// of course.
    ///
    /// #### Returns
    ///
    /// The pack alignment. Always between 0f and 1f, inclusive.
    ///
    /// #### Since
    ///
    /// 3.5
    public float getPackWidthAlign() {
        return pwAlign;
    }

    /// If there is a resize of the window due to packing (see `#setPackHeight(BoundSize)` this value, which is between 0f and 1f,
    /// decides where the extra/superfluous size is placed. 0f means that the window will resize so that the left part moves left and the
    /// right side stays in the same place. 0.5f will expand/reduce the window equally to the right and lefts. 1f will do the opposite of 0f
    /// of course.
    ///
    /// #### Parameters
    ///
    /// - `align`: The pack alignment. Always between 0f and 1f, inclusive. Values outside this will be truncated.
    ///
    /// #### Since
    ///
    /// 3.5
    public void setPackWidthAlign(float align) {
        pwAlign = Math.max(0f, Math.min(1f, align));
    }

    /// Returns the minimum/preferred/maximum size for the container that this layout constraint is set for. Any of these
    /// sizes that is not `null` will be returned directly instead of determining the corresponding size through
    /// asking the components in this container.
    ///
    /// #### Returns
    ///
    /// @return The width for the container that this layout constraint is set for. Not `null` but
    /// all sizes can be `null`.
    ///
    /// #### Since
    ///
    /// 3.5
    public BoundSize getWidth() {
        return width;
    }

    /// Sets the minimum/preferred/maximum size for the container that this layout constraint is set for. Any of these
    /// sizes that is not `null` will be returned directly instead of determining the corresponding size through
    /// asking the components in this container.
    ///
    /// #### Parameters
    ///
    /// - `size`: @param size The width for the container that this layout constraint is set for. `null` is translated to
    /// a bound size containing only null sizes.
    ///
    /// #### Since
    ///
    /// 3.5
    public void setWidth(BoundSize size) {
        this.width = size != null ? size : BoundSize.NULL_SIZE;
    }

    /// Returns the minimum/preferred/maximum size for the container that this layout constraint is set for. Any of these
    /// sizes that is not `null` will be returned directly instead of determining the corresponding size through
    /// asking the components in this container.
    ///
    /// #### Returns
    ///
    /// @return The height for the container that this layout constraint is set for. Not `null` but
    /// all sizes can be `null`.
    ///
    /// #### Since
    ///
    /// 3.5
    public BoundSize getHeight() {
        return height;
    }

    /// Sets the minimum/preferred/maximum size for the container that this layout constraint is set for. Any of these
    /// sizes that is not `null` will be returned directly instead of determining the corresponding size through
    /// asking the components in this container.
    ///
    /// #### Parameters
    ///
    /// - `size`: @param size The height for the container that this layout constraint is set for. `null` is translated to
    /// a bound size containing only null sizes.
    ///
    /// #### Since
    ///
    /// 3.5
    public void setHeight(BoundSize size) {
        this.height = size != null ? size : BoundSize.NULL_SIZE;
    }

    // ************************************************************************
    // * Builder methods.
    // ************************************************************************

    /// Short for, and thus same as, `.pack("pref", "pref")`.
    ///
    /// Same functionality as `#setPackHeight(BoundSize)` and `#setPackWidth(net.miginfocom.layout.BoundSize)`
    /// only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.5
    public LC pack() {
        return pack("pref", "pref");
    }

    /// Sets the pack width and height.
    ///
    /// Same functionality as `#setPackHeight(BoundSize)` and `#setPackWidth(net.miginfocom.layout.BoundSize)`
    /// only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `width`: The pack width. May be `null`.
    ///
    /// - `height`: The pack height. May be `null`.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.5
    public LC pack(String width, String height) {
        setPackWidth(width != null ? ConstraintParser.parseBoundSize(width, false, true) : BoundSize.NULL_SIZE);
        setPackHeight(height != null ? ConstraintParser.parseBoundSize(height, false, false) : BoundSize.NULL_SIZE);
        return this;
    }

    /// Sets the pack width and height alignment.
    ///
    /// Same functionality as `#setPackHeightAlign(float)` and `#setPackWidthAlign(float)`
    /// only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `alignX`: The pack width alignment. 0.5f is default.
    ///
    /// - `alignY`: The pack height alignment. 0.5f is default.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.5
    public LC packAlign(float alignX, float alignY) {
        setPackWidthAlign(alignX);
        setPackHeightAlign(alignY);
        return this;
    }

    /// Sets a wrap after the number of columns/rows that is defined in the `net.miginfocom.layout.AC`.
    ///
    /// Same functionality as calling `#setWrapAfter(int)` with `0` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC wrap() {
        setWrapAfter(0);
        return this;
    }

    /// Same functionality as `#setWrapAfter(int)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `count`: After what cell the grid should always auto wrap. If `0` the number of columns/rows in the
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC wrapAfter(int count) {
        setWrapAfter(count);
        return this;
    }

    /// Same functionality as calling `#setNoCache(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC noCache() {
        setNoCache(true);
        return this;
    }

    /// Same functionality as calling `#setFlowX(boolean)` with `false` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC flowY() {
        setFlowX(false);
        return this;
    }

    /// Same functionality as calling `#setFlowX(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC flowX() {
        setFlowX(true);
        return this;
    }

    /// Same functionality as calling `#setFillX(boolean)` with `true` and `#setFillY(boolean)` with `true` conmbined.T his method returns
    /// `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC fill() {
        setFillX(true);
        setFillY(true);
        return this;
    }

    /// Same functionality as calling `#setFillX(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC fillX() {
        setFillX(true);
        return this;
    }

    /// Same functionality as calling `#setFillY(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC fillY() {
        setFillY(true);
        return this;
    }

    /// Same functionality as `#setLeftToRight(Boolean)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `b`: `true` for forcing left-to-right. `false` for forcing right-to-left.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC leftToRight(boolean b) {
        setLeftToRight(b ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    /// Same functionality as setLeftToRight(false) only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public LC rightToLeft() {
        setLeftToRight(Boolean.FALSE);
        return this;
    }

    /// Same functionality as calling `#setTopToBottom(boolean)` with `false` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC bottomToTop() {
        setTopToBottom(false);
        return this;
    }

    /// Same functionality as calling `#setTopToBottom(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### Since
    ///
    /// 3.7.2
    public LC topToBottom() {
        setTopToBottom(true);
        return this;
    }

    /// Same functionality as calling `#setNoGrid(boolean)` with `true` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC noGrid() {
        setNoGrid(true);
        return this;
    }

    /// Same functionality as calling `#setVisualPadding(boolean)` with `false` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC noVisualPadding() {
        setVisualPadding(false);
        return this;
    }

    /// Sets the same inset (expressed as a `UnitValue`, e.g. "10px" or "20mm") all around.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `allSides`: @param allSides The unit value to set for all sides. May be `null` which means that the default panel insets
    /// for the platform is used.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setInsets(UnitValue[])
    public LC insetsAll(String allSides) {
        UnitValue insH = ConstraintParser.parseUnitValue(allSides, true);
        UnitValue insV = ConstraintParser.parseUnitValue(allSides, false);
        insets = new UnitValue[]{insV, insH, insV, insH}; // No setter to avoid copy again
        return this;
    }

    /// Same functionality as `setInsets(ConstraintParser.parseInsets(s, true))`. This method returns `this`
    /// for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `s`: The string to parse. E.g. "10 10 10 10" or "20". If less than 4 groups the last will be used for the missing.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setInsets(UnitValue[])
    public LC insets(String s) {
        insets = ConstraintParser.parseInsets(s, true);
        return this;
    }

    /// Sets the different insets (expressed as a `UnitValue`s, e.g. "10px" or "20mm") for the corresponding sides.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `top`: @param top    The top inset. E.g. "10px" or "10mm" or "related". May be `null` in which case the default inset for this
    /// side for the platform will be used.
    ///
    /// - `left`: @param left   The left inset. E.g. "10px" or "10mm" or "related". May be `null` in which case the default inset for this
    /// side for the platform will be used.
    ///
    /// - `bottom`: @param bottom The bottom inset. E.g. "10px" or "10mm" or "related". May be `null` in which case the default inset for this
    /// side for the platform will be used.
    ///
    /// - `right`: @param right  The right inset. E.g. "10px" or "10mm" or "related". May be `null` in which case the default inset for this
    /// side for the platform will be used.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setInsets(UnitValue[])
    public LC insets(String top, String left, String bottom, String right) {
        insets = new UnitValue[]{ // No setter to avoid copy again
                ConstraintParser.parseUnitValue(top, false),
                ConstraintParser.parseUnitValue(left, true),
                ConstraintParser.parseUnitValue(bottom, false),
                ConstraintParser.parseUnitValue(right, true)};
        return this;
    }

    /// Same functionality as `setAlignX(ConstraintParser.parseUnitValueOrAlign(unitValue, true))` only this method returns `this`
    /// for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `align`: The align keyword or for instance "100px". E.g "left", "right", "leading" or "trailing".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setAlignX(UnitValue)
    public LC alignX(String align) {
        setAlignX(ConstraintParser.parseUnitValueOrAlign(align, true, null));
        return this;
    }

    /// Same functionality as `setAlignY(ConstraintParser.parseUnitValueOrAlign(align, false))` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `align`: The align keyword or for instance "100px". E.g "top" or "bottom".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setAlignY(UnitValue)
    public LC alignY(String align) {
        setAlignY(ConstraintParser.parseUnitValueOrAlign(align, false, null));
        return this;
    }

    /// Sets both the alignX and alignY as the same time.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `ax`: The align keyword or for instance "100px". E.g "left", "right", "leading" or "trailing".
    ///
    /// - `ay`: The align keyword or for instance "100px". E.g "top" or "bottom".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #alignX(String)
    ///
    /// - #alignY(String)
    public LC align(String ax, String ay) {
        if (ax != null) {
            alignX(ax);
        }

        if (ay != null) {
            alignY(ay);
        }

        return this;
    }

    /// Same functionality as `setGridGapX(ConstraintParser.parseBoundSize(boundsSize, true, true))` only this method
    /// returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `boundsSize`: @param boundsSize The `BoundSize` of the gap. This is a minimum and/or preferred and/or maximum size. E.g.
    /// `"50:100:200"` or `"100px"`.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setGridGapX(BoundSize)
    public LC gridGapX(String boundsSize) {
        setGridGapX(ConstraintParser.parseBoundSize(boundsSize, true, true));
        return this;
    }

    /// Same functionality as `setGridGapY(ConstraintParser.parseBoundSize(boundsSize, true, false))` only this method
    /// returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `boundsSize`: @param boundsSize The `BoundSize` of the gap. This is a minimum and/or preferred and/or maximum size. E.g.
    /// `"50:100:200"` or `"100px"`.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setGridGapY(BoundSize)
    public LC gridGapY(String boundsSize) {
        setGridGapY(ConstraintParser.parseBoundSize(boundsSize, true, false));
        return this;
    }

    /// Sets both grid gaps at the same time. see `#gridGapX(String)` and `#gridGapY(String)`.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `gapx`: @param gapx The `BoundSize` of the gap. This is a minimum and/or preferred and/or maximum size. E.g.
    /// `"50:100:200"` or `"100px"`.
    ///
    /// - `gapy`: @param gapy The `BoundSize` of the gap. This is a minimum and/or preferred and/or maximum size. E.g.
    /// `"50:100:200"` or `"100px"`.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #gridGapX(String)
    ///
    /// - #gridGapY(String)
    public LC gridGap(String gapx, String gapy) {
        if (gapx != null) {
            gridGapX(gapx);
        }

        if (gapy != null) {
            gridGapY(gapy);
        }

        return this;
    }

    /// Calls `#debug(int)` with 300 as an argument.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setDebugMillis(int)
    public LC debug() {
        setDebugMillis(300);
        return this;
    }

    /// Same functionality as `repaintMillis)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `repaintMillis`: The new debug repaint interval.
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setDebugMillis(int)
    public LC debug(int repaintMillis) {
        setDebugMillis(repaintMillis);
        return this;
    }

    /// Same functionality as `mode)` only this method returns `this` for chaining multiple calls.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcomponents.com.
    ///
    /// #### Parameters
    ///
    /// - `mode`: @param mode The mode:
    ///
    /// 0 == Normal. Bounds will be calculated as if the component was visible.
    ///
    /// 1 == If hidden the size will be 0, 0 but the gaps remain.
    ///
    /// 2 == If hidden the size will be 0, 0 and gaps set to zero.
    ///
    /// 3 == If hidden the component will be disregarded completely and not take up a cell in the grid..
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    ///
    /// #### See also
    ///
    /// - #setHideMode(int)
    public LC hideMode(int mode) {
        setHideMode(mode);
        return this;
    }

    /// The minimum width for the container. The value will override any value that is set on the container itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or Cheat Sheet at www.migcontainers.com.
    ///
    /// #### Parameters
    ///
    /// - `width`: The width expressed as a `UnitValue`. E.g. "100px" or "200mm".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC minWidth(String width) {
        setWidth(LayoutUtil.derive(getWidth(), ConstraintParser.parseUnitValue(width, true), null, null));
        return this;
    }

    /// The width for the container as a min and/or preferred and/or maximum width. The value will override any value that is set on
    /// the container itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or Cheat Sheet at www.migcontainers.com.
    ///
    /// #### Parameters
    ///
    /// - `width`: The width expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC width(String width) {
        setWidth(ConstraintParser.parseBoundSize(width, false, true));
        return this;
    }

    /// The maximum width for the container. The value will override any value that is set on the container itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or Cheat Sheet at www.migcontainers.com.
    ///
    /// #### Parameters
    ///
    /// - `width`: The width expressed as a `UnitValue`. E.g. "100px" or "200mm".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC maxWidth(String width) {
        setWidth(LayoutUtil.derive(getWidth(), null, null, ConstraintParser.parseUnitValue(width, true)));
        return this;
    }

    /// The minimum height for the container. The value will override any value that is set on the container itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or Cheat Sheet at www.migcontainers.com.
    ///
    /// #### Parameters
    ///
    /// - `height`: The height expressed as a `UnitValue`. E.g. "100px" or "200mm".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC minHeight(String height) {
        setHeight(LayoutUtil.derive(getHeight(), ConstraintParser.parseUnitValue(height, false), null, null));
        return this;
    }

    /// The height for the container as a min and/or preferred and/or maximum height. The value will override any value that is set on
    /// the container itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcontainers.com.
    ///
    /// #### Parameters
    ///
    /// - `height`: The height expressed as a `BoundSize`. E.g. "50:100px:200mm" or "100px".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC height(String height) {
        setHeight(ConstraintParser.parseBoundSize(height, false, false));
        return this;
    }

    /// The maximum height for the container. The value will override any value that is set on the container itself.
    ///
    /// For a more thorough explanation of what this constraint does see the white paper or cheat Sheet at www.migcontainers.com.
    ///
    /// #### Parameters
    ///
    /// - `height`: The height expressed as a `UnitValue`. E.g. "100px" or "200mm".
    ///
    /// #### Returns
    ///
    /// `this` so it is possible to chain calls. E.g. `new LayoutConstraint().noGrid().gap().fill()`.
    public LC maxHeight(String height) {
        setHeight(LayoutUtil.derive(getHeight(), null, null, ConstraintParser.parseUnitValue(height, false)));
        return this;
    }
}
