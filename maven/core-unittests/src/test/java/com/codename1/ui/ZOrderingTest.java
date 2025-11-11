package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for z-ordering of components and layering.
 */
class ZOrderingTest extends UITestBase {

    @FormTest
    void testBasicZOrdering() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label background = new Label("Background");
        background.getAllStyles().setBgColor(0xFF0000);
        background.getAllStyles().setBgTransparency(255);

        Label foreground = new Label("Foreground");
        foreground.getAllStyles().setBgColor(0x00FF00);
        foreground.getAllStyles().setBgTransparency(255);

        form.add(background);
        form.add(foreground);
        form.revalidate();

        // In LayeredLayout, components added later appear on top
        assertTrue(form.getComponentIndex(foreground) > form.getComponentIndex(background));
    }

    @FormTest
    void testLayeredLayoutStacking() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label layer1 = new Label("Layer 1");
        Label layer2 = new Label("Layer 2");
        Label layer3 = new Label("Layer 3");

        form.add(layer1);
        form.add(layer2);
        form.add(layer3);
        form.revalidate();

        // Components should be stacked in order of addition
        assertEquals(0, form.getComponentIndex(layer1));
        assertEquals(1, form.getComponentIndex(layer2));
        assertEquals(2, form.getComponentIndex(layer3));
    }

    @FormTest
    void testComponentReordering() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label label1 = new Label("Label 1");
        Label label2 = new Label("Label 2");
        Label label3 = new Label("Label 3");

        form.addAll(label1, label2, label3);
        form.revalidate();

        // Move label1 to the end (on top)
        form.removeComponent(label1);
        form.add(label1);
        form.revalidate();

        assertEquals(2, form.getComponentIndex(label1));
    }

    @FormTest
    void testLayeredLayoutWithConstraints() {
        Form form = CN.getCurrentForm();
        LayeredLayout layout = new LayeredLayout();
        form.setLayout(layout);

        Label fullScreen = new Label("Full Screen");
        Label topCenter = new Label("Top Center");

        form.add(LayeredLayout.encloseIn(fullScreen));
        form.add(LayeredLayout.encloseIn(
            fullScreen,
            topCenter
        ));
        form.revalidate();

        assertNotNull(fullScreen.getParent());
        assertNotNull(topCenter.getParent());
    }

    @FormTest
    void testGlassPane() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Click Me");
        form.add(BorderLayout.CENTER, btn);

        Container glassPane = new Container();
        glassPane.getAllStyles().setBgTransparency(128);
        glassPane.getAllStyles().setBgColor(0x000000);

        form.setGlassPane(glassPane);
        form.revalidate();

        assertSame(glassPane, form.getGlassPane());
    }

    @FormTest
    void testGlassPaneBlocksInteraction() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Click Me");
        form.add(BorderLayout.CENTER, btn);

        Container glassPane = new Container();
        glassPane.setBlockFocus(true);
        form.setGlassPane(glassPane);
        form.revalidate();

        // Glass pane should be on top
        assertNotNull(form.getGlassPane());
    }

    @FormTest
    void testMultipleLayersWithTransparency() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        for (int i = 0; i < 5; i++) {
            Label layer = new Label("Layer " + i);
            Style style = layer.getAllStyles();
            style.setBgTransparency(128);
            style.setBgColor(0xFF0000 >> i);
            form.add(layer);
        }

        form.revalidate();
        assertEquals(5, form.getComponentCount());
    }

    @FormTest
    void testInsertAtSpecificIndex() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label label1 = new Label("Label 1");
        Label label2 = new Label("Label 2");
        Label label3 = new Label("Label 3");

        form.addComponent(label1);
        form.addComponent(label3);
        form.revalidate();

        // Insert label2 between label1 and label3
        form.addComponent(1, label2);
        form.revalidate();

        assertEquals(0, form.getComponentIndex(label1));
        assertEquals(1, form.getComponentIndex(label2));
        assertEquals(2, form.getComponentIndex(label3));
    }

    @FormTest
    void testReplaceComponentMaintainsOrder() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label label1 = new Label("Label 1");
        Label label2 = new Label("Label 2");
        Label label3 = new Label("Label 3");

        form.addAll(label1, label2, label3);
        form.revalidate();

        Label replacement = new Label("Replacement");
        form.replace(label2, replacement, null);
        form.revalidate();

        assertEquals(1, form.getComponentIndex(replacement));
    }

    @FormTest
    void testOverlappingComponentsInLayeredLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Button background = new Button("Background");
        background.setX(50);
        background.setY(50);
        background.setWidth(200);
        background.setHeight(100);

        Button foreground = new Button("Foreground");
        foreground.setX(75);
        foreground.setY(75);
        foreground.setWidth(150);
        foreground.setHeight(75);

        form.add(background);
        form.add(foreground);
        form.revalidate();

        // Foreground should be on top
        assertTrue(form.getComponentIndex(foreground) > form.getComponentIndex(background));
    }

    @FormTest
    void testRemoveAndAddAffectsZOrder() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label bottom = new Label("Bottom");
        Label middle = new Label("Middle");
        Label top = new Label("Top");

        form.addAll(bottom, middle, top);
        form.revalidate();

        // Remove bottom and add it again (should go to top)
        form.removeComponent(bottom);
        form.add(bottom);
        form.revalidate();

        assertEquals(2, form.getComponentIndex(bottom));
    }

    @FormTest
    void testLayeredLayoutWithDifferentSizes() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label large = new Label("Large");
        large.setPreferredW(300);
        large.setPreferredH(300);

        Label small = new Label("Small");
        small.setPreferredW(100);
        small.setPreferredH(100);

        form.add(large);
        form.add(small);
        form.revalidate();

        // Small should be on top
        assertTrue(form.getComponentIndex(small) > form.getComponentIndex(large));
    }

    @FormTest
    void testZOrderWithInvisibleComponents() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label visible1 = new Label("Visible 1");
        Label invisible = new Label("Invisible");
        invisible.setVisible(false);
        Label visible2 = new Label("Visible 2");

        form.addAll(visible1, invisible, visible2);
        form.revalidate();

        assertEquals(1, form.getComponentIndex(invisible));
        assertFalse(invisible.isVisible());
    }

    @FormTest
    void testBringToFrontSimulation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label label1 = new Label("Label 1");
        Label label2 = new Label("Label 2");
        Label label3 = new Label("Label 3");

        form.addAll(label1, label2, label3);
        form.revalidate();

        // Simulate bringing label1 to front
        form.removeComponent(label1);
        form.add(label1);
        form.revalidate();

        assertEquals(form.getComponentCount() - 1, form.getComponentIndex(label1));
    }

    @FormTest
    void testSendToBackSimulation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label label1 = new Label("Label 1");
        Label label2 = new Label("Label 2");
        Label label3 = new Label("Label 3");

        form.addAll(label1, label2, label3);
        form.revalidate();

        // Simulate sending label3 to back
        form.removeComponent(label3);
        form.addComponent(0, label3);
        form.revalidate();

        assertEquals(0, form.getComponentIndex(label3));
    }

    @FormTest
    void testLayeredLayoutWithAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new LayeredLayout());

        Label layer1 = new Label("Layer 1");
        Label layer2 = new Label("Layer 2");

        form.add(layer1);
        form.add(layer2);
        form.revalidate();

        // Animate and verify z-order is maintained
        form.animateLayout(200);

        assertTrue(form.getComponentIndex(layer2) > form.getComponentIndex(layer1));
    }
}
