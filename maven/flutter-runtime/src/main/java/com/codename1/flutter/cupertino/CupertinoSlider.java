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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Slider;

import dart.runtime.Funcs;

/**
 * An iOS-style slider — Flutter's {@code CupertinoSlider}. Same controlled
 * double-range semantics as the material slider; composed onto material
 * {@link Slider} (visually approximate this pass).
 */
public class CupertinoSlider extends StatelessWidget {

    private double value;
    private Double min;
    private Double max;
    private Long divisions;
    private Funcs.VoidFunc1<Double> onChanged;

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

    public void onChangeStart(Object v) {
    }

    public void onChangeEnd(Object v) {
    }

    public void activeColor(Color v) {
    }

    public void thumbColor(Color v) {
    }

    @Override
    public Widget build(BuildContext context) {
        Slider s = new Slider();
        s.value(value);
        if (min != null) {
            s.min(min);
        }
        if (max != null) {
            s.max(max);
        }
        if (divisions != null) {
            s.divisions(divisions);
        }
        s.onChanged(onChanged);
        return s;
    }
}
