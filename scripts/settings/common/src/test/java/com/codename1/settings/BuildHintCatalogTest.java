/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
import com.codename1.settings.hints.BuildHintType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BuildHintCatalogTest {
    @Test
    public void parsesDeveloperGuideBuildHintTable() {
        String doc = """
                Before
                |===
                |Name\t|Description

                |android.debug
                |true/false defaults to true - indicates whether to include debug.

                |ios.plistInject
                |Injects raw XML into the plist.

                |windows.signing.timestampUrl
                |RFC 3161 timestamp server URL.

                |===
                After
                """;
        BuildHintCatalog catalog = BuildHintCatalog.fromAsciiDoc(doc);
        assertNotNull(catalog.get("android.debug"));
        assertEquals(BuildHintType.BOOLEAN, catalog.get("android.debug").type());
        assertEquals(BuildHintType.XML, catalog.get("ios.plistInject").type());
        assertEquals(BuildHintType.URL, catalog.get("windows.signing.timestampUrl").type());
    }

    @Test
    public void packagedDeveloperGuideCatalogProvidesKnownHintTypes() throws Exception {
        try (InputStream in = CodenameOneSettings.class.getResourceAsStream(
                "/com/codename1/settings/hints/Advanced-Topics-Under-The-Hood.asciidoc")) {
            assertNotNull(in, "The Settings jar should carry the developer-guide build hint table.");
            String doc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            BuildHintCatalog catalog = BuildHintCatalog.fromAsciiDoc(doc);
            assertEquals(BuildHintType.INTEGER, catalog.get("java.version").type());
            assertEquals(BuildHintType.VERSION, catalog.get("build.cn1Version").type());
            assertEquals(BuildHintType.VERSION, catalog.get("ios.bundleVersion").type());
            assertEquals(BuildHintType.INTEGER, catalog.get("android.targetSDKVersion").type());
            assertEquals(BuildHintType.BOOLEAN, catalog.get("android.useAndroidX").type());
        }
    }

    @Test
    public void documentProviderHintsAreDiscoverableFromTheSettingsUi() throws Exception {
        // The Settings app is how all four IDEs edit build hints, and it learns them by parsing
        // the developer guide's canonical table rather than from a list of its own. A hint
        // documented only in its feature chapter is therefore invisible here -- which is a
        // feature a developer can use but cannot find.
        try (InputStream in = CodenameOneSettings.class.getResourceAsStream(
                "/com/codename1/settings/hints/Advanced-Topics-Under-The-Hood.asciidoc")) {
            assertNotNull(in, "The Settings jar should carry the developer-guide build hint table.");
            String doc = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            BuildHintCatalog catalog = BuildHintCatalog.fromAsciiDoc(doc);
            assertNotNull(catalog.get("ios.documentProvider.enabled"));
            assertEquals(BuildHintType.BOOLEAN, catalog.get("ios.documentProvider.enabled").type());
            assertEquals(BuildHintType.BOOLEAN,
                    catalog.get("ios.documentProvider.extension").type());
            // Types matter: they choose the editor the UI offers. The type is inferred from the
            // description, and an angle bracket anywhere in it makes the hint an XML field -- so
            // writing the app group's default as group.<package> silently gave a plain string an
            // XML editor.
            assertEquals(BuildHintType.TEXT, catalog.get("ios.documentProvider.appGroup").type());
            assertEquals(BuildHintType.TEXT, catalog.get("ios.documentProvider.displayName").type());
            assertEquals(BuildHintType.VERSION,
                    catalog.get("ios.documentProvider.deploymentTarget").type());
            assertEquals(BuildHintType.TEXT,
                    catalog.get("ios.documentProvider.buildSettings.SETTING").type());
            // Searching is how the hint is actually found in the UI.
            assertFalse(catalog.search("documentProvider").isEmpty());
        }
    }
}
