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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Web;

/**
 * Amazon S3, and anything that speaks its API (MinIO, Cloudflare R2, Backblaze
 * B2, Wasabi, Ceph) -- which is why the endpoint is configurable rather than
 * assembled from a region alone.
 *
 * Two addressing styles exist and the choice is not cosmetic: virtual-hosted
 * (`bucket.s3.region.amazonaws.com`) is what AWS requires for new buckets, and
 * path-style (`endpoint/bucket/key`) is what a local MinIO or a bucket whose name
 * is not DNS-safe needs. Both are supported because a backend is usually
 * developed against the second and deployed against the first.
 *
 * The presigned URL is the method to reach for from a mobile app: it lets the
 * device upload or download directly and keeps the object bytes out of the
 * server, which is most of the reason to use object storage from an app.
 */
public final class S3 {
    /**
     * How long before expiry to fetch again. Long enough that a request signed now
     * is still valid when it arrives, short enough not to refresh constantly.
     */
    private static final long CREDENTIAL_REFRESH_MARGIN = 5 * 60 * 1000L;

    private Credentials credentials;
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

    /**
     * The credentials to sign with, resolved again when a temporary one is near expiry.
     *
     * The role credentials ECS, EKS, EC2 and Lambda hand out live for minutes to
     * hours, so a server that captured one at startup would spend the rest of its
     * life signing with a credential the service has already forgotten. Only the
     * environment-resolved case can be refreshed: a credential passed to
     * {@link #forEndpoint} is the caller's, and there is no provider to ask again.
     *
     * Two request threads can resolve at once here and one of the two answers is
     * dropped. That is harmless -- both are valid credentials, and the cost of the
     * duplicate call is one metadata round trip an hour.
     */
    private Credentials credentials() throws IOException {
        if(fromEnvironment && credentials.isExpiring(CREDENTIAL_REFRESH_MARGIN)) {
            credentials = Credentials.resolve();
        }
        return credentials;
    }

    /**
     * AWS S3 in one region, with credentials resolved the usual way.
     *
     * The region is taken from AWS_REGION when not given, because that is what
     * Lambda and ECS set and hard-coding it is how a service ends up deployable in
     * exactly one place.
     */
    public static S3 forRegion(String region) throws IOException {
        String resolved = region != null && region.length() > 0
                ? region : System.getenv("AWS_REGION");
        if(resolved == null || resolved.length() == 0) {
            resolved = System.getenv("AWS_DEFAULT_REGION");
        }
        if(resolved == null || resolved.length() == 0) {
            throw new IOException("No AWS region: pass one, or set AWS_REGION");
        }
        return new S3(Credentials.resolve(), resolved,
                "s3." + resolved + ".amazonaws.com", false, true, true);
    }

