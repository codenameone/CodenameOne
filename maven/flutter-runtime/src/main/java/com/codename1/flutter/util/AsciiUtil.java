package com.codename1.flutter.util;

/**
 * ASCII string helpers — package:collection's {@code compareAsciiUpperCase}.
 * The settings page sorts locale display names with it.
 */
public final class AsciiUtil {

    private AsciiUtil() {
    }

    /**
     * Dart's {@code compareAsciiUpperCase(a, b)}: compares two strings by
     * upper-casing ASCII letters only (a-z -&gt; A-Z), leaving all other code
     * units untouched. Returns a negative, zero, or positive int like
     * {@link String#compareTo}.
     */
    public static long compareAsciiUpperCase(String a, String b) {
        if (a == null) {
            return b == null ? 0 : -1;
        }
        if (b == null) {
            return 1;
        }
        int len = Math.min(a.length(), b.length());
        for (int i = 0; i < len; i++) {
            int ca = toUpperAscii(a.charAt(i));
            int cb = toUpperAscii(b.charAt(i));
            if (ca != cb) {
                return ca - cb;
            }
        }
        return a.length() - b.length();
    }

    private static int toUpperAscii(char c) {
        if (c >= 'a' && c <= 'z') {
            return c - ('a' - 'A');
        }
        return c;
    }
}
