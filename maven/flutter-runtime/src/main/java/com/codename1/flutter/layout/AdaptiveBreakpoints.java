package com.codename1.flutter.layout;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Display;

/**
 * Window-size breakpoint helpers — the {@code adaptive_breakpoints} package.
 * {@link #getWindowType} buckets the current window by its LOGICAL width (CN1
 * device pixels / {@link Dp#scale()}), matching the package's Material window
 * size classes. Apps gate responsive layouts on this — the gallery treats
 * {@code >= medium} as "desktop", so a phone-sized window must report a small
 * bucket to get the mobile layout.
 */
public final class AdaptiveBreakpoints {

    private AdaptiveBreakpoints() {
    }

    public static AdaptiveWindowType getWindowType(BuildContext context) {
        double width = 360;
        try {
            if (Display.isInitialized()) {
                double scale = Dp.scale();
                if (scale > 0) {
                    width = Display.getInstance().getDisplayWidth() / scale;
                }
            }
        } catch (Throwable t) {
            // headless / no display: fall back to a phone-sized default
        }
        if (width < 600) {
            return AdaptiveWindowType.xsmall;
        }
        if (width < 1024) {
            return AdaptiveWindowType.small;
        }
        if (width < 1440) {
            return AdaptiveWindowType.medium;
        }
        if (width < 1920) {
            return AdaptiveWindowType.large;
        }
        return AdaptiveWindowType.xlarge;
    }
}
