package com.codename1.push;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import org.junit.jupiter.api.Assertions;

public class PushContentTest extends UITestBase {

    @FormTest
    public void testPushContent() {
        // PushContent relies on Display properties "com.codename1.push.prop.*"
        Display d = Display.getInstance();
        d.setProperty("com.codename1.push.prop.title", "Test Title");
        d.setProperty("com.codename1.push.prop.body", "Test Body");
        d.setProperty("com.codename1.push.prop.type", "1");

        Assertions.assertTrue(PushContent.exists());

        PushContent content = PushContent.get();
        Assertions.assertNotNull(content);
        Assertions.assertEquals("Test Title", content.getTitle());
        Assertions.assertEquals("Test Body", content.getBody());
        Assertions.assertEquals(1, content.getType());

        // After get(), it should be cleared
        Assertions.assertFalse(PushContent.exists());
    }
}
