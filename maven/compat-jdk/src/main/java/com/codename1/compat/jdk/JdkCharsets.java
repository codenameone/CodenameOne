/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.compat.jdk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/// The members of `java.nio.charset.Charset` and `StandardCharsets` the
/// device lacks, and the `Charset` overloads of the classes that take an
/// encoding by name there.
///
/// The device's `Charset` is a name and nothing else: text is encoded and
/// decoded by the `String` constructors and `getBytes` that take the name.
/// So every method here turns a `Charset` into its name and calls those.
/// UTF-8, US-ASCII and ISO-8859-1 are the encodings a device has, and
/// `forName` of any other is an `IllegalArgumentException` naming it.
/// `StandardCharsets.UTF_16` and its two byte orders are not provided.
public final class JdkCharsets {

    private JdkCharsets() {
    }

    private static String fold(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - 'a' + 'A');
            }
            if (c != '-' && c != '_') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /// `Charset.forName`, for the three encodings a device has under the
    /// names and aliases the JDK knows them by.
    public static Charset forName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Null charset name");
        }
        String folded = fold(name);
        if ("UTF8".equals(folded)) {
            return StandardCharsets.UTF_8;
        }
        if ("USASCII".equals(folded) || "ASCII".equals(folded)) {
            return StandardCharsets.US_ASCII;
        }
        if ("ISO88591".equals(folded) || "LATIN1".equals(folded)) {
            return StandardCharsets.ISO_8859_1;
        }
        // A device's Charset cannot be made for another name: the JDK's
        // class is abstract over encoders the device does not have, so no
        // class of this module can extend both.
        throw new IllegalArgumentException("Charset " + name
                + " is not available on a device; UTF-8, US-ASCII and ISO-8859-1 are");
    }

    /// `Charset.defaultCharset()`: UTF-8, as on every current JDK.
    public static Charset defaultCharset() {
        return StandardCharsets.UTF_8;
    }

    /// `Charset.isSupported`: whether the platform can encode with `name`.
    public static boolean isSupported(String name) {
        try {
            "a".getBytes(name(forName(name)));
            return true;
        } catch (UnsupportedEncodingException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /// `charset.name()`.
    public static String name(Charset charset) {
        return charset.displayName();
    }

    /// `charset.toString()`, which is the name.
    public static String toString(Charset charset) {
        return charset.displayName();
    }

    private static IllegalArgumentException unsupported(Charset charset) {
        return new IllegalArgumentException("Unsupported charset: " + charset.displayName());
    }

    /// `new String(bytes, offset, length, charset)`.
    public static String decode(byte[] bytes, int offset, int length, Charset charset) {
        try {
            return new String(bytes, offset, length, charset.displayName());
        } catch (UnsupportedEncodingException e) {
            throw unsupported(charset);
        }
    }

    /// `new String(bytes, charset)`.
    public static String decode(byte[] bytes, Charset charset) {
        return decode(bytes, 0, bytes.length, charset);
    }

    /// `string.getBytes(charset)`.
    public static byte[] getBytes(String s, Charset charset) {
        try {
            return s.getBytes(charset.displayName());
        } catch (UnsupportedEncodingException e) {
            throw unsupported(charset);
        }
    }

    /// `byteArrayOutputStream.toString(charset)`.
    public static String toString(ByteArrayOutputStream out, Charset charset) {
        return decode(out.toByteArray(), charset);
    }

    /// `new InputStreamReader(in, charset)`.
    public static InputStreamReader reader(InputStream in, Charset charset) {
        try {
            return new InputStreamReader(in, charset.displayName());
        } catch (UnsupportedEncodingException e) {
            throw unsupported(charset);
        }
    }

    /// `new OutputStreamWriter(out, charset)`.
    public static OutputStreamWriter writer(OutputStream out, Charset charset) {
        try {
            return new OutputStreamWriter(out, charset.displayName());
        } catch (UnsupportedEncodingException e) {
            throw unsupported(charset);
        }
    }

    /// `inputStreamReader.getEncoding()` cannot be asked of the device's
    /// reader; `new PrintWriter(out, autoFlush, charset)` and the like are
    /// reached through [#writer].
    static Charset orUtf8(Charset charset) {
        return charset == null ? StandardCharsets.UTF_8 : charset;
    }

    /// Reads everything `in` has left, without closing it.
    static byte[] drain(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
            if (n > 0) {
                out.write(buffer, 0, n);
            }
        }
        return out.toByteArray();
    }
}
