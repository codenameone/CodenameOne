package com.codenameone.examples.hellocodenameone;

import com.codename1.system.Lifecycle;
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
import com.codename1.ui.CN;

import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunner;
import com.codenameone.examples.hellocodenameone.tests.Cn1ssDeviceRunnerReporter;

public class HelloCodenameOne extends Lifecycle {
    @Override
    public void init(Object context) {
        super.init(context);
        TestReporting.setInstance(new Cn1ssDeviceRunnerReporter());
    }

    @Override
    public void runApp() {
        new Thread(() -> new Cn1ssDeviceRunner().runSuite()).start();
    }
}
