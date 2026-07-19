package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
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

    @Override
    public boolean runTest() throws Exception {
        if (CN.isTV() || CN.isWatch() || !BrowserComponent.isNativeBrowserSupported()) {
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
        // BrowserComponent is a DOM peer on HTML5, outside the CN1 canvas that
        // Display.screenshot() captures. Exercise the real peer and callback
        // path there, but finish as an assertion test instead of recording a
        // misleading black canvas rectangle as a visual baseline.
        this.readyRunnable = isHtml5() ? this::done : run;
        checkReady();
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return !isHtml5();
    }

    private static boolean isHtml5() {
        return "HTML5".equals(Display.getInstance().getPlatformName());
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
                        String text = result == null ? null : result.getValue();
                        if (text == null
                                || text.indexOf("Codename One") < 0
                                || text.indexOf("BrowserComponent instrumentation test content.") < 0) {
                            fail("BrowserComponent DOM content was not available through execute(): " + text);
                            return;
                        }
                        jsReady = true;
                        checkReady();
                    }
                });
            }
            return;
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
