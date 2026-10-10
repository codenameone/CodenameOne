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

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Units;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/// A group's layout bounds, asked before anything laid it out.
public class GroupBoundsTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    private static HBox fixed(double w, double h) {
        HBox box = new HBox();
        box.setMinSize(w, h);
        box.setPrefSize(w, h);
        box.setMaxSize(w, h);
        return box;
    }

    /// 2048FX builds its board in a constructor, reads the group's bounds on
    /// the next line and scales the game by them: bounds of zero there are
    /// a scale of infinity and an empty window.
    @Test
    public void aGroupOfRegionsHasTheirPreferredSizeBeforeAnyLayout() {
        VBox column = new VBox();
        column.getChildren().addAll(fixed(300, 90), fixed(300, 300));
        Group inner = new Group(column);
        Group outer = new Group(inner);
        Bounds b = outer.getLayoutBounds();
        assertEquals(300, b.getWidth(), 0.01);
        assertEquals(390, b.getHeight(), 0.01);
        assertEquals(300, column.getWidth(), 0.01);
    }

    @Test
    public void aGroupThatDoesNotSizeItsChildrenLeavesThemAlone() {
        HBox box = fixed(120, 40);
        Group group = new Group();
        group.setAutoSizeChildren(false);
        group.getChildren().add(box);
        assertEquals(0, group.getLayoutBounds().getWidth(), 0.01);
        assertEquals(0, box.getWidth(), 0.01);
    }

    @Test
    public void shapesNeedNoLayoutAndAChildAddedLaterIsCounted() {
        Group group = new Group(new Rectangle(10, 20, 30, 40));
        assertEquals(30, group.getLayoutBounds().getWidth(), 0.01);
        group.getChildren().add(fixed(200, 10));
        assertEquals(200, group.getLayoutBounds().getWidth(), 0.01);
        assertEquals(60, group.getLayoutBounds().getHeight(), 0.01);
    }
}
