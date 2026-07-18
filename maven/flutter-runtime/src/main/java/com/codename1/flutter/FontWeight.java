package com.codename1.flutter;

/**
 * Font weights w100..w900 with Flutter's {@code normal} (w400) and
 * {@code bold} (w700) aliases. CN1 fonts only distinguish plain/bold, so
 * weights of w600 and up render bold.
 */
public enum FontWeight {
    w100, w200, w300, w400, w500, w600, w700, w800, w900;

    public static final FontWeight normal = w400;
    public static final FontWeight bold = w700;

    /**
     * Whether this weight maps to CN1's bold style.
     */
    public boolean isBold() {
        return ordinal() >= w600.ordinal();
    }
}
