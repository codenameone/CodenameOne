package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.ChatInput;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// ChatInput screenshot: attach + voice + send affordances all visible.
/// Baselines the ChatInput / ChatInputField / ChatSendButton /
/// ChatAttachButton / ChatVoiceButton UIIDs in both modern theme
/// appearances (light + dark via DualAppearanceBaseTest).
public class ChatInputScreenshotTest extends DualAppearanceBaseTest {

    @Override
    protected String baseName() {
        return "ChatInput";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        ChatInput input = new ChatInput();
        // Setters install ActionListener -> button becomes visible. Use no-op
        // listeners; this is a layout test, not an interaction test.
        ActionListener noop = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
            }
        };
        input.setOnSend(noop);
        input.setOnAttach(noop);
        input.setOnVoice(noop);
        form.add(BorderLayout.SOUTH, input);
    }
}
