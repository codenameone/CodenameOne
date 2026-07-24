package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;

/**
 * Debug assertion helpers from Flutter's widgets layer. In release-style
 * transpiled output these are inert and always succeed.
 */
public abstract class Debug {

    private Debug() {
    }

    public static boolean debugCheckHasDirectionality(BuildContext context) {
        return true;
    }

    public static boolean debugCheckHasMediaQuery(BuildContext context) {
        return true;
    }
}
