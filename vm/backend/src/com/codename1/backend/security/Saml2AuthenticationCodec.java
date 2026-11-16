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
package com.codename1.backend.security;

import com.codename1.backend.security.saml2.Saml2AuthenticatedPrincipal;
import com.codename1.backend.security.saml2.Saml2Authentication;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Keeps a SAML sign-in in the session as what it is: a later request sees a
/// [Saml2Authentication] again, with the NameID, its format, the attributes,
/// the session indexes and the registration it came through. Installed by
/// `http.saml2Login(...)`.
final class Saml2AuthenticationCodec implements AuthenticationCodec {
    static final String KIND = "saml2";

    @Override
    public String getKind() {
        return KIND;
    }

    @Override
    public Map<String, Object> encode(Authentication authentication) {
        if (!(authentication instanceof Saml2Authentication)) {
            return null;
        }
        Saml2AuthenticatedPrincipal p = ((Saml2Authentication) authentication).getPrincipal();
        Map<String, Object> kept = new LinkedHashMap<String, Object>();
        kept.put("registrationId", p.getRelyingPartyRegistrationId());
        if (p.getNameIdFormat() != null) {
            kept.put("nameIdFormat", p.getNameIdFormat());
        }
        Map<String, Object> attributes = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, List<String>> e : p.getAttributes().entrySet()) {
            attributes.put(e.getKey(), new ArrayList<Object>(e.getValue()));
        }
        kept.put("attributes", attributes);
        kept.put("sessionIndexes", new ArrayList<Object>(p.getSessionIndexes()));
        if (p.getNameQualifier() != null) {
            kept.put("nameQualifier", p.getNameQualifier());
        }
        if (p.getSpNameQualifier() != null) {
            kept.put("spNameQualifier", p.getSpNameQualifier());
        }
        return kept;
    }

    @Override
    public Authentication decode(String name, List<GrantedAuthority> authorities, Map stored) {
        Object registrationId = stored.get("registrationId");
        Object format = stored.get("nameIdFormat");
        Object attributes = stored.get("attributes");
        Object indexes = stored.get("sessionIndexes");
        Object qualifier = stored.get("nameQualifier");
        Object spQualifier = stored.get("spNameQualifier");
        if (!(registrationId instanceof String) || !(attributes instanceof Map)
                || name == null || name.length() == 0) {
            return null;
        }
        Map<String, List<String>> said = new LinkedHashMap<String, List<String>>();
        for (Object entry : ((Map) attributes).entrySet()) {
            Map.Entry e = (Map.Entry) entry;
            if (e.getKey() instanceof String && e.getValue() instanceof List) {
                said.put((String) e.getKey(), strings((List) e.getValue()));
            }
        }
        return new Saml2Authentication(new Saml2AuthenticatedPrincipal(name,
                format instanceof String ? (String) format : null, said,
                indexes instanceof List ? strings((List) indexes) : null,
                (String) registrationId, qualifier instanceof String ? (String) qualifier : null,
                spQualifier instanceof String ? (String) spQualifier : null), authorities);
    }

    private static List<String> strings(List values) {
        List<String> out = new ArrayList<String>();
        for (Object v : values) {
            if (v instanceof String) {
                out.add((String) v);
            }
        }
        return out;
    }
}
