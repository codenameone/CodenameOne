/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package javafx.scene.layout;

import java.util.List;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.StyleList;
import com.codename1.fxcompat.runtime.Units;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;

/// A parent with a size of its own, a background, a border and padding;
/// the base of the layout panes and of the controls.
///
/// A region is resizable: its parent gives it a width and a height
/// between what `minWidth`/`minHeight` and `maxWidth`/`maxHeight` answer,
/// preferably `prefWidth`/`prefHeight`. Each of those is the value set
/// through the matching property, or computed from the content while the
/// property holds [#USE_COMPUTED_SIZE].
///
/// #### Painting
///
/// Each fill of the background is drawn in order, then each stroke of
/// the border. A border is always drawn inside the region's bounds, and a
/// stroke whose four sides differ in paint or width is drawn as four
/// straight sides without corner radii. Background images are not
/// supported.
///
/// #### Styling
///
/// The names a region takes through `cn1ApplyStyle` are listed in
/// `com.codename1.fxcompat.runtime.StyleTarget`. The background and
/// border names are an overlay: `-fx-background-color` replaces the
/// fills of the background the application set with one fill,
/// `-fx-background-radius` and `-fx-background-insets` change the radii
/// and insets of every fill, and the `-fx-border-*` names do the same to
/// the first stroke of the border. Withdrawing them all shows what the
/// application set again. `getBackground()` and `getBorder()` answer the
/// result of the overlay.
public class Region extends Parent {

    /// The size a region computes from its content.
    public static final double USE_COMPUTED_SIZE = -1;

    /// For a minimum or maximum: the same as the preferred size.
    public static final double USE_PREF_SIZE = Double.NEGATIVE_INFINITY;

    private static final int SIZE = Dirty.USER << 8;
    private static final int LOOK = Dirty.USER << 9;
    private static final int EDGE = Dirty.USER << 10;

    private final ReadOnlyDoubleWrapper width = new ReadOnlyDoubleWrapper(this, "width", 0);
    private final ReadOnlyDoubleWrapper height = new ReadOnlyDoubleWrapper(this, "height", 0);
    private final DoubleProperty minWidth = new FxDouble(this, "minWidth", USE_COMPUTED_SIZE, Dirty.LAYOUT);
    private final DoubleProperty minHeight = new FxDouble(this, "minHeight", USE_COMPUTED_SIZE, Dirty.LAYOUT);
    private final DoubleProperty prefWidth = new FxDouble(this, "prefWidth", USE_COMPUTED_SIZE, Dirty.LAYOUT);
    private final DoubleProperty prefHeight = new FxDouble(this, "prefHeight", USE_COMPUTED_SIZE, Dirty.LAYOUT);
    private final DoubleProperty maxWidth = new FxDouble(this, "maxWidth", USE_COMPUTED_SIZE, Dirty.LAYOUT);
    private final DoubleProperty maxHeight = new FxDouble(this, "maxHeight", USE_COMPUTED_SIZE, Dirty.LAYOUT);
    private final ObjectProperty<Insets> padding = new FxObject<Insets>(this, "padding", Insets.EMPTY, SIZE);
    private final ObjectProperty<Background> background = new FxObject<Background>(this, "background", null, LOOK);
    private final ObjectProperty<Border> border = new FxObject<Border>(this, "border", null, EDGE | SIZE);
    private final BooleanProperty snapToPixel = new FxBoolean(this, "snapToPixel", true, Dirty.LAYOUT);
    private final ObjectProperty<Shape> shape = new FxObject<Shape>(this, "shape", null, Dirty.PAINT);
    private final BooleanProperty scaleShape = new FxBoolean(this, "scaleShape", true, Dirty.PAINT);
    private final BooleanProperty centerShape = new FxBoolean(this, "centerShape", true, Dirty.PAINT);

    private Background baseBackground;
    private Border baseBorder;
    private boolean composing;
    private Object styleFill;
    private Object styleFillRadii;
    private Object styleFillInsets;
    private Object styleStroke;
    private Object styleStrokeWidths;
    private Object styleStrokeRadii;
    private Object styleStrokeStyle;
    private Object styleStrokeInsets;
    private Insets insetsCache;

    /// Creates an empty region.
    public Region() {
        setPickOnBounds(true);
    }

    @Override
    public void cn1Invalidated(int what) {
        int rest = what;
        if ((what & LOOK) != 0) {
            if (!composing) {
                baseBackground = background.get();
            }
            rest |= Dirty.PAINT;
        }
        if ((what & EDGE) != 0) {
            if (!composing) {
                baseBorder = border.get();
            }
            rest |= Dirty.PAINT;
        }
        if ((what & SIZE) != 0) {
            insetsCache = null;
            rest |= Dirty.LAYOUT;
            requestLayout();
        }
        super.cn1Invalidated(rest);
    }

    @Override
    protected void cn1RequestLayout() {
        // A change of this region's own constraints needs both this region
        // and its parent laid out again.
        requestLayout();
    }

    /// The standard theme gives the root of a scene the background colour
    /// of the theme, the light grey a JavaFX window has behind its
    /// controls; the white fill of the scene shows only under a root that
    /// is not a region. A sheet that gives the root a background of its
    /// own wins, as every rule does over these.
    @Override
    public String cn1DefaultStyle() {
        Scene scene = getScene();
        return scene != null && scene.getRoot() == this ? "-fx-background-color: -fx-background;" : null;
    }

    // -------------------------------------------------------------- size

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    protected boolean cn1SizedByChildren() {
        return false;
    }

