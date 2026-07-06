package com.codename1.certificatewizard.api;

import com.codename1.util.OnComplete;

import java.util.List;

public interface SigningService {
    void refresh(OnComplete<Result<SigningState>> callback);
    void saveCredential(String keyId, String issuerId, String privateKeyP8, OnComplete<Result<Void>> callback);
    void deleteCredential(OnComplete<Result<Void>> callback);
    void createCertificate(String certificateType, String displayName, OnComplete<Result<Void>> callback);
    void reconcile(OnComplete<Result<Void>> callback);
    void revokeCertificate(Long id, OnComplete<Result<Void>> callback);
    default void createBundleId(String identifier, String name, boolean push, OnComplete<Result<Void>> callback) {
        createBundleId(identifier, name, "IOS", push, callback);
    }
    void createBundleId(String identifier, String name, String platform, boolean push, OnComplete<Result<Void>> callback);
    void registerDevice(String name, String udid, OnComplete<Result<Void>> callback);
    void createProfile(String name, String profileType, String bundleIdAppleId, List<String> certificateAppleIds,
                       List<String> deviceAppleIds, OnComplete<Result<Void>> callback);
    void deleteProfile(Long id, OnComplete<Result<Void>> callback);
    void saveApnsKey(String keyId, String teamId, String privateKeyP8, String displayName,
                     OnComplete<Result<Void>> callback);
    void deleteApnsKey(String keyId, OnComplete<Result<Void>> callback);
    void clearSigningData(OnComplete<Result<Void>> callback);
    void downloadP12(Long certificateId, String password, String suggestedName, OnComplete<Result<String>> callback);
    void downloadProfile(Long profileId, String suggestedName, OnComplete<Result<String>> callback);

    final class Result<T> {
        public final boolean ok;
        public final T value;
        public final String message;

        private Result(boolean ok, T value, String message) {
            this.ok = ok;
            this.value = value;
            this.message = message;
        }

        public static <T> Result<T> ok(T value) {
            return new Result<T>(true, value, null);
        }

        public static <T> Result<T> fail(String message) {
            return new Result<T>(false, null, message == null ? "Operation failed" : message);
        }
    }
}
