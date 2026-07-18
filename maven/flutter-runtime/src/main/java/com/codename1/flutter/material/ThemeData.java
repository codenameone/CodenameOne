package com.codename1.flutter.material;

import com.codename1.flutter.Brightness;
import com.codename1.flutter.Color;

/**
 * Material theme configuration: a color scheme, the default text theme and a
 * brightness. When no explicit color scheme is set one is derived from the
 * default M3 seed honoring the brightness. M4 maps the ACTIVE ThemeData onto
 * the CN1 UIManager through {@link ThemeDataAdapter}.
 */
public class ThemeData {

    private static final Color DEFAULT_SEED = new Color(0xFF6750A4);

    private ColorScheme colorScheme;
    private TextTheme textTheme = new TextTheme();
    private boolean useMaterial3 = true;
    private Brightness brightness;

    public void colorScheme(ColorScheme v) {
        this.colorScheme = v;
    }

    /**
     * Accepted for source compatibility; M1 always renders one way.
     */
    public void useMaterial3(boolean v) {
        this.useMaterial3 = v;
    }

    /**
     * The overall theme brightness; drives the default color scheme's tones
     * when no explicit scheme is set.
     */
    public void brightness(Brightness v) {
        this.brightness = v;
    }

    public boolean getUseMaterial3() {
        return useMaterial3;
    }

    /**
     * The declared brightness, defaulting to light.
     */
    public Brightness brightness() {
        return brightness == null ? Brightness.light : brightness;
    }

    public ColorScheme colorScheme() {
        if (colorScheme == null) {
            colorScheme = ColorScheme.fromSeed(DEFAULT_SEED, brightness);
        }
        return colorScheme;
    }

    public TextTheme textTheme() {
        return textTheme;
    }
}
