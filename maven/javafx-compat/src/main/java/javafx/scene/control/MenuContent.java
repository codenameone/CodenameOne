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
import java.util.List;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/// The content of an open menu: its visible items stacked as rows, a
/// separator as a line.
final class MenuContent extends Region {

    private static final double GAP = 3;
    private static final Color LINE = Color.rgb(200, 200, 200);
    private static final Background HOVER = new Background(
            new BackgroundFill(Color.rgb(210, 230, 245), CornerRadii.EMPTY, Insets.EMPTY));

    private final ContextMenu popup;
    private final ArrayList<Node> rows = new ArrayList<Node>();
    private final ArrayList<MenuItem> rowItems = new ArrayList<MenuItem>();
    private final ArrayList<MenuLink> links = new ArrayList<MenuLink>();

    MenuContent(ContextMenu popup) {
        this.popup = popup;
        setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(LINE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
                new BorderWidths(1))));
        List<MenuItem> items = popup.rows();
        for (int i = 0; i < items.size(); i++) {
            MenuItem item = items.get(i);
            if (item == null || !item.isVisible()) {
                continue;
            }
            Node row = item instanceof SeparatorMenuItem ? line() : row(item);
            rows.add(row);
            rowItems.add(item);
            cn1Children().add(row);
        }
    }

    private static Region line() {
        Region r = new Region();
        r.setBackground(new Background(new BackgroundFill(LINE, CornerRadii.EMPTY, Insets.EMPTY)));
        return r;
    }

    private MenuRow row(final MenuItem item) {
        String text = item.shownText();
        boolean marked = item instanceof CheckMenuItem && ((CheckMenuItem) item).isSelected();
        final MenuRow row = new MenuRow(item instanceof Menu ? text + "   >" : text, marked);
        row.getStyleClass().setAll(item.getStyleClass());
        row.setId(item.getId());
        row.setFocusTraversable(false);
        row.setPadding(new Insets(4, 12, 4, 22));
        if (item.isDisable()) {
            row.setDisable(true);
            row.setTextFill(Color.GRAY);
            return row;
        }
        if (item instanceof Menu) {
            links.add(new MenuLink((Menu) item, row, Side.RIGHT).attach());
        }
        row.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, new Paint(row, HOVER));
        row.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, new Paint(row, null));
        row.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    event.consume();
                    choose(item);
                }
            }
        });
        return row;
    }

    /// Gives a row a background when the pointer enters or leaves it.
    private static final class Paint implements EventHandler<MouseEvent> {
        private final MenuRow row;
        private final Background background;

        Paint(MenuRow row, Background background) {
            this.row = row;
            this.background = background;
        }

        @Override
        public void handle(MouseEvent event) {
            row.setBackground(background);
        }
    }

    private int current = -1;

    /// Does what a key does to an open menu: Down and Up move to the next
    /// and the previous item that can be chosen, around the ends; Enter
    /// and Space choose the item moved to; Right opens the sub-menu of
    /// that item and Left closes a sub-menu; Escape closes the menu.
    /// Answers whether the key was one of these.
    boolean key(KeyCode code) {
        if (code == KeyCode.DOWN) {
            move(1);
        } else if (code == KeyCode.UP) {
            move(-1);
        } else if (code == KeyCode.ENTER || code == KeyCode.SPACE) {
            if (current >= 0) {
                choose(rowItems.get(current));
            }
        } else if (code == KeyCode.RIGHT) {
            if (current >= 0 && rowItems.get(current) instanceof Menu
                    && !((Menu) rowItems.get(current)).isShowing()) {
                choose(rowItems.get(current));
            }
        } else if (code == KeyCode.LEFT) {
            if (popup.isSubMenu()) {
                popup.hide();
            }
        } else if (code == KeyCode.ESCAPE) {
            popup.hide();
        } else {
            return false;
        }
        return true;
    }

    private void move(int step) {
        int n = rows.size();
        int at = current;
        for (int tries = 0; tries < n; tries++) {
            at = at < 0 ? (step > 0 ? 0 : n - 1) : (at + step + n) % n;
            Node row = rows.get(at);
            if (row instanceof MenuRow && !row.isDisabled()) {
                if (current >= 0 && rows.get(current) instanceof MenuRow) {
                    ((MenuRow) rows.get(current)).setBackground(null);
                }
                current = at;
                ((MenuRow) row).setBackground(HOVER);
                return;
            }
        }
    }

    /// Does what choosing the row of an item does.
    void choose(MenuItem item) {
        if (item.isDisable() || item instanceof SeparatorMenuItem) {
            return;
        }
        if (item instanceof Menu) {
            Menu m = (Menu) item;
            if (m.isShowing()) {
                m.hide();
            } else {
                for (int i = 0; i < links.size(); i++) {
                    if (links.get(i).menu() != m) {
                        links.get(i).menu().hide();
                    }
                }
                m.show();
            }
            return;
        }
        if (item instanceof CheckMenuItem) {
            CheckMenuItem check = (CheckMenuItem) item;
            check.setSelected(!check.isSelected());
        }
        popup.hideAll();
        item.fire();
    }

    /// Returns the row of an item, or `null` when it has none.
    Node rowOf(MenuItem item) {
        int i = rowItems.indexOf(item);
        return i < 0 ? null : rows.get(i);
    }

    /// Returns the open popup of a sub-menu, or `null`.
    ContextMenu popupOf(Menu menu) {
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).menu() == menu) {
                return links.get(i).popup();
            }
        }
        return null;
    }

    /// Closes the sub-menus and stops following them.
    void dispose() {
        ArrayList<MenuLink> all = new ArrayList<MenuLink>(links);
        links.clear();
        for (int i = 0; i < all.size(); i++) {
            all.get(i).detach();
        }
    }

    private double rowHeight(int i) {
        return rowItems.get(i) instanceof SeparatorMenuItem ? GAP + 1 + GAP : rows.get(i).prefHeight(-1);
    }

    @Override
    protected double computePrefWidth(double height) {
        double w = 60;
        for (int i = 0; i < rows.size(); i++) {
            if (!(rowItems.get(i) instanceof SeparatorMenuItem)) {
                w = Math.max(w, rows.get(i).prefWidth(-1));
            }
        }
        Insets in = getInsets();
        return in.getLeft() + w + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        double h = 0;
        for (int i = 0; i < rows.size(); i++) {
            h += rowHeight(i);
        }
        Insets in = getInsets();
        return in.getTop() + h + in.getBottom();
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double x = in.getLeft();
        double w = Math.max(0, getWidth() - x - in.getRight());
        double y = in.getTop();
        for (int i = 0; i < rows.size(); i++) {
            Node row = rows.get(i);
            double h = rowHeight(i);
            if (rowItems.get(i) instanceof SeparatorMenuItem) {
                row.resizeRelocate(x, y + GAP, w, 1);
            } else {
                row.resizeRelocate(x, y, w, h);
            }
            y += h;
        }
    }
}
