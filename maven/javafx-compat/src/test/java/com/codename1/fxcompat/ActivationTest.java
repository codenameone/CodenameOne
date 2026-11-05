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
import com.codename1.ui.Component;
import com.codename1.ui.accessibility.AccessibilityAction;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/// A node that handles clicks can be pressed through accessibility -- by a
/// screen reader or the MCP UI tools -- and the press reaches the scene as a
/// tap does.
public class ActivationTest {

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

    private static AccessibilityAction activate(Component peer) {
        for (AccessibilityAction a : peer.getSemantics().getActions()) {
            if (AccessibilityAction.ACTIVATE.equals(a.getId())) {
                return a;
            }
        }
        return null;
    }

    private Pane card;
    private Component peer;
    private final java.util.List<String> seen = new java.util.ArrayList<String>();

    /// A 100x60 card in the middle of a 200x200 scene, holding `child` (if
    /// any) over its centre.
    private void show(javafx.scene.Node child) {
        StackPane root = new StackPane();
        root.setMinSize(200, 200);
        card = child == null ? new Pane() : new StackPane(child);
        card.setMinSize(100, 60);
        card.setMaxSize(100, 60);
        root.getChildren().add(card);
        stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setScene(new Scene(root));
        stage.show();
        ((StageForm) stage.cn1Host()).revalidate();
        peer = card.cn1Peer();
    }

    private EventHandler<MouseEvent> log(final String who) {
        return new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                seen.add(who + ":" + event.getEventType().getName()
                        + (event.isStillSincePress() ? "" : "(moved)"));
            }
        };
    }

    @Test
    public void aClickHandlerMakesTheNodeActivatableAndActivatingItClicksIt() {
        show(null);
        assertEquals("a pane nobody clicks offers no action", null, activate(peer));

        card.setOnMouseClicked(log("card"));
        card.setOnMousePressed(log("card"));
        card.setOnMouseReleased(log("card"));
        AccessibilityAction action = activate(peer);
        assertTrue("a click handler adds the action", action != null);
        assertTrue(action.perform(peer, null));
        assertEquals("[card:MOUSE_PRESSED, card:MOUSE_RELEASED, card:MOUSE_CLICKED]", seen.toString());

        card.setOnMouseClicked(null);
        card.setOnMousePressed(null);
        card.setOnMouseReleased(null);
        assertFalse("removing the handlers takes the action away", activate(peer) != null);
    }

    @Test
    public void aChildOverTheCentreDoesNotTakeTheParentsActivation() {
        Pane inner = new Pane();
        inner.setMinSize(40, 40);
        inner.setMaxSize(40, 40);
        inner.setOnMouseClicked(log("inner"));
        show(inner);
        card.setOnMouseClicked(log("card"));
        assertTrue(activate(peer).perform(peer, null));
        assertEquals("[card:MOUSE_CLICKED]", seen.toString());
    }

    @Test
    public void aHandlerForEveryEventCountsAndAFilterDoesNot() {
        show(null);
        card.addEventFilter(MouseEvent.ANY, log("filter"));
        assertEquals("a filter tracking the mouse is no click handler", null, activate(peer));
        card.addEventHandler(javafx.event.Event.ANY, new EventHandler<javafx.event.Event>() {
            @Override
            public void handle(javafx.event.Event event) {
            }
        });
        assertTrue("a handler for every event sees clicks", activate(peer) != null);
    }

    @Test
    public void aDisabledNodeOffersNoAction() {
        show(null);
        card.setOnMouseClicked(log("card"));
        card.setDisable(true);
        assertEquals(null, activate(peer));
        card.setDisable(false);
        assertTrue(activate(peer) != null);
    }
}
