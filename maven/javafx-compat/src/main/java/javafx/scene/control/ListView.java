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

import java.util.ArrayList;

import com.codename1.ui.Component;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Callback;

/// A vertical list of items, each shown by a [ListCell].
///
/// The cells are JavaFX nodes made by the cell factory; only the rows in
/// view have one, and a cell is given another row as the list scrolls.
/// Every row has the same height: `fixedCellSize` when it is set, else
/// the preferred height of the cell of the first row. The list scrolls by
/// the wheel, by dragging and by [#scrollTo(int)].
///
/// A press selects the row under it. With
/// `SelectionMode.MULTIPLE` a press with the shortcut key down adds or
/// removes the row, and one with Shift down selects the range from the
/// row pressed last.
///
/// The list is always vertical and cannot be edited: `orientation`,
/// `editable`, the edit events and the focus model of JavaFX are not part
/// of this layer.
///
/// The control has no native component. It starts with a white
/// background and a thin grey border, which the `Region` style names
/// replace; it adds no style names of its own.
public class ListView<T> extends Control {

    private final ObjectProperty<ObservableList<T>> items = new SimpleObjectProperty<ObservableList<T>>(this,
            "items");
    private final ObjectProperty<Node> placeholder = new SimpleObjectProperty<Node>(this, "placeholder");
    private final ObjectProperty<MultipleSelectionModel<T>> selectionModel =
            new SimpleObjectProperty<MultipleSelectionModel<T>>(this, "selectionModel");
    private final ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactory =
            new SimpleObjectProperty<Callback<ListView<T>, ListCell<T>>>(this, "cellFactory");
    private final DoubleProperty fixedCellSize = new SimpleDoubleProperty(this, "fixedCellSize", USE_COMPUTED_SIZE);
    private final RowFlow flow;
    private int anchor = -1;

    private final ListChangeListener<T> itemsListener = new ListChangeListener<T>() {
        @Override
        public void onChanged(Change<? extends T> change) {
            RowSelection<T> own = ownSelection();
            if (own != null) {
                own.rowsChanged(change);
            }
            contentChanged();
        }
    };

    private final ListChangeListener<Integer> selectionListener = new ListChangeListener<Integer>() {
        @Override
        public void onChanged(Change<? extends Integer> change) {
            syncSelection();
        }
    };

    /// Creates a list with no items.
    public ListView() {
        this(FXCollections.<T>observableArrayList());
    }

