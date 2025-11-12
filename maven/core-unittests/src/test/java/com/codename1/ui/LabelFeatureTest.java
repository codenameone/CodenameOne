package com.codename1.ui;

import com.codename1.cloud.BindTarget;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.TextSelection.Span;
import com.codename1.ui.TextSelection.TextSelectionSupport;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.Arrays;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class LabelFeatureTest extends UITestBase {

    @FormTest
    void testLegacyRendererModeRespected() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Style labelStyle = UIManager.getInstance().getComponentStyle("Label");
        Image legacyIcon = FontImage.createMaterial(FontImage.MATERIAL_HOME, labelStyle);
        Image standardIcon = Image.createImage(12, 12);

        Label label = new Label("Legacy", legacyIcon);
        form.add(label);
        form.revalidate();

        assertTrue(label.isLegacyRenderer(), "FontImage based icon should trigger legacy renderer mode");

        label.setLegacyRenderer(false);
        assertFalse(label.isLegacyRenderer());

        label.setIcon(standardIcon);
        assertFalse(label.isLegacyRenderer(), "Mutable RGB image should not force legacy renderer");

        label.setIcon(legacyIcon);
        assertTrue(label.isLegacyRenderer(), "Switching back to a FontImage should restore legacy renderer");
    }

    @FormTest
    void testAutoSizeModeAdjustsFontWithinBounds() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Display display = Display.getInstance();
        Label label = new Label("Autosize verification text that should stretch");

        Font baseFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
        Font oversizedFont = baseFont.derive(display.convertToPixels(12f), baseFont.getStyle());
        label.getAllStyles().setFont(oversizedFont);

        int preferredWidth = display.convertToPixels(20f);
        label.setPreferredSize(new Dimension(preferredWidth, display.convertToPixels(8f)));
        label.setAutoSizeMode(true);
        label.setMinAutoSize(2f);
        label.setMaxAutoSize(30f);
        label.getAllStyles().setPadding(0, 0, 0, 0);

        float initialSize = label.getUnselectedStyle().getFont().getPixelSize();

        form.add(label);
        form.revalidate();

        Font resizedFont = label.getUnselectedStyle().getFont();
        float resizedSize = resizedFont.getPixelSize();

        assertTrue(resizedSize >= display.convertToPixels(label.getMinAutoSize()), "Autosize should respect minimum size");
        assertTrue(resizedSize <= display.convertToPixels(label.getMaxAutoSize()), "Autosize should respect maximum size");
        assertTrue(resizedSize < initialSize, "Font size should shrink to satisfy autosize constraints");

        int availableWidth = label.getWidth();
        assertTrue(resizedFont.stringWidth(label.getText()) <= availableWidth, "Resized font must fit component width");
        assertTrue(label.isAutoSizeMode());
    }

    @FormTest
    void testTextSelectionLifecycleAndSpanExtraction() {
        TestCodenameOneImplementation impl = implementation;
        impl.resetTextSelectionTracking();

        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Label label = new Label("Codename One text selection test");
        label.setTextSelectionEnabled(true);
        form.add(label);
        form.revalidate();

        TextSelection selection = form.getTextSelection();
        selection.setEnabled(true);

        assertEquals(1, impl.getInitializeTextSelectionCount(), "Initialization should be delegated to implementation");
        assertEquals(Component.TEXT_CURSOR, label.getCursor());

        Component selectionRoot = TextSelection.findSelectionRoot(label);
        int relativeX = label.getAbsoluteX() - selectionRoot.getAbsoluteX() + label.getWidth() / 2;
        int relativeY = label.getAbsoluteY() - selectionRoot.getAbsoluteY() + label.getHeight() / 2;

        TextSelectionSupport support = label.getTextSelectionSupport();
        Span span = support.triggerSelectionAt(selection, relativeX, relativeY);
        assertNotNull(span, "Triggering selection should return a span");

        String selected = support.getTextForSpan(selection, span).trim();
        assertFalse(selected.isEmpty());

        selection.setEnabled(false);
        assertEquals(1, impl.getDeinitializeTextSelectionCount(), "Disabling selection should deinitialize implementation");
    }

    @FormTest
    void testIconUiidIsAppliedFromTheme() {
        Image icon = Image.createImage(10, 10);
        Object mask = icon.createMask();
        Hashtable theme = new Hashtable();
        theme.put("Label.derive", "Label");
        theme.put("LabelIcon.derive", "Label");
        theme.put("@customMask", mask);
        UIManager.getInstance().setThemeProps(theme);

        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Label label = new Label("Icon UIID");
        label.setIcon(icon);
        form.add(label);
        form.revalidate();

        Component iconStyle = label.getIconStyleComponent();
        assertNotNull(iconStyle);
        assertEquals("LabelIcon", iconStyle.getUIID());
    }

    @FormTest
    void testPropertyBindingForText() {
        Label label = new Label("Initial");

        assertArrayEquals(new String[]{"text"}, label.getBindablePropertyNames());
        assertTrue(Arrays.asList(label.getBindablePropertyTypes()).contains(String.class));

        BindTarget target = new BindTarget() {
            @Override
            public void propertyChanged(Component source, String propertyName, Object oldValue, Object newValue) {
                // Property binding notifications for labels are deprecated, but listeners should be accepted.
            }
        };

        label.bindProperty("text", target);
        label.setText("Updated");
        assertEquals("Updated", label.getBoundPropertyValue("text"));
        label.unbindProperty("text", target);

        label.setBoundPropertyValue("text", "Bound");
        assertEquals("Bound", label.getText());
        assertEquals("Bound", label.getBoundPropertyValue("text"));
    }

    @FormTest
    void testIconMaskingCachesMaskedImage() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Image icon = Image.createImage(8, 8);
        Object mask = icon.createMask();

        Label label = new Label("Masked", icon);
        label.setMask(mask);
        form.add(label);
        form.revalidate();

        Image masked = label.getMaskedIcon();
        assertNotNull(masked);
        assertNotSame(icon, masked);
        assertSame(masked, label.getMaskedIcon(), "Masked icon should be cached");

        label.setMaskName("custom");
        assertEquals("custom", label.getMaskName());
    }

    @FormTest
    void testTickerLifecycleAndShiftUpdates() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Label label = new Label("Ticker requires narrow width");
        label.setPreferredSize(new Dimension(60, 40));
        form.add(label);
        form.revalidate();

        assertTrue(label.shouldTickerStart(), "Ticker should start when text is wider than available");

        label.startTicker(5, true);
        assertTrue(label.isTickerRunning());

        label.stopTicker();
        assertFalse(label.isTickerRunning());

        label.setTickerEnabled(false);
        label.startTicker(5, true);
        assertFalse(label.isTickerRunning(), "Disabled ticker should not start");
    }

    @FormTest
    void testInvalidVerticalAlignmentThrows() {
        Label label = new Label();
        assertThrows(IllegalArgumentException.class, () -> label.setVerticalAlignment(999));
    }

    @FormTest
    void testBaselineAlignmentCalculations() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Label label = new Label("Baseline");
        label.getAllStyles().setPadding(4, 6, 2, 2);
        label.setPreferredSize(new Dimension(120, 60));
        label.setVerticalAlignment(Component.BASELINE);
        form.add(label);
        form.revalidate();

        int width = label.getWidth();
        int height = label.getHeight();
        Style style = label.getStyle();
        Font font = style.getFont();
        int expected = style.getPaddingTop() + (height - style.getVerticalPadding() - font.getHeight()) / 2 + font.getAscent();
        assertEquals(Component.BASELINE, label.getVerticalAlignment());
        assertEquals(expected, label.getBaseline(width, height));
    }

    @FormTest
    void testSetFontIconAssignsFontImage() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();

        Label label = new Label();
        Font font = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        label.setFontIcon(font, 'A', 6f);

        assertEquals('A', label.getFontIcon());
        assertEquals(6f, label.getFontIconSize(), 0.001f);
        assertSame(font, label.getIconFont());
        assertNotNull(label.getIcon());
        assertTrue(label.getIcon() instanceof FontImage);
    }
}

