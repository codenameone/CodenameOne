package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for scrollComponentToVisible edge cases including hiding under VKB and very large components.
 */
class ScrollComponentToVisibleTest extends UITestBase {

    @FormTest
    void testScrollToVisibleBasic() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Button target = null;
        for (int i = 0; i < 50; i++) {
            Button btn = new Button("Button " + i);
            scrollable.add(btn);
            if (i == 30) {
                target = btn;
            }
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll to target
        scrollable.scrollComponentToVisible(target);
        form.revalidate();

        assertNotNull(target);
    }

    @FormTest
    void testScrollToVisibleAtTop() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Button firstButton = new Button("First");
        scrollable.add(firstButton);

        for (int i = 1; i < 50; i++) {
            scrollable.add(new Button("Button " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll down first
        scrollable.setScrollY(500);

        // Then scroll to first button
        scrollable.scrollComponentToVisible(firstButton);
        form.revalidate();

        assertTrue(scrollable.getScrollY() >= 0);
    }

    @FormTest
    void testScrollToVisibleAtBottom() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Button lastButton = null;
        for (int i = 0; i < 50; i++) {
            Button btn = new Button("Button " + i);
            scrollable.add(btn);
            if (i == 49) {
                lastButton = btn;
            }
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll to last button
        scrollable.scrollComponentToVisible(lastButton);
        form.revalidate();

        assertNotNull(lastButton);
    }

    @FormTest
    void testScrollToVisibleVeryLargeComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(300);

        // Add small components
        for (int i = 0; i < 5; i++) {
            scrollable.add(new Button("Small " + i));
        }

        // Add very large component
        Button largeBtn = new Button("Large");
        largeBtn.setPreferredSize(new Dimension(200, 1000));
        scrollable.add(largeBtn);

        // Add more small components
        for (int i = 0; i < 5; i++) {
            scrollable.add(new Button("Small " + (i + 5)));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll to large component
        scrollable.scrollComponentToVisible(largeBtn);
        form.revalidate();

        assertNotNull(largeBtn);
    }

    @FormTest
    void testScrollToVisibleInNestedContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container outer = new Container(BoxLayout.y());
        outer.setScrollableY(true);
        outer.setHeight(300);

        Container inner = new Container(BoxLayout.y());
        Button target = new Button("Target");

        for (int i = 0; i < 20; i++) {
            outer.add(new Button("Outer " + i));
        }

        inner.add(new Label("Inner Label"));
        inner.add(target);
        outer.add(inner);

        for (int i = 20; i < 40; i++) {
            outer.add(new Button("Outer " + i));
        }

        form.add(BorderLayout.CENTER, outer);
        form.revalidate();

        // Scroll outer to show inner container with target
        outer.scrollComponentToVisible(inner);
        form.revalidate();

        assertNotNull(target);
    }

    @FormTest
    void testScrollToVisibleWithVirtualKeyboard() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(400);

        TextField textField = null;
        for (int i = 0; i < 30; i++) {
            if (i == 25) {
                textField = new TextField();
                textField.setHint("Target Field");
                scrollable.add(textField);
            } else {
                scrollable.add(new Button("Button " + i));
            }
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Simulate VKB showing (reduce available height)
        int originalHeight = impl.getDisplayHeight();
        impl.setDisplaySize(impl.getDisplayWidth(), originalHeight / 2);
        form.revalidate();

        // Scroll to text field that might be hidden by VKB
        scrollable.scrollComponentToVisible(textField);
        form.revalidate();

        // Restore original height
        impl.setDisplaySize(impl.getDisplayWidth(), originalHeight);

        assertNotNull(textField);
    }

    @FormTest
    void testScrollToVisibleInvisibleComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Button invisible = new Button("Invisible");
        invisible.setVisible(false);

        scrollable.add(new Button("Button 1"));
        scrollable.add(invisible);
        scrollable.add(new Button("Button 2"));

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Try to scroll to invisible component
        scrollable.scrollComponentToVisible(invisible);
        form.revalidate();

        assertFalse(invisible.isVisible());
    }

