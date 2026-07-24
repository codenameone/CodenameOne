package com.codename1.flutter.services;

/**
 * Describes the status/navigation-bar appearance, mirroring Flutter's
 * {@code SystemUiOverlayStyle}. Inert here (CN1 manages system chrome
 * separately); the {@code light}/{@code dark} presets are provided for API
 * shape.
 */
public final class SystemUiOverlayStyle {

    /** Overlays suited to a light (bright) background. */
    public static final SystemUiOverlayStyle light = new SystemUiOverlayStyle("light");

    /** Overlays suited to a dark background. */
    public static final SystemUiOverlayStyle dark = new SystemUiOverlayStyle("dark");

    private final String name;

    private SystemUiOverlayStyle(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "SystemUiOverlayStyle." + name;
    }
}
