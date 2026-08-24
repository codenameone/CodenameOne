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
package com.codename1.builders;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppExtensionBuildSettingsTest {

    /**
     * The block is written the way it appears in a pbxproj: tab-indented, one
     * {@code KEY = VALUE;} per line.
     */
    private static final String BLOCK = "CLANG_ANALYZER_NONNULL = YES;\n"
            + "\t\t\t\tCLANG_CXX_LANGUAGE_STANDARD = \"gnu++14\";\n"
            + "\t\t\t\tCLANG_ENABLE_MODULES = YES;\n"
            + "\t\t\t\tCLANG_ENABLE_OBJC_ARC = YES;\n"
            + "\t\t\t\tCLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;";

    @Test
    public void valueLosesItsTrailingSemicolon() {
        Map<String, String> settings = IPhoneBuilder.parseXcodeBuildSettings(BLOCK);
        // A value of ";" is what CLANG_ENABLE_MODULES used to get, and Xcode reads that
        // as "off": no -fmodules, no clang autolinking, and an extension importing UIKit
        // reaches ld with Foundation alone and fails on _OBJC_CLASS_$_UIView.
        assertEquals("YES", settings.get("CLANG_ENABLE_MODULES"));
        assertEquals("YES", settings.get("CLANG_ENABLE_OBJC_ARC"));
        assertEquals("YES", settings.get("CLANG_ANALYZER_NONNULL"));
        assertEquals("YES_AGGRESSIVE", settings.get("CLANG_WARN_UNGUARDED_AVAILABILITY"));
    }

    @Test
    public void quotedValueIsUnwrappedBeforeItBecomesARubyLiteral() {
        Map<String, String> settings = IPhoneBuilder.parseXcodeBuildSettings(BLOCK);
        // Kept quotes would be emitted as e.build_settings['...'] = ""gnu++14"", which
        // is a Ruby syntax error that takes the whole project fixup script with it.
        assertEquals("gnu++14", settings.get("CLANG_CXX_LANGUAGE_STANDARD"));
    }

    @Test
    public void blankAndMalformedLinesAreSkipped() {
        Map<String, String> settings = IPhoneBuilder.parseXcodeBuildSettings(
                "\n   \nCLANG_ENABLE_MODULES = YES;\nnot a setting\n");
        assertEquals(1, settings.size());
        assertEquals("YES", settings.get("CLANG_ENABLE_MODULES"));
    }

    @Test
    public void paddingIsStrippedFromArchiveSettings() throws Exception {
        java.io.File dist = java.nio.file.Files.createTempDirectory("appext").toFile();
        java.io.File extension = new java.io.File(dist, "WalletUIExtension");
        assertTrue(extension.mkdirs());
        java.io.FileWriter w = new java.io.FileWriter(
                new java.io.File(extension, "buildSettings.properties"));
        w.write("PRODUCT_BUNDLE_IDENTIFIER=com.example.app.Ext \n");
        w.close();

        // Properties keeps the trailing space and Xcode does not. Kept, preflight validated
        // "com.example.app.Ext" while the target was handed "com.example.app.Ext " -- an
        // identifier no profile matches, from two readers of one file disagreeing.
        assertEquals("com.example.app.Ext", IPhoneBuilder.appExtensionBuildSettings(extension)
                .get("PRODUCT_BUNDLE_IDENTIFIER"));
    }

    @Test
    public void extensionDeviceFamiliesFollowTheApp() {
        // The translator gives the app target "1" for iphone and "2" for anything else that is
        // not "ios"; an extension pinned to "1,2" beside an iPhone-only app is an upload
        // rejection for an embedded bundle its container does not support.
        assertEquals("1", IPhoneBuilder.embeddedExtensionDeviceFamily("iphone"));
        assertEquals("2", IPhoneBuilder.embeddedExtensionDeviceFamily("ipad"));
        assertEquals("1,2", IPhoneBuilder.embeddedExtensionDeviceFamily("ios"));
        assertEquals("1,2", IPhoneBuilder.embeddedExtensionDeviceFamily(null));
    }
}
