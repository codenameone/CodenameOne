package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

public class FullScreenWithBrowserComponentSampleTest extends UITestBase {

    @FormTest
    public void testFullScreenWithBrowserComponent() {
        FullScreenWithBrowserComponentSample sample = new FullScreenWithBrowserComponentSample();
        sample.start();

        Form current = Display.getInstance().getCurrent();
        assertNotNull(current, "Form should be shown");
        assertTrue(current.getLayout() instanceof BorderLayout, "Form should use BorderLayout");

        // Check BrowserComponent
        BrowserComponent bc = sample.getBrowserComponent();
        assertNotNull(bc, "BrowserComponent should be present");

        // Check Button
        Button toggleBtn = sample.getButton();
        assertNotNull(toggleBtn, "Toggle button should be present");

        // Toggle FullScreen
        toggleBtn.pressed();
        toggleBtn.released();
        flushSerialCalls();


        // Toggle back
        toggleBtn.pressed();
        toggleBtn.released();
        flushSerialCalls();

    }

}
