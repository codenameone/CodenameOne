/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
import static org.junit.Assert.assertTrue;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/// Scrolling moves the content; it does not lay it out or style it again.
public class ScrollWithoutRelayoutTest {
    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private int layouts;

    @Before
    public void setUp() {
        HeadlessImplementation.install();
    }

    private int rowLayouts;

    private final class Rows extends VBox {
        @Override
        protected void layoutChildren() {
            layouts++;
            super.layoutChildren();
        }
    }

    private final class Row extends Label {
        Row(String text) {
            super(text);
        }

        @Override
        protected void layoutChildren() {
            rowLayouts++;
            super.layoutChildren();
        }
    }

    @Test
    public void draggingAndSettingTheValueOnlyMoveTheContent() {
        Rows rows = new Rows();
        for (int i = 0; i < 60; i++) {
            rows.getChildren().add(new Row("Row " + i));
        }
        ScrollPane pane = new ScrollPane(rows);
        pane.setPannable(true);
        Scene scene = new Scene(pane, 300, 300);
        scene.cn1Layout(300, 300);
        scene.cn1Layout(300, 300);
        layouts = 0;
        rowLayouts = 0;
        double before = rows.getLayoutY();

        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 150, 250, MouseButton.PRIMARY);
        for (int i = 1; i <= 10; i++) {
            scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, 150, 250 - 15 * i, MouseButton.PRIMARY);
            scene.cn1Layout(300, 300);
        }
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 150, 100, MouseButton.PRIMARY);
        for (int i = 1; i <= 10; i++) {
            pane.setVvalue(i / 10.0);
            scene.cn1Layout(300, 300);
        }
        assertTrue("the content moved", rows.getLayoutY() < before);
        assertEquals("the content is not laid out again", 0, layouts);
        assertEquals("nor are its rows", 0, rowLayouts);
    }

    @Test
    public void draggingAListMovesItsRowsWithoutLayingOutWhatHoldsIt() {
        ListView<String> list = new ListView<String>();
        for (int i = 0; i < 200; i++) {
            list.getItems().add("Item " + i);
        }
        Rows holder = new Rows();
        holder.getChildren().add(list);
        Scene scene = new Scene(holder, 300, 300);
        scene.cn1Layout(300, 300);
        scene.cn1Layout(300, 300);
        layouts = 0;
        scene.cn1Pointer(MouseEvent.MOUSE_PRESSED, 150, 250, MouseButton.PRIMARY);
        for (int i = 1; i <= 10; i++) {
            scene.cn1Pointer(MouseEvent.MOUSE_DRAGGED, 150, 250 - 15 * i, MouseButton.PRIMARY);
            scene.cn1Layout(300, 300);
        }
        scene.cn1Pointer(MouseEvent.MOUSE_RELEASED, 150, 100, MouseButton.PRIMARY);
        assertEquals("what holds the list is not laid out again", 0, layouts);
    }
}
