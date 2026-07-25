package com.codename1.flutter.animation;

import com.codename1.ui.CN;

import java.util.ArrayList;
import java.util.List;

/**
 * One clock for every running animation.
 *
 * <p>Each controller used to chain its own {@code setTimeout(16)}, so N concurrent
 * animations meant N timers, N wakeups and — because every tick marks its listeners
 * dirty and the build owner then revalidates the affected host — N rebuild/relayout
 * passes per frame instead of one. The gallery's home screen runs several at once (an
 * entrance animation per category item, a scale per carousel card), which is why it felt
 * heavy.</p>
 *
 * <p>Now controllers register here and are advanced together from a single timer: one
 * wakeup, one batch of listener notifications, and therefore one build flush per frame.
 * The driver stops itself when the last animation finishes, so an idle app has no timer
 * running at all.</p>
 */
final class FrameDriver {

    /** Target frame interval in milliseconds — 60fps. */
    private static final int FRAME_MS = 16;

    private static final List<AnimationController> RUNNING = new ArrayList<AnimationController>();
    private static boolean ticking;

    private FrameDriver() {
    }

    /** Adds a controller to the frame loop, starting the clock if it was idle. */
    static synchronized void add(AnimationController c) {
        if (!RUNNING.contains(c)) {
            RUNNING.add(c);
        }
        if (!ticking) {
            ticking = true;
            schedule();
        }
    }

    /** Removes a controller; the clock stops once none are left. */
    static synchronized void remove(AnimationController c) {
        RUNNING.remove(c);
    }

    private static void schedule() {
        CN.setTimeout(FRAME_MS, new Runnable() {
            @Override
            public void run() {
                frame();
            }
        });
    }

    private static void frame() {
        AnimationController[] due;
        synchronized (FrameDriver.class) {
            if (RUNNING.isEmpty()) {
                ticking = false;   // nothing left to animate; let the clock stop
                return;
            }
            due = RUNNING.toArray(new AnimationController[RUNNING.size()]);
        }
        // Advance every animation before anything rebuilds: the build owner coalesces
        // the dirty elements, so the whole frame costs one flush.
        for (int i = 0; i < due.length; i++) {
            try {
                due[i].advance();
            } catch (Throwable t) {
                // One misbehaving animation must not stop the clock for the others.
                com.codename1.flutter.FlutterErrorReport.record(t);
                remove(due[i]);
            }
        }
        synchronized (FrameDriver.class) {
            if (RUNNING.isEmpty()) {
                ticking = false;
                return;
            }
        }
        schedule();
    }
}
