package com.codename1.flutter.cupertino;

import com.codename1.flutter.TextStyle;

/**
 * The iOS default text styles, mirroring Flutter's {@code CupertinoTextThemeData}.
 * Each getter returns a fresh {@link TextStyle} carrying the default logical
 * size for that role (TextStyle is a mutable write-once config, so fresh
 * instances avoid leaking a call site's mutation).
 */
public class CupertinoTextThemeData {

    private static TextStyle sized(double size) {
        TextStyle t = new TextStyle();
        t.fontSize(size);
        return t;
    }

    public TextStyle textStyle() {
        return sized(17);
    }

    public TextStyle actionTextStyle() {
        return sized(17);
    }

    public TextStyle navTitleTextStyle() {
        return sized(17);
    }

    public TextStyle navLargeTitleTextStyle() {
        return sized(34);
    }

    public TextStyle tabLabelTextStyle() {
        return sized(10);
    }

    public TextStyle pickerTextStyle() {
        return sized(21);
    }
}
