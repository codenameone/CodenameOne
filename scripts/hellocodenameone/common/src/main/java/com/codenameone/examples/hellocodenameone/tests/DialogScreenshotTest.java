package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.Timer;
import java.util.TimerTask;

public class DialogScreenshotTest extends BaseTest {
    @Override
    public boolean runTest() {
        Form form = new Form("Dialog Test", new BorderLayout());
        form.add(BorderLayout.CENTER, new Label("Preparing dialog screenshot"));
        form.show();

        Dialog dialog = new Dialog("Dialog");
        dialog.setLayout(BoxLayout.y());
        dialog.add(new Label("Dialog content"));
        dialog.add(new Label("Modal screenshot verification"));

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Display.getInstance().callSerially(() -> {
                    try {
                        Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("Dialog");
                    } finally {
                        dialog.dispose();
                        done();
                        timer.cancel();
                    }
                });
            }
        }, 700);

        dialog.showPacked(BorderLayout.CENTER, true);
        return true;
    }
}
