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
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

/// A value picked from a range by moving a thumb, shown as an editable
/// Codename One `Slider`.
///
/// The value is kept inside `min` and `max`: setting it beyond either
/// end stores the end, and moving an end past the value moves the value.
/// The native slider counts in whole steps, so the range is mapped onto
/// 10000 of them; a value the user drags to is one of those.
///
/// The native slider draws no ticks. Tick marks and tick labels are drawn
/// by this control in a band of its own, below a horizontal slider and to
/// the right of a vertical one, with the label formatter's text when
/// there is one. `valueChanging` is true while the user
/// drags the slider and false again when the pointer is let go of.
/// `snapToTicks`,
/// `majorTickUnit` and `minorTickCount` do take part in
/// [#adjustValue(double)], [#increment()] and [#decrement()]. The
/// pseudo-classes `horizontal` and `vertical` follow the orientation.
public class Slider extends Control {

    private static final int MIN = Dirty.USER;
    private static final int MAX = Dirty.USER << 1;
    private static final int VALUE = Dirty.USER << 2;
    private static final int ORIENTATION = Dirty.USER << 3;
    private static final int STEPS = 10000;
    private static final PseudoClass HORIZONTAL = PseudoClass.getPseudoClass("horizontal");
    private static final PseudoClass VERTICAL = PseudoClass.getPseudoClass("vertical");

    private final DoubleProperty min = new FxDouble(this, "min", 0, MIN | Dirty.NATIVE);
    private final DoubleProperty max = new FxDouble(this, "max", 100, MAX | Dirty.NATIVE);
    private final DoubleProperty value = new FxDouble(this, "value", 0, VALUE | Dirty.NATIVE);
    private final ObjectProperty<Orientation> orientation = new FxObject<Orientation>(this, "orientation",
            Orientation.HORIZONTAL, ORIENTATION | Dirty.NATIVE | Dirty.LAYOUT);
    private final BooleanProperty valueChanging = new SimpleBooleanProperty(this, "valueChanging", false);
    private static final int TICKS = Dirty.PAINT | Dirty.LAYOUT;
    private static final double MAJOR_TICK = 6;
    private static final double MINOR_TICK = 3;
    private static final double TICK_GAP = 2;
    private static final int MAX_TICKS = 1000;

    private final BooleanProperty showTickLabels = new FxBoolean(this, "showTickLabels", false, TICKS);
    private final BooleanProperty showTickMarks = new FxBoolean(this, "showTickMarks", false, TICKS);
    private final DoubleProperty majorTickUnit = new FxDouble(this, "majorTickUnit", 25, TICKS);
    private final IntegerProperty minorTickCount = new SimpleIntegerProperty(this, "minorTickCount", 3);
    private final BooleanProperty snapToTicks = new SimpleBooleanProperty(this, "snapToTicks", false);
    private final DoubleProperty blockIncrement = new SimpleDoubleProperty(this, "blockIncrement", 10);
    private final ObjectProperty<StringConverter<Double>> labelFormatter =
            new FxObject<StringConverter<Double>>(this, "labelFormatter", null, TICKS);
    private boolean clamping;
    private boolean syncing;

    /// Creates a slider from 0 to 100 at 0.
    public Slider() {
        init();
    }

    /// Creates a slider with a range and a value.
    public Slider(double min, double max, double value) {
        init();
        setMax(max);
        setMin(min);
        setValue(value);
    }

