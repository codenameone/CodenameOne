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
/// The last of the VR / 360 tests calls {@link #restorePortrait} once its
/// capture is done, so the rest of the suite runs in portrait exactly as it
/// does on master (in particular the video tests, which are sensitive to both
/// orientation and to a GPU-heavy neighbor - hence these tests sit right after
/// the orientation test, far from them).
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

    /// Restores portrait (where the platform can rotate) and invokes
    /// {@code onDone} once the rotation has landed, so the next test starts in
    /// the same orientation the rest of the suite expects. A no-op that runs
    /// {@code onDone} immediately on platforms that cannot rotate.
    static void restorePortrait(Form form, Runnable onDone) {
        if (!CN.canForceOrientation()) {
            onDone.run();
            return;
        }
        CN.lockOrientation(true); // true == portrait
        awaitPortrait(form, POLL_ATTEMPTS, onDone);
    }

    private static void awaitPortrait(final Form form, final int attemptsLeft, final Runnable onDone) {
        form.revalidate();
        boolean portrait = form.getHeight() >= form.getWidth();
        if (portrait || attemptsLeft <= 0) {
            onDone.run();
            return;
        }
        UITimer.timer(POLL_INTERVAL_MS, false, form, new Runnable() {
            public void run() {
                awaitPortrait(form, attemptsLeft - 1, onDone);
            }
        });
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
