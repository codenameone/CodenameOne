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
import com.codename1.flutter.EdgeInsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A decoration resolves against the ambient inputDecorationTheme.
 *
 * <p>Flutter's {@code InputDecoration.applyDefaults}: every field the widget
 * leaves unset falls back to the theme. The theme was held opaquely and never
 * read, so Rally -- which names its dark fill once on the theme rather than on
 * each of its login fields -- rendered white blocks on a dark page.</p>
 */
class InputDecorationDefaultsTest {

    private static InputDecorationThemeData darkFilledTheme() {
        InputDecorationThemeData t = new InputDecorationThemeData();
        t.filled(true);
        t.fillColor(new Color(0xFF33333DL));
        t.contentPadding(EdgeInsets.all(20));
        return t;
    }

    @Test
    void aThemeFillReachesADecorationThatNamesNone() {
        InputDecoration d = new InputDecoration();
        assertTrue(TextFieldRenderElement.resolveFilled(d, darkFilledTheme()));
        assertEquals(0xFF33333DL,
                TextFieldRenderElement.resolveFill(d, darkFilledTheme()).value());
    }

    @Test
    void theDecorationsOwnFillWins() {
        InputDecoration d = new InputDecoration();
        d.filled(true);
        d.fillColor(new Color(0xFFAABBCCL));
        assertEquals(0xFFAABBCCL,
                TextFieldRenderElement.resolveFill(d, darkFilledTheme()).value());
    }

    @Test
    void withNoThemeNothingIsFilled() {
        InputDecoration d = new InputDecoration();
        assertFalse(TextFieldRenderElement.resolveFilled(d, null));
        assertNull(TextFieldRenderElement.resolveFill(d, null));
        assertNull(TextFieldRenderElement.resolvePadding(d, null));
    }

    @Test
    void paddingFallsBackToTheThemeToo() {
        InputDecoration d = new InputDecoration();
        EdgeInsets got = (EdgeInsets) TextFieldRenderElement.resolvePadding(d, darkFilledTheme());
        assertEquals(20.0, got.left(), 0.001);
        d.contentPadding(EdgeInsets.all(4));
        got = (EdgeInsets) TextFieldRenderElement.resolvePadding(d, darkFilledTheme());
        assertEquals(4.0, got.left(), 0.001);
    }

    @Test
    void aThemeDataOnTheThemeIsReadableBack() {
        ThemeData theme = new ThemeData();
        InputDecorationThemeData t = darkFilledTheme();
        theme.inputDecorationTheme(t);
        assertEquals(t, theme.inputDecorationTheme());
        // Anything else stays opaque rather than being mistaken for one.
        theme.inputDecorationTheme("not a theme");
        assertNull(theme.inputDecorationTheme());
    }
}
