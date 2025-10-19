package com.codename1.components;

import com.codename1.ui.Button;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class FloatingHintTest extends ComponentTestBase {

    @Test
    void constructorConfiguresHintComponents() throws Exception {
        TrackingTextArea textArea = new TrackingTextArea();
        textArea.setHint("Name");
        FloatingHint hint = new FloatingHint(textArea);

        assertEquals("", textArea.getHint(), "Constructor should clear original hint");
        Button hintButton = getPrivateField(hint, "hintButton", Button.class);
        Label hintLabel = getPrivateField(hint, "hintLabel", Label.class);
        assertEquals("FloatingHint", hintButton.getUIID());
        assertEquals("TextHint", hintLabel.getUIID());
        assertFalse(hintButton.isVisible());
        assertTrue(hintLabel.isVisible());
    }

    @Test
    void constructorWithExistingTextShowsHintButton() throws Exception {
        TrackingTextArea textArea = new TrackingTextArea();
        textArea.setText("Existing");
        textArea.setHint("Value");
        FloatingHint hint = new FloatingHint(textArea);
        Button hintButton = getPrivateField(hint, "hintButton", Button.class);
        Label hintLabel = getPrivateField(hint, "hintLabel", Label.class);
        assertTrue(hintButton.isVisible());
        assertFalse(hintLabel.isVisible());
    }

    @Test
    void hintButtonStartsEditingAsync() throws Exception {
        TrackingTextArea textArea = new TrackingTextArea();
        textArea.setHint("Email");
        FloatingHint hint = new FloatingHint(textArea);
        Button hintButton = getPrivateField(hint, "hintButton", Button.class);
        java.util.Vector listeners = hintButton.getListeners();
        for (Object listener : listeners) {
            ((ActionListener) listener).actionPerformed(new ActionEvent(hintButton));
        }
        assertTrue(textArea.editingStarted);
    }

    @Test
    void focusGainedWithoutInitializationShowsButton() throws Exception {
        TrackingTextArea textArea = new TrackingTextArea();
        textArea.setHint("Phone");
        FloatingHint hint = new FloatingHint(textArea);
        Method focusGained = FloatingHint.class.getDeclaredMethod("focusGainedImpl");
        focusGained.setAccessible(true);
        focusGained.invoke(hint);
        Button hintButton = getPrivateField(hint, "hintButton", Button.class);
        Label hintLabel = getPrivateField(hint, "hintLabel", Label.class);
        assertTrue(hintButton.isVisible());
        assertFalse(hintLabel.isVisible());
    }

    @Test
    void focusLostWithoutInitializationHidesButtonWhenEmpty() throws Exception {
        TrackingTextArea textArea = new TrackingTextArea();
        textArea.setHint("Code");
        FloatingHint hint = new FloatingHint(textArea);
        Method focusLost = FloatingHint.class.getDeclaredMethod("focusLostImpl");
        focusLost.setAccessible(true);
        focusLost.invoke(hint);
        Button hintButton = getPrivateField(hint, "hintButton", Button.class);
        Label hintLabel = getPrivateField(hint, "hintLabel", Label.class);
        assertFalse(hintButton.isVisible());
        assertTrue(hintLabel.isVisible());
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static class TrackingTextArea extends TextArea {
        boolean editingStarted;

        @Override
        public void startEditingAsync() {
            editingStarted = true;
        }
    }
}
