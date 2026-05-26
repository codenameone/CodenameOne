package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.ChatInput;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;

/// Renders ChatInput with attach + voice + send affordances all visible.
/// Baselines the ChatInputField / ChatSendButton / ChatAttachButton /
/// ChatVoiceButton UIIDs.
public class ChatInputScreenshotTest extends BaseTest {

    @Override
    public boolean runTest() {
        Form form = createForm("ChatInput", new BorderLayout(), "ChatInput");
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

        Container body = (Container) form.getContentPane();
        body.add(BorderLayout.SOUTH, input);
        form.show();
        return true;
    }
}
