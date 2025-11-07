package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class TextComponentPasswordTest extends UITestBase {

    private void triggerAction(Button action) {
        @SuppressWarnings("unchecked")
        Vector<ActionListener> listeners = action.getActionListeners();
        for (ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(action));
        }
    }

    @FormTest
    void testToggleConstraintAndIcon() {
        TextComponentPassword password = new TextComponentPassword();
        TextField field = password.getField();
        assertEquals(TextArea.PASSWORD, field.getConstraint());
        Button action = password.getAction();
        assertNotNull(action);

        triggerAction(action);
        assertEquals(TextField.NON_PREDICTIVE, field.getConstraint());
        assertEquals(FontImage.MATERIAL_VISIBILITY_OFF, action.getMaterialIcon());

        triggerAction(action);
        assertEquals(TextField.PASSWORD, field.getConstraint());
        assertEquals(FontImage.MATERIAL_VISIBILITY, action.getMaterialIcon());
    }

    @FormTest
    void testChainingMethods() {
        TextComponentPassword password = new TextComponentPassword();
        assertSame(password, password.label("Label"));
        assertSame(password, password.labelAndHint("Label"));
        assertSame(password, password.hint("Hint"));
    }
}
