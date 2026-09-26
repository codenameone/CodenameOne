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
package com.codename1.flutter.material;

import com.codename1.flutter.Color;

/**
 * Colours for text selection — Flutter's {@code TextSelectionThemeData}.
 *
 * <p>Held rather than applied: Codename One draws selection with its own theme colours, so
 * these are recorded for the styling pass to pick up. Keeping the type is not cosmetic
 * though — a theme that names it must still transpile, and three of the four studies set
 * one.</p>
 */
public class TextSelectionThemeData {

    private Color cursorColor;
    private Color selectionColor;
    private Color selectionHandleColor;

    public void cursorColor(Color v) {
        this.cursorColor = v;
    }

    public void selectionColor(Color v) {
        this.selectionColor = v;
    }

    public void selectionHandleColor(Color v) {
        this.selectionHandleColor = v;
    }

    public Color getCursorColor() {
        return cursorColor;
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public Color getSelectionHandleColor() {
        return selectionHandleColor;
    }
}
