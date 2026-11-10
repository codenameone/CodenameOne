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
package com.codename1.flutter.services;

/// Flutter's SystemUiOverlayStyle: the colours and icon brightness of the system
/// bars. copyWith builds a new style with the given values replaced.
public final class SystemUiOverlayStyle {

    /// Light icons, for a dark app bar.
    public static final SystemUiOverlayStyle light = new SystemUiOverlayStyle("light",
            new Object[] {new com.codename1.flutter.Color(0xFF000000L), null, com.codename1.flutter.Brightness.light, null,
                null, com.codename1.flutter.Brightness.dark, com.codename1.flutter.Brightness.light, null});

    /// Dark icons, for a light app bar.
    public static final SystemUiOverlayStyle dark = new SystemUiOverlayStyle("dark",
            new Object[] {new com.codename1.flutter.Color(0xFF000000L), null, com.codename1.flutter.Brightness.light, null,
                null, com.codename1.flutter.Brightness.light, com.codename1.flutter.Brightness.dark, null});

    private final String name;
    private final Object[] values;

    private SystemUiOverlayStyle(String name, Object[] values) {
        this.name = name;
        this.values = values;
    }

    public SystemUiOverlayStyle copyWith(Object systemNavigationBarColor, Object systemNavigationBarDividerColor,
            Object systemNavigationBarIconBrightness, Object systemNavigationBarContrastEnforced,
            Object statusBarColor, Object statusBarBrightness, Object statusBarIconBrightness,
            Object systemStatusBarContrastEnforced) {
        Object[] given = {systemNavigationBarColor, systemNavigationBarDividerColor,
            systemNavigationBarIconBrightness, systemNavigationBarContrastEnforced, statusBarColor,
            statusBarBrightness, statusBarIconBrightness, systemStatusBarContrastEnforced};
        Object[] v = new Object[values.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = given[i] != null ? given[i] : values[i];
        }
        return new SystemUiOverlayStyle(name, v);
    }

    public Object systemNavigationBarColor() {
        return values[0];
    }

    public Object systemNavigationBarDividerColor() {
        return values[1];
    }

    public Object systemNavigationBarIconBrightness() {
        return values[2];
    }

    public Object systemNavigationBarContrastEnforced() {
        return values[3];
    }

    public Object statusBarColor() {
        return values[4];
    }

    public Object statusBarBrightness() {
        return values[5];
    }

    public Object statusBarIconBrightness() {
        return values[6];
    }

    public Object systemStatusBarContrastEnforced() {
        return values[7];
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SystemUiOverlayStyle)) {
            return false;
        }
        Object[] w = ((SystemUiOverlayStyle) o).values;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null ? w[i] != null : !values[i].equals(w[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (Object x : values) {
            h = h * 31 + (x == null ? 0 : x.hashCode());
        }
        return h;
    }

    @Override
    public String toString() {
        return "SystemUiOverlayStyle." + name;
    }
}
