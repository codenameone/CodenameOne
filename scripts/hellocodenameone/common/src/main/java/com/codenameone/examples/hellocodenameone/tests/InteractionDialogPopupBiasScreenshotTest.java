package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.InteractionDialog;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;

public class InteractionDialogPopupBiasScreenshotTest extends BaseTest {
    private InteractionDialog topPreferredDialog;
    private InteractionDialog bottomPreferredDialog;
    private InteractionDialog topFallbackDialog;

    @Override
    public boolean runTest() {
        Form form = createForm("InteractionDialog Popup Bias", new BorderLayout(), "InteractionDialogPopupBias");
        Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        Label target = new Label("CENTER TARGET");
        target.getAllStyles().setPadding(2, 2, 2, 2);
        center.add(target);
        form.add(BorderLayout.CENTER, center);
        form.add(BorderLayout.NORTH, new Label("3 dialogs: above target, below target, and top-fallback"));
        form.add(BorderLayout.SOUTH, new Label("CENTER TARGET helps verify relative arrow direction"));
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        topPreferredDialog = createDialog("bias=true -> above target");
        bottomPreferredDialog = createDialog("bias=false -> below target");
        topFallbackDialog = createDialog("bias=true near top -> fallback below");
        int width = parent.getWidth();
        int height = parent.getHeight();

        // Keep the targets far apart to avoid overlap in screenshot.
        Rectangle upperTarget = new Rectangle(width / 2 - 45, height / 3, 90, 24);
        Rectangle lowerTarget = new Rectangle(width / 2 - 45, (height * 2 / 3) - 24, 90, 24);
        Rectangle nearTopTarget = new Rectangle(width / 2 - 45, 6, 90, 24);
        topPreferredDialog.showPopupDialog(upperTarget, true);
        bottomPreferredDialog.showPopupDialog(lowerTarget, false);
        topFallbackDialog.showPopupDialog(nearTopTarget, true);
        UITimer.timer(600, false, parent, run);
    }

    @Override
    public void cleanup() {
        if (topPreferredDialog != null && topPreferredDialog.isShowing()) {
            topPreferredDialog.dispose();
        }
        if (bottomPreferredDialog != null && bottomPreferredDialog.isShowing()) {
            bottomPreferredDialog.dispose();
        }
        if (topFallbackDialog != null && topFallbackDialog.isShowing()) {
            topFallbackDialog.dispose();
        }
    }

    private InteractionDialog createDialog(String text) {
        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(BoxLayout.y());
        dialog.add(new Label(text));
        dialog.setDisposeWhenPointerOutOfBounds(false);
        return dialog;
    }
}
