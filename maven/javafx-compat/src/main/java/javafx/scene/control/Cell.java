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

import java.util.List;

import com.codename1.fxcompat.runtime.CssEngine;
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
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

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
/// A cell without a text fill of its own is shown in white when what is
/// behind it is dark, and in the colour of the theme otherwise. What is
/// behind it is the nearest background up the tree, the cell's own
/// included, whose last fill is a colour at least half opaque. JavaFX
/// reaches the same end through the ladder of its default style sheet on
/// `-fx-control-inner-background`; a view whose style sheet sets that
/// colour without a background colour to match is not followed.
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
    private static final Color HIGHLIGHT = Color.rgb(0, 150, 201);

    private final ObjectProperty<T> item = new SimpleObjectProperty<T>(this, "item");
    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(this, "empty", true);
    private final ReadOnlyBooleanWrapper selected = new ReadOnlyBooleanWrapper(this, "selected", false);
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", true);
    private final ReadOnlyBooleanWrapper editing = new ReadOnlyBooleanWrapper(this, "editing", false);
    private int basePadding = -1;
    private int themeText = -1;

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

    @Override
    boolean ownsGraphic() {
        return true;
    }

    /// The room kept free before the graphic and the text, in logical
    /// pixels: none, but a cell of a tree keeps its indent and its arrow
    /// there.
    double leading() {
        return 0;
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
        Component before = cn1NativeIfCreated();
        if (before != null && themeText < 0) {
            themeText = before.getUnselectedStyle().getFgColor() & 0xffffff;
        }
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (c == null) {
            return;
        }
        if (textFillProperty().get() == null && themeText >= 0) {
            c.getAllStyles().setFgColor(isSelected() && getBackground() == null ? selectedText()
                    : (onDark() ? 0xffffff : themeText));
        }
        if (basePadding < 0) {
            basePadding = c.getStyle().getPaddingLeftNoRTL();
        }
        // The text starts after the graphic, which is a node of its own.
        Style style = c.getAllStyles();
        style.setPaddingUnitLeft(Style.UNIT_TYPE_PIXELS);
        style.setPaddingLeft(basePadding + Units.toPixels(leading() + graphicWidth()));
    }

    /// The bar behind a selected cell: the theme's `-fx-selection-bar`
    /// where the cell stands, which a style sheet recolours through
    /// `-fx-accent`.
    private Color selectionBar() {
        Color c = com.codename1.fxcompat.runtime.CssEngine.themeColor(this, "-fx-selection-bar");
        return c == null ? HIGHLIGHT : c;
    }

    /// White on a bar that reads as dark, the theme's text otherwise.
    private int selectedText() {
        Color c = selectionBar();
        return 0.3 * c.getRed() + 0.59 * c.getGreen() + 0.11 * c.getBlue() < 0.455 ? 0xffffff
                : (themeText >= 0 ? themeText : 0);
    }

    /// Whether the nearest background behind the text is a dark colour.
    private boolean onDark() {
        Node at = this;
        while (at != null) {
            if (at instanceof Region) {
                Background b = ((Region) at).getBackground();
                List<BackgroundFill> fills = b == null ? null : b.getFills();
                if (fills != null && !fills.isEmpty()) {
                    Paint p = fills.get(fills.size() - 1).getFill();
                    if (p instanceof Color && ((Color) p).getOpacity() >= 0.5) {
                        Color color = (Color) p;
                        return 0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue() < 0.4;
                    }
                }
            }
            at = at.getParent();
        }
        return false;
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
                g.relocate(in.getLeft() + leading() + Units.toLogical(Math.max(0, basePadding))
                        - g.getLayoutBounds().getMinX(),
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
            renderer.fillRect(0, 0, getWidth(), getHeight(), selectionBar());
        } else if (getBackground() == null && striped()) {
            // The standard theme shades every second row of a list and of
            // a table. It is painted, not given as a background, so that a
            // cell an application colours is still told from one it left.
            Color alt = CssEngine.themeColor(this, "-fx-control-inner-background-alt");
            if (alt != null) {
                renderer.fillRect(0, 0, getWidth(), getHeight(), alt);
            }
        }
    }

    /// Whether this cell is on a row the standard theme shades.
    boolean striped() {
        return false;
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
            // The text changes colour with the bar behind it.
            cn1Invalidated(com.codename1.fxcompat.runtime.Dirty.NATIVE);
            cn1Repaint();
        }
    }

    /// Returns whether two items differ enough for the cell to be
    /// updated; compares with `equals`.
    protected boolean isItemChanged(T oldItem, T newItem) {
        return oldItem != null ? !oldItem.equals(newItem) : newItem != null;
    }
}
