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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * A slider selecting a range between two thumbs — Flutter's {@code RangeSlider}.
 * This milestone renders a single {@link Slider} spanning the current range
 * (its thumb at {@code values.start}); the second thumb and range-drag gestures
 * are deferred. {@code onChanged} carries a {@link RangeValues}.
 */
public class RangeSlider extends StatelessWidget {

    private RangeValues values;
    private Double min;
    private Double max;
    private Long divisions;
    private RangeLabels labels;
    private Color activeColor;
    private Color inactiveColor;
    private Funcs.VoidFunc1<RangeValues> onChanged;
    private Funcs.VoidFunc1<RangeValues> onChangeStart;
    private Funcs.VoidFunc1<RangeValues> onChangeEnd;

    public void values(RangeValues v) {
        this.values = v;
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

    public void labels(RangeLabels v) {
        this.labels = v;
    }

    public void activeColor(Color v) {
        this.activeColor = v;
    }

    public void inactiveColor(Color v) {
        this.inactiveColor = v;
    }

    public void onChanged(Funcs.VoidFunc1<RangeValues> v) {
        this.onChanged = v;
    }

    public void onChangeStart(Funcs.VoidFunc1<RangeValues> v) {
        this.onChangeStart = v;
    }

    public void onChangeEnd(Funcs.VoidFunc1<RangeValues> v) {
        this.onChangeEnd = v;
    }

    public void semanticFormatterCallback(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        Slider s = new Slider();
        s.min(min == null ? 0.0 : min);
        s.max(max == null ? 1.0 : max);
        s.value(values != null ? values.start() : (min == null ? 0.0 : min));
        if (divisions != null) {
            s.divisions(divisions);
        }
        return s;
    }
}
