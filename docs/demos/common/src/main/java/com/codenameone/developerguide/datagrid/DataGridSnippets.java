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

import com.codename1.l10n.L10NManager;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Slider;
import com.codename1.ui.TextField;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.table.AbstractTableModel;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.DefaultGridCellRenderer;
import com.codename1.ui.table.DefaultTableModel;
import com.codename1.ui.table.GridAggregate;
import com.codename1.ui.table.GridCell;
import com.codename1.ui.table.GridCellEditor;
import com.codename1.ui.table.GridCellRenderer;
import com.codename1.ui.table.GridColumn;
import com.codename1.ui.table.GridFilter;
import com.codename1.ui.table.GridFormatter;
import com.codename1.ui.table.GridRow;
import com.codename1.ui.table.TreeTableModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

/// The samples of the data grid chapter. Each builds what the figure of the
/// same name shows, so the pictures are drawn by the code the chapter prints.
public final class DataGridSnippets {
    private DataGridSnippets() {
    }

    /// Model columns of the orders sample.
    public static final int ORDER = 0;
    public static final int CUSTOMER = 1;
    public static final int REGION = 2;
    public static final int PLACED = 3;
    public static final int AMOUNT = 4;
    public static final int SHIPPED = 5;
    public static final int PAID = 6;

    private static final String[] CUSTOMERS = {
        "Acme Corp", "Birch & Co", "Cobalt Labs", "Delta Freight", "Evergreen",
        "Fjord Foods", "Granite Bank", "Harbor Tools", "Indigo Print", "Juniper Health",
        "Kestrel Air", "Lumen Studio"
    };
    private static final String[] REGIONS = {"North", "East", "South", "West"};

    /// Noon UTC on 2026-03-02, so the dates read the same in every zone.
    private static final long FIRST_DAY = 1772452800000L;

    /// A fixed set of orders: the same rows every run, so a figure is the
    /// same picture every run.
    public static DefaultTableModel orders() {
        Object[][] rows = new Object[36][];
        for (int i = 0; i < rows.length; i++) {
            int k = (i * 7 + 3) % 36;
            rows[i] = new Object[]{
                "SO-" + (1040 + i),
                CUSTOMERS[(i * 5) % CUSTOMERS.length],
                REGIONS[(i * 3 + i / 4) % REGIONS.length],
                new Date(FIRST_DAY + (long) ((i * 11) % 28) * 24L * 3600000L),
                Double.valueOf(120 + (k * 397) % 4800 + (k % 4) * 0.25),
                Integer.valueOf((k * 29) % 101),
                Boolean.valueOf(k % 3 != 0)
            };
        }
        return new DefaultTableModel(
                new String[]{"Order", "Customer", "Region", "Placed", "Amount", "Shipped", "Paid"},
                rows, true);
    }

