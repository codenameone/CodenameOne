package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.AutoCompleteTextField;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.Container;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class AutocompleteSample2788PortTest extends UITestBase {

    @FormTest
    void replacingFieldWhilePopupOpenRemovesPopup() {
        Form form = new Form("Popup test", BorderLayout.absolute());
        final AutoCompleteTextField autocomplete = new AutoCompleteTextField("1", "2", "3", "11", "22", "33");
        autocomplete.setMinimumLength(1);
        final Label replacement = new Label("other content");
        Button tapMe = new Button("Tap me leaving the popup opened");
        tapMe.addActionListener(l -> {
            form.replace(autocomplete, replacement, null);
            form.revalidate();
        });

        form.add(BorderLayout.CENTER, autocomplete);
        form.add(BorderLayout.NORTH, tapMe);

        form.show();
        DisplayTest.flushEdt();
        flushSerialCalls();

        openPopup(autocomplete);
        Container popup = findVisibleAutocompletePopup(form);
        assertNotNull(popup, "Autocomplete popup should be visible after requesting suggestions");

        tapComponent(tapMe);
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        assertTrue(form.contains(replacement), "Replacement content should be part of the form after tap");
        assertFalse(form.contains(autocomplete), "Autocomplete field should be removed after replace");
        assertNull(findVisibleAutocompletePopup(form), "Autocomplete popup should be dismissed after field is replaced");
    }

    private void openPopup(AutoCompleteTextField field) {
        field.setText("1");
        field.showPopup();
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();
    }

    private Container findVisibleAutocompletePopup(Form form) {
        for (Object candidate : ComponentSelector.$("AutoCompletePopup", form)) {
            Container popup = (Container) candidate;
            if (popup.isVisible() && popup.getComponentCount() > 0) {
                return popup;
            }
        }
        return null;
    }
}
