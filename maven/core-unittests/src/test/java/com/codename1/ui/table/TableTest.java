package com.codename1.ui.table;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.TextArea;

import java.util.Comparator;
import java.util.Date;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class TableTest extends UITestBase {

    @FormTest
    void testEditingUpdatesModel() throws Exception {
        Object[][] data = {{"Value"}};
        String[] columns = {"Col"};
        DefaultTableModel model = new DefaultTableModel(columns, data, true);
        Table table = new Table(model);

        // Find cell component (TextField)
        Component cell = findCellComponent(table, 0, 0);
        assertTrue(cell instanceof TextArea, "Cell should be editable TextArea/TextField but was " + cell.getClass().getName());
        TextArea ta = (TextArea) cell;

        ta.setText("NewValue");

        // Use reflection to fire action event
        Method fireAction = TextArea.class.getDeclaredMethod("fireActionEvent");
        fireAction.setAccessible(true);
        fireAction.invoke(ta);

        assertEquals("NewValue", model.getValueAt(0, 0));
    }

    @FormTest
    void testComparatorCoverage() {
        Table table = new Table(new DefaultTableModel(new String[]{"Col"}, new Object[][]{{"A"}}));
        Comparator cmp = table.createColumnSortComparator(0);

        // String comparison
        assertTrue(cmp.compare("A", "B") < 0);
        assertTrue(cmp.compare("B", "A") > 0);
        assertEquals(0, cmp.compare("A", "A"));

        // Number comparison
        assertTrue(cmp.compare(1, 2) < 0);
        assertTrue(cmp.compare(2.5, 1.5) > 0);

        // Null comparison
        assertTrue(cmp.compare(null, "A") < 0);
        assertTrue(cmp.compare("A", null) > 0);
        assertEquals(0, cmp.compare(null, null));
    }

    private Component findCellComponent(Table table, int row, int column) {
        TableLayout tl = (TableLayout) table.getLayout();
        int componentRow = row + (table.isIncludeHeader() ? 1 : 0);
        return tl.getComponentAt(componentRow, column);
    }
}
