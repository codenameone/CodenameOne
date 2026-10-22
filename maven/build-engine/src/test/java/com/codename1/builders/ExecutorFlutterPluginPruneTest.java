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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// An unused Flutter plugin pack must not reach the build: the Android builder reads
/// permissions and Play Services dependencies off the classes it finds.
class ExecutorFlutterPluginPruneTest {
    @TempDir
    File dir;

    private void cls(String path, String... refs) throws IOException {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        StringBuilder body = new StringBuilder("CAFEBABE");
        for (String r : refs) {
            body.append('\u0001').append(r);
        }
        Files.write(f.toPath(), body.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void keepsReferencedPacksAndWhatTheyUseAndDropsTheRest() throws IOException {
        String p = Executor.FLUTTER_PLUGINS;
        cls("com/example/Main.class", p + "share/SharePlus");
        cls(p + "share/SharePlus.class", p + "crossfile/XFile");
        cls(p + "crossfile/XFile.class");
        cls(p + "geolocator/Geolocator.class", "com/codename1/location/LocationManager");
        cls(p + "firebase/FirebaseAuth.class");

        Executor.pruneUnusedFlutterPlugins(dir);

        assertTrue(new File(dir, p + "share/SharePlus.class").isFile());
        assertTrue(new File(dir, p + "crossfile/XFile.class").isFile());
        assertFalse(new File(dir, p + "geolocator").exists());
        assertFalse(new File(dir, p + "firebase").exists());
        assertTrue(new File(dir, "com/example/Main.class").isFile());
    }

    @Test
    void aPackOnlyALibraryJarUsesIsKept() throws IOException {
        String p = Executor.FLUTTER_PLUGINS;
        cls("com/example/Main.class");
        cls(p + "dio/Dio.class");
        cls(p + "geolocator/Geolocator.class");
        File libs = new File(dir.getParentFile(), dir.getName() + "-libs");
        libs.mkdirs();
        java.util.zip.ZipOutputStream z = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(new File(libs, "lib.jar")));
        z.putNextEntry(new java.util.zip.ZipEntry("com/lib/UsesDio.class"));
        z.write(("CAFEBABE\u0001" + p + "dio/Dio").getBytes(StandardCharsets.ISO_8859_1));
        z.closeEntry();
        z.close();

        Executor.pruneUnusedFlutterPlugins(dir, libs);

        assertTrue(new File(dir, p + "dio/Dio.class").isFile());
        assertFalse(new File(dir, p + "geolocator").exists());
    }

    @Test
    void anAppWithNoFlutterRuntimeIsUntouched() throws IOException {
        cls("com/example/Main.class");
        Executor.pruneUnusedFlutterPlugins(dir);
        assertTrue(new File(dir, "com/example/Main.class").isFile());
    }
}
