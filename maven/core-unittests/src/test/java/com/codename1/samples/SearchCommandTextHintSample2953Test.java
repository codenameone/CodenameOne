package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

public class SearchCommandTextHintSample2953Test extends UITestBase {

    @FormTest
    public void testSearchCommandFiltersContent() {
        final Form form = new Form("FormTitle");
        form.setLayout(BoxLayout.y());
        final Container content = form.getContentPane();
        final Label[] labels = new Label[20];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new Label("Label " + i);
            content.add(labels[i]);
        }

        Toolbar toolbar = form.getToolbar();
        ActionListener listener = e -> {
            String text = (String) e.getSource();
            for (Component c : form.getContentPane()) {
                boolean hide = c instanceof Label && ((Label) c).getText().indexOf(text) < 0;
                c.setHidden(hide);
            }
            form.getComponentForm().animateLayout(150);
        };
        toolbar.addSearchCommand(listener);

        form.show();
        flushSerialCalls();

        toolbar.showSearchBar(listener);
        Toolbar searchBar = form.getToolbar();
        TextField searchField = (TextField) searchBar.getTitleComponent();
        searchField.startEditingAsync();
        flushSerialCalls();

        String query = "Label 19";
        TestCodenameOneImplementation impl = implementation;
        for (int i = 0; i < query.length(); i++) {
            impl.dispatchKeyPress(query.charAt(i));
        }
        flushSerialCalls();

        for (int i = 0; i < labels.length; i++) {
            if (i == 19) {
                assertFalse(labels[i].isHidden(), "Matching label should remain visible");
            } else {
                assertTrue(labels[i].isHidden(), "Non-matching labels should be hidden");
            }
        }
    }
}
