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
package com.codename1.certificatewizard;

import com.codename1.certificatewizard.api.MockSigningService;
import com.codename1.certificatewizard.api.SigningService;
import com.codename1.certificatewizard.api.SigningState;
import com.codename1.certificatewizard.api.WizardDecisions;
import com.codename1.certificatewizard.project.ProjectBinding;
import com.codename1.certificatewizard.project.SigningAssetInstaller;
import com.codename1.ui.Display;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CertificateWizardModelTest {
    @BeforeAll
    static void initDisplay() {
        if (Display.getInstance() == null || !Display.isInitialized()) {
            Display.init(null);
        }
    }

    @Test
    void profileRulesMatchAppleProfileTypes() {
        assertFalse(WizardDecisions.profileRequiresDevices("IOS_APP_STORE"));
        assertTrue(WizardDecisions.profileRequiresDevices("IOS_APP_ADHOC"));
        assertTrue(WizardDecisions.profileRequiresDevices("IOS_APP_DEVELOPMENT"));
        assertTrue(WizardDecisions.profileRequiresDevices("MAC_APP_DEVELOPMENT"));
        assertEquals("IOS_DISTRIBUTION", WizardDecisions.requiredCertificateType("IOS_APP_STORE"));
        assertEquals("IOS_DEVELOPMENT", WizardDecisions.requiredCertificateType("IOS_APP_DEVELOPMENT"));
        assertEquals("MAC_APP_DISTRIBUTION", WizardDecisions.requiredCertificateType("MAC_APP_STORE"));
        assertEquals("DEVELOPER_ID_APPLICATION", WizardDecisions.requiredCertificateType("MAC_APP_DIRECT"));
    }

    @Test
    void compatibleCertificatesMustBeExportableForAutoSetupReuse() {
        long now = System.currentTimeMillis();
        List<SigningState.Certificate> certs = new ArrayList<SigningState.Certificate>();
        certs.add(new SigningState.Certificate(1L, "APPLE_EXISTING", "IOS_DISTRIBUTION",
                "Existing App Store Certificate", "SER1", now + 300L * 86400000L, "ACTIVE", false));
        certs.add(new SigningState.Certificate(2L, "APPLE_EXPORTABLE", "IOS_DISTRIBUTION",
                "Exportable App Store Certificate", "SER2", now + 300L * 86400000L, "ACTIVE", true));
        SigningState state = new SigningState(new SigningState.Credential(true, "KEY", "ISSUER"),
                certs, null, null, null, null, null);

        List<SigningState.Certificate> compatible = WizardDecisions.compatibleCertificates(state, "IOS_APP_STORE");

        assertEquals(1, compatible.size());
        assertEquals("APPLE_EXPORTABLE", compatible.get(0).appleCertId());
    }

    @Test
    void createProfileValidationRequiresDevicesOnlyWhenNeeded() {
        List<String> certs = new ArrayList<String>();
        certs.add("CERT1");
        assertTrue(WizardDecisions.canCreateProfile("IOS_APP_STORE", "BID1", certs, null, "Store"));
        assertFalse(WizardDecisions.canCreateProfile("IOS_APP_ADHOC", "BID1", certs, null, "Adhoc"));
        List<String> devices = new ArrayList<String>();
        devices.add("DEV1");
        assertTrue(WizardDecisions.canCreateProfile("IOS_APP_ADHOC", "BID1", certs, devices, "Adhoc"));
        assertFalse(WizardDecisions.canCreateProfile("IOS_APP_STORE", "BID1", certs, devices, ""));
    }

    /// Creating a profile sends the certificate's Apple ID and nothing else, so the picker must
    /// not apply the export-time private-key rule that compatibleCertificates does -- that hid a
    /// valid certificate synced from Apple and told its owner to make a second one. The type
    /// match is a different matter and stays: Apple rejects the wrong kind outright.
    @Test
    void profileCertificateChoicesKeepCertificatesWithNoStoredPrivateKey() {
        long now = System.currentTimeMillis();
        List<SigningState.Certificate> certs = new ArrayList<SigningState.Certificate>();
        certs.add(new SigningState.Certificate(1L, "APPLE_NO_KEY", "IOS_DISTRIBUTION",
                "Synced App Store Certificate", "SER1", now + 300L * 86400000L, "ACTIVE", false));
        certs.add(new SigningState.Certificate(2L, "APPLE_EXPORTABLE", "IOS_DISTRIBUTION",
                "Exportable App Store Certificate", "SER2", now + 300L * 86400000L, "ACTIVE", true));
        certs.add(new SigningState.Certificate(3L, "APPLE_REVOKED", "IOS_DISTRIBUTION",
                "Revoked", "SER3", now + 300L * 86400000L, "REVOKED", true));
        certs.add(new SigningState.Certificate(4L, "APPLE_MAC", "MAC_APP_DISTRIBUTION",
                "Mac App Store", "SER4", now + 300L * 86400000L, "ACTIVE", true));
        SigningState state = new SigningState(new SigningState.Credential(true, "KEY", "ISSUER"),
                certs, null, null, null, null, null);

        List<SigningState.Certificate> choices = WizardDecisions.profileCertificateChoices(state, "IOS_APP_STORE");

        assertEquals(2, choices.size(), "both active iOS distribution certificates are offered");
        assertEquals("APPLE_NO_KEY", choices.get(0).appleCertId());
        assertEquals("APPLE_EXPORTABLE", choices.get(1).appleCertId());
        // and the export path is unchanged, because THAT one really does need the key
        assertEquals(1, WizardDecisions.compatibleCertificates(state, "IOS_APP_STORE").size());
    }

    /// What the picker offers and what automatic setup sends have to be the same set, or a
    /// "select all" builds a request naming a disabled or wrong-platform device, which Apple
    /// rejects whole.
    @Test
    void onlyEnabledDevicesOfTheProfilePlatformAreOffered() {
        List<SigningState.Device> devices = new ArrayList<SigningState.Device>();
        devices.add(new SigningState.Device("DEV_1", "QA iPhone", "UDID1", "IOS", "ENABLED"));
        devices.add(new SigningState.Device("DEV_2", "Old iPad", "UDID2", "IOS", "DISABLED"));
        devices.add(new SigningState.Device("DEV_3", "Bench Mac", "UDID3", "MAC_OS", "ACTIVE"));
        SigningState state = new SigningState(new SigningState.Credential(true, "KEY", "ISSUER"),
                null, null, devices, null, null, null);

        List<SigningState.Device> ios = WizardDecisions.usableDevices(state, "IOS_APP_DEVELOPMENT");
        assertEquals(1, ios.size());
        assertEquals("DEV_1", ios.get(0).id());

        List<SigningState.Device> mac = WizardDecisions.usableDevices(state, "MAC_APP_DEVELOPMENT");
        assertEquals(1, mac.size());
        assertEquals("DEV_3", mac.get(0).id());

        assertEquals("IOS", WizardDecisions.devicePlatformFor("IOS_APP_ADHOC"));
        assertEquals("MAC_OS", WizardDecisions.devicePlatformFor("MAC_APP_DEVELOPMENT"));
        assertEquals("MAC_OS", WizardDecisions.devicePlatformFor("MAC_CATALYST_APP_DEVELOPMENT"));
        assertFalse(WizardDecisions.isUsableDevice(null, "IOS_APP_DEVELOPMENT"));
    }

    /// The platform field is an untyped string in the API, so an unanticipated value must not
    /// empty the picker and make a profile type uncreatable -- the rule excludes the known wrong
    /// platform rather than demanding the known right one.
    @Test
    void anUnknownDevicePlatformIsOfferedRatherThanHidden() {
        List<SigningState.Device> devices = new ArrayList<SigningState.Device>();
        devices.add(new SigningState.Device("DEV_1", "Universal", "UDID1", "UNIVERSAL", "ENABLED"));
        devices.add(new SigningState.Device("DEV_2", "No platform", "UDID2", null, "ENABLED"));
        devices.add(new SigningState.Device("DEV_3", "Blank platform", "UDID3", "  ", "ENABLED"));
        SigningState state = new SigningState(new SigningState.Credential(true, "KEY", "ISSUER"),
                null, null, devices, null, null, null);

        assertEquals(3, WizardDecisions.usableDevices(state, "IOS_APP_DEVELOPMENT").size());
        assertEquals(3, WizardDecisions.usableDevices(state, "MAC_APP_DEVELOPMENT").size());
    }

    /// Apple Development and Apple Distribution supersede the platform-specific certificate types
    /// and are valid wherever those are. An exact type match sent an account holding only those to
    /// generate a redundant certificate. The widening stops there: two other certificate types are
    /// also "distribution" and neither signs what the other is for.
    @Test
    void appleGenericCertificateTypesSatisfyTheirPlatformSpecificRequirement() {
        assertTrue(WizardDecisions.certificateTypeSatisfies("IOS_DEVELOPMENT", "DEVELOPMENT"));
        assertTrue(WizardDecisions.certificateTypeSatisfies("MAC_APP_DEVELOPMENT", "DEVELOPMENT"));
        assertTrue(WizardDecisions.certificateTypeSatisfies("IOS_DISTRIBUTION", "DISTRIBUTION"));
        assertTrue(WizardDecisions.certificateTypeSatisfies("MAC_APP_DISTRIBUTION", "DISTRIBUTION"));
        assertTrue(WizardDecisions.certificateTypeSatisfies("IOS_DISTRIBUTION", "IOS_DISTRIBUTION"));

        assertFalse(WizardDecisions.certificateTypeSatisfies("DEVELOPER_ID_APPLICATION", "DISTRIBUTION"),
                "Developer ID is not what Apple Distribution replaced");
        assertFalse(WizardDecisions.certificateTypeSatisfies("IOS_DISTRIBUTION", "MAC_INSTALLER_DISTRIBUTION"),
                "an installer certificate signs an installer, not an app");
        assertFalse(WizardDecisions.certificateTypeSatisfies("IOS_DISTRIBUTION", "DEVELOPMENT"));
        assertFalse(WizardDecisions.certificateTypeSatisfies("IOS_DEVELOPMENT", "DISTRIBUTION"));
        assertFalse(WizardDecisions.certificateTypeSatisfies(null, "DISTRIBUTION"));
        assertFalse(WizardDecisions.certificateTypeSatisfies("IOS_DISTRIBUTION", null));
    }

    /// The picker and automatic setup read one predicate, so an account can never be offered a
    /// certificate the automatic path would then refuse, or the other way round.
    @Test
    void aGenericCertificateReachesBothThePickerAndAutomaticSetup() {
        long now = System.currentTimeMillis();
        List<SigningState.Certificate> certs = new ArrayList<SigningState.Certificate>();
        certs.add(new SigningState.Certificate(1L, "APPLE_GENERIC", "DISTRIBUTION",
                "Apple Distribution", "SER1", now + 300L * 86400000L, "ACTIVE", true));
        certs.add(new SigningState.Certificate(2L, "APPLE_INSTALLER", "MAC_INSTALLER_DISTRIBUTION",
                "Mac Installer", "SER2", now + 300L * 86400000L, "ACTIVE", true));
        SigningState state = new SigningState(new SigningState.Credential(true, "KEY", "ISSUER"),
                certs, null, null, null, null, null);

        List<SigningState.Certificate> offered = WizardDecisions.profileCertificateChoices(state, "IOS_APP_STORE");
        assertEquals(1, offered.size());
        assertEquals("APPLE_GENERIC", offered.get(0).appleCertId());

        List<SigningState.Certificate> auto = WizardDecisions.compatibleCertificates(state, "IOS_APP_STORE");
        assertEquals(1, auto.size());
        assertEquals("APPLE_GENERIC", auto.get(0).appleCertId());
    }

    /// A device selection outlives the profile type it was made under. Once the picker started
    /// hiding devices of the wrong platform, a stale selection became invisible AND still counted:
    /// canCreateProfile saw the requirement met and the request named iPhones in a Mac profile.
    @Test
    void deviceSelectionDoesNotSurviveAChangeOfProfilePlatform() {
        List<SigningState.Device> devices = new ArrayList<SigningState.Device>();
        devices.add(new SigningState.Device("DEV_1", "QA iPhone", "UDID1", "IOS", "ENABLED"));
        devices.add(new SigningState.Device("DEV_2", "Retired iPhone", "UDID2", "IOS", "DISABLED"));
        devices.add(new SigningState.Device("DEV_3", "Bench Mac", "UDID3", "MAC_OS", "ENABLED"));
        SigningState state = new SigningState(new SigningState.Credential(true, "KEY", "ISSUER"),
                null, null, devices, null, null, null);
        List<String> selected = new ArrayList<String>();
        selected.add("DEV_1");
        selected.add("DEV_3");

        List<String> forIos = WizardDecisions.retainUsableDevices(state, "IOS_APP_DEVELOPMENT", selected);
        assertEquals(1, forIos.size());
        assertEquals("DEV_1", forIos.get(0));

        List<String> forMac = WizardDecisions.retainUsableDevices(state, "MAC_APP_DEVELOPMENT", selected);
        assertEquals(1, forMac.size());
        assertEquals("DEV_3", forMac.get(0));

        // and a type that names no devices at all must not carry one along
        assertTrue(WizardDecisions.retainUsableDevices(state, "IOS_APP_STORE", selected).isEmpty());
        assertTrue(WizardDecisions.retainUsableDevices(state, "IOS_APP_DEVELOPMENT", null).isEmpty());

        // the whole point: only a selection that survives may satisfy the create check
        List<String> certs = new ArrayList<String>();
        certs.add("CERT1");
        assertTrue(WizardDecisions.canCreateProfile("MAC_APP_DEVELOPMENT", "BID1", certs, forMac, "N"),
                "the Mac device that survived the switch is a real selection");
        List<String> onlyIos = new ArrayList<String>();
        onlyIos.add("DEV_1");
        assertFalse(WizardDecisions.canCreateProfile("MAC_APP_DEVELOPMENT", "BID1", certs,
                        WizardDecisions.retainUsableDevices(state, "MAC_APP_DEVELOPMENT", onlyIos), "N"),
                "a selection of iPhones must not satisfy a Mac profile's device requirement");
    }

    /// Sending someone to the certificate dialog to satisfy requiredCertificateType only helps if
    /// that dialog can actually produce the type. MAC_APP_DEVELOPMENT could not be, so the Mac
    /// Development profile's only suggested remedy led straight back to the disabled form.
    @Test
    void everyRequiredCertificateTypeCanBeGenerated() {
        String[] profileTypes = {"IOS_APP_STORE", "IOS_APP_ADHOC", "IOS_APP_DEVELOPMENT",
                "MAC_APP_STORE", "MAC_APP_DIRECT", "MAC_APP_DEVELOPMENT",
                "MAC_CATALYST_APP_STORE", "MAC_CATALYST_APP_DIRECT", "MAC_CATALYST_APP_DEVELOPMENT"};
        // the very array the dialog builds its segments from, so this cannot drift away from it
        List<String> offered = java.util.Arrays.asList(WizardDecisions.GENERATABLE_CERTIFICATE_TYPES);
        assertEquals(WizardDecisions.GENERATABLE_CERTIFICATE_TYPES.length,
                WizardDecisions.GENERATABLE_CERTIFICATE_LABELS.length, "every type needs a label");
        for (String profileType : profileTypes) {
            String required = WizardDecisions.requiredCertificateType(profileType);
            assertTrue(offered.contains(required),
                    profileType + " requires " + required + ", which the certificate dialog must offer");
        }
    }

    /// The create action is disabled until the request is complete, and the reporter of issue
    /// #5636 could not tell which of four sections was the incomplete one. The message names the
    /// FIRST thing missing, reading down the dialog, so following it always moves forward.
    @Test
    void missingProfileInputIsNamedInDialogOrder() {
        List<String> certs = new ArrayList<String>();
        List<String> devices = new ArrayList<String>();
        assertTrue(WizardDecisions.describeMissingProfileInput(null, null, certs, devices, "")
                .contains("profile type"));
        assertTrue(WizardDecisions.describeMissingProfileInput("IOS_APP_STORE", null, certs, devices, "")
                .contains("bundle ID"));
        assertTrue(WizardDecisions.describeMissingProfileInput("IOS_APP_STORE", "BID1", certs, devices, "")
                .contains("certificate"));
        certs.add("CERT1");
        assertTrue(WizardDecisions.describeMissingProfileInput("IOS_APP_STORE", "BID1", certs, devices, "")
                .contains("name"));
        assertTrue(WizardDecisions.describeMissingProfileInput("IOS_APP_ADHOC", "BID1", certs, devices, "Adhoc")
                .contains("device"));
        devices.add("DEV1");
        assertNull(WizardDecisions.describeMissingProfileInput("IOS_APP_ADHOC", "BID1", certs, devices, "Adhoc"));
        assertNull(WizardDecisions.describeMissingProfileInput("IOS_APP_STORE", "BID1", certs, null, "Store"));
    }

    /// Nothing may report a blocker while canCreateProfile says the request is complete, or the
    /// dialog would explain a button that is already enabled.
    @Test
    void missingProfileInputAgreesWithCreateProfileValidation() {
        List<String> certs = new ArrayList<String>();
        certs.add("CERT1");
        List<String> devices = new ArrayList<String>();
        devices.add("DEV1");
        String[] types = {"IOS_APP_STORE", "IOS_APP_ADHOC", "IOS_APP_DEVELOPMENT", "MAC_APP_STORE",
                "MAC_APP_DIRECT", "MAC_APP_DEVELOPMENT", null};
        String[] bundles = {null, "BID1"};
        String[] names = {"", "Profile"};
        for (String type : types) {
            for (String bundle : bundles) {
                for (String name : names) {
                    for (List<String> devs : java.util.Arrays.asList(new ArrayList<String>(), devices)) {
                        boolean ok = WizardDecisions.canCreateProfile(type, bundle, certs, devs, name);
                        String missing = WizardDecisions.describeMissingProfileInput(type, bundle, certs, devs, name);
                        assertEquals(ok, missing == null, type + "/" + bundle + "/" + name + "/" + devs.size());
                    }
                }
            }
        }
    }

    @Test
    void projectBindingParsesDescriptor() {
        ProjectBinding b = ProjectBinding.parse("projectDir=/p\nsettings=/p/codenameone_settings.properties\n"
                + "outputDir=/tmp/certs\nuser=a@b.com\ntoken=secret\nbaseUrl=https://example.com\n");
        assertTrue(b.isValid());
        assertEquals("/p", b.projectDir());
        assertEquals("/p/codenameone_settings.properties", b.settings());
        assertEquals("/tmp/certs", b.outputDir());
        assertEquals("a@b.com", b.user());
        assertEquals("secret", b.token());
        assertEquals("https://example.com", b.baseUrl());
    }

    @Test
    void mockServiceMutationsUpdateSnapshot() {
        MockSigningService service = new MockSigningService();
        final SigningState[] before = new SigningState[1];
        service.refresh(r -> before[0] = r.value);
        int certCount = before[0].certificates.size();
        service.createCertificate("IOS_DISTRIBUTION", "New Dist", r -> assertTrue(r.ok));
        final SigningState[] after = new SigningState[1];
        service.refresh(r -> after[0] = r.value);
        assertEquals(certCount + 1, after[0].certificates.size());

        service.createBundleId("com.example.newapp", "New App", true, r -> assertTrue(r.ok));
        service.registerDevice("QA", "00008120-000A1C3E0C68201E", r -> assertTrue(r.ok));
        service.refresh(r -> after[0] = r.value);
        assertTrue(after[0].bundleIds.size() >= 3);
        assertTrue(after[0].devices.size() >= 3);

        service.clearSigningData(r -> assertTrue(r.ok));
        service.refresh(r -> after[0] = r.value);
        assertFalse(after[0].credential.configured());
        assertTrue(after[0].certificates.isEmpty());
    }

    @Test
    void noTokenRefreshDoesNotEraseCachedCredentialState() {
        assertTrue(CertificateWizard.shouldPreserveCachedCredentialState("", true, SigningState.empty()));
        assertFalse(CertificateWizard.shouldPreserveCachedCredentialState("jwt", true, SigningState.empty()));
        assertFalse(CertificateWizard.shouldPreserveCachedCredentialState("", false, SigningState.empty()));
    }

    @Test
    void blankServerCredentialMetadataKeepsCachedDisplayValues() {
        SigningState current = new SigningState(new SigningState.Credential(true, "ABC123XYZ", "issuer-1"),
                null, null, null, null, null, null);
        SigningState refreshed = new SigningState(new SigningState.Credential(true, "", ""),
                null, null, null, null, null, null);

        SigningState merged = CertificateWizard.preserveCachedCredentialDetails(current, refreshed);

        assertTrue(merged.credential.configured());
        assertEquals("ABC123XYZ", merged.credential.keyId());
        assertEquals("issuer-1", merged.credential.issuerId());
    }

    @Test
    void widgetExtensionAndAppGroupDecisionsAreDeterministic() {
        assertEquals("com.example.app.CN1Widgets",
                WizardDecisions.widgetExtensionBundleId("com.example.app"));
        assertNull(WizardDecisions.widgetExtensionBundleId(null));
        assertNull(WizardDecisions.widgetExtensionBundleId("  "));
        assertEquals("group.com.example.app", WizardDecisions.defaultAppGroup("com.example.app"));
        assertNull(WizardDecisions.defaultAppGroup(""));
    }

    @Test
    void declaredAppGroupsSplitOnWhitespaceAsWellAsCommas() {
        // The documented format is space delimited, and IPhoneBuilder reads the hint that way.
        // Splitting on commas alone made "group.a group.b" ONE identifier: automatic setup then
        // tried to create a group by that impossible name instead of preserving the two the
        // project already had, and the App ID's association lost both.
        assertEquals(java.util.Arrays.asList("group.example.share", "group.example.other"),
                WizardDecisions.declaredAppGroups("group.example.share group.example.other"));
        assertEquals(java.util.Arrays.asList("group.example.share", "group.example.other"),
                WizardDecisions.declaredAppGroups("group.example.share,group.example.other"));
        assertEquals(java.util.Arrays.asList("group.example.share", "group.example.other"),
                WizardDecisions.declaredAppGroups(" group.example.share , group.example.other "));
        assertEquals(java.util.Arrays.asList("group.example.share"),
                WizardDecisions.declaredAppGroups("group.example.share group.example.share"));
        assertTrue(WizardDecisions.declaredAppGroups(null).isEmpty());
        assertTrue(WizardDecisions.declaredAppGroups("   ").isEmpty());
    }

    @Test
    void documentProviderBundleIdFollowsTheOverriddenTargetName() {
        // The project can rename the generated target through its build settings and the builder
        // applies that; provisioning the default name would create an App ID and profiles for a
        // target nobody is building, so setup would report success and the build fail in signing.
        assertEquals("com.example.app.CN1Documents",
                WizardDecisions.documentProviderExtensionBundleId("com.example.app", null));
        assertEquals("com.example.app.CN1Documents",
                WizardDecisions.documentProviderExtensionBundleId("com.example.app", "  "));
        assertEquals("com.example.app.files",
                WizardDecisions.documentProviderExtensionBundleId("com.example.app",
                        " com.example.app.files "));
    }

    @Test
    void createAppGroupFindsOrCreatesAndAppearsInRefreshedState() {
        MockSigningService service = new MockSigningService();
        final SigningState.AppGroup[] first = new SigningState.AppGroup[1];
        service.createAppGroup("group.com.example.app", "My App Shared", r -> {
            assertTrue(r.ok);
            first[0] = r.value;
        });
        assertNotNull(first[0]);
        assertEquals("group.com.example.app", first[0].identifier());

        final SigningState[] state = new SigningState[1];
        service.refresh(r -> state[0] = r.value);
        int groupCount = state[0].appGroups.size();
        assertTrue(groupCount >= 1);
        boolean found = false;
        for (SigningState.AppGroup g : state[0].appGroups) {
            if ("group.com.example.app".equals(g.identifier())) {
                found = true;
            }
        }
        assertTrue(found);

        final SigningState.AppGroup[] second = new SigningState.AppGroup[1];
        service.createAppGroup("group.com.example.app", "My App Shared", r -> second[0] = r.value);
        assertEquals(first[0].id(), second[0].id());
        service.refresh(r -> state[0] = r.value);
        assertEquals(groupCount, state[0].appGroups.size());
    }

    @Test
    void enableAppGroupCapabilityRecordsTheAssociation() {
        MockSigningService service = new MockSigningService();
        List<String> groupIds = new ArrayList<String>();
        groupIds.add("GRP_1");
        groupIds.add("GRP_2");
        service.enableAppGroupCapability("BID_A1", groupIds, r -> assertTrue(r.ok));
        assertEquals(groupIds, service.appGroupAssociation("BID_A1"));
        assertTrue(service.appGroupAssociation("BID_UNKNOWN").isEmpty());
    }

    @Test
    void surfacesAutoSetupPiecesProduceGroupCapabilitiesProfileAndSettings() throws Exception {
        MockSigningService service = new MockSigningService();
        String packageName = "com.example.app";
        String groupId = WizardDecisions.defaultAppGroup(packageName);
        String extId = WizardDecisions.widgetExtensionBundleId(packageName);
        assertEquals("group.com.example.app", groupId);
        assertEquals("com.example.app.CN1Widgets", extId);

        final SigningState.AppGroup[] group = new SigningState.AppGroup[1];
        service.createAppGroup(groupId, "My App Shared", r -> group[0] = r.value);
        List<String> groupIds = new ArrayList<String>();
        groupIds.add(group[0].id());

        service.enableAppGroupCapability("BID_A1", groupIds, r -> assertTrue(r.ok));
        service.createBundleId(extId, "My App Widgets", true, r -> assertTrue(r.ok));
        final SigningState[] state = new SigningState[1];
        service.refresh(r -> state[0] = r.value);
        String extAppleId = null;
        for (SigningState.BundleId b : state[0].bundleIds) {
            if (extId.equals(b.identifier())) {
                extAppleId = b.id();
            }
        }
        assertNotNull(extAppleId);
        service.enableAppGroupCapability(extAppleId, groupIds, r -> assertTrue(r.ok));
        assertEquals(groupIds, service.appGroupAssociation("BID_A1"));
        assertEquals(groupIds, service.appGroupAssociation(extAppleId));

        List<SigningState.Certificate> dist = WizardDecisions.compatibleCertificates(state[0], "IOS_APP_STORE");
        assertFalse(dist.isEmpty());
        List<String> certs = new ArrayList<String>();
        certs.add(dist.get(0).appleCertId());
        service.createProfile("My App Widgets App Store", "IOS_APP_STORE", extAppleId, certs,
                new ArrayList<String>(), r -> assertTrue(r.ok));
        List<SigningState.Certificate> dev = WizardDecisions.compatibleCertificates(state[0], "IOS_APP_DEVELOPMENT");
        assertFalse(dev.isEmpty());
        List<String> devCerts = new ArrayList<String>();
        devCerts.add(dev.get(0).appleCertId());
        List<String> deviceIds = new ArrayList<String>();
        for (SigningState.Device d : state[0].devices) {
            deviceIds.add(d.id());
        }
        assertTrue(WizardDecisions.canCreateProfile("IOS_APP_DEVELOPMENT", extAppleId, devCerts, deviceIds,
                "My App Widgets Development"));
        service.createProfile("My App Widgets Development", "IOS_APP_DEVELOPMENT", extAppleId, devCerts,
                deviceIds, r -> assertTrue(r.ok));
        service.refresh(r -> state[0] = r.value);
        boolean extStoreProfile = false;
        boolean extDevProfile = false;
        for (SigningState.Profile p : state[0].profiles) {
            if (extId.equals(p.bundleId()) && "IOS_APP_STORE".equals(p.profileType())) {
                extStoreProfile = true;
            }
            if (extId.equals(p.bundleId()) && "IOS_APP_DEVELOPMENT".equals(p.profileType())) {
                extDevProfile = true;
            }
        }
        assertTrue(extStoreProfile);
        assertTrue(extDevProfile);

        Path settings = Files.createTempFile("cn1-settings", ".properties");
        Files.writeString(settings, "codename1.packageName=com.example.app\n", StandardCharsets.UTF_8);
        SigningAssetInstaller.applyWidgetExtensionSigning(settings.toString(), groupId,
                "/tmp/CN1Widgets.mobileprovision", "/tmp/CN1Widgets_Development.mobileprovision");
        String written = Files.readString(settings, StandardCharsets.UTF_8);
        assertTrue(written.contains("codename1.arg.ios.surfaces.appGroup=group.com.example.app"));
        assertTrue(written.contains(
                "codename1.ios.appext.CN1Widgets.provision=/tmp/CN1Widgets.mobileprovision"));
        assertTrue(written.contains(
                "codename1.ios.release.appext.CN1Widgets.provision=/tmp/CN1Widgets.mobileprovision"));
        assertTrue(written.contains(
                "codename1.ios.debug.appext.CN1Widgets.provision=/tmp/CN1Widgets_Development.mobileprovision"));
        assertTrue(written.contains("codename1.packageName=com.example.app"));
    }

    @Test
    void widgetExtensionSigningWithoutDevelopmentProfileClearsStaleDebugKey() throws Exception {
        Path settings = Files.createTempFile("cn1-settings", ".properties");
        Files.writeString(settings, "codename1.packageName=com.example.app\n"
                + "codename1.ios.debug.appext.CN1Widgets.provision=/tmp/stale-dev.mobileprovision\n",
                StandardCharsets.UTF_8);
        SigningAssetInstaller.applyWidgetExtensionSigning(settings.toString(), "group.com.example.app",
                "/tmp/CN1Widgets.mobileprovision", null);
        String written = Files.readString(settings, StandardCharsets.UTF_8);
        assertTrue(written.contains(
                "codename1.ios.appext.CN1Widgets.provision=/tmp/CN1Widgets.mobileprovision"));
        assertTrue(written.contains(
                "codename1.ios.release.appext.CN1Widgets.provision=/tmp/CN1Widgets.mobileprovision"));
        assertFalse(written.contains("stale-dev.mobileprovision"));
        assertTrue(written.contains("codename1.ios.debug.appext.CN1Widgets.provision=\n"));
    }

    @Test
    void documentProviderExtensionDecisionsAreDeterministic() {
        // The suffix has to match the target name IPhoneBuilder generates and stamps as
        // PRODUCT_BUNDLE_IDENTIFIER. Apple signs an extension against its own App ID, so a
        // mismatch here is an archive-time signing failure naming neither side.
        assertEquals("com.example.app.CN1Documents",
                WizardDecisions.documentProviderExtensionBundleId("com.example.app"));
        assertNull(WizardDecisions.documentProviderExtensionBundleId(null));
        assertNull(WizardDecisions.documentProviderExtensionBundleId("  "));
    }

    @Test
    void documentProviderSigningWritesTheHintsAndBothProfiles() throws Exception {
        Path settings = Files.createTempFile("cn1-settings", ".properties");
        Files.writeString(settings, "codename1.packageName=com.example.app\n",
                StandardCharsets.UTF_8);
        SigningAssetInstaller.applyDocumentProviderSigning(settings.toString(),
                "group.com.example.app", "/tmp/CN1Documents.mobileprovision",
                "/tmp/CN1Documents_Development.mobileprovision");
        String written = Files.readString(settings, StandardCharsets.UTF_8);
        // Enabling the feature is part of installing its signing: an App ID, an App Group and two
        // profiles with no hint is a project that builds without the extension and leaves the
        // developer with signing assets nothing consumes.
        assertTrue(written.contains("codename1.arg.ios.documentProvider.enabled=true"));
        assertTrue(written.contains(
                "codename1.arg.ios.documentProvider.appGroup=group.com.example.app"));
        assertTrue(written.contains(
                "codename1.ios.appext.CN1Documents.provision=/tmp/CN1Documents.mobileprovision"));
        assertTrue(written.contains(
                "codename1.ios.release.appext.CN1Documents.provision=/tmp/CN1Documents.mobileprovision"));
        assertTrue(written.contains(
                "codename1.ios.debug.appext.CN1Documents.provision=/tmp/CN1Documents_Development.mobileprovision"));
        assertTrue(written.contains("codename1.packageName=com.example.app"));
    }

    @Test
    void documentProviderSigningWithoutDevelopmentProfileClearsStaleDebugKey() throws Exception {
        Path settings = Files.createTempFile("cn1-settings", ".properties");
        Files.writeString(settings, "codename1.packageName=com.example.app\n"
                + "codename1.ios.debug.appext.CN1Documents.provision=/tmp/stale-dev.mobileprovision\n",
                StandardCharsets.UTF_8);
        SigningAssetInstaller.applyDocumentProviderSigning(settings.toString(),
                "group.com.example.app", "/tmp/CN1Documents.mobileprovision", null);
        String written = Files.readString(settings, StandardCharsets.UTF_8);
        assertFalse(written.contains("stale-dev.mobileprovision"));
        assertTrue(written.contains("codename1.ios.debug.appext.CN1Documents.provision=\n"));
    }

    @Test
    void distributionProfilesDoNotAskForDevices() {
        // Every profile type that names devices, and every one that does not. A device
        // limited profile is a development or ad hoc one; the store and Developer ID types
        // cover whatever installs the build, and offering a device picker for them is a
        // screen of check boxes that changes nothing (issue #5653).
        assertFalse(WizardDecisions.profileRequiresDevices("IOS_APP_STORE"));
        assertFalse(WizardDecisions.profileRequiresDevices("MAC_APP_STORE"));
        assertFalse(WizardDecisions.profileRequiresDevices("MAC_APP_DIRECT"));
        assertFalse(WizardDecisions.profileRequiresDevices(null));
        assertTrue(WizardDecisions.profileRequiresDevices("IOS_APP_ADHOC"));
        assertTrue(WizardDecisions.profileRequiresDevices("IOS_APP_DEVELOPMENT"));
        assertTrue(WizardDecisions.profileRequiresDevices("MAC_APP_DEVELOPMENT"));

        // And a selection made under a device limited type cannot survive into one that
        // takes no devices, or canCreateProfile would read a satisfied requirement and the
        // request would name devices for a profile Apple does not accept them on.
        SigningState state = stateWithDevices();
        List<String> picked = new ArrayList<String>();
        picked.add("DEV_1");
        assertTrue(WizardDecisions.retainUsableDevices(state, "IOS_APP_DEVELOPMENT", picked).contains("DEV_1"));
        assertTrue(WizardDecisions.retainUsableDevices(state, "IOS_APP_STORE", picked).isEmpty());
    }

    @Test
    void theProfileDialogNarrowsToTheProjectsOwnBundleIds() {
        // The project's App ID and the ones the wizard derives from it for the extensions a
        // build generates. Everything else on the account is a different app (issue #5654).
        List<SigningState.BundleId> all = new ArrayList<SigningState.BundleId>();
        all.add(new SigningState.BundleId("1", "com.example.app", "App", "IOS", null));
        all.add(new SigningState.BundleId("2", "com.example.app.CN1Widgets", "Widgets", "IOS", null));
        all.add(new SigningState.BundleId("3", "com.example.other", "Other", "IOS", null));
        // Not a prefix match on the raw string: com.example.apparel is a different app that
        // merely starts with the same letters, which is why the dot is part of the test.
        all.add(new SigningState.BundleId("4", "com.example.apparel", "Apparel", "IOS", null));
        all.add(new SigningState.BundleId("5", "com.example.app", "App for Mac", "MAC_OS", null));

        List<SigningState.BundleId> own = WizardDecisions.projectBundleIds(all, "com.example.app");
        assertEquals(3, own.size());
        assertEquals("1", own.get(0).id());
        assertEquals("2", own.get(1).id());
        assertEquals("5", own.get(2).id());

        // Empty rather than everything when the project's identifier is not registered: the
        // caller needs to tell "narrowed to nothing" apart from "nothing to narrow", and show
        // the whole account rather than an empty picker.
        assertTrue(WizardDecisions.projectBundleIds(all, "com.example.unregistered").isEmpty());
        assertTrue(WizardDecisions.projectBundleIds(all, null).isEmpty());
        assertTrue(WizardDecisions.projectBundleIds(null, "com.example.app").isEmpty());
    }

    @Test
    void anAppIdIdentifierIsOwnedByTheAccountRatherThanByAPlatform() {
        // Apple registers an identifier once, so a registration that already covers the
        // platform -- explicitly, universally, or because the service did not say -- has to
        // count. Reading those as "not what I need" is what sent automatic setup off to
        // create a duplicate Apple always refuses, with its refusal reported as a failure of
        // the run (issue #5652).
        assertTrue(WizardDecisions.bundlePlatformSatisfies("MAC_OS", "MAC_OS"));
        assertTrue(WizardDecisions.bundlePlatformSatisfies("MAC_OS", "UNIVERSAL"));
        assertTrue(WizardDecisions.bundlePlatformSatisfies("IOS", "UNIVERSAL"));
        assertTrue(WizardDecisions.bundlePlatformSatisfies("MAC_OS", null));
        assertTrue(WizardDecisions.bundlePlatformSatisfies("MAC_OS", "  "));
        assertTrue(WizardDecisions.bundlePlatformSatisfies(null, "IOS"));
        assertFalse(WizardDecisions.bundlePlatformSatisfies("MAC_OS", "IOS"));
        assertFalse(WizardDecisions.bundlePlatformSatisfies("IOS", "MAC_OS"));
    }

    @Test
    void anAppIdWhoseCapabilitiesAreUnknownDoesNotClaimPushIsOff() {
        // The bundle-id listing carries the identifier, name and platform and nothing else,
        // so the wizard cannot know whether push is enabled. It used to fill that silence in
        // with false and print "Push: Off" beside an App ID whose profiles Apple shows
        // carrying Push Notifications (issue #5657).
        assertNull(new SigningState.BundleId("1", "com.example.app", "App", "IOS", null).pushEnabled());
        assertEquals(Boolean.TRUE,
                new SigningState.BundleId("1", "com.example.app", "App", "IOS", true).pushEnabled());
    }

    @Test
    void pushIsRequestedOnlyWhenTheProjectAsksForIt() {
        // Read the way IPhoneBuilder reads the same hint: absent is off, and only a trimmed
        // case-insensitive "true" is on. Automatic setup used to pass a hardcoded true, so
        // every App ID it created carried the capability and so did every profile issued
        // from it (issue #5657).
        assertTrue(WizardDecisions.pushRequested("true"));
        assertTrue(WizardDecisions.pushRequested(" TRUE "));
        assertFalse(WizardDecisions.pushRequested(null));
        assertFalse(WizardDecisions.pushRequested(""));
        assertFalse(WizardDecisions.pushRequested("false"));
        assertFalse(WizardDecisions.pushRequested("yes"));
    }

    @Test
    void aProjectThatDeclaresTheVoipBackgroundModeNeedsPush() {
        // IPhoneBuilder turns an ABSENT ios.includePush into true for a VoIP app, because
        // the call rings through a push. It knows the app is one from a scan of the
        // compiled classes; the wizard has none, but a project that declares the voip
        // background mode has said so in the settings file.
        assertTrue(WizardDecisions.pushRequested(null, "voip"));
        assertTrue(WizardDecisions.pushRequested("", "audio, voip"));
        assertTrue(WizardDecisions.pushRequested(null, "voip fetch"));
        assertFalse(WizardDecisions.pushRequested(null, "remote-notification"));
        assertFalse(WizardDecisions.pushRequested(null, null));

        // Whole tokens, the way the builder matches them: a mode merely CONTAINING "voip"
        // is a different mode, and a substring test reads it as VoIP already declared.
        assertFalse(WizardDecisions.declaresVoipBackgroundMode("myvoipmode"));
        assertTrue(WizardDecisions.declaresVoipBackgroundMode("myvoipmode voip"));

        // An explicit hint is the project speaking, and the builder refuses a VoIP app
        // that turned push off rather than overriding it -- so neither does this.
        assertFalse(WizardDecisions.pushRequested("false", "voip"));
        assertTrue(WizardDecisions.pushRequested("true", "remote-notification"));
    }

    @Test
    void theMacAppIdFollowsTheMacEntitlementRatherThanTheIosHint() {
        // A Mac build declares the APNs entitlement from macos.entitlements.apsEnvironment,
        // read the way MacOSBuildHints reads it: false and none suppress it, anything else
        // selects an environment. A Mac App ID has to grant what the Mac build declares.
        assertTrue(WizardDecisions.macPushRequested("production"));
        assertTrue(WizardDecisions.macPushRequested("development"));
        assertFalse(WizardDecisions.macPushRequested("false"));
        assertFalse(WizardDecisions.macPushRequested("NONE"));
        // Absent is where the builder falls back to its class scan, which the wizard has no
        // classes for -- the same residue the iOS side has, and not a guess.
        assertFalse(WizardDecisions.macPushRequested(null));
        assertFalse(WizardDecisions.macPushRequested("  "));

        // And the two questions stay apart: an iOS-only push app must not have the
        // capability put on its Mac App ID, nor a Mac-only one on its iOS App ID.
        assertFalse(WizardDecisions.macPushRequested(null));
        assertFalse(WizardDecisions.pushRequested(null, null));
    }

    private static SigningState stateWithDevices() {
        List<SigningState.Device> devices = new ArrayList<SigningState.Device>();
        devices.add(new SigningState.Device("DEV_1", "iPhone", "UDID_1", "IOS", "ENABLED"));
        return new SigningState(new SigningState.Credential(true, "K", "I"), null, null, devices,
                null, null, null);
    }

    @Test
    void cloudServiceUsesGeneratedClientForClearSigningData() throws Exception {
        Path sourcePath = Paths.get("src/main/java/com/codename1/certificatewizard/api/CloudSigningService.java");
        if (!Files.exists(sourcePath)) {
            sourcePath = Paths.get("../common/src/main/java/com/codename1/certificatewizard/api/CloudSigningService.java");
        }
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        assertTrue(source.contains("credentialApi.clearSigningData(bearerToken"));
        assertFalse(source.contains("Rest.delete(baseUrl + \"/appsec/7.0/apple/signing-data\")"));
    }
}
