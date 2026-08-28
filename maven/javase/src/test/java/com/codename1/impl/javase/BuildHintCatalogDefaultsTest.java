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
package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Build Hint editor's schema, as the simulator assembles it.
 *
 * <p>Both halves of this ran silently wrong at some point: the catalog resource
 * went missing from a build front end and the editor simply came up with no
 * catalog, and a hint whose group differed only in case from a hand-written one
 * put a second group in the editor next to the first.</p>
 */
public class BuildHintCatalogDefaultsTest {

    /**
     * The data file is on the classpath and describes hints.
     *
     * <p>It reaches the Maven build as a dependency and the Ant build through an
     * explicit copy in Ports/JavaSE/build.xml. A missing resource is not an
     * exception -- the loader treats it as an empty catalog -- so nothing but a
     * test notices that the editor has lost every annotated hint.</p>
     */
    @Test
    public void theCatalogResourceIsOnTheClasspath() throws Exception {
        java.io.InputStream in = BuildHintCatalogDefaults.class
                .getResourceAsStream("/cn1-build-hints.json");
        assertNotNull(in, "cn1-build-hints.json is not on the JavaSE classpath");
        try {
            byte[] buf = new byte[4096];
            int total = 0;
            int r;
            while ((r = in.read(buf)) > 0) {
                total += r;
            }
            assertTrue(total > 1000, "the catalog resource is implausibly small: " + total);
        } finally {
            in.close();
        }
    }

    /**
     * harden.rename joins the hand-written hardening group rather than starting a
     * second one.
     *
     * <p>It is the one HARDENING hint BuildHintSchemaDefaults does not declare, so
     * it is the one that used to be registered under the annotation's spelling --
     * `Hardening` -- while the other five sat under `hardening`. The editor keys
     * groups by that string, so the user saw "App Hardening" beside "App Hardening
     * (Enterprise)", each holding part of one setting group.</p>
     */
    @Test
    public void aCatalogHintJoinsTheHandWrittenGroupThatAlreadyExists() {
        BuildHintSchemaDefaults.register();
        BuildHintCatalogDefaults.register();

        assertEquals("App Hardening (Enterprise)",
                System.getProperty("codename1.arg.{{@hardening}}.label"));
        assertNotNull(System.getProperty("codename1.arg.{{#hardening#harden.rename}}.label"),
                "harden.rename is not in the hand-written hardening group");
        assertNull(System.getProperty("codename1.arg.{{@Hardening}}.label"),
                "a second hardening group was registered under the annotation's spelling");
        assertNull(System.getProperty("codename1.arg.{{#Hardening#harden.rename}}.label"),
                "harden.rename was registered under a second group");
    }

    /**
     * The group key is the annotation's real name, not one derived from the enum.
     *
     * <p>DESKTOP is `@DesktopBuild` -- `@Desktop` would clash with the public
     * com.codename1.ui.Desktop class -- and GENERAL is `@Build`. Camel-casing the
     * enum constant produced `Desktop` and `General`, group keys that name
     * nothing, so the editor's own idea of the annotation disagreed with the
     * annotation a developer would have to write.</p>
     */
    @Test
    public void theGroupKeyIsTheAnnotationsRealName() {
        BuildHintSchemaDefaults.register();
        BuildHintCatalogDefaults.register();

        assertNotNull(System.getProperty("codename1.arg.{{#DesktopBuild#desktop.titleBar}}.label"));
        assertNotNull(System.getProperty("codename1.arg.{{#Build#facebook.appId}}.label"));
        assertNull(System.getProperty("codename1.arg.{{@Desktop}}.label"),
                "the group key was camel-cased from the enum instead of naming @DesktopBuild");
        assertNull(System.getProperty("codename1.arg.{{@General}}.label"),
                "the group key was camel-cased from the enum instead of naming @Build");
    }

    /** A hand-written hint keeps its own description; the catalog does not restate it. */
    @Test
    public void aHandWrittenHintIsNotRegisteredTwice() {
        BuildHintSchemaDefaults.register();
        BuildHintCatalogDefaults.register();

        assertNull(System.getProperty("codename1.arg.{{#Hardening#harden.level}}.label"),
                "harden.level was registered a second time under the catalog's group");
        assertFalse(BuildHintSchemaDefaults.declaredHints().isEmpty());
    }
}
