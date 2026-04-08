package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.plaf.Style;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class ToastBarTest extends UITestBase {

    /**
     * Upper bound on the default UIID padding (in pixels).  Safe-area compensation
     * values are typically 30-100+ px, so anything below this threshold means the
     * safe-area code path did not add extra padding.
     */
    private static final int MAX_DEFAULT_STYLE_PADDING = 10;

    @FormTest
    void testGetInstanceReturnsSingleton() {
        ToastBar tb1 = ToastBar.getInstance();
        ToastBar tb2 = ToastBar.getInstance();
        assertSame(tb1, tb2);
    }

    @FormTest
    void testDefaultMessageTimeoutGetterAndSetter() {
        int original = ToastBar.getDefaultMessageTimeout();
        try {
            ToastBar.setDefaultMessageTimeout(5000);
            assertEquals(5000, ToastBar.getDefaultMessageTimeout());
        } finally {
            ToastBar.setDefaultMessageTimeout(original);
        }
    }

    @FormTest
    void testDefaultUIIDGetterAndSetter() {
        ToastBar tb = ToastBar.getInstance();
        tb.setDefaultUIID("CustomToast");
        assertEquals("CustomToast", tb.getDefaultUIID());
    }

    @FormTest
    void testDefaultMessageUIIDGetterAndSetter() {
        ToastBar tb = ToastBar.getInstance();
        tb.setDefaultMessageUIID("CustomMessage");
        assertEquals("CustomMessage", tb.getDefaultMessageUIID());
    }

    @FormTest
    void testPositionGetterAndSetter() {
        ToastBar tb = ToastBar.getInstance();
        tb.setPosition(com.codename1.ui.Component.TOP);
        assertEquals(com.codename1.ui.Component.TOP, tb.getPosition());

        tb.setPosition(com.codename1.ui.Component.BOTTOM);
        assertEquals(com.codename1.ui.Component.BOTTOM, tb.getPosition());
    }

    @FormTest
    void testCreateStatusReturnsStatus() {
        ToastBar tb = ToastBar.getInstance();
        ToastBar.Status status = tb.createStatus();
        assertNotNull(status);
    }

    @FormTest
    void testShowErrorMessageStatic() {
        ToastBar.showErrorMessage("Test Error");
        // Should not throw exception
        assertTrue(true);
    }

    @FormTest
    void testShowInfoMessageStatic() {
        ToastBar.showInfoMessage("Test Info");
        // Should not throw exception
        assertTrue(true);
    }

    @FormTest
    void testShowMessageWithIcon() {
        ToastBar.Status status = ToastBar.showMessage("Test", '\uE000', 1000);
        assertNotNull(status);
    }

    // ---- Regression tests for ToastBar TOP position safe area padding ----

    /**
     * Invokes the private getToastBarComponent(boolean) method via reflection so
     * that the component and its padding are set up without triggering animations.
     */
    private Container invokeGetToastBarComponent(ToastBar tb) throws Exception {
        Method m = ToastBar.class.getDeclaredMethod("getToastBarComponent", boolean.class);
        m.setAccessible(true);
        return (Container) m.invoke(tb, true);
    }

    /**
     * Cleans up the ToastBarComponent from the current form and resets the
     * implementation's safe area to the default.
     */
    private void cleanupToastBar(Container toastBarComponent) {
        if (toastBarComponent != null) {
            toastBarComponent.remove();
        }
        Form f = Display.getInstance().getCurrent();
        if (f != null) {
            f.putClientProperty("ToastBarComponent", null);
        }
        implementation.setDisplaySafeArea(null);
    }

    /**
     * Regression test: when position is TOP and the device has a safe area inset
     * (e.g. notch), the ToastBar should NOT double-count the inset if its parent
     * container is already positioned below the safe area boundary.
     */
    @FormTest
    void testTopPositionNoPaddingWhenParentBelowSafeArea() throws Exception {
        int safeTop = 100;
        // Simulate a device with a 100px top safe area inset
        implementation.setDisplaySafeArea(new Rectangle(0, safeTop, 1080, 1920 - safeTop));

        ToastBar tb = ToastBar.getInstance();
        tb.setPosition(Component.TOP);

        Form f = Display.getInstance().getCurrent();
        f.revalidate();

        Container c = invokeGetToastBarComponent(tb);
        assertNotNull(c, "ToastBarComponent should be created");

        Container parent = c.getParent();
        assertNotNull(parent, "ToastBarComponent should have a parent");

        // If the parent's absolute Y is at or beyond the safe area top,
        // no extra padding should be added (this was the double-counting bug).
        if (parent.getAbsoluteY() >= safeTop) {
            int paddingTop = c.getStyle().getPaddingTop();
            assertTrue(paddingTop < safeTop,
                    "Top padding should NOT be the full safe area inset (" + safeTop
                    + ") when parent is already at or below the safe area, got: " + paddingTop);
        } else {
            // Parent is above the safe area boundary, padding should be the difference
            int expectedPadding = safeTop - parent.getAbsoluteY();
            int paddingTop = c.getStyle().getPaddingTop();
            assertEquals(expectedPadding, paddingTop,
                    "Top padding should equal safeArea.getY() - parent.getAbsoluteY()");
        }

        cleanupToastBar(c);
    }

    /**
     * When position is TOP and the device has NO safe area inset (safeArea.getY() == 0),
     * no extra top padding should be applied by the safe area logic.
     */
    @FormTest
    void testTopPositionNoPaddingWithoutSafeAreaInset() throws Exception {
        // Default safe area: full display (y=0)
        implementation.setDisplaySafeArea(null);

        ToastBar tb = ToastBar.getInstance();
        tb.setPosition(Component.TOP);

        Form f = Display.getInstance().getCurrent();
        f.revalidate();

        Container c = invokeGetToastBarComponent(tb);
        assertNotNull(c, "ToastBarComponent should be created");

        // The default UIID may have some small padding, but it should be well below
        // any safe area inset value.
        int paddingTop = c.getStyle().getPaddingTop();
        assertTrue(paddingTop < MAX_DEFAULT_STYLE_PADDING,
                "Top padding should not contain safe area compensation when no inset, got: " + paddingTop);

        cleanupToastBar(c);
    }

    /**
     * When position is BOTTOM and the device has a safe area bottom inset,
     * the bottom padding should reflect the bottom safe area margin.
     */
    @FormTest
    void testBottomPositionPaddingWithSafeAreaInset() throws Exception {
        int safeTop = 50;
        int safeHeight = 1820;  // leaves 50px at bottom (1920 - 50 - 1820 = 50)
        implementation.setDisplaySafeArea(new Rectangle(0, safeTop, 1080, safeHeight));

        ToastBar tb = ToastBar.getInstance();
        tb.setPosition(Component.BOTTOM);

        Form f = Display.getInstance().getCurrent();
        f.revalidate();

        Container c = invokeGetToastBarComponent(tb);
        assertNotNull(c, "ToastBarComponent should be created");

        int expectedBottomPadding = 1920 - safeTop - safeHeight;  // 50
        Style s = c.getStyle();
        assertEquals(expectedBottomPadding, s.getPaddingBottom(),
                "Bottom padding should equal the safe area bottom margin");

        cleanupToastBar(c);
    }

    /**
     * When position is BOTTOM and the device has no safe area inset,
     * no extra bottom padding should be added by the safe area logic.
     */
    @FormTest
    void testBottomPositionNoPaddingWithoutSafeAreaInset() throws Exception {
        implementation.setDisplaySafeArea(null);

        ToastBar tb = ToastBar.getInstance();
        tb.setPosition(Component.BOTTOM);

        Form f = Display.getInstance().getCurrent();
        f.revalidate();

        Container c = invokeGetToastBarComponent(tb);
        assertNotNull(c, "ToastBarComponent should be created");

        // With full-screen safe area (y=0, height=displayHeight), bottom margin = 0
        // so no extra bottom padding should be applied.
        int paddingBottom = c.getStyle().getPaddingBottom();
        assertTrue(paddingBottom < MAX_DEFAULT_STYLE_PADDING,
                "Bottom padding should not contain safe area compensation, got: " + paddingBottom);

        cleanupToastBar(c);
    }

    /**
     * Verifies that the top padding equals the full safe area Y when the ToastBar's
     * parent starts at absolute Y = 0 (e.g. no toolbar, fullscreen layered pane).
     */
    @FormTest
    void testTopPositionFullPaddingWhenParentAtOrigin() throws Exception {
        int safeTop = 80;
        implementation.setDisplaySafeArea(new Rectangle(0, safeTop, 1080, 1920 - safeTop));

        ToastBar tb = ToastBar.getInstance();
        // Use the form layered pane which overlays the full form from Y=0
        tb.useFormLayeredPane(true);
        tb.setPosition(Component.TOP);

        Form f = Display.getInstance().getCurrent();
        f.revalidate();

        Container c = invokeGetToastBarComponent(tb);
        assertNotNull(c, "ToastBarComponent should be created");

        Container parent = c.getParent();
        assertNotNull(parent, "ToastBarComponent should have a parent");

        // FormLayeredPane starts at absolute Y=0, so full safe area padding is needed
        if (parent.getAbsoluteY() == 0) {
            int paddingTop = c.getStyle().getPaddingTop();
            assertEquals(safeTop, paddingTop,
                    "Top padding should equal safeArea.getY() when parent is at Y=0");
        }

        cleanupToastBar(c);
        tb.useFormLayeredPane(false);
    }
}
