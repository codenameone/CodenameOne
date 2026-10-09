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
import static org.junit.Assert.assertNull;
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
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.SceneInput;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Display;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.GestureEvent;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;

/// The swipe the scene makes out of a quick drag, the touch types that
/// are there to be registered, the one screen, and the flow of text.
public class TouchAndTextFlowTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> log = new ArrayList<String>();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
        SceneInput.setTouchInput(0);
    }

    /// On a touch screen every pointer event is a touch first: pressed,
    /// moved and released, one point, delivered to what the finger came
    /// down on wherever it went, each ahead of the mouse event JavaFX
    /// synthesises from it.
    @Test
    public void aFingerFiresATouchAheadOfEveryMouseEvent() {
        SceneInput.setTouchInput(1);
        assertTrue(Platform.isSupported(ConditionalFeature.INPUT_TOUCH));
        assertFalse(Platform.isSupported(ConditionalFeature.INPUT_MULTITOUCH));
        Pane root = new Pane();
        root.setPrefSize(200, 200);
        Pane inner = new Pane();
        inner.resizeRelocate(40, 40, 50, 50);
        inner.setStyle("-fx-background-color: red;");
        root.getChildren().add(inner);
        final List<TouchEvent> seen = new ArrayList<TouchEvent>();
        inner.addEventHandler(TouchEvent.ANY, e -> {
            seen.add(e);
            log.add(e.getEventType().getName() + " " + e.getTouchPoint().getState() + " "
                    + e.getTouchPoint().getX() + "," + e.getTouchPoint().getY() + " scene "
                    + e.getTouchPoint().getSceneX() + "," + e.getTouchPoint().getSceneY() + " n" + e.getTouchCount());
        });
        inner.setOnMousePressed(e -> log.add("mouse pressed"));
        inner.setOnMouseDragged(e -> log.add("mouse dragged"));
        inner.setOnMouseReleased(e -> log.add("mouse released"));
        Scene scene = new Scene(root, 200, 200);
        scene.cn1Layout(200, 200);
        inner.resizeRelocate(40, 40, 50, 50);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 50, 60, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, 150, 160, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 150, 160, MouseButton.PRIMARY);
        assertEquals("[TOUCH_PRESSED PRESSED 10.0,20.0 scene 50.0,60.0 n1, mouse pressed,"
                + " TOUCH_MOVED MOVED 110.0,120.0 scene 150.0,160.0 n1, mouse dragged,"
                + " TOUCH_RELEASED RELEASED 110.0,120.0 scene 150.0,160.0 n1, mouse released]", log.toString());
        assertEquals(1, seen.get(0).getTouchPoint().getId());
        assertSame(inner, seen.get(2).getTouchPoint().getTarget());
        assertTrue(seen.get(1).getEventSetId() > seen.get(0).getEventSetId());
        // A pointer that only moves is no finger on the screen.
        log.clear();
        scene.cn1Pointer(MouseEvent.MOUSE_MOVED, 50, 60, MouseButton.NONE);
        assertEquals("[]", log.toString());
    }

    private Scene swipeScene() {
        Pane root = new Pane();
        root.setPrefSize(400, 400);
        root.setOnSwipeUp(e -> log.add("up " + e.getSceneX() + "," + e.getSceneY() + " " + e.getTouchCount()));
        root.setOnSwipeDown(e -> log.add("down"));
        root.setOnSwipeLeft(e -> log.add("left"));
        root.setOnSwipeRight(e -> log.add("right"));
        root.setOnMouseReleased(e -> log.add("released"));
        Scene scene = new Scene(root, 400, 400);
        scene.cn1Layout(400, 400);
        return scene;
    }

    private static void drag(Scene scene, double x1, double y1, double x2, double y2) {
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, x1, y1, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, (x1 + x2) / 2, (y1 + y2) / 2, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, x2, y2, MouseButton.PRIMARY);
    }

    @Test
    public void aQuickDragAlongOneAxisIsASwipeAfterTheRelease() {
        Scene scene = swipeScene();
        drag(scene, 200, 300, 205, 100);
        assertEquals("[released, up 200.0,300.0 1]", log.toString());
        log.clear();
        drag(scene, 200, 100, 190, 300);
        drag(scene, 300, 200, 100, 210);
        drag(scene, 100, 200, 300, 190);
        assertEquals("[released, down, released, left, released, right]", log.toString());
    }

    @Test
    public void aShortOrDiagonalOrSecondaryDragIsNoSwipe() {
        Scene scene = swipeScene();
        drag(scene, 200, 200, 230, 200);
        drag(scene, 100, 100, 300, 300);
        drag(scene, 100, 100, 300, 220);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 100, 100, MouseButton.SECONDARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 300, 100, MouseButton.SECONDARY);
        assertEquals("[released, released, released, released]", log.toString());
    }

    @Test
    public void swipeHandlersAreProperties() {
        Pane p = new Pane();
        assertNull(p.getOnSwipeLeft());
        javafx.event.EventHandler<SwipeEvent> h = e -> log.add("fired " + e.getEventType());
        p.setOnSwipeLeft(h);
        assertSame(h, p.getOnSwipeLeft());
        assertSame(h, p.onSwipeLeftProperty().get());
        p.fireEvent(new SwipeEvent(SwipeEvent.SWIPE_LEFT, 1, 2, 3, 4, false, false, false, false, true, 2, null));
        assertEquals("[fired SWIPE_LEFT]", log.toString());
        p.onSwipeLeftProperty().set(null);
        assertNull(p.getOnSwipeLeft());
    }

    @Test
    public void swipeEventTypesAndCopies() {
        assertSame(GestureEvent.ANY, SwipeEvent.ANY.getSuperType());
        assertSame(InputEvent.ANY, GestureEvent.ANY.getSuperType());
        assertSame(SwipeEvent.ANY, SwipeEvent.SWIPE_DOWN.getSuperType());
        Rectangle r = new Rectangle(10, 10, 50, 50);
        r.setLayoutX(100);
        Pane root = new Pane(r);
        Scene scene = new Scene(root, 300, 300);
        scene.cn1Layout(300, 300);
        SwipeEvent e = new SwipeEvent(SwipeEvent.SWIPE_RIGHT, 120, 30, 500, 600, true, false, true, false, true, 3,
                null);
        assertEquals(120, e.getX(), 0);
        assertEquals(3, e.getTouchCount());
        assertTrue(e.isShiftDown());
        assertTrue(e.isAltDown());
        assertFalse(e.isShortcutDown());
        assertFalse(e.isInertia());
        assertTrue(e.isDirect());
        SwipeEvent copy = e.copyFor(r, r);
        assertSame(r, copy.getSource());
        assertEquals(20, copy.getX(), 1e-9);
        assertEquals(30, copy.getY(), 1e-9);
        assertEquals(120, copy.getSceneX(), 0);
        assertEquals(500, copy.getScreenX(), 0);
        assertEquals(600, copy.getScreenY(), 0);
        assertSame(SwipeEvent.SWIPE_UP, e.copyFor(r, r, SwipeEvent.SWIPE_UP).getEventType());
    }

    @Test
    public void touchHandlersRegisterAndAMouseFiresNoTouch() {
        SceneInput.setTouchInput(-1);
        assertFalse(Platform.isSupported(ConditionalFeature.INPUT_TOUCH));
        assertFalse(Platform.isSupported(ConditionalFeature.INPUT_MULTITOUCH));
        assertTrue(Platform.isSupported(ConditionalFeature.INPUT_POINTER));
        assertTrue(Platform.isSupported(ConditionalFeature.CONTROLS));
        assertTrue(Platform.isSupported(ConditionalFeature.FXML));
        assertFalse(Platform.isSupported(ConditionalFeature.SCENE3D));
        assertFalse(Platform.isSupported(ConditionalFeature.MEDIA));
        assertFalse(Platform.isSupported(null));
        Pane root = new Pane();
        root.setPrefSize(200, 200);
        root.setOnTouchPressed(e -> log.add("touch " + e.getTouchPoint().getState() + " " + e.getTouchCount()));
        root.setOnTouchReleased(e -> log.add("touch released"));
        root.setOnTouchMoved(e -> log.add("touch moved"));
        root.setOnTouchStationary(e -> log.add("touch stationary"));
        root.setOnMousePressed(e -> log.add("mouse"));
        Scene scene = new Scene(root, 200, 200);
        scene.cn1Layout(200, 200);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 50, 50, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 50, 50, MouseButton.PRIMARY);
        assertEquals("[mouse]", log.toString());
        log.clear();
        TouchPoint point = new TouchPoint(7, TouchPoint.State.PRESSED, 50, 60, 150, 160, root, null);
        List<TouchPoint> points = new ArrayList<TouchPoint>();
        points.add(point);
        TouchEvent event = new TouchEvent(TouchEvent.TOUCH_PRESSED, point, points, 4, false, true, false, false);
        root.fireEvent(event);
        assertEquals("[touch PRESSED 1]", log.toString());
        assertEquals(7, point.getId());
        assertEquals(4, event.getEventSetId());
        assertTrue(event.isControlDown());
        assertTrue(point.belongsTo(root));
        assertTrue(point.belongsTo(scene));
        assertFalse(point.belongsTo(new Pane()));
        assertNull(point.getGrabbed());
        point.grab();
        assertSame(root, point.getGrabbed());
        point.ungrab();
        assertNull(point.getGrabbed());
        assertSame(TouchEvent.ANY, TouchEvent.TOUCH_MOVED.getSuperType());
        EventType<TouchEvent> type = event.copyFor(root, root, TouchEvent.TOUCH_RELEASED).getEventType();
        assertSame(TouchEvent.TOUCH_RELEASED, type);
    }

    @Test
    public void theScreenIsTheDisplayInLogicalPixels() {
        Screen s = Screen.getPrimary();
        Display d = Display.getInstance();
        Rectangle2D b = s.getBounds();
        assertEquals(d.getDisplayWidth() / 2.0, b.getWidth(), 1e-9);
        assertEquals(d.getDisplayHeight() / 2.0, b.getHeight(), 1e-9);
        assertEquals(b, s.getVisualBounds());
        assertEquals(1, Screen.getScreens().size());
        assertSame(s, Screen.getScreens().get(0));
        assertEquals(2, s.getOutputScaleX(), 0);
        assertEquals(2, s.getOutputScaleY(), 0);
        assertEquals(96, s.getDpi(), 0);
        assertEquals(1, Screen.getScreensForRectangle(0, 0, 10, 10).size());
        assertEquals(0, Screen.getScreensForRectangle(-50, -50, 10, 10).size());
    }

    @Test
    public void aFontFileIsNotLoadedAndTheHintIsOnlyRecorded() {
        assertNull(Font.loadFont("file:/nowhere/Some.ttf", 12));
        assertNull(Font.loadFont((java.io.InputStream) null, 12));
        Stage stage = new Stage();
        assertNull(stage.getFullScreenExitHint());
        stage.setFullScreenExitHint("");
        assertEquals("", stage.getFullScreenExitHint());
        assertEquals("", stage.fullScreenExitHintProperty().get());
    }

    private static double width(Text t) {
        return Fonts.width(t.getFont(), t.getText());
    }

    @Test
    public void textFlowsOnOneBaselineAndBreaksAtALineFeed() {
        Text big = new Text("2048");
        big.setFont(Font.font(40));
        Text small = new Text("FX");
        small.setFont(Font.font(12));
        Text feed = new Text(" Game\n");
        Text next = new Text("second line");
        TextFlow flow = new TextFlow(big, small, feed, next);
        flow.setPadding(new Insets(10, 0, 0, 5));
        double bigLine = Fonts.lineHeight(big.getFont());
        double line = Fonts.lineHeight(next.getFont());
        double first = width(big) + width(small) + Fonts.width(feed.getFont(), " Game");
        assertEquals(5 + Math.max(first, width(next)), flow.prefWidth(-1), 1.01);
        assertEquals(10 + bigLine + line, flow.prefHeight(-1), 1.01);
        flow.resize(600, 300);
        flow.layout();
        double baseline = big.getLayoutY() + big.getBaselineOffset() + big.getLayoutBounds().getMinY();
        assertEquals(baseline, small.getLayoutY() + small.getLayoutBounds().getMinY() + small.getBaselineOffset(),
                1.01);
        assertEquals(5, big.getBoundsInParent().getMinX(), 0.51);
        assertEquals(5 + width(big), small.getBoundsInParent().getMinX(), 1.01);
        assertEquals(5 + width(big) + width(small), feed.getBoundsInParent().getMinX(), 1.01);
        assertEquals(5, next.getBoundsInParent().getMinX(), 0.51);
        assertEquals(10 + bigLine, next.getBoundsInParent().getMinY(), 1.01);
        assertEquals(10 + Fonts.ascent(big.getFont()), flow.getBaselineOffset(), 1.01);
    }

    @Test
    public void textFlowWrapsWholePiecesAndAlignsEachLine() {
        Text a = new Text("alpha ");
        Text b = new Text("beta ");
        Text c = new Text("gamma");
        TextFlow flow = new TextFlow(a, b, c);
        double line = Fonts.lineHeight(a.getFont());
        double narrow = width(a) + width(b) + 2;
        assertEquals(2 * line, flow.prefHeight(narrow), 1.01);
        assertEquals(3 * line, flow.prefHeight(width(c) + 1), 1.01);
        assertEquals(line, flow.prefHeight(-1), 1.01);
        flow.setTextAlignment(TextAlignment.RIGHT);
        flow.resize(narrow, 100);
        flow.layout();
        assertEquals(narrow - width(a) - width(b), a.getBoundsInParent().getMinX(), 1.01);
        assertEquals(narrow - width(c), c.getBoundsInParent().getMinX(), 1.01);
        assertEquals(line, c.getBoundsInParent().getMinY(), 1.01);
        flow.setTextAlignment(TextAlignment.CENTER);
        flow.layout();
        assertEquals((narrow - width(c)) / 2, c.getBoundsInParent().getMinX(), 1.01);
        flow.setLineSpacing(7);
        flow.layout();
        assertEquals(line + 7, c.getBoundsInParent().getMinY(), 1.01);
        assertEquals(TextAlignment.CENTER, flow.textAlignmentProperty().get());
        assertEquals(7, flow.lineSpacingProperty().get(), 0);
        assertEquals(0, new TextFlow().prefHeight(-1), 0);
    }

    @Test
    public void textFlowSizesAControlBesideText() {
        Text a = new Text("label ");
        Pane box = new Pane();
        box.setPrefSize(30, 50);
        TextFlow flow = new TextFlow(a, box);
        assertEquals(width(a) + 30, flow.prefWidth(-1), 1.01);
        // The box stands on the baseline; the descent of the text hangs below it.
        double descent = Fonts.lineHeight(a.getFont()) - Fonts.ascent(a.getFont());
        assertEquals(50 + descent, flow.prefHeight(-1), 1.01);
        flow.resize(300, 100);
        flow.layout();
        assertEquals(30, box.getWidth(), 0);
        assertEquals(50, box.getHeight(), 0);
        assertEquals(0, box.getLayoutY(), 0.51);
        assertEquals(50 - Fonts.ascent(a.getFont()), a.getBoundsInParent().getMinY(), 1.01);
    }

    /// A group gives a resizable child its preferred size when it is asked
    /// for its own bounds. The peer of the child asked the group while it
    /// was placing itself, with the child's bounds from before in hand,
    /// and stayed empty: a text flow in a group, shown after the scene was
    /// laid out once, drew nothing.
    @Test
    public void aRegionInAGroupAddedToAShownSceneHasAPeerOfItsSize() {
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 300);
        scene.cn1Layout(400, 300);
        root.cn1Peer();
        javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow(new javafx.scene.text.Text("some words"));
        root.getChildren().setAll(new javafx.scene.Group(flow));
        scene.cn1Layout(400, 300);
        org.junit.Assert.assertTrue(flow.getWidth() > 0);
        org.junit.Assert.assertEquals(com.codename1.fxcompat.runtime.Units.toPixels(flow.getWidth()),
                flow.cn1Peer().getWidth(), 1);
        org.junit.Assert.assertEquals(com.codename1.fxcompat.runtime.Units.toPixels(flow.getHeight()),
                flow.cn1Peer().getHeight(), 1);
    }

    /// A pane does not clip its children. Codename One clips a container
    /// to its bounds, so a tile pane taller than the pane it is in lost
    /// the rows below the pane's edge. What the children of a pane may
    /// paint in is what the pane itself may: the area of its parent.
    @Test
    public void aPaneLetsItsChildrenPaintBeyondItsBounds() {
        javafx.scene.layout.Pane small = new javafx.scene.layout.Pane();
        small.setMaxSize(40, 30);
        small.setPrefSize(40, 30);
        javafx.scene.layout.Pane tall = new javafx.scene.layout.Pane();
        tall.setPrefSize(40, 120);
        small.getChildren().add(tall);
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(small);
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 300);
        scene.cn1Layout(400, 300);
        org.junit.Assert.assertEquals(30, small.getHeight(), 0.5);
        org.junit.Assert.assertEquals(120, tall.getHeight(), 0.5);
        org.junit.Assert.assertFalse(com.codename1.fxcompat.runtime.ParentPeer.clipsChildren(small));
        org.junit.Assert.assertFalse(com.codename1.fxcompat.runtime.ParentPeer.clipsChildren(root));
        org.junit.Assert.assertFalse(
                com.codename1.fxcompat.runtime.ParentPeer.clipsChildren(new javafx.scene.Group()));
        // Under a scaled parent the clip of the screen cannot be handed
        // down, and a pane is left with the one it has.
        org.junit.Assert.assertFalse(com.codename1.fxcompat.runtime.ParentPeer.underTransform(small));
        root.setScaleX(0.5);
        org.junit.Assert.assertTrue(com.codename1.fxcompat.runtime.ParentPeer.underTransform(small));
        org.junit.Assert.assertFalse(com.codename1.fxcompat.runtime.ParentPeer.underTransform(root));
        // The content of a scroll pane or a list stays inside it.
        org.junit.Assert.assertTrue(com.codename1.fxcompat.runtime.ParentPeer
                .clipsChildren(new javafx.scene.control.ScrollPane()));
        org.junit.Assert.assertTrue(com.codename1.fxcompat.runtime.ParentPeer
                .clipsChildren(new javafx.scene.control.ListView<String>()));
    }
}
