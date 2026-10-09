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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A decision that turned on what the caller is granted, with the authorities
/// any one of which would have done.
///
/// A refusal that knows what was missing can say so: a bearer challenge names
/// the scope to ask for. See [AuthorizationFilter#REQUIRED_AUTHORITIES].
public class AuthorityAuthorizationDecision extends AuthorizationDecision {
    private final List<String> authorities;

    /// @param authorities the authorities the rule accepts, as it writes them
    public AuthorityAuthorizationDecision(boolean granted, String[] authorities) {
        super(granted);
        List<String> all = new ArrayList<String>();
        if (authorities != null) {
            for (String authority : authorities) {
                all.add(authority);
            }
        }
        this.authorities = Collections.unmodifiableList(all);
    }

    /// The authorities the rule accepts; the caller needed one of them.
    public List<String> getAuthorities() {
        return authorities;
    }

    @Override
    public String toString() {
        return getClass().getName() + " [granted=" + isGranted() + ", authorities=" + authorities
                + "]";
    }
}
