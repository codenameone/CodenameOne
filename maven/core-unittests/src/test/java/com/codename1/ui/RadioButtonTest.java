package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RadioButtonTest extends UITestBase {

    @Test
    void testCreateToggleAddsToGroupAndSetsToggleUiid() {
        ButtonGroup group = new ButtonGroup();
        RadioButton radio = RadioButton.createToggle("Choice", group);

        assertEquals(1, group.getButtonCount());
        assertTrue(radio.isToggle());
        assertEquals("ToggleButton", radio.getUIID());
    }

    @Test
    void testSelectionChangeNotifiesListeners() {
        RadioButton radio = new RadioButton("Pick me");
        AtomicInteger changes = new AtomicInteger();
        ActionListener listener = evt -> changes.incrementAndGet();
        radio.addChangeListener(listener);

        radio.setSelected(true);
        radio.setSelected(true);
        radio.setSelected(false);

        assertEquals(2, changes.get());
        radio.removeChangeListeners(listener);
        radio.setSelected(true);
        assertEquals(2, changes.get());
    }

    @Test
    void testReleasedHonorsUnselectAllowedFlag() {
        when(implementation.isBuiltinSoundsEnabled()).thenReturn(false);
        RadioButton radio = new RadioButton("Option");
        radio.setSelected(true);
        radio.setUnselectAllowed(false);

        radio.released(0, 0);
        assertTrue(radio.isSelected());

        radio.setUnselectAllowed(true);
        radio.released(0, 0);
        assertFalse(radio.isSelected());
    }

    @Test
    void testThemeConstantControlsOppositeSide() {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@radioOppositeSideBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        RadioButton radio = new RadioButton("Side");
        radio.refreshTheme(false);

        assertTrue(radio.isOppositeSide());
    }
}
