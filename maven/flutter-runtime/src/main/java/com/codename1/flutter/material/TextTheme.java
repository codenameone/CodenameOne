package com.codename1.flutter.material;

import com.codename1.flutter.TextStyle;

/**
 * Material default text styles (M1 subset). Fresh TextStyle instances are
 * returned on every call because TextStyle is a mutable write-once config
 * object; sharing instances would let one call site's mutation leak into
 * another's.
 */
public class TextTheme {

    /**
     * Material headlineMedium: 28lp.
     */
    public TextStyle headlineMedium() {
        TextStyle t = new TextStyle();
        t.fontSize(28);
        return t;
    }

    /**
     * Material bodyMedium: 14lp.
     */
    public TextStyle bodyMedium() {
        TextStyle t = new TextStyle();
        t.fontSize(14);
        return t;
    }

    /**
     * Material titleLarge: 22lp.
     */
    public TextStyle titleLarge() {
        TextStyle t = new TextStyle();
        t.fontSize(22);
        return t;
    }
}
