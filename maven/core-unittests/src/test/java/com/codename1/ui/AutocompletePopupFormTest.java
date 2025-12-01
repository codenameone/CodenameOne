package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class AutocompletePopupFormTest extends UITestBase {

    @FormTest
    void popupOpensFromTriggerButtonAndSelectsEntry() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = new Form("Hi World", BoxLayout.y());

        AutoCompleteTextField field = new AutoCompleteTextField("Red", "Green", "Blue", "Orange", "Yellow");
        field.setMinimumElementsShownInPopup(5);

        Button open = new Button();
        Container content = new Container(new BorderLayout());
        content.add(BorderLayout.CENTER, field);
        content.add(BorderLayout.EAST, open);
        open.addActionListener(evt -> field.showPopup());

        form.add(content);
        form.show();
        flushSerialCalls();

        Container layered = form.getLayeredPane(AutoCompleteTextField.class, true);
        assertTrue(layered.getComponentCount() > 0, "Popup wrapper should be attached to layered pane");
        Container popupWrapper = (Container) layered.getComponentAt(0);
        Container popup = (Container) popupWrapper.getComponentAt(0);
        assertFalse(popup.isVisible(), "Popup should be hidden initially");

        TestCodenameOneImplementation.getInstance().tapComponent(open);
        flushSerialCalls();

        assertTrue(popup.isVisible(), "Popup should become visible after triggering showPopup");
        com.codename1.ui.List suggestionList = (com.codename1.ui.List) popup.getComponentAt(0);
        assertEquals(5, suggestionList.getModel().getSize());
        assertEquals(5, suggestionList.getMinElementHeight(), "Minimum elements hint should propagate to list");

        implementation.dispatchPointerPressAndRelease(suggestionList.getAbsoluteX() + 1, suggestionList.getAbsoluteY() + 1);
        flushSerialCalls();

        assertEquals("Red", field.getText(), "Selecting the first suggestion should update the text");
        assertFalse(popup.isVisible(), "Popup should hide after selection");
    }
}
