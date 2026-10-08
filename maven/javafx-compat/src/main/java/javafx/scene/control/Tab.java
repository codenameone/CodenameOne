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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;

/// One page of a [TabPane]: a header with text and an optional graphic,
/// and the content shown while the tab is selected.
///
/// A tab is not a node. Its events are delivered to the tab alone:
/// `SELECTION_CHANGED_EVENT` when it becomes or stops being the selected
/// tab, `TAB_CLOSE_REQUEST_EVENT` when the user asks to close it (a
/// handler that consumes it keeps the tab open) and `CLOSED_EVENT` after
/// it was closed. The context menu and tooltip of a JavaFX tab are not
/// part of this layer.
public class Tab implements EventTarget, Styleable {

    /// Fired when the tab becomes, or stops being, the selected tab.
    public static final EventType<Event> SELECTION_CHANGED_EVENT = new EventType<Event>(Event.ANY,
            "SELECTION_CHANGED_EVENT");

    /// Fired after the tab was closed.
    public static final EventType<Event> CLOSED_EVENT = new EventType<Event>(Event.ANY, "TAB_CLOSED");

    /// Fired when the user asks to close the tab; consuming it keeps the
    /// tab open.
    public static final EventType<Event> TAB_CLOSE_REQUEST_EVENT = new EventType<Event>(Event.ANY,
            "TAB_CLOSE_REQUEST_EVENT");

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass DISABLED = PseudoClass.getPseudoClass("disabled");

    private final EventHandlerManager events = new EventHandlerManager(this);
    private final StringProperty id = new SimpleStringProperty(this, "id");
    private final StringProperty style = new SimpleStringProperty(this, "style", "");
    private final ObservableList<String> styleClass = FXCollections.observableArrayList();
    private final ObservableSet<PseudoClass> pseudoClasses = FXCollections.observableSet(
            new HashSet<PseudoClass>());
    private final ReadOnlyBooleanWrapper selected = new ReadOnlyBooleanWrapper(this, "selected", false);
    private final ReadOnlyObjectWrapper<TabPane> tabPane = new ReadOnlyObjectWrapper<TabPane>(this, "tabPane");
    private final StringProperty text = new SimpleStringProperty(this, "text");
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<Node>(this, "graphic");
    private final ObjectProperty<Node> content = new SimpleObjectProperty<Node>(this, "content");
    private final BooleanProperty closable = new SimpleBooleanProperty(this, "closable", true);
    private final BooleanProperty disable = new SimpleBooleanProperty(this, "disable", false);
    private final ReadOnlyBooleanWrapper disabled = new ReadOnlyBooleanWrapper(this, "disabled", false);
    private final ObjectProperty<EventHandler<Event>> onSelectionChanged = slot(SELECTION_CHANGED_EVENT,
            "onSelectionChanged");
    private final ObjectProperty<EventHandler<Event>> onClosed = slot(CLOSED_EVENT, "onClosed");
    private final ObjectProperty<EventHandler<Event>> onCloseRequest = slot(TAB_CLOSE_REQUEST_EVENT,
            "onCloseRequest");
    private Object userData;
    private ObservableMap<Object, Object> properties;

    /// Creates a tab with no text.
    public Tab() {
        this(null);
    }

    /// Creates a tab with text.
    public Tab(String text) {
        this(text, null);
    }

