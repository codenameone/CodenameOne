package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Form;
import com.codename1.ui.RichTextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;

/**
 * Screenshot coverage for {@link RichTextArea}. Renders a non-editable (caret-free, for a
 * deterministic capture) rich text document exercising headings, bold/italic, a colored span, a link
 * and a bulleted list. The editor is hosted in the native web widget, so we wait for the editor to
 * become ready and then let it settle before capturing - mirroring {@link BrowserComponentScreenshotTest}.
 */
public class RichTextAreaScreenshotTest extends BaseTest {
    private RichTextArea editor;
    private Form form;
    private Runnable readyRunnable;
    private boolean ready;

    @Override
    public boolean runTest() throws Exception {
        if (!BrowserComponent.isNativeBrowserSupported()) {
            done();
            return true;
        }
        form = createForm("Rich Text", new BorderLayout(), "RichTextArea");
        editor = new RichTextArea();
        // capture the rendered document without a blinking caret for determinism
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
        maybeSettle();
    }

    private void maybeSettle() {
        if (!ready || readyRunnable == null) {
            return;
        }
        UITimer.timer(2000, false, form, readyRunnable);
        readyRunnable = null;
    }
}
