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
package com.codename1.backend.storage.azure;

import com.codename1.backend.Base64;
import com.codename1.backend.Config;
import com.codename1.backend.Crypto;
import com.codename1.backend.Http1Date;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.security.oauth2.client.credentials.AccessToken;
import com.codename1.backend.security.oauth2.client.credentials.AccessTokenGrant;
import com.codename1.backend.security.oauth2.client.credentials.AccessTokenProvider;
import com.codename1.backend.security.oauth2.client.credentials.TokenAcquisitionException;
import com.codename1.backend.storage.ObjectInfo;
import com.codename1.backend.storage.ObjectStorage;
import com.codename1.backend.storage.ObjectStorages;
import com.codename1.impl.backend.CivilTime;
import com.codename1.impl.backend.FlatXml;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/// Azure Blob Storage, through its REST API, with no Azure library. A bucket
/// here is a container and an object a block blob.
///
/// Two ways to authenticate:
///
/// - **An account key** (Shared Key), from the key itself or a connection
///   string. Presigned URLs are service SAS tokens computed here.
/// - **A managed identity** (Microsoft Entra ID), on an Azure VM, App Service,
///   Container Apps or Functions, with no secret on the machine. The identity
///   needs a data-plane role such as "Storage Blob Data Contributor". Presigned
///   URLs are then user-delegation SAS tokens, which costs one call to fetch a
///   delegation key a day.
///
/// [#forConnectionString] with `UseDevelopmentStorage=true` talks to a local
/// Azurite.
public final class AzureBlobStorage implements ObjectStorage {
    /// The REST version every request and SAS is made for. Pinned: a newer
    /// one changes the SAS string-to-sign.
    static final String VERSION = "2021-08-06";
    private static final String RESOURCE = "https://storage.azure.com/";
    private static final String AZURITE_ACCOUNT = "devstoreaccount1";
    private static final String AZURITE_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsu"
            + "Fq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    /// How far ahead a delegation key is asked for. Azure allows seven days.
    private static final long DELEGATION_LIFETIME = 24L * 3600 * 1000;

    private final String account;
    private final String base;
    private final String basePath;
    private final byte[] accountKey;
    private final AccessTokenProvider tokens;
    private DelegationKey delegation;

    private AzureBlobStorage(String account, String base, byte[] accountKey,
                             AccessTokenProvider tokens) {
        requireAccount(account);
        this.account = account;
        String b = base;
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        this.base = b;
        int scheme = b.indexOf("://");
        int slash = b.indexOf('/', scheme < 0 ? 0 : scheme + 3);
        this.basePath = slash < 0 ? "" : b.substring(slash);
        this.accountKey = accountKey;
        this.tokens = tokens;
    }

    /// The account `account`, authenticated with its base64 access key.
    public static AzureBlobStorage forAccountKey(String account, String key) {
        return new AzureBlobStorage(account, defaultEndpoint(account), Base64.decode(key), null);
    }

    /// The account `account`, authenticated as the machine's managed identity.
    /// `clientId` picks a user-assigned identity; null takes the system one.
    public static AzureBlobStorage forManagedIdentity(String account, String clientId) {
        return new AzureBlobStorage(account, defaultEndpoint(account), null,
                new AccessTokenProvider(new ManagedIdentityGrant(clientId)));
    }

    /// The account a connection string from the Azure portal names, or Azurite
    /// for `UseDevelopmentStorage=true`. It must carry an `AccountKey`; a
    /// connection string holding only a SAS is not supported.
    ///
    /// @throws IOException when the string names no account or key
    public static AzureBlobStorage forConnectionString(String connectionString)
            throws IOException {
        Map<String, String> parts = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        for (String part : split(connectionString, ';')) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                parts.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        if ("true".equalsIgnoreCase(parts.get("UseDevelopmentStorage"))) {
            String proxy = parts.get("DevelopmentStorageProxyUri");
            return new AzureBlobStorage(AZURITE_ACCOUNT, (proxy == null
                    ? "http://127.0.0.1:10000" : proxy) + "/" + AZURITE_ACCOUNT,
                    Base64.decode(AZURITE_KEY), null);
        }
        String name = parts.get("AccountName");
        String key = parts.get("AccountKey");
        if (name == null || key == null) {
            throw new IOException("The Azure connection string has no AccountName or no "
                    + "AccountKey");
        }
        String endpoint = parts.get("BlobEndpoint");
        if (endpoint == null) {
            String protocol = parts.get("DefaultEndpointsProtocol");
            String suffix = parts.get("EndpointSuffix");
            endpoint = (protocol == null ? "https" : protocol) + "://" + name + ".blob."
                    + (suffix == null ? "core.windows.net" : suffix);
        }
        return new AzureBlobStorage(name, endpoint, Base64.decode(key), null);
    }

