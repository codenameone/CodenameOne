package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ButtonListTest extends UITestBase {

    @FormTest
    void testConstructorInitializesModel() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        TestButtonList list = new TestButtonList(model);
        assertSame(model, list.getModel());
    }

    @FormTest
    void testRefreshCreatesComponentsFromModel() {
        DefaultListModel<String> model = new DefaultListModel<>("Red", "Green", "Blue");
        TestButtonList list = new TestButtonList(model);
        list.fireReady();
        assertEquals(3, list.getComponentCount());
    }

    @FormTest
    void testSetModelChangesData() {
        DefaultListModel<String> model1 = new DefaultListModel<>("One", "Two");
        DefaultListModel<String> model2 = new DefaultListModel<>("Three", "Four", "Five");
        TestButtonList list = new TestButtonList(model1);
        list.fireReady();
        assertEquals(2, list.getComponentCount());

        list.setModel(model2);
        assertEquals(3, list.getComponentCount());
    }

    @FormTest
    void testDataChangedAddsComponent() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two");
        TestButtonList list = new TestButtonList(model);
        list.fireReady();
        assertEquals(2, list.getComponentCount());

        model.addItem("Three");
        assertEquals(3, list.getComponentCount());
    }

    @FormTest
    void testDataChangedRemovesComponent() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two", "Three");
        TestButtonList list = new TestButtonList(model);
        list.fireReady();
        assertEquals(3, list.getComponentCount());

        model.removeItem(1);
        assertEquals(2, list.getComponentCount());
    }

    @FormTest
    void testSetLayoutTriggersRefresh() {
        DefaultListModel<String> model = new DefaultListModel<>("A", "B");
        TestButtonList list = new TestButtonList(model);
        list.fireReady();
        list.resetRefreshCount();

        list.setLayout(BoxLayout.y());
        assertEquals(1, list.getRefreshCount());
    }

    @FormTest
    void testActionListenerPropagatesFromButtons() {
        DefaultListModel<String> model = new DefaultListModel<>("Item");
        TestButtonList list = new TestButtonList(model);
        list.fireReady();

        AtomicInteger actionCount = new AtomicInteger();
        list.addActionListener(evt -> actionCount.incrementAndGet());

        list.fireButtonAction(0);
        assertEquals(1, actionCount.get());
    }

    @FormTest
    void testRemoveActionListener() {
        DefaultListModel<String> model = new DefaultListModel<>("Item");
        TestButtonList list = new TestButtonList(model);
        list.fireReady();

        AtomicInteger actionCount = new AtomicInteger();
        list.addActionListener(evt -> actionCount.incrementAndGet());
        list.removeActionListener(evt -> actionCount.incrementAndGet());

        list.fireButtonAction(0);
        assertEquals(1, actionCount.get());
    }

    @FormTest
    void testSetCellUIIDAppliestoAllCells() {
        DefaultListModel<String> model = new DefaultListModel<>("One", "Two");
        TestButtonList list = new TestButtonList(model);
        list.fireReady();

        list.setCellUIID("CustomCell");
        for (Component c : list) {
            assertEquals("CustomCell", c.getUIID());
        }
    }

    @FormTest
    void testDecoratorIsAppliedToComponents() {
        DefaultListModel<String> model = new DefaultListModel<>("Item1", "Item2");
        TestButtonList list = new TestButtonList(model);

        AtomicInteger decorateCount = new AtomicInteger();
        list.addDecorator(new ButtonList.Decorator<String, Component>() {
            @Override
            public void decorate(String modelItem, Component viewItem) {
                decorateCount.incrementAndGet();
            }

            @Override
            public void undecorate(Component viewItem) {
            }
        });

        list.fireReady();
        assertEquals(2, decorateCount.get());
    }

    @FormTest
    void testRemoveDecoratorPreventsDecoration() {
        DefaultListModel<String> model = new DefaultListModel<>("Item");
        TestButtonList list = new TestButtonList(model);

        AtomicInteger decorateCount = new AtomicInteger();
        ButtonList.Decorator<String, Component> decorator = new ButtonList.Decorator<String, Component>() {
            @Override
            public void decorate(String modelItem, Component viewItem) {
                decorateCount.incrementAndGet();
            }

            @Override
            public void undecorate(Component viewItem) {
            }
        };

        list.addDecorator(decorator);
        list.removeDecorator(decorator);
        list.fireReady();

        assertEquals(0, decorateCount.get());
    }

    private static class TestButtonList extends ButtonList {
        private int refreshCount = 0;

        public TestButtonList(DefaultListModel<String> model) {
            super(model);
        }

        @Override
        public boolean isAllowMultipleSelection() {
            return false;
        }

        @Override
        protected Component createButton(Object model) {
            Label label = new Label(model.toString());
            label.setFocusable(true);
            return label;
        }

        @Override
        protected void setSelected(Component button, boolean selected) {
            // No-op for test
        }

        @Override
        public void refresh() {
            super.refresh();
            refreshCount++;
        }

        public int getRefreshCount() {
            return refreshCount;
        }

        public void resetRefreshCount() {
            refreshCount = 0;
        }

        public void fireButtonAction(int index) {
            Component button = getComponentAt(index);
            actionPerformed(new com.codename1.ui.events.ActionEvent(button));
        }
    }
}
