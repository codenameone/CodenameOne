/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.aws;

import com.codename1.backend.Config;
import com.codename1.backend.storage.ObjectStorage;
import com.codename1.backend.storage.ObjectStorages;
import com.codename1.impl.backend.FlatXml;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Web;

/// Amazon S3, and anything that speaks its API (MinIO, Cloudflare R2, Backblaze
/// B2, Wasabi, Ceph) -- which is why the endpoint is configurable rather than
/// assembled from a region alone.
///
/// Two addressing styles exist and the choice is not cosmetic: virtual-hosted
/// (`bucket.s3.region.amazonaws.com`) is what AWS requires for new buckets, and
/// path-style (`endpoint/bucket/key`) is what a local MinIO or a bucket whose name
/// is not DNS-safe needs. Both are supported because a backend is usually
/// developed against the second and deployed against the first.
///
/// The presigned URL is the method to reach for from a mobile app: it lets the
/// device upload or download directly and keeps the object bytes out of the
/// server, which is most of the reason to use object storage from an app.
public final class S3 implements ObjectStorage {
    /// How long before expiry to fetch again. Long enough that a request signed now
    /// is still valid when it arrives, short enough not to refresh constantly.
    private static final long CREDENTIAL_REFRESH_MARGIN = 5 * 60 * 1000L;

    /// VOLATILE, because the refresh below happens on whichever request thread
    /// noticed the expiry and every other worker reads it without a lock.
    ///
    /// Credentials is immutable with final fields, so a thread that sees the new
    /// reference sees the whole credential -- but without volatile nothing makes it
    /// see the new reference at all, and it would go on signing with the expired
    /// one until something else happened to publish it. Those requests come back
    /// rejected, which is the outcome the refresh exists to prevent.
    ///
    /// Volatile and not a synchronized credentials(): the refresh calls
    /// Credentials.resolve, which is an HTTP round trip to the metadata service, so
    /// a monitor around it would stop every other S3 operation on this client for
    /// its duration. The benign race the method already documents -- two threads
    /// resolving at once and one answer being dropped -- is deliberate and stays.
    private volatile Credentials credentials; //NOPMD AvoidUsingVolatile - shared across request threads, see above
    private final String region;
    private final String endpoint;
    private final boolean pathStyle;
    private final boolean secure;
    private final boolean fromEnvironment;

    private S3(Credentials credentials, String region, String endpoint, boolean pathStyle,
            boolean secure, boolean fromEnvironment) {
        this.credentials = credentials;
        this.region = region;
        this.endpoint = endpoint;
        this.pathStyle = pathStyle;
        this.secure = secure;
        this.fromEnvironment = fromEnvironment;
    }

    /// The credentials to sign with, resolved again when a temporary one is near expiry.
    ///
    /// The role credentials ECS, EKS, EC2 and Lambda hand out live for minutes to
    /// hours, so a server that captured one at startup would spend the rest of its
    /// life signing with a credential the service has already forgotten. Only the
    /// environment-resolved case can be refreshed: a credential passed to
    /// [#forEndpoint] is the caller's, and there is no provider to ask again.
    ///
    /// Two request threads can resolve at once here and one of the two answers is
    /// dropped. That is harmless -- both are valid credentials, and the cost of the
    /// duplicate call is one metadata round trip an hour.
    private Credentials credentials() throws IOException {
        if (fromEnvironment && credentials.isExpiring(CREDENTIAL_REFRESH_MARGIN)) {
            try {
                credentials = Credentials.resolve();
            } catch (IOException err) {
                // A FAILED REFRESH IS NOT AN EXPIRED CREDENTIAL, and the margin is
                // the whole reason: it exists so the refresh can be attempted early
                // and retried. Letting the failure out turned a metadata service
                // that blinked -- ECS, EKS and IMDS all being ordinary HTTP
                // endpoints that can time out -- into an outage of every S3
                // operation, up to five minutes before AWS itself would have
                // stopped honouring what is already in hand.
                //
                // isExpiring(0) is the real question: only when the credential has
                // ACTUALLY expired is there nothing left to sign with, and then the
                // failure is the honest answer.
                if (credentials.isExpiring(0)) {
                    throw err;
                }
            }
        }
        return credentials;
    }

