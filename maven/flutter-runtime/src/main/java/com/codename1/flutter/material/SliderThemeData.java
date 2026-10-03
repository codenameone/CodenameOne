/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.Color;
import com.codename1.flutter.TextStyle;

/**
 * The visual configuration of a {@link Slider} / {@link RangeSlider} —
 * Flutter's {@code SliderThemeData}. new_gallery's sliders demo derives a custom
 * theme via {@code theme.sliderTheme.copyWith(...)} and reads back
 * {@link #thumbColor()} / {@link #disabledThumbColor()} /
 * {@link #valueIndicatorColor()}.
 *
 * <p>Shape parameters (thumb/track/tick-mark/value-indicator shapes) are held
 * opaquely as {@code Object}; their {@code SliderComponentShape} /
 * {@code RangeSliderThumbShape} base types are owned by the widget-extension
 * category. Named constructor parameters map to same-named setters;
 * {@link #copyWith} takes every parameter positionally in declaration order.</p>
 */
public class SliderThemeData {

    private Double trackHeight;
    private Color activeTrackColor;
    private Color inactiveTrackColor;
    private Color disabledActiveTrackColor;
    private Color disabledInactiveTrackColor;
    private Color activeTickMarkColor;
    private Color inactiveTickMarkColor;
    private Color disabledActiveTickMarkColor;
    private Color disabledInactiveTickMarkColor;
    private Color thumbColor;
    private Color disabledThumbColor;
    private Color overlayColor;
    private Color valueIndicatorColor;
    private Object overlayShape;
    private Object tickMarkShape;
    private Object thumbShape;
    private Object trackShape;
    private Object valueIndicatorShape;
    private Object rangeThumbShape;
    private Object rangeTrackShape;
    private Object rangeTickMarkShape;
    private Object rangeValueIndicatorShape;
    private ShowValueIndicator showValueIndicator;
    private TextStyle valueIndicatorTextStyle;

    public SliderThemeData() {
    }

    // ------------------------------------------------------------------
    // Named-parameter setters
    // ------------------------------------------------------------------

    public void trackHeight(double v) { this.trackHeight = v; }
    public void activeTrackColor(Color v) { this.activeTrackColor = v; }
    public void inactiveTrackColor(Color v) { this.inactiveTrackColor = v; }
    public void disabledActiveTrackColor(Color v) { this.disabledActiveTrackColor = v; }
    public void disabledInactiveTrackColor(Color v) { this.disabledInactiveTrackColor = v; }
    public void activeTickMarkColor(Color v) { this.activeTickMarkColor = v; }
    public void inactiveTickMarkColor(Color v) { this.inactiveTickMarkColor = v; }
    public void disabledActiveTickMarkColor(Color v) { this.disabledActiveTickMarkColor = v; }
    public void disabledInactiveTickMarkColor(Color v) { this.disabledInactiveTickMarkColor = v; }
    public void thumbColor(Color v) { this.thumbColor = v; }
    public void disabledThumbColor(Color v) { this.disabledThumbColor = v; }
    public void overlayColor(Color v) { this.overlayColor = v; }
    public void valueIndicatorColor(Color v) { this.valueIndicatorColor = v; }
    public void overlayShape(Object v) { this.overlayShape = v; }
    public void tickMarkShape(Object v) { this.tickMarkShape = v; }
    public void thumbShape(Object v) { this.thumbShape = v; }
    public void trackShape(Object v) { this.trackShape = v; }
    public void valueIndicatorShape(Object v) { this.valueIndicatorShape = v; }
    public void rangeThumbShape(Object v) { this.rangeThumbShape = v; }
    public void rangeTrackShape(Object v) { this.rangeTrackShape = v; }
    public void rangeTickMarkShape(Object v) { this.rangeTickMarkShape = v; }
    public void rangeValueIndicatorShape(Object v) { this.rangeValueIndicatorShape = v; }
    public void showValueIndicator(ShowValueIndicator v) { this.showValueIndicator = v; }
    public void valueIndicatorTextStyle(TextStyle v) { this.valueIndicatorTextStyle = v; }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public Double trackHeight() { return trackHeight; }
    public Color activeTrackColor() { return activeTrackColor; }
    public Color inactiveTrackColor() { return inactiveTrackColor; }
    public Color activeTickMarkColor() { return activeTickMarkColor; }
    public Color inactiveTickMarkColor() { return inactiveTickMarkColor; }
    public Color thumbColor() { return thumbColor; }
    public Color disabledThumbColor() { return disabledThumbColor; }
    public Color overlayColor() { return overlayColor; }
    public Color valueIndicatorColor() { return valueIndicatorColor; }
    public ShowValueIndicator showValueIndicator() { return showValueIndicator; }
    public TextStyle valueIndicatorTextStyle() { return valueIndicatorTextStyle; }

