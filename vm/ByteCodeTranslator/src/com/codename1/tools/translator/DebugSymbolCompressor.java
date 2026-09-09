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
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Compresses the on-device-debug symbol table.
 *
 * This is the translator's only use of {@code java.util.zip}, and it exists as its
 * own class so that it is the only thing that has to be replaced when the
 * translator is compiled against ParparVM's JavaAPI in order to translate itself.
 * JavaAPI has no java.util.zip and cannot gain one: it is mirrored by
 * Ports/CLDC11, where the package does not belong.
 *
 * Nothing else needs the package. The translator reads directories of class files,
 * never archives -- every caller extracts a jar before invoking it -- and
 * {@code NativeSignatureVerifier}'s archive scan lives behind its own command-line
 * entry point.
 *
 * Symbol tables are large and highly repetitive, so compressing keeps a debug
 * binary's footprint modest.
 */
final class DebugSymbolCompressor {
    private DebugSymbolCompressor() {
    }

    static byte[] gzip(ByteArrayOutputStream raw) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.size() / 3 + 64);
        GZIPOutputStream gz = new GZIPOutputStream(out);
        try {
            raw.writeTo(gz);
        } finally {
            gz.close();
        }
        return out.toByteArray();
    }
}
