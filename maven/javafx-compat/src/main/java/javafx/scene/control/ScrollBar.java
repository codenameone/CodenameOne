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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.ui.Component;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/// A bar with a thumb that is dragged along a track to choose a value
/// between a minimum and a maximum.
///
/// The bar is two regions a style sheet reaches as `.track` and `.thumb`.
/// The length of the thumb is the visible amount as a part of the range,
/// and never less than the bar is thick. Dragging the thumb moves the
/// value; a press on the track beside it moves the value by the block
/// increment towards the press. There are no arrow buttons at the ends.
///
/// The preferred size is 14 by 100 logical pixels, the long side along
/// the orientation.
public class ScrollBar extends Control {

    private static final int VALUE = Dirty.USER;
    private static final int ORIENTATION = Dirty.USER << 1;
    private static final double THICKNESS = 14;
    private static final double LENGTH = 100;
    private static final PseudoClass HORIZONTAL = PseudoClass.getPseudoClass("horizontal");
    private static final PseudoClass VERTICAL = PseudoClass.getPseudoClass("vertical");

    private final DoubleProperty min = new FxDouble(this, "min", 0, VALUE | Dirty.LAYOUT);
    private final DoubleProperty max = new FxDouble(this, "max", 100, VALUE | Dirty.LAYOUT);
    private final DoubleProperty value = new FxDouble(this, "value", 0, VALUE | Dirty.LAYOUT);
    private final DoubleProperty visibleAmount = new FxDouble(this, "visibleAmount", 15, VALUE | Dirty.LAYOUT);
    private final DoubleProperty unitIncrement = new FxDouble(this, "unitIncrement", 1, 0);
    private final DoubleProperty blockIncrement = new FxDouble(this, "blockIncrement", 10, 0);
    private final ObjectProperty<Orientation> orientation = new FxObject<Orientation>(this, "orientation",
            Orientation.HORIZONTAL, ORIENTATION | Dirty.LAYOUT);
    private final Region track = new Part("track",
            "-fx-background-color: derive(-fx-base, -9%), derive(-fx-base, 12%); -fx-background-insets: 0, 1;");
    private final Region thumb = new Part("thumb",
            "-fx-background-color: derive(-fx-base, -30%), derive(-fx-base, -8%);"
                    + " -fx-background-insets: 2, 3; -fx-background-radius: 5, 4;");
    private double grabbed;

    private static final class Part extends Region {
        private final String look;

        Part(String styleClass, String look) {
            this.look = look;
            getStyleClass().add(styleClass);
            setManaged(false);
        }

        @Override
        public String cn1DefaultStyle() {
            return look;
        }
    }

