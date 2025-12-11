package com.codename1.samples;

import com.codename1.components.ToastBar;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.layouts.BoxLayout;
import static org.junit.jupiter.api.Assertions.*;

public class ToastBarSampleTest extends UITestBase {

    @FormTest
    public void testToastBarSample() {
        CN.setProperty("Component.revalidateOnStyleChange", "false");
        CN.setProperty("Form.revalidateFromRoot", "false");

        Form hi = new Form("Hi World", BoxLayout.y());
        Button btn = new Button("Test");

        final boolean[] toastShown = new boolean[1];
        // We can't easily intercept ToastBar.showInfoMessage in a test without UI interaction or mocking,
        // but we can verify the button action doesn't crash.

        btn.addActionListener(evt->{
            ToastBar.showInfoMessage("This is a toastbar message");
            toastShown[0] = true;
        });
        hi.add(btn);
        hi.show();
        waitForForm(hi);

        btn.pressed();
        btn.released();

        assertTrue(toastShown[0], "Button action listener should have been triggered");
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
