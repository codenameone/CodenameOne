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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.Style;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/// What the native component of a control keeps of the theme: nothing
/// behind the text of a label, nothing behind a button that has a
/// background of its own, and a text colour a cell can be read in.
public class NativeChromeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
    }

    private static Background fill(Color color) {
        return new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY));
    }

    @Test
    public void aLabelHasNoBackgroundOfTheTheme() {
        Label label = new Label("text");
        Component c = label.cn1Native();
        // Whatever the theme says of a Label, the opaque style is undone.
        c.getAllStyles().setBgTransparency(255);
        label.setText("other");
        assertEquals(0, c.getUnselectedStyle().getBgTransparency() & 0xff);
        assertTrue(c.getUnselectedStyle().getBorder().isEmptyBorder());
    }

    @Test
    public void aButtonWithItsOwnBackgroundDropsTheOneOfTheTheme() {
        Button button = new Button("go");
        Component c = button.cn1Native();
        Style theme = c.getUnselectedStyle();
        theme.setBgTransparency(255);
        button.setText("still themed");
        assertEquals(255, c.getUnselectedStyle().getBgTransparency() & 0xff);

        button.setBackground(fill(Color.RED));
        assertEquals(0, c.getUnselectedStyle().getBgTransparency() & 0xff);
        assertEquals(0, c.getPressedStyle().getBgTransparency() & 0xff);
        assertTrue(c.getUnselectedStyle().getBorder().isEmptyBorder());

        // Without it the styles are those of the theme again, not the
        // ones this test or the layer wrote.
        button.setBackground(null);
        assertFalse(c.getUnselectedStyle() == theme);
        assertEquals("still themed", ((com.codename1.ui.Button) c).getText());
    }

    @Test
    public void aCellIsReadableOnADarkBackground() {
        Pane pane = new Pane();
        ListCell<String> cell = new ListCell<String>();
        pane.getChildren().add(cell);
        Component c = cell.cn1Native();
        int theme = c.getUnselectedStyle().getFgColor() & 0xffffff;

        pane.setBackground(fill(Color.web("#1d1d1d")));
        cell.setText("dark");
        assertEquals(0xffffff, c.getUnselectedStyle().getFgColor() & 0xffffff);

        pane.setBackground(fill(Color.web("#f4f4f4")));
        cell.setText("light");
        assertEquals(theme, c.getUnselectedStyle().getFgColor() & 0xffffff);

        // A translucent wash does not count; the dark one behind it does.
        Pane outer = new Pane();
        outer.setBackground(fill(Color.BLACK));
        outer.getChildren().add(pane);
        pane.setBackground(fill(Color.color(1, 1, 1, 0.1)));
        cell.setText("washed");
        assertEquals(0xffffff, c.getUnselectedStyle().getFgColor() & 0xffffff);

        // A text fill of the application's own is left alone.
        cell.setTextFill(Color.RED);
        assertEquals(0xff0000, c.getUnselectedStyle().getFgColor() & 0xffffff);
    }
}
