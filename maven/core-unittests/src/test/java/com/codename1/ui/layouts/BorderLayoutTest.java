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

class BorderLayoutTest extends UITestBase {

    @Test
    void testAddComponentWithConstraints() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);

        Label north = new Label("North");
        Label south = new Label("South");
        Label east = new Label("East");
        Label west = new Label("West");
        Label center = new Label("Center");

        container.add(BorderLayout.NORTH, north);
        container.add(BorderLayout.SOUTH, south);
        container.add(BorderLayout.EAST, east);
        container.add(BorderLayout.WEST, west);
        container.add(BorderLayout.CENTER, center);

        assertEquals(BorderLayout.NORTH, layout.getComponentConstraint(north));
        assertEquals(BorderLayout.SOUTH, layout.getComponentConstraint(south));
        assertEquals(BorderLayout.EAST, layout.getComponentConstraint(east));
        assertEquals(BorderLayout.WEST, layout.getComponentConstraint(west));
        assertEquals(BorderLayout.CENTER, layout.getComponentConstraint(center));
    }

    @Test
    void testGetPositionComponents() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);

        Label north = new Label("North");
        Label south = new Label("South");
        Label east = new Label("East");
        Label west = new Label("West");
        Label center = new Label("Center");

        container.add(BorderLayout.NORTH, north);
        container.add(BorderLayout.SOUTH, south);
        container.add(BorderLayout.EAST, east);
        container.add(BorderLayout.WEST, west);
        container.add(BorderLayout.CENTER, center);

        assertSame(north, layout.getNorth());
        assertSame(south, layout.getSouth());
        assertSame(east, layout.getEast());
        assertSame(west, layout.getWest());
        assertSame(center, layout.getCenter());
    }

    @Test
    void testOverlayComponent() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);

        Label overlay = new Label("Overlay");
        container.add(BorderLayout.OVERLAY, overlay);

        assertSame(overlay, layout.getOverlay());
        assertEquals(BorderLayout.OVERLAY, layout.getComponentConstraint(overlay));
    }

    @Test
    void testRemoveComponent() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);

        Label north = new Label("North");
        Label center = new Label("Center");

        container.add(BorderLayout.NORTH, north);
        container.add(BorderLayout.CENTER, center);

        container.removeComponent(north);

        assertNull(layout.getNorth());
        assertNull(layout.getComponentConstraint(north));
        assertSame(center, layout.getCenter());
    }

    @Test
    void testReplaceComponentInSamePosition() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);

        Label original = new Label("Original");
        Label replacement = new Label("Replacement");

        container.add(BorderLayout.NORTH, original);
        assertEquals(1, container.getComponentCount());

        container.add(BorderLayout.NORTH, replacement);
        assertEquals(1, container.getComponentCount());
        assertSame(replacement, layout.getNorth());
        assertFalse(container.contains(original));
    }

    @Test
    void testAddWithIntegerConstraints() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);

        Label top = new Label("Top");
        Label bottom = new Label("Bottom");
        Label left = new Label("Left");
        Label right = new Label("Right");
        Label middle = new Label("Middle");

        container.add(Component.TOP, top);
        container.add(Component.BOTTOM, bottom);
        container.add(Component.LEFT, left);
        container.add(Component.RIGHT, right);
        container.add(Component.CENTER, middle);

        assertSame(top, layout.getNorth());
        assertSame(bottom, layout.getSouth());
        assertSame(left, layout.getWest());
        assertSame(right, layout.getEast());
        assertSame(middle, layout.getCenter());
    }

    @Test
    void testAddWithoutConstraintThrowsException() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);
        Label label = new Label("Test");

        assertThrows(IllegalArgumentException.class, () -> {
            container.add(label);
        });
    }

    @Test
    void testCenterBehaviorScale() {
        BorderLayout layout = new BorderLayout(BorderLayout.CENTER_BEHAVIOR_SCALE);
        assertEquals(BorderLayout.CENTER_BEHAVIOR_SCALE, layout.getCenterBehavior());
    }

    @Test
    void testCenterBehaviorCenter() {
        BorderLayout layout = new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER);
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER, layout.getCenterBehavior());
    }

    @Test
    void testCenterBehaviorCenterAbsolute() {
        BorderLayout layout = new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE);
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE, layout.getCenterBehavior());
    }

    @Test
    void testCenterBehaviorTotalBelow() {
        BorderLayout layout = new BorderLayout(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW);
        assertEquals(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW, layout.getCenterBehavior());
    }

    @Test
    void testSetCenterBehavior() {
        BorderLayout layout = new BorderLayout();

        layout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_CENTER);
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER, layout.getCenterBehavior());

        layout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_SCALE);
        assertEquals(BorderLayout.CENTER_BEHAVIOR_SCALE, layout.getCenterBehavior());
    }

    @Test
    void testStaticFactoryMethods() {
        BorderLayout center = BorderLayout.center();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER, center.getCenterBehavior());

        BorderLayout absolute = BorderLayout.absolute();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE, absolute.getCenterBehavior());

        BorderLayout totalBelow = BorderLayout.totalBelow();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW, totalBelow.getCenterBehavior());
    }

    @Test
    void testScaleEdges() {
        BorderLayout layout = new BorderLayout();
        assertTrue(layout.isScaleEdges());

        layout.setScaleEdges(false);
        assertFalse(layout.isScaleEdges());

        layout.setScaleEdges(true);
        assertTrue(layout.isScaleEdges());
    }

    @Test
    void testLandscapeSwap() {
        BorderLayout layout = new BorderLayout();

        layout.defineLandscapeSwap(BorderLayout.NORTH, BorderLayout.WEST);
        assertEquals(BorderLayout.WEST, layout.getLandscapeSwap(BorderLayout.NORTH));
        assertEquals(BorderLayout.NORTH, layout.getLandscapeSwap(BorderLayout.WEST));
    }

    @Test
    void testGetLandscapeSwapWithNoSwapDefined() {
        BorderLayout layout = new BorderLayout();
        assertNull(layout.getLandscapeSwap(BorderLayout.NORTH));
    }

    @Test
    void testDeprecatedAbsoluteCenter() {
        BorderLayout layout = new BorderLayout();
        assertFalse(layout.isAbsoluteCenter());

        layout.setAbsoluteCenter(true);
        assertTrue(layout.isAbsoluteCenter());
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER, layout.getCenterBehavior());

        layout.setAbsoluteCenter(false);
        assertFalse(layout.isAbsoluteCenter());
        assertEquals(BorderLayout.CENTER_BEHAVIOR_SCALE, layout.getCenterBehavior());
    }

    @Test
    void testIsOverlapSupported() {
        BorderLayout layout = new BorderLayout();
        assertFalse(layout.isOverlapSupported());

        layout.setCenterBehavior(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW);
        assertTrue(layout.isOverlapSupported());

        BorderLayout layoutWithOverlay = new BorderLayout();
        Container container = new Container(layoutWithOverlay);
        container.add(BorderLayout.OVERLAY, new Label("Overlay"));
        assertTrue(layoutWithOverlay.isOverlapSupported());
    }

    @Test
    void testIsConstraintTracking() {
        BorderLayout layout = new BorderLayout();
        assertTrue(layout.isConstraintTracking());
    }

    @Test
    void testObscuresPotential() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);

        assertFalse(layout.obscuresPotential(container));

        container.add(BorderLayout.CENTER, new Label("Center"));
        assertTrue(layout.obscuresPotential(container));
    }

    @Test
    void testToString() {
        BorderLayout layout = new BorderLayout();
        assertEquals("BorderLayout", layout.toString());
    }

    @Test
    void testEquals() {
        BorderLayout layout1 = new BorderLayout();
        BorderLayout layout2 = new BorderLayout();
        BorderLayout layout3 = new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER);

        assertTrue(layout1.equals(layout2));
        assertFalse(layout1.equals(layout3));
    }

    @Test
    void testEqualsWithLandscapeSwap() {
        BorderLayout layout1 = new BorderLayout();
        BorderLayout layout2 = new BorderLayout();

        layout1.defineLandscapeSwap(BorderLayout.NORTH, BorderLayout.WEST);
        assertFalse(layout1.equals(layout2));

        layout2.defineLandscapeSwap(BorderLayout.NORTH, BorderLayout.WEST);
        assertTrue(layout1.equals(layout2));
    }

    @Test
    void testPreferredSizeWithNoComponents() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(100, 100));

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertEquals(0, preferredSize.getWidth());
        assertEquals(0, preferredSize.getHeight());
    }

    @Test
    void testPreferredSizeWithComponents() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);

        Button north = new Button("North");
        Button center = new Button("Center");

        container.add(BorderLayout.NORTH, north);
        container.add(BorderLayout.CENTER, center);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertTrue(preferredSize.getWidth() > 0);
        assertTrue(preferredSize.getHeight() > 0);
    }

    @FormTest
    void testLayoutContainerBasicPositioning() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        Label north = new Label("North");
        Label south = new Label("South");
        Label east = new Label("East");
        Label west = new Label("West");
        Label center = new Label("Center");

        container.add(BorderLayout.NORTH, north);
        container.add(BorderLayout.SOUTH, south);
        container.add(BorderLayout.EAST, east);
        container.add(BorderLayout.WEST, west);
        container.add(BorderLayout.CENTER, center);

        container.layoutContainer();

        // North should be at or near the top (accounting for padding)
        assertTrue(north.getY() < 10, "North should be near top, but was at Y=" + north.getY());

        // South should be at bottom
        assertTrue(south.getY() + south.getHeight() <= container.getHeight());

        // Center should be between north/south and west/east
        assertTrue(center.getY() >= north.getY() + north.getHeight());
        assertTrue(center.getY() + center.getHeight() <= south.getY());
    }

    @FormTest
    void testLayoutContainerWithOverlay() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        Label center = new Label("Center");
        Label overlay = new Label("Overlay");

        container.add(BorderLayout.CENTER, center);
        container.add(BorderLayout.OVERLAY, overlay);

        container.layoutContainer();

        // Overlay should cover entire container
        assertEquals(0, overlay.getX());
        assertEquals(0, overlay.getY());
        assertEquals(container.getWidth(), overlay.getWidth());
        assertEquals(container.getHeight(), overlay.getHeight());
    }

    @Test
    void testStaticContainerFactories() {
        Label centerLabel = new Label("Center");
        Container centerContainer = BorderLayout.center(centerLabel);
        assertTrue(centerContainer.getLayout() instanceof BorderLayout);
        assertTrue(centerContainer.contains(centerLabel));

        Label northLabel = new Label("North");
        Container northContainer = BorderLayout.north(northLabel);
        assertTrue(northContainer.getLayout() instanceof BorderLayout);
        assertTrue(northContainer.contains(northLabel));

        Label southLabel = new Label("South");
        Container southContainer = BorderLayout.south(southLabel);
        assertTrue(southContainer.getLayout() instanceof BorderLayout);
        assertTrue(southContainer.contains(southLabel));

        Label eastLabel = new Label("East");
        Container eastContainer = BorderLayout.east(eastLabel);
        assertTrue(eastContainer.getLayout() instanceof BorderLayout);
        assertTrue(eastContainer.contains(eastLabel));

        Label westLabel = new Label("West");
        Container westContainer = BorderLayout.west(westLabel);
        assertTrue(westContainer.getLayout() instanceof BorderLayout);
        assertTrue(westContainer.contains(westLabel));
    }

    @Test
    void testCenterEastWestFactory() {
        Label center = new Label("Center");
        Label east = new Label("East");
        Label west = new Label("West");

        Container container = BorderLayout.centerEastWest(center, east, west);
        assertTrue(container.getLayout() instanceof BorderLayout);
        assertTrue(container.contains(center));
        assertTrue(container.contains(east));
        assertTrue(container.contains(west));
    }

    @Test
    void testCenterEastWestFactoryWithNulls() {
        Label center = new Label("Center");

        Container container = BorderLayout.centerEastWest(center, null, null);
        assertTrue(container.getLayout() instanceof BorderLayout);
        assertTrue(container.contains(center));
        assertEquals(1, container.getComponentCount());
    }

    @Test
    void testCenterAbsoluteEastWestFactory() {
        Label center = new Label("Center");
        Label east = new Label("East");

        Container container = BorderLayout.centerAbsoluteEastWest(center, east, null);
        BorderLayout layout = (BorderLayout) container.getLayout();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE, layout.getCenterBehavior());
    }

    @Test
    void testCenterCenterEastWestFactory() {
        Label center = new Label("Center");

        Container container = BorderLayout.centerCenterEastWest(center, null, null);
        BorderLayout layout = (BorderLayout) container.getLayout();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER, layout.getCenterBehavior());
    }

    @Test
    void testCenterTotalBelowEastWestFactory() {
        Label center = new Label("Center");

        Container container = BorderLayout.centerTotalBelowEastWest(center, null, null);
        BorderLayout layout = (BorderLayout) container.getLayout();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW, layout.getCenterBehavior());
    }

    @Test
    void testCenterCenter() {
        Label label = new Label("Test");
        Container container = BorderLayout.centerCenter(label);
        BorderLayout layout = (BorderLayout) container.getLayout();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER, layout.getCenterBehavior());
    }

    @Test
    void testCenterAbsolute() {
        Label label = new Label("Test");
        Container container = BorderLayout.centerAbsolute(label);
        BorderLayout layout = (BorderLayout) container.getLayout();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE, layout.getCenterBehavior());
    }

    @Test
    void testCenterTotalBelow() {
        Label label = new Label("Test");
        Container container = BorderLayout.centerTotalBelow(label);
        BorderLayout layout = (BorderLayout) container.getLayout();
        assertEquals(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW, layout.getCenterBehavior());
    }

    @Test
    void testDeprecatedCenterBehaviorTotalBellow() {
        // Test that the deprecated constant has the same value as the correct one
        assertEquals(BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW, BorderLayout.CENTER_BEHAVIOR_TOTAL_BELLOW);
    }

    @Test
    void testOverridesTabIndices() {
        BorderLayout layout = new BorderLayout();
        Container container = new Container(layout);
        assertTrue(layout.overridesTabIndices(container));
    }
}