    /// The same account and credentials at another endpoint: a sovereign cloud
    /// (`https://<account>.blob.core.chinacloudapi.cn`), a private endpoint, or an
    /// emulator, whose URL then includes the account path.
    public AzureBlobStorage withEndpoint(String endpoint) {
        return new AzureBlobStorage(account, endpoint, accountKey, tokens);
    }

    private static String defaultEndpoint(String account) {
        requireAccount(account);
        return "https://" + account + ".blob.core.windows.net";
    }

    /// Opens `azblob://` URLs for [ObjectStorages]: `azblob://<account>`.
    ///
    /// Credentials, first match wins: `cn1.storage.azure.connection-string` or
    /// `AZURE_STORAGE_CONNECTION_STRING` (the account in the URL may then be
    /// omitted), `cn1.storage.azure.account-key` or `AZURE_STORAGE_KEY`, else the
    /// managed identity, user-assigned when `cn1.storage.azure.client-id` or
    /// `AZURE_CLIENT_ID` names one. `cn1.storage.azure.endpoint` overrides the
    /// endpoint.
    public static final class Factory implements ObjectStorages.Factory {
        @Override
        public ObjectStorage open(String url, Config config) throws IOException {
            String name = ObjectStorages.authority(url);
            String connection = ObjectStorages.setting(config,
                    "cn1.storage.azure.connection-string", "AZURE_STORAGE_CONNECTION_STRING");
            AzureBlobStorage storage;
            if (connection != null) {
                storage = forConnectionString(connection);
                if (name.length() > 0 && !name.equals(storage.account)) {
                    throw new IOException("cn1.storage.url names the Azure account " + name
                            + " but the connection string is for " + storage.account);
                }
            } else {
                if (name.length() == 0) {
                    throw new IOException("The Azure storage URL names no account: " + url);
                }
                String key = ObjectStorages.setting(config, "cn1.storage.azure.account-key",
                        "AZURE_STORAGE_KEY");
                storage = key != null ? forAccountKey(name, key)
                        : forManagedIdentity(name, ObjectStorages.setting(config,
                        "cn1.storage.azure.client-id", "AZURE_CLIENT_ID"));
            }
            String endpoint = ObjectStorages.setting(config, "cn1.storage.azure.endpoint", null);
            return endpoint == null ? storage : storage.withEndpoint(endpoint);
        }
    }

    @Override
    public String getName() {
        return "azure";
    }

    @Override
    public void createBucket(String bucket) throws IOException {
        requireContainer(bucket);
        Map<String, String> query = new TreeMap<String, String>();
        query.put("restype", "container");
        Web.Result r = send("PUT", "/" + bucket, query, null, new byte[0]);
        // Container names are per account, so "already exists" is ours.
        if (r.isSuccess() || r.getStatus() == 409
                && "ContainerAlreadyExists".equals(errorCode(r))) {
            return;
        }
        throw failure(r, "CREATE CONTAINER", bucket, "");
    }

    @Override
    public String putObject(String bucket, String key, byte[] content, String contentType)
            throws IOException {
        List<String> headers = new ArrayList<String>(2);
        headers.add("x-ms-blob-type: BlockBlob");
        headers.add("Content-Type: " + (contentType == null
                ? "application/octet-stream" : contentType));
        Web.Result r = send("PUT", blobPath(bucket, key), null, headers,
                content == null ? new byte[0] : content);
        if (!r.isSuccess()) {
            throw failure(r, "PUT", bucket, key);
        }
        return unquote(r.getHeader("etag"));
    }

    @Override
    public byte[] getObject(String bucket, String key) throws IOException {
        Web.Result r = send("GET", blobPath(bucket, key), null, null, null);
        if (!r.isSuccess()) {
            throw failure(r, "GET", bucket, key);
        }
        return r.getBody();
    }

