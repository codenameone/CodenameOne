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
}
