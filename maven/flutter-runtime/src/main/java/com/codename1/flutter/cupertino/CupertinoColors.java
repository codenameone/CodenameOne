package com.codename1.flutter.cupertino;

/**
 * The iOS system colors, mirroring Flutter's {@code CupertinoColors}. Each is
 * a {@link CupertinoDynamicColor}; this single-appearance runtime uses the
 * light-mode value (the demos resolve them via {@code resolveFrom(context)}
 * but never switch appearance).
 */
public final class CupertinoColors {

    private CupertinoColors() {
    }

    public static final CupertinoDynamicColor systemBackground = new CupertinoDynamicColor(0xFFFFFFFFL);
    public static final CupertinoDynamicColor label = new CupertinoDynamicColor(0xFF000000L);
    public static final CupertinoDynamicColor inactiveGray = new CupertinoDynamicColor(0xFF999999L);
    public static final CupertinoDynamicColor systemBlue = new CupertinoDynamicColor(0xFF007AFFL);
    public static final CupertinoDynamicColor systemGrey = new CupertinoDynamicColor(0xFF8E8E93L);
    public static final CupertinoDynamicColor activeBlue = new CupertinoDynamicColor(0xFF007AFFL);
    public static final CupertinoDynamicColor activeGreen = new CupertinoDynamicColor(0xFF34C759L);
    public static final CupertinoDynamicColor destructiveRed = new CupertinoDynamicColor(0xFFFF3B30L);
    public static final CupertinoDynamicColor white = new CupertinoDynamicColor(0xFFFFFFFFL);
    public static final CupertinoDynamicColor black = new CupertinoDynamicColor(0xFF000000L);
}