    @Override
    protected Bounds cn1ComputeLayoutBounds() {
        return new BoundingBox(0, 0, getWidth(), getHeight());
    }

    @Override
    public void resize(double width, double height) {
        double w = width < 0 || Double.isNaN(width) ? 0 : width;
        double h = height < 0 || Double.isNaN(height) ? 0 : height;
        if (w != getWidth() || h != getHeight()) {
            this.width.set(w);
            this.height.set(h);
            cn1MarkNeedsLayout();
            cn1ResizedGeometry();
        }
    }

    private void cn1ResizedGeometry() {
        cn1GeometryChanged();
    }

    /// Returns the width the parent gave this region.
    public final double getWidth() {
        return width.get();
    }

    /// The width the parent gave this region.
    public final ReadOnlyDoubleProperty widthProperty() {
        return width.getReadOnlyProperty();
    }

    /// Sets the width; for subclasses.
    protected void setWidth(double value) {
        resize(value, getHeight());
    }

    /// Returns the height the parent gave this region.
    public final double getHeight() {
        return height.get();
    }

    /// The height the parent gave this region.
    public final ReadOnlyDoubleProperty heightProperty() {
        return height.getReadOnlyProperty();
    }

    /// Sets the height; for subclasses.
    protected void setHeight(double value) {
        resize(getWidth(), value);
    }

    /// Returns the minimum width override.
    public final double getMinWidth() {
        return minWidth.get();
    }

    /// Sets the minimum width, [#USE_COMPUTED_SIZE] or [#USE_PREF_SIZE].
    public final void setMinWidth(double value) {
        minWidth.set(value);
    }

    /// The minimum width override.
    public final DoubleProperty minWidthProperty() {
        return minWidth;
    }

    /// Returns the minimum height override.
    public final double getMinHeight() {
        return minHeight.get();
    }

    /// Sets the minimum height, [#USE_COMPUTED_SIZE] or [#USE_PREF_SIZE].
    public final void setMinHeight(double value) {
        minHeight.set(value);
    }

    /// The minimum height override.
    public final DoubleProperty minHeightProperty() {
        return minHeight;
    }

    /// Sets both minimum overrides.
    public void setMinSize(double minWidth, double minHeight) {
        setMinWidth(minWidth);
        setMinHeight(minHeight);
    }

    /// Returns the preferred width override.
    public final double getPrefWidth() {
        return prefWidth.get();
    }

    /// Sets the preferred width, or [#USE_COMPUTED_SIZE].
    public final void setPrefWidth(double value) {
        prefWidth.set(value);
    }

    /// The preferred width override.
    public final DoubleProperty prefWidthProperty() {
        return prefWidth;
    }

    /// Returns the preferred height override.
    public final double getPrefHeight() {
        return prefHeight.get();
    }

    /// Sets the preferred height, or [#USE_COMPUTED_SIZE].
    public final void setPrefHeight(double value) {
        prefHeight.set(value);
    }

    /// The preferred height override.
    public final DoubleProperty prefHeightProperty() {
        return prefHeight;
    }

    /// Sets both preferred overrides.
    public void setPrefSize(double prefWidth, double prefHeight) {
        setPrefWidth(prefWidth);
        setPrefHeight(prefHeight);
    }

    /// Returns the maximum width override.
    public final double getMaxWidth() {
        return maxWidth.get();
    }

    /// Sets the maximum width, [#USE_COMPUTED_SIZE] or [#USE_PREF_SIZE].
    public final void setMaxWidth(double value) {
        maxWidth.set(value);
    }

    /// The maximum width override.
    public final DoubleProperty maxWidthProperty() {
        return maxWidth;
    }

    /// Returns the maximum height override.
    public final double getMaxHeight() {
        return maxHeight.get();
    }

    /// Sets the maximum height, [#USE_COMPUTED_SIZE] or [#USE_PREF_SIZE].
    public final void setMaxHeight(double value) {
        maxHeight.set(value);
    }

    /// The maximum height override.
    public final DoubleProperty maxHeightProperty() {
        return maxHeight;
    }

    /// Sets both maximum overrides.
    public void setMaxSize(double maxWidth, double maxHeight) {
        setMaxWidth(maxWidth);
        setMaxHeight(maxHeight);
    }

