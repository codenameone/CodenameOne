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
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// A node scaled to nothing, which is how a node that grows into view
/// starts, and a node faded out to nothing paint nothing and leave the rest
/// of the scene painting.
public class CollapsedTransformTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<Paint> filled = new ArrayList<Paint>();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Renderer.setTrace(null);
        List<javafx.stage.Window> open = new ArrayList<javafx.stage.Window>(javafx.stage.Window.getWindows());
        for (int i = 0; i < open.size(); i++) {
            open.get(i).hide();
        }
        Units.setScale(0);
    }

    private void paint() {
        filled.clear();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                if ("fill".equals(operation)) {
                    filled.add(paint);
                }
            }
        });
        Form f = Display.getInstance().getCurrent();
        f.revalidate();
        f.paintComponent(Image.createImage(HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT).getGraphics());
        Renderer.setTrace(null);
    }

    @Test
    public void aShapeAndAGroupScaledToZeroPaintNothingUntilTheyGrow() {
        Rectangle inGroup = new Rectangle(10, 10, 40, 40);
        inGroup.setFill(Color.RED);
        Group growing = new Group(inGroup);
        Rectangle alone = new Rectangle(60, 10, 40, 40);
        alone.setFill(Color.GREEN);
        Rectangle steady = new Rectangle(110, 10, 40, 40);
        steady.setFill(Color.BLUE);
        Stage stage = new Stage();
        stage.setScene(new Scene(new Group(growing, alone, steady), 200, 100));
        stage.show();

        growing.setScaleX(0);
        growing.setScaleY(0);
        alone.setScaleY(0);
        paint();
        assertFalse("a group scaled to nothing painted its child", filled.contains(Color.RED));
        assertFalse("a shape scaled to nothing was painted", filled.contains(Color.GREEN));
        assertTrue("the node after them was not painted", filled.contains(Color.BLUE));

        growing.setScaleX(0.5);
        growing.setScaleY(0.5);
        alone.setScaleY(1);
        paint();
        assertTrue(filled.contains(Color.RED));
        assertTrue(filled.contains(Color.GREEN));
        assertTrue(filled.contains(Color.BLUE));
    }

    /// What a fade to nothing ends on: a node at opacity zero.
    @Test
    public void aNodeFadedOutPaintsNothingAndItsTextIsNotDrawn() {
        Rectangle faded = new Rectangle(10, 10, 40, 40);
        faded.setFill(Color.RED);
        javafx.scene.control.Label points = new javafx.scene.control.Label("+8");
        Rectangle steady = new Rectangle(110, 10, 40, 40);
        steady.setFill(Color.BLUE);
        Stage stage = new Stage();
        stage.setScene(new Scene(new Group(faded, points, steady), 200, 100));
        stage.show();

        HeadlessImplementation.drawnText.clear();
        HeadlessImplementation.recordText = true;
        try {
            paint();
            assertTrue(filled.contains(Color.RED));
            assertTrue("the label was not drawn at all", drewText("+8"));

            faded.setOpacity(0);
            points.setOpacity(0);
            HeadlessImplementation.drawnText.clear();
            paint();
            assertFalse(filled.contains(Color.RED));
            assertFalse("a label at opacity zero was drawn", drewText("+8"));
            assertTrue(filled.contains(Color.BLUE));
        } finally {
            HeadlessImplementation.recordText = false;
            HeadlessImplementation.drawnText.clear();
        }
    }

    private static boolean drewText(String wanted) {
        for (int i = 0; i < HeadlessImplementation.drawnText.size(); i++) {
            Object[] entry = HeadlessImplementation.drawnText.get(i);
            for (int j = 0; j < entry.length; j++) {
                if (wanted.equals(entry[j])) {
                    return true;
                }
            }
        }
        return false;
    }
}
