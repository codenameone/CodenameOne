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
import static org.junit.Assert.assertFalse;
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
                + "cn1.buildHints.sourceDigest=" + digestOf("@Ios(teamId = \"ABCDE12345\")")
                + "\ncodename1.arg.ios.pods=Alamofire\n");
        writeAnnotatedClass(classes);
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
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest=" + digestOf("@Ios(teamId = \"ABCDE12345\")")
                + "\n");
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

    /**
     * The processor ran once and then stopped -- goal unbound, skipped, or moved
     * to a phase that no longer runs -- and the annotation changed afterwards.
     * Nothing clears {@code target/classes}, so the old manifest is still there,
     * still naming the right main class. Trusting it ships the previous values
     * and hides the fact that the goal is not running at all.
     */
    @Test
    public void aManifestThatDoesNotMatchTheCompiledAnnotationsIsRefused() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest=" + digestOf("@Ios(teamId = \"OLD\")") + "\n"
                + "codename1.arg.ios.teamId=OLD\n");
        writeAnnotatedClass(classes);   // compiled with teamId = ABCDE12345

        Properties target = new Properties();
        try {
            merge(target, classes, "MyApp", "com.example");
            fail("expected the stale manifest to be refused");
        } catch (InvocationTargetException ex) {
            assertTrue(String.valueOf(ex.getCause().getMessage()),
                    ex.getCause().getMessage().contains("left over from an earlier build"));
        }
        assertNull("the stale value must not have been applied",
                target.getProperty("codename1.arg.ios.teamId"));
    }

    /** The fingerprint of the annotations the build actually compiled matches. */
    @Test
    public void aManifestGeneratedFromTheCompiledAnnotationsIsAccepted() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest="
                + digestOf("@Ios(teamId = \"ABCDE12345\")") + "\n"
                + "codename1.arg.ios.teamId=ABCDE12345\n");
        writeAnnotatedClass(classes);

        Properties target = new Properties();
        merge(target, classes, "MyApp", "com.example");

        assertEquals("ABCDE12345", target.getProperty("codename1.arg.ios.teamId"));
    }

    /**
     * A manifest with no fingerprint was not written by the processor, which
     * always records one.
     *
     * <p>Anything on the classpath can carry this file name -- a dependency, or
     * a copy a project keeps in {@code src/main/resources} -- and taking one at
     * face value both applied somebody else's hints and counted as proof that
     * the processor ran, which is what suppresses the refusal when the goal is
     * not bound at all. So the build refuses rather than shipping an app whose
     * annotated hints are silently missing.</p>
     */
    @Test
    public void aManifestWithNoFingerprintIsNotAnnotationOutput() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "codename1.arg.ios.teamId=OLD\n");
        writeAnnotatedClass(classes);

        Properties target = new Properties();
        try {
            merge(target, classes, "MyApp", "com.example");
            fail("expected the build to be refused");
        } catch (InvocationTargetException ex) {
            assertTrue(String.valueOf(ex.getCause().getMessage()),
                    ex.getCause().getMessage().contains("process-annotations"));
        }
        assertNull("somebody else's value must not have been applied",
                target.getProperty("codename1.arg.ios.teamId"));
    }

    /**
     * ...and one that names no main class is not ours either.
     *
     * <p>The digest here MATCHES the compiled class, so the fingerprint check
     * cannot be what rejects it -- this is the stamp doing the work.</p>
     */
    @Test
    public void aManifestWithNoStampIsNotAnnotationOutput() throws Exception {
        File classes = manifest("cn1.buildHints.sourceDigest="
                + digestOf("@Ios(teamId = \"ABCDE12345\")") + "\n"
                + "codename1.arg.ios.teamId=OLD\n");
        writeAnnotatedClass(classes);

        Properties target = new Properties();
        try {
            merge(target, classes, "MyApp", "com.example");
            fail("expected the build to be refused");
        } catch (InvocationTargetException ex) {
            assertTrue(String.valueOf(ex.getCause().getMessage()),
                    ex.getCause().getMessage().contains("process-annotations"));
        }
        assertNull(target.getProperty("codename1.arg.ios.teamId"));
    }

    /**
     * The fingerprint covers the annotations and nothing else, so editing the
     * properties file cannot invalidate it. With processing skipped, a line added
     * for a hint an annotation already sets left a manifest that still matched --
     * and the overlay quietly replaced the value the developer had just written,
     * until the next clean build regenerated the manifest and failed instead.
     */
    @Test
    public void aPropertiesLineForAnAnnotatedHintIsRefusedNotOverwritten() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest="
                + digestOf("@Ios(teamId = \"ABCDE12345\")") + "\n"
                + "cn1.buildHints.origin.ios.teamId=@Ios(teamId)\n"
                + "codename1.arg.ios.teamId=ABCDE12345\n");
        writeAnnotatedClass(classes);

        Properties target = new Properties();
        target.setProperty("codename1.arg.ios.teamId", "FROMFILE");
        try {
            merge(target, classes, "MyApp", "com.example");
            fail("expected the duplicate declaration to be refused");
        } catch (InvocationTargetException ex) {
            String message = String.valueOf(ex.getCause().getMessage());
            assertTrue(message, message.contains("declared twice"));
            assertTrue(message, message.contains("@Ios(teamId)"));
        }
        assertEquals("the file's value must not have been replaced",
                "FROMFILE", target.getProperty("codename1.arg.ios.teamId"));
    }

    /** A hint only the file sets is not a conflict -- that is the escape hatch. */
    @Test
    public void aPropertiesLineForAnUnannotatedHintIsLeftAlone() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest="
                + digestOf("@Ios(teamId = \"ABCDE12345\")") + "\n"
                + "codename1.arg.ios.teamId=ABCDE12345\n");
        writeAnnotatedClass(classes);

        Properties target = new Properties();
        target.setProperty("codename1.arg.ios.pods", "Alamofire");

        merge(target, classes, "MyApp", "com.example");

        assertEquals("Alamofire", target.getProperty("codename1.arg.ios.pods"));
        assertEquals("ABCDE12345", target.getProperty("codename1.arg.ios.teamId"));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Accepting a manifest does not mean the processor ran THIS build. Its
     * fingerprint covers the main class, so an annotation added to a live class
     * beside it leaves the manifest looking entirely current -- and with the
     * goal unbound or skipped the build succeeded having neither applied that
     * class's hints nor said the annotation was in the wrong place.
     */
    @Test
    public void aMisplacedAnnotationIsRefusedEvenWhenTheManifestIsCurrent() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest="
                + digestOf("@Ios(teamId = \"ABCDE12345\")") + "\n"
                + "codename1.arg.ios.teamId=ABCDE12345\n");
        writeAnnotatedClass(classes);
        File src = writeAnnotatedHelperSource();
        compileInto(classes, "com.example.Helper", helperSource());

        Properties target = new Properties();
        try {
            merge(target, classes, "MyApp", "com.example", src);
            fail("expected the misplaced annotation to be refused");
        } catch (InvocationTargetException ex) {
            assertTrue(String.valueOf(ex.getCause().getMessage()),
                    ex.getCause().getMessage().contains("com.example.Helper"));
        }
    }

    /**
     * The class's OWN module says where its sources are.
     *
     * <p>In the generated layout the application's classes come from `common`
     * while a platform module runs the build. Asking the running project where
     * its sources are answers for the wrong module, so every class compiled from
     * `common` has no backing source, reads as stale, and its misplaced
     * annotation goes unreported -- with the goal unbound or skipped the build
     * then succeeds having neither applied the hint nor said anything.</p>
     */
    @Test
    public void theRootsComeFromTheModuleThatProducedTheClasses() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest="
                + digestOf("@Ios(teamId = \"ABCDE12345\")") + "\n"
                + "codename1.arg.ios.teamId=ABCDE12345\n");
        writeAnnotatedClass(classes);
        File src = writeAnnotatedHelperSource();
        compileInto(classes, "com.example.Helper", helperSource());

        CN1BuildMojo mojo = new CN1BuildMojo();
        // The module that RUNS: a platform module, with a source root of its own
        // that knows nothing about com.example.Helper.
        org.apache.maven.project.MavenProject running =
                new org.apache.maven.project.MavenProject();
        running.addCompileSourceRoot(tmp.newFolder().getAbsolutePath());
        set(mojo, "project", running);
        // The module that PRODUCED the classes, which is where the source is.
        org.apache.maven.project.MavenProject owner =
                new org.apache.maven.project.MavenProject();
        owner.setBuild(new org.apache.maven.model.Build());
        owner.getBuild().setOutputDirectory(classes.getAbsolutePath());
        owner.addCompileSourceRoot(src.getAbsolutePath());
        set(mojo, "reactorProjects", java.util.Arrays.asList(running, owner));

        Properties settings = new Properties();
        settings.setProperty("codename1.mainName", "MyApp");
        settings.setProperty("codename1.packageName", "com.example");
        set(mojo, "properties", settings);

        Method m = findMethod(mojo.getClass(), "mergeAnnotationBuildHints",
                Properties.class, List.class);
        m.setAccessible(true);
        try {
            m.invoke(mojo, new Properties(),
                    Collections.singletonList(classes.getAbsolutePath()));
            fail("expected the misplaced annotation to be refused");
        } catch (InvocationTargetException ex) {
            assertTrue(String.valueOf(ex.getCause().getMessage()),
                    ex.getCause().getMessage().contains("com.example.Helper"));
        }
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = findField(target.getClass(), field);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * `-D` wins over an annotation whichever spelling it uses.
     *
     * <p>An alias and its target are one effective setting, and the overlay that
     * applies the command line only replaces the SAME key. So
     * `-Dcodename1.arg.cn1.androidTheme` against an annotated `and.themeMode`
     * left both set -- and the two readers then disagreed: JavaSEPort takes the
     * canonical and falls back to the alias, so the annotation won in the
     * simulator, while AndroidGradleBuilder writes both properties with the
     * alias last, so `-D` won on the device.</p>
     */
    @Test
    public void aCommandLineAliasBeatsTheAnnotation() {
        Properties deprecatedSpelling = new Properties();
        deprecatedSpelling.setProperty("codename1.arg.cn1.androidTheme", "legacy");
        assertTrue("the deprecated spelling sets the same hint",
                CN1BuildMojo.overriddenOnTheCommandLine(
                        "codename1.arg.and.themeMode", deprecatedSpelling));

        // ...and the other way round: the canonical on the command line against
        // an annotation that happens to write the alias.
        Properties canonical = new Properties();
        canonical.setProperty("codename1.arg.and.themeMode", "modern");
        assertTrue(CN1BuildMojo.overriddenOnTheCommandLine(
                "codename1.arg.cn1.androidTheme", canonical));

        // The same key is of course still the same key.
        assertTrue(CN1BuildMojo.overriddenOnTheCommandLine(
                "codename1.arg.and.themeMode", canonical));
    }

    /** ...and an unrelated -D leaves the annotation value alone. */
    @Test
    public void anUnrelatedCommandLineHintDoesNotSuppressTheAnnotation() {
        Properties unrelated = new Properties();
        unrelated.setProperty("codename1.arg.ios.pods", "Alamofire");
        unrelated.setProperty("maven.test.skip", "true");
        assertFalse(CN1BuildMojo.overriddenOnTheCommandLine(
                "codename1.arg.and.themeMode", unrelated));
        assertFalse(CN1BuildMojo.overriddenOnTheCommandLine(
                "codename1.arg.and.themeMode", new Properties()));
        assertFalse(CN1BuildMojo.overriddenOnTheCommandLine("codename1.arg.and.themeMode", null));
    }

    /** The main class carrying them is the whole point, so it is not a misplacement. */
    @Test
    public void theMainClassCarryingAnnotationsIsNotAMisplacement() throws Exception {
        File classes = manifest("cn1.buildHints.mainClass=com.example.MyApp\n"
                + "cn1.buildHints.sourceDigest="
                + digestOf("@Ios(teamId = \"ABCDE12345\")") + "\n"
                + "codename1.arg.ios.teamId=ABCDE12345\n");
        writeAnnotatedClass(classes);

        Properties target = new Properties();
        merge(target, classes, "MyApp", "com.example", writeAnnotatedHelperSource());

        assertEquals("ABCDE12345", target.getProperty("codename1.arg.ios.teamId"));
    }

    private static String helperSource() {
        return "package com.example;\n"
                + "import com.codename1.annotations.buildhints.Ios;\n"
                + "@Ios(pods = \"Alamofire\")\n"
                + "public class Helper {\n}\n";
    }

    /** A source root holding Helper.java, so the class counts as live. */
    private File writeAnnotatedHelperSource() throws Exception {
        File src = tmp.newFolder();
        File f = new File(src, "com/example/Helper.java");
        f.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8")) {
            w.write(helperSource());
        }
        return src;
    }

    private void compileInto(File classes, String binaryName, String source) throws Exception {
        com.codename1.maven.annotations.JavaSourceCompiler.compile(
                com.codename1.maven.annotations.JavaSourceCompiler.singleSource(
                        binaryName, source),
                classes,
                Arrays.asList(new File(Class.forName("com.codename1.annotations.buildhints.Ios")
                        .getProtectionDomain().getCodeSource().getLocation().toURI())));
    }

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
        merge(target, classesDir, mainName, pkg, null);
    }

    private void merge(Properties target, File classesDir, String mainName, String pkg,
                       File sourceRoot) throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        if (sourceRoot != null) {
            org.apache.maven.project.MavenProject p = new org.apache.maven.project.MavenProject();
            p.addCompileSourceRoot(sourceRoot.getAbsolutePath());
            Field proj = findField(mojo.getClass(), "project");
            proj.setAccessible(true);
            proj.set(mojo, p);
        }

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

    /** The fingerprint the processor would record for a main class annotated so. */
    private String digestOf(String annotations) throws Exception {
        File dir = tmp.newFolder();
        com.codename1.maven.annotations.JavaSourceCompiler.compile(
                com.codename1.maven.annotations.JavaSourceCompiler.singleSource(
                        "com.example.MyApp",
                        "package com.example;\n"
                                + "import com.codename1.annotations.buildhints.*;\n"
                                + annotations + "\n"
                                + "public class MyApp {\n}\n"),
                dir,
                Arrays.asList(new File(Class.forName("com.codename1.annotations.buildhints.Ios")
                        .getProtectionDomain().getCodeSource().getLocation().toURI())));
        return com.codename1.maven.processors.BuildHintAnnotationProcessor.sourceDigest(
                com.codename1.maven.annotations.ClassScanner.readClass(
                        new File(dir, "com/example/MyApp.class")));
    }
}
