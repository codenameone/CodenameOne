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

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The pre-flight exists because of a real pair of cloud build failures minutes apart on
 * one account: the first ran four minutes and died with {@code exportArchive Provisioning
 * profile "HBZ_PROD_DISTRIBUTION" is not an "iOS Ad Hoc" profile}; the next two died with
 * a bare {@code SAXParseException ... Premature end of file} that never named the profile,
 * after the profile was swapped for an unreadable file. Both are decidable from the file
 * on disk, before anything is uploaded.
 */
public class IOSProvisioningPreflightTest {

    private static final String HEAD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
            + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\"><dict>\n";

    /**
     * The shape of a real profile: the fields every Apple-issued one carries, which is what
     * tells a provisioning profile apart from any other plist the setting might point at.
     */
    private static String profile(String name, String expiry, String body) {
        return HEAD
                + "<key>Name</key><string>" + name + "</string>\n"
                + "<key>UUID</key><string>0f7ac3c1-4d0e-4e8a-9d1f-8b6a2c5e7d90</string>\n"
                + "<key>ExpirationDate</key><date>" + expiry + "</date>\n"
                + "<key>DeveloperCertificates</key><array><data>Zm9v</data></array>\n"
                + body
                + "</dict></plist>";
    }

    private static final String FUTURE = "2099-01-01T00:00:00Z";

    private static String appStore(String name) {
        return profile(name, FUTURE,
                "<key>Entitlements</key><dict><key>get-task-allow</key><false/></dict>\n");
    }

    private static String adHoc(String name) {
        return profile(name, FUTURE,
                "<key>ProvisionedDevices</key><array><string>abc123</string></array>\n"
                + "<key>Entitlements</key><dict><key>get-task-allow</key><false/></dict>\n");
    }

    private static String development(String name) {
        return profile(name, FUTURE,
                "<key>ProvisionedDevices</key><array><string>abc123</string></array>\n"
                + "<key>Entitlements</key><dict><key>get-task-allow</key><true/></dict>\n");
    }

    private static String enterprise(String name) {
        return profile(name, FUTURE,
                "<key>ProvisionsAllDevices</key><true/>\n"
                + "<key>Entitlements</key><dict><key>get-task-allow</key><false/></dict>\n");
    }

    /** Wraps a plist the way a real .mobileprovision does: plain text inside a CMS envelope. */
    private File write(String plist) throws Exception {
        byte[] payload = plist == null ? new byte[0] : plist.getBytes("UTF-8");
        byte[] wrapped = new byte[payload.length + 24];
        for (int i = 0; i < 16; i++) {
            wrapped[i] = (byte) (0x80 + i);
        }
        System.arraycopy(payload, 0, wrapped, 16, payload.length);
        return writeRaw(plist == null ? new byte[0] : wrapped);
    }

