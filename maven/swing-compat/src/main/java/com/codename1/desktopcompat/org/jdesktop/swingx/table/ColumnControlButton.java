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

import com.codename1.desktopcompat.javax.swing.Icon;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.org.jdesktop.swingx.JXTable;

/// The button a `JXTable` offers for its scroll pane's corner.
///
/// In SwingX it opens a popup menu that shows and hides columns. The
/// layer has no popup menu, so this button opens nothing: it exists so
/// that code which makes, keeps or replaces the column control compiles
/// and runs. Hide and show columns with
/// [TableColumnExt#setVisible(boolean)].
public class ColumnControlButton extends JButton {

    public static final String COLUMN_CONTROL_MARKER = "column.";
    public static final String COLUMN_CONTROL_BUTTON_ICON_KEY = "ColumnControlButton.actionIcon";
    public static final String COLUMN_CONTROL_BUTTON_MARGIN_KEY = "ColumnControlButton.margin";

    private final JXTable cn1Table;

    public ColumnControlButton(JXTable table) {
        this(table, null);
    }

    public ColumnControlButton(JXTable table, Icon icon) {
        super();
        cn1Table = table;
        if (icon != null) {
            setIcon(icon);
        }
        setFocusable(false);
    }

    /// Whether there is a table whose columns could be controlled.
    protected boolean canControl() {
        return cn1Table != null;
    }
}
