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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessorContext;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Covers the conversion from typed annotation back to wire-format build hint.
///
/// The cases that matter most are the silent ones: a hint written for an
/// attribute the developer never set, an enum written as its constant name
/// rather than the value the builder compares against, and a list joined with
/// the wrong delimiter. None of those fail a build -- the builder falls back to
/// a default or writes a malformed fragment -- so only a test catches them.
public class BuildHintAnnotationProcessorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String MAIN = "com.example.MyApp";

    // ------------------------------------------------------------------
    // value conversion
    // ------------------------------------------------------------------

    @Test
    public void aBooleanAttributeIsWrittenAsTrueOrFalse() throws Exception {
        Properties p = hintsOf("@Ios(newStorageLocation = true)");
        assertEquals("true", p.getProperty("codename1.arg.ios.newStorageLocation"));

        p = hintsOf("@Ios(newStorageLocation = false)");
        assertEquals("false", p.getProperty("codename1.arg.ios.newStorageLocation"));
    }

    @Test
    public void anIntAttributeIsStringified() throws Exception {
        Properties p = hintsOf("@Desktop(width = 1280, height = 720)");
        assertEquals("1280", p.getProperty("codename1.arg.desktop.width"));
        assertEquals("720", p.getProperty("codename1.arg.desktop.height"));
    }

    @Test
    public void aStringAttributeIsWrittenVerbatim() throws Exception {
        Properties p = hintsOf("@Ios(teamId = \"ABCDE12345\")");
        assertEquals("ABCDE12345", p.getProperty("codename1.arg.ios.teamId"));
    }

    /// The builder compares against the catalog's value, not the constant name.
    /// `IOS7` happens to lowercase to `ios7`, but `INTERNAL_ONLY` does not
    /// lowercase to `internalOnly`, and a builder given an unrecognized value
    /// silently uses its default -- so a name-based conversion would fail with
    /// no diagnostic anywhere.
    @Test
    public void anEnumAttributeUsesTheCatalogValueNotTheConstantName() throws Exception {
        Properties p = hintsOf("@Android(installLocation = InstallLocation.INTERNAL_ONLY)");
        assertEquals("internalOnly", p.getProperty("codename1.arg.android.installLocation"));

        p = hintsOf("@Ios(themeMode = IosThemeMode.IOS7)");
        assertEquals("ios7", p.getProperty("codename1.arg.ios.themeMode"));

        p = hintsOf("@Desktop(titleBar = DesktopTitleBar.TOOLBAR)");
        assertEquals("toolbar", p.getProperty("codename1.arg.desktop.titleBar"));
    }

    @Test
    public void aStringArrayIsJoinedWithTheHintsOwnSeparator() throws Exception {
        // ios.pods is comma delimited, ios.add_libs is semicolon delimited --
        // the same shape in Java, two different wire formats.
        Properties p = hintsOf("@Ios(pods = {\"Alamofire\", \"SwiftyJSON\"}, "
                + "addLibs = {\"libz.tbd\", \"libsqlite3.tbd\"})");
        assertEquals("Alamofire,SwiftyJSON", p.getProperty("codename1.arg.ios.pods"));
        assertEquals("libz.tbd;libsqlite3.tbd", p.getProperty("codename1.arg.ios.add_libs"));
    }

    @Test
    public void aNewlineDelimitedListSurvivesThePropertiesRoundTrip() throws Exception {
        Properties p = hintsOf("@Android(proguardKeep = {\"-keep class com.a.** { *; }\", "
                + "\"-keep class com.b.** { *; }\"})");
        assertEquals("-keep class com.a.** { *; }\n-keep class com.b.** { *; }",
                p.getProperty("codename1.arg.android.proguardKeep"));
    }

    // ------------------------------------------------------------------
    // set vs unset
    // ------------------------------------------------------------------

    /// The load-bearing one. javac omits a member left at its default from the
    /// class file, which is the only thing distinguishing "not set" from "set
    /// to the default". Reading attributes through a getOrDefault would write
    /// a hint for every attribute of every annotation the project uses.
    @Test
    public void anAttributeThatWasNotWrittenProducesNoHint() throws Exception {
        Properties p = hintsOf("@Ios(pods = {\"Alamofire\"})");
        assertEquals("Alamofire", p.getProperty("codename1.arg.ios.pods"));
        assertNull("an unset attribute must not be written at all",
                p.getProperty("codename1.arg.ios.newStorageLocation"));
        assertNull(p.getProperty("codename1.arg.ios.objC"));
        assertNull(p.getProperty("codename1.arg.ios.teamId"));
    }

    /// The other half of the same contract: a value the developer typed is
    /// written even when it equals the annotation's declared default, because
    /// typing it is a statement of intent.
    @Test
    public void anExplicitlyWrittenDefaultValueIsStillEmitted() throws Exception {
        // ios.objC is declared `default true` by the generator.
        Properties p = hintsOf("@Ios(objC = true)");
        assertEquals("true", p.getProperty("codename1.arg.ios.objC"));
    }

    // ------------------------------------------------------------------
    // determinism and cleanup
    // ------------------------------------------------------------------

    @Test
    public void theEmittedResourceIsByteStableAcrossRuns() throws Exception {
        String src = "@Ios(pods = {\"A\", \"B\"}, teamId = \"T\")\n@Desktop(width = 640)";
        byte[] first = rawResource(src);
        byte[] second = rawResource(src);
        assertTrue("the emitted resource must not change between identical builds",
                Arrays.equals(first, second));
    }

    @Test
    public void removingTheLastAnnotationRemovesTheGeneratedResource() throws Exception {
        File classes = compile("@Ios(teamId = \"T\")");
        ProcessorContext ctx = run(classes, settings(), MAIN, true);
        File emitted = new File(classes,
                BuildHintAnnotationProcessor.MANIFEST_RESOURCE);
        emitted.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(emitted);
        out.write(ctx.getEmittedResources().get(
                BuildHintAnnotationProcessor.MANIFEST_RESOURCE));
        out.close();
        assertTrue(emitted.exists());

        // Recompile with no build hint annotation at all.
        File plain = compile("");
        // Point the processor at the directory still holding yesterday's file.
        copyInto(plain, emitted);
        run(plain, settings(), MAIN, true);
        assertFalse("a stale build-hints resource would ship hints the project no longer "
                + "declares", new File(plain,
                BuildHintAnnotationProcessor.MANIFEST_RESOURCE).exists());
    }

    // ------------------------------------------------------------------
    // placement
    // ------------------------------------------------------------------

    @Test
    public void annotationsOnANonMainClassAreRejected() throws Exception {
        File classes = compile("@Ios(teamId = \"T\")");
        ProcessorContext ctx = run(classes, settings(), "com.example.SomethingElse", false);
        assertErrorContaining(ctx, "belong on the application's main class");
    }

    @Test
    public void aModuleWithNoMainClassIsRejected() throws Exception {
        File classes = compile("@Ios(teamId = \"T\")");
        ProcessorContext ctx = run(classes, settings(), null, false);
        assertErrorContaining(ctx, "declares no codename1.mainName");
    }

    /// A project that uses none of these annotations must not start failing
    /// because it has no main class -- a cn1lib, for instance.
    @Test
    public void aModuleWithNoAnnotationsAndNoMainClassIsFine() throws Exception {
        File classes = compile("");
        ProcessorContext ctx = run(classes, settings(), null, true);
        assertFalse(ctx.hasErrors());
    }

    // ------------------------------------------------------------------
    // conflicts with the properties file
    // ------------------------------------------------------------------

    @Test
    public void aHintSetInBothPlacesIsAnError() throws Exception {
        Properties s = settings();
        s.setProperty("codename1.arg.ios.teamId", "FROMFILE");
        File classes = compile("@Ios(teamId = \"FROMANNOTATION\")");
        ProcessorContext ctx = run(classes, s, MAIN, false);
        assertErrorContaining(ctx, "codename1.arg.ios.teamId is declared twice");
        assertErrorContaining(ctx, "@Ios(teamId)");
    }

    @Test
    public void aHintOnlyInThePropertiesFileIsFine() throws Exception {
        Properties s = settings();
        s.setProperty("codename1.arg.ios.plistInject", "<key>X</key>");
        File classes = compile("@Ios(teamId = \"T\")");
        ProcessorContext ctx = run(classes, s, MAIN, true);
        assertFalse(ctx.hasErrors());
    }

    /// A commented-out line is not a declaration, and the archetype ships
    /// several. Properties.load skips them, so this is really a guard against
    /// anyone reintroducing a hand-rolled line scan.
    @Test
    public void aCommentedOutPropertyIsNotAConflict() throws Exception {
        Properties s = new Properties();
        s.load(new ByteArrayInputStream(
                ("codename1.mainName=MyApp\ncodename1.packageName=com.example\n"
                 + "#codename1.arg.ios.teamId=OLD\n").getBytes("ISO-8859-1")));
        File classes = compile("@Ios(teamId = \"T\")");
        ProcessorContext ctx = run(classes, s, MAIN, true);
        assertFalse(ctx.hasErrors());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static String source(String annotations) {
        return "package com.example;\n"
                + "import com.codename1.annotations.buildhints.*;\n"
                + annotations + "\n"
                + "public class MyApp {\n}\n";
    }

    private File compile(String annotations) throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(MAIN, source(annotations)),
                classes, Arrays.asList(testClassesDir(), coreJar()));
        return classes;
    }

    private ProcessorContext run(File classes, Properties settings, String mainClass,
                                 boolean expectClean) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        BuildHintAnnotationProcessor proc = new BuildHintAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog(), tmp.newFolder(), settings, mainClass);
        proc.start(ctx);
        for (AnnotatedClass cls : index.values()) {
            proc.processClass(cls, ctx);
        }
        proc.finish(ctx);
        if (expectClean && ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("unexpected errors:\n");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
                sb.append("  ").append(e).append('\n');
            }
            fail(sb.toString());
        }
        return ctx;
    }

    private byte[] rawResource(String annotations) throws Exception {
        ProcessorContext ctx = run(compile(annotations), settings(), MAIN, true);
        byte[] bytes = ctx.getEmittedResources()
                .get(BuildHintAnnotationProcessor.MANIFEST_RESOURCE);
        assertTrue("build-hints.properties must be emitted", bytes != null);
        return bytes;
    }

    private Properties hintsOf(String annotations) throws Exception {
        Properties p = new Properties();
        p.load(new ByteArrayInputStream(rawResource(annotations)));
        return p;
    }

    private static Properties settings() {
        Properties p = new Properties();
        p.setProperty("codename1.mainName", "MyApp");
        p.setProperty("codename1.packageName", "com.example");
        return p;
    }

    private static void assertErrorContaining(ProcessorContext ctx, String fragment) {
        assertTrue("expected a validation error", ctx.hasErrors());
        StringBuilder all = new StringBuilder();
        for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
            all.append(e).append('\n');
        }
        assertTrue("expected an error containing \"" + fragment + "\" but got:\n" + all,
                all.toString().contains(fragment));
    }

    private static void copyInto(File classesDir, File existing) throws Exception {
        File target = new File(classesDir, BuildHintAnnotationProcessor.MANIFEST_RESOURCE);
        target.getParentFile().mkdirs();
        java.nio.file.Files.copy(existing.toPath(), target.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static File testClassesDir() throws Exception {
        URL url = BuildHintAnnotationProcessorTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }

    /// The generated annotations live in codenameone-core, which is already a
    /// dependency of the plugin, so the compiled sources can reference them.
    private static File coreJar() throws Exception {
        URL url = Class.forName("com.codename1.annotations.buildhints.Ios")
                .getProtectionDomain().getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
