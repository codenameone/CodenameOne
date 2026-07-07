package com.codename1.certificatewizard.api;

import com.codename1.certificatewizard.cloud.APNsKeysApi;
import com.codename1.certificatewizard.cloud.BundleIDsApi;
import com.codename1.certificatewizard.cloud.CertificatesApi;
import com.codename1.certificatewizard.cloud.CredentialApi;
import com.codename1.certificatewizard.cloud.DevicesApi;
import com.codename1.certificatewizard.cloud.ProfilesApi;
import com.codename1.certificatewizard.cloud.model.ApnsKeyRequest;
import com.codename1.certificatewizard.cloud.model.ApnsKeyStatus;
import com.codename1.certificatewizard.cloud.model.AscCredentialRequest;
import com.codename1.certificatewizard.cloud.model.AscCredentialStatus;
import com.codename1.certificatewizard.cloud.model.BundleIdDTO;
import com.codename1.certificatewizard.cloud.model.CapabilityRequest;
import com.codename1.certificatewizard.cloud.model.CertDTO;
import com.codename1.certificatewizard.cloud.model.CreateBundleIdRequest;
import com.codename1.certificatewizard.cloud.model.CreateCertRequest;
import com.codename1.certificatewizard.cloud.model.CreateProfileRequest;
import com.codename1.certificatewizard.cloud.model.DeviceDTO;
import com.codename1.certificatewizard.cloud.model.ProfileDTO;
import com.codename1.certificatewizard.cloud.model.RegisterDeviceRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.NetworkEvent;
import com.codename1.io.Util;
import com.codename1.io.rest.Response;
import com.codename1.io.rest.Rest;
import com.codename1.util.OnComplete;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public final class CloudSigningService implements SigningService {
    public static final String DEFAULT_BASE_URL = "https://cloud.codenameone.com";

    private final String baseUrl;
    private final String bearerToken;
    private final String downloadDir;

    private final CredentialApi credentialApi;
    private final CertificatesApi certificatesApi;
    private final BundleIDsApi bundleIdsApi;
    private final DevicesApi devicesApi;
    private final ProfilesApi profilesApi;
    private final APNsKeysApi apnsKeysApi;

    public CloudSigningService(String baseUrl, String token, String downloadDir) {
        this.baseUrl = baseUrl == null || baseUrl.trim().isEmpty() ? DEFAULT_BASE_URL : baseUrl.trim();
        this.bearerToken = normalizeBearer(token);
        this.downloadDir = downloadDir;
        credentialApi = CredentialApi.of(this.baseUrl);
        certificatesApi = CertificatesApi.of(this.baseUrl);
        bundleIdsApi = BundleIDsApi.of(this.baseUrl);
        devicesApi = DevicesApi.of(this.baseUrl);
        profilesApi = ProfilesApi.of(this.baseUrl);
        apnsKeysApi = APNsKeysApi.of(this.baseUrl);
    }

    public void refresh(OnComplete<Result<SigningState>> callback) {
        if (!hasToken()) {
            callback.completed(Result.ok(SigningState.empty()));
            return;
        }
        final AscCredentialStatus[] cred = new AscCredentialStatus[1];
        final List<CertDTO>[] certs = new List[1];
        final List<BundleIdDTO>[] bundles = new List[1];
        final List<DeviceDTO>[] devices = new List[1];
        final List<ProfileDTO>[] profiles = new List[1];
        final List<ApnsKeyStatus>[] apns = new List[1];

        credentialApi.getCredential(bearerToken, r1 -> {
            if (authFailure(r1)) {
                callback.completed(Result.ok(SigningState.empty()));
                return;
            }
            if (!ok(r1)) {
                callback.completed(Result.fail(message(r1)));
                return;
            }
            cred[0] = r1.getResponseData();
            if (cred[0] == null || !Boolean.TRUE.equals(cred[0].configured())) {
                callback.completed(Result.ok(toState(cred[0], null, null, null, null, null)));
                return;
            }
            certificatesApi.listCertificates(bearerToken, r2 -> {
                if (authFailure(r2)) {
                    callback.completed(Result.ok(SigningState.empty()));
                    return;
                }
                if (!ok(r2)) {
                    callback.completed(Result.fail(message(r2)));
                    return;
                }
                certs[0] = r2.getResponseData();
                bundleIdsApi.listBundleIds(bearerToken, r3 -> {
                    if (authFailure(r3)) {
                        callback.completed(Result.ok(SigningState.empty()));
                        return;
                    }
                    if (!ok(r3)) {
                        callback.completed(Result.fail(message(r3)));
                        return;
                    }
                    bundles[0] = r3.getResponseData();
                    devicesApi.listDevices(bearerToken, r4 -> {
                        if (authFailure(r4)) {
                            callback.completed(Result.ok(SigningState.empty()));
                            return;
                        }
                        if (!ok(r4)) {
                            callback.completed(Result.fail(message(r4)));
                            return;
                        }
                        devices[0] = r4.getResponseData();
                        profilesApi.listProfiles(bearerToken, r5 -> {
                            if (authFailure(r5)) {
                                callback.completed(Result.ok(SigningState.empty()));
                                return;
                            }
                            if (!ok(r5)) {
                                callback.completed(Result.fail(message(r5)));
                                return;
                            }
                            profiles[0] = r5.getResponseData();
                            apnsKeysApi.listApnsKeys(bearerToken, r6 -> {
                                if (authFailure(r6)) {
                                    callback.completed(Result.ok(SigningState.empty()));
                                    return;
                                }
                                if (!ok(r6)) {
                                    callback.completed(Result.fail(message(r6)));
                                    return;
                                }
                                apns[0] = r6.getResponseData();
                                callback.completed(Result.ok(toState(cred[0], certs[0], bundles[0],
                                        devices[0], profiles[0], apns[0])));
                            });
                        });
                    });
                });
            });
        });
    }

    public void saveCredential(String keyId, String issuerId, String privateKeyP8, OnComplete<Result<Void>> callback) {
        credentialApi.putCredential(new AscCredentialRequest(keyId, issuerId, privateKeyP8), bearerToken,
                r -> done(r, callback));
    }

    public void deleteCredential(OnComplete<Result<Void>> callback) {
        credentialApi.deleteCredential(bearerToken, r -> done(r, callback));
    }

    public void createCertificate(String certificateType, String displayName, OnComplete<Result<Void>> callback) {
        certificatesApi.createCertificate(new CreateCertRequest(certificateType, displayName), bearerToken,
                r -> done(r, callback));
    }

    public void reconcile(OnComplete<Result<Void>> callback) {
        certificatesApi.reconcileCertificates(bearerToken, r -> done(r, callback));
    }

    public void revokeCertificate(Long id, OnComplete<Result<Void>> callback) {
        certificatesApi.revokeCertificate(id, bearerToken, r -> done(r, callback));
    }

    public void createBundleId(String identifier, String name, String platform, boolean push,
                               OnComplete<Result<Void>> callback) {
        bundleIdsApi.createBundleId(new CreateBundleIdRequest(identifier, name, platform), bearerToken, r -> {
            if (!ok(r)) {
                callback.completed(Result.fail(message(r)));
                return;
            }
            BundleIdDTO created = r.getResponseData();
            if (push && created != null && created.id() != null) {
                bundleIdsApi.enableCapability(created.id(), new CapabilityRequest("PUSH_NOTIFICATIONS"),
                        bearerToken, rr -> done(rr, callback));
            } else {
                callback.completed(Result.ok(null));
            }
        });
    }

    public void registerDevice(String name, String udid, OnComplete<Result<Void>> callback) {
        devicesApi.registerDevice(new RegisterDeviceRequest(name, udid, "IOS"), bearerToken, r -> done(r, callback));
    }

    public void createProfile(String name, String profileType, String bundleIdAppleId, List<String> certificateAppleIds,
                              List<String> deviceAppleIds, OnComplete<Result<Void>> callback) {
        profilesApi.createProfile(new CreateProfileRequest(name, profileType, bundleIdAppleId,
                certificateAppleIds, deviceAppleIds), bearerToken, r -> done(r, callback));
    }

    public void deleteProfile(Long id, OnComplete<Result<Void>> callback) {
        profilesApi.deleteProfile(id, bearerToken, r -> done(r, callback));
    }

    public void saveApnsKey(String keyId, String teamId, String privateKeyP8, String displayName,
                            OnComplete<Result<Void>> callback) {
        apnsKeysApi.putApnsKey(new ApnsKeyRequest(keyId, teamId, privateKeyP8, displayName), bearerToken,
                r -> done(r, callback));
    }

    public void deleteApnsKey(String keyId, OnComplete<Result<Void>> callback) {
        apnsKeysApi.deleteApnsKey(keyId, bearerToken, r -> done(r, callback));
    }

    public void clearSigningData(OnComplete<Result<Void>> callback) {
        credentialApi.clearSigningData(bearerToken, r -> done(r, callback));
    }

    public void downloadP12(Long certificateId, String password, String suggestedName, OnComplete<Result<String>> callback) {
        String url = baseUrl + "/appsec/7.0/apple/certificates/" + certificateId + "/p12";
        Rest.get(url).queryParam("password", password == null ? "" : password)
                .header("Authorization", bearerToken)
                .onError(evt -> {
                    evt.consume();
                    callback.completed(Result.fail(networkMessage(evt)));
                }, false)
                .onErrorCodeBytes(r -> saveBytes(r, suggestedName, callback))
                .fetchAsBytes(r -> saveBytes(r, suggestedName, callback));
    }

    public void downloadProfile(Long profileId, String suggestedName, OnComplete<Result<String>> callback) {
        String url = baseUrl + "/appsec/7.0/apple/profiles/" + profileId + "/download";
        Rest.get(url).header("Authorization", bearerToken)
                .onError(evt -> {
                    evt.consume();
                    callback.completed(Result.fail(networkMessage(evt)));
                }, false)
                .onErrorCodeBytes(r -> saveBytes(r, suggestedName, callback))
                .fetchAsBytes(r -> saveBytes(r, suggestedName, callback));
    }

    private boolean hasToken() {
        return bearerToken != null && bearerToken.length() > "Bearer ".length();
    }

    private static String normalizeBearer(String token) {
        if (token == null || token.trim().isEmpty()) {
            return "";
        }
        String t = token.trim();
        return t.startsWith("Bearer ") ? t : "Bearer " + t;
    }

    private static boolean ok(Response<?> r) {
        return r != null && r.getResponseCode() >= 200 && r.getResponseCode() < 300;
    }

    private static boolean authFailure(Response<?> r) {
        return r != null && (r.getResponseCode() == 401 || r.getResponseCode() == 403);
    }

    private static String message(Response<?> r) {
        if (r == null) {
            return "No response from server";
        }
        int code = r.getResponseCode();
        if (authFailure(r)) {
            return "Codename One login expired. Run the wizard again to refresh the sign-in token.";
        }
        if (code == 503) {
            return "Codename One cloud signing service is unavailable (HTTP 503). Try again later.";
        }
        if (code >= 500) {
            return "Codename One cloud signing service failed (HTTP " + code + "). Try again later.";
        }
        if (code <= 0) {
            String transport = r.getResponseErrorMessage();
            if (isTransportArtifact(transport)) {
                return "Could not read the server response. Try again later.";
            }
            return transport == null || transport.isEmpty() ? "Could not reach the Codename One cloud service."
                    : "Connection failed: " + transport;
        }
        String msg = r.getResponseErrorMessage();
        if ((msg == null || msg.isEmpty()) && r.getResponseData() instanceof String) {
            msg = (String) r.getResponseData();
        }
        if (isTransportArtifact(msg)) {
            return "Request failed (HTTP " + code + "). Try again later.";
        }
        return msg == null || msg.isEmpty() ? "HTTP " + r.getResponseCode() : msg;
    }

    private static boolean isTransportArtifact(String msg) {
        if (msg == null) {
            return false;
        }
        String m = msg.trim().toLowerCase();
        return m.equals("stream closed") || m.equals("socket closed") || m.equals("unexpected end of stream")
                || m.equals("premature eof") || m.equals("connection reset");
    }

    private static String networkMessage(NetworkEvent evt) {
        if (evt == null) {
            return "Network request failed";
        }
        String message = evt.getMessage();
        if ((message == null || message.trim().isEmpty()) && evt.getError() != null) {
            message = evt.getError().getMessage();
        }
        if (message == null || message.trim().isEmpty()) {
            message = "Network request failed";
        }
        if (evt.getResponseCode() > 0) {
            return "Codename One cloud request failed (HTTP " + evt.getResponseCode() + "): " + message;
        }
        return message;
    }

    private static void done(Response<?> r, OnComplete<Result<Void>> callback) {
        callback.completed(ok(r) ? Result.ok(null) : Result.<Void>fail(message(r)));
    }

    private SigningState toState(AscCredentialStatus cred, List<CertDTO> certs, List<BundleIdDTO> bundles,
                                 List<DeviceDTO> devices, List<ProfileDTO> profiles, List<ApnsKeyStatus> apns) {
        List<SigningState.Certificate> outCerts = new ArrayList<SigningState.Certificate>();
        if (certs != null) {
            for (CertDTO c : certs) {
                outCerts.add(new SigningState.Certificate(c.id(), c.appleCertId(), c.certificateType(),
                        c.displayName(), c.serialNumber(), c.expiresAt(), c.status(),
                        Boolean.TRUE.equals(c.privateKeyPresent())));
            }
        }
        List<SigningState.BundleId> outBundles = new ArrayList<SigningState.BundleId>();
        if (bundles != null) {
            for (BundleIdDTO b : bundles) {
                outBundles.add(new SigningState.BundleId(b.id(), b.identifier(), b.name(), b.platform(), false));
            }
        }
        List<SigningState.Device> outDevices = new ArrayList<SigningState.Device>();
        if (devices != null) {
            for (DeviceDTO d : devices) {
                outDevices.add(new SigningState.Device(d.id(), d.name(), d.udid(), d.platform(), d.status()));
            }
        }
        List<SigningState.Profile> outProfiles = new ArrayList<SigningState.Profile>();
        if (profiles != null) {
            for (ProfileDTO p : profiles) {
                outProfiles.add(new SigningState.Profile(p.id(), p.appleProfileId(), p.name(), p.profileType(),
                        p.bundleId(), p.uuid(), p.expiresAt(), p.status()));
            }
        }
        List<SigningState.ApnsKey> outApns = new ArrayList<SigningState.ApnsKey>();
        if (apns != null) {
            for (ApnsKeyStatus a : apns) {
                outApns.add(new SigningState.ApnsKey(a.keyId(), a.teamId(), a.displayName(), a.createdAt()));
            }
        }
        SigningState.Credential c = cred == null ? new SigningState.Credential(false, null, null)
                : new SigningState.Credential(Boolean.TRUE.equals(cred.configured()), cred.keyId(), cred.issuerId());
        return new SigningState(c, outCerts, outBundles, outDevices, outProfiles, outApns);
    }

    private void saveBytes(Response<byte[]> response, String suggestedName, OnComplete<Result<String>> callback) {
        if (!ok(response)) {
            callback.completed(Result.fail(message(response)));
            return;
        }
        byte[] data = response.getResponseData();
        if (data == null) {
            callback.completed(Result.fail("Server returned no data"));
            return;
        }
        String dir = downloadDir == null || downloadDir.isEmpty()
                ? FileSystemStorage.getInstance().getAppHomePath() : downloadDir;
        String path = join(dir, suggestedName);
        OutputStream out = null;
        try {
            out = FileSystemStorage.getInstance().openOutputStream(path);
            out.write(data);
            out.flush();
            callback.completed(Result.ok(path));
        } catch (IOException ex) {
            callback.completed(Result.fail(ex.getMessage()));
        } finally {
            Util.cleanup(out);
        }
    }

    private static String join(String dir, String name) {
        if (dir.endsWith("/") || dir.endsWith("\\")) {
            return dir + name;
        }
        return dir + "/" + name;
    }
}
