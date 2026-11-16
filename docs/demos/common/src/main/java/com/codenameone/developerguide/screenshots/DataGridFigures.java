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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
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
        v.add(wide("datagrid-editing", grid(DataGridSnippets::editing), (form, g) -> g.editCellAt(2, 1)));
        v.add(wide("datagrid-tree", grid(DataGridSnippets::tree), (form, g) -> {
            g.expandAll();
            g.setSelectedRow(3);
        }));
        v.add(wide("datagrid-million", grid(DataGridSnippets::million), (form, g) -> {
            g.scrollToCell(500000, 0);
            g.setSelectedRow(500002);
        }));
        v.add(wide("datagrid-appearance", grid(DataGridSnippets::appearance), null));
        v.add(new FigureVariant(new Fig("datagrid-css", grid(DataGridSnippets::styled), null, true),
                FigureDevice.ANDROID, false, true, "datagrid-css.png"));
        Fig themed = new Fig("datagrid-appearance-theme", grid(DataGridSnippets::sorted), null, false);
        v.add(new FigureVariant(themed, FigureDevice.ANDROID, true, true, "datagrid-dark.png"));
        v.add(new FigureVariant(themed, FigureDevice.IOS, false, true, "datagrid-ios.png"));
        return v.toArray(new FigureVariant[v.size()]);
    }

    private interface GridScene {
        DataGrid build();
    }

    private static Scene grid(final GridScene s) {
        return () -> {
            Form f = new Form("Orders", new BorderLayout());
            f.add(BorderLayout.CENTER, s.build());
            return f;
        };
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
