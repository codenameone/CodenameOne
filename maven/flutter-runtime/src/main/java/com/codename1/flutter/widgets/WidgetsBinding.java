package com.codename1.flutter.widgets;

import com.codename1.flutter.Brightness;
import com.codename1.flutter.scheduler.SchedulerBinding;

/**
 * The glue binding the widget layer to the engine — Flutter's
 * {@code WidgetsBinding}. new_gallery reaches the ambient brightness through
 * {@code WidgetsBinding.instance.platformDispatcher.platformBrightness}.
 *
 * <p>Dart's {@code static get instance} is emitted as a Java static field
 * reference (accessed as {@code WidgetsBinding.instance}, no call), mirroring
 * {@link SchedulerBinding#instance}.</p>
 */
public final class WidgetsBinding {

    /**
     * The engine's view of the platform, exposed to match the Dart chain
     * {@code platformDispatcher.platformBrightness}. {@code platformBrightness}
     * is a public field so the (dynamic in Dart) {@code .platformBrightness}
     * access resolves as a member read.
     */
    public static final class PlatformDispatcher {

        /** The system-wide brightness. Defaults to light in this runtime. */
        public Brightness platformBrightness = Brightness.light;

        /** The primary display's device-pixel ratio. */
        public double devicePixelRatio = 1.0;
    }

    /** Dart's {@code WidgetsBinding.instance}. */
    public static final WidgetsBinding instance = new WidgetsBinding();

    private final PlatformDispatcher platformDispatcher = new PlatformDispatcher();

    private WidgetsBinding() {
    }

    /** Dart's {@code platformDispatcher} getter. */
    public PlatformDispatcher platformDispatcher() {
        return platformDispatcher;
    }

    /** Dart's {@code window} getter (legacy alias for the dispatcher). */
    public Object window() {
        return platformDispatcher;
    }

    /** Dart's {@code addPostFrameCallback}: run after the current frame. */
    public void addPostFrameCallback(dart.runtime.Funcs.VoidFunc1<Object> callback) {
        SchedulerBinding.instance.addPostFrameCallback(callback);
    }

    /** Dart's {@code addObserver}: register a binding observer. A no-op here. */
    public void addObserver(Object observer) {
    }

    /** Dart's {@code removeObserver}: unregister a binding observer. A no-op. */
    public void removeObserver(Object observer) {
    }
}
