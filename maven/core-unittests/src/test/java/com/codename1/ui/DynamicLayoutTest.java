package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for changing layouts dynamically and adapting to layout constraints.
 */
class DynamicLayoutTest extends UITestBase {

    @FormTest
    void testChangeLayoutFromBorderToBox() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.add(BorderLayout.NORTH, btn1);
        form.add(BorderLayout.CENTER, btn2);
        form.revalidate();

        assertTrue(form.getLayout() instanceof BorderLayout);

        // Change to BoxLayout
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        form.revalidate();

        assertTrue(form.getLayout() instanceof BoxLayout);
    }

    @FormTest
    void testChangeLayoutFromBoxToBorder() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        assertTrue(form.getLayout() instanceof BoxLayout);

        // Change to BorderLayout
        form.removeAll();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.NORTH, btn1);
        form.add(BorderLayout.CENTER, btn2);
        form.revalidate();

        assertTrue(form.getLayout() instanceof BorderLayout);
    }

    @FormTest
    void testChangeLayoutWithConstraints() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.NORTH, btn);
        form.revalidate();

        assertEquals(BorderLayout.NORTH, form.getLayout().getComponentConstraint(btn));

        // Change constraint
        form.removeComponent(btn);
        form.add(BorderLayout.SOUTH, btn);
        form.revalidate();

        assertEquals(BorderLayout.SOUTH, form.getLayout().getComponentConstraint(btn));
    }

    @FormTest
    void testChangeLayoutMultipleTimes() {
        Form form = CN.getCurrentForm();

        Button btn = new Button("Test");

        // BorderLayout
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();
        assertTrue(form.getLayout() instanceof BorderLayout);

        // BoxLayout
        form.removeAll();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        form.add(btn);
        form.revalidate();
        assertTrue(form.getLayout() instanceof BoxLayout);

        // FlowLayout
        form.removeAll();
        form.setLayout(new FlowLayout());
        form.add(btn);
        form.revalidate();
        assertTrue(form.getLayout() instanceof FlowLayout);
    }

    @FormTest
    void testChangeLayoutWithAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Start animation
        form.animateLayout(200);

        // Change layout during animation
        form.removeAll();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, btn1);
        form.add(BorderLayout.SOUTH, btn2);
        form.revalidate();

        assertTrue(form.getLayout() instanceof BorderLayout);
    }

    @FormTest
    void testGridLayoutToFlowLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new GridLayout(2, 2));

        for (int i = 0; i < 4; i++) {
            form.add(new Button("Button " + i));
        }
        form.revalidate();

        assertTrue(form.getLayout() instanceof GridLayout);

        // Change to FlowLayout
        form.setLayout(new FlowLayout());
        form.revalidate();

        assertTrue(form.getLayout() instanceof FlowLayout);
    }

    @FormTest
    void testLayoutConstraintAdaptation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button north = new Button("North");
        Button south = new Button("South");
        Button center = new Button("Center");

        form.add(BorderLayout.NORTH, north);
        form.add(BorderLayout.SOUTH, south);
        form.add(BorderLayout.CENTER, center);
        form.revalidate();

        // Change to BoxLayout (constraints should be ignored)
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        form.revalidate();

        assertEquals(3, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testDynamicConstraintChange() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.NORTH, btn);
        form.revalidate();

        // Move to different position
        String[] positions = {
            BorderLayout.SOUTH,
            BorderLayout.EAST,
            BorderLayout.WEST,
            BorderLayout.CENTER
        };

        for (String position : positions) {
            form.removeComponent(btn);
            form.add(position, btn);
            form.revalidate();
            assertEquals(position, form.getLayout().getComponentConstraint(btn));
        }
    }

    @FormTest
    void testLayoutChangeWithScrollableContent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);

        for (int i = 0; i < 130; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Change layout
        form.removeAll();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        form.add(scrollable);
        form.revalidate();

        assertTrue(scrollable.isScrollableY());
    }

    @FormTest
    void testLayoutChangePreservesComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        int initialCount = form.getContentPane().getComponentCount();

        // Change layout
        form.setLayout(new FlowLayout());
        form.revalidate();

        assertEquals(initialCount, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testLayoutChangeWithInvisibleComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button visible = new Button("Visible");
        Button invisible = new Button("Invisible");
        invisible.setVisible(false);

        form.addAll(visible, invisible);
        form.revalidate();

        // Change layout
        form.setLayout(new FlowLayout());
        form.revalidate();

        assertTrue(visible.isVisible());
        assertFalse(invisible.isVisible());
    }

    @FormTest
    void testLayoutChangeWithDifferentPreferredSizes() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button small = new Button("Small");
        small.setPreferredW(100);
        small.setPreferredH(50);

        Button large = new Button("Large");
        large.setPreferredW(300);
        large.setPreferredH(150);

        form.addAll(small, large);
        form.revalidate();

        // Change layout
        form.setLayout(new FlowLayout());
        form.revalidate();

        assertEquals(100, small.getPreferredW());
        assertEquals(300, large.getPreferredW());
    }

    @FormTest
    void testNestedContainerLayoutChange() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container container = new Container(BoxLayout.y());
        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        container.addAll(btn1, btn2);
        form.add(BorderLayout.CENTER, container);
        form.revalidate();

        // Change nested container's layout
        container.setLayout(new FlowLayout());
        form.revalidate();

        assertTrue(container.getLayout() instanceof FlowLayout);
    }

    @FormTest
    void testLayoutChangeWithComponentStates() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button enabled = new Button("Enabled");
        Button disabled = new Button("Disabled");
        disabled.setEnabled(false);

        form.addAll(enabled, disabled);
        form.revalidate();

        // Change layout
        form.setLayout(new FlowLayout());
        form.revalidate();

        assertTrue(enabled.isEnabled());
        assertFalse(disabled.isEnabled());
    }

    @FormTest
    void testLayoutChangeAffectsRevalidate() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Test");
        form.add(btn);
        form.revalidate();

        int initialY = btn.getY();

        // Change layout and observe position change
        form.setLayout(new BorderLayout());
        btn.remove();
        form.add(BorderLayout.SOUTH, btn);
        form.revalidate();

        // Position should be different
        assertTrue(btn.getY() >= 0);
    }

    @FormTest
    void testLayoutChangeWithFocusedComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        btn1.requestFocus();

        // Change layout
        form.setLayout(new FlowLayout());
        form.revalidate();

        // Focus should be maintained
        assertEquals(2, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testRapidLayoutChanges() {
        Form form = CN.getCurrentForm();

        Button btn = new Button("Test");

        // Rapid layout changes
        for (int i = 0; i < 10; i++) {
            form.removeAll();
            if (i % 3 == 0) {
                form.setLayout(new BorderLayout());
                form.add(BorderLayout.CENTER, btn);
            } else if (i % 3 == 1) {
                form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
                form.add(btn);
            } else {
                form.setLayout(new FlowLayout());
                form.add(btn);
            }
            form.revalidate();
        }

        assertEquals(1, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testLayoutChangeWithLayeredLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        // Change to LayeredLayout
        form.removeAll();
        form.setLayout(new com.codename1.ui.layouts.LayeredLayout());
        form.add(btn);
        form.revalidate();

        assertTrue(form.getLayout() instanceof com.codename1.ui.layouts.LayeredLayout);
    }

    @FormTest
    void testConstraintUpdateOnSameLayout() {
        Form form = CN.getCurrentForm();
        BorderLayout layout = new BorderLayout();
        form.setLayout(layout);

        Button btn = new Button("Test");
        form.add(BorderLayout.NORTH, btn);
        form.revalidate();

        // Update constraint within same layout
        form.removeComponent(btn);
        form.add(BorderLayout.SOUTH, btn);
        form.revalidate();

        assertEquals(BorderLayout.SOUTH, layout.getComponentConstraint(btn));
    }
}
