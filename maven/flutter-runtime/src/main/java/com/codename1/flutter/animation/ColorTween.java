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
package com.codename1.flutter.animation;

import com.codename1.flutter.Color;

/**
 * A {@link Tween} that interpolates ARGB {@link Color}s channel by channel —
 * Flutter's {@code ColorTween}.
 */
public class ColorTween extends Tween<Color> {

    @Override
    public Color lerp(double t) {
        Color b = begin();
        Color e = end();
        if (b == null && e == null) {
            return null;
        }
        if (b == null) {
            return scaleAlpha(e, t);
        }
        if (e == null) {
            return scaleAlpha(b, 1.0 - t);
        }
        int a = lerpChannel(b.alpha(), e.alpha(), t);
        int r = lerpChannel(b.red(), e.red(), t);
        int g = lerpChannel(b.green(), e.green(), t);
        int bl = lerpChannel(b.blue(), e.blue(), t);
        return new Color((a << 24) | (r << 16) | (g << 8) | bl);
    }

    private static Color scaleAlpha(Color c, double t) {
        int a = lerpChannel(0, c.alpha(), t);
        return new Color((a << 24) | (c.value() & 0xFFFFFFL));
    }

    private static int lerpChannel(int a, int b, double t) {
        int v = (int) Math.round(a + (b - a) * t);
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