    /// AWS S3 in one region, with credentials resolved the usual way.
    ///
    /// The region is taken from AWS_REGION when not given, because that is what
    /// Lambda and ECS set and hard-coding it is how a service ends up deployable in
    /// exactly one place.
    public static S3 forRegion(String region) throws IOException {
        String resolved = regionOrEnvironment(region);
        return new S3(Credentials.resolve(), resolved, endpointFor(resolved),
                false, true, true);
    }

    /// `region`, or the environment's when it is empty, checked to be a region
    /// this class can build an endpoint for.
    private static String regionOrEnvironment(String region) throws IOException {
        String resolved = region != null && region.length() > 0
                ? region : System.getenv("AWS_REGION");
        if (resolved == null || resolved.length() == 0) {
            resolved = System.getenv("AWS_DEFAULT_REGION");
        }
        if (resolved == null || resolved.length() == 0) {
            throw new IOException("No AWS region: pass one, or set AWS_REGION");
        }
        requireRegionName(resolved);
        requireSupportedPartition(resolved);
        return resolved;
    }

    /// Refuses a region whose partition this cannot build an endpoint for.
    ///
    /// The ISO partitions -- us-iso, us-isob, us-isof, eu-isoe -- have DNS
    /// suffixes of their own, and endpointFor's comment says they are deliberately
    /// not guessed at because a wrong suffix is a silent misdirection. It then
    /// guessed at them anyway, by falling through to the commercial suffix: every
    /// request and every presigned URL for such a region named a host in the wrong
    /// partition, which is exactly the failure that comment describes.
    ///
    /// Refused HERE rather than in endpointFor, because this is the path that
    /// infers a host from a region name. forEndpoint takes the host from its
    /// caller, so somebody who can reach one of those networks can still name it
    /// and is not blocked by a guess this method declines to make.
    static void requireSupportedPartition(String region) throws IOException {
        int firstDash = region.indexOf('-');
        if (firstDash < 0) {
            return;
        }
        int secondDash = region.indexOf('-', firstDash + 1);
        String area = secondDash < 0 ? region.substring(firstDash + 1)
                                     : region.substring(firstDash + 1, secondDash);
        if (area.length() >= 3 && area.charAt(0) == 'i' && area.charAt(1) == 's'
                && area.charAt(2) == 'o') {
            throw new IOException("The region " + region + " is in an ISO partition, "
                    + "whose S3 endpoints do not end in amazonaws.com. This runtime "
                    + "will not guess that suffix; reach it with forEndpoint and the "
                    + "host your network actually uses");
        }
    }

    /// The S3 endpoint for a region, in that region's AWS PARTITION.
    ///
    /// The commercial suffix is not universal. AWS China is a separate
    /// partition with its own DNS suffix, amazonaws.com.cn, so a deployment in
    /// cn-north-1 or cn-northwest-1 was being pointed at a commercial hostname
    /// that does not serve it -- every request and every presigned URL, which is
    /// the worse half: a presigned URL is handed to somebody else to fetch, so the
    /// wrong host outlives the process that made it.
    ///
    /// GovCloud needs nothing here: us-gov-east-1 and us-gov-west-1 are their
    /// own partition but keep the commercial suffix. The two ISO partitions have
    /// suffixes of their own and are air-gapped networks this runtime has no way
    /// to reach, so they are deliberately not guessed at -- a wrong suffix there
    /// would be a silent misdirection of the same kind this fixes.
    static String endpointFor(String region) {
        boolean china = region.length() >= 3 && region.charAt(0) == 'c'
                && region.charAt(1) == 'n' && region.charAt(2) == '-';
        return "s3." + region + (china ? ".amazonaws.com.cn" : ".amazonaws.com");
    }

