package com.codename1.share;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for the SharedContent value object and its builder.
class SharedContentTest {

    @Test
    void emptyContentHasNoItems() {
        SharedContent c = SharedContent.builder().build();
        assertEquals(0, c.getItems().length);
        assertNull(c.getFirstItem());
        assertFalse(c.hasText());
        assertFalse(c.hasFiles());
        assertNull(c.getSubject());
    }

    @Test
    void textAndUrlClassifiedAsText() {
        SharedContent c = SharedContent.builder()
                .subject("Re: hi")
                .addText("hello")
                .addUrl("https://codenameone.com")
                .build();
        assertEquals("Re: hi", c.getSubject());
        assertEquals(2, c.getItems().length);
        assertTrue(c.hasText());
        assertFalse(c.hasFiles());
        assertEquals(SharedContent.TYPE_TEXT, c.getFirstItem().getType());
        assertEquals("hello", c.getFirstItem().getText());
        assertEquals(SharedContent.TYPE_URL, c.getItems()[1].getType());
    }

    @Test
    void imageAndFileClassifiedAsFiles() {
        SharedContent c = SharedContent.builder()
                .addImage("image/png", "file:///shared/pic.png", "pic.png")
                .addFile("application/pdf", "file:///shared/doc.pdf", "doc.pdf")
                .build();
        assertTrue(c.hasFiles());
        assertFalse(c.hasText());
        SharedContent.Item img = c.getItems()[0];
        assertEquals(SharedContent.TYPE_IMAGE, img.getType());
        assertEquals("image/png", img.getMimeType());
        assertEquals("file:///shared/pic.png", img.getFilePath());
        assertEquals("pic.png", img.getTitle());
        assertNull(img.getText());
        assertEquals(SharedContent.TYPE_FILE, c.getItems()[1].getType());
    }
}
