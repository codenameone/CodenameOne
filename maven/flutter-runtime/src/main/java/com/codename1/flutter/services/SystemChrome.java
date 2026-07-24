package com.codename1.flutter.services;

import java.util.List;

/**
 * System chrome controls, mirroring Flutter's {@code SystemChrome}. No-ops:
 * CN1 owns orientation and overlay behaviour through its own APIs.
 */
public abstract class SystemChrome {

    private SystemChrome() {
    }

    public static void setSystemUIOverlayStyle(SystemUiOverlayStyle style) {
        // no-op
    }

    public static void setPreferredOrientations(List<?> orientations) {
        // no-op
    }

    public static void setEnabledSystemUIMode(Object mode) {
        // no-op
    }
}
