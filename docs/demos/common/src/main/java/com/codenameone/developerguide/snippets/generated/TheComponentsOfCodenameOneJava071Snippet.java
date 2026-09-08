/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.table.*;

class TheComponentsOfCodenameOneJava071Snippet {
    void snippet() {
        // tag::the-components-of-codename-one-java-071[]
        Form hi = new Form("Table", new BorderLayout());
        TableModel model = new DefaultTableModel(
                new String[] {"Name", "Description", "Status"},
                new Object[][] {
                    {"Row 1", "A short value", "OK"},
                    {"Row 2", "A much longer value that wraps to multiple lines", "OK"},
                    {"Row 3", "Another long table cell that needs wrapping", "Pending"},
                });
        Table table = new Table(model) {
            @Override
            protected Component createCell(Object value, int row, int column, boolean editable) {
                if (row > -1 && column == 1) {
                    TextArea cell = new TextArea(String.valueOf(value));
                    cell.setUIID("TableCell");
                    cell.setEditable(false);
                    cell.setGrowByContent(true);
                    return cell;
                }
                return super.createCell(value, row, column, editable);
            }

            @Override
            protected TableLayout.Constraint createCellConstraint(Object value, int row, int column) {
                TableLayout.Constraint con = super.createCellConstraint(value, row, column);
                con.setWidthPercentage(column == 1 ? 50 : 25);
                return con;
            }
        };
        hi.add(BorderLayout.CENTER, table);
        hi.show();
        // end::the-components-of-codename-one-java-071[]
    }
}
