package com.codenameone.examples.hellocodenameone;

import com.codename1.testing.TestReporting;
import com.codename1.ui.Button;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
static import com.codename1.ui.CN.*;

import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunner;
import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunnerReporter;

public class HelloCodenameOne {
    private Form current;
    private Form mainForm;
    private static boolean deviceRunnerExecuted;

    public void init(Object context) {
        TestReporting.setInstance(new Cn1ssDeviceRunnerReporter());
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        if (!deviceRunnerExecuted) {
            deviceRunnerExecuted = true;
            callSerially(() -> new Cn1ssDeviceRunner().runSuite());
        }
        new Form("Fallback").show();
        //showMainForm();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
        // Nothing to clean up for this sample
    }
}
