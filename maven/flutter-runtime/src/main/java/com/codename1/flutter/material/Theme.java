package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;

/**
 * Theme lookup: {@link #of(BuildContext)} walks up the element tree to the
 * nearest {@link MaterialApp} and returns its EFFECTIVE ThemeData (theme vs
 * darkTheme per themeMode), falling back to a default ThemeData when no
 * themed ancestor exists.
 */
public final class Theme {

    private Theme() {
    }

    public static ThemeData of(BuildContext context) {
        MaterialApp app = context == null
                ? null
                : context.findAncestorWidgetOfExactType(MaterialApp.class);
        if (app != null) {
            return app.effectiveTheme();
        }
        return new ThemeData();
    }
}
