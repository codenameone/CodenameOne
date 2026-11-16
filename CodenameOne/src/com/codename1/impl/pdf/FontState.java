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
package com.codename1.impl.pdf;

import java.io.IOException;

/// What one document knows about one font: its resource name, its object
/// number and, for an embedded font, the glyphs used so far.
@com.codename1.impl.SharedWithBackend
public abstract class FontState {
    /// The name the page resources refer to the font by, such as `F1`.
    public String resourceName;
    /// The object number of the font dictionary.
    public int objectNumber;

    /// Appends `text` as the string operand of a `Tj`, recording the
    /// glyphs it uses.
    public abstract void showText(StringBuilder content, String text);

    /// Appends the logical-order `text` as a `Tj` operand; with `rtl`, as
    /// a right-to-left run: reversed, with mirrored characters swapped.
    public void showText(StringBuilder content, String text, boolean rtl) {
        if (!rtl) {
            showText(content, text);
            return;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = text.length() - 1; i >= 0; i--) {
            sb.append(PdfBidi.mirror(text.charAt(i)));
        }
        showText(content, sb.toString());
    }

    /// Writes the font dictionary as object [#objectNumber] and anything
    /// it refers to.
    public abstract void write(PdfWriter writer) throws IOException;
}
