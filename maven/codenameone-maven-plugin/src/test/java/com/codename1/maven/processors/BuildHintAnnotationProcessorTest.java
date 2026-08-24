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

import static org.junit.Assert.assertArrayEquals;
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

    /// A hint's deprecated alias names the same setting, so declaring the alias
    /// in the properties file collides with the annotation just as the canonical
    /// name would. Without this one value silently wins: AndroidGradleBuilder
    /// reads `and.themeMode` and falls back to `cn1.androidTheme`.
    @Test
    public void aDeprecatedAliasOfAnAnnotatedHintIsAConflict() throws Exception {
        Properties s = settings();
        s.setProperty("codename1.arg.cn1.androidTheme", "legacy");
        File classes = compile("@Android(themeMode = AndroidThemeMode.MODERN)");
        ProcessorContext ctx = run(classes, s, MAIN, false);
        assertErrorContaining(ctx, "codename1.arg.cn1.androidTheme is declared twice");
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

    /// An annotation with every member left at its default is legal Java, and
    /// `@Ios()` is what is left after the last attribute is deleted. The manifest
    /// is still emitted for it, carrying only the main-class stamp: its presence
    /// is what tells the build that processing ran at all, and dropping it here
    /// would make a harmless annotation indistinguishable from an unbound goal.
    @Test
    public void anAnnotationWithNoMembersStillEmitsAStampedManifest() throws Exception {
        Properties p = hintsOf("@Ios()");
        assertEquals(MAIN, p.getProperty("cn1.buildHints.mainClass"));
        for (String key : p.stringPropertyNames()) {
            assertFalse("no hint should have been written, got " + key,
                    key.startsWith("codename1.arg."));
        }
    }

    /// `and.captureRecord` is not an abbreviation: the builder reads
    /// `android.captureRecord` and then lets the short spelling override it, so
    /// the two name one setting. Without the alias the annotation and a
    /// properties line spelling it the short way were both accepted -- and the
    /// properties line wins in the builder, leaving the compile-checked
    /// annotation silently ineffective.
    @Test
    public void theShortSpellingOfAnAliasedHintStillConflicts() throws Exception {
        Properties s = new Properties();
        s.load(new ByteArrayInputStream(
                ("codename1.mainName=MyApp\ncodename1.packageName=com.example\n"
                 + "codename1.arg.and.captureRecord=disabled\n").getBytes("ISO-8859-1")));
        File classes = compile("@Android(captureRecord = \"enabled\")");
        ProcessorContext ctx = run(classes, s, MAIN, false);
        assertErrorContaining(ctx, "and.captureRecord");
    }

    /// A nested type's binary name is Main$Wrong and no source declares a type
    /// spelled that way, so looking for it found nothing and the class was
    /// dropped as an orphan -- taking the placement error with it and letting the
    /// build succeed with the requested hints silently absent.
    @Test
    public void anAnnotationOnANestedTypeIsStillReported() throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource(MAIN,
                        "package com.example;\n"
                                + "import com.codename1.annotations.buildhints.*;\n"
                                + "public class MyApp {\n"
                                + "    @Ios(teamId = \"ABCDE12345\")\n"
                                + "    public static class Wrong {}\n"
                                + "}\n"),
                classes, Arrays.asList(testClassesDir(), coreJar()));
        ProcessorContext ctx = run(classes, settings(), MAIN, false);
        assertErrorContaining(ctx, "belong on the application's main class");
    }

    /// What counts as a declaration has one answer, shared by the processor's
    /// orphan check and the migration goal's source lookup. These pin it.
    @Test
    public void aDeclarationIsFoundOnlyInCode() {
        assertTrue(BuildHintAnnotationProcessor.declaresType("public class MyApp {}", "MyApp"));
        assertTrue(BuildHintAnnotationProcessor.declaresType("object MyApp", "MyApp"));
        assertFalse(BuildHintAnnotationProcessor.declaresType("// class MyApp", "MyApp"));
        assertFalse(BuildHintAnnotationProcessor.declaresType("/* class MyApp */", "MyApp"));
        assertFalse(BuildHintAnnotationProcessor.declaresType("String s = \"class MyApp\";",
                "MyApp"));
        assertTrue(BuildHintAnnotationProcessor.declaresType(
                "// class MyApp\nclass MyApp {}", "MyApp"));
        assertFalse(BuildHintAnnotationProcessor.declaresType("class MyApplication {}", "MyApp"));
    }

    @Test
    public void thePackageIsReadFromCodeToo() {
        assertEquals("com.example",
                BuildHintAnnotationProcessor.declaredPackageIn("package com.example;\n"));
        assertEquals("com.example",
                BuildHintAnnotationProcessor.declaredPackageIn("package com.example\n"));
        assertEquals("", BuildHintAnnotationProcessor.declaredPackageIn("// package com.example;"));
        assertEquals("", BuildHintAnnotationProcessor.declaredPackageIn("class MyApp {}"));
    }

    /// Blanking preserves length and line breaks, so an offset into the blanked
    /// text still means the same place in the original.
    @Test
    public void blankingKeepsThePositionsIntact() {
        String src = "a // b\nc /* d */ e\n";
        String blanked = BuildHintAnnotationProcessor.blankNonCode(src);
        assertEquals(src.length(), blanked.length());
        assertEquals(2, blanked.split("\n", -1).length - 1);
    }

    /// The whole nesting path, in order. Checking only the innermost name let an
    /// unrelated Main.B.Wrong vouch for a deleted Main.A.Wrong; checking only the
    /// outer class was the same bug one level out.
    @Test
    public void aNestedPathMustNestTheSameWay() {
        String src = "package com.example;\n"
                + "public class Main {\n"
                + "    static class A { }\n"
                + "    static class B { static class Wrong { } }\n"
                + "}\n";
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                src, new String[] {"Main", "B", "Wrong"}));
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                src, new String[] {"Main", "A", "Wrong"}));
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                src, new String[] {"Main", "Wrong"}));
    }

    /// Braces inside comments and strings must not move the nesting.
    @Test
    public void bracesInCommentsAndStringsDoNotBreakNesting() {
        String src = "class Main {\n"
                + "    // }\n"
                + "    String s = \"}\";\n"
                + "    static class Wrong { }\n"
                + "}\n";
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                src, new String[] {"Main", "Wrong"}));
    }

    /// javac names a NAMED local class Main$1Wrong. No source declares that
    /// spelling, so looking for it finds nothing -- and concluding "orphan" from
    /// that dropped a live annotated class before the placement check could
    /// reject it, letting the build succeed with the hints silently discarded.
    /// Checking for a wholly numeric segment missed $1Wrong exactly.
    @Test
    public void anAnnotationOnANamedLocalClassIsStillReported() throws Exception {
        String source = "package com.example;\n"
                + "import com.codename1.annotations.buildhints.*;\n"
                + "public class MyApp {\n"
                + "    void go() {\n"
                + "        @Ios(teamId = \"ABCDE12345\")\n"
                + "        class Wrong {}\n"
                + "        new Wrong();\n"
                + "    }\n"
                + "}\n";
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(JavaSourceCompiler.singleSource(MAIN, source),
                classes, Arrays.asList(testClassesDir(), coreJar()));
        // The source root has to be real, or the orphan filter is never consulted
        // and this passes whatever it does.
        ProcessorContext ctx = run(classes, settings(), MAIN, false, sourceRootWith(source));
        assertErrorContaining(ctx, "belong on the application's main class");
    }

    /// The rule the case above rests on, stated directly: javac's own segments
    /// are unjudgeable, a developer's are not.
    @Test
    public void javacsOwnNestedSegmentsAreNotLookedFor() {
        assertNull(BuildHintAnnotationProcessor.nestedNameOf("com.example.Main$1Wrong"));
        assertNull(BuildHintAnnotationProcessor.nestedNameOf("com.example.Main$1"));
        assertNull(BuildHintAnnotationProcessor.nestedNameOf("com.example.Main$A$1B"));
        assertArrayEquals(new String[] {"Main", "Wrong"},
                BuildHintAnnotationProcessor.nestedNameOf("com.example.Main$Wrong"));
        assertNull(BuildHintAnnotationProcessor.nestedNameOf("com.example.Main"));
    }

    /// A source root holding MyApp.java with the given text.
    private File sourceRootWith(String source) throws Exception {
        File root = tmp.newFolder();
        File dir = new File(root, "com" + File.separator + "example");
        assertTrue(dir.mkdirs());
        try (java.io.Writer w = new java.io.OutputStreamWriter(
                new FileOutputStream(new File(dir, "MyApp.java")), "UTF-8")) {
            w.write(source);
        }
        return root;
    }

    /// A declaration may put any legal whitespace, or a comment, between the
    /// keyword and the name. Requiring exactly one space read `class\nWrong` as
    /// no declaration at all -- so a live type looked stale to the orphan check,
    /// and the migration goal reported it could not find the main source.
    @Test
    public void anyLegalSeparatorBeforeATypeNameIsAccepted() {
        assertTrue(BuildHintAnnotationProcessor.declaresType("class\nWrong {}", "Wrong"));
        assertTrue(BuildHintAnnotationProcessor.declaresType("class\tWrong {}", "Wrong"));
        assertTrue(BuildHintAnnotationProcessor.declaresType("class /* why */ Wrong {}", "Wrong"));
        assertTrue(BuildHintAnnotationProcessor.declaresType(
                "public\nfinal\nclass\n   Wrong\n{}", "Wrong"));
        assertFalse(BuildHintAnnotationProcessor.declaresType("class Wronger {}", "Wrong"));
    }

    /// A brace inside a char literal is not syntax. Counting it loses the
    /// nesting, so a live nested class reads as an orphan and its misplaced
    /// annotation is skipped instead of reported.
    @Test
    public void aBraceInACharLiteralDoesNotMoveTheNesting() {
        // Deliberately UNBALANCED. My first version of this test had both '{'
        // and '}', which cancel out, so it passed with the char branch removed
        // and proved nothing.
        String src = "class Main {\n"
                + "    char open = '{';\n"
                + "    static class Wrong { }\n"
                + "}\n";
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                src, new String[] {"Main", "Wrong"}));

        // An escaped quote must not end the literal early, or the brace after it
        // is counted again.
        String escaped = "class Main {\n"
                + "    char quote = '\\'';\n"
                + "    char open = '{';\n"
                + "    static class Wrong { }\n"
                + "}\n";
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                escaped, new String[] {"Main", "Wrong"}));
    }

    /// A declaration below any fixed prefix must still be found: a line bound
    /// meant a type after a long header read as absent, so a live class looked
    /// stale and its placement error was never reported.
    @Test
    public void aDeclarationFarDownTheFileIsStillFound() {
        StringBuilder src = new StringBuilder("package com.example;\n");
        for (int i = 0; i < 900; i++) {
            src.append("import java.util.List").append(i).append(";\n");
        }
        src.append("public class MyApp {}\n");
        assertTrue(BuildHintAnnotationProcessor.declaresType(src.toString(), "MyApp"));
        assertEquals("com.example",
                BuildHintAnnotationProcessor.declaredPackageIn(src.toString()));
    }

    /// `package\ncom.example;` is valid Java. A line-oriented parse saw an empty
    /// remainder and reported the default package, so a live class looked like it
    /// belonged elsewhere, read as an orphan, and its misplaced annotation went
    /// unreported.
    @Test
    public void aPackageDeclarationMaySpanLines() {
        assertEquals("com.example",
                BuildHintAnnotationProcessor.declaredPackageIn("package\ncom.example;\n"));
        assertEquals("com.example",
                BuildHintAnnotationProcessor.declaredPackageIn("package   com.example ;\n"));
        assertEquals("com.example",
                BuildHintAnnotationProcessor.declaredPackageIn("package /* x */ com.example\n"));
        assertEquals("", BuildHintAnnotationProcessor.declaredPackageIn("// package com.example;"));
    }

    /// A value is a place a developer writes arbitrary text, so it must not be
    /// able to forge the structure around it. With plain delimiters these two
    /// annotations fingerprinted identically, and a stale manifest was then
    /// accepted for a genuinely different configuration.
    @Test
    public void aValueCannotForgeTheDigestStructure() throws Exception {
        String forged = digestOf(
                "@Ios(bundleVersion = \"1;teamId=java.lang.String:X\")");
        String real = digestOf("@Ios(bundleVersion = \"1\", teamId = \"X\")");
        assertFalse("a value must not be able to imitate another member",
                forged.equals(real));
    }

    /// Neighbouring values must not run together either: {"a","bc"} is not
    /// {"ab","c"}, and a list of one is not the value itself.
    @Test
    public void adjacentValuesDoNotRunTogether() throws Exception {
        assertFalse(digestOf("@Ios(pods = {\"a\", \"bc\"})")
                .equals(digestOf("@Ios(pods = {\"ab\", \"c\"})")));
        assertFalse(digestOf("@Ios(pods = {\"a\"})")
                .equals(digestOf("@Ios(teamId = \"a\")")));
    }

    /// ...while the same annotations still fingerprint the same, or the check
    /// would refuse every build instead of only the wrong ones.
    @Test
    public void theSameAnnotationsFingerprintTheSame() throws Exception {
        assertEquals(digestOf("@Ios(teamId = \"X\", bundleVersion = \"1\")"),
                digestOf("@Ios(bundleVersion = \"1\", teamId = \"X\")"));
    }

    /// The digest of a main class annotated so.
    private String digestOf(String annotations) throws Exception {
        File dir = tmp.newFolder();
        JavaSourceCompiler.compile(JavaSourceCompiler.singleSource(MAIN, source(annotations)),
                dir, Arrays.asList(testClassesDir(), coreJar()));
        Map<String, AnnotatedClass> index = ClassScanner.scan(dir);
        return BuildHintAnnotationProcessor.sourceDigest(index.values().iterator().next());
    }

    /// An element may legally be empty -- a newline-delimited value that starts
    /// with a newline is {"", "..."} -- and joining on "what has been written so
    /// far" skipped the separator after it, silently dropping the leading
    /// newline from the hint the builder receives.
    @Test
    public void anEmptyListElementStillGetsItsSeparator() throws Exception {
        Properties p = hintsOf("@Android(xgradle = {\"\", \"apply plugin: 'x'\"})");
        assertEquals("\napply plugin: 'x'", p.getProperty("codename1.arg.android.xgradle"));
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
        return run(classes, settings, mainClass, expectClean, null);
    }

    /// With `sourceRoot` the orphan filter runs for real. Without it the context
    /// reports no compile source roots, which the filter reads as "not told" and
    /// keeps every class -- so a test that means to exercise the filter has to
    /// supply one, or it passes whatever the filter does.
    private ProcessorContext run(File classes, Properties settings, String mainClass,
                                 boolean expectClean, File sourceRoot) throws Exception {
        Map<String, AnnotatedClass> index = ClassScanner.scan(classes);
        BuildHintAnnotationProcessor proc = new BuildHintAnnotationProcessor();
        ProcessorContext ctx = new ProcessorContext(classes, tmp.newFolder(), index,
                new SystemStreamLog(), tmp.newFolder(), settings, mainClass,
                sourceRoot == null ? null
                        : java.util.Collections.singletonList(sourceRoot.getAbsolutePath()));
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
