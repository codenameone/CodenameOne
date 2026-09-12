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
package com.codename1.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A jar on the compile classpath has to be unpacked before the translator sees it.
///
/// ByteCodeTranslator walks every input with File.listFiles, which answers null
/// for a jar and is read as an empty directory -- so a dependency resolved from
/// the repository contributed nothing, silently, and the build failed later while
/// linking the symbols of classes it had never been shown. A module in the same
/// reactor resolves to its target/classes, which is why this never showed in the
/// generated project's own contract module.
class BackendPackageJarDependencyTest {

    @Test
    void unpacksAJarAndLeavesADirectoryAlone(@TempDir File tmp) throws Exception {
        File first = new File(tmp, "first.jar");
        writeJar(first,
                "com/example/Dto.class", "FIRST",
                "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n",
                "cn1-native/dep.c", "int dep(void) { return 1; }\n");
        File second = new File(tmp, "second.jar");
        writeJar(second, "com/example/Dto.class", "SECOND",
                "com/example/Only.class", "ONLY");
        File asDirectory = new File(tmp, "sibling-classes");
        assertTrue(new File(asDirectory, "com/example").mkdirs());
        File unpacked = new File(tmp, "dependency-classes");
        assertTrue(unpacked.mkdirs());
        File natives = new File(tmp, "native");
        assertTrue(natives.mkdirs());

        List<String> inputs = unpack(Arrays.asList(first.getAbsolutePath(),
                asDirectory.getAbsolutePath(), second.getAbsolutePath()),
                unpacked, natives);

        // The point of the change: no jar may reach the translator as a jar.
        for (int i = 0; i < inputs.size(); i++) {
            assertFalse(inputs.get(i).endsWith(".jar"),
                    "a jar cannot be a translator input: " + inputs);
        }
        assertTrue(inputs.contains(asDirectory.getAbsolutePath()),
                "a classpath entry that is already a directory is passed through: " + inputs);
        assertTrue(inputs.contains(unpacked.getAbsolutePath()),
                "the unpacked jars have to be an input: " + inputs);
        assertTrue(new File(unpacked, "com/example/Only.class").isFile(),
                "a class only the second jar carries was dropped");
        // javac's rule for the same classpath: the FIRST entry wins.
        assertEquals("FIRST", read(new File(unpacked, "com/example/Dto.class")),
                "the earlier jar on the classpath must win the class both carry");
        assertFalse(new File(unpacked, "META-INF").exists(),
                "META-INF belongs to the jar, not to the translation");
        assertTrue(new File(natives, "dep.c").isFile(),
                "a dependency's cn1-native belongs with the runtime's");
    }

    @Test
    void addsNoInputWhenNothingNeededUnpacking(@TempDir File tmp) throws Exception {
        File asDirectory = new File(tmp, "sibling-classes");
        assertTrue(asDirectory.mkdirs());
        File unpacked = new File(tmp, "dependency-classes");
        assertTrue(unpacked.mkdirs());
        File natives = new File(tmp, "native");
        assertTrue(natives.mkdirs());

        List<String> inputs = unpack(java.util.Collections.singletonList(
                asDirectory.getAbsolutePath()), unpacked, natives);

        assertEquals(java.util.Collections.singletonList(asDirectory.getAbsolutePath()),
                inputs, "an empty directory of unpacked jars is not an input");
    }

    private static List<String> unpack(List<String> classpath, File unpacked, File natives)
            throws Exception {
        BackendPackageMojo mojo = new BackendPackageMojo();
        Method unpackJars = BackendPackageMojo.class.getDeclaredMethod(
                "unpackJarDependencies", List.class, File.class, File.class);
        unpackJars.setAccessible(true);
        Object out = unpackJars.invoke(mojo, classpath, unpacked, natives);
        return new ArrayList<String>((List<String>) out);
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), "UTF-8");
    }

    /** entries as name, content, name, content ... */
    private static void writeJar(File jar, String... entries) throws Exception {
        OutputStream raw = new FileOutputStream(jar);
        try {
            ZipOutputStream zip = new ZipOutputStream(raw);
            for (int i = 0; i < entries.length; i += 2) {
                zip.putNextEntry(new ZipEntry(entries[i]));
                zip.write(entries[i + 1].getBytes("UTF-8"));
                zip.closeEntry();
            }
            zip.finish();
        } finally {
            raw.close();
        }
    }
}
