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

import java.util.ArrayList;
import java.util.List;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// The size a layout pane gives a control whose preferred size was set.
public class ControlSizeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Renderer.setTrace(null);
        Units.setScale(0);
    }

    /// The toolbar of 2048FX: buttons with no text, sized by the
    /// application and drawn as an icon by `-fx-shape`.
    @Test
    public void aButtonWithNoTextKeepsThePreferredSizeItWasGiven() {
        Button icon = new Button();
        icon.setPrefSize(40, 40);
        assertEquals(40, icon.maxWidth(-1), 0.01);
        assertEquals(40, icon.maxHeight(-1), 0.01);
        HBox bar = new HBox(icon);
        bar.resize(300, 60);
        bar.layout();
        assertEquals(40, icon.getWidth(), 0.01);
        assertEquals(40, icon.getHeight(), 0.01);
    }

    @Test
    public void aStackPaneDoesNotStretchAButtonPastItsPreferredSize() {
        Button icon = new Button();
        icon.setPrefSize(40, 30);
        StackPane pane = new StackPane(icon);
        pane.resize(200, 200);
        pane.layout();
        assertEquals(40, icon.getWidth(), 0.01);
        assertEquals(30, icon.getHeight(), 0.01);
    }

    @Test
    public void theShapeOfASizedButtonIsFilled() {
        Button icon = new Button();
        icon.setPrefSize(40, 40);
        icon.setStyle("-fx-shape: \"M0 0h4v12h-4z\"; -fx-background-color: #f9f6f2;");
        // A style is applied by the scene, at its layout.
        new javafx.scene.Scene(new HBox(icon), 300, 60).cn1Layout(300, 60);
        assertEquals(40, icon.getWidth(), 0.01);
        final List<String> drawn = new ArrayList<String>();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                drawn.add(operation + ":" + Math.round(deviceBounds[2]) + "x" + Math.round(deviceBounds[3]));
            }
        });
        icon.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(100, 100, 0).getGraphics(), 0, 0));
        assertTrue("drew " + drawn, drawn.contains("fill:40x40"));
    }
}
