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

import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.Style;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.paint.Color;

/// One visual of a list or table: a labeled control that shows the item
/// it was last given through [#updateItem(Object, boolean)].
///
/// A view creates few cells and reuses them for whatever rows are
/// visible, so a cell must take everything it shows from the arguments of
/// `updateItem` and clear it when `empty` is true.
///
/// The text is a Codename One `Label`. The graphic is a JavaFX node, a
/// child of the cell, placed at the leading edge with the text after it;
/// `ContentDisplay.GRAPHIC_ONLY` hides the text and `TEXT_ONLY` the
/// graphic, the other values are shown as `LEFT`.
///
/// A selected cell without a background of its own is drawn with a
/// translucent highlight. The pseudo-class states `empty`, `filled` and
/// `selected` follow the cell, and `focused` the control.
///
/// A cell can be edited: [#startEdit()], [#commitEdit(Object)] and
/// [#cancelEdit()] move it in and out of the editing state, and a
/// subclass that edits overrides them to swap what it shows.
///
/// Styled through `cn1ApplyStyle` with the `Labeled` and `Region` names
/// listed in `com.codename1.fxcompat.runtime.StyleTarget`; a cell adds no
/// names of its own.
public class Cell<T> extends Labeled {

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");
    private static final PseudoClass FILLED = PseudoClass.getPseudoClass("filled");
    private static final Color HIGHLIGHT = Color.color(0, 0.59, 0.79, 0.35);

    private final ObjectProperty<T> item = new SimpleObjectProperty<T>(this, "item");
    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(this, "empty", true);
    private final ReadOnlyBooleanWrapper selected = new ReadOnlyBooleanWrapper(this, "selected", false);
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);
    private final ReadOnlyBooleanWrapper editing = new ReadOnlyBooleanWrapper(this, "editing", false);
    private int basePadding = -1;

    /// Creates an empty cell.
    public Cell() {
        setText(null);
        setFocusTraversable(false);
        getStyleClass().add("cell");
        pseudoClassStateChanged(EMPTY, true);
        graphicProperty().addListener(new ChangeListener<Node>() {
            @Override
            public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
                if (oldValue != null) {
                    cn1Children().remove(oldValue);
                }
                if (newValue != null && !cn1Children().contains(newValue)) {
                    cn1Children().add(newValue);
                }
            }
        });
    }

    @Override
    protected Component cn1CreateNative() {
        return new com.codename1.ui.Label();
    }

    private Node shownGraphic() {
        Node g = getGraphic();
        return g == null || getContentDisplay() == ContentDisplay.TEXT_ONLY ? null : g;
    }

    private double graphicWidth() {
        Node g = shownGraphic();
        return g == null ? 0 : g.prefWidth(-1) + getGraphicTextGap();
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c == null) {
            return;
        }
        if (basePadding < 0) {
            basePadding = c.getStyle().getPaddingLeftNoRTL();
        }
        // The text starts after the graphic, which is a node of its own.
        Style style = c.getAllStyles();
        style.setPaddingUnitLeft(Style.UNIT_TYPE_PIXELS);
        style.setPaddingLeft(basePadding + Units.toPixels(graphicWidth()));
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        Node g = getGraphic();
        if (g != null) {
            boolean show = shownGraphic() != null;
            g.setVisible(show);
            if (show) {
                Insets in = getInsets();
                double w = g.prefWidth(-1);
                double h = g.prefHeight(-1);
                double space = getHeight() - in.getTop() - in.getBottom();
                if (g.isResizable()) {
                    g.resize(w, h);
                }
                g.relocate(in.getLeft() + Units.toLogical(Math.max(0, basePadding)) - g.getLayoutBounds().getMinX(),
                        in.getTop() + (space - h) / 2 - g.getLayoutBounds().getMinY());
            }
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        double h = super.computePrefHeight(width);
        Node g = shownGraphic();
        if (g != null) {
            Insets in = getInsets();
            h = Math.max(h, in.getTop() + g.prefHeight(-1) + in.getBottom());
        }
        return h;
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        super.cn1Paint(renderer);
        if (isSelected() && getBackground() == null) {
            renderer.fillRect(0, 0, getWidth(), getHeight(), HIGHLIGHT);
        }
    }

    /// The item this cell shows.
    public final ObjectProperty<T> itemProperty() {
        return item;
    }

    /// Sets the item; a view does, through [#updateItem(Object, boolean)].
    public final void setItem(T value) {
        item.set(value);
    }

    /// Returns the item this cell shows, `null` when empty.
    public final T getItem() {
        return item.get();
    }

    /// Whether this cell shows no item.
    public final ReadOnlyBooleanProperty emptyProperty() {
        return empty.getReadOnlyProperty();
    }

    /// Returns whether this cell shows no item.
    public final boolean isEmpty() {
        return empty.get();
    }

    /// Whether the row of this cell is selected.
    public final ReadOnlyBooleanProperty selectedProperty() {
        return selected.getReadOnlyProperty();
    }

    /// Returns whether the row of this cell is selected.
    public final boolean isSelected() {
        return selected.get();
    }

    /// Whether this cell may be edited; true unless the application says
    /// otherwise.
    public final BooleanProperty editableProperty() {
        return editable;
    }

    /// Sets whether this cell may be edited.
    public final void setEditable(boolean value) {
        editable.set(value);
    }

    /// Returns whether this cell may be edited.
    public final boolean isEditable() {
        return editable.get();
    }

    /// Whether this cell is being edited.
    public final ReadOnlyBooleanProperty editingProperty() {
        return editing.getReadOnlyProperty();
    }

    /// Returns whether this cell is being edited.
    public final boolean isEditing() {
        return editing.get();
    }

    /// Starts editing, if the cell is editable, shows an item and is not
    /// edited already. An override calls the super implementation and
    /// then asks [#isEditing()] whether the edit began.
    public void startEdit() {
        if (isEditable() && !isEditing() && !isEmpty()) {
            editing.set(true);
        }
    }

    /// Ends the edit without a new value.
    public void cancelEdit() {
        if (isEditing()) {
            editing.set(false);
        }
    }

    /// Ends the edit with a new value. The cell does not keep the value;
    /// the view it belongs to tells whoever owns the data.
    public void commitEdit(T newValue) {
        if (isEditing()) {
            editing.set(false);
        }
    }

    /// Gives the cell another item to show. An override calls the super
    /// implementation first, then sets the text and the graphic, or
    /// clears both when `empty` is true.
    protected void updateItem(T item, boolean empty) {
        setItem(item);
        if (this.empty.get() != empty) {
            this.empty.set(empty);
            pseudoClassStateChanged(EMPTY, empty);
            pseudoClassStateChanged(FILLED, !empty);
        }
        if (empty && isSelected()) {
            updateSelected(false);
        }
    }

    /// Tells the cell whether its row is selected. An empty cell is
    /// never selected.
    public void updateSelected(boolean selected) {
        if (selected && isEmpty()) {
            return;
        }
        if (this.selected.get() != selected) {
            this.selected.set(selected);
            pseudoClassStateChanged(SELECTED, selected);
            cn1Repaint();
        }
    }

    /// Returns whether two items differ enough for the cell to be
    /// updated; compares with `equals`.
    protected boolean isItemChanged(T oldItem, T newItem) {
        return oldItem != null ? !oldItem.equals(newItem) : newItem != null;
    }
}
