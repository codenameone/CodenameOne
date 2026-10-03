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

import com.codename1.flutter.TextStyle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The AppBar title falls back to the text theme's titleLarge.
 *
 * <p>Flutter resolves it as {@code AppBar.titleTextStyle ??
 * AppBarTheme.titleTextStyle ?? textTheme.titleLarge}. The last link was
 * missing, so a bar whose theme names no title style -- most of them -- fell
 * through to whatever a bare Text picks: about 16 logical pixels against
 * titleLarge's 22, which rendered every title in the gallery at roughly seven
 * tenths of its size.</p>
 */
class AppBarTitleStyleTest {

    @Test
    void withoutAThemeStyleTheTitleTakesTitleLarge() {
        TextStyle chosen = AppBarRenderElement.chooseTitleStyle(null, new TextTheme());
        assertEquals(22.0, chosen.getFontSize(), 0.001);
    }

    @Test
    void aThemeStyleWins() {
        TextStyle themed = new TextStyle();
        themed.fontSize(31);
        TextStyle chosen = AppBarRenderElement.chooseTitleStyle(themed, new TextTheme());
        assertEquals(31.0, chosen.getFontSize(), 0.001);
    }

    @Test
    void noTextThemeAtAllLeavesTheTitleUnstyled() {
        assertNull(AppBarRenderElement.chooseTitleStyle(null, null));
    }
}
