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
import com.codename1.ui.RGBImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.util.SuccessCallback;

public class BrowserComponentScreenshotTest extends BaseTest {
    private static final int VISUAL_RETRY_MS = 250;
    /**
     * Wall-clock budget for WebKit to composite its native peer into the
     * capture.
     *
     * Generous on purpose. This is not a correctness threshold -- the
     * assertion is that the peer composites at all, and
     * {@link #containsRenderedBrowserContent} still decides that -- it is only
     * how long a loaded CI runner is allowed to take getting there. When the
     * budget is too tight the test emits no screenshot at all, so the suite
     * reports "actual screenshot missing", which reads like a lost capture
     * rather than a slow one.
     */
    private static final long VISUAL_TIMEOUT_MS = 45000L;

    private BrowserComponent browser;
    private boolean loaded;
    private Runnable readyRunnable;
    private Form form;
    private boolean jsReady;
    private boolean jsCheckPending;
    private RGBImage visualBand;
    /// Which attempt the state below belongs to. Advanced by resetForRetry() and
    /// by every runTest(), and captured by every asynchronous callback so a
    /// superseded attempt can never answer for the current one. A page load, a
    /// pending execute() and an in-flight screenshot all outlive the attempt
    /// that started them, and the state they write -- loaded, jsReady, the
    /// emitted capture, a fail() -- is shared. Guarding one of the three would
    /// just move the bug.
    ///
    /// EDT-confined: the harness finalizes on the EDT and all three callbacks
    /// are delivered there, so a plain int needs no further publication.
    private int attempt;

    /// Retire the current generation the moment the harness decides to retry,
    /// not when the queued runTest() eventually runs.
    ///
    /// finalizeTest() calls this and then queues the next runTest() through
    /// CN.callSerially, so incrementing only in runTest() left a window in
    /// between where a callback from the timed-out attempt still matched the
    /// current generation -- and resetForRetry() has just cleared done, failed
    /// and captureStarted, so an emitImage() or fail() arriving in that window
    /// is honoured, completing or poisoning a retry that has not begun.
    @Override
    public synchronized void resetForRetry() {
        attempt++;
        super.resetForRetry();
    }