    private void init() {
        getStyleClass().add("slider");
        // The value is changing from the moment a press on the slider turns
        // into a drag until the pointer is let go of.
        addEventHandler(javafx.scene.input.MouseEvent.DRAG_DETECTED,
                new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
                    @Override
                    public void handle(javafx.scene.input.MouseEvent event) {
                        if (!isDisabled()) {
                            setValueChanging(true);
                        }
                    }
                });
        addEventHandler(javafx.scene.input.MouseEvent.MOUSE_RELEASED,
                new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
                    @Override
                    public void handle(javafx.scene.input.MouseEvent event) {
                        if (isValueChanging()) {
                            setValueChanging(false);
                        }
                    }
                });
        minorTickCount.addListener(new javafx.beans.InvalidationListener() {
            @Override
            public void invalidated(javafx.beans.Observable observable) {
                cn1Repaint();
            }
        });
        pseudoClassStateChanged(HORIZONTAL, true);
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.Slider s = new com.codename1.ui.Slider();
        s.setMinValue(0);
        s.setMaxValue(STEPS);
        s.setEditable(true);
        s.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                // The native slider reports the new position as the index,
                // in one case before it stores it.
                if (!syncing && !value.isBound()) {
                    double lo = getMin();
                    setValue(lo + (getMax() - lo) * index / STEPS);
                }
            }
        });
        return s;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (!(c instanceof com.codename1.ui.Slider)) {
            return;
        }
        com.codename1.ui.Slider s = (com.codename1.ui.Slider) c;
        double lo = getMin();
        double span = getMax() - lo;
        int position = span > 0 ? (int) Math.round((getValue() - lo) / span * STEPS) : 0;
        position = Math.max(0, Math.min(STEPS, position));
        double block = span > 0 ? getBlockIncrement() / span * STEPS : 0;
        syncing = true;
        try {
            s.setVertical(getOrientation() == Orientation.VERTICAL);
            s.setIncrements(Math.max(1, (int) Math.round(block)));
            if (s.getProgress() != position) {
                s.setProgress(position);
            }
        } finally {
            syncing = false;
        }
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & (MIN | MAX | VALUE)) != 0 && !clamping) {
            clamping = true;
            try {
                if ((what & MIN) != 0 && getMin() > getMax() && !max.isBound()) {
                    setMax(getMin());
                } else if ((what & MAX) != 0 && getMax() < getMin() && !min.isBound()) {
                    setMin(getMax());
                }
                if (!value.isBound()) {
                    double v = getValue();
                    if (v < getMin()) {
                        setValue(getMin());
                    } else if (v > getMax()) {
                        setValue(getMax());
                    }
                }
            } finally {
                clamping = false;
            }
        }
        if ((what & ORIENTATION) != 0) {
            boolean vertical = getOrientation() == Orientation.VERTICAL;
            pseudoClassStateChanged(VERTICAL, vertical);
            pseudoClassStateChanged(HORIZONTAL, !vertical);
            Component c = cn1NativeIfCreated();
            if (c != null) {
                c.setShouldCalcPreferredSize(true);
            }
        }
        super.cn1Invalidated(what);
    }

    // ------------------------------------------------------------ ticks

    /// The font of the tick labels: the default one, a little smaller.
    private static Font tickFont() {
        return Font.font(Font.getDefault().getSize() * 0.85);
    }

    /// What a tick label reads: the formatter's text, or the number with
    /// no fraction when it has none.
    private String tickText(double v) {
        StringConverter<Double> f = getLabelFormatter();
        if (f != null) {
            String s = f.toString(Double.valueOf(v));
            return s == null ? "" : s;
        }
        if (v == Math.floor(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        return String.valueOf(Math.round(v * 100) / 100.0);
    }

    /// How many major ticks the range holds, the one at `min` included;
    /// zero when the unit cannot be drawn.
    private int majorTicks() {
        double unit = getMajorTickUnit();
        double span = getMax() - getMin();
        if (!(unit > 0) || !(span > 0)) {
            return 0;
        }
        double n = Math.floor(span / unit + 1e-9) + 1;
        return n > MAX_TICKS ? 0 : (int) n;
    }

    /// The room the tick marks and the tick labels take: below the track
    /// of a horizontal slider, to the right of a vertical one.
    private double tickBand() {
        double band = isShowTickMarks() ? MAJOR_TICK + TICK_GAP : 0;
        if (!isShowTickLabels()) {
            return band;
        }
        Font font = tickFont();
        if (getOrientation() != Orientation.VERTICAL) {
            return band + Fonts.lineHeight(font) + TICK_GAP;
        }
        double widest = 0;
        int n = majorTicks();
        for (int i = 0; i < n; i++) {
            widest = Math.max(widest, Fonts.width(font, tickText(getMin() + i * getMajorTickUnit())));
        }
        return band + widest + TICK_GAP;
    }

    @Override
    protected double cn1NativeBand(boolean below) {
        if (below == (getOrientation() == Orientation.VERTICAL)) {
            return 0;
        }
        double band = isShowTickMarks() || isShowTickLabels() ? tickBand() : 0;
        return band > 0 ? band : 0;
    }

    /// Draws the tick marks and the tick labels the application asked
    /// for, in the band kept beside the native slider, which draws
    /// neither. A tick is where the thumb stands at that value: the
    /// native slider moves it over its width less its padding.
    @Override
    public void cn1Paint(Renderer renderer) {
        super.cn1Paint(renderer);
        boolean marks = isShowTickMarks();
        boolean labels = isShowTickLabels();
        int n = marks || labels ? majorTicks() : 0;
        if (n == 0) {
            return;
        }
        boolean vertical = getOrientation() == Orientation.VERTICAL;
        Insets in = getInsets();
        double band = tickBand();
        double lead = 0;
        double trail = 0;
        Component c = cn1NativeIfCreated();
        if (c != null) {
            Style s = c.getStyle();
            lead = Units.toLogical(vertical ? s.getPaddingTop() : s.getPaddingLeftNoRTL());
            trail = Units.toLogical(vertical ? s.getPaddingBottom() : s.getPaddingRightNoRTL());
        }
        // The line the ticks start from, and the run of the track along it.
        double from = vertical ? in.getTop() + lead : in.getLeft() + lead;
        double length = (vertical ? getHeight() - in.getBottom() : getWidth() - in.getRight()) - trail - from;
        double edge = vertical ? getWidth() - in.getRight() - band : getHeight() - in.getBottom() - band;
        if (!(length > 0)) {
            return;
        }
        int fg = UIManager.getInstance().getComponentStyle("Label").getFgColor();
        Color ink = Color.rgb((fg >> 16) & 0xff, (fg >> 8) & 0xff, fg & 0xff);
        Color line = Color.rgb((fg >> 16) & 0xff, (fg >> 8) & 0xff, fg & 0xff, 0.55);
        double unit = getMajorTickUnit();
        double span = getMax() - getMin();
        double t = Units.toLogical(1);
        Font font = tickFont();
        double lineHeight = Fonts.lineHeight(font);
        int minor = Math.max(0, getMinorTickCount());
        double step = unit / (minor + 1) / span * length;
        double lastEnd = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double v = getMin() + i * unit;
            double f = (v - getMin()) / span;
            double at = from + (vertical ? (1 - f) * length : f * length);
            if (marks) {
                tick(renderer, vertical, at, edge, MAJOR_TICK, t, line);
                // The marks between this one and the next, where they
                // can be told apart.
                for (int j = 1; j <= minor && step >= 2 && v + j * unit / (minor + 1) <= getMax() + 1e-9; j++) {
                    tick(renderer, vertical, at + (vertical ? -j * step : j * step), edge, MINOR_TICK, t, line);
                }
            }
            if (labels) {
                String text = tickText(v);
                double w = Fonts.width(font, text);
                double start = edge + (marks ? MAJOR_TICK + TICK_GAP : TICK_GAP);
                if (vertical) {
                    double y = Math.max(0, Math.min(getHeight() - lineHeight, at - lineHeight / 2));
                    // Upwards: a label is drawn when it ends above the one before.
                    if (i == 0 || y + lineHeight <= lastEnd) {
                        renderer.drawText(text, start, y, font, ink);
                        lastEnd = y;
                    }
                } else {
                    double x = Math.max(0, Math.min(getWidth() - w, at - w / 2));
                    if (x >= lastEnd) {
                        renderer.drawText(text, x, start, font, ink);
                        lastEnd = x + w + TICK_GAP;
                    }
                }
            }
        }
    }

    private static void tick(Renderer renderer, boolean vertical, double at, double edge, double length, double t,
            Color line) {
        double p = Units.snap(at - t / 2);
        if (vertical) {
            renderer.fillRect(edge, p, length, t, line);
        } else {
            renderer.fillRect(p, edge, t, length, line);
        }
    }

    /// The length a slider asks for is JavaFX's, 140, and not the platform
    /// slider's, which asks for most of a screen: a slider among other
    /// controls in a row would push them out of it.
    @Override
    protected double computePrefWidth(double height) {
        if (getOrientation() == Orientation.VERTICAL) {
            return super.computePrefWidth(height) + tickBand();
        }
        Insets in = getInsets();
        return in.getLeft() + 140 + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        if (getOrientation() != Orientation.VERTICAL) {
            return super.computePrefHeight(width) + tickBand();
        }
        Insets in = getInsets();
        return in.getTop() + 140 + in.getBottom();
    }

    @Override
    protected double computeMinWidth(double height) {
        return getOrientation() == Orientation.VERTICAL ? computePrefWidth(height) : Math.min(60, computePrefWidth(height));
    }

    @Override
    protected double computeMinHeight(double width) {
        return getOrientation() == Orientation.VERTICAL ? Math.min(60, computePrefHeight(width)) : computePrefHeight(width);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return getOrientation() == Orientation.VERTICAL ? computePrefWidth(height) : Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return getOrientation() == Orientation.VERTICAL ? Double.MAX_VALUE : computePrefHeight(width);
    }

    /// Moves the value to the given one, kept inside the range and, with
    /// `snapToTicks`, moved to the nearest tick. Does nothing while the
    /// range is empty.
    public void adjustValue(double newValue) {
        double lo = getMin();
        double hi = getMax();
        if (hi <= lo) {
            return;
        }
        double v = newValue < lo ? lo : (newValue > hi ? hi : newValue);
        setValue(snap(v));
    }

    /// Raises the value by the block increment.
    public void increment() {
        adjustValue(getValue() + getBlockIncrement());
    }

    /// Lowers the value by the block increment.
    public void decrement() {
        adjustValue(getValue() - getBlockIncrement());
    }

    private double snap(double v) {
        if (!isSnapToTicks()) {
            return v;
        }
        double spacing = getMajorTickUnit();
        int minor = getMinorTickCount();
        if (minor > 0) {
            spacing = spacing / (minor + 1);
        }
        if (!(spacing > 0)) {
            return v;
        }
        double lo = getMin();
        double hi = getMax();
        double before = Math.floor((v - lo) / spacing) * spacing + lo;
        double after = before + spacing;
        double snapped = v - before <= after - v ? before : after;
        return snapped < lo ? lo : (snapped > hi ? hi : snapped);
    }

    /// Returns the low end of the range.
    public final double getMin() {
        return min.get();
    }

    /// Sets the low end of the range.
    public final void setMin(double value) {
        min.set(value);
    }

    /// The low end of the range.
    public final DoubleProperty minProperty() {
        return min;
    }

    /// Returns the high end of the range.
    public final double getMax() {
        return max.get();
    }

    /// Sets the high end of the range.
    public final void setMax(double value) {
        max.set(value);
    }

    /// The high end of the range.
    public final DoubleProperty maxProperty() {
        return max;
    }

    /// Returns the value.
    public final double getValue() {
        return value.get();
    }

    /// Sets the value; one outside the range becomes the nearer end.
    public final void setValue(double value) {
        if (!this.value.isBound()) {
            this.value.set(value);
        }
    }

    /// The value.
    public final DoubleProperty valueProperty() {
        return value;
    }

    /// Returns whether the value is in the middle of being changed by
    /// the user. Nothing in this layer sets it.
    public final boolean isValueChanging() {
        return valueChanging.get();
    }

    /// Sets whether the value is in the middle of being changed.
    public final void setValueChanging(boolean value) {
        valueChanging.set(value);
    }

    /// Whether the value is in the middle of being changed.
    public final BooleanProperty valueChangingProperty() {
        return valueChanging;
    }

    /// Returns the direction of the slider.
    public final Orientation getOrientation() {
        Orientation o = orientation.get();
        return o == null ? Orientation.HORIZONTAL : o;
    }

    /// Sets the direction of the slider.
    public final void setOrientation(Orientation value) {
        orientation.set(value);
    }

    /// The direction of the slider.
    public final ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    /// Returns whether tick labels are asked for.
    public final boolean isShowTickLabels() {
        return showTickLabels.get();
    }

    /// Asks for tick labels.
    public final void setShowTickLabels(boolean value) {
        showTickLabels.set(value);
    }

    /// Whether tick labels are asked for.
    public final BooleanProperty showTickLabelsProperty() {
        return showTickLabels;
    }

    /// Returns whether tick marks are asked for.
    public final boolean isShowTickMarks() {
        return showTickMarks.get();
    }

    /// Asks for tick marks.
    public final void setShowTickMarks(boolean value) {
        showTickMarks.set(value);
    }

    /// Whether tick marks are asked for.
    public final BooleanProperty showTickMarksProperty() {
        return showTickMarks;
    }

    /// Returns the distance between major ticks.
    public final double getMajorTickUnit() {
        return majorTickUnit.get();
    }

    /// Sets the distance between major ticks.
    public final void setMajorTickUnit(double value) {
        majorTickUnit.set(value);
    }

    /// The distance between major ticks.
    public final DoubleProperty majorTickUnitProperty() {
        return majorTickUnit;
    }

    /// Returns the number of minor ticks between two major ones.
    public final int getMinorTickCount() {
        return minorTickCount.get();
    }

    /// Sets the number of minor ticks between two major ones.
    public final void setMinorTickCount(int value) {
        minorTickCount.set(value);
    }

    /// The number of minor ticks between two major ones.
    public final IntegerProperty minorTickCountProperty() {
        return minorTickCount;
    }

    /// Returns whether adjusting the value moves it to the nearest tick.
    public final boolean isSnapToTicks() {
        return snapToTicks.get();
    }

    /// Sets whether adjusting the value moves it to the nearest tick.
    public final void setSnapToTicks(boolean value) {
        snapToTicks.set(value);
    }

    /// Whether adjusting the value moves it to the nearest tick.
    public final BooleanProperty snapToTicksProperty() {
        return snapToTicks;
    }

    /// Returns the step of [#increment()] and [#decrement()].
    public final double getBlockIncrement() {
        return blockIncrement.get();
    }

    /// Sets the step of [#increment()] and [#decrement()].
    public final void setBlockIncrement(double value) {
        blockIncrement.set(value);
    }

    /// The step of [#increment()] and [#decrement()].
    public final DoubleProperty blockIncrementProperty() {
        return blockIncrement;
    }

    /// Returns the converter for tick labels.
    public final StringConverter<Double> getLabelFormatter() {
        return labelFormatter.get();
    }

    /// Sets the converter for tick labels.
    public final void setLabelFormatter(StringConverter<Double> value) {
        labelFormatter.set(value);
    }

    /// The converter for tick labels.
    public final ObjectProperty<StringConverter<Double>> labelFormatterProperty() {
        return labelFormatter;
    }
}
