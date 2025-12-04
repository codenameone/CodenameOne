package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class BindButtonStateSampleTest extends UITestBase {

    @FormTest
    void boundButtonReflectsPressAndReleaseFromSource() {
        implementation.setDisplaySize(1080, 1920);

        Form form = new Form("Hi World", BoxLayout.y());
        form.add(new Label("Hi World"));

        Button primary = new Button("Button 1");
        primary.setIconUIID("GrayIcon");
        Button follower = new Button("Button 2");
        follower.bindStateTo(primary);
        form.addAll(primary, follower);

        form.show();
        form.revalidate();
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();

        ensureSized(primary, form);
        ensureSized(follower, form);

        implementation.pressComponent(primary);
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();

        assertEquals(Button.STATE_PRESSED, primary.getState());
        assertEquals(Button.STATE_PRESSED, follower.getState());

        implementation.releaseComponent(primary);
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();

        assertEquals(Button.STATE_DEFAULT, primary.getState());
        assertEquals(Button.STATE_DEFAULT, follower.getState());
    }

    private void ensureSized(Component component, Form form) {
        for (int i = 0; i < 5 && (component.getWidth() <= 0 || component.getHeight() <= 0); i++) {
            form.revalidate();
            flushSerialCalls();
        }
    }
}
