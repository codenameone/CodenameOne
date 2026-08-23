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
}
