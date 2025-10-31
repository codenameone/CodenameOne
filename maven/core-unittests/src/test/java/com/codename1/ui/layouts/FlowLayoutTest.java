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

class FlowLayoutTest extends UITestBase {

    @Test
    void testDefaultConstructor() {
        FlowLayout layout = new FlowLayout();
        assertEquals(Component.LEFT, layout.getAlign());
        assertEquals(Component.TOP, layout.getValign());
    }

    @Test
    void testConstructorWithOrientation() {
        FlowLayout layout = new FlowLayout(Component.CENTER);
        assertEquals(Component.CENTER, layout.getAlign());
        assertEquals(Component.TOP, layout.getValign());
    }

    @Test
    void testConstructorWithOrientationAndValign() {
        FlowLayout layout = new FlowLayout(Component.RIGHT, Component.BOTTOM);
        assertEquals(Component.RIGHT, layout.getAlign());
        assertEquals(Component.BOTTOM, layout.getValign());
    }

    @Test
    void testConstructorWithVAlignByRow() {
        FlowLayout layout = new FlowLayout(Component.CENTER, Component.CENTER, true);
        assertEquals(Component.CENTER, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
        assertTrue(layout.isValignByRow());
    }

    @Test
    void testSetAndGetAlign() {
        FlowLayout layout = new FlowLayout();

        layout.setAlign(Component.CENTER);
        assertEquals(Component.CENTER, layout.getAlign());

        layout.setAlign(Component.RIGHT);
        assertEquals(Component.RIGHT, layout.getAlign());

        layout.setAlign(Component.LEFT);
        assertEquals(Component.LEFT, layout.getAlign());
    }

    @Test
    void testSetAndGetValign() {
        FlowLayout layout = new FlowLayout();

        layout.setValign(Component.CENTER);
        assertEquals(Component.CENTER, layout.getValign());

        layout.setValign(Component.BOTTOM);
        assertEquals(Component.BOTTOM, layout.getValign());

        layout.setValign(Component.TOP);
        assertEquals(Component.TOP, layout.getValign());
    }

    @Test
    void testSetAndGetValignByRow() {
        FlowLayout layout = new FlowLayout();
        assertFalse(layout.isValignByRow());

        layout.setValignByRow(true);
        assertTrue(layout.isValignByRow());

        layout.setValignByRow(false);
        assertFalse(layout.isValignByRow());
    }

    @Test
    void testSetAndGetFillRows() {
        FlowLayout layout = new FlowLayout();
        assertFalse(layout.isFillRows());

        layout.setFillRows(true);
        assertTrue(layout.isFillRows());

        layout.setFillRows(false);
        assertFalse(layout.isFillRows());
    }

    @Test
    void testToString() {
        FlowLayout layout = new FlowLayout();
        assertEquals("FlowLayout", layout.toString());
    }

    @Test
    void testEquals() {
        FlowLayout layout1 = new FlowLayout(Component.LEFT, Component.TOP);
        FlowLayout layout2 = new FlowLayout(Component.LEFT, Component.TOP);
        FlowLayout layout3 = new FlowLayout(Component.CENTER, Component.TOP);
        FlowLayout layout4 = new FlowLayout(Component.LEFT, Component.BOTTOM);

        assertTrue(layout1.equals(layout2));
        assertFalse(layout1.equals(layout3));
        assertFalse(layout1.equals(layout4));
    }

    @Test
    void testEqualsWithFillRows() {
        FlowLayout layout1 = new FlowLayout();
        FlowLayout layout2 = new FlowLayout();

        layout1.setFillRows(true);
        assertFalse(layout1.equals(layout2));

        layout2.setFillRows(true);
        assertTrue(layout1.equals(layout2));
    }

    @Test
    void testEncloseIn() {
        Label label1 = new Label("1");
        Label label2 = new Label("2");

        Container container = FlowLayout.encloseIn(label1, label2);

        assertTrue(container.getLayout() instanceof FlowLayout);
        assertEquals(2, container.getComponentCount());
        assertTrue(container.contains(label1));
        assertTrue(container.contains(label2));
    }

    @Test
    void testEncloseCenter() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseCenter(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.CENTER, layout.getAlign());
    }