    /// Refuses a region that is not one label of a hostname.
    ///
    /// THE REGION BECOMES PART OF A HOST. "s3." + region + ".amazonaws.com" with a
    /// region of "evil.example/ignored" is a URL whose authority is s3.evil.example
    /// -- the amazonaws.com suffix has landed in the PATH -- so objects would be
    /// uploaded to, and download responses accepted from, a host somebody else
    /// owns, over a TLS connection that verifies correctly for them. A dot is
    /// refused along with the separators because one label is all a region is:
    /// us-east-1, eu-west-2, cn-north-1.
    ///
    /// It matters even though the value comes from configuration rather than
    /// from a request. AWS_REGION is read from the environment, which is exactly
    /// the kind of thing a deployment pipeline templates from somewhere else, and
    /// a region is not a place a reader would look for a way to redirect traffic.
    private static void requireRegionName(String region) throws IOException {
        for (int iter = 0 ; iter < region.length() ; iter++) {
            char c = region.charAt(iter);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-';
            if (!allowed) {
                throw new IOException("Not an AWS region: " + region + ". A region "
                        + "is one hostname label -- letters, digits and hyphens, as "
                        + "in us-east-1 -- because it is interpolated into the S3 "
                        + "endpoint's host");
            }
        }
        if (region.charAt(0) == '-' || region.charAt(region.length() - 1) == '-') {
            throw new IOException("Not an AWS region: " + region
                    + ". A hostname label cannot begin or end with a hyphen");
        }
    }

    /// Whether the endpoint opens with this scheme, ignoring case. See forEndpoint.
    private static boolean hasScheme(String value, String scheme) {
        return value.length() >= scheme.length()
                && value.regionMatches(true, 0, scheme, 0, scheme.length());
    }

