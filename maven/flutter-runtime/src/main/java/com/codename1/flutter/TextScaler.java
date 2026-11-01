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
    /// The style a text with this scaler draws with: the font size (14 when the style
    /// names none, Flutter's default) scaled. Anything but a TextScaler, or no
    /// scaling, answers the style unchanged.
    public static TextStyle applyTo(Object scaler, TextStyle style) {
        if (!(scaler instanceof TextScaler) || ((TextScaler) scaler).textScaleFactor() == 1.0) {
            return style;
        }
        TextStyle base = style != null ? style : new TextStyle();
        double size = base.getFontSize() != null ? base.getFontSize().doubleValue() : 14.0;
        return base.copyWith(null, null, null, null, ((TextScaler) scaler).scale(size),
                null, null, null, null, null, null, null, null);
    }

    /// {@link #applyTo} for a text widget: its own scaler when it set one, else the
    /// ambient {@code MediaQuery.textScalerOf(context)}, as Flutter's Text/RichText do
    /// -- so {@code MediaQuery.withClampedTextScaling} reaches the text below it.
    public static TextStyle applyTo(Object scaler, TextStyle style, BuildContext context) {
        if (scaler == null && context != null) {
            scaler = MediaQuery.textScalerOf(context);
        }
        return applyTo(scaler, style);
    }

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
