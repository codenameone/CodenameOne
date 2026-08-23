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

    @Test
    public void searchStillMatchesOnNameAndDescription() {
        BuildHintCatalog catalog = BuildHintCatalog.load();
        assertFalse(catalog.search("pods").isEmpty());
        assertFalse(catalog.search("android").isEmpty());
    }
}
