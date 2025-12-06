package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.io.Util;

import static org.junit.jupiter.api.Assertions.*;

class ButtonLongPointerPressSampleTest extends UITestBase {

    @FormTest
    void longPressCallbackInvokedOnButton() {
        implementation.setDisplaySize(1080, 1920);

        Form form = new Form("Hi World", BoxLayout.y());
        form.add(new Label("Hi World"));

        final boolean[] longPressReceived = new boolean[1];
        Button button = new Button("Long Press Me") {
            @Override
            public void longPointerPress(int x, int y) {
                longPressReceived[0] = true;
                super.longPointerPress(x, y);
            }
        };

        form.add(button);
        form.show();
        drainEdt(form);

        ensureSized(button, form);

        implementation.pressComponent(button);
        drainEdt(form);

        Util.sleep(600);
        drainEdt(form);

        assertTrue(longPressReceived[0], "Long pointer press should trigger override");

        implementation.releaseComponent(button);
        drainEdt(form);
    }

    private void ensureSized(Button button, Form form) {
        for (int i = 0; i < 5 && (button.getWidth() <= 0 || button.getHeight() <= 0); i++) {
            form.revalidate();
            drainEdt(form);
        }
        assertTrue(button.getWidth() > 0, "Button should have width after layout");
        assertTrue(button.getHeight() > 0, "Button should have height after layout");
    }

    private void drainEdt(Form form) {
        form.revalidate();
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();
    }
}
