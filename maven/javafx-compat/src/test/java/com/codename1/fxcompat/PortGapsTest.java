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
import com.codename1.fxcompat.runtime.StyleEngine;
import com.codename1.fxcompat.runtime.Units;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/// Gaps found porting a real JavaFX application to the layer.
public class PortGapsTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
        StyleEngine.setInstance(null);
    }

    /// JavaFX tells the change listeners of a list property about a change
    /// of the list's content, with the list as old and new value.
    @Test
    public void listPropertyListenersHearContentChanges() {
        ObservableList<String> list = FXCollections.observableArrayList();
        SimpleListProperty<String> p = new SimpleListProperty<String>(list);
        final List<Object> seen = new ArrayList<Object>();
        final int[] invalid = new int[1];
        p.addListener((javafx.beans.value.ChangeListener<ObservableList<String>>) (o, a, b) -> {
            seen.add(a);
            seen.add(b);
        });
        p.addListener((javafx.beans.InvalidationListener) o -> invalid[0]++);
        list.add("x");
        assertEquals(2, seen.size());
        assertSame(list, seen.get(0));
        assertSame(list, seen.get(1));
        assertEquals(1, invalid[0]);
    }

    /// A cell's graphic whose visibility the application bound is laid out
    /// without setting it.
    @Test
    public void cellLeavesABoundGraphicVisibilityAlone() {
        ListCell<String> cell = new ListCell<String>();
        HBox box = new HBox();
        box.visibleProperty().bind(cell.itemProperty().isNotNull());
        cell.setGraphic(box);
        StackPane root = new StackPane(cell);
        new Scene(root, 100, 40);
        root.layout();
        cell.requestLayout();
        cell.layout();
        assertTrue(box.visibleProperty().isBound());
    }

    @Test
    public void mapFlatMapAndOrElse() {
        SimpleStringProperty s = new SimpleStringProperty("ab");
        ObservableValue<Integer> len = s.map(String::length);
        final int[] changes = new int[1];
        len.addListener((o, a, b) -> changes[0]++);
        assertEquals(Integer.valueOf(2), len.getValue());
        s.set("abc");
        assertEquals(Integer.valueOf(3), len.getValue());
        assertEquals(1, changes[0]);
        s.set(null);
        assertNull(len.getValue());
        assertEquals(Integer.valueOf(-1), len.orElse(-1).getValue());

        SimpleStringProperty inner = new SimpleStringProperty("in");
        ObjectProperty<SimpleStringProperty> outer = new SimpleObjectProperty<SimpleStringProperty>(inner);
        ObservableValue<String> flat = outer.flatMap(x -> x);
        final int[] flatChanges = new int[1];
        flat.addListener((o, a, b) -> flatChanges[0]++);
        inner.set("changed");
        assertEquals("changed", flat.getValue());
        assertEquals(1, flatChanges[0]);
        outer.set(null);
        assertNull(flat.getValue());
    }

    @Test
    public void insetsPropertyFollowsPadding() {
        Region r = new Region();
        assertEquals(Insets.EMPTY, r.insetsProperty().get());
        final int[] changes = new int[1];
        r.insetsProperty().addListener((o, a, b) -> changes[0]++);
        r.setPadding(new Insets(3, 4, 5, 6));
        assertEquals(new Insets(3, 4, 5, 6), r.insetsProperty().get());
        assertEquals(1, changes[0]);
    }

    /// `-fx-fit-to-width` and the bar policies reach a scroll pane from CSS.
    @Test
    public void scrollPaneTakesItsPropertiesFromCss() {
        Region content = new Region();
        content.setPrefSize(500, 50);
        ScrollPane pane = new ScrollPane(content);
        pane.setStyle("-fx-fit-to-width: true; -fx-hbar-policy: never; -fx-pannable: true");
        StackPane root = new StackPane(pane);
        new Scene(root, 200, 100);
        pane.applyCss();
        assertTrue(pane.isFitToWidth());
        assertTrue(pane.isPannable());
        assertEquals(ScrollPane.ScrollBarPolicy.NEVER, pane.getHbarPolicy());
        root.layout();
        assertTrue("content " + content.getWidth(), content.getWidth() <= 200);
    }

    @Test
    public void imageViewTakesFitSizeFromCss() {
        ImageView view = new ImageView();
        view.setStyle("-fx-fit-width: 40; -fx-fit-height: 30; -fx-preserve-ratio: true");
        StackPane root = new StackPane(view);
        new Scene(root, 200, 100);
        view.applyCss();
        assertEquals(40, view.getFitWidth(), 0);
        assertEquals(30, view.getFitHeight(), 0);
        assertTrue(view.isPreserveRatio());
    }
}
