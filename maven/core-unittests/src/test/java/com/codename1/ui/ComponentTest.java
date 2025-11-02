package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextHolder;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentTest extends UITestBase {

    @Test
    void testPutClientPropertyStoresAndRemovesValues() {
        Component component = new Component();

        assertNull(component.getClientProperty("missing"));

        component.putClientProperty("key", "value");
        assertEquals("value", component.getClientProperty("key"));

        component.putClientProperty("key", null);
        assertNull(component.getClientProperty("key"));

        component.putClientProperty("another", Integer.valueOf(42));
        assertEquals(Integer.valueOf(42), component.getClientProperty("another"));

        component.clearClientProperties();
        assertNull(component.getClientProperty("another"));
    }

    @Test
    void testStripMarginAndPaddingResetsStyleAndBorder() {
        Component component = new Component();
        Style style = component.getAllStyles();

        style.setMargin(3, 4, 5, 6);
        style.setPadding(7, 8, 9, 10);
        style.setBorder(Border.createLineBorder(2));

        Component chained = component.stripMarginAndPadding();
        assertSame(component, chained);

        Style[] states = new Style[]{
                component.getUnselectedStyle(),
                component.getSelectedStyle(),
                component.getPressedStyle(),
                component.getDisabledStyle()
        };

        for (Style state : states) {
            assertEquals(0f, state.getMarginFloatValue(false, Component.TOP));
            assertEquals(0f, state.getMarginFloatValue(false, Component.BOTTOM));
            assertEquals(0f, state.getMarginFloatValue(false, Component.LEFT));
            assertEquals(0f, state.getMarginFloatValue(false, Component.RIGHT));

            assertEquals(0f, state.getPaddingFloatValue(false, Component.TOP));
            assertEquals(0f, state.getPaddingFloatValue(false, Component.BOTTOM));
            assertEquals(0f, state.getPaddingFloatValue(false, Component.LEFT));
            assertEquals(0f, state.getPaddingFloatValue(false, Component.RIGHT));

            assertTrue(state.getBorder().isEmptyBorder());
        }
    }

    @Test
    void testSetEnabledTriggersRepaintOnlyWhenStateChanges() {
        CountingComponent component = new CountingComponent();

        component.setEnabled(false);
        assertEquals(1, component.repaintCalls, "Disabling should trigger a repaint");

        component.setEnabled(false);
        assertEquals(1, component.repaintCalls, "Setting the same state should not repaint");

        component.setEnabled(true);
        assertEquals(2, component.repaintCalls, "Re-enabling should repaint exactly once");
    }

    @Test
    void testGetStyleReturnsStateSpecificStyles() {
        Display.getInstance().setPureTouch(false);
        StyleLifecycleComponent component = new StyleLifecycleComponent();

        Style unselected = component.getStyle();
        assertSame(component.getUnselectedStyle(), unselected, "Default style should be unselected");

        component.setFocus(true);
        Style selected = component.getStyle();
        assertSame(component.getSelectedStyle(), selected, "Focused components should use the selected style");

        component.setEnabled(false);
        Style disabled = component.getStyle();
        assertSame(component.getDisabledStyle(), disabled, "Disabled components should use the disabled style");
    }

    @Test
    void testSelectedAndDisabledStylesInitializeOnlyOnceAndInstallPainters() {
        StyleLifecycleComponent component = new StyleLifecycleComponent();

        Style selected = component.getSelectedStyle();
        Style disabled = component.getDisabledStyle();

        assertNotNull(selected.getBgPainter(), "Selected style should install a default painter");
        assertNotNull(disabled.getBgPainter(), "Disabled style should install a default painter");

        assertEquals(1, component.selectedInitCount, "Selected style should be initialized exactly once");
        assertEquals(1, component.disabledInitCount, "Disabled style should be initialized exactly once");

        // Subsequent calls should return cached instances without reinitializing.
        component.getSelectedStyle();
        component.getDisabledStyle();

        assertEquals(1, component.selectedInitCount);
        assertEquals(1, component.disabledInitCount);
    }

    @Test
    void testStyleChangeRecalculatesPreferredSize() {
        PreferredSizeComponent component = new PreferredSizeComponent(11, 17);
        component.getUnselectedStyle();

        Dimension first = component.getPreferredSize();
        assertEquals(new Dimension(11, 17), first);
        assertEquals(1, component.calcCalls, "Initial preferred size calculation should run once");
        assertFalse(component.isPreferredSizeDirty(), "Preferred size cache should be clean after calculation");

        // Cached preferred size should not trigger an additional calculation.
        component.getPreferredSize();
        assertEquals(1, component.calcCalls, "Preferred size should be cached until a style change occurs");

        component.styleChanged(Style.MARGIN, component.getStyle());
        assertEquals(Style.MARGIN, component.lastStyleChangeProperty, "Style change callback should capture the property name");
        assertTrue(component.isPreferredSizeDirty(), "Style change should flag preferred size as dirty");

        component.getPreferredSize();
        assertEquals(2, component.calcCalls, "Style changes that affect layout should invalidate preferred size");
        assertFalse(component.isPreferredSizeDirty(), "Preferred size recalculation should clear the dirty flag");
    }

    @Test
    void testSetSameWidthSynchronizesPreferredSizes() {
        PreferredSizeComponent small = new PreferredSizeComponent(5, 10);
        PreferredSizeComponent medium = new PreferredSizeComponent(15, 8);
        PreferredSizeComponent large = new PreferredSizeComponent(20, 6);

        Component.setSameWidth(small, medium, large);

        Dimension size = small.getPreferredSize();
        assertEquals(20, size.getWidth(), "Grouped components should share the largest preferred width");

        assertEquals(20, medium.getPreferredSize().getWidth());
        assertEquals(20, large.getPreferredSize().getWidth());

        // Removing a component from the group should reset its same width reference.
        Component.setSameWidth(medium);
        assertNull(medium.getSameWidth(), "Removing a single component from the group should clear its sameWidth array");
    }

    @Test
    void testMergeStyleRespectsModifiedStyles() {
        StyleLifecycleComponent component = new StyleLifecycleComponent();
        Style modified = new Style();
        modified.setFgColor(0x123456);
        Style replacement = new Style();
        replacement.setFgColor(0x654321);

        Style result = component.mergeForTest(modified, replacement);
        assertSame(modified, result, "Modified styles should merge into themselves");
        assertEquals(0x123456, result.getFgColor(), "Explicit modifications should take precedence over merged values");

        Style pristine = new Style();
        Style pristineReplacement = new Style();
        pristineReplacement.setBgColor(0xabcdef);

        Style pristineResult = component.mergeForTest(pristine, pristineReplacement);
        assertSame(pristineReplacement, pristineResult, "Unmodified styles should be replaced entirely");
        assertEquals(0xabcdef, pristineResult.getBgColor());
    }

    @Test
    void testSetParentRejectsSelfReference() {
        Container container = new Container();
        assertThrows(IllegalArgumentException.class, () -> container.setParent(container));
    }

    @Test
    void testAccessibilityTextFallbackOrder() {
        AccessibilityComponent component = new AccessibilityComponent();

        assertNull(component.getAccessibilityText(), "Components without accessibility text should return null");

        component.setText("Holder text");
        assertEquals("Holder text", component.getAccessibilityText(), "TextHolder content should be used when explicit text is missing");

        component.setText(null);
        Label label = new Label("Label text");
        component.setLabelForComponent(label);
        assertEquals("Label text", component.getAccessibilityText(), "Associated label text should provide accessibility description");

        component.setAccessibilityText("Explicit");
        assertEquals("Explicit", component.getAccessibilityText(), "Explicit accessibility text should take precedence");
    }

    @Test
    void testDeinitializeImplClearsInitializationState() {
        StyleLifecycleComponent component = new StyleLifecycleComponent();
        Form form = new Form();
        form.add(component);

        component.initComponentImpl();
        assertTrue(component.wasInitComponentCalled);
        assertTrue(component.isInitializedPublic());

        // Emulate background image usage to ensure deinitialize unlocks resources.
        component.getStyle().setBgImage(Image.createImage(1, 1, 0xff0000));
        component.getStyle().getBgImage().lock();
        component.deinitializeImpl();

        assertTrue(component.wasDeinitializeCalled);
        assertFalse(component.isInitializedPublic());
        assertTrue(component.hideNativeOverlayCalled);
    }

    private static class CountingComponent extends Component {
        int repaintCalls;

        @Override
        public void repaint() {
            repaintCalls++;
        }

        @Override
        public void repaint(int x, int y, int w, int h) {
            repaintCalls++;
        }

        @Override
        public Dimension getScrollDimension() {
            return new Dimension(0, 0);
        }
    }

    private static class PreferredSizeComponent extends Component {
        private final Dimension preferred;
        int calcCalls;
        String lastStyleChangeProperty;

        PreferredSizeComponent(int width, int height) {
            preferred = new Dimension(width, height);
        }

        @Override
        protected Dimension calcPreferredSize() {
            calcCalls++;
            return new Dimension(preferred);
        }

        boolean isPreferredSizeDirty() {
            return shouldCalcPreferredSize;
        }

        @Override
        public void styleChanged(String propertyName, Style source) {
            super.styleChanged(propertyName, source);
            lastStyleChangeProperty = propertyName;
        }
    }

    private static class StyleLifecycleComponent extends Component {
        int selectedInitCount;
        int disabledInitCount;
        boolean wasInitComponentCalled;
        boolean wasDeinitializeCalled;
        boolean hideNativeOverlayCalled;

        @Override
        protected void initSelectedStyle(Style selectedStyle) {
            selectedInitCount++;
        }

        @Override
        protected void initDisabledStyle(Style disabledStyle) {
            disabledInitCount++;
        }

        @Override
        protected void initComponent() {
            wasInitComponentCalled = true;
        }

        @Override
        protected void deinitialize() {
            wasDeinitializeCalled = true;
        }

        @Override
        protected void hideNativeOverlay() {
            hideNativeOverlayCalled = true;
        }

        @Override
        protected void showNativeOverlay() {
            // Avoid calling into the implementation. The tests only care that the lifecycle hooks run.
        }

        boolean isInitializedPublic() {
            return isInitialized();
        }

        Style mergeForTest(Style toMerge, Style newStyle) {
            return mergeStyle(toMerge, newStyle);
        }
    }

    private static class AccessibilityComponent extends Component implements TextHolder {
        private String text;

        @Override
        public String getText() {
            return text;
        }

        @Override
        public void setText(String text) {
            this.text = text;
        }
    }
}
