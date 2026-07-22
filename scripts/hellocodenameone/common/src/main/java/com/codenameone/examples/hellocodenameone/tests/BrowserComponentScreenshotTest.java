/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.util.SuccessCallback;

public class BrowserComponentScreenshotTest extends BaseTest {
    private static final int VISUAL_RETRY_MS = 250;
    private static final int VISUAL_TIMEOUT_MS = 12000;

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

        if (isHtml5()) {
            UITimer.timer(2000, false, form, readyRunnable);
        } else {
            // DOM readiness and even WebKit's first meaningful paint do not
            // guarantee that the native peer has reached the Metal surface.
            // Verify the exact screen image that will be emitted instead of
            // relying on a fixed delay and then taking an unrelated capture.
            awaitRenderedBrowserFrame(0);
        }
        readyRunnable = null;
    }

    private void awaitRenderedBrowserFrame(final int waitedMs) {
        browser.repaint();
        form.repaint();
        markCaptureStarted();
        Display.getInstance().screenshot(screen -> {
            if (screen == null) {
                fail("BrowserComponent screen capture returned null");
                return;
            }
            if (containsRenderedBrowserContent(screen)) {
                Cn1ssDeviceRunnerHelper.emitImage(screen, "BrowserComponent", this::done);
                return;
            }
            screen.dispose();
            if (waitedMs >= VISUAL_TIMEOUT_MS) {
                fail("BrowserComponent DOM loaded, but its native peer was not composited into the screen capture");
                return;
            }
            UITimer.timer(VISUAL_RETRY_MS, false, form,
                    () -> awaitRenderedBrowserFrame(waitedMs + VISUAL_RETRY_MS));
        });
    }

    private boolean containsRenderedBrowserContent(Image screen) {
        int screenWidth = screen.getWidth();
        int screenHeight = screen.getHeight();
        int displayWidth = Display.getInstance().getDisplayWidth();
        int displayHeight = Display.getInstance().getDisplayHeight();
        if (screenWidth <= 0 || screenHeight <= 0 || displayWidth <= 0 || displayHeight <= 0
                || browser.getWidth() <= 0 || browser.getHeight() <= 0) {
            return false;
        }

        double scaleX = screenWidth / (double) displayWidth;
        double scaleY = screenHeight / (double) displayHeight;
        int insetX = Math.max(1, (int) Math.round(8 * scaleX));
        int insetY = Math.max(1, (int) Math.round(8 * scaleY));
        int left = Math.max(0, (int) Math.round(browser.getAbsoluteX() * scaleX) + insetX);
        int right = Math.min(screenWidth,
                (int) Math.round((browser.getAbsoluteX() + browser.getWidth()) * scaleX) - insetX);
        int top = Math.max(0, (int) Math.round(browser.getAbsoluteY() * scaleY) + insetY);
        int contentBandHeight = Math.min(browser.getHeight() - 16, 180);
        int bottom = Math.min(screenHeight, top + Math.max(1,
                (int) Math.round(contentBandHeight * scaleY)));
        if (left >= right || top >= bottom) {
            return false;
        }

        int[] rgb = screen.getRGB();
        int requiredBrightPixels = Math.max(32, (right - left) / 20);
        int brightPixels = 0;
        for (int y = top; y < bottom; y++) {
            int rowOffset = y * screenWidth;
            for (int x = left; x < right; x++) {
                int color = rgb[rowOffset + x];
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;
                // The local fixture contains white and cyan text on a dark
                // background. The black uncomposited peer contains neither.
                if ((r > 160 && g > 160 && b > 160)
                        || (g > 120 && b > 160 && b > r + 30)) {
                    if (++brightPixels >= requiredBrightPixels) {
                        return true;
                    }
                }
            }
        }
        return false;
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
