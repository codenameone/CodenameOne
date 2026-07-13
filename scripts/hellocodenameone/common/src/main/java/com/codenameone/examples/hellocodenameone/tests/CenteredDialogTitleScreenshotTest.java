package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.UIManager;

/**
 * Screenshot coverage for a real {@link Dialog} using the centered-title
 * runtime flag. The dialog is laid out at its preferred packed size and placed
 * in the middle of the host form.
 */
public class CenteredDialogTitleScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "CenteredDialogTitle";
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
        Dialog dialog = new Dialog("Delete Conversation?", BoxLayout.y());
        dialog.setTitleCentered(true);
        dialog.placeButtonCommands(new Command[]{
                new Command("Cancel"),
                new Command("Delete")
        });

        SpanLabel message = new SpanLabel(
                "This conversation will be removed from all of your devices.");
        message.setUIID("DialogBody");
        int maxPercent = UIManager.getInstance().getThemeConstant("dialogMaxWidthPercentInt", 72);
        int maxWidth = Display.getInstance().getDisplayWidth() * maxPercent / 100;
        message.setPreferredW(maxWidth
                - dialog.getDialogStyle().getHorizontalPadding()
                - dialog.getContentPane().getStyle().getHorizontalPadding());
        dialog.add(message);

        Container center = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        center.add(dialog);
        form.add(BorderLayout.CENTER, center);
    }
}
