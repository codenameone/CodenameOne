package com.codename1.push;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;
import com.codename1.junit.FormTest;

public class PushActionCategoryTest extends UITestBase {

    @FormTest
    public void testPushActionCategory() {
        // Constructor: PushActionCategory(String id, PushAction... actions)
        PushActionCategory category = new PushActionCategory("id1");
        Assertions.assertEquals("id1", category.getId());
        // getActions returns PushAction[]
        Assertions.assertNotNull(category.getActions());
        Assertions.assertEquals(0, category.getActions().length);

        PushAction action = new PushAction("act1", "Action 1", "icon");
        category = new PushActionCategory("id2", action);
        Assertions.assertEquals("id2", category.getId());
        Assertions.assertEquals(1, category.getActions().length);
        Assertions.assertEquals(action, category.getActions()[0]);

        // Test getAllActions
        PushAction action2 = new PushAction("act2", "Action 2", "icon");
        PushActionCategory category2 = new PushActionCategory("id3", action2);

        PushAction[] allActions = PushActionCategory.getAllActions(category, category2);
        Assertions.assertEquals(2, allActions.length);
    }
}
