package com.codename1.initializr.model;

public final class ProjectOptions {
    public enum ThemeMode {
        LIGHT,
        DARK
    }

    public enum Accent {
        DEFAULT,
        TEAL,
        BLUE,
        ORANGE
    }

    public final ThemeMode themeMode;
    public final Accent accent;
    public final boolean roundedButtons;

    public ProjectOptions(ThemeMode themeMode, Accent accent, boolean roundedButtons) {
        this.themeMode = themeMode;
        this.accent = accent;
        this.roundedButtons = roundedButtons;
    }

    public static ProjectOptions defaults() {
        return new ProjectOptions(ThemeMode.LIGHT, Accent.DEFAULT, true);
    }
}
