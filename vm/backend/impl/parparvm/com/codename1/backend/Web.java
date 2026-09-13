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
package com.codename1.backend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Outbound HTTP and HTTPS for server-side binaries. Backed by libcurl, so TLS
 * verification, redirects, chunked decoding and the system certificate store all
 * come from a library that is maintained for the purpose.
 *
 * Distinct from [Http], which is a raw-socket plaintext client for the host
 * runtime's loopback control protocol. Use this one for anything real.
 *
 * **Certificate store.** A dynamically linked build finds the system CA bundle.
 * A fully static build has whatever the image provides, which for a `scratch`
 * container is nothing - and TLS then fails with "unable to get local issuer
 * certificate". Ship a `ca-certificates.crt` and point curl at it with the
 * `CURL_CA_BUNDLE` or `SSL_CERT_FILE` environment variable; both are read by
 * libcurl itself, so no code here has to know about it.
 */
public final class Web {

    /**
     * ASCII lower case, because String.toLowerCase() is LOCALE SENSITIVE and this
     * platform has no Locale to ask for the root one. On a device set to Turkish
     * the I of an ASCII token folds to a dotless i, so a header stored under one
     * spelling is looked up under another and getHeader answers null: nothing is
     * thrown, nothing is logged, and the caller reads a header that is there as
     * absent. A header name is ASCII by specification. Copied rather than shared;
     * see CLAUDE.md. Both arms of Web carry it, because both index headers.
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

    private Web() {
    }

    /** An outbound response: status, body, and libcurl's message when it failed. */
    public static final class Result {
        private final int status;
        private final byte[] body;
        private final String error;
        private final Map headers;

        Result(int status, byte[] body, String error, Map headers) {
            this.status = status;
            this.body = body;
            this.error = error;
            this.headers = headers == null ? new LinkedHashMap() : headers;
        }

        /** The HTTP status, or -1 when the transfer itself failed. */
        public int getStatus() {
            return status;
        }

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }

        public byte[] getBody() {
            return body;
        }

        public String getBodyAsString() {
            if(body == null) {
                return null;
            }
            try {
                return new String(body, "UTF-8");
            } catch (IOException err) {
                return new String(body);
            }
        }

        /** Non-null only when the transfer failed before producing a status. */
        public String getError() {
            return error;
        }

    /**
     * The response headers, lower-cased names to values.
     *
     * A response's headers are half of what an API says -- the ETag S3 returns for
     * a PUT, the content type of an object, the rate-limit budget a service
     * publishes -- and a client that can only read the body cannot see any of it.
     * Names are lower-cased because HTTP header names are case insensitive and a
     * caller should not have to guess which case this server chose.
     */
    public Map getHeaders() {
        return headers;
    }

