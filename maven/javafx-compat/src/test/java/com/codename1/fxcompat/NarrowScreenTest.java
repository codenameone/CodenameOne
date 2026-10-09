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
import com.codename1.fxcompat.runtime.StageForm;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Container;
import com.codename1.ui.Display;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/// A scene on a screen it was not written for. JavaFX clips what does
/// not fit its window; a phone's window is the screen, so a scene that
/// cannot be laid out that small is laid out at the least it can be and
/// the stage scrolls along the axis that overflows.
public class NarrowScreenTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private boolean implicit;
    private Stage stage;

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

    private Container shown(Parent root) {
        stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setScene(new Scene(root));
        stage.show();
        StageForm form = (StageForm) stage.cn1Host();
        form.revalidate();
        return form.getContentPane();
    }

    @Test
    public void aSceneThatFitsIsTheSizeOfTheFormAndDoesNotScroll() {
        Pane root = new Pane();
        root.setMinSize(100, 100);
        Container content = shown(root);
        assertTrue(content.getWidth() > 100);
        assertEquals(content.getWidth(), root.getWidth(), 0.01);
        assertEquals(content.getHeight(), root.getHeight(), 0.01);
        assertFalse(content.isScrollableX());
        assertFalse(content.isScrollableY());
        // A point of a node on the screen is its point in the scene moved
        // by where the window is; a node in no window has none.
        Pane child = new Pane();
        child.relocate(30, 40);
        root.getChildren().add(child);
        javafx.geometry.Point2D scene = child.localToScene(1, 2);
        javafx.geometry.Point2D screen = child.localToScreen(1, 2);
        assertEquals(scene.getX() + stage.getX() + root.getScene().getX(), screen.getX(), 0.01);
        assertEquals(scene.getY() + stage.getY() + root.getScene().getY(), screen.getY(), 0.01);
        javafx.geometry.Point2D back = child.screenToLocal(screen);
        assertEquals(1, back.getX(), 0.01);
        assertEquals(2, back.getY(), 0.01);
        assertEquals(null, new Pane().localToScreen(0, 0));
    }

    @Test
    public void aRootWiderThanTheFormAtItsLeastIsLaidOutThatWideAndScrolls() {
        Pane root = new Pane();
        int wide = Display.getInstance().getDisplayWidth() + 500;
        root.setMinSize(wide, 100);
        Container content = shown(root);
        assertEquals(wide, root.getWidth(), 0.01);
        assertEquals(wide, root.getScene().getWidth(), 0.01);
        // Only the axis that overflows scrolls; the other is the form's.
        assertEquals(content.getHeight(), root.getHeight(), 0.01);
        assertTrue(content.isScrollableX());
        assertFalse(content.isScrollableY());
        assertEquals(wide, content.getScrollDimension().getWidth());
        // The window is still what is on screen.
        assertEquals(content.getWidth(), stage.getWidth(), 0.01);

        // The scene follows a pan: what is under a point of the screen is
        // what was scrolled there.
        content.scrollRectToVisible(300, 0, content.getWidth(), 10, content);
        assertEquals(300, content.getScrollX());
        assertEquals(-300, root.cn1Peer().getAbsoluteX() - (content.getAbsoluteX() + content.getScrollX()));

        // Made able to fit again, it fits and is back where it started.
        root.setMinSize(100, 100);
        content.getComponentForm().revalidate();
        assertFalse(content.isScrollableX());
        assertEquals(0, content.getScrollX());
        assertEquals(content.getWidth(), root.getWidth(), 0.01);
    }

    @Test
    public void aGroupLargerThanTheFormScrollsOverAllOfIt() {
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        Group root = new Group(new Rectangle(0, 0, w + 200, h + 300));
        Container content = shown(root);
        assertTrue(content.isScrollableX());
        assertTrue(content.isScrollableY());
        assertEquals(w + 200, content.getScrollDimension().getWidth());
        assertEquals(h + 300, content.getScrollDimension().getHeight());
    }

    /// The display hands a form every pointer event as arrays of
    /// coordinates, one element for one finger. A press and a release get
    /// to the plain overloads from there and a drag does not, so a scene
    /// that heard of drags only through the plain overload heard of none
    /// on a device: no swipe, no slider, no divider.
    @Test
    public void aDragDeliveredAsTheDisplayDeliversItReachesTheScene() {
        Pane root = new Pane();
        root.setMinSize(100, 100);
        final java.util.List<String> seen = new java.util.ArrayList<String>();
        javafx.event.EventHandler<javafx.scene.input.MouseEvent> log =
                new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
            @Override
            public void handle(javafx.scene.input.MouseEvent event) {
                seen.add(event.getEventType().getName() + "@" + Math.round(event.getX()));
            }
        };
        root.setOnMousePressed(log);
        root.setOnMouseDragged(log);
        root.setOnMouseReleased(log);
        Container content = shown(root);
        com.codename1.ui.Form form = (com.codename1.ui.Form) stage.cn1Host();
        int x = content.getAbsoluteX() + 20;
        int y = content.getAbsoluteY() + 20;
        form.pointerPressed(new int[] {x}, new int[] {y});
        form.pointerDragged(new int[] {x + 30}, new int[] {y});
        form.pointerReleased(new int[] {x + 30}, new int[] {y});
        assertEquals("[MOUSE_PRESSED@20, MOUSE_DRAGGED@50, MOUSE_RELEASED@50]", seen.toString());
        // Two fingers are a gesture of Codename One's, not a drag of the scene's.
        seen.clear();
        form.pointerDragged(new int[] {x, x + 40}, new int[] {y, y});
        assertEquals("[]", seen.toString());
    }

    /// A pane that holds a group has the size of the group for its
    /// minimum, whatever the application does with the group. One that
    /// scales the group to the pane -- a game board that follows its
    /// window -- fits any pane, and was laid out at the full size of the
    /// board all the same: larger than the screen, scrolling, with every
    /// swipe moving the page and none reaching the game.
    @Test
    public void aRootThatScalesWhatItHoldsToFitIsNotMadeToScroll() {
        final int wide = Display.getInstance().getDisplayWidth() * 2;
        final int tall = Display.getInstance().getDisplayHeight() * 2;
        final javafx.scene.Group board = new javafx.scene.Group(new javafx.scene.shape.Rectangle(wide, tall));
        final javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(board);
        javafx.beans.value.ChangeListener<Number> fit = new javafx.beans.value.ChangeListener<Number>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Number> o, Number before, Number now) {
                double scale = Math.min(root.getWidth() / wide, root.getHeight() / tall);
                board.setScaleX(scale);
                board.setScaleY(scale);
            }
        };
        root.widthProperty().addListener(fit);
        root.heightProperty().addListener(fit);
        assertTrue(root.minWidth(-1) >= wide);
        Container content = shown(root);
        assertFalse(content.isScrollableX());
        assertFalse(content.isScrollableY());
        assertEquals(content.getWidth(), root.getWidth(), 0.01);
        assertEquals(content.getHeight(), root.getHeight(), 0.01);

        // The same board left at its size does not fit, and scrolls.
        javafx.scene.layout.StackPane plain = new javafx.scene.layout.StackPane(
                new javafx.scene.Group(new javafx.scene.shape.Rectangle(wide, 10)));
        stage.hide();
        Container other = shown(plain);
        assertTrue(other.isScrollableX());
        assertFalse(other.isScrollableY());
        assertEquals(wide, plain.getWidth(), 1);
    }

    /// To learn whether a scene has to scroll it is laid out in the pane
    /// first, and then at its minimum. Done on every pass, that moved
    /// every node twice each time, each move asked for another pass, and
    /// a scene wider than the screen was laid out and painted for as long
    /// as it was shown. A pass with nothing changed leaves the scene be.
    @Test
    public void aSceneThatScrollsIsNotLaidOutAgainByEveryPass() {
        final int wide = Display.getInstance().getDisplayWidth() * 2;
        final javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(
                new javafx.scene.Group(new javafx.scene.shape.Rectangle(wide, 10)));
        Container content = shown(root);
        assertTrue(content.isScrollableX());
        assertEquals(wide, root.getWidth(), 1);
        final int[] changes = new int[1];
        root.widthProperty().addListener(new javafx.beans.value.ChangeListener<Number>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Number> o, Number before, Number now) {
                changes[0]++;
            }
        });
        for (int i = 0; i < 3; i++) {
            content.setShouldCalcPreferredSize(true);
            content.getComponentForm().revalidate();
        }
        assertEquals(0, changes[0]);
        assertTrue(content.isScrollableX());
        assertEquals(wide, root.getWidth(), 1);
    }
}
