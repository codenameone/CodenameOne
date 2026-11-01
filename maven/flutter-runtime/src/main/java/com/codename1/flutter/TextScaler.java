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
package com.codename1.flutter;

/** Flutter's {@code TextScaler}: how font sizes are scaled for accessibility. */
public final class TextScaler {

    public static final TextScaler noScaling = new TextScaler(1.0);

    private final double factor;

    private TextScaler(double factor) {
        this.factor = factor;
    }

    /** Flutter's {@code TextScaler.linear}. */
    public static TextScaler linear(double textScaleFactor) {
        return new TextScaler(textScaleFactor);
    }

    /** Flutter's {@code scale}: the scaled font size. */
    public double scale(double fontSize) {
        return fontSize * factor;
    }

    /** Flutter's (deprecated, still common) {@code textScaleFactor}. */
    public double textScaleFactor() {
        return factor;
    }

    /** Flutter's {@code clamp}: this scaler with its factor kept within the bounds. */
    public TextScaler clamp(Double minScaleFactor, Double maxScaleFactor) {
        double lo = minScaleFactor == null ? 0 : minScaleFactor.doubleValue();
        double hi = maxScaleFactor == null ? Double.POSITIVE_INFINITY : maxScaleFactor.doubleValue();
        return new TextScaler(Math.max(lo, Math.min(hi, factor)));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TextScaler && ((TextScaler) o).factor == factor;
    }

    @Override
    public int hashCode() {
        long b = Double.doubleToLongBits(factor);
        return (int) (b ^ (b >>> 32));
    }
}
