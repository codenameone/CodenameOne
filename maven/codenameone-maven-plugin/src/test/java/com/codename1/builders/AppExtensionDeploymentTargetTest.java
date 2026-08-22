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
}
