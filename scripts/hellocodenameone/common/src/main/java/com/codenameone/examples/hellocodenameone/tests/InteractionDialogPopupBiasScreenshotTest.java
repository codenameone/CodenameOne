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
    private InteractionDialog preferredSideDialog;
    private InteractionDialog fallbackSideDialog;

    @Override
    public boolean runTest() {
        Form form = createForm("InteractionDialog Popup Bias", new BorderLayout(), "InteractionDialogPopupBias");
        Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        Label target = new Label("CENTER TARGET");
        target.getAllStyles().setPadding(2, 2, 2, 2);
        center.add(target);
        form.add(BorderLayout.CENTER, center);
        form.add(BorderLayout.NORTH, new Label("Top popup: bias works (prefers top when possible)"));
        form.add(BorderLayout.SOUTH, new Label("Bottom popup: bias fallback (top requested but unavailable)"));
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        preferredSideDialog = createDialog("bias=true -> prefers top");
        fallbackSideDialog = createDialog("bias=true -> falls back to bottom");
        int width = parent.getWidth();
        int height = parent.getHeight();

        // Center target: enough room above and below, so bias=true should place popup above.
        Rectangle centerTarget = new Rectangle(width / 2 - 45, height / 2 - 12, 90, 24);
        preferredSideDialog.showPopupDialog(centerTarget, true);

        // Near top target: not enough room above, so bias=true should fall back below.
        Rectangle topEdgeTarget = new Rectangle(width / 2 - 45, 4, 90, 24);
        fallbackSideDialog.showPopupDialog(topEdgeTarget, true);
        UITimer.timer(600, false, parent, run);
    }

    @Override
    public void cleanup() {
        if (preferredSideDialog != null && preferredSideDialog.isShowing()) {
            preferredSideDialog.dispose();
        }
        if (fallbackSideDialog != null && fallbackSideDialog.isShowing()) {
            fallbackSideDialog.dispose();
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
