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
package com.codename1.certificatewizard.api;

import java.util.ArrayList;
import java.util.List;

public final class WizardDecisions {
    public static final String WIDGET_EXTENSION_SUFFIX = ".CN1Widgets";

    /// The document provider extension's App ID is the app's plus this, matching the target name
    /// IPhoneBuilder generates and the PRODUCT_BUNDLE_IDENTIFIER it stamps on it. Apple signs an
    /// extension against its own App ID, so a mismatch here is a signing failure at archive time.
    public static final String DOCUMENT_PROVIDER_EXTENSION_SUFFIX = ".CN1Documents";

    private WizardDecisions() {
    }

    public static String widgetExtensionBundleId(String mainBundleId) {
        if (mainBundleId == null || mainBundleId.trim().isEmpty()) {
            return null;
        }
        return mainBundleId.trim() + WIDGET_EXTENSION_SUFFIX;
    }

    public static String documentProviderExtensionBundleId(String mainBundleId) {
        if (mainBundleId == null || mainBundleId.trim().isEmpty()) {
            return null;
        }
        return mainBundleId.trim() + DOCUMENT_PROVIDER_EXTENSION_SUFFIX;
    }

    /// The document provider's App ID, honouring a target renamed through its build settings.
    ///
    /// The project can override PRODUCT_BUNDLE_IDENTIFIER for the generated target and the
    /// builder applies it, so provisioning the default name would create an App ID and profiles
    /// for a target nobody is building -- setup reporting success and the build then failing in
    /// signing. The provisioning preflight reads the same override.
    public static String documentProviderExtensionBundleId(String mainBundleId, String override) {
        if (override != null && !override.trim().isEmpty()) {
            return override.trim();
        }
        return documentProviderExtensionBundleId(mainBundleId);
    }

