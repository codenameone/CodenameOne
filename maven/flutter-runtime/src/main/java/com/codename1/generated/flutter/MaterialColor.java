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
 * A color swatch with a primary value plus its indexed shades (50, 100..900, and
 * for grey also 350 and 850) — Flutter's {@code MaterialColor}. The colors demo
 * indexes it (Dart's {@code swatch[key]}, transpiled to {@link #idx(long)}) to
 * list every shade of a palette.
 *
 * <p>Lives in the transpiler's generated package because new_gallery references
 * it unqualified (the Flutter SDK type carries no {@code @JavaName} mapping) and
 * {@code _Palette} names it in the same package.</p>
 *
 * <p>The swatch is two parallel {@code long} arrays rather than a map: a swatch
 * has at most a dozen entries, so a scan beats a hash, and primitive arrays cost
 * no boxing on the ports with the tightest runtime.</p>
 */
public class MaterialColor extends Color {

    private final long[] keys;
    private final long[] values;

    /**
     * A swatch whose shades all resolve to {@code primary}.
     *
     * <p>Retained for a palette that has no shade table of its own; prefer the
     * three-argument constructor, because a swatch that answers the same color
     * for every shade renders a palette as one flat block.</p>
     */
    public MaterialColor(long primary) {
        this(primary, null, null);
    }

    public MaterialColor(long primary, long[] keys, long[] values) {
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

    public Color shade50() {
        return idx(50);
    }

    public Color shade100() {
        return idx(100);
    }

    public Color shade200() {
        return idx(200);
    }

    public Color shade300() {
        return idx(300);
    }

    public Color shade400() {
        return idx(400);
    }

    public Color shade500() {
        return idx(500);
    }

    public Color shade600() {
        return idx(600);
    }

    public Color shade700() {
        return idx(700);
    }

    public Color shade800() {
        return idx(800);
    }

    public Color shade900() {
        return idx(900);
    }
}
