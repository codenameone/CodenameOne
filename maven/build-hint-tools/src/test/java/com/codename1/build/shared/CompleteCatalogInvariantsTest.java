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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Invariants over the COMPLETE hint set.
 *
 * <p>Here rather than beside the catalog because the complete set only exists on
 * a classpath carrying the rendered data file, and this is the only module that
 * can render one for its own tests: the generator lives here and depends on the
 * catalog, so the catalog cannot render its own. On the catalog's own classpath
 * {@code BuildHints.entries()} holds the hand written half alone, and these two
 * assertions were quietly asserting nothing about the annotated hints.</p>
 */
public class CompleteCatalogInvariantsTest {

    @Test
    void aliasesResolveToARealNonAliasHint() {
        for (BuildHints.Hint h : BuildHints.entries()) {
            if (h.aliasOf() == null) {
                continue;
            }
            BuildHints.Hint target = BuildHints.byName(h.aliasOf());
            assertNotNull(target, h.name() + " aliases unknown hint " + h.aliasOf());
            assertTrue(target.aliasOf() == null,
                    h.name() + " aliases " + target.name() + ", which is itself an alias");
            assertEquals(target.name(), BuildHints.canonicalName(h.name()));
        }
    }

    @Test
    void theCatalogAgreesWithLibraryHintMergerOnEverySeparatorItDefines() {
        for (Map.Entry<String, String> e : libraryHintMergerSeparators().entrySet()) {
            BuildHints.Hint h = BuildHints.byName(e.getKey());
            assertNotNull(h, "LibraryHintMerger defines a separator for " + e.getKey()
                    + " but the catalog does not describe it");
            assertEquals(e.getValue(), BuildHints.separatorFor(e.getKey()),
                    "separator mismatch for " + e.getKey() + ": LibraryHintMerger says "
                            + quote(e.getValue()) + ", catalog says "
                            + quote(BuildHints.separatorFor(e.getKey())));
        }
    }

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

    private static String quote(String s) {
        return s == null ? "null" : "\"" + s + "\"";
    }
}
