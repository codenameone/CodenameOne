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
package com.codename1.builders;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/// Fetches the native artifacts `PlatformFeatureCatalog` names by Maven
/// coordinates (whisper.cpp, llama.cpp) from the Codename One repository.
///
/// The Android builder needs none of this -- Gradle resolves an AAR itself --
/// so it serves the iOS builder, whose Xcode project has no dependency
/// resolver for a plain xcframework.
///
/// Every download is checked against the `.sha256` the repository publishes
/// beside it (the release staging writes all four checksums), and a mismatch
/// fails the build: a truncated or substituted archive must never reach the
/// linker, where it would surface as an unrelated undefined symbol.
final class CatalogNatives {
    /// Default home of the `com.codenameone.natives` artifacts.
    static final String DEFAULT_REPOSITORY = "https://repo.codenameone.com/maven2";

    private static final int TIMEOUT_MS = 60000;

    private CatalogNatives() {
    }

    /// Resolves `group:artifact:version` to the URL of its zip.
    ///
    /// @param repository repository root, with or without a trailing slash
    /// @param coordinates `group:artifact:version`
    /// @return the artifact URL
    static String zipUrl(String repository, String coordinates) {
        String[] gav = coordinates.split(":");
        if (gav.length != 3 || gav[0].length() == 0 || gav[1].length() == 0
                || gav[2].length() == 0) {
            throw new IllegalArgumentException("Expected group:artifact:version, got "
                    + coordinates);
        }
        String root = repository.endsWith("/")
                ? repository.substring(0, repository.length() - 1) : repository;
        return root + "/" + gav[0].replace('.', '/') + "/" + gav[1] + "/" + gav[2]
                + "/" + gav[1] + "-" + gav[2] + ".zip";
    }

    /// Downloads an artifact's zip into `dir` and verifies its checksum.
    ///
    /// @param repository repository root (`https:` or `file:`)
    /// @param coordinates `group:artifact:version`
    /// @param dir directory to write the zip into
    /// @return the verified zip
    /// @throws IOException when the download fails or the checksum differs
    static File fetchZip(String repository, String coordinates, File dir) throws IOException {
        String url = zipUrl(repository, coordinates);
        String expected = parseSha256(new String(read(url + ".sha256"), "UTF-8"));
        if (expected == null) {
            throw new IOException("No SHA-256 published for " + url);
        }
        String[] gav = coordinates.split(":");
        File zip = new File(dir, gav[1] + "-" + gav[2] + ".zip");
        byte[] data = read(url);
        String actual = sha256(data);
        if (!expected.equalsIgnoreCase(actual)) {
            throw new IOException("Checksum mismatch for " + url + ": expected "
                    + expected + ", got " + actual);
        }
        OutputStream out = new FileOutputStream(zip);
        try {
            out.write(data);
        } finally {
            out.close();
        }
        return zip;
    }

    /// Reads the digest out of a `.sha256` file, which is either the bare
    /// hex digest or `sha256sum` output (`<digest>  <file name>`).
    ///
    /// @param contents the file's text
    /// @return the 64-character digest, or `null` when there is none
    static String parseSha256(String contents) {
        String t = contents.trim();
        int space = indexOfWhitespace(t);
        if (space >= 0) {
            t = t.substring(0, space);
        }
        if (t.length() != 64) {
            return null;
        }
        for (int i = 0; i < t.length(); i++) {
            if (Character.digit(t.charAt(i), 16) < 0) {
                return null;
            }
        }
        return t;
    }

    static String sha256(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (int i = 0; i < d.length; i++) {
                sb.append(Character.forDigit((d[i] >> 4) & 0xf, 16));
                sb.append(Character.forDigit(d[i] & 0xf, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static byte[] read(String url) throws IOException {
        InputStream in;
        if (url.startsWith("file:")) {
            in = new FileInputStream(new File(java.net.URI.create(url)));
        } else {
            URLConnection c = new URL(url).openConnection();
            c.setConnectTimeout(TIMEOUT_MS);
            c.setReadTimeout(TIMEOUT_MS);
            in = c.getInputStream();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