    /**
     * Returns a copy with the supplied fields overridden (null keeps current).
     * Parameters are positional in Dart declaration order.
     */
    public SliderThemeData copyWith(
            Double trackHeight, Color activeTrackColor, Color inactiveTrackColor,
            Color disabledActiveTrackColor, Color disabledInactiveTrackColor,
            Color activeTickMarkColor, Color inactiveTickMarkColor,
            Color disabledActiveTickMarkColor, Color disabledInactiveTickMarkColor,
            Color thumbColor, Color disabledThumbColor, Color overlayColor,
            Color valueIndicatorColor,
            Object overlayShape, Object tickMarkShape, Object thumbShape,
            Object trackShape, Object valueIndicatorShape, Object rangeThumbShape,
            Object rangeTrackShape, Object rangeTickMarkShape, Object rangeValueIndicatorShape,
            ShowValueIndicator showValueIndicator, TextStyle valueIndicatorTextStyle) {
        SliderThemeData c = new SliderThemeData();
        c.trackHeight = trackHeight != null ? trackHeight : this.trackHeight;
        c.activeTrackColor = activeTrackColor != null ? activeTrackColor : this.activeTrackColor;
        c.inactiveTrackColor = inactiveTrackColor != null ? inactiveTrackColor : this.inactiveTrackColor;
        c.disabledActiveTrackColor = disabledActiveTrackColor != null ? disabledActiveTrackColor : this.disabledActiveTrackColor;
        c.disabledInactiveTrackColor = disabledInactiveTrackColor != null ? disabledInactiveTrackColor : this.disabledInactiveTrackColor;
        c.activeTickMarkColor = activeTickMarkColor != null ? activeTickMarkColor : this.activeTickMarkColor;
        c.inactiveTickMarkColor = inactiveTickMarkColor != null ? inactiveTickMarkColor : this.inactiveTickMarkColor;
        c.disabledActiveTickMarkColor = disabledActiveTickMarkColor != null ? disabledActiveTickMarkColor : this.disabledActiveTickMarkColor;
        c.disabledInactiveTickMarkColor = disabledInactiveTickMarkColor != null ? disabledInactiveTickMarkColor : this.disabledInactiveTickMarkColor;
        c.thumbColor = thumbColor != null ? thumbColor : this.thumbColor;
        c.disabledThumbColor = disabledThumbColor != null ? disabledThumbColor : this.disabledThumbColor;
        c.overlayColor = overlayColor != null ? overlayColor : this.overlayColor;
        c.valueIndicatorColor = valueIndicatorColor != null ? valueIndicatorColor : this.valueIndicatorColor;
        c.overlayShape = overlayShape != null ? overlayShape : this.overlayShape;
        c.tickMarkShape = tickMarkShape != null ? tickMarkShape : this.tickMarkShape;
        c.thumbShape = thumbShape != null ? thumbShape : this.thumbShape;
        c.trackShape = trackShape != null ? trackShape : this.trackShape;
        c.valueIndicatorShape = valueIndicatorShape != null ? valueIndicatorShape : this.valueIndicatorShape;
        c.rangeThumbShape = rangeThumbShape != null ? rangeThumbShape : this.rangeThumbShape;
        c.rangeTrackShape = rangeTrackShape != null ? rangeTrackShape : this.rangeTrackShape;
        c.rangeTickMarkShape = rangeTickMarkShape != null ? rangeTickMarkShape : this.rangeTickMarkShape;
        c.rangeValueIndicatorShape = rangeValueIndicatorShape != null ? rangeValueIndicatorShape : this.rangeValueIndicatorShape;
        c.showValueIndicator = showValueIndicator != null ? showValueIndicator : this.showValueIndicator;
        c.valueIndicatorTextStyle = valueIndicatorTextStyle != null ? valueIndicatorTextStyle : this.valueIndicatorTextStyle;
        return c;
    }
}
