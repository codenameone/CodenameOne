package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

abstract class AbstractGraphicsScreenshotTest extends AbstractTest {
    protected abstract Component createContent();

    protected abstract String screenshotName();

    @Override
    public boolean runTest() throws Exception {
        final Form[] holder = new Form[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            Form form = new Form("Graphics", new BorderLayout());
            form.add(BorderLayout.CENTER, createContent());
            holder[0] = form;
            form.show();
        });

        Cn1ssDeviceRunnerHelper.waitForMillis(1200);

        final boolean[] result = new boolean[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            if (holder[0] != null) {
                holder[0].revalidate();
                holder[0].repaint();
            }
            result[0] = Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot(screenshotName());
        });
        return result[0];
    }
}
