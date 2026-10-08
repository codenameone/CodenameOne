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

import com.codename1.fxcompat.runtime.EventHandlerManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Node;

/// One entry of a menu. It is not a node: a [ContextMenu] or a [Menu]
/// that holds it shows it as a row.
///
/// Choosing the row calls [#fire()], which sends an `ActionEvent` to the
/// item; from there it goes on to the menus above it and to the context
/// menu that holds them. A disabled item is shown greyed and cannot be
/// chosen, an invisible one is left out.
///
/// The graphic is recorded and not drawn. Accelerators need
/// `KeyCombination`, which this layer does not have, and mnemonics are
/// not parsed; `accelerator`, `mnemonicParsing` and the menu validation
/// event are absent.
public class MenuItem implements EventTarget, Styleable {

    private final StringProperty id = new SimpleStringProperty(this, "id");
    private final StringProperty style = new SimpleStringProperty(this, "style", "");
    private final ObservableList<String> styleClass = FXCollections.observableArrayList();
    private final StringProperty text = new SimpleStringProperty(this, "text");
    private final ObjectProperty<Node> graphic = new SimpleObjectProperty<Node>(this, "graphic");
    private final BooleanProperty disable = new SimpleBooleanProperty(this, "disable", false);
    private final BooleanProperty visible = new SimpleBooleanProperty(this, "visible", true);
    private final ReadOnlyObjectWrapper<Menu> parentMenu = new ReadOnlyObjectWrapper<Menu>(this, "parentMenu");
    private final ReadOnlyObjectWrapper<ContextMenu> parentPopup = new ReadOnlyObjectWrapper<ContextMenu>(this,
            "parentPopup");
    private final EventHandlerManager events = new EventHandlerManager(this);
    private ObservableMap<Object, Object> properties;
    private Object userData;

    /// Creates an item with no text.
    public MenuItem() {
        this(null, null);
    }

    /// Creates an item with a text.
    public MenuItem(String text) {
        this(text, null);
    }

    /// Creates an item with a text and a graphic.
    public MenuItem(String text, Node graphic) {
        this.text.set(text);
        this.graphic.set(graphic);
        styleClass.add("menu-item");
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
        return "MenuItem";
    }

    @Override
    public Styleable getStyleableParent() {
        Menu menu = getParentMenu();
        return menu != null ? menu : getParentPopup();
    }

    @Override
    public final ObservableSet<PseudoClass> getPseudoClassStates() {
        return FXCollections.<PseudoClass>emptyObservableSet();
    }

    /// Records the menu this item is in.
    protected final void setParentMenu(Menu value) {
        parentMenu.set(value);
    }

    /// Returns the menu this item is in, or `null`.
    public final Menu getParentMenu() {
        return parentMenu.get();
    }

    /// The menu this item is in.
    public final ReadOnlyObjectProperty<Menu> parentMenuProperty() {
        return parentMenu.getReadOnlyProperty();
    }

    /// Records the context menu this item is in.
    protected final void setParentPopup(ContextMenu value) {
        parentPopup.set(value);
    }

    /// Returns the context menu this item is in, or `null`.
    public final ContextMenu getParentPopup() {
        return parentPopup.get();
    }

    /// The context menu this item is in.
    public final ReadOnlyObjectProperty<ContextMenu> parentPopupProperty() {
        return parentPopup.getReadOnlyProperty();
    }

    /// Sets the text of the row.
    public final void setText(String value) {
        text.set(value);
    }

    /// Returns the text of the row.
    public final String getText() {
        return text.get();
    }

    /// The text of the row.
    public final StringProperty textProperty() {
        return text;
    }

    /// Sets the graphic; recorded.
    public final void setGraphic(Node value) {
        graphic.set(value);
    }

    /// Returns the graphic.
    public final Node getGraphic() {
        return graphic.get();
    }

    /// The graphic.
    public final ObjectProperty<Node> graphicProperty() {
        return graphic;
    }

    /// Sets the handler of the item being chosen.
    public final void setOnAction(EventHandler<ActionEvent> value) {
        events.setSlot(ActionEvent.ACTION, "onAction", value);
    }

    /// Returns the handler of the item being chosen.
    public final EventHandler<ActionEvent> getOnAction() {
        return onActionProperty().get();
    }

    /// The handler of the item being chosen.
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ObjectProperty<EventHandler<ActionEvent>> onAction() {
        // The slot is typed for any handler of a super type of the event;
        // this property only ever holds handlers of the event itself.
        return (ObjectProperty) events.slot(ActionEvent.ACTION, "onAction");
    }

    /// Sets whether the item cannot be chosen.
    public final void setDisable(boolean value) {
        disable.set(value);
    }

    /// Returns whether the item cannot be chosen.
    public final boolean isDisable() {
        return disable.get();
    }

    /// Whether the item cannot be chosen.
    public final BooleanProperty disableProperty() {
        return disable;
    }

    /// Sets whether the item is shown in its menu.
    public final void setVisible(boolean value) {
        visible.set(value);
    }

    /// Returns whether the item is shown in its menu.
    public final boolean isVisible() {
        return visible.get();
    }

    /// Whether the item is shown in its menu.
    public final BooleanProperty visibleProperty() {
        return visible;
    }

    /// Chooses the item: sends it an `ActionEvent`.
    public void fire() {
        Event.fireEvent(this, new ActionEvent(this, this));
    }

    /// Adds a handler called in the bubbling phase.
    public <E extends Event> void addEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        events.addEventHandler(eventType, eventHandler);
    }

    /// Removes a handler.
    public <E extends Event> void removeEventHandler(EventType<E> eventType, EventHandler<E> eventHandler) {
        events.removeEventHandler(eventType, eventHandler);
    }

    @Override
    public EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        EventDispatchChain chain = tail;
        Menu menu = getParentMenu();
        ContextMenu popup = getParentPopup();
        if (menu != null) {
            chain = menu.buildEventDispatchChain(chain);
        } else if (popup != null) {
            chain = popup.buildEventDispatchChain(chain);
        }
        return chain.prepend(events);
    }

    /// Returns the application's own object attached to this item.
    public Object getUserData() {
        return userData;
    }

    /// Attaches an object of the application to this item.
    public void setUserData(Object value) {
        userData = value;
    }

    /// Returns a map for the application's own values on this item.
    public ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableHashMap();
        }
        return properties;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[text=" + getText() + "]";
    }
}
