package com.codename1.ui.layouts;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoordinateLayoutTest extends UITestBase {

    @Test
    void testDefaultConstructor() {
        CoordinateLayout layout = new CoordinateLayout();
        assertNotNull(layout);
    }

    @Test
    void testConstructorWithDimensions() {
        CoordinateLayout layout = new CoordinateLayout(640, 480);
        assertNotNull(layout);
    }

    @Test
    void testConstructorWithDimensionObject() {
        Dimension dimension = new Dimension(800, 600);
        CoordinateLayout layout = new CoordinateLayout(dimension);
        assertNotNull(layout);
    }

    @Test
    void testIsOverlapSupported() {
        CoordinateLayout layout = new CoordinateLayout();
        assertTrue(layout.isOverlapSupported());
    }

    @Test
    void testPreferredSizeWithEmptyContainer() {
        CoordinateLayout layout = new CoordinateLayout();
        Container container = new Container(layout);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertEquals(0, preferredSize.getWidth());
        assertEquals(0, preferredSize.getHeight());
    }

    @Test
    void testPreferredSizeBasedOnComponentPositions() {
        CoordinateLayout layout = new CoordinateLayout();
        Container container = new Container(layout);

        Button button = new Button("Button");
        button.setX(100);
        button.setY(50);

        container.add(button);

        Dimension preferredSize = layout.getPreferredSize(container);

        // Preferred size should include component position plus its size
        assertTrue(preferredSize.getWidth() >= 100 + button.getPreferredW());
        assertTrue(preferredSize.getHeight() >= 50 + button.getPreferredH());
    }

    @Test
    void testPreferredSizeWithMultipleComponents() {
        CoordinateLayout layout = new CoordinateLayout();
        Container container = new Container(layout);

        Button button1 = new Button("1");
        button1.setX(50);
        button1.setY(50);

        Button button2 = new Button("2");
        button2.setX(200);
        button2.setY(150);

        container.add(button1);
        container.add(button2);

        Dimension preferredSize = layout.getPreferredSize(container);

        // Preferred size should accommodate the rightmost/bottommost component
        assertTrue(preferredSize.getWidth() >= 200 + button2.getPreferredW());
        assertTrue(preferredSize.getHeight() >= 150 + button2.getPreferredH());
    }

    @Test
    void testLayoutContainerWithoutDimensions() {
        // Layout with default constructor (no scaling)
        CoordinateLayout layout = new CoordinateLayout();
        Container container = new Container(layout);
        container.setSize(400, 400);

        Button button = new Button("Button");
        button.setX(50);
        button.setY(100);

        container.add(button);

        container.layoutContainer();

        // Without dimensions, positions should remain as set
        assertEquals(50, button.getX());
        assertEquals(100, button.getY());
    }

    @Test
    void testLayoutContainerWithDimensions() {
        // Layout with specified dimensions (scaling enabled)
        CoordinateLayout layout = new CoordinateLayout(400, 300);
        Container container = new Container(layout);
        container.setSize(800, 600);

        Button button = new Button("Button");
        button.setX(100);
        button.setY(75);

        container.add(button);

        container.layoutContainer();

        // With scaling, positions should be scaled
        // 100 * 800/400 = 200, 75 * 600/300 = 150
        assertEquals(200, button.getX());
        assertEquals(150, button.getY());
    }

    @Test
    void testLayoutContainerSetsPreferredSize() {
        CoordinateLayout layout = new CoordinateLayout(400, 300);
        Container container = new Container(layout);
        container.setSize(400, 300);

        Button button = new Button("Button");
        button.setX(50);
        button.setY(50);

        container.add(button);

        container.layoutContainer();

        // Component should be sized to its preferred size
        assertEquals(button.getPreferredW(), button.getWidth());
        assertEquals(button.getPreferredH(), button.getHeight());
    }

    @Test
    void testLayoutContainerWithEmptyContainer() {
        CoordinateLayout layout = new CoordinateLayout(400, 300);
        Container container = new Container(layout);
        container.setSize(400, 300);

        // Should not throw exception with empty container
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @Test
    void testMultipleComponentsAtDifferentPositions() {
        CoordinateLayout layout = new CoordinateLayout(500, 500);
        Container container = new Container(layout);
        container.setSize(500, 500);

        Button button1 = new Button("1");
        button1.setX(10);
        button1.setY(10);

        Button button2 = new Button("2");
        button2.setX(100);
        button2.setY(100);

        Button button3 = new Button("3");
        button3.setX(200);
        button3.setY(200);

        container.add(button1);
        container.add(button2);
        container.add(button3);

        container.layoutContainer();

        // Positions should be preserved (no scaling in this case)
        assertEquals(10, button1.getX());
        assertEquals(10, button1.getY());
        assertEquals(100, button2.getX());
        assertEquals(100, button2.getY());
        assertEquals(200, button3.getX());
        assertEquals(200, button3.getY());
    }

    @Test
    void testOverlappingComponents() {
        CoordinateLayout layout = new CoordinateLayout();
        Container container = new Container(layout);

        Button button1 = new Button("1");
        button1.setX(50);
        button1.setY(50);

        Button button2 = new Button("2");
        button2.setX(50);
        button2.setY(50);

        container.add(button1);
        container.add(button2);

        // Components can overlap - layout supports this
        assertTrue(layout.isOverlapSupported());

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
    }

    @Test
    void testScalingDownFromLargerDimensions() {
        CoordinateLayout layout = new CoordinateLayout(1000, 1000);
        Container container = new Container(layout);
        container.setSize(500, 500);

        Button button = new Button("Button");
        button.setX(400);
        button.setY(400);

        container.add(button);

        container.layoutContainer();

        // Positions should be scaled down: 400 * 500/1000 = 200
        assertEquals(200, button.getX());
        assertEquals(200, button.getY());
    }

    @Test
    void testScalingUpFromSmallerDimensions() {
        CoordinateLayout layout = new CoordinateLayout(200, 200);
        Container container = new Container(layout);
        container.setSize(400, 400);

        Button button = new Button("Button");
        button.setX(50);
        button.setY(50);

        container.add(button);

        container.layoutContainer();

        // Positions should be scaled up: 50 * 400/200 = 100
        assertEquals(100, button.getX());
        assertEquals(100, button.getY());
    }

    @Test
    void testComponentAtOrigin() {
        CoordinateLayout layout = new CoordinateLayout();
        Container container = new Container(layout);

        Button button = new Button("Button");
        button.setX(0);
        button.setY(0);

        container.add(button);

        Dimension preferredSize = layout.getPreferredSize(container);

        // Preferred size should still include the component's size
        assertTrue(preferredSize.getWidth() >= button.getPreferredW());
        assertTrue(preferredSize.getHeight() >= button.getPreferredH());
    }

    @Test
    void testNonUniformScaling() {
        CoordinateLayout layout = new CoordinateLayout(400, 200);
        Container container = new Container(layout);
        container.setSize(800, 600);

        Button button = new Button("Button");
        button.setX(200);
        button.setY(100);

        container.add(button);

        container.layoutContainer();

        // X should scale by 2x (800/400), Y should scale by 3x (600/200)
        assertEquals(400, button.getX());
        assertEquals(300, button.getY());
    }

    @Test
    void testPreferredSizeMaxOfAllComponents() {
        CoordinateLayout layout = new CoordinateLayout();
        Container container = new Container(layout);

        // Component that extends furthest right
        Button button1 = new Button("Wide");
        button1.setX(300);
        button1.setY(10);

        // Component that extends furthest down
        Button button2 = new Button("Tall");
        button2.setX(10);
        button2.setY(400);

        container.add(button1);
        container.add(button2);

        Dimension preferredSize = layout.getPreferredSize(container);

        // Should accommodate both the rightmost and bottommost extents
        assertTrue(preferredSize.getWidth() >= 300 + button1.getPreferredW());
        assertTrue(preferredSize.getHeight() >= 400 + button2.getPreferredH());
    }
}
