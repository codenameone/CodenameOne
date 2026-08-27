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
package com.codename1.maven;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * An app extension is signed against its own App ID, and the app's profile cannot stand in
 * for one.
 *
 * <p>A Wallet extension brought in as an Xcode project ("Mode 2") reached the build server
 * with no profile of its own, so the extension targets inherited the app's -- and the build
 * died four minutes in with {@code Provisioning profile "HBZ_PROD_DISTRIBUTION" has app ID
 * "com.example.app", which does not match the bundle ID "com.example.app.WalletUIExtension"}.
 * Everything needed to say so is on disk before the build is sent: the folder that becomes
 * the target, and the App ID the app's profile covers.</p>
 */
public class IOSAppExtensionSigningPreflightTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String HEAD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
            + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\"><dict>\n";

    /** A profile of the shape the parser insists on, covering one App ID. */
    private File profile(String name, String appIdentifier) throws Exception {
        String plist = HEAD
                + "<key>Name</key><string>" + name + "</string>\n"
                + "<key>UUID</key><string>0f7ac3c1-4d0e-4e8a-9d1f-8b6a2c5e7d90</string>\n"
                + "<key>ExpirationDate</key><date>2099-01-01T00:00:00Z</date>\n"
                + "<key>DeveloperCertificates</key><array><data>Zm9v</data></array>\n"
                + "<key>Entitlements</key><dict>"
                + "<key>application-identifier</key><string>" + appIdentifier + "</string>"
                + "<key>get-task-allow</key><false/></dict>\n"
                + "</dict></plist>";
        byte[] payload = plist.getBytes("UTF-8");
        byte[] wrapped = new byte[payload.length + 24];
        for (int i = 0; i < 16; i++) {
            wrapped[i] = (byte) (0x80 + i);
        }
        System.arraycopy(payload, 0, wrapped, 16, payload.length);
        File f = tmp.newFile(name + ".mobileprovision");
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(wrapped);
        } finally {
            out.close();
        }
        return f;
    }

    private File iosDir() throws Exception {
        File ios = tmp.newFolder("ios");
        new File(ios, "app_extensions").mkdirs();
        return ios;
    }

    private File extensionFolder(File ios, String name) {
        File dir = new File(new File(ios, "app_extensions"), name);
        dir.mkdirs();
        return dir;
    }

    private void write(File f, String contents) throws Exception {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(contents.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private Properties settings(File appProfile) throws Exception {
        Properties p = new Properties();
        p.setProperty("codename1.packageName", "com.example.app");
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                appProfile.getAbsolutePath());
        return p;
    }

    private static List<IOSProvisioningPreflight.Problem> check(Properties p, File ios) {
        return IOSProvisioningPreflight.checkAppExtensions(p, true, ios);
    }

    // ---- extensions the builder GENERATES, which never appear on disk ----

    private static List<IOSProvisioningPreflight.Problem> checkGenerated(Properties p) {
        return IOSProvisioningPreflight.checkGeneratedExtensions(p, true);
    }

    @Test
    public void generatedDocumentProviderWithNoProfileOfItsOwnIsRefused() throws Exception {
        // No folder under ios/app_extensions -- the builder synthesizes this target from hints --
        // so the folder-driven check cannot see it and this one has to.
        Properties p = settings(profile("PROD", "ABCD1234.com.example.app"));
        p.setProperty("codename1.arg.ios.documentProvider.enabled", "true");
        List<IOSProvisioningPreflight.Problem> problems = checkGenerated(p);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).fatal);
        assertTrue(problems.get(0).message,
                problems.get(0).message.contains("com.example.app.CN1Documents"));
    }

    @Test
    public void generatedDocumentProviderWithItsOwnProfileIsAccepted() throws Exception {
        Properties p = settings(profile("PROD", "ABCD1234.com.example.app"));
        p.setProperty("codename1.arg.ios.documentProvider.enabled", "true");
        p.setProperty("codename1.ios.appext.CN1Documents.provision", "/tmp/CN1Documents.mobileprovision");
        assertTrue(checkGenerated(p).isEmpty());
    }

    @Test
    public void aWildcardAppIdDoesNotCoverTheGeneratedDocumentProvider() throws Exception {
        // A wildcard covers the bundle ID and still cannot sign this extension: it declares the
        // App Group it resolves its container from, and Apple does not offer App Groups on a
        // wildcard App ID. Passing it here would move the failure to Xcode, where the message
        // is about an entitlement rather than about the profile nobody supplied.
        Properties p = settings(profile("PROD", "ABCD1234.com.example.*"));
        p.setProperty("codename1.arg.ios.documentProvider.enabled", "true");
        List<IOSProvisioningPreflight.Problem> problems = checkGenerated(p);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).message, problems.get(0).message.contains("App Groups"));

        // A profile of its own settles it, wildcard app profile or not.
        p.setProperty("codename1.ios.appext.CN1Documents.provision", "/tmp/CN1Documents.mobileprovision");
        assertTrue(checkGenerated(p).isEmpty());
    }

    @Test
    public void anAppThatDoesNotPublishDocumentsIsNotChecked() throws Exception {
        Properties p = settings(profile("PROD", "ABCD1234.com.example.app"));
        assertTrue(checkGenerated(p).isEmpty());
        // Opting out generates no target, so there is nothing to sign and nothing to refuse.
        p.setProperty("codename1.arg.ios.documentProvider.enabled", "true");
        p.setProperty("codename1.arg.ios.documentProvider.extension", "false");
        assertTrue(checkGenerated(p).isEmpty());
    }

    @Test
    public void anUnreadableAppProfileIsNotReportedAsAnExtensionProblem() throws Exception {
        // check() reports that as itself; refusing the build here would name the wrong cause.
        Properties p = new Properties();
        p.setProperty("codename1.packageName", "com.example.app");
        p.setProperty("codename1.arg.ios.documentProvider.enabled", "true");
        assertTrue(checkGenerated(p).isEmpty());
    }

    // ---- the failure this exists to catch ----

    @Test
    public void extensionWithNoProfileOfItsOwnIsRefused() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "WalletUIExtension");
        List<IOSProvisioningPreflight.Problem> problems =
                check(settings(profile("HBZ_PROD_DISTRIBUTION", "ABCD1234.com.example.app")), ios);
        assertEquals(1, problems.size());
        IOSProvisioningPreflight.Problem problem = problems.get(0);
        assertTrue("must be fatal: " + problem.message, problem.fatal);
        assertTrue(problem.message.contains("WalletUIExtension"));
        assertTrue(problem.message.contains("com.example.app.WalletUIExtension"));
        assertTrue(problem.message.contains("HBZ_PROD_DISTRIBUTION"));
        // all three carriers, since which one fits depends on where the profile can live
        assertTrue(problem.message.contains("ios/app_extensions/WalletUIExtension/"));
        assertTrue(problem.message.contains(
                "codename1.ios.release.appext.WalletUIExtension.provision="));
        assertTrue(problem.message.contains(
                "codename1.arg.ios.release.appext.WalletUIExtension.provisioningURL"));
    }

    /** One message per extension: the Wallet pair is two App IDs, and both have to be made. */
    @Test
    public void everyUnsignableExtensionIsNamed() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "WalletUIExtension");
        extensionFolder(ios, "WalletNonUIExtension");
        assertEquals(2, check(settings(profile("Dist", "ABCD1234.com.example.app")), ios).size());
    }

    /** The debug build type names the debug setting -- the profiles differ per build type. */
    @Test
    public void debugBuildNamesTheDebugSetting() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        Properties p = new Properties();
        p.setProperty("codename1.packageName", "com.example.app");
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(false),
                profile("Dev", "ABCD1234.com.example.app").getAbsolutePath());
        List<IOSProvisioningPreflight.Problem> problems =
                IOSProvisioningPreflight.checkAppExtensions(p, false, ios);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).message.contains(
                "codename1.ios.debug.appext.MyExtension.provision="));
    }

    // ---- what must NOT be refused ----

    @Test
    public void profileInsideTheExtensionFolderSatisfiesIt() throws Exception {
        File ios = iosDir();
        File ext = extensionFolder(ios, "MyExtension");
        write(new File(ext, "MyExtension.mobileprovision"), "does not have to parse");
        assertTrue(check(settings(profile("Dist", "ABCD1234.com.example.app")), ios).isEmpty());
    }

    @Test
    public void provisionSettingSatisfiesIt() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        Properties p = settings(profile("Dist", "ABCD1234.com.example.app"));
        p.setProperty("codename1.ios.release.appext.MyExtension.provision", "/certs/ext.mobileprovision");
        assertTrue(check(p, ios).isEmpty());
    }

    @Test
    public void oppositeBuildTypeProfileDoesNotSatisfyIt() throws Exception {
        // A release build holding only the DEBUG-qualified profile. CN1BuildMojo's
        // resolveAppExtensionBuildTypeQualifiers promotes the matching qualifier to the plain key
        // and removes the other one without promoting it, so this build would reach the server
        // with no extension profile at all. Passing it here would move the failure to signing.
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        Properties p = settings(profile("Dist", "ABCD1234.com.example.app"));
        p.setProperty("codename1.ios.debug.appext.MyExtension.provision", "/certs/ext.mobileprovision");
        assertEquals(1, check(p, ios).size());
    }

    @Test
    public void generatedExtensionOppositeBuildTypeProfileDoesNotSatisfyIt() throws Exception {
        Properties p = settings(profile("PROD", "ABCD1234.com.example.app"));
        p.setProperty("codename1.arg.ios.documentProvider.enabled", "true");
        p.setProperty("codename1.arg.ios.debug.appext.CN1Documents.provisioningURL",
                "https://example.com/ext");
        assertEquals(1, checkGenerated(p).size());
    }

    @Test
    public void generatedExtensionMatchingBuildTypeProfileSatisfiesIt() throws Exception {
        Properties p = settings(profile("PROD", "ABCD1234.com.example.app"));
        p.setProperty("codename1.arg.ios.documentProvider.enabled", "true");
        p.setProperty("codename1.arg.ios.release.appext.CN1Documents.provisioningURL",
                "https://example.com/ext");
        assertTrue(checkGenerated(p).isEmpty());
    }

    @Test
    public void anExplicitlyBlankQualifierBeatsTheUnqualifiedFallback() throws Exception {
        // What the wizard writes when it could not produce a development profile: the unqualified
        // key keeps the DISTRIBUTION profile for older tooling, and the debug key is blanked. A
        // debug build that accepted the fallback would sign the extension with a distribution
        // profile and fail on the server, so the blank has to win here.
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        File appProfile = profile("Dist", "ABCD1234.com.example.app");
        Properties p = settings(appProfile);
        // The debug build needs an app profile of its own, or the check returns before it looks
        // at any extension.
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(false),
                appProfile.getAbsolutePath());
        p.setProperty("codename1.ios.appext.MyExtension.provision", "/certs/dist.mobileprovision");
        p.setProperty("codename1.ios.debug.appext.MyExtension.provision", "");
        assertEquals(1, IOSProvisioningPreflight.checkAppExtensions(p, false, ios).size());
        // The release build is unaffected: its own profile is there.
        assertTrue(IOSProvisioningPreflight.checkAppExtensions(p, true, ios).isEmpty());
    }

    @Test
    public void provisioningUrlHintSatisfiesIt() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        Properties p = settings(profile("Dist", "ABCD1234.com.example.app"));
        p.setProperty("codename1.arg.ios.appext.MyExtension.provisioningURL", "https://example.com/ext");
        assertTrue(check(p, ios).isEmpty());
    }

    /** A wildcard App ID really does sign the whole subtree, so this build works as it is. */
    @Test
    public void wildcardAppIdCoversTheExtension() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        assertTrue(check(settings(profile("Wildcard", "ABCD1234.com.example.*")), ios).isEmpty());
    }

    /** An overridden bundle id is what the target carries, so it is what has to be judged. */
    @Test
    public void overriddenBundleIdIsJudgedInsteadOfTheDefault() throws Exception {
        File ios = iosDir();
        File ext = extensionFolder(ios, "MyExtension");
        write(new File(ext, "buildSettings.properties"),
                "PRODUCT_BUNDLE_IDENTIFIER=com.example.app.renamed\n");
        List<IOSProvisioningPreflight.Problem> problems =
                check(settings(profile("Dist", "ABCD1234.com.example.app")), ios);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).message.contains("com.example.app.renamed"));
        assertFalse(problems.get(0).message.contains("com.example.app.MyExtension"));
    }

    /** A zipped extension is unzipped into the same folder, so a profile inside it counts. */
    @Test
    public void profileInsideAZippedExtensionSatisfiesIt() throws Exception {
        File ios = iosDir();
        File zip = new File(new File(ios, "app_extensions"), "MyExtension.zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        try {
            out.putNextEntry(new ZipEntry("MyExtension.mobileprovision"));
            out.write("profile".getBytes("UTF-8"));
            out.closeEntry();
        } finally {
            out.close();
        }
        assertTrue(check(settings(profile("Dist", "ABCD1234.com.example.app")), ios).isEmpty());
    }

    @Test
    public void zippedExtensionWithoutAProfileIsRefusedByItsTargetName() throws Exception {
        File ios = iosDir();
        File zip = new File(new File(ios, "app_extensions"), "MyExtension.zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        try {
            out.putNextEntry(new ZipEntry("Info.plist"));
            out.write("<plist/>".getBytes("UTF-8"));
            out.closeEntry();
        } finally {
            out.close();
        }
        List<IOSProvisioningPreflight.Problem> problems =
                check(settings(profile("Dist", "ABCD1234.com.example.app")), ios);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).message.contains("com.example.app.MyExtension"));
    }

    // ---- nothing to judge: never refuse ----

    @Test
    public void noAppExtensionsFolderIsSilent() throws Exception {
        File ios = tmp.newFolder("bare-ios");
        assertTrue(check(settings(profile("Dist", "ABCD1234.com.example.app")), ios).isEmpty());
    }

    @Test
    public void unreadableAppProfileIsLeftToTheProfileChecks() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        File garbage = tmp.newFile("garbage.mobileprovision");
        write(garbage, "not a profile");
        Properties p = new Properties();
        p.setProperty("codename1.packageName", "com.example.app");
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                garbage.getAbsolutePath());
        assertTrue(check(p, ios).isEmpty());
    }

    /** A profile with no application-identifier says nothing about what it can sign. */
    @Test
    public void profileWithoutAnAppIdIsNotEvidence() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        Properties p = new Properties();
        p.setProperty("codename1.packageName", "com.example.app");
        File noAppId = tmp.newFile("noappid.mobileprovision");
        String plist = HEAD
                + "<key>Name</key><string>NoAppId</string>\n"
                + "<key>UUID</key><string>0f7ac3c1-4d0e-4e8a-9d1f-8b6a2c5e7d90</string>\n"
                + "<key>ExpirationDate</key><date>2099-01-01T00:00:00Z</date>\n"
                + "<key>DeveloperCertificates</key><array><data>Zm9v</data></array>\n"
                + "<key>Entitlements</key><dict><key>get-task-allow</key><false/></dict>\n"
                + "</dict></plist>";
        write(noAppId, plist);
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                noAppId.getAbsolutePath());
        assertTrue(check(p, ios).isEmpty());
    }

    @Test
    public void unsetPackageNameIsNotJudged() throws Exception {
        File ios = iosDir();
        extensionFolder(ios, "MyExtension");
        Properties p = new Properties();
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                profile("Dist", "ABCD1234.com.example.app").getAbsolutePath());
        assertTrue(check(p, ios).isEmpty());
    }

    // ---- the matching rule itself ----

    @Test
    public void appIdMatchingFollowsApplesRule() {
        assertTrue(IOSProvisioningPreflight.profileCoversBundleId(
                "ABCD1234.com.example.app", "com.example.app"));
        assertFalse(IOSProvisioningPreflight.profileCoversBundleId(
                "ABCD1234.com.example.app", "com.example.app.Ext"));
        assertTrue(IOSProvisioningPreflight.profileCoversBundleId(
                "ABCD1234.com.example.*", "com.example.app.Ext"));
        assertTrue(IOSProvisioningPreflight.profileCoversBundleId(
                "ABCD1234.*", "anything.at.all"));
        // a prefix that is not a wildcard is not a match: com.example.appstore is a different app
        assertFalse(IOSProvisioningPreflight.profileCoversBundleId(
                "ABCD1234.com.example.app", "com.example.appstore"));
        // unknown answers are covered -- this gates a hard refusal
        assertTrue(IOSProvisioningPreflight.profileCoversBundleId(null, "com.example.app"));
        assertTrue(IOSProvisioningPreflight.profileCoversBundleId("", "com.example.app"));
    }
}
