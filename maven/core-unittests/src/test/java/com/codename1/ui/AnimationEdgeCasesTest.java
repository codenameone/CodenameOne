package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for animation edge cases including adding/removing components while animating.
 */
class AnimationEdgeCasesTest extends UITestBase {

    @FormTest
    void testAddComponentDuringLayoutAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.add(btn1);
        form.add(btn2);
        form.revalidate();

        // Start layout animation
        form.animateLayout(200);

        // Add component during animation
        Button btn3 = new Button("Button 3");
        form.add(btn3);
        form.revalidate();

        assertEquals(3, form.getComponentCount());
    }

    @FormTest
    void testRemoveComponentDuringLayoutAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        // Start layout animation
        form.animateLayout(200);

        // Remove component during animation
        form.removeComponent(btn2);
        form.revalidate();

        assertEquals(2, form.getComponentCount());
        assertFalse(form.contains(btn2));
    }

    @FormTest
    void testReplaceComponentDuringAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Start animation
        form.animateLayout(200);

        // Replace component during animation
        Button replacement = new Button("Replacement");
        form.replace(btn2, replacement, null);
        form.revalidate();

        assertEquals(2, form.getComponentCount());
        assertTrue(form.contains(replacement));
        assertFalse(form.contains(btn2));
    }

    @FormTest
    void testHierarchyAnimationWithComponentAddition() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container container = new Container(BoxLayout.y());
        Button btn1 = new Button("Button 1");
        container.add(btn1);

        form.add(container);
        form.revalidate();

        // Start hierarchy animation
        form.animateHierarchy(200);

        // Add component to container during animation
        Button btn2 = new Button("Button 2");
        container.add(btn2);
        form.revalidate();

        assertEquals(2, container.getComponentCount());
    }

    @FormTest
    void testAnimationOnEmptyContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container container = new Container(BoxLayout.y());
        form.add(container);
        form.revalidate();

        // Animate empty container - should not crash
        assertDoesNotThrow(() -> form.animateLayout(200));
    }

    @FormTest
    void testMultipleSimultaneousAnimations() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Start multiple animations
        form.animateLayout(200);
        form.animateHierarchy(200);

        // Should handle gracefully
        assertEquals(2, form.getComponentCount());
    }

    @FormTest
    void testAnimationWithInvisibleComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        btn2.setVisible(false);

        form.addAll(btn1, btn2);
        form.revalidate();

        // Animate with invisible component
        form.animateLayout(200);

        assertFalse(btn2.isVisible());
        assertTrue(btn1.isVisible());
    }

    @FormTest
    void testToggleVisibilityDuringAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Start animation
        form.animateLayout(200);

        // Toggle visibility during animation
        btn2.setVisible(false);
        form.revalidate();

        assertFalse(btn2.isVisible());
    }

    @FormTest
    void testAnimationWithZeroSpeed() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Button");
        form.add(btn);
        form.revalidate();

        // Animation with zero duration should complete immediately
        form.animateLayout(0);

        assertNotNull(btn.getParent());
    }

    @FormTest
    void testRemoveAllDuringAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        for (int i = 0; i < 10; i++) {
            form.add(new Button("Button " + i));
        }
        form.revalidate();

        // Start animation
        form.animateLayout(200);

        // Remove all components
        form.removeAll();
        form.revalidate();

        assertEquals(0, form.getComponentCount());
    }

    @FormTest
    void testMorphAnimationWithComponentRemoval() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container container = new Container(BoxLayout.y());
        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        container.addAll(btn1, btn2, btn3);
        form.add(container);
        form.revalidate();

        // Start morph animation
        container.animateHierarchy(200);

        // Remove component during morph
        container.removeComponent(btn2);
        form.revalidate();

        assertEquals(2, container.getComponentCount());
    }

    @FormTest
    void testAnimationInterruption() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Start first animation
        form.animateLayout(300);

        // Interrupt with second animation
        form.animateLayout(200);

        assertEquals(2, form.getComponentCount());
    }

    @FormTest
    void testAnimateUnlayoutAndRemove() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);
        form.revalidate();

        // Animate unlayout and remove component
        form.animateUnlayoutAndWait(btn2, 200);

        assertEquals(2, form.getComponentCount());
        assertFalse(form.contains(btn2));
    }

    @FormTest
    void testAnimationWithChangedBounds() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Button");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        int initialWidth = btn.getWidth();

        // Change form size and animate
        form.setWidth(form.getWidth() + 100);
        form.animateLayout(200);

        assertTrue(btn.getWidth() >= initialWidth);
    }

    @FormTest
    void testHierarchyAnimationWithOpacity() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        // Animate with fade
        form.animateHierarchyFade(200, 0);

        assertEquals(2, form.getComponentCount());
    }

    @FormTest
    void testAnimationFlush() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Button");
        form.add(btn);
        form.revalidate();

        // Start animation and flush
        form.animateLayout(200);
        form.getAnimationManager().flush();

        assertNotNull(btn.getParent());
    }

    @FormTest
    void testComponentAnimationWithTransition() {
        Form form1 = CN.getCurrentForm();
        form1.setLayout(new BorderLayout());
        form1.add(BorderLayout.CENTER, new Label("Form 1"));

        Form form2 = new Form("Form 2", new BorderLayout());
        form2.add(BorderLayout.CENTER, new Label("Form 2"));

        // Set transition
        form2.setTransitionOutAnimator(CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 200));

        assertNotNull(form2.getTransitionOutAnimator());
    }

    @FormTest
    void testAnimationWithLayoutConstraintChange() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Button");
        form.add(BorderLayout.NORTH, btn);
        form.revalidate();

        // Start animation
        form.animateLayout(200);

        // Change constraint
        form.removeComponent(btn);
        form.add(BorderLayout.SOUTH, btn);
        form.revalidate();

        assertEquals(BorderLayout.SOUTH, form.getLayout().getComponentConstraint(btn));
    }

    @FormTest
    void testAnimationWithDisabledComponent() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        btn2.setEnabled(false);

        form.addAll(btn1, btn2);
        form.revalidate();

        form.animateLayout(200);

        assertTrue(btn1.isEnabled());
        assertFalse(btn2.isEnabled());
    }
}
