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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/// A row of menus. Each [Menu] is shown as its text; pressing it opens
/// the popup of the menu under it, pressing it again or anywhere outside
/// closes it. [Menu#show()] and [Menu#hide()] do the same from code.
///
/// The bar is always drawn inside the scene: the menu bar of the system,
/// and with it `useSystemMenuBar`, is not part of this layer.
///
/// ## Style
///
/// The bar has the style class `menu-bar`, and takes `-fx-background-color`
/// and the other region properties from a style sheet; without one it is
/// light grey. The text of each menu is a label with the style classes of
/// its menu (`menu`, `menu-item`).
public class MenuBar extends Control {

    private final ObservableList<Menu> menus = FXCollections.observableArrayList();
    private final ArrayList<Header> headers = new ArrayList<Header>();
    private Menu justClosed;

    /// Creates a bar with no menus.
    public MenuBar() {
        this((Menu[]) null);
    }

    /// Creates a bar with menus.
    public MenuBar(Menu... menus) {
        getStyleClass().add("menu-bar");
        setFocusTraversable(false);
        setBackground(new Background(new BackgroundFill(Color.rgb(236, 236, 236), CornerRadii.EMPTY, Insets.EMPTY)));
        this.menus.addListener(new ListChangeListener<Menu>() {
            @Override
            public void onChanged(Change<? extends Menu> change) {
                rebuild();
            }
        });
        if (menus != null) {
            this.menus.addAll(menus);
        }
    }

    /// Returns the menus of the bar, in the order they are shown.
    public final ObservableList<Menu> getMenus() {
        return menus;
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    private void rebuild() {
        for (int i = 0; i < headers.size(); i++) {
            headers.get(i).dispose();
        }
        headers.clear();
        cn1Children().clear();
        for (int i = 0; i < menus.size(); i++) {
            Menu m = menus.get(i);
            if (m != null) {
                Header h = new Header(m);
                headers.add(h);
                cn1Children().add(h.label);
            }
        }
        requestLayout();
    }

    private void pressed(Menu menu) {
        if (menu.isShowing()) {
            menu.hide();
            return;
        }
        if (justClosed == menu) {
            // The press that closed the popup is this one.
            return;
        }
        for (int i = 0; i < headers.size(); i++) {
            headers.get(i).menu.hide();
        }
        menu.show();
    }

    private void closed(final Menu menu) {
        justClosed = menu;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (justClosed == menu) {
                    justClosed = null;
                }
            }
        });
    }

    /// Returns the label that shows a menu of the bar, or `null`.
    Label header(Menu menu) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).menu == menu) {
                return headers.get(i).label;
            }
        }
        return null;
    }

    /// Returns the open popup of a menu of the bar, or `null`.
    ContextMenu popup(Menu menu) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).menu == menu) {
                return headers.get(i).link.popup();
            }
        }
        return null;
    }

    @Override
    protected double computePrefWidth(double height) {
        double w = 0;
        for (int i = 0; i < headers.size(); i++) {
            Header h = headers.get(i);
            if (h.menu.isVisible()) {
                w += h.label.prefWidth(-1);
            }
        }
        Insets in = getInsets();
        return in.getLeft() + w + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        double h = 0;
        for (int i = 0; i < headers.size(); i++) {
            h = Math.max(h, headers.get(i).label.prefHeight(-1));
        }
        Insets in = getInsets();
        return in.getTop() + (h <= 0 ? 24 : h) + in.getBottom();
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double x = in.getLeft();
        double y = in.getTop();
        double h = Math.max(0, getHeight() - y - in.getBottom());
        for (int i = 0; i < headers.size(); i++) {
            Header header = headers.get(i);
            boolean show = header.menu.isVisible();
            header.label.setVisible(show);
            if (show) {
                double w = header.label.prefWidth(-1);
                header.label.resizeRelocate(x, y, w, h);
                x += w;
            }
        }
    }

    /// One menu of the bar: the label that shows it and what follows the
    /// menu.
    private final class Header {
        final Menu menu;
        final Label label;
        final MenuLink link;
        private final ChangeListener<Object> sync = new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
                sync();
            }
        };
        private final ChangeListener<Boolean> showing = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue != null && !newValue.booleanValue()) {
                    closed(menu);
                }
            }
        };

        Header(Menu m) {
            menu = m;
            label = new Label();
            label.getStyleClass().setAll(m.getStyleClass());
            label.setFocusTraversable(false);
            label.setPadding(new Insets(4, 8, 4, 8));
            link = new MenuLink(m, label, Side.BOTTOM).attach();
            m.textProperty().addListener(sync);
            m.mnemonicParsingProperty().addListener(sync);
            m.disableProperty().addListener(sync);
            m.visibleProperty().addListener(sync);
            m.showingProperty().addListener(showing);
            label.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        event.consume();
                        pressed(menu);
                    }
                }
            });
            sync();
        }

        void sync() {
            label.setText(menu.shownText());
            label.setDisable(menu.isDisable());
            requestLayout();
        }

        void dispose() {
            menu.textProperty().removeListener(sync);
            menu.disableProperty().removeListener(sync);
            menu.visibleProperty().removeListener(sync);
            menu.showingProperty().removeListener(showing);
            link.detach();
        }
    }
}
