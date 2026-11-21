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
package com.codename1.wordprocessing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// The entries of an office package (DOCX, ODT), read whole into memory under a budget.
///
/// The parts of a package can come in any order -- Word writes the body before the styles it
/// uses -- so a reader needs them all before it can interpret any. A compressed entry can
/// unpack to a thousand times its size, so both the total and the number of entries are capped.
final class ZipParts {
    /// The most bytes a package may unpack to: every part is held in memory at once, and
    /// buffering one briefly needs twice its size, so this has to fit a phone.
    static final long MAX_TOTAL = 64L * 1024 * 1024;

    /// The most entries a package may have.
    static final int MAX_ENTRIES = 20000;

    private ZipParts() {
    }

    /// Reads every file entry. Names lose a leading slash.
    static Map<String, byte[]> read(InputStream in, String kind) throws IOException {
        ZipInputStream zip = new ZipInputStream(in);
        Map<String, byte[]> parts = new HashMap<String, byte[]>();
        long left = MAX_TOTAL;
        int entries = 0;
        byte[] buffer = new byte[8192];
        try {
            for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                if (++entries > MAX_ENTRIES) { //NOPMD AssignmentInOperand - the entry count bounds a hostile archive
                    throw new IOException("The " + kind + " has more than " + MAX_ENTRIES
                            + " entries");
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
                        throw new IOException("The " + kind + " unpacks to more than " + MAX_TOTAL
                                + " bytes");
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

    /// Resolves a relationship target against the directory of the part that names it.
    static String resolve(String base, String target) {
        if (target.startsWith("/")) {
            return target.substring(1);
        }
        int slash = base.lastIndexOf('/');
        String dir = slash < 0 ? "" : base.substring(0, slash + 1);
        String path = dir + target;
        StringBuilder out = new StringBuilder();
        String[] segs = split(path);
        java.util.List<String> stack = new java.util.ArrayList<String>();
        for (String s : segs) {
            if ("..".equals(s)) {
                if (!stack.isEmpty()) {
                    stack.remove(stack.size() - 1);
                }
            } else if (s.length() > 0 && !".".equals(s)) {
                stack.add(s);
            }
        }
        for (int i = 0; i < stack.size(); i++) {
            if (i > 0) {
                out.append('/');
            }
            out.append(stack.get(i));
        }
        return out.toString();
    }

    private static String[] split(String path) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        int start = 0;
        for (int i = 0; i <= path.length(); i++) {
            if (i == path.length() || path.charAt(i) == '/') {
                out.add(path.substring(start, i));
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }
}
