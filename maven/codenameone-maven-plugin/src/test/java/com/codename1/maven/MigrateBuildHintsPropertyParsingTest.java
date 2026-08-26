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

import org.junit.Test;

import java.io.File;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/// Covers the properties parsing in `MigrateBuildHintsMojo`.
///
/// The migration deletes a hint's declaration and replaces it with an
/// annotation. A declaration the deletion pass fails to recognise is left
/// behind while the annotation is added, and the very next build fails with the
/// duplicate-hint error the goal exists to prevent -- so the parser has to
/// accept every form `java.util.Properties` does, not just `key=value`.
public class MigrateBuildHintsPropertyParsingTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void equalsSeparatorIsRecognized() {
        assertEquals("codename1.arg.ios.teamId",
                MigrateBuildHintsMojo.propertyKeyOf("codename1.arg.ios.teamId=ABCDE"));
    }

    @Test
    public void colonSeparatorIsRecognized() {
        assertEquals("codename1.arg.ios.teamId",
                MigrateBuildHintsMojo.propertyKeyOf("codename1.arg.ios.teamId:ABCDE"));
    }

    /// The form that used to survive the deletion pass and break the next build.
    @Test
    public void whitespaceSeparatorIsRecognized() {
        assertEquals("codename1.arg.ios.teamId",
                MigrateBuildHintsMojo.propertyKeyOf("codename1.arg.ios.teamId ABCDE"));
        assertEquals("codename1.arg.ios.teamId",
                MigrateBuildHintsMojo.propertyKeyOf("codename1.arg.ios.teamId\tABCDE"));
    }

    @Test
    public void leadingWhitespaceIsIgnored() {
        assertEquals("codename1.arg.ios.teamId",
                MigrateBuildHintsMojo.propertyKeyOf("    codename1.arg.ios.teamId = ABCDE"));
    }

    @Test
    public void spacingAroundTheSeparatorIsIgnored() {
        assertEquals("codename1.arg.ios.teamId",
                MigrateBuildHintsMojo.propertyKeyOf("codename1.arg.ios.teamId   =   ABCDE"));
    }

    /// A key may escape the characters that would otherwise end it.
    @Test
    public void escapedSeparatorsStayPartOfTheKey() {
        assertEquals("a=b", MigrateBuildHintsMojo.propertyKeyOf("a\\=b=value"));
        assertEquals("a b", MigrateBuildHintsMojo.propertyKeyOf("a\\ b=value"));
        assertEquals("a:b", MigrateBuildHintsMojo.propertyKeyOf("a\\:b=value"));
    }

    @Test
    public void commentsAndBlanksDeclareNothing() {
        assertNull(MigrateBuildHintsMojo.propertyKeyOf("# codename1.arg.ios.teamId=ABCDE"));
        assertNull(MigrateBuildHintsMojo.propertyKeyOf("! codename1.arg.ios.teamId=ABCDE"));
        assertNull(MigrateBuildHintsMojo.propertyKeyOf(""));
        assertNull(MigrateBuildHintsMojo.propertyKeyOf("    "));
    }

    /// Kotlin interpolates `$` inside a string; Java does not. A hint value
    /// carrying one -- a Gradle snippet such as `${'$'}{version}` -- would either
    /// fail to compile as an unresolved reference or resolve to something else.
    @Test
    public void dollarSignsAreEscapedOnlyForKotlin() {
        assertEquals("\"implementation 'x:y:\\$version'\"",
                MigrateBuildHintsMojo.quoteFor("implementation 'x:y:$version'", true));
        assertEquals("\"implementation 'x:y:$version'\"",
                MigrateBuildHintsMojo.quoteFor("implementation 'x:y:$version'", false));
    }

    /// The class declaration is the only safe anchor in a default-package source:
    /// there is no `package` line, and the old arithmetic put the import at the
    /// first newline in the file, which is inside the copyright comment.
    @Test
    public void aDefaultPackageSourceStillGetsAUsableAnchor() {
        String src = "/*\n * Copyright\n */\npublic class MyApp {\n}\n";
        assertEquals(src.indexOf("public class MyApp"),
                MigrateBuildHintsMojo.classDeclarationIndex(src, false, "MyApp"));
    }

    @Test
    public void aValueOnlyLineHasNoSeparator() {
        assertEquals("bare", MigrateBuildHintsMojo.propertyKeyOf("bare"));
    }

    /// `Properties.load` turns a `\\u20ac` in the settings file into a real euro
    /// sign, and the migrated source is written back through ISO-8859-1 to keep
    /// the untouched part of the file byte-identical. Emitting the character raw
    /// would write `?` for anything ISO-8859-1 cannot map, and a high byte for
    /// anything it can -- which corrupts a UTF-8 source. The verification build
    /// would not notice: it checks that the hint came back, not its value.
    @Test
    public void aNonAsciiCharacterIsWrittenAsAnAsciiEscape() {
        assertEquals("\"a\\u20acb\"", MigrateBuildHintsMojo.quoteFor("a\u20acb", false));
        assertEquals("\"a\\u20acb\"", MigrateBuildHintsMojo.quoteFor("a\u20acb", true));
        // Latin-1 is mappable and still escaped: the source may well be UTF-8.
        assertEquals("\"caf\\u00e9\"", MigrateBuildHintsMojo.quoteFor("caf\u00e9", false));
    }

    /// A backslash before an escaped character has to stay a backslash. In Java a
    /// unicode escape is recognised before parsing and only after an even number
    /// of backslashes, so the doubled pair plus our own opener is what makes this
    /// come out right rather than a coincidence.
    @Test
    public void aBackslashBeforeAnEscapedCharacterSurvives() {
        assertEquals("\"\\\\\\u00e9\"", MigrateBuildHintsMojo.quoteFor("\\\u00e9", false));
    }

    /// Control characters without a short escape would otherwise go in raw.
    @Test
    public void aControlCharacterIsEscaped() {
        assertEquals("\"\\u0001\"", MigrateBuildHintsMojo.quoteFor("\u0001", false));
    }

    /// `Properties.load` decodes a Unicode escape in a KEY too, so the key this parser
    /// returns has to be the decoded one. Reading the escape literally left the
    /// original line in place, and the migration then rolled back over a
    /// duplicate declaration it had created itself.
    @Test
    public void aUnicodeEscapeInAKeyIsDecoded() {
        assertEquals("codename1.arg.ios.teamId",
                MigrateBuildHintsMojo.propertyKeyOf("codename1.arg.\\u0069os.teamId=ABCDE"));
    }

    /// Not every backslash-u is an escape. Four hex digits or it is a literal u.
    @Test
    public void aMalformedUnicodeEscapeIsNotDecoded() {
        assertEquals("a.uZZZZb", MigrateBuildHintsMojo.propertyKeyOf("a.\\uZZZZb=1"));
    }

    /// A documented spelling that is not its own constant migrates to the one it
    /// means, rather than being refused as outside the domain -- which is what an
    /// existing project setting a legacy spelling would have hit.
    @Test
    public void anAcceptedSpellingMigratesToTheConstantItMeans() {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        com.codename1.build.shared.BuildHints.Hint ios =
                com.codename1.build.shared.BuildHints.byName("ios.themeMode");
        assertEquals("ThemeMode.IOS7", mojo.toSourceLiteral(ios, "flat", false));
        assertEquals("ThemeMode.MODERN", mojo.toSourceLiteral(ios, "liquid", false));
        assertEquals("ThemeMode.MODERN", mojo.toSourceLiteral(ios, "modern", false));
        assertNull(mojo.toSourceLiteral(ios, "nonsense", false));
    }

    /// An enum constant comes from the catalog, never from the wire value.
    ///
    /// Upper-casing the value works only while the constant happens to be its
    /// spelling. AndroidMinSdk.API_23 sends "23" and Toggle.ON sends "true", and
    /// the derivation produced V23 and TRUE -- constants that do not exist, in
    /// source written by the goal whose job is to migrate a project without
    /// breaking it.
    @Test
    public void anEnumConstantComesFromTheCatalog() {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        com.codename1.build.shared.BuildHints.Hint min =
                com.codename1.build.shared.BuildHints.byName("android.min_sdk_version");
        assertEquals("AndroidMinSdk.API_23", mojo.toSourceLiteral(min, "23", false));
        // A level the enum does not carry is refused rather than invented.
        assertNull(mojo.toSourceLiteral(min, "7", false));
    }

    /// A value its own pattern rejects is refused, not migrated.
    ///
    /// Rendering it produces an annotation the processor then refuses, and this
    /// goal reacts to that by rolling the WHOLE migration back -- so one
    /// misspelled orientation would cost a project every other hint's migration
    /// instead of staying in the properties file where it already is.
    @Test
    public void aValueItsPatternRejectsIsNotMigrated() {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        com.codename1.build.shared.BuildHints.Hint orientation =
                com.codename1.build.shared.BuildHints.byName("ios.interface_orientation");
        assertNotNull(orientation.valuePattern());
        assertEquals("\"UIInterfaceOrientationPortrait\"",
                mojo.toSourceLiteral(orientation, "UIInterfaceOrientationPortrait", false));
        assertNull(mojo.toSourceLiteral(orientation, "UIInterfaceOrientationPortraitt", false));
        assertNull(mojo.toSourceLiteral(orientation, "Portrait", false));
    }

    /// A value that is not already canonical is refused, not normalised.
    /// AndroidGradleBuilder compares android.hideStatusBar with .equals("true"),
    /// so `TRUE` is false today and migrating it to `true` would flip the app's
    /// behaviour while reporting a successful migration.
    @Test
    public void aNonCanonicalScalarIsRefused() {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        com.codename1.build.shared.BuildHints.Hint bool =
                com.codename1.build.shared.BuildHints.byName("android.hideStatusBar");
        // Toggle.ON, not `true`: the attribute has three states and a boolean
        // literal cannot say which. A hint written in the properties file was
        // deliberately set, so it migrates to ON -- never to DEFAULT, which would
        // drop the setting and hand the decision back to the build server.
        assertEquals("Toggle.ON", mojo.toSourceLiteral(bool, "true", false));
        assertEquals("Toggle.OFF", mojo.toSourceLiteral(bool, "false", false));
        assertNull(mojo.toSourceLiteral(bool, "TRUE", false));
        assertNull(mojo.toSourceLiteral(bool, "True", false));
        assertNull(mojo.toSourceLiteral(bool, "true ", false));

        com.codename1.build.shared.BuildHints.Hint ios =
                com.codename1.build.shared.BuildHints.byName("ios.themeMode");
        assertEquals("ThemeMode.MODERN", mojo.toSourceLiteral(ios, "modern", false));
        assertNull(mojo.toSourceLiteral(ios, "MODERN", false));
    }

    /// An int that does not round-trip would be rewritten too.
    ///
    /// android.targetSDKVersion, not android.min_sdk_version: the floor is an
    /// AndroidMinSdk constant now, while the target stays an int because the
    /// build server's default for it is the highest platform IT has installed --
    /// an unbounded domain this framework build cannot enumerate ahead of time.
    @Test
    public void anIntThatDoesNotRoundTripIsRefused() {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        com.codename1.build.shared.BuildHints.Hint i =
                com.codename1.build.shared.BuildHints.byName("android.targetSDKVersion");
        assertEquals("24", mojo.toSourceLiteral(i, "24", false));
        assertNull(mojo.toSourceLiteral(i, "024", false));
        assertNull(mojo.toSourceLiteral(i, "+24", false));
    }

    /// Top level is brace depth zero, not column zero. Anchoring to the start of
    /// a line refused `  public class MyApp`, which compiles fine -- so the goal
    /// rolled back on a project whose source it had just accepted.
    @Test
    public void anIndentedDeclarationIsStillTopLevel() {
        String src = "package com.example;\n\n  public final class MyApp {\n}\n";
        int at = MigrateBuildHintsMojo.classDeclarationIndex(src, false, "MyApp");
        assertEquals(src.indexOf("  public final class") + 2, at);
    }

    /// The insertion point precedes the modifiers, so the annotations do not land
    /// between `public` and `class`.
    @Test
    public void theInsertionPointPrecedesTheModifiers() {
        String src = "package com.example;\npublic abstract class MyApp {\n}\n";
        assertEquals(src.indexOf("public abstract class"),
                MigrateBuildHintsMojo.classDeclarationIndex(src, false, "MyApp"));
    }

    /// A nested type of the same name is not the top-level declaration.
    @Test
    public void aNestedTypeIsNotTheInsertionPoint() {
        String src = "package com.example;\nclass Outer {\n    class MyApp {}\n}\n"
                + "class MyApp {}\n";
        assertEquals(src.lastIndexOf("class MyApp {}"),
                MigrateBuildHintsMojo.classDeclarationIndex(src, false, "MyApp"));
    }

    /// An import goes above EVERY top-level declaration, so the anchor for a
    /// file with no package and no import is the first declaration in it --
    /// whatever kind it is. Anchoring on the MAIN class instead put the import
    /// below a `fun helper()` written before it, which neither language allows,
    /// and verification rolled back a valid migration.
    @Test
    public void theAnchorIsTheFirstDeclarationNotTheMainClass() {
        String kt = "fun helper() {}\n\nclass MyApp\n";
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration(kt, true));

        String java = "class Helper {}\n\n@Deprecated\nclass MyApp {}\n";
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration(java));
    }

    /// A default-package class that already carries an annotation: the import
    /// must go ABOVE it. Anchoring on the declaration put the import between the
    /// annotation and the class, which is not valid in either language.
    @Test
    public void theImportGoesAboveAnExistingAnnotation() {
        String head = "/* c */\n@SuppressWarnings(\"unchecked\")\n";
        assertEquals(head.indexOf("@SuppressWarnings"),
                MigrateBuildHintsMojo.startOfFirstDeclaration(head));
    }

    /// A parenthesis inside an annotation argument must not stop the walk.
    @Test
    public void anArgumentContainingAParenthesisDoesNotStopTheWalk() {
        String head = "@Deprecated\n@SuppressWarnings(\"a(b\")\n";
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration(head));
    }

    /// With no declaration there is nothing to go above.
    @Test
    public void withNoDeclarationTheAnchorIsTheEnd() {
        String head = "/* copyright */\n\n";
        assertEquals(head.length(), MigrateBuildHintsMojo.startOfFirstDeclaration(head));
    }

    /// Modifiers and annotations may interleave: `public @Deprecated final
    /// class Main` is legal, and the whole run is below the import.
    @Test
    public void theAnchorClearsModifiersInterleavedWithAnnotations() {
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration("public @Deprecated "));
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration("public "));
    }

    /// A Kotlin annotation may have an ESCAPED name, which is code the scan has
    /// to read rather than stop on.
    @Test
    public void anEscapedAnnotationNameIsPartOfTheLeadingRun() {
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration("@`when`\n", true));
        assertEquals(0,
                MigrateBuildHintsMojo.startOfFirstDeclaration("@`when`(\"x\")\n@Deprecated\n", true));
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration("@com.`when`.Ann\n", true));
    }

    /// Kotlin's FILE annotations sit above the package header and the imports
    /// both, so the import goes BELOW them -- the one thing the anchor steps
    /// over rather than displaces.
    @Test
    public void aKotlinFileAnnotationStaysAboveTheImport() {
        String kt = "@file:Suppress(\"unchecked\")\n@file:JvmName(\"X\")\n\nclass MyApp\n";
        assertEquals(kt.indexOf("class MyApp"),
                MigrateBuildHintsMojo.startOfFirstDeclaration(kt, true));

        // One target may carry a BRACKETED list of annotations.
        String grouped = "@file:[JvmName(\"X\") Suppress(\"unchecked\")]\n\nclass MyApp\n";
        assertEquals(grouped.indexOf("class MyApp"),
                MigrateBuildHintsMojo.startOfFirstDeclaration(grouped, true));

        // ...and a bracketed list with no target is not a FILE annotation, so
        // the import goes above it like any other.
        String untargeted = "@[JvmName(\"X\") Suppress(\"unchecked\")]\nclass MyApp\n";
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration(untargeted, true));

        // An ordinary annotation is not a file annotation.
        String ordinary = "@Suppress(\"unchecked\")\nclass MyApp\n";
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration(ordinary, true));

        // Java has no such form, so `@file` there is an ordinary annotation.
        assertEquals(0, MigrateBuildHintsMojo.startOfFirstDeclaration(kt, false));
    }

    /// The migration's source lookup must require a TOP-LEVEL declaration. A
    /// leftover Main.kt holding `class Outer { class Main }` otherwise stopped
    /// the search, the annotations went onto Outer, and the verification build
    /// rejected the placement and rolled the migration back.
    @Test
    public void onlyATopLevelDeclarationIdentifiesTheMainClass() {
        String nested = "package com.example\nclass Outer { class MyApp }\n";
        assertFalse(com.codename1.maven.processors.BuildHintAnnotationProcessor
                .declaresNestedPath(nested, new String[] {"MyApp"}, true));
        String topLevel = "package com.example\nclass Outer { }\nclass MyApp\n";
        assertTrue(com.codename1.maven.processors.BuildHintAnnotationProcessor
                .declaresNestedPath(topLevel, new String[] {"MyApp"}, true));
    }

    /// The word "package" in a header sentence is not the package declaration. A
    /// raw search selected it and the import went in before the real statement,
    /// or inside the comment, so the verification build failed and rolled back an
    /// otherwise correct migration.
    @Test
    public void theWordPackageInACommentIsNotTheDeclaration() {
        String head = "// The package layout is documented here\npackage com.example;\n\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        assertEquals(head.indexOf("package com.example"),
                MigrateBuildHintsMojo.livePackageIndex(code));
    }

    /// The anchor is past the whole declaration, not at the first newline:
    /// `package\ncom.example;` is valid Java and cutting there would put the
    /// import inside the statement.
    @Test
    public void theAnchorClearsAMultiLinePackageDeclaration() {
        String head = "package\ncom.example;\nclass X {}\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        int pkg = MigrateBuildHintsMojo.livePackageIndex(code);
        assertEquals(head.indexOf("class X"),
                MigrateBuildHintsMojo.endOfPackageDeclaration(code, pkg));
    }

    /// Kotlin has no semicolon; the declaration ends with the name.
    @Test
    public void aKotlinPackageDeclarationEndsAtItsName() {
        String head = "package com.example\nclass X\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, true);
        int pkg = MigrateBuildHintsMojo.livePackageIndex(code);
        assertEquals(head.indexOf("class X"),
                MigrateBuildHintsMojo.endOfPackageDeclaration(code, pkg));
    }

    /// `public\nclass Main` is legal. Stopping at the line break left `public` in
    /// the head, so the generated import was written after it -- not valid Java,
    /// and the verification build rolled the migration back.
    @Test
    public void modifiersOnEarlierLinesArePartOfTheDeclaration() {
        String src = "public\nfinal\nclass MyApp {\n}\n";
        assertEquals(0, MigrateBuildHintsMojo.classDeclarationIndex(src, false, "MyApp"));

        String annotated = "@Deprecated\npublic\nclass MyApp {\n}\n";
        assertEquals(annotated.indexOf("public"),
                MigrateBuildHintsMojo.classDeclarationIndex(annotated, false, "MyApp"));
    }

    /// ...and the walk still stops at a word that is not a modifier.
    @Test
    public void aNonModifierWordStopsTheWalk() {
        String src = "interface Other {}\npublic class MyApp {}\n";
        assertEquals(src.indexOf("public class"),
                MigrateBuildHintsMojo.classDeclarationIndex(src, false, "MyApp"));
    }

    /// The words in a comment are not an import. A javadoc line mentioning the
    /// package aborted the migration on a source that compiles perfectly well.
    @Test
    public void mentioningThePackageIsNotImportingIt() {
        String mention = com.codename1.maven.processors.BuildHintAnnotationProcessor.blankNonCode(
                "// see com.codename1.annotations.buildhints for the annotations\n"
                        + "public class MyApp {}\n", false);
        assertFalse(MigrateBuildHintsMojo.importsBuildHints(mention));

        String real = com.codename1.maven.processors.BuildHintAnnotationProcessor.blankNonCode(
                "import com.codename1.annotations.buildhints.Ios;\npublic class MyApp {}\n",
                false);
        assertTrue(MigrateBuildHintsMojo.importsBuildHints(real));
    }

    /// An import may legally span lines, so the insertion point is the end of the
    /// DECLARATION. Cutting at the first newline after the keyword spliced the
    /// new import into the middle of the old one.
    @Test
    public void theInsertionPointClearsAMultiLineImport() {
        String head = "import java.\n util.List;\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        int last = MigrateBuildHintsMojo.lastImportIndex(code);
        assertEquals(head.length(), MigrateBuildHintsMojo.endOfImportDeclaration(code, last));
    }

    /// A Kotlin main class may escape its name in backticks, and
    /// `codename1.mainName` holds the name between them. Reading only identifier
    /// characters recorded nothing, so the goal reported "Could not find the
    /// class declaration" and rolled back a valid migration of a file the
    /// lookup had just accepted as the right one.
    @Test
    public void theDeclarationLocatorReadsAnEscapedKotlinName() {
        String src = "package com.example\n\nclass `when` {\n}\n";
        assertEquals(src.indexOf("class `when`"),
                MigrateBuildHintsMojo.classDeclarationIndex(src, true, "when"));
        // An escaped declaration also counts as the first one, which is what a
        // name that matches nothing falls back to. Recording no name at all left
        // even that fallback unset.
        assertEquals(src.indexOf("class `when`"),
                MigrateBuildHintsMojo.classDeclarationIndex(src, true, "Other"));
    }

    /// `blankNonCode` leaves an escaped identifier as the code it is, so a
    /// scanner looking for a KEYWORD has to step over it. `fun `import`() {}`
    /// declares a function called import, and reading it as an import directive
    /// put the generated import after a top-level declaration -- where Kotlin
    /// does not allow one, so verification failed and rolled back a valid
    /// migration.
    @Test
    public void anEscapedIdentifierIsNotAKeyword() {
        String kt = "package com.example\n\nfun `import`() {}\n\nclass MyApp\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(kt, true);
        assertEquals(-1, MigrateBuildHintsMojo.lastImportIndex(code));
        assertFalse(MigrateBuildHintsMojo.importsBuildHints(code));
        assertEquals(0, MigrateBuildHintsMojo.livePackageIndex(code));

        // ...and the package keyword the same way. A file with no package
        // declaration at all has none, whatever a function is called.
        String noPackage = "fun `package`() {}\n\nclass MyApp\n";
        assertEquals(-1, MigrateBuildHintsMojo.livePackageIndex(
                com.codename1.maven.processors.BuildHintAnnotationProcessor
                        .blankNonCode(noPackage, true)));
    }

    /// The build hints PACKAGE, not any name that starts with its letters. An
    /// unrelated `com.codename1.annotations.buildhintsExtra.Widget` read as
    /// "already imported" and aborted a migration with nothing to conflict with.
    @Test
    public void anImportOfALongerPackageIsNotOurs() {
        String other = com.codename1.maven.processors.BuildHintAnnotationProcessor.blankNonCode(
                "import com.codename1.annotations.buildhintsExtra.Widget;\n"
                        + "public class MyApp {}\n", false);
        assertFalse(MigrateBuildHintsMojo.importsBuildHints(other));

        String ours = com.codename1.maven.processors.BuildHintAnnotationProcessor.blankNonCode(
                "import com.codename1.annotations.buildhints.*;\npublic class MyApp {}\n",
                false);
        assertTrue(MigrateBuildHintsMojo.importsBuildHints(ours));
    }

    /// A blanked block comment keeps its newlines, so
    /// `import foo.Bar /* note\n */ ;` left the semicolon unconsumed and ended
    /// the declaration at that newline -- INSIDE the comment, where the
    /// generated import was then written and stayed commented out.
    @Test
    public void theInsertionPointClearsAMultiLineTrailingComment() {
        String head = "import foo.Bar /* note\n */ ;\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        int last = MigrateBuildHintsMojo.lastImportIndex(code);
        assertEquals(head.length(), MigrateBuildHintsMojo.endOfImportDeclaration(code, last));
    }

    /// The package declaration has the same terminator and the same hazard.
    @Test
    public void theAnchorClearsAMultiLineTrailingComment() {
        String head = "package com.example /* note\n */ ;\nclass X {}\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        int pkg = MigrateBuildHintsMojo.livePackageIndex(code);
        assertEquals(head.indexOf("class X"),
                MigrateBuildHintsMojo.endOfPackageDeclaration(code, pkg));
    }

    /// `static` is a modifier, not the imported name. Reading it as the name
    /// ended the declaration at the newline inside the REAL name, so the
    /// generated import was spliced into the middle of the static import and the
    /// verification build rolled back a valid migration.
    @Test
    public void theInsertionPointClearsAMultiLineStaticImport() {
        String head = "import static java.util.\n Collections.emptyList;\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        int last = MigrateBuildHintsMojo.lastImportIndex(code);
        assertEquals(head.length(), MigrateBuildHintsMojo.endOfImportDeclaration(code, last));
    }

    /// A name that merely STARTS with `static` is a name.
    @Test
    public void anImportOfATypeNamedStaticallyIsNotAStaticImport() {
        String head = "import staticky.Thing;\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        int last = MigrateBuildHintsMojo.lastImportIndex(code);
        assertEquals(head.length(), MigrateBuildHintsMojo.endOfImportDeclaration(code, last));
    }

    /// A Kotlin alias belongs to the declaration too.
    @Test
    public void theInsertionPointClearsAKotlinAlias() {
        String head = "import com.example.Ios as TheirIos\nclass X\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, true);
        int last = MigrateBuildHintsMojo.lastImportIndex(code);
        assertEquals(head.indexOf("class X"),
                MigrateBuildHintsMojo.endOfImportDeclaration(code, last));
    }

    /// The LAST import is the anchor, not the first.
    @Test
    public void theAnchorIsTheLastImport() {
        String head = "import a.B;\nimport c.D;\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        assertEquals(head.indexOf("import c.D"), MigrateBuildHintsMojo.lastImportIndex(code));
    }

    /// `package com.\nexample;` is legal, and a contiguous scan stops at the
    /// newline -- so the import was inserted before `example;`, producing invalid
    /// source, and the verification build rolled back a correct migration.
    @Test
    public void theAnchorClearsAPackageNameThatSpansLines() {
        String head = "package com.\nexample;\nclass X {}\n";
        String code = com.codename1.maven.processors.BuildHintAnnotationProcessor
                .blankNonCode(head, false);
        int pkg = MigrateBuildHintsMojo.livePackageIndex(code);
        assertEquals(head.indexOf("class X"),
                MigrateBuildHintsMojo.endOfPackageDeclaration(code, pkg));
    }

    /// The goal promises to delete the migrated declarations and leave every
    /// other line byte for byte as it was. Reading with readLine() discarded
    /// each terminator, and appending a newline to every retained line rewrote a
    /// CRLF checkout end to end -- a whole-file diff from a goal that should
    /// have touched three lines.
    @Test
    public void removingLinesKeepsTheFilesOwnLineEndings() throws Exception {
        File f = File.createTempFile("cn1-settings", ".properties");
        f.deleteOnExit();
        String before = "# a comment\r\n"
                + "codename1.displayName=Demo\r\n"
                + "codename1.arg.ios.pods=Alamofire\r\n"
                + "codename1.arg.android.min_sdk_version=24\r\n";
        write(f, before);

        MigrateBuildHintsMojo.removeMigratedLines(f,
                java.util.Arrays.asList("codename1.arg.ios.pods"));

        assertEquals("# a comment\r\n"
                        + "codename1.displayName=Demo\r\n"
                        + "codename1.arg.android.min_sdk_version=24\r\n",
                read(f));
    }

    /// A file that does not end in a newline must not acquire one, and a mixed
    /// file keeps each line as it found it.
    @Test
    public void removingLinesInventsNoTerminator() throws Exception {
        File f = File.createTempFile("cn1-settings", ".properties");
        f.deleteOnExit();
        write(f, "codename1.arg.ios.pods=Alamofire\n"
                + "codename1.displayName=Demo\r\n"
                + "codename1.arg.ios.teamId=ABCDE12345");
        MigrateBuildHintsMojo.removeMigratedLines(f,
                java.util.Arrays.asList("codename1.arg.ios.pods"));
        assertEquals("codename1.displayName=Demo\r\n"
                + "codename1.arg.ios.teamId=ABCDE12345", read(f));
    }

    /// A continuation belongs to its declaration, terminators included.
    @Test
    public void removingAContinuedDeclarationTakesEveryLineOfIt() throws Exception {
        File f = File.createTempFile("cn1-settings", ".properties");
        f.deleteOnExit();
        write(f, "codename1.arg.ios.pods=Alamofire,\\\r\n"
                + "    SwiftyJSON\r\n"
                + "codename1.displayName=Demo\r\n");
        MigrateBuildHintsMojo.removeMigratedLines(f,
                java.util.Arrays.asList("codename1.arg.ios.pods"));
        assertEquals("codename1.displayName=Demo\r\n", read(f));
    }

    private static void write(File f, String text) throws Exception {
        java.io.Writer w = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(f), "ISO-8859-1");
        try {
            w.write(text);
        } finally {
            w.close();
        }
    }

    private static String read(File f) throws Exception {
        byte[] all = java.nio.file.Files.readAllBytes(f.toPath());
        return new String(all, "ISO-8859-1");
    }

    /// The source is read byte for byte, while `codename1.packageName` and
    /// `codename1.mainName` come from a properties file and are real Unicode.
    /// For an ASCII name the two spellings are identical; for
    /// `package com.\u5e94\u7528` in a UTF-8 file they are not, and comparing
    /// only the Unicode one made the goal refuse a valid migration saying it
    /// could not find the main source.
    @Test
    public void aNonAsciiNameIsMatchedAsTheSourceSpellsIt() throws Exception {
        String pkg = "com.\u5e94\u7528";
        String asWritten = MigrateBuildHintsMojo.asWrittenInSource(pkg);
        // The bytes of the UTF-8 file, read one per character.
        assertEquals(new String(pkg.getBytes("UTF-8"), "ISO-8859-1"), asWritten);
        assertTrue("a non-ASCII name must read differently", !asWritten.equals(pkg));

        // An ASCII name is untouched, so nothing about the common case changes.
        assertEquals("com.example", MigrateBuildHintsMojo.asWrittenInSource("com.example"));

        // And the declaration locator accepts either spelling of the class name.
        String source = "package " + asWritten + ";\n\nclass "
                + MigrateBuildHintsMojo.asWrittenInSource("\u5e94\u7528") + " {\n}\n";
        assertEquals(source.indexOf("class "),
                MigrateBuildHintsMojo.classDeclarationIndex(source, false, "\u5e94\u7528"));
    }

    /// The continuation backslash is a MARKER, not part of the value.
    /// `Properties.load` drops it, so leaving it in made `key\` + ` =value` read
    /// as an escaped `=`; the key never matched the one being migrated, the
    /// declaration stayed behind, and the verification build failed on the
    /// duplicate the goal had just created.
    @Test
    public void aSeparatorOnAContinuationLineStillNamesItsKey() throws Exception {
        File f = File.createTempFile("cn1-settings", ".properties");
        f.deleteOnExit();
        write(f, "codename1.arg.ios.teamId\\\n"
                + " =ABCDE12345\n"
                + "codename1.displayName=Demo\n");

        // Exactly what Properties.load calls the key, so the plan and the
        // removal cannot disagree about it.
        java.util.Properties loaded = new java.util.Properties();
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            loaded.load(in);
        } finally {
            in.close();
        }
        assertEquals("ABCDE12345", loaded.getProperty("codename1.arg.ios.teamId"));

        MigrateBuildHintsMojo.removeMigratedLines(f,
                java.util.Arrays.asList("codename1.arg.ios.teamId"));
        assertEquals("codename1.displayName=Demo\n", read(f));
    }

    /// A continuation marker with nothing after it is still a marker: a file
    /// whose last byte is that backslash reads as an empty value to
    /// `Properties.load`, while leaving it in produced a key ending in `\` that
    /// matched nothing -- so the declaration stayed and the verification build
    /// failed on the duplicate.
    @Test
    public void aContinuationMarkerAtTheEndOfTheFileIsStillAMarker() throws Exception {
        File f = File.createTempFile("cn1-settings", ".properties");
        f.deleteOnExit();
        write(f, "codename1.displayName=Demo\n"
                + "codename1.arg.ios.teamId\\");

        java.util.Properties loaded = new java.util.Properties();
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            loaded.load(in);
        } finally {
            in.close();
        }
        assertEquals("", loaded.getProperty("codename1.arg.ios.teamId"));

        MigrateBuildHintsMojo.removeMigratedLines(f,
                java.util.Arrays.asList("codename1.arg.ios.teamId"));
        assertEquals("codename1.displayName=Demo\n", read(f));
    }

    /// A comment is a natural line: continuation does not apply to it, so
    /// `# note \` ends at the newline and the declaration below it is an
    /// ordinary property. Joining the two made the pair read as a comment, so
    /// the migrated declaration was retained and the verification build failed
    /// on the duplicate.
    @Test
    public void aCommentEndingInABackslashDoesNotSwallowTheNextLine() throws Exception {
        File f = File.createTempFile("cn1-settings", ".properties");
        f.deleteOnExit();
        write(f, "# note \\\n"
                + "codename1.arg.ios.pods=Alamofire\n"
                + "codename1.displayName=Demo\n");

        java.util.Properties loaded = new java.util.Properties();
        java.io.InputStream in = new java.io.FileInputStream(f);
        try {
            loaded.load(in);
        } finally {
            in.close();
        }
        assertEquals("Alamofire", loaded.getProperty("codename1.arg.ios.pods"));

        MigrateBuildHintsMojo.removeMigratedLines(f,
                java.util.Arrays.asList("codename1.arg.ios.pods"));
        assertEquals("# note \\\ncodename1.displayName=Demo\n", read(f));
    }

    /// A `!` comment is a comment too, and a real continuation still continues.
    @Test
    public void onlyCommentsAreExemptFromContinuation() throws Exception {
        File f = File.createTempFile("cn1-settings", ".properties");
        f.deleteOnExit();
        write(f, "! note \\\n"
                + "codename1.arg.ios.pods=Alamofire,\\\n"
                + "    SwiftyJSON\n"
                + "codename1.displayName=Demo\n");

        MigrateBuildHintsMojo.removeMigratedLines(f,
                java.util.Arrays.asList("codename1.arg.ios.pods"));
        assertEquals("! note \\\ncodename1.displayName=Demo\n", read(f));
    }

    /// A wildcard import loses to an explicit `import com.example.Build;` and to
    /// a type in the file's own package, so the generated `@Build` referred to
    /// theirs and the verification build failed. Named imports are written
    /// instead, and the fully qualified name for any simple name the file has
    /// already given away.
    @Test
    public void theGeneratedAnnotationsNameOurAnnotations() throws Exception {
        String migrated = migrate("package com.example;\n"
                        + "import com.example.other.Build;\n"
                        + "public class MyApp {\n}\n",
                "@Build(nativeTheme = ThemeMode.MODERN)\n@Ios(teamId = \"X\")\n");

        // The taken name is qualified and not imported...
        assertTrue(migrated,
                migrated.contains("@com.codename1.annotations.buildhints.Build(nativeTheme"));
        assertFalse(migrated,
                migrated.contains("import com.codename1.annotations.buildhints.Build;"));
        // ...the free one is imported and written plainly...
        assertTrue(migrated, migrated.contains("import com.codename1.annotations.buildhints.Ios;"));
        assertTrue(migrated, migrated.contains("@Ios(teamId"));
        // ...and the package is never imported on demand.
        assertFalse(migrated, migrated.contains("buildhints.*"));
    }

    /// A type in the file's own package is beaten by the named import, so it is
    /// not a collision; a type in THIS file cannot be imported at all.
    @Test
    public void onlyThisFilesOwnDeclarationForcesTheQualifiedName() throws Exception {
        String plain = migrate("package com.example;\npublic class MyApp {\n}\n",
                "@Ios(teamId = \"X\")\n");
        assertTrue(plain, plain.contains("import com.codename1.annotations.buildhints.Ios;"));
        assertTrue(plain, plain.contains("@Ios(teamId"));

        String declaresIt = migrate("package com.example;\n"
                        + "@interface Ios { String teamId(); }\n"
                        + "public class MyApp {\n}\n",
                "@Ios(teamId = \"X\")\n");
        assertTrue(declaresIt,
                declaresIt.contains("@com.codename1.annotations.buildhints.Ios(teamId"));
        assertFalse(declaresIt,
                declaresIt.contains("import com.codename1.annotations.buildhints.Ios;"));
    }

    /// A Kotlin alias is a TOKEN, and `import com.example.Other as\nIos` is
    /// legal. Searching for the literal `" as "` missed it, so the goal wrote
    /// its own `import ...buildhints.Ios` beside it -- two imports giving the
    /// same local name, which does not compile.
    @Test
    public void anAliasSpanningLinesStillTakesTheName() throws Exception {
        String migrated = migrateKotlin("package com.example\n"
                        + "import com.example.other.Other as\n Ios\n"
                        + "class MyApp\n",
                "@Ios(teamId = \"X\")\n");
        assertTrue(migrated,
                migrated.contains("@com.codename1.annotations.buildhints.Ios(teamId"));
        assertFalse(migrated, migrated.contains("import com.codename1.annotations.buildhints.Ios"));

        // An alias that takes some OTHER name leaves ours alone.
        String free = migrateKotlin("package com.example\n"
                        + "import com.example.other.Other as\n Something\n"
                        + "class MyApp\n",
                "@Ios(teamId = \"X\")\n");
        assertTrue(free, free.contains("import com.codename1.annotations.buildhints.Ios"));
        assertTrue(free, free.contains("@Ios(teamId"));
    }

    /// An enum-valued hint renders as `ThemeMode.MODERN`, which is a second
    /// type to account for: without its own import the generated annotation does
    /// not compile, so every enum-valued migration was rolled back by its own
    /// verification build.
    @Test
    public void anEnumValueBringsItsOwnType() throws Exception {
        String migrated = migrate("package com.example;\npublic class MyApp {\n}\n",
                "@Ios(themeMode = ThemeMode.MODERN)\n");
        assertTrue(migrated,
                migrated.contains("import com.codename1.annotations.buildhints.ThemeMode;"));
        assertTrue(migrated, migrated.contains("@Ios(themeMode = ThemeMode.MODERN)"));

        // A file that has given that name away gets the qualified form instead.
        String taken = migrate("package com.example;\n"
                        + "import com.example.other.ThemeMode;\n"
                        + "public class MyApp {\n}\n",
                "@Ios(themeMode = ThemeMode.MODERN)\n");
        assertTrue(taken, taken.contains(
                "themeMode = com.codename1.annotations.buildhints.ThemeMode.MODERN"));
        assertFalse(taken,
                taken.contains("import com.codename1.annotations.buildhints.ThemeMode;"));
    }

    /// A `typealias` is a declaration this file makes, so it takes the name as
    /// surely as a class does. Writing a named import beside one gives the same
    /// local name twice, which does not compile.
    @Test
    public void aTypeAliasTakesTheNameTheImportWouldWant() throws Exception {
        String migrated = migrateKotlin("package com.example\n"
                        + "typealias Ios = com.example.other.Ios\n"
                        + "class MyApp\n",
                "@Ios(teamId = \"X\")\n");
        assertTrue(migrated,
                migrated.contains("@com.codename1.annotations.buildhints.Ios(teamId"));
        assertFalse(migrated,
                migrated.contains("import com.codename1.annotations.buildhints.Ios"));
    }

    private String migrateKotlin(String source, String annotations) throws Exception {
        File f = File.createTempFile("MyApp", ".kt");
        f.deleteOnExit();
        write(f, source);
        new MigrateBuildHintsMojo().insertAnnotations(f, annotations, "MyApp");
        return read(f);
    }

    /// The byte spelling depends on the encoding the compiler uses: the UTF-8
    /// bytes of a name are not its Shift_JIS bytes, so assuming UTF-8 matched
    /// neither spelling of a non-ASCII name in a multibyte-encoded source and
    /// the goal refused a migration it could have made.
    @Test
    public void theByteSpellingFollowsTheCompilersEncoding() throws Exception {
        String pkg = "com.\u30a2\u30d7\u30ea";
        assertEquals(new String(pkg.getBytes("Shift_JIS"), "ISO-8859-1"),
                MigrateBuildHintsMojo.asWrittenInSource(pkg, "Shift_JIS"));
        assertEquals(new String(pkg.getBytes("UTF-8"), "ISO-8859-1"),
                MigrateBuildHintsMojo.asWrittenInSource(pkg, "UTF-8"));
        // The two disagree, which is the whole point.
        assertFalse(MigrateBuildHintsMojo.asWrittenInSource(pkg, "Shift_JIS")
                .equals(MigrateBuildHintsMojo.asWrittenInSource(pkg, "UTF-8")));

        // ASCII is ASCII in every one of them, so the common case is untouched.
        assertEquals("com.example",
                MigrateBuildHintsMojo.asWrittenInSource("com.example", "Shift_JIS"));

        // Decoded, the plain name matches -- which is why identifying the file
        // reads it in its own encoding rather than comparing byte spellings.
        // Whether a byte spelling happens to be readable as an identifier
        // depends on the bytes: these katakana are letters in ISO-8859-1, while
        // the CJK ideographs a page over are control characters. Reading the
        // source properly does not depend on which.
        String decoded = "package " + pkg + ";\n\nclass \u30a2\u30d7\u30ea {\n}\n";
        assertEquals(pkg, com.codename1.maven.processors.BuildHintAnnotationProcessor
                .declaredPackageIn(decoded, false));
        assertTrue(com.codename1.maven.processors.BuildHintAnnotationProcessor
                .declaresType(decoded, "\u30a2\u30d7\u30ea", false));

        // So the real source, written in Shift_JIS, is identified by reading it
        // in Shift_JIS -- and is not identified when UTF-8 is assumed.
        File f = File.createTempFile("Sjis", ".java");
        f.deleteOnExit();
        java.io.OutputStream os = new java.io.FileOutputStream(f);
        try {
            os.write(decoded.getBytes("Shift_JIS"));
        } finally {
            os.close();
        }
        assertTrue(mojoWithEncoding("Shift_JIS").declares(f, pkg, "\u30a2\u30d7\u30ea"));
        assertFalse(mojoWithEncoding("UTF-8").declares(f, pkg, "\u30a2\u30d7\u30ea"));
    }

    /// A project that declares no encoding is not assumed to be UTF-8.
    ///
    /// javac with no `-encoding` uses the PLATFORM default, so a source in a
    /// single-byte encoding with a non-ASCII class name is perfectly ordinary.
    /// Read as UTF-8 it comes back as replacement characters, the name never
    /// matches, and the goal refused a project that compiles.
    @Test
    public void anUndeclaredEncodingIsNotAssumedToBeUtf8() throws Exception {
        String pkg = "com.example";
        // A name that is not ASCII and not valid UTF-8 in this encoding's bytes.
        String main = "Caf\u00e9App";
        String source = "package " + pkg + ";\npublic class " + main + " {\n}\n";
        File f = File.createTempFile("Latin1", ".java");
        f.deleteOnExit();
        java.io.OutputStream os = new java.io.FileOutputStream(f);
        try {
            os.write(source.getBytes("windows-1252"));
        } finally {
            os.close();
        }

        // No project.build.sourceEncoding anywhere, which is the whole point.
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        mojo.project = new org.apache.maven.project.MavenProject();

        assertTrue("a source that compiles was reported as not declaring its class",
                mojo.declares(f, pkg, main));
    }

    /// The rewrite uses the charset the read used.
    ///
    /// These were two chains: the read fell back to the platform default and the
    /// rewrite to byte-transparent. On a platform whose default is UTF-16 the
    /// migration therefore identified the main class correctly and then spliced
    /// single-byte ASCII into it -- the verification build failed and every
    /// migration rolled back.
    @Test
    public void theRewriteUsesTheCharsetTheReadUsed() throws Exception {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        mojo.project = new org.apache.maven.project.MavenProject();

        // Nothing declares an encoding, and the file is plain ASCII, so the
        // platform default decodes it and the two must agree on that.
        File ascii = File.createTempFile("Ascii", ".java");
        ascii.deleteOnExit();
        write(ascii, "package com.example;\npublic class MyApp {\n}\n");
        assertEquals(mojo.sourceCharset(ascii), mojo.rewriteCharset(ascii));

        // ...and a file the platform charset cannot decode falls to
        // byte-transparent on BOTH paths rather than one of them.
        File broken = File.createTempFile("Broken", ".java");
        broken.deleteOnExit();
        java.io.OutputStream os = new java.io.FileOutputStream(broken);
        try {
            os.write("package com.example;\n// ".getBytes("ISO-8859-1"));
            os.write(0xFF);
            os.write("\npublic class MyApp {\n}\n".getBytes("ISO-8859-1"));
        } finally {
            os.close();
        }
        assertEquals(mojo.sourceCharset(broken), mojo.rewriteCharset(broken));
    }

    /// A -Dcodename1.mainName override decides the entry point for the whole
    /// migration, and only the entry point.
    ///
    /// The verification build is a nested Maven run that inherits the
    /// developer's command line, so it applies the override whether or not the
    /// migration does. Annotating the file's entry point while the verification
    /// expected the overridden one made process-annotations call the placement
    /// misplaced, and the migration rolled back a change that was correct.
    @Test
    public void theEntryPointOverrideAppliesToTheWholeMigration() throws Exception {
        java.util.Properties settings = settingsFor("com.example", "FileApp");
        settings.setProperty("codename1.arg.ios.pods", "FromTheFile");

        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        mojo.properties = new java.util.Properties();
        mojo.properties.setProperty("codename1.mainName", "OverriddenApp");
        mojo.properties.setProperty("codename1.packageName", "com.other");
        // What -D also carried, and what must NOT become a migrated hint: an
        // override of a value for one build is not a declaration in the project.
        mojo.properties.setProperty("codename1.arg.desktop.titleBar", "NATIVE");

        mojo.overlayEffectiveIdentity(settings);

        assertEquals("OverriddenApp", settings.getProperty("codename1.mainName"));
        assertEquals("com.other", settings.getProperty("codename1.packageName"));
        assertEquals("FromTheFile", settings.getProperty("codename1.arg.ios.pods"));
        assertNull(settings.getProperty("codename1.arg.desktop.titleBar"));
    }

    /// With nothing overridden the file still decides.
    @Test
    public void theFileDecidesTheEntryPointWhenNothingOverrodeIt() throws Exception {
        java.util.Properties settings = settingsFor("com.example", "FileApp");
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        mojo.overlayEffectiveIdentity(settings);
        assertEquals("FileApp", settings.getProperty("codename1.mainName"));
        assertEquals("com.example", settings.getProperty("codename1.packageName"));
    }

    /// Maven's roots decide which file is the main class, not the conventions.
    ///
    /// A module that replaces `src/main/java` with a root of its own can still
    /// have a dormant copy of the main class at the conventional path. Trying
    /// the conventions first let that copy win: the annotations went into a file
    /// Maven ignores, the verification build found no manifest, and the
    /// migration rolled itself back on a perfectly valid project.
    @Test
    public void theSourceComesFromTheRootsMavenCompiles() throws Exception {
        File basedir = tmp.newFolder();
        String source = "package com.example;\npublic class MyApp {\n}\n";
        File dormant = new File(basedir, "src/main/java/com/example");
        assertTrue(dormant.mkdirs());
        write(new File(dormant, "MyApp.java"), source);
        File live = new File(basedir, "appsrc/com/example");
        assertTrue(live.mkdirs());
        write(new File(live, "MyApp.java"), source);

        MigrateBuildHintsMojo mojo = mojoAt(basedir);
        mojo.project.addCompileSourceRoot(new File(basedir, "appsrc").getAbsolutePath());

        String found = mojo.findMainClassSource(basedir, settingsFor("com.example", "MyApp"));
        assertEquals(new File(live, "MyApp.java").getAbsolutePath(), found);
    }

    /// ...and when Maven answered, that IS the set: a main class that is in no
    /// compiled root is not found by falling back to the conventions.
    @Test
    public void aMainClassOutsideEveryCompiledRootIsNotFound() throws Exception {
        File basedir = tmp.newFolder();
        File dormant = new File(basedir, "src/main/java/com/example");
        assertTrue(dormant.mkdirs());
        write(new File(dormant, "MyApp.java"), "package com.example;\npublic class MyApp {\n}\n");

        MigrateBuildHintsMojo mojo = mojoAt(basedir);
        mojo.project.addCompileSourceRoot(new File(basedir, "appsrc").getAbsolutePath());

        assertNull(mojo.findMainClassSource(basedir, settingsFor("com.example", "MyApp")));
    }

    /// With no reactor to ask -- which is how this goal runs outside a build --
    /// the conventions are the only guess there is, and they still work.
    @Test
    public void theConventionsAreUsedWhenNobodyResolvedTheRoots() throws Exception {
        File basedir = tmp.newFolder();
        File dir = new File(basedir, "src/main/java/com/example");
        assertTrue(dir.mkdirs());
        write(new File(dir, "MyApp.java"), "package com.example;\npublic class MyApp {\n}\n");

        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        mojo.project = new org.apache.maven.project.MavenProject();
        // No reactorProjects, so moduleAt finds nothing and compileSourceRoots
        // returns null.
        assertEquals(new File(dir, "MyApp.java").getAbsolutePath(),
                mojo.findMainClassSource(basedir, settingsFor("com.example", "MyApp")));
    }

    private MigrateBuildHintsMojo mojoAt(File basedir) {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        org.apache.maven.project.MavenProject project =
                new org.apache.maven.project.MavenProject();
        project.setBuild(new org.apache.maven.model.Build());
        project.setFile(new File(basedir, "pom.xml"));
        mojo.project = project;
        mojo.reactorProjects = java.util.Collections.singletonList(project);
        return mojo;
    }

    private static java.util.Properties settingsFor(String pkg, String main) {
        java.util.Properties p = new java.util.Properties();
        p.setProperty("codename1.packageName", pkg);
        p.setProperty("codename1.mainName", main);
        return p;
    }

    /// A multibyte name is not readable in the byte-transparent view, and
    /// falling back to the first top-level declaration put the annotations on
    /// an ASCII helper that happened to come first -- which the verification
    /// build then rejected.
    @Test
    public void theNamedDeclarationIsFoundInAMultibyteSource() throws Exception {
        String main = "\u30a2\u30d7\u30ea";
        String decoded = "package com.example;\n"
                + "class Helper {\n}\n"
                + "class " + main + " {\n}\n";
        File f = File.createTempFile("Sjis", ".java");
        f.deleteOnExit();
        java.io.OutputStream os = new java.io.FileOutputStream(f);
        try {
            os.write(decoded.getBytes("Shift_JIS"));
        } finally {
            os.close();
        }

        MigrateBuildHintsMojo mojo = mojoWithEncoding("Shift_JIS");
        mojo.insertAnnotations(f, "@Ios(teamId = \"X\")\n", main);

        String migrated = new String(java.nio.file.Files.readAllBytes(f.toPath()), "Shift_JIS");
        // Above the main class, not above the helper that precedes it.
        assertTrue(migrated, migrated.indexOf("@Ios(teamId") > migrated.indexOf("class Helper"));
        assertTrue(migrated, migrated.indexOf("@Ios(teamId") < migrated.indexOf("class " + main));
        // ...and the rest of the file survives its own encoding intact.
        assertTrue(migrated, migrated.contains("class " + main));
    }

    /// A UTF-16 source is parsed and rewritten in its own charset.
    ///
    /// The rewrite is byte-transparent by default -- read and write ISO-8859-1
    /// so every byte round-trips -- which is right for a file whose encoding
    /// cannot be known and works for any ASCII-compatible one. UTF-16 is
    /// neither: every ASCII character is two bytes with a NUL beside it, so read
    /// that way the file is `p\0a\0c\0k\0...`. No import and no package
    /// declaration is found, and the ASCII spliced in would be written as single
    /// bytes into a two-byte-per-character file -- mangling a source `declares`
    /// had just correctly identified, since that half already reads in the
    /// compiler's charset.
    @Test
    public void aUtf16SourceIsRewrittenInItsOwnCharset() throws Exception {
        String decoded = "package com.example;\n"
                + "\n"
                + "import com.codename1.system.Lifecycle;\n"
                + "\n"
                + "public class MyApp extends Lifecycle {\n}\n";
        File f = File.createTempFile("Utf16", ".java");
        f.deleteOnExit();
        java.io.OutputStream os = new java.io.FileOutputStream(f);
        try {
            os.write(decoded.getBytes("UTF-16LE"));
        } finally {
            os.close();
        }

        MigrateBuildHintsMojo mojo = mojoWithEncoding("UTF-16LE");
        mojo.insertAnnotations(f, "@Ios(teamId = \"X\")\n", "MyApp");

        // Still UTF-16LE, and still valid: read back in any other charset this
        // would be unreadable rather than merely different.
        String migrated = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-16LE");
        assertTrue(migrated, migrated.contains("@Ios(teamId = \"X\")"));
        // The annotation goes above the class, and the import lands after the
        // existing one rather than at the top of the file.
        assertTrue(migrated, migrated.indexOf("@Ios(teamId") < migrated.indexOf("public class MyApp"));
        assertTrue(migrated, migrated.indexOf("import com.codename1.annotations.buildhints.Ios;")
                > migrated.indexOf("import com.codename1.system.Lifecycle;"));
        assertTrue(migrated, migrated.startsWith("package com.example;"));
    }

    /// ...but a file that does NOT round-trip through the declared charset is
    /// left byte-transparent.
    ///
    /// A source declared UTF-8 that is not valid UTF-8 decodes into replacement
    /// characters quite happily, and writing that back destroys the bytes it
    /// could not read. Refusing to decode is the conservative answer: the
    /// migration either works byte-transparently or does not happen.
    @Test
    public void aSourceThatDoesNotDecodeKeepsItsBytes() throws Exception {
        byte[] head = "package com.example;\npublic class MyApp {\n".getBytes("ISO-8859-1");
        // 0xFF is not a legal UTF-8 lead byte anywhere.
        byte[] raw = new byte[head.length + 6];
        System.arraycopy(head, 0, raw, 0, head.length);
        raw[head.length] = '/';
        raw[head.length + 1] = '/';
        raw[head.length + 2] = (byte) 0xFF;
        raw[head.length + 3] = '\n';
        raw[head.length + 4] = '}';
        raw[head.length + 5] = '\n';
        File f = File.createTempFile("Broken", ".java");
        f.deleteOnExit();
        java.io.OutputStream os = new java.io.FileOutputStream(f);
        try {
            os.write(raw);
        } finally {
            os.close();
        }

        MigrateBuildHintsMojo mojo = mojoWithEncoding("UTF-8");
        mojo.insertAnnotations(f, "@Ios(teamId = \"X\")\n", "MyApp");

        byte[] after = java.nio.file.Files.readAllBytes(f.toPath());
        String asBytes = new String(after, "ISO-8859-1");
        assertTrue(asBytes, asBytes.contains("@Ios(teamId = \"X\")"));
        // The byte it could not decode is still exactly that byte.
        assertTrue(asBytes, asBytes.indexOf((char) 0xFF) >= 0);
    }

    /// The manifest's presence proves nothing on its own: a project can keep
    /// one in `src/main/resources`, and any resource-producing plugin then
    /// recreates it whether or not the processor ran -- so the keys are all
    /// there, the migration is reported as verified, and the properties lines
    /// are deleted for good although nothing processed the annotations.
    @Test
    public void aManifestTheProcessorDidNotWriteIsNotVerification() throws Exception {
        File out = File.createTempFile("classes", "");
        assertTrue(out.delete() && out.mkdirs());
        out.deleteOnExit();
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();

        // A copied resource: the hints are there, the fingerprint is not.
        java.util.Properties copied = new java.util.Properties();
        copied.setProperty("codename1.arg.ios.pods", "Alamofire");
        assertTrue(String.valueOf(mojo.notWrittenByTheProcessor(copied, out, "com.example.MyApp"))
                .contains("sourceDigest"));

        // Somebody else's output, which the stamp gives away.
        java.util.Properties theirs = new java.util.Properties();
        theirs.setProperty("cn1.buildHints.mainClass", "com.other.TheirApp");
        theirs.setProperty("cn1.buildHints.sourceDigest", "whatever");
        assertTrue(String.valueOf(mojo.notWrittenByTheProcessor(theirs, out, "com.example.MyApp"))
                .contains("com.other.TheirApp"));

        // A fingerprint with no class to check it against cannot be judged, and
        // is not called a failure on a guess.
        java.util.Properties ours = new java.util.Properties();
        ours.setProperty("cn1.buildHints.mainClass", "com.example.MyApp");
        ours.setProperty("cn1.buildHints.sourceDigest", "whatever");
        assertNull(mojo.notWrittenByTheProcessor(ours, out, "com.example.MyApp"));
    }

    /// A mojo whose project compiles with `encoding`.
    private MigrateBuildHintsMojo mojoWithEncoding(String encoding) {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        org.apache.maven.project.MavenProject project =
                new org.apache.maven.project.MavenProject();
        project.getProperties().setProperty("project.build.sourceEncoding", encoding);
        mojo.project = project;
        return mojo;
    }

    /// Runs the real insertion over a temporary file and hands back the result.
    private String migrate(String source, String annotations) throws Exception {
        File f = File.createTempFile("MyApp", ".java");
        f.deleteOnExit();
        write(f, source);
        new MigrateBuildHintsMojo().insertAnnotations(f, annotations, "MyApp");
        return read(f);
    }
}
