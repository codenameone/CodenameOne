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
package com.codename1.backend.storage.gcs;

import com.codename1.backend.Base64;
import com.codename1.backend.Config;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.security.oauth2.client.credentials.AccessTokenProvider;
import com.codename1.backend.security.oauth2.client.credentials.GoogleMetadataServerGrant;
import com.codename1.backend.security.oauth2.client.credentials.GoogleServiceAccountCredentials;
import com.codename1.backend.security.oauth2.client.credentials.TokenAcquisitionException;
import com.codename1.backend.storage.ObjectInfo;
import com.codename1.backend.storage.ObjectStorage;
import com.codename1.backend.storage.ObjectStorages;
import com.codename1.impl.backend.CivilTime;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Google Cloud Storage, through its JSON API, with no Google library.
///
/// Authenticated by a service-account key file, or -- on Compute Engine,
/// Cloud Run, GKE and App Engine -- by the machine's own service account from
/// the metadata server. [#forEmulator] talks to a local fake-gcs-server
/// without credentials.
///
/// Presigned URLs are V4 signed URLs. With a key file they are computed here, as
/// S3's are. With the metadata server there is no private key on the machine,
/// so each one costs a call to the IAM Credentials `signBlob` method, and the
/// service account needs `iam.serviceAccounts.signBlob` on itself
/// (the "Service Account Token Creator" role).
public final class GcsStorage implements ObjectStorage {
    private static final String API = "https://storage.googleapis.com";
    private static final String HOST = "storage.googleapis.com";
    private static final String SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final String SIGN_BLOB = "https://iamcredentials.googleapis.com/v1/"
            + "projects/-/serviceAccounts/";
    /// Google's ceiling for a V4 signed URL: seven days.
    private static final int MAX_EXPIRY = 604800;

    private final String base;
    private final String project;
    private final String location;
    private final AccessTokenProvider tokens;
    private final GoogleServiceAccountCredentials key;
    private String metadataEmail;

    private GcsStorage(String base, String project, String location, AccessTokenProvider tokens,
                       GoogleServiceAccountCredentials key) {
        this.base = base;
        this.project = project;
        this.location = location;
        this.tokens = tokens;
        this.key = key;
    }

    /// Storage acting as the service account `credentials` holds. `project` is
    /// needed only by [#createBucket]; null takes the key file's own.
    public static GcsStorage forCredentials(GoogleServiceAccountCredentials credentials,
                                            String project) {
        return new GcsStorage(API, project == null ? credentials.getProjectId() : project, null,
                new AccessTokenProvider(credentials.grant(SCOPE)), credentials);
    }

    /// Storage acting as the machine's own service account, from the metadata
    /// server. `project` is needed only by [#createBucket].
    public static GcsStorage forMetadataServer(String project) {
        return new GcsStorage(API, project, null,
                new AccessTokenProvider(new GoogleMetadataServerGrant()), null);
    }

    /// Storage on a local emulator (fake-gcs-server) at `endpoint`, such as
    /// `http://localhost:4443`, with no credentials. Presigned URLs from it
    /// carry no valid signature: the emulator does not check one.
    public static GcsStorage forEmulator(String endpoint, String project) {
        String e = endpoint.trim();
        if (!e.regionMatches(true, 0, "http://", 0, 7)
                && !e.regionMatches(true, 0, "https://", 0, 8)) {
            e = "http://" + e;
        }
        while (e.endsWith("/")) {
            e = e.substring(0, e.length() - 1);
        }
        return new GcsStorage(e, project, null, null, null);
    }

    /// The same storage, creating buckets in `location` (`US`, `EU`,
    /// `europe-west1`) rather than Google's default.
    public GcsStorage withLocation(String bucketLocation) {
        return new GcsStorage(base, project, bucketLocation, tokens, key);
    }