    /// Creates a horizontal bar from 0 to 100.
    public ScrollBar() {
        getStyleClass().add("scroll-bar");
        setFocusTraversable(false);
        pseudoClassStateChanged(HORIZONTAL, true);
        cn1Children().add(track);
        cn1Children().add(thumb);
        track.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (!isDisabled()) {
                adjustValue(positionOf(e.getX(), e.getY()));
                e.consume();
            }
        });
        thumb.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            grabbed = upright() ? e.getY() : e.getX();
            e.consume();
        });
        thumb.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!isDisabled()) {
                dragTo((upright() ? thumb.getLayoutY() + e.getY() : thumb.getLayoutX() + e.getX()) - grabbed);
                e.consume();
            }
        });
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    private boolean upright() {
        return getOrientation() == Orientation.VERTICAL;
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & ORIENTATION) != 0) {
            pseudoClassStateChanged(VERTICAL, upright());
            pseudoClassStateChanged(HORIZONTAL, !upright());
        }
        super.cn1Invalidated(what);
    }

    private double range() {
        return Math.max(0, getMax() - getMin());
    }

    private static double clamp(double low, double v, double high) {
        return v < low ? low : (v > high ? high : v);
    }

    /// Returns `{start of the track, its length, length of the thumb}`
    /// along the bar, inside the insets.
    private double[] along() {
        Insets in = getInsets();
        boolean upright = upright();
        double start = upright ? in.getTop() : in.getLeft();
        double length = Math.max(0, upright ? getHeight() - in.getTop() - in.getBottom()
                : getWidth() - in.getLeft() - in.getRight());
        double across = Math.max(0, upright ? getWidth() - in.getLeft() - in.getRight()
                : getHeight() - in.getTop() - in.getBottom());
        double range = range();
        double part = range > 0 ? clamp(0, getVisibleAmount() / range, 1) : 1;
        double thumbLength = Math.min(length, Math.max(across, length * part));
        return new double[] {start, length, thumbLength};
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double w = Math.max(0, getWidth() - in.getLeft() - in.getRight());
        double h = Math.max(0, getHeight() - in.getTop() - in.getBottom());
        track.resizeRelocate(in.getLeft(), in.getTop(), w, h);
        double[] a = along();
        double range = range();
        double at = range > 0 ? clamp(0, (getValue() - getMin()) / range, 1) : 0;
        double offset = a[0] + (a[1] - a[2]) * at;
        if (upright()) {
            thumb.resizeRelocate(in.getLeft(), offset, w, a[2]);
        } else {
            thumb.resizeRelocate(offset, in.getTop(), a[2], h);
        }
    }

    /// The place of a point of the track along the bar, from 0 to 1.
    private double positionOf(double x, double y) {
        double[] a = along();
        return a[1] > 0 ? clamp(0, ((upright() ? y : x) - a[0]) / a[1], 1) : 0;
    }

    private void dragTo(double thumbStart) {
        double[] a = along();
        double room = a[1] - a[2];
        if (room > 0) {
            setValue(getMin() + range() * clamp(0, (thumbStart - a[0]) / room, 1));
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + (upright() ? THICKNESS : LENGTH) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + (upright() ? LENGTH : THICKNESS) + in.getBottom();
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + THICKNESS + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + THICKNESS + in.getBottom();
    }

    @Override
    protected double computeMaxWidth(double height) {
        return upright() ? computePrefWidth(height) : Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return upright() ? Double.MAX_VALUE : computePrefHeight(width);
    }

    /// Moves the value by the block increment towards a place along the
    /// bar, from 0 to 1, and not past it.
    public void adjustValue(double position) {
        double target = getMin() + range() * clamp(0, position, 1);
        double now = getValue();
        double block = getBlockIncrement();
        if (target > now) {
            setValue(clamp(getMin(), Math.min(target, now + block), getMax()));
        } else if (target < now) {
            setValue(clamp(getMin(), Math.max(target, now - block), getMax()));
        }
    }

    /// Adds the unit increment to the value, up to the maximum.
    public void increment() {
        setValue(clamp(getMin(), getValue() + getUnitIncrement(), getMax()));
    }

    /// Takes the unit increment off the value, down to the minimum.
    public void decrement() {
        setValue(clamp(getMin(), getValue() - getUnitIncrement(), getMax()));
    }

    /// Returns the least value.
    public final double getMin() {
        return min.get();
    }

    /// Sets the least value.
    public final void setMin(double v) {
        min.set(v);
    }

    /// The least value.
    public final DoubleProperty minProperty() {
        return min;
    }

    /// Returns the greatest value.
    public final double getMax() {
        return max.get();
    }

    /// Sets the greatest value.
    public final void setMax(double v) {
        max.set(v);
    }

    /// The greatest value.
    public final DoubleProperty maxProperty() {
        return max;
    }

    /// Returns the value, which the thumb shows.
    public final double getValue() {
        return value.get();
    }

    /// Sets the value. It is not held between the minimum and the
    /// maximum, as it is not in JavaFX; the thumb is.
    public final void setValue(double v) {
        value.set(v);
    }

    /// The value.
    public final DoubleProperty valueProperty() {
        return value;
    }

    /// Returns how much of the range is in view, which is the length of
    /// the thumb.
    public final double getVisibleAmount() {
        return visibleAmount.get();
    }

    /// Sets how much of the range is in view.
    public final void setVisibleAmount(double v) {
        visibleAmount.set(v);
    }

    /// How much of the range is in view.
    public final DoubleProperty visibleAmountProperty() {
        return visibleAmount;
    }

    /// Returns what [#increment()] and [#decrement()] move the value by.
    public final double getUnitIncrement() {
        return unitIncrement.get();
    }

    /// Sets what [#increment()] and [#decrement()] move the value by.
    public final void setUnitIncrement(double v) {
        unitIncrement.set(v);
    }

    /// The small step.
    public final DoubleProperty unitIncrementProperty() {
        return unitIncrement;
    }

    /// Returns what a press on the track moves the value by.
    public final double getBlockIncrement() {
        return blockIncrement.get();
    }

    /// Sets what a press on the track moves the value by.
    public final void setBlockIncrement(double v) {
        blockIncrement.set(v);
    }

    /// The large step.
    public final DoubleProperty blockIncrementProperty() {
        return blockIncrement;
    }

    /// Returns whether the bar lies or stands.
    public final Orientation getOrientation() {
        return orientation.get();
    }

    /// Makes the bar lie or stand.
    public final void setOrientation(Orientation v) {
        orientation.set(v == null ? Orientation.HORIZONTAL : v);
    }

    /// Whether the bar lies or stands.
    public final ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }
}
