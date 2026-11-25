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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// The entries of a ZIP package, read whole into memory under a budget. A compressed entry
/// can unpack to a thousand times its size, so the total and the entry count are capped.
final class ArtZip {
    static final long MAX_TOTAL = 256L * 1024 * 1024;
    static final int MAX_ENTRIES = 20000;

    private ArtZip() {
    }

    static Map<String, byte[]> read(InputStream in, String kind) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        Map<String, byte[]> parts = new HashMap<String, byte[]>();
        long left = MAX_TOTAL;
        int entries = 0;
        byte[] buffer = new byte[8192];
        try {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                entries++;
                if (entries > MAX_ENTRIES) {
                    throw new IOException("The " + kind + " has more than " + MAX_ENTRIES + " entries");
                }
                if (e.isDirectory()) {
                    continue;
                }
                String name = e.getName();
                while (name.startsWith("/")) {
                    name = name.substring(1);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                for (int n = zip.read(buffer); n > 0; n = zip.read(buffer)) {
                    left -= n;
                    if (left < 0) {
                        throw new IOException("The " + kind + " unpacks to more than " + MAX_TOTAL + " bytes");
                    }
                    out.write(buffer, 0, n);
                }
                parts.put(name, out.toByteArray());
                zip.closeEntry();
            }
        } catch (IllegalArgumentException e) {
            throw damaged(kind, e);
        } catch (IndexOutOfBoundsException e) {
            throw damaged(kind, e);
        }
        if (parts.isEmpty()) {
            throw new IOException("Not a " + kind + ": the file is not a ZIP package");
        }
        return parts;
    }

    private static IOException damaged(String kind, RuntimeException cause) {
        IOException e = new IOException("The " + kind + " is damaged: " + cause.getMessage());
        e.initCause(cause);
        return e;
    }
}
