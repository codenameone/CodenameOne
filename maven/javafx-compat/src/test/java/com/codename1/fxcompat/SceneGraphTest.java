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
import static org.junit.Assert.assertNotNull;
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
import com.codename1.fxcompat.runtime.FxLifecycle;
import com.codename1.fxcompat.runtime.StageForm;
import com.codename1.fxcompat.runtime.StyleEngine;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/// The core of the scene graph: tree, geometry, peers, events, focus,
/// stages and the styling hooks.
public class SceneGraphTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
        StyleEngine.setInstance(null);
    }

    private static Region box(double w, double h) {
        Region r = new Region();
        r.setPrefSize(w, h);
        return r;
    }

    @Test
    public void childrenKeepOneParentAndFollowTheScene() {
        Pane a = new Pane();
        Pane b = new Pane();
        Region child = box(10, 10);
        a.getChildren().add(child);
        assertSame(a, child.getParent());
        b.getChildren().add(child);
        assertSame(b, child.getParent());
        assertTrue(a.getChildren().isEmpty());
        Scene scene = new Scene(b, 100, 100);
        assertSame(scene, child.getScene());
        b.getChildren().remove(child);
        assertNull(child.getScene());
        assertNull(child.getParent());
        assertTrue(b.getStyleClass().contains("root"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateChildIsRejected() {
        Pane p = new Pane();
        Region child = box(1, 1);
        p.getChildren().addAll(child, child);
    }

    @Test
    public void paneSizesChildrenAndPeersFollowInDevicePixels() {
        Pane root = new Pane();
        Region child = box(30, 20);
        child.setLayoutX(5);
        child.setLayoutY(7);
        root.getChildren().add(child);
        root.resize(200, 100);
        root.layout();
        assertEquals(30, child.getWidth(), 0);
        assertEquals(20, child.getHeight(), 0);
        Component peer = child.cn1Peer();
        assertEquals(10, peer.getX());
        assertEquals(14, peer.getY());
        assertEquals(60, peer.getWidth());
        assertEquals(40, peer.getHeight());
        child.setTranslateX(1.5);
        assertEquals(13, peer.getX());
        assertTrue(root.cn1Peer() instanceof Container);
        assertSame(root.cn1Peer(), peer.getParent());
        root.getChildren().clear();
        assertNull(peer.getParent());
    }

    @Test
    public void peerOrderFollowsChildOrder() {
        Pane root = new Pane();
        Region a = box(1, 1);
        Region b = box(1, 1);
        Region c = box(1, 1);
        root.getChildren().addAll(a, b, c);
        Container peer = (Container) root.cn1Peer();
        assertEquals(3, peer.getComponentCount());
        a.toFront();
        assertSame(b.cn1Peer(), peer.getComponentAt(0));
        assertSame(a.cn1Peer(), peer.getComponentAt(2));
        c.toBack();
        assertSame(c.cn1Peer(), peer.getComponentAt(0));
        root.getChildren().remove(b);
        assertEquals(2, peer.getComponentCount());
    }

    @Test
    public void regionSizingHonoursOverrides() {
        Region r = new Region();
        r.setPadding(new Insets(1, 2, 3, 4));
        assertEquals(6, r.prefWidth(-1), 0);
        assertEquals(4, r.prefHeight(-1), 0);
        r.setPrefWidth(50);
        r.setMinWidth(Region.USE_PREF_SIZE);
        r.setMaxHeight(40);
        assertEquals(50, r.minWidth(-1), 0);
        assertEquals(Double.MAX_VALUE, r.maxWidth(-1), 0);
        assertEquals(40, r.maxHeight(-1), 0);
        r.autosize();
        assertEquals(50, r.getWidth(), 0);
    }

    @Test
    public void groupBoundsAreTheUnionOfItsChildren() {
        Region a = box(10, 10);
        Region b = box(10, 10);
        b.setLayoutX(30);
        b.setLayoutY(-5);
        Group g = new Group(a, b);
        g.layout();
        Bounds lb = g.getLayoutBounds();
        assertEquals(0, lb.getMinX(), 0);
        assertEquals(-5, lb.getMinY(), 0);
        assertEquals(40, lb.getWidth(), 0);
        assertEquals(15, lb.getHeight(), 0);
        b.setLayoutX(50);
        assertEquals(60, g.getLayoutBounds().getWidth(), 0);
    }

    @Test
    public void coordinatesConvertThroughScaleAndRotation() {
        Pane root = new Pane();
        Region child = box(20, 10);
        child.setLayoutX(100);
        child.setLayoutY(50);
        root.getChildren().add(child);
        root.resize(300, 300);
        root.layout();
        Point2D p = child.localToScene(0, 0);
        assertEquals(100, p.getX(), 1e-9);
        assertEquals(50, p.getY(), 1e-9);
        child.setScaleX(2);
        assertEquals(90, child.localToScene(0, 0).getX(), 1e-9);
        child.setScaleX(1);
        child.setRotate(90);
        Point2D corner = child.localToParent(0, 0);
        assertEquals(115, corner.getX(), 1e-9);
        assertEquals(45, corner.getY(), 1e-9);
        Point2D back = child.sceneToLocal(corner.getX(), corner.getY());
        assertEquals(0, back.getX(), 1e-9);
        assertEquals(0, back.getY(), 1e-9);
        Bounds inParent = child.getBoundsInParent();
        assertEquals(10, inParent.getWidth(), 1e-9);
        assertEquals(20, inParent.getHeight(), 1e-9);
    }

    @Test
    public void lookupMatchesIdClassTypeAndDescendants() {
        Pane root = new Pane();
        Pane inner = new Pane();
        inner.setId("inner");
        Region leaf = box(1, 1);
        leaf.getStyleClass().add("leaf");
        inner.getChildren().add(leaf);
        root.getChildren().add(inner);
        assertSame(inner, root.lookup("#inner"));
        assertSame(leaf, root.lookup(".leaf"));
        assertSame(leaf, root.lookup("#inner .leaf"));
        assertSame(leaf, root.lookup("Region.leaf"));
        assertNull(root.lookup("#inner #inner"));
        assertEquals(2, root.lookupAll("Pane").size());
    }

    @Test
    public void disableIsInherited() {
        Pane root = new Pane();
        Region leaf = box(1, 1);
        root.getChildren().add(leaf);
        root.setDisable(true);
        assertTrue(leaf.isDisabled());
        assertFalse(leaf.isDisable());
        assertTrue(leaf.getPseudoClassStates().contains(PseudoClass.getPseudoClass("disabled")));
        root.setDisable(false);
        assertFalse(leaf.isDisabled());
    }

    private static final class Log implements EventHandler<Event> {
        final List<String> entries;
        final String name;
        boolean consume;

        Log(List<String> entries, String name) {
            this.entries = entries;
            this.name = name;
        }

        @Override
        public void handle(Event event) {
            entries.add(name + ":" + event.getEventType().getName());
            if (consume) {
                event.consume();
            }
        }
    }

    @Test
    public void mouseEventsCaptureThenBubbleAndFiltersCanConsume() {
        List<String> log = new ArrayList<String>();
        Pane root = new Pane();
        Pane mid = new Pane();
        mid.setPrefSize(100, 100);
        Region leaf = box(50, 50);
        mid.getChildren().add(leaf);
        root.getChildren().add(mid);
        Scene scene = new Scene(root, 200, 200);
        scene.cn1Layout(200, 200);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, new Log(log, "sceneF"));
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, new Log(log, "rootF"));
        Log midFilter = new Log(log, "midF");
        mid.addEventFilter(MouseEvent.MOUSE_PRESSED, midFilter);
        leaf.addEventHandler(MouseEvent.MOUSE_PRESSED, new Log(log, "leafH"));
        leaf.setOnMousePressed(new Log(log, "leafOn"));
        mid.addEventHandler(MouseEvent.MOUSE_PRESSED, new Log(log, "midH"));
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, new Log(log, "sceneH"));

        assertFalse(scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 10, 10, MouseButton.PRIMARY));
        assertEquals("[sceneF:MOUSE_PRESSED, rootF:MOUSE_PRESSED, midF:MOUSE_PRESSED, leafH:MOUSE_PRESSED, "
                + "leafOn:MOUSE_PRESSED, midH:MOUSE_PRESSED, sceneH:MOUSE_PRESSED]", log.toString());
        assertTrue(leaf.isPressed());
        assertTrue(mid.isPressed());
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 10, 10, MouseButton.PRIMARY);
        assertFalse(leaf.isPressed());

        log.clear();
        midFilter.consume = true;
        assertTrue(scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 10, 10, MouseButton.PRIMARY));
        assertEquals("[sceneF:MOUSE_PRESSED, rootF:MOUSE_PRESSED, midF:MOUSE_PRESSED]", log.toString());
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 10, 10, MouseButton.PRIMARY);
    }

    @Test
    public void clickHoverAndLocalCoordinates() {
        final List<String> log = new ArrayList<String>();
        Pane root = new Pane();
        final Region leaf = box(50, 50);
        leaf.setLayoutX(20);
        leaf.setLayoutY(30);
        root.getChildren().add(leaf);
        Scene scene = new Scene(root, 200, 200);
        scene.cn1Layout(200, 200);
        leaf.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                log.add("click " + e.getX() + "," + e.getY() + " scene " + e.getSceneX() + " n=" + e.getClickCount()
                        + " src=" + (e.getSource() == leaf));
            }
        });
        leaf.setOnMouseEntered(new Log(log, "in"));
        leaf.setOnMouseExited(new Log(log, "out"));
        root.setOnMouseEntered(new Log(log, "rootIn"));
        scene.cn1Pointer(MouseEvent.MOUSE_MOVED, 25, 35, MouseButton.NONE);
        assertTrue(leaf.isHover());
        assertTrue(root.isHover());
        assertEquals("[rootIn:MOUSE_ENTERED, in:MOUSE_ENTERED]", log.toString());
        log.clear();
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 25, 35, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 25, 35, MouseButton.PRIMARY);
        assertEquals("[click 5.0,5.0 scene 25.0 n=1 src=true]", log.toString());
        log.clear();
        scene.cn1Pointer(MouseEvent.MOUSE_MOVED, 150, 150, MouseButton.NONE);
        assertFalse(leaf.isHover());
        assertEquals("[out:MOUSE_EXITED]", log.toString());

        log.clear();
        leaf.setDisable(true);
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 25, 35, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 25, 35, MouseButton.PRIMARY);
        assertTrue(log.toString(), log.isEmpty());
        leaf.setDisable(false);
        leaf.setMouseTransparent(true);
        final boolean[] rootClicked = new boolean[1];
        root.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                rootClicked[0] = true;
            }
        });
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 25, 35, MouseButton.PRIMARY);
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 25, 35, MouseButton.PRIMARY);
        assertTrue(rootClicked[0]);
        // The pointer leaves the leaf once it turns transparent; it is
        // never clicked.
        assertEquals("[out:MOUSE_EXITED]", log.toString());
    }

    @Test
    public void keysGoToTheFocusOwnerAndTabTraverses() {
        final List<String> log = new ArrayList<String>();
        Pane root = new Pane();
        Region a = box(10, 10);
        Region b = box(10, 10);
        a.setFocusTraversable(true);
        b.setFocusTraversable(true);
        root.getChildren().addAll(a, b);
        Scene scene = new Scene(root, 100, 100);
        a.requestFocus();
        assertTrue(a.isFocused());
        assertSame(a, scene.getFocusOwner());
        a.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                log.add("a " + e.getCode());
            }
        });
        scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent e) {
                log.add("scene " + e.getCode());
            }
        });
        scene.cn1Key(KeyEvent.KEY_PRESSED, KeyCode.A, "a");
        assertEquals("[a A, scene A]", log.toString());
        scene.cn1Key(KeyEvent.KEY_PRESSED, KeyCode.TAB, "");
        assertSame(b, scene.getFocusOwner());
        assertFalse(a.isFocused());
        scene.cn1Key(KeyEvent.KEY_PRESSED, KeyCode.TAB, "");
        assertSame(a, scene.getFocusOwner());
        root.getChildren().remove(a);
        assertNull(scene.getFocusOwner());
    }

    @Test
    public void stageShowsItsSceneInAFormAndLaysItOut() {
        Pane root = new Pane();
        Region child = box(40, 40);
        root.getChildren().add(child);
        Stage stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setScene(new Scene(root));
        final List<String> log = new ArrayList<String>();
        stage.setOnShown(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                log.add("shown");
            }
        });
        stage.setOnHidden(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent e) {
                log.add("hidden");
            }
        });
        boolean implicit = Platform.isImplicitExit();
        Platform.setImplicitExit(false);
        try {
            stage.show();
            assertTrue(stage.isShowing());
            assertTrue(javafx.stage.Window.getWindows().contains(stage));
            assertTrue(Display.getInstance().getCurrent() instanceof StageForm);
            StageForm form = (StageForm) Display.getInstance().getCurrent();
            form.revalidate();
            assertSame(stage, root.getScene().getWindow());
            assertEquals(Units.toLogical(root.cn1Peer().getWidth()), root.getWidth(), 0.01);
            assertTrue(root.getWidth() > 0);
            assertEquals(40, child.getWidth(), 0);
            assertEquals(80, child.cn1Peer().getWidth());
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent e) {
                    e.consume();
                }
            });
            assertFalse(stage.cn1CloseRequested());
            assertTrue(stage.isShowing());
            stage.setOnCloseRequest(null);
            assertTrue(stage.cn1CloseRequested());
            assertFalse(stage.isShowing());
            assertEquals("[shown, hidden]", log.toString());
        } finally {
            Platform.setImplicitExit(implicit);
        }
    }

    @Test
    public void lifecycleDrivesTheApplication() {
        final List<String> log = new ArrayList<String>();
        final Application app = new Application() {
            @Override
            public void init() {
                log.add("init");
            }

            @Override
            public void start(Stage primaryStage) {
                log.add("start primary=" + primaryStage.cn1IsPrimary() + " fx=" + Platform.isFxApplicationThread());
            }

            @Override
            public void stop() {
                log.add("stop");
            }
        };
        FxLifecycle lifecycle = new FxLifecycle() {
            @Override
            protected Application createApplication() {
                return app;
            }
        };
        lifecycle.runApp();
        assertSame(app, lifecycle.getApplication());
        assertNotNull(lifecycle.getPrimaryStage());
        lifecycle.destroy();
        lifecycle.destroy();
        assertEquals("[init, start primary=true fx=true, stop]", log.toString());
        com.codename1.fxcompat.runtime.StageHosts.setExitHook(null);
    }

    @Test
    public void buttonFiresActionFromItsNativeComponent() {
        Button button = new Button("Go");
        final int[] fired = new int[1];
        button.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                fired[0]++;
            }
        });
        Component nativeButton = button.cn1Native();
        assertTrue(nativeButton instanceof com.codename1.ui.Button);
        assertEquals("Go", ((com.codename1.ui.Button) nativeButton).getText());
        assertTrue(button.prefWidth(-1) > 0);
        ((com.codename1.ui.Button) nativeButton).pressed();
        ((com.codename1.ui.Button) nativeButton).released();
        assertEquals(1, fired[0]);
        button.setText("Longer text");
        assertEquals("Longer text", ((com.codename1.ui.Button) nativeButton).getText());
        button.setDisable(true);
        assertFalse(nativeButton.isEnabled());
        button.fire();
        assertEquals(1, fired[0]);
        Label label = new Label("x");
        assertFalse(label.isFocusTraversable());
        assertEquals("Label", label.getTypeSelector());
    }

    @Test
    public void styleTargetAppliesAndRestores() {
        Region r = new Region();
        r.setOpacity(0.5);
        assertTrue(r.cn1ApplyStyle("-fx-opacity", Double.valueOf(0.25)));
        assertEquals(0.25, r.getOpacity(), 0);
        assertTrue(r.cn1ApplyStyle("-fx-opacity", Double.valueOf(0.75)));
        assertTrue(r.cn1ApplyStyle("-fx-opacity", null));
        assertEquals(0.5, r.getOpacity(), 0);
        assertFalse(r.cn1ApplyStyle("-fx-no-such-thing", "x"));
        assertFalse(r.cn1ApplyStyle("-fx-opacity", "not a number"));
        assertEquals(0.5, r.getOpacity(), 0);

        Background own = new Background(new BackgroundFill(Color.RED, new CornerRadii(3), Insets.EMPTY));
        r.setBackground(own);
        assertTrue(r.cn1ApplyStyle("-fx-background-color", Color.BLUE));
        assertTrue(r.cn1ApplyStyle("-fx-background-radius", Double.valueOf(8)));
        BackgroundFill styled = r.getBackground().getFills().get(0);
        assertEquals(Color.BLUE, styled.getFill());
        assertEquals(8, styled.getRadii().getTopLeftHorizontalRadius(), 0);
        assertTrue(r.cn1ApplyStyle("-fx-background-color", null));
        assertEquals(Color.RED, r.getBackground().getFills().get(0).getFill());
        assertTrue(r.cn1ApplyStyle("-fx-background-radius", null));
        assertEquals(own, r.getBackground());

        assertTrue(r.cn1ApplyStyle("-fx-border-color", Color.BLACK));
        assertTrue(r.cn1ApplyStyle("-fx-border-width", Double.valueOf(2)));
        assertEquals(2, r.getInsets().getLeft(), 0);
        assertTrue(r.cn1ApplyStyle("-fx-border-style", "dashed"));
        assertTrue(r.cn1ApplyStyle("-fx-padding", new Insets(5)));
        assertEquals(7, r.getInsets().getTop(), 0);
        assertTrue(r.cn1ApplyStyle("-fx-border-color", null));
        assertNull(r.getBorder());

        Label label = new Label("t");
        assertTrue(label.cn1ApplyStyle("-fx-text-fill", Color.GREEN));
        assertEquals(Color.GREEN, label.getTextFill());
        assertTrue(label.cn1ApplyStyle("-fx-font-size", Double.valueOf(20)));
        assertEquals(20, label.getFont().getSize(), 0);
        assertTrue(label.cn1ApplyStyle("-fx-font-size", null));
        assertTrue(label.cn1ApplyStyle("-fx-text-fill", null));
        assertEquals(Color.BLACK, label.getTextFill());
    }

    @Test
    public void styleEngineIsToldWhenANodeMayHaveChanged() {
        final List<String> log = new ArrayList<String>();
        StyleEngine.setInstance(new StyleEngine() {
            @Override
            public void restyle(Node node) {
                log.add(node.getTypeSelector() + (node.getId() == null ? "" : "#" + node.getId()));
            }
        });
        Pane root = new Pane();
        Region leaf = box(1, 1);
        leaf.setId("leaf");
        leaf.getStyleClass().add("ignored-outside-a-scene");
        root.getChildren().add(leaf);
        assertTrue(log.isEmpty());
        Scene scene = new Scene(root, 10, 10);
        assertEquals("[Pane, Region#leaf]", log.toString());
        log.clear();
        leaf.getStyleClass().add("x");
        leaf.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
        leaf.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
        leaf.setStyle("-fx-opacity: 0.5");
        leaf.setId("other");
        assertEquals("[Region#leaf, Region#leaf, Region#leaf, Region#other]", log.toString());
        log.clear();
        scene.getStylesheets().add("app.css");
        assertEquals("[Pane, Region#other]", log.toString());
    }

    /// A JavaFX window is light grey behind its controls: the standard
    /// theme gives the root of a scene the theme's background colour. The
    /// layer left a root with no background, and the white fill of the
    /// scene showed instead. A group has no background, a pane that is
    /// not the root none either, and a sheet's own rule wins.
    @Test
    public void theRootRegionOfASceneHasTheBackgroundOfTheTheme() {
        javafx.scene.layout.Pane inner = new javafx.scene.layout.Pane();
        javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane(inner);
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 200, 100);
        scene.cn1Layout(200, 100);
        assertNotNull(root.getBackground());
        assertEquals(Color.web("#f4f4f4").toString(),
                root.getBackground().getFills().get(0).getFill().toString());
        assertNull(inner.getBackground());

        javafx.scene.layout.StackPane styled = new javafx.scene.layout.StackPane();
        styled.setStyle("-fx-background-color: red;");
        javafx.scene.Scene other = new javafx.scene.Scene(styled, 200, 100);
        other.cn1Layout(200, 100);
        assertEquals(Color.RED, styled.getBackground().getFills().get(0).getFill());

        // No longer the root, no longer grey.
        scene.setRoot(new javafx.scene.Group());
        javafx.scene.layout.StackPane holder = new javafx.scene.layout.StackPane(root);
        javafx.scene.Scene third = new javafx.scene.Scene(holder, 200, 100);
        third.cn1Layout(200, 100);
        assertNull(root.getBackground());
    }
}
