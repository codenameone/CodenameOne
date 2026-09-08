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

package com.codenameone.developerguide.screenshots;

import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.spinner.*;
import com.codename1.ui.table.*;

class ComponentsTablePinstripeFigure implements GuideFigure {

    

    @Override
    public String id() {
        return "components-table-pinstripe";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-070[]
        Form hi = new Form("Table", new BorderLayout());
        TableModel model = new DefaultTableModel(
        new String[] {"Col 1", "Col 2", "Col 3"},
        new Object[][] {
        {"Row 1", "Row A", "Row X"},
        {"Row 2", "Row B can now stretch", null},
        {"Row 3", "Row C", "Row Z"},
        {"Row 4", "Row D", "Row K"},
        }) {
        public boolean isCellEditable(int row, int col) {
        return col != 0;
        }
        };
        Table table = new Table(model) {
        @Override
        protected Component createCell(Object value, int row, int column, boolean editable) {
        Component cell;
        if (row == 1 && column == 1) {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_STRINGS);
        p.setStrings("Row B can now stretch", "This is a good value",
        "So Is This", "Better than text field");
        p.setSelectedString((String)value);
        p.setUIID("TableCell");
        p.addActionListener(e -> getModel().setValueAt(row, column, p.getSelectedString()));
        cell = p;
        } else {
        cell = super.createCell(value, row, column, editable);
        }
        if (row > -1 && row % 2 == 0) {
        cell.getAllStyles().setBgColor(0xeeeeee);
        cell.getAllStyles().setBgTransparency(255);
        }
        return cell;
        }

        @Override
        protected TableLayout.Constraint createCellConstraint(Object value, int row, int column) {
        TableLayout.Constraint con = super.createCellConstraint(value, row, column);
        if (row == 1 && column == 1) {
        con.setHorizontalSpan(2);
        }
        con.setWidthPercentage(33);
        return con;
        }
        };
        hi.add(BorderLayout.CENTER, table);
        hi.show();
        // end::the-components-of-codename-one-java-070[]
        return hi;
    }
}
