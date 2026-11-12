/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A builder reads its port out of jars it was handed, not only off the class path.
class ExecutorPortJarTest {
    private static File jar(Path dir, String name, String entry, String content) throws IOException {
        File f = dir.resolve(name).toFile();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
            out.putNextEntry(new ZipEntry(entry));
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return f;
    }

    private static String read(InputStream in) throws IOException {
        assertNotNull(in);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            for (int n = in.read(buf); n >= 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    @Test
    void anAbsoluteResourceIsReadFromAPortJar(@TempDir Path dir) throws IOException {
        Executor builder = new JavaScriptBuilder();
        assertNull(builder.getResourceAsStream("/cn1-port-jar-test.txt"));
        builder.addPortJar(jar(dir, "first.jar", "unrelated.txt", "no"));
        builder.addPortJar(jar(dir, "second.jar", "cn1-port-jar-test.txt", "from the port"));
        assertEquals("from the port", read(builder.getResourceAsStream("/cn1-port-jar-test.txt")));
        // Twice: the first stream closed its jar, and the second has to open it again.
        assertEquals("from the port", read(builder.getResourceAsStream("/cn1-port-jar-test.txt")));
    }

    @Test
    void aRelativeResourceIsRelativeToTheBuildersPackage(@TempDir Path dir) throws IOException {
        // Class.getResourceAsStream resolves a name without a leading slash against
        // the class's package, and the Android port's sources are read that way.
        Executor builder = new JavaScriptBuilder();
        builder.addPortJar(jar(dir, "port.jar", "com/codename1/builders/cn1-port-jar-test.txt", "relative"));
        assertEquals("relative", read(builder.getResourceAsStream("cn1-port-jar-test.txt")));
        assertNull(builder.getResourceAsStream("/cn1-port-jar-test.txt"));
    }

    @Test
    void theClassPathStillWins(@TempDir Path dir) throws IOException {
        // The cloud builder runs with the ports on its class path and hands over none.
        String onClassPath = "/com/codename1/builders/Executor.class";
        Executor builder = new JavaScriptBuilder();
        builder.addPortJar(jar(dir, "port.jar", onClassPath.substring(1), "not a class"));
        assertTrue(read(builder.getResourceAsStream(onClassPath)).length() > "not a class".length());
    }

    @Test
    void aPortJarThatCannotBeReadIsAnErrorAndNotAMissingResource(@TempDir Path dir) throws IOException {
        File broken = dir.resolve("broken.jar").toFile();
        try (FileOutputStream out = new FileOutputStream(broken)) {
            out.write("not a zip".getBytes(StandardCharsets.UTF_8));
        }
        Executor builder = new JavaScriptBuilder();
        builder.addPortJar(broken);
        assertThrows(IllegalStateException.class, () -> builder.getResourceAsStream("/cn1-port-jar-test.txt"));
    }
}
