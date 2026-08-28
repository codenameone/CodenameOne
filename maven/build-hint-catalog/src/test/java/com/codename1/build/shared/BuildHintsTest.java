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
package com.codename1.build.shared;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Self-consistency of the build hint catalog.
 *
 * <p>Everything downstream is generated from this table, so a defect here
 * becomes a broken annotation, a wrong manifest entry, or a hint that silently
 * does nothing. These checks are the reason the catalog is Java rather than a
 * data file.</p>
 */
class BuildHintsTest {

    /**
     * Annotation members cannot be named after a public method of Object or
     * Annotation: JLS 9.6.1 makes that a compile error, and it is not a keyword
     * rule so it is easy to miss until the generated source will not build.
     */
    private static final Set<String> ILLEGAL_MEMBER_NAMES = new HashSet<String>(Arrays.asList(
            "equals", "hashCode", "toString", "annotationType", "clone", "getClass",
            "notify", "notifyAll", "wait", "finalize"));

    private static final Set<String> JAVA_KEYWORDS = new HashSet<String>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new",
            "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while",
            "true", "false", "null", "_", "var", "record", "yield", "sealed", "permits"));

    private static final Set<String> LEGAL_SEPARATORS = new HashSet<String>(Arrays.asList(
            "", ";", ",", "\n", " "));

    /**
     * The separators {@code LibraryHintMerger} defines today. The catalog has to
     * agree with all of them before that map can be deleted in favour of
     * {@link BuildHints#separatorFor(String)} -- if the two disagree, a cn1lib's
     * contribution is spliced onto the project's value with the wrong delimiter
     * and the resulting Gradle or plist fragment is malformed.
     */
    private static Map<String, String> libraryHintMergerSeparators() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("android.gradleDep", ";");
        m.put("gradleDependencies", "\n");
        m.put("android.topDependency", "\n");
        m.put("android.repositories", "\n");
        m.put("android.xgradle", "\n");
        m.put("android.gradle.androidx", "\n");
        m.put("android.xgradle_default_config", "\n");
        m.put("android.gradlePlugin", "\n");
        m.put("android.supportv4Dep", "\n");
        m.put("android.proguardKeep", "\n");
        m.put("ios.pods", ",");
        m.put("ios.applicationQueriesSchemes", ",");
        m.put("ios.add_libs", ";");
        m.put("android.xapplication_attr", " ");
        return m;
    }

    /**
     * The hand written half. The annotated hints are rendered into a data file
     * during a build rather than committed, so on THIS module's classpath
     * {@code entries()} holds only what these sources declare. The invariants
     * that need the complete set live in build-hint-tools, which is the only
     * module that can render one for its own tests.
     */
    @Test
    void theCatalogIsNotEmpty() {
        assertTrue(BuildHints.entries().size() > 400,
                "expected the mined hint set, got " + BuildHints.entries().size());
    }

    @Test
    void namesAreUniqueAndLookupWorksWithOrWithoutThePrefix() {
        for (BuildHints.Hint h : BuildHints.entries()) {
            assertSame(h, BuildHints.byName(h.name()));
            assertSame(h, BuildHints.byName(BuildHints.ARG_PREFIX + h.name()));
        }
    }

    private static void assertSame(BuildHints.Hint expected, BuildHints.Hint actual) {
        if (expected != actual) {
            fail("lookup returned a different entry for " + expected.name());
        }
    }


    @Test
    void everyAnnotationAttributeIsClaimedExactlyOnce() {
        Map<String, String> claimed = new HashMap<String, String>();
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (!h.isAnnotated()) {
                continue;
            }
            String key = h.group().annotationSimpleName() + "#" + h.attr();
            String previous = claimed.put(key, h.name());
            assertTrue(previous == null,
                    "@" + key + " is claimed by both " + previous + " and " + h.name());
        }
    }

    @Test
    void annotationAttributeNamesAreLegalJavaMembers() {
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (!h.isAnnotated()) {
                continue;
            }
            String a = h.attr();
            assertTrue(a.length() > 0, h.name() + " has an empty attribute name");
            assertTrue(Character.isJavaIdentifierStart(a.charAt(0)),
                    h.name() + " -> '" + a + "' is not a legal identifier start");
            for (int i = 1; i < a.length(); i++) {
                assertTrue(Character.isJavaIdentifierPart(a.charAt(i)),
                        h.name() + " -> '" + a + "' has an illegal identifier character");
            }
            assertFalse(JAVA_KEYWORDS.contains(a),
                    h.name() + " -> '" + a + "' is a Java keyword");
            assertFalse(ILLEGAL_MEMBER_NAMES.contains(a),
                    h.name() + " -> '" + a + "' is override-equivalent to a method of "
                            + "Object or Annotation, which JLS 9.6.1 forbids as an "
                            + "annotation member name");
        }
    }

    @Test
    void anAnnotatedHintIsNeverDynamicAndNeverAnAlias() {
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (!h.isAnnotated()) {
                continue;
            }
            assertFalse(h.isDynamic(),
                    h.name() + " is a dynamic family; a Java annotation cannot express a map");
            assertTrue(h.aliasOf() == null,
                    h.name() + " is an alias, so annotating it would create two attributes "
                            + "for one effective setting");
        }
    }

    @Test
    void declaredDefaultsMatchTheirDeclaredType() {
        for (BuildHints.Hint h : BuildHints.entries()) {
            String d = h.def();
            if (d == null || d.length() == 0) {
                continue;
            }
            switch (h.type()) {
                case BOOLEAN:
                    assertTrue("true".equals(d) || "false".equals(d),
                            h.name() + " is BOOLEAN but defaults to '" + d + "'");
                    break;
                case INT:
                    try {
                        Integer.parseInt(d.trim());
                    } catch (NumberFormatException e) {
                        fail(h.name() + " is INT but defaults to '" + d + "'");
                    }
                    break;
                case ENUM:
                    assertTrue(h.values().contains(d),
                            h.name() + " defaults to '" + d + "', which is outside its domain "
                                    + h.values());
                    break;
                default:
                    break;
            }
        }
    }

    @Test
    void everyEnumHasAUsableDomain() {
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (h.type() != HintType.ENUM) {
                continue;
            }
            assertNotNull(h.enumName(), h.name() + " is ENUM with no enum type name");
            assertTrue(h.values().size() >= 2,
                    h.name() + " is ENUM with fewer than two values: " + h.values());
            for (String v : h.values()) {
                assertFalse(v.indexOf(',') >= 0,
                        h.name() + " value '" + v + "' contains a comma, which the simulator's "
                                + "Build Hint editor uses to delimit its value list");
            }
            if (!h.valueLabels().isEmpty()) {
                assertEquals(h.values().size(), h.valueLabels().size(),
                        h.name() + " has a label list of a different length to its values");
            }
        }
    }

    /**
     * One-way, deliberately. A list needs a delimiter, but a hint can carry a
     * delimiter without being a list the user edits as items --
     * {@code android.xapplication_attr} joins XML attributes with a space.
     */
    @Test
    void everyListHintHasANonEmptySeparator() {
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (h.type() == HintType.STRING_LIST) {
                assertNotNull(h.separator(), h.name() + " is a list with no separator");
                assertFalse(h.separator().isEmpty(),
                        h.name() + " is a list with an empty separator, so its values would "
                                + "run together");
            }
            if (h.separator() != null) {
                assertTrue(LEGAL_SEPARATORS.contains(h.separator()),
                        h.name() + " uses an unsupported separator " + quote(h.separator()));
            }
        }
    }


    @Test
    void anUnknownHintFallsBackToBareConcatenation() {
        assertEquals("", BuildHints.separatorFor("some.hint.nobody.catalogued"));
        assertEquals("", BuildHints.separatorFor(null));
    }

    @Test
    void everyDynamicFamilyDeclaresItsPattern() {
        int found = 0;
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (!h.isDynamic()) {
                continue;
            }
            found++;
            assertNotNull(h.pattern(), h.name() + " is dynamic with no pattern");
            assertTrue(h.pattern().indexOf('*') >= 0,
                    h.name() + " is dynamic but its pattern matches only itself");
        }
        assertTrue(found > 10, "expected the known dynamic families, found " + found);
    }

    @Test
    void derivedTypeVocabulariesCoverEveryHintType() {
        Set<String> widgets = new HashSet<String>(
                Arrays.asList("TextField", "TextArea", "Checkbox", "Select"));
        for (HintType t : HintType.values()) {
            assertNotNull(BuildHints.settingsType(t));
            assertTrue(widgets.contains(BuildHints.editorWidget(t)),
                    t + " maps to '" + BuildHints.editorWidget(t)
                            + "', which the Build Hint editor does not recognise and would "
                            + "silently render as a plain text field");
        }
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "'" + s.replace("\n", "\\n") + "'";
    }
}
