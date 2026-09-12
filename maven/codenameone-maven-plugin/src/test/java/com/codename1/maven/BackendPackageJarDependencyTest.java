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
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The compile classpath has to reach the translator as one first-wins tree.
///
/// ByteCodeTranslator walks every input with File.listFiles, which answers null
/// for a jar and is read as an empty directory -- so a dependency resolved from
/// the repository contributed nothing, silently, and the build failed later while
/// linking the symbols of classes it had never been shown. A module in the same
/// reactor resolves to its target/classes, which is why this never showed in the
/// generated project's own contract module.
///
/// And order is precedence: Parser.classIndex keeps the FIRST definition it
/// parsed, exactly as javac resolves the same classpath, so anything that
/// reorders the inputs compiles against one definition and translates another.
/// Staging into one tree settles it on disk and leaves no order to get wrong.
class BackendPackageJarDependencyTest {

    @Test
    void stagesEverythingIntoOneTree(@TempDir File tmp) throws Exception {
        File first = new File(tmp, "first.jar");
        writeJar(first,
                "com/example/Dto.class", "FIRST",
                "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n",
                "cn1-native/dep.c", "int dep(void) { return 1; }\n");
        File second = new File(tmp, "second.jar");
        writeJar(second, "com/example/Dto.class", "SECOND",
                "com/example/Only.class", "ONLY");
        File asDirectory = new File(tmp, "sibling-classes");
        writeFile(new File(asDirectory, "com/example/Dto.class"), "DIRECTORY");
        writeFile(new File(asDirectory, "com/example/FromDir.class"), "FROMDIR");
        File staged = new File(tmp, "dependency-classes");
        assertTrue(staged.mkdirs());
        File natives = new File(tmp, "native");
        assertTrue(natives.mkdirs());

        List<String> inputs = stage(Arrays.asList(first.getAbsolutePath(),
                asDirectory.getAbsolutePath(), second.getAbsolutePath()),
                staged, natives);

        assertEquals(Collections.singletonList(staged.getAbsolutePath()), inputs,
                "one input, so there is no order for the translator to resolve: " + inputs);
        assertEquals("FIRST", read(new File(staged, "com/example/Dto.class")),
                "the first classpath entry wins the class three of them carry, "
                        + "which is what javac did when the module was compiled");
        assertEquals("ONLY", read(new File(staged, "com/example/Only.class")),
                "a class only the last jar carries is still staged");
        assertEquals("FROMDIR", read(new File(staged, "com/example/FromDir.class")),
                "and a class only the directory carries, which is copied in rather "
                        + "than passed through");
        assertFalse(new File(staged, "META-INF").exists(),
                "META-INF belongs to the jar, not to the translation");
        assertTrue(new File(natives, "dep.c").isFile(),
                "a dependency's cn1-native belongs with the runtime's");
    }

    @Test
    void letsADirectoryWinWhenItComesFirst(@TempDir File tmp) throws Exception {
        // The other order, which is the half a reverse traversal got wrong: a
        // directory ahead of a jar has to keep its precedence.
        File asDirectory = new File(tmp, "sibling-classes");
        writeFile(new File(asDirectory, "com/example/Dto.class"), "DIRECTORY");
        File jar = new File(tmp, "later.jar");
        writeJar(jar, "com/example/Dto.class", "JAR");
        File staged = new File(tmp, "dependency-classes");
        assertTrue(staged.mkdirs());
        File natives = new File(tmp, "native");
        assertTrue(natives.mkdirs());

        stage(Arrays.asList(asDirectory.getAbsolutePath(), jar.getAbsolutePath()),
                staged, natives);

        assertEquals("DIRECTORY", read(new File(staged, "com/example/Dto.class")),
                "the earlier entry wins whichever kind it is");
    }

    @Test
    void addsNoInputWhenThereAreNoDependencies(@TempDir File tmp) throws Exception {
        File staged = new File(tmp, "dependency-classes");
        assertTrue(staged.mkdirs());
        File natives = new File(tmp, "native");
        assertTrue(natives.mkdirs());

        assertTrue(stage(Collections.<String>emptyList(), staged, natives).isEmpty(),
                "an empty staging directory is not an input");
    }

    private static List<String> stage(List<String> classpath, File staged, File natives)
            throws Exception {
        BackendPackageMojo mojo = new BackendPackageMojo();
        Method stageClasses = BackendPackageMojo.class.getDeclaredMethod(
                "stageDependencyClasses", List.class, File.class, File.class);
        stageClasses.setAccessible(true);
        Object out = stageClasses.invoke(mojo, classpath, staged, natives);
        return new ArrayList<String>((List<String>) out);
    }

    private static String read(File file) throws Exception {
        assertTrue(file.isFile(), "nothing was staged at " + file);
        return new String(Files.readAllBytes(file.toPath()), "UTF-8");
    }

    private static void writeFile(File file, String content) throws Exception {
        assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(content.getBytes("UTF-8"));
        } finally {
            out.close();
        }
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
