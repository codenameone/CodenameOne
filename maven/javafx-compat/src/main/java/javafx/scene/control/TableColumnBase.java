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

import java.util.HashSet;

import com.codename1.fxcompat.runtime.EventHandlerManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.EventDispatchChain;
import javafx.event.EventTarget;

/// What the columns of the table controls share: a header text, a width
/// between a minimum and a maximum, and whether the column is shown.
///
/// The width of a column is its preferred width held between the minimum
/// and the maximum; the user cannot drag a column wider, and `resizable`
/// and `sortable` are recorded. Nested columns, sorting, the header
/// graphic and the column context menu are not part of this layer.
public abstract class TableColumnBase<S, T> implements EventTarget, Styleable {

    static final double DEFAULT_WIDTH = 80;
    static final double DEFAULT_MIN_WIDTH = 10;
    static final double DEFAULT_MAX_WIDTH = 5000;

    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final BooleanProperty visible = new SimpleBooleanProperty(this, "visible", true);
    private final BooleanProperty resizable = new SimpleBooleanProperty(this, "resizable", true);
    private final BooleanProperty sortable = new SimpleBooleanProperty(this, "sortable", true);
    private final DoubleProperty minWidth = new SimpleDoubleProperty(this, "minWidth", DEFAULT_MIN_WIDTH);
    private final DoubleProperty prefWidth = new SimpleDoubleProperty(this, "prefWidth", DEFAULT_WIDTH);
    private final DoubleProperty maxWidth = new SimpleDoubleProperty(this, "maxWidth", DEFAULT_MAX_WIDTH);
    private final ReadOnlyDoubleWrapper width = new ReadOnlyDoubleWrapper(this, "width", DEFAULT_WIDTH);
    private final StringProperty id = new SimpleStringProperty(this, "id");
    private final StringProperty style = new SimpleStringProperty(this, "style", "");
    private final ObservableList<String> styleClass = FXCollections.observableArrayList();
    private final EventHandlerManager events = new EventHandlerManager(this);
    private ObservableSet<PseudoClass> pseudoClasses;
    private ObservableMap<Object, Object> properties;
    private Object userData;

    /// Creates a column with no header text.
    protected TableColumnBase() {
        this("");
    }

    /// Creates a column with a header text.
    protected TableColumnBase(String text) {
        this.text.set(text);
        ChangeListener<Number> sized = new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                doSetWidth(prefWidth.get());
            }
        };
        prefWidth.addListener(sized);
        minWidth.addListener(sized);
        maxWidth.addListener(sized);
    }

    /// Sets the width, held between the minimum and the maximum.
    final void doSetWidth(double value) {
        double w = Math.min(Math.max(value, minWidth.get()), maxWidth.get());
        width.set(w < 0 || Double.isNaN(w) ? 0 : w);
    }

    /// The header text.
    public final StringProperty textProperty() {
        return text;
    }

    /// Sets the header text.
    public final void setText(String value) {
        text.set(value);
    }

    /// Returns the header text.
    public final String getText() {
        return text.get();
    }

    /// Sets whether the column is shown.
    public final void setVisible(boolean value) {
        visible.set(value);
    }

    /// Returns whether the column is shown.
    public final boolean isVisible() {
        return visible.get();
    }

    /// Whether the column is shown.
    public final BooleanProperty visibleProperty() {
        return visible;
    }

    /// Sets the id a style sheet matches.
    public final void setId(String value) {
        id.set(value);
    }

    @Override
    public final String getId() {
        return id.get();
    }

    /// The id a style sheet matches.
    public final StringProperty idProperty() {
        return id;
    }

    /// Sets the inline style; recorded.
    public final void setStyle(String value) {
        style.set(value);
    }

    @Override
    public final String getStyle() {
        return style.get();
    }

    /// The inline style.
    public final StringProperty styleProperty() {
        return style;
    }

    @Override
    public ObservableList<String> getStyleClass() {
        return styleClass;
    }

    /// The style classes, for a constructor.
    final ObservableList<String> styleClasses() {
        return styleClass;
    }

    @Override
    public String getTypeSelector() {
        return "TableColumn";
    }

    @Override
    public Styleable getStyleableParent() {
        return null;
    }

    @Override
    public final ObservableSet<PseudoClass> getPseudoClassStates() {
        if (pseudoClasses == null) {
            pseudoClasses = FXCollections.observableSet(new HashSet<PseudoClass>());
        }
        return pseudoClasses;
    }

    /// The width the column has.
    public final ReadOnlyDoubleProperty widthProperty() {
        return width.getReadOnlyProperty();
    }

    /// Returns the width the column has.
    public final double getWidth() {
        return width.get();
    }

    /// Sets the least width.
    public final void setMinWidth(double value) {
        minWidth.set(value);
    }

    /// Returns the least width.
    public final double getMinWidth() {
        return minWidth.get();
    }

    /// The least width.
    public final DoubleProperty minWidthProperty() {
        return minWidth;
    }

    /// Sets the width the column asks for.
    public final void setPrefWidth(double value) {
        prefWidth.set(value);
    }

    /// Returns the width the column asks for.
    public final double getPrefWidth() {
        return prefWidth.get();
    }

    /// The width the column asks for.
    public final DoubleProperty prefWidthProperty() {
        return prefWidth;
    }

    /// Sets the greatest width.
    public final void setMaxWidth(double value) {
        maxWidth.set(value);
    }

    /// Returns the greatest width.
    public final double getMaxWidth() {
        return maxWidth.get();
    }

    /// The greatest width.
    public final DoubleProperty maxWidthProperty() {
        return maxWidth;
    }

    /// Sets whether the user may resize the column; recorded.
    public final void setResizable(boolean value) {
        resizable.set(value);
    }

    /// Returns whether the user may resize the column.
    public final boolean isResizable() {
        return resizable.get();
    }

    /// Whether the user may resize the column.
    public final BooleanProperty resizableProperty() {
        return resizable;
    }

    /// Sets whether the table may be sorted by this column; recorded.
    public final void setSortable(boolean value) {
        sortable.set(value);
    }

    /// Returns whether the table may be sorted by this column.
    public final boolean isSortable() {
        return sortable.get();
    }

    /// Whether the table may be sorted by this column.
    public final BooleanProperty sortableProperty() {
        return sortable;
    }

    /// Attaches an object of the application to this column.
    public void setUserData(Object value) {
        userData = value;
    }

    /// Returns the application's own object attached to this column.
    public Object getUserData() {
        return userData;
    }

    /// Returns a map for the application's own values on this column.
    public final ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableHashMap();
        }
        return properties;
    }

    /// Returns whether [#getProperties()] holds anything.
    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }

    /// Returns the value this column shows for a row, or `null`.
    public final T getCellData(final int index) {
        ObservableValue<T> result = getCellObservableValue(index);
        return result == null ? null : result.getValue();
    }

    /// Returns the value this column shows for an item, or `null`.
    public final T getCellData(final S item) {
        ObservableValue<T> result = getCellObservableValue(item);
        return result == null ? null : result.getValue();
    }

    /// Returns the observable value this column shows for a row.
    public abstract ObservableValue<T> getCellObservableValue(int index);

    /// Returns the observable value this column shows for an item.
    public abstract ObservableValue<T> getCellObservableValue(S item);

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(events);
    }
}
