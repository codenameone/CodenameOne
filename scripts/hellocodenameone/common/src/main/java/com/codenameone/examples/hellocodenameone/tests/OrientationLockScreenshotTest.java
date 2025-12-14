package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.TestUtils;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

public class OrientationLockScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() throws Exception {
        Form hi = createForm("Orientation Lock", new BoxLayout(BoxLayout.Y_AXIS), "landscape");
        hi.add(new Label("Testing orientation lock..."));

        hi.show();

        CN.lockOrientation(false);

        TestUtils.waitFor(250);

        return true;
    }
}