    /// The App Groups a project declares in ios.app_groups.
    ///
    /// Commas AND whitespace, which is how IPhoneBuilder reads the same hint and how the
    /// documented format writes it. Splitting on commas alone turned "group.a group.b" into one
    /// identifier: the wizard then tried to create a group by that impossible name instead of
    /// preserving the two the project already had, and the App ID lost both.
    public static java.util.List<String> declaredAppGroups(String declared) {
        java.util.List<String> groups = new java.util.ArrayList<String>();
        if (declared == null) {
            return groups;
        }
        for (String token : declared.split("[,\\s]+")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty() && !groups.contains(trimmed)) {
                groups.add(trimmed);
            }
        }
        return groups;
    }

    public static String defaultAppGroup(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return null;
        }
        return "group." + packageName.trim();
    }

    public static boolean profileRequiresDevices(String profileType) {
        return "IOS_APP_DEVELOPMENT".equals(profileType) || "IOS_APP_ADHOC".equals(profileType)
                || "MAC_APP_DEVELOPMENT".equals(profileType)
                || "MAC_CATALYST_APP_DEVELOPMENT".equals(profileType);
    }

    public static String requiredCertificateType(String profileType) {
        if ("IOS_APP_DEVELOPMENT".equals(profileType)) {
            return "IOS_DEVELOPMENT";
        }
        if ("MAC_APP_DEVELOPMENT".equals(profileType) || "MAC_CATALYST_APP_DEVELOPMENT".equals(profileType)) {
            return "MAC_APP_DEVELOPMENT";
        }
        if ("MAC_APP_STORE".equals(profileType) || "MAC_CATALYST_APP_STORE".equals(profileType)) {
            return "MAC_APP_DISTRIBUTION";
        }
        if ("MAC_APP_DIRECT".equals(profileType) || "MAC_CATALYST_APP_DIRECT".equals(profileType)) {
            return "DEVELOPER_ID_APPLICATION";
        }
        return "IOS_DISTRIBUTION";
    }

    public static List<SigningState.Certificate> compatibleCertificates(SigningState state, String profileType) {
        String required = requiredCertificateType(profileType);
        List<SigningState.Certificate> out = new ArrayList<SigningState.Certificate>();
        for (SigningState.Certificate c : state.certificates) {
            if ("ACTIVE".equals(c.status()) && required.equals(c.certificateType())
                    && c.appleCertId() != null && c.privateKeyPresent()) {
                out.add(c);
            }
        }
        return out;
    }

    /// The one input still missing before a profile can be created, phrased for the user, or null
    /// when nothing is. Reported in the same order the dialog lays the sections out, so the
    /// message always points at the first thing above the button rather than at whichever check
    /// happens to be written first.
    public static String describeMissingProfileInput(String profileType, String bundleId,
                                                     List<String> certificateIds, List<String> deviceIds,
                                                     String name) {
        if (profileType == null) {
            return "Choose a profile type to continue.";
        }
        if (bundleId == null) {
            return "Choose the bundle ID this profile is for.";
        }
        if (certificateIds == null || certificateIds.isEmpty()) {
            return "Choose a " + humanCertificateType(requiredCertificateType(profileType)) + " certificate.";
        }
        if (profileRequiresDevices(profileType) && (deviceIds == null || deviceIds.isEmpty())) {
            return "Select at least one device for this profile type.";
        }
        if (name == null || name.trim().isEmpty()) {
            return "Enter a name for the profile.";
        }
        return null;
    }

    private static String humanCertificateType(String certificateType) {
        if (certificateType == null) {
            return "signing";
        }
        return certificateType.replace("IOS_", "").replace("MAC_", "Mac ")
                .replace("DEVELOPER_ID_", "Developer ID ").replace('_', ' ').toLowerCase();
    }

    /// The certificates a profile of this type may be created against.
    ///
    /// Deliberately weaker than [#compatibleCertificates]: creating a profile sends only the
    /// certificate's Apple ID, so a locally stored private key is not needed for it. That key is
    /// needed to EXPORT the .p12 afterwards, which is why the auto-setup and reuse path insists
    /// on it -- but insisting on it here hides a perfectly valid certificate that came back from
    /// a sync with Apple and tells its owner to generate a second one they do not need.
    ///
    /// The type match is kept, because that one is not a preference: Apple rejects a profile
    /// whose certificate is the wrong kind for it.
    public static List<SigningState.Certificate> profileCertificateChoices(SigningState state, String profileType) {
        String required = requiredCertificateType(profileType);
        List<SigningState.Certificate> out = new ArrayList<SigningState.Certificate>();
        for (SigningState.Certificate c : state.certificates) {
            if ("ACTIVE".equals(c.status()) && required.equals(c.certificateType()) && c.appleCertId() != null) {
                out.add(c);
            }
        }
        return out;
    }

    /// Whether Apple will accept this device in a new profile. A device that has been disabled is
    /// still listed on the account, and putting its ID in the request gets the whole request
    /// rejected -- so the same predicate has to decide what a picker OFFERS and what automatic
    /// setup SENDS, or a "select all" quietly builds a request that cannot succeed.
    public static boolean isUsableDevice(SigningState.Device device) {
        return device != null && ("ENABLED".equals(device.status()) || "ACTIVE".equals(device.status()));
    }

    public static List<SigningState.Device> usableDevices(SigningState state) {
        List<SigningState.Device> out = new ArrayList<SigningState.Device>();
        for (SigningState.Device d : state.devices) {
            if (isUsableDevice(d)) {
                out.add(d);
            }
        }
        return out;
    }

    public static boolean canCreateProfile(String profileType, String bundleId, List<String> certificateIds,
                                           List<String> deviceIds, String name) {
        if (profileType == null || bundleId == null || certificateIds == null || certificateIds.isEmpty()) {
            return false;
        }
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return !profileRequiresDevices(profileType) || (deviceIds != null && !deviceIds.isEmpty());
    }
}
