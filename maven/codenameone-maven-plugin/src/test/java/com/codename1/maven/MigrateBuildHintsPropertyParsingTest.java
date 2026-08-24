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

    /// A default-package class that already carries an annotation: the import
    /// must go ABOVE it. Anchoring on the declaration put the import between the
    /// annotation and the class, which is not valid in either language.
    @Test
    public void theImportGoesAboveAnExistingAnnotation() {
        String head = "/* c */\n@SuppressWarnings(\"unchecked\")\n";
        assertEquals(head.indexOf("@SuppressWarnings"),
                MigrateBuildHintsMojo.startOfLeadingAnnotations(head));
    }

    /// A parenthesis inside an annotation argument must not stop the walk.
    @Test
    public void anArgumentContainingAParenthesisDoesNotStopTheWalk() {
        String head = "@Deprecated\n@SuppressWarnings(\"a(b\")\n";
        assertEquals(0, MigrateBuildHintsMojo.startOfLeadingAnnotations(head));
    }

    /// With no annotation there is nothing to move above.
    @Test
    public void withNoAnnotationTheAnchorIsTheEnd() {
        String head = "/* copyright */\n\n";
        assertEquals(head.length(), MigrateBuildHintsMojo.startOfLeadingAnnotations(head));
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
}
