package com.codename1.ui.table;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

class TablePackageTest extends UITestBase {

    @FormTest
    void testSortableTableModelOrdersRows() {
        Object[][] data = new Object[][]{
                {"Alice", 3},
                {"Bob", 1},
                {"Carol", 2}
        };
        String[] columns = new String[]{"name", "score"};
        DefaultTableModel model = new DefaultTableModel(columns, data);

        Comparator<Object> comparator = new Comparator<Object>() {
            public int compare(Object left, Object right) {
                return ((Comparable) left).compareTo(right);
            }
        };

        SortableTableModel ascending = new SortableTableModel(1, false, model, comparator);
        assertEquals("Bob", ascending.getValueAt(0, 0));
        assertEquals("Carol", ascending.getValueAt(1, 0));
        assertEquals(1, ascending.getSortedPosition(0));

        SortableTableModel descending = new SortableTableModel(1, true, model, comparator);
        assertEquals("Alice", descending.getValueAt(0, 0));
        assertEquals("Carol", descending.getValueAt(1, 0));
    }

    @FormTest
    void testTableLayoutConstraintBuilders() {
        TableLayout layout = new TableLayout(2, 2);
        Container container = new Container(layout);
        TableLayout.Constraint constraint = layout.createConstraint(1, 0)
                .widthPercentage(50)
                .heightPercentage(25)
                .horizontalSpan(2)
                .verticalSpan(1);
        constraint.horizontalAlign(Component.CENTER);
        constraint.verticalAlign(Component.BOTTOM);

        Label value = new Label("Value");
        container.addComponent(constraint, value);

        TableLayout.Constraint stored = (TableLayout.Constraint) layout.getComponentConstraint(value);
        assertNotNull(stored);
        assertEquals(1, stored.getRow());
        assertEquals(0, stored.getColumn());
        assertEquals(50, stored.getWidthPercentage());
        assertEquals(25, stored.getHeightPercentage());
        assertEquals(Component.CENTER, stored.getHorizontalAlign());
        assertEquals(Component.BOTTOM, stored.getVerticalAlign());

        Table table = new Table(modelFrom());
        table.setModel(modelFrom());
        assertNotNull(table.getModel());
        table.setDrawBorder(true);
        assertTrue(table.isDrawBorder());
    }

    private TableModel modelFrom() {
        Object[][] values = new Object[][]{{"Cell", 1}};
        return new DefaultTableModel(new String[]{"name", "value"}, values);
    }
}
