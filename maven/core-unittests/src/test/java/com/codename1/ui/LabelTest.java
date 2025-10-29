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
}