    /// Opens `gs://` URLs for [ObjectStorages]: `gs://` or `gs://<project>`.
    ///
    /// The key file is `cn1.storage.gcs.credentials`, else
    /// `GOOGLE_APPLICATION_CREDENTIALS`, else the metadata server answers. An
    /// emulator at `cn1.storage.gcs.emulator-host` or `STORAGE_EMULATOR_HOST`
    /// takes precedence over both. `cn1.storage.gcs.location` is where
    /// [#createBucket] puts a bucket.
    public static final class Factory implements ObjectStorages.Factory {
        @Override
        public ObjectStorage open(String url, Config config) throws IOException {
            String project = ObjectStorages.authority(url);
            if (project.length() == 0) {
                project = ObjectStorages.setting(config, "cn1.storage.gcs.project",
                        "GOOGLE_CLOUD_PROJECT");
            }
            String locationSetting = ObjectStorages.setting(config, "cn1.storage.gcs.location",
                    null);
            String emulator = ObjectStorages.setting(config, "cn1.storage.gcs.emulator-host",
                    "STORAGE_EMULATOR_HOST");
            GcsStorage storage;
            if (emulator != null) {
                storage = forEmulator(emulator, project);
            } else {
                String keyFile = ObjectStorages.setting(config, "cn1.storage.gcs.credentials",
                        "GOOGLE_APPLICATION_CREDENTIALS");
                storage = keyFile != null
                        ? forCredentials(GoogleServiceAccountCredentials.fromFile(keyFile), project)
                        : forMetadataServer(project);
            }
            return locationSetting == null ? storage : storage.withLocation(locationSetting);
        }
    }

    @Override
    public String getName() {
        return "gcs";
    }

