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
package com.codename1.maven.annotations;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DependencyClassesTest {

    private static final String GENERATED = "com/example/api/NotesApiImpl.class";

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /// The module's output directory is on its own compile classpath and holds
    /// the previous build's generated classes. Counting it made every build
    /// after the first conclude a dependency had generated them already.
    @Test
    public void ownOutputDoesNotCountAsADependency() throws Exception {
        File own = tmp.newFolder("own");
        File sibling = tmp.newFolder("sibling");
        touch(new File(own, GENERATED));
        List<String> classpath = Arrays.asList(own.getPath(), sibling.getPath());

        assertTrue(DependencyClasses.onClasspath(classpath, GENERATED));
        assertFalse(DependencyClasses.onClasspath(classpath, own, GENERATED));
    }

    @Test
    public void aSiblingDirectoryStillCounts() throws Exception {
        File own = tmp.newFolder("own");
        File sibling = tmp.newFolder("sibling");
        touch(new File(own, GENERATED));
        touch(new File(sibling, GENERATED));

        assertTrue(DependencyClasses.onClasspath(
                Arrays.asList(own.getPath(), sibling.getPath()), own, GENERATED));
    }

    @Test
    public void anArchiveStillCounts() throws Exception {
        File own = tmp.newFolder("own");
        File jar = new File(tmp.getRoot(), "dependency.jar");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar));
        try {
            out.putNextEntry(new ZipEntry(GENERATED));
            out.closeEntry();
        } finally {
            out.close();
        }

        assertTrue(DependencyClasses.onClasspath(
                Arrays.asList(own.getPath(), jar.getPath()), own, GENERATED));
        assertFalse(DependencyClasses.onClasspath(
                Arrays.asList(own.getPath(), jar.getPath()), own, "com/example/Missing.class"));
    }

    /// The output directory is compared canonically: Maven hands the classpath
    /// entry and the configured directory over spelled differently.
    @Test
    public void ownOutputIsRecognizedThroughADifferentSpelling() throws Exception {
        File own = tmp.newFolder("own");
        touch(new File(own, GENERATED));
        File respelled = new File(new File(own, "com"), "..");

        assertFalse(DependencyClasses.onClasspath(
                Arrays.asList(respelled.getPath()), own, GENERATED));
    }

    /// Only a class carrying a wanted annotation is returned, the module's own
    /// output is left out, and the first definition of a name wins -- from a
    /// directory or an archive alike.
    @Test
    public void scanFindsAnnotatedClassesOfDependenciesOnly() throws Exception {
        File own = tmp.newFolder("own");
        File sibling = tmp.newFolder("sibling");
        File packed = tmp.newFolder("packed");
        compile(own, "com.example.Mine", "@Deprecated public class Mine {}");
        compile(sibling, "com.example.Marked", "@Deprecated public class Marked {}");
        compile(sibling, "com.example.Plain", "public class Plain {}");
        compile(packed, "com.example.Marked", "@Deprecated public interface Marked {}");
        compile(packed, "com.example.Archived", "@Deprecated public class Archived {}");
        File jar = jar(packed, "com/example/Marked.class", "com/example/Archived.class");

        java.util.Map<String, AnnotatedClass> found = DependencyClasses.scan(
                Arrays.asList(own.getPath(), sibling.getPath(), jar.getPath(),
                        new File(tmp.getRoot(), "absent").getPath()),
                own, java.util.Collections.singleton("Ljava/lang/Deprecated;"));

        assertEquals(new java.util.TreeSet<String>(Arrays.asList(
                "com/example/Marked", "com/example/Archived")),
                new java.util.TreeSet<String>(found.keySet()));
        assertFalse("the directory comes first on the classpath, so its class wins",
                found.get("com/example/Marked").isInterface());
    }

    /// The runtimes are thousands of classes and none of them the application's.
    @Test
    public void scanSkipsARuntime() throws Exception {
        File runtime = tmp.newFolder("runtime");
        compile(runtime, "com.example.Marked", "@Deprecated public class Marked {}");
        touch(new File(runtime, "com/codename1/ui/Display.class"));

        assertTrue(DependencyClasses.scan(Arrays.asList(runtime.getPath()), null,
                java.util.Collections.singleton("Ljava/lang/Deprecated;")).isEmpty());
        assertTrue(DependencyClasses.scan(null, null,
                java.util.Collections.singleton("Ljava/lang/Deprecated;")).isEmpty());
        assertTrue(DependencyClasses.scan(Arrays.asList(runtime.getPath()), null,
                java.util.Collections.<String>emptySet()).isEmpty());
    }

    @Test
    public void readFindsAnUnannotatedClassInADirectoryOrAnArchive() throws Exception {
        File sibling = tmp.newFolder("sibling");
        File packed = tmp.newFolder("packed");
        compile(sibling, "com.example.Level", "public enum Level { LOW, HIGH }");
        compile(packed, "com.example.Kind", "public enum Kind { A }");
        List<String> classpath = Arrays.asList(sibling.getPath(),
                jar(packed, "com/example/Kind.class").getPath());

        assertTrue(DependencyClasses.read(classpath, "com/example/Level").isEnum());
        assertTrue(DependencyClasses.read(classpath, "com/example/Kind").isEnum());
        assertNull(DependencyClasses.read(classpath, "com/example/Missing"));
        assertNull("the JDK is never read off the classpath",
                DependencyClasses.read(classpath, "java/lang/String"));
        assertNull(DependencyClasses.read(null, "com/example/Level"));
    }

    /// `lookup` keeps meaning "this module's", which several processors rely
    /// on; only `lookupWithDependencies` reaches a dependency, and it reads the
    /// classpath only in a build where dependency classes were offered at all.
    @Test
    public void theContextKeepsOwnAndDependencyClassesApart() throws Exception {
        File own = tmp.newFolder("own");
        File sibling = tmp.newFolder("sibling");
        compile(own, "com.example.Mine", "public class Mine {}");
        compile(sibling, "com.example.Marked", "@Deprecated public class Marked {}");
        compile(sibling, "com.example.Level", "public enum Level { LOW, HIGH }");
        List<String> classpath = Arrays.asList(own.getPath(), sibling.getPath());
        java.util.Map<String, AnnotatedClass> index = ClassScanner.scan(own);
        ProcessorContext ctx = new ProcessorContext(own, tmp.newFolder(), index,
                new com.codename1.build.SystemStreamLog(), null, null, null, null, null, classpath);

        assertTrue(ctx.getDependencyIndex().isEmpty());
        assertNotNull(ctx.lookupWithDependencies("com/example/Mine"));
        assertNull("nothing was offered, so the classpath is not read",
                ctx.lookupWithDependencies("com/example/Level"));

        java.util.Map<String, AnnotatedClass> dependencies = DependencyClasses.scan(classpath, own,
                java.util.Collections.singleton("Ljava/lang/Deprecated;"));
        ctx.setDependencyIndex(dependencies);
        AnnotatedClass marked = dependencies.get("com/example/Marked");

        assertNull(ctx.lookup("com/example/Marked"));
        assertSame(marked, ctx.lookupWithDependencies("com/example/Marked"));
        assertTrue(ctx.isDependencyClass(marked));
        assertFalse(ctx.isDependencyClass(index.get("com/example/Mine")));
        assertFalse(ctx.isDependencyClass(null));
        AnnotatedClass level = ctx.lookupWithDependencies("com/example/Level");
        assertTrue("a type a dependency class refers to is read on demand", level.isEnum());
        assertSame("and read once", level, ctx.lookupWithDependencies("com/example/Level"));
        assertNull(ctx.lookupWithDependencies("com/example/Missing"));

        ctx.setDependencyIndex(null);
        assertTrue(ctx.getDependencyIndex().isEmpty());
    }

    private static void compile(File into, String fqn, String declaration) throws Exception {
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(fqn, "package com.example;\n" + declaration + "\n"),
                into, java.util.Collections.<File>emptyList());
    }

    private File jar(File classes, String... entries) throws Exception {
        File jar = new File(tmp.getRoot(), classes.getName() + ".jar");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(jar));
        try {
            for (String entry : entries) {
                out.putNextEntry(new ZipEntry(entry));
                out.write(java.nio.file.Files.readAllBytes(new File(classes, entry).toPath()));
                out.closeEntry();
            }
        } finally {
            out.close();
        }
        return jar;
    }

    private static void touch(File file) throws Exception {
        assertTrue(file.getParentFile().isDirectory() || file.getParentFile().mkdirs());
        assertTrue(file.createNewFile());
    }
}
