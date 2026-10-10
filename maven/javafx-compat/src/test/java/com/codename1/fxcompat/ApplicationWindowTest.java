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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.StageForm;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Image;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/// The primary stage in the application's own window on a desktop: the
/// window takes the stage's title and size, and the first frame painted
/// for a display size is followed by another.
public class ApplicationWindowTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private boolean implicit;
    private Stage stage;

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
        implicit = Platform.isImplicitExit();
        Platform.setImplicitExit(false);
        // A form an earlier test class left showing is not a stage's, and
        // the first stage shown over it would be a guest in its window,
        // which sizes nothing: whichever test ran first here failed. A
        // stage shown and hidden leaves a stage's form behind instead.
        Stage first = new Stage();
        first.cn1MarkPrimary();
        first.setScene(new Scene(new Pane(), 10, 10));
        first.show();
        first.hide();
        HeadlessImplementation.windowTitle = null;
        HeadlessImplementation.windowSize = null;
    }

    @After
    public void tearDown() {
        if (stage != null && stage.isShowing()) {
            stage.hide();
        }
        Platform.setImplicitExit(implicit);
        Units.setScale(0);
        HeadlessImplementation.setDesktop(false);
        HeadlessImplementation.nativeTitle = false;
        HeadlessImplementation.windowTitle = null;
        HeadlessImplementation.windowSize = null;
        HeadlessImplementation.windowFrame = null;
    }

    private static void desktop() {
        HeadlessImplementation.setDesktop(true);
        HeadlessImplementation.nativeTitle = true;
    }

    private StageForm primary(String title, Scene scene) {
        stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
        return (StageForm) stage.cn1Host();
    }

    /// The port pushes a title only when asked to, and Codename One asks
    /// only for a title set on the form that is showing. A stage is
    /// titled before it is shown, and the primary one has no title area.
    @Test
    public void theTitleGivenBeforeShowingReachesTheWindow() {
        desktop();
        primary("Ledger", new Scene(new Pane(), 320, 200));
        assertEquals("Ledger", HeadlessImplementation.windowTitle);
        stage.setTitle("Ledger - accounts.txt");
        assertEquals("Ledger - accounts.txt", HeadlessImplementation.windowTitle);
    }

    @Test
    public void theWindowIsAskedForTheSizeOfTheSceneInDevicePixels() {
        desktop();
        primary("Ledger", new Scene(new Pane(), 320, 200));
        assertArrayEquals(new int[] {Units.sizeToPixels(320), Units.sizeToPixels(200)},
                HeadlessImplementation.windowSize);
    }

    /// The size of a scene is that of what is drawn in the window, and a
    /// port is asked for the size of the window: the frame goes on top.
    @Test
    public void theFrameOfTheWindowIsAddedToTheSizeOfAScene() {
        desktop();
        HeadlessImplementation.windowFrame = new int[] {2, 25};
        primary("Ledger", new Scene(new Pane(), 320, 200));
        assertArrayEquals(new int[] {Units.sizeToPixels(320) + 2, Units.sizeToPixels(200) + 25},
                HeadlessImplementation.windowSize);
    }

    /// A window that already shows a display of the size of the scene is
    /// left alone. Mandelbrot, whose scene is the 800 by 600 the native
    /// Linux window opens at, was resized to that size less the frame, took
    /// the resize in its first frame for one made by the user, and kept a
    /// black picture.
    @Test
    public void aSceneTheDisplayAlreadyFitsResizesNothing() {
        desktop();
        HeadlessImplementation.windowFrame = new int[] {2, 25};
        primary("Ledger", new Scene(new Pane(), Units.toLogical(HeadlessImplementation.WIDTH),
                Units.toLogical(HeadlessImplementation.HEIGHT)));
        assertNull(HeadlessImplementation.windowSize);
    }

    /// The size of a stage is its window's: nothing is added to it.
    @Test
    public void aSizeGivenToTheStageIsTheWindowsFrameIncluded() {
        desktop();
        HeadlessImplementation.windowFrame = new int[] {2, 25};
        stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setScene(new Scene(new Pane(), 320, 200));
        stage.setWidth(500);
        stage.setHeight(400);
        stage.show();
        assertArrayEquals(new int[] {Units.sizeToPixels(500), Units.sizeToPixels(400)},
                HeadlessImplementation.windowSize);
    }

    @Test
    public void aSizeGivenToTheStageWins() {
        desktop();
        stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setScene(new Scene(new Pane(), 320, 200));
        stage.setWidth(500);
        stage.setHeight(400);
        stage.show();
        assertArrayEquals(new int[] {Units.sizeToPixels(500), Units.sizeToPixels(400)},
                HeadlessImplementation.windowSize);
    }

    /// Off a desktop the display is the screen: nothing resizes it, and a
    /// title has no window to go to.
    @Test
    public void aScreenIsNeitherResizedNorTitled() {
        primary("Ledger", new Scene(new Pane(), 320, 200));
        assertNull(HeadlessImplementation.windowSize);
        assertNull(HeadlessImplementation.windowTitle);
    }

    /// The native Linux port shows an empty buffer after the first frame
    /// of a window whose size it has just changed, so the first paint for
    /// a display size asks for one more; a paint for the same size does
    /// not, or the form would paint for ever.
    @Test
    public void theFirstPaintForADisplaySizeAsksForAnother() {
        StageForm form = primary("Ledger", new Scene(new Pane(), 320, 200));
        Image target = Image.createImage(HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
        form.paintComponent(target.getGraphics());
        assertEquals(1, form.cn1PaintedAgain());
        form.paintComponent(target.getGraphics());
        form.paintComponent(target.getGraphics());
        assertEquals(1, form.cn1PaintedAgain());
    }
}