    private File writeRaw(byte[] contents) throws Exception {
        File f = File.createTempFile("preflight", ".mobileprovision");
        f.deleteOnExit();
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(contents);
        } finally {
            out.close();
        }
        return f;
    }

    private Properties settings(File profile, boolean release, String method) {
        Properties p = new Properties();
        if (profile != null) {
            p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(release),
                    profile.getAbsolutePath());
        }
        if (method != null) {
            p.setProperty("codename1.arg.ios." + (release ? "release" : "debug") + ".distributionMethod", method);
        }
        return p;
    }

    private static List<IOSProvisioningPreflight.Problem> check(Properties p, boolean release) {
        return IOSProvisioningPreflight.check(p, release, new Date());
    }

    private static void assertFatal(List<IOSProvisioningPreflight.Problem> problems, String... expectedFragments) {
        assertFalse("expected a problem", problems.isEmpty());
        IOSProvisioningPreflight.Problem first = problems.get(0);
        assertTrue("expected a fatal problem, got: " + first.message, first.fatal);
        for (String fragment : expectedFragments) {
            assertTrue("expected the message to mention '" + fragment + "', got: " + first.message,
                    first.message.contains(fragment));
        }
    }

    // ---- the two failures that prompted this ----

    /** The 17:30 failure: ad-hoc requested, App Store profile supplied. */
    @Test
    public void appStoreProfileCannotExportAsAdHoc() throws Exception {
        Properties p = settings(write(appStore("HBZ_PROD_DISTRIBUTION")), true, "ad-hoc");
        assertFatal(check(p, true), "HBZ_PROD_DISTRIBUTION", "App Store distribution",
                "ad-hoc", "ios.release.distributionMethod=app-store");
    }

    /** The 17:37 and 17:42 failures: the swapped-in profile decoded to nothing. */
    @Test
    public void unreadableProfileIsNamedAsSuch() throws Exception {
        File garbage = writeRaw("not a provisioning profile at all".getBytes("UTF-8"));
        assertFatal(check(settings(garbage, true, null), true),
                garbage.getAbsolutePath(), "not a valid .mobileprovision file");
    }

    @Test
    public void emptyProfileIsNamedAsSuch() throws Exception {
        File empty = writeRaw(new byte[0]);
        assertFatal(check(settings(empty, true, null), true), "is empty (0 bytes)");
    }

    @Test
    public void missingProfileIsNamedAsSuch() throws Exception {
        Properties p = new Properties();
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                new File(System.getProperty("java.io.tmpdir"), "nope.mobileprovision").getAbsolutePath());
        assertFatal(check(p, true), "was not found at", "nope.mobileprovision");
    }

    @Test
    public void expiredProfileIsRefused() throws Exception {
        Calendar past = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        past.add(Calendar.YEAR, -1);
        String expiry = String.format("%tFT%<tTZ", past);
        File f = write(profile("Old Profile", expiry,
                "<key>Entitlements</key><dict><key>get-task-allow</key><false/></dict>\n"));
        assertFatal(check(settings(f, true, null), true), "Old Profile", "expired on");
    }

    // ---- the combinations that must NOT be refused ----

    @Test
    public void matchingReleaseProfilePasses() throws Exception {
        assertTrue(check(settings(write(appStore("Store")), true, "app-store"), true).isEmpty());
        assertTrue(check(settings(write(adHoc("AdHoc")), true, "ad-hoc"), true).isEmpty());
    }

    @Test
    public void matchingDebugProfilePasses() throws Exception {
        assertTrue(check(settings(write(development("Dev")), false, null), false).isEmpty());
    }

    /** Release defaults to app-store, debug to development -- exactly as the server resolves it. */
    @Test
    public void defaultsMatchTheServer() {
        Properties empty = new Properties();
        assertEquals("app-store", IOSProvisioningPreflight.effectiveDistributionMethod(empty, true));
        assertEquals("development", IOSProvisioningPreflight.effectiveDistributionMethod(empty, false));

        Properties shared = new Properties();
        shared.setProperty("codename1.arg.ios.distributionMethod", "enterprise");
        assertEquals("enterprise", IOSProvisioningPreflight.effectiveDistributionMethod(shared, true));

        // the build-type-qualified hint wins over the shared one
        shared.setProperty("codename1.arg.ios.release.distributionMethod", "ad-hoc");
        assertEquals("ad-hoc", IOSProvisioningPreflight.effectiveDistributionMethod(shared, true));
        assertEquals("enterprise", IOSProvisioningPreflight.effectiveDistributionMethod(shared, false));
    }

    /**
     * An in-house profile is the one case Xcode accepts for more than one method, so a
     * mismatch there warns rather than blocking a build that would have worked.
     */
    @Test
    public void enterpriseMismatchOnlyWarns() throws Exception {
        List<IOSProvisioningPreflight.Problem> problems =
                check(settings(write(enterprise("InHouse")), true, "app-store"), true);
        assertEquals(1, problems.size());
        assertFalse("an enterprise mismatch must not block the build", problems.get(0).fatal);
    }

    /** No profile configured is the server's call to make, not a local refusal. */
    @Test
    public void absentSettingWarnsButDoesNotBlock() {
        List<IOSProvisioningPreflight.Problem> problems = check(new Properties(), true);
        assertEquals(1, problems.size());
        assertFalse(problems.get(0).fatal);
    }

    // ---- classification ----

    @Test
    public void classifiesEveryProfileKind() throws Exception {
        assertEquals("app-store", IOSProvisioningPreflight.parse(appStore("a").getBytes("UTF-8")).type);
        assertEquals("ad-hoc", IOSProvisioningPreflight.parse(adHoc("a").getBytes("UTF-8")).type);
        assertEquals("development", IOSProvisioningPreflight.parse(development("a").getBytes("UTF-8")).type);
        assertEquals("enterprise", IOSProvisioningPreflight.parse(enterprise("a").getBytes("UTF-8")).type);
    }

    @Test
    public void readsTheProfileNameAndExpiry() throws Exception {
        IOSProvisioningPreflight.Profile parsed =
                IOSProvisioningPreflight.parse(appStore("My Profile").getBytes("UTF-8"));
        assertNotNull(parsed);
        assertEquals("My Profile", parsed.name);
        assertNotNull(parsed.expirationDate);
        assertTrue(parsed.expirationDate.after(new Date()));
    }

    // ---- Ant placeholders in the path ----

    /**
     * A profile path is routinely written as {@code ${user.home}/certs/dev.mobileprovision},
     * and Ant expands it before the build task sees it. Handing the literal string to
     * {@code new File} would report a perfectly good profile as missing and refuse a build
     * that works today.
     */
    @Test
    public void expandsAPlaceholderInTheProfilePath() throws Exception {
        File profile = write(appStore("Store"));
        Properties p = new Properties();
        p.setProperty("certs.dir", profile.getParent());
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                "${certs.dir}/" + profile.getName());
        p.setProperty("codename1.arg.ios.release.distributionMethod", "app-store");
        assertTrue("an expandable path must be read, not reported missing",
                check(p, true).isEmpty());
    }

    /** System properties are part of that context -- {@code user.home} lives there. */
    @Test
    public void expandsSystemPropertiesToo() {
        assertEquals(System.getProperty("user.home") + "/certs/dev.mobileprovision",
                IOSProvisioningPreflight.resolvePlaceholders(
                        "${user.home}/certs/dev.mobileprovision", new Properties()));
    }

    /** A path this code cannot expand is a path it may not judge. */
    @Test
    public void aPathItCannotResolveIsNotRefused() {
        Properties p = new Properties();
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true),
                "${some.ant.property.only.ant.knows}/dev.mobileprovision");
        assertTrue("an unresolvable placeholder must not fail the build",
                check(p, true).isEmpty());
        assertTrue(IOSProvisioningPreflight.checkProfileFile(p, true, new Date()).isEmpty());
    }

    /** A self-referential property must not spin. */
    @Test
    public void resolutionTerminates() {
        Properties p = new Properties();
        p.setProperty("a", "${a}");
        assertEquals("${a}", IOSProvisioningPreflight.resolvePlaceholders("${a}", p));
    }

    @Test
    public void leavesAnOrdinaryPathAlone() {
        assertEquals("/certs/dev.mobileprovision",
                IOSProvisioningPreflight.resolvePlaceholders("/certs/dev.mobileprovision", new Properties()));
    }

    // ---- which builds this applies to ----

    /**
     * {@code cn1:buildIosOnDeviceDebug} selects {@code ios-on-device-debug}, whose buildxml
     * target submits {@code codename1.ios.debug.provision} to the build server exactly like
     * {@code ios-device}. Missing it meant a bad profile still burned a cloud build slot on
     * that flow.
     */
    @Test
    public void appliesToEveryCloudIOSDeviceBuild() {
        assertTrue(IOSProvisioningPreflight.appliesTo("ios", "ios-device"));
        assertTrue(IOSProvisioningPreflight.appliesTo("ios", "ios-device-release"));
        assertTrue(IOSProvisioningPreflight.appliesTo("ios", "ios-on-device-debug"));
    }

    /** ...and to nothing the build server does not sign with the app's iOS profile. */
    @Test
    public void doesNotApplyToLocalOrNonIOSBuilds() {
        assertFalse("a local Xcode project is signed later, or not at all",
                IOSProvisioningPreflight.appliesTo("ios", "ios-source"));
        assertFalse("native Mac rides platform=ios with a different identity",
                IOSProvisioningPreflight.appliesTo("ios", "mac-os-x-native"));
        assertFalse(IOSProvisioningPreflight.appliesTo("javascript", "javascript"));
        assertFalse(IOSProvisioningPreflight.appliesTo("android", "android-device"));
        assertFalse(IOSProvisioningPreflight.appliesTo(null, "ios-device"));
        assertFalse(IOSProvisioningPreflight.appliesTo("ios", null));
    }

    /** On-device debug signs with the DEBUG profile, like every non-release target. */
    @Test
    public void onlyTheReleaseTargetUsesTheReleaseProfile() {
        assertTrue(IOSProvisioningPreflight.isReleaseTarget("ios-device-release"));
        assertFalse(IOSProvisioningPreflight.isReleaseTarget("ios-device"));
        assertFalse(IOSProvisioningPreflight.isReleaseTarget("ios-on-device-debug"));
        assertFalse(IOSProvisioningPreflight.isReleaseTarget(null));

        assertEquals("codename1.ios.debug.provision",
                IOSProvisioningPreflight.provisioningProfileSettingKey(
                        IOSProvisioningPreflight.isReleaseTarget("ios-on-device-debug")));
    }

    // ---- the mismatch decision waits for the merged settings ----

    /**
     * A CN1Lib can supply {@code codename1.arg.ios.release.distributionMethod} through its
     * appended/required properties, and the mojo merges those only later. Judging the profile
     * kind before that merge would refuse an Ad Hoc profile against the release default of
     * app-store -- a build the merge was about to make correct. So the early pass reads the
     * file and stops short of the comparison.
     */
    @Test
    public void earlyPassDoesNotJudgeTheMethodALibraryCouldStillSupply() throws Exception {
        Properties beforeMerge = settings(write(adHoc("AdHoc")), true, null);
        assertTrue("the early pass must not refuse on a method the merge can still change",
                IOSProvisioningPreflight.checkProfileFile(beforeMerge, true, new Date()).isEmpty());

        // the same settings, once the library's property has been merged in: still fine
        Properties afterMerge = new Properties();
        afterMerge.putAll(beforeMerge);
        afterMerge.setProperty("codename1.arg.ios.release.distributionMethod", "ad-hoc");
        assertTrue("a library-supplied method that matches must not be refused",
                check(afterMerge, true).isEmpty());
    }

    /** The early pass still fails what the merge cannot fix. */
    @Test
    public void earlyPassStillCatchesAnUnusableFile() throws Exception {
        File garbage = writeRaw("not a profile".getBytes("UTF-8"));
        List<IOSProvisioningPreflight.Problem> problems =
                IOSProvisioningPreflight.checkProfileFile(settings(garbage, true, null), true, new Date());
        assertFalse(problems.isEmpty());
        assertTrue(problems.get(0).fatal);

        File missing = new File(System.getProperty("java.io.tmpdir"), "absent.mobileprovision");
        Properties p = new Properties();
        p.setProperty(IOSProvisioningPreflight.provisioningProfileSettingKey(true), missing.getAbsolutePath());
        assertTrue(IOSProvisioningPreflight.checkProfileFile(p, true, new Date()).get(0).fatal);
    }

    /** No profile configured yet is not the early pass's business -- the merge may supply one. */
    @Test
    public void earlyPassIsSilentWhenNoProfileIsConfiguredYet() {
        assertTrue(IOSProvisioningPreflight.checkProfileFile(new Properties(), true, new Date()).isEmpty());
    }

    /** The merged pass is the one that catches the real mismatch. */
    @Test
    public void mergedPassCatchesTheMismatch() throws Exception {
        Properties merged = settings(write(appStore("HBZ_PROD_DISTRIBUTION")), true, "ad-hoc");
        assertFatal(check(merged, true), "HBZ_PROD_DISTRIBUTION", "App Store distribution");
    }

    // ---- it has to actually be a profile ----

    /**
     * An ordinary plist parses perfectly well. {@code Info.plist} has no device list, so it
     * would have been classified as an App Store profile -- and a release build, whose
     * default method is app-store, would have passed this check and uploaded a file that
     * cannot provision or sign anything.
     */
    @Test
    public void anOrdinaryPlistIsNotAProfile() throws Exception {
        String infoPlist = HEAD
                + "<key>CFBundleName</key><string>MyApp</string>\n"
                + "<key>CFBundleIdentifier</key><string>com.example.myapp</string>\n"
                + "<key>CFBundleVersion</key><string>1.0</string>\n"
                + "</dict></plist>";
        assertNull("an Info.plist is not a provisioning profile",
                IOSProvisioningPreflight.parse(infoPlist.getBytes("UTF-8")));

        File f = writeRaw(infoPlist.getBytes("UTF-8"));
        assertFatal(check(settings(f, true, null), true),
                "not a valid .mobileprovision file");
    }

    /** A profile missing the fields every Apple-issued one carries is not one either. */
    @Test
    public void aPlistWithoutTheProfileFieldsIsNotAProfile() throws Exception {
        String half = HEAD
                + "<key>Name</key><string>Looks Like A Profile</string>\n"
                + "<key>ExpirationDate</key><date>" + FUTURE + "</date>\n"
                + "</dict></plist>";
        assertNull("UUID, Entitlements and DeveloperCertificates are all required",
                IOSProvisioningPreflight.parse(half.getBytes("UTF-8")));
    }

    /** ...and a real one still is. */
    @Test
    public void aRealProfileStillParses() throws Exception {
        IOSProvisioningPreflight.Profile parsed =
                IOSProvisioningPreflight.parse(appStore("Store").getBytes("UTF-8"));
        assertNotNull(parsed);
        assertEquals("Store", parsed.name);
    }

    // ---- a profile is untrusted input ----

    /**
     * Nothing here verifies the profile's CMS signature, and a profile can arrive from
     * anyone. A crafted one must not be able to read a local file during someone's build --
     * the DOCTYPE has to stay legal (every real plist declares one), but the entity in its
     * internal subset must not be resolved. Disabling only {@code load-external-dtd} would
     * leave this open.
     */
    @Test
    public void doesNotResolveExternalEntitiesFromACraftedProfile() throws Exception {
        File secret = File.createTempFile("preflight-secret", ".txt");
        secret.deleteOnExit();
        OutputStream out = new FileOutputStream(secret);
        try {
            out.write("TOP-SECRET-KEYCHAIN-CONTENTS".getBytes("UTF-8"));
        } finally {
            out.close();
        }

        String attack = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE plist [ <!ENTITY xxe SYSTEM \"file://" + secret.getAbsolutePath() + "\"> ]>\n"
                + "<plist version=\"1.0\"><dict>\n"
                + "<key>Name</key><string>&xxe;</string>\n"
                + "<key>ExpirationDate</key><date>" + FUTURE + "</date>\n"
                + "<key>Entitlements</key><dict><key>get-task-allow</key><false/></dict>\n"
                + "</dict></plist>";

        String name;
        try {
            IOSProvisioningPreflight.Profile parsed = IOSProvisioningPreflight.parse(attack.getBytes("UTF-8"));
            name = parsed == null || parsed.name == null ? "" : parsed.name;
        } catch (Exception refusedOutright) {
            name = "";
        }
        assertFalse("the profile must not be able to read a local file, got: " + name,
                name.contains("TOP-SECRET-KEYCHAIN-CONTENTS"));

        // and the same profile, run through the full check, must not leak it into the message either
        Properties p = settings(writeRaw(attack.getBytes("UTF-8")), true, "ad-hoc");
        for (IOSProvisioningPreflight.Problem problem : check(p, true)) {
            assertFalse("no message may carry the file's contents: " + problem.message,
                    problem.message.contains("TOP-SECRET-KEYCHAIN-CONTENTS"));
        }
    }

    /** A real profile still parses with the hardened parser -- the DOCTYPE stays legal. */
    @Test
    public void hardenedParserStillReadsARealProfile() throws Exception {
        IOSProvisioningPreflight.Profile parsed =
                IOSProvisioningPreflight.parse(development("Dev Profile").getBytes("UTF-8"));
        assertNotNull(parsed);
        assertEquals("Dev Profile", parsed.name);
        assertEquals("development", parsed.type);
        assertNotNull(parsed.expirationDate);
    }

    @Test
    public void reportsNoPlistWhenThereIsNone() throws Exception {
        assertNull(IOSProvisioningPreflight.extractEmbeddedPlist(new byte[0]));
        assertNull(IOSProvisioningPreflight.extractEmbeddedPlist("nothing".getBytes("UTF-8")));
        // an opening tag with no close is a truncated file, not a profile
        assertNull(IOSProvisioningPreflight.extractEmbeddedPlist("<?xml version=\"1.0\"?><plist>".getBytes("UTF-8")));
    }
}
