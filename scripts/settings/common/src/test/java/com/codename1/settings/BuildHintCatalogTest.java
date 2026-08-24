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
package com.codename1.settings;

import com.codename1.settings.hints.BuildHintCatalog;
import com.codename1.settings.hints.BuildHintMetadata;
import com.codename1.settings.hints.BuildHintType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hint catalog the Settings tool offers for editing.
 *
 * <p>It used to be scraped out of the developer guide's AsciiDoc table at runtime,
 * with each hint's type guessed by string-matching its description prose. It now
 * comes from {@code com.codename1.build.shared.BuildHints}, the same table the
 * build hint annotations are generated from and the same one the drift gate holds
 * the builders against.</p>
 */
public class BuildHintCatalogTest {

    @Test
    public void carriesTheHintsTheDeveloperGuideDocuments() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertNotNull(catalog.get("android.debug"));
        assertNotNull(catalog.get("ios.plistInject"));
        assertNotNull(catalog.get("windows.signing.timestampUrl"));
        assertTrue(catalog.all().size() > 400,
                "expected the full catalog, got " + catalog.all().size());
    }

    @Test
    public void knownHintsCarryTheRightType() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertEquals(BuildHintType.BOOLEAN, catalog.get("android.debug").type());
        assertEquals(BuildHintType.XML, catalog.get("ios.plistInject").type());
        assertEquals(BuildHintType.INTEGER, catalog.get("java.version").type());
        assertEquals(BuildHintType.INTEGER, catalog.get("android.min_sdk_version").type());
        assertEquals(BuildHintType.BOOLEAN, catalog.get("android.useAndroidX").type());
        assertEquals(BuildHintType.CSV, catalog.get("ios.pods").type());
    }

    /**
     * The tool used to accept any string for every hint but an integer, a version
     * or a URL. A hint with a closed domain is the one case where a wrong value is
     * certainly wrong, because the builder compares against those strings and
     * silently falls back to its default when it matches none of them.
     */
    @Test
    public void hintsWithAClosedDomainExposeIt() {
        BuildHintMetadata titleBar = BuildHintCatalog.load().get("desktop.titleBar");
        assertNotNull(titleBar);
        assertEquals(BuildHintType.ENUM, titleBar.type());
        assertTrue(titleBar.values().contains("native"));
        assertTrue(titleBar.values().contains("custom"));
        assertTrue(titleBar.values().contains("toolbar"));
        assertFalse(titleBar.values().contains("natvie"));
    }

    /** A hint with a checked form should say so, so the UI can point at it. */
    @Test
    public void annotatedHintsNameTheirAnnotation() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertEquals("@Ios(pods)", catalog.get("ios.pods").annotation());
        assertEquals("@Desktop(titleBar)", catalog.get("desktop.titleBar").annotation());
        // Not every hint has one; the properties file remains the way to set those.
        assertEquals(null, catalog.get("android.xmanifest").annotation());
    }

    /**
     * Dynamic families such as {@code android.permission.<NAME>} are patterns, not
     * keys, so there is nothing for the editor to set.
     */
    @Test
    public void dynamicFamiliesAreNotOffered() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        for (BuildHintMetadata h : catalog.all()) {
            assertFalse(h.name().contains("*"),
                    h.name() + " is a pattern, not a hint the editor can set");
        }
    }

    /**
     * The generated project ships hints like ios.themeMode as annotations, not
     * properties lines. The catalog has to say which hints have an annotation
     * form so the Build Hints UI can refuse to write a second declaration --
     * doing so would fail the very next build with a duplicate-hint error.
     */
    @Test
    public void everyAnnotatedHintNamesItsAttribute() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        int annotated = 0;
        for (BuildHintMetadata h : catalog.all()) {
            if (h.annotation() == null) {
                continue;
            }
            annotated++;
            assertTrue(h.annotation().startsWith("@"), h.name() + " -> " + h.annotation());
            assertTrue(h.annotation().endsWith(")"), h.name() + " -> " + h.annotation());
        }
        assertTrue(annotated > 50, "expected the curated set, got " + annotated);
    }

    /**
     * The Settings field for a credential is masked from its type. The catalog
     * that replaced the old name-matching scraper has to keep classifying these
     * as SECRET, or a stored certificate password renders as visible text.
     */
    @Test
    public void credentialHintsStayMasked() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        for (BuildHintMetadata h : catalog.all()) {
            String n = h.name().toLowerCase();
            if (n.contains("password") || n.contains("secret") || n.contains("token")) {
                assertEquals(BuildHintType.SECRET, h.type(),
                        h.name() + " holds a credential and must render masked");
            }
        }
    }

    /**
     * A deprecated alias configures the same effective setting as its target, so
     * the Build Hints UI has to treat it as annotation-owned too -- otherwise its
     * row still offers Add and creates the duplicate the next build refuses.
     */
    @Test
    public void aliasesResolveToTheirCanonicalName() {
        assertEquals("and.themeMode",
                com.codename1.build.shared.BuildHints.canonicalName("cn1.androidTheme"));
        assertEquals("nativeTheme",
                com.codename1.build.shared.BuildHints.canonicalName("cn1.nativeTheme"));
        assertEquals("ios.pods",
                com.codename1.build.shared.BuildHints.canonicalName("ios.pods"));
    }

    /**
     * Right after cn1:migrate-build-hints the source declares the annotations and
     * no build has emitted the manifest yet. Treating them as unowned there would
     * offer Add for a hint the annotations already set, and the next build would
     * fail on the duplicate declaration -- so the source is read directly.
     */
    @Test
    public void annotationsAreFoundInSourceBeforeTheProjectIsBuilt() {
        String src = "package com.example;\n"
                + "import com.codename1.annotations.buildhints.*;\n"
                + "@Ios(pods = {\"A\", \"B\"}, teamId = \"T\")\n"
                + "@Desktop(titleBar = DesktopTitleBar.NATIVE)\n"
                + "public class MyApp extends Lifecycle {\n}\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned);
        assertEquals("@Ios(pods)", owned.get("ios.pods"));
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));
        assertEquals("@Desktop(titleBar)", owned.get("desktop.titleBar"));
        assertTrue(owned.get("ios.objC") == null, "an attribute nobody set is not owned");
    }

    /**
     * Attribute detection must not be fooled by a value that contains an equals
     * sign, a comma or a bracket -- android.xpermissions is XML, and gradleDep
     * entries carry both.
     */
    @Test
    public void valuesContainingSeparatorsDoNotCreatePhantomOwnership() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "@Android(xpermissions = \"<uses-permission android:name=\\\"X\\\"/>\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned);
        assertEquals("@Android(xpermissions)", owned.get("android.xpermissions"));
        assertTrue(owned.get("android.gradleDep") == null,
                "nothing inside a string value may register as an attribute");
        assertTrue(owned.get("android.debug") == null);
    }

    /**
     * A comment inside an annotation can carry an unmatched delimiter. Counting
     * it as syntax loses the annotation's boundary, and the hint it owns stays
     * editable -- so Add writes the duplicate the next build refuses.
     */
    @Test
    public void commentsInsideAnAnnotationDoNotBreakOwnership() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "@Ios(/* required for issue ( */ teamId = \"T\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));
    }

    @Test
    public void lineCommentsAndCharLiteralsDoNotBreakOwnership() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "@Ios(\n"
                + "    // a stray ) in a line comment\n"
                + "    teamId = \"T\",\n"
                + "    urlScheme = \"x\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));
        assertEquals("@Ios(urlScheme)", owned.get("ios.urlScheme"));

        String withChar = "import com.codename1.annotations.buildhints.*;\n"
                + "@Android(xpermissions = \"a\") // ')'\npublic class MyApp {}\n";
        java.util.Map<String, String> owned2 = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(withChar, owned2);
        assertEquals("@Android(xpermissions)", owned2.get("android.xpermissions"));
    }

    /**
     * The fully qualified spelling needs no import and is equally valid. Missing
     * it left the hint editable, and Add then wrote the duplicate declaration.
     */
    @Test
    public void fullyQualifiedAnnotationsAreRecognized() {
        String src = "@com.codename1.annotations.buildhints.Ios(teamId = \"T\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));
    }

    /** `@Ios` must not match `@IosPrivacy`, which is a different annotation. */
    @Test
    public void aSimpleNameDoesNotMatchALongerAnnotation() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "@IosPrivacy(cameraUsageDescription = \"why\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned);
        assertEquals("@IosPrivacy(cameraUsageDescription)",
                owned.get("ios.NSCameraUsageDescription"));
        assertTrue(owned.get("ios.teamId") == null,
                "@IosPrivacy must not be read as @Ios");
    }

    @Test
    public void searchStillMatchesOnNameAndDescription() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertFalse(catalog.search("pods").isEmpty());
        assertFalse(catalog.search("android").isEmpty());
    }

    /// Kotlin lets a file rename what it imports, and then the annotation's own
    /// name appears nowhere in the source. Reading that as unowned put the hint
    /// back on the Add list, and Add writes the properties line that makes the
    /// next process-annotations fail on a duplicate the tool itself created.
    @Test
    public void aKotlinAliasedImportIsRecognized() {
        String src = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios as BuildIos\n"
                + "@BuildIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, true);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// The alias only counts when it really is one: the import must say `as`.
    @Test
    public void aPlainImportIsNotReadAsAnAlias() {
        String src = "import com.codename1.annotations.buildhints.Ios\n"
                + "@Ios(teamId = \"ABCDE12345\")\n";
        assertNull(CodenameOneSettings.kotlinImportAlias(src, "Ios", true));
    }

    /// A commented-out annotation is not an annotation. Reading it as one made
    /// Settings withhold Add and the editor for a hint the processor never
    /// emits, which looks like the tool being broken.
    @Test
    public void aCommentedOutAnnotationIsNotOwnership() {
        String src = "package com.example;\n"
                + "import com.codename1.annotations.buildhints.Ios;\n"
                + "// @Ios(teamId = \"OLD\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out);
        assertNull(out.get("ios.teamId"));
    }

    /// Same for a block comment and for an annotation quoted inside a string.
    @Test
    public void aBlockCommentOrStringIsNotOwnership() {
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(
                "/* @Ios(teamId = \"OLD\") */ public class MyApp {}", out);
        assertNull(out.get("ios.teamId"));

        out.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(
                "String doc = \"@Ios(teamId = x)\";", out);
        assertNull(out.get("ios.teamId"));
    }

    /// And the real one is still found when a commented-out copy precedes it.
    @Test
    public void aLiveAnnotationAfterACommentedOneIsStillFound() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "// @Ios(teamId = \"OLD\")\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// A commented-out earlier alias must not win over the live import. It did,
    /// and then the live `@BuildIos` was never looked for at all -- the very bug
    /// the alias support exists to prevent.
    @Test
    public void aCommentedOutAliasDoesNotShadowTheLiveOne() {
        String src = "// import com.codename1.annotations.buildhints.Ios as Old\n"
                + "import com.codename1.annotations.buildhints.Ios as BuildIos\n"
                + "@BuildIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        assertEquals("BuildIos", CodenameOneSettings.kotlinImportAlias(src, "Ios", true));

        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, true);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// The package named somewhere that is not an import is not an alias.
    @Test
    public void aMentionThatIsNotAnImportIsNotAnAlias() {
        assertNull(CodenameOneSettings.kotlinImportAlias(
                "val doc = com.codename1.annotations.buildhints.Ios as Whatever", "Ios", true));
    }

    /// Parentheses are optional on an annotation, so searching forward for the
    /// next `(` adopted whatever call came after it. Settings then withheld the
    /// Add and editor controls for a hint the processor never emits.
    @Test
    public void aBareAnnotationDoesNotAdoptTheNextCall() {
        String src = "@Ios\n"
                + "class MyApp {\n"
                + "    fun setUp() { configure(teamId = \"ABCDE12345\") }\n"
                + "}\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out);
        assertNull(out.get("ios.teamId"));
    }

    /// A comment between the name and its own argument list is still its own.
    @Test
    public void anAnnotationsOwnArgumentListIsStillFoundAcrossAComment() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "@Ios /* why */ (teamId = \"ABCDE12345\")\nclass MyApp\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// The two capture-record spellings are one setting: the builder reads the
    /// long name and then lets the short one override it. Without the alias an
    /// annotation and a properties line are not seen as a conflict, and the
    /// properties line silently wins over the compile-checked annotation.
    @Test
    public void theShortCaptureRecordSpellingIsAnAliasOfTheLongOne() {
        assertEquals("android.captureRecord",
                com.codename1.build.shared.BuildHints.canonicalName("and.captureRecord"));
        assertEquals("android.facebook_permissions",
                com.codename1.build.shared.BuildHints.canonicalName("and.facebook_permissions"));
    }

    /// A Kotlin raw string containing a quote was read as an empty literal
    /// followed by a new one, and that new one then swallowed the annotation
    /// after it -- so the hint read as unowned and Add wrote the duplicate.
    @Test
    public void aTripleQuotedStringDoesNotSwallowTheAnnotation() {
        String src = "import com.codename1.annotations.buildhints.*\n"
                + "val doc = \"\"\"quoted \" text\"\"\"\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, true);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// And an annotation written INSIDE a raw string is still not ownership.
    @Test
    public void anAnnotationInsideATripleQuotedStringIsNotOwnership() {
        String src = "val doc = \"\"\"@Ios(teamId = \"x\")\"\"\"\nclass MyApp\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, true);
        assertNull(out.get("ios.teamId"));
    }

    /// Java text blocks DO process escapes, so `\\"""` is an escaped quote and
    /// two more -- not the closing delimiter. Reading it as one made the scanner
    /// treat the REAL delimiter as a new text block and run past the annotation
    /// after it.
    @Test
    public void anEscapedQuoteRunDoesNotCloseAJavaTextBlock() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "String doc = \"\"\"\n"
                + "  a \\\"\"\" b\n"
                + "  \"\"\";\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, false);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// Kotlin does NOT process escapes in a raw string, and a run of four quotes
    /// closes at the last three -- the extra one belongs to the value. Applying
    /// Java's rule here would keep scanning and swallow the annotation.
    @Test
    public void aQuoteRunClosesAKotlinRawStringAtItsLastThree() {
        String src = "import com.codename1.annotations.buildhints.*\n"
                + "val doc = \"\"\"a\"\"\"\"\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, true);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// `@Build` and `@Android` are ordinary enough names that another library's
    /// annotation with a matching attribute would be read as ownership -- and
    /// Settings would then hide the editor for a hint the processor never emits.
    /// The simple name only counts when an import makes it ours.
    @Test
    public void anUnrelatedAnnotationOfTheSameNameIsNotOwnership() {
        String src = "import com.example.other.Ios;\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "public class MyApp {}\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out);
        assertNull(out.get("ios.teamId"));
    }

    /// A wildcard import counts, and so does the fully qualified spelling, which
    /// needs no import at all.
    @Test
    public void aWildcardImportAndTheQualifiedSpellingBothCount() {
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(
                "import com.codename1.annotations.buildhints.*;\n"
                        + "@Ios(teamId = \"T\")\npublic class MyApp {}\n", out);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));

        out.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(
                "@com.codename1.annotations.buildhints.Ios(teamId = \"T\")\n"
                        + "public class MyApp {}\n", out);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// A commented-out import does not bring the name in.
    @Test
    public void aCommentedOutImportDoesNotCount() {
        assertFalse(CodenameOneSettings.importsAnnotation(
                "// import com.codename1.annotations.buildhints.Ios;\n", "Ios", false));
        assertTrue(CodenameOneSettings.importsAnnotation(
                "import com.codename1.annotations.buildhints.Ios;\n", "Ios", false));
    }
}
