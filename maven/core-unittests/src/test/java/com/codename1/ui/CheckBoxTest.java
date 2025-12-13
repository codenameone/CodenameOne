package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.plaf.UIManager;
import org.junit.jupiter.api.Test;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class CheckBoxTest extends UITestBase {

    @Test
    void testCreateToggleProducesToggleUiid() {
        CheckBox checkBox = CheckBox.createToggle("value");
        assertTrue(checkBox.isToggle());
        assertEquals("ToggleButton", checkBox.getUIID());
    }

    @FormTest
    void testSelectionChangeNotifiesListenersAndBinding() {
        CheckBox checkBox = new CheckBox("Accept");
        AtomicInteger changes = new AtomicInteger();
        ActionListener listener = evt -> changes.incrementAndGet();
        checkBox.addChangeListener(listener);

        checkBox.setSelected(true);
        checkBox.setSelected(true);
        checkBox.setSelected(false);

        assertEquals(2, changes.get());
        checkBox.removeChangeListeners(listener);

        checkBox.setBoundPropertyValue("selected", Boolean.TRUE);
        assertTrue(checkBox.isSelected());
        assertEquals(Boolean.TRUE, checkBox.getBoundPropertyValue("selected"));
    }

    @FormTest
    void testReleasedTogglesSelection() {
        implementation.setBuiltinSoundsEnabled(false);
        CheckBox checkBox = new CheckBox("Notify");
        assertFalse(checkBox.isSelected());

        checkBox.released(0, 0);
        assertTrue(checkBox.isSelected());

        checkBox.released(0, 0);
        assertFalse(checkBox.isSelected());
    }

    @Test
    void testThemeConstantControlsOppositeSide() {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("@checkBoxOppositeSideBool", "true");
        UIManager.getInstance().setThemeProps(theme);

        CheckBox checkBox = new CheckBox("Align");
        checkBox.refreshTheme(false);

        assertTrue(checkBox.isOppositeSide());
    }
}
