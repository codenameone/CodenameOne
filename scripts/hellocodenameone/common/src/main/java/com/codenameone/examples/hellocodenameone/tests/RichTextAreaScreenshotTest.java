package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Form;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/**
 * Screenshot coverage for {@link RichTextArea}. Renders a non-editable (caret-free, for a
 * deterministic capture) rich text document.
 *
 * The editor lives inside the platform web widget, which not every platform can render into a
 * screenshot (some native ports can't capture peer-component pixels). To keep the suite robust the
 * capture is hard-bounded: it fires shortly after the editor becomes ready, or after a few seconds
 * regardless, so a platform that can't render/capture the peer produces a (blank-region) screenshot
 * instead of stalling the whole suite.
 */
public class RichTextAreaScreenshotTest extends BaseTest {
    private Form form;
    private Runnable readyRunnable;
    private boolean ready;
    private boolean captured;
    /// Set when the first attempt went silent to hand off to the runner's
    /// silent-timeout retry (fresh full budget, warm WebKit); the second pass
    /// captures at the bound regardless so a genuinely broken editor still
    /// fails loudly as a differ rather than hanging.
    private boolean retriedOnce;

    @Override
    public boolean runTest() throws Exception {
        if (!BrowserComponent.isNativeBrowserSupported()) {
            done();
            return true;
        }
        // Reset per attempt: the runner's retry re-runs this method on the SAME
        // instance, and a stale ready/captured from the first attempt would
        // short-circuit the second.
        ready = false;
        captured = false;
        readyRunnable = null;
        form = createForm("Rich Text", new BorderLayout(), "RichTextArea");
        RichTextArea editor = new RichTextArea();
        editor.setEditable(false);
        editor.setHtml("<h2>Trip itinerary</h2>"
                + "<p>Meet at the <b>main lobby</b> by <i>9:00 AM</i>. Bring a "
                + "<span style=\"color:#d93025\">valid passport</span> and your "
                + "<a href=\"#\">boarding pass</a>.</p>"
                + "<ul><li>Day 1 &mdash; city tour</li><li>Day 2 &mdash; museum &amp; harbor</li>"
                + "<li>Day 3 &mdash; free time</li></ul>");
        editor.addReadyListener(evt -> {
            ready = true;
            maybeSettle();
        });
        form.add(BorderLayout.CENTER, editor);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        this.readyRunnable = run;
        // Bound so a web view that never reports ready can't stall the suite --
        // but do NOT capture a blank peer on the first exhaustion: a cold WebKit
        // on a starved metal runner has been observed shipping a solid-black
        // web-view region (the page hadn't painted), a guaranteed mismatch.
        // Instead go silent and let the runner's silent-timeout retry re-run the
        // whole test with a fresh 30s budget and a warm WebKit (same pattern as
        // GoogleWebMap). The second pass captures at the bound regardless, so a
        // genuinely broken editor fails loudly as a differ.
        UITimer.timer(12000, false, parent, this::boundFired);
        maybeSettle();
    }

    private void boundFired() {
        if (captured || ready) {
            return; // capture already happened / is being scheduled by the ready path
        }
        if (!retriedOnce) {
            retriedOnce = true;
            System.out.println("CN1SS:WARN:test=RichTextArea editor not ready after 12000ms; "
                    + "going silent to hand off to the runner's timeout retry");
            readyRunnable = null;
            return;
        }
        capture();
    }

    private void maybeSettle() {
        if (ready && form != null) {
            UITimer.timer(3500, false, form, this::capture);
        }
    }

    /// Metal can present the web-view layer a beat after the settle chain --
    /// force a fresh, fully-presented frame before capturing (the same
    /// mitigation the other browser-peer tests use).
    @Override
    protected long extraSettleBeforeCaptureMillis() {
        return 700;
    }

    private void capture() {
        if (captured || readyRunnable == null) {
            return;
        }
        captured = true;
        Runnable r = readyRunnable;
        readyRunnable = null;
        r.run();
    }
}
