package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

public class BrowserComponentScreenshotTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        if (!BrowserComponent.isNativeBrowserSupported()) {
            return true;
        }
        Form form = new Form("Browser Test", new BorderLayout()) {
            @Override
            protected void onShowCompleted() {
                TestUtils.waitFor(100);
                Cn1ssDeviceRunnerHelper.emitCurrentFormScreenshot("BrowserComponent");
            }
        };
        BrowserComponent browser = new BrowserComponent();
        browser.setPage(buildHtml(), null);
        form.add(BorderLayout.CENTER, browser);
        form.show();
        TestUtils.waitFor(250);
        return true;
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
