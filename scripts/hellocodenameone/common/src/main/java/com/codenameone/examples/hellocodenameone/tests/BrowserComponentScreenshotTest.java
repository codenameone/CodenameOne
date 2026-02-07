package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Form;
import com.codename1.ui.Sheet;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.util.SuccessCallback;

public class BrowserComponentScreenshotTest extends BaseTest {
    private BrowserComponent browser;
    private boolean loaded;
    private Runnable readyRunnable;
    private Form form;
    private boolean jsReady;
    private boolean jsCheckPending;
    private boolean sheetShown;

    @Override
    public boolean runTest() throws Exception {
        if (!BrowserComponent.isNativeBrowserSupported()) {
            done();
            return true;
        }
        form = createForm("Browser Test", new BorderLayout(), "BrowserComponent");
        browser = new BrowserComponent();
        browser.addWebEventListener(BrowserComponent.onLoad, evt -> {
            loaded = true;
            checkReady();
        });
        browser.setPage(buildHtml(), null);
        form.add(BorderLayout.CENTER, browser);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        this.readyRunnable = run;
        checkReady();
    }

    private void checkReady() {
        if (!loaded || readyRunnable == null) {
            return;
        }
        if (!jsReady) {
            if (!jsCheckPending) {
                jsCheckPending = true;
                // Verify content is actually present in the DOM
                browser.execute("callback.onSuccess(document.body.innerText)", new SuccessCallback<BrowserComponent.JSRef>() {
                    public void onSucess(BrowserComponent.JSRef result) {
                        jsReady = true;
                        checkReady();
                    }
                });
            }
            return;
        }

        if (!sheetShown) {
            sheetShown = true;
            Sheet sheet = new Sheet(null, "Browser Sheet");
            sheet.show();
        }
        UITimer.timer(2000, false, form, readyRunnable);
        readyRunnable = null;
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
