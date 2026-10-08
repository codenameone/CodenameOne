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
package com.codename1.impl.backend.test;

import com.codename1.backend.security.AnonymousAuthenticationToken;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.UsernamePasswordAuthenticationToken;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.test.TestSecurityContextHolder;
import java.util.ArrayList;
import java.util.List;

/// What `@WithMockUser` and `@WithAnonymousUser` do around a test. The build
/// reads the annotations and writes down, for each test, the user they
/// describe: the JUnit extension asks the generated context for it by method
/// name, and a compiled runner carries the same values as a literal -- so a
/// compiled run that uses neither annotation links none of the security layer.
public final class TestSecurity {
    /// A [#apply] argument's first element for a signed-in user: then the name,
    /// the password, and the authorities.
    public static final String USER = "user";
    /// A [#apply] argument's only element for nobody.
    public static final String ANONYMOUS = "anonymous";

    private TestSecurity() {
    }

    /// Runs the calling thread's test as who `who` describes.
    public static void apply(String[] who) {
        if (who == null || who.length == 0) {
            return;
        }
        Authentication authentication;
        if (ANONYMOUS.equals(who[0])) {
            List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
            authentication = new AnonymousAuthenticationToken("key", "anonymous", authorities);
        } else {
            List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
            for (int iter = 3 ; iter < who.length ; iter++) {
                authorities.add(new SimpleGrantedAuthority(who[iter]));
            }
            authentication = UsernamePasswordAuthenticationToken.authenticated(
                    new User(who[1], who[2], authorities), who[2], authorities);
        }
        TestSecurityContextHolder.setAuthentication(authentication);
    }

    /// Ends what [#apply] started.
    public static void clear() {
        TestSecurityContextHolder.clearContext();
    }
}
