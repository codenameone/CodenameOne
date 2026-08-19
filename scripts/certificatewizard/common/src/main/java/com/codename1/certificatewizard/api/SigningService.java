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
    void createAppGroup(String identifier, String name, OnComplete<Result<SigningState.AppGroup>> callback);
    void enableAppGroupCapability(String bundleIdAppleId, List<String> appGroupIds, OnComplete<Result<Void>> callback);
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
        /** What kind of failure this was, so the UI can offer the right next step. Null when ok. */
        public final SigningError error;

        private Result(boolean ok, T value, String message, SigningError error) {
            this.ok = ok;
            this.value = value;
            this.message = message;
            this.error = error;
        }

        public static <T> Result<T> ok(T value) {
            return new Result<T>(true, value, null, null);
        }

        public static <T> Result<T> fail(String message) {
            return new Result<T>(false, null, message == null ? "Operation failed" : message, null);
        }

        public static <T> Result<T> fail(SigningError error) {
            if (error == null) {
                return fail((String) null);
            }
            return new Result<T>(false, null, error.message(), error);
        }

        /** The failure kind, or {@link SigningError.Kind#OTHER} when there is nothing more specific. */
        public SigningError.Kind errorKind() {
            return error == null ? SigningError.Kind.OTHER : error.kind();
        }
    }
}