    @Override
    public ObjectInfo headObject(String bucket, String key) throws IOException {
        Web.Result r = send("HEAD", blobPath(bucket, key), null, null, null);
        if (r.getStatus() == 404) {
            return null;
        }
        if (!r.isSuccess()) {
            throw failure(r, "HEAD", bucket, key);
        }
        long size = -1;
        String length = r.getHeader("content-length");
        if (length != null) {
            try {
                size = Long.parseLong(length.trim());
            } catch (NumberFormatException err) {
                size = -1;
            }
        }
        return new ObjectInfo(key, size, r.getHeader("content-type"),
                unquote(r.getHeader("etag")), r.getHeader("last-modified"));
    }

    @Override
    public void deleteObject(String bucket, String key) throws IOException {
        Web.Result r = send("DELETE", blobPath(bucket, key), null, null, null);
        if (!r.isSuccess() && r.getStatus() != 404) {
            throw failure(r, "DELETE", bucket, key);
        }
    }

    @Override
    public List<String> listObjects(String bucket, String prefix, int max) throws IOException {
        if (max < 0) {
            throw new IllegalArgumentException("listObjects max must not be negative: " + max);
        }
        requireContainer(bucket);
        List<String> keys = new ArrayList<String>();
        String marker = null;
        while (keys.size() < max) {
            Map<String, String> query = new TreeMap<String, String>();
            query.put("restype", "container");
            query.put("comp", "list");
            query.put("maxresults", String.valueOf(Math.min(5000, max - keys.size())));
            if (prefix != null && prefix.length() > 0) {
                query.put("prefix", prefix);
            }
            if (marker != null) {
                query.put("marker", marker);
            }
            Web.Result r = send("GET", "/" + bucket, query, null, null);
            if (!r.isSuccess()) {
                throw failure(r, "LIST", bucket, prefix == null ? "" : prefix);
            }
            String body = r.getBodyAsString();
            int blobs = body.indexOf("<Blobs>");
            for (String[] name : FlatXml.tagged(blobs < 0 ? "" : body.substring(blobs),
                    "Name")) {
                if (keys.size() < max) {
                    keys.add(blobName(name[0], name[1]));
                }
            }
            List<String> next = FlatXml.elements(body, "NextMarker");
            if (next.isEmpty() || next.get(0).length() == 0) {
                break;
            }
            marker = next.get(0);
        }
        return keys;
    }

    @Override
    public String presignGet(String bucket, String key, int seconds) throws IOException {
        return presign("r", bucket, key, seconds, System.currentTimeMillis());
    }

    @Override
    public String presignPut(String bucket, String key, int seconds) throws IOException {
        return presign("cw", bucket, key, seconds, System.currentTimeMillis());
    }

    /// A blob SAS URL granting `permissions` until `now + seconds`: a service
    /// SAS with an account key, a user-delegation SAS with a managed identity.
    /// Package-private with an explicit clock so a test can hold it to a fixed
    /// vector.
    String presign(String permissions, String bucket, String key, int seconds, long now)
            throws IOException {
        if (seconds <= 0) {
            throw new IllegalArgumentException("A SAS lives at least one second, not "
                    + seconds);
        }
        String path = blobPath(bucket, key);
        String expiry = iso(now + seconds * 1000L);
        String protocol = base.regionMatches(true, 0, "https:", 0, 6) ? "https" : "https,http";
        String resource = "/blob/" + account + "/" + bucket + "/" + key;
        Map<String, String> query = new TreeMap<String, String>();
        query.put("sp", permissions);
        query.put("se", expiry);
        query.put("spr", protocol);
        query.put("sv", VERSION);
        query.put("sr", "b");
        String toSign;
        byte[] signingKey;
        if (accountKey != null) {
            toSign = permissions + "\n\n" + expiry + "\n" + resource + "\n\n\n" + protocol
                    + "\n" + VERSION + "\nb\n\n\n\n\n\n\n";
            signingKey = accountKey;
        } else {
            DelegationKey d = delegationKey(now + seconds * 1000L);
            query.put("skoid", d.objectId);
            query.put("sktid", d.tenantId);
            query.put("skt", d.start);
            query.put("ske", d.expiry);
            query.put("sks", d.service);
            query.put("skv", d.version);
            toSign = permissions + "\n\n" + expiry + "\n" + resource + "\n" + d.objectId + "\n"
                    + d.tenantId + "\n" + d.start + "\n" + d.expiry + "\n" + d.service + "\n"
                    + d.version + "\n\n\n\n\n" + protocol + "\n" + VERSION + "\nb\n\n\n\n\n\n\n";
            signingKey = d.value;
        }
        query.put("sig", Base64.encode(Crypto.hmacSha256(signingKey, utf8(toSign))));
        return base + Aws.encodePath(path) + "?" + Aws.canonicalQuery(query);
    }

