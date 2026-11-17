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
package com.codename1.backend.security.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// One entry a search found: its distinguished name and the attributes asked
/// for. Attribute names are matched without regard to ASCII case, as LDAP
/// matches them.
public final class LdapEntry {
    private final String dn;
    private final Map<String, List<String>> attributes = new LinkedHashMap<String, List<String>>();
    private final Map<String, String> names = new LinkedHashMap<String, String>();

    public LdapEntry(String dn) {
        this.dn = dn;
    }

    public String getDn() {
        return dn;
    }

    /// Adds a value. Used by the client; an application reads entries.
    public void add(String attribute, String value) {
        String key = LdapEncoder.asciiLower(attribute);
        List<String> values = attributes.get(key);
        if (values == null) {
            values = new ArrayList<String>();
            attributes.put(key, values);
            names.put(key, attribute);
        }
        values.add(value);
    }

    /// Every value of `attribute`; empty when it has none.
    public List<String> getValues(String attribute) {
        List<String> values = attributes.get(LdapEncoder.asciiLower(attribute));
        return values == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(values);
    }

    /// The first value of `attribute`, or null.
    public String getValue(String attribute) {
        List<String> values = attributes.get(LdapEncoder.asciiLower(attribute));
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    /// The attribute names, as the server spelled them.
    public List<String> getAttributeNames() {
        return new ArrayList<String>(names.values());
    }

    @Override
    public String toString() {
        return dn;
    }
}
