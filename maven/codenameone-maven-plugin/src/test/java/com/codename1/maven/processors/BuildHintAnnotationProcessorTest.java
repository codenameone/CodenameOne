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
        Properties p = hintsOf("@Ios(newStorageLocation = Toggle.ON)");
        assertEquals("true", p.getProperty("codename1.arg.ios.newStorageLocation"));

        p = hintsOf("@Ios(newStorageLocation = Toggle.OFF)");
        assertEquals("false", p.getProperty("codename1.arg.ios.newStorageLocation"));
    }

    @Test
    public void anIntAttributeIsStringified() throws Exception {
        Properties p = hintsOf("@DesktopBuild(width = 1280, height = 720)");
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

        p = hintsOf("@DesktopBuild(titleBar = DesktopTitleBar.TOOLBAR)");
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
    /// written, because typing it is a statement of intent.
    @Test
    public void anExplicitlyWrittenValueIsStillEmitted() throws Exception {
        Properties p = hintsOf("@Ios(objC = Toggle.ON)");
        assertEquals("true", p.getProperty("codename1.arg.ios.objC"));
    }

    /// The unset constant is written as if nothing had been written at all.
    ///
    /// An annotation member must name SOME constant as its default, so the one
    /// it names says nothing. Naming it explicitly has to mean the same, or
    /// there would be two ways to say "leave it alone" that behave differently.
    /// Sending the constant's own name would set the hint to a value no builder
    /// recognises and every builder silently ignores.
    @Test
    public void theUnsetConstantSendsNothing() throws Exception {
        Properties p = hintsOf("@Ios(objC = Toggle.DEFAULT, teamId = \"T\")");
        assertEquals("T", p.getProperty("codename1.arg.ios.teamId"));
        assertNull("Toggle.DEFAULT must not be written",
                p.getProperty("codename1.arg.ios.objC"));
    }

    /// ...and so does an enum's own unset constant.
    @Test
    public void anEnumsUnsetConstantSendsNothing() throws Exception {
        Properties p = hintsOf("@Ios(themeMode = IosThemeMode.DEFAULT, teamId = \"T\")");
        assertEquals("T", p.getProperty("codename1.arg.ios.teamId"));
        assertNull(p.getProperty("codename1.arg.ios.themeMode"));
    }

    // ------------------------------------------------------------------
    // determinism and cleanup
    // ------------------------------------------------------------------

    @Test
    public void theEmittedResourceIsByteStableAcrossRuns() throws Exception {
        String src = "@Ios(pods = {\"A\", \"B\"}, teamId = \"T\")\n@DesktopBuild(width = 640)";
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

    /// Kotlin builds a local class's binary name out of the enclosing FUNCTION
    /// names -- Main$start$Wrong -- with nothing marking `start` as synthetic.
    /// Requiring it to be a declared type dropped the live annotated class
    /// silently. Past the outermost type a Kotlin segment is inconclusive.
    @Test
    public void aKotlinLocalClassPathIsInconclusiveNotAnOrphan() {
        String kt = "package com.example\n"
                + "class Main {\n"
                + "    fun start() {\n"
                + "        class Wrong\n"
                + "    }\n"
                + "}\n";
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                kt, new String[] {"Main", "start", "Wrong"}, true));
        // The outermost type is still required: it is what the file declares.
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                kt, new String[] {"Other", "start", "Wrong"}, true));
        // ...and so is the LAST segment, which is the class itself. Leniency
        // there would keep a deleted nested type's orphan and fail every
        // incremental build.
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                kt, new String[] {"Main", "start", "Gone"}, true));
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                "package com.example\nclass Main { }\n", new String[] {"Main", "Wrong"}, true));
        // A function that does not exist is not an excuse either.
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                kt, new String[] {"Main", "start", "Wrong", "Deeper"}, true));
        // Java keeps the strict reading, since javac marks its locals with $1.
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                "class Main { void start() { } }", new String[] {"Main", "start", "Wrong"},
                false));
    }

    /// The simulator has no bytecode reader, so it cannot recompute the source
    /// digest and was left comparing file timestamps -- which a jar records to
    /// two seconds and a reproducible build stamps identically, making the
    /// comparison inert rather than coarse. Hashing the class file needs no
    /// bytecode reader, so the manifest records that instead.
    @Test
    public void theManifestRecordsTheCompiledClassesOwnDigest() throws Exception {
        File classes = compile("@Ios(teamId = \"ABCDE12345\")");
        ProcessorContext ctx = run(classes, settings(), MAIN, true);
        Properties p = new Properties();
        p.load(new ByteArrayInputStream(ctx.getEmittedResources()
                .get(BuildHintAnnotationProcessor.MANIFEST_RESOURCE)));

        String recorded = p.getProperty(BuildHintAnnotationProcessor.CLASS_DIGEST_KEY);
        assertTrue("no class digest was recorded", recorded != null);
        assertEquals(sha256Of(new File(classes, "com/example/MyApp.class")), recorded);
    }

    private static String sha256Of(File f) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            byte[] buf = new byte[8192];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                md.update(buf, 0, n);
            }
        } finally {
            in.close();
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : md.digest()) {
            hex.append(Character.forDigit((b >> 4) & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    /// A processor may REPLACE the main class through `emitClass`, and those are
    /// flushed only after every processor's `finish()` -- so a manifest written
    /// during ours records the class as the compiler left it, not as the build
    /// ships it. The simulator would then read a freshly generated manifest as
    /// stale and drop every annotated hint under `cn1:run`.
    @Test
    public void theStampIsCorrectedOnceTheClassesAreFlushed() throws Exception {
        File classes = compile("@Ios(teamId = \"ABCDE12345\")");
        ProcessorContext ctx = run(classes, settings(), MAIN, true);
        File manifest = new File(classes, BuildHintAnnotationProcessor.MANIFEST_RESOURCE);
        manifest.getParentFile().mkdirs();
        java.nio.file.Files.write(manifest.toPath(),
                ctx.getEmittedResources()
                        .get(BuildHintAnnotationProcessor.MANIFEST_RESOURCE));

        // Stand in for the instrumented replacement a later processor writes.
        File classFile = new File(classes, "com/example/MyApp.class");
        byte[] original = java.nio.file.Files.readAllBytes(classFile.toPath());
        byte[] replaced = new byte[original.length + 1];
        System.arraycopy(original, 0, replaced, 0, original.length);
        java.nio.file.Files.write(classFile.toPath(), replaced);

        BuildHintAnnotationProcessor.restampClassDigest(classes);

        Properties after = new Properties();
        java.io.InputStream in = new java.io.FileInputStream(manifest);
        try {
            after.load(in);
        } finally {
            in.close();
        }
        assertEquals(sha256Of(classFile),
                after.getProperty(BuildHintAnnotationProcessor.CLASS_DIGEST_KEY));
        // Everything else is left exactly as it was.
        assertEquals("ABCDE12345", after.getProperty("codename1.arg.ios.teamId"));
        assertEquals(MAIN, after.getProperty("cn1.buildHints.mainClass"));
    }

    /// Nothing to correct is not an error: a project with no build hint
    /// annotations emits no manifest at all.
    @Test
    public void theStampStepIsSilentWithoutAManifest() throws Exception {
        BuildHintAnnotationProcessor.restampClassDigest(tmp.newFolder());
    }

    /// The scan for the package keyword steps over an escaped identifier too.
    /// `fun `package helper`() {}` in a default-package file reported `helper`
    /// as the declared package, so a live annotated class in it looked like it
    /// belonged elsewhere and was dropped as an orphan.
    @Test
    public void anEscapedIdentifierIsNotAPackageDeclaration() {
        assertEquals("", BuildHintAnnotationProcessor.declaredPackageIn(
                "fun `package helper`() {}\n\nclass MyApp\n", true));
        // The real one is still found when there is one.
        assertEquals("com.example", BuildHintAnnotationProcessor.declaredPackageIn(
                "package com.example\n\nfun `package helper`() {}\n", true));
    }

    /// Inside a Kotlin template expression the first quote starts a NEW literal
    /// rather than closing the outer one, so a `class` written inside one was
    /// exposed as live code and read as a declaration nobody wrote.
    @Test
    public void aStringInsideAKotlinTemplateIsStillAString() {
        String kt = "package com.example\n"
                + "class Real {\n"
                + "    val note = \"${\"class Fake\"}\"\n"
                + "}\n";
        assertFalse(BuildHintAnnotationProcessor.declaresType(kt, "Fake", true));
        assertTrue(BuildHintAnnotationProcessor.declaresType(kt, "Real", true));

        // A brace inside the nested literal must not close the expression early,
        // or the nesting scan loses its place from there on.
        String braced = "package com.example\n"
                + "class Real {\n"
                + "    val note = \"${\"} class Fake\"}\"\n"
                + "}\n";
        assertFalse(BuildHintAnnotationProcessor.declaresType(braced, "Fake", true));

        // The expression is ordinary code, so it holds ordinary comments and
        // char literals, and a quote inside one of those is not a nested string.
        String commented = "package com.example\n"
                + "class Real {\n"
                + "    val note = \"${ /* \\\" */ 1 }\"\n"
                + "}\n"
                + "class After\n";
        assertTrue(BuildHintAnnotationProcessor.declaresType(commented, "After", true));

        String charLiteral = "package com.example\n"
                + "class Real {\n"
                + "    val note = \"${ if (c == '\\\"') 1 else 2 }\"\n"
                + "}\n"
                + "class After\n";
        assertTrue(BuildHintAnnotationProcessor.declaresType(charLiteral, "After", true));

        // A brace inside a comment there must not close the expression either.
        String bracedComment = "package com.example\n"
                + "class Real {\n"
                + "    val note = \"${ /* } */ 1 }\"\n"
                + "}\n"
                + "class After\n";
        assertTrue(BuildHintAnnotationProcessor.declaresType(bracedComment, "After", true));

        // An escaped identifier inside the expression is a NAME: a quote in it
        // does not open a string and a brace does not close the expression.
        String escapedName = "package com.example\n"
                + "class Real {\n"
                + "    val note = \"${ `\\\"` }\"\n"
                + "}\n"
                + "class After\n";
        assertTrue(BuildHintAnnotationProcessor.declaresType(escapedName, "After", true));

        String bracedName = "package com.example\n"
                + "class Real {\n"
                + "    val note = \"${ `}` }\"\n"
                + "}\n"
                + "class After\n";
        assertTrue(BuildHintAnnotationProcessor.declaresType(bracedName, "After", true));

        // A raw string carries templates too.
        String raw = "package com.example\n"
                + "class Real {\n"
                + "    val note = \"\"\"${\"class Fake\"}\"\"\"\n"
                + "}\n";
        assertFalse(BuildHintAnnotationProcessor.declaresType(raw, "Fake", true));
    }

    /// The compiler's source encoding is a project setting this scan cannot
    /// see, and decoding an ISO-8859-1 source as UTF-8 produced replacement
    /// characters -- so a name with a non-ASCII character never matched, the
    /// class read as an orphan, and its misplaced annotation went unreported.
    @Test
    public void aSourceEncodingThatIsNotUtf8StillMatches() throws Exception {
        String pkg = "com.caf\u00e9";
        for (String charset : new String[] {"UTF-8", "ISO-8859-1"}) {
            File src = tmp.newFolder();
            File dir = new File(src, "com/cafe");
            dir.mkdirs();
            java.io.OutputStream os =
                    new java.io.FileOutputStream(new File(dir, "Accented.java"));
            try {
                os.write(("package " + pkg + ";\npublic class Accented {\n}\n")
                        .getBytes(charset));
            } finally {
                os.close();
            }

            File classes = tmp.newFolder();
            JavaSourceCompiler.compile(
                    JavaSourceCompiler.singleSource(pkg + ".Accented",
                            "package " + pkg + ";\npublic class Accented {\n}\n"),
                    classes, Arrays.asList(testClassesDir(), coreJar()));
            AnnotatedClass cls = ClassScanner.scan(classes)
                    .get(pkg.replace('.', '/') + "/Accented");
            assertTrue("the class under test must have been compiled", cls != null);

            assertTrue("not matched when the source is " + charset,
                    BuildHintAnnotationProcessor.hasBackingSource(cls,
                            java.util.Collections.singletonList(src.getAbsolutePath())));

            // Read correctly, not merely judged unreadable: the name comes back
            // as it was written, whichever of the two encodings the file is in.
            assertTrue("misread when the source is " + charset,
                    BuildHintAnnotationProcessor.readHead(new File(dir, "Accented.java"))
                            .contains(pkg));
        }
    }

    /// A non-ASCII NESTED name is not judged without the compiler's encoding.
    ///
    /// The package and the simple name both treated a name outside ASCII as
    /// unjudgeable; the nesting path did not. A Windows-1251 source decodes
    /// through ISO-8859-1 without error and into DIFFERENT -- equally valid --
    /// identifier characters, so the nested name was not found, the file was
    /// read as declaring some other type, and the live class was dropped as an
    /// orphan. `processClass` then skipped the misplaced-annotation error it
    /// exists to raise: a green build with the hints quietly ignored.
    @Test
    public void aNonAsciiNestedNameIsNotJudgedWithoutTheEncoding() throws Exception {
        String cyr = "\u041a\u043b\u044e\u0447";
        File src = tmp.newFolder();
        File dir = new File(src, "com/ex");
        assertTrue(dir.mkdirs());
        String text = "package com.ex;\npublic class Main {\n  static class " + cyr
                + " {}\n}\n";
        writeAs(new File(dir, "Main.java"), text, "windows-1251");

        AnnotatedClass cls = compiledClass("com.ex.Main", text, "com/ex/Main$" + cyr);
        assertTrue("kept when the encoding is unknown",
                BuildHintAnnotationProcessor.hasBackingSource(cls,
                        java.util.Collections.singletonList(src.getAbsolutePath())));
    }

    /// With the module's encoding, the same name is judged EXACTLY.
    ///
    /// "Cannot tell" is the right answer only while the encoding is unknown. Left
    /// there it is its own bug in the other direction: a genuinely deleted class
    /// with a non-ASCII name would be kept on every incremental build and fail
    /// its placement check forever. So a module that declares its encoding gets
    /// a real answer -- the live nested name matches, and a stale one does not.
    @Test
    public void theModulesEncodingJudgesANonAsciiNameExactly() throws Exception {
        String cyr = "\u041a\u043b\u044e\u0447";
        String other = "\u0414\u0440\u0443\u0433\u043e\u0439";
        File src = tmp.newFolder();
        File dir = new File(src, "com/ex");
        assertTrue(dir.mkdirs());
        String text = "package com.ex;\npublic class Main {\n  static class " + cyr
                + " {}\n}\n";
        writeAs(new File(dir, "Main.java"), text, "windows-1251");
        java.util.List<String> roots = java.util.Collections.singletonList(src.getAbsolutePath());

        assertTrue("the live nested class is matched",
                BuildHintAnnotationProcessor.hasBackingSource(
                        compiledClass("com.ex.Main", text, "com/ex/Main$" + cyr),
                        roots, "windows-1251"));

        // The same source, asked about a nested name it does not declare.
        String stale = "package com.ex;\npublic class Main {\n  static class " + other
                + " {}\n}\n";
        assertFalse("a stale nested class is not vouched for",
                BuildHintAnnotationProcessor.hasBackingSource(
                        compiledClass("com.ex.Main", stale, "com/ex/Main$" + other),
                        roots, "windows-1251"));
    }

    /// A declared encoding the file does not actually decode through is ignored.
    ///
    /// A module that says UTF-8 and has one source that is not gains nothing from
    /// being read as UTF-8: the bytes are malformed, they come back as
    /// replacement characters, and a name spelled with one never matches. The
    /// byte sniffing reads that file correctly, so it stays the better answer --
    /// and the declaration is not taken on faith.
    ///
    /// The non-ASCII name is a NESTED one, so every path this test touches stays
    /// ASCII. Putting it in the file name instead passed here and failed on CI:
    /// a runner whose `sun.jnu.encoding` is ASCII cannot represent the name, so
    /// the file was written under a mangled one and the scan never found it --
    /// the test measuring the runner's locale rather than the decoder.
    @Test
    public void aDeclaredEncodingTheFileDoesNotDecodeThroughIsIgnored() throws Exception {
        String cafe = "Caf\u00e9";
        File src = tmp.newFolder();
        File dir = new File(src, "com/ex");
        assertTrue(dir.mkdirs());
        String text = "package com.ex;\npublic class Main {\n  static class " + cafe
                + " {}\n}\n";
        writeAs(new File(dir, "Main.java"), text, "ISO-8859-1");

        assertTrue(BuildHintAnnotationProcessor.hasBackingSource(
                compiledClass("com.ex.Main", text, "com/ex/Main$" + cafe),
                java.util.Collections.singletonList(src.getAbsolutePath()), "UTF-8"));
    }

    /// Writes `text` to `f` in `charset`.
    private static void writeAs(File f, String text, String charset) throws Exception {
        java.io.OutputStream os = new java.io.FileOutputStream(f);
        try {
            os.write(text.getBytes(charset));
        } finally {
            os.close();
        }
    }

    /// Compiles `text` and returns the scanned class at `internalName`.
    private AnnotatedClass compiledClass(String binaryName, String text, String internalName)
            throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(JavaSourceCompiler.singleSource(binaryName, text),
                classes, Arrays.asList(testClassesDir(), coreJar()));
        AnnotatedClass cls = ClassScanner.scan(classes).get(internalName);
        assertTrue("the class under test must have been compiled: " + internalName, cls != null);
        return cls;
    }

    /// A UTF-16 source is read as UTF-16, even though it decodes as UTF-8.
    ///
    /// An ASCII-named class compiled as UTF-16LE is `p\0a\0c\0k\0...` on
    /// disk. NUL is a perfectly good UTF-8 character, so the UTF-8 test the
    /// charset guess relied on SUCCEEDED and left a NUL between every character.
    /// Neither the package nor the type declaration was then found, so a live
    /// annotated class read as an orphan and `processClass` skipped the
    /// misplaced-annotation error it exists to raise -- a green build with the
    /// hint quietly ignored, which is the failure this whole feature removes.
    @Test
    public void aUtf16SourceIsNotMistakenForAnOrphan() throws Exception {
        for (String charset : new String[]{"UTF-16LE", "UTF-16BE", "UTF-16"}) {
            String source = "package com.example;\npublic class Wide {\n}\n";
            File src = tmp.newFolder();
            File dir = new File(src, "com/example");
            assertTrue(dir.mkdirs());
            java.io.OutputStream os =
                    new java.io.FileOutputStream(new File(dir, "Wide.java"));
            try {
                // "UTF-16" writes a byte order mark; the LE/BE spellings do not,
                // so this covers both the marked and the unmarked shape.
                os.write(source.getBytes(charset));
            } finally {
                os.close();
            }

            File classes = tmp.newFolder();
            JavaSourceCompiler.compile(
                    JavaSourceCompiler.singleSource("com.example.Wide", source),
                    classes, Arrays.asList(testClassesDir(), coreJar()));
            AnnotatedClass cls = ClassScanner.scan(classes).get("com/example/Wide");
            assertTrue("the class under test must have been compiled", cls != null);

            assertTrue("a live class read as an orphan when the source is " + charset,
                    BuildHintAnnotationProcessor.hasBackingSource(cls,
                            java.util.Collections.singletonList(src.getAbsolutePath())));

            // Read, not merely judged unreadable: no NUL survives the decode.
            String head = BuildHintAnnotationProcessor.readHead(new File(dir, "Wide.java"));
            assertTrue("misread when the source is " + charset,
                    head.contains("package com.example;"));
            assertTrue("NUL left in the text when the source is " + charset,
                    head.indexOf(0) < 0);
        }
    }

    /// ...and neither is a UTF-8 source that starts with a byte order mark,
    /// which is what an editor on Windows writes by default.
    ///
    /// The mark decodes to U+FEFF and stays in the text. It is not whitespace,
    /// so the text does not begin with the package declaration and the class
    /// read as an orphan -- the same silent skip as the UTF-16 case, on a file
    /// far more likely to exist.
    @Test
    public void aByteOrderMarkIsNotProofOfAnOrphan() throws Exception {
        String source = "package com.example;\npublic class Marked {\n}\n";
        File src = tmp.newFolder();
        File dir = new File(src, "com/example");
        assertTrue(dir.mkdirs());
        java.io.OutputStream os = new java.io.FileOutputStream(new File(dir, "Marked.java"));
        try {
            os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            os.write(source.getBytes("UTF-8"));
        } finally {
            os.close();
        }

        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Marked", source),
                classes, Arrays.asList(testClassesDir(), coreJar()));
        AnnotatedClass cls = ClassScanner.scan(classes).get("com/example/Marked");
        assertTrue("the class under test must have been compiled", cls != null);

        assertTrue("a live class read as an orphan because of its byte order mark",
                BuildHintAnnotationProcessor.hasBackingSource(cls,
                        java.util.Collections.singletonList(src.getAbsolutePath())));
        assertTrue(BuildHintAnnotationProcessor.readHead(new File(dir, "Marked.java"))
                .startsWith("package com.example;"));
    }

    /// Running out of search budget is "cannot tell", not "no such source".
    /// Answering no dropped a live annotated class silently, with its placement
    /// error lost, for the sake of a bound -- which is the wrong way round:
    /// everywhere else in this walk an unanswerable question keeps the class.
    @Test
    public void aDeepSourceTreeIsNotProofOfAnOrphan() throws Exception {
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Deep",
                        "package com.example;\npublic class Deep {\n}\n"),
                classes, Arrays.asList(testClassesDir(), coreJar()));
        AnnotatedClass cls = ClassScanner.scan(classes).get("com/example/Deep");
        assertTrue("the class under test must have been compiled", cls != null);

        // Deeper than the old cutoff, and the file is genuinely there.
        assertTrue(BuildHintAnnotationProcessor.hasBackingSource(cls,
                java.util.Collections.singletonList(nest(30).getAbsolutePath())));

        // Deeper than the budget: unanswerable, so the class is kept.
        assertTrue(BuildHintAnnotationProcessor.hasBackingSource(cls,
                java.util.Collections.singletonList(nest(70).getAbsolutePath())));
    }

    /// A source root with Deep.java `levels` directories down.
    private File nest(int levels) throws Exception {
        File root = tmp.newFolder();
        File at = root;
        for (int i = 0; i < levels; i++) {
            at = new File(at, "d" + i);
        }
        at.mkdirs();
        java.io.Writer w = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(new File(at, "Deep.java")), "UTF-8");
        try {
            w.write("package com.example;\npublic class Deep {\n}\n");
        } finally {
            w.close();
        }
        return root;
    }

    /// javac processes `\\uXXXX` before it tokenizes anything, so
    /// `package com.ex\\u0061mple;` really declares com.example -- and it
    /// rejects an ill-formed one in a comment too, which is why the sequences
    /// here are written with two backslashes. Reading the
    /// text literally stopped the component at the backslash and recorded
    /// `com.ex`, so a live annotated class looked like it belonged elsewhere and
    /// was dropped as an orphan with its placement error unreported.
    @Test
    public void javaUnicodeEscapesAreTranslatedBeforeTheSourceIsRead() throws Exception {
        // Through the orphan filter, which is where it decides anything: the
        // source spells its package with an escape, the compiled class does not.
        File src = tmp.newFolder();
        File pkgDir = new File(src, "com/example");
        pkgDir.mkdirs();
        java.io.Writer w = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(new File(pkgDir, "Escaped.java")), "UTF-8");
        try {
            w.write("package com.ex" + "\\u0061" + "mple;\npublic class Escaped {\n}\n");
        } finally {
            w.close();
        }
        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Escaped",
                        "package com.example;\npublic class Escaped {\n}\n"),
                classes, Arrays.asList(testClassesDir(), coreJar()));
        AnnotatedClass cls = ClassScanner.scan(classes).get("com/example/Escaped");
        assertTrue("the class under test must have been compiled", cls != null);
        assertTrue(BuildHintAnnotationProcessor.hasBackingSource(cls,
                java.util.Collections.singletonList(src.getAbsolutePath())));

        // A doubled backslash is not an escape, which is what keeps a string
        // literal spelling one.
        assertEquals("String s = \"\\\\u0041\";",
                BuildHintAnnotationProcessor.decodeUnicodeEscapes(
                        "String s = \"\\\\u0041\";"));

        // Any number of u's is one escape, and a malformed one is left alone.
        assertEquals("A", BuildHintAnnotationProcessor.decodeUnicodeEscapes("\\uuu0041"));
        assertEquals("\\uZZZZ", BuildHintAnnotationProcessor.decodeUnicodeEscapes("\\uZZZZ"));
        assertEquals("\\n", BuildHintAnnotationProcessor.decodeUnicodeEscapes("\\n"));
    }

    /// `$` is a legal character in a Java type name, so a top-level
    /// `class Wrong$Type` has binary name Wrong$Type and is not nested at all.
    /// Reading every `$` as nesting looked for a `Wrong` that does not exist,
    /// dropped the live class as an orphan, and lost the placement error it
    /// should have raised.
    @Test
    public void aDollarInATopLevelJavaNameIsNotNesting() throws Exception {
        File src = tmp.newFolder();
        File pkgDir = new File(src, "com/example");
        pkgDir.mkdirs();
        java.io.Writer w = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(new File(pkgDir, "Wrong$Type.java")), "UTF-8");
        try {
            w.write("package com.example;\npublic class Wrong$Type {\n}\n");
        } finally {
            w.close();
        }

        File classes = tmp.newFolder();
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Wrong$Type",
                        "package com.example;\npublic class Wrong$Type {\n}\n"),
                classes, Arrays.asList(testClassesDir(), coreJar()));
        AnnotatedClass cls = ClassScanner.scan(classes).get("com/example/Wrong$Type");
        assertTrue("the class under test must have been compiled", cls != null);

        assertTrue(BuildHintAnnotationProcessor.hasBackingSource(cls,
                java.util.Collections.singletonList(src.getAbsolutePath())));
    }

    /// An escaped identifier may contain anything, spaces and keywords
    /// included, and it is left as the code it is -- so a declaration scanner
    /// has to step over it rather than read what is inside. `val `class Main``
    /// declares a property, and reading it as a declaration of Main made a class
    /// that belongs elsewhere look like it belonged here.
    @Test
    public void anEscapedIdentifierIsNotADeclaration() {
        String kt = "package com.example\nval `class Main` = 1\nclass Other\n";
        assertFalse(BuildHintAnnotationProcessor.declaresType(kt, "Main", true));
        assertTrue(BuildHintAnnotationProcessor.declaresType(kt, "Other", true));
    }

    /// A qualified name may escape a COMPONENT: `package com.`when`` is legal
    /// Kotlin and the compiled class belongs to `com.when`. Stopping at the
    /// backtick recorded `com.`, so a live annotated class looked like it
    /// belonged to another package, was dropped as an orphan, and its misplaced
    /// hints went unreported on a green build.
    @Test
    public void aQualifiedNameMayEscapeAComponent() {
        assertEquals("com.when", BuildHintAnnotationProcessor.declaredPackageIn(
                "package com.`when`\nclass Foo\n", true));
        assertEquals("com.when.x", BuildHintAnnotationProcessor.declaredPackageIn(
                "package com.`when`.x\nclass Foo\n", true));
        // The first component too, and an ordinary name is unchanged.
        assertEquals("in.example", BuildHintAnnotationProcessor.declaredPackageIn(
                "package `in`.example\nclass Foo\n", true));
        assertEquals("com.example", BuildHintAnnotationProcessor.declaredPackageIn(
                "package com.example\nclass Foo\n", true));
    }

    /// Kotlin lets a declaration escape its name in backticks, and the binary
    /// name is plainly the text between them. Reading it with the identifier
    /// rule recorded an empty name, so a LIVE annotated type looked undeclared,
    /// was dropped as an orphan before placement validation, and its misplaced
    /// hints went unreported on a green build.
    @Test
    public void aKotlinEscapedNameIsTheTextBetweenTheBackticks() {
        assertTrue(BuildHintAnnotationProcessor.declaresType(
                "package com.example\nclass `when` {\n}\n", "when", true));

        // A quote is a legal character in an escaped name, and the name is not a
        // literal -- treating it as one blanked the rest of the file.
        String quoted = "package com.example\nclass `say\"hi` { }\nclass Real { }\n";
        assertTrue(BuildHintAnnotationProcessor.declaresType(quoted, "Real", true));
        assertTrue(BuildHintAnnotationProcessor.declaresType(quoted, "say\"hi", true));

        // Functions are escaped at least as often, and a local class takes the
        // enclosing function's name as a segment of its own binary name.
        String fn = "package com.example\n"
                + "class Main {\n"
                + "    fun `does the thing`() {\n"
                + "        class Wrong\n"
                + "    }\n"
                + "}\n";
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                fn, new String[] {"Main", "does the thing", "Wrong"}, true));
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                fn, new String[] {"Main", "does the thing", "Gone"}, true));
    }

    /// Kotlin's UNNAMED companion object is `Companion` in the binary name and
    /// is spelled `companion object` in the source, so nothing there is called
    /// Companion. Treating that as inconclusive accepted the whole path without
    /// ever checking the class at the end of it, so a deleted
    /// `Main$Companion$Wrong` kept its orphan and failed every incremental
    /// build until the output directory was cleaned.
    @Test
    public void anUnnamedCompanionObjectIsAScopeNotAWildcard() {
        String kt = "package com.example\n"
                + "class Main {\n"
                + "    companion object {\n"
                + "        class Wrong\n"
                + "    }\n"
                + "}\n";
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                kt, new String[] {"Main", "Companion", "Wrong"}, true));
        // The class at the end of the path is still checked.
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                kt, new String[] {"Main", "Companion", "Gone"}, true));
        // No companion at all keeps the surrounding leniency: an intermediate
        // segment nothing accounts for is inconclusive, because concluding
        // orphan there drops a live annotated class silently while keeping a
        // stale one only costs a visible placement error.
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                "package com.example\nclass Main {\n    class Wrong\n}\n",
                new String[] {"Main", "Companion", "Wrong"}, true));

        // A NAMED companion carries its own name into the binary name, so the
        // ordinary declaration lookup is what applies to it.
        String named = "package com.example\n"
                + "class Main {\n"
                + "    companion object Named {\n"
                + "        class Wrong\n"
                + "    }\n"
                + "}\n";
        assertTrue(BuildHintAnnotationProcessor.declaresNestedPath(
                named, new String[] {"Main", "Named", "Wrong"}, true));
        assertFalse(BuildHintAnnotationProcessor.declaresNestedPath(
                named, new String[] {"Main", "Named", "Gone"}, true));
    }

    /// A Kotlin file annotation holding a raw string that ends in a quote closes
    /// on a run of four. Reading it by Java's rules blanked the package
    /// declaration that followed.
    @Test
    public void aKotlinRawStringBeforeThePackageDoesNotEatIt() {
        String kt = "@file:Suppress(\"\"\"a\"\"\"\")\npackage com.example\nclass MyApp\n";
        assertEquals("com.example", BuildHintAnnotationProcessor.declaredPackageIn(kt, true));
    }

    /// `package com /* generated */ . example;` is legal. Reading the name as one
    /// contiguous run recorded `com`, so a live class looked like it belonged
    /// elsewhere, read as an orphan, and its misplaced annotation went
    /// unreported.
    @Test
    public void aPackageNameMaySpanSeparators() {
        assertEquals("com.example", BuildHintAnnotationProcessor.declaredPackageIn(
                "package com /* generated */ . example;\n", false));
        assertEquals("com.example", BuildHintAnnotationProcessor.declaredPackageIn(
                "package com\n   . example\n", true));
        assertEquals("com.example.deep", BuildHintAnnotationProcessor.declaredPackageIn(
                "package com . example . deep ;\n", false));
    }

    /// Kotlin block comments NEST; Java's do not. Stopping at the first `*/` in
    /// Kotlin ended the comment early, so a commented-out package declaration was
    /// read as live code and a class looked like it belonged elsewhere.
    @Test
    public void aNestedKotlinBlockCommentStaysClosed() {
        String kt = "/* docs /* sample */ package old.name */\n"
                + "package com.example\nclass MyApp\n";
        assertEquals("com.example", BuildHintAnnotationProcessor.declaredPackageIn(kt, true));
        // Java does not nest, so there the inner `*/` really does close it and
        // `package old.name` is what follows.
        assertEquals("old.name", BuildHintAnnotationProcessor.declaredPackageIn(kt, false));
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
