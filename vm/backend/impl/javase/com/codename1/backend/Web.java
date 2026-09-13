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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java SE twin of Web, on HttpURLConnection.
 *
 * Certificate verification is the JVM's default trust store, and there is
 * deliberately no way to turn it off here either -- an "insecure" flag is the kind
 * of thing that ships enabled.
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

    public static final class Result {
        private final int status;
        private final byte[] body;
        private final String error;
        private final Map headers;
        private final Map values;

        /** @param values name (lower case) to every value, as the twin takes. */
        Result(int status, byte[] body, String error, Map values) {
            this.status = status;
            this.body = body;
            this.error = error;
            this.values = values == null ? new LinkedHashMap() : values;
            this.headers = new LinkedHashMap();
            java.util.Iterator it = this.values.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry entry = (Map.Entry)it.next();
                List all = (List)entry.getValue();
                if(all != null && !all.isEmpty()) {
                    this.headers.put(entry.getKey(), all.get(0));
                }
            }
        }

        public int getStatus() {
            return status;
        }

        /**
         * The response headers, lower-cased names to values. See the translated
         * twin for why this exists.
         */
        public Map getHeaders() {
            return headers;
        }

        /** The FIRST value for a name; see the translated twin. */
        public String getHeader(String name) {
            List all = getHeaderValues(name);
            return all.isEmpty() ? null : (String)all.get(0);
        }

        /** Every value for a name, in arrival order; empty when absent. */
        public List getHeaderValues(String name) {
            if(name == null) {
                return new java.util.ArrayList();
            }
            List all = (List)values.get(asciiLower(name));
            return all == null ? new java.util.ArrayList() : all;
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

        public String getError() {
            return error;
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
        return request("POST", url, headers, json == null ? new byte[0] : json.getBytes("UTF-8"));
    }

    /**
     * The ceiling on one outbound response body, from CN1_WEB_MAX_RESPONSE_MB.
     *
     * Defaults to the 64MB CN1_HTTP_MAX_UPLOAD_MB uses for the inbound direction.
     * A value that is not 1..2047 plain digits is IGNORED rather than clamped: a
     * misconfigured bound must not silently become a different one than the
     * caller believes it set.
     */
    private static long maxResponseBytes() {
        long mb = 64;
        String text = System.getenv("CN1_WEB_MAX_RESPONSE_MB");
        if(text != null && text.length() > 0) {
            long parsed = 0;
            boolean digits = text.length() <= 4;
            for(int iter = 0 ; digits && iter < text.length() ; iter++) {
                char c = text.charAt(iter);
                if(c < '0' || c > '9') {
                    digits = false;
                } else {
                    parsed = parsed * 10 + (c - '0');
                }
            }
            if(digits && parsed >= 1 && parsed <= 2047) {
                mb = parsed;
            }
        }
        return mb * 1024L * 1024L;
    }

    public static Result request(String method, String url, List headers, byte[] body)
            throws IOException {
        Urls.requireHttp(url);
        // The request line's other field; see HeaderLines.requireMethod.
        HeaderLines.requireMethod(method);
        HeaderLines.validate(headers);
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
        } catch (IOException err) {
            throw new IOException("Request to " + Urls.forMessage(url) + " failed: " + err.getMessage());
        }
        try {
            String verb = method == null ? "GET" : method;
            try {
                connection.setRequestMethod(verb);
            } catch (java.net.ProtocolException unsupported) {
                // HttpURLConnection has a FIXED set of verbs and PATCH is not in
                // it, on every JDK this runs on. The packaged arm sends it through
                // CURLOPT_CUSTOMREQUEST and does not care, so an integration that
                // works once packaged fails here -- and the JDK's own message,
                // "Invalid HTTP method: PATCH", says nothing about that being the
                // difference. Reflecting over the private field is the usual trick
                // and is not one: measured, it works on 8 and throws
                // InaccessibleObjectException on 21 and 25, which are the versions
                // this actually runs on.
                throw new IOException("The local Java SE runtime cannot send " + verb
                        + " -- HttpURLConnection accepts a fixed set of verbs and this "
                        + "is not one of them. The packaged backend sends it normally, "
                        + "so this is a limitation of cn1:backend rather than of your "
                        + "code. Exercise this path against the packaged binary, or use "
                        + "POST with the override header your service expects.");
            }
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            // Following a redirect RESENDS the caller's headers to wherever it
            // points, and HttpURLConnection carries every request property over
            // -- it knows nothing about which of them is an X-Api-Key. A single
            // 3xx from a service that has been taken over, or one that simply
            // redirects off-domain, is then enough to hand the credential to the
            // new host, with the caller never seeing where its header went.
            // The packaged arm makes exactly this distinction (see the
            // CURLOPT_FOLLOWLOCATION comment in cn1_backend_web.c) and the two
            // must not disagree about it: whatever is unsafe there is unsafe
            // here, and a difference between the arms is one more thing that
            // only shows up after packaging.
            connection.setInstanceFollowRedirects(headers == null || headers.isEmpty());
            connection.setRequestProperty("User-Agent", "codenameone-backend");
            if(headers != null) {
                for(int iter = 0 ; iter < headers.size() ; iter++) {
                    String header = String.valueOf(headers.get(iter));
                    int colon = header.indexOf(':');
                    if(colon > 0) {
                        // addRequestProperty, not set: the packaged client appends
                        // every line it is given, so two Cookie or two extension
                        // lines both go out there while setRequestProperty kept
                        // only the last -- an integration that depends on a
                        // repeated header works once packaged and quietly sends
                        // half of what it meant to under cn1:backend.
                        connection.addRequestProperty(header.substring(0, colon).trim(),
                                header.substring(colon + 1).trim());
                    }
                }
            }
            if(body != null && body.length > 0) {
                if("GET".equals(verb)) {
                    // SILENTLY REWRITTEN OTHERWISE. HttpURLConnection turns a GET
                    // into a POST the moment anything is written to it -- the JDK
                    // does it for backward compatibility and says nothing -- while
                    // the packaged arm sends the GET the caller asked for through
                    // CUSTOMREQUEST. A search endpoint that takes a body would be
                    // exercised with the wrong verb in development and the right
                    // one in production, which is the failure this arm exists to
                    // not have. Said out loud instead, the way PATCH above is.
                    throw new IOException("The local Java SE runtime cannot send a "
                            + "GET with a body -- HttpURLConnection rewrites the verb "
                            + "to POST as soon as one is written, and does it "
                            + "silently. The packaged backend sends it as GET, so "
                            + "this is a limitation of cn1:backend rather than of "
                            + "your code. Exercise this path against the packaged "
                            + "binary, or move the payload into the query string.");
                }
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                OutputStream out = connection.getOutputStream();
                out.write(body);
                out.flush();
            }
            int status;
            try {
                status = connection.getResponseCode();
            } catch (IOException err) {
                // No status at all: DNS, connect or TLS failed. The translated twin
                // throws here too rather than reporting a status of -1.
                throw new IOException("Request to " + Urls.forMessage(url) + " failed: " + err.getMessage());
            }
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if(in != null) {
                // BOUNDED. This arm had no ceiling at all, so an endless or merely
                // huge upstream response grew this buffer until the JVM died --
                // and a read timeout is no help when the bytes arrive quickly.
                // The packaged arm reads the SAME variable with the SAME default,
                // so the two refuse the same response; see
                // CN1_WEB_MAX_RESPONSE_MB in cn1_backend_web.c.
                long allowed = maxResponseBytes();
                byte[] chunk = new byte[8192];
                int n;
                long total = 0;
                while((n = in.read(chunk)) > 0) {
                    total += n;
                    if(total > allowed) {
                        throw new IOException("Request to " + Urls.forMessage(url) + " failed: the response "
                                + "is larger than the " + (allowed / (1024 * 1024))
                                + "MB CN1_WEB_MAX_RESPONSE_MB allows");
                    }
                    buffer.write(chunk, 0, n);
                }
            }
            // Lower-cased names, as the translated twin produces: a caller must
            // not have to know which case this particular server chose.
            Map responseHeaders = new LinkedHashMap();
            // BY INDEX, not through getHeaderFields. Every value is kept now --
            // taking one delivered a single cookie out of a response that set
            // three -- and the order has to be the order they arrived in, which is
            // what the translated twin produces and what this documents. The map
            // getHeaderFields builds is in the JDK's own order, and for a repeated
            // field that is not arrival order; the indexed accessors are.
            for(int iter = 0 ; ; iter++) {
                String name = connection.getHeaderFieldKey(iter);
                String value = connection.getHeaderField(iter);
                if(name == null && value == null) {
                    break;      // past the end
                }
                if(name == null) {
                    continue;   // the status line, which has no key
                }
                String key = asciiLower(name);
                List all = (List)responseHeaders.get(key);
                if(all == null) {
                    all = new java.util.ArrayList();
                    responseHeaders.put(key, all);
                }
                all.add(value);
            }
            return new Result(status, buffer.toByteArray(), null, responseHeaders);
        } finally {
            connection.disconnect();
        }
    }
}
