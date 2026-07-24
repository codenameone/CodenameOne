package com.codename1.flutter.cupertino;

import com.codename1.flutter.Brightness;
import com.codename1.flutter.Color;

/**
 * The resolved iOS theme, mirroring Flutter's {@code CupertinoThemeData}:
 * brightness, a few key colors and the {@link CupertinoTextThemeData}. Named
 * constructor parameters and {@link #copyWith} arrive as setters / a
 * positional copy respectively.
 */
public class CupertinoThemeData {

    private Brightness brightness;
    private Color primaryColor;
    private Color primaryContrastingColor;
    private Color scaffoldBackgroundColor;
    private Color barBackgroundColor;
    private CupertinoTextThemeData textTheme;

    public void brightness(Brightness v) {
        this.brightness = v;
    }

    public void primaryColor(Color v) {
        this.primaryColor = v;
    }

    public void primaryContrastingColor(Color v) {
        this.primaryContrastingColor = v;
    }

    public void scaffoldBackgroundColor(Color v) {
        this.scaffoldBackgroundColor = v;
    }

    public void barBackgroundColor(Color v) {
        this.barBackgroundColor = v;
    }

    public void textTheme(CupertinoTextThemeData v) {
        this.textTheme = v;
    }

    public Brightness brightness() {
        return brightness;
    }

    public Color primaryColor() {
        return primaryColor != null ? primaryColor : CupertinoColors.systemBlue;
    }

    public Color scaffoldBackgroundColor() {
        return scaffoldBackgroundColor != null ? scaffoldBackgroundColor : CupertinoColors.systemBackground;
    }

    public Color barBackgroundColor() {
        return barBackgroundColor != null ? barBackgroundColor : CupertinoColors.systemBackground;
    }

    public CupertinoTextThemeData textTheme() {
        return textTheme != null ? textTheme : new CupertinoTextThemeData();
    }

    /**
     * Returns a copy with the supplied (non-null) fields overridden;
     * parameters follow the order declared in the Dart stub.
     */
    public CupertinoThemeData copyWith(Brightness brightness, Color primaryColor,
                                       Color primaryContrastingColor, Color scaffoldBackgroundColor,
                                       Color barBackgroundColor, CupertinoTextThemeData textTheme) {
        CupertinoThemeData c = new CupertinoThemeData();
        c.brightness = brightness != null ? brightness : this.brightness;
        c.primaryColor = primaryColor != null ? primaryColor : this.primaryColor;
        c.primaryContrastingColor = primaryContrastingColor != null ? primaryContrastingColor : this.primaryContrastingColor;
        c.scaffoldBackgroundColor = scaffoldBackgroundColor != null ? scaffoldBackgroundColor : this.scaffoldBackgroundColor;
        c.barBackgroundColor = barBackgroundColor != null ? barBackgroundColor : this.barBackgroundColor;
        c.textTheme = textTheme != null ? textTheme : this.textTheme;
        return c;
    }
}
