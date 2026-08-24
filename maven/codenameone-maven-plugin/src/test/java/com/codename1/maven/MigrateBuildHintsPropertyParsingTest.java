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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

/// Covers the properties parsing in `MigrateBuildHintsMojo`.
///
/// The migration deletes a hint's declaration and replaces it with an
/// annotation. A declaration the deletion pass fails to recognise is left
/// behind while the annotation is added, and the very next build fails with the
/// duplicate-hint error the goal exists to prevent -- so the parser has to
/// accept every form `java.util.Properties` does, not just `key=value`.
public class MigrateBuildHintsPropertyParsingTest {

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
        assertEquals("IosThemeMode.IOS7", mojo.toSourceLiteral(ios, "flat", false));
        assertEquals("IosThemeMode.MODERN", mojo.toSourceLiteral(ios, "liquid", false));
        assertEquals("IosThemeMode.MODERN", mojo.toSourceLiteral(ios, "modern", false));
        assertNull(mojo.toSourceLiteral(ios, "nonsense", false));
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
        assertEquals("true", mojo.toSourceLiteral(bool, "true", false));
        assertNull(mojo.toSourceLiteral(bool, "TRUE", false));
        assertNull(mojo.toSourceLiteral(bool, "True", false));
        assertNull(mojo.toSourceLiteral(bool, "true ", false));

        com.codename1.build.shared.BuildHints.Hint ios =
                com.codename1.build.shared.BuildHints.byName("ios.themeMode");
        assertEquals("IosThemeMode.MODERN", mojo.toSourceLiteral(ios, "modern", false));
        assertNull(mojo.toSourceLiteral(ios, "MODERN", false));
    }

    /// An int that does not round-trip would be rewritten too.
    @Test
    public void anIntThatDoesNotRoundTripIsRefused() {
        MigrateBuildHintsMojo mojo = new MigrateBuildHintsMojo();
        com.codename1.build.shared.BuildHints.Hint i =
                com.codename1.build.shared.BuildHints.byName("android.min_sdk_version");
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
}
