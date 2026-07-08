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
