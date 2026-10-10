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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.Node;

/// Keeps the popup of a [Menu] in step with its `showing` property: the
/// popup opens beside a node while the menu shows and closes with it.
final class MenuLink implements ChangeListener<Boolean> {

    private final Menu menu;
    private final Node anchor;
    private final Side side;
    private ContextMenu popup;

    MenuLink(Menu menu, Node anchor, Side side) {
        this.menu = menu;
        this.anchor = anchor;
        this.side = side;
    }

    /// Starts following the menu.
    MenuLink attach() {
        menu.showingProperty().addListener(this);
        return this;
    }

    Menu menu() {
        return menu;
    }

    Node anchor() {
        return anchor;
    }

    /// Returns the open popup of the menu, or `null`.
    ContextMenu popup() {
        return popup;
    }

    /// Stops following the menu and closes it.
    void detach() {
        menu.hide();
        menu.showingProperty().removeListener(this);
        close();
    }

    private void close() {
        ContextMenu p = popup;
        popup = null;
        if (p != null) {
            p.hide();
        }
    }

    @Override
    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        if (newValue != null && newValue.booleanValue()) {
            if (popup == null) {
                ContextMenu p = new ContextMenu(menu);
                popup = p;
                p.show(anchor, side, 0, 0);
            }
        } else {
            close();
        }
    }
}
