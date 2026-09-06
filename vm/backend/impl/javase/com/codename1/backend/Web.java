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
    private Web() {
    }

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

        /** One header by name, matched case-insensitively. Null when absent. */
        public String getHeader(String name) {
            return name == null ? null : (String)headers.get(name.toLowerCase());
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

    public static Result request(String method, String url, List headers, byte[] body)
            throws IOException {
        if(url == null) {
            throw new IOException("No URL");
        }
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
        } catch (IOException err) {
            throw new IOException("Request to " + url + " failed: " + err.getMessage());
        }
        try {
            connection.setRequestMethod(method == null ? "GET" : method);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "codenameone-backend");
            if(headers != null) {
                for(int iter = 0 ; iter < headers.size() ; iter++) {
                    String header = String.valueOf(headers.get(iter));
                    int colon = header.indexOf(':');
                    if(colon > 0) {
                        connection.setRequestProperty(header.substring(0, colon).trim(),
                                header.substring(colon + 1).trim());
                    }
                }
            }
            if(body != null && body.length > 0) {
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
                throw new IOException("Request to " + url + " failed: " + err.getMessage());
            }
            InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            if(in != null) {
                byte[] chunk = new byte[8192];
                int n;
                while((n = in.read(chunk)) > 0) {
                    buffer.write(chunk, 0, n);
                }
            }
            // Lower-cased names, as the translated twin produces: a caller must
            // not have to know which case this particular server chose.
            Map responseHeaders = new LinkedHashMap();
            Map raw = connection.getHeaderFields();
            if(raw != null) {
                java.util.Iterator it = raw.entrySet().iterator();
                while(it.hasNext()) {
                    Map.Entry entry = (Map.Entry)it.next();
                    Object name = entry.getKey();
                    if(name == null) {
                        continue; // the status line, which getHeaderFields keys as null
                    }
                    List values = (List)entry.getValue();
                    if(values != null && !values.isEmpty()) {
                        responseHeaders.put(String.valueOf(name).toLowerCase(),
                                String.valueOf(values.get(values.size() - 1)));
                    }
                }
            }
            return new Result(status, buffer.toByteArray(), null, responseHeaders);
        } finally {
            connection.disconnect();
        }
    }
}
