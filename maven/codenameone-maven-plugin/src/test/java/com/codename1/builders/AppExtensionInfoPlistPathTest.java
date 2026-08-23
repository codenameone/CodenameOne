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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class AppExtensionInfoPlistPathTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void theDefaultIsTheFoldersOwnInfoPlist() throws Exception {
        File extension = extension();
        assertEquals(new File(extension, "Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void anOverriddenPathIsReadRelativeToTheProjectDirectory() throws Exception {
        File extension = extension();
        // INFOPLIST_FILE is what Xcode processes into the .appex, so it is the file that has to
        // carry the identifier -- stamping the folder's Info.plist would edit a file nothing
        // builds.
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = WalletUIExtension/Release-Info.plist\n");
        assertEquals(new File(extension, "Release-Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void theProjectRootPrefixIsUnderstood() throws Exception {
        File extension = extension();
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = \"$(SRCROOT)/WalletUIExtension/Release-Info.plist\"\n");
        assertEquals(new File(extension, "Release-Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void theSettingsThisBuildKnowsAreSubstituted() throws Exception {
        File extension = extension();
        // What an Xcode project actually writes for the plist in the extension's own folder. Every
        // part of it is known here: SRCROOT is the project directory the folders are extracted
        // into, and TARGET_NAME is the folder's name, because that is the name the target is
        // created with.
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = $(SRCROOT)/$(TARGET_NAME)/Info.plist\n");
        assertEquals(new File(extension, "Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void theBraceSpellingResolvesToo() throws Exception {
        File extension = extension();
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = ${PROJECT_DIR}/${PRODUCT_NAME}/Custom-Info.plist\n");
        assertEquals(new File(extension, "Custom-Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void anOverriddenProductNameIsUsedForItsOwnReference() throws Exception {
        File extension = extension();
        write(new File(extension, "buildSettings.properties"),
                "PRODUCT_NAME = Renamed\nINFOPLIST_FILE = $(PRODUCT_NAME)/Info.plist\n");
        assertEquals(new File(extension.getParentFile(), "Renamed/Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void anAbsolutePathOutsideTheProjectIsRefused() throws Exception {
        File extension = extension();
        File outside = new File(tmp.getRoot(), "outside.plist");
        write(outside, "<plist><dict/></plist>");
        // The archive is a customer upload and the stamper WRITES to whatever this names, so an
        // absolute path would have the daemon rewriting a file outside the build.
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = " + outside.getAbsolutePath() + "\n");
        assertNull(IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void aTraversalOutOfTheProjectIsRefused() throws Exception {
        File extension = extension();
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = ../../shared.plist\n");
        assertNull(IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void aSymlinkOutOfTheProjectIsRefused() throws Exception {
        File extension = extension();
        File outside = new File(tmp.getRoot(), "outside.plist");
        write(outside, "<plist><dict/></plist>");
        // A zip may carry symlinks, so a path that sits inside the project can still land outside.
        Files.createSymbolicLink(new File(extension, "Info.plist").toPath(), outside.toPath());
        assertNull(IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void aPlistBesideTheExtensionFolderIsStillAllowed() throws Exception {
        File extension = extension();
        // Under the project directory but outside the extension's own folder: legitimate, an
        // extension may share a plist with the rest of the project.
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = Shared-Info.plist\n");
        assertEquals(new File(extension.getParentFile(), "Shared-Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void theArchivesOwnSettingsExpandInThePath() throws Exception {
        File extension = extension();
        // Both settings are copied onto the target, so Xcode resolves this path; expanding only
        // the built-in four called it unresolvable and left the real plist unstamped.
        write(new File(extension, "buildSettings.properties"),
                "PLIST_DIR = WalletUIExtension\nINFOPLIST_FILE = $(PLIST_DIR)/Info.plist\n");
        assertEquals(new File(extension, "Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void aSettingThatNamesAnotherSettingExpandsToo() throws Exception {
        File extension = extension();
        write(new File(extension, "buildSettings.properties"),
                "ROOT = $(SRCROOT)/WalletUIExtension\nPLIST_DIR = $(ROOT)\n"
                + "INFOPLIST_FILE = $(PLIST_DIR)/Custom-Info.plist\n");
        assertEquals(new File(extension, "Custom-Info.plist"),
                IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    @Test
    public void anUnresolvableReferenceIsRefusedRatherThanGuessed() throws Exception {
        File extension = extension();
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = $(CONFIGURATION)/Info.plist\n");
        // Null makes the caller say so and leave every file alone; editing the default here would
        // be editing a plist the build does not use.
        assertNull(IPhoneBuilder.appExtensionInfoPlist(extension));
    }

    private File extension() throws Exception {
        File dist = tmp.newFolder("dist");
        File extension = new File(dist, "WalletUIExtension");
        extension.mkdirs();
        return extension;
    }

    private static void write(File file, String contents) throws Exception {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(contents.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    @Test
    public void theContainingAppsPlistIsNotStamped() throws Exception {
        File dist = tmp.newFolder("hostplist");
        File appSrc = new File(dist, "MyApp-src");
        assertTrue(appSrc.mkdirs());
        File hostPlist = new File(appSrc, "MyApp-Info.plist");
        assertTrue(hostPlist.createNewFile());
        File extension = new File(dist, "WalletUIExtension");
        assertTrue(extension.mkdirs());
        File ownPlist = new File(extension, "Info.plist");
        assertTrue(ownPlist.createNewFile());

        // Everything under the project directory is writable on purpose, so an extension setting
        // that names the app's own plist -- by relative path or through a reference -- reaches
        // the stamper like any other candidate. What would be written there is an EXTENSION's
        // identity: the app's version, or its identifier handed to a $(PRODUCT_BUNDLE_IDENTIFIER)
        // that means something else in the app target.
        assertTrue(IPhoneBuilder.isHostAppInfoPlist(hostPlist, dist, "MyApp"));
        assertTrue(IPhoneBuilder.isHostAppInfoPlist(
                new File(extension, "../MyApp-src/MyApp-Info.plist"), dist, "MyApp"));
        assertFalse(IPhoneBuilder.isHostAppInfoPlist(ownPlist, dist, "MyApp"));
        assertFalse(IPhoneBuilder.isHostAppInfoPlist(hostPlist, dist, null));
    }

    @Test
    public void oneFileNamedByTwoConditionsIsStampedInBoth() throws Exception {
        File dist = tmp.newFolder("shared-plist");
        File extension = new File(dist, "WalletUIExtension");
        assertTrue(extension.mkdirs());
        File shared = new File(extension, "Info.plist");
        write(shared, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<plist version=\"1.0\">\n<dict>\n"
                + "\t<key>CFBundleShortVersionString</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleVersion</key>\n\t<string>$(MARKETING_VERSION)</string>\n"
                + "\t<key>CFBundleIdentifier</key>\n\t<string>com.example.app.WalletUIExtension</string>\n"
                + "</dict>\n</plist>\n");
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = WalletUIExtension/Info.plist\n"
                + "INFOPLIST_FILE[config\\=Debug] = WalletUIExtension/Info.plist\n"
                + "MARKETING_VERSION = 5.4\n"
                + "MARKETING_VERSION[config\\=Debug] = 1.0\n");

        BuildRequest request = new BuildRequest();
        request.setMainClass("MyApp");
        request.setPackageName("com.example.app");
        request.setVersion("5.4");
        new IPhoneBuilder().stampAppExtensionInfoPlist(extension, request,
                IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64", null));

        // One physical plist, named by the base setting and by a Debug-qualified one. Under
        // Release the reference already resolves to the app's 5.4 and is left; deduplicating on
        // the path alone then skipped the Debug pass, and the Debug build off these sources
        // shipped $(MARKETING_VERSION) = 1.0. The file cannot be right for both while the
        // reference stands, so the literal has to win.
        String stamped = new String(Files.readAllBytes(shared.toPath()), "UTF-8");
        assertTrue(stamped, stamped.contains("<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>"));
        assertFalse(stamped, stamped.contains("$(MARKETING_VERSION)"));
    }
}
