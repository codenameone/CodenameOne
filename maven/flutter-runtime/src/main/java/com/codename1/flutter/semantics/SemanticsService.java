package com.codename1.flutter.semantics;

import com.codename1.flutter.TextDirection;

/**
 * Fires accessibility announcements — Flutter's {@code SemanticsService}. The
 * settings panel announces its open/close state; this runtime records the last
 * announcement so the platform accessibility layer (or a test) can observe it,
 * but performs no screen-reader I/O in this pass.
 */
public abstract class SemanticsService {

    private static volatile String lastAnnouncement;
    private static volatile String lastTooltip;

    private SemanticsService() {
    }

    /**
     * Dart's {@code SemanticsService.announce(message, textDirection,
     * {assertiveness})}. Records the message for observation.
     */
    public static void announce(String message, TextDirection textDirection, Object assertiveness) {
        lastAnnouncement = message;
    }

    /** Dart's {@code SemanticsService.tooltip(message)}. */
    public static void tooltip(String message) {
        lastTooltip = message;
    }

    /** The most recently announced message, or null. */
    public static String lastAnnouncement() {
        return lastAnnouncement;
    }

    /** The most recently announced tooltip, or null. */
    public static String lastTooltip() {
        return lastTooltip;
    }
}
