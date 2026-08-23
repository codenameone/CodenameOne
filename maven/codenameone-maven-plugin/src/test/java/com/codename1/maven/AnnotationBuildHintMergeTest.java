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
package com.codename1.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Where the hints that came from annotations meet the build request.
 *
 * <p>The merge has to distinguish three states that look alike from here: the
 * processor ran and produced hints, it ran and produced none, and it never ran
 * at all. Only the third is a broken build, and getting that wrong either ships
 * an app with its build configuration silently missing or refuses one that is
 * perfectly configured.</p>
 */
public class AnnotationBuildHintMergeTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String RESOURCE = "META-INF/codenameone/build-hints.properties";

    @Test
    public void hintsFromTheManifestReachTheBuildRequest() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "codename1.arg.ios.pods=Alamofire\n");
        Properties target = new Properties();

        merge(target, classes, "MyApp", "com.example");

        assertEquals("Alamofire", target.getProperty("codename1.arg.ios.pods"));
    }

    /**
     * A manifest with no hints in it is what {@code @Ios()} produces once the
     * last attribute is deleted -- still legal, still processed. Judging by the
     * hint count alone read that as "the processor never ran" and refused every
     * build until the annotation itself was removed.
     */
    @Test
    public void anEmptyManifestIsProofTheProcessorRan() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n");
        // The annotated class has to be there too -- that is the whole situation:
        // an annotation the compiler recorded and a manifest that carries no hint
        // for it. Without the class the refusal path has nothing to trip on and
        // the test would pass against the bug it exists for.
        writeAnnotatedClass(classes);
        Properties target = new Properties();

        merge(target, classes, "MyApp", "com.example");

        assertTrue("no hint should have been applied", target.isEmpty());
    }

    /**
     * The refusal still has to fire for the case it exists for: annotations in
     * the compiled classes with no manifest anywhere means the goal is unbound,
     * and every annotated hint is missing from the build.
     */
    @Test
    public void annotatedClassesWithNoManifestAreRefused() throws Exception {
        File classes = tmp.newFolder();
        writeAnnotatedClass(classes);
        try {
            merge(new Properties(), classes, "MyApp", "com.example");
            fail("expected the build to be refused");
        } catch (InvocationTargetException ex) {
            assertTrue(String.valueOf(ex.getCause().getMessage()),
                    ex.getCause().getMessage().contains("process-annotations"));
        }
    }

    /** No annotations and no manifest is an ordinary properties-file project. */
    @Test
    public void aProjectWithNoAnnotationsIsLeftAlone() throws Exception {
        Properties target = new Properties();
        merge(target, tmp.newFolder(), "MyApp", "com.example");
        assertTrue(target.isEmpty());
    }

    /** A manifest stamped for another project is somebody else's configuration. */
    @Test
    public void aManifestStampedForAnotherMainClassIsIgnored() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.other.TheirApp\n"
                + "codename1.arg.ios.pods=Alamofire\n");
        Properties target = new Properties();

        merge(target, classes, "MyApp", "com.example");

        assertNull(target.getProperty("codename1.arg.ios.pods"));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private File manifest(String body) throws Exception {
        File classes = tmp.newFolder();
        File out = new File(classes, RESOURCE);
        out.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), "ISO-8859-1")) {
            w.write(body);
        }
        return classes;
    }

    /** Compiles a main class carrying one build hint annotation. */
    private void writeAnnotatedClass(File classes) throws Exception {
        com.codename1.maven.annotations.JavaSourceCompiler.compile(
                com.codename1.maven.annotations.JavaSourceCompiler.singleSource(
                        "com.example.MyApp",
                        "package com.example;\n"
                                + "import com.codename1.annotations.buildhints.Ios;\n"
                                + "@Ios(teamId = \"ABCDE12345\")\n"
                                + "public class MyApp {\n}\n"),
                classes,
                Arrays.asList(new File(Class.forName("com.codename1.annotations.buildhints.Ios")
                        .getProtectionDomain().getCodeSource().getLocation().toURI())));
    }

    /**
     * Drives the shipped merge rather than a restatement of it, so a change to
     * the rule is a change to what this asserts.
     */
    private void merge(Properties target, File classesDir, String mainName, String pkg)
            throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();

        Properties settings = new Properties();
        settings.setProperty("codename1.mainName", mainName);
        settings.setProperty("codename1.packageName", pkg);
        Field props = findField(mojo.getClass(), "properties");
        props.setAccessible(true);
        props.set(mojo, settings);

        List<String> cp = Collections.singletonList(classesDir.getAbsolutePath());
        Method m = findMethod(mojo.getClass(), "mergeAnnotationBuildHints",
                Properties.class, List.class);
        m.setAccessible(true);
        m.invoke(mojo, target, cp);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException keepLooking) {
                // up the chain
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... args)
            throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredMethod(name, args);
            } catch (NoSuchMethodException keepLooking) {
                // up the chain
            }
        }
        throw new NoSuchMethodException(name);
    }
}
