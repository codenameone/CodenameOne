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
package com.codename1.backend.test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Request or response headers: names in any case, each with its values in order.
public final class HttpHeaders {
    private final Map values = new LinkedHashMap();
    private final Map names = new LinkedHashMap();

    /// Replaces a header's values with this one.
    public void set(String name, String value) {
        String key = MockRequestBuilder.lower(name);
        List list = new ArrayList();
        list.add(value);
        values.put(key, list);
        names.put(key, name);
    }

    /// Adds a value to a header.
    public void add(String name, String value) {
        String key = MockRequestBuilder.lower(name);
        List list = (List) values.get(key);
        if (list == null) {
            list = new ArrayList();
            values.put(key, list);
            names.put(key, name);
        }
        list.add(value);
    }

    /// The first value, or null.
    public String getFirst(String name) {
        List list = (List) values.get(MockRequestBuilder.lower(name));
        return list == null || list.isEmpty() ? null : (String) list.get(0);
    }

    /// Every value, or null when the header is absent.
    public List get(String name) {
        return (List) values.get(MockRequestBuilder.lower(name));
    }

    public boolean containsKey(String name) {
        return values.containsKey(MockRequestBuilder.lower(name));
    }

    public void setContentType(String type) {
        set("Content-Type", type);
    }

    public String getContentType() {
        return getFirst("Content-Type");
    }

    public void setBearerAuth(String token) {
        set("Authorization", "Bearer " + token);
    }

    public void setBasicAuth(String username, String password) {
        set("Authorization", "Basic " + com.codename1.backend.Base64.encode(
                MockRequestBuilder.utf8(username + ":" + password)));
    }

    /// The names as they were first given, in order.
    public List names() {
        return new ArrayList(names.values());
    }

    /// "Name: value" lines, one per value, for the client.
    List lines() {
        List out = new ArrayList();
        java.util.Iterator it = values.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            List list = (List) e.getValue();
            for (Object entry : list) {
                out.add(names.get(e.getKey()) + ": " + entry);
            }
        }
        return out;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
