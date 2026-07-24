package com.codename1.flutter.scheduler;

/**
 * Top-level members of Flutter's {@code package:flutter/scheduler.dart} library
 * that the app references directly, mirrored as Java statics.
 *
 * <p>{@code timeDilation} slows every {@code AnimationController} in the app by
 * the given factor. It is a mutable top-level {@code double} in Flutter
 * (default {@code 1.0}); the transpiler routes both reads and writes of the
 * bare {@code timeDilation} identifier to this field.</p>
 */
public final class SchedulerLib {

    private SchedulerLib() {
    }

    /** Flutter's {@code scheduler.timeDilation}; 1.0 == real time. */
    public static double timeDilation = 1.0;
}
