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

import com.codename1.flutter.rendering.Size;

/**
 * A rectangle described as insets from the four edges of a containing box —
 * Flutter's {@code RelativeRect} (used by Positioned/Stack and the
 * RelativeRectTween).
 */
public final class RelativeRect {

    public static final RelativeRect fill = new RelativeRect(0, 0, 0, 0);

    private final double left;
    private final double top;
    private final double right;
    private final double bottom;

    private RelativeRect(double left, double top, double right, double bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static RelativeRect fromLTRB(double left, double top, double right, double bottom) {
        return new RelativeRect(left, top, right, bottom);
    }

    public static RelativeRect fromRect(Rect rect, Rect container) {
        return fromLTRB(
                rect.left() - container.left(),
                rect.top() - container.top(),
                container.right() - rect.right(),
                container.bottom() - rect.bottom());
    }

    public static RelativeRect fromSize(Rect rect, Size container) {
        return fromLTRB(
                rect.left(),
                rect.top(),
                container.width() - rect.right(),
                container.height() - rect.bottom());
    }

    public double left() {
        return left;
    }

    public double top() {
        return top;
    }

    public double right() {
        return right;
    }

    public double bottom() {
        return bottom;
    }

    /**
     * Interpolates between two rects -- Flutter's {@code RelativeRect.lerp}.
     *
     * <p>A null end is treated as the other end, which is what Flutter does, so an
     * animation with only one side configured still runs.</p>
     */
    public static RelativeRect lerp(RelativeRect a, RelativeRect b, double t) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return fromLTRB(b.left * t, b.top * t, b.right * t, b.bottom * t);
        }
        if (b == null) {
            double k = 1.0 - t;
            return fromLTRB(a.left * k, a.top * k, a.right * k, a.bottom * k);
        }
        return fromLTRB(a.left + (b.left - a.left) * t,
                a.top + (b.top - a.top) * t,
                a.right + (b.right - a.right) * t,
                a.bottom + (b.bottom - a.bottom) * t);
    }

    public Rect toRect(Rect container) {
        return Rect.fromLTRB(
                left + container.left(),
                top + container.top(),
                container.right() - right,
                container.bottom() - bottom);
    }

    @Override
    public String toString() {
        return "RelativeRect.fromLTRB(" + left + ", " + top + ", " + right + ", " + bottom + ")";
    }
}
