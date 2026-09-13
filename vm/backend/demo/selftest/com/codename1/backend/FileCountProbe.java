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

package com.codename1.backend;

/**
 * Test-only access to backend internals the product does not expose.
 *
 * <p>In this package because that is where the fields are: openFileCount is how
 * StaticFiles reports what it has handed out, and a Response's descriptor is not
 * public either. A probe beside the test keeps both out of the product's API --
 * see ExpiryProbe, which exists for the same reason.
 */
public final class FileCountProbe {
    private FileCountProbe() {
    }

    /** Descriptors StaticFiles has handed to a Response and not yet closed. */
    public static int openFiles() {
        return StaticFiles.openFileCount();
    }

    /** The descriptor a file-backed response carries, or -1 when it has none. */
    public static int fdOf(HttpServer.Response response) {
        return response.fileFd;
    }

    /** The number of bytes that response means to send from it. */
    public static long lengthOf(HttpServer.Response response) {
        return response.fileLength;
    }

    /**
     * A header block as the bytes the natives are handed.
     *
     * The wire is not a practical place to check this: nghttp2 may Huffman-code
     * the value it is given, so what the frame carries is not the octets
     * themselves. The rule is one character to one byte, and this is where it is
     * decided.
     */
    public static byte[] headerBytes(String value) {
        return HeaderLines.narrowed(value);
    }

    /**
     * The Host field Http builds for a host and port.
     *
     * Asked here rather than over the wire: an IPv6 loopback is not something a
     * build machine can be relied on to have, and the formatting is the whole of
     * what went wrong.
     */
    public static String hostField(String host, int port) {
        return Http.authority(host, port);
    }
}
