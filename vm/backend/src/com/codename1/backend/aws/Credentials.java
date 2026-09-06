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
    public static Credentials fromEnvironment() {
        String id = System.getenv("AWS_ACCESS_KEY_ID");
        String secret = System.getenv("AWS_SECRET_ACCESS_KEY");
        if(id == null || id.length() == 0 || secret == null || secret.length() == 0) {
            return null;
        }
        String token = System.getenv("AWS_SESSION_TOKEN");
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
        Web.Result result = Web.request("GET", url, headers, null);
        if(!result.isSuccess()) {
            throw new IOException("The container credential endpoint answered "
                    + result.getStatus());
        }
        return fromJson(result.getBodyAsString());
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
        if(id == null || secret == null) {
            throw new IOException("The credential endpoint returned no key pair");
        }
        String token = string(parsed, "Token");
        if(token == null) {
            token = string(parsed, "SessionToken");
        }
        return new Credentials(id, secret, token, expiryMillis(string(parsed, "Expiration")));
    }

    /**
     * "2026-08-28T13:45:00Z" to epoch millis. Parsed by hand for the same reason
     * {@link Clock} formats by hand: the translated runtime's date parsing is
     * locale-aware and this format is not.
     */
    static long expiryMillis(String iso) {
        if(iso == null || iso.length() < 19) {
            return 0;
        }
        try {
            int year = Integer.parseInt(iso.substring(0, 4));
            int month = Integer.parseInt(iso.substring(5, 7));
            int day = Integer.parseInt(iso.substring(8, 10));
            int hour = Integer.parseInt(iso.substring(11, 13));
            int minute = Integer.parseInt(iso.substring(14, 16));
            int second = Integer.parseInt(iso.substring(17, 19));
            long days = 0;
            for(int y = 1970 ; y < year ; y++) {
                days += isLeap(y) ? 366 : 365;
            }
            int[] lengths = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
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
