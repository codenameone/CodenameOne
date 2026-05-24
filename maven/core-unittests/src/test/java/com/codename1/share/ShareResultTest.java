package com.codename1.share;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareResultTest {

    @Test
    void sharedTo_carriesPackageName() {
        ShareResult r = ShareResult.sharedTo("com.whatsapp");
        assertTrue(r.isSharedTo());
        assertFalse(r.isDismissed());
        assertFalse(r.isFailed());
        assertEquals(ShareResult.STATUS_SHARED_TO, r.getStatus());
        assertEquals("com.whatsapp", r.getPackageName());
        assertNull(r.getError());
    }

    @Test
    void sharedTo_acceptsNullPackageName() {
        // Older Android and Web Share API cannot expose the chosen
        // target; the listener still fires so app code can proceed.
        ShareResult r = ShareResult.sharedTo(null);
        assertTrue(r.isSharedTo());
        assertNull(r.getPackageName());
    }

    @Test
    void dismissed_hasNoPackageOrError() {
        ShareResult r = ShareResult.dismissed();
        assertTrue(r.isDismissed());
        assertFalse(r.isSharedTo());
        assertFalse(r.isFailed());
        assertEquals(ShareResult.STATUS_DISMISSED, r.getStatus());
        assertNull(r.getPackageName());
        assertNull(r.getError());
    }

    @Test
    void failed_carriesErrorMessage() {
        ShareResult r = ShareResult.failed("attachment too large");
        assertTrue(r.isFailed());
        assertFalse(r.isSharedTo());
        assertFalse(r.isDismissed());
        assertEquals(ShareResult.STATUS_FAILED, r.getStatus());
        assertEquals("attachment too large", r.getError());
        assertNull(r.getPackageName());
    }

    @Test
    void toString_isDeterministicForEachStatus() {
        assertEquals("ShareResult{SHARED_TO com.example}",
                ShareResult.sharedTo("com.example").toString());
        assertEquals("ShareResult{DISMISSED}",
                ShareResult.dismissed().toString());
        assertEquals("ShareResult{FAILED oops}",
                ShareResult.failed("oops").toString());
    }
}
