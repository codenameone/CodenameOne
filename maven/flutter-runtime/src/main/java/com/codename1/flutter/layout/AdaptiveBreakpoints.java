package com.codename1.flutter.layout;

import com.codename1.flutter.BuildContext;

/**
 * Window-size breakpoint helpers — the {@code adaptive_breakpoints} package.
 * {@link #getWindowType} returns the {@link AdaptiveWindowType} bucket for the
 * current window; deferred, it reports {@code medium} (a desktop-ish default).
 */
public final class AdaptiveBreakpoints {

    private AdaptiveBreakpoints() {
    }

    public static AdaptiveWindowType getWindowType(BuildContext context) {
        return AdaptiveWindowType.medium;
    }
}
