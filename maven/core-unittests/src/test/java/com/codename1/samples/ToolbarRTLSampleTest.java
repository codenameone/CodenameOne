package com.codename1.samples;

import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import static org.junit.jupiter.api.Assertions.*;

public class ToolbarRTLSampleTest extends UITestBase {

    @FormTest
    public void testToolbarRTL() {
        UIManager.getInstance().getLookAndFeel().setRTL(true);
        try {
            Form hi = new Form("Hi World", BoxLayout.y());
            Toolbar tb = new Toolbar();
            hi.setToolbar(tb);

            final boolean[] actionPerformed = new boolean[1];
            Command cmd = new Command("Test") {
                public void actionPerformed(ActionEvent e) {
                    actionPerformed[0] = true;
                }
            };

            tb.addCommandToLeftSideMenu(cmd);
            hi.add(new Label("Hi World"));
            hi.show();
            waitForForm(hi);

            assertTrue(UIManager.getInstance().getLookAndFeel().isRTL(), "LookAndFeel should be RTL");

            // Simulate command execution
            cmd.actionPerformed(new ActionEvent(tb));
            assertTrue(actionPerformed[0], "Command action should be executed");

        } finally {
            UIManager.getInstance().getLookAndFeel().setRTL(false);
        }
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
