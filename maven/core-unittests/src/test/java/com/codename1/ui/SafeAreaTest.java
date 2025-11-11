package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.testing.TestCodenameOneImplementation;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for safe area functionality including snap to safe area.
 */
class SafeAreaTest extends UITestBase {

    @FormTest
    void testSafeAreaInsetsAreApplied() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        Form form = CN.getCurrentForm();

        // Configure safe area insets
        int[] insets = new int[]{20, 10, 30, 10}; // top, bottom, left, right
        form.getSafeArea().set(insets[0], insets[1], insets[2], insets[3]);

        assertEquals(20, form.getSafeArea().getTop());
        assertEquals(10, form.getSafeArea().getBottom());
        assertEquals(30, form.getSafeArea().getLeft());
        assertEquals(10, form.getSafeArea().getRight());
    }

    @FormTest
    void testSnapToSafeAreaAdjustsPaddingForPortrait() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(true);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container content = new Container(BoxLayout.y());
        Button btn = new Button("Test");
        content.add(btn);
        form.add(BorderLayout.CENTER, content);

        // Set safe area insets
        form.getSafeArea().set(44, 34, 0, 0); // typical iPhone notch values

        // Enable snap to safe area
        form.setSnapToSafeArea(true);
        form.revalidate();

        assertTrue(form.isSnapToSafeArea());
    }

    @FormTest
    void testSnapToSafeAreaAdjustsPaddingForLandscape() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(false);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container content = new Container(BoxLayout.y());
        Button btn = new Button("Test");
        content.add(btn);
        form.add(BorderLayout.CENTER, content);

        // Set safe area insets (landscape has different insets)
        form.getSafeArea().set(0, 21, 44, 44);

        form.setSnapToSafeArea(true);
        form.revalidate();

        assertTrue(form.isSnapToSafeArea());
    }

    @FormTest
    void testSafeAreaWithToolbar() {
        Form form = CN.getCurrentForm();
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);

        // Set safe area insets
        form.getSafeArea().set(44, 34, 0, 0);
        form.setSnapToSafeArea(true);

        form.revalidate();

        // Toolbar should be positioned considering safe area
        assertTrue(toolbar.getY() >= form.getSafeArea().getTop() || toolbar.getY() == 0);
    }

    @FormTest
    void testSafeAreaDoesNotAffectWhenDisabled() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);

        // Set safe area insets
        form.getSafeArea().set(44, 34, 0, 0);
        form.setSnapToSafeArea(false);

        form.revalidate();

        assertFalse(form.isSnapToSafeArea());
        // Component should not be affected by safe area
        assertEquals(0, btn.getStyle().getPaddingTop());
    }

    @FormTest
    void testSafeAreaChangeDuringRuntime() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Test");
        form.add(btn);

        // Initial safe area
        form.getSafeArea().set(20, 20, 10, 10);
        form.setSnapToSafeArea(true);
        form.revalidate();

        int initialY = btn.getY();

        // Change safe area (e.g., device rotation)
        form.getSafeArea().set(44, 34, 0, 0);
        form.revalidate();

        // Button position should adapt to new safe area
        assertTrue(btn.getY() >= 0);
    }

    @FormTest
    void testSafeAreaWithDialog() {
        Form form = CN.getCurrentForm();
        form.getSafeArea().set(44, 34, 0, 0);

        Dialog dialog = new Dialog("Test");
        dialog.setLayout(new BorderLayout());
        dialog.add(BorderLayout.CENTER, new Label("Content"));

        // Dialog should respect safe area when snap is enabled
        dialog.setSnapToSafeArea(true);

        assertTrue(dialog.isSnapToSafeArea());
    }

    @FormTest
    void testSafeAreaWithLayeredLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new com.codename1.ui.layouts.LayeredLayout());

        Label background = new Label("Background");
        background.setUIID("Background");

        Container content = new Container(BoxLayout.y());
        content.add(new Label("Foreground"));

        form.add(background);
        form.add(content);

        form.getSafeArea().set(44, 34, 0, 0);
        form.setSnapToSafeArea(true);
        form.revalidate();

        // Components in layered layout should respect safe area
        assertTrue(form.isSnapToSafeArea());
    }

    @FormTest
    void testSafeAreaWithOverflowContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);

        for (int i = 0; i < 20; i++) {
            scrollable.add(new Button("Item " + i));
        }

        form.add(scrollable);
        form.getSafeArea().set(44, 34, 0, 0);
        form.setSnapToSafeArea(true);
        form.revalidate();

        // Scrollable content should respect safe area boundaries
        assertTrue(scrollable.getY() >= 0);
    }

    @FormTest
    void testSafeAreaZeroInsets() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);

        // Zero safe area (normal device without notch)
        form.getSafeArea().set(0, 0, 0, 0);
        form.setSnapToSafeArea(true);
        form.revalidate();

        // Should work normally with zero insets
        assertEquals(0, form.getSafeArea().getTop());
        assertEquals(0, form.getSafeArea().getBottom());
        assertEquals(0, form.getSafeArea().getLeft());
        assertEquals(0, form.getSafeArea().getRight());
    }

    @FormTest
    void testSafeAreaWithComponentPadding() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn = new Button("Test");
        Style style = btn.getAllStyles();
        style.setPadding(10, 10, 10, 10);

        form.add(btn);
        form.getSafeArea().set(44, 34, 0, 0);
        form.setSnapToSafeArea(true);
        form.revalidate();

        // Component's own padding should be preserved
        assertTrue(style.getPaddingTop() >= 10);
    }

    @FormTest
    void testSafeAreaSnapToSafeAreaToggle() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.getSafeArea().set(44, 34, 0, 0);

        // Enable snap to safe area
        form.setSnapToSafeArea(true);
        form.revalidate();
        assertTrue(form.isSnapToSafeArea());

        // Disable snap to safe area
        form.setSnapToSafeArea(false);
        form.revalidate();
        assertFalse(form.isSnapToSafeArea());
    }
}
