/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package dart.core;

/**
 * dart:core statics the transpiler lowers calls on a core type name to, when the type's
 * own runtime class has no such member: Uri's component codecs, ArgumentError's checks
 * and identityHashCode.
 */
public final class CoreStatics {
    private static final String HEX = "0123456789ABCDEF";

    private CoreStatics() {
    }

    /** Uri.encodeComponent: everything but unreserved characters percent-encoded as UTF-8. */
    public static String encodeComponent(String s) {
        return encode(s, "-_.!~*'()", false);
    }

    /** Uri.encodeQueryComponent: form encoding, space as '+'. */
    public static String encodeQueryComponent(String s) {
        return encode(s, "-_.*", true);
    }

    public static String decodeQueryComponent(String s) {
        return DartUri.decodeComponent(s.replace('+', ' '));
    }

    static String encodeKeeping(String s, String keep) {
        return encode(s, keep, false);
    }

    private static String encode(String s, String keep, boolean plus) {
        StringBuilder sb = new StringBuilder();
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        for (byte bt : bytes) {
            int b = bt & 0xff;
            char c = (char) b;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || (b < 128 && keep.indexOf(c) >= 0)) {
                sb.append(c);
            } else if (plus && c == ' ') {
                sb.append('+');
            } else {
                sb.append('%').append(HEX.charAt(b >> 4)).append(HEX.charAt(b & 15));
            }
        }
        return sb.toString();
    }

    /** ArgumentError.checkNotNull(value[, name]). */
    public static <T> T checkNotNull(T value, String name) {
        if (value == null) {
            throw notNull(name);
        }
        return value;
    }

    /** ArgumentError.notNull(name). */
    public static ArgumentError notNull(String name) {
        return new ArgumentError(null, name, "Must not be null");
    }

    public static long identityHashCode(Object o) {
        return System.identityHashCode(o);
    }

    /** String.fromCharCode: one UTF-16 unit or a supplementary code point. */
    public static String fromCharCode(long code) {
        return new String(Character.toChars((int) code));
    }

    public static String fromCharCodes(Iterable<?> codes) {
        StringBuilder sb = new StringBuilder();
        for (Object o : codes) {
            sb.appendCodePoint(((Number) o).intValue());
        }
        return sb.toString();
    }

    /** Future.sync: runs f now; its result or its exception completes the future. */
    @SuppressWarnings("unchecked")
    public static dart.async.Future<Object> futureSync(dart.runtime.Funcs.Func0<Object> f) {
        try {
            Object r = f.call();
            return r instanceof dart.async.Future ? (dart.async.Future<Object>) r : dart.async.Future.<Object>value(r);
        } catch (RuntimeException t) {
            return dart.async.Future.<Object>error(t);
        }
    }

    /** Dart's {@code num.isNegative}: true for -0.0 and every value below zero, false for NaN. */
    public static boolean isNegative(double d) {
        return d < 0 || (d == 0 && Double.doubleToRawLongBits(d) != 0L);
    }

    /** Dart's {@code int.isNegative}. */
    public static boolean isNegative(long v) {
        return v < 0;
    }
}
