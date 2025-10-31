package com.mycompany.app;

import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.io.Log;
import java.io.IOException;
import com.codename1.ui.Image;
import java.io.ByteArrayOutputStream;
import com.codename1.io.Base64;

public class DeviceRunnerTest {

    private Form current;

    public void init(Object context) {
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Hi World", new BorderLayout());
        hi.add(BorderLayout.CENTER, new Label("Hello, World!"));
        hi.show();
        UITimer.timer(1000, false, () -> {
            Image screenshot = Image.createImage(hi.getWidth(), hi.getHeight());
            hi.paintComponent(screenshot.getGraphics(), true);
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                com.codename1.ui.ImageIO.getImageIO().save(screenshot, os, com.codename1.ui.Image.FORMAT_PNG, 1.0f);
                System.out.println("CN1SS_BEGIN");
                System.out.println(Base64.encode(os.toByteArray()));
                System.out.println("CN1SS_END");
            } catch (IOException e) {
                Log.e(e);
            }
            Display.getInstance().exitApplication();
        });
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }
}
