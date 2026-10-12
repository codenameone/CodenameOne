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
import com.codename1.io.zip.ZipArchiveOutputStream;
import java.io.*;
import java.nio.charset.*;
import java.util.zip.*;

/** Tests provider-backed charset objects that cannot be looked up by name. */
public class ZipCharsetApp {
    public static void main(String[] args) throws Exception {
        for (final Charset delegate : new Charset[] {StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8}) {
            Charset charset = new Charset(delegate == StandardCharsets.UTF_8 ? "UTF-8" : "X-CN1-ZIP-Test", null) {
                public String displayName() { return "Human readable ZIP charset"; }
                public boolean contains(Charset other) { return delegate.contains(other); }
                public CharsetEncoder newEncoder() { return delegate.newEncoder(); }
                public CharsetDecoder newDecoder() { return delegate.newDecoder(); }
            };
            for (int writer = 0; writer < 2; writer++) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZipEntry entry = new ZipEntry("caf\u00e9");
                entry.setComment("\u00e9t\u00e9");
                if (writer == 0) {
                    try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(bytes, charset)) {
                        out.setComment("\u00e9t\u00e9");
                        out.putNextEntry(entry);
                        out.write(42);
                    }
                } else {
                    try (ZipOutputStream out = new ZipOutputStream(bytes, charset)) {
                        out.setComment("\u00e9t\u00e9");
                        out.putNextEntry(entry);
                        out.write(42);
                    }
                }
                byte[] archive = bytes.toByteArray();
                if (((archive[7] & 8) != 0) != (delegate == StandardCharsets.UTF_8))
                    throw new AssertionError("UTF-8 flag must use canonical charset name");
                try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(archive), charset)) {
                    if (!"caf\u00e9".equals(in.getNextEntry().getName()) || in.read() != 42 || in.read() != -1)
                        throw new AssertionError("Custom charset did not round trip");
                }
            }
        }
        System.out.println("ZIP_CHARSET_OK");
    }
}
