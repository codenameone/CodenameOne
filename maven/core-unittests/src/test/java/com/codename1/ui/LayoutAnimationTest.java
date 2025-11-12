package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for layout and hierarchy animation including unlayout animation and opacity.
 */
class LayoutAnimationTest extends UITestBase {

    @FormTest
    void testBasicLayoutAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Start layout animation
        form.animateLayout(200);

        assertEquals(2, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testHierarchyAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container container = new Container(BoxLayout.y());
        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        container.addAll(btn1, btn2);

        form.add(container);
        form.revalidate();

        // Animate entire hierarchy
        form.animateHierarchy(200);

        assertEquals(2, container.getComponentCount());
    }

    @FormTest
    void testHierarchyAnimationWithFade() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Animate with fade effect
        form.animateHierarchyFade(200, 0);

        assertEquals(2, form.getContentPane().getComponentCount());
    }


    @FormTest
    void testLayoutAnimationWithMultipleComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        for (int i = 0; i < 10; i++) {
            form.add(new Button("Button " + i));
        }
        form.revalidate();

        // Animate all components
        form.animateLayout(300);

        assertEquals(10, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testAnimateLayoutAndWait() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Test");
        form.add(btn);
        form.revalidate();

        // Animate and wait for completion
        form.animateLayoutAndWait(200);

        assertEquals(1, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testHierarchyAnimationWithOpacity() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        // Animate with opacity fade
        form.animateHierarchyFade(300, 0);

        assertEquals(3, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testLayoutAnimationZeroDuration() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Test");
        form.add(btn);
        form.revalidate();

        // Zero duration should complete immediately
        form.animateLayout(0);

        assertEquals(1, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testLayoutAnimationWithBorderLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button north = new Button("North");
        Button center = new Button("Center");
        Button south = new Button("South");

        form.add(BorderLayout.NORTH, north);
        form.add(BorderLayout.CENTER, center);
        form.add(BorderLayout.SOUTH, south);
        form.revalidate();

        // Animate BorderLayout
        form.animateLayout(200);

        assertEquals(3, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testHierarchyAnimationWithNestedContainers() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container outer = new Container(BoxLayout.y());
        Container inner = new Container(BoxLayout.y());

        inner.addAll(new Button("Inner 1"), new Button("Inner 2"));
        outer.add(inner);
        form.add(outer);
        form.revalidate();

        // Animate nested hierarchy
        form.animateHierarchy(250);

        assertEquals(2, inner.getComponentCount());
    }

    @FormTest
    void testAnimationFlush() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Test");
        form.add(btn);
        form.revalidate();

        // Start animation
        form.animateLayout(300);

        // Flush animation queue
        form.getAnimationManager().flush();

        assertEquals(1, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testLayoutAnimationWithInvisibleComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button visible = new Button("Visible");
        Button invisible = new Button("Invisible");
        invisible.setVisible(false);

        form.addAll(visible, invisible);
        form.revalidate();

        // Animate with invisible component
        form.animateLayout(200);

        assertTrue(visible.isVisible());
        assertFalse(invisible.isVisible());
    }


    @FormTest
    void testMorphAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container container = new Container(BoxLayout.y());
        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        container.addAll(btn1, btn2, btn3);
        form.add(container);
        form.revalidate();

        // Morph from btn1 to btn3
        container.morphAndWait(btn1, btn3, 200);

        assertEquals(3, container.getComponentCount());
    }

    @FormTest
    void testLayoutAnimationWithScrollable() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);

        for (int i = 0; i < 30; i++) {
            scrollable.add(new Button("Button " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Animate scrollable container
        scrollable.animateLayout(250);

        assertEquals(30, scrollable.getComponentCount());
    }

    @FormTest
    void testHierarchyAnimationWithOpacityValues() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Full Opacity");
        Button btn2 = new Button("Half Opacity");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Animate with different opacity
        form.animateHierarchyFade(200, 128);

        assertEquals(2, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testLayoutAnimationInterruption() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Test");
        form.add(btn);
        form.revalidate();

        // Start first animation
        form.animateLayout(500);

        // Start second animation (interrupts first)
        form.animateLayout(200);

        assertEquals(1, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testUnlayoutWithoutAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Remove without animation
        form.removeComponent(btn1);
        form.revalidate();

        assertEquals(1, form.getContentPane().getComponentCount());
        assertFalse(form.contains(btn1));
    }

    @FormTest
    void testLayoutAnimationWithRTL() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.X_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.setRTL(true);
        form.revalidate();

        // Animate with RTL enabled
        form.animateLayout(200);

        assertTrue(form.isRTL());
    }

    @FormTest
    void testHierarchyAnimationDepth() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container level1 = new Container(BoxLayout.y());
        Container level2 = new Container(BoxLayout.y());
        Container level3 = new Container(BoxLayout.y());

        level3.add(new Button("Deep Button"));
        level2.add(level3);
        level1.add(level2);
        form.add(level1);
        form.revalidate();

        // Animate deep hierarchy
        form.animateHierarchy(300);

        assertNotNull(level3.getComponentAt(0));
    }

    @FormTest
    void testAnimationWithDynamicComponentAddition() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        form.add(btn1);
        form.revalidate();

        // Start animation
        form.animateLayout(300);

        // Add component during animation
        Button btn2 = new Button("Button 2");
        form.add(btn2);

        assertEquals(1, form.getContentPane().getComponentCount());
        form.getAnimationManager().flush();
        assertEquals(2, form.getContentPane().getComponentCount());
    }
}