    /// Creates a list of some items.
    public ListView(ObservableList<T> items) {
        getStyleClass().add("list-view");
        setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(Color.rgb(200, 200, 200), BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(1))));
        flow = new RowFlow(new FlowRows());
        flow.stripes = true;
        cn1Children().add(flow);
        this.items.addListener(new ChangeListener<ObservableList<T>>() {
            @Override
            public void changed(ObservableValue<? extends ObservableList<T>> observable, ObservableList<T> oldValue,
                    ObservableList<T> newValue) {
                if (oldValue != null) {
                    oldValue.removeListener(itemsListener);
                }
                if (newValue != null) {
                    newValue.addListener(itemsListener);
                }
                RowSelection<T> own = ownSelection();
                if (own != null) {
                    own.rowsReplaced();
                }
                flow.scrollTo(0);
                contentChanged();
            }
        });
        selectionModel.addListener(new ChangeListener<MultipleSelectionModel<T>>() {
            @Override
            public void changed(ObservableValue<? extends MultipleSelectionModel<T>> observable,
                    MultipleSelectionModel<T> oldValue, MultipleSelectionModel<T> newValue) {
                if (oldValue != null) {
                    oldValue.getSelectedIndices().removeListener(selectionListener);
                }
                if (newValue != null) {
                    newValue.getSelectedIndices().addListener(selectionListener);
                }
                syncSelection();
            }
        });
        placeholder.addListener(new ChangeListener<Node>() {
            @Override
            public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
                if (oldValue != null) {
                    cn1Children().remove(oldValue);
                }
                if (newValue != null && !cn1Children().contains(newValue)) {
                    cn1Children().add(newValue);
                }
                requestLayout();
            }
        });
        cellFactory.addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                flow.rebuild();
            }
        });
        fixedCellSize.addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                flow.rowsChanged();
            }
        });
        flow.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                pressed(event);
            }
        });
        selectionModel.set(new Model<T>(this));
        this.items.set(items);
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    /// The selection of the model this list created, which follows the
    /// items; `null` once the application installed its own model.
    @SuppressWarnings("unchecked")
    private RowSelection<T> ownSelection() {
        MultipleSelectionModel<T> model = getSelectionModel();
        return model instanceof Model ? ((Model<T>) model).selection : null;
    }

    private void contentChanged() {
        flow.rowsChanged();
        requestLayout();
    }

    private void syncSelection() {
        MultipleSelectionModel<T> model = getSelectionModel();
        ArrayList<IndexedCell<?>[]> slots = flow.slots();
        for (int i = 0; i < slots.size(); i++) {
            IndexedCell<?>[] slot = slots.get(i);
            for (int c = 0; c < slot.length; c++) {
                int row = slot[c].getIndex();
                slot[c].updateSelected(model != null && row >= 0 && model.isSelected(row));
            }
        }
    }

    private void pressed(MouseEvent event) {
        MultipleSelectionModel<T> model = getSelectionModel();
        int row = flow.rowAt(event);
        if (model == null || row < 0) {
            return;
        }
        anchor = RowPress.apply(model, event.getButton() == MouseButton.PRIMARY, event.isShortcutDown(),
                event.isShiftDown(), row, anchor);
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double w = Math.max(0, getWidth() - in.getLeft() - in.getRight());
        double h = Math.max(0, getHeight() - in.getTop() - in.getBottom());
        Node empty = getPlaceholder();
        ObservableList<T> list = getItems();
        boolean showEmpty = empty != null && (list == null || list.isEmpty());
        flow.setVisible(!showEmpty);
        flow.resizeRelocate(in.getLeft(), in.getTop(), w, h);
        if (empty != null) {
            empty.setVisible(showEmpty);
            if (showEmpty) {
                layoutInArea(empty, in.getLeft(), in.getTop(), w, h, 0, javafx.geometry.HPos.CENTER,
                        javafx.geometry.VPos.CENTER);
            }
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        return 248;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 400;
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + in.getBottom();
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    @Override
    public double getBaselineOffset() {
        return BASELINE_OFFSET_SAME_AS_HEIGHT;
    }

    /// Sets the items shown. Changes to an observable list show at once.
    public final void setItems(ObservableList<T> value) {
        items.set(value);
    }

    /// Returns the items shown.
    public final ObservableList<T> getItems() {
        return items.get();
    }

    /// The items shown.
    public final ObjectProperty<ObservableList<T>> itemsProperty() {
        return items;
    }

    /// The node shown instead of the rows while there are no items.
    public final ObjectProperty<Node> placeholderProperty() {
        return placeholder;
    }

    /// Sets the node shown while there are no items.
    public final void setPlaceholder(Node value) {
        placeholder.set(value);
    }

    /// Returns the node shown while there are no items.
    public final Node getPlaceholder() {
        return placeholder.get();
    }

    /// Sets the selection model.
    public final void setSelectionModel(MultipleSelectionModel<T> value) {
        selectionModel.set(value);
    }

    /// Returns the selection model.
    public final MultipleSelectionModel<T> getSelectionModel() {
        return selectionModel.get();
    }

    /// The selection model.
    public final ObjectProperty<MultipleSelectionModel<T>> selectionModelProperty() {
        return selectionModel;
    }

    /// Sets the height of every row; `Region.USE_COMPUTED_SIZE` takes it
    /// from the first cell.
    public final void setFixedCellSize(double value) {
        fixedCellSize.set(value);
    }

    /// Returns the height of every row, or `Region.USE_COMPUTED_SIZE`.
    public final double getFixedCellSize() {
        return fixedCellSize.get();
    }

    /// The height of every row.
    public final DoubleProperty fixedCellSizeProperty() {
        return fixedCellSize;
    }

    /// Sets what creates the cells. Without one a cell shows the item's
    /// `toString()`, or the item itself when it is a node.
    public final void setCellFactory(Callback<ListView<T>, ListCell<T>> value) {
        cellFactory.set(value);
    }

    /// Returns what creates the cells, or `null` for the default.
    public final Callback<ListView<T>, ListCell<T>> getCellFactory() {
        return cellFactory.get();
    }

    /// What creates the cells.
    public final ObjectProperty<Callback<ListView<T>, ListCell<T>>> cellFactoryProperty() {
        return cellFactory;
    }

    /// Scrolls so that a row is the first one shown, as far as the rows
    /// reach.
    public void scrollTo(int index) {
        flow.show(index);
    }

    /// Scrolls to an item of the list.
    public void scrollTo(T object) {
        ObservableList<T> list = getItems();
        int at = list == null ? -1 : list.indexOf(object);
        if (at >= 0) {
            scrollTo(at);
        }
    }

    /// Shows every visible row again, for items that changed in a way
    /// the list could not observe.
    public void refresh() {
        flow.refresh();
    }

    private static final class DefaultCell<T> extends ListCell<T> {
        @Override
        protected void updateItem(T item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (item instanceof Node) {
                setText(null);
                setGraphic((Node) item);
            } else {
                setText(item == null ? "null" : item.toString());
                setGraphic(null);
            }
        }
    }

    private final class FlowRows implements RowFlow.Rows {
        @Override
        public int rowCount() {
            ObservableList<T> list = getItems();
            return list == null ? 0 : list.size();
        }

        @Override
        public int columnCount() {
            return 1;
        }

        @Override
        public void columns(double width, double[] x, double[] w) {
            x[0] = 0;
            w[0] = width;
        }

        @Override
        public IndexedCell<?> createCell(int column) {
            Callback<ListView<T>, ListCell<T>> factory = getCellFactory();
            ListCell<T> cell = factory == null ? null : factory.call(ListView.this);
            if (cell == null) {
                cell = new DefaultCell<T>();
            }
            cell.updateListView(ListView.this);
            return cell;
        }

        @Override
        public void updateCell(IndexedCell<?> cell, int column, int row, boolean force) {
            if (force && cell instanceof ListCell) {
                ((ListCell<?>) cell).markStale();
            }
            cell.updateIndex(row);
        }

        @Override
        public double fixedCellSize() {
            return getFixedCellSize();
        }
    }

    private static final class Model<T> extends MultipleSelectionModel<T> implements RowSelection.Owner<T> {

        private final ListView<T> view;
        private final RowSelection<T> selection;

        Model(ListView<T> view) {
            this.view = view;
            this.selection = new RowSelection<T>(this);
        }

        @Override
        public ObservableList<T> rows() {
            return view.getItems();
        }

        @Override
        public boolean multiple() {
            return getSelectionMode() == SelectionMode.MULTIPLE;
        }

        @Override
        public void lead(int index, T item) {
            setSelectedIndex(index);
            setSelectedItem(item);
        }

        @Override
        public ObservableList<Integer> getSelectedIndices() {
            return selection.indices();
        }

        @Override
        public ObservableList<T> getSelectedItems() {
            return selection.items();
        }

        @Override
        public void selectIndices(int index, int... indices) {
            selection.selectIndices(index, indices);
        }

        @Override
        public void selectAll() {
            selection.selectAll();
        }

        @Override
        public void selectFirst() {
            if (selection.rowCount() > 0) {
                select(0);
            }
        }

        @Override
        public void selectLast() {
            int n = selection.rowCount();
            if (n > 0) {
                select(n - 1);
            }
        }

        @Override
        public void clearAndSelect(int index) {
            selection.clearAndSelect(index);
        }

        @Override
        public void select(int index) {
            selection.select(index);
        }

        @Override
        public void select(T obj) {
            selection.select(obj);
        }

        @Override
        public void clearSelection(int index) {
            selection.clear(index);
        }

        @Override
        public void clearSelection() {
            selection.clear();
        }

        @Override
        public boolean isSelected(int index) {
            return selection.isSelected(index);
        }

        @Override
        public boolean isEmpty() {
            return selection.isEmpty();
        }

        @Override
        public void selectPrevious() {
            int lead = selection.lead();
            if (lead > 0) {
                select(lead - 1);
            }
        }

        @Override
        public void selectNext() {
            int lead = selection.lead();
            if (lead < selection.rowCount() - 1) {
                select(lead + 1);
            }
        }
    }
}
