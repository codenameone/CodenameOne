package com.codename1.push;

import com.codename1.xml.Element;
import com.codename1.xml.XMLParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PushBuilderTest {
    @Test
    void buildGeneratesBodyForStandardTypes() {
        PushBuilder builder = new PushBuilder().body("hello").type(1);
        assertEquals("hello", builder.build());

        builder = new PushBuilder().metaData("meta").type(2);
        assertEquals("meta", builder.build());

        builder = new PushBuilder().metaData("meta").body("body").type(3);
        assertEquals("meta;body", builder.build());

        builder = new PushBuilder().title("Title").body("Body").type(4);
        assertEquals("Title;Body", builder.build());

        builder = new PushBuilder().badge(9).type(6);
        assertEquals("9", builder.build());

        builder = new PushBuilder().badge(10).body("Body").type(101);
        assertEquals("10 Body", builder.build());

        builder = new PushBuilder().badge(3).title("Hi").body("There").type(102);
        assertEquals("3;Hi;There", builder.build());
    }

    @Test
    void isRichPushReflectsImageOrCategory() {
        PushBuilder builder = new PushBuilder();
        assertFalse(builder.isRichPush());
        assertEquals(0, builder.getType());

        builder.imageUrl("https://example.com/image.png");
        builder.type(4);
        assertTrue(builder.isRichPush());
        assertEquals(99, builder.getType());

        String xml = builder.body("Body").title("Title").build();
        Element element = parse(xml);
        assertEquals("4", element.getAttribute("type"));
        assertEquals("Title;Body", element.getAttribute("body"));
        assertEquals("https://example.com/image.png", element.getChildAt(0).getAttribute("src"));
    }

    @Test
    void richPushSupportsCategoryWithoutImage() {
        PushBuilder builder = new PushBuilder().body("content").type(1).category("alert");
        assertTrue(builder.isRichPush());
        assertEquals(99, builder.getType());

        Element element = parse(builder.build());
        assertEquals("alert", element.getAttribute("category"));
        assertEquals("content", element.getAttribute("body"));
        assertEquals(0, element.getNumChildren());
    }

    private Element parse(String xml) {
        XMLParser parser = new XMLParser();
        return parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
