package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.animations.Transition;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// Drives a `Transition` from a source form to a destination form through six
/// deterministic frames. Subclasses provide the `Transition` instance and may
/// customize the source/destination forms; defaults give a simple coloured form
/// pair so individual transitions are easy to add.
public abstract class AbstractTransitionScreenshotTest extends AbstractAnimationScreenshotTest {
    private Form sourceForm;
    private Form destForm;
    private Transition transition;

    protected abstract Transition createTransition(int duration);

    protected void buildSourceForm(Form form) {
        styleForm(form, "Source", 0x1f4068, 0xffffff);
    }

    protected void buildDestForm(Form form) {
        styleForm(form, "Destination", 0x9c1d1d, 0xffffff);
    }

    private static void styleForm(Form form, String label, int bgColor, int fgColor) {
        form.setLayout(new BorderLayout());
        Style cps = form.getContentPane().getAllStyles();
        cps.setBgTransparency(255);
        cps.setBgColor(bgColor);
        cps.setFgColor(fgColor);
        Container content = new Container(BoxLayout.y());
        Label heading = new Label(label);
        heading.getAllStyles().setFgColor(fgColor);
        heading.getAllStyles().setMargin(8, 8, 8, 8);
        content.add(heading);
        Button action = new Button("Action");
        action.getAllStyles().setFgColor(fgColor);
        action.getAllStyles().setBgColor(bgColor ^ 0xffffff);
        action.getAllStyles().setBgTransparency(180);
        content.add(action);
        Label footnote = new Label("frame test - " + label);
        footnote.getAllStyles().setFgColor(fgColor);
        content.add(footnote);
        form.add(BorderLayout.CENTER, content);
    }

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);
        sourceForm = new Form(getSourceTitle());
        sourceForm.setWidth(frameWidth);
        sourceForm.setHeight(frameHeight);
        // Forms default to invisible until shown; without this paintComponent
        // is a no-op, leaving every transition frame empty.
        sourceForm.setVisible(true);
        buildSourceForm(sourceForm);

        destForm = new Form(getDestTitle());
        destForm.setWidth(frameWidth);
        destForm.setHeight(frameHeight);
        destForm.setVisible(true);
        buildDestForm(destForm);

        transition = createTransition(getAnimationDurationMillis());
        transition.init(sourceForm, destForm);
        transition.initTransition();
    }

    @Override
    protected void finishCapture() {
        if (transition != null) {
            try {
                transition.cleanup();
            } catch (Throwable ignore) {
                // best effort
            }
        }
        sourceForm = null;
        destForm = null;
        transition = null;
        super.finishCapture();
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        transition.animate();
        transition.paint(g);
    }

    protected String getSourceTitle() {
        return "Source";
    }

    protected String getDestTitle() {
        return "Destination";
    }
}
