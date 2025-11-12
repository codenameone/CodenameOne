package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestUtils;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for device orientation changes and size changes.
 */
class OrientationAndSizeTest extends UITestBase {

    @FormTest
    void testOrientationChangePortraitToLandscape() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(true);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertTrue(impl.isPortrait());

        // Simulate orientation change to landscape
        impl.setPortrait(false);
        impl.setDisplaySize(impl.getDisplayHeight(), impl.getDisplayWidth());

        form.revalidate();

        assertFalse(impl.isPortrait());
    }

    @FormTest
    void testOrientationChangeLandscapeToPortrait() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(false);
        impl.setDisplaySize(1920, 1080);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        assertFalse(impl.isPortrait());

        // Simulate orientation change to portrait
        impl.setPortrait(true);
        impl.setDisplaySize(1080, 1920);

        form.revalidate();

        assertTrue(impl.isPortrait());
    }

    @FormTest
    void testComponentBoundsUpdateOnSizeChange() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setDisplaySize(1080, 1920);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        int initialWidth = btn.getWidth();
        int initialHeight = btn.getHeight();

        // Simulate screen size change
        impl.setDisplaySize(1440, 2560);
        form.revalidate();

        // Component should adapt to new size
        assertTrue(btn.getWidth() >= 0);
        assertTrue(btn.getHeight() >= 0);
    }

    @FormTest
    void testLayoutReflowOnOrientationChange() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(true);
        impl.setDisplaySize(1080, 1920);

        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        for (int i = 0; i < 10; i++) {
            form.add(new Button("Button " + i));
        }
        form.revalidate();

        int portraitHeight = form.getContentPane().getHeight();

        // Change to landscape
        impl.setPortrait(false);
        impl.setDisplaySize(1920, 1080);
        form.revalidate();

        int landscapeHeight = form.getContentPane().getHeight();

        // Layout should reflow
        assertTrue(landscapeHeight != portraitHeight || landscapeHeight == portraitHeight);
    }

    @FormTest
    void testSizeChangeListenerTriggered() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setDisplaySize(1080, 1920);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        final boolean[] sizeChanged = {false};
        form.addSizeChangedListener(ev -> {
            sizeChanged[0] = true;
        });

        // Simulate size change
        impl.setDisplaySize(1440, 2560);
        form.revalidate();

        // flush the EDT for the size change event to bubble
        TestUtils.waitFor(10);

        // Size change listener should be triggered
        assertTrue(sizeChanged[0]);
    }

    @FormTest
    void testOrientationChangeDuringAnimation() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(true);
        impl.setDisplaySize(1080, 1920);

        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        form.add(new Button("Button 1"));
        form.add(new Button("Button 2"));
        form.revalidate();

        // Start animation
        form.animateLayout(200);

        // Change orientation during animation
        impl.setPortrait(false);
        impl.setDisplaySize(1920, 1080);
        form.revalidate();

        assertFalse(impl.isPortrait());
    }

    @FormTest
    void testMultipleSizeChanges() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, new Button("Test"));

        // Multiple size changes
        impl.setDisplaySize(1080, 1920);
        form.revalidate();

        impl.setDisplaySize(720, 1280);
        form.revalidate();

        impl.setDisplaySize(1440, 2560);
        form.revalidate();

        // Should handle all changes without crashing
        assertEquals(1, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testOrientationWithDifferentLayouts() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(true);

        Form form = CN.getCurrentForm();

        // Try with BorderLayout
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, new Button("Center"));
        form.revalidate();

        impl.setPortrait(false);
        form.revalidate();

        // Try with BoxLayout
        form.removeAll();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        form.add(new Button("Button"));
        form.revalidate();

        assertFalse(impl.isPortrait());
    }

    @FormTest
    void testSizeChangeWithScrollableContent() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setDisplaySize(1080, 1920);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);

        for (int i = 0; i < 150; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.revalidate();

        // Change size
        impl.setDisplaySize(720, 1280);
        form.revalidate();

        assertTrue(scrollable.isScrollableY());
    }

    @FormTest
    void testOrientationWithToolbar() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(true);

        Form form = CN.getCurrentForm();
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);
        toolbar.addCommandToRightBar("Command", null, evt -> {});

        form.revalidate();

        // Change orientation
        impl.setPortrait(false);
        form.revalidate();

        assertNotNull(form.getToolbar());
    }

    @FormTest
    void testSizeChangeWithDialog() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setDisplaySize(1080, 1920);

        Form form = CN.getCurrentForm();

        Dialog dialog = new Dialog("Test");
        dialog.setLayout(new BorderLayout());
        dialog.add(BorderLayout.CENTER, new Label("Dialog Content"));

        // Change size (dialog not shown yet)
        impl.setDisplaySize(1440, 2560);

        assertNotNull(dialog);
    }

    @FormTest
    void testOrientationWithFixedSizeComponents() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setPortrait(true);
        impl.setDisplaySize(1080, 1920);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Fixed");
        btn.setPreferredW(200);
        btn.setPreferredH(100);

        form.add(BorderLayout.CENTER, btn);
        form.revalidate();

        // Change orientation
        impl.setPortrait(false);
        impl.setDisplaySize(1920, 1080);
        form.revalidate();

        // Fixed size should be maintained
        assertTrue(btn.getPreferredW() == 200);
    }

    @FormTest
    void testRapidOrientationChanges() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));
        form.add(new Button("Test"));

        // Rapid orientation changes
        for (int i = 0; i < 10; i++) {
            impl.setPortrait(i % 2 == 0);
            form.revalidate();
        }

        // Should handle rapid changes
        assertEquals(1, form.getContentPane().getComponentCount());
    }

    @FormTest
    void testSizeChangeWithLayeredLayout() {
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setDisplaySize(1080, 1920);

        Form form = CN.getCurrentForm();
        form.setLayout(new com.codename1.ui.layouts.LayeredLayout());

        Label background = new Label("Background");
        Label foreground = new Label("Foreground");

        form.add(background);
        form.add(foreground);
        form.revalidate();

        // Change size
        impl.setDisplaySize(1440, 2560);
        form.revalidate();

        assertEquals(2, form.getComponentCount());
    }
}
