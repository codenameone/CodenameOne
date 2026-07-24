package com.codename1.flutter.foundation;

/**
 * Top-level {@code const} values of Flutter's
 * {@code package:flutter/foundation.dart} that new_gallery references directly,
 * mirrored as Java statics.
 */
public final class FoundationConstants {

    private FoundationConstants() {
    }

    /**
     * Flutter's {@code kIsWeb}: true only in a web (dart2js/dartdevc) build.
     * Codename One never targets the Flutter web backend, so this is always
     * {@code false}; the app uses it to gate web-only code paths.
     */
    public static final boolean kIsWeb = false;
}
