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
        content.morphAndWait(a, c, 20);
        assertEquals(3, content.getComponentCount());
        assertEquals(c.getBounds(), a.getBounds());
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
