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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.SceneInput;
import com.codename1.fxcompat.runtime.StageForm;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Display;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/// Input as a port delivers it: through the form on screen, at the
/// position of the pointer on the display, with nothing told to the
/// scene or to a control directly.
///
/// The tests here exist because an application passed every test that
/// fired its events at the node they were meant for, and was dead under a
/// real pointer: where a node is drawn and where its Codename One peer
/// lies are two different places once a parent is scaled, and a key
/// pressed before the first click had nowhere to go.
public class PortInputTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private boolean implicit;
    private Stage stage;
    private StageForm form;
    private final List<String> seen = new ArrayList<String>();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
        implicit = Platform.isImplicitExit();
        Platform.setImplicitExit(false);
    }

    @After
    public void tearDown() {
        if (stage != null && stage.isShowing()) {
            stage.hide();
        }
        Platform.setImplicitExit(implicit);
        Units.setScale(0);
    }

    private void show(Parent root) {
        stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setScene(new Scene(root));
        stage.show();
        form = (StageForm) stage.cn1Host();
        form.revalidate();
    }

    /// A click as the display delivers it, at a point of the scene.
    private void click(double sceneX, double sceneY) {
        int x = form.getContentPane().getAbsoluteX() + Units.toPixels(sceneX);
        int y = form.getContentPane().getAbsoluteY() + Units.toPixels(sceneY);
        form.pointerPressed(new int[] {x}, new int[] {y});
        form.pointerReleased(new int[] {x}, new int[] {y});
        // A native component fires its listeners on the event thread.
        MainThreadRule.drain();
    }

    private void press(double sceneX, double sceneY) {
        int x = form.getContentPane().getAbsoluteX() + Units.toPixels(sceneX);
        int y = form.getContentPane().getAbsoluteY() + Units.toPixels(sceneY);
        form.pointerPressed(new int[] {x}, new int[] {y});
    }

    private void release(double sceneX, double sceneY) {
        int x = form.getContentPane().getAbsoluteX() + Units.toPixels(sceneX);
        int y = form.getContentPane().getAbsoluteY() + Units.toPixels(sceneY);
        form.pointerReleased(new int[] {x}, new int[] {y});
        MainThreadRule.drain();
    }

    /// A press on a button that is let go of somewhere else fires nothing,
    /// scaled or not, and leaves the button ready for the next press.
    @Test
    public void aPressLetGoOfOutsideAScaledButtonFiresNothing() {
        Pane root = new Pane();
        Group group = new Group();
        Button a = button("a");
        Button b = button("b");
        b.relocate(360, 360);
        group.getChildren().addAll(a, b);
        group.setScaleX(0.5);
        group.setScaleY(0.5);
        root.getChildren().add(group);
        show(root);
        press(110, 110);
        assertTrue(a.isPressed());
        release(200, 200);
        assertFalse(a.isPressed());
        assertEquals("[]", seen.toString());
        click(110, 110);
        assertEquals("[a]", seen.toString());
    }

    /// A rotated parent: the button is pressed where the rotation puts it.
    @Test
    public void aButtonInARotatedGroupFiresWhereItIsDrawn() {
        Pane root = new Pane();
        Group group = new Group();
        Button a = button("a");
        Button b = button("b");
        b.relocate(360, 360);
        group.getChildren().addAll(a, b);
        group.setRotate(180);
        root.getChildren().add(group);
        show(root);
        // Half a turn about the centre at 200 puts a where b was laid out.
        click(380, 380);
        assertEquals("[a]", seen.toString());
        click(20, 20);
        assertEquals("[a, b]", seen.toString());
    }

    private Button button(final String name) {
        Button b = new Button(name);
        b.setPrefSize(40, 40);
        b.setMinSize(40, 40);
        b.setMaxSize(40, 40);
        b.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                seen.add(name);
            }
        });
        return b;
    }

    @Test
    public void aButtonFiresWhereItIsDrawn() {
        Pane root = new Pane();
        Button b = button("plain");
        b.relocate(100, 100);
        root.getChildren().add(b);
        show(root);
        click(120, 120);
        assertEquals("[plain]", seen.toString());
        click(160, 120);
        assertEquals("[plain]", seen.toString());
    }

    /// A group scaled to half its size about its centre, as a game board
    /// scaled to its window is: the button is drawn at half its size and
    /// nearer the centre, and that is where it is pressed.
    @Test
    public void aButtonInAScaledGroupFiresWhereItIsDrawn() {
        Pane root = new Pane();
        Group group = new Group();
        Button a = button("a");
        a.relocate(0, 0);
        Button b = button("b");
        b.relocate(360, 360);
        group.getChildren().addAll(a, b);
        group.setScaleX(0.5);
        group.setScaleY(0.5);
        root.getChildren().add(group);
        show(root);
        // The group is 400 wide about a centre at 200: a is drawn from 100 to 120, b from 280 to 300.
        click(110, 110);
        assertEquals("[a]", seen.toString());
        click(290, 290);
        assertEquals("[a, b]", seen.toString());
        // Where the peers lie, unscaled, nothing is drawn and nothing fires.
        click(20, 20);
        click(380, 380);
        assertEquals("[a, b]", seen.toString());
    }

    @Test
    public void aButtonMovedByATranslateFiresWhereItIsDrawn() {
        Pane root = new Pane();
        Button b = button("moved");
        b.relocate(100, 100);
        b.setTranslateX(200);
        root.getChildren().add(b);
        show(root);
        click(320, 120);
        assertEquals("[moved]", seen.toString());
        click(120, 120);
        assertEquals("[moved]", seen.toString());
    }

    @Test
    public void aCheckBoxInAScaledGroupIsTickedWhereItIsDrawn() {
        Pane root = new Pane();
        Group group = new Group();
        CheckBox box = new CheckBox("x");
        box.relocate(250, 250);
        Pane filler = new Pane();
        filler.setPrefSize(400, 400);
        filler.setMinSize(400, 400);
        filler.setMouseTransparent(true);
        group.getChildren().addAll(filler, box);
        group.setScaleX(2);
        group.setScaleY(2);
        root.getChildren().add(group);
        show(root);
        filler.resize(400, 400);
        javafx.geometry.Bounds at = box.localToScene(box.getBoundsInLocal());
        assertFalse(box.isSelected());
        click(at.getMinX() + 6, at.getMinY() + at.getHeight() / 2);
        assertTrue(box.isSelected());
    }

    /// A drag as the display delivers it: a press, moves through the
    /// overload a display calls, a release.
    private void drag(double fromX, double fromY, double toX, double toY) {
        int ox = form.getContentPane().getAbsoluteX();
        int oy = form.getContentPane().getAbsoluteY();
        form.pointerPressed(new int[] {ox + Units.toPixels(fromX)}, new int[] {oy + Units.toPixels(fromY)});
        for (int i = 1; i <= 4; i++) {
            double x = fromX + (toX - fromX) * i / 4;
            double y = fromY + (toY - fromY) * i / 4;
            form.pointerDragged(new int[] {ox + Units.toPixels(x)}, new int[] {oy + Units.toPixels(y)});
        }
        form.pointerReleased(new int[] {ox + Units.toPixels(toX)}, new int[] {oy + Units.toPixels(toY)});
        MainThreadRule.drain();
    }

    private Pane swipeBoard() {
        Pane root = new Pane();
        root.setPrefSize(400, 400);
        root.setOnSwipeLeft(new EventHandler<SwipeEvent>() {
            @Override
            public void handle(SwipeEvent event) {
                seen.add("left");
            }
        });
        root.setOnSwipeDown(new EventHandler<SwipeEvent>() {
            @Override
            public void handle(SwipeEvent event) {
                seen.add("down");
            }
        });
        return root;
    }

    /// A finger dragged across the scene is one swipe, once, when it lifts.
    @Test
    public void aFingerDraggedAcrossTheSceneIsOneSwipe() {
        SceneInput.setTouchInput(1);
        try {
            show(swipeBoard());
            drag(300, 200, 100, 205);
            assertEquals("[left]", seen.toString());
            drag(200, 100, 195, 300);
            assertEquals("[left, down]", seen.toString());
            // Too short to be a swipe.
            drag(200, 200, 185, 200);
            assertEquals("[left, down]", seen.toString());
        } finally {
            SceneInput.setTouchInput(0);
        }
    }

    /// A mouse dragged across a JavaFX scene is a drag: no swipe comes of
    /// it, and a game that moves on a swipe stays put.
    @Test
    public void aMouseDraggedAcrossTheSceneIsNoSwipe() {
        SceneInput.setTouchInput(-1);
        try {
            show(swipeBoard());
            drag(300, 200, 100, 205);
            drag(200, 100, 195, 300);
            assertEquals("[]", seen.toString());
        } finally {
            SceneInput.setTouchInput(0);
        }
    }

    /// The application asks for the focus on its root once the stage is
    /// showing; a key pressed before any click goes there.
    @Test
    public void aKeyPressedBeforeAnyClickReachesTheNodeThatAskedForTheFocus() {
        Pane root = new Pane();
        root.getChildren().add(button("b"));
        root.setFocusTraversable(true);
        root.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                seen.add(String.valueOf(event.getCode()));
            }
        });
        show(root);
        root.requestFocus();
        assertSame(root, root.getScene().getFocusOwner());
        int left = Display.getInstance().getKeyCode(Display.GAME_LEFT);
        Display.getInstance().getCurrent().keyPressed(left);
        Display.getInstance().getCurrent().keyReleased(left);
        assertEquals("[LEFT]", seen.toString());
    }
}
