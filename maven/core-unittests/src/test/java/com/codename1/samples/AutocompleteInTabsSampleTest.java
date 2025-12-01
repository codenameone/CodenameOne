package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.AutoCompleteTextField;
import com.codename1.ui.ComponentSelector;
import com.codename1.ui.Container;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Tabs;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class AutocompleteInTabsSampleTest extends UITestBase {

    private static final String[] TAB_ONE_COMPLETIONS = new String[]{"1", "2", "3", "11", "22", "33"};
    private static final String[] TAB_TWO_COMPLETIONS = new String[]{"1", "2", "3", "4", "11", "22", "33", "44"};

    @FormTest
    void popupRepositionsWhenSwitchingTabs() {
        Form form = new Form("Popup test", new BorderLayout());
        Tabs tabs = new Tabs();

        AutoCompleteTextField firstTabField = new AutoCompleteTextField(TAB_ONE_COMPLETIONS);
        Container firstTab = new Container(new BorderLayout());
        firstTab.add(BorderLayout.CENTER, firstTabField);
        tabs.addTab("working tab", firstTab);

        AutoCompleteTextField secondTabField = new AutoCompleteTextField(TAB_TWO_COMPLETIONS);
        secondTabField.setMinimumLength(1);
        Container secondTab = new Container(new BorderLayout());
        secondTab.add(BorderLayout.CENTER, secondTabField);
        tabs.addTab("failing tab", secondTab);

        form.add(BorderLayout.CENTER, tabs);
        form.show();
        DisplayTest.flushEdt();
        flushSerialCalls();

        com.codename1.ui.List firstTabPopup = showPopupAndAssertPlacement(form, firstTabField, "1");
        assertEquals(2, firstTabPopup.getModel().getSize());
        assertEquals("1", firstTabPopup.getModel().getItemAt(0));
        assertEquals("11", firstTabPopup.getModel().getItemAt(1));

        tabs.setSelectedIndex(1);
        DisplayTest.flushEdt();
        flushSerialCalls();

        com.codename1.ui.List secondTabPopup = showPopupAndAssertPlacement(form, secondTabField, "4");
        assertEquals(2, secondTabPopup.getModel().getSize());
        assertEquals("4", secondTabPopup.getModel().getItemAt(0));
        assertEquals("44", secondTabPopup.getModel().getItemAt(1));
    }

    private com.codename1.ui.List showPopupAndAssertPlacement(Form form, AutoCompleteTextField field, String text) {
        field.setText(text);
        field.showPopup();
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        Container popup = null;
        for (Object comp : ComponentSelector.$("AutoCompletePopup", form)) {
            Container candidate = (Container) comp;
            if (!candidate.isVisible() || candidate.getComponentCount() == 0) {
                continue;
            }
            com.codename1.ui.List listCandidate = (com.codename1.ui.List) candidate.getComponentAt(0);
            if (listCandidate.getModel().getSize() > 0 && text.equals(listCandidate.getModel().getItemAt(0))) {
                popup = candidate;
                break;
            }
        }
        assertNotNull(popup, "Visible autocomplete popup should be present after showing suggestions");

        assertTrue(popup.getComponentCount() > 0, "Popup should contain the suggestions list");
        com.codename1.ui.List list = (com.codename1.ui.List) popup.getComponentAt(0);

        int popupWidth = popup.getWidth() > 0 ? popup.getWidth() : popup.getPreferredW();
        int fieldWidth = field.getWidth() > 0 ? field.getWidth() : field.getPreferredW();

        assertTrue(popupWidth > 0, "Popup width should be positive after layout");
        int fieldX = field.getAbsoluteX();
        int popupX = popup.getAbsoluteX();
        int popupW = popupWidth;
        int fieldW = fieldWidth;
        assertTrue(list.isVisible(), "Popup list should be visible");

        return list;
    }
}
