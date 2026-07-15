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

/// The color palette for the pure code editor, mirroring the light (GitHub style) and dark (VS Code
/// style) themes used by the previous `BrowserComponent` backend so switching backends is visually
/// consistent.
public final class ThemePalette {
    /// The GitHub style light palette.
    public static final ThemePalette LIGHT = new ThemePalette(
            0xffffff, 0x24292e, 0xb3d4fc,
            0xd73a49, 0x032f62, 0x6a737d, 0x005cc5,
            0xf6f8fa, 0xb0b6bd,
            0xe51400, 0xe36209, 0x1a73e8);

    /// The VS Code style dark palette.
    public static final ThemePalette DARK = new ThemePalette(
            0x1e1e1e, 0xd4d4d4, 0x264f78,
            0x569cd6, 0xce9178, 0x6a9955, 0xb5cea8,
            0x252526, 0x6e7681,
            0xf14c4c, 0xcca700, 0x3794ff);

    private final int background;
    private final int foreground;
    private final int selection;
    private final int keyword;
    private final int string;
    private final int comment;
    private final int number;
    private final int gutterBackground;
    private final int gutterForeground;
    private final int errorColor;
    private final int warningColor;
    private final int infoColor;

    private ThemePalette(int background, int foreground, int selection, int keyword, int string,
                         int comment, int number, int gutterBackground, int gutterForeground,
                         int errorColor, int warningColor, int infoColor) {
        this.background = background;
        this.foreground = foreground;
        this.selection = selection;
        this.keyword = keyword;
        this.string = string;
        this.comment = comment;
        this.number = number;
        this.gutterBackground = gutterBackground;
        this.gutterForeground = gutterForeground;
        this.errorColor = errorColor;
        this.warningColor = warningColor;
        this.infoColor = infoColor;
    }

    /// Returns the palette for the given theme id (`"dark"` returns the dark palette, anything else the
    /// light palette).
    public static ThemePalette forName(String theme) {
        return "dark".equals(theme) ? DARK : LIGHT;
    }

    /// Returns the color for the given `Tokenizer` token kind.
    public int colorForKind(int kind) {
        switch (kind) {
            case Tokenizer.KEYWORD:
                return keyword;
            case Tokenizer.STRING:
                return string;
            case Tokenizer.COMMENT:
                return comment;
            case Tokenizer.NUMBER:
                return number;
            case Tokenizer.TYPE:
                return keyword;
            case Tokenizer.PROPERTY:
                return string;
            default:
                return foreground;
        }
    }

    /// The editor background color.
    public int getBackground() {
        return background;
    }

    /// The default text color.
    public int getForeground() {
        return foreground;
    }

    /// The selection highlight color.
    public int getSelection() {
        return selection;
    }

    /// The gutter background color.
    public int getGutterBackground() {
        return gutterBackground;
    }

    /// The gutter text color.
    public int getGutterForeground() {
        return gutterForeground;
    }

    /// The error diagnostic color.
    public int getErrorColor() {
        return errorColor;
    }

    /// The warning diagnostic color.
    public int getWarningColor() {
        return warningColor;
    }

    /// The info diagnostic color.
    public int getInfoColor() {
        return infoColor;
    }
}
