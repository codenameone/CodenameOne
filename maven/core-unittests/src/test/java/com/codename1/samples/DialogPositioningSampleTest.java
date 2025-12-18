package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.geom.Dimension;

import static org.junit.jupiter.api.Assertions.*;

class DialogPositioningSampleTest extends UITestBase {

    @FormTest
    void dialogPositionsItselfBelowTitleAreaAndStretchesOnPhone() {
        implementation.setDisplaySize(1080, 1920);

        boolean originalGlobalToolbar = Toolbar.isGlobalToolbar();
        Toolbar.setGlobalToolbar(true);
        Form form = new Form("Hi World", BoxLayout.y());
        try {
            configureTitleArea(form);

            Button trigger = new Button("Show Dialog");
            form.add(new Label("Hi World"));
            form.add(trigger);

            final Dialog[] dialogHolder = new Dialog[1];
            final int[] expectedTop = new int[1];
            final int[] expectedLeft = new int[1];
            final int[] actualTop = new int[1];
            final int[] actualContentTop = new int[1];
            final int[] actualLeft = new int[1];
            final int[] actualContentWidth = new int[1];
            final int[] actualHeight = new int[1];

            trigger.addActionListener(evt -> showPositionedDialog(form, dialogHolder, expectedTop, expectedLeft,
                    actualTop, actualContentTop, actualLeft, actualContentWidth, actualHeight));

            form.show();
            form.revalidate();
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            ensureSized(trigger, form);
            tapComponent(trigger);
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();

            Dialog dialog = dialogHolder[0];
            assertNotNull(dialog, "Dialog should be created when button is tapped");

            int titleBottom = form.getTitleArea().getAbsoluteY()
                    + form.getTitleArea().getHeight()
                    + form.getTitleArea().getStyle().getMarginBottom();

            assertTrue(titleBottom > 0, "Title area should occupy space to offset the dialog");
            assertTrue(actualContentTop[0] >= expectedTop[0], "Dialog content should start beneath the title area: expected >= "
                    + expectedTop[0] + " actual " + actualContentTop[0] + " titleBottom " + titleBottom);
            assertEquals(expectedLeft[0], actualLeft[0], "Dialog should be horizontally centered using calculated left margin");
            int horizontalTrim = 16;
            assertTrue(actualContentWidth[0] >= form.getWidth() - horizontalTrim,
                    "Dialog content should stretch to the available form width on phones: expected >= "
                            + (form.getWidth() - horizontalTrim) + " actual " + actualContentWidth[0]);
            assertTrue(actualContentWidth[0] <= form.getWidth(), "Dialog content should not exceed form width");
            int heightTolerance = 16;
            assertTrue(actualHeight[0] <= form.getHeight() + heightTolerance,
                    "Dialog height should not exceed the form height: available " + form.getHeight()
                            + " actual " + actualHeight[0]);
        } finally {
            Toolbar.setGlobalToolbar(originalGlobalToolbar);
        }
    }

    private void showPositionedDialog(Form form, Dialog[] dialogHolder, int[] expectedTop, int[] expectedLeft,
                                      int[] actualTop, int[] actualContentTop, int[] actualLeft, int[] actualContentWidth, int[] actualHeight) {
        Dialog dialog = new Dialog("Hello Dialog", new BorderLayout());
        dialog.add(BorderLayout.CENTER, BoxLayout.encloseY(new Label("Here is some text"), new Label("And Some More"), new Button("Cancel")));

        int contentWidth = form.getWidth()
                - dialog.getStyle().getHorizontalPadding()
                - dialog.getContentPane().getStyle().getHorizontalMargins()
                - dialog.getContentPane().getStyle().getHorizontalPadding();
        if (!Display.getInstance().isTablet()) {
            dialog.getContentPane().setPreferredW(contentWidth);
        }

        dialog.addShowListener(evt -> Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                DisplayTest.flushEdt();
                actualTop[0] = dialog.getAbsoluteY();
                actualContentTop[0] = dialog.getContentPane().getAbsoluteY();
                actualLeft[0] = dialog.getAbsoluteX();
                actualContentWidth[0] = dialog.getContentPane().getWidth();
                actualHeight[0] = dialog.getHeight();
                dialog.dispose();
            }
        }));

        int w = dialog.getDialogPreferredSize().getWidth();
        int h = dialog.getDialogPreferredSize().getHeight();

        int top = form.getTitleArea().getAbsoluteY()
                + form.getTitleArea().getHeight()
                + form.getTitleArea().getStyle().getMarginBottom();
        int left = (form.getWidth() - w) / 2;
        int bottom = form.getHeight() - top - h;

        bottom = Math.max(0, bottom);
        top = Math.max(0, top);

        expectedTop[0] = top;
        expectedLeft[0] = left;
        dialogHolder[0] = dialog;
        dialog.show(top, bottom, left, left);

    }

    private void configureTitleArea(Form form) {
        Dimension titleSize = new Dimension(form.getPreferredW(), 120);
        form.getTitleArea().setPreferredSize(titleSize);
        form.getTitleArea().getStyle().setMarginBottom(12);
    }

    private void ensureSized(Button button, Form form) {
        for (int i = 0; i < 5 && (button.getWidth() <= 0 || button.getHeight() <= 0); i++) {
            form.revalidate();
            flushSerialCalls();
        }
    }
}
