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
package javafx.scene.chart;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxString;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Side;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

/// One axis of a chart: its label and how its ticks are shown.
///
/// An axis is drawn by the chart it belongs to and is not a child of it;
/// it is a region so that code and FXML that treat it as one compile,
/// and its own size and position mean nothing. The axis is drawn on its
/// `side` of the plot, with its tick marks and labels outside the plot,
/// and its tick labels turned by `tickLabelRotation` degrees.
public abstract class Axis<T> extends Region {

    private final StringProperty label = new FxString(this, "label", null, Dirty.PAINT);
    private final ObjectProperty<Side> side = new FxObject<Side>(this, "side", null, Dirty.PAINT);
    private final BooleanProperty autoRanging = new FxBoolean(this, "autoRanging", true, Dirty.PAINT);
    private final BooleanProperty tickLabelsVisible = new FxBoolean(this, "tickLabelsVisible", true, Dirty.PAINT);
    private final BooleanProperty tickMarkVisible = new FxBoolean(this, "tickMarkVisible", true, Dirty.PAINT);
    private final DoubleProperty tickLabelRotation = new FxDouble(this, "tickLabelRotation", 0, Dirty.PAINT);
    private final DoubleProperty tickLength = new FxDouble(this, "tickLength", 8, Dirty.PAINT);
    private final DoubleProperty tickLabelGap = new FxDouble(this, "tickLabelGap", 3, Dirty.PAINT);
    private final ObjectProperty<Font> tickLabelFont = new FxObject<Font>(this, "tickLabelFont", Font.font(10),
            Dirty.PAINT);
    private final ObjectProperty<Paint> tickLabelFill = new FxObject<Paint>(this, "tickLabelFill",
            Color.web("#707070"), Dirty.PAINT);
    private boolean animated = true;
    private Chart chart;

    /// Creates an axis.
    public Axis() {
    }

    /// The chart that draws this axis.
    final void attach(Chart owner) {
        chart = owner;
    }

    /// A change of the axis is a repaint of its chart.
    @Override
    public void cn1Invalidated(int what) {
        super.cn1Invalidated(what);
        if (chart != null && what != Dirty.NONE) {
            chart.cn1Repaint();
        }
    }

    /// Returns the label of the axis, `null` for none.
    public final String getLabel() {
        return label.get();
    }

    /// Sets the label drawn along the axis.
    public final void setLabel(String value) {
        label.set(value);
    }

    /// The label drawn along the axis.
    public final StringProperty labelProperty() {
        return label;
    }

    /// Returns the side of the plot the axis is drawn on.
    public final Side getSide() {
        return side.get();
    }

    /// Sets the side of the plot the axis is drawn on. A side that does
    /// not suit the direction of the axis - left or right for the X axis
    /// of a chart - is taken as bottom, or left for a Y axis.
    public final void setSide(Side value) {
        side.set(value);
    }

    /// The side of the plot the axis is drawn on.
    public final ObjectProperty<Side> sideProperty() {
        return side;
    }

    /// The side the axis is drawn on when it runs in the given direction.
    final Side side(boolean horizontal) {
        Side s = side.get();
        if (horizontal) {
            return s == Side.TOP ? Side.TOP : Side.BOTTOM;
        }
        return s == Side.RIGHT ? Side.RIGHT : Side.LEFT;
    }

    /// Returns whether the axis takes its range from the data.
    public final boolean isAutoRanging() {
        return autoRanging.get();
    }

    /// Sets whether the axis takes its range from the data.
    public final void setAutoRanging(boolean value) {
        autoRanging.set(value);
    }

    /// Whether the axis takes its range from the data.
    public final BooleanProperty autoRangingProperty() {
        return autoRanging;
    }

    /// Returns whether the labels of the ticks are drawn.
    public final boolean isTickLabelsVisible() {
        return tickLabelsVisible.get();
    }

    /// Sets whether the labels of the ticks are drawn.
    public final void setTickLabelsVisible(boolean value) {
        tickLabelsVisible.set(value);
    }

    /// Whether the labels of the ticks are drawn.
    public final BooleanProperty tickLabelsVisibleProperty() {
        return tickLabelsVisible;
    }

    /// Returns whether the tick marks are drawn.
    public final boolean isTickMarkVisible() {
        return tickMarkVisible.get();
    }

    /// Sets whether the tick marks are drawn.
    public final void setTickMarkVisible(boolean value) {
        tickMarkVisible.set(value);
    }

    /// Whether the tick marks are drawn.
    public final BooleanProperty tickMarkVisibleProperty() {
        return tickMarkVisible;
    }

    /// Returns the rotation of the tick labels, in degrees.
    public final double getTickLabelRotation() {
        return tickLabelRotation.get();
    }

    /// Sets the rotation of the tick labels, in degrees clockwise about
    /// the middle of each label.
    public final void setTickLabelRotation(double value) {
        tickLabelRotation.set(value);
    }

    /// The rotation of the tick labels, in degrees.
    public final DoubleProperty tickLabelRotationProperty() {
        return tickLabelRotation;
    }

    /// Returns the font of the tick labels.
    public final Font getTickLabelFont() {
        return tickLabelFont.get();
    }

    /// Sets the font of the tick labels.
    public final void setTickLabelFont(Font value) {
        tickLabelFont.set(value);
    }

    /// The font of the tick labels.
    public final ObjectProperty<Font> tickLabelFontProperty() {
        return tickLabelFont;
    }

    /// Returns the paint of the tick labels.
    public final Paint getTickLabelFill() {
        return tickLabelFill.get();
    }

    /// Sets the paint of the tick labels.
    public final void setTickLabelFill(Paint value) {
        tickLabelFill.set(value);
    }

    /// The paint of the tick labels.
    public final ObjectProperty<Paint> tickLabelFillProperty() {
        return tickLabelFill;
    }

    /// Returns the length of a tick mark.
    public final double getTickLength() {
        return tickLength.get();
    }

    /// Sets the length of a tick mark.
    public final void setTickLength(double value) {
        tickLength.set(value);
    }

    /// The length of a tick mark.
    public final DoubleProperty tickLengthProperty() {
        return tickLength;
    }

    /// Returns the gap between a tick mark and its label.
    public final double getTickLabelGap() {
        return tickLabelGap.get();
    }

    /// Sets the gap between a tick mark and its label.
    public final void setTickLabelGap(double value) {
        tickLabelGap.set(value);
    }

    /// The gap between a tick mark and its label.
    public final DoubleProperty tickLabelGapProperty() {
        return tickLabelGap;
    }

    /// Returns whether changes were asked to be animated.
    public final boolean getAnimated() {
        return animated;
    }

    /// Records whether changes are to be animated; they are drawn at once.
    public final void setAnimated(boolean value) {
        animated = value;
    }

    /// The words of a value on this axis, as a tick shows it.
    abstract String text(Object value);
}
