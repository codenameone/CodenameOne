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
import java.util.Map;

import com.codename1.backend.Json;
import com.codename1.backend.Web;

/**
 * AWS credentials, and the ways a server actually obtains them.
 *
 * Deliberately in this order, which is the order the AWS SDKs use and the order
 * that matters operationally:
 *
 * 1. The environment. This is what Lambda sets, what a local developer exports,
 *    and what a CI job injects.
 * 2. The container credential endpoint. ECS and EKS publish a relative URI on
 *    169.254.170.2 (or a full URI for EKS Pod Identity) that returns a temporary
 *    credential and refreshes it. This is how a task gets a ROLE rather than a
 *    long-lived key, which is the arrangement any reviewer will ask for.
 * 3. The instance metadata service, IMDSv2 only. v1 is a plain GET that any
 *    process -- or any server-side request forgery -- can make; v2 requires a PUT
 *    to obtain a token first. Falling back to v1 would undo that, so this does
 *    not.
 *
 * Temporary credentials expire. {@link #isExpiring} says when to fetch again;
 * {@link Session} does it.
 */
public final class Credentials {
    private final String accessKeyId;
    private final String secretKey;
    private final String sessionToken;
    private final long expiresAtMillis;

    public Credentials(String accessKeyId, String secretKey, String sessionToken) {
        this(accessKeyId, secretKey, sessionToken, 0);
    }

