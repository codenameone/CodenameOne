package com.codename1.samples;

import com.codename1.l10n.L10NManager;
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

public class UpdateTextAreaWhileEditingSampleTest extends UITestBase {

    @FormTest
    public void testUpdateTextAreaWhileEditing() {
        Form hi = new Form("Hi World", BoxLayout.y());
        TextArea ta = new TextArea();
        Button reset = new Button("Reset");
        Button setToDate = new Button("Set to date");

        reset.addActionListener(e->{
           ta.setText("");
        });

        setToDate.addActionListener(e->{
            ta.setText(L10NManager.getInstance().formatDateTime(new Date()));
        });
        hi.add(GridLayout.encloseIn(2, reset, setToDate));
        hi.add(ta);
        hi.show();
        waitForForm(hi);

        // Test reset
        ta.setText("Some text");
        reset.pressed();
        reset.released();
        assertEquals("", ta.getText(), "TextArea should be empty after reset");

        // Test set to date
        setToDate.pressed();
        setToDate.released();
        assertNotEquals("", ta.getText(), "TextArea should not be empty after setting date");
    }

    private void waitForForm(Form form) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 3000) {
            if (Display.getInstance().getCurrent() == form) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        fail("Form did not become current in time");
    }
}
