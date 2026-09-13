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
package com.codename1.initializr.model;

import java.io.IOException;

/** Supplies Unix modes missing from ZipSupport's classic ZIP central directory. */
final class ProjectZipPermissions {
    private ProjectZipPermissions() { }

    // The generator writes a classic ZIP with no archive comment. Keep ZipSupport
    // for CRCs, offsets and content; only set the creator OS and file attributes.
    // No java.nio or desktop ZIP dependency: this also runs in the browser port.
    static void apply(byte[] zip) throws IOException {
        int end = zip.length - 22;
        if (end < 0 || readInt(zip, end) != 0x06054b50) {
            throw new IOException("Missing project ZIP directory");
        }
        int count = readShort(zip, end + 10);
        int offset = readInt(zip, end + 16);
        if (offset < 0 || readInt(zip, end + 12) != end - offset) {
            throw new IOException("Invalid project ZIP directory size");
        }
        for (int i = 0; i < count; i++) {
            if (offset < 0 || offset > end - 46 || readInt(zip, offset) != 0x02014b50) {
                throw new IOException("Invalid project ZIP entry");
            }
            int nameLength = readShort(zip, offset + 28);
            int next = offset + 46 + nameLength + readShort(zip, offset + 30) + readShort(zip, offset + 32);
            if (next > end) throw new IOException("Truncated project ZIP entry");
            String name = new String(zip, offset + 46, nameLength, "UTF-8");
            boolean executable = "build.sh".equals(name) || "run.sh".equals(name) || "mvnw".equals(name);
            int mode = name.endsWith("/") ? 040755 : (executable ? 0100755 : 0100644);
            zip[offset + 5] = 3; // version-made-by host: Unix
            int attributes = (mode << 16) | (name.endsWith("/") ? 0x10 : 0);
            for (int b = 0; b < 4; b++) zip[offset + 38 + b] = (byte) (attributes >>> (8 * b));
            offset = next;
        }
        if (offset != end) throw new IOException("Unexpected project ZIP directory contents");
    }

    private static int readShort(byte[] data, int offset) {
        return (data[offset] & 255) | ((data[offset + 1] & 255) << 8);
    }

    private static int readInt(byte[] data, int offset) {
        return readShort(data, offset) | (readShort(data, offset + 2) << 16);
    }
}
