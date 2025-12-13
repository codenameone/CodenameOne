package com.codename1.messaging;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest extends UITestBase {

    @FormTest
    void testSendMessageDelegatesToDisplay() throws Exception {
        Message message = new Message("Body");
        message.setMimeType(Message.MIME_HTML);
        message.setAttachment("file://app/data/report.txt");
        message.setAttachmentMimeType(Message.MIME_TEXT);

        String[] recipients = new String[]{"alice@example.com", "bob@example.com"};
        Message.sendMessage(recipients, "Greetings", message);

        assertArrayEquals(recipients, implementation.getLastSentMessageRecipients());
        assertEquals("Greetings", implementation.getLastSentMessageSubject());
        assertSame(message, implementation.getLastSentMessage());
        assertEquals(Message.MIME_HTML, message.getMimeType());
    }

    @FormTest
    void testAttachmentsAndCloudFailureFlag() throws Exception {
        Message message = new Message("Hello");
        message.setAttachment("file://app/photo.jpg");
        message.setAttachmentMimeType(Message.MIME_IMAGE_JPG);

        Map<String, String> attachments = message.getAttachments();
        assertEquals(1, attachments.size());
        assertEquals(Message.MIME_IMAGE_JPG, attachments.get("file://app/photo.jpg"));

        message.setAttachment("file://app/icon.png");
        message.setAttachmentMimeType(Message.MIME_IMAGE_PNG);
        Map<String, String> updated = message.getAttachments();
        assertEquals(2, updated.size());
        assertEquals(Message.MIME_IMAGE_PNG, updated.get("file://app/icon.png"));

        updated.put("file://app/extra.txt", Message.MIME_TEXT);
        assertEquals(Message.MIME_TEXT, message.getAttachments().get("file://app/extra.txt"));

        message.setCloudMessageFailSilently(true);
        assertTrue(message.isCloudMessageFailSilently());
    }
}
