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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.SceneEmbed;
import com.codename1.fxcompat.runtime.StageDialog;
import com.codename1.fxcompat.runtime.StageForm;
import com.codename1.fxcompat.runtime.StageHosts;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/// JavaFX inside a Codename One application that has a main class of its
/// own (library mode): a scene graph embedded in a form of the
/// application, stages shown over its forms and closed back to them, and
/// a menu bar reachable inside either. No lifecycle of the layer runs in
/// any of these, so nothing installs an exit hook: the application must
/// survive every one of them.
public class FxInteropTest {

    static {
        // The rule runs a test on the event thread only once Codename One
        // is up, and a form shown from another thread is not current when
        // show() returns.
        HeadlessImplementation.install();
    }

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private boolean implicitExit;

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
        implicitExit = Platform.isImplicitExit();
        StageHosts.setExitHook(null);
    }

    @After
    public void tearDown() {
        List<javafx.stage.Window> open = new ArrayList<javafx.stage.Window>(javafx.stage.Window.getWindows());
        for (int i = 0; i < open.size(); i++) {
            open.get(i).hide();
        }
        Units.setScale(0);
        Platform.setImplicitExit(implicitExit);
        FxInterop.setNativeWindows(true);
    }

    private static final class Count implements EventHandler<ActionEvent> {
        int n;

        @Override
        public void handle(ActionEvent event) {
            n++;
        }
    }

    private static Form home(String title) {
        Form f = new Form(title, new BorderLayout());
        f.show();
        return f;
    }

    private static List<Object[]> paint(Form f) {
        HeadlessImplementation.drawnText.clear();
        HeadlessImplementation.recordText = true;
        try {
            Image target = Image.createImage(HeadlessImplementation.WIDTH, HeadlessImplementation.HEIGHT);
            f.paintComponent(target.getGraphics());
            return new ArrayList<Object[]>(HeadlessImplementation.drawnText);
        } finally {
            HeadlessImplementation.recordText = false;
            HeadlessImplementation.drawnText.clear();
        }
    }

    private static boolean drew(List<Object[]> text, String wanted) {
        for (int i = 0; i < text.size(); i++) {
            Object[] entry = text.get(i);
            for (int j = 0; j < entry.length; j++) {
                if (wanted.equals(entry[j])) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Presses and releases on the form, at a point of `node`: what a touch
    /// on the screen does.
    private static void click(Form f, Component hosted, Node node) {
        Point2D p = node.localToScene(4, 4);
        int x = hosted.getAbsoluteX() + Units.toPixels(p.getX());
        int y = hosted.getAbsoluteY() + Units.toPixels(p.getY());
        f.pointerPressed(x, y);
        f.pointerReleased(x, y);
    }

    @Test
    public void aSceneGraphIsAComponentOfACodenameOneForm() {
        Label label = new Label("Embedded label");
        Button button = new Button("Press");
        Count clicks = new Count();
        button.setOnAction(clicks);
        VBox box = new VBox(8, label, button);
        box.setPrefSize(120, 90);

        Component hosted = FxInterop.asComponent(box);
        assertTrue(hosted instanceof SceneEmbed);
        assertEquals("the preferred size is the root's, in device pixels", 240, hosted.getPreferredW());
        assertEquals(180, hosted.getPreferredH());
        assertNotNull(box.getScene());
        assertNotNull("a scene has a window to answer for it", box.getScene().getWindow());
        assertTrue(box.getScene().getWindow().cn1Embedded());
        assertTrue(box.getScene().getWindow().isShowing());
        assertEquals("which is not one of the application's windows", 0, javafx.stage.Window.getWindows().size());

        Form form = home("Host");
        form.add(BorderLayout.CENTER, hosted);
        form.revalidate();
        assertSame(form, Display.getInstance().getCurrent());
        assertTrue(hosted.getWidth() > 0 && hosted.getHeight() > 0);
        assertEquals("laid out to the size Codename One gave the component", Units.toLogical(hosted.getWidth()),
                box.getWidth(), 0.51);
        assertEquals(Units.toLogical(hosted.getHeight()), box.getHeight(), 0.51);
        assertTrue(button.getWidth() > 0 && button.getHeight() > 0);

        List<Object[]> text = paint(form);
        assertTrue("the graph paints with the form", drew(text, "Embedded label"));
        assertTrue(drew(text, "Press"));

        click(form, hosted, button);
        assertEquals("pointer input on the form reaches the graph", 1, clicks.n);

        // It cannot be closed from inside, and closing it ends nothing.
        box.getScene().getWindow().hide();
        assertTrue(box.getScene().getWindow().isShowing());
        click(form, hosted, button);
        assertEquals(2, clicks.n);

        // Off the screen it takes no more input, and takes it again later.
        Form other = home("Other");
        other.pointerPressed(hosted.getAbsoluteX() + 10, hosted.getAbsoluteY() + 10);
        other.pointerReleased(hosted.getAbsoluteX() + 10, hosted.getAbsoluteY() + 10);
        assertEquals(2, clicks.n);
        form.show();
        click(form, hosted, button);
        assertEquals(3, clicks.n);
    }

    @Test
    public void aSceneKeepsItsOwnSettingsWhenEmbedded() {
        VBox box = new VBox(new Label("Styled"));
        Scene scene = new Scene(box, 100, 60);
        scene.getStylesheets().add("app.css");
        Component hosted = FxInterop.asComponent(scene);
        assertSame(scene, box.getScene());
        assertEquals(1, scene.getStylesheets().size());
        assertSame("a root that has a scene is shown with it", scene,
                ((SceneEmbed) FxInterop.asComponent(box)).window().getScene());
        assertNotNull(hosted);
        try {
            FxInterop.asComponent(null);
            fail("nothing");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            FxInterop.asComponent("a string");
            fail("neither a scene nor a root");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void aMenuBarIsReachableInAnEmbeddedScene() {
        Menu file = new Menu("File");
        MenuItem open = new MenuItem("Open");
        Count opened = new Count();
        open.setOnAction(opened);
        file.getItems().add(open);
        MenuBar bar = new MenuBar(file);
        VBox box = new VBox(bar, new Label("Body"));
        Component hosted = FxInterop.asComponent(box);
        Form form = home("Menus");
        form.add(BorderLayout.CENTER, hosted);
        form.revalidate();

        assertTrue("the bar is drawn in the scene, on every form factor", drew(paint(form), "File"));
        List<Node> headers = bar.getChildrenUnmodifiable();
        assertEquals(1, headers.size());
        click(form, hosted, headers.get(0));
        assertTrue("a touch on its text opens the menu", file.isShowing());
        open.fire();
        assertEquals(1, opened.n);
        file.hide();
        assertFalse(file.isShowing());
    }

    @Test
    public void aStageShownFromACodenameOneFormClosesBackToIt() {
        Form app = home("Application");
        Platform.setImplicitExit(true);
        Menu file = new Menu("File");
        file.getItems().add(new MenuItem("Open"));
        MenuBar bar = new MenuBar(file);
        Stage stage = new Stage();
        stage.setTitle("Details");
        stage.setScene(new Scene(new VBox(bar, new Label("In a stage")), 200, 120));
        stage.show();
        assertTrue(stage.isShowing());
        Form shown = Display.getInstance().getCurrent();
        assertTrue("a stage is a form over the application's", shown instanceof StageForm);
        shown.revalidate();
        List<Object[]> text = paint(shown);
        assertTrue(drew(text, "In a stage"));
        assertTrue("its menu bar is drawn in the scene", drew(text, "File"));
        assertNotNull("the form has a way back", shown.getBackCommand());

        shown.getBackCommand().actionPerformed(new com.codename1.ui.events.ActionEvent(shown));
        assertFalse(stage.isShowing());
        assertSame("closing the last stage returns to the application, which keeps running", app,
                Display.getInstance().getCurrent());

        // Platform.exit() is the lifecycle's to honour, and there is none.
        Platform.exit();
        assertSame(app, Display.getInstance().getCurrent());
    }

    @Test
    public void anAlertFloatsOverTheApplicationsFormAndAnswers() {
        Form app = home("Application");
        FxInterop.setNativeWindows(false);
        final Alert alert = new Alert(AlertType.CONFIRMATION, "Sure?");
        final List<String> log = new ArrayList<String>();
        // The dialog is on screen once its transition ran, which is after
        // the work that was queued when showAndWait() was called.
        Display.getInstance().setTimeout(50, new Runnable() {
            private int tries;

            @Override
            public void run() {
                boolean up = Display.getInstance().getCurrent() instanceof StageDialog;
                if (!up && ++tries < 100) {
                    Display.getInstance().setTimeout(50, this);
                    return;
                }
                log.add("dialog=" + up);
                Node ok = alert.getDialogPane().lookupButton(ButtonType.OK);
                if (ok instanceof Button) {
                    ((Button) ok).fire();
                }
            }
        });
        Optional<ButtonType> answer = alert.showAndWait();
        assertEquals("[dialog=true]", log.toString());
        assertTrue(answer.isPresent());
        assertSame(ButtonType.OK, answer.get());
        assertSame(app, Display.getInstance().getCurrent());
    }
}
