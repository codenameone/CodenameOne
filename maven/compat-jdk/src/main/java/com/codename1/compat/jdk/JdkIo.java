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
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/// Members of the `java.io` streams that came after the device's class
/// library was cut, as static methods that take the stream first.
public final class JdkIo {
    private static final int CHUNK = 8192;

    private JdkIo() {
    }

    /// Everything left in `in`.
    public static byte[] readAllBytes(InputStream in) throws IOException {
        return readNBytes(in, Integer.MAX_VALUE);
    }

    /// Up to `len` bytes of `in`: fewer only when the stream ends first.
    public static byte[] readNBytes(InputStream in, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[CHUNK];
        int left = len;
        while (left > 0) {
            int n = in.read(chunk, 0, left < CHUNK ? left : CHUNK);
            if (n < 0) {
                break;
            }
            out.write(chunk, 0, n);
            left -= n;
        }
        return out.toByteArray();
    }

    /// Fills `b` from `off` with up to `len` bytes, reading until that
    /// many arrived or the stream ended, and answers how many there were.
    public static int readNBytes(InputStream in, byte[] b, int off, int len) throws IOException {
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        int total = 0;
        while (total < len) {
            int n = in.read(b, off + total, len - total);
            if (n < 0) {
                break;
            }
            total += n;
        }
        return total;
    }

    /// Copies everything left in `in` to `out`, closing neither.
    public static long transferTo(InputStream in, OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException("out");
        }
        byte[] chunk = new byte[CHUNK];
        long total = 0;
        int n = in.read(chunk, 0, CHUNK);
        while (n >= 0) {
            out.write(chunk, 0, n);
            total += n;
            n = in.read(chunk, 0, CHUNK);
        }
        return total;
    }

    /// Copies everything left in `in` to `out`, closing neither.
    public static long transferTo(Reader in, Writer out) throws IOException {
        if (out == null) {
            throw new NullPointerException("out");
        }
        char[] chunk = new char[CHUNK];
        long total = 0;
        int n = in.read(chunk, 0, CHUNK);
        while (n >= 0) {
            out.write(chunk, 0, n);
            total += n;
            n = in.read(chunk, 0, CHUNK);
        }
        return total;
    }

    /// The lines left in `reader`. They are read when this is called
    /// rather than as the stream is walked, so a failure is thrown here, as
    /// the unchecked exception the JDK throws from the stream.
    public static Stream<String> lines(BufferedReader reader) {
        List<String> lines = new ArrayList<String>();
        try {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return JdkCollections.stream(lines);
    }
}
