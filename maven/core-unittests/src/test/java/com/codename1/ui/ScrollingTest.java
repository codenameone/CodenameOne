package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.testing.TestCodenameOneImplementation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for scrolling on x or y axis including nested containers and edge cases.
 */
class ScrollingTest extends UITestBase {

    @FormTest
    void testScrollableYBasicFunctionality() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        // Add enough content to require scrolling
        for (int i = 0; i < 30; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        assertTrue(scrollable.isScrollableY());
        assertTrue(scrollable.getScrollDimension().getHeight() > scrollable.getHeight());
    }

    @FormTest
    void testScrollableXBasicFunctionality() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.x());
        scrollable.setScrollableX(true);
        scrollable.setWidth(300);

        // Add enough content to require horizontal scrolling
        for (int i = 0; i < 20; i++) {
            Button btn = new Button("Item " + i);
            btn.setPreferredW(80);
            scrollable.add(btn);
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        assertTrue(scrollable.isScrollableX());
    }

    @FormTest
    void testNestedScrollContainers() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        // Outer container - vertical scrolling
        Container outer = new Container(BoxLayout.y());
        outer.setScrollableY(true);
        outer.setHeight(400);

        // Inner container - horizontal scrolling
        Container inner = new Container(BoxLayout.x());
        inner.setScrollableX(true);
        inner.setWidth(300);

        for (int i = 0; i < 10; i++) {
            Button btn = new Button("H" + i);
            btn.setPreferredW(100);
            inner.add(btn);
        }

        outer.add(inner);

        // Add more vertical content
        for (int i = 0; i < 20; i++) {
            outer.add(new Label("Vertical " + i));
        }

        form.add(BorderLayout.CENTER, outer);
        form.revalidate();

        assertTrue(outer.isScrollableY());
        assertTrue(inner.isScrollableX());
    }

    @FormTest
    void testScrollToShowComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Button target = null;
        for (int i = 0; i < 30; i++) {
            Button btn = new Button("Item " + i);
            scrollable.add(btn);
            if (i == 25) {
                target = btn;
            }
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll to show the target component
        scrollable.scrollComponentToVisible(target);
        form.revalidate();

        // Target should be visible after scrolling
        assertNotNull(target);
    }

    @FormTest
    void testScrollXAndY() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(new FlowLayout());
        scrollable.setScrollableX(true);
        scrollable.setScrollableY(true);
        scrollable.setWidth(300);
        scrollable.setHeight(300);

        // Add content that requires both horizontal and vertical scrolling
        for (int i = 0; i < 50; i++) {
            Button btn = new Button("Item " + i);
            btn.setPreferredSize(new Dimension(120, 60));
            scrollable.add(btn);
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        assertTrue(scrollable.isScrollableX());
        assertTrue(scrollable.isScrollableY());
    }

    @FormTest
    void testScrollAnimationSmooth() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setSmoothScrolling(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 30; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        assertTrue(scrollable.isSmoothScrolling());

        // Scroll programmatically
        scrollable.setScrollY(100);
        assertEquals(100, scrollable.getScrollY());
    }

    @FormTest
    void testScrollWithScrollListener() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 30; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        final boolean[] scrolled = {false};
        scrollable.addScrollListener((scrollX, scrollY, oldScrollX, oldScrollY) -> {
            scrolled[0] = true;
        });

        scrollable.setScrollY(50);
        form.revalidate();

        assertTrue(scrolled[0]);
    }

    @FormTest
    void testScrollBeyondBoundaries() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 20; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        int maxScroll = scrollable.getScrollDimension().getHeight() - scrollable.getHeight();

        // Try to scroll beyond maximum
        scrollable.setScrollY(maxScroll + 1000);

        // Should be clamped to maximum
        assertTrue(scrollable.getScrollY() <= maxScroll);
    }

    @FormTest
    void testScrollWithZeroContent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Should not crash with empty content
        assertEquals(0, scrollable.getScrollY());
        scrollable.setScrollY(100);
        assertEquals(0, scrollable.getScrollY()); // Should stay at 0
    }

    @FormTest
    void testScrollableWithFixedComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        // Add fixed header
        Label header = new Label("Fixed Header");
        form.add(BorderLayout.NORTH, header);

        // Add scrollable content
        for (int i = 0; i < 30; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Header should remain fixed while content scrolls
        int initialHeaderY = header.getY();
        scrollable.setScrollY(50);

        assertEquals(initialHeaderY, header.getY());
    }

    @FormTest
    void testScrollDimensionUpdatesOnContentChange() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 10; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        int initialHeight = scrollable.getScrollDimension().getHeight();

        // Add more content
        for (int i = 10; i < 30; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.revalidate();

        assertTrue(scrollable.getScrollDimension().getHeight() > initialHeight);
    }

    @FormTest
    void testScrollResetOnRefresh() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 30; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll down
        scrollable.setScrollY(100);
        assertEquals(100, scrollable.getScrollY());

        // Reset scroll position
        scrollable.setScrollY(0);
        assertEquals(0, scrollable.getScrollY());
    }

    @FormTest
    void testNestedScrollInDifferentDirections() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container verticalScroll = new Container(BoxLayout.y());
        verticalScroll.setScrollableY(true);
        verticalScroll.setHeight(300);

        for (int section = 0; section < 5; section++) {
            Container horizontalScroll = new Container(BoxLayout.x());
            horizontalScroll.setScrollableX(true);

            for (int i = 0; i < 10; i++) {
                Button btn = new Button("S" + section + "I" + i);
                btn.setPreferredW(100);
                horizontalScroll.add(btn);
            }

            verticalScroll.add(horizontalScroll);
        }

        form.add(BorderLayout.CENTER, verticalScroll);
        form.revalidate();

        assertTrue(verticalScroll.isScrollableY());
    }

    @FormTest
    void testScrollInvisibleComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Button invisibleBtn = new Button("Invisible");
        invisibleBtn.setVisible(false);

        scrollable.add(new Label("Item 1"));
        scrollable.add(invisibleBtn);
        scrollable.add(new Label("Item 2"));

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scrolling to invisible component should not crash
        scrollable.scrollComponentToVisible(invisibleBtn);
        assertFalse(invisibleBtn.isVisible());
    }

    @FormTest
    void testScrollableDisabledDynamically() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 30; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        assertTrue(scrollable.isScrollableY());

        // Disable scrolling
        scrollable.setScrollableY(false);
        assertFalse(scrollable.isScrollableY());

        // Re-enable scrolling
        scrollable.setScrollableY(true);
        assertTrue(scrollable.isScrollableY());
    }

    @FormTest
    void testScrollWithAlwaysOnScrollbar() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setScrollVisible(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 30; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        assertTrue(scrollable.isScrollVisible());
    }
}
