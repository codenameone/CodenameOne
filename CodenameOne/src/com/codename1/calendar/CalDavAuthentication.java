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
package com.codename1.calendar;

import com.codename1.security.Hash;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import java.util.HashMap;
import java.util.Map;

/// App-owned CalDAV authentication strategy. Built-in strategies cover Basic,
/// OAuth Bearer, and RFC 7616 Digest authentication.
public abstract class CalDavAuthentication {

    public abstract AsyncResource<String> authorization(String method, String uri, String challenge, boolean forceRefresh);

    public static CalDavAuthentication basic(final String username, final String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username and password required");
        }
        return new CalDavAuthentication() {

            @Override
            public AsyncResource<String> authorization(String method, String uri, String challenge, boolean forceRefresh) {
                return complete("Basic " + Base64.encodeNoNewline(StringUtil.getBytes(username + ":" + password)));
            }
        };
    }

    public static CalDavAuthentication bearer(final CalendarTokenProvider tokens, final String... scopes) {
        if (tokens == null) {
            throw new IllegalArgumentException("token provider required");
        }
        return new CalDavAuthentication() {

            @Override
            public AsyncResource<String> authorization(String method, String uri, String challenge, boolean forceRefresh) {
                final AsyncResource<String> out = new AsyncResource<String>();
                tokens.getToken(scopes, forceRefresh).ready(new SuccessCallback<CalendarAuthToken>() {

                    @Override
                    public void onSucess(CalendarAuthToken token) {
                        out.complete("Bearer " + token.getAccessToken());
                    }
                }).except(new SuccessCallback<Throwable>() {

                    @Override
                    public void onSucess(Throwable error) {
                        out.error(error);
                    }
                });
                return out;
            }
        };
    }

    public static CalDavAuthentication digest(final String username, final String password) {
        if (username == null || password == null) {
            throw new IllegalArgumentException("username and password required");
        }
        return new Digest(username, password);
    }

    private static final class Digest extends CalDavAuthentication {

        private final String username;

        private final String password;

        private int nonceCount;

        Digest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public synchronized AsyncResource<String> authorization(String method, String uri, String challenge, boolean forceRefresh) {
            if (challenge == null || !challenge.toLowerCase().startsWith("digest ")) {
                return complete(null);
            }
            Map<String, String> p = parse(challenge.substring(7));
            String realm = p.get("realm");
            String nonce = p.get("nonce");
            String opaque = p.get("opaque");
            String qop = selectQop(p.get("qop"));
            if (realm == null || nonce == null) {
                return failed(new CalendarException(CalendarError.AUTHENTICATION_REQUIRED, "Invalid Digest challenge"));
            }
            String algorithm = p.get("algorithm");
            if (algorithm != null && !"MD5".equalsIgnoreCase(algorithm) && !"MD5-sess".equalsIgnoreCase(algorithm)) {
                return failed(new CalendarException(CalendarError.NOT_SUPPORTED, "Digest algorithm " + algorithm + " is not supported"));
            }
            String nc = hex(++nonceCount, 8);
            String cnonce = md5(String.valueOf(System.currentTimeMillis()) + ":" + nonceCount).substring(0, 16);
            String ha1 = md5(username + ":" + realm + ":" + password);
            if ("MD5-sess".equalsIgnoreCase(algorithm)) {
                ha1 = md5(ha1 + ":" + nonce + ":" + cnonce);
            }
            String ha2 = md5(method + ":" + uri);
            String response = qop == null ? md5(ha1 + ":" + nonce + ":" + ha2) : md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);
            StringBuilder out = new StringBuilder("Digest username=\"").append(escape(username)).append("\", realm=\"").append(escape(realm)).append("\", nonce=\"").append(escape(nonce)).append("\", uri=\"").append(escape(uri)).append("\", response=\"").append(response).append('"');
            if (algorithm != null) {
                out.append(", algorithm=").append(algorithm);
            }
            if (opaque != null) {
                out.append(", opaque=\"").append(escape(opaque)).append('"');
            }
            if (qop != null) {
                out.append(", qop=").append(qop).append(", nc=").append(nc).append(", cnonce=\"").append(cnonce).append('"');
            }
            return complete(out.toString());
        }
    }

    private static Map<String, String> parse(String value) {
        Map<String, String> out = new HashMap<String, String>();
        int i = 0;
        while (i < value.length()) {
            while (i < value.length() && (value.charAt(i) == ' ' || value.charAt(i) == ',')) {
                i++;
            }
            int eq = value.indexOf('=', i);
            if (eq < 0) {
                break;
            }
            String key = value.substring(i, eq).trim().toLowerCase();
            i = eq + 1;
            String data;
            if (i < value.length() && value.charAt(i) == '"') {
                i++;
                StringBuilder b = new StringBuilder();
                while (i < value.length()) {
                    char c = value.charAt(i++);
                    if (c == '"') {
                        break;
                    }
                    if (c == '\\' && i < value.length()) {
                        c = value.charAt(i++);
                    }
                    b.append(c);
                }
                data = b.toString();
            } else {
                int comma = value.indexOf(',', i);
                if (comma < 0) {
                    comma = value.length();
                }
                data = value.substring(i, comma).trim();
                i = comma;
            }
            out.put(key, data);
        }
        return out;
    }

    private static String selectQop(String qop) {
        if (qop == null) {
            return null;
        }
        for (String v : CalendarDateUtil.split(qop, ',')) {
            if ("auth".equalsIgnoreCase(v.trim())) {
                return "auth";
            }
        }
        return null;
    }

    private static String md5(String value) {
        Hash hash = Hash.create(Hash.MD5);
        hash.update(StringUtil.getBytes(value));
        byte[] bytes = hash.digest();
        StringBuilder out = new StringBuilder();
        for (byte b : bytes) {
            out.append(hex(b & 255, 2));
        }
        return out.toString();
    }

    private static String hex(int value, int width) {
        String s = Integer.toHexString(value);
        while (s.length() < width) {
            s = "0" + s;
        }
        return s;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static <T> AsyncResource<T> complete(T value) {
        AsyncResource<T> out = new AsyncResource<T>();
        out.complete(value);
        return out;
    }

    private static <T> AsyncResource<T> failed(Throwable error) {
        AsyncResource<T> out = new AsyncResource<T>();
        out.error(error);
        return out;
    }
}
