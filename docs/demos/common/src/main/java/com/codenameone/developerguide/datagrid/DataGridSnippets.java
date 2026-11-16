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
package com.codenameone.developerguide.datagrid;

import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.DefaultTableModel;
import com.codename1.ui.table.GridAggregate;
import com.codename1.ui.table.GridColumn;
import com.codename1.ui.table.GridFilter;
import com.codename1.ui.table.GridRow;
import com.codename1.ui.table.TreeTableModel;

import java.util.Vector;

public class DataGridSnippets {
    public void firstGrid(Form form) {
        // tag::grid-first[]
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Name", "Team", "Score", "Active"},
                new Object[][]{
                        {"Dana", "Red", Integer.valueOf(7), Boolean.TRUE},
                        {"Ann", "Blue", Integer.valueOf(3), Boolean.FALSE},
                        {"Carl", "Red", Integer.valueOf(3), Boolean.TRUE},
                }, true);
        DataGrid grid = new DataGrid(model);
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, grid);
        // end::grid-first[]
    }

    public void columns(DataGrid grid) {
        // tag::grid-columns[]
        grid.getColumn(0).setFlex(1);
        grid.getColumn(2)
                .setKind(GridColumn.Kind.NUMBER)
                .setWidthMM(18)
                .setAggregate(GridAggregate.SUM);
        grid.getColumn(1).setChoices("Red", "Blue", "Green");
        grid.setFrozenColumns(1);
        // end::grid-columns[]
    }

    public void sortFilterGroup(DataGrid grid) {
        // tag::grid-sort-filter[]
        grid.sortBy(grid.getColumn(2), false);
        grid.addSort(grid.getColumn(0), true);
        grid.setFilter(new GridFilter() {
            @Override
            public boolean accept(GridRow row) {
                return Boolean.TRUE.equals(row.getValue(3));
            }
        });
        grid.setGroupColumn(grid.getColumn(1));
        // end::grid-sort-filter[]
    }

    // tag::grid-tree[]
    static class FolderModel implements TreeTableModel {
        @Override
        public Vector getChildren(Object parent) {
            // null asks for the roots; called once per node, when it opens
            return Folders.list((Folder) parent);
        }

        @Override
        public boolean isLeaf(Object node) {
            return !((Folder) node).isDirectory();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Name" : "Size";
        }

        @Override
        public Object getValueAt(Object node, int column) {
            Folder f = (Folder) node;
            return column == 0 ? f.getName() : Long.valueOf(f.getSize());
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return column == 0;
        }

        @Override
        public void setValueAt(Object node, int column, Object value) {
            ((Folder) node).rename((String) value);
        }
    }
    // end::grid-tree[]

    public void selection(DataGrid grid) {
        // tag::grid-selection[]
        grid.setSelectionMode(DataGrid.SelectionMode.MULTIPLE_ROWS);
        grid.addSelectionListener((oldRow, newRow) -> {
            int[] rows = grid.getSelectedRows();
            System.out.println(rows.length + " selected");
        });
        grid.addActionListener(e -> {
            int modelRow = grid.getModelRow(grid.getSelectedRow());
            System.out.println("open " + modelRow);
        });
        // end::grid-selection[]
    }

    interface Folder {
        String getName();

        long getSize();

        boolean isDirectory();

        void rename(String name);
    }

    static final class Folders {
        private Folders() {
        }

        static Vector list(Folder parent) {
            return new Vector();
        }
    }
}
