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
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Icons;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.flutter.widgets.Icon;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two things {@code updateComponent} used to leave stale on a rebuild, both needing a
 * REAL native component ({@link RasterDisplay}, not headless -- {@code apply()} only
 * runs {@code if (field != null)}, and {@code field} stays null without a Display):
 *
 * <ul>
 *   <li>{@code maxLength}: null/negative must mean unlimited, and a rebuild that drops
 *       a previous finite limit must lift the native cap back up, not keep it.</li>
 *   <li>The decoration's prefix/suffix icon glyph: {@code decorated()} only builds the
 *       row ONCE, on mount, so a password field toggling between
 *       {@code Icons.visibility}/{@code visibility_off} kept drawing the glyph it was
 *       built with forever.</li>
 * </ul>
 */
class TextFieldDecorationUpdateTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static TextFieldRenderElement mount(TextField widget) {
        Element root = FlutterUI.mount(widget, new RenderHost(), new BuildOwner());
        assertTrue(root instanceof TextFieldRenderElement, "expected a TextFieldRenderElement");
        return (TextFieldRenderElement) root;
    }

    // ------------------------------------------------------------------
    // maxLength
    // ------------------------------------------------------------------

    @Test
    void maxLengthGoingUnlimitedOnAnUpdateLiftsThePreviousFiniteCap() {
        TextField limited = new TextField();
        limited.maxLength(10);
        TextFieldRenderElement e = mount(limited);
        assertEquals(10 * 16, e.nativeMaxSizeForTest(), "the finite cap applied on mount");

        // A rebuild that drops the limit entirely (maxLength left unset == null,
        // Flutter's "no limit"). The native cap must come back UP, not stay pinned
        // at the old finite value.
        TextField unlimited = new TextField();
        e.update(unlimited);

        assertEquals(Integer.MAX_VALUE, e.nativeMaxSizeForTest(),
                "dropping maxLength on a rebuild must lift the native cap back to unlimited");
    }

    @Test
    void aFieldCreatedWithNoMaxLengthStartsUnlimitedNotTextAreasDefault() {
        TextField unlimited = new TextField();
        TextFieldRenderElement e = mount(unlimited);

        assertEquals(Integer.MAX_VALUE, e.nativeMaxSizeForTest(),
                "no maxLength at all means Flutter's unlimited, not TextArea's "
                        + "constructor default of 124");
    }

    // ------------------------------------------------------------------
    // Decoration icon sync
    // ------------------------------------------------------------------

    private static TextField passwordField(boolean obscured) {
        TextField tf = new TextField();
        tf.obscureText(true);
        InputDecoration d = new InputDecoration();
        IconButton toggle = new IconButton();
        toggle.icon(new Icon(obscured ? Icons.visibility_off : Icons.visibility));
        d.suffixIcon(toggle);
        tf.decoration(d);
        return tf;
    }

    @Test
    void togglingThePasswordVisibilityIconOnAnUpdateChangesTheRenderedGlyph() {
        TextFieldRenderElement e = mount(passwordField(true));
        Object obscuredGlyph = e.suffixGlyphIconForTest();
        assertNotNull(obscuredGlyph, "a suffix icon must bake a glyph image on mount");

        e.update(passwordField(false));

        Object revealedGlyph = e.suffixGlyphIconForTest();
        assertNotNull(revealedGlyph);
        assertNotEquals(obscuredGlyph, revealedGlyph,
                "updateComponent must rebuild the suffix glyph for the CURRENT "
                        + "suffixIcon -- decorated() only builds the row once, on mount");
    }

    @Test
    void clearingTheSuffixIconOnAnUpdateClearsTheGlyph() {
        TextFieldRenderElement e = mount(passwordField(true));
        assertNotNull(e.suffixGlyphIconForTest());

        // A rebuild whose decoration no longer has a suffixIcon at all.
        TextField noIcon = new TextField();
        noIcon.obscureText(true);
        noIcon.decoration(new InputDecoration());
        e.update(noIcon);

        assertNull(e.suffixGlyphIconForTest(),
                "a decoration that drops its suffixIcon must clear the rendered glyph");
    }
}
