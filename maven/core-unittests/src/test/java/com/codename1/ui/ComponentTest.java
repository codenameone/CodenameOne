package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextHolder;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
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

    @Test
    void testPointerAnimationAndRenderingUtilities() {
        Form form = new Form();
        Container wrapper = new Container();
        form.add(wrapper);

        CoverageComponent component = new CoverageComponent();
        component.setWidth(40);
        component.setHeight(30);
        component.setX(5);
        component.setY(7);
        component.setDraggable(true);
        component.setTestScrollableX(true);
        component.setTestScrollableY(true);
        component.setScrollOpacityChangeSpeed(120);
        component.setDragTransparency((byte) 200);
        component.setDropTarget(true);
        assertTrue(component.draggingOver(component, 1, 1));
        component.setDragActivated(true);
        component.setTensileDragEnabled(true);
        component.setTensileLength(50);
        component.setSnapToGrid(true);
        component.setFlatten(true);
        component.setPreferredSizeStr("20 25");
        assertEquals("20 25", component.getPreferredSizeStr());

        wrapper.add(component);

        Image canvas = Image.createImage(80, 80, 0x0);
        Graphics g = canvas.getGraphics();

        assertFalse(component.animate());

        Style source = component.getUnselectedStyle();
        Style target = component.getSelectedStyle();
        assertNotNull(component.createStyleAnimation(source, target, 100, component.getUIID()));

        assertNotNull(component.getDragImage());
        assertNotNull(component.toImage());
        assertEquals(Component.DRAG_REGION_LIKELY_DRAG_XY, component.getDragRegionStatus(1, 1));
        assertTrue(component.isDraggable());
        assertTrue(component.isDragActivated());
        assertTrue(component.isTensileDragEnabled());
        assertEquals(50, component.getTensileLength());
        assertTrue(component.isSnapToGrid());
        assertTrue(component.isFlatten());
        assertEquals(120, component.getScrollOpacityChangeSpeed());
        assertEquals(0xff, component.getScrollOpacity());
        assertEquals(200, component.getDragTransparency());
        assertTrue(component.isDropTarget());
        assertNotNull(component.paintLock(false));
        component.paintLockRelease();

        component.paintRippleOverlay(g, component.getX(), component.getY(), 500);
        component.paintRippleOverlay(g, component.getX(), component.getY(), 1000);

        assertEquals(component, component.getScrollable());
        assertTrue(component.respondsToPointerEvents());

        final boolean[] pointerDragTriggered = {false};
        component.addPointerDraggedListener(evt -> pointerDragTriggered[0] = true);

        form.pointerPressed(component.getAbsoluteX() + 1, component.getAbsoluteY() + 1);
        component.pointerDragged(new int[]{component.getAbsoluteX() + 2}, new int[]{component.getAbsoluteY() + 2});
        assertTrue(pointerDragTriggered[0], "Pointer drag listener should trigger");

        component.pointerPressed(new int[]{component.getAbsoluteX()}, new int[]{component.getAbsoluteY()});
        component.pointerReleased(new int[]{component.getAbsoluteX()}, new int[]{component.getAbsoluteY()});

        component.repaint(0, 0, 10, 10);

        component.drawDraggedImage(g);
        component.drawDraggedImage(g, component.getDragImage(), component.getX(), component.getY());

        component.registerElevatedInternal(component);
        component.tryDeregisterAnimated();

        assertTrue(component.isInClippingRegion(g));
    }

    @Test
    void testPreferredSizeParsingAndScrollSizing() {
        Dimension base = new Dimension(10, 20);
        Dimension parsed = Component.parsePreferredSize("15px 25px", base);
        assertSame(base, parsed, "Parser should reuse the provided dimension instance");
        assertEquals(15, parsed.getWidth());
        assertEquals(25, parsed.getHeight());

        Dimension inherit = Component.parsePreferredSize("inherit inherit", new Dimension(7, 9));
        assertEquals(7, inherit.getWidth(), "inherit should leave the width untouched");
        assertEquals(9, inherit.getHeight(), "inherit should leave the height untouched");

        InspectableComponent component = new InspectableComponent();
        component.setPreferredDimension(12, 18);

        Dimension preferred = component.getPreferredSize();
        assertEquals(new Dimension(12, 18), preferred);
        assertEquals(1, component.calcPrefCalls, "Initial preferred size should be calculated once");

        component.getStyle().setMargin(2, 3, 4, 5);
        Dimension withMargin = component.getPreferredSizeWithMargin();
        assertEquals(new Dimension(12 + 4 + 5, 18 + 2 + 3), withMargin, "Preferred size with margin should include style margins");

        component.setPreferredSizeStr("30 40");
        Dimension stringPreferred = component.getPreferredSize();
        assertEquals(new Dimension(30, 40), stringPreferred, "Preferred size string should override calculated size");
        assertEquals("30 40", component.getPreferredSizeStr());

        component.setScrollSize(new Dimension(100, 90));
        Dimension customScroll = component.getScrollDimension();
        assertEquals(new Dimension(100, 90), customScroll, "Custom scroll size should be returned as-is");

        component.setScrollSize(null);
        component.setPreferredDimension(50, 60);
        component.setShouldCalcPreferredSize(true);
        Dimension recalculatedScroll = component.getScrollDimension();
        assertEquals(new Dimension(50, 60), recalculatedScroll, "Resetting scroll size should defer to calculated preferred size");
        assertTrue(component.calcScrollCalls > 0, "Scroll dimension recalculation should invoke calcScrollSize");
    }

    @Test
    void testInlineStylesAndGrouping() {
        InspectableComponent first = new InspectableComponent();
        InspectableComponent second = new InspectableComponent();
        first.setPreferredDimension(10, 15);
        second.setPreferredDimension(12, 22);

        Component.setSameHeight(first, second);
        assertNotNull(first.getSameHeight());
        assertNotNull(second.getSameHeight());

        int sharedHeight = first.getPreferredSize().getHeight();
        assertEquals(sharedHeight, second.getPreferredSize().getHeight(), "Grouped components should share the largest preferred height");
        assertEquals(22, sharedHeight, "Preferred height should match the tallest component");

        Component.setSameHeight(first);
        assertNull(first.getSameHeight(), "Removing a group should clear references");

        InspectableComponent inline = new InspectableComponent();
        inline.setInlineAllStyles("fgColor:ff0000");
        inline.setInlineSelectedStyles("font:2mm");
        inline.setInlineUnselectedStyles("bgColor:00ff00");
        inline.setInlineDisabledStyles("opacity:0.5");
        inline.setInlinePressedStyles("border:1px");

        assertEquals("fgColor:ff0000", inline.getInlineAllStyles());
        assertEquals("font:2mm", inline.getInlineSelectedStyles());
        assertEquals("bgColor:00ff00", inline.getInlineUnselectedStyles());
        assertEquals("opacity:0.5", inline.getInlineDisabledStyles());
        assertEquals("border:1px", inline.getInlinePressedStyles());

        inline.setInlineAllStyles("   ");
        inline.setInlineSelectedStyles("   ");
        inline.setInlineUnselectedStyles("   ");
        inline.setInlineDisabledStyles("   ");
        inline.setInlinePressedStyles("   ");

        assertNull(inline.getInlineAllStyles(), "Whitespace should clear inline styles");
        assertNull(inline.getInlineSelectedStyles());
        assertNull(inline.getInlineUnselectedStyles());
        assertNull(inline.getInlineDisabledStyles());
        assertNull(inline.getInlinePressedStyles());
    }

    @Test
    void testGeometryOwnershipAndVisibilityHelpers() {
        InspectableContainer root = new InspectableContainer();
        root.setX(50);
        root.setY(60);
        root.setWidth(200);
        root.setHeight(150);
        root.forceInitialized(true);
        root.setVisible(true);

        InspectableComponent child = new InspectableComponent();
        child.setX(10);
        child.setY(15);
        child.setWidth(40);
        child.setHeight(30);
        child.getStyle().setMargin(2, 3, 4, 5);
        child.getStyle().setPadding(6, 7, 8, 9);
        root.addComponent(child);
        child.forceInitialized(true);
        child.setVisible(true);

        Rectangle bounds = child.getBounds(new Rectangle());
        assertEquals(new Rectangle(10, 15, 40, 30), bounds);

        Rectangle visibleBounds = child.getVisibleBounds(new Rectangle());
        assertEquals(bounds, visibleBounds);

        Rectangle visibleRect = new Rectangle();
        child.getVisibleRect(visibleRect, true);
        assertEquals(child.getAbsoluteX(), visibleRect.getX());
        assertEquals(child.getAbsoluteY(), visibleRect.getY());
        assertEquals(child.getWidth(), visibleRect.getWidth());
        assertEquals(child.getHeight(), visibleRect.getHeight());

        int innerX = child.getInnerX();
        int outerX = child.getOuterX();
        int innerY = child.getInnerY();
        int outerY = child.getOuterY();
        assertEquals(child.getX() + child.getStyle().getMarginLeftNoRTL(), innerX);
        assertEquals(child.getX() - 4, outerX);
        assertEquals(child.getY() + child.getStyle().getPaddingTop(), innerY);
        assertEquals(child.getY() - 2, outerY);

        assertEquals(child.getWidth() + child.getStyle().getHorizontalMargins(), child.getOuterWidth());
        assertEquals(child.getWidth() - child.getStyle().getHorizontalPadding(), child.getInnerWidth());

        int relativeX = child.getRelativeX(root);
        int relativeY = child.getRelativeY(root);
        assertEquals(child.getX(), relativeX);
        assertEquals(child.getY(), relativeY);

        int insideX = child.getAbsoluteX() + 1;
        int insideY = child.getAbsoluteY() + 1;
        assertTrue(child.contains(insideX, insideY));
        assertTrue(child.visibleBoundsContains(insideX, insideY));
        assertTrue(child.containsOrOwns(insideX, insideY));

        Component owner = new Component();
        child.setOwner(owner);
        assertTrue(child.isOwnedBy(owner));

        Component outside = new Component();
        assertFalse(child.visibleBoundsContains(outside.getAbsoluteX(), outside.getAbsoluteY()));
    }

    @Test
    void testFocusTraversalAndDragFlags() {
        InspectableComponent component = new InspectableComponent();
        InspectableComponent next = new InspectableComponent();
        InspectableComponent prev = new InspectableComponent();
        InspectableComponent left = new InspectableComponent();
        InspectableComponent right = new InspectableComponent();

        component.setFocusable(true);
        component.setVisible(true);
        component.setEnabled(true);

        component.setPreferredTabIndex(-1);
        component.setTraversable(true);
        assertTrue(component.isTraversable(), "Enabling traversable should set a non-negative preferred tab index");
        assertEquals(0, component.getPreferredTabIndex());

        component.setTraversable(false);
        assertFalse(component.isTraversable());
        assertEquals(-1, component.getPreferredTabIndex());

        component.setTabIndex(5);
        assertEquals(5, component.getTabIndex());

        component.setNextFocusDown(next);
        component.setNextFocusUp(prev);
        component.setNextFocusLeft(left);
        component.setNextFocusRight(right);
        assertSame(next, component.getNextFocusDown());
        assertSame(prev, component.getNextFocusUp());
        assertSame(left, component.getNextFocusLeft());
        assertSame(right, component.getNextFocusRight());

        component.setBlockLead(true);
        assertTrue(component.isBlockLead());

        component.setDragTransparency((byte) 120);
        assertEquals(120, component.getDragTransparency());

        component.setDragActivated(true);
        assertTrue(component.isDragActivatedPublic());

        component.setScrollAnimationSpeed(750);
        assertEquals(750, component.getScrollAnimationSpeed());
    }

    @Test
    void testExtendedPropertyAndListenerCoverage() {
        InspectableComponent component = new InspectableComponent();
        component.setWidth(40);
        component.setHeight(30);
        component.forceInitialized(true);
        component.setVisible(true);

        Style pressed = new Style();
        Style selected = new Style();
        Style disabled = new Style();
        component.setPressedStyle(pressed);
        component.setSelectedStyle(selected);
        component.setDisabledStyle(disabled);
        assertSame(pressed, component.getPressedStyle());
        assertSame(selected, component.getSelectedStyle());
        assertSame(disabled, component.getDisabledStyle());

        component.setCloudBoundProperty("bound");
        component.setCloudDestinationProperty("dest");
        assertEquals("bound", component.getCloudBoundProperty());
        assertEquals("dest", component.getCloudDestinationProperty());

        component.setTooltip("tip");
        assertEquals("tip", component.getTooltip());

        component.setSmoothScrolling(true);
        assertTrue(component.isSmoothScrolling());

        Component.setDisableSmoothScrolling(true);
        try {
            assertFalse(component.isSmoothScrolling(), "Global disable flag should override component smooth scrolling state");
        } finally {
            Component.setDisableSmoothScrolling(false);
        }

        component.setDropTarget(true);
        assertTrue(component.isDropTarget());

        component.setHideInLandscape(true);
        component.setHideInPortrait(true);
        assertTrue(component.isHideInLandscape());
        assertTrue(component.isHideInPortrait());

        component.setPinchBlocksDragAndDrop(true);
        assertTrue(component.isPinchBlocksDragAndDrop());

        component.setSnapToGrid(true);
        component.setFlatten(true);
        component.setTensileDragEnabled(true);
        component.setTensileLength(75);
        assertTrue(component.isSnapToGrid());
        assertTrue(component.isFlatten());
        assertTrue(component.isTensileDragEnabled());
        assertEquals(75, component.getTensileLength());

        component.setScrollOpacityChangeSpeed(30);
        assertEquals(30, component.getScrollOpacityChangeSpeed());

        component.setScrollAnimationSpeed(5);
        assertEquals(5, component.getScrollAnimationSpeed());

        Component.setDefaultDragTransparency((byte) 123);
        component.setDragTransparency((byte) 110);
        assertEquals(123, Component.getDefaultDragTransparency());
        assertEquals(110, component.getDragTransparency());

        component.setDragActivated(true);
        assertTrue(component.isDragActivatedPublic());

        component.setBlockLead(true);
        assertTrue(component.isBlockLead());

        component.setTraversable(true);
        assertTrue(component.isTraversable());

        component.setTabIndex(4);
        assertEquals(4, component.getTabIndex());

        component.setNextFocusUp(component);
        component.setNextFocusDown(component);
        component.setNextFocusLeft(component);
        component.setNextFocusRight(component);
        assertSame(component, component.getNextFocusUp());
        assertSame(component, component.getNextFocusDown());
        assertSame(component, component.getNextFocusLeft());
        assertSame(component, component.getNextFocusRight());

        component.setOwner(component);
        assertSame(component, component.getOwner());

        component.setUIID("Test", "Test");
        assertEquals("Test", component.getUIID());

        component.setCloudBoundProperty(null);
        component.setCloudDestinationProperty(null);
        assertNull(component.getCloudBoundProperty());
        assertNull(component.getCloudDestinationProperty());
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

    private static class InspectableComponent extends Component {
        private Dimension preferred = new Dimension(10, 10);
        int calcPrefCalls;
        int calcScrollCalls;

        void setPreferredDimension(int width, int height) {
            preferred = new Dimension(width, height);
            setShouldCalcPreferredSize(true);
        }

        void forceInitialized(boolean value) {
            setInitialized(value);
        }

        boolean isDragActivatedPublic() {
            return isDragActivated();
        }

        @Override
        protected Dimension calcPreferredSize() {
            calcPrefCalls++;
            return new Dimension(preferred);
        }

        @Override
        protected Dimension calcScrollSize() {
            calcScrollCalls++;
            return new Dimension(preferred);
        }
    }

    private static class InspectableContainer extends Container {
        void forceInitialized(boolean value) {
            setInitialized(value);
        }
    }

    private static class CoverageComponent extends Component {
        private boolean scrollableX;
        private boolean scrollableY;

        CoverageComponent() {
            setUIID("Label");
        }

        void setTestScrollableX(boolean value) {
            scrollableX = value;
        }

        void setTestScrollableY(boolean value) {
            scrollableY = value;
        }

        @Override
        public boolean isScrollableX() {
            return scrollableX;
        }

        @Override
        public boolean isScrollableY() {
            return scrollableY;
        }

        @Override
        protected void paintBackground(Graphics g) {
            g.setColor(0xff00ff);
            g.fillRect(getX(), getY(), getWidth(), getHeight());
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(0);
            g.drawRect(getX(), getY(), getWidth(), getHeight());
        }
    }
}
