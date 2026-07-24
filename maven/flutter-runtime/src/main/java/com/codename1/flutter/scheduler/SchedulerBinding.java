package com.codename1.flutter.scheduler;

import com.codename1.ui.Display;

import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * The singleton that drives frame scheduling — Flutter's {@code SchedulerBinding}.
 * new_gallery reaches it through {@code SchedulerBinding.instance} to register
 * post-frame callbacks (e.g. feature-discovery overlays that must measure a
 * widget after its first layout).
 *
 * <p>Callbacks are dispatched on the Codename One EDT after the current event
 * loop via {@link Display#callSerially}, which is the closest analogue to
 * "after the current frame". The {@code FrameTiming} argument Flutter passes is
 * approximated with a zero {@link Duration}.</p>
 */
public final class SchedulerBinding {

    /**
     * Dart's {@code SchedulerBinding.instance}. A Dart {@code static get}
     * accessor is emitted as a Java static field reference, so this is a field
     * rather than a method.
     */
    public static final SchedulerBinding instance = new SchedulerBinding();

    private int nextCallbackId = 1;

    private SchedulerBinding() {
    }

    /** Dart's {@code addPostFrameCallback}: run {@code callback} after this frame. */
    public void addPostFrameCallback(final Funcs.VoidFunc1<Object> callback) {
        dispatch(callback);
    }

    /**
     * Dart's {@code scheduleFrameCallback}: schedule a transient frame callback
     * and return its id. {@code rescheduling} is accepted for API shape.
     */
    public int scheduleFrameCallback(final Object callback, Boolean rescheduling) {
        dispatch(callback);
        return nextCallbackId++;
    }

    /** Dart's {@code scheduleFrame}: request a new frame. A no-op in this pass. */
    public void scheduleFrame() {
    }

    @SuppressWarnings("unchecked")
    private void dispatch(final Object callback) {
        if (callback == null) {
            return;
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (callback instanceof Funcs.VoidFunc1) {
                    ((Funcs.VoidFunc1<Object>) callback).call(Duration.zero);
                } else if (callback instanceof Runnable) {
                    ((Runnable) callback).run();
                }
            }
        };
        try {
            if (Display.isInitialized()) {
                Display.getInstance().callSerially(r);
            } else {
                r.run();
            }
        } catch (Throwable t) {
            // Best effort: a callback throwing must not abort scheduling.
        }
    }
}
