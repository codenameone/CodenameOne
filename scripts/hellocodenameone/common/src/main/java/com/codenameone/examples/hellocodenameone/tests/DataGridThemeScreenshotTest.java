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
import com.codename1.ui.table.DefaultTableModel;
import com.codename1.ui.table.GridAggregate;
import com.codename1.ui.table.GridColumn;

/// A flat grid, sorted, grouped by team with a sum on the group rows, a frozen
/// first column and a selected row.
public class DataGridThemeScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.

    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=DataGridTheme status=SKIPPED reason=data-grid-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "DataGridTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    static DefaultTableModel people() {
        String[] names = {"Dana", "Ann", "Carl", "Bea", "Eli", "Fay", "Gus", "Hana", "Ivo", "June", "Kai", "Lea"};
        String[] teams = {"Red", "Blue", "Green"};
        Object[][] rows = new Object[names.length][];
        for (int i = 0; i < names.length; i++) {
            rows[i] = new Object[]{names[i], teams[i % 3], Integer.valueOf((i * 37) % 50 + 3),
                    Boolean.valueOf(i % 4 != 1), "Note " + (i + 1)};
        }
        return new DefaultTableModel(new String[]{"Name", "Team", "Score", "Active", "Notes"}, rows, true);
    }

    @Override
    protected void populate(Form form, String suffix) {
        DataGrid grid = new DataGrid(people());
        grid.getColumn(2).setKind(GridColumn.Kind.NUMBER).setAggregate(GridAggregate.SUM);
        grid.getColumn(4).setFlex(1);
        grid.setFrozenColumns(1);
        grid.sortBy(grid.getColumn(2), false);
        grid.setGroupColumn(grid.getColumn(1));
        grid.setSelectedRow(2);
        form.add(BorderLayout.CENTER, grid);
    }
}
