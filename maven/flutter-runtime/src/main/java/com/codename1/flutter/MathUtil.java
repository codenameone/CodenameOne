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

/**
 * Small math helpers from dart:ui that new_gallery reaches by bare name.
 * Currently only {@code lerpDouble}, used by the cut-corners input border to
 * animate its notch.
 */
public final class MathUtil {

    private MathUtil() {
    }

    /**
     * dart:ui's top-level {@code lerpDouble(a, b, t)}: linearly interpolate
     * between two nullable numbers. Returns null when both {@code a} and
     * {@code b} are null; treats a lone null endpoint as 0. {@code a}/{@code b}
     * are declared {@code Object} to mirror the {@code num?} stub (they arrive
     * as boxed {@link Double}/{@link Long}).
     */
    public static Double lerpDouble(Object a, Object b, double t) {
        if (a == null && b == null) {
            return null;
        }
        double da = toDouble(a);
        double db = toDouble(b);
        return da + (db - da) * t;
    }

    private static double toDouble(Object n) {
        if (n instanceof Number) {
            return ((Number) n).doubleValue();
        }
        return 0.0;
    }
}
