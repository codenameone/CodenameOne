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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// Where the screenshots go: one file per test, named from the wire after the name
/// has been made safe, with the device's length and hash checked.
///
/// ## Why the name is sanitised twice
///
/// The name arrives over the wire, so it cannot be trusted even though the device
/// helper already restricts it. It is folded to `[A-Za-z0-9_.-]`, refused outright
/// if it holds a separator or is `.` or `..`, and the resolved path is then checked
/// to be inside the output directory -- guarding the exact File that is written
/// rather than a separately derived value, which is what CodeQL's
/// java/path-injection asks for.
@Component
public class ScreenshotStore {
    @Value("${cn1ss.out:cn1ss-out}")
    private String out;

    private File dir;
    /// png hash to the first test that produced it, for the duplicate warning.
    private final Map hashes = new HashMap();
    private int received;

    @PostConstruct
    void open() throws IOException {
        dir = new File(out).getCanonicalFile();
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("cannot create the screenshot directory " + dir);
        }
    }

    /// The directory screenshots are written to.
    public File getDirectory() {
        return dir;
    }

    /// How many screenshots have been stored.
    public synchronized int getReceived() {
        return received;
    }

    /// Writes one screenshot and returns the reply the device waits for.
    public synchronized String write(String name, long declaredBytes, String declaredHash,
                                     byte[] payload, int offset, int length) throws IOException {
        String safe = sanitise(name);
        if (safe == null) {
            System.err.println("[cn1ss] rejected unsafe test name: " + name);
            return "NACK rejected status=invalid_test_name";
        }
        File target = new File(dir, safe + ".png").getCanonicalFile();
        // Guarding the exact File the stream below opens.
        if (!isInside(dir, target)) {
            System.err.println("[cn1ss] rejected out-of-base path: " + name + " -> " + target);
            return "NACK rejected status=path_escape";
        }
        String status = "ok";
        StringBuilder warn = new StringBuilder();
        if (declaredBytes >= 0 && declaredBytes != length) {
            status = "length_mismatch";
            warn.append("expected=").append(declaredBytes).append(",got=").append(length);
        }
        String actualHash = fnv1a64Hex(payload, offset, length);
        if (declaredHash != null && !declaredHash.equals(actualHash)) {
            if ("ok".equals(status)) {
                status = "hash_mismatch";
            }
            if (warn.length() > 0) {
                warn.append(';');
            }
            warn.append("expected_hash=").append(declaredHash).append(",actual_hash=")
                    .append(actualHash);
        }
        Object previous = hashes.get(actualHash);
        if (previous == null) {
            hashes.put(actualHash, safe);
        } else if (!previous.equals(safe)) {
            // Two tests that produced pixel-identical images. This is what catches
            // a stale frame being captured for a test that never rendered.
            System.out.println("CN1SS:WARN:test=" + safe + " duplicate_image_with=" + previous
                    + " png_fnv1a64=" + actualHash);
        }
        OutputStream stream = new FileOutputStream(target);
        try {
            stream.write(payload, offset, length);
        } finally {
            stream.close();
        }
        received++;
        System.out.println("CN1SS:INFO:test=" + safe + " png_bytes=" + length + " png_fnv1a64="
                + actualHash + " status=" + status + (warn.length() == 0 ? "" : " warn=" + warn));
        return "ACK " + safe + " status=" + status;
    }

    /// The name with every character outside `[A-Za-z0-9_.-]` replaced by `_`, or
    /// null when it cannot be a file name at all.
    static String sanitise(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf('\0') >= 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(name.length());
        for (int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean alphanumeric = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9');
            sb.append(alphanumeric || c == '_' || c == '.' || c == '-' ? c : '_');
        }
        String result = sb.toString();
        return ".".equals(result) || "..".equals(result) ? null : result;
    }

    /// Whether `target` really sits under `base`, both already canonical.
    static boolean isInside(File base, File target) {
        String basePath = base.getPath();
        String targetPath = target.getPath();
        if (!targetPath.startsWith(basePath)) {
            return false;
        }
        // "/out" must not be read as a prefix of "/output".
        return targetPath.length() == basePath.length()
                || targetPath.charAt(basePath.length()) == File.separatorChar;
    }

    /// The same FNV-1a-64 the device computes, so the two can be compared.
    static String fnv1a64Hex(byte[] bytes, int offset, int length) {
        long hash = 0xcbf29ce484222325L;
        long prime = 0x100000001b3L;
        for (int iter = 0 ; iter < length ; iter++) {
            hash ^= bytes[offset + iter] & 0xff;
            hash *= prime;
        }
        StringBuilder sb = new StringBuilder(16);
        for (int shift = 60 ; shift >= 0 ; shift -= 4) {
            int nibble = (int) ((hash >>> shift) & 0xf);
            sb.append((char) (nibble < 10 ? '0' + nibble : 'a' + (nibble - 10)));
        }
        return sb.toString();
    }
}
