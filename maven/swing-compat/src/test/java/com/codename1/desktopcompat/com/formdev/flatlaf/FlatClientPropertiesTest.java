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
package com.codename1.desktopcompat.com.formdev.flatlaf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JComponent;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JPanel;
import com.codename1.desktopcompat.javax.swing.JTextField;
import com.codename1.desktopcompat.javax.swing.UIManager;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.javax.swing.border.EmptyBorder;
import com.codename1.desktopcompat.rt.ClientProps;
import org.junit.Test;

/// The FlatLaf client properties that do something in this layer, each
/// shown doing it, and one that is only recorded.
public class FlatClientPropertiesTest extends KernelTestBase {

    /// What `putClientProperty` does once it calls the layer's hook; done
    /// by hand here so the test says the same thing either way.
    private static void put(JComponent c, String key, Object value) {
        c.putClientProperty(key, value);
        ClientProps.changed(c, key, value);
    }

    @Test
    public void thePlaceholderIsTheHintOfTheField() {
        JTextField field = new JTextField(12);
        put(field, FlatClientProperties.PLACEHOLDER_TEXT, "Search");
        com.codename1.ui.Component peer = field.cn1Peer();
        assertTrue(peer instanceof com.codename1.ui.TextArea);
        assertEquals("Search", ((com.codename1.ui.TextArea) peer).getHint());
        assertEquals("Search", field.getClientProperty("JTextField.placeholderText"));
        put(field, FlatClientProperties.PLACEHOLDER_TEXT, null);
        assertEquals("", ((com.codename1.ui.TextArea) peer).getHint());
    }

    @Test
    public void aBorderlessOrToolBarButtonHasNoBorderAndNoFill() {
        JButton button = new JButton("Go");
        assertTrue(button.isBorderPainted());
        put(button, FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        assertFalse(button.isBorderPainted());
        assertFalse(button.isContentAreaFilled());
        put(button, FlatClientProperties.BUTTON_TYPE, null);
        assertTrue(button.isBorderPainted());
        assertTrue(button.isContentAreaFilled());
        put(button, FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        assertFalse(button.isBorderPainted());
        // A shape is the theme's: recorded, and the button stays a button.
        put(button, FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        assertTrue(button.isBorderPainted());
        assertEquals("roundRect", button.getClientProperty(FlatClientProperties.BUTTON_TYPE));
    }

    @Test
    public void anOutlineIsALineAroundTheComponentAndGoesAgain() {
        JPanel panel = new JPanel();
        Border own = new EmptyBorder(1, 1, 1, 1);
        panel.setBorder(own);
        JFrame f = new JFrame();
        f.add(panel, BorderLayout.CENTER);
        f.setSize(200, 100);
        show(f);
        put(panel, FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        int error = UIManager.getColor("Component.error.focusedBorderColor").getRGB() & 0xffffff;
        int[][] rows = raster(f);
        assertEquals(error, pixel(rows, panel, 0, panel.getHeight() / 2) & 0xffffff);
        assertEquals(error, pixel(rows, panel, panel.getWidth() / 2, 1) & 0xffffff);
        assertFalse(error == (pixel(rows, panel, panel.getWidth() / 2, panel.getHeight() / 2) & 0xffffff));

        Color mine = new Color(10, 200, 30);
        put(panel, FlatClientProperties.OUTLINE, mine);
        rows = raster(f);
        assertEquals(mine.getRGB() & 0xffffff, pixel(rows, panel, 0, panel.getHeight() / 2) & 0xffffff);
        put(panel, FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_WARNING);
        rows = raster(f);
        assertEquals(UIManager.getColor("Component.warning.focusedBorderColor").getRGB() & 0xffffff,
                pixel(rows, panel, 0, panel.getHeight() / 2) & 0xffffff);

        put(panel, FlatClientProperties.OUTLINE, null);
        assertSame(own, panel.getBorder());
        rows = raster(f);
        assertFalse(error == (pixel(rows, panel, 0, panel.getHeight() / 2) & 0xffffff));
    }

    @Test
    public void aStyleClassAndAStyleGiveTheFontAndColorsTheyName() {
        JLabel plain = new JLabel("plain");
        JLabel title = new JLabel("title");
        float base = plain.getFont().getSize2D();
        put(title, FlatClientProperties.STYLE_CLASS, "unknown h1");
        assertTrue(title.getFont().getSize2D() > base);
        assertTrue(title.getFont().isBold());
        JLabel small = new JLabel("small");
        put(small, FlatClientProperties.STYLE_CLASS, "small");
        assertTrue(small.getFont().getSize2D() < base);
        JLabel mono = new JLabel("mono");
        put(mono, FlatClientProperties.STYLE_CLASS, "monospaced");
        assertEquals(Font.MONOSPACED, mono.getFont().getFamily());

        JLabel styled = new JLabel("styled");
        put(styled, FlatClientProperties.STYLE, "background: #102030; foreground: #fff; font: bold 20; arc: 8");
        assertEquals(0x102030, styled.getBackground().getRGB() & 0xffffff);
        assertEquals(0xffffff, styled.getForeground().getRGB() & 0xffffff);
        assertTrue(styled.getFont().isBold());
        assertEquals(20, styled.getFont().getSize());
        JLabel bigger = new JLabel("bigger");
        put(bigger, FlatClientProperties.STYLE, "font: +4 italic");
        assertTrue(bigger.getFont().getSize2D() > base);
        assertTrue(bigger.getFont().isItalic());
    }

    @Test
    public void aMinimumWidthIsTheMinimumSize() {
        JButton button = new JButton("x");
        put(button, FlatClientProperties.MINIMUM_WIDTH, Integer.valueOf(140));
        assertEquals(140, button.getMinimumSize().width);
        put(button, FlatClientProperties.MINIMUM_HEIGHT, Integer.valueOf(44));
        assertEquals(44, button.getMinimumSize().height);
        assertEquals(140, button.getMinimumSize().width);
    }

    @Test
    public void everyOtherPropertyIsKeptAndChangesNothing() {
        JTextField field = new JTextField("text");
        Border before = field.getBorder();
        Font font = field.getFont();
        put(field, FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, Boolean.TRUE);
        put(field, FlatClientProperties.COMPONENT_ROUND_RECT, Boolean.TRUE);
        put(field, FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY, FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY_ALWAYS);
        assertEquals(Boolean.TRUE, field.getClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON));
        assertTrue(FlatClientProperties.clientPropertyBoolean(field, FlatClientProperties.COMPONENT_ROUND_RECT, false));
        assertTrue(FlatClientProperties.clientPropertyEquals(field, FlatClientProperties.SELECT_ALL_ON_FOCUS_POLICY,
                "always"));
        assertSame(before, field.getBorder());
        assertEquals(font, field.getFont());
        assertNull(field.getClientProperty(FlatClientProperties.OUTLINE));
        assertEquals(7, FlatClientProperties.clientPropertyInt(field, "No.such", 7));
    }
}
