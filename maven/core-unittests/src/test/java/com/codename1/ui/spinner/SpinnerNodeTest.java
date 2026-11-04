package com.codename1.ui.spinner;

import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.list.DefaultListModel;
import static org.junit.jupiter.api.Assertions.*;

public class SpinnerNodeTest extends UITestBase {

    @FormTest
    public void testSpinnerNodeRenderer() {
        TestSpinnerNode node = new TestSpinnerNode();
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addItem("Item 1");
        model.addItem("Item 2");
        node.setListModel(model);

        node.setWidth(200);
        node.doLayout();

        // Verify node has children (overlay + items)
        assertTrue(node.getChildCount() > 0, "SpinnerNode should have children after layout");
    }

    @FormTest
    public void testSelectedIndexRoundTripsForEveryRow() {
        // Issue #6165: with 365 rows, selecting "7" (index 6) read back as index 5.
        SpinnerNode node = new SpinnerNode();
        DefaultListModel<String> model = new DefaultListModel<>();
        for (int i = 1; i <= 365; i++) {
            model.addItem(Integer.toString(i));
        }
        node.setListModel(model);
        for (int i = 0; i < 365; i++) {
            node.setSelectedIndex(i);
            assertEquals(i, node.getSelectedIndex(), "row " + (i + 1));
        }
    }

    public static class TestSpinnerNode extends SpinnerNode {
        public void doLayout() {
            layoutChildren();
        }
        public void setWidth(double w) {
             this.boundsInLocal.get().setWidth(w);
        }
    }
}
