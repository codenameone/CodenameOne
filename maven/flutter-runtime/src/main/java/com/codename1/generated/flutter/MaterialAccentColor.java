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
package com.codename1.generated.flutter;

import com.codename1.flutter.Color;

/**
 * An accent color swatch with a primary value plus the four accent shades
 * (100, 200, 400, 700) — Flutter's {@code MaterialAccentColor}. The colors demo
 * indexes it (Dart's {@code swatch[key]}, transpiled to {@link #idx(long)}) to
 * list every accent shade of a palette.
 *
 * <p>Lives in the transpiler's generated package for the same reason as
 * {@link MaterialColor}.</p>
 */
public class MaterialAccentColor extends Color {

    private final long[] keys;
    private final long[] values;

    /** A swatch whose shades all resolve to {@code primary}. */
    public MaterialAccentColor(long primary) {
        this(primary, null, null);
    }

    public MaterialAccentColor(long primary, long[] keys, long[] values) {
        super(primary);
        this.keys = keys;
        this.values = values;
    }

    /**
     * The shade for {@code key} (Dart's {@code operator []}), or the primary
     * value when this swatch does not define that shade.
     */
    public Color idx(long key) {
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] == key) {
                    return new Color(values[i]);
                }
            }
        }
        return this;
    }

    public Color shade100() {
        return idx(100);
    }

    public Color shade200() {
        return idx(200);
    }

    public Color shade400() {
        return idx(400);
    }

    public Color shade700() {
        return idx(700);
    }
}