    @Override
    public final double prefWidth(double height) {
        double override = getPrefWidth();
        if (override == USE_COMPUTED_SIZE) {
            return super.prefWidth(height);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
    }

    @Override
    public final double prefHeight(double width) {
        double override = getPrefHeight();
        if (override == USE_COMPUTED_SIZE) {
            return super.prefHeight(width);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
    }

    @Override
    public final double minWidth(double height) {
        double override = getMinWidth();
        if (override == USE_COMPUTED_SIZE) {
            return super.minWidth(height);
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
    }

    @Override
    public final double minHeight(double width) {
        double override = getMinHeight();
        if (override == USE_COMPUTED_SIZE) {
            return super.minHeight(width);
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
    }

    @Override
    public final double maxWidth(double height) {
        double override = getMaxWidth();
        if (override == USE_COMPUTED_SIZE) {
            return computeMaxWidth(height);
        } else if (override == USE_PREF_SIZE) {
            return prefWidth(height);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
    }

    @Override
    public final double maxHeight(double width) {
        double override = getMaxHeight();
        if (override == USE_COMPUTED_SIZE) {
            return computeMaxHeight(width);
        } else if (override == USE_PREF_SIZE) {
            return prefHeight(width);
        }
        return Double.isNaN(override) || override < 0 ? 0 : override;
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + in.getBottom();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + super.computePrefWidth(height) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + super.computePrefHeight(width) + in.getBottom();
    }

    /// Computes the maximum width; unbounded unless overridden.
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    /// Computes the maximum height; unbounded unless overridden.
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public boolean contains(double localX, double localY) {
        return localX >= 0 && localY >= 0 && localX <= getWidth() && localY <= getHeight();
    }

    @Override
    protected boolean cn1PickSelf(double x, double y) {
        return contains(x, y);
    }

    // ------------------------------------------------ padding and insets

    /// Returns the space kept free inside the border.
    public final Insets getPadding() {
        Insets p = padding.get();
        return p == null ? Insets.EMPTY : p;
    }

    /// Sets the space kept free inside the border.
    public final void setPadding(Insets value) {
        padding.set(value);
    }

    /// The space kept free inside the border.
    public final ObjectProperty<Insets> paddingProperty() {
        return padding;
    }

    /// Returns the space between the edges and the content: the border's
    /// insets plus the padding.
    public final Insets getInsets() {
        if (insetsCache == null) {
            Insets p = getPadding();
            Border b = getBorder();
            Insets bi = b == null ? Insets.EMPTY : b.getInsets();
            insetsCache = new Insets(snapSizeY(p.getTop() + bi.getTop()), snapSizeX(p.getRight() + bi.getRight()),
                    snapSizeY(p.getBottom() + bi.getBottom()), snapSizeX(p.getLeft() + bi.getLeft()));
        }
        return insetsCache;
    }

    /// Returns the top inset on a pixel boundary.
    public final double snappedTopInset() {
        return getInsets().getTop();
    }

    /// Returns the bottom inset on a pixel boundary.
    public final double snappedBottomInset() {
        return getInsets().getBottom();
    }

    /// Returns the left inset on a pixel boundary.
    public final double snappedLeftInset() {
        return getInsets().getLeft();
    }

    /// Returns the right inset on a pixel boundary.
    public final double snappedRightInset() {
        return getInsets().getRight();
    }

    /// Returns whether layout rounds to device pixels.
    public final boolean isSnapToPixel() {
        return snapToPixel.get();
    }

    /// Sets whether layout rounds to device pixels.
    public final void setSnapToPixel(boolean value) {
        snapToPixel.set(value);
    }

    /// Whether layout rounds to device pixels.
    public final BooleanProperty snapToPixelProperty() {
        return snapToPixel;
    }

    /// Rounds a horizontal gap to device pixels when snapping.
    public double snapSpaceX(double value) {
        return isSnapToPixel() ? Units.snap(value) : value;
    }

    /// Rounds a vertical gap to device pixels when snapping.
    public double snapSpaceY(double value) {
        return isSnapToPixel() ? Units.snap(value) : value;
    }

    /// Grows a width to whole device pixels when snapping.
    public double snapSizeX(double value) {
        return isSnapToPixel() ? Units.snapSize(value) : value;
    }

    /// Grows a height to whole device pixels when snapping.
    public double snapSizeY(double value) {
        return isSnapToPixel() ? Units.snapSize(value) : value;
    }

    /// Rounds an x position to device pixels when snapping.
    public double snapPositionX(double value) {
        return isSnapToPixel() ? Units.snap(value) : value;
    }

    /// Rounds a y position to device pixels when snapping.
    public double snapPositionY(double value) {
        return isSnapToPixel() ? Units.snap(value) : value;
    }

    // ------------------------------------------------- layout utilities

    static double boundedSize(double min, double pref, double max) {
        double a = pref >= min ? pref : min;
        double b = min >= max ? min : max;
        return a <= b ? a : b;
    }

    /// Returns the width a child takes in an area of a given height, less
    /// its margin: its preferred width kept between its minimum and
    /// maximum.
    double computeChildPrefAreaWidth(Node child, Insets margin, double height) {
        double left = margin == null ? 0 : snapSpaceX(margin.getLeft());
        double right = margin == null ? 0 : snapSpaceX(margin.getRight());
        double alt = -1;
        if (height != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) {
            double top = margin == null ? 0 : snapSpaceY(margin.getTop());
            double bottom = margin == null ? 0 : snapSpaceY(margin.getBottom());
            alt = boundedSize(child.minHeight(-1), height - top - bottom, child.maxHeight(-1));
        }
        return left + snapSizeX(boundedSize(child.minWidth(alt), child.prefWidth(alt), child.maxWidth(alt))) + right;
    }

    double computeChildPrefAreaHeight(Node child, Insets margin, double width) {
        double top = margin == null ? 0 : snapSpaceY(margin.getTop());
        double bottom = margin == null ? 0 : snapSpaceY(margin.getBottom());
        double alt = -1;
        if (width != -1 && child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) {
            double left = margin == null ? 0 : snapSpaceX(margin.getLeft());
            double right = margin == null ? 0 : snapSpaceX(margin.getRight());
            alt = boundedSize(child.minWidth(-1), width - left - right, child.maxWidth(-1));
        }
        return top + snapSizeY(boundedSize(child.minHeight(alt), child.prefHeight(alt), child.maxHeight(alt)))
                + bottom;
    }

    double computeChildMinAreaWidth(Node child, Insets margin, double height) {
        double left = margin == null ? 0 : snapSpaceX(margin.getLeft());
        double right = margin == null ? 0 : snapSpaceX(margin.getRight());
        double alt = -1;
        if (height != -1 && child.isResizable() && child.getContentBias() == Orientation.VERTICAL) {
            double top = margin == null ? 0 : snapSpaceY(margin.getTop());
            double bottom = margin == null ? 0 : snapSpaceY(margin.getBottom());
            alt = boundedSize(child.minHeight(-1), height - top - bottom, child.maxHeight(-1));
        }
        return left + snapSizeX(child.minWidth(alt)) + right;
    }

    double computeChildMinAreaHeight(Node child, Insets margin, double width) {
        double top = margin == null ? 0 : snapSpaceY(margin.getTop());
        double bottom = margin == null ? 0 : snapSpaceY(margin.getBottom());
        double alt = -1;
        if (width != -1 && child.isResizable() && child.getContentBias() == Orientation.HORIZONTAL) {
            double left = margin == null ? 0 : snapSpaceX(margin.getLeft());
            double right = margin == null ? 0 : snapSpaceX(margin.getRight());
            alt = boundedSize(child.minWidth(-1), width - left - right, child.maxWidth(-1));
        }
        return top + snapSizeY(child.minHeight(alt)) + bottom;
    }

    double computeChildMaxAreaWidth(Node child, Insets margin, double height) {
        double max = child.maxWidth(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }
        double left = margin == null ? 0 : snapSpaceX(margin.getLeft());
        double right = margin == null ? 0 : snapSpaceX(margin.getRight());
        return left + snapSizeX(boundedSize(child.minWidth(-1), max, Double.MAX_VALUE)) + right;
    }

    double computeChildMaxAreaHeight(Node child, Insets margin, double width) {
        double max = child.maxHeight(-1);
        if (max == Double.MAX_VALUE) {
            return max;
        }
        double top = margin == null ? 0 : snapSpaceY(margin.getTop());
        double bottom = margin == null ? 0 : snapSpaceY(margin.getBottom());
        return top + snapSizeY(boundedSize(child.minHeight(-1), max, Double.MAX_VALUE)) + bottom;
    }

    /// Sizes a child to fit an area and positions it there.
    protected void layoutInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
            double areaBaselineOffset, HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, Insets.EMPTY, true, true,
                halignment, valignment);
    }

    /// Sizes a child to fit an area less a margin and positions it there.
    protected void layoutInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
            double areaBaselineOffset, Insets margin, HPos halignment, VPos valignment) {
        layoutInArea(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, margin, true, true, halignment,
                valignment);
    }

    /// Sizes a child to an area less a margin and positions it there. A
    /// resizable child fills the area in each direction asked for, up to
    /// its maximum; otherwise it takes its preferred size, never more
    /// than the area unless its minimum is larger.
    protected void layoutInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
            double areaBaselineOffset, Insets margin, boolean fillWidth, boolean fillHeight, HPos halignment,
            VPos valignment) {
        Insets m = margin == null ? Insets.EMPTY : margin;
        double top = snapSpaceY(m.getTop());
        double bottom = snapSpaceY(m.getBottom());
        double left = snapSpaceX(m.getLeft());
        double right = snapSpaceX(m.getRight());
        if (valignment == VPos.BASELINE) {
            double bo = child.getBaselineOffset();
            if (bo == BASELINE_OFFSET_SAME_AS_HEIGHT) {
                if (child.isResizable()) {
                    bottom += snapSpaceY(areaHeight - areaBaselineOffset);
                } else {
                    top = snapSpaceY(areaBaselineOffset - child.getLayoutBounds().getHeight());
                }
            } else {
                top = snapSpaceY(areaBaselineOffset - bo);
            }
        }
        if (child.isResizable()) {
            double innerW = Math.max(0, areaWidth - left - right);
            double innerH = Math.max(0, areaHeight - top - bottom);
            Orientation bias = child.getContentBias();
            double w;
            double h;
            if (bias == Orientation.HORIZONTAL) {
                w = areaSize(innerW, child.minWidth(-1), child.prefWidth(-1), child.maxWidth(-1), fillWidth);
                h = areaSize(innerH, child.minHeight(w), child.prefHeight(w), child.maxHeight(w), fillHeight);
            } else if (bias == Orientation.VERTICAL) {
                h = areaSize(innerH, child.minHeight(-1), child.prefHeight(-1), child.maxHeight(-1), fillHeight);
                w = areaSize(innerW, child.minWidth(h), child.prefWidth(h), child.maxWidth(h), fillWidth);
            } else {
                w = areaSize(innerW, child.minWidth(-1), child.prefWidth(-1), child.maxWidth(-1), fillWidth);
                h = areaSize(innerH, child.minHeight(-1), child.prefHeight(-1), child.maxHeight(-1), fillHeight);
            }
            child.resize(snapSizeX(w), snapSizeY(h));
        }
        position(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, top, right, bottom, left,
                halignment, valignment);
    }

    private static double areaSize(double available, double min, double pref, double max, boolean fill) {
        double wanted = fill ? available : Math.min(available, pref);
        return boundedSize(min, wanted, max);
    }

    /// Positions a child in an area without resizing it.
    protected void positionInArea(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
            double areaBaselineOffset, HPos halignment, VPos valignment) {
        position(child, areaX, areaY, areaWidth, areaHeight, areaBaselineOffset, 0, 0, 0, 0, halignment, valignment);
    }

    /// Positions a child in an area less a margin without resizing it;
    /// for the panes of this package.
    void positionInMargin(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
            Insets margin, HPos halignment, VPos valignment) {
        Insets m = margin == null ? Insets.EMPTY : margin;
        position(child, areaX, areaY, areaWidth, areaHeight, 0, snapSpaceY(m.getTop()), snapSpaceX(m.getRight()),
                snapSpaceY(m.getBottom()), snapSpaceX(m.getLeft()), halignment, valignment);
    }

    private void position(Node child, double areaX, double areaY, double areaWidth, double areaHeight,
            double areaBaselineOffset, double top, double right, double bottom, double left, HPos hpos, VPos vpos) {
        Bounds lb = child.getLayoutBounds();
        double w = lb.getWidth();
        double h = lb.getHeight();
        double x = left;
        if (hpos == HPos.CENTER) {
            x = left + (areaWidth - left - right - w) / 2;
        } else if (hpos == HPos.RIGHT) {
            x = areaWidth - right - w;
        }
        double y = top;
        if (vpos == VPos.CENTER) {
            y = top + (areaHeight - top - bottom - h) / 2;
        } else if (vpos == VPos.BOTTOM) {
            y = areaHeight - bottom - h;
        } else if (vpos == VPos.BASELINE) {
            double bo = child.getBaselineOffset();
            y = bo == BASELINE_OFFSET_SAME_AS_HEIGHT ? areaBaselineOffset - h : areaBaselineOffset - bo;
        }
        child.relocate(snapPositionX(areaX + x), snapPositionY(areaY + y));
    }

    // -------------------------------------------------------------- shape

    /// Returns the shape this region takes its form from, `null` for the
    /// rectangle it is.
    public final Shape getShape() {
        return shape.get();
    }

    /// Gives this region the form of a shape: its background fills and
    /// its border are drawn as that shape instead of as rectangles. The
    /// shape is read when the region paints; it is not a child of the
    /// region, and changing it afterwards shows at the next repaint of
    /// the region, not by itself. The size the region computes for
    /// itself does not change with the shape.
    public final void setShape(Shape value) {
        shape.set(value);
    }

    /// The shape this region takes its form from.
    public final ObjectProperty<Shape> shapeProperty() {
        return shape;
    }

    /// Returns whether the shape is stretched to the size of the region.
    public final boolean isScaleShape() {
        return scaleShape.get();
    }

    /// Sets whether the shape is stretched to the size of the region.
    public final void setScaleShape(boolean value) {
        scaleShape.set(value);
    }

    /// Whether the shape is stretched to the size of the region.
    public final BooleanProperty scaleShapeProperty() {
        return scaleShape;
    }

    /// Returns whether a shape that is not stretched is centred.
    public final boolean isCenterShape() {
        return centerShape.get();
    }

    /// Sets whether a shape that is not stretched is centred.
    public final void setCenterShape(boolean value) {
        centerShape.set(value);
    }

    /// Whether a shape that is not stretched is centred.
    public final BooleanProperty centerShapeProperty() {
        return centerShape;
    }

    // --------------------------------------------- background and border

    /// Returns the background, or `null`.
    public final Background getBackground() {
        return background.get();
    }

    /// Sets the background.
    public final void setBackground(Background value) {
        background.set(value);
    }

    /// The background of this region.
    public final ObjectProperty<Background> backgroundProperty() {
        return background;
    }

    /// Returns the border, or `null`.
    public final Border getBorder() {
        return border.get();
    }

    /// Sets the border.
    public final void setBorder(Border value) {
        border.set(value);
    }

    /// The border of this region.
    public final ObjectProperty<Border> borderProperty() {
        return border;
    }

    private static CornerRadii radii(Object value, CornerRadii fallback) {
        if (value instanceof CornerRadii) {
            return (CornerRadii) value;
        } else if (value instanceof Number) {
            return new CornerRadii(((Number) value).doubleValue());
        }
        return fallback;
    }

    private static Insets insets(Object value, Insets fallback) {
        if (value instanceof Insets) {
            return (Insets) value;
        } else if (value instanceof Number) {
            return new Insets(((Number) value).doubleValue());
        }
        return fallback;
    }

    private static Paint side(StyleList sides, int index) {
        Object p = sides.get(index);
        return p instanceof Paint ? (Paint) p : null;
    }

    /// Whether `value` is what `single` accepts, or layers that each are.
    private static boolean layersOf(Object value, int what) {
        for (int i = 0; i < StyleList.layers(value); i++) {
            Object v = StyleList.layer(value, i);
            boolean ok;
            switch (what) {
                case 0:
                    ok = v instanceof Paint;
                    break;
                case 1:
                    ok = v instanceof Paint || (v instanceof StyleList && ((StyleList) v).isSides());
                    break;
                case 2:
                    ok = v instanceof CornerRadii || v instanceof Number;
                    break;
                case 3:
                    ok = v instanceof Insets || v instanceof Number || v instanceof BorderWidths;
                    break;
                default:
                    ok = strokeStyle(v) != null;
                    break;
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private void compose() {
        composing = true;
        try {
            Background bg = baseBackground;
            if (styleFill != null || styleFillRadii != null || styleFillInsets != null) {
                List<BackgroundFill> fills = bg == null ? null : bg.getFills();
                if (styleFill != null) {
                    // One fill per colour; a colour without radii or
                    // insets of its own takes those of the last layer
                    // that has them.
                    BackgroundFill first = fills == null || fills.isEmpty() ? null : fills.get(0);
                    BackgroundFill[] made = new BackgroundFill[StyleList.layers(styleFill)];
                    for (int i = 0; i < made.length; i++) {
                        Object paint = StyleList.layer(styleFill, i);
                        made[i] = new BackgroundFill(paint instanceof Paint ? (Paint) paint : null,
                                radii(StyleList.layer(styleFillRadii, i),
                                        first == null ? CornerRadii.EMPTY : first.getRadii()),
                                insets(StyleList.layer(styleFillInsets, i),
                                        first == null ? Insets.EMPTY : first.getInsets()));
                    }
                    bg = new Background(made);
                } else if (fills != null && !fills.isEmpty()) {
                    BackgroundFill[] changed = new BackgroundFill[fills.size()];
                    for (int i = 0; i < changed.length; i++) {
                        BackgroundFill f = fills.get(i);
                        changed[i] = new BackgroundFill(f.getFill(),
                                radii(StyleList.layer(styleFillRadii, i), f.getRadii()),
                                insets(StyleList.layer(styleFillInsets, i), f.getInsets()));
                    }
                    bg = new Background(changed);
                }
            }
            background.set(bg);
            Border b = baseBorder;
            if (styleStroke != null || styleStrokeWidths != null || styleStrokeRadii != null
                    || styleStrokeStyle != null || styleStrokeInsets != null) {
                List<BorderStroke> strokes = b == null ? null : b.getStrokes();
                BorderStroke first = strokes == null || strokes.isEmpty() ? null : strokes.get(0);
                // One stroke per colour, as one fill per colour above.
                BorderStroke[] made = new BorderStroke[StyleList.layers(styleStroke)];
                int count = 0;
                for (int i = 0; i < made.length; i++) {
                    Object paint = StyleList.layer(styleStroke, i);
                    Paint top = first == null ? null : first.getTopStroke();
                    Paint right = first == null ? null : first.getRightStroke();
                    Paint bottom = first == null ? null : first.getBottomStroke();
                    Paint left = first == null ? null : first.getLeftStroke();
                    if (paint instanceof Paint) {
                        top = (Paint) paint;
                        right = top;
                        bottom = top;
                        left = top;
                    } else if (paint instanceof StyleList && ((StyleList) paint).size() == 4) {
                        StyleList sides = (StyleList) paint;
                        top = side(sides, 0);
                        right = side(sides, 1);
                        bottom = side(sides, 2);
                        left = side(sides, 3);
                    }
                    if (top == null && right == null && bottom == null && left == null) {
                        continue;
                    }
                    BorderWidths widths = first == null ? BorderWidths.DEFAULT : first.getWidths();
                    Object w = StyleList.layer(styleStrokeWidths, i);
                    if (w instanceof BorderWidths) {
                        widths = (BorderWidths) w;
                    } else if (w instanceof Insets) {
                        Insets in = (Insets) w;
                        widths = new BorderWidths(in.getTop(), in.getRight(), in.getBottom(), in.getLeft());
                    } else if (w instanceof Number) {
                        widths = new BorderWidths(((Number) w).doubleValue());
                    }
                    BorderStrokeStyle strokeStyle = strokeStyle(StyleList.layer(styleStrokeStyle, i));
                    if (strokeStyle == null) {
                        strokeStyle = first == null ? BorderStrokeStyle.SOLID : first.getTopStyle();
                    }
                    made[count++] = new BorderStroke(top, right, bottom, left, strokeStyle, strokeStyle,
                            strokeStyle, strokeStyle,
                            radii(StyleList.layer(styleStrokeRadii, i),
                                    first == null ? CornerRadii.EMPTY : first.getRadii()),
                            widths,
                            insets(StyleList.layer(styleStrokeInsets, i),
                                    first == null ? Insets.EMPTY : first.getInsets()));
                }
                if (count > 0) {
                    BorderStroke[] kept = new BorderStroke[count];
                    System.arraycopy(made, 0, kept, 0, count);
                    b = new Border(kept);
                }
            }
            border.set(b);
        } finally {
            composing = false;
        }
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-background-color".equals(property)) {
            return styleFill;
        } else if ("-fx-background-radius".equals(property)) {
            return styleFillRadii;
        } else if ("-fx-background-insets".equals(property)) {
            return styleFillInsets;
        } else if ("-fx-border-color".equals(property)) {
            return styleStroke;
        } else if ("-fx-border-width".equals(property)) {
            return styleStrokeWidths;
        } else if ("-fx-border-radius".equals(property)) {
            return styleStrokeRadii;
        } else if ("-fx-border-style".equals(property)) {
            return styleStrokeStyle;
        } else if ("-fx-border-insets".equals(property)) {
            return styleStrokeInsets;
        } else if ("-fx-padding".equals(property)) {
            return getPadding();
        } else if ("-fx-min-width".equals(property)) {
            return Double.valueOf(getMinWidth());
        } else if ("-fx-pref-width".equals(property)) {
            return Double.valueOf(getPrefWidth());
        } else if ("-fx-max-width".equals(property)) {
            return Double.valueOf(getMaxWidth());
        } else if ("-fx-min-height".equals(property)) {
            return Double.valueOf(getMinHeight());
        } else if ("-fx-pref-height".equals(property)) {
            return Double.valueOf(getPrefHeight());
        } else if ("-fx-max-height".equals(property)) {
            return Double.valueOf(getMaxHeight());
        } else if ("-fx-snap-to-pixel".equals(property)) {
            return Boolean.valueOf(isSnapToPixel());
        } else if ("-fx-shape".equals(property)) {
            return getShape();
        } else if ("-fx-scale-shape".equals(property)) {
            return Boolean.valueOf(isScaleShape());
        } else if ("-fx-position-shape".equals(property)) {
            return Boolean.valueOf(isCenterShape());
        }
        return super.cn1StyleValue(property);
    }

    private static BorderStrokeStyle strokeStyle(Object value) {
        if (value instanceof BorderStrokeStyle) {
            return (BorderStrokeStyle) value;
        } else if (value instanceof String) {
            String s = ((String) value).trim();
            if ("none".equalsIgnoreCase(s)) {
                return BorderStrokeStyle.NONE;
            } else if ("solid".equalsIgnoreCase(s)) {
                return BorderStrokeStyle.SOLID;
            } else if ("dashed".equalsIgnoreCase(s)) {
                return BorderStrokeStyle.DASHED;
            } else if ("dotted".equalsIgnoreCase(s)) {
                return BorderStrokeStyle.DOTTED;
            }
        }
        return null;
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-background-color".equals(property)) {
            if (value != null && !layersOf(value, 0)) {
                return false;
            }
            styleFill = value;
        } else if ("-fx-background-radius".equals(property)) {
            if (value != null && !layersOf(value, 2)) {
                return false;
            }
            styleFillRadii = value;
        } else if ("-fx-background-insets".equals(property)) {
            if (value != null && !layersOf(value, 3)) {
                return false;
            }
            styleFillInsets = value;
        } else if ("-fx-border-color".equals(property)) {
            if (value != null && !layersOf(value, 1)) {
                return false;
            }
            styleStroke = value;
        } else if ("-fx-border-width".equals(property)) {
            if (value != null && !layersOf(value, 3)) {
                return false;
            }
            styleStrokeWidths = value;
        } else if ("-fx-border-radius".equals(property)) {
            if (value != null && !layersOf(value, 2)) {
                return false;
            }
            styleStrokeRadii = value;
        } else if ("-fx-border-style".equals(property)) {
            if (value != null && !layersOf(value, 4)) {
                return false;
            }
            styleStrokeStyle = value;
        } else if ("-fx-border-insets".equals(property)) {
            if (value != null && !layersOf(value, 3)) {
                return false;
            }
            styleStrokeInsets = value;
        } else if ("-fx-padding".equals(property)) {
            Insets in = insets(value, null);
            if (in == null) {
                return false;
            }
            setPadding(in);
            return true;
        } else if ("-fx-snap-to-pixel".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            setSnapToPixel(((Boolean) value).booleanValue());
            return true;
        } else if ("-fx-shape".equals(property)) {
            // A style sheet gives the path as text; what comes back to
            // restore the property is the shape that was there before.
            if (value instanceof String) {
                SVGPath svg = new SVGPath();
                svg.setContent((String) value);
                setShape(svg);
            } else if (value instanceof Shape) {
                setShape((Shape) value);
            } else if (value == null) {
                setShape(null);
            } else {
                return false;
            }
            return true;
        } else if ("-fx-scale-shape".equals(property) || "-fx-position-shape".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            if ("-fx-scale-shape".equals(property)) {
                setScaleShape(((Boolean) value).booleanValue());
            } else {
                setCenterShape(((Boolean) value).booleanValue());
            }
            return true;
        } else if (property.endsWith("-width") || property.endsWith("-height")) {
            DoubleProperty target = sizeProperty(property);
            if (target == null) {
                return super.cn1SetStyleValue(property, value);
            }
            if (!(value instanceof Number)) {
                return false;
            }
            target.set(((Number) value).doubleValue());
            return true;
        } else {
            return super.cn1SetStyleValue(property, value);
        }
        compose();
        return true;
    }

    private DoubleProperty sizeProperty(String property) {
        if ("-fx-min-width".equals(property)) {
            return minWidth;
        } else if ("-fx-pref-width".equals(property)) {
            return prefWidth;
        } else if ("-fx-max-width".equals(property)) {
            return maxWidth;
        } else if ("-fx-min-height".equals(property)) {
            return minHeight;
        } else if ("-fx-pref-height".equals(property)) {
            return prefHeight;
        } else if ("-fx-max-height".equals(property)) {
            return maxHeight;
        }
        return null;
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Background bg = getBackground();
        Shape form = getShape();
        FxPath formPath = form == null ? null : form.cn1Outline();
        double[] formBounds = formPath == null ? null : formPath.bounds();
        if (formBounds != null) {
            paintShaped(renderer, formPath, formBounds, w, h);
            return;
        }
        if (bg != null) {
            List<BackgroundFill> fills = bg.getFills();
            for (int i = 0; i < fills.size(); i++) {
                BackgroundFill f = fills.get(i);
                Insets in = f.getInsets();
                double x = in.getLeft();
                double y = in.getTop();
                double fw = w - in.getLeft() - in.getRight();
                double fh = h - in.getTop() - in.getBottom();
                if (f.getFill() == null || fw <= 0 || fh <= 0) {
                    continue;
                }
                CornerRadii r = f.getRadii();
                if (isSquare(r)) {
                    renderer.fillRect(x, y, fw, fh, f.getFill());
                } else {
                    renderer.fill(outline(r, x, y, fw, fh, 0), f.getFill(), x, y, fw, fh);
                }
            }
        }
        Border b = getBorder();
        if (b != null) {
            List<BorderStroke> strokes = b.getStrokes();
            for (int i = 0; i < strokes.size(); i++) {
                paintStroke(renderer, strokes.get(i), w, h);
            }
        }
    }

    /// The shape placed in a rectangle of this region: stretched over it
    /// when the shape is scaled, else at its own size and centred when
    /// it is positioned.
    private FxPath placed(FxPath path, double[] b, double x, double y, double w, double h) {
        double pw = b[2] - b[0];
        double ph = b[3] - b[1];
        double sx = 1;
        double sy = 1;
        if (isScaleShape()) {
            sx = pw > 0 ? w / pw : 1;
            sy = ph > 0 ? h / ph : 1;
        }
        double tx;
        double ty;
        if (isScaleShape() || isCenterShape()) {
            tx = x + (w - pw * sx) / 2 - b[0] * sx;
            ty = y + (h - ph * sy) / 2 - b[1] * sy;
        } else {
            tx = x;
            ty = y;
        }
        return path.transformed(new double[] {sx, 0, 0, sy, tx, ty});
    }

    /// Paints the fills and the strokes in the form of the shape rather
    /// than as rectangles; the corner radii play no part then.
    private void paintShaped(Renderer renderer, FxPath path, double[] bounds, double w, double h) {
        Background bg = getBackground();
        if (bg != null) {
            List<BackgroundFill> fills = bg.getFills();
            for (int i = 0; i < fills.size(); i++) {
                BackgroundFill f = fills.get(i);
                Insets in = f.getInsets();
                double x = in.getLeft();
                double y = in.getTop();
                double fw = w - in.getLeft() - in.getRight();
                double fh = h - in.getTop() - in.getBottom();
                if (f.getFill() != null && fw > 0 && fh > 0) {
                    renderer.fill(placed(path, bounds, x, y, fw, fh), f.getFill(), x, y, fw, fh);
                }
            }
        }
        Border b = getBorder();
        if (b != null) {
            List<BorderStroke> strokes = b.getStrokes();
            for (int i = 0; i < strokes.size(); i++) {
                BorderStroke s = strokes.get(i);
                BorderWidths bw = s.getWidths();
                BorderStrokeStyle style = s.getTopStyle();
                if (bw == null || style == null || style == BorderStrokeStyle.NONE || bw.getTop() <= 0
                        || s.getTopStroke() == null) {
                    continue;
                }
                renderer.stroke(placed(path, bounds, 0, 0, w, h), s.getTopStroke(), bw.getTop(),
                        style.getLineCap(), style.getLineJoin(), style.getMiterLimit(), dashes(style),
                        style.getDashOffset());
            }
        }
    }

    private static boolean isSquare(CornerRadii r) {
        return r == null || (r.getTopLeftHorizontalRadius() <= 0 && r.getTopRightHorizontalRadius() <= 0
                && r.getBottomRightHorizontalRadius() <= 0 && r.getBottomLeftHorizontalRadius() <= 0);
    }

    private static double radius(double value, boolean percent, double w, double h, double shrink) {
        double r = percent ? value * Math.min(w, h) : value;
        r -= shrink;
        double limit = Math.min(w, h) / 2;
        return r < 0 ? 0 : (r > limit ? limit : r);
    }

    private static FxPath outline(CornerRadii r, double x, double y, double w, double h, double shrink) {
        return Renderer.roundRect(x, y, w, h,
                radius(r.getTopLeftHorizontalRadius(), r.isTopLeftHorizontalRadiusAsPercentage(), w, h, shrink),
                radius(r.getTopRightHorizontalRadius(), r.isTopRightHorizontalRadiusAsPercentage(), w, h, shrink),
                radius(r.getBottomRightHorizontalRadius(), r.isBottomRightHorizontalRadiusAsPercentage(), w, h,
                        shrink),
                radius(r.getBottomLeftHorizontalRadius(), r.isBottomLeftHorizontalRadiusAsPercentage(), w, h,
                        shrink));
    }

    private static double[] dashes(BorderStrokeStyle style) {
        List<Double> list = style == null ? null : style.getDashArray();
        if (list == null || list.isEmpty()) {
            return null;
        }
        double[] out = new double[list.size()];
        for (int i = 0; i < out.length; i++) {
            Double d = list.get(i);
            out[i] = d == null ? 0 : d.doubleValue();
        }
        return out;
    }

    private void paintStroke(Renderer renderer, BorderStroke s, double w, double h) {
        BorderWidths bw = s.getWidths();
        Insets in = s.getInsets();
        double x = in.getLeft();
        double y = in.getTop();
        double sw = w - in.getLeft() - in.getRight();
        double sh = h - in.getTop() - in.getBottom();
        if (sw <= 0 || sh <= 0) {
            return;
        }
        double t = bw.getTop();
        boolean uniformWidth = t == bw.getRight() && t == bw.getBottom() && t == bw.getLeft();
        BorderStrokeStyle style = s.getTopStyle();
        if (uniformWidth && s.isStrokeUniform()) {
            if (t <= 0 || s.getTopStroke() == null || style == BorderStrokeStyle.NONE) {
                return;
            }
            CornerRadii r = s.getRadii() == null ? CornerRadii.EMPTY : s.getRadii();
            FxPath path = outline(r, x + t / 2, y + t / 2, sw - t, sh - t, t / 2);
            renderer.stroke(path, s.getTopStroke(), t, style.getLineCap(), style.getLineJoin(),
                    style.getMiterLimit(), dashes(style), style.getDashOffset());
            return;
        }
        if (bw.getTop() > 0 && s.getTopStroke() != null && s.getTopStyle() != BorderStrokeStyle.NONE) {
            renderer.fillRect(x, y, sw, bw.getTop(), s.getTopStroke());
        }
        if (bw.getBottom() > 0 && s.getBottomStroke() != null && s.getBottomStyle() != BorderStrokeStyle.NONE) {
            renderer.fillRect(x, y + sh - bw.getBottom(), sw, bw.getBottom(), s.getBottomStroke());
        }
        if (bw.getLeft() > 0 && s.getLeftStroke() != null && s.getLeftStyle() != BorderStrokeStyle.NONE) {
            renderer.fillRect(x, y, bw.getLeft(), sh, s.getLeftStroke());
        }
        if (bw.getRight() > 0 && s.getRightStroke() != null && s.getRightStyle() != BorderStrokeStyle.NONE) {
            renderer.fillRect(x + sw - bw.getRight(), y, bw.getRight(), sh, s.getRightStroke());
        }
    }
}
