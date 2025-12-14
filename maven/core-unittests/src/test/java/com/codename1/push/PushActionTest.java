package com.codename1.push;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class PushActionTest extends UITestBase {

    @FormTest
    public void testPushActionConstructorsAndGetters() {
        PushAction action1 = new PushAction("id1", "title1", "icon1");
        Assertions.assertEquals("id1", action1.getId());
        Assertions.assertEquals("title1", action1.getTitle());
        Assertions.assertEquals("icon1", action1.getIcon());
        Assertions.assertNull(action1.getTextInputPlaceholder());
        Assertions.assertNull(action1.getTextInputButtonText());

        PushAction action2 = new PushAction("id2", "title2");
        Assertions.assertEquals("id2", action2.getId());
        Assertions.assertEquals("title2", action2.getTitle());
        Assertions.assertNull(action2.getIcon());

        PushAction action3 = new PushAction("title3");
        Assertions.assertEquals("title3", action3.getId());
        Assertions.assertEquals("title3", action3.getTitle());

        PushAction action4 = new PushAction("id4", "title4", "icon4", "placeholder", "buttonText");
        Assertions.assertEquals("id4", action4.getId());
        Assertions.assertEquals("title4", action4.getTitle());
        Assertions.assertEquals("icon4", action4.getIcon());
        Assertions.assertEquals("placeholder", action4.getTextInputPlaceholder());
        Assertions.assertEquals("buttonText", action4.getTextInputButtonText());
    }
}