    /// A user delegation key, as Get User Delegation Key returned it.
    private static final class DelegationKey {
        String objectId;
        String tenantId;
        String start;
        String expiry;
        String service;
        String version;
        byte[] value;
        long expiresAt;
    }

    /// The cached delegation key, fetched again when it would expire before
    /// `until`. A SAS cannot outlive the key that signed it.
    private synchronized DelegationKey delegationKey(long until) throws IOException {
        if (delegation != null && delegation.expiresAt >= until) {
            return delegation;
        }
        long now = System.currentTimeMillis();
        long end = Math.max(until, now + DELEGATION_LIFETIME);
        if (end - now > 7L * 24 * 3600 * 1000) {
            throw new IllegalArgumentException("A user-delegation SAS lives at most seven "
                    + "days");
        }
        String body = "<?xml version=\"1.0\" encoding=\"utf-8\"?><KeyInfo><Start>"
                + iso(now - 5 * 60 * 1000L) + "</Start><Expiry>" + iso(end)
                + "</Expiry></KeyInfo>";
        Map<String, String> query = new TreeMap<String, String>();
        query.put("restype", "service");
        query.put("comp", "userdelegationkey");
        List<String> headers = new ArrayList<String>(1);
        headers.add("Content-Type: application/xml");
        Web.Result r = send("POST", "/", query, headers, utf8(body));
        if (!r.isSuccess()) {
            throw new IOException("Azure Get User Delegation Key failed with "
                    + r.getStatus() + " " + errorCode(r) + ". A presigned URL from a "
                    + "managed identity needs a role that grants "
                    + "Microsoft.Storage/storageAccounts/blobServices/generateUserDelegationKey");
        }
        String xml = r.getBodyAsString();
        DelegationKey d = new DelegationKey();
        d.objectId = first(xml, "SignedOid");
        d.tenantId = first(xml, "SignedTid");
        d.start = first(xml, "SignedStart");
        d.expiry = first(xml, "SignedExpiry");
        d.service = first(xml, "SignedService");
        d.version = first(xml, "SignedVersion");
        d.value = Base64.decode(first(xml, "Value"));
        d.expiresAt = end;
        delegation = d;
        return d;
    }

    private static String first(String xml, String name) throws IOException {
        List<String> values = FlatXml.elements(xml, name);
        if (values.isEmpty()) {
            throw new IOException("Azure's user delegation key has no " + name);
        }
        return values.get(0);
    }

    private String blobPath(String bucket, String key) {
        requireContainer(bucket);
        if (key == null || key.length() == 0 || key.length() > 1024) {
            throw new IllegalArgumentException("Not an Azure blob name: " + key);
        }
        return "/" + bucket + "/" + key;
    }

    /// One authenticated request. `path` is the container-relative path, raw;
    /// `query` is raw too and encoded here.
    private Web.Result send(String method, String path, Map<String, String> query,
            List<String> extra, byte[] body) throws IOException {
        String urlPath = basePath + Aws.encodePath(path);
        String date = Http1Date.format(System.currentTimeMillis());
        List<String> headers = new ArrayList<String>();
        if (extra != null) {
            headers.addAll(extra);
        }
        headers.add("x-ms-date: " + date);
        headers.add("x-ms-version: " + VERSION);
        if (accountKey != null) {
            headers.add("Authorization: SharedKey " + account + ":"
                    + sharedKey(accountKey, account, method, urlPath, query, headers,
                    body == null ? 0 : body.length));
        } else {
            try {
                headers.add("Authorization: Bearer " + tokens.getTokenValue());
            } catch (TokenAcquisitionException err) {
                throw new IOException(err.getMessage(), err);
            }
        }
        StringBuilder url = new StringBuilder(base.substring(0, base.length() - basePath.length()))
                .append(urlPath);
        if (query != null && !query.isEmpty()) {
            url.append('?').append(Aws.canonicalQuery(query));
        }
        return Web.request(method, url.toString(), headers, body);
    }

