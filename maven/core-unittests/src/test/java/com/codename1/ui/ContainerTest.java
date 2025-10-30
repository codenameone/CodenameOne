package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerTest extends UITestBase {

    @FormTest
    void testMorphAnimation() {
        Form form = CN.getCurrentForm();
        form.setAllowEnableLayoutOnPaint(false);
        form.setLayout(new BorderLayout());

        Button a = new Button("A");
        Button b = new Button("B");
        Label c = new Label("C");
        Container content = BoxLayout.encloseY(a, b, c);
        form.add(BorderLayout.CENTER, content);
        form.revalidate();
        assertNotEquals(c.getBounds(), a.getBounds());
        content.morphAndWait(a, c, 120);
        assertEquals(3, content.getComponentCount());
        assertEquals(c.getBounds(), a.getBounds());
    }

    @FormTest
    void testAnimateHierarchyWithDefaultOpacityDoesNotCrash() {
        Form form = CN.getCurrentForm();
        form.removeAll();
        form.setLayout(BoxLayout.y());

        Label first = new Label("First");
        Label second = new Label("Second");
        form.addAll(first, second);
        form.revalidate();

        // Regression guard for commit 4e5cbc2c2092721c8861a34d557fe56fe742e82b where
        // MorphAnimation's constructor began initializing opacity to an empty array.
        // When animateHierarchy() is invoked without fade (the default, matching
        // Form.animateHierarchy and Codename One 7.0.208), MorphAnimation.opacity
        // remains zero-length yet non-null. The flush() call below mirrors Display.flushEdt()
        // from the stack trace and would previously trigger an ArrayIndexOutOfBoundsException.
        assertDoesNotThrow(() -> {
            form.animateHierarchy(100);
            form.getAnimationManager().flush();
        });
    }

    @Test
    void testScrollableFlagsRespectBorderLayout() {
        Container container = new Container(new BorderLayout());

        container.setScrollableX(true);
        container.setScrollableY(true);

        assertFalse(container.scrollableXFlag(), "BorderLayout containers cannot scroll horizontally");
        assertFalse(container.scrollableYFlag(), "BorderLayout containers cannot scroll vertically");
    }

    @Test
    void testSetScrollableAppliesToBothAxes() {
        Container container = new Container(new FlowLayout());

        container.setScrollable(true);
        assertTrue(container.scrollableXFlag());
        assertTrue(container.scrollableYFlag());

        container.setScrollable(false);
        assertFalse(container.scrollableXFlag());
        assertFalse(container.scrollableYFlag());
    }

    @Test
    void testIsScrollableXDependsOnScrollSize() {
        Container container = new Container(new FlowLayout());
        container.setScrollableX(true);
        container.setWidth(100);
        container.setHeight(50);

        container.setScrollSize(new Dimension(150, 50));
        assertTrue(container.isScrollableX(), "Scroll width larger than component width should allow scrolling");

        container.setScrollSize(new Dimension(80, 50));
        assertFalse(container.isScrollableX(), "Scroll width smaller than component width should disable scrolling");
    }

    @Test
    void testEncloseInUsesLayoutConstraints() {
        Label north = new Label("north");
        Container container = Container.encloseIn(new BorderLayout(), north, BorderLayout.NORTH);

        assertEquals(1, container.getComponentCount());
        assertSame(north, container.getComponentAt(0));
        assertTrue(container.getLayout() instanceof BorderLayout);
        BorderLayout layout = (BorderLayout) container.getLayout();
        assertEquals(BorderLayout.NORTH, layout.getComponentConstraint(north));
    }

    @Test
    void testEncloseInAddsComponentArgumentsSequentially() {
        Label first = new Label("first");
        Label second = new Label("second");

        Container container = Container.encloseIn(new FlowLayout(), first, second);

        assertEquals(2, container.getComponentCount());
        assertSame(first, container.getComponentAt(0));
        assertSame(second, container.getComponentAt(1));
    }
}