    /** One header by name, matched case-insensitively. Null when absent. */
    public String getHeader(String name) {
        return name == null ? null : (String)headers.get(asciiLower(name));
    }
    }

    public static Result get(String url) throws IOException {
        return request("GET", url, null, null);
    }

    public static Result getJson(String url, String bearerToken) throws IOException {
        List headers = new ArrayList();
        headers.add("Accept: application/json");
        if(bearerToken != null) {
            headers.add("Authorization: Bearer " + bearerToken);
        }
        return request("GET", url, headers, null);
    }

    public static Result postJson(String url, String json, String bearerToken) throws IOException {
        List headers = new ArrayList();
        headers.add("Content-Type: application/json");
        headers.add("Accept: application/json");
        if(bearerToken != null) {
            headers.add("Authorization: Bearer " + bearerToken);
        }
        byte[] payload;
        try {
            payload = json == null ? new byte[0] : json.getBytes("UTF-8");
        } catch (IOException err) {
            throw new IOException("Could not encode the request body");
        }
        return request("POST", url, headers, payload);
    }

    /**
     * - `headers`: a list of "Name: value" strings, or null
     */
    public static Result request(String method, String url, List headers, byte[] body) throws IOException {
        Urls.requireHttp(url);
        // The request line's other field; see HeaderLines.requireMethod.
        HeaderLines.requireMethod(method);
        HeaderLines.validate(headers);
        StringBuilder joined = new StringBuilder();
        if(headers != null) {
            for(int iter = 0 ; iter < headers.size() ; iter++) {
                if(iter > 0) {
                    joined.append('\n');
                }
                joined.append(String.valueOf(headers.get(iter)));
            }
        }
        initialiseCurlOnce();
        long handle = performImpl(method, url, HeaderLines.narrowed(joined.toString()), body);
        if(handle == 0) {
            // REDACTED, because this message goes wherever the caller logs it and
            // the URL may be a presigned one whose signature is the credential.
            throw new IOException("Could not start a request to "
                    + Urls.forMessage(url));
        }
        try {
            int status = statusImpl(handle);
            String error = errorImpl(handle);
            if(status < 0) {
                throw new IOException("Request to " + Urls.forMessage(url) + " failed: "
                        + (error == null ? "unknown error" : error));
            }
            return new Result(status, bodyImpl(handle), error,
                    parseHeaders(headersImpl(handle)));
        } finally {
            freeImpl(handle);
        }
    }

    /**
     * libcurl hands back the raw header block, status lines and all. Redirects
     * mean there can be several blocks; the LAST one describes the response the
     * caller got, so a later block replaces an earlier one rather than merging
     * with it.
     */
    static Map parseHeaders(String raw) {
        Map out = new LinkedHashMap();
        if(raw == null) {
            return out;
        }
        int at = 0;
        while(at < raw.length()) {
            int end = raw.indexOf('\n', at);
            if(end < 0) {
                end = raw.length();
            }
            String line = raw.substring(at, end).trim();
            at = end + 1;
            if(line.length() == 0) {
                continue;
            }
            if(line.regionMatches(true, 0, "HTTP/", 0, 5)) {
                // A new status line: everything before it belonged to a redirect.
                out.clear();
                continue;
            }
            int colon = line.indexOf(':');
            if(colon <= 0) {
                continue;
            }
            out.put(asciiLower(line.substring(0, colon).trim()),
                    line.substring(colon + 1).trim());
        }
        return out;
    }

    /** Whether libcurl's global initialisation has been done successfully. */
    private static boolean curlInitialised;

    /**
     * Initialises libcurl once, before any thread can be inside the library.
     *
     * curl_easy_init does this implicitly on first use, and the implicit path is
     * NOT thread safe below libcurl 7.84 or in a build whose curl_version_info
     * does not report CURL_VERSION_THREADSAFE. Two workers whose first outbound
     * request overlaps would both enter it. The link is against whatever -lcurl
     * the system provides, with no version floor, so this cannot be assumed away.
     *
     * <p>Every path into libcurl here is request(), and request() is the only
     * caller of performImpl, so holding the monitor across the initialisation is
     * enough: no other thread can be inside curl while it runs.
     *
     * <p>Under the monitor rather than behind a double-checked volatile read.
     * Every caller is about to make a network request, beside which an
     * uncontended monitor costs nothing, and it keeps the guarantee off the
     * question of how the translated runtime orders a volatile.
     *
     * <p>The flag is set only when the initialisation SUCCEEDED, so a failure is
     * retried rather than remembered as done. There is no matching
     * curl_global_cleanup: the process is exiting by the time one would apply,
     * and calling it while another thread might still be in libcurl is the very
     * thing this avoids.
     */
    private static synchronized void initialiseCurlOnce() throws IOException {
        if(curlInitialised) {
            return;
        }
        int rc = globalInitImpl();
        if(rc != 0) {
            // THROWN, not left for the request to carry on past. Returning here
            // set out to retry later and meanwhile let THIS worker walk into
            // curl_easy_init, whose implicit initialisation is the very thing
            // this monitor exists to keep one thread at a time -- so a failure
            // put one worker inside the implicit path while the next was inside
            // curl_global_init under the lock, which is the race, restored.
            throw new IOException("libcurl could not be initialised (CURLcode "
                    + rc + "), so no outbound request can be made");
        }
        curlInitialised = true;
    }

    /** curl_global_init's CURLcode -- 0 is CURLE_OK. */
    private static native int globalInitImpl();

    private static native long performImpl(String method, String url, byte[] headerLines,
                                           byte[] body);
    private static native String headersImpl(long handle);
    private static native int statusImpl(long handle);
    private static native String errorImpl(long handle);
    private static native byte[] bodyImpl(long handle);
    private static native void freeImpl(long handle);
}
