package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.*;

class SwipeableContainerTest extends UITestBase {

    @FormTest
    void testOpenToRightAndClose() {
        Form form = Display.getInstance().getCurrent();
        Button left = new Button("Left");
        Button top = new Button("Top");
        left.setPreferredSize(new Dimension(80, 40));
        top.setPreferredSize(new Dimension(80, 40));
        SwipeableContainer container = new SwipeableContainer(left, top);
        form.add(container);
        form.revalidate();

        assertFalse(container.isOpen());
        container.openToRight();
        assertTrue(container.isOpen());
        assertTrue(container.isOpenedToRight());

        container.close();
        assertFalse(container.isOpen());
    }

    @FormTest
    void testOpenToLeft() {
        Form form = Display.getInstance().getCurrent();
        Button right = new Button("Right");
        Button top = new Button("Top");
        right.setPreferredSize(new Dimension(80, 40));
        top.setPreferredSize(new Dimension(80, 40));
        SwipeableContainer container = new SwipeableContainer(null, right, top);
        form.add(container);
        form.revalidate();

        container.openToLeft();
        assertTrue(container.isOpen());
        assertTrue(container.isOpenedToLeft());
        container.close();
        assertFalse(container.isOpen());
    }

    @FormTest
    void testSwipeActivationAndComponentAt() {
        Form form = Display.getInstance().getCurrent();
        Button left = new Button("Left");
        Button top = new Button("Top");
        left.setPreferredSize(new Dimension(60, 40));
        top.setPreferredSize(new Dimension(60, 40));
        SwipeableContainer container = new SwipeableContainer(left, top);
        form.add(container);
        form.revalidate();

        assertTrue(container.isSwipeActivated());
        container.setSwipeActivated(false);
        assertFalse(container.isSwipeActivated());

        Component child = container.getComponentAt(1, 1);
        assertTrue(child instanceof Container);
        assertSame(child, top.getParent());
    }

    @FormTest
    void testPreviouslyOpenedSetter() {
        SwipeableContainer first = new SwipeableContainer(new Button("Left"), new Button("Top"));
        SwipeableContainer second = new SwipeableContainer(new Button("Left"), new Button("Top"));
        second.setPreviouslyOpened(first);
        assertSame(first, second.getPreviouslyOpened());
    }

    @FormTest
    void testConstructorWithOnlyLeftAndTop() {
        Button left = new Button("Left");
        Button top = new Button("Top");
        SwipeableContainer container = new SwipeableContainer(left, top);

        assertNotNull(container);
        assertEquals(3, container.getComponentCount());
        assertFalse(container.isOpen());
    }

    @FormTest
    void testConstructorWithAllComponents() {
        Button left = new Button("Left");
        Button right = new Button("Right");
        Button top = new Button("Top");
        SwipeableContainer container = new SwipeableContainer(left, right, top);

        assertNotNull(container);
        assertEquals(3, container.getComponentCount());
    }

    @FormTest
    void testConstructorWithNullBottomLeft() {
        Button right = new Button("Right");
        Button top = new Button("Top");
        SwipeableContainer container = new SwipeableContainer(null, right, top);

        assertNotNull(container);
        assertEquals(3, container.getComponentCount());
    }

    @FormTest
    void testConstructorWithNullBottomRight() {
        Button left = new Button("Left");
        Button top = new Button("Top");
        SwipeableContainer container = new SwipeableContainer(left, null, top);

        assertNotNull(container);
        assertEquals(3, container.getComponentCount());
    }

    @FormTest
    void testOpenToRightWhenNoBottomLeft() {
        Form form = Display.getInstance().getCurrent();
        Button right = new Button("Right");
        Button top = new Button("Top");
        SwipeableContainer container = new SwipeableContainer(null, right, top);
        form.add(container);
        form.revalidate();

        container.openToRight();
        // Should not open if no bottom left component
        assertFalse(container.isOpen());
    }

    @FormTest
    void testOpenToLeftWhenNoBottomRight() {
        Form form = Display.getInstance().getCurrent();
        Button left = new Button("Left");
        Button top = new Button("Top");
        SwipeableContainer container = new SwipeableContainer(left, null, top);
        form.add(container);
        form.revalidate();

        container.openToLeft();
        // Should not open if no bottom right component
        assertFalse(container.isOpen());
    }

    @FormTest
    void testOpenToRightTwiceDoesNothing() {
        Form form = Display.getInstance().getCurrent();
        Button left = new Button("Left");
        Button top = new Button("Top");
        left.setPreferredSize(new Dimension(80, 40));
        top.setPreferredSize(new Dimension(80, 40));
        SwipeableContainer container = new SwipeableContainer(left, top);
        form.add(container);
        form.revalidate();

        container.openToRight();
        assertTrue(container.isOpen());

        container.openToRight();
        // Should still be open, but second call should have no effect
        assertTrue(container.isOpen());
        assertTrue(container.isOpenedToRight());
    }

    @FormTest
    void testOpenToLeftTwiceDoesNothing() {
        Form form = Display.getInstance().getCurrent();
        Button right = new Button("Right");
        Button top = new Button("Top");
        right.setPreferredSize(new Dimension(80, 40));
        top.setPreferredSize(new Dimension(80, 40));
        SwipeableContainer container = new SwipeableContainer(null, right, top);
        form.add(container);
        form.revalidate();

        container.openToLeft();
        assertTrue(container.isOpen());

        container.openToLeft();
        // Should still be open, but second call should have no effect
        assertTrue(container.isOpen());
        assertTrue(container.isOpenedToLeft());
    }

    @FormTest
    void testCloseWhenNotOpen() {
        Form form = Display.getInstance().getCurrent();
        Button left = new Button("Left");
        Button top = new Button("Top");
        SwipeableContainer container = new SwipeableContainer(left, top);
        form.add(container);
        form.revalidate();

        container.close();
        // Should not crash when closing an already closed container
        assertFalse(container.isOpen());
    }

    @FormTest
    void testSwipeActivationToggle() {
        SwipeableContainer container = new SwipeableContainer(new Button("Left"), new Button("Top"));

        assertTrue(container.isSwipeActivated());

        container.setSwipeActivated(false);
        assertFalse(container.isSwipeActivated());

        container.setSwipeActivated(true);
        assertTrue(container.isSwipeActivated());
    }

    @FormTest
    void testOpenedStateFlags() {
        Form form = Display.getInstance().getCurrent();
        Button left = new Button("Left");
        Button right = new Button("Right");
        Button top = new Button("Top");
        left.setPreferredSize(new Dimension(80, 40));
        right.setPreferredSize(new Dimension(80, 40));
        top.setPreferredSize(new Dimension(80, 40));
        SwipeableContainer container = new SwipeableContainer(left, right, top);
        form.add(container);
        form.revalidate();

        assertFalse(container.isOpenedToRight());
        assertFalse(container.isOpenedToLeft());

        container.openToRight();
        assertTrue(container.isOpenedToRight());
        assertFalse(container.isOpenedToLeft());

        container.close();
        assertFalse(container.isOpen());

        container.openToLeft();
        assertTrue(container.isOpenedToLeft());
        assertFalse(container.isOpenedToRight());
    }

    @FormTest
    void testLayoutIsLayered() {
        SwipeableContainer container = new SwipeableContainer(new Button("Left"), new Button("Top"));
        assertTrue(container.getLayout() instanceof com.codename1.ui.layouts.LayeredLayout);
    }
}
