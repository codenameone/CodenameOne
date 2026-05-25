/*
 * Test stub of com.codename1.io.Util. Only #decode is exercised by the
 * generated Routes class; provide a minimal URL-decoder implementation.
 */
package com.codename1.io;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public final class Util {
    private Util() { }

    public static String decode(String s, String encoding, boolean plusToSpace) {
        if (s == null) {
            return null;
        }
        try {
            return URLDecoder.decode(plusToSpace ? s : s.replace("+", "%2B"), encoding);
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}
