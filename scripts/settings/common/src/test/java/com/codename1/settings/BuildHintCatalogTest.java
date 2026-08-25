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

    /// The runtime accepts spellings the picklist does not offer:
    /// IOSImplementation.installNativeTheme compares against `flat` and `liquid`,
    /// AndroidImplementation against `material` and `holo`. Rejecting them told a
    /// developer a working configuration was invalid and refused the edit.
    @Test
    public void documentedThemeSpellingsAreAccepted() {
        com.codename1.build.shared.BuildHints.Hint ios =
                com.codename1.build.shared.BuildHints.byName("ios.themeMode");
        assertEquals("ios7", ios.canonicalValue("flat"));
        assertEquals("modern", ios.canonicalValue("liquid"));
        assertEquals("legacy", ios.canonicalValue("iphone"));
        assertEquals("modern", ios.canonicalValue("MODERN"));
        assertNull(ios.canonicalValue("nonsense"));

        com.codename1.build.shared.BuildHints.Hint and =
                com.codename1.build.shared.BuildHints.byName("and.themeMode");
        assertEquals("modern", and.canonicalValue("material"));
        assertEquals("hololight", and.canonicalValue("holo"));
        assertNull(and.canonicalValue("nonsense"));
    }

    /// An alias does not become a picklist choice or an enum constant: one
    /// behaviour, one constant, or the annotation asks a question with no right
    /// answer.
    @Test
    public void anAcceptedSpellingIsNotOfferedAsAChoice() {
        com.codename1.build.shared.BuildHints.Hint ios =
                com.codename1.build.shared.BuildHints.byName("ios.themeMode");
        assertFalse(ios.values().contains("flat"));
        assertFalse(ios.values().contains("liquid"));
    }

    /// `import ...Ios as BuildIos` puts BuildIos in scope, NOT Ios. Counting it
    /// as a simple-name import attributed another library's @Ios to us.
    @Test
    public void anAliasedImportDoesNotBringTheSimpleNameIntoScope() {
        String src = "import com.codename1.annotations.buildhints.Ios as BuildIos\n"
                + "import com.example.other.Ios\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        assertFalse(CodenameOneSettings.importsAnnotation(src, "Ios", true));

        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, true);
        assertNull(out.get("ios.teamId"));
    }

    /// ...while the alias itself still is.
    @Test
    public void theAliasMarkerStillCounts() {
        String src = "import com.codename1.annotations.buildhints.Ios as BuildIos\n"
                + "@BuildIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, true);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// Kotlin does not require a file to be named after the class it declares, so
    /// the declaration is what identifies the main class -- not the filename and
    /// not the directory.
    @Test
    public void aClassIsIdentifiedByItsDeclarationNotItsFile() {
        String kt = "package com.example\n\nclass Helper\n\nclass MyApp\n";
        assertTrue(CodenameOneSettings.declaresClass(kt, "MyApp", "com.example"));
        assertTrue(CodenameOneSettings.declaresClass(kt, "Helper", "com.example"));
        assertFalse(CodenameOneSettings.declaresClass(kt, "Other", "com.example"));
    }

    /// A same-named class in another package is a different class. Accepting it
    /// is how a moved class makes its own orphan look alive.
    @Test
    public void thePackageIsPartOfTheIdentity() {
        String src = "package com.example.moved;\npublic class MyApp {}\n";
        assertFalse(CodenameOneSettings.declaresClass(src, "MyApp", "com.example"));
        assertTrue(CodenameOneSettings.declaresClass(src, "MyApp", "com.example.moved"));
    }

    /// A Kotlin `object` declares a type too.
    @Test
    public void anObjectDeclarationCounts() {
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example\nobject MyApp\n", "MyApp", "com.example"));
    }

    /// The default package is "" on both sides rather than null on one.
    @Test
    public void theDefaultPackageMatches() {
        assertTrue(CodenameOneSettings.declaresClass("public class MyApp {}\n", "MyApp", null));
        assertTrue(CodenameOneSettings.declaresClass("public class MyApp {}\n", "MyApp", ""));
        assertFalse(CodenameOneSettings.declaresClass("public class MyApp {}\n", "MyApp", "com.x"));
    }

    /// A commented-out or quoted mention of a declaration is not a declaration.
    /// An unrelated file answering for the main class reads ownership as empty,
    /// and Settings then offers Add for a hint the real main class annotates.
    @Test
    public void aMentionOfADeclarationIsNotADeclaration() {
        assertFalse(CodenameOneSettings.declaresClass(
                "package com.example\n// class MyApp\n", "MyApp", "com.example", true));
        assertFalse(CodenameOneSettings.declaresClass(
                "package com.example\n/* class MyApp */\n", "MyApp", "com.example", true));
        assertFalse(CodenameOneSettings.declaresClass(
                "package com.example\nval s = \"class MyApp\"\n", "MyApp", "com.example", true));
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example\n// class MyApp\nclass MyApp\n", "MyApp",
                "com.example", true));
    }

    /// The same for a commented-out package statement, which would otherwise make
    /// a default-package file claim to be in one.
    @Test
    public void aCommentedPackageStatementIsNotThePackage() {
        assertTrue(CodenameOneSettings.declaresClass(
                "// package com.example\nclass MyApp\n", "MyApp", "", true));
        assertFalse(CodenameOneSettings.declaresClass(
                "// package com.example\nclass MyApp\n", "MyApp", "com.example", true));
    }

    /// `class\nMain` and `class /* why */ Main` are both legal. Stopping at a
    /// space read the declaration as unnamed, so the file did not declare the
    /// main class and ownership came back empty.
    @Test
    public void anyLegalSeparatorBeforeTheNameIsAccepted() {
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example\nclass\nMyApp\n", "MyApp", "com.example", true));
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example\nclass /* why */ MyApp\n", "MyApp", "com.example", true));
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example;\npublic class\t MyApp {}\n", "MyApp",
                "com.example", false));
    }

    /// An application's main class is top-level. Accepting a nested one let an
    /// unrelated `class Outer { class Main }` end the fallback search on the
    /// wrong file, so annotations on the real main class were never read.
    @Test
    public void aNestedDeclarationIsNotTheMainClass() {
        assertFalse(CodenameOneSettings.declaresClass(
                "package com.example\nclass Outer { class MyApp }\n", "MyApp",
                "com.example", true));
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example\nclass Outer { }\nclass MyApp\n", "MyApp",
                "com.example", true));
    }

    /// A brace inside a char literal is not syntax; counting it loses the depth
    /// and turns a top-level declaration into a nested one or the reverse.
    @Test
    public void aBraceInACharLiteralDoesNotMoveTheDepth() {
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example;\npublic class Helper { char c = '{'; }\n"
                        + "class MyApp {}\n", "MyApp", "com.example", false));
    }

    /// A single-type import shadows an on-demand one -- the language rule, not a
    /// preference. A file importing our package with a wildcard AND another
    /// library's Ios by name is using theirs.
    @Test
    public void anExplicitImportBeatsOurWildcard() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "import com.example.other.Ios;\n"
                + "@Ios(teamId = \"T\")\npublic class MyApp {}\n";
        assertFalse(CodenameOneSettings.importsAnnotation(src, "Ios", false));

        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, out, false);
        assertNull(out.get("ios.teamId"));
    }

    /// Our own explicit import is not "another library's".
    @Test
    public void ourOwnExplicitImportStillCounts() {
        String src = "import com.codename1.annotations.buildhints.*;\n"
                + "import com.codename1.annotations.buildhints.Ios;\n"
                + "@Ios(teamId = \"T\")\npublic class MyApp {}\n";
        assertTrue(CodenameOneSettings.importsAnnotation(src, "Ios", false));
    }

    /// Nor is a Kotlin alias, which introduces its alias rather than the name.
    @Test
    public void anAliasedForeignImportDoesNotShadow() {
        String src = "import com.codename1.annotations.buildhints.*\n"
                + "import com.example.other.Ios as TheirIos\n"
                + "@Ios(teamId = \"T\")\nclass MyApp\n";
        assertTrue(CodenameOneSettings.importsAnnotation(src, "Ios", true));
    }

    /// Only one spelling of a closed-domain value works everywhere:
    /// AndroidGradleBuilder copies android.installLocation straight into the
    /// case-sensitive android:installLocation manifest attribute. Accepting
    /// `INTERNALONLY` and storing it verbatim marked it valid and then failed the
    /// Android build.
    @Test
    public void aClosedDomainValueHasOneWorkingSpelling() {
        com.codename1.build.shared.BuildHints.Hint h =
                com.codename1.build.shared.BuildHints.byName("android.installLocation");
        assertEquals("internalOnly", h.canonicalValue("INTERNALONLY"));
        assertEquals("internalOnly", h.canonicalValue("internalonly"));
        assertEquals("internalOnly", h.canonicalValue("internalOnly"));
        assertNull(h.canonicalValue("nowhere"));
    }

    /// `import /* build hints */ com.codename1...Ios;` is legal. Backing up from
    /// the name over spaces only missed it, so the live @Ios was read as somebody
    /// else's and Add offered a hint that is already annotated.
    @Test
    public void anImportSeparatedByCommentsOrNewlinesIsRecognised() {
        assertTrue(CodenameOneSettings.importsAnnotation(
                "import /* build hints */ com.codename1.annotations.buildhints.Ios;\n",
                "Ios", false));
        assertTrue(CodenameOneSettings.importsAnnotation(
                "import\n    com.codename1.annotations.buildhints.Ios;\n", "Ios", false));
        assertEquals("BuildIos", CodenameOneSettings.kotlinImportAlias(
                "import  com.codename1.annotations.buildhints.Ios  as  BuildIos\n",
                "Ios", true));
    }

    /// ...and a mention that is not an import still does not count.
    @Test
    public void aNonImportMentionIsStillNotAnImport() {
        assertFalse(CodenameOneSettings.importsAnnotation(
                "val t = com.codename1.annotations.buildhints.Ios::class\n", "Ios", true));
        assertFalse(CodenameOneSettings.importsAnnotation(
                "// import com.codename1.annotations.buildhints.Ios;\n", "Ios", false));
    }

    /// `package /* generated */ com.example;` is legal. Taking the remainder of
    /// the text and trimming it started the name at the comment, so the real main
    /// source was rejected by both the conventional lookup and the fallback.
    @Test
    public void aCommentBetweenPackageAndItsNameIsSkipped() {
        assertTrue(CodenameOneSettings.declaresClass(
                "package /* generated */ com.example;\npublic class MyApp {}\n",
                "MyApp", "com.example", false));
        assertTrue(CodenameOneSettings.declaresClass(
                "package\n    com.example\nclass MyApp\n", "MyApp", "com.example", true));
    }

    /// Adding a hint must start from what the build already does. Seeding a
    /// type-wide placeholder wrote a value the project did not have:
    /// android.NotificationChannel.importance defaults to 2, and persisting 0
    /// silences the channel before the user has typed anything.
    @Test
    public void theCatalogCarriesTheBuildersOwnDefault() {
        BuildHintMetadata importance =
                BuildHintCatalog.load().get("android.NotificationChannel.importance");
        assertNotNull(importance);
        assertEquals("2", importance.defaultValue());

        BuildHintMetadata pods = BuildHintCatalog.load().get("ios.pods");
        assertNotNull(pods);
        assertTrue(pods.defaultValue() == null || pods.defaultValue().isEmpty(),
                "a hint with no builder default must not invent one");
    }

    /// `build` is an ordinary package name -- this repository has
    /// com.codename1.build.shared -- so refusing to descend into it meant a main
    /// class living there could not be read. An output directory is never nested
    /// under src/.
    @Test
    public void buildIsAPackageNameInsideASourceTree() {
        assertTrue(CodenameOneSettings.insideSourceTree("/p/common/src/main/kotlin/com/example"));
        assertTrue(CodenameOneSettings.insideSourceTree("/p/common/src"));
        assertFalse(CodenameOneSettings.insideSourceTree("/p/common"));
        assertFalse(CodenameOneSettings.insideSourceTree("/p/common/target/classes"));
    }

    /// facebook.appId has no default: both builders decide whether Facebook
    /// support is in the app by asking whether the hint is null, so seeding the
    /// literal from the call site enabled the integration against an unrelated
    /// shared app ID the moment Add was clicked.
    @Test
    public void facebookAppIdHasNoDefault() {
        BuildHintMetadata meta = BuildHintCatalog.load().get("facebook.appId");
        assertNotNull(meta);
        assertTrue(meta.defaultValue() == null || meta.defaultValue().isEmpty());
    }

    /// A qualified import may carry whitespace or a comment around any dot.
    /// Reading the name as one contiguous run stopped at the separator and
    /// recorded only the prefix, so the import went unrecognised and the live
    /// @Ios read as somebody else's.
    @Test
    public void aQualifiedImportMaySpanSeparators() {
        assertTrue(CodenameOneSettings.importsAnnotation(
                "import com.codename1.annotations. /* generated */ buildhints.Ios;\n",
                "Ios", false));
        assertTrue(CodenameOneSettings.importsAnnotation(
                "import com.codename1\n   .annotations\n   .buildhints\n   .*;\n",
                "Ios", false));
        assertEquals("BuildIos", CodenameOneSettings.kotlinImportAlias(
                "import com.codename1.annotations . buildhints . Ios as BuildIos\n",
                "Ios", true));
    }

    /// ...and a foreign import spanning separators still shadows ours.
    @Test
    public void aForeignImportSpanningSeparatorsStillShadows() {
        assertFalse(CodenameOneSettings.importsAnnotation(
                "import com.codename1.annotations.buildhints.*;\n"
                        + "import com.example . other . Ios;\n", "Ios", false));
    }

    /// For a hint whose value the build computes when the line is ABSENT, there
    /// is nothing safe to seed. android.targetSDKVersion has no catalog default,
    /// and writing 0 does not create an unset hint -- it overrides the
    /// computation, selecting the legacy android-14 target and emitting
    /// targetSdkVersion="0".
    @Test
    public void aHintWithNoDefaultHasNothingSafeToSeed() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        BuildHintMetadata target = catalog.get("android.targetSDKVersion");
        assertNotNull(target);
        assertTrue(target.defaultValue() == null || target.defaultValue().isEmpty(),
                "the catalog must not invent a default the builder computes");

        BuildHintMetadata facebook = catalog.get("facebook.appId");
        assertNotNull(facebook);
        assertTrue(facebook.defaultValue() == null || facebook.defaultValue().isEmpty(),
                "presence is the switch, so an empty seed would enable the feature");
    }

    /// ...while a hint the builder does have a default for is seeded with it.
    @Test
    public void aHintWithADefaultIsSeededWithIt() {
        assertEquals("2",
                BuildHintCatalog.load().get("android.NotificationChannel.importance")
                        .defaultValue());
    }

    /// The same separator rule as imports, applied to the package name: reading
    /// it as one contiguous run recorded `com` and rejected the real main source.
    @Test
    public void aPackageNameMaySpanSeparatorsInSettingsToo() {
        assertTrue(CodenameOneSettings.declaresClass(
                "package com /* generated */ . example;\npublic class MyApp {}\n",
                "MyApp", "com.example", false));
        assertTrue(CodenameOneSettings.declaresClass(
                "package com\n  . example\nclass MyApp\n", "MyApp", "com.example", true));
        assertFalse(CodenameOneSettings.declaresClass(
                "package com . other;\npublic class MyApp {}\n", "MyApp", "com.example", false));
    }

    /// A fully qualified annotation may carry separators between components, and
    /// matching a contiguous literal could not see it -- ownership read as empty
    /// and Add wrote the duplicate.
    @Test
    public void aQualifiedAnnotationMaySpanSeparators() {
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(
                "@com.codename1.annotations. /* generated */ buildhints.Ios(teamId = \"X\")\n"
                        + "public class MyApp {}\n", out, false);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));

        out.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(
                "@com.codename1.annotations\n    .buildhints\n    .Ios(teamId = \"X\")\n"
                        + "class MyApp\n", out, true);
        assertEquals("@Ios(teamId)", out.get("ios.teamId"));
    }

    /// Another library's qualified annotation of the same simple name is not ours
    /// however it is spaced.
    @Test
    public void aQualifiedForeignAnnotationIsStillNotOurs() {
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        CodenameOneSettings.collectAnnotationOwnedHints(
                "@com.example . other . Ios(teamId = \"X\")\npublic class MyApp {}\n",
                out, false);
        assertNull(out.get("ios.teamId"));
    }

    /// Kotlin block comments NEST. Stopping at the first `*/` ended the comment
    /// early, so a commented-out package declaration was read as live code.
    @Test
    public void aNestedKotlinBlockCommentStaysClosed() {
        String kt = "/* docs /* sample */ package old.name */\n"
                + "package com.example\nclass MyApp\n";
        assertTrue(CodenameOneSettings.declaresClass(kt, "MyApp", "com.example", true));
        // Java does not nest, so the same text really does end at the inner `*/`.
        assertFalse(CodenameOneSettings.declaresClass(kt, "MyApp", "com.example", false));
    }

    /// A Kotlin main class may escape its name in backticks, and
    /// `codename1.mainName` holds the name between them. Reading it with the
    /// identifier rule recorded an empty name, so the real main source was
    /// rejected, nothing knew which hints an annotation already owns, and
    /// Settings offered Add for one of them -- the duplicate declaration that
    /// fails the next build.
    @Test
    public void aKotlinEscapedMainNameIsRecognised() {
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example\nclass `when` {\n}\n", "when", "com.example"));
        assertFalse(CodenameOneSettings.declaresClass(
                "package com.example\nclass `when` {\n}\n", "Other", "com.example"));

        // A quote inside an escaped name is legal, and is not the start of a
        // literal: reading it as one blanked the declaration that followed.
        String quoted = "package com.example\nclass `say\"hi` { }\nclass MyApp { }\n";
        assertTrue(CodenameOneSettings.declaresClass(quoted, "MyApp", "com.example"));
    }

    /// A Kotlin package may escape a COMPONENT -- `package com.`when`` is legal
    /// and the class belongs to com.when. Reading only identifier characters
    /// recorded `com.`, so the real main source was rejected: nothing knew which
    /// hints an annotation already owns, and Settings could write the duplicate
    /// properties declaration that the next build rejects.
    @Test
    public void aKotlinPackageMayEscapeAComponent() {
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.`when`\nclass MyApp {\n}\n", "MyApp", "com.when"));
        assertFalse(CodenameOneSettings.declaresClass(
                "package com.`when`\nclass MyApp {\n}\n", "MyApp", "com"));
        // The first component too, and an ordinary name is unchanged.
        assertTrue(CodenameOneSettings.declaresClass(
                "package `in`.example\nclass MyApp {\n}\n", "MyApp", "in.example"));
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example\nclass MyApp {\n}\n", "MyApp", "com.example"));
    }

    /// An import may escape a component too -- `import
    /// com.codename1.annotations.`buildhints`.Ios` is legal Kotlin. Reading only
    /// identifier characters recorded `com.codename1.annotations.`, so the
    /// import went unrecognised, a live @Ios was read as somebody else's, and
    /// Settings could write the duplicate the next build refuses.
    @Test
    public void aKotlinImportMayEscapeAComponent() {
        assertTrue(CodenameOneSettings.importsAnnotation(
                "package com.example\n"
                        + "import com.codename1.annotations.`buildhints`.Ios\n"
                        + "class MyApp\n", "Ios", true));
        // The type name itself, and the on-demand form.
        assertTrue(CodenameOneSettings.importsAnnotation(
                "package com.example\n"
                        + "import com.codename1.annotations.buildhints.`Ios`\n"
                        + "class MyApp\n", "Ios", true));
        // An escaped ALIAS is the name the file then uses -- read through the
        // reader for aliases, which is what an aliased import belongs to.
        assertEquals("when", CodenameOneSettings.kotlinImportAlias(
                "package com.example\n"
                        + "import com.codename1.annotations.buildhints.Ios as `when`\n"
                        + "class MyApp\n", "Ios", true));
        // Somebody else's package is still somebody else's.
        assertFalse(CodenameOneSettings.importsAnnotation(
                "package com.example\n"
                        + "import com.other.`buildhints`.Ios\n"
                        + "class MyApp\n", "Ios", true));
    }

    /// Kotlin can rename a type in the FILE, with no import involved:
    /// `typealias AppIos = Ios` and then `@AppIos(...)`. The compiled
    /// annotation is still ours, so missing it left the hint editable and Add
    /// wrote the duplicate declaration the next build refuses.
    @Test
    public void aKotlinTypeAliasStillOwnsTheHint() {
        String src = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias AppIos = Ios\n"
                + "@AppIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // The fully qualified right-hand side needs no import.
        String qualified = "package com.example\n"
                + "typealias AppIos = com.codename1.annotations.buildhints.Ios\n"
                + "@AppIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(qualified, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // Somebody else's annotation renamed to the same alias is not ours.
        String theirs = "package com.example\n"
                + "typealias AppIos = com.other.Ios\n"
                + "@AppIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(theirs, owned, true);
        assertNull(owned.get("ios.teamId"));

        // Java has no typealias, so the same text owns nothing there.
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned, false);
        assertNull(owned.get("ios.teamId"));
    }

    /// A file may name the same annotation twice. Answering with the first
    /// alias left the one actually used unrecognised, so the hint read as
    /// unowned and Add wrote the duplicate the next build refuses.
    @Test
    public void everyKotlinAliasCounts() {
        String twoTypeAliases = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias First = Ios\n"
                + "typealias AppIos = Ios\n"
                + "@AppIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(twoTypeAliases, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        String twoImportAliases = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios as First\n"
                + "import com.codename1.annotations.buildhints.Ios as AppIos\n"
                + "@AppIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(twoImportAliases, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));
    }

    /// A `typealias` is a top-level declaration, not a file-scoped one, so it
    /// may be written in another file and used on the main class. Looking only
    /// at the main source read the hint as unowned, and Add wrote the duplicate
    /// the next build refuses.
    @Test
    public void aTypeAliasFromAnotherFileStillOwnsTheHint() {
        String main = "package com.example\n"
                + "@AppIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        String sibling = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias AppIos = Ios\n";

        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(main, owned, true,
                java.util.Collections.singletonList(sibling));
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // Without the sibling there is nothing to resolve the name to.
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(main, owned, true, null);
        assertNull(owned.get("ios.teamId"));

        // An IMPORT alias is file-scoped, so another file's does not apply.
        String importAliasElsewhere = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios as AppIos\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(main, owned, true,
                java.util.Collections.singletonList(importAliasElsewhere));
        assertNull(owned.get("ios.teamId"));
    }

    /// Both languages allow a non-ASCII identifier. Stopping at the first such
    /// character read a short name, so the real main source was rejected and
    /// Settings could offer a hint an annotation already owns.
    @Test
    public void aNonAsciiNameIsStillAName() {
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.\u5e94\u7528;\npublic class MyApp {}\n", "MyApp", "com.\u5e94\u7528"));
        assertFalse(CodenameOneSettings.declaresClass(
                "package com.\u5e94\u7528;\npublic class MyApp {}\n", "MyApp", "com"));
        // The class name too.
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example;\npublic class \u5e94\u7528 {}\n", "\u5e94\u7528",
                "com.example"));
        // An ASCII name is unchanged, and a separator still separates.
        assertTrue(CodenameOneSettings.declaresClass(
                "package com.example;\npublic class MyApp {}\n", "MyApp", "com.example"));
    }

    /// javac translates a unicode escape before it tokenizes anything, so
    /// `package com.ex` + an escaped `a` + `mple;` really declares com.example.
    /// Reading the text literally recorded `com.ex`, so the real main source was
    /// rejected and Settings could offer a hint an annotation already owns.
    @Test
    public void javaUnicodeEscapesAreTranslatedBeforeTheSourceIsRead() {
        String escaped = "package com.ex" + "\\u0061" + "mple;\npublic class MyApp {}\n";
        assertTrue(CodenameOneSettings.declaresClass(
                CodenameOneSettings.decodeUnicodeEscapes(escaped), "MyApp", "com.example"));

        // A doubled backslash is not an escape, which is what keeps a string
        // literal spelling one.
        String literal = "String s = \"" + "\\\\u0041" + "\";";
        assertEquals(literal, CodenameOneSettings.decodeUnicodeEscapes(literal));

        // Any number of u's is one escape, and a malformed one is left alone.
        assertEquals("A", CodenameOneSettings.decodeUnicodeEscapes("\\uuu0041"));
        assertEquals("\\uZZZZ", CodenameOneSettings.decodeUnicodeEscapes("\\uZZZZ"));
        assertEquals("\\n", CodenameOneSettings.decodeUnicodeEscapes("\\n"));
    }

    /// `typealias AppIos = Ios` then `typealias CustomIos = AppIos` is legal,
    /// and `@CustomIos(...)` still compiles to our annotation. Accepting only a
    /// right-hand side that names the annotation directly left the hint reading
    /// as unowned, so Add wrote the duplicate the next build refuses.
    @Test
    public void aChainOfTypeAliasesIsFollowed() {
        String main = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias AppIos = Ios\n"
                + "typealias CustomIos = AppIos\n"
                + "@CustomIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(main, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // The chain may cross files: the link naming our annotation in one, the
        // link the main class writes in another.
        String usesIt = "package com.example\n"
                + "@CustomIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        String declaresIt = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias AppIos = Ios\n"
                + "typealias CustomIos = AppIos\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(usesIt, owned, true,
                java.util.Collections.singletonList(declaresIt));
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // A chain that never reaches our annotation is not ours, and a cycle
        // must not hang the reader.
        String theirs = "package com.example\n"
                + "typealias AppIos = com.other.Ios\n"
                + "typealias CustomIos = AppIos\n"
                + "typealias A = B\n"
                + "typealias B = A\n"
                + "@CustomIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(theirs, owned, true);
        assertNull(owned.get("ios.teamId"));
    }

    /// Inside a Kotlin template expression the first quote starts a NEW literal
    /// rather than closing the outer one, so `"${"@Ios(teamId = x)"}"` ended the
    /// string early and exposed its contents as live code -- an annotation
    /// nobody wrote, which hid the editor for a hint nothing owns.
    @Test
    public void aStringInsideATemplateIsStillAString() {
        String src = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "class MyApp {\n"
                + "    val note = \"${\"@Ios(teamId = fake)\"}\"\n"
                + "}\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned, true);
        assertNull(owned.get("ios.teamId"));

        // A real annotation in the same file is still found.
        String real = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "class MyApp {\n"
                + "    val note = \"${\"@Ios(pods = fake)\"}\"\n"
                + "}\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(real, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));
        assertNull(owned.get("ios.pods"));

        // The expression is ordinary code, so it holds ordinary comments and
        // char literals, and a quote inside one of those is not a nested string.
        String commented = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "class Helper {\n"
                + "    val note = \"${ /* \\\" */ 1 }\"\n"
                + "}\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(commented, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // A brace inside the nested literal must not close the expression early.
        String braced = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "class MyApp {\n"
                + "    val note = \"${\"} @Ios(teamId = fake)\"}\"\n"
                + "}\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(braced, owned, true);
        assertNull(owned.get("ios.teamId"));
    }

    /// `import ...Ios as Base` then `typealias AppIos = Base` is legal, and the
    /// compiled annotation is still ours. Collecting the two kinds of alias into
    /// one list left the typealias unresolved, because its right-hand side names
    /// the IMPORT alias rather than the annotation.
    @Test
    public void aTypeAliasOfAnImportAliasIsFollowed() {
        String src = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios as Base\n"
                + "typealias AppIos = Base\n"
                + "@AppIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // An import alias applies only to the file that writes it, so a
        // typealias in ANOTHER file naming the same word is not this one.
        String usesIt = "package com.example\n"
                + "typealias AppIos = Base\n"
                + "@AppIos(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        String importsIt = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios as Base\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(usesIt, owned, true,
                java.util.Collections.singletonList(importsIt));
        assertNull(owned.get("ios.teamId"));
    }

    /// An escaped identifier inside a template expression is a NAME: a quote in
    /// it does not open a string and a brace does not close the expression.
    @Test
    public void anEscapedIdentifierInsideATemplateIsNotAString() {
        String src = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "class Helper {\n"
                + "    val note = \"${ `\\\"` }\"\n"
                + "}\n"
                + "@Ios(teamId = \"ABCDE12345\")\n"
                + "class MyApp\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(src, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));
    }

    /// A `typealias` is top-level but not global, and visibility is per SYMBOL
    /// rather than per package: `import com.other.Unrelated` exposes nothing
    /// else from `com.other`, and `import com.other.AppIos as Custom` exposes
    /// that one under `Custom`. A package-level answer was wrong both ways --
    /// it let an unrelated import expose an alias, hiding the editor for a hint
    /// nothing owns, and it lost the renamed name so a real annotation went
    /// unrecognised and Add wrote the duplicate.
    @Test
    public void aliasVisibilityIsPerSymbol() {
        String elsewhere = "package com.other\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias AppIos = Ios\n";
        java.util.List<String> others = java.util.Collections.singletonList(elsewhere);

        // Not imported at all: invisible.
        String plain = "package com.example\n@AppIos(teamId = \"X\")\nclass MyApp\n";
        assertTrue(CodenameOneSettings.kotlinTypeAliases(
                CodenameOneSettings.visibleTypeAliases(plain, others), "Ios", true).isEmpty());

        // Another symbol from the same package: still invisible.
        String unrelated = "package com.example\n"
                + "import com.other.Unrelated\n"
                + "@AppIos(teamId = \"X\")\nclass MyApp\n";
        assertTrue(CodenameOneSettings.kotlinTypeAliases(
                CodenameOneSettings.visibleTypeAliases(unrelated, others), "Ios", true).isEmpty());

        // Named: visible under its own name.
        String named = "package com.example\n"
                + "import com.other.AppIos\n"
                + "@AppIos(teamId = \"X\")\nclass MyApp\n";
        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(named, others), "Ios", true));

        // Renamed: visible under the NEW name, and not under the old one.
        String renamed = "package com.example\n"
                + "import com.other.AppIos as Custom\n"
                + "@Custom(teamId = \"X\")\nclass MyApp\n";
        assertEquals(java.util.Collections.singletonList("Custom"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(renamed, others), "Ios", true));

        // On demand: visible under its own name.
        String wildcard = "package com.example\n"
                + "import com.other.*\n"
                + "@AppIos(teamId = \"X\")\nclass MyApp\n";
        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(wildcard, others), "Ios", true));

        // Same package needs no import.
        String samePackage = "package com.other\n@AppIos(teamId = \"X\")\nclass MyApp\n";
        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(samePackage, others), "Ios", true));
    }

    /// A chain resolves in the package it is written in, and only its visible
    /// end reaches the main file -- under whatever name the import gives it.
    @Test
    public void aChainResolvesInItsOwnScope() {
        String elsewhere = "package com.other\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias Base = Ios\n"
                + "typealias AppIos = Base\n";
        String main = "package com.example\n"
                + "import com.other.AppIos as Custom\n"
                + "@Custom(teamId = \"ABCDE12345\")\nclass MyApp\n";

        // Custom resolves; Base, which the main file cannot name, does not leak.
        assertEquals(java.util.Collections.singletonList("Custom"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(main,
                                java.util.Collections.singletonList(elsewhere)),
                        "Ios", true));

        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(main, owned, true,
                java.util.Collections.singletonList(elsewhere));
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));
    }

    /// A chain link may cross a package boundary: `a` declares
    /// `typealias Base = Ios`, `b` imports `a.Base` and declares
    /// `typealias AppIos = Base`. Looking only in the declaring file's own
    /// package stopped the chain there, so the hint read as unowned and Add
    /// wrote the duplicate the next build refuses.
    @Test
    public void aChainLinkMayBeImportedFromAnotherPackage() {
        String a = "package a\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias Base = Ios\n";
        String b = "package b\n"
                + "import a.Base\n"
                + "typealias AppIos = Base\n";
        String main = "package com.example\n"
                + "import b.AppIos\n"
                + "@AppIos(teamId = \"ABCDE12345\")\nclass MyApp\n";
        java.util.List<String> others = java.util.Arrays.asList(a, b);

        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(main, others), "Ios", true));

        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(main, owned, true, others);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // The renamed form of the link, and the qualified spelling.
        String renamedLink = "package b\n"
                + "import a.Base as Root\n"
                + "typealias AppIos = Root\n";
        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(main,
                                java.util.Arrays.asList(a, renamedLink)), "Ios", true));

        String qualifiedLink = "package b\ntypealias AppIos = a.Base\n";
        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(main,
                                java.util.Arrays.asList(a, qualifiedLink)), "Ios", true));

        // A link that names nothing reachable is still not ours.
        String broken = "package b\ntypealias AppIos = Base\n";
        assertTrue(CodenameOneSettings.kotlinTypeAliases(
                CodenameOneSettings.visibleTypeAliases(main,
                        java.util.Arrays.asList(a, broken)), "Ios", true).isEmpty());
    }

    /// On a top-level Kotlin declaration `private` means this FILE only, not
    /// this package. Exposing another file's private alias let it vouch for an
    /// unrelated annotation of the same name, hiding the editor for a hint
    /// nothing owns.
    @Test
    public void aPrivateAliasBelongsToItsFile() {
        String sibling = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "private typealias AppIos = Ios\n";
        String main = "package com.example\n@AppIos(teamId = \"X\")\nclass MyApp\n";
        java.util.List<String> others = java.util.Collections.singletonList(sibling);

        assertTrue(CodenameOneSettings.kotlinTypeAliases(
                CodenameOneSettings.visibleTypeAliases(main, others), "Ios", true).isEmpty());

        // Without the modifier the same declaration is visible in the package.
        String shared = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "typealias AppIos = Ios\n";
        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(main,
                                java.util.Collections.singletonList(shared)), "Ios", true));

        // A private alias in the MAIN file is the file it belongs to.
        String privateHere = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "private typealias AppIos = Ios\n"
                + "@AppIos(teamId = \"ABCDE12345\")\nclass MyApp\n";
        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(privateHere, owned, true);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // A comment between the modifier and the keyword is legal, and the
        // backward walk skips only whitespace -- so it is read over blanked
        // code, where the comment is spaces.
        String commented = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "private /* note */ typealias AppIos = Ios\n";
        assertTrue(CodenameOneSettings.kotlinTypeAliases(
                CodenameOneSettings.visibleTypeAliases(main,
                        java.util.Collections.singletonList(commented)), "Ios", true).isEmpty());

        // `internal` is module-wide, so it is not this file's alone.
        String internal = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "internal typealias AppIos = Ios\n";
        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(main,
                                java.util.Collections.singletonList(internal)), "Ios", true));

        // A `private` belonging to whatever came before is not this one's.
        String before = "package com.example\n"
                + "import com.codename1.annotations.buildhints.Ios\n"
                + "private val x = 1\n"
                + "typealias AppIos = Ios\n";
        assertEquals(java.util.Collections.singletonList("AppIos"),
                CodenameOneSettings.kotlinTypeAliases(
                        CodenameOneSettings.visibleTypeAliases(main,
                                java.util.Collections.singletonList(before)), "Ios", true));
    }

    /// A same-package type beats an ON-DEMAND import in both languages, so a
    /// project with its own `Ios` and a wildcard import of ours writes its own.
    /// Reading that as ours hid the editor for a hint the processor never emits.
    @Test
    public void aSamePackageTypeBeatsAWildcardImport() {
        String ownAnnotation = "package com.example;\n"
                + "import com.codename1.annotations.buildhints.*;\n"
                + "@interface Ios { String teamId(); }\n";
        String main = "package com.example;\n"
                + "import com.codename1.annotations.buildhints.*;\n"
                + "@Ios(teamId = \"X\")\n"
                + "public class MyApp {}\n";

        java.util.Map<String, String> owned = new java.util.HashMap<>();
        CodenameOneSettings.collectAnnotationOwnedHints(main, owned, false,
                java.util.Collections.singletonList(ownAnnotation));
        assertNull(owned.get("ios.teamId"));

        // Declared in the main file itself, which is the same rule one step in.
        String declaresItHere = "package com.example;\n"
                + "import com.codename1.annotations.buildhints.*;\n"
                + "@interface Ios { String teamId(); }\n"
                + "@Ios(teamId = \"X\")\n"
                + "class MyApp {}\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(declaresItHere, owned, false);
        assertNull(owned.get("ios.teamId"));

        // With no such type the wildcard import is ours, as before.
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(main, owned, false);
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // A NAMED import is the more specific statement and still wins.
        String named = "package com.example;\n"
                + "import com.codename1.annotations.buildhints.Ios;\n"
                + "@Ios(teamId = \"X\")\n"
                + "public class MyApp {}\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(named, owned, false,
                java.util.Collections.singletonList(ownAnnotation));
        assertEquals("@Ios(teamId)", owned.get("ios.teamId"));

        // Kotlin declares an annotation with `annotation class`.
        String kotlinOwn = "package com.example\n"
                + "annotation class Ios(val teamId: String)\n";
        String kotlinMain = "package com.example\n"
                + "import com.codename1.annotations.buildhints.*\n"
                + "@Ios(teamId = \"X\")\n"
                + "class MyApp\n";
        owned.clear();
        CodenameOneSettings.collectAnnotationOwnedHints(kotlinMain, owned, true,
                java.util.Collections.singletonList(kotlinOwn));
        assertNull(owned.get("ios.teamId"));
    }
}
