package com.codename1.share;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Image;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareServiceResultDeliveryTest extends UITestBase {

    @FormTest
    void finishDeliversSharedToResultExactlyOnce() {
        TrackingShare svc = new TrackingShare();
        List<ShareResult> received = new ArrayList<>();
        svc.setShareResultListener(received::add);

        svc.finish();
        // calling finish() again must not deliver a second result
        svc.finish();

        assertEquals(1, received.size(),
                "finish() should deliver a result exactly once");
        ShareResult r = received.get(0);
        assertTrue(r.isSharedTo());
        assertEquals("Tracking", r.getPackageName(),
                "default SHARED_TO carries the command name as identifier");
    }

    @FormTest
    void deliverResultRunsBeforeFinishFallback() {
        TrackingShare svc = new TrackingShare();
        List<ShareResult> received = new ArrayList<>();
        svc.setShareResultListener(received::add);

        svc.publishFailure("smtp down");
        svc.finish();

        assertEquals(1, received.size(),
                "explicit deliverResult must short-circuit the finish() fallback");
        assertTrue(received.get(0).isFailed());
        assertEquals("smtp down", received.get(0).getError());
    }

    @FormTest
    void noListenerStillTracksDeliveryStateInternally() {
        // A service without a listener should still respect "deliver once"
        // semantics, so that adding a listener after the fact does not
        // see stale results.
        TrackingShare svc = new TrackingShare();
        svc.finish();
        List<ShareResult> received = new ArrayList<>();
        svc.setShareResultListener(received::add);
        svc.finish();
        // resultDelivered was cleared by setShareResultListener;
        // second finish() now delivers once.
        assertEquals(1, received.size());
    }

    private static class TrackingShare extends ShareService {
        TrackingShare() {
            super("Tracking", (Image) null);
        }

        @Override
        public void share(String text) { /* unused for these tests */ }

        @Override
        public boolean canShareImage() { return true; }

        void publishFailure(String msg) {
            deliverResult(ShareResult.failed(msg));
        }
    }
}
