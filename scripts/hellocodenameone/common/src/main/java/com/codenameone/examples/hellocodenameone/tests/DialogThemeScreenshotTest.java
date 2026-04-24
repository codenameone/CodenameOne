package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/**
 * Screenshot coverage for Dialog / DialogBody / DialogTitle / dialog command
 * area. The dialog is rendered inline as a styled container (not as a modal
 * show()) so the screenshot captures the dialog chrome reliably without
 * waiting for modal animation to settle.
 */
public class DialogThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "DialogTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected boolean useTexturedBackdrop() {
        // Dialog may have translucent tints in the modern theme - paint
        // over a colourful texture so any see-through is visible.
        return true;
    }

    @Override
    protected void populate(Form form, String suffix) {
        Container dialog = new Container(new BorderLayout());
        dialog.setUIID("Dialog");

        Container body = new Container(BoxLayout.y());
        body.setUIID("DialogBody");

        Label title = new Label("Example dialog");
        title.setUIID("DialogTitle");
        body.add(title);

        SpanLabel message = new SpanLabel(
                "Are you sure you want to continue with this action? "
                + "This is a sample of a dialog body with a span label message.");
        body.add(message);

        Container commands = new Container(new FlowLayout(Component.RIGHT));
        commands.setUIID("DialogCommandArea");
        commands.add(new Button("Cancel")).add(new Button("OK"));

        dialog.add(BorderLayout.CENTER, body).add(BorderLayout.SOUTH, commands);
        form.add(dialog);
    }
}
