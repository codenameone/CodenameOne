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

    @Test
    public void aClickHandlerMakesTheNodeActivatableAndActivatingItClicksIt() {
        StackPane root = new StackPane();
        root.setMinSize(200, 200);
        Pane card = new Pane();
        card.setMinSize(100, 60);
        card.setMaxSize(100, 60);
        root.getChildren().add(card);
        stage = new Stage();
        stage.cn1MarkPrimary();
        stage.setScene(new Scene(root));
        stage.show();
        ((StageForm) stage.cn1Host()).revalidate();

        Component peer = card.cn1Peer();
        assertEquals("a pane nobody clicks offers no action", null, activate(peer));

        final int[] clicks = {0};
        card.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                clicks[0]++;
            }
        });
        AccessibilityAction action = activate(peer);
        assertTrue("a click handler adds the action", action != null);
        assertTrue(action.perform(peer, null));
        assertEquals("activating delivers one click to the node", 1, clicks[0]);

        card.setOnMouseClicked(null);
        assertFalse("removing the handler takes the action away", activate(peer) != null);
    }
}
