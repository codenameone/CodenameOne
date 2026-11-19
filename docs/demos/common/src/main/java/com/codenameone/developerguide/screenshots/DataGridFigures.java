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
package com.codenameone.developerguide.screenshots;

import com.codename1.spreadsheet.FormulaBar;
import com.codename1.spreadsheet.Workbook;
import com.codename1.ui.CodeEditor;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.util.Resources;
import com.codenameone.developerguide.datagrid.DataGridSnippets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/// The figures of the data grid chapter. Each draws what a sample of
/// `DataGridSnippets` builds, from a fixed data set, so a figure is the same
/// picture every run.
public final class DataGridFigures {
    private DataGridFigures() {
    }

    private interface Scene {
        Form build();
    }

    private interface Act {
        void run(Form form, DataGrid grid);
    }

    private static final class Fig implements GuideFigure {
        private final String id;
        private final Scene scene;
        private final Act act;
        private final boolean styled;

        Fig(String id, Scene scene, Act act, boolean styled) {
            this.id = id;
            this.scene = scene;
            this.act = act;
            this.styled = styled;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Form build() {
            if (styled) {
                overlay("DataGrid");
            }
            return scene.build();
        }

        @Override
        public void afterShow(Form form) {
            DataGrid grid = find(form.getContentPane());
            if (act != null && grid != null) {
                act.run(form, grid);
            }
            form.getContentPane().repaint();
        }

        @Override
        public boolean fillsViewport() {
            return true;
        }

        @Override
        public boolean changesTheme() {
            return styled;
        }
    }

    public static FigureVariant[] variants() {
        List<FigureVariant> v = new ArrayList<FigureVariant>();
        v.add(new FigureVariant(new Fig("datagrid-first", DataGridSnippets::firstGrid, null, false),
                FigureDevice.ANDROID, false, "datagrid-first.png"));
        v.add(wide("datagrid-columns", grid(DataGridSnippets::columns), null));
        v.add(wide("datagrid-renderers", grid(DataGridSnippets::renderers), null));
        v.add(wide("datagrid-sorted", grid(DataGridSnippets::sorted), null));
        v.add(wide("datagrid-search", DataGridSnippets::search, (form, g) -> {
            TextField search = (TextField) ((BorderLayout) form.getContentPane().getLayout()).getNorth();
            search.setText("co");
        }));
        v.add(wide("datagrid-grouped", grid(DataGridSnippets::grouped), null));
        v.add(wide("datagrid-selection", grid(DataGridSnippets::selection), (form, g) -> {
            g.selectRows(1, 3, false);
            g.selectRows(6, 6, true);
        }));
        v.add(wide("datagrid-editing", grid(DataGridSnippets::editing), (form, g) -> {
            // The simulator edits in a native field that a capture can't
            // see, so the figure shows the edit once it has been written
            // back, the way the cell looks when the field closes.
            g.getTableModel().setValueAt(2, 1, "Kestrel Aviation");
            g.setSelectedRow(2);
        }));
        v.add(wide("datagrid-tree", grid("Files", DataGridSnippets::tree), (form, g) -> {
            g.expandAll();
            g.setSelectedRow(3);
        }));
        v.add(wide("datagrid-million", grid("Ledger", DataGridSnippets::million), (form, g) -> {
            g.scrollToCell(500000, 0);
            g.setSelectedRow(500002);
        }));
        v.add(wide("datagrid-appearance", grid(DataGridSnippets::appearance), null));
        v.add(new FigureVariant(new Fig("datagrid-css", grid(DataGridSnippets::styled), null, true),
                FigureDevice.ANDROID, false, true, "datagrid-css.png"));
        Fig themed = new Fig("datagrid-appearance-theme", grid(DataGridSnippets::sorted), null, false);
        v.add(new FigureVariant(themed, FigureDevice.ANDROID, true, true, "datagrid-dark.png"));
        v.add(new FigureVariant(themed, FigureDevice.IOS, false, true, "datagrid-ios.png"));
        v.add(wide("datagrid-sheet-orders", book("Regional sales", DataGridSnippets::salesWorkbook, null, 0), null));
        v.add(wide("datagrid-sheet-summary", book("Regional sales", DataGridSnippets::salesWorkbook, null, 2), null));
        v.add(wide("datagrid-sheet-recalc",
                book("Regional sales", DataGridSnippets::salesWorkbook, DataGridSnippets::correctAnOrder, 2), null));
        v.add(wide("datagrid-sheet-formula-hint", book("Regional sales", DataGridSnippets::salesWorkbook, null, 2),
                (form, g) -> type(form, g, 0, 5, "=ROUND(D2*VLOOKUP(A2, Rates!$A$2:$C$5, 2, FALSE), ")));
        v.add(wide("datagrid-sheet-formula-complete", book("Regional sales", DataGridSnippets::salesWorkbook, null, 2),
                (form, g) -> type(form, g, 1, 5, "=ROU")));
        v.add(wide("datagrid-sheet-formula-bar", () -> {
            Workbook workbook;
            try {
                workbook = DataGridSnippets.salesWorkbook();
            } catch (IOException err) {
                throw new IllegalStateException("the guide's workbook did not load", err);
            }
            Form f = new Form("Regional sales", new BorderLayout());
            f.add(BorderLayout.CENTER, DataGridSnippets.formulaBar(workbook.getSheet(0)));
            return f;
        }, (form, g) -> {
            tap(g, 3, 7);
            FormulaBar bar = findBar(form);
            settle();
            bar.setText("=E5*F5*(1-G5)*(1+");
            settle();
        }));
        v.add(wide("datagrid-sheet-loan", book("Loan", DataGridSnippets::loanWorkbook, null, 1), null));
        v.add(wide("datagrid-sheet-loan-rate",
                book("Loan", DataGridSnippets::loanWorkbook, DataGridSnippets::raiseTheRate, 1), null));
        return v.toArray(new FigureVariant[v.size()]);
    }

