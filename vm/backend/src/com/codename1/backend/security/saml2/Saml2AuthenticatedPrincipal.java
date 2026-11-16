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
package com.codename1.backend.security.saml2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The user an identity provider vouched for in a SAML assertion: the
/// assertion's NameID, the attributes it stated, and the session indexes it
/// gave.
public final class Saml2AuthenticatedPrincipal {
    private final String name;
    private final String nameIdFormat;
    private final Map<String, List<String>> attributes;
    private final List<String> sessionIndexes;
    private final String relyingPartyRegistrationId;
    private final String nameQualifier;
    private final String spNameQualifier;

    public Saml2AuthenticatedPrincipal(String name, String nameIdFormat,
                                       Map<String, List<String>> attributes,
                                       List<String> sessionIndexes,
                                       String relyingPartyRegistrationId) {
        this(name, nameIdFormat, attributes, sessionIndexes, relyingPartyRegistrationId, null,
                null);
    }

    /// With the NameID's `NameQualifier` and `SPNameQualifier`, which a logout
    /// request must repeat as the assertion gave them: ADFS refuses one that
    /// does not.
    public Saml2AuthenticatedPrincipal(String name, String nameIdFormat,
                                       Map<String, List<String>> attributes,
                                       List<String> sessionIndexes,
                                       String relyingPartyRegistrationId,
                                       String nameQualifier, String spNameQualifier) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        this.name = name;
        this.nameIdFormat = nameIdFormat;
        Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
        if (attributes != null) {
            for (Map.Entry<String, List<String>> e : attributes.entrySet()) {
                copy.put(e.getKey(), Collections.unmodifiableList(
                        new ArrayList<String>(e.getValue())));
            }
        }
        this.attributes = Collections.unmodifiableMap(copy);
        this.sessionIndexes = sessionIndexes == null ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(sessionIndexes));
        this.relyingPartyRegistrationId = relyingPartyRegistrationId;
        this.nameQualifier = nameQualifier;
        this.spNameQualifier = spNameQualifier;
    }

    /// The NameID's `NameQualifier`, or null.
    public String getNameQualifier() {
        return nameQualifier;
    }

    /// The NameID's `SPNameQualifier`, or null.
    public String getSpNameQualifier() {
        return spNameQualifier;
    }

    /// The NameID.
    public String getName() {
        return name;
    }

    /// The NameID's format, or null when the assertion did not say.
    public String getNameIdFormat() {
        return nameIdFormat;
    }

    /// Every attribute, by its `Name`, with its values in order.
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    /// The values of the attribute `name`; empty when there is none.
    public List<String> getAttribute(String name) {
        List<String> v = attributes.get(name);
        return v == null ? Collections.<String>emptyList() : v;
    }

    /// The first value of the attribute `name`, or null.
    public String getFirstAttribute(String name) {
        List<String> v = attributes.get(name);
        return v == null || v.isEmpty() ? null : v.get(0);
    }

    public List<String> getSessionIndexes() {
        return sessionIndexes;
    }

    public String getRelyingPartyRegistrationId() {
        return relyingPartyRegistrationId;
    }

    @Override
    public String toString() {
        return name;
    }
}
