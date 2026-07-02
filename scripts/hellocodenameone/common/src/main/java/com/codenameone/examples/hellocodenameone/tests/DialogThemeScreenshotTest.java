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

        // Constrain the inline dialog to a centered card width so the screenshot
        // reads as a real dialog on wide screens (desktop / Mac native) instead of
        // a full-width strip. Mirrors the ~72% width a packed Dialog.show() now caps
        // to (dialogMaxWidthPercentInt); narrow phone/JS screens were already
        // card-width, so this only tightens the wide-screen render.
        // The cap goes on the SPAN LABEL, not the card container:
        // Component.setPreferredW would freeze the card's preferred HEIGHT at its
        // unwrapped value (clipping the message wherever the cap binds, as the
        // 375px JavaScript-port screen showed), while SpanLabel.setPreferredW
        // keeps the height dynamic -- it re-measures the wrapped rows -- and the
        // card's width simply follows its widest child.
        int cap = com.codename1.ui.Display.getInstance().getDisplayWidth() * 72 / 100;
        message.setPreferredW(cap
                - dialog.getStyle().getHorizontalPadding()
                - body.getStyle().getHorizontalPadding());
        Container center = new Container(new FlowLayout(Component.CENTER));
        center.add(dialog);
        form.add(center);
    }
}
