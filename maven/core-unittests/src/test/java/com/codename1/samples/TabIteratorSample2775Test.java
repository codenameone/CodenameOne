package com.codename1.samples;

import com.codename1.ui.CheckBox;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.Display;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

public class TabIteratorSample2775Test extends UITestBase {

    @FormTest
    public void testTabIterator() {
        Form hi = new Form("Hi World", BoxLayout.y());
        TextField tf1 = new TextField("Text 1");
        hi.add(tf1);
        Picker p1 = new Picker();
        p1.setType(Display.PICKER_TYPE_STRINGS);
        p1.setStrings("Red", "Green", "Blue", "Orange");
        hi.add(p1);
        hi.add(new TextField("Text 2"));

        CheckBox enableTabsCheckBox = new CheckBox("Enable Tabbing");
        enableTabsCheckBox.setSelected(true);
        enableTabsCheckBox.addActionListener(e->{
            $("*", hi).each(c->{
                c.setPreferredTabIndex(enableTabsCheckBox.isSelected() ? 0 : -1);
            });
        });
        hi.add(enableTabsCheckBox);
        hi.show();

        // Initially selected
        assertTrue(enableTabsCheckBox.isSelected());
        // Since we didn't run the listener yet (it's only on action), and the component doesn't call it on init,
        // we should verify initial state if relevant, but the logic is inside the listener.
        // Let's trigger it.

        enableTabsCheckBox.pressed();
        enableTabsCheckBox.released();

        assertFalse(enableTabsCheckBox.isSelected());
        assertEquals(-1, tf1.getPreferredTabIndex());

        enableTabsCheckBox.pressed();
        enableTabsCheckBox.released();

        assertTrue(enableTabsCheckBox.isSelected());
        assertEquals(0, tf1.getPreferredTabIndex());
    }
}
