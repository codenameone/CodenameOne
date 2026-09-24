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

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A material slider over a double range with CONTROLLED semantics: drags
 * fire {@code onChanged(newValue)} and the thumb snaps back to the widget's
 * configured {@code value} until a rebuild moves it. Backed by a CN1
 * {@link com.codename1.ui.Slider} (UIID "FlutterSlider") whose int progress
 * model the double range is scaled onto ({@code divisions} steps when given,
 * a fine-grained default otherwise).
 */
public class Slider extends Widget {

    private double value;
    private Double min;
    private Double max;
    private Long divisions;
    private Funcs.VoidFunc1<Double> onChanged;
    private String label;
    private Funcs.Func1<Double, String> semanticFormatterCallback;
    private com.codename1.flutter.Color activeColor;
    private com.codename1.flutter.Color inactiveColor;
    private Funcs.VoidFunc1<Double> onChangeStart;
    private Funcs.VoidFunc1<Double> onChangeEnd;

    public void label(String v) {
        this.label = v;
    }

    public void semanticFormatterCallback(Funcs.Func1<Double, String> v) {
        this.semanticFormatterCallback = v;
    }

    public void activeColor(com.codename1.flutter.Color v) {
        this.activeColor = v;
    }

    public void inactiveColor(com.codename1.flutter.Color v) {
        this.inactiveColor = v;
    }

    public void onChangeStart(Funcs.VoidFunc1<Double> v) {
        this.onChangeStart = v;
    }

    public void onChangeEnd(Funcs.VoidFunc1<Double> v) {
        this.onChangeEnd = v;
    }

    public void value(double v) {
        this.value = v;
    }

    public void min(double v) {
        this.min = v;
    }

    public void max(double v) {
        this.max = v;
    }

    public void divisions(long v) {
        this.divisions = v;
    }

    public void onChanged(Funcs.VoidFunc1<Double> v) {
        this.onChanged = v;
    }

    public double getValue() {
        return value;
    }

    /** Flutter default: 0.0. */
    public double getMin() {
        return min == null ? 0.0 : min;
    }

    /** Flutter default: 1.0. */
    public double getMax() {
        return max == null ? 1.0 : max;
    }

    public Long getDivisions() {
        return divisions;
    }

    public Funcs.VoidFunc1<Double> getOnChanged() {
        return onChanged;
    }

    public Funcs.VoidFunc1<Double> getOnChangeStart() {
        return onChangeStart;
    }

    public Funcs.VoidFunc1<Double> getOnChangeEnd() {
        return onChangeEnd;
    }

    @Override
    public Element createElement() {
        return new SliderRenderElement(this);
    }
}
