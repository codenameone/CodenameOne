package com.codename1.ui.events;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import org.junit.jupiter.api.Assertions;

public class ComponentStateChangeEventTest extends UITestBase {

    @FormTest
    public void testComponentStateChangeEvent() {
        Component cmp = new Container();
        ComponentStateChangeEvent event = new ComponentStateChangeEvent(cmp, true);

        Assertions.assertEquals(cmp, event.getSource());
        Assertions.assertTrue(event.isInitialized());

        event = new ComponentStateChangeEvent(cmp, false);
        Assertions.assertFalse(event.isInitialized());
    }

}
