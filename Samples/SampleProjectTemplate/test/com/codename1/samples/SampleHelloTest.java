package com.codename1.samples;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;

/**
 * A simple Codename One unit test that verifies the "Hi World" label is displayed
 * when {@link SampleMain} starts.
 */
public class SampleHelloTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        SampleMain app = new SampleMain();
        app.init(null);
        app.start();

        Form current = Display.getInstance().getCurrent();
        assertNotNull(current, "The main form should be visible");

        Label found = findLabelWithText(current, "Hi World");
        assertNotNull(found, "Expected to find the \"Hi World\" label on the form");
        return true;
    }

    private Label findLabelWithText(Form form, String expected) {
        for (int i = 0; i < form.getContentPane().getComponentCount(); i++) {
            Component child = form.getContentPane().getComponentAt(i);
            if (child instanceof Label) {
                Label label = (Label) child;
                if (expected.equals(label.getText())) {
                    return label;
                }
            }
        }
        return null;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public String toString() {
        return "SampleHelloTest";
    }
}
