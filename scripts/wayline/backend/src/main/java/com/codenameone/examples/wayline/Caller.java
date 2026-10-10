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
package com.codenameone.examples.wayline;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SecurityContextHolder;

import java.util.Iterator;

/// Who is making the request being served.
///
/// The endpoints implement interfaces generated from the shared contract, whose
/// methods take what the app sends and nothing else, so the caller is read from
/// the security context of the request and not from a parameter.
public final class Caller {
    private Caller() {
    }

    /// The signed-in user's name, which is the e-mail address of the account.
    /// Answers 401 when nobody is signed in; the filter chain has normally done
    /// so already, and this is what stands behind it.
    public static String name() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (who == null || !who.isAuthenticated() || who.getName() == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        return who.getName();
    }

    /// Whether the caller holds `role`, named without its `ROLE_` prefix.
    public static boolean hasRole(String role) {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (who == null || who.getAuthorities() == null) {
            return false;
        }
        String wanted = "ROLE_" + role;
        Iterator<? extends GrantedAuthority> it = who.getAuthorities().iterator();
        while (it.hasNext()) {
            if (wanted.equals(it.next().getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
