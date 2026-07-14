package com.codename1.certificatewizard.api;

import com.codename1.util.OnComplete;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MockSigningService implements SigningService {
    private SigningState.Credential credential = new SigningState.Credential(true, "ABCD1234EF",
            "11111111-2222-3333-4444-555555555555");
    private final List<SigningState.Certificate> certificates = new ArrayList<SigningState.Certificate>();
    private final List<SigningState.BundleId> bundles = new ArrayList<SigningState.BundleId>();
    private final List<SigningState.Device> devices = new ArrayList<SigningState.Device>();
    private final List<SigningState.Profile> profiles = new ArrayList<SigningState.Profile>();
    private final List<SigningState.ApnsKey> apns = new ArrayList<SigningState.ApnsKey>();
    private final List<SigningState.AppGroup> appGroups = new ArrayList<SigningState.AppGroup>();
    private final Map<String, List<String>> appGroupAssociations = new LinkedHashMap<String, List<String>>();
    private long seq = 100;

    public MockSigningService() {
        long now = System.currentTimeMillis();
        certificates.add(new SigningState.Certificate(1L, "CERT9F2A", "IOS_DISTRIBUTION",
                "App Store Distribution", "4A2B9C7E11F0", now + 312L * 86400000L, "ACTIVE", true));
        certificates.add(new SigningState.Certificate(2L, "CERT3B7C", "IOS_DEVELOPMENT",
                "Development", "88DE1140AA92", now + 27L * 86400000L, "ACTIVE", true));
        certificates.add(new SigningState.Certificate(3L, "CERTMAC1", "MAC_APP_DISTRIBUTION",
                "Mac App Store Distribution", "MACD1140AA92", now + 300L * 86400000L, "ACTIVE", true));
        certificates.add(new SigningState.Certificate(4L, "CERTDEV1", "DEVELOPER_ID_APPLICATION",
                "Developer ID Application", "DEVID140AA92", now + 300L * 86400000L, "ACTIVE", true));
        bundles.add(new SigningState.BundleId("BID_A1", "com.example.myapp", "My App", "IOS", true));
        bundles.add(new SigningState.BundleId("BID_B2", "com.example.watch", "Watch App", "IOS", false));
        bundles.add(new SigningState.BundleId("BID_MAC", "com.example.myapp", "My App Mac", "MAC_OS", true));
        devices.add(new SigningState.Device("DEV_1", "Shai's iPhone", "00008120-000A1C3E0C68201E", "IOS", "ENABLED"));
        devices.add(new SigningState.Device("DEV_2", "QA iPad", "00008027-0004450E2688002E", "IOS", "ENABLED"));
        profiles.add(new SigningState.Profile(1L, "PRF_STORE", "My App App Store", "IOS_APP_STORE",
                "com.example.myapp", "STORE-UUID", now + 312L * 86400000L, "ACTIVE"));
        profiles.add(new SigningState.Profile(2L, "PRF_DEV", "My App Development", "IOS_APP_DEVELOPMENT",
                "com.example.myapp", "DEV-UUID", now + 27L * 86400000L, "ACTIVE"));
        profiles.add(new SigningState.Profile(3L, "PRF_MAC_STORE", "My App Mac App Store", "MAC_APP_STORE",
                "com.example.myapp", "MAC-STORE-UUID", now + 300L * 86400000L, "ACTIVE"));
        profiles.add(new SigningState.Profile(4L, "PRF_MAC_DIRECT", "My App Developer ID", "MAC_APP_DIRECT",
                "com.example.myapp", "MAC-DIRECT-UUID", now + 300L * 86400000L, "ACTIVE"));
        apns.add(new SigningState.ApnsKey("A1B2C3D4E5", "9WQ7X2K4LM", "Production APNs",
                now - 120L * 86400000L));
    }

    public void refresh(OnComplete<Result<SigningState>> callback) {
        callback.completed(Result.ok(snapshot()));
    }

    public void saveCredential(String keyId, String issuerId, String privateKeyP8, OnComplete<Result<Void>> callback) {
        credential = new SigningState.Credential(true, keyId, issuerId);
        callback.completed(Result.ok(null));
    }

    public void deleteCredential(OnComplete<Result<Void>> callback) {
        credential = new SigningState.Credential(false, null, null);
        callback.completed(Result.ok(null));
    }

    public void createCertificate(String certificateType, String displayName, OnComplete<Result<Void>> callback) {
        long id = ++seq;
        certificates.add(new SigningState.Certificate(id, "CERT" + id, certificateType,
                displayName == null || displayName.isEmpty() ? certificateType : displayName,
                "SER" + id, System.currentTimeMillis() + 365L * 86400000L, "ACTIVE", true));
        callback.completed(Result.ok(null));
    }

    public void reconcile(OnComplete<Result<Void>> callback) {
        callback.completed(Result.ok(null));
    }

    public void revokeCertificate(Long id, OnComplete<Result<Void>> callback) {
        for (int i = 0; i < certificates.size(); i++) {
            SigningState.Certificate c = certificates.get(i);
            if (c.id().equals(id)) {
                certificates.set(i, new SigningState.Certificate(c.id(), c.appleCertId(), c.certificateType(),
                        c.displayName(), c.serialNumber(), c.expiresAt(), "REVOKED", c.privateKeyPresent()));
            }
        }
        callback.completed(Result.ok(null));
    }

    public void createBundleId(String identifier, String name, String platform, boolean push,
                               OnComplete<Result<Void>> callback) {
        bundles.add(new SigningState.BundleId("BID_" + (++seq), identifier, name,
                platform == null || platform.trim().length() == 0 ? "IOS" : platform, push));
        callback.completed(Result.ok(null));
    }

    public void createAppGroup(String identifier, String name, OnComplete<Result<SigningState.AppGroup>> callback) {
        for (SigningState.AppGroup g : appGroups) {
            if (g.identifier() != null && g.identifier().equals(identifier)) {
                callback.completed(Result.ok(g));
                return;
            }
        }
        SigningState.AppGroup created = new SigningState.AppGroup("GRP_" + (++seq), identifier, name);
        appGroups.add(created);
        callback.completed(Result.ok(created));
    }

    public void enableAppGroupCapability(String bundleIdAppleId, List<String> appGroupIds,
                                         OnComplete<Result<Void>> callback) {
        appGroupAssociations.put(bundleIdAppleId,
                appGroupIds == null ? new ArrayList<String>() : new ArrayList<String>(appGroupIds));
        callback.completed(Result.ok(null));
    }

    public List<String> appGroupAssociation(String bundleIdAppleId) {
        List<String> assoc = appGroupAssociations.get(bundleIdAppleId);
        return assoc == null ? new ArrayList<String>() : new ArrayList<String>(assoc);
    }

    public void registerDevice(String name, String udid, OnComplete<Result<Void>> callback) {
        devices.add(new SigningState.Device("DEV_" + (++seq), name, udid, "IOS", "ENABLED"));
        callback.completed(Result.ok(null));
    }

    public void createProfile(String name, String profileType, String bundleIdAppleId, List<String> certificateAppleIds,
                              List<String> deviceAppleIds, OnComplete<Result<Void>> callback) {
        String bundle = bundleIdAppleId;
        for (SigningState.BundleId b : bundles) {
            if (b.id().equals(bundleIdAppleId)) {
                bundle = b.identifier();
            }
        }
        profiles.add(new SigningState.Profile(++seq, "PRF_" + seq, name, profileType, bundle,
                "UUID-" + seq, System.currentTimeMillis() + 365L * 86400000L, "ACTIVE"));
        callback.completed(Result.ok(null));
    }

    public void deleteProfile(Long id, OnComplete<Result<Void>> callback) {
        for (int i = profiles.size() - 1; i >= 0; i--) {
            if (profiles.get(i).id().equals(id)) {
                profiles.remove(i);
            }
        }
        callback.completed(Result.ok(null));
    }

    public void saveApnsKey(String keyId, String teamId, String privateKeyP8, String displayName,
                            OnComplete<Result<Void>> callback) {
        apns.add(new SigningState.ApnsKey(keyId, teamId, displayName, System.currentTimeMillis()));
        callback.completed(Result.ok(null));
    }

    public void deleteApnsKey(String keyId, OnComplete<Result<Void>> callback) {
        for (int i = apns.size() - 1; i >= 0; i--) {
            if (apns.get(i).keyId().equals(keyId)) {
                apns.remove(i);
            }
        }
        callback.completed(Result.ok(null));
    }

    public void clearSigningData(OnComplete<Result<Void>> callback) {
        credential = new SigningState.Credential(false, null, null);
        certificates.clear();
        bundles.clear();
        devices.clear();
        profiles.clear();
        apns.clear();
        appGroups.clear();
        appGroupAssociations.clear();
        callback.completed(Result.ok(null));
    }

    public void downloadP12(Long certificateId, String password, String suggestedName, OnComplete<Result<String>> callback) {
        callback.completed(Result.ok("/tmp/" + suggestedName));
    }

    public void downloadProfile(Long profileId, String suggestedName, OnComplete<Result<String>> callback) {
        callback.completed(Result.ok("/tmp/" + suggestedName));
    }

    private SigningState snapshot() {
        return new SigningState(credential, certificates, bundles, devices, profiles, apns, appGroups);
    }
}
