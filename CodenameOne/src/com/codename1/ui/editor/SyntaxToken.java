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
package com.codename1.ui.editor;

/// One highlighted source span. Third-party highlighters may use a standard semantic kind or provide
/// explicit colors without depending on the built-in tokenizer implementation.
public final class SyntaxToken {
    /// Standard semantic kind for a language keyword.
    public static final int KEYWORD = 1;
    /// Standard semantic kind for a string or character literal.
    public static final int STRING = 2;
    /// Standard semantic kind for a comment.
    public static final int COMMENT = 3;
    /// Standard semantic kind for a numeric literal.
    public static final int NUMBER = 4;
    /// Standard semantic kind for a tag or declared type.
    public static final int TYPE = 5;
    /// Standard semantic kind for a property or attribute name.
    public static final int PROPERTY = 6;

    /// Zero-based start column.
    public final int start;
    /// Length of the highlighted range.
    public final int length;
    /// Semantic kind, usually one of the constants in this class.
    public final int kind;
    /// Explicit light-theme RGB color, or -1 to use the palette color for `kind`.
    public final int lightColor;
    /// Explicit dark-theme RGB color, or -1 to use the palette color for `kind`.
    public final int darkColor;

    /// Creates a token that uses the editor palette for its semantic kind.
    public SyntaxToken(int start, int length, int kind) {
        this(start, length, kind, -1, -1);
    }

    /// Creates a token with explicit light and dark theme colors.
    public SyntaxToken(int start, int length, int kind, int lightColor, int darkColor) {
        if (start < 0 || length < 0) {
            throw new IllegalArgumentException("Token range must not be negative");
        }
        this.start = start;
        this.length = length;
        this.kind = kind;
        this.lightColor = lightColor;
        this.darkColor = darkColor;
    }
}
