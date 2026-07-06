package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.util.UITimer;

/// Landscape helper for the immersive VR / 360 screenshot tests, which read
/// best in a wide frame. On a phone that can rotate this locks the device to
/// landscape and waits (via {@link #awaitLandscape}) for the rotation to
/// actually land before the capture. On platforms that cannot force
/// orientation - desktop, browser, tvOS - it is a no-op and the fixed window
/// aspect is captured as-is.
///
/// Orientation is intentionally left in landscape afterwards: these run as the
/// last screenshot tests and the tests that follow them capture off-screen
/// images (or make no capture at all), so nothing needs it restored.
final class LandscapeCapture {
    private static final int POLL_INTERVAL_MS = 100;
    // ~4s is comfortably longer than an emulator rotate + layout pass while
    // staying well under the per-test timeout; platforms that never rotate
    // short-circuit below and never wait this long.
    private static final int POLL_ATTEMPTS = 40;

    private LandscapeCapture() {
    }

    /// Locks the device to landscape where the platform supports it. Safe to
    /// call before the form is shown.
    static void lock() {
        if (CN.canForceOrientation()) {
            CN.lockOrientation(false); // false == landscape
        }
    }

    /// Invokes {@code onReady} once {@code form} is in landscape, or
    /// immediately on platforms that cannot rotate (or are already landscape).
    static void awaitLandscape(Form form, Runnable onReady) {
        awaitLandscape(form, POLL_ATTEMPTS, onReady);
    }

    private static void awaitLandscape(final Form form, final int attemptsLeft, final Runnable onReady) {
        form.revalidate();
        boolean landscape = form.getWidth() > form.getHeight();
        // Give up waiting on platforms that can't rotate (already at their
        // fixed aspect) so they capture immediately with no penalty.
        if (landscape || !CN.canForceOrientation() || attemptsLeft <= 0) {
            onReady.run();
            return;
        }
        UITimer.timer(POLL_INTERVAL_MS, false, form, new Runnable() {
            public void run() {
                awaitLandscape(form, attemptsLeft - 1, onReady);
            }
        });
    }
}