    /// The Shared Key signature of one request: HMAC-SHA256, under the account
    /// key, of the verb, the standard headers, the `x-ms-` headers and the
    /// resource, each canonicalized as the service will. Package-private for
    /// the test vectors.
    static String sharedKey(byte[] key, String account, String method, String urlPath,
            Map<String, String> query, List<String> headers, int contentLength) {
        Map<String, String> standard = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> msHeaders = new TreeMap<String, String>();
        for (String line : headers) {
            int colon = line.indexOf(':');
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (name.regionMatches(true, 0, "x-ms-", 0, 5)) {
                msHeaders.put(asciiLower(name), value);
            } else {
                standard.put(name, value);
            }
        }
        StringBuilder sts = new StringBuilder(method).append('\n');
        String[] names = {"Content-Encoding", "Content-Language", null, "Content-MD5",
            "Content-Type", "Date", "If-Modified-Since", "If-Match", "If-None-Match",
            "If-Unmodified-Since", "Range"};
        for (String name : names) {
            if (name == null) {
                // Since version 2015-02-21 a zero length is signed as empty.
                sts.append(contentLength == 0 ? "" : String.valueOf(contentLength));
            } else {
                String v = standard.get(name);
                sts.append(v == null ? "" : v);
            }
            sts.append('\n');
        }
        for (Map.Entry<String, String> e : msHeaders.entrySet()) {
            sts.append(e.getKey()).append(':').append(e.getValue()).append('\n');
        }
        sts.append('/').append(account).append(urlPath);
        if (query != null) {
            Map<String, String> sorted = new TreeMap<String, String>();
            Iterator<Map.Entry<String, String>> it = query.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> e = it.next();
                sorted.put(asciiLower(e.getKey()), e.getValue());
            }
            for (Map.Entry<String, String> e : sorted.entrySet()) {
                sts.append('\n').append(e.getKey()).append(':').append(e.getValue());
            }
        }
        return Base64.encode(Crypto.hmacSha256(key, utf8(sts.toString())));
    }

    /// The managed identity's token for Azure Storage, from the App Service /
    /// Functions / Container Apps identity endpoint when the platform sets
    /// `IDENTITY_ENDPOINT`, otherwise from the VM instance metadata service.
    private static final class ManagedIdentityGrant implements AccessTokenGrant {
        private final String clientId;

        ManagedIdentityGrant(String clientId) {
            this.clientId = clientId;
        }

        @Override
        public AccessToken fetch() {
            String endpoint = System.getenv("IDENTITY_ENDPOINT");
            String secret = System.getenv("IDENTITY_HEADER");
            List<String> headers = new ArrayList<String>(1);
            String url;
            if (endpoint != null && secret != null) {
                url = endpoint + "?api-version=2019-08-01&resource=" + Aws.encode(RESOURCE);
                headers.add("X-IDENTITY-HEADER: " + secret);
            } else {
                url = "http://169.254.169.254/metadata/identity/oauth2/token"
                        + "?api-version=2018-02-01&resource=" + Aws.encode(RESOURCE);
                headers.add("Metadata: true");
            }
            if (clientId != null) {
                url += "&client_id=" + Aws.encode(clientId);
            }
            try {
                Web.Result r = Web.request("GET", url, headers, null);
                if (!r.isSuccess()) {
                    throw new TokenAcquisitionException("The managed identity endpoint "
                            + "refused a token (" + r.getStatus() + ")", r.getStatus(), null,
                            true, null);
                }
                Map reply = Json.parseObject(r.getBodyAsString());
                Object token = reply.get("access_token");
                if (!(token instanceof String)) {
                    throw new TokenAcquisitionException("The managed identity endpoint sent "
                            + "no token", r.getStatus(), null, true, null);
                }
                // Sent as a string by IMDS and as a number by some hosts.
                // IMDS sends expires_in; App Service and Functions
                // (IDENTITY_ENDPOINT, api-version 2019-08-01) send only
                // expires_on, epoch seconds. Without reading it every token
                // there was taken to live 300 s and refetched every 150.
                long seconds = number(reply.get("expires_in"), -1);
                if (seconds < 0) {
                    long on = number(reply.get("expires_on"), -1);
                    seconds = on > 0 ? on - System.currentTimeMillis() / 1000 : 300;
                    if (seconds <= 0) {
                        seconds = 300;
                    }
                }
                return new AccessToken((String) token, "Bearer",
                        System.currentTimeMillis() + seconds * 1000,
                        Collections.<String>emptySet());
            } catch (IOException err) {
                throw new TokenAcquisitionException("No Azure credentials: no connection "
                        + "string or account key is configured and the managed identity "
                        + "endpoint did not answer: " + err.getMessage(), 0, null, true, err);
            }
        }
    }

    /// A listed blob name. Azure percent-encodes a name holding characters XML
    /// 1.0 cannot carry and says so with `Encoded="true"` on the element; such a
    /// name is decoded here, '+' left as it is (this is not form encoding).
    static String blobName(String attributes, String value) throws IOException {
        if (attributes.indexOf("Encoded=\"true\"") < 0) {
            return value;
        }
        byte[] out = new byte[value.length()];
        int size = 0;
        for (int iter = 0; iter < value.length(); iter++) {
            char c = value.charAt(iter);
            int high = c == '%' && iter + 2 < value.length() ? hexDigit(value.charAt(iter + 1)) : -1;
            int low = high < 0 ? -1 : hexDigit(value.charAt(iter + 2));
            if (low >= 0) {
                out[size++] = (byte) ((high << 4) | low);
                iter += 2;
            } else if (c == '%' || c > 0x7f) {
                throw new IOException("Azure sent a malformed encoded blob name: " + value);
            } else {
                out[size++] = (byte) c;
            }
        }
        return new String(out, 0, size, "UTF-8");
    }

    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    private static long number(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException err) {
                return fallback;
            }
        }
        return fallback;
    }

    /// An account name becomes a host label: 3 to 24 lowercase letters and digits.
    private static void requireAccount(String account) {
        int length = account == null ? 0 : account.length();
        boolean ok = length >= 3 && length <= 24;
        for (int iter = 0 ; ok && iter < length ; iter++) {
            char c = account.charAt(iter);
            ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
        }
        if (!ok) {
            throw new IllegalArgumentException("Not an Azure storage account name: " + account);
        }
    }

    /// Azure's container rules: 3 to 63 lowercase letters, digits and single
    /// hyphens, starting and ending with a letter or digit.
    private static void requireContainer(String container) {
        int length = container == null ? 0 : container.length();
        boolean ok = length >= 3 && length <= 63;
        for (int iter = 0 ; ok && iter < length ; iter++) {
            char c = container.charAt(iter);
            boolean alnum = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            ok = alnum || c == '-' && iter > 0 && iter < length - 1
                    && container.charAt(iter - 1) != '-';
        }
        if (!ok) {
            throw new IllegalArgumentException("Not an Azure container name: " + container);
        }
    }

    private static IOException failure(Web.Result r, String operation, String bucket,
            String key) {
        List<String> message = FlatXml.elements(r.getBodyAsString(), "Message");
        return new IOException("Azure " + operation + " " + bucket + "/" + key + " failed with "
                + r.getStatus() + " " + errorCode(r)
                + (message.isEmpty() ? "" : ": " + message.get(0)));
    }

    private static String errorCode(Web.Result r) {
        String code = r.getHeader("x-ms-error-code");
        if (code != null) {
            return code;
        }
        List<String> codes = FlatXml.elements(r.getBodyAsString(), "Code");
        return codes.isEmpty() ? "" : codes.get(0);
    }

    private static String unquote(String etag) {
        return etag == null ? "" : etag.replace("\"", "");
    }

    /// `yyyy-MM-dd'T'HH:mm:ss'Z'` in UTC, by hand, for the same reason as the
    /// other signers: no locale may touch a signed value.
    static String iso(long millis) {
        long seconds = CivilTime.floorDiv(millis, 1000L);
        long days = CivilTime.floorDiv(seconds, 86400L);
        int secondOfDay = (int) (seconds - days * 86400L);
        StringBuilder out = new StringBuilder(20);
        CivilTime.appendDate(out, days, '-').append('T');
        CivilTime.pad(out, secondOfDay / 3600, 2).append(':');
        CivilTime.pad(out, (secondOfDay / 60) % 60, 2).append(':');
        CivilTime.pad(out, secondOfDay % 60, 2);
        return out.append('Z').toString();
    }

    private static List<String> split(String value, char separator) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        while (from <= value.length()) {
            int at = value.indexOf(separator, from);
            int end = at < 0 ? value.length() : at;
            out.add(value.substring(from, end));
            from = end + 1;
        }
        return out;
    }

    /// ASCII lower case; String.toLowerCase is locale sensitive. See CLAUDE.md.
    private static String asciiLower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing", err);
        }
    }
}
