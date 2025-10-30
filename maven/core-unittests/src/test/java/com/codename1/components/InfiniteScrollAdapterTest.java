package com.codename1.components;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InfiniteScrollAdapterTest extends UITestBase {

    @Test
    void createInfiniteScrollAddsProgressAndInvokesFetch() {
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        AtomicInteger fetchCount = new AtomicInteger();
        InfiniteScrollAdapter adapter = InfiniteScrollAdapter.createInfiniteScroll(container, fetchCount::incrementAndGet);
        assertEquals(1, container.getComponentCount(), "Progress indicator should be added immediately");
        flushSerialCalls();
        assertEquals(1, fetchCount.get());
        assertNotNull(adapter.getInfiniteProgress());
    }

    @Test
    void addMoreComponentsAppendsItemsAndEndMarker() {
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        InfiniteScrollAdapter adapter = InfiniteScrollAdapter.createInfiniteScroll(container, () -> {
        }, false);
        assertEquals(1, container.getComponentCount());
        Component initial = container.getComponentAt(0);
        assertEquals("EdgeMarker", initial.getClass().getSimpleName());

        Component[] components = {new Label("One"), new Label("Two")};
        adapter.addMoreComponents(components, true);
        assertEquals(3, container.getComponentCount());
        assertSame(components[0], container.getComponentAt(0));
        assertSame(components[1], container.getComponentAt(1));
        assertEquals("EdgeMarker", container.getComponentAt(2).getClass().getSimpleName());
    }

    @Test
    void addMoreComponentsWithoutMoreRemovesIndicators() {
        Container container = new Container(new FlowLayout());
        InfiniteScrollAdapter adapter = InfiniteScrollAdapter.createInfiniteScroll(container, () -> {
        }, false);
        Component[] components = {new Label("A")};
        adapter.addMoreComponents(components, false);
        assertEquals(1, container.getComponentCount());
        assertSame(components[0], container.getComponentAt(0));
    }

    @Test
    void componentLimitRemovesOverflow() {
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        InfiniteScrollAdapter adapter = InfiniteScrollAdapter.createInfiniteScroll(container, () -> {
        }, false);
        adapter.setComponentLimit(2);

        Component[] firstBatch = {new Label("First"), new Label("Second")};
        adapter.addMoreComponents(firstBatch, true);
        assertEquals(3, container.getComponentCount());

        Component[] secondBatch = {new Label("Third"), new Label("Fourth"), new Label("Fifth")};
        adapter.addMoreComponents(secondBatch, true);
        assertEquals(3, container.getComponentCount(), "Two items plus end marker expected");
        assertEquals("Fourth", ((Label) container.getComponentAt(0)).getText());
        assertEquals("Fifth", ((Label) container.getComponentAt(1)).getText());
        assertEquals("EdgeMarker", container.getComponentAt(2).getClass().getSimpleName());
    }

    @Test
    void continueFetchingRunsCallbackWhenEndMarkerRemoved() {
        Container container = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        AtomicInteger fetchCount = new AtomicInteger();
        InfiniteScrollAdapter adapter = InfiniteScrollAdapter.createInfiniteScroll(container, fetchCount::incrementAndGet, false);
        Component[] components = {new Label("Item")};
        adapter.addMoreComponents(components, false);
        assertEquals(1, container.getComponentCount());
        InfiniteScrollAdapter.continueFetching(container);
        assertEquals(1, fetchCount.get());
    }
}
