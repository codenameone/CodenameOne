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

    /// A flow of tiles is as tall as its width makes it. Asked how tall it
    /// has to be with no width named, it answers for the width it would
    /// like -- one row -- and a scene judged by that fits any screen. In
    /// the screen's width the tiles are many rows, taller than the pane,
    /// and the pane that centres them put the first rows above its top
    /// and the last below its bottom, neither in reach. The minimum is
    /// asked for the width the scene gets, and the pane scrolls.
    @Test
    public void aFlowTallerThanTheFormInItsWidthStartsAtTheTopAndScrolls() {
        int w = Display.getInstance().getDisplayWidth();
        int h = Display.getInstance().getDisplayHeight();
        javafx.scene.layout.FlowPane flow = new javafx.scene.layout.FlowPane();
        // One row at the width it would like.
        flow.setPrefWrapLength(100000);
        int tile = w / 3;
        int tiles = 3 * (h / tile + 2);
        for (int i = 0; i < tiles; i++) {
            flow.getChildren().add(new Rectangle(tile - 1, tile));
        }
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(flow);
        assertTrue(root.minHeight(-1) < h);
        Container content = shown(root);
        assertTrue("taller than the form in its width", root.minHeight(content.getWidth()) > content.getHeight());
        assertFalse(content.isScrollableX());
        assertTrue(content.isScrollableY());
        assertEquals(content.getWidth(), root.getWidth(), 0.01);
        assertEquals(root.minHeight(content.getWidth()), root.getHeight(), 1);
        assertEquals("the first row is at the top", 0, flow.getBoundsInParent().getMinY(), 0.01);
        assertTrue(content.getScrollDimension().getHeight() > content.getHeight());
    }

    /// A form whose fields ask for more columns than the screen has: the
    /// field, told to take whatever room there is, is what gives way, and
    /// the label before it keeps every letter. The field had its preferred
    /// width for a minimum, so the label lost all that was missing and
    /// read "P..." beside a field with room to spare.
    @Test
    public void aLabelKeepsItsTextBesideAFieldWiderThanTheScreen() {
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(8);
        javafx.scene.control.Label label = new javafx.scene.control.Label("Password:");
        javafx.scene.control.TextField field = new javafx.scene.control.TextField("secret");
        field.setPrefColumnCount(200);
        grid.add(label, 0, 0);
        grid.add(field, 1, 0);
        javafx.scene.layout.GridPane.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.control.Label boxed = new javafx.scene.control.Label("Planet:");
        javafx.scene.control.TextField inBox = new javafx.scene.control.TextField("Earth");
        inBox.setPrefColumnCount(200);
        javafx.scene.layout.HBox.setHgrow(inBox, javafx.scene.layout.Priority.ALWAYS);
        // And a field nothing told to grow: it has no one right width
        // either, where the label beside it has exactly one.
        javafx.scene.layout.GridPane plain = new javafx.scene.layout.GridPane();
        javafx.scene.control.Label owner = new javafx.scene.control.Label("Owner:");
        javafx.scene.control.TextField named = new javafx.scene.control.TextField("Ada");
        named.setPrefColumnCount(200);
        plain.add(owner, 0, 0);
        plain.add(named, 1, 0);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(grid, new javafx.scene.layout.HBox(8, boxed, inBox),
                plain);
        Container content = shown(root);
        assertTrue("wider than the screen", field.prefWidth(-1) > content.getWidth());
        assertFalse(content.isScrollableX());
        assertEquals(label.prefWidth(-1), label.getWidth(), 0.5);
        assertEquals(boxed.prefWidth(-1), boxed.getWidth(), 0.5);
        assertEquals(owner.prefWidth(-1), owner.getWidth(), 0.5);
        assertEquals(content.getWidth(), named.getBoundsInParent().getMaxX(), 1);
        assertEquals(content.getWidth(), field.getBoundsInParent().getMaxX(), 1);
        assertEquals(content.getWidth(), inBox.getBoundsInParent().getMaxX(), 1);
    }

    /// Three buttons under a theme that puts a wide margin beside the text
    /// of a button, as the themes of phones do: together wider than the
    /// screen, though their words would fit it twice. Each gives up
    /// padding, and no letter.
    @Test
    public void buttonsTooWideForTheScreenGiveUpPaddingBeforeLetters() {
        java.util.Hashtable<String, Object> theme = new java.util.Hashtable<String, Object>();
        theme.put("Button.padding", "10,10,160,160");
        theme.put("Button.padUnit", new byte[] {0, 0, 0, 0});
        com.codename1.ui.plaf.UIManager.getInstance().addThemeProps(theme);
        try {
            javafx.scene.control.Button[] buttons = {new javafx.scene.control.Button("Disabled"),
                new javafx.scene.control.Button("Cancel"), new javafx.scene.control.Button("OK")};
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8, buttons);
            Container content = shown(new javafx.scene.layout.VBox(row));
            double asked = row.prefWidth(-1);
            assertTrue("wider than the screen: " + asked, asked > content.getWidth());
            assertFalse(content.isScrollableX());
            for (int i = 0; i < buttons.length; i++) {
                com.codename1.ui.Label n = (com.codename1.ui.Label) buttons[i].cn1Native();
                com.codename1.ui.plaf.Style s = n.getUnselectedStyle();
                int room = n.getWidth() - s.getHorizontalPadding();
                assertTrue(n.getText() + " has " + room + " for its text",
                        room >= s.getFont().stringWidth(n.getText()));
                // Shares of the missing width are whole pixels; what is
                // left of it after the last share is under one each.
                assertTrue("inside the screen: " + buttons[i].getBoundsInParent(),
                        buttons[i].getBoundsInParent().getMaxX() <= content.getWidth() + buttons.length);
            }
            // What they ask for is what they asked before giving anything up.
            content.getComponentForm().revalidate();
            assertEquals(asked, row.prefWidth(-1), 0.5);
        } finally {
            // The theme is the display's, and the classes after this one
            // draw their controls with it.
            com.codename1.ui.plaf.UIManager.getInstance().setThemeProps(
                    new java.util.Hashtable<String, Object>());
        }
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
