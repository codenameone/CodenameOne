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
