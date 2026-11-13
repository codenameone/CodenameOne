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
package com.codename1.backend.mongo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// A parsed `mongodb://` connection string.
///
/// `mongodb://user:pass@host1:27017,host2/db?authSource=admin&tls=true`. The
/// options read are `authSource`, `tls`/`ssl`, `tlsCAFile`,
/// `tlsAllowInvalidHostnames`, `connectTimeoutMS`, `socketTimeoutMS` and
/// `maxPoolSize`; others are ignored, as the official drivers ignore options
/// they do not know.
final class MongoUri {
    final List hosts = new ArrayList();
    String user;
    String password;
    String database;
    String authSource;
    boolean tls;
    String tlsCaFile;
    boolean tlsAllowInvalidHostnames;
    int connectTimeoutMillis = 10000;
    int socketTimeoutMillis;
    int maxPoolSize = 16;

    static MongoUri parse(String uri) throws IOException {
        if (uri == null) {
            throw new IOException("No MongoDB connection string");
        }
        if (uri.regionMatches(true, 0, "mongodb+srv://", 0, 14)) {
            throw new IOException("mongodb+srv:// needs DNS SRV lookups this runtime does "
                    + "not have; use the mongodb:// form with the hosts listed (Atlas "
                    + "shows it under \"standard connection string\")");
        }
        if (!uri.regionMatches(true, 0, "mongodb://", 0, 10)) {
            throw new IOException("Not a mongodb:// connection string");
        }
        MongoUri u = new MongoUri();
        String rest = uri.substring(10);
        int q = rest.indexOf('?');
        String options = q < 0 ? "" : rest.substring(q + 1);
        if (q >= 0) {
            rest = rest.substring(0, q);
        }
        int slash = rest.indexOf('/');
        String authority = slash < 0 ? rest : rest.substring(0, slash);
        if (slash >= 0 && slash + 1 < rest.length()) {
            u.database = decode(rest.substring(slash + 1));
        }
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            String cred = authority.substring(0, at);
            authority = authority.substring(at + 1);
            int colon = cred.indexOf(':');
            u.user = decode(colon < 0 ? cred : cred.substring(0, colon));
            u.password = colon < 0 ? null : decode(cred.substring(colon + 1));
        }
        int start = 0;
        while (start <= authority.length()) {
            int comma = authority.indexOf(',', start);
            String h = comma < 0 ? authority.substring(start) : authority.substring(start, comma);
            if (h.length() > 0) {
                u.hosts.add(h);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        if (u.hosts.isEmpty()) {
            throw new IOException("A MongoDB connection string names no host");
        }
        int pos = 0;
        while (pos < options.length()) {
            int amp = options.indexOf('&', pos);
            String pair = amp < 0 ? options.substring(pos) : options.substring(pos, amp);
            int eq = pair.indexOf('=');
            if (eq > 0) {
                u.option(pair.substring(0, eq), decode(pair.substring(eq + 1)));
            }
            if (amp < 0) {
                break;
            }
            pos = amp + 1;
        }
        if (u.authSource == null) {
            u.authSource = u.database == null ? "admin" : u.database;
        }
        return u;
    }

    private void option(String key, String value) throws IOException {
        boolean on = "true".equalsIgnoreCase(value);
        if ("authSource".equalsIgnoreCase(key)) {
            authSource = value;
        } else if ("tls".equalsIgnoreCase(key) || "ssl".equalsIgnoreCase(key)) {
            tls = on;
        } else if ("tlsCAFile".equalsIgnoreCase(key)) {
            tlsCaFile = value;
            tls = true;
        } else if ("tlsAllowInvalidHostnames".equalsIgnoreCase(key)) {
            tlsAllowInvalidHostnames = on;
        } else if ("connectTimeoutMS".equalsIgnoreCase(key)) {
            connectTimeoutMillis = number(key, value);
        } else if ("socketTimeoutMS".equalsIgnoreCase(key)) {
            socketTimeoutMillis = number(key, value);
        } else if ("maxPoolSize".equalsIgnoreCase(key)) {
            maxPoolSize = number(key, value);
        } else if ("authMechanism".equalsIgnoreCase(key)
                && !"SCRAM-SHA-256".equalsIgnoreCase(value)) {
            throw new IOException("authMechanism=" + value + " is not supported; this "
                    + "client authenticates with SCRAM-SHA-256");
        }
    }

    private static int number(String key, String value) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException err) {
            throw new IOException(key + " is not a number: " + value);
        }
    }

    static String decode(String s) throws IOException {
        if (s.indexOf('%') < 0) {
            return s;
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    throw new IOException("Bad percent escape in a MongoDB URI");
                }
                out.write((hi << 4) | lo);
                i += 2;
            } else {
                // The whole run up to the next escape at once, so a surrogate pair
                // is encoded as one character rather than two broken halves.
                int next = s.indexOf('%', i + 1);
                if (next < 0) {
                    next = s.length();
                }
                byte[] b = com.codename1.impl.backend.WireBuffer.utf8(s.substring(i, next));
                out.write(b, 0, b.length);
                i = next - 1;
            }
        }
        return com.codename1.impl.backend.WireBuffer.fromUtf8(out.toByteArray());
    }

    static String hostName(String hostPort) {
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            return close < 0 ? hostPort : hostPort.substring(1, close);
        }
        int colon = hostPort.lastIndexOf(':');
        return colon < 0 ? hostPort : hostPort.substring(0, colon);
    }

    static int port(String hostPort) {
        int close = hostPort.indexOf(']');
        int colon = hostPort.lastIndexOf(':');
        if (colon < 0 || colon < close) {
            return 27017;
        }
        try {
            return Integer.parseInt(hostPort.substring(colon + 1));
        } catch (NumberFormatException err) {
            return 27017;
        }
    }
}
