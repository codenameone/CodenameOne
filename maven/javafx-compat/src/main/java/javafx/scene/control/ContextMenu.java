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

import com.codename1.fxcompat.runtime.StagePopup;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

/// A popup that lists menu items, one row each.
///
/// [#show(Node, double, double)] opens it with its top left corner at a
/// point, [#show(Node, Side, double, double)] beside a node. Choosing a
/// row hides the menu, and the menus it was opened from, and then fires
/// the item; the `ActionEvent` goes on to the handler set with
/// [#setOnAction(EventHandler)]. A press outside hides the menu too. A
/// row of a [Menu] opens the sub-menu to its right when it is chosen. A
/// menu with no items does not open.
///
/// The rows are built when the menu opens: a change to an item while its
/// menu is open shows the next time. Keyboard navigation is not part of
/// this layer.
///
/// Set on a control with `Control.setContextMenu`, the menu opens where a
/// context menu is asked for: a secondary click or a long press.
///
/// ## Style
///
/// The popup has the style class `context-menu` and its rows those of
/// their items (`menu-item`, `menu`, `check-menu-item`). They are
/// recorded and not matched by a style sheet: the rows are in a scene of
/// their own, which does not have the style sheets of the owner.
public class ContextMenu extends PopupControl {

    private final ObservableList<MenuItem> items = FXCollections.observableArrayList();
    private final ObjectProperty<EventHandler<ActionEvent>> onAction =
            new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onAction");
    private final Menu source;
    private MenuContent content;
    private Scene popupScene;
    private boolean hiding;

    /// Creates a menu with no items.
    public ContextMenu() {
        this((Menu) null);
        items.addListener(new ListChangeListener<MenuItem>() {
            @Override
            public void onChanged(Change<? extends MenuItem> change) {
                while (change.next()) {
                    List<? extends MenuItem> removed = change.getRemoved();
                    for (int i = 0; i < removed.size(); i++) {
                        MenuItem item = removed.get(i);
                        if (!items.contains(item)) {
                            adopt(item, null);
                        }
                    }
                    List<? extends MenuItem> added = change.getAddedSubList();
                    for (int i = 0; i < added.size(); i++) {
                        adopt(added.get(i), ContextMenu.this);
                    }
                }
            }
        });
    }

    /// Creates a menu with items.
    public ContextMenu(MenuItem... items) {
        this();
        if (items != null) {
            this.items.addAll(items);
        }
    }

    /// Creates the popup of a menu: it lists the items of the menu, which
    /// stay the menu's.
    ContextMenu(Menu source) {
        this.source = source;
        getStyleClass().add("context-menu");
        setAutoHide(true);
        addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                EventHandler<ActionEvent> handler = onAction.get();
                if (handler != null) {
                    handler.handle(event);
                }
            }
        });
    }

    private static void adopt(MenuItem item, ContextMenu popup) {
        item.setParentPopup(popup);
        if (item instanceof Menu) {
            List<MenuItem> children = ((Menu) item).getItems();
            for (int i = 0; i < children.size(); i++) {
                adopt(children.get(i), popup);
            }
        }
    }

    /// Returns the items of this menu.
    public final ObservableList<MenuItem> getItems() {
        return items;
    }

    /// Returns the items the popup lists.
    List<MenuItem> rows() {
        return source != null ? source.getItems() : items;
    }

    /// Sets the handler called when an item of this menu is chosen.
    public final void setOnAction(EventHandler<ActionEvent> value) {
        onAction.set(value);
    }

    /// Returns the handler called when an item of this menu is chosen.
    public final EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
    }

    /// The handler called when an item of this menu is chosen.
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    /// Opens the menu at a side of a node, moved by an offset.
    public void show(Node anchor, Side side, double dx, double dy) {
        if (anchor == null || rows().isEmpty()) {
            return;
        }
        MenuContent probe = new MenuContent(this);
        double w = probe.prefWidth(-1);
        double h = probe.prefHeight(-1);
        probe.dispose();
        Bounds b = anchor.getLayoutBounds();
        double x = b.getMinX();
        double y = b.getMinY();
        if (side == Side.TOP) {
            y -= h;
        } else if (side == Side.LEFT) {
            x -= w;
        } else if (side == Side.RIGHT) {
            x += b.getWidth();
        } else {
            y += b.getHeight();
        }
        Point2D p = StagePopup.anchor(anchor, x, y);
        show(anchor, p.getX() + dx, p.getY() + dy);
    }

    /// Opens the menu for a node with its top left corner at a point, in
    /// the coordinates input events report as screen coordinates.
    @Override
    public void show(Node anchor, double screenX, double screenY) {
        if (anchor == null || rows().isEmpty()) {
            return;
        }
        build();
        super.show(anchor, screenX, screenY);
    }

    /// Opens the menu over a window with its top left corner at a point.
    @Override
    public void show(Window ownerWindow, double anchorX, double anchorY) {
        if (ownerWindow == null || rows().isEmpty()) {
            return;
        }
        build();
        super.show(ownerWindow, anchorX, anchorY);
    }

    private void build() {
        if (content != null) {
            content.dispose();
        }
        content = new MenuContent(this);
        if (popupScene == null) {
            popupScene = new Scene(content);
            setScene(popupScene);
        } else {
            popupScene.setRoot(content);
        }
    }

    /// Returns the rows of the menu as it last opened, or `null`.
    MenuContent content() {
        return content;
    }

    /// Hides this menu and every menu it was opened from.
    void hideAll() {
        ContextMenu top = this;
        for (int depth = 0; depth < 16; depth++) {
            Node owner = top.getOwnerNode();
            Scene s = owner == null ? null : owner.getScene();
            Window w = s == null ? null : s.getWindow();
            if (w instanceof ContextMenu) {
                top = (ContextMenu) w;
            } else {
                break;
            }
        }
        top.hide();
    }

    /// Hides the menu and the sub-menus opened from it.
    @Override
    public void hide() {
        if (hiding) {
            return;
        }
        hiding = true;
        try {
            if (content != null) {
                content.dispose();
            }
            super.hide();
            if (source != null && source.isShowing()) {
                source.hide();
            }
        } finally {
            hiding = false;
        }
    }

    @Override
    public String getTypeSelector() {
        return "ContextMenu";
    }
}
