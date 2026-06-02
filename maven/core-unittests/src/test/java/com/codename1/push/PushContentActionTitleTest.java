package com.codename1.push;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

/// Verifies the actionTitle parity property added to PushContent so push and local
/// notification actions surface the chosen action title consistently.
class PushContentActionTitleTest extends UITestBase {

    @FormTest
    void actionTitleRoundTrips() {
        PushContent.reset();
        assertFalse(PushContent.exists());

        PushContent.setActionId("reply");
        PushContent.setActionTitle("Reply");
        PushContent.setTextResponse("on my way");

        assertTrue(PushContent.exists());
        PushContent content = PushContent.get();
        assertNotNull(content);
        assertEquals("reply", content.getActionId());
        assertEquals("Reply", content.getActionTitle());
        assertEquals("on my way", content.getTextResponse());

        // get() pops the content; a second get must return null
        assertNull(PushContent.get());
        assertFalse(PushContent.exists());
    }
}
