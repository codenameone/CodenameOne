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

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.OutlineInputBorder;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.RasterDisplay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A theme-level {@code contentPadding} must still reach a field whose BORDER
 * this runtime does not know how to approximate -- Shrine's login, whose
 * {@code inputDecorationTheme} states both a {@code CutCornersBorder} (an
 * application subclass of {@code OutlineInputBorder} that paints itself) and
 * {@code contentPadding: EdgeInsets.symmetric(vertical: 20, horizontal: 16)}.
 *
 * <p>{@code applyDecoration}'s self-painting branch used to {@code return}
 * before the padding block, which sat at the very end of the method -- so
 * neither "Username" nor "Password" ever received it, and both fields
 * measured about 80% of the reference's height.</p>
 */
class TextFieldContentPaddingTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    /** Stands in for the gallery's {@code CutCornersBorder extends OutlineInputBorder}. */
    private static final class SelfPaintingBorder extends OutlineInputBorder {
    }

    private static TextFieldRenderElement mountUsernameField() {
        InputDecorationThemeData themed = new InputDecorationThemeData();
        themed.contentPadding(EdgeInsets.symmetric(16, 20));
        themed.border(new SelfPaintingBorder());

        ThemeData data = new ThemeData();
        data.inputDecorationTheme(themed);

        InputDecoration decoration = new InputDecoration();
        decoration.labelText("Username");

        TextField field = new TextField();
        field.decoration(decoration);

        Theme theme = new Theme();
        theme.data(data);
        theme.child(field);

        Element root = FlutterUI.mount(theme, new RenderHost(), new BuildOwner());
        final TextFieldRenderElement[] found = new TextFieldRenderElement[1];
        root.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element e) {
                if (e instanceof TextFieldRenderElement) {
                    found[0] = (TextFieldRenderElement) e;
                }
            }
        });
        assertNotNull(found[0], "expected the TextField to mount directly under the Theme");
        return found[0];
    }

    @Test
    void aSelfPaintingBorderStillGetsTheThemesContentPadding() {
        TextFieldRenderElement el = mountUsernameField();
        com.codename1.ui.Component c = el.component();
        assertNotNull(c, "RasterDisplay gives createComponent a real field");

        com.codename1.ui.plaf.Style s = c.getUnselectedStyle();
        assertEquals((int) Math.round(Dp.px(20)), s.getPaddingTop());
        assertEquals((int) Math.round(Dp.px(20)), s.getPaddingBottom());
        assertEquals((int) Math.round(Dp.px(16)), s.getPaddingLeftNoRTL());
        assertEquals((int) Math.round(Dp.px(16)), s.getPaddingRightNoRTL());
    }

    @Test
    void resolvePaddingPrefersTheFieldsOwnOverTheThemes() {
        InputDecorationThemeData themed = new InputDecorationThemeData();
        themed.contentPadding(EdgeInsets.symmetric(16, 20));

        InputDecoration d = new InputDecoration();
        d.contentPadding(EdgeInsets.all(4));

        com.codename1.flutter.EdgeInsetsGeometry got = TextFieldRenderElement.resolvePadding(d, themed);
        assertEquals(d.getContentPadding(), got);
    }
}
