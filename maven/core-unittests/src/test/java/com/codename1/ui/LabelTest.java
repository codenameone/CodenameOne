package com.codename1.ui;

import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LabelTest extends UITestBase {

    @Test
    void testTextPositionValidation() {
        Label label = new Label();
        label.setTextPosition(Label.RIGHT);
        assertThrows(IllegalArgumentException.class, () -> label.setTextPosition(999));
    }

    @Test
    void testBadgeConfigurationCreatesStyleComponent() {
        Label label = new Label();
        assertNull(label.getBadgeStyleComponent());

        label.setBadgeText("9");
        label.setBadgeUIID("CustomBadge");

        assertEquals("9", label.getBadgeText());
        assertNotNull(label.getBadgeStyleComponent());
        assertEquals("CustomBadge", label.getBadgeStyleComponent().getUIID());
    }

    @Test
    void testLocalizationCanBeDisabledPerLabel() {
        Map<String, String> bundle = new HashMap<String, String>();
        bundle.put("key", "Localized");
        UIManager.getInstance().setBundle(bundle);

        Label localized = new Label();
        localized.setText("key");
        assertEquals("Localized", localized.getText());

        Label raw = new Label();
        raw.setShouldLocalize(false);
        raw.setText("key");
        assertEquals("key", raw.getText());
    }

    @Test
    void testMaskGapAndShiftSettingsPersist() {
        Label label = new Label();
        Object mask = new Object();
        label.setMaskName("rounded");
        label.setGap(7);
        label.setEndsWith3Points(false);
        label.setShiftMillimeters(5);
        assertEquals(5, label.getShiftMillimeters());

        label.setShiftMillimeters(2.5f);
        label.setShowEvenIfBlank(true);

        assertEquals("rounded", label.getMaskName());
        assertNull(label.getMask());

        label.setMask(mask);

        assertSame(mask, label.getMask());
        assertEquals("rounded", label.getMaskName());
        assertEquals(7, label.getGap());
        assertFalse(label.isEndsWith3Points());
        assertEquals(3, label.getShiftMillimeters());
        assertEquals(2.5f, label.getShiftMillimetersF(), 0.0001f);
        assertTrue(label.isShowEvenIfBlank());
    }

    // New comprehensive tests

    @Test
    void testDefaultConstructor() {
        Label label = new Label();
        assertNotNull(label);
        assertEquals("", label.getText());
        assertNull(label.getIcon());
    }

    @Test
    void testTextConstructor() {
        Label label = new Label("Hello");
        assertEquals("Hello", label.getText());
        assertNull(label.getIcon());
    }

    @Test
    void testTextAndUIIDConstructor() {
        Label label = new Label("World", "CustomLabel");
        assertEquals("World", label.getText());
        assertEquals("CustomLabel", label.getUIID());
    }

    @Test
    void testIconConstructor() {
        Image icon = Image.createImage(10, 10);
        Label label = new Label(icon);
        assertSame(icon, label.getIcon());
        assertEquals("", label.getText());
    }

    @Test
    void testIconAndUIIDConstructor() {
        Image icon = Image.createImage(10, 10);
        Label label = new Label(icon, "IconLabel");
        assertSame(icon, label.getIcon());
        assertEquals("IconLabel", label.getUIID());
    }

    @Test
    void testTextAndIconConstructor() {
        Image icon = Image.createImage(10, 10);
        Label label = new Label("Text", icon);
        assertEquals("Text", label.getText());
        assertSame(icon, label.getIcon());
    }

    @Test
    void testTextIconAndUIIDConstructor() {
        Image icon = Image.createImage(10, 10);
        Label label = new Label("Text", icon, "FullLabel");
        assertEquals("Text", label.getText());
        assertSame(icon, label.getIcon());
        assertEquals("FullLabel", label.getUIID());
    }

    @Test
    void testSetText() {
        Label label = new Label();
        label.setText("New Text");
        assertEquals("New Text", label.getText());
    }

    @Test
    void testSetTextNull() {
        Label label = new Label("Initial");
        label.setText(null);
        assertEquals("", label.getText());
    }

    @Test
    void testSetIcon() {
        Label label = new Label();
        Image icon = Image.createImage(20, 20);
        label.setIcon(icon);
        assertSame(icon, label.getIcon());
    }

    @Test
    void testSetIconNull() {
        Image icon = Image.createImage(20, 20);
        Label label = new Label(icon);
        label.setIcon(null);
        assertNull(label.getIcon());
    }

    @Test
    void testAlignmentDefault() {
        Label label = new Label();
        // Default alignment varies, just ensure it returns a valid value
        int align = label.getAlignment();
        assertTrue(align == Component.LEFT || align == Component.CENTER || align == Component.RIGHT);
    }

    @Test
    void testSetAlignment() {
        Label label = new Label();
        label.setAlignment(Component.CENTER);
        assertEquals(Component.CENTER, label.getAlignment());

        label.setAlignment(Component.LEFT);
        assertEquals(Component.LEFT, label.getAlignment());

        label.setAlignment(Component.RIGHT);
        assertEquals(Component.RIGHT, label.getAlignment());
    }

    @Test
    void testVerticalAlignmentDefault() {
        Label label = new Label();
        int valign = label.getVerticalAlignment();
        assertTrue(valign == Component.TOP || valign == Component.CENTER || valign == Component.BOTTOM);
    }

    @Test
    void testSetVerticalAlignment() {
        Label label = new Label();
        label.setVerticalAlignment(Component.TOP);
        assertEquals(Component.TOP, label.getVerticalAlignment());

        label.setVerticalAlignment(Component.CENTER);
        assertEquals(Component.CENTER, label.getVerticalAlignment());

        label.setVerticalAlignment(Component.BOTTOM);
        assertEquals(Component.BOTTOM, label.getVerticalAlignment());
    }

    @Test
    void testTextPositionDefault() {
        Label label = new Label();
        int pos = label.getTextPosition();
        assertTrue(pos == Label.LEFT || pos == Label.RIGHT || pos == Label.BOTTOM || pos == Label.TOP);
    }

    @Test
    void testSetTextPosition() {
        Label label = new Label();
        label.setTextPosition(Label.LEFT);
        assertEquals(Label.LEFT, label.getTextPosition());

        label.setTextPosition(Label.RIGHT);
        assertEquals(Label.RIGHT, label.getTextPosition());

        label.setTextPosition(Label.TOP);
        assertEquals(Label.TOP, label.getTextPosition());

        label.setTextPosition(Label.BOTTOM);
        assertEquals(Label.BOTTOM, label.getTextPosition());
    }

    @Test
    void testGapDefault() {
        Label label = new Label();
        assertEquals(Label.getDefaultGap(), label.getGap());
    }

    @Test
    void testSetGap() {
        Label label = new Label();
        label.setGap(10);
        assertEquals(10, label.getGap());

        label.setGap(0);
        assertEquals(0, label.getGap());
    }

    @Test
    void testDefaultGapStatic() {
        int original = Label.getDefaultGap();

        Label.setDefaultGap(15);
        assertEquals(15, Label.getDefaultGap());

        Label.setDefaultGap(5);
        assertEquals(5, Label.getDefaultGap());

        // Restore
        Label.setDefaultGap(original);
    }

    @Test
    void testDefaultTickerEnabled() {
        boolean original = Label.isDefaultTickerEnabled();

        Label.setDefaultTickerEnabled(true);
        assertTrue(Label.isDefaultTickerEnabled());

        Label.setDefaultTickerEnabled(false);
        assertFalse(Label.isDefaultTickerEnabled());

        // Restore
        Label.setDefaultTickerEnabled(original);
    }

    @Test
    void testBadgeTextNull() {
        Label label = new Label();
        label.setBadgeText("Badge");
        assertEquals("Badge", label.getBadgeText());

        label.setBadgeText(null);
        assertNull(label.getBadgeText());
    }

    @Test
    void testBadgeUIID() {
        Label label = new Label();
        label.setBadgeText("5");
        label.setBadgeUIID("NotificationBadge");

        Component badge = label.getBadgeStyleComponent();
        assertNotNull(badge);
        assertEquals("NotificationBadge", badge.getUIID());
    }

    @Test
    void testIconStyleComponent() {
        Label label = new Label();
        Image icon = Image.createImage(10, 10);
        label.setIcon(icon);

        Component iconStyle = label.getIconStyleComponent();
        assertNotNull(iconStyle);
    }

    @Test
    void testMaterialIcon() {
        Label label = new Label();
        label.setMaterialIcon('A');
        assertEquals('A', label.getMaterialIcon());
    }

    @Test
    void testMaterialIconWithSize() {
        Label label = new Label();
        label.setMaterialIcon('B', 5.0f);
        assertEquals('B', label.getMaterialIcon());
        assertEquals(5.0f, label.getMaterialIconSize(), 0.001f);
    }

    @Test
    void testFontIcon() {
        Label label = new Label();
        label.setFontIcon('X');
        assertEquals('X', label.getFontIcon());
    }

    @Test
    void testFontIconWithFont() {
        Label label = new Label();
        Font font = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        label.setFontIcon(font, 'Y');
        assertEquals('Y', label.getFontIcon());
        assertSame(font, label.getIconFont());
    }

    @Test
    void testFontIconWithFontAndSize() {
        Label label = new Label();
        Font font = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        label.setFontIcon(font, 'Z', 3.5f);
        assertEquals('Z', label.getFontIcon());
        assertEquals(3.5f, label.getFontIconSize(), 0.001f);
    }

    @Test
    void testGetIconFont() {
        Label label = new Label();
        Font font = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
        label.setFontIcon(font, 'A');
        assertSame(font, label.getIconFont());
    }

    @Test
    void testShiftText() {
        Label label = new Label();
        label.setShiftText(5);
        assertEquals(5, label.getShiftText());

        label.setShiftText(-3);
        assertEquals(-3, label.getShiftText());
    }

    @Test
    void testAutoSizeDefault() {
        Label label = new Label();
        assertTrue(label.getMaxAutoSize() > 0);
        assertTrue(label.getMinAutoSize() > 0);
    }

    @Test
    void testSetMaxAutoSize() {
        Label label = new Label();
        label.setMaxAutoSize(10.0f);
        assertEquals(10.0f, label.getMaxAutoSize(), 0.001f);
    }

    @Test
    void testSetMinAutoSize() {
        Label label = new Label();
        label.setMinAutoSize(2.0f);
        assertEquals(2.0f, label.getMinAutoSize(), 0.001f);
    }

    @Test
    void testGetBaseline() {
        Label label = new Label("Test");
        int baseline = label.getBaseline(100, 50);
        assertTrue(baseline >= 0);
    }

    @Test
    void testGetBaselineResizeBehavior() {
        Label label = new Label();
        int behavior = label.getBaselineResizeBehavior();
        assertTrue(behavior >= Component.BRB_CONSTANT_ASCENT);
    }

    @Test
    void testSetUIIDUpdatesStyles() {
        Label label = new Label();
        String originalUIID = label.getUIID();
        label.setUIID("CustomLabelStyle");
        assertEquals("CustomLabelStyle", label.getUIID());
    }

    @Test
    void testRefreshTheme() {
        Label label = new Label("Test");
        // Just verify it doesn't throw
        label.refreshTheme(false);
        label.refreshTheme(true);
    }

    @Test
    void testEndsWith3Points() {
        Label label = new Label();
        label.setEndsWith3Points(true);
        assertTrue(label.isEndsWith3Points());

        label.setEndsWith3Points(false);
        assertFalse(label.isEndsWith3Points());
    }

    @Test
    void testShowEvenIfBlank() {
        Label label = new Label();
        label.setShowEvenIfBlank(true);
        assertTrue(label.isShowEvenIfBlank());

        label.setShowEvenIfBlank(false);
        assertFalse(label.isShowEvenIfBlank());
    }

    @Test
    void testTickerEnabled() {
        Label label = new Label();
        label.setTickerEnabled(true);
        assertTrue(label.isTickerEnabled());

        label.setTickerEnabled(false);
        assertFalse(label.isTickerEnabled());
    }

    @Test
    void testShouldLocalize() {
        Label label = new Label();
        label.setShouldLocalize(true);
        assertTrue(label.shouldLocalize());

        label.setShouldLocalize(false);
        assertFalse(label.shouldLocalize());
    }

    @Test
    void testMaskName() {
        Label label = new Label();
        label.setMaskName("circle");
        assertEquals("circle", label.getMaskName());

        label.setMaskName("square");
        assertEquals("square", label.getMaskName());
    }

    @Test
    void testMask() {
        Label label = new Label();
        Object mask1 = new Object();
        Object mask2 = new Object();

        label.setMask(mask1);
        assertSame(mask1, label.getMask());

        label.setMask(mask2);
        assertSame(mask2, label.getMask());
    }

    @Test
    void testShiftMillimeters() {
        Label label = new Label();
        label.setShiftMillimeters(3);
        assertEquals(3, label.getShiftMillimeters());

        label.setShiftMillimeters(7);
        assertEquals(7, label.getShiftMillimeters());
    }

    @Test
    void testShiftMillimetersFloat() {
        Label label = new Label();
        label.setShiftMillimeters(2.5f);
        assertEquals(2.5f, label.getShiftMillimetersF(), 0.0001f);

        label.setShiftMillimeters(4.7f);
        assertEquals(4.7f, label.getShiftMillimetersF(), 0.0001f);
    }

    @Test
    void testTextAndIconTogether() {
        Image icon = Image.createImage(15, 15);
        Label label = new Label("Combined", icon);

        assertEquals("Combined", label.getText());
        assertSame(icon, label.getIcon());

        label.setText("Updated");
        assertEquals("Updated", label.getText());
        assertSame(icon, label.getIcon());

        Image newIcon = Image.createImage(20, 20);
        label.setIcon(newIcon);
        assertEquals("Updated", label.getText());
        assertSame(newIcon, label.getIcon());
    }

    @Test
    void testTextPositionAffectsLayout() {
        Image icon = Image.createImage(10, 10);
        Label label = new Label("Text", icon);

        label.setTextPosition(Label.LEFT);
        label.setTextPosition(Label.RIGHT);
        label.setTextPosition(Label.TOP);
        label.setTextPosition(Label.BOTTOM);

        // Just verify all positions are accepted
        assertEquals(Label.BOTTOM, label.getTextPosition());
    }

    @Test
    void testPreferredSizeCalculation() {
        Label label = new Label("Test Label");
        int prefW = label.getPreferredW();
        int prefH = label.getPreferredH();

        assertTrue(prefW > 0);
        assertTrue(prefH > 0);
    }

    @Test
    void testPreferredSizeWithIcon() {
        Image icon = Image.createImage(30, 30);
        Label label = new Label("Text", icon);

        int prefW = label.getPreferredW();
        int prefH = label.getPreferredH();

        assertTrue(prefW > 0);
        assertTrue(prefH > 0);
    }

    @Test
    void testEmptyLabelSize() {
        Label label = new Label();
        // Empty label should still have some size
        assertTrue(label.getPreferredW() >= 0);
        assertTrue(label.getPreferredH() >= 0);
    }
}
