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

    public static class TestSpinnerNode extends SpinnerNode {
        public void doLayout() {
            layoutChildren();
        }
        public void setWidth(double w) {
             this.boundsInLocal.get().setWidth(w);
        }
    }
}
