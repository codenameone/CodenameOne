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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.Web;

/**
 * AWS Signature Version 4, and the request plumbing every AWS service shares.
 *
 * This is the whole of what an AWS client needs that is not service specific:
 * canonicalise a request, derive a signing key, and send it. {@link S3} is one
 * service built on it; SQS, DynamoDB and Secrets Manager are the same three steps
 * with a different host and payload, which is why the signer is a separate class
 * rather than something private to S3.
 *
 * Written rather than pulled in because the AWS SDK is not an option here: it
 * wants reflection, a class loader and a threading model a translated server
 * binary does not have. SigV4 itself is a hash chain -- five HMACs and a SHA-256
 * -- over a canonical form of the request, and the specification is public and
 * stable.
 *
 * The part that is easy to get wrong, and the reason for the length of this file,
 * is the CANONICAL form: the signature covers a normalised URI, a sorted query
 * string, sorted lower-cased headers and a hash of the body, and a single
 * difference from what the service computes produces a 403 with no indication of
 * which field disagreed.
 */
public final class Aws {
    /** The unsigned-payload marker, for a body the caller does not want hashed. */
    public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Aws() {
    }

    /**
     * A signed, sent request.
     *
     * `headers` are extra "Name: value" strings; Host, x-amz-date and
     * x-amz-content-sha256 are added here because they are part of the signature.
     */
    public static Web.Result send(Credentials credentials, String region, String service,
            String method, String host, String path, Map query, Map headers,
            byte[] body, String timestamp) throws IOException {
        return send(credentials, region, service, method, host, path, query, headers,
                body, timestamp, true);
    }

