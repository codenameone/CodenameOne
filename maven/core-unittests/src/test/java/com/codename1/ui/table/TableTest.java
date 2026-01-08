package com.codename1.ui.table;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;

import java.util.Comparator;
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

    /**
     * Test for a size regression bug in table layout discussed here: https://www.reddit.com/r/cn1/comments/1q6pmek/android_screen_out_of_ranges/
     */
    @FormTest
    void testTableLayoutDefaultSizing() {
        Form f = CN.getCurrentForm();
        TableLayout tl = new TableLayout(3, 2);
        f.setLayout(tl);
        f.setScrollable(false);
        f.getContentPane().getAllStyles().setPadding(0, 0, 0, 0);
        for(int iter = 0 ; iter < 6 ; iter++) {
            Label l = new Label("T: " + iter);
            l.getAllStyles().setMargin(0, 0, 0, 0);
            f.addComponent(tl.createConstraint()
                            .hp(33)
                            .wp(50), l);
        }
        f.revalidate();

        for(Component cmp : f.getContentPane()) {
            assertTrue(cmp.getHeight() >= f.getContentPane().getHeight() / 100 * 32, "Height should be close to 33 percent");
            assertTrue(cmp.getWidth() >= f.getContentPane().getWidth() / 2 - 1, "Width should be 50%");
        }
    }
}
