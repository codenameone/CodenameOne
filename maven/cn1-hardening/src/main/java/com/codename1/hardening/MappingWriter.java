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
package com.codename1.hardening;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Finalizes the ProGuard-format mapping: prepends a provenance header (engine and
 * ProGuard versions, platform, build key) so a support ticket can say exactly which
 * engine produced it, and computes the {@code mappingId} -- the SHA-256 of the
 * mapping body -- stamped into the app so a crash report can be tied to the exact
 * mapping even when a rebuild reuses the build key.
 */
public final class MappingWriter {

    private MappingWriter() {
    }

    /**
     * Prepends the header to {@code mappingFile} in place and returns its {@code mappingId}
     * computed over the ProGuard body (excluding the header, so the id is stable regardless of
     * header text).
     */
    public static String finalizeMapping(File mappingFile, String engineVersion, String proguardVersion,
                                         String platform, String buildKey) throws HardeningException {
        try {
            byte[] body = mappingFile.isFile()
                    ? Files.readAllBytes(mappingFile.toPath())
                    : new byte[0];
            String mappingId = sha256Hex(body);
            StringBuilder header = new StringBuilder();
            header.append("# Codename One App Hardening mapping\n");
            header.append("# engine: ").append(engineVersion).append('\n');
            header.append("# proguard: ").append(proguardVersion).append('\n');
            header.append("# platform: ").append(platform).append('\n');
            header.append("# buildKey: ").append(buildKey == null ? "" : buildKey).append('\n');
            header.append("# mappingId: ").append(mappingId).append('\n');
            FileOutputStream fo = new FileOutputStream(mappingFile);
            try {
                OutputStream out = fo;
                out.write(header.toString().getBytes(Charset.forName("UTF-8")));
                out.write(body);
                out.flush();
            } finally {
                fo.close();
            }
            return mappingId;
        } catch (IOException e) {
            throw new HardeningException("Could not finalize the mapping file", e);
        }
    }

    /**
     * Injects R8-style {@code sourceFile} metadata comments into the mapping so a retrace can report the
     * real source filename. The engine strips the {@code SourceFile} attribute and ProGuard's mapping
     * records no filename, so without this a Kotlin class ({@code Screen.kt}) or a package-private class
     * declared in a differently named file retraces to a synthesized {@code <Class>.java} that does not
     * exist. Only classes whose recorded filename differs from that synthesized default are written (the
     * ordinary {@code Foo}/{@code Foo.java} case needs no comment). The comment is INDENTED so the retrace
     * parser attaches it to the preceding class line, matching R8's own emission.
     *
     * @param sourceFileByFqcn original dotted class name to its {@code SourceFile}, for the classes worth
     *                         recording; captured before the attribute was stripped.
     */
    static void injectSourceFiles(File mappingFile, java.util.Map<String, String> sourceFileByFqcn)
            throws HardeningException {
        if (sourceFileByFqcn == null || sourceFileByFqcn.isEmpty() || mappingFile == null
                || !mappingFile.isFile()) {
            return;
        }
        try {
            java.util.List<String> lines = Files.readAllLines(mappingFile.toPath(),
                    Charset.forName("UTF-8"));
            StringBuilder out = new StringBuilder();
            for (String line : lines) {
                out.append(line).append('\n');
                // A class line is unindented, contains " -> ", and ends with ':'. Its members follow
                // indented, so inject the metadata comment right after it (also indented).
                if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0)) && line.endsWith(":")) {
                    int arrow = line.indexOf(" -> ");
                    if (arrow > 0) {
                        String original = line.substring(0, arrow).trim();
                        String sf = sourceFileByFqcn.get(original);
                        if (sf != null) {
                            out.append("    # {\"id\":\"sourceFile\",\"fileName\":\"")
                                    .append(jsonEscape(sf)).append("\"}\n");
                        }
                    }
                }
            }
            Files.write(mappingFile.toPath(), out.toString().getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            throw new HardeningException("Could not inject source-file metadata into the mapping", e);
        }
    }

    /**
     * Escapes a string for a JSON value. Besides {@code \} and {@code "}, a control character (newline,
     * tab, ...) must be escaped too: the metadata is a single mapping-comment line, so a raw newline would
     * split it and a raw control char is invalid JSON, either of which stops the reader from recovering the
     * filename. Reachable for a Kotlin or package-private Java class stored in an unusually named file.
     */
    private static String jsonEscape(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': b.append("\\\\"); break;
                case '"': b.append("\\\""); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        b.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            b.append('0');
                        }
                        b.append(hex);
                    } else {
                        b.append(c);
                    }
            }
        }
        return b.toString();
    }

    static String sha256Hex(byte[] data) throws HardeningException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xf, 16));
                sb.append(Character.forDigit(b & 0xf, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new HardeningException("SHA-256 unavailable", e);
        }
    }
}
