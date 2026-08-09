package com.codename1.flutter.animation;

/**
 * Public window onto the frame clock's counters, for the diagnostics in
 * {@code BuildOwner.frameStats()}. {@link FrameDriver} itself stays package-private:
 * nothing outside this package should be able to reach the clock, only to read it.
 */
public final class AnimationTrace {

    private AnimationTrace() {
    }

    /** Starts or stops recording clock ticks; resets the counters either way. */
    public static void trace(boolean on) {
        FrameDriver.trace(on);
    }

    /** The recorded numbers as JSON object members, without the enclosing braces. */
    public static String stats() {
        return FrameDriver.stats();
    }
}
