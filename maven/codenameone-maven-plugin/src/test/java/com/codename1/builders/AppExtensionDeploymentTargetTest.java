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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppExtensionDeploymentTargetTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void theArchivesOwnValueWinsAboveTheFloor() throws Exception {
        // An extension knows which APIs it calls; nothing here should second-guess it.
        assertEquals("16.1", IPhoneBuilder.appExtensionDeploymentTarget("16.1", null, "11"));
        assertEquals("15.0", IPhoneBuilder.appExtensionDeploymentTarget("15.0", walletEntitlements(), "11"));
        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentTarget("14", walletEntitlements(), "11"));
    }

    @Test
    public void aDeclaredValueBelowTheFloorIsRaised() throws Exception {
        // An archive exported from an old project carries its own legacy target. Honouring that
        // unconditionally reproduces the rejection this exists to prevent.
        assertEquals("14.0",
                IPhoneBuilder.appExtensionDeploymentTarget("10.0", walletEntitlements(), "11"));
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget("10.0", null, "11"));
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget("  ", null, "9.0"));
    }

    @Test
    public void anIssuerProvisioningWalletExtensionNeeds14() throws Exception {
        // PKIssuerProvisioningExtensionHandler arrived in iOS 14, and App Store validation says so
        // on upload: "Please ensure the MinimumOSVersion value of your extension is 14 or later".
        assertEquals("14.0",
                IPhoneBuilder.appExtensionDeploymentTarget(null, walletEntitlements(), "11"));
    }

    @Test
    public void anythingElseSitsOnTheAppsTargetOrTheSdkFloor() throws Exception {
        // 10.0, which this used to hand out, is below what the current SDK will build against.
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(null, null, "11"));
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(null, null, null));
        assertEquals("15.4", IPhoneBuilder.appExtensionDeploymentTarget(null, null, "15.4"));
    }

    @Test
    public void versionsCompareByNumberNotByText() throws Exception {
        // "9.0" is not above "12.0", and a two-part value is not below its own major.
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(null, null, "9.0"));
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(null, null, "12"));
        assertEquals("12.4", IPhoneBuilder.appExtensionDeploymentTarget(null, null, "12.4"));
    }

    private File walletEntitlements() throws Exception {
        File file = new File(tmp.getRoot(), "WalletNonUIExtension.entitlements");
        write(file, "<plist><dict>\n"
                + "<key>com.apple.developer.payment-pass-provisioning</key>\n<true/>\n"
                + "</dict></plist>\n");
        return file;
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
    public void theFloorReadsTheEntitlementsTheTargetIsSignedWith() throws Exception {
        File dist = tmp.newFolder("dist2");
        File extension = new File(dist, "WalletNonUIExtension");
        assertTrue(extension.mkdirs());
        // Named after the extension, but NOT what the archive signs with.
        write(new File(extension, "WalletNonUIExtension.entitlements"), "<plist><dict/></plist>");
        File configured = new File(extension, "Release.entitlements");
        write(configured, "<plist><dict>\n<key>com.apple.developer.payment-pass-provisioning</key>\n"
                + "<true/>\n</dict></plist>");

        File signed = IPhoneBuilder.appExtensionSignedEntitlements(extension,
                "WalletNonUIExtension/Release.entitlements",
                new File(extension, "WalletNonUIExtension.entitlements"),
                new java.util.HashMap<String, String>());
        assertEquals(configured, signed);
        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentTarget(null, signed, "11"));
    }

    @Test
    public void withNoConfiguredEntitlementsTheNamedOneStands() throws Exception {
        File byName = walletEntitlements();
        java.util.Map<String, String> none = new java.util.HashMap<String, String>();
        assertEquals(byName, IPhoneBuilder.appExtensionSignedEntitlements(tmp.getRoot(),
                "$(NS_CODE_SIGN_ENTITLEMENTS)", byName, none));
        assertEquals(byName, IPhoneBuilder.appExtensionSignedEntitlements(tmp.getRoot(), null, byName, none));
    }

    @Test
    public void aPathThroughProductNameUsesTheSettingsNotTheDeletedFile() throws Exception {
        File dist = tmp.newFolder("dist3");
        File extension = new File(dist, "WalletNonUIExtension");
        assertTrue(extension.mkdirs());
        File renamed = new File(extension, "Renamed.entitlements");
        write(renamed, "<plist><dict>\n<key>com.apple.developer.payment-pass-provisioning</key>\n"
                + "<true/>\n</dict></plist>");
        // buildSettings.properties is loaded into the map and DELETED before this runs, so the
        // override has to come from the map or $(PRODUCT_NAME) resolves to the folder name.
        java.util.Map<String, String> settings = new java.util.HashMap<String, String>();
        settings.put("PRODUCT_NAME", "Renamed");

        File signed = IPhoneBuilder.appExtensionSignedEntitlements(extension,
                "WalletNonUIExtension/$(PRODUCT_NAME).entitlements", null, settings);

        assertEquals(renamed, signed);
        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentTarget(null, signed, "11"));
    }

    @Test
    public void aUtf16EntitlementsFileIsStillRead() throws Exception {
        File file = new File(tmp.getRoot(), "utf16.entitlements");
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-16\"?>\n<plist version=\"1.0\"><dict>\n"
                + "<key>com.apple.developer.payment-pass-provisioning</key>\n<true/>\n</dict></plist>\n";
        java.io.OutputStream out = new java.io.FileOutputStream(file);
        try {
            out.write(new byte[]{(byte) 0xFF, (byte) 0xFE});
            out.write(xml.getBytes("UTF-16LE"));
        } finally {
            out.close();
        }
        // A byte search for the key finds nothing here, and the extension keeps a floor Apple
        // rejects it for.
        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentTarget(null, file, "11"));
    }

    @Test
    public void theEntitlementMustBeGrantedNotJustMentioned() throws Exception {
        File commented = new File(tmp.getRoot(), "commented.entitlements");
        write(commented, "<plist><dict>\n"
                + "<!-- com.apple.developer.payment-pass-provisioning is requested separately -->\n"
                + "<key>com.apple.security.application-groups</key><array/>\n</dict></plist>");
        // Pushing an extension to iOS 14 on the strength of a comment drops it off every 12 and 13
        // device it would have run on.
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(null, commented, "11"));

        File denied = new File(tmp.getRoot(), "denied.entitlements");
        write(denied, "<plist><dict>\n"
                + "<key>com.apple.developer.payment-pass-provisioning</key>\n<false/>\n</dict></plist>");
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(null, denied, "11"));
    }

    @Test
    public void aNestedMentionIsNotAGrant() throws Exception {
        File nested = new File(tmp.getRoot(), "nested.entitlements");
        write(nested, "<plist><dict>\n<key>com.apple.developer.associated-domains</key>\n<dict>\n"
                + "<key>com.apple.developer.payment-pass-provisioning</key>\n<true/>\n</dict>\n"
                + "</dict></plist>");
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(null, nested, "11"));
    }

    @Test
    public void aBinaryEntitlementsPlistIsStillRead() throws Exception {
        File file = new File(tmp.getRoot(), "binary.entitlements");
        // A real binary plist on a machine with plutil, and on one without it the byte fallback
        // sees the same key. Either way an issuer-provisioning extension must not fall back to
        // the 12.0 floor Apple rejects it for.
        java.io.OutputStream out = new java.io.FileOutputStream(file);
        try {
            out.write("bplist00".getBytes("UTF-8"));
            out.write("com.apple.developer.payment-pass-provisioning".getBytes("UTF-8"));
        } finally {
            out.close();
        }
        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentTarget(null, file, "11"));
    }

    @Test
    public void anXmlPlistIsStillJudgedByItsKeys() throws Exception {
        // The byte fallback is for binary files only: XML still goes through the parser, where a
        // mention in a comment is not a grant.
        File commented = new File(tmp.getRoot(), "xml-comment.entitlements");
        write(commented, "<plist><dict>\n<!-- com.apple.developer.payment-pass-provisioning -->\n"
                + "</dict></plist>");
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(null, commented, "11"));
    }

    @Test
    public void aQualifiedDeploymentTargetIsClampedToo() throws Exception {
        // Xcode picks the qualified value for the device build, so clamping only the base left the
        // archive shipping 10.0 -- the very rejection the floor exists to prevent.
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("IPHONEOS_DEPLOYMENT_TARGET", "14.0");
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]", "10.0");
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphonesimulator*]", "15.0");

        java.util.List<String> notes = IPhoneBuilder.repairQualifiedExtensionSettings(settings,
                "com.example.app", "14.0");

        assertEquals("14.0", settings.get("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]"));
        assertEquals("15.0", settings.get("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphonesimulator*]"));
        assertEquals(1, notes.size());
    }

    @Test
    public void aQualifiedIdentifierFromAnotherProjectIsDropped() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("PRODUCT_BUNDLE_IDENTIFIER", "com.example.app.Ext");
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos*]", "com.old.project.Ext");
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphonesimulator*]", "com.example.app.Ext.sim");

        java.util.List<String> notes = IPhoneBuilder.repairQualifiedExtensionSettings(settings,
                "com.example.app", "12.0");

        // Dropped, so the base value -- the one this builder set -- governs the device build.
        assertFalse(settings.containsKey("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos*]"));
        assertEquals("com.example.app.Ext.sim",
                settings.get("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphonesimulator*]"));
        assertEquals("com.example.app.Ext", settings.get("PRODUCT_BUNDLE_IDENTIFIER"));
        assertEquals(1, notes.size());
    }

    @Test
    public void settingsThatAreAlreadyFineAreLeftAlone() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]", "16.0");
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos*]", "com.example.app.Ext");
        // and a mangled key, which Xcode does not honour and which nothing here should touch
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[sdk", "10.0");

        java.util.List<String> notes = IPhoneBuilder.repairQualifiedExtensionSettings(settings,
                "com.example.app", "14.0");

        assertTrue(notes.toString(), notes.isEmpty());
        assertEquals("10.0", settings.get("IPHONEOS_DEPLOYMENT_TARGET[sdk"));
    }

    @Test
    public void anIdentifierOutsideTheAppStopsTheBuild() throws Exception {
        // The message is the build's last word on it, so it has to say what to change.
        String message = IPhoneBuilder.outOfNamespaceExtensionIdMessage("WalletUIExtension",
                "com.old.project.WalletUIExtension", "com.example.app");
        assertTrue(message, message.contains("com.old.project.WalletUIExtension"));
        assertTrue(message, message.contains("buildSettings.properties"));
        assertTrue(message, message.contains("com.example.app.WalletUIExtension"));
    }

    @Test
    public void anIdentifierUnderTheAppIsNoProblem() throws Exception {
        assertNull(IPhoneBuilder.outOfNamespaceExtensionIdMessage("WalletUIExtension",
                "com.example.app.WalletUIExtension", "com.example.app"));
        // and with no package to judge against, this is not the check that should fail the build
        assertNull(IPhoneBuilder.outOfNamespaceExtensionIdMessage("WalletUIExtension",
                "com.anything.Ext", null));
    }

    @Test
    public void aQualifiedTargetWrittenThroughAnotherSettingIsKept() throws Exception {
        // $(EXTENSION_MIN) is not a number, and reading that as "below the floor" overwrote an
        // extension's iOS 16 target with 12.0 -- taking its iOS 16 APIs down with it.
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("EXTENSION_MIN", "16.0");
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]", "$(EXTENSION_MIN)");

        java.util.List<String> notes = IPhoneBuilder.repairQualifiedExtensionSettings(settings,
                "com.example.app", "14.0");

        assertEquals("$(EXTENSION_MIN)", settings.get("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]"));
        assertTrue(notes.toString(), notes.isEmpty());
    }

    @Test
    public void aQualifiedTargetResolvingBelowTheFloorIsStillClamped() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("EXTENSION_MIN", "10.0");
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]", "$(EXTENSION_MIN)");

        IPhoneBuilder.repairQualifiedExtensionSettings(settings, "com.example.app", "14.0");

        assertEquals("14.0", settings.get("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]"));
    }

    @Test
    public void aReferenceToNothingIsNotAMinimumAtAll() throws Exception {
        File extension = tmp.newFolder("dist21", "WalletUIExtension");
        // Xcode expands a reference nothing defines to the empty string, so the extension would
        // declare no minimum -- which is why this is the floor's answer and not the expression's.
        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentTarget("$(SOMETHING_ELSE)",
                walletEntitlements(), "11", extension,
                new java.util.LinkedHashMap<String, String>()));
    }

    @Test
    public void aQualifiedIdentifierWrittenThroughAnotherSettingIsKept() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("EXTENSION_ID", "com.example.app.Ext");
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos*]", "$(EXTENSION_ID)");

        java.util.List<String> notes = IPhoneBuilder.repairQualifiedExtensionSettings(settings,
                "com.example.app", "12.0");

        assertTrue(settings.containsKey("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos*]"));
        assertTrue(notes.isEmpty());
    }

    @Test
    public void aBaseTargetWrittenThroughAnotherSettingIsKept() throws Exception {
        File extension = tmp.newFolder("dist9", "WalletUIExtension");
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("EXTENSION_MIN", "16.0");
        // The reference parsed as no version at all, so the floor overwrote an iOS 16 target.
        assertEquals("$(EXTENSION_MIN)", IPhoneBuilder.appExtensionDeploymentTarget(
                "$(EXTENSION_MIN)", (File) null, "11", extension, settings));
    }

    @Test
    public void aBaseTargetResolvingBelowTheFloorIsStillClamped() throws Exception {
        File extension = tmp.newFolder("dist10", "WalletUIExtension");
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("EXTENSION_MIN", "10.0");
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentTarget(
                "$(EXTENSION_MIN)", (File) null, "11", extension, settings));
    }

    @Test
    public void anIdentifierThroughTargetNameResolvesRatherThanTruncating() throws Exception {
        File extension = tmp.newFolder("dist11", "WalletUIExtension");
        java.util.Map<String, String> settings = IPhoneBuilder.extensionSettingsWithBuiltIns(
                extension, new java.util.LinkedHashMap<String, String>());
        // Deleting $(TARGET_NAME) recorded "com.example.app." as the export-options key, matching
        // nothing in the archive.
        assertEquals("WalletUIExtension", settings.get("TARGET_NAME"));
        assertEquals("WalletUIExtension", settings.get("PRODUCT_NAME"));
    }

    @Test
    public void aQualifiedEntitlementsFileCanRaiseTheFloor() throws Exception {
        File extension = tmp.newFolder("dist12", "WalletUIExtension");
        File plain = new File(extension, "Plain.entitlements");
        write(plain, "<plist><dict/></plist>");
        File device = new File(extension, "Device.entitlements");
        write(device, "<plist><dict>\n<key>com.apple.developer.payment-pass-provisioning</key>\n"
                + "<true/>\n</dict></plist>");
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("CODE_SIGN_ENTITLEMENTS", "WalletUIExtension/Plain.entitlements");
        settings.put("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*]", "WalletUIExtension/Device.entitlements");

        java.util.List<File> candidates = IPhoneBuilder.appExtensionEntitlementsCandidates(
                extension, settings, null);

        // The device archive is signed with the qualified file, so its entitlement decides.
        assertEquals(2, candidates.size());
        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentFloor(candidates));
    }

    @Test
    public void anIndirectProductNameKeepsItsChain() throws Exception {
        File extension = tmp.newFolder("dist13", "WalletUIExtension");
        java.util.Map<String, String> declared = new java.util.LinkedHashMap<String, String>();
        declared.put("EXTENSION_NAME", "WalletKit");
        declared.put("PRODUCT_NAME", "$(EXTENSION_NAME)");

        java.util.Map<String, String> settings = IPhoneBuilder.extensionSettingsWithBuiltIns(
                extension, declared);

        // Xcode expands the chain; flattening it to the folder name recorded an identifier the
        // archive does not contain.
        assertEquals("WalletKit", settings.get("PRODUCT_NAME"));
        assertEquals("WalletUIExtension", settings.get("TARGET_NAME"));
    }

    @Test
    public void anUnresolvableProductNameFallsBackToTheTarget() throws Exception {
        File extension = tmp.newFolder("dist14", "WalletUIExtension");
        java.util.Map<String, String> declared = new java.util.LinkedHashMap<String, String>();
        declared.put("PRODUCT_NAME", "$(TARGET_NAME)");
        assertEquals("WalletUIExtension", IPhoneBuilder.extensionSettingsWithBuiltIns(
                extension, declared).get("PRODUCT_NAME"));
    }

    @Test
    public void aConditionForAnotherBuildDoesNotRaiseThisFloor() throws Exception {
        File extension = tmp.newFolder("dist15", "WalletUIExtension");
        File release = new File(extension, "Release.entitlements");
        write(release, "<plist><dict/></plist>");
        File debug = new File(extension, "Debug.entitlements");
        write(debug, "<plist><dict>\n<key>com.apple.developer.payment-pass-provisioning</key>\n"
                + "<true/>\n</dict></plist>");
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("CODE_SIGN_ENTITLEMENTS", "WalletUIExtension/Release.entitlements");
        settings.put("CODE_SIGN_ENTITLEMENTS[config=Debug]", "WalletUIExtension/Debug.entitlements");
        settings.put("CODE_SIGN_ENTITLEMENTS[sdk=iphonesimulator*]",
                "WalletUIExtension/Debug.entitlements");

        java.util.List<File> forRelease = IPhoneBuilder.appExtensionEntitlementsCandidates(
                extension, settings, null, "iphoneos", "Release");

        // The release device archive is not signed with either of those, so neither decides its
        // minimum iOS -- raising it would drop the extension off iOS 12 and 13 for nothing.
        assertEquals(1, forRelease.size());
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentFloor(forRelease));
    }

    @Test
    public void aConditionForThisBuildStillCounts() throws Exception {
        assertTrue(IPhoneBuilder.conditionApplies("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*]",
                "iphoneos", "Release"));
        assertTrue(IPhoneBuilder.conditionApplies("CODE_SIGN_ENTITLEMENTS[config=Release]",
                "iphoneos", "Release"));
        assertFalse(IPhoneBuilder.conditionApplies("CODE_SIGN_ENTITLEMENTS[sdk=iphonesimulator*]",
                "iphoneos", "Release"));
        assertFalse(IPhoneBuilder.conditionApplies("CODE_SIGN_ENTITLEMENTS[config=Debug]",
                "iphoneos", "Release"));
        // a condition this build has no answer for, and one it cannot read, both count
        assertTrue(IPhoneBuilder.conditionApplies("CODE_SIGN_ENTITLEMENTS[arch=arm64]",
                "iphoneos", "Release"));
        assertTrue(IPhoneBuilder.conditionApplies("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*]",
                null, null));
    }

    @Test
    public void aQualifiedEntitlementsFileOverridesTheBaseRatherThanAddingToIt() throws Exception {
        File extension = tmp.newFolder("dist16", "WalletUIExtension");
        File base = new File(extension, "Base.entitlements");
        write(base, "<plist><dict>\n<key>com.apple.developer.payment-pass-provisioning</key>\n"
                + "<true/>\n</dict></plist>");
        File device = new File(extension, "Device.entitlements");
        write(device, "<plist><dict/></plist>");
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("CODE_SIGN_ENTITLEMENTS", "WalletUIExtension/Base.entitlements");
        settings.put("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*]", "WalletUIExtension/Device.entitlements");

        File signing = IPhoneBuilder.appExtensionSigningEntitlements(extension, settings, null,
                "iphoneos", "Release");

        // Xcode signs the device archive with the qualified file ALONE. Reading both and taking
        // the stricter answer raised the extension to iOS 14 for an entitlement it never carries.
        assertEquals(device, signing);
        assertEquals("12.0", IPhoneBuilder.appExtensionDeploymentFloor(signing));
    }

    @Test
    public void theWinningEntitlementsFileStillRaisesTheFloorWhenItGrants() throws Exception {
        File extension = tmp.newFolder("dist17", "WalletUIExtension");
        File base = new File(extension, "Base.entitlements");
        write(base, "<plist><dict/></plist>");
        File device = new File(extension, "Device.entitlements");
        write(device, "<plist><dict>\n<key>com.apple.developer.payment-pass-provisioning</key>\n"
                + "<true/>\n</dict></plist>");
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("CODE_SIGN_ENTITLEMENTS", "WalletUIExtension/Base.entitlements");
        settings.put("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*]", "WalletUIExtension/Device.entitlements");

        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentFloor(
                IPhoneBuilder.appExtensionSigningEntitlements(extension, settings, null,
                        "iphoneos", "Release")));
    }

    @Test
    public void theMostSpecificApplicableConditionWins() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("PRODUCT_BUNDLE_IDENTIFIER", "com.old.Ext");
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos*]", "com.example.app.Ext");
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphonesimulator*]", "com.example.app.Sim");

        // The device archive uses the device value, so a stale base is not a reason to refuse it.
        assertEquals("com.example.app.Ext", IPhoneBuilder.winningSetting(settings,
                "PRODUCT_BUNDLE_IDENTIFIER", "iphoneos", "Release"));
        assertEquals("com.example.app.Sim", IPhoneBuilder.winningSetting(settings,
                "PRODUCT_BUNDLE_IDENTIFIER", "iphonesimulator", "Release"));
        assertNull(IPhoneBuilder.winningSetting(settings, "SOMETHING_ELSE", "iphoneos", "Release"));
    }

    @Test
    public void withNoApplicableConditionThePlainSettingGoverns() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("PRODUCT_BUNDLE_IDENTIFIER", "com.example.app.Ext");
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[config=Debug]", "com.example.app.Ext.debug");
        assertEquals("com.example.app.Ext", IPhoneBuilder.winningSetting(settings,
                "PRODUCT_BUNDLE_IDENTIFIER", "iphoneos", "Release"));
    }

    @Test
    public void aVersionedSdkQualifierMatchesTheArchivesSdk() throws Exception {
        // xcodebuild is given iphoneos14.4, not iphoneos, so a condition naming the version is
        // the one Xcode picks -- and rejecting it aborted on a stale base identifier.
        assertTrue(IPhoneBuilder.conditionApplies("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos14.4]",
                "iphoneos14.4", "Release"));
        // Not this one: Xcode matches an unwildcarded condition against the versioned SDK_NAME
        // exactly, so [sdk=iphoneos] never applies to iphoneos14.4 and selecting on it would pick
        // a setting Xcode ignores. [sdk=iphoneos*] is the spelling that applies.
        assertFalse(IPhoneBuilder.conditionApplies("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos]",
                "iphoneos14.4", "Release"));
        // But when THIS build does not know its own SDK version, a versioned condition still
        // counts, because it cannot be evaluated either way.
        assertTrue(IPhoneBuilder.conditionApplies("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos26.0]",
                "iphoneos", "Release"));
        assertTrue(IPhoneBuilder.conditionApplies("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos*]",
                "iphoneos14.4", "Release"));
        // a different platform still does not
        assertFalse(IPhoneBuilder.conditionApplies("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphonesimulator14.4]",
                "iphoneos14.4", "Release"));
        // and a different version of the same platform, when both name one
        assertFalse(IPhoneBuilder.conditionApplies("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphoneos13.0]",
                "iphoneos14.4", "Release"));
    }

    @Test
    public void anArchitectureQualifierIsDecidedByTheArchiveNotByMapOrder() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        // x86_64 first, so map order would pick it.
        settings.put("CODE_SIGN_ENTITLEMENTS[arch=x86_64]", "WalletUIExtension/Sim.entitlements");
        settings.put("CODE_SIGN_ENTITLEMENTS[arch=arm64]", "WalletUIExtension/Device.entitlements");

        assertEquals("WalletUIExtension/Device.entitlements", IPhoneBuilder.winningSetting(
                settings, "CODE_SIGN_ENTITLEMENTS", "iphoneos14.4", "Release", "arm64"));
        // and with no architecture to judge by, both still count rather than one being guessed away
        assertTrue(IPhoneBuilder.conditionApplies("CODE_SIGN_ENTITLEMENTS[arch=x86_64]",
                "iphoneos14.4", "Release", null));
    }

    @Test
    public void theArchivesConfigurationSdkAndArchResolve() throws Exception {
        File extension = tmp.newFolder("dist18", "WalletUIExtension");
        java.util.Map<String, String> settings = IPhoneBuilder.extensionSettingsWithBuiltIns(
                extension, new java.util.LinkedHashMap<String, String>(), "Release",
                "iphoneos14.4", "arm64");
        assertEquals("Release", settings.get("CONFIGURATION"));
        assertEquals("iphoneos14.4", settings.get("SDK_NAME"));
        assertEquals("iphoneos", settings.get("PLATFORM_NAME"));
        assertEquals("arm64", settings.get("CURRENT_ARCH"));
    }

    @Test
    public void anEntitlementsPathThroughTheConfigurationResolves() throws Exception {
        File extension = tmp.newFolder("dist19", "WalletUIExtension");
        File release = new File(extension, "Release.entitlements");
        write(release, "<plist><dict>\n<key>com.apple.developer.payment-pass-provisioning</key>\n"
                + "<true/>\n</dict></plist>");
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("CODE_SIGN_ENTITLEMENTS", "WalletUIExtension/$(CONFIGURATION).entitlements");

        File signing = IPhoneBuilder.appExtensionSigningEntitlements(extension, settings, null,
                "iphoneos14.4", "Release", "arm64");

        // Falling back to a by-name file here left a payment-pass extension on the 12.0 floor.
        assertEquals(release, signing);
        assertEquals("14.0", IPhoneBuilder.appExtensionDeploymentFloor(signing));
    }

    @Test
    public void aPartiallyResolvableValueIsNotRecordedAsATruncation() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("CONFIGURATION", "Release");
        // Known: expands. Unknown: the whole answer is withheld rather than truncated, because
        // "com.example.app." as an export-options key names no bundle in the archive.
        assertEquals("com.example.app.Release", IPhoneBuilder.resolveSettingsFully(
                "com.example.app.$(CONFIGURATION)", settings));
        assertNull(IPhoneBuilder.resolveSettingsFully(
                "com.example.app.$(SOMETHING_UNKNOWN)", settings));
        assertEquals("com.example.app.Ext", IPhoneBuilder.resolveSettingsFully(
                "com.example.app.Ext", settings));
    }

    @Test
    public void aProductNameThroughTheConfigurationResolvesToo() throws Exception {
        File extension = tmp.newFolder("dist20", "WalletUIExtension");
        java.util.Map<String, String> declared = new java.util.LinkedHashMap<String, String>();
        declared.put("PRODUCT_NAME", "$(CONFIGURATION)-Wallet");

        java.util.Map<String, String> settings = IPhoneBuilder.extensionSettingsWithBuiltIns(
                extension, declared, "Release", "iphoneos14.4", "arm64");

        // Resolving PRODUCT_NAME before the context was in the map left "-Wallet", and that went
        // into the identifier and into the export-options key.
        assertEquals("Release-Wallet", settings.get("PRODUCT_NAME"));
        assertEquals("com.example.app.Release-Wallet", IPhoneBuilder.resolveSettingsFully(
                "com.example.app.$(PRODUCT_NAME)", settings));
    }

    @Test
    public void repairsLeaveOtherBuildsSettingsAlone() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]", "10.0");
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphonesimulator*]", "10.0");
        settings.put("IPHONEOS_DEPLOYMENT_TARGET[config=Debug]", "10.0");

        java.util.List<String> notes = IPhoneBuilder.repairQualifiedExtensionSettings(settings,
                "com.example.app", "14.0", "iphoneos14.4", "Release", "arm64");

        // The floor came from the entitlements THIS archive is signed with. Applying it to a
        // simulator or Debug condition edits a target those entitlements have nothing to do with,
        // and the edit lives on in the generated project.
        assertEquals("14.0", settings.get("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*]"));
        assertEquals("10.0", settings.get("IPHONEOS_DEPLOYMENT_TARGET[sdk=iphonesimulator*]"));
        assertEquals("10.0", settings.get("IPHONEOS_DEPLOYMENT_TARGET[config=Debug]"));
        assertEquals(1, notes.size());
    }

    @Test
    public void anIdentifierForAnotherBuildIsNotDroppedEither() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphonesimulator*]", "com.other.Sim");
        IPhoneBuilder.repairQualifiedExtensionSettings(settings, "com.example.app", "12.0",
                "iphoneos14.4", "Release", "arm64");
        assertTrue(settings.containsKey("PRODUCT_BUNDLE_IDENTIFIER[sdk=iphonesimulator*]"));
    }

    @Test
    public void aModifierReferenceResolvesAsXcodeExpandsIt() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("PRODUCT_NAME", "Wallet UI");
        // ${PRODUCT_NAME:rfc1034identifier} is the ordinary way to write this, and treating it as
        // plain text recorded the expression itself as the bundle identifier.
        assertEquals("com.example.app.Wallet-UI", IPhoneBuilder.resolveSettingsFully(
                "com.example.app.${PRODUCT_NAME:rfc1034identifier}", settings));
        assertEquals("com.example.app.wallet ui", IPhoneBuilder.resolveSettingsFully(
                "com.example.app.$(PRODUCT_NAME:lower)", settings));
    }

    @Test
    public void aModifierThisBuildDoesNotKnowIsNotCalledResolved() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("PRODUCT_NAME", "Wallet");
        assertNull(IPhoneBuilder.resolveSettingsFully(
                "com.example.app.$(PRODUCT_NAME:somethingNew)", settings));
    }

    @Test
    public void anExactConditionOutranksAWildcardOne() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        // Wildcard first, so iteration order would pick it if the two scored equal.
        settings.put("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*]", "Wildcard.entitlements");
        settings.put("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos26.0]", "Exact.entitlements");

        assertEquals("Exact.entitlements", IPhoneBuilder.winningSetting(settings,
                "CODE_SIGN_ENTITLEMENTS", "iphoneos26.0", "Release", "arm64"));
        assertTrue(IPhoneBuilder.conditionSpecificity("X[sdk=iphoneos26.0]")
                > IPhoneBuilder.conditionSpecificity("X[sdk=iphoneos*]"));
        // and more conditions still beat fewer
        assertTrue(IPhoneBuilder.conditionSpecificity("X[sdk=iphoneos*,config=Release]")
                > IPhoneBuilder.conditionSpecificity("X[sdk=iphoneos*]"));
    }

    @Test
    public void aNarrowerWildcardOutranksABroaderOne() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        // Broader first, so iteration order would pick it if the two scored equal.
        settings.put("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*]", "Broad.entitlements");
        settings.put("CODE_SIGN_ENTITLEMENTS[sdk=iphoneos14.*]", "Narrow.entitlements");

        assertEquals("Narrow.entitlements", IPhoneBuilder.winningSetting(settings,
                "CODE_SIGN_ENTITLEMENTS", "iphoneos14.4", "Release", "arm64"));
        assertTrue(IPhoneBuilder.conditionSpecificity("X[sdk=iphoneos14.*]")
                > IPhoneBuilder.conditionSpecificity("X[sdk=iphoneos*]"));
        // and an exact value still beats both
        assertTrue(IPhoneBuilder.conditionSpecificity("X[sdk=iphoneos14.4]")
                > IPhoneBuilder.conditionSpecificity("X[sdk=iphoneos14.*]"));
    }

    @Test
    public void aVariantThisBuilderNeverArchivesDoesNotWin() throws Exception {
        java.util.Map<String, String> settings = new java.util.LinkedHashMap<String, String>();
        settings.put("PRODUCT_BUNDLE_IDENTIFIER", "com.example.app.Ext");
        settings.put("PRODUCT_BUNDLE_IDENTIFIER[variant=profile]", "com.other.Ext");

        // This builder archives the normal variant, so a profile-variant setting belongs to a
        // build that does not happen here -- and it is more specific than the plain one, so
        // accepting it let it win.
        assertEquals("com.example.app.Ext", IPhoneBuilder.winningSetting(settings,
                "PRODUCT_BUNDLE_IDENTIFIER", "iphoneos14.4", "Release", "arm64"));
        assertFalse(IPhoneBuilder.conditionApplies("X[variant=profile]", "iphoneos14.4",
                "Release", "arm64"));
        assertTrue(IPhoneBuilder.conditionApplies("X[variant=normal]", "iphoneos14.4",
                "Release", "arm64"));
    }
}
