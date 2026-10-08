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
package android.animation;

import com.codename1.util.MathUtil;

/// Interpolates between two ARGB colors in linear light, as Android does,
/// so a blend does not darken in the middle.
public class ArgbEvaluator implements TypeEvaluator {

    private static final ArgbEvaluator INSTANCE = new ArgbEvaluator();

    public static ArgbEvaluator getInstance() {
        return INSTANCE;
    }

    @Override
    public Object evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = ((Number) startValue).intValue();
        int endInt = ((Number) endValue).intValue();
        return Integer.valueOf(evaluate(fraction, startInt, endInt));
    }

    static int evaluate(float fraction, int startInt, int endInt) {
        float startA = ((startInt >> 24) & 0xff) / 255.0f;
        float startR = ((startInt >> 16) & 0xff) / 255.0f;
        float startG = ((startInt >> 8) & 0xff) / 255.0f;
        float startB = (startInt & 0xff) / 255.0f;
        float endA = ((endInt >> 24) & 0xff) / 255.0f;
        float endR = ((endInt >> 16) & 0xff) / 255.0f;
        float endG = ((endInt >> 8) & 0xff) / 255.0f;
        float endB = (endInt & 0xff) / 255.0f;
        startR = (float) MathUtil.pow(startR, 2.2);
        startG = (float) MathUtil.pow(startG, 2.2);
        startB = (float) MathUtil.pow(startB, 2.2);
        endR = (float) MathUtil.pow(endR, 2.2);
        endG = (float) MathUtil.pow(endG, 2.2);
        endB = (float) MathUtil.pow(endB, 2.2);
        float a = startA + fraction * (endA - startA);
        float r = startR + fraction * (endR - startR);
        float g = startG + fraction * (endG - startG);
        float b = startB + fraction * (endB - startB);
        a = a * 255.0f;
        r = (float) MathUtil.pow(r, 1.0 / 2.2) * 255.0f;
        g = (float) MathUtil.pow(g, 1.0 / 2.2) * 255.0f;
        b = (float) MathUtil.pow(b, 1.0 / 2.2) * 255.0f;
        return Math.round(a) << 24 | Math.round(r) << 16 | Math.round(g) << 8 | Math.round(b);
    }
}
