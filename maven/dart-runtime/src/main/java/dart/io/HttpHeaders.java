/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.io;

import dart.core.DartList;
import dart.runtime.Dyn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dart's {@code HttpHeaders}: names are case-insensitive and kept lower-cased, as
 * dart:io does; each name holds a list of values.
 */
public class HttpHeaders {
    public static final String acceptHeader = "accept";
    public static final String acceptEncodingHeader = "accept-encoding";
    public static final String acceptLanguageHeader = "accept-language";
    public static final String authorizationHeader = "authorization";
    public static final String cacheControlHeader = "cache-control";
    public static final String contentEncodingHeader = "content-encoding";
    public static final String contentLengthHeader = "content-length";
    public static final String contentTypeHeader = "content-type";
    public static final String cookieHeader = "cookie";
    public static final String dateHeader = "date";
    public static final String etagHeader = "etag";
    public static final String hostHeader = "host";
    public static final String ifModifiedSinceHeader = "if-modified-since";
    public static final String ifNoneMatchHeader = "if-none-match";
    public static final String lastModifiedHeader = "last-modified";
    public static final String locationHeader = "location";
    public static final String setCookieHeader = "set-cookie";
    public static final String userAgentHeader = "user-agent";

    private final Map<String, List<String>> values = new LinkedHashMap<String, List<String>>();
    private boolean chunked;

    /** ASCII lower-casing: header names are ASCII and must not fold by locale. */
    static String asciiLower(String s) {
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(s);
                }
                sb.setCharAt(i, (char) (c + 32));
            }
        }
        return sb == null ? s : sb.toString();
    }

    public DartList<String> $index(String name) {
        List<String> v = values.get(asciiLower(name));
        if (v == null) {
            return null;
        }
        DartList<String> out = new DartList<String>();
        out.addAll(v);
        return out;
    }

    public String value(String name) {
        List<String> v = values.get(asciiLower(name));
        if (v == null || v.isEmpty()) {
            return null;
        }
        if (v.size() > 1) {
            throw new HttpException("More than one value for header " + name);
        }
        return v.get(0);
    }

    public void add(String name, Object value) {
        add(name, value, null);
    }

    public void add(String name, Object value, Boolean preserveHeaderCase) {
        String key = asciiLower(name);
        List<String> v = values.get(key);
        if (v == null) {
            v = new ArrayList<String>();
            values.put(key, v);
        }
        if (value instanceof Iterable) {
            for (Object o : (Iterable<?>) value) {
                v.add(String.valueOf(o));
            }
        } else {
            v.add(String.valueOf(value));
        }
    }

    public void set(String name, Object value) {
        set(name, value, null);
    }

    public void set(String name, Object value, Boolean preserveHeaderCase) {
        values.remove(asciiLower(name));
        if (value != null) {
            add(name, value);
        }
    }

    public void remove(String name, Object value) {
        List<String> v = values.get(asciiLower(name));
        if (v != null) {
            v.remove(String.valueOf(value));
            if (v.isEmpty()) {
                values.remove(asciiLower(name));
            }
        }
    }

    public void removeAll(String name) {
        values.remove(asciiLower(name));
    }

    public void clear() {
        values.clear();
    }

    /** Dart's {@code forEach((name, values) {...})}. */
    public void forEach(Object action) {
        for (Map.Entry<String, List<String>> e : new ArrayList<Map.Entry<String, List<String>>>(values.entrySet())) {
            DartList<String> l = new DartList<String>();
            l.addAll(e.getValue());
            Dyn.invoke(action, e.getKey(), l);
        }
    }

    public ContentType contentType() {
        String v = value(contentTypeHeader);
        return v == null ? null : ContentType.parse(v);
    }

    public void contentType(ContentType type) {
        set(contentTypeHeader, type == null ? null : type.toString());
    }

    public long contentLength() {
        String v = value(contentLengthHeader);
        if (v == null) {
            return -1;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void contentLength(long length) {
        if (length < 0) {
            removeAll(contentLengthHeader);
        } else {
            set(contentLengthHeader, Long.toString(length));
        }
    }

    public boolean chunkedTransferEncoding() {
        return chunked;
    }

    public void chunkedTransferEncoding(boolean v) {
        chunked = v;
    }

    /** Every name and value, in insertion order; for the request that sends them. */
    Map<String, List<String>> all() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> e : values.entrySet()) {
            for (String v : e.getValue()) {
                sb.append(e.getKey()).append(": ").append(v).append('\n');
            }
        }
        return sb.toString();
    }
}
