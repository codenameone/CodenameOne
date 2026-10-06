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

import com.codename1.backend.HttpServer;
import com.codename1.backend.HttpSession;
import com.codename1.backend.security.core.userdetails.User;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Keeps who is signed in in the HTTP session, under
/// [#SPRING_SECURITY_CONTEXT_KEY].
///
/// What is stored is a map of plain values -- the name, the authorities, whether
/// the authentication is trusted, and its details when they are plain values
/// too -- rather than the [Authentication] itself. The database session store
/// keeps what JSON can write and hands it to whichever server takes the
/// client's next request, so an object would not survive the trip, and a
/// server whose sessions are in memory behaves the same way so that moving
/// to the database changes nothing.
///
/// The consequence: on a later request the principal is a
/// [User] rebuilt from the name and authorities, with no password, not the
/// object the user store returned at sign-in. A subclass that needs more
/// overrides [#toMap] and [#fromMap].
public class HttpSessionSecurityContextRepository implements SecurityContextRepository {
    /// The session attribute the context is stored under.
    public static final String SPRING_SECURITY_CONTEXT_KEY = "SPRING_SECURITY_CONTEXT";

    private boolean allowSessionCreation = true;

    /// Whether saving may start a session; true unless changed. A chain with
    /// [SessionCreationPolicy#NEVER] turns it off.
    public void setAllowSessionCreation(boolean allowSessionCreation) {
        this.allowSessionCreation = allowSessionCreation;
    }

    @Override
    public SecurityContext loadContext(HttpServer.Request request) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        HttpSession session = request.getSession(false);
        if (session == null) {
            return context;
        }
        Object stored = session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
        if (stored instanceof Map) {
            context.setAuthentication(fromMap((Map) stored));
        }
        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServer.Request request) {
        Authentication authentication = context == null ? null : context.getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute(SPRING_SECURITY_CONTEXT_KEY) != null) {
                session.removeAttribute(SPRING_SECURITY_CONTEXT_KEY);
            }
            return;
        }
        HttpSession session = request.getSession(allowSessionCreation);
        if (session != null) {
            session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, toMap(authentication));
        }
    }

    @Override
    public boolean containsContext(HttpServer.Request request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(SPRING_SECURITY_CONTEXT_KEY) != null;
    }

    /// `authentication` as the plain values a session store can keep.
    protected Map<String, Object> toMap(Authentication authentication) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("name", authentication.getName());
        List<String> authorities = new ArrayList<String>();
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            authorities.add(authority.getAuthority());
        }
        out.put("authorities", authorities);
        out.put("authenticated", Boolean.valueOf(authentication.isAuthenticated()));
        Object details = authentication.getDetails();
        if (isPlain(details, 0)) {
            out.put("details", details);
        }
        return out;
    }

    /// The authentication [#toMap] stored, or null when `stored` is not one.
    protected Authentication fromMap(Map stored) {
        Object name = stored.get("name");
        if (!(name instanceof String) || ((String) name).length() == 0) {
            return null;
        }
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        Object listed = stored.get("authorities");
        if (listed instanceof List) {
            for (Object authority : (List) listed) {
                if (authority instanceof String && ((String) authority).length() > 0) {
                    authorities.add(new SimpleGrantedAuthority((String) authority));
                }
            }
        }
        if (!Boolean.TRUE.equals(stored.get("authenticated"))) {
            return null;
        }
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken
                .authenticated(new User((String) name, "", authorities), null, authorities);
        token.setDetails(stored.get("details"));
        return token;
    }

    /// Whether `value` is something JSON can write and read back unchanged.
    private static boolean isPlain(Object value, int depth) {
        if (value == null || depth > 8) {
            return false;
        }
        if (value instanceof String || value instanceof Boolean || value instanceof Long
                || value instanceof Integer || value instanceof Double) {
            return true;
        }
        if (value instanceof Map) {
            for (Object entry : ((Map) value).entrySet()) {
                Map.Entry e = (Map.Entry) entry;
                if (!(e.getKey() instanceof String) || !isPlain(e.getValue(), depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        if (value instanceof List) {
            for (Object element : (List) value) {
                if (!isPlain(element, depth + 1)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
