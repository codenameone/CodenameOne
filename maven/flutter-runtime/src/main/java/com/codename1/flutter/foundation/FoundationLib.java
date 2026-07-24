package com.codename1.flutter.foundation;

import com.codename1.flutter.TargetPlatform;

/**
 * Top-level members of Flutter's {@code package:flutter/foundation.dart} that
 * the app references directly, mirrored as Java statics.
 *
 * <p>{@code defaultTargetPlatform} reports the platform the app is running on.
 * It is resolved once from the Codename One runtime and cached.</p>
 */
public final class FoundationLib {

    private FoundationLib() {
    }

    /** Flutter's {@code defaultTargetPlatform}. */
    public static final TargetPlatform defaultTargetPlatform = detect();

    private static TargetPlatform detect() {
        try {
            String p = com.codename1.ui.Display.getInstance().getPlatformName();
            if (p != null) {
                p = p.toLowerCase();
                if (p.startsWith("ios")) {
                    return TargetPlatform.iOS;
                }
                if (p.startsWith("and")) {
                    return TargetPlatform.android;
                }
                if (p.startsWith("mac")) {
                    return TargetPlatform.macOS;
                }
                if (p.startsWith("win")) {
                    return TargetPlatform.windows;
                }
            }
        } catch (Throwable t) {
            // fall through to a sensible default when no runtime is available
        }
        return TargetPlatform.android;
    }
}
