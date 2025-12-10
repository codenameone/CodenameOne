package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.messaging.Message;
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.events.ActionEvent;
import org.junit.jupiter.api.Assertions;

public class SendMessageSample2756Test extends UITestBase {

    @FormTest
    public void testSendMessage() {
        Form hi = new Form("Send Message Sample", BoxLayout.y());
        Button b = new Button("Send Message");
        b.addActionListener(e -> {
            String email = "nospam@nospam.com";
            Message message = new Message("");
            Display.getInstance().sendMessage(new String[]{email}, "", message);
        });
        hi.add(b);
        hi.show();

        // Simulate button click
        implementation.pressComponent(b);
        implementation.releaseComponent(b);

        String[] recipients = implementation.getLastSentMessageRecipients();
        Assertions.assertNotNull(recipients, "Recipients should not be null");
        Assertions.assertEquals(1, recipients.length, "Should have 1 recipient");
        Assertions.assertEquals("nospam@nospam.com", recipients[0]);
    }
}
