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
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/// What is inside a scaled parent is painted on a port that keeps its clip
/// in the coordinates of the screen while a matrix is installed, which is
/// what the native Linux port does. Codename One paints a child only where
/// its bounds meet the clip; there the two are in different spaces, and
/// 2048, whose whole scene is one scaled group, lost everything but the
/// board.
public class ScreenSpaceClipTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<Paint> filled = new ArrayList<Paint>();
    private Graphics target;
    private int[] clipAtRed;
    private double[] redLands;

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
        HeadlessImplementation.trackClip = true;
        HeadlessImplementation.screenSpaceClip = true;
    }

    @After
    public void tearDown() {
        Renderer.setTrace(null);
        HeadlessImplementation.screenSpaceClip = false;
        HeadlessImplementation.trackClip = false;
        List<javafx.stage.Window> open = new ArrayList<javafx.stage.Window>(javafx.stage.Window.getWindows());
        for (int i = 0; i < open.size(); i++) {
            open.get(i).hide();
        }
        Units.setScale(0);
    }

    private Graphics paint() {
        filled.clear();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                if ("fill".equals(operation)) {
                    filled.add(paint);
                    if (paint == Color.RED && target != null) {
                        // The clip as the port has it, without the translation
                        // the graphics adds to what it is given.
                        clipAtRed = new int[] {target.getClipX() + target.getTranslateX(),
                            target.getClipY() + target.getTranslateY(), target.getClipWidth(),
                            target.getClipHeight()};
                        // And where the port puts the middle of what is drawn.
                        redLands = HeadlessImplementation.onScreen(
                                (deviceBounds[0] + deviceBounds[2]) / 2 + target.getTranslateX(),
                                (deviceBounds[1] + deviceBounds[3]) / 2 + target.getTranslateY());
                    }
                }
            }
        });
        Form f = Display.getInstance().getCurrent();
        f.revalidate();
        Graphics g = Image.createImage(HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT).getGraphics();
        target = g;
        clipAtRed = null;
        redLands = null;
        g.setClip(0, 0, HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
        f.paintComponent(g);
        Renderer.setTrace(null);
        return g;
    }

    /// The red square is 1500 pixels along in the coordinates of the group
    /// that holds it, beyond the 1080 of the screen, and at half size it is
    /// drawn at 750: on the screen, and outside the clip only for whoever
    /// compares the two numbers as they are.
    @Test
    public void aChildOfAScaledParentThatLandsOnTheScreenIsPainted() {
        Rectangle near = new Rectangle(10, 10, 40, 40);
        near.setFill(Color.BLUE);
        Rectangle far = new Rectangle(1500, 10, 100, 100);
        far.setFill(Color.RED);
        Group inner = new Group(near, far);
        Group scaled = new Group(inner);
        scaled.getTransforms().add(new Scale(0.5, 0.5, 0, 0));
        Stage stage = new Stage();
        stage.setScene(new Scene(new Group(scaled), 1000, 400));
        stage.show();

        // The first class to run in a JVM is not on the event dispatch thread
        // yet, and the form is then shown from the queue.
        MainThreadRule.drain();

        Graphics g = paint();
        assertTrue(filled.contains(Color.BLUE));
        assertTrue("a child that lands on the screen under its parent's scale was culled",
                filled.contains(Color.RED));
        // The widened clip is the scaled parent's own business: what paints
        // after it has the clip it had.
        assertEquals(0, g.getClipX());
        assertEquals(0, g.getClipY());
        assertEquals(HeadlessImplementation.WIDTH, g.getClipWidth());
        assertEquals(HeadlessImplementation.HEIGHT, g.getClipHeight());
    }

    /// A node with a clip of its own, which is what every label is. The clip
    /// that holds while it draws has to be where the port draws it: the
    /// layer used to read the clip back, in the coordinates of the screen,
    /// and hand it to the port again as if it were in those of the group.
    /// That put it through the matrix twice, and the text of every tile of
    /// 2048 was cut away.
    @Test
    public void aClippedNodeUnderAScaledParentKeepsItsClipWhereItLands() {
        Rectangle far = new Rectangle(1500, 10, 100, 100);
        far.setFill(Color.RED);
        far.setClip(new Rectangle(1500, 10, 100, 100));
        Group scaled = new Group(new Group(far));
        scaled.getTransforms().add(new Scale(0.5, 0.5, 0, 0));
        Stage stage = new Stage();
        stage.setScene(new Scene(new Group(scaled), 1000, 400));
        stage.show();
        MainThreadRule.drain();

        paint();
        assertTrue(filled.contains(Color.RED));
        assertTrue("clip " + java.util.Arrays.toString(clipAtRed) + " does not hold what is drawn at "
                + java.util.Arrays.toString(redLands),
                clipAtRed != null && clipAtRed[0] <= redLands[0] && clipAtRed[0] + clipAtRed[2] >= redLands[0]
                        && clipAtRed[1] <= redLands[1] && clipAtRed[1] + clipAtRed[3] >= redLands[1]);
    }
}
