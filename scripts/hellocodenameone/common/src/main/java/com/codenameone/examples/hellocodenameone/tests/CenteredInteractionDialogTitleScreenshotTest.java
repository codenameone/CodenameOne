package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.InteractionDialog;
import com.codename1.components.SpanLabel;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.UIManager;

/**
 * Screenshot coverage for a real {@link InteractionDialog} using the
 * centered-title runtime flag. The dialog remains at its preferred packed size
 * in the middle of the host form.
 */
public class CenteredInteractionDialogTitleScreenshotTest extends DualAppearanceBaseTest {
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=CenteredInteractionDialogTitle status=SKIPPED reason=phone-dialog-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "CenteredInteractionDialogTitle";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected boolean useTexturedBackdrop() {
        return true;
    }

    @Override
    protected void populate(Form form, String suffix) {
        InteractionDialog dialog = new InteractionDialog("Location Access", new BorderLayout());
        dialog.setTitleCentered(true);
        dialog.configureCommands(new Command[]{
                new Command("Not Now"),
                new Command("Allow")
        }, true);

        SpanLabel message = new SpanLabel(
                "Allow location access while you are using the application?");
        message.setUIID("DialogBody");
        int maxPercent = UIManager.getInstance().getThemeConstant("dialogMaxWidthPercentInt", 72);
        int maxWidth = Display.getInstance().getDisplayWidth() * maxPercent / 100;
        message.setPreferredW(maxWidth
                - dialog.getStyle().getHorizontalPadding()
                - dialog.getContentPane().getStyle().getHorizontalPadding());
        dialog.addComponent(BorderLayout.CENTER, message);

        Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        center.add(dialog);
        form.add(BorderLayout.CENTER, center);
    }
}
