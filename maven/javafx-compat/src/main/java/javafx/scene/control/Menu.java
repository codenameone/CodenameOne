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

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;

/// A menu item that opens a popup of further items: an entry of a
/// [MenuBar], or a sub-menu inside another menu.
///
/// [#show()] and [#hide()] change `showing`; the menu bar or the menu
/// that shows this one opens and closes the popup to match.
public class Menu extends MenuItem {

    /// Sent to a menu before it shows.
    public static final EventType<Event> ON_SHOWING = new EventType<Event>(Event.ANY, "MENU_ON_SHOWING");

    /// Sent to a menu after it showed.
    public static final EventType<Event> ON_SHOWN = new EventType<Event>(Event.ANY, "MENU_ON_SHOWN");

    /// Sent to a menu before it hides.
    public static final EventType<Event> ON_HIDING = new EventType<Event>(Event.ANY, "MENU_ON_HIDING");

    /// Sent to a menu after it hid.
    public static final EventType<Event> ON_HIDDEN = new EventType<Event>(Event.ANY, "MENU_ON_HIDDEN");

    private final ObservableList<MenuItem> items = FXCollections.observableArrayList();
    private final ReadOnlyBooleanWrapper showing = new ReadOnlyBooleanWrapper(this, "showing", false);

    /// Creates a menu with no text.
    public Menu() {
        this("");
    }

    /// Creates a menu with a text.
    public Menu(String text) {
        this(text, null);
    }

    /// Creates a menu with a text and a graphic.
    public Menu(String text, Node graphic) {
        this(text, graphic, (MenuItem[]) null);
    }

    /// Creates a menu with a text, a graphic and items.
    public Menu(String text, Node graphic, MenuItem... items) {
        super(text, graphic);
        styleClasses().add("menu");
        this.items.addListener(new ListChangeListener<MenuItem>() {
            @Override
            public void onChanged(Change<? extends MenuItem> change) {
                while (change.next()) {
                    List<? extends MenuItem> removed = change.getRemoved();
                    for (int i = 0; i < removed.size(); i++) {
                        MenuItem item = removed.get(i);
                        if (!Menu.this.items.contains(item)) {
                            item.setParentMenu(null);
                            item.setParentPopup(null);
                        }
                    }
                    List<? extends MenuItem> added = change.getAddedSubList();
                    for (int i = 0; i < added.size(); i++) {
                        MenuItem item = added.get(i);
                        Menu old = item.getParentMenu();
                        if (old != null && old != Menu.this) {
                            old.getItems().remove(item);
                        }
                        item.setParentMenu(Menu.this);
                        item.setParentPopup(getParentPopup());
                    }
                }
                if (Menu.this.items.isEmpty() && isShowing()) {
                    hide();
                }
            }
        });
        if (items != null) {
            this.items.addAll(items);
        }
    }

    /// Returns the items of this menu.
    public final ObservableList<MenuItem> getItems() {
        return items;
    }

    /// Returns whether the popup of this menu is open.
    public final boolean isShowing() {
        return showing.get();
    }

    /// Whether the popup of this menu is open.
    public final ReadOnlyBooleanProperty showingProperty() {
        return showing.getReadOnlyProperty();
    }

    /// Opens the popup of this menu. A disabled menu and one without
    /// items stay closed.
    public void show() {
        if (isDisable() || isShowing() || items.isEmpty()) {
            return;
        }
        Event.fireEvent(this, new Event(ON_SHOWING));
        showing.set(true);
        Event.fireEvent(this, new Event(ON_SHOWN));
    }

    /// Closes the popup of this menu and of the sub-menus in it.
    public void hide() {
        if (!isShowing()) {
            return;
        }
        Event.fireEvent(this, new Event(ON_HIDING));
        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            if (item instanceof Menu) {
                ((Menu) item).hide();
            }
        }
        showing.set(false);
        Event.fireEvent(this, new Event(ON_HIDDEN));
    }
}