    public Credentials(String accessKeyId, String secretKey, String sessionToken,
            long expiresAtMillis) {
        this.accessKeyId = accessKeyId;
        this.secretKey = secretKey;
        this.sessionToken = sessionToken;
        this.expiresAtMillis = expiresAtMillis;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    /** Null for a long-lived key pair; set for anything temporary. */
    public String getSessionToken() {
        return sessionToken;
    }

    /** 0 when these do not expire. */
    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    /**
     * True within `marginMillis` of expiry. A margin rather than the exact instant
     * because a request signed just before expiry can still arrive just after it.
     */
    public boolean isExpiring(long marginMillis) {
        return expiresAtMillis > 0
                && System.currentTimeMillis() + marginMillis >= expiresAtMillis;
    }

    /**
     * The first source that answers, in the order documented on this class.
     * Throws when none does, naming what was tried -- "no credentials" with no
     * further detail is the least useful message a deployment can get.
     */
    public static Credentials resolve() throws IOException {
        Credentials fromEnvironment = fromEnvironment();
        if(fromEnvironment != null) {
            return fromEnvironment;
        }
        Credentials fromContainer = fromContainer();
        if(fromContainer != null) {
            return fromContainer;
        }
        Credentials fromInstance = fromInstanceMetadata();
        if(fromInstance != null) {
            return fromInstance;
        }
        throw new IOException("No AWS credentials: AWS_ACCESS_KEY_ID is unset, "
                + "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI and "
                + "AWS_CONTAINER_CREDENTIALS_FULL_URI are unset, and the instance "
                + "metadata service did not answer");
    }

    /** AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_SESSION_TOKEN, or null. */
    public static Credentials fromEnvironment() throws IOException {
        return credentialsFrom(System.getenv("AWS_ACCESS_KEY_ID"),
                System.getenv("AWS_SECRET_ACCESS_KEY"),
                System.getenv("AWS_SESSION_TOKEN"));
    }

    /**
     * The environment pair as credentials: null when NEITHER half is set, and an
     * exception when one is.
     *
     * <p>HALF A PAIR IS A BROKEN DEPLOYMENT, not an absent one. Answering null
     * for it sent resolve() on to the container endpoint and then to instance
     * metadata, so a misspelled key in a Kubernetes Secret -- or a secret that
     * failed to mount -- did not fail the workload: it ran under the node's role
     * instead, with whatever that role can do, and the only evidence was an
     * access denied somewhere else entirely, or nothing at all if the node role
     * happened to be wider. Every other AWS SDK treats this as an error for the
     * same reason; botocore has a name for it, PartialCredentialsError.
     *
     * <p>Taken apart from the getenv calls so the rule can be driven with values
     * rather than by a process's environment, which it cannot set for itself.
     * The session token is genuinely optional and is not part of the pair.
     */
    static Credentials credentialsFrom(String id, String secret, String token)
            throws IOException {
        boolean hasId = id != null && id.length() > 0;
        boolean hasSecret = secret != null && secret.length() > 0;
        if(!hasId && !hasSecret) {
            return null;
        }
        if(!hasId || !hasSecret) {
            // The names only. This message goes wherever the caller logs it, and
            // the half that IS set is a live credential.
            throw new IOException("AWS_" + (hasId ? "SECRET_ACCESS_KEY" : "ACCESS_KEY_ID")
                    + " is unset while AWS_"
                    + (hasId ? "ACCESS_KEY_ID" : "SECRET_ACCESS_KEY")
                    + " is set. Half a credential pair is a misconfiguration, so "
                    + "it is refused rather than ignored: ignoring it would run "
                    + "this workload under the container or instance role instead, "
                    + "which is a different identity than the one configured here.");
        }
        return new Credentials(id, secret,
                token == null || token.length() == 0 ? null : token);
    }

    /** The ECS / EKS container credential endpoint, or null when not in one. */
    public static Credentials fromContainer() throws IOException {
        String relative = System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI");
        String full = System.getenv("AWS_CONTAINER_CREDENTIALS_FULL_URI");
        String url;
        if(relative != null && relative.length() > 0) {
            url = "http://169.254.170.2" + relative;
        } else if(full != null && full.length() > 0) {
            url = full;
        } else {
            return null;
        }
        java.util.List headers = new java.util.ArrayList();
        String tokenFile = System.getenv("AWS_CONTAINER_AUTHORIZATION_TOKEN_FILE");
        String token = System.getenv("AWS_CONTAINER_AUTHORIZATION_TOKEN");
        if(tokenFile != null && tokenFile.length() > 0) {
            token = readFile(tokenFile);
        }
        if(token != null && token.length() > 0) {
            headers.add("Authorization: " + token.trim());
        }
        requireSafeCredentialEndpoint(url);
        Web.Result result = Web.request("GET", url, headers, null);
        if(!result.isSuccess()) {
            throw new IOException("The container credential endpoint answered "
                    + result.getStatus());
        }
        return fromJson(result.getBodyAsString());
    }

    /**
     * Refuses a container credential endpoint that would carry credentials in the
     * clear to somewhere off this host.
     *
     * <p>AWS_CONTAINER_CREDENTIALS_FULL_URI is taken from the environment and used
     * as given. Pointed at an http:// host that is not this container's, the
     * request carries the container authorization token TO that host and brings
     * the role's access key, secret and session token back from it -- in
     * plaintext, for anything on the path to read. A deployment typo is enough;
     * so is one injected environment value.
     *
     * <p>The rule the AWS SDKs apply, and the one applied here: https anywhere,
     * http only to this host's loopback or to the ECS and EKS link-local
     * addresses that ARE the container credential service.
     *
     * <p>The loopback test parses the address rather than matching a prefix. A
     * host is only 127.0.0.0/8 if it is four decimal octets beginning with 127 --
     * "127.evil.example" is a NAME, and a name resolves wherever its owner says.
     */
    static void requireSafeCredentialEndpoint(String url) throws IOException {
        if(url.regionMatches(true, 0, "https://", 0, 8)) {
            return;
        }
        if(!url.regionMatches(true, 0, "http://", 0, 7)) {
            throw new IOException("A container credential endpoint must be http or "
                    + "https and this one is neither: " + url);
        }
        int at = 7;
        while(at < url.length() && url.charAt(at) != '/' && url.charAt(at) != '?'
                && url.charAt(at) != '#') {
            at++;
        }
        String authority = url.substring(7, at);
        int userinfo = authority.lastIndexOf('@');
        if(userinfo >= 0) {
            authority = authority.substring(userinfo + 1);
        }
        String host = authority;
        if(host.length() > 0 && host.charAt(0) == '[') {
            int close = host.indexOf(']');
            host = close < 0 ? host : host.substring(1, close);
        } else {
            int colon = host.indexOf(':');
            if(colon >= 0) {
                host = host.substring(0, colon);
            }
        }
        // 169.254.170.2 is the ECS task metadata address and 169.254.170.23 the
        // EKS pod identity one; both are link-local, so they are this host by
        // definition.
        if(isLoopbackAddress(host) || "169.254.170.2".equals(host)
                || "169.254.170.23".equals(host)) {
            return;
        }
        throw new IOException("A container credential endpoint that is not this "
                + "host's must use https: " + url + " would send the container "
                + "authorization token and receive the role's keys in the clear");
    }

    /** True for ::1, for localhost, and for a dotted quad in 127.0.0.0/8. */
    private static boolean isLoopbackAddress(String host) {
        if("::1".equals(host) || "localhost".equalsIgnoreCase(host)) {
            return true;
        }
        int octets = 0;
        int at = 0;
        int first = -1;
        while(at <= host.length()) {
            int dot = host.indexOf('.', at);
            int end = dot < 0 ? host.length() : dot;
            if(end == at || end - at > 3) {
                return false;
            }
            int value = 0;
            for(int iter = at ; iter < end ; iter++) {
                char c = host.charAt(iter);
                if(c < '0' || c > '9') {
                    return false;
                }
                value = value * 10 + (c - '0');
            }
            if(value > 255) {
                return false;
            }
            if(octets == 0) {
                first = value;
            }
            octets++;
            if(dot < 0) {
                break;
            }
            at = dot + 1;
        }
        return octets == 4 && first == 127;
    }

    /**
     * IMDSv2. The PUT that obtains a token is the whole point: a v1 GET can be
     * made by anything that can persuade this process to fetch a URL.
     */
    public static Credentials fromInstanceMetadata() {
        try {
            java.util.List tokenHeaders = new java.util.ArrayList();
            tokenHeaders.add("X-aws-ec2-metadata-token-ttl-seconds: 300");
            Web.Result token = Web.request("PUT",
                    "http://169.254.169.254/latest/api/token", tokenHeaders, new byte[0]);
            if(!token.isSuccess()) {
                return null;
            }
            java.util.List headers = new java.util.ArrayList();
            headers.add("X-aws-ec2-metadata-token: " + token.getBodyAsString().trim());
            Web.Result roles = Web.request("GET",
                    "http://169.254.169.254/latest/meta-data/iam/security-credentials/",
                    headers, null);
            if(!roles.isSuccess()) {
                return null;
            }
            String role = roles.getBodyAsString().trim();
            int newline = role.indexOf('\n');
            if(newline > 0) {
                role = role.substring(0, newline).trim();
            }
            if(role.length() == 0) {
                return null;
            }
            Web.Result body = Web.request("GET",
                    "http://169.254.169.254/latest/meta-data/iam/security-credentials/" + role,
                    headers, null);
            if(!body.isSuccess()) {
                return null;
            }
            return fromJson(body.getBodyAsString());
        } catch (Exception err) {
            // Not on EC2, or the link-local address is unreachable. That is not an
            // error at this layer -- resolve() reports what it tried.
            return null;
        }
    }

    /**
     * The shape both endpoints return: AccessKeyId, SecretAccessKey, Token and
     * Expiration.
     */
    static Credentials fromJson(String json) throws IOException {
        Map parsed = Json.parseObject(json);
        String id = string(parsed, "AccessKeyId");
        String secret = string(parsed, "SecretAccessKey");
        if(isMissing(id) || isMissing(secret)) {
            throw new IOException("The credential endpoint returned no usable key "
                    + "pair");
        }
        String token = string(parsed, "Token");
        if(isMissing(token)) {
            token = string(parsed, "SessionToken");
        }
        // AND A TOKEN, by the same argument the expiry below is required by: both
        // callers are the metadata providers and everything they hand out is
        // temporary, which is signed with x-amz-security-token. Without one every
        // signed request comes back rejected -- and because the expiry beside it
        // may be hours away, S3 would cache the unusable credential until the
        // refresh margin rather than resolving again. A credential that cannot
        // sign is not a credential.
        if(isMissing(token)) {
            throw new IOException("The credential endpoint returned no session "
                    + "token. Temporary credentials are signed with one, so every "
                    + "request made with this pair would be rejected");
        }
        // AN EXPIRY IS REQUIRED HERE. Both callers of this are the metadata
        // providers -- ECS/EKS and IMDS -- and everything they hand out is
        // TEMPORARY. expiryMillis answers 0 for an Expiration that is missing or
        // that it cannot read, and 0 is the sentinel isExpiring reads as "never
        // expires": the credential would then be refreshed exactly never, and
        // every request after the provider's real expiry would sign with a dead
        // key until the process restarted. A field this code could not understand
        // is a reason to fail here, where the message can say so, rather than in
        // an hour's time as an authorization error with no visible cause.
        String expiration = string(parsed, "Expiration");
        long expiresAt = expiryMillis(expiration);
        if(expiresAt <= 0) {
            throw new IOException("The credential endpoint returned "
                    + (expiration == null ? "no Expiration" : "an Expiration this "
                            + "runtime cannot read: " + expiration)
                    + ". Temporary credentials that never expire would be refreshed "
                    + "never and used after the provider retired them");
        }
        return new Credentials(id, secret, token, expiresAt);
    }

    /**
     * "2026-08-28T13:45:00Z" to epoch millis. Parsed by hand for the same reason
     * {@link Clock} formats by hand: the translated runtime's date parsing is
     * locale-aware and this format is not.
     */
    static long expiryMillis(String iso) {
        if(iso == null || iso.length() < 20) {
            return 0;
        }
        // THE SEPARATORS, because every field below is taken by POSITION. Without
        // this, anything with digits in the right places parses: "2026-08-28
        // 13:45:00Z" with a space for the T, or a local time with an offset, which
        // the arithmetic then reads as if it were UTC.
        if(iso.charAt(4) != '-' || iso.charAt(7) != '-' || iso.charAt(10) != 'T'
                || iso.charAt(13) != ':' || iso.charAt(16) != ':') {
            return 0;
        }
        // Optional fractional seconds, then Z and nothing else. An offset is
        // refused rather than misread: this arithmetic has no notion of one, so
        // "-05:00" would move the expiry five hours the wrong way.
        int at = 19;
        if(iso.charAt(at) == '.') {
            at++;
            int digits = 0;
            while(at < iso.length() && iso.charAt(at) >= '0' && iso.charAt(at) <= '9') {
                at++;
                digits++;
            }
            if(digits == 0) {
                return 0;
            }
        }
        if(at != iso.length() - 1 || iso.charAt(at) != 'Z') {
            return 0;
        }
        try {
            int year = Integer.parseInt(iso.substring(0, 4));
            int month = Integer.parseInt(iso.substring(5, 7));
            int day = Integer.parseInt(iso.substring(8, 10));
            int hour = Integer.parseInt(iso.substring(11, 13));
            int minute = Integer.parseInt(iso.substring(14, 16));
            int second = Integer.parseInt(iso.substring(17, 19));
            int[] lengths = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
            // A DATE THAT EXISTS. Digits alone are not enough: the arithmetic below
            // is a running total, so February the 31st simply carries into March
            // and an hour of 99 adds four days. Both are LATER than the provider
            // meant, which is the direction that matters -- the credentials would
            // be treated as live after they had been retired, and the failure would
            // arrive as an authorization error with no visible cause.
            if(year < 1970 || month < 1 || month > 12 || day < 1
                    || hour > 23 || minute > 59 || second > 60) {
                return 0;
            }
            int maxDay = lengths[month - 1] + (month == 2 && isLeap(year) ? 1 : 0);
            if(day > maxDay) {
                return 0;
            }
            long days = 0;
            for(int y = 1970 ; y < year ; y++) {
                days += isLeap(y) ? 366 : 365;
            }
            for(int m = 0 ; m < month - 1 ; m++) {
                days += lengths[m] + (m == 1 && isLeap(year) ? 1 : 0);
            }
            days += day - 1;
            return ((days * 24L + hour) * 60L + minute) * 60L * 1000L + second * 1000L;
        } catch (Exception err) {
            return 0;
        }
    }

    private static boolean isLeap(int year) {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
    }

    /**
     * Whether a field the credential endpoint returned is missing in any of the
     * ways that leave it unusable.
     *
     * <p>Absent, empty and whitespace are ONE CASE. A key or a token made of
     * spaces signs exactly as well as one that is not there -- which is not at
     * all -- so a response carrying either is malformed, and the only thing the
     * difference decides is whether the failure is visible here or an hour from
     * now as an authorization error with nothing attached to explain it. The
     * expiry beside these fields may be hours away, so S3 would cache the
     * credential that long rather than resolving again.
     */
    private static boolean isMissing(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String string(Map map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String readFile(String path) throws IOException {
        java.io.InputStream in = new java.io.FileInputStream(path);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while((n = in.read(chunk)) > 0) {
                out.write(chunk, 0, n);
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }
}
