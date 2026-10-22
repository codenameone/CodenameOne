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
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Button;
import com.codename1.ui.Component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression for the gallery's Text Button demo bottom row: a disabled
 * {@code TextButton.icon}'s leading "+" ({@link Icons#add}) kept drawing in
 * the button's full-strength enabled purple, while its text label correctly
 * faded to the disabled ink. Flutter paints both the icon and the label of a
 * disabled button in the same resolved foreground (one {@code
 * ButtonStyle.foregroundColor} covers the whole button, text and icon alike
 * -- see {@code ButtonStyleButton.build}).
 *
 * <p>{@link ButtonRenderElement#apply} used to bake the glyph via {@code
 * FontImage.setMaterialIcon} BEFORE {@code setEnabled}/{@code
 * applyDisabledInk} ran, so the button's disabled style still equalled its
 * unselected one at the moment the icon was created. {@code
 * FontImage.setIcon(IconHolder, ...)} only rasterises a SEPARATE disabled
 * image when the disabled style already differs from the unselected one (its
 * own per-channel comparison), so with the two equal it always called {@code
 * b.setDisabledIcon(null)} and the button kept wearing the one
 * enabled-coloured image in every state. Label text was unaffected because
 * {@code Label} reads its foreground live from the Style at paint time,
 * never baking a colour into a bitmap -- so moving the enable/ink pass
 * earlier is the whole fix.</p>
 */
class DisabledButtonIconForegroundTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static Button mount(dart.runtime.Funcs.VoidFunc0 onPressed) {
        TextButton b = TextButton.icon(null, onPressed, null,
                new Icon(Icons.add()), new Text("Button"));
        Element root = FlutterUI.mount(b, new RenderHost(), new BuildOwner());
        Component c = ((ButtonRenderElement) root).component();
        assertNotNull(c, "RasterDisplay gives createComponent a real Button");
        return (Button) c;
    }

    @Test
    void aDisabledIconButtonBakesASeparateFadedDisabledGlyph() {
        Button b = mount(null);
        assertFalse(b.isEnabled(), "a null onPressed disables the button");
        assertNotNull(b.getIcon(), "the enabled-state glyph is still created");
        assertNotNull(b.getDisabledIcon(),
                "the disabled style now differs from the unselected one (applyDisabledInk "
                        + "ran BEFORE the glyph was baked), so FontImage must have rasterised "
                        + "a separate, faded disabled glyph rather than reusing the purple one");
        assertNotEquals(b.getIcon(), b.getDisabledIcon(),
                "the disabled glyph must be a DIFFERENT image, not the enabled one reused");
    }

    @Test
    void anEnabledIconButtonNeedsNoSeparateDisabledGlyph() {
        Button b = mount(() -> { });
        assertNotNull(b.getIcon());
        assertNull(b.getDisabledIcon(),
                "an enabled button's disabled style still equals its unselected one, so "
                        + "FontImage correctly skips rasterising a redundant second image");
    }

}
