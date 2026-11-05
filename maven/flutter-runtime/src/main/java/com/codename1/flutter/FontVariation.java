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
 * Flutter's {@code FontVariation}: one axis setting of a variable font, such as weight
 * ({@code wght}) or optical size ({@code opsz}). Text styles keep the variations they are
 * given; the platform fonts the runtime draws with are not variable, so only the
 * {@code wght} axis changes what is drawn, through the style's weight.
 */
public final class FontVariation {
    private final String axis;
    private final double value;

    public FontVariation(String axis, double value) {
        this.axis = axis;
        this.value = value;
    }

    public static FontVariation weight(double v) {
        return new FontVariation("wght", v);
    }

    public static FontVariation width(double v) {
        return new FontVariation("wdth", v);
    }

    public static FontVariation slant(double v) {
        return new FontVariation("slnt", v);
    }

    public static FontVariation opticalSize(double v) {
        return new FontVariation("opsz", v);
    }

    public static FontVariation italic(double v) {
        return new FontVariation("ital", v);
    }

    public String axis() {
        return axis;
    }

    public double value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FontVariation)) {
            return false;
        }
        FontVariation f = (FontVariation) o;
        return f.axis.equals(axis) && f.value == value;
    }

    @Override
    public int hashCode() {
        return axis.hashCode() * 31 + (int) Double.doubleToLongBits(value);
    }

    @Override
    public String toString() {
        return "FontVariation('" + axis + "', " + value + ")";
    }
}
