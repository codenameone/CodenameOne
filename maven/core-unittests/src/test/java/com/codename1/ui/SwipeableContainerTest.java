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
}