    private interface GridScene {
        DataGrid build();
    }

    private static Scene grid(final GridScene s) {
        return grid("Orders", s);
    }

    private static Scene grid(final String title, final GridScene s) {
        return () -> {
            Form f = new Form(title, new BorderLayout());
            f.add(BorderLayout.CENTER, s.build());
            return f;
        };
    }

    private interface BookSource {
        Workbook open() throws IOException;
    }

    private interface BookEdit {
        void apply(Workbook book);
    }

    /// A workbook of the guide's resources with a tab per sheet. The edit is made
    /// after the grids exist, the way an application makes one: what the figure
    /// shows is then what the recalculation told the grid to repaint.
    private static Scene book(final String title, final BookSource source, final BookEdit edit, final int sheet) {
        return () -> {
            Workbook workbook;
            try {
                workbook = source.open();
            } catch (IOException err) {
                throw new IllegalStateException("the guide's workbook did not load", err);
            }
            Tabs tabs = DataGridSnippets.workbookTabs(workbook);
            if (edit != null) {
                edit.apply(workbook);
            }
            tabs.setSelectedIndex(sheet);
            Form f = new Form(title, new BorderLayout());
            f.add(BorderLayout.CENTER, tabs);
            return f;
        };
    }

    /// Edits a cell and types into it, the way a user does: the editor is painted by Codename
    /// One, so the capture shows the text, its colors and the popup under it.
    private static void type(Form form, DataGrid first, int row, int column, String text) {
        DataGrid grid = first;
        Tabs tabs = findTabs(form.getContentPane());
        if (tabs != null && tabs.getSelectedComponent() instanceof DataGrid) {
            // the sheet the tabs show, not the first one
            grid = (DataGrid) tabs.getSelectedComponent();
        }
        grid.editCellAt(row, column);
        CodeEditor editor = findEditor(form.getFormLayeredPane(DataGrid.class, true));
        // the editor's backend comes up on the EDT after it is shown; type once it is there
        settle();
        editor.setText("");
        editor.insertAtCursor(text);
        settle();
    }

    private static void settle() {
        for (int i = 0; i < 3; i++) {
            // a block that takes a moment, so the EDT runs what is queued while it waits
            Display.getInstance().invokeAndBlock(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    private static void tap(DataGrid grid, int row, int column) {
        int[] b = new int[4];
        grid.getCellBounds(row, column, b);
        int x = grid.getAbsoluteX() + b[0] + b[2] / 2;
        int y = grid.getAbsoluteY() + b[1] + b[3] / 2;
        grid.pointerPressed(x, y);
        grid.pointerReleased(x, y);
    }

    private static CodeEditor findEditor(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof CodeEditor) {
                return (CodeEditor) k;
            }
            if (k instanceof Container) {
                CodeEditor e = findEditor((Container) k);
                if (e != null) {
                    return e;
                }
            }
        }
        return null;
    }

    private static Tabs findTabs(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof Tabs) {
                return (Tabs) k;
            }
            if (k instanceof Container) {
                Tabs t = findTabs((Container) k);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    private static FormulaBar findBar(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof FormulaBar) {
                return (FormulaBar) k;
            }
            if (k instanceof Container) {
                FormulaBar b = findBar((Container) k);
                if (b != null) {
                    return b;
                }
            }
        }
        return null;
    }

    private static FigureVariant wide(String name, Scene scene, Act act) {
        return new FigureVariant(new Fig(name, scene, act, false), FigureDevice.ANDROID, false, true, name + ".png");
    }

    private static DataGrid find(Container c) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof DataGrid) {
                return (DataGrid) k;
            }
            if (k instanceof Container) {
                DataGrid g = find((Container) k);
                if (g != null) {
                    return g;
                }
            }
        }
        return null;
    }

    /// Lays the guide stylesheet's rules for the grid's UIIDs over the
    /// installed theme, so the styled figure is drawn by the CSS the chapter
    /// prints.
    private static void overlay(String prefix) {
        try {
            Resources res = Resources.open("/guide-snippets-theme.res");
            Hashtable<?, ?> theme = res.getTheme(res.getThemeResourceNames()[0]);
            Hashtable<String, Object> rules = new Hashtable<String, Object>();
            for (Object k : theme.keySet()) {
                String key = (String) k;
                if (key.startsWith(prefix)) {
                    rules.put(key, theme.get(k));
                }
            }
            if (rules.isEmpty()) {
                throw new IllegalStateException("the guide's stylesheet has no rule for " + prefix);
            }
            UIManager.getInstance().addThemeProps(rules);
        } catch (IOException err) {
            throw new IllegalStateException("the guide's stylesheet did not load", err);
        }
    }
}
