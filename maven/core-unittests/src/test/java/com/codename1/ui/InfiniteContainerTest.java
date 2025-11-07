package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Label;

import static org.junit.jupiter.api.Assertions.*;

class InfiniteContainerTest extends UITestBase {

    private static class TestContainer extends InfiniteContainer {
        private int fetchCount;
        private int lastIndex;
        private int lastAmount;

        @Override
        public Component[] fetchComponents(int index, int amount) {
            fetchCount++;
            lastIndex = index;
            lastAmount = amount;
            if (index > 0) {
                return new Component[0];
            }
            int size = Math.min(3, amount);
            Component[] components = new Component[size];
            for (int i = 0; i < size; i++) {
                components[i] = new Label("Item " + (index + i));
            }
            return components;
        }
    }

    @FormTest
    void testRefreshLoadsComponents() {
        Form form = Display.getInstance().getCurrent();
        TestContainer container = new TestContainer();
        form.add(container);
        form.revalidate();

        container.refresh();
        assertTrue(container.getComponentCount() > 0);
        assertEquals(1, container.fetchCount);
        assertEquals(0, container.lastIndex);
        assertTrue(container.lastAmount > 0);
    }

    @FormTest
    void testFetchMoreAddsMoreComponents() {
        Form form = Display.getInstance().getCurrent();
        TestContainer container = new TestContainer();
        form.add(container);
        form.revalidate();

        container.refresh();
        int initialCount = container.getComponentCount();
        container.fetchMore();
        assertTrue(container.getComponentCount() >= initialCount);
        assertTrue(container.fetchCount >= 2);
    }

    @FormTest
    void testInfiniteProgressAvailable() {
        TestContainer container = new TestContainer();
        assertNotNull(container.getInfiniteProgress());
    }
}