    public static Form firstGrid() {
        // tag::grid-first[]
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Name", "Team", "Score", "Active"},
                new Object[][]{
                        {"Dana", "Red", Integer.valueOf(7), Boolean.TRUE},
                        {"Ann", "Blue", Integer.valueOf(3), Boolean.FALSE},
                        {"Carl", "Red", Integer.valueOf(3), Boolean.TRUE},
                        {"Bea", "Green", Integer.valueOf(9), Boolean.TRUE},
                        {"Eli", "Blue", Integer.valueOf(5), Boolean.FALSE},
                }, true);
        DataGrid grid = new DataGrid(model);
        Form form = new Form("Players", new BorderLayout());
        form.add(BorderLayout.CENTER, grid);
        form.show();
        // end::grid-first[]
        return form;
    }

    public static DataGrid columns() {
        // tag::grid-columns[]
        DataGrid grid = new DataGrid(orders());
        grid.getColumn(ORDER).setWidthMM(20);
        grid.getColumn(CUSTOMER).setFlex(1).setMinWidthMM(24);
        grid.getColumn(REGION).setChoices("North", "East", "South", "West");
        grid.getColumn(AMOUNT)
                .setKind(GridColumn.Kind.NUMBER)
                .setWidthMM(20);
        grid.getColumn(SHIPPED).setTitle("Shipped %");
        // The order number stays in place while the rest scroll sideways.
        grid.setFrozenColumns(1);
        // end::grid-columns[]
        formatted(grid);
        return grid;
    }

    // tag::grid-formatter[]
    /// Amounts with a currency sign and two decimals, read back without them.
    static final class Money implements GridFormatter {
        @Override
        public String format(Object value) {
            if (!(value instanceof Number)) {
                return "";
            }
            double d = ((Number) value).doubleValue();
            long cents = Math.round(Math.abs(d) * 100);
            String fraction = Long.toString(100 + cents % 100).substring(1);
            String whole = L10NManager.getInstance().format((int) (cents / 100));
            return (d < 0 ? "-$" : "$") + whole + "." + fraction;
        }

        @Override
        public Object parse(String text, Object previous) {
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c >= '0' && c <= '9' || c == '.' || c == '-') {
                    digits.append(c);
                }
            }
            try {
                return Double.valueOf(digits.toString());
            } catch (NumberFormatException notANumber) {
                return previous;
            }
        }
    }

    /// Dates as day, month and year, in a zone of the app's choosing.
    static final class Day implements GridFormatter {
        private final SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy");

        Day() {
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public String format(Object value) {
            return value instanceof Date ? format.format((Date) value) : "";
        }

        @Override
        public Object parse(String text, Object previous) {
            return previous;
        }
    }
    // end::grid-formatter[]

    static void formatted(DataGrid grid) {
        // tag::grid-formatter-use[]
        grid.getColumn(AMOUNT).setFormatter(new Money());
        grid.getColumn(PLACED).setFormatter(new Day()).setEditable(false);
        // end::grid-formatter-use[]
    }

    // tag::grid-renderer[]
    /// Paints a shipped percentage as a bar with the number beside it.
    static final class ProgressRenderer implements GridCellRenderer {
        @Override
        public void paint(Graphics g, GridCell cell, int x, int y, int width, int height) {
            Object v = cell.getValue();
            if (cell.isGroupRow() || !(v instanceof Number)) {
                return;
            }
            int percent = Math.max(0, Math.min(100, ((Number) v).intValue()));
            Style s = cell.getStyle();
            int left = x + s.getPaddingLeftNoRTL();
            int inner = width - s.getPaddingLeftNoRTL() - s.getPaddingRightNoRTL();
            String text = percent + "%";
            int textW = s.getFont().stringWidth("100%");
            int barW = Math.max(0, inner - textW - height / 4);
            int barH = Math.max(2, height / 5);
            int barY = y + (height - barH) / 2;
            int alpha = g.getAlpha();
            g.setColor(s.getFgColor());
            g.setAlpha(alpha / 5);
            g.fillRoundRect(left, barY, barW, barH, barH, barH);
            g.setAlpha(alpha);
            g.setColor(percent == 100 ? 0x2e7d32 : 0x1e88e5);
            g.fillRoundRect(left, barY, barW * percent / 100, barH, barH, barH);
            g.setColor(s.getFgColor());
            g.setFont(s.getFont());
            g.drawString(text, x + width - s.getPaddingRightNoRTL() - s.getFont().stringWidth(text),
                    y + (height - s.getFont().getHeight()) / 2);
        }

        @Override
        public int getPreferredWidth(GridCell cell) {
            return cell.getStyle().getFont().stringWidth("100%") * 3;
        }
    }
    // end::grid-renderer[]

    public static DataGrid renderers() {
        DataGrid grid = columns();
        // tag::grid-renderer-use[]
        grid.getColumn(SHIPPED).setRenderer(new ProgressRenderer());
        // A renderer that changes one thing: unpaid amounts in red.
        grid.getColumn(AMOUNT).setRenderer(new DefaultGridCellRenderer() {
            @Override
            protected int textColor(GridCell cell, int defaultColor) {
                if (cell.isGroupRow() || cell.isRowSelected()) {
                    return defaultColor;
                }
                int modelRow = cell.getGrid().getModelRow(cell.getRow());
                Object paid = cell.getGrid().getTableModel().getValueAt(modelRow, PAID);
                return Boolean.FALSE.equals(paid) ? 0xc62828 : defaultColor;
            }
        });
        // end::grid-renderer-use[]
        return grid;
    }

    public static DataGrid sorted() {
        DataGrid grid = renderers();
        // tag::grid-sort[]
        // Newest first, then by customer for orders placed the same day.
        grid.sortBy(grid.getColumn(PLACED), false);
        grid.addSort(grid.getColumn(CUSTOMER), true);
        // end::grid-sort[]
        return grid;
    }

    public static DataGrid regionOrder() {
        DataGrid grid = renderers();
        // tag::grid-comparator[]
        // Regions in the order of the sales territories, not alphabetically.
        final List<String> territory = new ArrayList<String>();
        territory.add("North");
        territory.add("East");
        territory.add("South");
        territory.add("West");
        grid.getColumn(REGION).setComparator(new Comparator<Object>() {
            @Override
            public int compare(Object a, Object b) {
                return territory.indexOf(a) - territory.indexOf(b);
            }
        });
        grid.sortBy(grid.getColumn(REGION), true);
        // end::grid-comparator[]
        return grid;
    }

    public static Form search() {
        // tag::grid-search[]
        final DataGrid grid = renderers();
        final TextField search = new TextField("", "Search customers", 20, TextField.ANY);
        search.addDataChangedListener((type, index) -> {
            final String text = search.getText();
            if (text.length() == 0) {
                grid.setFilter(null);
                return;
            }
            grid.setFilter(new GridFilter() {
                @Override
                public boolean accept(GridRow row) {
                    Object customer = row.getValue(CUSTOMER);
                    return customer != null && customer.toString().toLowerCase()
                            .indexOf(text.toLowerCase()) >= 0;
                }
            });
        });
        Form form = new Form("Orders", new BorderLayout());
        form.add(BorderLayout.NORTH, search);
        form.add(BorderLayout.CENTER, grid);
        // end::grid-search[]
        return form;
    }

    // tag::grid-aggregate[]
    /// Counts the values that are false: in a Paid column, the unpaid orders.
    static final class CountFalse extends GridAggregate {
        @Override
        public Object fold(GridColumn column, Object soFar, Object value) {
            int n = soFar instanceof Integer ? ((Integer) soFar).intValue() : 0;
            return Integer.valueOf(Boolean.FALSE.equals(value) ? n + 1 : n);
        }

        @Override
        public Object finish(Object soFar) {
            int n = soFar instanceof Integer ? ((Integer) soFar).intValue() : 0;
            return n == 0 ? "all paid" : n + " unpaid";
        }
    }
    // end::grid-aggregate[]

    public static DataGrid grouped() {
        DataGrid grid = renderers();
        // tag::grid-group[]
        grid.getColumn(AMOUNT).setAggregate(GridAggregate.SUM);
        grid.getColumn(SHIPPED).setAggregate(GridAggregate.AVERAGE);
        grid.getColumn(ORDER).setAggregate(GridAggregate.COUNT);
        grid.getColumn(PAID).setAggregate(new CountFalse());
        grid.setGroupColumn(grid.getColumn(REGION));
        grid.sortBy(grid.getColumn(AMOUNT), false);
        // end::grid-group[]
        return grid;
    }

    public static DataGrid selection() {
        final DataGrid grid = renderers();
        // tag::grid-selection[]
        grid.setSelectionMode(DataGrid.SelectionMode.MULTIPLE_ROWS);
        grid.addSelectionListener((oldRow, newRow) -> {
            int[] rows = grid.getSelectedRows();
            System.out.println(rows.length + " selected");
        });
        // A double tap, or Enter, on a row that isn't edited.
        grid.addActionListener(e -> {
            int modelRow = grid.getModelRow(grid.getSelectedRow());
            System.out.println("open " + grid.getTableModel().getValueAt(modelRow, ORDER));
        });
        // end::grid-selection[]
        return grid;
    }

    // tag::grid-editor[]
    /// Edits a percentage with a slider rather than a text field.
    static final class PercentEditor implements GridCellEditor {
        @Override
        public Component createEditor(DataGrid grid, GridCell cell) {
            Slider s = new Slider();
            s.setEditable(true);
            s.setMinValue(0);
            s.setMaxValue(100);
            Object v = cell.getValue();
            s.setProgress(v instanceof Number ? ((Number) v).intValue() : 0);
            // The edit ends when the finger lifts.
            s.addActionListener(e -> grid.commitEdit());
            return s;
        }

        @Override
        public Object getEditorValue(Component editor, GridCell cell) {
            return Integer.valueOf(((Slider) editor).getProgress());
        }
    }
    // end::grid-editor[]

    public static DataGrid editing() {
        DataGrid grid = renderers();
        // tag::grid-editing[]
        grid.getColumn(ORDER).setEditable(false);
        grid.getColumn(SHIPPED).setEditor(new PercentEditor());
        // end::grid-editing[]
        return grid;
    }

    // tag::grid-million[]
    /// A million rows computed when they're painted: nothing is stored.
    static final class Ledger extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return 1000000;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int i) {
            return i == 0 ? "Entry" : i == 1 ? "Account" : "Balance";
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return Integer.valueOf(row + 1);
            }
            if (column == 1) {
                return "ACC-" + (1000 + row % 997);
            }
            return Double.valueOf((row * 7919L % 200000) / 100.0 - 1000);
        }

        @Override
        public void setValueAt(int row, int column, Object o) {
        }

        // The rows never change, so there is no one to tell.
        @Override
        public void addDataChangeListener(DataChangedListener d) {
        }

        @Override
        public void removeDataChangeListener(DataChangedListener d) {
        }
    }
    // end::grid-million[]

    public static DataGrid million() {
        // tag::grid-million-use[]
        DataGrid grid = new DataGrid(new Ledger());
        grid.getColumn(1).setFlex(1);
        grid.getColumn(2).setFormatter(new Money());
        // end::grid-million-use[]
        return grid;
    }

    public static DataGrid appearance() {
        DataGrid grid = renderers();
        // tag::grid-appearance[]
        grid.setStriped(false);
        grid.setGridLines(true, true);
        grid.setRowHeightMM(9);
        grid.setEmptyText("No orders match");
        // end::grid-appearance[]
        return grid;
    }

    public static DataGrid styled() {
        DataGrid grid = renderers();
        grid.setGridLines(true, false);
        return grid;
    }

    // tag::grid-tree[]
    /// A folder or a file: what the tree model below shows.
    public static final class Item {
        final String name;
        final long size;
        final String kind;
        final List<Item> children;

        Item(String name, long size, String kind) {
            this.name = name;
            this.size = size;
            this.kind = kind;
            this.children = null;
        }

        Item(String name, Item... children) {
            this.name = name;
            this.kind = "Folder";
            this.children = new ArrayList<Item>();
            long total = 0;
            for (Item c : children) {
                this.children.add(c);
                total += c.size;
            }
            this.size = total;
        }
    }

    /// The tree model: the grid asks it for a node's children only when the
    /// node first opens.
    public static final class Files implements TreeTableModel {
        private final List<Item> roots;

        public Files(List<Item> roots) {
            this.roots = roots;
        }

        @Override
        public Vector getChildren(Object parent) {
            List<Item> list = parent == null ? roots : ((Item) parent).children;
            Vector v = new Vector();
            if (list != null) {
                for (Item i : list) {
                    v.addElement(i);
                }
            }
            return v;
        }

        @Override
        public boolean isLeaf(Object node) {
            return ((Item) node).children == null;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Name" : column == 1 ? "Kind" : "Size (KB)";
        }

        @Override
        public Object getValueAt(Object node, int column) {
            Item i = (Item) node;
            return column == 0 ? i.name : column == 1 ? i.kind : Long.valueOf(i.size);
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return false;
        }

        @Override
        public void setValueAt(Object node, int column, Object value) {
        }
    }
    // end::grid-tree[]

    public static List<Item> project() {
        List<Item> roots = new ArrayList<Item>();
        roots.add(new Item("src",
                new Item("main",
                        new Item("App.java", 14, "Java"),
                        new Item("Orders.java", 22, "Java"),
                        new Item("Grid.java", 31, "Java")),
                new Item("test",
                        new Item("OrdersTest.java", 9, "Java"))));
        roots.add(new Item("css",
                new Item("theme.css", 6, "CSS")));
        roots.add(new Item("images",
                new Item("logo.png", 48, "Image"),
                new Item("splash.png", 212, "Image")));
        roots.add(new Item("README.md", 3, "Text"));
        roots.add(new Item("pom.xml", 5, "XML"));
        return roots;
    }

    public static DataGrid tree() {
        // tag::grid-tree-use[]
        DataGrid grid = new DataGrid(new Files(project()));
        grid.getColumn(0).setFlex(1);
        grid.getColumn(2).setWidthMM(18);
        // end::grid-tree-use[]
        return grid;
    }
}
