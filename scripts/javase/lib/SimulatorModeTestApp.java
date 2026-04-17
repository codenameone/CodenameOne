package com.codenameone.examples.javase.tests;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/**
 * Small simulator app used by JavaSE integration tests.
 */
public class SimulatorModeTestApp {
    private Form current;

    public void init(Object context) {
        try {
            Resources theme = Resources.openLayered("/theme");
            UIManager.getInstance().setThemeProps(theme.getTheme(theme.getThemeResourceNames()[0]));
        } catch (Exception ignored) {
            // Fallback to default theme if test resource isn't available.
        }
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        String mode = System.getProperty("cn1.test.window.mode", "unknown");
        Form form = new Form("JavaSE Simulator Test", new BorderLayout());
        form.add(BorderLayout.NORTH, new Label("Window mode: " + mode));

        com.codename1.ui.Container body = new com.codename1.ui.Container(BoxLayout.y());
        body.add(new Label("Robot validation baseline"));
        body.add(new Button("Primary Action"));
        Button dialogButton = new Button("Open Dialog");
        dialogButton.addActionListener(evt -> Dialog.show("Mode", "Current mode: " + mode, "OK", null));
        body.add(dialogButton);
        form.add(BorderLayout.CENTER, body);

        current = form;
        form.show();

        if (Boolean.getBoolean("cn1.test.landscape")) {
            CN.callSerially(() -> {
                try {
                    CN.setWindowSize(900, 520);
                    CN.lockOrientation(false);
                    form.revalidate();
                } catch (Throwable ignored) {
                }
            });
        }

        if (Boolean.getBoolean("cn1.test.doNetwork")) {
            ConnectionRequest req = new ConnectionRequest();
            req.setUrl("https://example.com");
            req.setPost(false);
            req.setFailSilently(true);
            req.setHttpMethod("GET");
            NetworkManager.getInstance().addToQueue(req);
        }

        // Safety exit in case a harness forgets to close the simulator.
        CN.setTimeout(120000, () -> {
            if (CN.getCurrentForm() != null) {
                CN.log("JavaSE simulator integration app safety timeout reached.");
                CN.exitApplication();
            }
        });
    }

    public void stop() {
        current = CN.getCurrentForm();
    }

    public void destroy() {
        current = null;
    }
}
