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

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The annotations say everything the catalog used to say about their hints.
 *
 * <p>This is what lets those entries leave the catalog. It compiles the real
 * annotation sources, reads them back through {@link BuildHintAnnotationReader},
 * and compares the result field by field against the catalog's own annotated
 * entries. Anything the annotation cannot carry shows up here as a difference
 * rather than as a silently thinner developer guide.</p>
 */
public class BuildHintAnnotationReaderTest {

    @Test
    public void theAnnotationsCarryEverythingTheCatalogDoes() throws Exception {
        // Through readFromSources, which is what the generator calls: the
        // prose lives in the attribute's `///` documentation so an IDE shows it,
        // and that is not in the class file at all. Reading the bytecode alone
        // would test a path nothing uses and report every doc as missing.
        Map<String, BuildHints.Hint> fromAnnotations = byName(
                BuildHintAnnotationReader.readFromSources(annotationSources()));

        List<BuildHints.Hint> annotatedInCatalog = new ArrayList<BuildHints.Hint>();
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (h.isAnnotated()) {
                annotatedInCatalog.add(h);
            }
        }
        assertTrue(annotatedInCatalog.size() > 50,
                "the catalog should still describe the annotated hints at this point");

        List<String> differences = new ArrayList<String>();
        for (BuildHints.Hint expected : annotatedInCatalog) {
            BuildHints.Hint actual = fromAnnotations.remove(expected.name());
            if (actual == null) {
                differences.add(expected.name() + ": no annotation attribute writes it");
                continue;
            }
            compare(differences, expected, actual);
        }
        for (String leftOver : fromAnnotations.keySet()) {
            differences.add(leftOver + ": written by an annotation the catalog does not describe");
        }
        if (!differences.isEmpty()) {
            fail(differences.size() + " difference(s) between the annotations and the catalog:\n  "
                    + String.join("\n  ", differences));
        }
    }

    private static void compare(List<String> out, BuildHints.Hint expected,
                                BuildHints.Hint actual) {
        String at = expected.name() + ": ";
        check(out, at + "type", expected.type(), actual.type());
        check(out, at + "attribute", expected.attr(), actual.attr());
        check(out, at + "group", expected.group(), actual.group());
        check(out, at + "default", expected.def(), actual.def());
        check(out, at + "separator", expected.separator(), actual.separator());
        check(out, at + "platform", expected.platform(), actual.platform());
        check(out, at + "values", expected.values(), actual.values());
        check(out, at + "valueAliases", expected.valueAliases(), actual.valueAliases());
        check(out, at + "valueLabels", expected.valueLabels(), actual.valueLabels());
        check(out, at + "enum", expected.enumName(), actual.enumName());
        check(out, at + "aliasOf", expected.aliasOf(), actual.aliasOf());
        check(out, at + "deprecated", expected.deprecated(), actual.deprecated());
        check(out, at + "external", expected.isExternal(), actual.isExternal());
        check(out, at + "enterpriseOnly", expected.isEnterpriseOnly(), actual.isEnterpriseOnly());
        check(out, at + "link", expected.link(), actual.link());
        // The annotation may say MORE than the catalog did, and for the iOS
        // privacy keys it does: the catalog left their doc empty and the
        // generator synthesised a sentence per key on its way out. Saying less
        // is the failure -- that is a thinner developer guide -- so only the
        // empty-to-something direction is allowed.
        if (isEmpty(expected.doc())) {
            if (isEmpty(actual.doc())) {
                out.add(at + "doc: neither the catalog nor the annotation describes it");
            }
        } else {
            // Against the ASCII form: a source file cannot carry an em dash --
            // the Ant javac step rejects it as unmappable -- so the generator
            // converts on the way in and the annotation is required to hold the
            // converted text, not the original.
            check(out, at + "doc", BuildHintCodeGenerator.toAscii(expected.doc()), actual.doc());
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    private static void check(List<String> out, String what, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            out.add(what + ": catalog has [" + expected + "], annotation has [" + actual + "]");
        }
    }

    private static Map<String, BuildHints.Hint> byName(List<BuildHints.Hint> hints) {
        Map<String, BuildHints.Hint> out = new LinkedHashMap<String, BuildHints.Hint>();
        for (BuildHints.Hint h : hints) {
            out.put(h.name(), h);
        }
        return out;
    }

    /// The real annotation sources, so this tests the tree that ships rather
    /// than a fixture that resembles it.
    private static File annotationSources() {
        File src = new File("../../CodenameOne/src/com/codename1/annotations/buildhints");
        assertTrue(src.isDirectory(), "annotation sources not found at " + src.getAbsolutePath());
        return src;
    }


    /// No hint attribute may declare a default that carries a VALUE.
    ///
    /// The build server owns what happens when a hint is not set, and it may
    /// change that. A copy of that answer in an annotation cannot follow: it is
    /// compiled into every app already built against it, and it is what IDE
    /// completion and the javadoc show. `boolean appBundle() default false` read
    /// as "off unless you turn it on" while AndroidGradleBuilder defaults
    /// android.appBundle to true.
    ///
    /// So every default here must be a marker for "nothing was said" -- `""`,
    /// `{}`, `0`, or an enum's @HintUnset constant. A boolean cannot express
    /// that, which is why there are no boolean hint attributes left.
    @Test
    public void noHintAttributeDeclaresAValueBearingDefault() throws Exception {
        File dir = new File("../../CodenameOne/src/com/codename1/annotations/buildhints");
        assertTrue(dir.isDirectory(), dir.getAbsolutePath());
        java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
                "^\\s+([A-Za-z0-9_\\[\\]]+) ([A-Za-z0-9_]+)\\(\\) default ([^;]+);",
                java.util.regex.Pattern.MULTILINE);
        java.util.List<String> bad = new java.util.ArrayList<String>();
        for (String group : new String[]{"Android", "Ios", "IosPrivacy", "DesktopBuild",
                                         "Build", "Hardening", "OnDeviceDebug"}) {
            File f = new File(dir, group + ".java");
            assertTrue(f.isFile(), f.getAbsolutePath());
            String text = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
            java.util.regex.Matcher m = decl.matcher(text);
            while (m.find()) {
                String value = m.group(3).trim();
                boolean unset = "\"\"".equals(value) || "{}".equals(value)
                        || "0".equals(value) || value.endsWith(".DEFAULT");
                if (!unset) {
                    bad.add(group + "." + m.group(2) + " defaults to " + value);
                }
            }
        }
        assertTrue(bad.isEmpty(), "these restate a value the build server owns: " + bad);
    }

    /// ...and every enum a hint uses declares exactly one @HintUnset constant.
    ///
    /// Without it the attribute's default would have to name a real value, which
    /// is the same claim in another shape. Two would make "not set" ambiguous.
    @Test
    public void everyHintEnumDeclaresOneUnsetConstant() throws Exception {
        File dir = new File("../../CodenameOne/src/com/codename1/annotations/buildhints");
        for (String e : new String[]{"Toggle", "ThemeMode", "ThemeMode",
                                     "IosProjectType", "IosDependencyManager", "InstallLocation",
                                     "HardenStrings", "HardenLevel", "HardenControlFlow",
                                     "DesktopTitleBar", "ThemeMode"}) {
            File f = new File(dir, e + ".java");
            assertTrue(f.isFile(), f.getAbsolutePath());
            String text = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
            int count = 0;
            int at = text.indexOf("@HintUnset");
            while (at >= 0) {
                count++;
                at = text.indexOf("@HintUnset", at + 1);
            }
            assertEquals(1, count, e + " must mark exactly one constant @HintUnset");
        }
    }
}
