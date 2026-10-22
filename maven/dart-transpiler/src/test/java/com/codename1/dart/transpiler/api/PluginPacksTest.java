/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.dart.transpiler.api;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The transpiler copies a plugin pack into the app only when something names it.
class PluginPacksTest {
    private static void write(File f, String s) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), s.getBytes(StandardCharsets.UTF_8));
    }

    /// A runtime class directory with a stub that names one pack, and three packs:
    /// share (which names crossfile), crossfile, and geolocator.
    private static File runtime(File dir) throws IOException {
        write(new File(dir, "META-INF/dart/plugin_share.dart"),
                "@JavaName('com.codename1.flutter.plugins.share.Share')\n"
                + "class Share {\n  external static void share(String text);\n}\n");
        String root = "META-INF/dart/java/com/codename1/flutter/plugins/";
        write(new File(dir, root + "share/Share.java"),
                "package com.codename1.flutter.plugins.share;\n"
                + "public class Share {\n"
                + "  public static void share(String t) { com.codename1.flutter.plugins.crossfile.XFile.x(); }\n"
                + "}\n");
        write(new File(dir, root + "crossfile/XFile.java"),
                "package com.codename1.flutter.plugins.crossfile;\n"
                + "public class XFile {\n  public static void x() { }\n}\n");
        write(new File(dir, root + "geolocator/Geolocator.java"),
                "package com.codename1.flutter.plugins.geolocator;\n"
                + "public class Geolocator {\n  public static void g() { }\n}\n");
        return dir;
    }

    private static File out(File work, String pack, String cls) {
        return new File(work, "out/com/codename1/flutter/plugins/" + pack + "/" + cls + ".java");
    }

    @Test
    void copiesOnlyNamedPacksAndWhatTheyName() throws IOException {
        File work = Files.createTempDirectory("packs").toFile();
        File rt = runtime(new File(work, "rt"));
        File src = new File(work, "dart");
        write(new File(src, "main.dart"), "void main() { print('hi'); }\n");
        TranspileRequest req = new TranspileRequest().sourceRoot(src).outputDir(new File(work, "out"))
                .stubClasspathEntry(rt).stateFile(new File(work, "state"));
        assertFalse(new DartTranspiler().transpile(req).hasErrors());
        assertFalse(new File(work, "out/com/codename1/flutter/plugins").exists(),
                "a program that names no pack gets none");

        write(new File(src, "main.dart"),
                "import 'package:share_plus/share_plus.dart';\nvoid main() { Share.share('x'); }\n");
        assertFalse(new DartTranspiler().transpile(req).hasErrors());
        assertTrue(out(work, "share", "Share").isFile());
        assertTrue(out(work, "crossfile", "XFile").isFile(), "share names crossfile");
        assertFalse(out(work, "geolocator", "Geolocator").exists());

        // The app stops using share: its packs go, rather than linger with their classes.
        write(new File(src, "main.dart"), "void main() { print('bye'); }\n");
        assertFalse(new DartTranspiler().transpile(req).hasErrors());
        assertFalse(out(work, "share", "Share").exists());
        assertFalse(out(work, "crossfile", "XFile").exists());
    }

    @Test
    void theAppsOwnJavaSelectsAPack() throws IOException {
        File work = Files.createTempDirectory("packs").toFile();
        File rt = runtime(new File(work, "rt"));
        File src = new File(work, "dart");
        write(new File(src, "main.dart"), "void main() { print('hi'); }\n");
        File java = new File(work, "java");
        TranspileRequest req = new TranspileRequest().sourceRoot(src).outputDir(new File(work, "out"))
                .stubClasspathEntry(rt).javaSourceRoot(java).stateFile(new File(work, "state"));
        assertFalse(new DartTranspiler().transpile(req).hasErrors());
        assertFalse(out(work, "geolocator", "Geolocator").exists());

        // Only Java changed, so the Dart digest alone would call this up to date.
        write(new File(java, "app/Main.java"), "package app;\nclass Main {\n"
                + "  void f() { com.codename1.flutter.plugins.geolocator.Geolocator.g(); }\n}\n");
        TranspileResult r = new DartTranspiler().transpile(req);
        assertFalse(r.hasErrors());
        assertFalse(r.isUpToDate());
        assertTrue(out(work, "geolocator", "Geolocator").isFile());

        // A copied pack file deleted by hand fails the cache check, not the compile.
        assertTrue(out(work, "geolocator", "Geolocator").delete());
        r = new DartTranspiler().transpile(req);
        assertFalse(r.isUpToDate());
        assertTrue(out(work, "geolocator", "Geolocator").isFile());
    }

    @Test
    void namedReadsWholeIdentifiers() {
        assertEquals("[firebase, share]", PluginPacks.named(
                "import com.codename1.flutter.plugins.firebase.FirebaseAuth;\n"
                + "x = com.codename1.flutter.plugins.share.Share.share(y);").toString());
    }
}