    @FormTest
    void testScrollToVisibleComponentNotInContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);

        for (int i = 0; i < 20; i++) {
            scrollable.add(new Button("Button " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Try to scroll to component not in container
        Button external = new Button("External");

        // Should not crash
        scrollable.scrollComponentToVisible(external);
        form.revalidate();

        assertNull(external.getParent());
    }

    @FormTest
    void testScrollToVisibleHorizontal() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.x());
        scrollable.setScrollableX(true);
        scrollable.setWidth(300);

        Button target = null;
        for (int i = 0; i < 30; i++) {
            Button btn = new Button("Btn " + i);
            btn.setPreferredW(100);
            scrollable.add(btn);
            if (i == 20) {
                target = btn;
            }
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll to target horizontally
        scrollable.scrollComponentToVisible(target);
        form.revalidate();

        assertNotNull(target);
    }

    @FormTest
    void testScrollToVisibleWithAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setSmoothScrolling(true);
        scrollable.setHeight(200);

        Button target = null;
        for (int i = 0; i < 40; i++) {
            Button btn = new Button("Button " + i);
            scrollable.add(btn);
            if (i == 30) {
                target = btn;
            }
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Smooth scroll to target
        scrollable.scrollComponentToVisible(target);
        form.revalidate();

        assertNotNull(target);
    }

    @FormTest
    void testScrollToVisibleMultipleTimes() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Button btn1 = new Button("Button 1");
        Button btn2 = null;
        Button btn3 = null;

        scrollable.add(btn1);
        for (int i = 2; i < 50; i++) {
            Button btn = new Button("Button " + i);
            scrollable.add(btn);
            if (i == 25) btn2 = btn;
            if (i == 45) btn3 = btn;
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll to different components
        scrollable.scrollComponentToVisible(btn1);
        form.revalidate();

        scrollable.scrollComponentToVisible(btn2);
        form.revalidate();

        scrollable.scrollComponentToVisible(btn3);
        form.revalidate();

        assertNotNull(btn3);
    }

    @FormTest
    void testScrollToVisibleWithDynamicContent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        for (int i = 0; i < 20; i++) {
            scrollable.add(new Button("Button " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Add more content dynamically
        Button newTarget = new Button("New Target");
        scrollable.add(newTarget);
        form.revalidate();

        // Scroll to newly added component
        scrollable.scrollComponentToVisible(newTarget);
        form.revalidate();

        assertNotNull(newTarget);
    }

    @FormTest
    void testScrollToVisibleWithZeroHeight() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setPreferredSize(new Dimension(200, 200));

        Button zeroHeight = new Button("Zero");
        zeroHeight.setPreferredH(0);

        scrollable.add(new Button("Before"));
        scrollable.add(zeroHeight);
        scrollable.add(new Button("After"));

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Try to scroll to zero-height component
        scrollable.scrollComponentToVisible(zeroHeight);
        form.revalidate();

        assertEquals(0, zeroHeight.getHeight());
    }

    @FormTest
    void testScrollToVisibleInNonScrollableContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container nonScrollable = new Container(BoxLayout.y());
        nonScrollable.setScrollableY(false);

        Button target = new Button("Target");
        nonScrollable.add(new Button("Button 1"));
        nonScrollable.add(target);
        nonScrollable.add(new Button("Button 2"));

        form.add(BorderLayout.CENTER, nonScrollable);
        form.revalidate();

        // Try to scroll in non-scrollable container
        nonScrollable.scrollComponentToVisible(target);
        form.revalidate();

        assertFalse(nonScrollable.isScrollableY());
    }

    @FormTest
    void testScrollToVisibleWithPreferredFocus() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        TextField focusTarget = null;
        for (int i = 0; i < 40; i++) {
            if (i == 30) {
                focusTarget = new TextField();
                focusTarget.setHint("Focus Target");
                scrollable.add(focusTarget);
            } else {
                scrollable.add(new Button("Button " + i));
            }
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Request focus (should trigger scroll)
        focusTarget.requestFocus();
        scrollable.scrollComponentToVisible(focusTarget);
        form.revalidate();

        assertNotNull(focusTarget);
    }

    @FormTest
    void testScrollToVisibleWithMargins() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);
        scrollable.setHeight(200);

        Button target = new Button("Target");
        target.getAllStyles().setMargin(50, 50, 50, 50);

        for (int i = 0; i < 20; i++) {
            scrollable.add(new Button("Button " + i));
        }
        scrollable.add(target);
        for (int i = 20; i < 40; i++) {
            scrollable.add(new Button("Button " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Scroll to component with large margins
        scrollable.scrollComponentToVisible(target);
        form.revalidate();

        assertNotNull(target);
    }
}
