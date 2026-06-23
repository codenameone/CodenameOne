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

    @Override
    public boolean runTest() throws Exception {
        if (!BrowserComponent.isNativeBrowserSupported()) {
            done();
            return true;
        }
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
        UITimer.timer(4500, false, parent, this::capture);
        maybeSettle();
    }

    private void maybeSettle() {
        if (ready && form != null) {
            UITimer.timer(1200, false, form, this::capture);
        }
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
