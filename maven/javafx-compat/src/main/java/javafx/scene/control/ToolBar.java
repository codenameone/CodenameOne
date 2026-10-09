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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.ui.Component;

import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/// A bar of controls, in a row or in a column.
///
/// The items are laid out one after the other at their preferred sizes,
/// centred across the bar, on the standard theme's bar: a gradient of the
/// base colour over a line of the border colour. A style sheet reaches
/// the bar as `.tool-bar` and sets its spacing with `-fx-spacing` on it.
///
/// Items that do not fit are cut off at the end of the bar: there is no
/// overflow button with a menu of them.
public class ToolBar extends Control {

    private static final int ORIENTATION = Dirty.USER;
    private static final PseudoClass HORIZONTAL = PseudoClass.getPseudoClass("horizontal");
    private static final PseudoClass VERTICAL = PseudoClass.getPseudoClass("vertical");

    private final ObservableList<Node> items = FXCollections.observableArrayList();
    private final ObjectProperty<Orientation> orientation = new FxObject<Orientation>(this, "orientation",
            Orientation.HORIZONTAL, ORIENTATION | Dirty.LAYOUT);
    private final HBox row = new HBox();
    private final VBox column = new VBox();
    private double spacing = 4;

    /// Creates a bar without items.
    public ToolBar() {
        this((Node[]) null);
    }

    /// Creates a bar of items.
    public ToolBar(Node... items) {
        getStyleClass().add("tool-bar");
        setFocusTraversable(false);
        pseudoClassStateChanged(HORIZONTAL, true);
        row.setAlignment(Pos.CENTER_LEFT);
        column.setAlignment(Pos.TOP_CENTER);
        this.items.addListener((ListChangeListener<Node>) change -> rebuild());
        if (items != null) {
            this.items.addAll(items);
        }
        rebuild();
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    @Override
    public String cn1DefaultStyle() {
        return "-fx-background-color: -fx-box-border, linear-gradient(to bottom, derive(-fx-base, 46.9%),"
                + " derive(-fx-base, -2.1%)); -fx-background-insets: 0, 0 0 1 0;"
                + " -fx-padding: 0.416667em 0.833333em 0.416667em 0.833333em; -fx-spacing: 0.333em;";
    }

    private void rebuild() {
        boolean upright = getOrientation() == Orientation.VERTICAL;
        Pane holder = upright ? column : row;
        Pane other = upright ? row : column;
        other.getChildren().clear();
        holder.getChildren().setAll(items);
        row.setSpacing(spacing);
        column.setSpacing(spacing);
        if (cn1Made() != holder) {
            cn1MadeOf(holder);
        }
        requestLayout();
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & ORIENTATION) != 0) {
            boolean upright = getOrientation() == Orientation.VERTICAL;
            pseudoClassStateChanged(VERTICAL, upright);
            pseudoClassStateChanged(HORIZONTAL, !upright);
            rebuild();
        }
        super.cn1Invalidated(what);
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-spacing".equals(property)) {
            return Double.valueOf(spacing);
        }
        return super.cn1StyleValue(property);
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-spacing".equals(property)) {
            if (value instanceof Number) {
                spacing = ((Number) value).doubleValue();
                row.setSpacing(spacing);
                column.setSpacing(spacing);
            }
            return true;
        }
        return super.cn1SetStyleValue(property, value);
    }

    /// A bar is as long as it is given room for.
    @Override
    protected double computeMaxWidth(double height) {
        return getOrientation() == Orientation.VERTICAL ? computePrefWidth(height) : Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return getOrientation() == Orientation.VERTICAL ? Double.MAX_VALUE : computePrefHeight(width);
    }

    /// A bar may be made shorter than its items, which are then cut off.
    @Override
    protected double computeMinWidth(double height) {
        return getOrientation() == Orientation.VERTICAL ? computePrefWidth(height) : 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return getOrientation() == Orientation.VERTICAL ? 0 : computePrefHeight(width);
    }

    /// Returns the items of the bar, in the order they are shown.
    public final ObservableList<Node> getItems() {
        return items;
    }

    /// Returns whether the bar is a row or a column.
    public final Orientation getOrientation() {
        return orientation.get();
    }

    /// Makes the bar a row or a column.
    public final void setOrientation(Orientation value) {
        orientation.set(value == null ? Orientation.HORIZONTAL : value);
    }

    /// Whether the bar is a row or a column.
    public final ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }
}
