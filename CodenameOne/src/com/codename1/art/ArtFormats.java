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
package com.codename1.art;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// Opens a graphics document in whichever format it is: PSD or PSB, SVG, Sketch, `.cn1gfx`,
/// or Figma REST API JSON. The format is found from the bytes, not the name.
public final class ArtFormats {
    // A Sketch or .cn1gfx package carries its bitmaps whole, so it gets four times the
    // budget of an office package; ZipParts still caps the entry count the same way.
    static final long MAX_PACKAGE_BYTES = 256L * 1024 * 1024;

    /// The largest file read.
    public static final int MAX_BYTES = 512 * 1024 * 1024;

    private ArtFormats() {
    }

    /// Reads a document.
    ///
    /// #### Parameters
    ///
    /// - `in`: the file; read to its end, not closed
    public static ArtDocument read(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            total += n;
            if (total > MAX_BYTES) {
                throw new IOException("File is larger than " + MAX_BYTES + " bytes");
            }
            bo.write(buf, 0, n);
        }
        return read(bo.toByteArray());
    }

    /// Reads a document from bytes.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file
    public static ArtDocument read(byte[] data) throws IOException {
        try {
            return dispatch(data);
        } catch (RuntimeException e) {
            throw damaged(e);
        }
    }

    /// A damaged file must fail as an IOException, whatever a reader trips over.
    static IOException damaged(RuntimeException e) {
        IOException x = new IOException("Damaged or unsupported file: " + e);
        x.initCause(e);
        return x;
    }

    private static ArtDocument dispatch(byte[] data) throws IOException {
        if (data.length >= 4 && data[0] == '8' && data[1] == 'B' && data[2] == 'P' && data[3] == 'S') {
            return PsdReader.read(data);
        }
        if (data.length >= 4 && data[0] == 'P' && data[1] == 'K' && data[2] == 3 && data[3] == 4) {
            // Both are ZIPs with a document.json; only Sketch has pages.
            if (contains(data, "pages/")) {
                return SketchReader.read(new ByteArrayInputStream(data));
            }
            return NativeFormat.read(new ByteArrayInputStream(data));
        }
        if (data.length >= 16 && startsWith(data, "SQLite format 3")) {
            throw new IOException("Sketch files older than Sketch 43 are not supported");
        }
        int i = skipSpace(data);
        if (i < data.length && data[i] == '{') {
            return FigmaReader.read(data, null);
        }
        if (i < data.length && data[i] == '<') {
            return SvgReader.read(data);
        }
        throw new IOException("Unknown graphics format");
    }

    /// The format of a file: "psd", "svg", "sketch", "cn1gfx", "figma" or null.
    ///
    /// #### Parameters
    ///
    /// - `data`: the start of the file; a few kilobytes are enough
    public static String detect(byte[] data) {
        if (data.length >= 4 && data[0] == '8' && data[1] == 'B' && data[2] == 'P' && data[3] == 'S') {
            return "psd";
        }
        if (data.length >= 4 && data[0] == 'P' && data[1] == 'K' && data[2] == 3 && data[3] == 4) {
            return contains(data, "pages/") ? "sketch" : "cn1gfx";
        }
        int i = skipSpace(data);
        if (i < data.length && data[i] == '{') {
            return "figma";
        }
        if (i < data.length && data[i] == '<') {
            return "svg";
        }
        return null;
    }

    private static int skipSpace(byte[] d) {
        int i = 0;
        if (d.length >= 3 && (d[0] & 0xff) == 0xef && (d[1] & 0xff) == 0xbb && (d[2] & 0xff) == 0xbf) {
            i = 3;
        }
        while (i < d.length && (d[i] == ' ' || d[i] == '\n' || d[i] == '\r' || d[i] == '\t')) {
            i++;
        }
        return i;
    }

    private static boolean startsWith(byte[] d, String s) {
        if (d.length < s.length()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (d[i] != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /// True when the ASCII text appears anywhere: ZIP entry names are stored uncompressed.
    private static boolean contains(byte[] d, String s) {
        int n = s.length();
        for (int i = 0; i + n <= d.length; i++) {
            int k = 0;
            while (k < n && d[i + k] == s.charAt(k)) {
                k++;
            }
            if (k == n) {
                return true;
            }
        }
        return false;
    }
}
