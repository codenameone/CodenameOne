/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.fxcompat.runtime;

import com.codename1.compat.jdk.Resources;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// Finds the application resource a URL string names.
///
/// An application hands a loader `getResource(name).toExternalForm()`,
/// which is `cn1res:/com/example/img/logo.png` here and was
/// `jar:file:/app.jar!/com/example/img/logo.png` or a `file:` URL of a
/// build directory on a desktop. A device keeps every resource at the root
/// of its bundle under a flat name only
/// [com.codename1.compat.jdk.Resources] knows, so every loader of the
/// layer -- images, fonts -- resolves its URL here rather than asking the
/// platform for a path with directories, which a port refuses outright.
public final class ResourceUrls {

    private ResourceUrls() {
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /// Undoes percent encoding, reading the escaped bytes as UTF-8. An
    /// escape that is not one is left as it is written.
    static String decode(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        int n = s.length();
        byte[] bytes = new byte[n * 3];
        int used = 0;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < n && hex(s.charAt(i + 1)) >= 0 && hex(s.charAt(i + 2)) >= 0) {
                bytes[used++] = (byte) (hex(s.charAt(i + 1)) * 16 + hex(s.charAt(i + 2)));
                i += 2;
            } else if (c < 0x80) {
                bytes[used++] = (byte) c;
            } else if (c < 0x800) {
                bytes[used++] = (byte) (0xc0 | (c >> 6));
                bytes[used++] = (byte) (0x80 | (c & 0x3f));
            } else {
                bytes[used++] = (byte) (0xe0 | (c >> 12));
                bytes[used++] = (byte) (0x80 | ((c >> 6) & 0x3f));
                bytes[used++] = (byte) (0x80 | (c & 0x3f));
            }
        }
        try {
            return new String(bytes, 0, used, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            // Every platform has UTF-8.
            return s;
        }
    }

    /// The path part of a URL string: no scheme, no archive, no query, the
    /// escapes undone, and no leading slash. A string that is a plain path
    /// already is answered as it is.
    public static String path(String url) {
        String path = url;
        int bang = path.lastIndexOf("!/");
        if (bang >= 0) {
            path = path.substring(bang + 2);
        } else {
            int colon = path.indexOf(':');
            int slash = path.indexOf('/');
            if (colon > 0 && (slash < 0 || colon < slash)) {
                path = path.substring(colon + 1);
            }
        }
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int start = 0;
        while (start < path.length() && path.charAt(start) == '/') {
            start++;
        }
        return decode(path.substring(start));
    }

    /// The resource path, without a leading slash, a URL string names: its
    /// whole path when the application ships that, else the longest tail of
    /// it the application does ship -- a desktop URL starts with directories
    /// the bundle knows nothing about. Answers `null` when there is none.
    public static String find(String url) {
        if (url == null) {
            return null;
        }
        String path = path(url);
        while (path.length() > 0) {
            if (Resources.exists(path)) {
                return path;
            }
            int next = path.indexOf('/');
            if (next < 0) {
                return null;
            }
            path = path.substring(next + 1);
        }
        return null;
    }

    /// Opens the resource a URL string names, as [#find(String)] finds it;
    /// `null` when the application ships none.
    public static InputStream open(String url) {
        String path = find(url);
        return path == null ? null : Resources.open(path);
    }

    /// Reads a stream to its end. The stream is left open.
    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n = in.read(chunk);
        while (n >= 0) {
            out.write(chunk, 0, n);
            n = in.read(chunk);
        }
        return out.toByteArray();
    }
}
