/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.rbx;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The engine runs on the server, which is compiled natively without the
/// client framework. Every class here may reference only the engine, the Luau
/// runtime and `MathUtil` (shared with the backend); anything else of
/// `com.codename1` belongs in rbx-view.
class ServerSafeTest {
    @Test
    void engineNamesNothingOfTheClientFramework() throws IOException {
        List<String> bad = new ArrayList<String>();
        scan(new File("target/classes"), bad);
        assertTrue(bad.isEmpty(), "client framework references: " + bad);
    }

    private static void scan(File f, List<String> bad) throws IOException {
        File[] kids = f.listFiles();
        if (kids != null) {
            for (File k : kids) {
                scan(k, bad);
            }
            return;
        }
        if (!f.getName().endsWith(".class")) {
            return;
        }
        byte[] data = new byte[(int) f.length()];
        try (InputStream in = new FileInputStream(f)) {
            int off = 0;
            while (off < data.length) {
                int r = in.read(data, off, data.length - off);
                if (r < 0) {
                    break;
                }
                off += r;
            }
        }
        String text = new String(data, "ISO-8859-1");
        int i = text.indexOf("com/codename1/");
        while (i >= 0) {
            String rest = text.substring(i + 14);
            if (!rest.startsWith("rbx/") && !rest.startsWith("luau/")
                    && !rest.startsWith("util/MathUtil")) {
                bad.add(f.getName() + " -> " + rest.substring(0, Math.min(40, rest.length())));
            }
            i = text.indexOf("com/codename1/", i + 1);
        }
    }
}