    @Override
    public boolean runTest() throws Exception {
        if (CN.isTV() || CN.isWatch() || !BrowserComponent.isNativeBrowserSupported()) {
            done();
            return true;
        }
        // Every attempt starts from nothing. resetForRetry() clears the harness
        // flags only, on the documented assumption that a subclass builds a
        // fresh Form in runTest() and therefore carries no state across a retry
        // -- and this test broke that assumption. It kept loaded and jsReady,
        // so the retry saw a page that the PREVIOUS BrowserComponent had
        // loaded, skipped waiting for the new peer entirely, and went straight
        // into a twelve-second visual poll against a web view that had not
        // rendered anything yet. The retry that exists to survive a slow
        // engine start could therefore never survive one: the second attempt
        // timed out exactly like the first.
        loaded = false;
        jsReady = false;
        jsCheckPending = false;
        readyRunnable = null;
        visualBand = null;
        final int generation = ++attempt;
        form = createForm("Browser Test", new BorderLayout(), "BrowserComponent");
        browser = new BrowserComponent();
        browser.addWebEventListener(BrowserComponent.onLoad, evt -> {
            if (generation != attempt) {
                return;
            }
            loaded = true;
            checkReady(generation);
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
        checkReady(attempt);
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return !isHtml5();
    }

    private static boolean isHtml5() {
        return "HTML5".equals(Display.getInstance().getPlatformName());
    }

    private static boolean isLinux() {
        return "linux".equals(Display.getInstance().getPlatformName());
    }

    private void checkReady(final int generation) {
        if (generation != attempt || !loaded || readyRunnable == null) {
            return;
        }
        if (!jsReady) {
            if (!jsCheckPending) {
                jsCheckPending = true;
                // Verify content is actually present in the DOM
                browser.execute("callback.onSuccess(document.body.innerText)", new SuccessCallback<BrowserComponent.JSRef>() {
                    public void onSucess(BrowserComponent.JSRef result) {
                        // A retry replaces the component while this execute() is
                        // still outstanding, and the answer describes the DOM of
                        // the page the PREVIOUS web view had loaded. Letting it
                        // set jsReady would hand the retry a result that says
                        // nothing about the component now on screen -- and on the
                        // desktop and HTML5 paths, where the contents are never
                        // checked visually, that is enough to pass the test over
                        // a replacement page that never became usable.
                        if (generation != attempt) {
                            return;
                        }
                        String text = result == null ? null : result.getValue();
                        if (text == null
                                || text.indexOf("Codename One") < 0
                                || text.indexOf("BrowserComponent instrumentation test content.") < 0) {
                            fail("BrowserComponent DOM content was not available through execute(): " + text);
                            return;
                        }
                        jsReady = true;
                        checkReady(generation);
                    }
                });
            }
            return;
        }

        if (isHtml5() || CN.isDesktop() || isLinux()) {
            // Desktop screenshots intentionally cannot include the native web
            // peer (the committed Mac/JavaSE baselines contain its black
            // placeholder), and the Linux port cannot either: its
            // browserCapturePng is a documented stub pending the async WebKit
            // snapshot bridge, so generatePeerImage always answers null and the
            // peer never reaches the capture. The execute() assertion above
            // validates the real DOM; use the normal harness capture for the
            // surrounding form.
            UITimer.timer(2000, false, form, readyRunnable);
        } else {
            // DOM readiness and even WebKit's first meaningful paint do not
            // guarantee that the native peer has reached the Metal surface.
            // Verify the exact screen image that will be emitted instead of
            // relying on a fixed delay and then taking an unrelated capture.
            awaitRenderedBrowserFrame(generation, System.currentTimeMillis() + VISUAL_TIMEOUT_MS);
        }
        readyRunnable = null;
    }

    /**
     * @param generation the retry attempt this poll belongs to. A retry replaces the
     *      component while a timer from the previous attempt is still scheduled, and
     *      letting that timer keep polling would capture -- and pass or fail -- against
     *      a component that is no longer on screen.
     * @param deadline wall-clock instant to give up at, rather than a count of
     *      the retry delays. Each attempt also performs a full
     *      {@code Display.screenshot()}, and on a loaded runner that capture
     *      costs far more than the 250ms delay between attempts -- summing the
     *      delays alone therefore under-counted real time by roughly 2x, so a
     *      nominal 12s budget gave up after 24s of wall clock. Timing out
     *      against the clock makes the number mean what it says.
     */
    private void awaitRenderedBrowserFrame(final int generation, final long deadline) {
        if (generation != attempt) {
            return;
        }
        browser.repaint();
        form.repaint();
        markCaptureStarted();
        Display.getInstance().screenshot(screen -> {
            // The capture is asynchronous, so it too can land after a retry has
            // replaced the component. Emitting it would baseline the superseded
            // attempt's screen, and fail() would report the old attempt's verdict
            // against the new one.
            if (generation != attempt) {
                if (screen != null) {
                    screen.dispose();
                }
                return;
            }
            if (screen == null) {
                fail("BrowserComponent screen capture returned null");
                return;
            }
            if (containsRenderedBrowserContent(screen)) {
                Cn1ssDeviceRunnerHelper.emitImage(screen, "BrowserComponent", this::done);
                return;
            }
            screen.dispose();
            if (System.currentTimeMillis() >= deadline) {
                fail("BrowserComponent DOM loaded, but its native peer was not composited"
                        + " into the screen capture within " + VISUAL_TIMEOUT_MS + "ms");
                return;
            }
            UITimer.timer(VISUAL_RETRY_MS, false, form,
                    () -> awaitRenderedBrowserFrame(generation, deadline));
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

        int bandWidth = right - left;
        int bandHeight = bottom - top;
        // Both colours, not either one.
        //
        // The fixture is white and cyan text on a near-black background, and this
        // originally looked only for the bright pixels because the case it was written
        // for was iOS, where a peer that has not reached the Metal surface is BLACK --
        // brightness alone separates the two there. On Android an unpainted WebView is
        // WHITE, which passes a brightness test on its own the very first time it is
        // asked, so the harness emitted a blank white frame and the run failed as a
        // screenshot mismatch rather than waiting the further quarter-second the peer
        // needed. Requiring the dark background as well describes the fixture instead of
        // one platform's failure colour, and neither blank state can satisfy it.
        if (visualBand == null || visualBand.getWidth() != bandWidth
                || visualBand.getHeight() != bandHeight) {
            visualBand = new RGBImage(new int[bandWidth * bandHeight], bandWidth, bandHeight);
        }
        screen.toRGB(visualBand, 0, 0, left, top, bandWidth, bandHeight);
        int[] rgb = visualBand.getRGB();
        int requiredBrightPixels = Math.max(32, bandWidth / 20);
        // The text occupies a small part of the band and the background the rest, so a
        // quarter of the pixels is a comfortable floor for a genuinely rendered page and
        // unreachable for a white one.
        int requiredDarkPixels = Math.max(32, (bandWidth * bandHeight) / 4);
        int brightPixels = 0;
        int darkPixels = 0;
        for (int y = 0; y < bandHeight; y++) {
            int rowOffset = y * bandWidth;
            for (int x = 0; x < bandWidth; x++) {
                int color = rgb[rowOffset + x];
                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;
                // The local fixture contains white and cyan text on a dark
                // background. The black uncomposited peer has the dark and none of
                // the text; the white unpainted one has neither.
                if ((r > 160 && g > 160 && b > 160)
                        || (g > 120 && b > 160 && b > r + 30)) {
                    brightPixels++;
                } else if (r < 48 && g < 48 && b < 48) {
                    // The fixture's #0e1116 backdrop. Requiring it as well as
                    // the text is what stops a blank frame from passing: a peer
                    // that never composited leaves the form's plain white
                    // background, which is bright everywhere and would satisfy
                    // the text test on its own -- which is exactly how the
                    // Linux port, whose peer capture is still a stub, recorded
                    // an empty rectangle as a rendered page.
                    darkPixels++;
                }
                if (brightPixels >= requiredBrightPixels && darkPixels >= requiredDarkPixels) {
                    return true;
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
