package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

public class BrowserComponentScreenshotTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        final boolean[] supported = new boolean[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> supported[0] = BrowserComponent.isNativeBrowserSupported());
        if (!supported[0]) {
            TestUtils.log("BrowserComponent native support unavailable; skipping screenshot test");
            return true;
        }

        final boolean[] loadFinished = new boolean[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> {
            Form form = new Form("Browser Test", new BorderLayout());
            BrowserComponent browser = new BrowserComponent();
            browser.addWebEventListener(BrowserComponent.onLoad, evt -> loadFinished[0] = true);
            browser.setPage(buildHtml(), null);
            form.add(BorderLayout.CENTER, browser);
            form.show();
        });

        for (int elapsed = 0; elapsed < 10000 && !loadFinished[0]; elapsed += 200) {
            TestUtils.waitFor(200);
        }
        if (!loadFinished[0]) {
            TestUtils.log("BrowserComponent content did not finish loading in time");
            return false;
        }

        Cn1ssDeviceRunnerHelper.waitForMillis(500);

        final boolean[] result = new boolean[1];
        Cn1ssDeviceRunnerHelper.runOnEdtSync(() -> result[0] = Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("BrowserComponent"));
        return result[0];
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
