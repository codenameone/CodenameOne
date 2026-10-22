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
package com.codename1.flutter.plugins.dio;

import java.util.List;
import java.util.Map;

import dart.core.DartList;
import dart.core.DartMap;

/// dio's `Headers`: response headers, names folded to lower case.
public class Headers {
    public static final String contentTypeHeader = "content-type";
    public static final String jsonContentType = "application/json; charset=utf-8";
    public static final String formUrlEncodedContentType = "application/x-www-form-urlencoded";
    public static final String textPlainContentType = "text/plain";

    private final DartMap<String, DartList<String>> map = new DartMap<String, DartList<String>>();

    public Headers() {
    }

    static String fold(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return sb.toString();
    }

    public DartMap<String, DartList<String>> map() {
        return map;
    }

    public DartList<String> $index(String name) {
        return map.get(fold(name));
    }

    /// The single value of `name`, joined with ", " when there are several.
    public String value(String name) {
        List<String> vs = map.get(fold(name));
        if (vs == null || vs.isEmpty()) {
            return null;
        }
        if (vs.size() == 1) {
            return vs.get(0);
        }
        StringBuilder sb = new StringBuilder();
        for (String v : vs) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(v);
        }
        return sb.toString();
    }

    public void add(String name, String value) {
        String k = fold(name);
        DartList<String> vs = map.get(k);
        if (vs == null) {
            vs = new DartList<String>();
            map.put(k, vs);
        }
        vs.add(value);
    }

    public void set(String name, Object value) {
        DartList<String> vs = new DartList<String>();
        if (value instanceof List) {
            for (Object o : (List<?>) value) {
                vs.add(String.valueOf(o));
            }
        } else if (value != null) {
            vs.add(String.valueOf(value));
        }
        map.put(fold(name), vs);
    }

    public void removeAll(String name) {
        map.remove(fold(name));
    }

    public void forEach(dart.runtime.Funcs.VoidFunc2<String, DartList<String>> f) {
        for (Map.Entry<String, DartList<String>> e : map.entrySet()) {
            f.call(e.getKey(), e.getValue());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, DartList<String>> e : map.entrySet()) {
            sb.append(e.getKey()).append(": ").append(value(e.getKey())).append('\n');
        }
        return sb.toString();
    }
}
