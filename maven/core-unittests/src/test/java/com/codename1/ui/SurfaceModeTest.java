package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for surface mode and elevated shadows.
 */
class SurfaceModeTest extends UITestBase {

    @FormTest
    void testEnableSurfaceMode() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);

        // Enable surface mode (elevated appearance)
        btn.getAllStyles().setSurface(true);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());
    }

    @FormTest
    void testDisableSurfaceMode() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        btn.getAllStyles().setSurface(true);
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());

        // Disable surface mode
        btn.getAllStyles().setSurface(false);
        form.revalidate();

        assertFalse(btn.getAllStyles().isSurface());
    }

    @FormTest
    void testSurfaceModeWithElevation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(5);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());
        assertEquals(5, btn.getAllStyles().getElevation());
    }

    @FormTest
    void testMultipleSurfaceLevels() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button low = new Button("Low Elevation");
        low.getAllStyles().setSurface(true);
        low.getAllStyles().setElevation(1);

        Button medium = new Button("Medium Elevation");
        medium.getAllStyles().setSurface(true);
        medium.getAllStyles().setElevation(4);

        Button high = new Button("High Elevation");
        high.getAllStyles().setSurface(true);
        high.getAllStyles().setElevation(8);

        form.addAll(low, medium, high);
        form.revalidate();

        assertTrue(low.getAllStyles().getElevation() < medium.getAllStyles().getElevation());
        assertTrue(medium.getAllStyles().getElevation() < high.getAllStyles().getElevation());
    }

    @FormTest
    void testSurfaceModeWithContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container container = new Container(BoxLayout.y());
        container.getAllStyles().setSurface(true);
        container.getAllStyles().setElevation(3);

        container.add(new Label("Content"));
        form.add(BorderLayout.CENTER, container);
        form.revalidate();

        assertTrue(container.getAllStyles().isSurface());
    }

    @FormTest
    void testSurfaceModeWithRoundedCorners() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Rounded Surface");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(4);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());
    }

    @FormTest
    void testSurfaceModeWithBackgroundColor() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Colored Surface");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(3);
        btn.getAllStyles().setBgColor(0xFF5722);
        btn.getAllStyles().setBgTransparency(255);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());
        assertEquals(0xFF5722, btn.getAllStyles().getBgColor());
    }

    @FormTest
    void testSurfaceModeInherited() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container parent = new Container(BoxLayout.y());
        parent.getAllStyles().setSurface(true);

        Button child = new Button("Child");
        parent.add(child);

        form.add(BorderLayout.CENTER, parent);
        form.revalidate();

        assertTrue(parent.getAllStyles().isSurface());
    }

    @FormTest
    void testSurfaceModeToggle() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Toggle Surface");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        // Toggle surface mode multiple times
        for (int i = 0; i < 5; i++) {
            btn.getAllStyles().setSurface(i % 2 == 0);
            form.revalidate();
        }

        assertFalse(btn.getAllStyles().isSurface());
    }

    @FormTest
    void testSurfaceModeWithAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Animated Surface");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(4);

        form.add(btn);
        form.revalidate();

        // Animate layout
        form.animateLayout(200);

        assertTrue(btn.getAllStyles().isSurface());
    }

    @FormTest
    void testZeroElevation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Zero Elevation");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(0);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertEquals(0, btn.getAllStyles().getElevation());
    }

    @FormTest
    void testMaxElevation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Max Elevation");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(24);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertEquals(24, btn.getAllStyles().getElevation());
    }

    @FormTest
    void testSurfaceModeWithDifferentStates() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Multi-State");

        btn.getUnselectedStyle().setSurface(true);
        btn.getUnselectedStyle().setElevation(2);

        btn.getSelectedStyle().setSurface(true);
        btn.getSelectedStyle().setElevation(8);

        btn.getPressedStyle().setSurface(true);
        btn.getPressedStyle().setElevation(1);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getUnselectedStyle().isSurface());
        assertTrue(btn.getSelectedStyle().isSurface());
        assertTrue(btn.getPressedStyle().isSurface());
    }

    @FormTest
    void testSurfaceModeWithTransparency() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Transparent Surface");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(4);
        btn.getAllStyles().setBgTransparency(128);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());
        assertEquals(128, btn.getAllStyles().getBgTransparency());
    }

    @FormTest
    void testSurfaceModeInDialog() {
        Form form = CN.getCurrentForm();

        Dialog dialog = new Dialog("Surface Dialog");
        dialog.setLayout(new BorderLayout());

        Button btn = new Button("Dialog Button");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(4);

        dialog.add(BorderLayout.CENTER, btn);

        assertTrue(btn.getAllStyles().isSurface());
    }

    @FormTest
    void testSurfaceModeWithMarginAndPadding() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Padded Surface");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(3);
        btn.getAllStyles().setMargin(10, 10, 10, 10);
        btn.getAllStyles().setPadding(15, 15, 15, 15);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());
    }

    @FormTest
    void testSurfaceModeWithBorder() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Border Surface");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(3);
        btn.getAllStyles().setBorder(com.codename1.ui.plaf.Border.createLineBorder(2));

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());
    }

    @FormTest
    void testSurfaceModeInScrollableContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);

        for (int i = 0; i < 20; i++) {
            Button btn = new Button("Surface " + i);
            btn.getAllStyles().setSurface(true);
            btn.getAllStyles().setElevation(2 + i);
            scrollable.add(btn);
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        assertTrue(scrollable.getComponentCount() == 20);
    }

    @FormTest
    void testSurfaceModePreservationOnRevalidate() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Persistent Surface");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(5);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());

        // Revalidate multiple times
        for (int i = 0; i < 5; i++) {
            form.revalidate();
        }

        assertTrue(btn.getAllStyles().isSurface());
        assertEquals(5, btn.getAllStyles().getElevation());
    }

    @FormTest
    void testSurfaceModeWithRTL() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("RTL Surface");
        btn.getAllStyles().setSurface(true);
        btn.getAllStyles().setElevation(4);

        form.add(BorderLayout.CENTER, btn);
        form.setRTL(true);
        form.revalidate();

        assertTrue(btn.getAllStyles().isSurface());
        assertTrue(form.isRTL());
    }
}
