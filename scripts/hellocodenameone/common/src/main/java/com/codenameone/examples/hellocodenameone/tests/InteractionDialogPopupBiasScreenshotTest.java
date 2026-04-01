package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.InteractionDialog;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;

public class InteractionDialogPopupBiasScreenshotTest extends BaseTest {
    private InteractionDialog northFallbackDialog;
    private InteractionDialog centerTopDialog;
    private InteractionDialog centerBottomDialog;
    private InteractionDialog southFallbackDialog;
    private Label northTarget;
    private Label centerTarget;
    private Label southTarget;

    @Override
    public boolean runTest() {
        Form form = createForm("InteractionDialog Popup", new BorderLayout(), "InteractionDialogPopupBias");
        northTarget = createTarget("NORTH TARGET");
        centerTarget = createTarget("CENTER TARGET");
        southTarget = createTarget("SOUTH TARGET");
        Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        center.add(centerTarget);
        form.add(BorderLayout.NORTH, wrapTarget(northTarget));
        form.add(BorderLayout.CENTER, center);
        form.add(BorderLayout.SOUTH, wrapTarget(southTarget));
        form.show();
        return true;
    }

    @Override
    protected void registerReadyCallback(Form parent, Runnable run) {
        northFallbackDialog = createDialog("1) North + bias=true -> fallback below");
        centerTopDialog = createDialog("2) Center + bias=true -> above");
        centerBottomDialog = createDialog("3) Center + bias=false -> below");
        southFallbackDialog = createDialog("4) South + bias=false -> fallback above");
        northFallbackDialog.showPopupDialog(northTarget, true);
        centerTopDialog.showPopupDialog(centerTarget, true);
        centerBottomDialog.showPopupDialog(centerTarget, false);
        southFallbackDialog.showPopupDialog(southTarget, false);
        UITimer.timer(600, false, parent, run);
    }

    @Override
    public void cleanup() {
        if (northFallbackDialog != null && northFallbackDialog.isShowing()) {
            northFallbackDialog.dispose();
        }
        if (centerTopDialog != null && centerTopDialog.isShowing()) {
            centerTopDialog.dispose();
        }
        if (centerBottomDialog != null && centerBottomDialog.isShowing()) {
            centerBottomDialog.dispose();
        }
        if (southFallbackDialog != null && southFallbackDialog.isShowing()) {
            southFallbackDialog.dispose();
        }
    }

    private InteractionDialog createDialog(String text) {
        InteractionDialog dialog = new InteractionDialog();
        dialog.setLayout(BoxLayout.y());
        dialog.add(new Label(text));
        dialog.setDisposeWhenPointerOutOfBounds(false);
        return dialog;
    }

    private Label createTarget(String text) {
        Label target = new Label(text);
        target.getAllStyles().setPadding(2, 2, 2, 2);
        return target;
    }

    private Container wrapTarget(Component target) {
        Container wrapper = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        wrapper.add(target);
        return wrapper;
    }
}
