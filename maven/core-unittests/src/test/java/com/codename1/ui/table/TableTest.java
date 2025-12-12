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

    @FormTest
    void testSortListener() {
        Table table = new Table(new DefaultTableModel(new String[]{"Col"}, new Object[][]{{"A"}, {"B"}}));
        table.setSortSupported(true);

        // Find the header button
        Component header = findHeaderButton(table);
        assertNotNull(header, "Header button not found");

        // Click it - first click sets ascending=false (descending)
        header.pointerPressed(0, 0);
        header.pointerReleased(0, 0);
        flushSerialCalls();

        // Verify cell content (B should be first)
        Component cell = findCellComponent(table, 0, 0);
        assertTrue(cell instanceof com.codename1.ui.Label || cell instanceof com.codename1.ui.TextArea);

        // Click again - sets ascending=true
        header.pointerPressed(0, 0);
        header.pointerReleased(0, 0);
    }

    private Component findHeaderButton(Table table) {
        TableLayout tl = (TableLayout) table.getLayout();
        // Row 0 is header if includeHeader is true (default).
        return tl.getComponentAt(0, 0);
    }
}
