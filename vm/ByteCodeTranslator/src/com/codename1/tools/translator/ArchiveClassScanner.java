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
package com.codename1.tools.translator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Collects the native methods declared by every class inside a jar or zip.
 *
 * Split out of {@link NativeSignatureVerifier} because it is that class's only use
 * of {@code java.util.zip}, and it is reachable only from the offline command-line
 * entry point that scripts/check-native-signatures.sh drives -- never from a
 * translation. Isolating it is what lets the rest of the verifier compile against
 * ParparVM's JavaAPI, which has no java.util.zip and cannot gain one: JavaAPI is
 * mirrored by Ports/CLDC11, where the package does not belong.
 *
 * The translator itself never reads an archive. Every caller extracts a jar into a
 * directory of class files before invoking it.
 */
final class ArchiveClassScanner {
    private ArchiveClassScanner() {
    }

    /**
     * Entries are visited in sorted order so that two runs over the same archive
     * report findings in the same order.
     */
    static void collect(File archive, List<NativeSignatureVerifier.Signature> into) throws IOException {
        ZipFile zip = new ZipFile(archive);
        try {
            List<String> names = new ArrayList<String>();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".class")
                        && !entry.getName().endsWith("module-info.class")) {
                    names.add(entry.getName());
                }
            }
            Collections.sort(names);
            for (String name : names) {
                InputStream in = zip.getInputStream(zip.getEntry(name));
                try {
                    NativeSignatureVerifier.collectFromClassBytes(readAll(in), into);
                } finally {
                    in.close();
                }
            }
        } finally {
            zip.close();
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
