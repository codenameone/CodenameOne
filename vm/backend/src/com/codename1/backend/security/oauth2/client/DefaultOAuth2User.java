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
package com.codename1.backend.security.oauth2.client;

import com.codename1.backend.security.GrantedAuthority;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An [OAuth2User] that holds what it is given.
public class DefaultOAuth2User implements OAuth2User {
    private final List<GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String name;

    /// @param nameAttributeKey the attribute whose value names the user
    public DefaultOAuth2User(Collection<? extends GrantedAuthority> authorities,
                             Map<String, Object> attributes, String nameAttributeKey) {
        this(named(attributes, nameAttributeKey), authorities, attributes);
    }

    /// A user called `name`, whatever the attributes say: for a service that
    /// ties the provider's user to an account of the application's.
    public DefaultOAuth2User(String name, Collection<? extends GrantedAuthority> authorities,
                             Map<String, Object> attributes) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("A user needs a name");
        }
        this.name = name;
        List<GrantedAuthority> copy = new ArrayList<GrantedAuthority>();
        if (authorities != null) {
            copy.addAll(authorities);
        }
        this.authorities = Collections.unmodifiableList(copy);
        this.attributes = Collections.unmodifiableMap(attributes == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(attributes));
    }

    private static String named(Map<String, Object> attributes, String nameAttributeKey) {
        Object value = attributes == null || nameAttributeKey == null ? null
                : attributes.get(nameAttributeKey);
        if (value == null || String.valueOf(value).length() == 0) {
            throw new IllegalArgumentException("Missing attribute '" + nameAttributeKey
                    + "' in attributes");
        }
        // A whole number read from JSON may be a Double: 583231.0 is not a name.
        if (value instanceof Double) {
            double d = ((Double) value).doubleValue();
            long whole = (long) d;
            if (whole == d) {
                return String.valueOf(whole);
            }
        }
        return String.valueOf(value);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Name: [" + name + "], Granted Authorities: [" + authorities + "]";
    }
}
