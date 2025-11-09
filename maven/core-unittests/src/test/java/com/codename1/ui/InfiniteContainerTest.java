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

    @FormTest
    void testConstructorWithAmount() {
        TestContainer container = new TestContainer();
        assertNotNull(container);
        assertEquals(0, container.getComponentCount());
    }

    @FormTest
    void testConstructorWithInvalidAmountThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new InfiniteContainer(-1) {
                @Override
                public Component[] fetchComponents(int index, int amount) {
                    return new Component[0];
                }
            };
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new InfiniteContainer(0) {
                @Override
                public Component[] fetchComponents(int index, int amount) {
                    return new Component[0];
                }
            };
        });
    }

    @FormTest
    void testConstructorWithValidAmount() {
        InfiniteContainer container = new InfiniteContainer(5) {
            @Override
            public Component[] fetchComponents(int index, int amount) {
                return new Component[0];
            }
        };
        assertNotNull(container);
    }

    @FormTest
    void testContinueFetching() {
        Form form = Display.getInstance().getCurrent();
        TestContainer container = new TestContainer();
        form.add(container);
        form.revalidate();

        container.refresh();
        flushSerialCalls();
        int countBeforeContinue = container.getComponentCount();

        container.continueFetching();
        flushSerialCalls();

        // Should not crash
        assertTrue(container.getComponentCount() >= countBeforeContinue);
    }

    @FormTest
    void testLayoutIsBoxLayoutY() {
        TestContainer container = new TestContainer();
        assertTrue(container.getLayout() instanceof com.codename1.ui.layouts.BoxLayout);
    }

    @FormTest
    void testRefreshWithNullComponents() {
        Form form = Display.getInstance().getCurrent();
        InfiniteContainer container = new InfiniteContainer() {
            @Override
            public Component[] fetchComponents(int index, int amount) {
                return null;
            }
        };
        form.add(container);
        form.revalidate();

        container.refresh();
        flushSerialCalls();

        // Should not crash, should have 0 components
        assertEquals(0, container.getComponentCount());
    }

    @FormTest
    void testRefreshClearsExistingComponents() {
        Form form = Display.getInstance().getCurrent();
        TestContainer container = new TestContainer();
        form.add(container);
        form.revalidate();

        container.refresh();
        flushSerialCalls();
        int firstCount = container.getComponentCount();

        container.refresh();
        flushSerialCalls();

        // After refresh, should have similar count (not accumulate)
        assertTrue(Math.abs(container.getComponentCount() - firstCount) <= 3);
    }

    @FormTest
    void testFetchMoreDoesNotClearExisting() {
        Form form = Display.getInstance().getCurrent();
        TestContainer container = new TestContainer();
        form.add(container);
        form.revalidate();

        container.refresh();
        flushSerialCalls();
        int initialCount = container.getComponentCount();

        container.fetchMore();
        flushSerialCalls();

        // fetchMore should add, not replace
        assertTrue(container.getComponentCount() > initialCount);
    }

    @FormTest
    void testAsyncFalseByDefault() {
        InfiniteContainer container = new InfiniteContainer() {
            @Override
            public Component[] fetchComponents(int index, int amount) {
                return new Component[0];
            }
        };
        assertFalse(container.isAsync());
    }

    @FormTest
    void testRefreshBeforeInitialized() {
        InfiniteContainer container = new InfiniteContainer() {
            @Override
            public Component[] fetchComponents(int index, int amount) {
                return new Component[0];
            }
        };

        // Should not crash when called before initialization
        assertDoesNotThrow(() -> container.refresh());
    }
}
