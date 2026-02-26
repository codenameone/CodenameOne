package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;

public class OrientationLockScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() {
        Form hi = new Form("Orientation Lock", new BoxLayout(BoxLayout.Y_AXIS)) {
            @Override
            protected void onShowCompleted() {
                CN.lockOrientation(false);
                UITimer.timer(300, false, this, () -> {
                    Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("landscape");
                    CN.lockOrientation(true);
                    UITimer.timer(300, false, this, OrientationLockScreenshotTest.this::done);
                });
            }
        };
        hi.add(new Label("Testing orientation lock..."));
        hi.show();
        return true;
    }
}
