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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.TreeTableModel;

import java.util.HashMap;
import java.util.Vector;

/// A tree grid with two levels open and one closed.
public class TreeGridThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=TreeGridTheme status=SKIPPED reason=data-grid-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "TreeGridTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    static final class Files implements TreeTableModel {
        private final HashMap<String, String[]> kids = new HashMap<String, String[]>();
        private final HashMap<String, Integer> sizes = new HashMap<String, Integer>();

        Files() {
            kids.put("", new String[]{"src", "docs", "build.xml", "README.md"});
            kids.put("src", new String[]{"main", "test"});
            kids.put("main", new String[]{"App.java", "Model.java", "View.java"});
            kids.put("test", new String[]{"AppTest.java"});
            kids.put("docs", new String[]{"guide.adoc", "api.adoc"});
            String[] all = {"build.xml", "README.md", "App.java", "Model.java", "View.java", "AppTest.java",
                    "guide.adoc", "api.adoc"};
            for (int i = 0; i < all.length; i++) {
                sizes.put(all[i], Integer.valueOf(1200 + i * 731));
            }
        }

        @Override
        public Vector getChildren(Object parent) {
            String[] k = kids.get(parent == null ? "" : parent.toString());
            Vector v = new Vector();
            if (k != null) {
                for (int i = 0; i < k.length; i++) {
                    v.addElement(k[i]);
                }
            }
            return v;
        }

        @Override
        public boolean isLeaf(Object node) {
            return !kids.containsKey(node.toString());
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? "Name" : column == 1 ? "Size" : "Kind";
        }

        @Override
        public Object getValueAt(Object node, int column) {
            String n = node.toString();
            if (column == 0) {
                return n;
            }
            if (column == 1) {
                return sizes.get(n);
            }
            return isLeaf(node) ? "File" : "Folder";
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return false;
        }

        @Override
        public void setValueAt(Object node, int column, Object value) {
        }
    }

    @Override
    protected void populate(Form form, String suffix) {
        DataGrid grid = new DataGrid(new Files());
        grid.getColumn(0).setFlex(1);
        grid.setNodeExpanded("src", true);
        grid.setNodeExpanded("main", true);
        grid.setSelectedRow(3);
        form.add(BorderLayout.CENTER, grid);
    }
}
