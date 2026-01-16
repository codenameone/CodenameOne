package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Display;
import com.codename1.ui.plaf.UIManager;

public class BackgroundThreadUiAccessTest extends BaseTest {
    @Override
    public boolean runTest() {
        Thread worker = new Thread(() -> {
            try {
                Display display = Display.getInstance();
                int width = display.getDisplayWidth();
                int pixels = display.convertToPixels(10, true);
                UIManager manager = UIManager.getInstance();
                if (width <= 0 || pixels <= 0 || manager == null) {
                    fail("Unexpected display metrics: width=" + width + " pixels=" + pixels);
                    return;
                }
                done();
            } catch (Throwable t) {
                fail("Background UI access test failed: " + t);
            }
        }, "cn1-ui-access-bg");
        worker.start();
        return true;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
