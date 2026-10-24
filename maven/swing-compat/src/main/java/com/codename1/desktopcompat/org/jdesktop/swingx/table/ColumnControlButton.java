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
package com.codename1.desktopcompat.org.jdesktop.swingx.table;

import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JCheckBoxMenuItem;
import com.codename1.desktopcompat.javax.swing.JMenuItem;
import com.codename1.desktopcompat.javax.swing.JPopupMenu;
import com.codename1.desktopcompat.javax.swing.table.TableColumn;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXTable;
import java.util.List;

/// The button a `JXTable` offers as its column control. A press opens
/// a popup menu with a check box item per hideable column, which shows
/// or hides that column, and below them items that pack all columns and
/// switch horizontal scrolling.
///
/// The table records this button as the upper trailing corner of its
/// scroll pane, but the layer's scroll pane shows no corners: put the
/// button, which [JXTable#getColumnControl()] answers, into a tool bar
/// or wherever the application has room for it.
///
/// The last visible column cannot be hidden from the menu. The menu's
/// texts are English.
public class ColumnControlButton extends JButton {

    public static final String COLUMN_CONTROL_MARKER = "column.";
    public static final String COLUMN_CONTROL_BUTTON_ICON_KEY = "ColumnControlButton.actionIcon";
    public static final String COLUMN_CONTROL_BUTTON_MARGIN_KEY = "ColumnControlButton.margin";

    private final JXTable cn1Table;
    private JPopupMenu cn1Popup;
    private boolean cn1AdditionalActions = true;

    public ColumnControlButton(JXTable table) {
        this(table, null);
    }

    public ColumnControlButton(JXTable table, Icon icon) {
        super();
        cn1Table = table;
        if (icon != null) {
            setIcon(icon);
        } else {
            setText("...");
        }
        setFocusable(false);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePopup();
            }
        });
    }

    /// Opens the menu under the button, or closes it when it is open.
    public void togglePopup() {
        if (cn1Popup != null && cn1Popup.isVisible()) {
            cn1Popup.setVisible(false);
            return;
        }
        if (!canControl()) {
            return;
        }
        cn1Popup = cn1BuildPopup();
        cn1Popup.show(this, 0, getHeight());
    }

    public boolean getAdditionalActionsVisible() {
        return cn1AdditionalActions;
    }

    /// Whether the menu has the pack and scroll items below the columns.
    public void setAdditionalActionsVisible(boolean additionalActionsVisible) {
        boolean old = cn1AdditionalActions;
        cn1AdditionalActions = additionalActionsVisible;
        firePropertyChange("additionalActionsVisible", old, additionalActionsVisible);
    }

    /// Whether there is a table whose columns can be controlled.
    protected boolean canControl() {
        return cn1Table != null;
    }

    /// The menu as it would open now.
    JPopupMenu cn1BuildPopup() {
        JPopupMenu menu = new JPopupMenu();
        List<TableColumn> columns = cn1Table.getColumns(true);
        for (int i = 0; i < columns.size(); i++) {
            TableColumn c = columns.get(i);
            if (!(c instanceof TableColumnExt)) {
                continue;
            }
            final TableColumnExt column = (TableColumnExt) c;
            if (!column.isHideable()) {
                continue;
            }
            String title = column.getTitle();
            final JCheckBoxMenuItem item = new JCheckBoxMenuItem(title != null ? title : "", column.isVisible());
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean show = item.getState();
                    if (!show && column.isVisible() && cn1Table.getColumnCount() <= 1) {
                        item.setState(true);
                        return;
                    }
                    column.setVisible(show);
                }
            });
            menu.add(item);
        }
        if (cn1AdditionalActions) {
            if (menu.getComponentCount() > 0) {
                menu.addSeparator();
            }
            final JCheckBoxMenuItem scroll = new JCheckBoxMenuItem("Horizontal Scroll",
                    cn1Table.isHorizontalScrollEnabled());
            scroll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cn1Table.setHorizontalScrollEnabled(scroll.getState());
                }
            });
            menu.add(scroll);
            JMenuItem packAll = new JMenuItem("Pack All Columns");
            packAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cn1Table.packAll();
                }
            });
            menu.add(packAll);
            JMenuItem packSelected = new JMenuItem("Pack Selected Column");
            packSelected.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cn1Table.packSelected();
                }
            });
            menu.add(packSelected);
        }
        return menu;
    }
}
