/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class FontImageTest extends UITestBase {
    @BeforeEach
    void resetMaterialFont() throws Exception {
        Field field = FontImage.class.getDeclaredField("materialDesignFont");
        field.setAccessible(true);
        field.set(null, null);
    }

    @FormTest
    void testCreateCopiesStyleState() {
        Font iconFont = Font.createTrueTypeFont("IconFont", "icon.ttf");
        Style style = new Style();
        style.setFont(iconFont);
        style.setBgTransparency((byte) 77);
        style.setBgColor(0x112233);
        style.setFgColor(0x445566);
        style.setOpacity(200);
        style.setFgAlpha(180);

        FontImage image = FontImage.create("A", style, iconFont);
        assertEquals(10, image.getWidth());
        assertEquals(10, image.getHeight());
        assertEquals(1, image.getPadding());

        assertEquals(0x445566, getPrivateInt(image, "color"));
        assertEquals(0x112233, getPrivateInt(image, "backgroundColor"));
        assertEquals(77, getPrivateInt(image, "backgroundOpacity"));
        assertEquals(200, getPrivateInt(image, "opacity"));
        assertEquals(180, getPrivateInt(image, "fgAlpha"));
        assertEquals("A", getPrivateString(image, "text"));
        assertSame(iconFont, image.getFont());
    }

    @FormTest
    void testSetPaddingAdjustsFontSize() {
        Font iconFont = Font.createTrueTypeFont("PaddingFont", "padding.ttf");
        Style style = new Style();
        style.setFont(iconFont);
        FontImage image = FontImage.create("A", style, iconFont);

        image.setPadding(3);
        assertEquals(3, image.getPadding());
        assertNotSame(iconFont, image.getFont());
        assertEquals(7f, image.getFont().getPixelSize(), 0.001f);
    }

    @FormTest
    void testGetMaterialDesignFontCachesValueWhenSupported() {
        Font first = FontImage.getMaterialDesignFont();
        Font second = FontImage.getMaterialDesignFont();
        assertSame(first, second);
        assertTrue(first.isTTFNativeFont());
    }

    @FormTest
    void testSetIconOnSelectableIconHolderCreatesStateIcons() {
        Font iconFont = Font.createTrueTypeFont("ButtonFont", "button.ttf");
        Button button = new Button("Action");
        button.getUnselectedStyle().setFgColor(0x111111);
        button.getSelectedStyle().setFgColor(0x222222);
        button.getPressedStyle().setFgColor(0x333333);
        button.getDisabledStyle().setFgColor(0x444444);

        char[] icons = new char[]{'a', 'b', 'c', 'd', 'e'};
        FontImage.setIcon(button, iconFont, icons, 4f);

        assertTrue(button.getIcon() instanceof FontImage);
        assertTrue(button.getPressedIcon() instanceof FontImage);
        assertTrue(button.getDisabledIcon() instanceof FontImage);
        assertTrue(button.getRolloverPressedIcon() instanceof FontImage);

        assertEquals("a", getPrivateString(button.getIcon(), "text"));
        assertEquals("c", getPrivateString(button.getPressedIcon(), "text"));
        assertEquals("e", getPrivateString(button.getDisabledIcon(), "text"));
    }

    @FormTest
    void testMaterialIconsOfTheSameSizeShareOneDerivedFont() {
        Style s = new Style();
        FontImage a = FontImage.createMaterial(FontImage.MATERIAL_ADD, s, 4f);
        FontImage b = FontImage.createMaterial(FontImage.MATERIAL_CLOSE, s, 4f);
        assertSame(a.getFont(), b.getFont(),
                "icons of the same size must share one derived font: deriving per icon "
                + "builds a native font per icon");
    }

    @FormTest
    void testMaterialFontCacheIsDroppedWhenTheBaseFontChanges() throws Exception {
        Style s = new Style();
        Font first = FontImage.createMaterial(FontImage.MATERIAL_ADD, s, 4f).getFont();
        // A DIFFERENT icon font, not merely a re-created one: clearing the field
        // and letting it rebuild hands back the same cached instance, and the
        // sizes derived from it are then still correct.
        Field field = FontImage.class.getDeclaredField("materialDesignFont");
        field.setAccessible(true);
        field.set(null, Font.createTrueTypeFont("OtherIcons", "other.ttf"));
        Font second = FontImage.createMaterial(FontImage.MATERIAL_ADD, s, 4f).getFont();
        assertNotSame(first, second,
                "a different icon font must invalidate every size derived from the old one");
    }

    @FormTest
    void testMaterialFontSurvivesTheSoftReferenceBeingCleared() throws Exception {
        Style s = new Style();
        Font first = FontImage.createMaterial(FontImage.MATERIAL_ADD, s, 4f).getFont();
        // What a low-memory device does to the cache. The next call must rebuild
        // it rather than hand back null.
        Field field = FontImage.class.getDeclaredField("materialByPixels");
        field.setAccessible(true);
        field.set(null, null);
        Font second = FontImage.createMaterial(FontImage.MATERIAL_ADD, s, 4f).getFont();
        assertNotNull(second);
        assertEquals(first.getPixelSize(), second.getPixelSize(), 0.001f);
    }

    private int getPrivateInt(Object target, String name) {
        try {
            Field field = FontImage.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private String getPrivateString(Object target, String name) {
        try {
            Field field = FontImage.class.getDeclaredField(name);
            field.setAccessible(true);
            return (String) field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