    /// Creates a tab with text and content.
    public Tab(String text, Node content) {
        styleClass.add("tab");
        this.text.set(text);
        this.content.set(content);
        ChangeListener<Object> changed = new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                updateDisabled();
                TabPane pane = getTabPane();
                if (pane != null) {
                    pane.tabChanged(Tab.this);
                }
            }
        };
        this.text.addListener(changed);
        this.graphic.addListener(changed);
        this.content.addListener(changed);
        this.closable.addListener(changed);
        this.disable.addListener(changed);
    }

    private ObjectProperty<EventHandler<Event>> slot(final EventType<Event> type, final String name) {
        return new SimpleObjectProperty<EventHandler<Event>>(this, name) {
            @Override
            protected void invalidated() {
                events.setSlot(type, name, get());
            }
        };
    }

    // ------------------------------------------------------- package hooks

    final void setTabPane(TabPane value) {
        tabPane.set(value);
        updateDisabled();
    }

    final void setSelected(boolean value) {
        if (selected.get() != value) {
            selected.set(value);
            if (value) {
                pseudoClasses.add(SELECTED);
            } else {
                pseudoClasses.remove(SELECTED);
            }
            Event.fireEvent(this, new Event(this, this, SELECTION_CHANGED_EVENT));
        }
    }

    final void updateDisabled() {
        TabPane pane = getTabPane();
        boolean now = isDisable() || (pane != null && pane.isDisabled());
        if (disabled.get() != now) {
            disabled.set(now);
            if (now) {
                pseudoClasses.add(DISABLED);
            } else {
                pseudoClasses.remove(DISABLED);
            }
        }
    }

    // --------------------------------------------------------------- state

    /// Returns whether this is the selected tab of its tab pane.
    public final boolean isSelected() {
        return selected.get();
    }

    /// Whether this is the selected tab of its tab pane.
    public final ReadOnlyBooleanProperty selectedProperty() {
        return selected.getReadOnlyProperty();
    }

    /// Returns the tab pane this tab is in, or `null`.
    public final TabPane getTabPane() {
        return tabPane.get();
    }

    /// The tab pane this tab is in.
    public final ReadOnlyObjectProperty<TabPane> tabPaneProperty() {
        return tabPane.getReadOnlyProperty();
    }

    /// Sets the text of the header.
    public final void setText(String value) {
        text.set(value);
    }

    /// Returns the text of the header.
    public final String getText() {
        return text.get();
    }

    /// The text of the header.
    public final StringProperty textProperty() {
        return text;
    }

    /// Sets the node shown in the header before the text.
    public final void setGraphic(Node value) {
        graphic.set(value);
    }

    /// Returns the node shown in the header before the text, or `null`.
    public final Node getGraphic() {
        return graphic.get();
    }

    /// The node shown in the header before the text.
    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    /// Sets the node shown while this tab is selected.
    public final void setContent(Node value) {
        content.set(value);
    }

    /// Returns the node shown while this tab is selected, or `null`.
    public final Node getContent() {
        return content.get();
    }

    /// The node shown while this tab is selected.
    public final ObjectProperty<Node> contentProperty() {
        return content;
    }

    /// Sets whether the user can close this tab, where the tab pane's
    /// closing policy lets any be closed.
    public final void setClosable(boolean value) {
        closable.set(value);
    }

    /// Returns whether the user can close this tab.
    public final boolean isClosable() {
        return closable.get();
    }

    /// Whether the user can close this tab.
    public final BooleanProperty closableProperty() {
        return closable;
    }

    /// Sets whether this tab is disabled: it cannot be selected by the
    /// user and its content is disabled.
    public final void setDisable(boolean value) {
        disable.set(value);
    }

    /// Returns whether this tab was disabled itself.
    public final boolean isDisable() {
        return disable.get();
    }

    /// Whether this tab was disabled itself.
    public final BooleanProperty disableProperty() {
        return disable;
    }

    /// Returns whether this tab or its tab pane is disabled.
    public final boolean isDisabled() {
        return disabled.get();
    }

    /// Whether this tab or its tab pane is disabled.
    public final ReadOnlyBooleanProperty disabledProperty() {
        return disabled.getReadOnlyProperty();
    }

    // -------------------------------------------------------------- events

    /// Sets the handler of this tab becoming or ceasing to be selected.
    public final void setOnSelectionChanged(EventHandler<Event> value) {
        onSelectionChanged.set(value);
    }

    /// Returns the handler of this tab becoming or ceasing to be selected.
    public final EventHandler<Event> getOnSelectionChanged() {
        return onSelectionChanged.get();
    }

    /// The handler of this tab becoming or ceasing to be selected.
    public final ObjectProperty<EventHandler<Event>> onSelectionChangedProperty() {
        return onSelectionChanged;
    }

    /// Sets the handler of this tab having been closed.
    public final void setOnClosed(EventHandler<Event> value) {
        onClosed.set(value);
    }

    /// Returns the handler of this tab having been closed.
    public final EventHandler<Event> getOnClosed() {
        return onClosed.get();
    }

    /// The handler of this tab having been closed.
    public final ObjectProperty<EventHandler<Event>> onClosedProperty() {
        return onClosed;
    }

    /// Sets the handler of the user asking to close this tab.
    public final void setOnCloseRequest(EventHandler<Event> value) {
        onCloseRequest.set(value);
    }

    /// Returns the handler of the user asking to close this tab.
    public final EventHandler<Event> getOnCloseRequest() {
        return onCloseRequest.get();
    }

    /// The handler of the user asking to close this tab.
    public final ObjectProperty<EventHandler<Event>> onCloseRequestProperty() {
        return onCloseRequest;
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        return tail.prepend(events);
    }

    // ------------------------------------------------- styling, user data

    /// Sets the id a style sheet can select this tab by.
    public final void setId(String value) {
        id.set(value);
    }

    /// Returns the id a style sheet can select this tab by.
    @Override
    public final String getId() {
        return id.get();
    }

    /// The id a style sheet can select this tab by.
    public final StringProperty idProperty() {
        return id;
    }

    /// Sets the inline style of this tab.
    public final void setStyle(String value) {
        style.set(value);
    }

    /// Returns the inline style of this tab.
    @Override
    public final String getStyle() {
        return style.get();
    }

    /// The inline style of this tab.
    public final StringProperty styleProperty() {
        return style;
    }

    /// Returns the style classes of this tab; `tab` to begin with.
    @Override
    public ObservableList<String> getStyleClass() {
        return styleClass;
    }

    /// Returns `Tab`.
    @Override
    public String getTypeSelector() {
        return "Tab";
    }

    /// Returns the tab pane this tab is in.
    @Override
    public Styleable getStyleableParent() {
        return getTabPane();
    }

    /// Returns the pseudo-class states of this tab: `selected` and
    /// `disabled`.
    @Override
    public final ObservableSet<PseudoClass> getPseudoClassStates() {
        return FXCollections.unmodifiableObservableSet(pseudoClasses);
    }

    /// Attaches an object of the application's.
    public void setUserData(Object value) {
        userData = value;
    }

    /// Returns the object the application attached, or `null`.
    public Object getUserData() {
        return userData;
    }

    /// Returns the map of properties the application attached.
    public final ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableHashMap();
        }
        return properties;
    }

    /// Returns whether any property was attached.
    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }
}
