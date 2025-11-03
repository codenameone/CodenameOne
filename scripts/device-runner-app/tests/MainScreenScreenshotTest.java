package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

public class MainScreenScreenshotTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            Form form = new Form("Main Screen", new BorderLayout());

            Container content = new Container(BoxLayout.y());
            content.getAllStyles().setBgColor(0x1f2937);
            content.getAllStyles().setBgTransparency(255);
            content.getAllStyles().setPadding(6, 6, 6, 6);
            content.getAllStyles().setFgColor(0xf9fafb);

            Label heading = new Label("Hello Codename One");
            heading.getAllStyles().setFgColor(0x38bdf8);
            heading.getAllStyles().setMargin(0, 4, 0, 0);

            Label body = new Label("Instrumentation main activity preview");
            body.getAllStyles().setFgColor(0xf9fafb);

            content.add(heading);
            content.add(body);

            form.add(BorderLayout.CENTER, content);
            form.show();
        });

        Cn1ssDeviceRunnerHelper.waitForMillis(500);

        final boolean[] result = new boolean[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> result[0] = Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("MainActivity"));
        return result[0];
    }
}
