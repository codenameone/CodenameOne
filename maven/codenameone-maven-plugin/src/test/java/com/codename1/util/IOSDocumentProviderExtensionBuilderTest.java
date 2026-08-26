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
package com.codename1.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated file provider target. Nothing downstream of this class is exercised by a JVM test
 * -- the Swift is compiled by Xcode and the plist is read by the OS -- so the assertions here are
 * about the handful of values whose absence produces a target that builds green and does nothing.
 */
public class IOSDocumentProviderExtensionBuilderTest {

    private IOSDocumentProviderExtensionBuilder validBuilder() {
        return new IOSDocumentProviderExtensionBuilder()
                .setAppGroupId("group.com.example.myapp")
                .setHostBundleId("com.example.myapp")
                .setDisplayName("My Documents");
    }

    private static String text(Map<String, byte[]> files, String name) {
        byte[] data = files.get(name);
        assertTrue(data != null, name + " must be present in file map");
        return new String(data, StandardCharsets.UTF_8);
    }

    @Test
    public void fileMapCarriesTheTargetAndExactlyOneProvider() throws Exception {
        Map<String, byte[]> files = validBuilder().buildFileMap();
        assertTrue(files.containsKey("Info.plist"));
        assertTrue(files.containsKey("CN1Documents.entitlements"),
                "entitlements file must be named after the extension");
        assertTrue(files.containsKey("buildSettings.properties"));
        assertTrue(files.containsKey("CN1DocumentConfig.swift"));
        assertTrue(files.containsKey("CN1DocumentIndex.swift"));
        assertTrue(files.containsKey("CN1DocumentItem.swift"));
        assertTrue(files.containsKey("CN1DocumentEnumerator.swift"));
        assertTrue(files.containsKey("CN1DocumentRemote.swift"));
        // Both providers claim com.apple.fileprovider-nonui, so a target holding the two would
        // have two principal classes for one extension point.
        assertTrue(files.containsKey("CN1FileProviderExtension.swift"));
        assertFalse(files.containsKey("CN1FileProviderClassic.swift"));
    }

    @Test
    public void aLowDeploymentTargetSwapsInTheClassicProvider() throws Exception {
        Map<String, byte[]> files = validBuilder().setDeploymentTarget("14.0").buildFileMap();
        assertTrue(files.containsKey("CN1FileProviderClassic.swift"));
        assertFalse(files.containsKey("CN1FileProviderExtension.swift"));
        String plist = text(files, "Info.plist");
        assertTrue(plist.contains("CN1FileProviderClassic"), plist);
        assertTrue(plist.contains("NSExtensionFileProviderSupportsEnumeration"), plist);
    }

    @Test
    public void versionsAreComparedNumericallyNotLexically() {
        // "16.0".compareTo("9.0") is negative, which would hand a modern deployment target the
        // deprecated provider and leave the extension inert on every device that could run the
        // good one.
        assertTrue(IOSDocumentProviderExtensionBuilder.compareVersions("16.0", "9.0") > 0);
        assertTrue(IOSDocumentProviderExtensionBuilder.compareVersions("15.9", "16.0") < 0);
        assertEquals(0, IOSDocumentProviderExtensionBuilder.compareVersions("16", "16.0"));
        assertTrue(IOSDocumentProviderExtensionBuilder.compareVersions("17.4", "16.0") > 0);
        assertFalse(new IOSDocumentProviderExtensionBuilder()
                .setAppGroupId("group.a").setHostBundleId("a")
                .setDeploymentTarget("15.0").usesReplicatedApi());
        assertTrue(new IOSDocumentProviderExtensionBuilder()
                .setAppGroupId("group.a").setHostBundleId("a")
                .setDeploymentTarget("16.0").usesReplicatedApi());
    }

    @Test
    public void plistNamesTheGroupTwiceBecauseTheSystemAndTheSwiftReadDifferentKeys() throws Exception {
        String plist = text(validBuilder().buildFileMap(), "Info.plist");
        assertTrue(plist.contains("<key>CN1DocumentsAppGroup</key>"), plist);
        assertTrue(plist.contains("<key>NSExtensionFileProviderDocumentGroup</key>"), plist);
        assertTrue(plist.contains("com.apple.fileprovider-nonui"), plist);
        assertTrue(plist.contains("$(PRODUCT_MODULE_NAME).CN1FileProviderExtension"), plist);
        assertEquals(2, countOccurrences(plist, "group.com.example.myapp"),
                "both keys must name the same group: " + plist);
    }

