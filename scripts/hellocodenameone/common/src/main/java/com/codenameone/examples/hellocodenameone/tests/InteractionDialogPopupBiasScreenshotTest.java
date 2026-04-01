package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.InteractionDialog;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.plaf.Style;
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
        Form form = createForm("InteractionDialog Popup", new BorderLayout(), "InteractionDialogPopupBias");
        Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        Label target = new Label("CENTER TARGET");
        target.getAllStyles().setPadding(2, 2, 2, 2);
        center.add(target);
        form.add(BorderLayout.CENTER, center);
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        topPreferredDialog = createDialog("T: bias=true");
        bottomPreferredDialog = createDialog("B: bias=false");
        topFallbackDialog = createDialog("F: top fallback");
        int width = parent.getWidth();
        int height = parent.getHeight();

        // Keep the targets far apart to avoid overlap in screenshot.
        Rectangle upperTarget = new Rectangle(width / 2 - 24, Math.max(40, (height / 2) - 130), 48, 20);
        Rectangle lowerTarget = new Rectangle(width / 2 - 24, Math.min(height - 60, (height / 2) + 90), 48, 20);
        Rectangle nearTopTarget = new Rectangle(width / 2 - 24, 2, 48, 20);
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
        dialog.setUIID("PopupDialogCN1SSBias");
        dialog.setDialogUIID("PopupDialogCN1SSBiasContent");
        dialog.setLayout(BoxLayout.y());
        Label label = new Label(text);
        label.getAllStyles().setFont(label.getUnselectedStyle().getFont().derive(label.getUnselectedStyle().getFont().getHeight() * 0.75f, Style.FONT_STYLE_PLAIN));
        dialog.add(label);
        dialog.setDisposeWhenPointerOutOfBounds(false);
        return dialog;
    }
}