    @Override
    public void createBucket(String bucket) throws IOException {
        requireBucket(bucket);
        if (project == null || project.length() == 0) {
            throw new IOException("Creating a GCS bucket needs a project: name it in the URL "
                    + "(gs://<project>), in cn1.storage.gcs.project or GOOGLE_CLOUD_PROJECT");
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("name", bucket);
        if (location != null) {
            body.put("location", location);
        }
        Web.Result r = call("POST", base + "/storage/v1/b?project=" + Aws.encode(project),
                "application/json", utf8(Json.write(body)));
        if (r.isSuccess()) {
            return;
        }
        // 409 is "the name is taken" -- by this project or by anybody, since
        // bucket names are global. Only a bucket this account can read is ours;
        // anything else is reported, never returned as success.
        if (r.getStatus() == 409
                && call("GET", base + "/storage/v1/b/" + Aws.encode(bucket), null, null)
                .isSuccess()) {
            return;
        }
        throw failure(r, "CREATE BUCKET", bucket, "");
    }

    @Override
    public String putObject(String bucket, String key, byte[] content, String contentType)
            throws IOException {
        requireBucket(bucket);
        requireKey(key);
        Web.Result r = call("POST", base + "/upload/storage/v1/b/" + Aws.encode(bucket)
                + "/o?uploadType=media&name=" + Aws.encode(key),
                contentType == null ? "application/octet-stream" : contentType,
                content == null ? new byte[0] : content);
        if (!r.isSuccess()) {
            throw failure(r, "PUT", bucket, key);
        }
        Object etag = Json.parseObject(r.getBodyAsString()).get("etag");
        return etag instanceof String ? (String) etag : "";
    }

    @Override
    public byte[] getObject(String bucket, String key) throws IOException {
        Web.Result r = call("GET", objectUrl(bucket, key) + "?alt=media", null, null);
        if (!r.isSuccess()) {
            throw failure(r, "GET", bucket, key);
        }
        return r.getBody();
    }

    @Override
    public ObjectInfo headObject(String bucket, String key) throws IOException {
        Web.Result r = call("GET", objectUrl(bucket, key), null, null);
        if (r.getStatus() == 404) {
            return null;
        }
        if (!r.isSuccess()) {
            throw failure(r, "HEAD", bucket, key);
        }
        Map meta = Json.parseObject(r.getBodyAsString());
        return new ObjectInfo(key, parseLong(meta.get("size")), text(meta, "contentType"),
                text(meta, "etag"), text(meta, "updated"));
    }

    @Override
    public void deleteObject(String bucket, String key) throws IOException {
        Web.Result r = call("DELETE", objectUrl(bucket, key), null, null);
        if (!r.isSuccess() && r.getStatus() != 404) {
            throw failure(r, "DELETE", bucket, key);
        }
    }

    @Override
    public List<String> listObjects(String bucket, String prefix, int max) throws IOException {
        if (max < 0) {
            throw new IllegalArgumentException("listObjects max must not be negative: " + max);
        }
        requireBucket(bucket);
        List<String> keys = new ArrayList<String>();
        String token = null;
        while (keys.size() < max) {
            StringBuilder url = new StringBuilder(base).append("/storage/v1/b/")
                    .append(Aws.encode(bucket)).append("/o?fields=items(name),nextPageToken")
                    .append("&maxResults=").append(Math.min(1000, max - keys.size()));
            if (prefix != null && prefix.length() > 0) {
                url.append("&prefix=").append(Aws.encode(prefix));
            }
            if (token != null) {
                url.append("&pageToken=").append(Aws.encode(token));
            }
            Web.Result r = call("GET", url.toString(), null, null);
            if (!r.isSuccess()) {
                throw failure(r, "LIST", bucket, prefix == null ? "" : prefix);
            }
            Map page = Json.parseObject(r.getBodyAsString());
            Object items = page.get("items");
            if (items instanceof List) {
                for (Object item : (List) items) {
                    if (item instanceof Map && ((Map) item).get("name") instanceof String
                            && keys.size() < max) {
                        keys.add((String) ((Map) item).get("name"));
                    }
                }
            }
            Object next = page.get("nextPageToken");
            if (!(next instanceof String) || ((String) next).length() == 0) {
                break;
            }
            token = (String) next;
        }
        return keys;
    }

    @Override
    public String presignGet(String bucket, String key, int seconds) throws IOException {
        return presign("GET", bucket, key, seconds, System.currentTimeMillis());
    }

    @Override
    public String presignPut(String bucket, String key, int seconds) throws IOException {
        return presign("PUT", bucket, key, seconds, System.currentTimeMillis());
    }

    /// A V4 signed URL (`GOOG4-RSA-SHA256`), path-style, signing the host alone
    /// so the client may send any headers.
    String presign(String method, String bucket, String key, int seconds, long now)
            throws IOException {
        String[] parts = signingParts(method, bucket, key, seconds, now, signerEmail());
        String signature = Aws.hex(sign(utf8(parts[2])));
        return (tokens == null ? base : API) + parts[0] + "?" + parts[1]
                + "&X-Goog-Signature=" + signature;
    }

    /// The encoded path, the canonical query and the string to sign of a V4
    /// signed URL, in that order. Package-private with an explicit clock and
    /// signer so a test can hold it to Google's conformance vectors.
    String[] signingParts(String method, String bucket, String key, int seconds, long now,
            String email) throws IOException {
        requireBucket(bucket);
        requireKey(key);
        if (seconds <= 0 || seconds > MAX_EXPIRY) {
            throw new IOException("A GCS signed URL lives 1 to " + MAX_EXPIRY
                    + " seconds, not " + seconds);
        }
        String stamp = timestamp(now);
        String scope = stamp.substring(0, 8) + "/auto/storage/goog4_request";
        String host = tokens == null ? hostOf(base) : HOST;
        String path = Aws.encodePath("/" + bucket + "/" + key);
        Map<String, String> query = new TreeMap<String, String>();
        query.put("X-Goog-Algorithm", "GOOG4-RSA-SHA256");
        query.put("X-Goog-Credential", email + "/" + scope);
        query.put("X-Goog-Date", stamp);
        query.put("X-Goog-Expires", String.valueOf(seconds));
        query.put("X-Goog-SignedHeaders", "host");
        String canonicalQuery = Aws.canonicalQuery(query);
        String canonical = method + "\n" + path + "\n" + canonicalQuery + "\nhost:" + host
                + "\n\nhost\nUNSIGNED-PAYLOAD";
        String toSign = "GOOG4-RSA-SHA256\n" + stamp + "\n" + scope + "\n"
                + Aws.sha256Hex(utf8(canonical));
        return new String[] {path, canonicalQuery, toSign};
    }

    private String signerEmail() throws IOException {
        if (key != null) {
            return key.getClientEmail();
        }
        if (tokens == null) {
            return "emulator@localhost";
        }
        synchronized (this) {
            if (metadataEmail == null) {
                metadataEmail = GoogleMetadataServerGrant.serviceAccountEmail();
            }
            return metadataEmail;
        }
    }

    private byte[] sign(byte[] data) throws IOException {
        if (key != null) {
            return key.signRsaSha256(data);
        }
        if (tokens == null) {
            // The emulator does not verify signatures; any bytes will do.
            return new byte[32];
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("payload", Base64.encode(data));
        Web.Result r = call("POST", SIGN_BLOB + Aws.encode(signerEmail()) + ":signBlob",
                "application/json", utf8(Json.write(body)));
        if (!r.isSuccess()) {
            throw new IOException("IAM signBlob failed with " + r.getStatus() + ": "
                    + errorMessage(r) + ". A presigned GCS URL without a key file needs the "
                    + "service account to hold iam.serviceAccounts.signBlob on itself");
        }
        Object signed = Json.parseObject(r.getBodyAsString()).get("signedBlob");
        if (!(signed instanceof String)) {
            throw new IOException("IAM signBlob answered without a signature");
        }
        return Base64.decode((String) signed);
    }

    private String objectUrl(String bucket, String key) {
        requireBucket(bucket);
        requireKey(key);
        return base + "/storage/v1/b/" + Aws.encode(bucket) + "/o/" + Aws.encode(key);
    }

    private Web.Result call(String method, String url, String contentType, byte[] body)
            throws IOException {
        List<String> headers = new ArrayList<String>(2);
        if (contentType != null) {
            headers.add("Content-Type: " + contentType);
        }
        if (tokens != null) {
            try {
                headers.add("Authorization: Bearer " + tokens.getTokenValue());
            } catch (TokenAcquisitionException err) {
                throw new IOException(err.getMessage(), err);
            }
        }
        return Web.request(method, url, headers, body);
    }

    /// GCS's own bucket rules, reduced to what keeps the name one URL segment
    /// and one host label: 3 to 222 characters of lowercase letter, digit, dot,
    /// hyphen or underscore. Google enforces the rest.
    private static void requireBucket(String bucket) {
        int length = bucket == null ? 0 : bucket.length();
        boolean ok = length >= 3 && length <= 222;
        for (int iter = 0 ; ok && iter < length ; iter++) {
            char c = bucket.charAt(iter);
            ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '-'
                    || c == '_';
        }
        if (!ok || bucket.indexOf("..") >= 0) {
            throw new IllegalArgumentException("Not a GCS bucket name: " + bucket);
        }
    }

    /// An object name is 1 to 1024 bytes of UTF-8 and may not be `.` or `..`.
    private static void requireKey(String key) {
        if (key == null || key.length() == 0 || utf8(key).length > 1024 || ".".equals(key)
                || "..".equals(key)) {
            throw new IllegalArgumentException("Not a GCS object name: " + key);
        }
    }

    private static IOException failure(Web.Result r, String operation, String bucket,
            String key) {
        return new IOException("GCS " + operation + " gs://" + bucket + "/" + key
                + " failed with " + r.getStatus() + ": " + errorMessage(r));
    }

    private static String errorMessage(Web.Result r) {
        String body = r.getBodyAsString();
        try {
            Object error = Json.parseObject(body).get("error");
            if (error instanceof Map && ((Map) error).get("message") instanceof String) {
                return (String) ((Map) error).get("message");
            }
        } catch (IOException | RuntimeException notJson) {
            // Fall through to the raw body: a proxy's HTML error page, say.
        }
        return body == null ? "" : body.length() > 200 ? body.substring(0, 200) : body;
    }

    private static String hostOf(String url) {
        int at = url.indexOf("://");
        String rest = at < 0 ? url : url.substring(at + 3);
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    private static String text(Map map, String name) {
        Object value = map.get(name);
        return value instanceof String ? (String) value : null;
    }

    /// GCS sends `size` as a string, to keep 64-bit values exact in JSON.
    private static long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException err) {
                return -1;
            }
        }
        return -1;
    }

    /// `yyyyMMdd'T'HHmmss'Z'` in UTC, by hand: locale-aware formatting could
    /// produce digits the service cannot reproduce in its own signature.
    static String timestamp(long millis) {
        long seconds = CivilTime.floorDiv(millis, 1000L);
        long days = CivilTime.floorDiv(seconds, 86400L);
        int secondOfDay = (int) (seconds - days * 86400L);
        StringBuilder out = new StringBuilder(16);
        CivilTime.appendDate(out, days, (char) 0).append('T');
        CivilTime.pad(out, secondOfDay / 3600, 2);
        CivilTime.pad(out, (secondOfDay / 60) % 60, 2);
        CivilTime.pad(out, secondOfDay % 60, 2);
        return out.append('Z').toString();
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing", err);
        }
    }
}