    /// An S3-compatible endpoint -- MinIO, R2, Ceph -- addressed path-style.
    ///
    /// `endpoint` is a host with an optional port and an optional scheme:
    /// "minio.internal", "localhost:9000", "http://localhost:9000". TLS is assumed
    /// unless the endpoint says http:// -- a default of "encrypted" is the one
    /// that fails safely.
    public static S3 forEndpoint(Credentials credentials, String region, String endpoint) {
        String host = endpoint == null ? "" : endpoint;
        boolean useTls = true;
        // IGNORING CASE, because a scheme is case-insensitive and what happens
        // otherwise is not a rejection. "HTTP://localhost:9000" kept its scheme
        // inside `host` and left TLS on, so the request went to
        // "https://HTTP://localhost:9000/..." -- which never reaches the endpoint
        // that was configured and may resolve the host "HTTP" instead.
        //
        // regionMatches rather than toLowerCase: String.toLowerCase is locale
        // sensitive and there is no Locale here to ask for the root one, so on a
        // Turkish device the I of HTTP folds to a dotless i and stops matching.
        if (hasScheme(host, "http://")) {
            useTls = false;
            host = host.substring("http://".length());
        } else if (hasScheme(host, "https://")) {
            host = host.substring("https://".length());
        }
        while (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return new S3(credentials, region == null ? "us-east-1" : region, host, true, useTls,
                false);
    }

    /// Opens `s3://` and `r2://` storage URLs for
    /// [com.codename1.backend.storage.ObjectStorages]; the generated wiring
    /// registers it under whichever of the two the configuration names.
    ///
    /// - `s3://region` (or `s3://`, which reads `AWS_REGION`) is AWS. With
    ///   `cn1.storage.s3.endpoint` set it is that S3-compatible endpoint instead,
    ///   addressed path-style.
    /// - `r2://<account id>` is Cloudflare R2, at
    ///   `<account id>.r2.cloudflarestorage.com`, or the jurisdiction
    ///   `cn1.storage.r2.jurisdiction` names (`eu`, `fedramp`).
    ///
    /// Keys come from `cn1.storage.<s3|r2>.access-key-id` and
    /// `.secret-access-key`. Without them AWS resolves its usual chain
    /// (environment, container, instance role) and R2 reads the
    /// `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` environment, which is
    /// where R2's own documentation puts its S3 keys.
    public static final class Factory implements ObjectStorages.Factory {
        @Override
        public ObjectStorage open(String url, Config config) throws IOException {
            String authority = ObjectStorages.authority(url);
            boolean r2 = url.regionMatches(true, 0, "r2:", 0, 3);
            String prefix = r2 ? "cn1.storage.r2." : "cn1.storage.s3.";
            String keyId = ObjectStorages.setting(config, prefix + "access-key-id", null);
            String secret = ObjectStorages.setting(config, prefix + "secret-access-key", null);
            Credentials explicit = keyId != null && secret != null
                    ? new Credentials(keyId, secret, null) : null;
            if (r2) {
                requireAccountId(authority);
                String jurisdiction = ObjectStorages.setting(config,
                        "cn1.storage.r2.jurisdiction", null);
                if (jurisdiction != null) {
                    requireAccountId(jurisdiction);
                }
                return forEndpoint(explicit != null ? explicit : Credentials.fromEnvironment(),
                        "auto", "https://" + authority
                        + (jurisdiction == null ? "" : "." + jurisdiction)
                        + ".r2.cloudflarestorage.com");
            }
            String endpoint = ObjectStorages.setting(config, "cn1.storage.s3.endpoint", null);
            if (endpoint != null) {
                return forEndpoint(explicit != null ? explicit : Credentials.resolve(),
                        authority.length() == 0 ? null : authority, endpoint);
            }
            if (explicit == null) {
                return forRegion(authority);
            }
            String region = regionOrEnvironment(authority);
            return new S3(explicit, region, endpointFor(region), false, true, false);
        }

        /// An R2 account id or jurisdiction becomes a hostname label, so it is
        /// held to one: letters and digits only. Anything else could move the
        /// request -- and the signed key id -- to a different host.
        private static void requireAccountId(String value) throws IOException {
            boolean ok = value.length() > 0 && value.length() <= 63;
            for (int iter = 0 ; ok && iter < value.length() ; iter++) {
                char c = value.charAt(iter);
                ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
            }
            if (!ok) {
                throw new IOException("Not an R2 account id or jurisdiction: \"" + value
                        + "\". It becomes part of the host name, so it is letters and "
                        + "digits only");
            }
        }
    }

    /// `s3` for AWS and for every S3-compatible endpoint, R2 included.
    @Override
    public String getName() {
        return "s3";
    }

    /// Creates a bucket, and says nothing when it already exists.
    ///
    /// Usually infrastructure's job rather than the application's, but a first run
    /// against a fresh MinIO or a test fixture needs it, and the alternative is a
    /// shell script that speaks a protocol this class already speaks.
    @Override
    public void createBucket(String bucket) throws IOException {
        Web.Result result = send("PUT", bucket, "", null, null, createBucketBody());
        if (result.isSuccess()) {
            return;
        }
        List code = elements(result.getBodyAsString(), "Code");
        String reason = code.isEmpty() ? "" : String.valueOf(code.get(0));
        // Only "owned by you" is the idempotent case. Bucket names are global on AWS,
        // so "already exists" means somebody else has it: returning normally there
        // would report success for a bucket the caller does not have and cannot use,
        // and every later call would fail on authorization instead of here.
        if ("BucketAlreadyOwnedByYou".equals(reason)) {
            return;
        }
        requireSuccess(result, "CREATE BUCKET", bucket, "");
    }

    /// Uploads an object. Returns its ETag, which is the server's receipt.
    @Override
    public String putObject(String bucket, String key, byte[] content, String contentType)
            throws IOException {
        Map headers = new LinkedHashMap();
        headers.put("content-type", contentType == null
                ? "application/octet-stream" : contentType);
        Web.Result result = send("PUT", bucket, key, null, headers,
                content == null ? new byte[0] : content);
        requireSuccess(result, "PUT", bucket, key);
        String etag = result.getHeader("etag");
        return etag == null ? "" : etag.replace("\"", "");
    }

    /// Downloads an object. Throws when it is missing, rather than returning null.
    @Override
    public byte[] getObject(String bucket, String key) throws IOException {
        Web.Result result = send("GET", bucket, key, null, null, null);
        requireSuccess(result, "GET", bucket, key);
        return result.getBody();
    }

    /// The object's metadata, or null when it does not exist.
    @Override
    public ObjectInfo headObject(String bucket, String key) throws IOException {
        Web.Result result = send("HEAD", bucket, key, null, null, null);
        if (result.getStatus() == 404) {
            return null;
        }
        requireSuccess(result, "HEAD", bucket, key);
        String etag = result.getHeader("etag");
        return new ObjectInfo(key, parseLong(result.getHeader("content-length")),
                result.getHeader("content-type"),
                etag == null ? null : etag.replace("\"", ""),
                result.getHeader("last-modified"));
    }

    @Override
    public void deleteObject(String bucket, String key) throws IOException {
        Web.Result result = send("DELETE", bucket, key, null, null, null);
        // S3 answers 204 for a delete, and also for a key that was not there.
        if (result.getStatus() != 204 && result.getStatus() != 200) {
            requireSuccess(result, "DELETE", bucket, key);
        }
    }

    /// Lists up to `max` objects under a prefix.
    ///
    /// ListObjectsV2, and paginated: S3 caps a page at 1000 keys whatever you ask
    /// for, and a caller that ignores the continuation token silently sees only the
    /// first page. This follows the token until the listing is complete or `max` is
    /// reached.
    @Override
    public List<String> listObjects(String bucket, String prefix, int max)
            throws IOException {
        // UP TO MAX MEANS UP TO MAX. The loop below stops at the limit only while
        // it is positive, so zero and below meant "follow every continuation token
        // to the end of the bucket" -- the opposite of what was asked, and on a
        // large bucket a request for nothing became many round trips and a list
        // of everything in the heap. There is no documented unlimited mode to
        // preserve: the contract above is "lists up to max objects".
        if (max == 0) {
            return new ArrayList<String>();
        }
        if (max < 0) {
            throw new IllegalArgumentException("listObjects max must not be negative: "
                    + max);
        }
        List<String> keys = new ArrayList<String>();
        String token = null;
        while (true) {
            Map query = new LinkedHashMap();
            query.put("list-type", "2");
            if (prefix != null && prefix.length() > 0) {
                query.put("prefix", prefix);
            }
            if (token != null) {
                query.put("continuation-token", token);
            }
            Web.Result result = send("GET", bucket, "", query, null, null);
            requireSuccess(result, "LIST", bucket, prefix == null ? "" : prefix);
            String body = result.getBodyAsString();
            List page = elements(body, "Key");
            for (Object key : page) {
                keys.add((String) key);
                // max is positive here: zero and negative were answered above.
                if (keys.size() >= max) {
                    return keys;
                }
            }
            List next = elements(body, "NextContinuationToken");
            if (next.isEmpty()) {
                return keys;
            }
            token = (String) next.get(0);
        }
    }

    /// A URL that downloads the object without any credentials, for `seconds`.
    ///
    /// Nothing is sent here: a presigned URL is a computation, so this costs no
    /// round trip and can be handed straight to a client.
    @Override
    public String presignGet(String bucket, String key, int seconds) throws IOException {
        return Aws.presign(credentials(), region, "s3", "GET", hostFor(bucket),
                pathFor(bucket, key), null, seconds, null, secure);
    }

    /// The upload counterpart: a URL a client can PUT to, for `seconds`.
    @Override
    public String presignPut(String bucket, String key, int seconds) throws IOException {
        return Aws.presign(credentials(), region, "s3", "PUT", hostFor(bucket),
                pathFor(bucket, key), null, seconds, null, secure);
    }

    /// What [#headObject] reports; the provider-neutral
    /// [com.codename1.backend.storage.ObjectInfo] under the name this class has
    /// always returned.
    public static final class ObjectInfo extends com.codename1.backend.storage.ObjectInfo {
        ObjectInfo(String key, long size, String contentType, String etag,
                   String lastModified) {
            super(key, size, contentType, etag, lastModified);
        }
    }

    /// CreateBucket's body names the region, except in us-east-1 where it must not.
    ///
    /// S3 reads an empty CreateBucket as a request for us-east-1, so a bucket asked
    /// for anywhere else comes back as IllegalLocationConstraintException unless the
    /// body says where -- and us-east-1 rejects the body that says so. The
    /// S3-compatible endpoints take the empty body, which is what pathStyle
    /// distinguishes: forEndpoint addresses those path-style, forRegion does not.
    private byte[] createBucketBody() throws IOException {
        if (pathStyle || "us-east-1".equals(region)) {
            return new byte[0];
        }
        return ("<CreateBucketConfiguration "
                + "xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                + "<LocationConstraint>" + region + "</LocationConstraint>"
                + "</CreateBucketConfiguration>").getBytes("UTF-8");
    }

    private Web.Result send(String method, String bucket, String key, Map query,
            Map headers, byte[] body) throws IOException {
        return Aws.send(credentials(), region, "s3", method, hostFor(bucket),
                pathFor(bucket, key), query, headers, body, null, secure);
    }

    /// Refuses a bucket name that could change the host this request goes to.
    ///
    /// Virtual-hosted addressing puts the name in front of the endpoint and the
    /// result is concatenated straight after "https://", so a name carrying a
    /// slash -- "attacker.example/ignored" -- makes the authority the attacker's
    /// host and the rest a path. The request then carries the signed access key
    /// identifier and session token there. A caller that derives the name from
    /// tenant or request input is the case this exists for; one that hard-codes
    /// it loses nothing, because a name that fails this could not have resolved
    /// as a hostname anyway.
    ///
    /// These are S3's own rules for a DNS-compatible name: 3 to 63 characters of
    /// lowercase letter, digit, dot or hyphen, beginning and ending with a letter
    /// or digit, and no two dots in a row.
    private static void requireDnsBucket(String bucket) {
        int length = bucket == null ? 0 : bucket.length();
        boolean ok = length >= 3 && length <= 63;
        for (int iter = 0 ; ok && iter < length ; iter++) {
            char c = bucket.charAt(iter);
            boolean alnum = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (!alnum && c != '.' && c != '-') {
                ok = false;
            } else if ((iter == 0 || iter == length - 1) && !alnum) {
                ok = false;
            } else if (c == '.' && iter > 0 && bucket.charAt(iter - 1) == '.') {
                ok = false;
            }
        }
        if (!ok) {
            throw new IllegalArgumentException("Not a DNS-compatible S3 bucket "
                    + "name, so it cannot be addressed virtual-hosted: " + bucket);
        }
    }

    /// Refuses a bucket name that could change the PATH this request addresses.
    ///
    /// Deliberately narrower than the DNS rules above. Path style is what a bucket
    /// that cannot satisfy those rules uses -- the legacy us-east-1 names with
    /// uppercase and underscores are exactly that -- so applying them here would
    /// refuse the buckets this addressing mode exists to serve. What matters when
    /// the name goes into the path is only that it stays one segment.
    private static void requirePathSafeBucket(String bucket) {
        if (bucket == null || bucket.length() == 0 || bucket.indexOf('/') >= 0
                || bucket.indexOf('\\') >= 0 || bucket.indexOf("..") >= 0) {
            throw new IllegalArgumentException("An S3 bucket name cannot contain a "
                    + "path separator or \"..\": " + bucket);
        }
    }

    /// Whether THIS bucket has to be addressed path style.
    ///
    /// A dotted name cannot go in front of the endpoint over TLS. The wildcard
    /// certificate for `*.s3.<region>.amazonaws.com` matches exactly one label, so
    /// "photos.example" would need it to match two and every request -- and every
    /// presigned URL handed to a client -- fails hostname verification. The name
    /// is perfectly legal; it is the addressing that cannot carry it, so the
    /// request takes the path form instead of failing.
    private boolean usesPathStyle(String bucket) {
        return pathStyle || (secure && bucket != null && bucket.indexOf('.') >= 0);
    }

    private String hostFor(String bucket) {
        if (!usesPathStyle(bucket)) {
            requireDnsBucket(bucket);
            return bucket + "." + endpoint;
        }
        return endpoint;
    }

    private String pathFor(String bucket, String key) {
        String suffix = key == null ? "" : key;
        if (!usesPathStyle(bucket)) {
            return "/" + suffix;
        }
        // Checked here rather than only in send(): presign() calls hostFor and
        // pathFor directly, so a check on the send path alone would leave the two
        // presigning entry points unguarded.
        requirePathSafeBucket(bucket);
        return "/" + bucket + "/" + suffix;
    }

    /// S3 reports failures as an XML body with a Code and a Message, and the status
    /// alone ("403") does not say whether the key, the bucket, the signature or the
    /// clock is at fault. Both go into the exception.
    private static void requireSuccess(Web.Result result, String operation, String bucket,
            String key) throws IOException {
        if (result.isSuccess()) {
            return;
        }
        String body = result.getBodyAsString();
        List code = elements(body, "Code");
        List message = elements(body, "Message");
        throw new IOException("S3 " + operation + " s3://" + bucket + "/" + key
                + " failed with " + result.getStatus()
                + (code.isEmpty() ? "" : " " + code.get(0))
                + (message.isEmpty() ? "" : ": " + message.get(0)));
    }

    /// The text of every `<name>` element, in order; see
    /// [com.codename1.impl.backend.FlatXml#elements].
    static List elements(String xml, String name) {
        return FlatXml.elements(xml, name);
    }

    private static long parseLong(String value) {
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException err) {
            return -1;
        }
    }
}
