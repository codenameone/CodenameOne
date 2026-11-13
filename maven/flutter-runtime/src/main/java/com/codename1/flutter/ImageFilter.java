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
 * dart:ui's {@code ImageFilter}. Only {@code blur} is rendered: a Gaussian blur with the
 * given sigmas, applied through Codename One's {@code Display.gaussianBlurImage}.
 */
public class ImageFilter {
    private final double sigmaX;
    private final double sigmaY;

    ImageFilter(double sigmaX, double sigmaY) {
        this.sigmaX = sigmaX;
        this.sigmaY = sigmaY;
    }

    public static ImageFilter blur(Double sigmaX, Double sigmaY, TileMode tileMode) {
        return new ImageFilter(sigmaX == null ? 0 : sigmaX.doubleValue(),
                sigmaY == null ? 0 : sigmaY.doubleValue());
    }

    public double sigmaX() {
        return sigmaX;
    }

    public double sigmaY() {
        return sigmaY;
    }

    /** The blur radius in device pixels: Codename One blurs with one radius for both axes. */
    public float deviceRadius(double dpr) {
        return (float) (Math.max(sigmaX, sigmaY) * dpr);
    }
}