    /**
     * An S3-compatible endpoint -- MinIO, R2, Ceph -- addressed path-style.
     *
     * `endpoint` is a host with an optional port and an optional scheme:
     * "minio.internal", "localhost:9000", "http://localhost:9000". TLS is assumed
     * unless the endpoint says http:// -- a default of "encrypted" is the one
     * that fails safely.
     */
    public static S3 forEndpoint(Credentials credentials, String region, String endpoint) {
        String host = endpoint == null ? "" : endpoint;
        boolean useTls = true;
        if(host.startsWith("http://")) {
            useTls = false;
            host = host.substring("http://".length());
        } else if(host.startsWith("https://")) {
            host = host.substring("https://".length());
        }
        while(host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        return new S3(credentials, region == null ? "us-east-1" : region, host, true, useTls,
                false);
    }

    /**
     * Creates a bucket, and says nothing when it already exists.
     *
     * Usually infrastructure's job rather than the application's, but a first run
     * against a fresh MinIO or a test fixture needs it, and the alternative is a
     * shell script that speaks a protocol this class already speaks.
     */
    public void createBucket(String bucket) throws IOException {
        Web.Result result = send("PUT", bucket, "", null, null, createBucketBody());
        if(result.isSuccess()) {
            return;
        }
        List code = elements(result.getBodyAsString(), "Code");
        String reason = code.isEmpty() ? "" : String.valueOf(code.get(0));
        // Only "owned by you" is the idempotent case. Bucket names are global on AWS,
        // so "already exists" means somebody else has it: returning normally there
        // would report success for a bucket the caller does not have and cannot use,
        // and every later call would fail on authorization instead of here.
        if("BucketAlreadyOwnedByYou".equals(reason)) {
            return;
        }
        requireSuccess(result, "CREATE BUCKET", bucket, "");
    }

    /** Uploads an object. Returns its ETag, which is the server's receipt. */
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

    /** Downloads an object. Throws when it is missing, rather than returning null. */
    public byte[] getObject(String bucket, String key) throws IOException {
        Web.Result result = send("GET", bucket, key, null, null, null);
        requireSuccess(result, "GET", bucket, key);
        return result.getBody();
    }

    /** The object's metadata, or null when it does not exist. */
    public ObjectInfo headObject(String bucket, String key) throws IOException {
        Web.Result result = send("HEAD", bucket, key, null, null, null);
        if(result.getStatus() == 404) {
            return null;
        }
        requireSuccess(result, "HEAD", bucket, key);
        ObjectInfo info = new ObjectInfo();
        info.key = key;
        info.size = parseLong(result.getHeader("content-length"));
        info.contentType = result.getHeader("content-type");
        String etag = result.getHeader("etag");
        info.etag = etag == null ? null : etag.replace("\"", "");
        info.lastModified = result.getHeader("last-modified");
        return info;
    }

    public void deleteObject(String bucket, String key) throws IOException {
        Web.Result result = send("DELETE", bucket, key, null, null, null);
        // S3 answers 204 for a delete, and also for a key that was not there.
        if(result.getStatus() != 204 && result.getStatus() != 200) {
            requireSuccess(result, "DELETE", bucket, key);
        }
    }

    /**
     * Lists up to `max` objects under a prefix.
     *
     * ListObjectsV2, and paginated: S3 caps a page at 1000 keys whatever you ask
     * for, and a caller that ignores the continuation token silently sees only the
     * first page. This follows the token until the listing is complete or `max` is
     * reached.
     */
    public List listObjects(String bucket, String prefix, int max) throws IOException {
        List keys = new ArrayList();
        String token = null;
        while(true) {
            Map query = new LinkedHashMap();
            query.put("list-type", "2");
            if(prefix != null && prefix.length() > 0) {
                query.put("prefix", prefix);
            }
            if(token != null) {
                query.put("continuation-token", token);
            }
            Web.Result result = send("GET", bucket, "", query, null, null);
            requireSuccess(result, "LIST", bucket, prefix == null ? "" : prefix);
            String body = result.getBodyAsString();
            List page = elements(body, "Key");
            for(int iter = 0 ; iter < page.size() ; iter++) {
                keys.add(page.get(iter));
                if(max > 0 && keys.size() >= max) {
                    return keys;
                }
            }
            List next = elements(body, "NextContinuationToken");
            if(next.isEmpty()) {
                return keys;
            }
            token = (String)next.get(0);
        }
    }

    /**
     * A URL that downloads the object without any credentials, for `seconds`.
     *
     * Nothing is sent here: a presigned URL is a computation, so this costs no
     * round trip and can be handed straight to a client.
     */
    public String presignGet(String bucket, String key, int seconds) throws IOException {
        return Aws.presign(credentials(), region, "s3", "GET", hostFor(bucket),
                pathFor(bucket, key), null, seconds, null, secure);
    }

    /** The upload counterpart: a URL a client can PUT to, for `seconds`. */
    public String presignPut(String bucket, String key, int seconds) throws IOException {
        return Aws.presign(credentials(), region, "s3", "PUT", hostFor(bucket),
                pathFor(bucket, key), null, seconds, null, secure);
    }

    /** What {@link #headObject} reports. */
    public static final class ObjectInfo {
        String key;
        long size;
        String contentType;
        String etag;
        String lastModified;

        public String getKey() {
            return key;
        }

        public long getSize() {
            return size;
        }

        public String getContentType() {
            return contentType;
        }

        public String getEtag() {
            return etag;
        }

        /** The raw HTTP date the service sent, not a parsed one. */
        public String getLastModified() {
            return lastModified;
        }
    }

    /**
     * CreateBucket's body names the region, except in us-east-1 where it must not.
     *
     * S3 reads an empty CreateBucket as a request for us-east-1, so a bucket asked
     * for anywhere else comes back as IllegalLocationConstraintException unless the
     * body says where -- and us-east-1 rejects the body that says so. The
     * S3-compatible endpoints take the empty body, which is what pathStyle
     * distinguishes: forEndpoint addresses those path-style, forRegion does not.
     */
    private byte[] createBucketBody() throws IOException {
        if(pathStyle || "us-east-1".equals(region)) {
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

    /**
     * Refuses a bucket name that could change the host this request goes to.
     *
     * Virtual-hosted addressing puts the name in front of the endpoint and the
     * result is concatenated straight after "https://", so a name carrying a
     * slash -- "attacker.example/ignored" -- makes the authority the attacker's
     * host and the rest a path. The request then carries the signed access key
     * identifier and session token there. A caller that derives the name from
     * tenant or request input is the case this exists for; one that hard-codes
     * it loses nothing, because a name that fails this could not have resolved
     * as a hostname anyway.
     *
     * These are S3's own rules for a DNS-compatible name: 3 to 63 characters of
     * lowercase letter, digit, dot or hyphen, beginning and ending with a letter
     * or digit, and no two dots in a row.
     */
    private static void requireDnsBucket(String bucket) {
        int length = bucket == null ? 0 : bucket.length();
        boolean ok = length >= 3 && length <= 63;
        for(int iter = 0 ; ok && iter < length ; iter++) {
            char c = bucket.charAt(iter);
            boolean alnum = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if(!alnum && c != '.' && c != '-') {
                ok = false;
            } else if((iter == 0 || iter == length - 1) && !alnum) {
                ok = false;
            } else if(c == '.' && iter > 0 && bucket.charAt(iter - 1) == '.') {
                ok = false;
            }
        }
        if(!ok) {
            throw new IllegalArgumentException("Not a DNS-compatible S3 bucket "
                    + "name, so it cannot be addressed virtual-hosted: " + bucket);
        }
    }

    /**
     * Refuses a bucket name that could change the PATH this request addresses.
     *
     * Deliberately narrower than the DNS rules above. Path style is what a bucket
     * that cannot satisfy those rules uses -- the legacy us-east-1 names with
     * uppercase and underscores are exactly that -- so applying them here would
     * refuse the buckets this addressing mode exists to serve. What matters when
     * the name goes into the path is only that it stays one segment.
     */
    private static void requirePathSafeBucket(String bucket) {
        if(bucket == null || bucket.length() == 0 || bucket.indexOf('/') >= 0
                || bucket.indexOf('\\') >= 0 || bucket.indexOf("..") >= 0) {
            throw new IllegalArgumentException("An S3 bucket name cannot contain a "
                    + "path separator or \"..\": " + bucket);
        }
    }

    private String hostFor(String bucket) {
        if(!pathStyle) {
            requireDnsBucket(bucket);
        }
        return pathStyle ? endpoint : bucket + "." + endpoint;
    }

    private String pathFor(String bucket, String key) {
        if(pathStyle) {
            // Checked here rather than only in send(): presign() calls hostFor and
            // pathFor directly, so a check on the send path alone would leave the
            // two presigning entry points unguarded.
            requirePathSafeBucket(bucket);
        }
        String suffix = key == null ? "" : key;
        return pathStyle ? "/" + bucket + "/" + suffix : "/" + suffix;
    }

    /**
     * S3 reports failures as an XML body with a Code and a Message, and the status
     * alone ("403") does not say whether the key, the bucket, the signature or the
     * clock is at fault. Both go into the exception.
     */
    private static void requireSuccess(Web.Result result, String operation, String bucket,
            String key) throws IOException {
        if(result.isSuccess()) {
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

    /**
     * The text of every &lt;name&gt; element, in order.
     *
     * A deliberate non-parser: S3's list and error responses are flat, the element
     * names wanted are known, and a real XML parser is a dependency this runtime
     * does not have. It decodes the five predefined entities, which is what S3
     * escapes in a key.
     */
    static List elements(String xml, String name) {
        List out = new ArrayList();
        if(xml == null) {
            return out;
        }
        String open = "<" + name + ">";
        String close = "</" + name + ">";
        int at = 0;
        while(true) {
            int start = xml.indexOf(open, at);
            if(start < 0) {
                return out;
            }
            int end = xml.indexOf(close, start + open.length());
            if(end < 0) {
                return out;
            }
            out.add(unescape(xml.substring(start + open.length(), end)));
            at = end + close.length();
        }
    }

    static String unescape(String value) {
        if(value.indexOf('&') < 0) {
            return value;
        }
        StringBuilder out = new StringBuilder(value.length());
        int at = 0;
        while(at < value.length()) {
            char c = value.charAt(at);
            if(c != '&') {
                out.append(c);
                at++;
                continue;
            }
            int semi = value.indexOf(';', at);
            if(semi < 0) {
                out.append(c);
                at++;
                continue;
            }
            String entity = value.substring(at + 1, semi);
            if("amp".equals(entity)) {
                out.append('&');
            } else if("lt".equals(entity)) {
                out.append('<');
            } else if("gt".equals(entity)) {
                out.append('>');
            } else if("quot".equals(entity)) {
                out.append('"');
            } else if("apos".equals(entity)) {
                out.append('\'');
            } else if(entity.length() > 1 && entity.charAt(0) == '#') {
                try {
                    int code = entity.charAt(1) == 'x' || entity.charAt(1) == 'X'
                            ? Integer.parseInt(entity.substring(2), 16)
                            : Integer.parseInt(entity.substring(1));
                    out.append((char)code);
                } catch (NumberFormatException err) {
                    out.append('&').append(entity).append(';');
                }
            } else {
                out.append('&').append(entity).append(';');
            }
            at = semi + 1;
        }
        return out.toString();
    }

    private static long parseLong(String value) {
        if(value == null) {
            return -1;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException err) {
            return -1;
        }
    }
}