    /**
     * As above, over plain HTTP when `secure` is false.
     *
     * The signature covers the host and not the scheme, so this changes only how
     * the request travels. It exists for a local MinIO or a test double on
     * loopback; nothing reachable off the machine should use it, and AWS itself
     * does not accept it.
     */
    public static Web.Result send(Credentials credentials, String region, String service,
            String method, String host, String path, Map query, Map headers,
            byte[] body, String timestamp, boolean secure) throws IOException {
        Map signedHeaders = headers == null ? new LinkedHashMap() : new LinkedHashMap(headers);
        String stamp = timestamp == null ? Clock.timestamp() : timestamp;
        String payloadHash = body == null ? sha256Hex(new byte[0]) : sha256Hex(body);

        signedHeaders.put("host", host);
        signedHeaders.put("x-amz-date", stamp);
        signedHeaders.put("x-amz-content-sha256", payloadHash);
        if(credentials.getSessionToken() != null) {
            // A temporary credential's token is part of the signature, not an
            // afterthought: a request signed without it is rejected.
            signedHeaders.put("x-amz-security-token", credentials.getSessionToken());
        }

        String authorization = authorization(credentials, region, service, method, path,
                query, signedHeaders, payloadHash, stamp);
        signedHeaders.put("authorization", authorization);

        List headerLines = new ArrayList();
        Iterator it = signedHeaders.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            // SIGNED BUT NOT SENT. host belongs in the canonical request -- the
            // signature is computed over it and the service recomputes the same --
            // but on the wire it is the transport's, derived from the URL, which
            // is built from this very host one line below. Sending it as a header
            // as well is how a request would carry two, and HeaderLines refuses
            // the field now for the vhost override it hands an untrusted caller.
            if("host".equals(entry.getKey())) {
                continue;
            }
            headerLines.add(entry.getKey() + ": " + entry.getValue());
        }
        String url = (secure ? "https://" : "http://") + host + encodePath(path);
        String canonicalQuery = canonicalQuery(query);
        if(canonicalQuery.length() > 0) {
            url = url + "?" + canonicalQuery;
        }
        return Web.request(method, url, headerLines, body);
    }

    /** The Authorization header value for one request. */
    public static String authorization(Credentials credentials, String region, String service,
            String method, String path, Map query, Map headers, String payloadHash,
            String timestamp) throws IOException {
        String date = timestamp.substring(0, 8);
        String scope = date + "/" + region + "/" + service + "/aws4_request";

        // Header names are lower-cased and sorted; values have their runs of
        // whitespace collapsed. All three are part of the specification, and all
        // three are invisible in a failure -- the service just says 403.
        TreeMap canonicalHeaders = new TreeMap();
        Iterator it = headers.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            canonicalHeaders.put(asciiLower(String.valueOf(entry.getKey())),
                    collapse(String.valueOf(entry.getValue())));
        }
        StringBuilder headerBlock = new StringBuilder();
        StringBuilder signedNames = new StringBuilder();
        it = canonicalHeaders.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            headerBlock.append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
            if(signedNames.length() > 0) {
                signedNames.append(';');
            }
            signedNames.append(entry.getKey());
        }

        String canonicalRequest = method + "\n"
                + encodePath(path) + "\n"
                + canonicalQuery(query) + "\n"
                + headerBlock + "\n"
                + signedNames + "\n"
                + payloadHash;
        String stringToSign = ALGORITHM + "\n" + timestamp + "\n" + scope + "\n"
                + sha256Hex(utf8(canonicalRequest));
        byte[] signingKey = signingKey(credentials.getSecretKey(), date, region, service);
        String signature = hex(Crypto.hmacSha256(signingKey, utf8(stringToSign)));

        return ALGORITHM + " Credential=" + credentials.getAccessKeyId() + "/" + scope
                + ", SignedHeaders=" + signedNames + ", Signature=" + signature;
    }

    /**
     * A presigned URL: the signature travels in the query string, so anyone
     * holding the URL can make that one request until it expires.
     *
     * This is what hands a mobile client a direct download or upload without
     * proxying the bytes through the server, which is most of the reason to use
     * object storage from an app at all.
     */
    /** SigV4's ceiling: seven days. */
    private static final int MAX_PRESIGN_SECONDS = 7 * 24 * 60 * 60;

    public static String presign(Credentials credentials, String region, String service,
            String method, String host, String path, Map query, int expiresSeconds,
            String timestamp) throws IOException {
        return presign(credentials, region, service, method, host, path, query,
                expiresSeconds, timestamp, true);
    }

    /** As above, producing an http:// URL when `secure` is false. See {@link #send}. */
    public static String presign(Credentials credentials, String region, String service,
            String method, String host, String path, Map query, int expiresSeconds,
            String timestamp, boolean secure) throws IOException {
        // SigV4 accepts 1 second to 7 days, and anything else produces a URL that
        // LOOKS right and is refused when someone tries to use it. These URLs are
        // handed straight to a device, so the failure would surface far from the
        // call that caused it -- and a caller computing a lifetime from
        // configuration is exactly how a zero or a negative one gets here.
        if(expiresSeconds < 1 || expiresSeconds > MAX_PRESIGN_SECONDS) {
            throw new IOException("A presigned URL lasts between 1 second and 7 days; "
                    + expiresSeconds + " would be refused when it was used");
        }
        String stamp = timestamp == null ? Clock.timestamp() : timestamp;
        String date = stamp.substring(0, 8);
        String scope = date + "/" + region + "/" + service + "/aws4_request";

        Map signedQuery = query == null ? new LinkedHashMap() : new LinkedHashMap(query);
        signedQuery.put("X-Amz-Algorithm", ALGORITHM);
        signedQuery.put("X-Amz-Credential", credentials.getAccessKeyId() + "/" + scope);
        signedQuery.put("X-Amz-Date", stamp);
        signedQuery.put("X-Amz-Expires", String.valueOf(expiresSeconds));
        signedQuery.put("X-Amz-SignedHeaders", "host");
        if(credentials.getSessionToken() != null) {
            signedQuery.put("X-Amz-Security-Token", credentials.getSessionToken());
        }

        String canonicalRequest = method + "\n"
                + encodePath(path) + "\n"
                + canonicalQuery(signedQuery) + "\n"
                + "host:" + host + "\n\n"
                + "host\n"
                + UNSIGNED_PAYLOAD;
        String stringToSign = ALGORITHM + "\n" + stamp + "\n" + scope + "\n"
                + sha256Hex(utf8(canonicalRequest));
        byte[] signingKey = signingKey(credentials.getSecretKey(), date, region, service);
        String signature = hex(Crypto.hmacSha256(signingKey, utf8(stringToSign)));

        signedQuery.put("X-Amz-Signature", signature);
        return (secure ? "https://" : "http://") + host + encodePath(path) + "?"
                + canonicalQuery(signedQuery);
    }

    /**
     * The four-step key derivation. The signing key is scoped to a date, a region
     * and a service, which is what keeps a leaked signature from being reusable
     * anywhere else.
     *
     * Public, along with the four canonicalisation helpers below, because a
     * service this class does not wrap -- SQS, DynamoDB, Secrets Manager -- is the
     * same signature over a different payload, and because these are the pieces a
     * known-answer test can pin. A signature implementation that can only be
     * tested end to end is one whose failures all look like 403.
     */
    public static byte[] signingKey(String secretKey, String date, String region, String service)
            throws IOException {
        byte[] key = Crypto.hmacSha256(utf8("AWS4" + secretKey), utf8(date));
        key = Crypto.hmacSha256(key, utf8(region));
        key = Crypto.hmacSha256(key, utf8(service));
        return Crypto.hmacSha256(key, utf8("aws4_request"));
    }

    /**
     * Query parameters sorted by name, each name and value percent-encoded.
     * Sorting is by the ENCODED name, which matters for names that differ only in
     * a character the encoding changes.
     */
    public static String canonicalQuery(Map query) {
        if(query == null || query.isEmpty()) {
            return "";
        }
        List pairs = new ArrayList();
        Iterator it = query.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            Object value = entry.getValue();
            pairs.add(encode(String.valueOf(entry.getKey())) + "="
                    + encode(value == null ? "" : String.valueOf(value)));
        }
        Collections.sort(pairs);
        StringBuilder out = new StringBuilder();
        for(int iter = 0 ; iter < pairs.size() ; iter++) {
            if(iter > 0) {
                out.append('&');
            }
            out.append(pairs.get(iter));
        }
        return out.toString();
    }

    /**
     * The path, percent-encoded segment by segment. The slashes between segments
     * are NOT encoded; everything else that is not unreserved is -- which is why
     * this cannot just call {@link #encode} on the whole path.
     */
    public static String encodePath(String path) {
        if(path == null || path.length() == 0) {
            return "/";
        }
        StringBuilder out = new StringBuilder();
        int at = 0;
        while(at <= path.length()) {
            int end = path.indexOf('/', at);
            if(end < 0) {
                end = path.length();
            }
            out.append(encode(path.substring(at, end)));
            if(end == path.length()) {
                break;
            }
            out.append('/');
            at = end + 1;
        }
        return out.length() == 0 ? "/" : out.toString();
    }

    /**
     * RFC 3986 unreserved characters pass; everything else becomes %XX with UPPER
     * case hex. Note this is not URLEncoder: a space is %20 here, never '+', and
     * '~' is not encoded. Both differences produce a signature mismatch.
     */
    public static String encode(String value) {
        if(value == null) {
            return "";
        }
        byte[] raw = utf8(value);
        StringBuilder out = new StringBuilder(raw.length);
        for(int iter = 0 ; iter < raw.length ; iter++) {
            int c = raw[iter] & 0xff;
            if((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~') {
                out.append((char)c);
            } else {
                out.append('%')
                   .append(Character.toUpperCase(HEX[(c >> 4) & 0xf]))
                   .append(Character.toUpperCase(HEX[c & 0xf]));
            }
        }
        return out.toString();
    }

    /** Leading and trailing space removed, internal runs collapsed to one space. */
    /**
     * ASCII lower case, because String.toLowerCase() is LOCALE SENSITIVE and
     * this platform has no Locale to ask for the root one. On a device set to
     * Turkish the I of an ASCII token folds to a dotless i, so the result stops
     * equalling the constant it is compared against: nothing is thrown, nothing
     * is logged, and the feature is simply inert for those users. A header name
     * is ASCII by specification. Copied rather than shared; see CLAUDE.md.
     */
    private static String asciiLower(String value) {
        if(value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length());
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char)(c + 32) : c);
        }
        return out.toString();
    }

    public static String collapse(String value) {
        if(value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        boolean space = false;
        String trimmed = value.trim();
        for(int iter = 0 ; iter < trimmed.length() ; iter++) {
            char c = trimmed.charAt(iter);
            if(c == ' ' || c == '\t') {
                space = true;
                continue;
            }
            if(space && out.length() > 0) {
                out.append(' ');
            }
            space = false;
            out.append(c);
        }
        return out.toString();
    }

    public static String sha256Hex(byte[] data) {
        return hex(Crypto.sha256(data));
    }

    public static String hex(byte[] data) {
        StringBuilder out = new StringBuilder(data.length * 2);
        for(int iter = 0 ; iter < data.length ; iter++) {
            out.append(HEX[(data[iter] >> 4) & 0xf]).append(HEX[data[iter] & 0xf]);
        }
        return out.toString();
    }

    static byte[] utf8(String value) {
        if(value == null) {
            return new byte[0];
        }
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException("UTF-8 is missing");
        }
    }

    /** Base64 of a raw digest, for the services that want it that way. */
    static String base64(byte[] data) {
        return Base64.encode(data);
    }
}
