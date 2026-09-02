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

    /// Whether a certificate of this type can sign a profile that requires `required`.
    ///
    /// Not an equality test, because Apple's generic "Apple Development" (DEVELOPMENT) and "Apple
    /// Distribution" (DISTRIBUTION) types supersede the platform-specific ones and are valid
    /// wherever those are. An account whose certificates came back from a reconcile holding only
    /// those was told it had no compatible certificate at all and sent off to generate a
    /// redundant one. isDevelopmentCertificate in the wizard already counts DEVELOPMENT as a
    /// development certificate for both platforms, so this is the same reading applied to the
    /// distribution half.
    ///
    /// Deliberately not a "kind" test: DEVELOPER_ID_APPLICATION and MAC_INSTALLER_DISTRIBUTION are
    /// distribution certificates too, and neither can sign what the other is for. Only the two
    /// generic types widen anything.
    public static boolean certificateTypeSatisfies(String required, String certificateType) {
        if (required == null || certificateType == null) {
            return false;
        }
        if (required.equals(certificateType)) {
            return true;
        }
        if ("DEVELOPMENT".equals(certificateType)) {
            return "IOS_DEVELOPMENT".equals(required) || "MAC_APP_DEVELOPMENT".equals(required);
        }
        if ("DISTRIBUTION".equals(certificateType)) {
            return "IOS_DISTRIBUTION".equals(required) || "MAC_APP_DISTRIBUTION".equals(required);
        }
        return false;
    }

    public static List<SigningState.Certificate> compatibleCertificates(SigningState state, String profileType) {
        String required = requiredCertificateType(profileType);
        List<SigningState.Certificate> out = new ArrayList<SigningState.Certificate>();
        for (SigningState.Certificate c : state.certificates) {
            if ("ACTIVE".equals(c.status()) && certificateTypeSatisfies(required, c.certificateType())
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
            if ("ACTIVE".equals(c.status()) && certificateTypeSatisfies(required, c.certificateType())
                    && c.appleCertId() != null) {
                out.add(c);
            }
        }
        return out;
    }

    /// The App IDs that belong to the project the wizard was opened from: its own bundle
    /// identifier, and the ones the wizard derives from it for the extensions a build
    /// generates (`.CN1Widgets`, `.CN1Documents`).
    ///
    /// An account accumulates App IDs -- one per app, per test, per colleague -- and the
    /// profile dialog offered all of them with equal weight, which is a list to search
    /// rather than a choice to make when exactly one of them can sign this project
    /// (issue #5654). The full list stays one click away rather than being removed: a
    /// profile for a bundle the project does not declare is unusual, not wrong.
    ///
    /// Empty when the project's identifier is not registered at all, so a caller can tell
    /// "nothing to narrow to" from "narrowed to one" and fall back to the whole list
    /// instead of showing an empty picker.
    public static List<SigningState.BundleId> projectBundleIds(List<SigningState.BundleId> all,
                                                               String projectBundleId) {
        List<SigningState.BundleId> out = new ArrayList<SigningState.BundleId>();
        if (all == null || projectBundleId == null || projectBundleId.trim().isEmpty()) {
            return out;
        }
        String wanted = projectBundleId.trim();
        for (SigningState.BundleId b : all) {
            String identifier = b.identifier() == null ? null : b.identifier().trim();
            if (identifier == null) {
                continue;
            }
            if (identifier.equals(wanted) || identifier.startsWith(wanted + ".")) {
                out.add(b);
            }
        }
        return out;
    }

    /// Whether an App ID registered for `platform` covers a profile that needs `required`.
    ///
    /// Apple's App ID identifiers are unique across the whole account rather than per
    /// platform, so an identifier already registered for iOS cannot be registered a
    /// second time for macOS -- the attempt comes back as "An App ID with Identifier
    /// '...' is not available. Please enter a different string." (issue #5652). A
    /// registration marked UNIVERSAL already covers both, and one whose platform the
    /// service does not report cannot be told apart from it, so both count: reading them
    /// as "not the platform I need" is what sent automatic setup off to create the
    /// duplicate Apple always refuses.
    public static boolean bundlePlatformSatisfies(String required, String platform) {
        if (required == null || platform == null || platform.trim().isEmpty()) {
            return true;
        }
        String actual = platform.trim();
        return required.equals(actual) || "UNIVERSAL".equals(actual);
    }

    /// The App IDs a profile of this platform can be created against.
    ///
    /// The dialog used to list every App ID whatever the profile type was, so a Mac App
    /// Store profile could be pointed at an iOS App ID and Apple rejected the request --
    /// the same "the wizard said it was fine" gap the certificate list closed. The
    /// certificate section beside it is filtered exactly this way.
    ///
    /// Note this excludes the KNOWN WRONG platform rather than requiring the known right
    /// one, for the reason [#isUsableDevice] gives: UNIVERSAL covers both, and a value the
    /// service did not report must not empty the picker. Offering one App ID too many
    /// costs a rejected request; offering none costs the whole flow.
    public static List<SigningState.BundleId> usableBundleIds(List<SigningState.BundleId> all,
                                                              String requiredPlatform) {
        List<SigningState.BundleId> out = new ArrayList<SigningState.BundleId>();
        if (all == null) {
            return out;
        }
        for (SigningState.BundleId b : all) {
            if (bundlePlatformSatisfies(requiredPlatform, b.platform())) {
                out.add(b);
            }
        }
        return out;
    }

    /// The certificate types the wizard can generate, and how they are labelled.
    ///
    /// Kept here rather than inside the dialog so the set is one thing: every type
    /// [#requiredCertificateType] can ask for has to appear in it, or a picker that sends someone
    /// to the certificate dialog to satisfy that requirement leads them nowhere.
    public static final String[] GENERATABLE_CERTIFICATE_TYPES = {
        "IOS_DISTRIBUTION", "IOS_DEVELOPMENT", "MAC_APP_DISTRIBUTION",
        "MAC_APP_DEVELOPMENT", "DEVELOPER_ID_APPLICATION", "MAC_INSTALLER_DISTRIBUTION"};

    public static final String[] GENERATABLE_CERTIFICATE_LABELS = {
        "iOS Distribution", "iOS Development", "Mac App Store",
        "Mac Development", "Developer ID", "Mac Installer"};

    /// Whether a project's `ios.includePush` hint asks for push notifications.
    ///
    /// Read exactly the way IPhoneBuilder reads it -- absent is off, and only a trimmed,
    /// case-insensitive "true" is on -- because the App ID capability and the entitlement
    /// the build stamps on the app have to agree. Automatic setup used to pass a hardcoded
    /// true instead, so a project that never asked for push got an App ID carrying the
    /// capability, and every profile issued from it carried it too (issue #5657).
    public static boolean pushRequested(String includePushHint) {
        return includePushHint != null && "true".equalsIgnoreCase(includePushHint.trim());
    }

    /// Apple's BundleIdPlatform for the devices a profile of this type can name.
    public static String devicePlatformFor(String profileType) {
        return profileType != null
                && (profileType.startsWith("MAC_APP_") || profileType.startsWith("MAC_CATALYST_APP_"))
                ? "MAC_OS" : "IOS";
    }

    /// Whether Apple will accept this device in a new profile of this type. A device that has been
    /// disabled is still listed on the account, and naming it gets the whole request rejected --
    /// so the same predicate has to decide what a picker OFFERS and what automatic setup SENDS, or
    /// a "select all" quietly builds a request that cannot succeed. The platform matters the same
    /// way: an iPhone cannot be named in a Mac development profile.
    ///
    /// Note the platform test excludes the KNOWN WRONG one rather than requiring the known right
    /// one. The field is an untyped string in the API, documented only as "Apple
    /// BundleIdPlatform", so a value we did not anticipate -- UNIVERSAL, empty, something added
    /// later -- must not silently empty the picker and make a profile type uncreatable. Offering
    /// one device too many costs a rejected request; offering none costs the whole flow.
    public static boolean isUsableDevice(SigningState.Device device, String profileType) {
        if (device == null || !("ENABLED".equals(device.status()) || "ACTIVE".equals(device.status()))) {
            return false;
        }
        String platform = device.platform();
        if (platform == null || platform.trim().isEmpty()) {
            return true;
        }
        String wrong = "MAC_OS".equals(devicePlatformFor(profileType)) ? "IOS" : "MAC_OS";
        return !wrong.equals(platform.trim());
    }

    /// The subset of `selectedIds` that a profile of this type may still name.
    ///
    /// A selection outlives the profile type it was made under: pick devices for iOS Development,
    /// switch to Mac Development, and the picker correctly stops showing those iPhones while the
    /// selection quietly keeps them. canCreateProfile then reads a satisfied device requirement
    /// and the request goes to Apple naming devices of the wrong platform -- invisible, because
    /// nothing on screen shows them any more.
    public static List<String> retainUsableDevices(SigningState state, String profileType,
                                                   List<String> selectedIds) {
        List<String> out = new ArrayList<String>();
        if (selectedIds == null || !profileRequiresDevices(profileType)) {
            return out;
        }
        for (SigningState.Device d : usableDevices(state, profileType)) {
            if (selectedIds.contains(d.id())) {
                out.add(d.id());
            }
        }
        return out;
    }

    public static List<SigningState.Device> usableDevices(SigningState state, String profileType) {
        List<SigningState.Device> out = new ArrayList<SigningState.Device>();
        for (SigningState.Device d : state.devices) {
            if (isUsableDevice(d, profileType)) {
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