    @Test
    public void entitlementsGrantTheGroupAndNothingElseOnIos() throws Exception {
        String ent = text(validBuilder().buildFileMap(), "CN1Documents.entitlements");
        assertTrue(ent.contains("com.apple.security.application-groups"), ent);
        assertTrue(ent.contains("group.com.example.myapp"), ent);
        assertFalse(ent.contains("app-sandbox"), ent);
    }

    @Test
    public void theMacTargetIsSandboxedAndMayReachTheNetwork() throws Exception {
        Map<String, byte[]> files = validBuilder().setMacTarget(true)
                .setDeploymentTarget("13.0").buildFileMap();
        String ent = text(files, "CN1Documents.entitlements");
        assertTrue(ent.contains("com.apple.security.app-sandbox"), ent);
        // Without this the remote mode fails in a way the user reads as a missing file.
        assertTrue(ent.contains("com.apple.security.network.client"), ent);
        String settings = text(files, "buildSettings.properties");
        assertTrue(settings.contains("MACOSX_DEPLOYMENT_TARGET=13.0"), settings);
        assertTrue(settings.contains("SDKROOT=macosx"), settings);
        assertFalse(settings.contains("IPHONEOS_DEPLOYMENT_TARGET"), settings);
        // AppKit marks the classic API unavailable, so macOS is replicated whatever is asked for.
        assertTrue(files.containsKey("CN1FileProviderExtension.swift"));
    }

    @Test
    public void buildSettingsPointXcodeAtTheGeneratedPlistAndEntitlements() throws Exception {
        String settings = text(validBuilder().buildFileMap(), "buildSettings.properties");
        assertTrue(settings.contains("PRODUCT_BUNDLE_IDENTIFIER=com.example.myapp.CN1Documents"),
                settings);
        assertTrue(settings.contains("CODE_SIGN_ENTITLEMENTS=CN1Documents/CN1Documents.entitlements"),
                settings);
        assertTrue(settings.contains("INFOPLIST_FILE=CN1Documents/Info.plist"), settings);
        assertTrue(settings.contains("SKIP_INSTALL=YES"), settings);
    }

    @Test
    public void configSwiftEscapesWhatItInterpolates() throws Exception {
        String swift = text(validBuilder().setDisplayName("My \"Docs\"").buildFileMap(),
                "CN1DocumentConfig.swift");
        assertTrue(swift.contains("\\\"Docs\\\""), swift);
        assertTrue(swift.contains("static let appGroupId = \"group.com.example.myapp\""), swift);
    }

    @Test
    public void extensionNameFlowsIntoTheBundleIdAndTheFileNames() throws Exception {
        Map<String, byte[]> files = validBuilder().setExtensionName("MyDocs").buildFileMap();
        assertTrue(files.containsKey("MyDocs.entitlements"));
        assertTrue(text(files, "buildSettings.properties")
                .contains("PRODUCT_BUNDLE_IDENTIFIER=com.example.myapp.MyDocs"));
    }

    @Test
    public void refusesAConfigurationThatWouldBuildButNotWork() {
        assertThrows(IllegalStateException.class,
                () -> new IOSDocumentProviderExtensionBuilder()
                        .setHostBundleId("com.example.myapp").buildFileMap(),
                "an extension with no App Group reads an empty container");
        assertThrows(IllegalStateException.class,
                () -> new IOSDocumentProviderExtensionBuilder()
                        .setAppGroupId("group.com.example.myapp").buildFileMap(),
                "without the host bundle id the extension cannot derive its own");
        assertThrows(IllegalStateException.class,
                () -> validBuilder().setExtensionName("My Docs").buildFileMap(),
                "a target name with a space is not a usable bundle id component");
        assertThrows(IllegalStateException.class,
                () -> validBuilder().setMacTarget(true).setDeploymentTarget("12.0").buildFileMap(),
                "macOS below the replicated floor has no provider at all");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int at = haystack.indexOf(needle);
        while (at >= 0) {
            count++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return count;
    }
}
