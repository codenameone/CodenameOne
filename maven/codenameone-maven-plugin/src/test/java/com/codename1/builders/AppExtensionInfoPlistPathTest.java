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
import static org.junit.Assert.assertNotNull;
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

    @Test
    public void eachConfigurationsPlistKeepsItsOwnIdentifier() throws Exception {
        File dist = tmp.newFolder("per-config-id");
        File extension = new File(dist, "WalletUIExtension");
        assertTrue(extension.mkdirs());
        File release = new File(extension, "release.plist");
        File debug = new File(extension, "debug.plist");
        write(release, identityPlist("com.example.app.Release"));
        write(debug, identityPlist("com.example.app.Debug"));
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = WalletUIExtension/release.plist\n"
                + "INFOPLIST_FILE[config\\=Debug] = WalletUIExtension/debug.plist\n");

        BuildRequest request = new BuildRequest();
        request.setMainClass("MyApp");
        request.setPackageName("com.example.app");
        request.setVersion("5.4");
        new IPhoneBuilder().stampAppExtensionInfoPlist(extension, request,
                IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64", null));

        // Judged against the ARCHIVE's identifier, the Debug plist's own literal read as
        // disagreeing and was replaced with $(PRODUCT_BUNDLE_IDENTIFIER) -- and the
        // per-configuration discovery that follows records literals and ignores references, so
        // the Debug identity was silently lost and that configuration fell back to the Release
        // identifier, to be signed with a profile issued for another bundle.
        String debugText = new String(Files.readAllBytes(debug.toPath()), "UTF-8");
        assertTrue(debugText, debugText.contains("<string>com.example.app.Debug</string>"));

        String releaseText = new String(Files.readAllBytes(release.toPath()), "UTF-8");
        assertTrue(releaseText, releaseText.contains("<string>com.example.app.Release</string>"));

        // And both survive into the settings the target is configured with.
        java.util.Map<String, String> identifiers = IPhoneBuilder.appExtensionPlistIdentifiers(
                extension, IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64",
                        null), "com.example.app");
        assertEquals("com.example.app.Release", identifiers.get("PRODUCT_BUNDLE_IDENTIFIER"));
        assertEquals("com.example.app.Debug",
                identifiers.get("PRODUCT_BUNDLE_IDENTIFIER[config=Debug]"));
    }

    private static String identityPlist(String identifier) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<plist version=\"1.0\">\n<dict>\n"
                + "\t<key>CFBundleIdentifier</key>\n\t<string>" + identifier + "</string>\n"
                + "\t<key>CFBundleShortVersionString</key>\n\t<string>5.4</string>\n"
                + "\t<key>CFBundleVersion</key>\n\t<string>5.4</string>\n"
                + "</dict>\n</plist>\n";
    }

    @Test
    public void aSharedPlistKeepsItsBaseIdentityForDiscovery() throws Exception {
        File dist = tmp.newFolder("shared-id");
        File extension = new File(dist, "WalletUIExtension");
        assertTrue(extension.mkdirs());
        File shared = new File(extension, "Info.plist");
        write(shared, identityPlist("com.example.app.Base"));
        // The base setting and a qualified one naming the SAME file, with the qualified
        // configuration carrying its own explicit identifier.
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = WalletUIExtension/Info.plist\n"
                + "INFOPLIST_FILE[config\\=Debug] = WalletUIExtension/Info.plist\n"
                + "PRODUCT_BUNDLE_IDENTIFIER[config\\=Debug] = com.example.app.Debug\n");

        IPhoneBuilder.ArchiveContext archive = IPhoneBuilder.ArchiveContext.of("iphoneos14.4",
                "Release", "arm64", null);
        // Read BEFORE stamping, which is the point: stamping the Debug context rewrites the
        // shared file's literal to $(PRODUCT_BUNDLE_IDENTIFIER), and judging each candidate in
        // its own context cannot help once another context has rewritten the file they share.
        java.util.Map<String, String> before = IPhoneBuilder.appExtensionPlistIdentifiers(
                extension, archive, "com.example.app");
        assertEquals("com.example.app.Base", before.get("PRODUCT_BUNDLE_IDENTIFIER"));

        BuildRequest request = new BuildRequest();
        request.setMainClass("MyApp");
        request.setPackageName("com.example.app");
        request.setVersion("5.4");
        new IPhoneBuilder().stampAppExtensionInfoPlist(extension, request, archive);

        // Afterwards the literal may well be a reference -- which is exactly why the builder
        // captures the identities first rather than reading them back here.
        String text = new String(Files.readAllBytes(shared.toPath()), "UTF-8");
        assertTrue(text, text.contains("<key>CFBundleIdentifier</key>"));
    }

    @Test
    public void anExplicitSettingIsNotOverriddenByARejectedPlistLiteral() throws Exception {
        File dist = tmp.newFolder("explicit-vs-plist");
        File extension = new File(dist, "WalletUIExtension");
        assertTrue(extension.mkdirs());
        write(new File(extension, "debug.plist"), identityPlist("com.example.app.FromPlist"));
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE[config\\=Debug] = WalletUIExtension/debug.plist\n"
                + "PRODUCT_BUNDLE_IDENTIFIER = com.example.app.FromSettings\n");

        java.util.Map<String, String> declared =
                IPhoneBuilder.appExtensionBuildSettings(extension);
        IPhoneBuilder.ArchiveContext archive = IPhoneBuilder.ArchiveContext.of("iphoneos14.4",
                "Release", "arm64", declared);

        // The plist states an identifier for Debug...
        java.util.Map<String, String> identifiers = IPhoneBuilder.appExtensionPlistIdentifiers(
                extension, archive, "com.example.app");
        assertEquals("com.example.app.FromPlist",
                identifiers.get("PRODUCT_BUNDLE_IDENTIFIER[config=Debug]"));

        // ...but the archive states one for the target, which outranks it -- which is why
        // stamping replaces that literal. Writing it back as a qualified setting would have Xcode
        // build Debug under FromPlist while preflight, the profile check and the export options
        // all used FromSettings.
        assertNotNull(IPhoneBuilder.winningSetting(declared, "PRODUCT_BUNDLE_IDENTIFIER",
                IPhoneBuilder.contextForCondition("PRODUCT_BUNDLE_IDENTIFIER[config=Debug]",
                        archive)));
    }

    @Test
    public void anUnqualifiedPathThatVariesByConfigurationIsEnumerated() throws Exception {
        File dist = tmp.newFolder("varying-path");
        File extension = new File(dist, "WalletUIExtension");
        assertTrue(new File(extension, "Release").mkdirs());
        assertTrue(new File(extension, "Debug").mkdirs());
        File release = new File(extension, "Release/Info.plist");
        File debug = new File(extension, "Debug/Info.plist");
        write(release, identityPlist("com.example.app.Release"));
        write(debug, identityPlist("com.example.app.Debug"));
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = WalletUIExtension/$(CONFIGURATION)/Info.plist\n");

        java.util.Map<String, File> plists = IPhoneBuilder.appExtensionInfoPlists(extension,
                IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64", null));

        // The KEY is unqualified and the VALUE still names a different file per configuration.
        // Enumerated by key alone, only the archive's own file was ever seen: the Debug plist was
        // never stamped, and the identifier read out of the Release one was recorded as if it
        // were universal.
        assertTrue(plists.toString(), plists.values().contains(release));
        assertTrue(plists.toString(), plists.values().contains(debug));

        // And each identity stays in its own configuration rather than becoming the base.
        java.util.Map<String, String> identifiers = IPhoneBuilder.appExtensionPlistIdentifiers(
                extension, IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64",
                        null), "com.example.app");
        assertEquals("com.example.app.Debug",
                identifiers.get("PRODUCT_BUNDLE_IDENTIFIER[config=Debug]"));
    }

    @Test
    public void aPathThroughAConditionalHelperIsEnumeratedToo() throws Exception {
        File dist = tmp.newFolder("helper-path");
        File extension = new File(dist, "WalletUIExtension");
        assertTrue(extension.mkdirs());
        File release = new File(extension, "release.plist");
        File debug = new File(extension, "debug.plist");
        write(release, identityPlist("com.example.app.Release"));
        write(debug, identityPlist("com.example.app.Debug"));
        write(new File(extension, "buildSettings.properties"),
                "PLIST_PATH = WalletUIExtension/release.plist\n"
                + "PLIST_PATH[config\\=Debug] = WalletUIExtension/debug.plist\n"
                + "INFOPLIST_FILE = $(PLIST_PATH)\n");

        java.util.Map<String, File> plists = IPhoneBuilder.appExtensionInfoPlists(extension,
                IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64", null));

        // The helper is the thing that varies, and helper keys are not plist candidates of their
        // own -- so the Debug plist was invisible and never stamped.
        assertTrue(plists.toString(), plists.values().contains(release));
        assertTrue(plists.toString(), plists.values().contains(debug));
    }

    @Test
    public void aPathThatDoesNotVaryAddsNothing() throws Exception {
        File dist = tmp.newFolder("stable-path");
        File extension = new File(dist, "WalletUIExtension");
        assertTrue(extension.mkdirs());
        write(new File(extension, "Info.plist"), identityPlist("com.example.app.Ext"));
        write(new File(extension, "buildSettings.properties"),
                "INFOPLIST_FILE = WalletUIExtension/$(TARGET_NAME:identifier).plist\n");

        java.util.Map<String, File> plists = IPhoneBuilder.appExtensionInfoPlists(extension,
                IPhoneBuilder.ArchiveContext.of("iphoneos14.4", "Release", "arm64", null));

        // The ordinary case: a reference that means the same thing in every configuration adds no
        // second candidate, so nothing downstream sees a file twice.
        assertEquals(plists.toString(), 1, plists.size());
    }
}
