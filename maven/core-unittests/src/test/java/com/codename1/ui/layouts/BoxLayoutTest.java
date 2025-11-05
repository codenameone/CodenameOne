package com.codename1.ui.layouts;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoxLayoutTest extends UITestBase {

    @Test
    void testXAxisConstructor() {
        BoxLayout layout = new BoxLayout(BoxLayout.X_AXIS);
        assertEquals(BoxLayout.X_AXIS, layout.getAxis());
    }

    @Test
    void testYAxisConstructor() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
    }

    @Test
    void testXAxisNoGrowConstructor() {
        BoxLayout layout = new BoxLayout(BoxLayout.X_AXIS_NO_GROW);
        assertEquals(BoxLayout.X_AXIS_NO_GROW, layout.getAxis());
    }

    @Test
    void testYAxisBottomLastConstructor() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST);
        assertEquals(BoxLayout.Y_AXIS_BOTTOM_LAST, layout.getAxis());
    }

    @Test
    void testStaticYFactory() {
        BoxLayout layout = BoxLayout.y();
        assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
    }

    @Test
    void testStaticXFactory() {
        BoxLayout layout = BoxLayout.x();
        assertEquals(BoxLayout.X_AXIS, layout.getAxis());
    }

    @Test
    void testStaticYLastFactory() {
        BoxLayout layout = BoxLayout.yLast();
        assertEquals(BoxLayout.Y_AXIS_BOTTOM_LAST, layout.getAxis());
    }

    @Test
    void testStaticYCenterFactory() {
        BoxLayout layout = BoxLayout.yCenter();
        assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
        assertEquals(Component.CENTER, layout.getAlign());
    }

    @Test
    void testStaticYBottomFactory() {
        BoxLayout layout = BoxLayout.yBottom();
        assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
        assertEquals(Component.BOTTOM, layout.getAlign());
    }

    @Test
    void testStaticXCenterFactory() {
        BoxLayout layout = BoxLayout.xCenter();
        assertEquals(BoxLayout.X_AXIS, layout.getAxis());
        assertEquals(Component.CENTER, layout.getAlign());
    }

    @Test
    void testStaticXRightFactory() {
        BoxLayout layout = BoxLayout.xRight();
        assertEquals(BoxLayout.X_AXIS, layout.getAxis());
        assertEquals(Component.RIGHT, layout.getAlign());
    }

    @Test
    void testAlignmentDefaultsToTop() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        assertEquals(Component.TOP, layout.getAlign());
    }

    @Test
    void testSetAndGetAlign() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);

        layout.setAlign(Component.CENTER);
        assertEquals(Component.CENTER, layout.getAlign());

        layout.setAlign(Component.BOTTOM);
        assertEquals(Component.BOTTOM, layout.getAlign());
    }

    @Test
    void testEncloseY() {
        Label label1 = new Label("1");
        Label label2 = new Label("2");

        Container container = BoxLayout.encloseY(label1, label2);

        assertTrue(container.getLayout() instanceof BoxLayout);
        assertEquals(BoxLayout.Y_AXIS, ((BoxLayout) container.getLayout()).getAxis());
        assertEquals(2, container.getComponentCount());
        assertTrue(container.contains(label1));
        assertTrue(container.contains(label2));
    }

    @Test
    void testEncloseYCenter() {
        Label label = new Label("Test");
        Container container = BoxLayout.encloseYCenter(label);

        BoxLayout layout = (BoxLayout) container.getLayout();
        assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
        assertEquals(Component.CENTER, layout.getAlign());
    }

    @Test
    void testEncloseYBottom() {
        Label label = new Label("Test");
        Container container = BoxLayout.encloseYBottom(label);

        BoxLayout layout = (BoxLayout) container.getLayout();
        assertEquals(BoxLayout.Y_AXIS, layout.getAxis());
        assertEquals(Component.BOTTOM, layout.getAlign());
    }

    @Test
    void testEncloseYBottomLast() {
        Label label = new Label("Test");
        Container container = BoxLayout.encloseYBottomLast(label);

        BoxLayout layout = (BoxLayout) container.getLayout();
        assertEquals(BoxLayout.Y_AXIS_BOTTOM_LAST, layout.getAxis());
    }

    @Test
    void testEncloseX() {
        Label label1 = new Label("1");
        Label label2 = new Label("2");

        Container container = BoxLayout.encloseX(label1, label2);

        assertTrue(container.getLayout() instanceof BoxLayout);
        assertEquals(BoxLayout.X_AXIS, ((BoxLayout) container.getLayout()).getAxis());
        assertEquals(2, container.getComponentCount());
    }

    @Test
    void testEncloseXNoGrow() {
        Label label = new Label("Test");
        Container container = BoxLayout.encloseXNoGrow(label);

        BoxLayout layout = (BoxLayout) container.getLayout();
        assertEquals(BoxLayout.X_AXIS_NO_GROW, layout.getAxis());
    }

    @Test
    void testEncloseXCenter() {
        Label label = new Label("Test");
        Container container = BoxLayout.encloseXCenter(label);

        BoxLayout layout = (BoxLayout) container.getLayout();
        assertEquals(BoxLayout.X_AXIS, layout.getAxis());
        assertEquals(Component.CENTER, layout.getAlign());
    }

    @Test
    void testEncloseXRight() {
        Label label = new Label("Test");
        Container container = BoxLayout.encloseXRight(label);

        BoxLayout layout = (BoxLayout) container.getLayout();
        assertEquals(BoxLayout.X_AXIS, layout.getAxis());
        assertEquals(Component.RIGHT, layout.getAlign());
    }

    @Test
    void testToStringX() {
        BoxLayout layout = new BoxLayout(BoxLayout.X_AXIS);
        assertEquals("BoxLayout X", layout.toString());
    }

    @Test
    void testToStringY() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        assertEquals("BoxLayout Y", layout.toString());
    }

    @Test
    void testEquals() {
        BoxLayout layout1 = new BoxLayout(BoxLayout.Y_AXIS);
        BoxLayout layout2 = new BoxLayout(BoxLayout.Y_AXIS);
        BoxLayout layout3 = new BoxLayout(BoxLayout.X_AXIS);

        assertTrue(layout1.equals(layout2));
        assertFalse(layout1.equals(layout3));
    }

    @Test
    void testPreferredSizeYAxis() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        Container container = new Container(layout);

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertTrue(preferredSize.getHeight() > 0);
        assertTrue(preferredSize.getWidth() > 0);
    }

    @Test
    void testPreferredSizeXAxis() {
        BoxLayout layout = new BoxLayout(BoxLayout.X_AXIS);
        Container container = new Container(layout);

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertTrue(preferredSize.getHeight() > 0);
        assertTrue(preferredSize.getWidth() > 0);
    }

    @FormTest
    void testLayoutContainerYAxis() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 400));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");
        Button button3 = new Button("Button 3");

        container.add(button1);
        container.add(button2);
        container.add(button3);

        container.layoutContainer();

        // In Y axis, components should be stacked vertically
        assertTrue(button1.getY() < button2.getY());
        assertTrue(button2.getY() < button3.getY());

        // Components should use the full width
        assertTrue(button1.getWidth() > 0);
        assertTrue(button2.getWidth() > 0);
        assertTrue(button3.getWidth() > 0);
    }

    @FormTest
    void testLayoutContainerXAxis() {
        BoxLayout layout = new BoxLayout(BoxLayout.X_AXIS);
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");
        Button button3 = new Button("Button 3");

        container.add(button1);
        container.add(button2);
        container.add(button3);

        container.layoutContainer();

        // In X axis, components should be arranged horizontally
        assertTrue(button1.getX() < button2.getX());
        assertTrue(button2.getX() < button3.getX());

        // Components should use the full height
        assertTrue(button1.getHeight() > 0);
        assertTrue(button2.getHeight() > 0);
        assertTrue(button3.getHeight() > 0);
    }

    @FormTest
    void testLayoutContainerXAxisNoGrow() {
        BoxLayout layout = new BoxLayout(BoxLayout.X_AXIS_NO_GROW);
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        container.layoutContainer();

        // In X axis no grow, components should be arranged horizontally
        assertTrue(button1.getX() < button2.getX());

        // But their heights should be based on preferred size, not container height
        assertTrue(button1.getHeight() > 0);
        assertTrue(button2.getHeight() > 0);
    }

    @FormTest
    void testLayoutContainerYAxisBottomLast() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS_BOTTOM_LAST);
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 400));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");
        Button button3 = new Button("Button 3");

        container.add(button1);
        container.add(button2);
        container.add(button3);

        container.layoutContainer();

        // The last component should be pushed to the bottom
        assertTrue(button3.getY() + button3.getHeight() >= container.getHeight() - 10);
    }

    @FormTest
    void testLayoutContainerWithEmptyContainer() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 400));

        // Should not throw exception with empty container
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @Test
    void testPreferredSizeWithEmptyContainer() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        Container container = new Container(layout);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertEquals(0, preferredSize.getWidth());
        assertEquals(0, preferredSize.getHeight());
    }

    @FormTest
    void testYAxisCenterAlignment() {
        BoxLayout layout = BoxLayout.yCenter();
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 400));

        Button smallButton = new Button("Small");
        container.add(smallButton);

        container.layoutContainer();

        // With center alignment and content smaller than container,
        // component should be centered vertically
        assertTrue(smallButton.getY() > 0);
    }

    @FormTest
    void testYAxisBottomAlignment() {
        BoxLayout layout = BoxLayout.yBottom();
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 400));

        Button smallButton = new Button("Small");
        container.add(smallButton);

        container.layoutContainer();

        // With bottom alignment, component should be near the bottom
        assertTrue(smallButton.getY() > 0);
    }

    @FormTest
    void testXAxisCenterAlignment() {
        BoxLayout layout = BoxLayout.xCenter();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 200));

        Button smallButton = new Button("Small");
        container.add(smallButton);

        container.layoutContainer();

        // With center alignment, component should be centered horizontally
        assertTrue(smallButton.getX() > 0);
    }

    @FormTest
    void testXAxisRightAlignment() {
        BoxLayout layout = BoxLayout.xRight();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 200));

        Button smallButton = new Button("Small");
        container.add(smallButton);

        container.layoutContainer();

        // With right alignment, component should be near the right
        assertTrue(smallButton.getX() > 0);
    }

    @Test
    void testMultipleComponentsYAxisPreferredSize() {
        BoxLayout layout = new BoxLayout(BoxLayout.Y_AXIS);
        Container container = new Container(layout);

        for (int i = 0; i < 5; i++) {
            container.add(new Button("Button " + i));
        }

        Dimension preferredSize = layout.getPreferredSize(container);

        // Height should be sum of all component heights
        assertTrue(preferredSize.getHeight() > 50);

        // Width should be max of all component widths
        assertTrue(preferredSize.getWidth() > 0);
    }

    @Test
    void testMultipleComponentsXAxisPreferredSize() {
        BoxLayout layout = new BoxLayout(BoxLayout.X_AXIS);
        Container container = new Container(layout);

        for (int i = 0; i < 5; i++) {
            container.add(new Button("Button " + i));
        }

        Dimension preferredSize = layout.getPreferredSize(container);

        // Width should be sum of all component widths
        assertTrue(preferredSize.getWidth() > 50);

        // Height should be max of all component heights
        assertTrue(preferredSize.getHeight() > 0);
    }
}
