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
            if (index >= 9) {
                return new Component[0];
            }
            int start = Math.max(0, index < 0 ? 0 : index + (index == 0 ? 0 : 1));
            int size = Math.min(3, amount);
            Component[] components = new Component[size];
            for (int i = 0; i < size; i++) {
                components[i] = new Label("Item " + (start + i));
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
        flushSerialCalls();
        assertTrue(container.getComponentCount() > 0);
        assertTrue(container.fetchCount >= 1);
        assertTrue(container.lastIndex >= 0);
        assertTrue(container.lastAmount > 0);
    }

    @FormTest
    void testFetchMoreAddsMoreComponents() {
        Form form = Display.getInstance().getCurrent();
        TestContainer container = new TestContainer();
        form.add(container);
        form.revalidate();

        container.refresh();
        flushSerialCalls();
        int initialCount = container.getComponentCount();
        container.fetchMore();
        flushSerialCalls();
        assertTrue(container.getComponentCount() > initialCount);
        assertTrue(container.fetchCount >= 2);
    }

    @FormTest
    void testInfiniteProgressAvailable() {
        Form form = Display.getInstance().getCurrent();
        TestContainer container = new TestContainer();
        form.add(container);
        form.revalidate();
        assertNotNull(container.getInfiniteProgress());
    }
}
