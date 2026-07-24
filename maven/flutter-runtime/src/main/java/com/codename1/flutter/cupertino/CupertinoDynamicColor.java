package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;

/**
 * A {@link Color} that, in Flutter, resolves to a different concrete color
 * depending on the ambient {@code CupertinoTheme} brightness, accessibility
 * contrast and elevation. This runtime is single-appearance, so
 * {@link #resolveFrom(BuildContext)} returns this same color — enough for the
 * demos, which call {@code resolveFrom(context)} purely to obtain a plain
 * Color.
 */
public class CupertinoDynamicColor extends Color {

    public CupertinoDynamicColor(long value) {
        super(value);
    }

    /**
     * Resolves against the given context; this single-appearance runtime
     * returns the color unchanged.
     */
    public Color resolveFrom(BuildContext context) {
        return this;
    }
}