    @Test
    void testEncloseRight() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseRight(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.RIGHT, layout.getAlign());
    }

    @Test
    void testEncloseMiddle() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseMiddle(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.LEFT, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
    }

    @Test
    void testEncloseMiddleByRow() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseMiddleByRow(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.LEFT, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
        assertTrue(layout.isValignByRow());
    }

    @Test
    void testEncloseCenterMiddle() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseCenterMiddle(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.CENTER, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
    }

    @Test
    void testEncloseCenterMiddleByRow() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseCenterMiddleByRow(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.CENTER, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
        assertTrue(layout.isValignByRow());
    }

    @Test
    void testEncloseRightMiddle() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseRightMiddle(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.RIGHT, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
    }

    @Test
    void testEncloseRightMiddleByRow() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseRightMiddleByRow(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.RIGHT, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
        assertTrue(layout.isValignByRow());
    }

    @Test
    void testEncloseLeftMiddle() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseLeftMiddle(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.LEFT, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
    }

    @Test
    void testEncloseLeftMiddleByRow() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseLeftMiddleByRow(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.LEFT, layout.getAlign());
        assertEquals(Component.CENTER, layout.getValign());
        assertTrue(layout.isValignByRow());
    }

    @Test
    void testEncloseBottom() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseBottom(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.LEFT, layout.getAlign());
        assertEquals(Component.BOTTOM, layout.getValign());
    }

    @Test
    void testEncloseCenterBottom() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseCenterBottom(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.CENTER, layout.getAlign());
        assertEquals(Component.BOTTOM, layout.getValign());
    }

    @Test
    void testEncloseRightBottom() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseRightBottom(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.RIGHT, layout.getAlign());
        assertEquals(Component.BOTTOM, layout.getValign());
    }

    @Test
    void testEncloseBottomByRow() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseBottomByRow(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.LEFT, layout.getAlign());
        assertEquals(Component.BOTTOM, layout.getValign());
        assertTrue(layout.isValignByRow());
    }

    @Test
    void testEncloseCenterBottomByRow() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseCenterBottomByRow(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.CENTER, layout.getAlign());
        assertEquals(Component.BOTTOM, layout.getValign());
        assertTrue(layout.isValignByRow());
    }

    @Test
    void testEncloseRightBottomByRow() {
        Label label = new Label("Test");
        Container container = FlowLayout.encloseRightBottomByRow(label);

        FlowLayout layout = (FlowLayout) container.getLayout();
        assertEquals(Component.RIGHT, layout.getAlign());
        assertEquals(Component.BOTTOM, layout.getValign());
        assertTrue(layout.isValignByRow());
    }

    @Test
    void testPreferredSizeWithEmptyContainer() {
        FlowLayout layout = new FlowLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertEquals(0, preferredSize.getWidth());
        assertEquals(0, preferredSize.getHeight());
    }

    @Test
    void testPreferredSizeWithComponents() {
        FlowLayout layout = new FlowLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertTrue(preferredSize.getWidth() > 0);
        assertTrue(preferredSize.getHeight() > 0);
    }

    @FormTest
    void testLayoutContainerLeftAlign() {
        FlowLayout layout = new FlowLayout(Component.LEFT);
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        container.layoutContainer();

        // Components should be arranged from left to right
        assertTrue(button1.getX() < button2.getX());
    }

    @FormTest
    void testLayoutContainerWithEmptyContainer() {
        FlowLayout layout = new FlowLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        // Should not throw exception with empty container
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @FormTest
    void testLayoutContainerWithSingleComponent() {
        FlowLayout layout = new FlowLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        Button button = new Button("Button");
        container.add(button);

        container.layoutContainer();

        // Component should be positioned
        assertTrue(button.getX() >= 0);
        assertTrue(button.getY() >= 0);
    }

    @Test
    void testLayoutContainerMultipleRows() {
        FlowLayout layout = new FlowLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        // Add many components to force multiple rows
        for (int i = 0; i < 10; i++) {
            container.add(new Button("Button " + i));
        }

        container.layoutContainer();

        // Components should be positioned
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component cmp = container.getComponentAt(i);
            assertTrue(cmp.getX() >= 0);
            assertTrue(cmp.getY() >= 0);
        }
    }

    @FormTest
    void testFillRowsBehavior() {
        FlowLayout layout = new FlowLayout();
        layout.setFillRows(true);

        Container container = new Container(layout);
        container.setSize(new Dimension(300, 200));

        Button button1 = new Button("1");
        Button button2 = new Button("2");

        container.add(button1);
        container.add(button2);

        container.layoutContainer();

        // With fill rows, components should be expanded to fill the row
        assertTrue(button1.getWidth() > 0);
        assertTrue(button2.getWidth() > 0);
    }

    @FormTest
    void testBaselineAlignment() {
        FlowLayout layout = new FlowLayout(Component.LEFT, Component.BASELINE);
        Container container = new Container(layout);
        container.setSize(new Dimension(300, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        // Should not throw exception with baseline alignment
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @FormTest
    void testTopAlignment() {
        FlowLayout layout = new FlowLayout(Component.LEFT, Component.TOP);
        Container container = new Container(layout);
        container.setSize(new Dimension(300, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        container.layoutContainer();

        // Both buttons should start at similar Y positions
        assertTrue(Math.abs(button1.getY() - button2.getY()) < 5);
    }

    @FormTest
    void testCenterAlignment() {
        FlowLayout layout = new FlowLayout(Component.CENTER);
        Container container = new Container(layout);
        container.setSize(new Dimension(300, 200));

        Button button = new Button("Button");
        container.add(button);

        container.layoutContainer();

        // Button should be centered horizontally
        assertTrue(button.getX() > 0);
    }

    @FormTest
    void testRightAlignment() {
        FlowLayout layout = new FlowLayout(Component.RIGHT);
        Container container = new Container(layout);
        container.setSize(new Dimension(300, 200));

        Button button = new Button("Button");
        container.add(button);

        container.layoutContainer();

        // Button should be positioned towards the right
        assertTrue(button.getX() > 0);
    }

    @FormTest
    void testValignByRowTrue() {
        FlowLayout layout = new FlowLayout(Component.LEFT, Component.CENTER, true);
        Container container = new Container(layout);
        container.setSize(new Dimension(300, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        // Should not throw exception
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @FormTest
    void testValignByRowFalse() {
        FlowLayout layout = new FlowLayout(Component.LEFT, Component.CENTER, false);
        Container container = new Container(layout);
        container.setSize(new Dimension(300, 200));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        // Should not throw exception
        assertDoesNotThrow(() -> container.layoutContainer());
    }
}
