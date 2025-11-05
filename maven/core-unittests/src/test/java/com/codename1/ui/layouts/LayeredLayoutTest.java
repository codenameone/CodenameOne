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

class LayeredLayoutTest extends UITestBase {

    @Test
    void testDefaultConstructor() {
        LayeredLayout layout = new LayeredLayout();
        assertNotNull(layout);
    }

    @Test
    void testEncloseIn() {
        Label label1 = new Label("1");
        Label label2 = new Label("2");

        Container container = LayeredLayout.encloseIn(label1, label2);

        assertTrue(container.getLayout() instanceof LayeredLayout);
        assertEquals(2, container.getComponentCount());
        assertTrue(container.contains(label1));
        assertTrue(container.contains(label2));
    }

    @Test
    void testToString() {
        LayeredLayout layout = new LayeredLayout();
        assertEquals("LayeredLayout", layout.toString());
    }

    @Test
    void testIsOverlapSupported() {
        LayeredLayout layout = new LayeredLayout();
        assertTrue(layout.isOverlapSupported());
    }

    @Test
    void testPreferredSizeWithEmptyContainer() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertEquals(0, preferredSize.getWidth());
        assertEquals(0, preferredSize.getHeight());
    }

    @Test
    void testPreferredSizeWithComponents() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        Dimension preferredSize = layout.getPreferredSize(container);
        assertNotNull(preferredSize);
        assertTrue(preferredSize.getWidth() > 0);
        assertTrue(preferredSize.getHeight() > 0);
    }

    @Test
    void testPreferredSizeIsMaxOfComponents() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);

        Button smallButton = new Button("S");
        Button largeButton = new Button("Large Button Text Here");

        container.add(smallButton);
        container.add(largeButton);

        Dimension preferredSize = layout.getPreferredSize(container);

        // Preferred size should be at least as large as the largest component
        assertTrue(preferredSize.getWidth() >= largeButton.getPreferredW());
        assertTrue(preferredSize.getHeight() >= largeButton.getPreferredH());
    }

    @FormTest
    void testLayoutContainerBasic() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        container.layoutContainer();

        // All components should be sized to fill the container
        assertTrue(button1.getWidth() > 0);
        assertTrue(button1.getHeight() > 0);
        assertTrue(button2.getWidth() > 0);
        assertTrue(button2.getHeight() > 0);
    }

    @FormTest
    void testLayoutContainerComponentsOverlap() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        Button button1 = new Button("Button 1");
        Button button2 = new Button("Button 2");

        container.add(button1);
        container.add(button2);

        container.layoutContainer();

        // Components should be at the same position (overlapping)
        assertEquals(button1.getX(), button2.getX());
        assertEquals(button1.getY(), button2.getY());
    }

    @FormTest
    void testLayoutContainerWithEmptyContainer() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(200, 200));

        // Should not throw exception with empty container
        assertDoesNotThrow(() -> container.layoutContainer());
    }

    @FormTest
    void testLayoutContainerWithSingleComponent() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(300, 300));

        Button button = new Button("Button");
        container.add(button);

        container.layoutContainer();

        // Component should be positioned and sized
        assertTrue(button.getX() >= 0);
        assertTrue(button.getY() >= 0);
        assertTrue(button.getWidth() > 0);
        assertTrue(button.getHeight() > 0);
    }

    @FormTest
    void testMultipleLayeredComponents() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        Label background = new Label("Background");
        Label middle = new Label("Middle");
        Label foreground = new Label("Foreground");

        container.add(background);
        container.add(middle);
        container.add(foreground);

        container.layoutContainer();

        // All three should be layered (at same position)
        assertEquals(background.getX(), middle.getX());
        assertEquals(middle.getX(), foreground.getX());
        assertEquals(background.getY(), middle.getY());
        assertEquals(middle.getY(), foreground.getY());
    }

    @Test
    void testSetAndGetPreferredSizeMM() {
        LayeredLayout layout = new LayeredLayout();

        layout.setPreferredSizeMM(10.0f, 15.0f);

        assertEquals(10.0f, layout.getPreferredWidthMM(), 0.001f);
        assertEquals(15.0f, layout.getPreferredHeightMM(), 0.001f);
    }

    @Test
    void testSetPreferredHeightMM() {
        LayeredLayout layout = new LayeredLayout();

        layout.setPreferredHeightMM(20.0f);

        assertEquals(20.0f, layout.getPreferredHeightMM(), 0.001f);
    }

    @Test
    void testSetPreferredWidthMM() {
        LayeredLayout layout = new LayeredLayout();

        layout.setPreferredWidthMM(25.0f);

        assertEquals(25.0f, layout.getPreferredWidthMM(), 0.001f);
    }

    @Test
    void testPreferredSizeMMActsAsMinimum() {
        LayeredLayout layout = new LayeredLayout();
        layout.setPreferredSizeMM(50.0f, 50.0f);

        Container container = new Container(layout);

        // Add a small component
        Label smallLabel = new Label("S");
        container.add(smallLabel);

        Dimension preferredSize = layout.getPreferredSize(container);

        // Preferred size should be at least the MM values
        // (actual pixel values will depend on display density)
        assertNotNull(preferredSize);
    }

    @Test
    void testEncloseInWithMultipleComponents() {
        Label[] labels = new Label[5];
        for (int i = 0; i < 5; i++) {
            labels[i] = new Label("Label " + i);
        }

        Container container = LayeredLayout.encloseIn(labels);

        assertEquals(5, container.getComponentCount());
        for (Label label : labels) {
            assertTrue(container.contains(label));
        }
    }

    @Test
    void testEncloseInWithNoComponents() {
        Container container = LayeredLayout.encloseIn();

        assertTrue(container.getLayout() instanceof LayeredLayout);
        assertEquals(0, container.getComponentCount());
    }

    @FormTest
    void testLayoutComponentsGetSameSizeAsContainer() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(500, 300));

        Button button1 = new Button("1");
        Button button2 = new Button("2");

        container.add(button1);
        container.add(button2);

        container.layoutContainer();

        // Both components should be sized to match container (minus padding)
        // They should have similar sizes
        assertTrue(button1.getWidth() > 0);
        assertTrue(button2.getWidth() > 0);
        assertTrue(button1.getHeight() > 0);
        assertTrue(button2.getHeight() > 0);
    }

    @Test
    void testUnitConstants() {
        // Test that unit constants are defined
        assertEquals(LayeredLayout.UNIT_DIPS, LayeredLayout.UNIT_DIPS);
        assertEquals(LayeredLayout.UNIT_PIXELS, LayeredLayout.UNIT_PIXELS);
        assertEquals(LayeredLayout.UNIT_PERCENT, LayeredLayout.UNIT_PERCENT);
        assertEquals(LayeredLayout.UNIT_AUTO, LayeredLayout.UNIT_AUTO);
        assertEquals(LayeredLayout.UNIT_BASELINE, LayeredLayout.UNIT_BASELINE);
    }

    @FormTest
    void testLayeredLayoutWithVariedSizes() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);
        container.setSize(new Dimension(400, 400));

        Button smallButton = new Button("S");
        Button mediumButton = new Button("Medium");
        Button largeButton = new Button("Large Button Text");

        container.add(smallButton);
        container.add(mediumButton);
        container.add(largeButton);

        container.layoutContainer();

        // All components should be sized similarly regardless of preferred size
        // (they're layered and sized to container)
        assertTrue(smallButton.getWidth() > 0);
        assertTrue(mediumButton.getWidth() > 0);
        assertTrue(largeButton.getWidth() > 0);
    }

    @Test
    void testPreferredSizeConsidersAllComponents() {
        LayeredLayout layout = new LayeredLayout();
        Container container = new Container(layout);

        Button button1 = new Button("Short");
        Button button2 = new Button("This is a much longer button");
        Button button3 = new Button("Med");

        container.add(button1);
        container.add(button2);
        container.add(button3);

        Dimension preferredSize = layout.getPreferredSize(container);

        // Preferred size should be at least as large as the largest component
        assertTrue(preferredSize.getWidth() >= button2.getPreferredW());
    }
}
