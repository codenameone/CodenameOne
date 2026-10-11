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

import com.codename1.flutter.Brightness;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Color;
import com.codename1.flutter.Colors;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.RasterDisplay;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An {@code enabled: false} TextField honours Material 2's disabled styling
 * (InputDecorator._getActiveColor, packages/flutter/lib/src/material/input_decorator.dart):
 * once {@code decoration.enabled} is false, the label/hint, the input text and
 * the border all recolour to {@code ThemeData.disabledColor} (black38 on a
 * light theme), OVER whatever the field's own style or the active/hint-colour
 * chain would otherwise pick -- and a filled field's fill washes a little
 * lighter the same fraction. The gallery's text-field demo states {@code
 * enabled: false} on its second "Email" row and no colour of its own, so
 * nothing but this rule greys it.
 */
class TextFieldDisabledStyleTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    // ------------------------------------------------------------------
    // The pure building blocks (no mounting required)
    // ------------------------------------------------------------------

    @Test
    void defaultDisabledColorIsBlack38LightAndWhite30Dark() {
        assertEquals(0x61000000L, TextFieldRenderElement.defaultDisabledColor(null).value());
        assertEquals(0x61000000L,
                TextFieldRenderElement.defaultDisabledColor(Brightness.light).value());
        assertEquals(0x4DFFFFFFL,
                TextFieldRenderElement.defaultDisabledColor(Brightness.dark).value());
    }

    @Test
    void washingTheFillLightensItTheMeasuredAmount() {
        // The gallery's text-field demo: enabled fill (242,242,242), disabled
        // fill (245,245,245) -- within rounding of blending white at the
        // disabled ink's own alpha fraction over the enabled fill.
        Color enabledFill = new Color(0xFFF2F2F2L);
        Color washed = TextFieldRenderElement.washDisabledFill(enabledFill,
                TextFieldRenderElement.defaultDisabledColor(Brightness.light));
        assertEquals(255, washed.alpha());
        assertTrue(washed.red() > enabledFill.red(), "a disabled fill washes LIGHTER, never darker");
        assertEquals(245, washed.red(), 3);
        assertEquals(245, washed.green(), 3);
    }

    // ------------------------------------------------------------------
    // Mounted resolution (Theme.of(this) reaches a real ancestor Theme)
    // ------------------------------------------------------------------

    private static TextFieldRenderElement mount(ThemeData theme, TextField widget) {
        Theme t = new Theme();
        t.data(theme);
        t.child(widget);
        Element root = FlutterUI.mount(t, new RenderHost(), new BuildOwner());
        final TextFieldRenderElement[] found = new TextFieldRenderElement[1];
        root.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element e) {
                if (e instanceof TextFieldRenderElement) {
                    found[0] = (TextFieldRenderElement) e;
                }
            }
        });
        assertTrue(found[0] != null, "expected the TextField to mount directly under the Theme");
        return found[0];
    }

    private static TextField emailField(boolean enabled) {
        TextField tf = new TextField();
        tf.enabled(enabled);
        InputDecoration d = new InputDecoration();
        d.filled(true);
        d.labelText("Email");
        tf.decoration(d);
        return tf;
    }

    private static ThemeData lightTheme() {
        ThemeData t = new ThemeData();
        t.colorScheme(ColorScheme.fromSeed(Colors.deepPurple));
        return t;
    }

    @Test
    void aDisabledFieldsLabelDimsToDisabledColorInsteadOfTheActiveOrHintChain() {
        ThemeData theme = lightTheme();
        TextFieldRenderElement enabled = mount(theme, emailField(true));
        TextFieldRenderElement disabled = mount(theme, emailField(false));

        long disabledInk = TextFieldRenderElement.defaultDisabledColor(theme.brightness()).value();
        InputDecoration decoration = ((TextField) disabled.widget()).getDecoration();
        assertEquals(disabledInk, disabled.hintStyle(decoration).getColor().value());
        assertNotEquals(disabledInk, enabled.hintStyle(((TextField) enabled.widget()).getDecoration())
                .getColor().value(), "an ENABLED labelText takes onSurfaceVariant, not disabledColor");
    }

    @Test
    void aDisabledFieldsInputTextAlsoDimsToDisabledColor() {
        ThemeData theme = lightTheme();
        TextFieldRenderElement disabled = mount(theme, emailField(false));
        long disabledInk = TextFieldRenderElement.defaultDisabledColor(theme.brightness()).value();
        assertEquals(disabledInk, disabled.inputStyle().getColor().value());
    }

    @Test
    void aDisabledFieldsUnderlineFadesWhileTheEnabledOneStaysBlack() {
        ThemeData theme = lightTheme();
        TextFieldRenderElement enabled = mount(theme, emailField(true));
        TextFieldRenderElement disabled = mount(theme, emailField(false));

        assertEquals(0x000000, enabled.underlineRgb(),
                "Flutter's default BorderSide is opaque black while resting");
        assertNotEquals(0x000000, disabled.underlineRgb(),
                "a disabled field's rule fades toward the page, per InputDecorator");
        // Same formula production code uses: disabledColor composited over
        // whatever the field paints behind it (no fill is stated here, so
        // that is the theme's surface, not plain white).
        int expected = Color.alphaBlend(TextFieldRenderElement.defaultDisabledColor(theme.brightness()),
                new Color(0xFF000000L | (disabled.backdropRgb() & 0xFFFFFFL))).rgb();
        assertEquals(expected, disabled.underlineRgb());
    }
}
