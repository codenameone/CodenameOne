package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

public class BrowserComponentScreenshotTest extends BaseTest {
    private BrowserComponent browser;
    @Override
    public boolean runTest() throws Exception {
        if (!BrowserComponent.isNativeBrowserSupported()) {
            done();
            return true;
        }
        Form form = createForm("Browser Test", new BorderLayout(), "BrowserComponent");
        browser = new BrowserComponent();
        browser.setPage(buildHtml(), null);
        form.add(BorderLayout.CENTER, browser);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        new Thread(() -> {
            int attempts = 0;
            while (attempts < 50) { // 10 seconds (50 * 200ms)
                try {
                    String text = browser.executeAndReturnString("document.body.innerText");
                    if (text != null && text.contains("Codename One")) {
                        Display.getInstance().callSerially(run);
                        return;
                    }
                } catch (Exception e) {
                    // ignore errors while loading
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {}
                attempts++;
            }
            Display.getInstance().callSerially(run);
        }).start();
    }

    private static String buildHtml() {
        return "<html><head><meta charset='utf-8'/>"
                + "<style>body{margin:0;font-family:sans-serif;background:#0e1116;color:#f3f4f6;}"
                + ".container{padding:24px;text-align:center;}h1{font-size:24px;margin-bottom:12px;}"
                + "p{font-size:16px;line-height:1.4;}span{color:#4cc9f0;}</style></head>"
                + "<body><div class='container'><h1>Codename One</h1>"
                + "<p>BrowserComponent <span>instrumentation</span> test content.</p></div></body></html>";
    }
}
