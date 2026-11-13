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
 * Flutter's {@code TextDecoration}: a set of lines drawn with text, combinable with
 * {@link #combine}. Underline and line-through are drawn by the text renderer through
 * the Codename One style; an overline is recorded, since Codename One has no overline.
 */
public final class TextDecoration {

    private static final int UNDERLINE = 1;
    private static final int OVERLINE = 2;
    private static final int LINE_THROUGH = 4;

    public static final TextDecoration none = new TextDecoration(0);
    public static final TextDecoration underline = new TextDecoration(UNDERLINE);
    public static final TextDecoration overline = new TextDecoration(OVERLINE);
    public static final TextDecoration lineThrough = new TextDecoration(LINE_THROUGH);

    private final int mask;

    private TextDecoration(int mask) {
        this.mask = mask;
    }

    /** Flutter's {@code TextDecoration.combine}: the union of the given decorations. */
    public static TextDecoration combine(Iterable<TextDecoration> decorations) {
        int m = 0;
        if (decorations != null) {
            for (TextDecoration d : decorations) {
                if (d != null) {
                    m |= d.mask;
                }
            }
        }
        return new TextDecoration(m);
    }

    /** Flutter's {@code contains}: whether every line of {@code other} is in this one. */
    public boolean contains(TextDecoration other) {
        return other != null && (mask | other.mask) == mask;
    }

    public boolean hasUnderline() {
        return (mask & UNDERLINE) != 0;
    }

    public boolean hasOverline() {
        return (mask & OVERLINE) != 0;
    }

    public boolean hasLineThrough() {
        return (mask & LINE_THROUGH) != 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TextDecoration && ((TextDecoration) o).mask == mask;
    }

    @Override
    public int hashCode() {
        return mask;
    }

    @Override
    public String toString() {
        if (mask == 0) {
            return "TextDecoration.none";
        }
        StringBuilder b = new StringBuilder();
        if (hasUnderline()) {
            b.append("underline");
        }
        if (hasOverline()) {
            b.append(b.length() > 0 ? ", " : "").append("overline");
        }
        if (hasLineThrough()) {
            b.append(b.length() > 0 ? ", " : "").append("lineThrough");
        }
        return (mask == UNDERLINE || mask == OVERLINE || mask == LINE_THROUGH)
                ? "TextDecoration." + b : "TextDecoration.combine([" + b + "])";
    }
}
