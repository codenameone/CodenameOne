/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.designer.css.raster;

/// The CSS `border-style` keywords.
public enum BorderStyle {
    NONE, HIDDEN, SOLID, DASHED, DOTTED, DOUBLE, GROOVE, RIDGE, INSET, OUTSET;

    /// Parses a CSS `border-style` keyword. The comparison ignores case
    /// without folding it, so it does not depend on the default locale.
    ///
    /// #### Parameters
    ///
    /// - `cssKeyword`: the keyword, surrounding white space is ignored
    ///
    /// #### Returns
    ///
    /// the matching style, or [#NONE] for `null` or an unknown keyword
    public static BorderStyle parse(String cssKeyword) {
        if (cssKeyword == null) {
            return NONE;
        }
        String k = cssKeyword.trim();
        for (BorderStyle s : values()) {
            if (s.name().equalsIgnoreCase(k)) {
                return s;
            }
        }
        return NONE;
    }

    /// Whether a border of this style paints anything and takes up width.
    /// `NONE` and `HIDDEN` do not.
    public boolean isVisible() {
        return this != NONE && this != HIDDEN;
    }
}
