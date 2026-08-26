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
