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
package com.codename1.backend.test;

import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.SecurityContext;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecurityContextImpl;
import com.codename1.impl.backend.security.SecurityAccess;

/// Who the calling thread's tests are running as. [WithMockUser] sets it for a
/// test; a test that needs to change it part-way does so here.
///
/// ```java
/// TestSecurityContextHolder.setAuthentication(
///         UsernamePasswordAuthenticationToken.authenticated("ada", null, authorities));
/// ```
///
/// While one is set, every request the thread sends through [MockMvc] is from
/// that authentication, and `SecurityContextHolder` on the thread holds it.
public final class TestSecurityContextHolder {
    private TestSecurityContextHolder() {
    }

    /// The context in place, or an empty one.
    public static SecurityContext getContext() {
        SecurityContext context = SecurityAccess.get().testContext();
        return context == null ? SecurityContextHolder.createEmptyContext() : context;
    }

    /// Puts `context` in place for this thread's tests.
    public static void setContext(SecurityContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        SecurityAccess.get().testContext(context);
        SecurityContextHolder.setContext(new SecurityContextImpl(context.getAuthentication()));
    }

    /// Puts `authentication` in place for this thread's tests.
    public static void setAuthentication(Authentication authentication) {
        setContext(new SecurityContextImpl(authentication));
    }

    /// Removes what was put in place: the thread's tests run as nobody again.
    public static void clearContext() {
        SecurityAccess.get().testContext(null);
        SecurityContextHolder.clearContext();
    }
}
