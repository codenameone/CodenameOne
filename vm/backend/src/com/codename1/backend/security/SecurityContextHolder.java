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

/// Who the calling thread's request is from.
///
/// ```java
/// Authentication who = SecurityContextHolder.getContext().getAuthentication();
/// ```
///
/// The context belongs to the thread serving a request, for the length of that
/// request: the chain that guards the request fills it in, and the server
/// clears it when the request ends. It does not follow work handed to another
/// thread -- an `@Async` method or a scheduled job starts with none.
public final class SecurityContextHolder {
    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<SecurityContext>();
    /// A context a test put in place for the requests it sends from this thread.
    private static final ThreadLocal<SecurityContext> TEST = new ThreadLocal<SecurityContext>();

    static {
        // The runtime's door into this package, for the generated wiring and the
        // server; see SecurityAccess.
        com.codename1.impl.backend.security.SecurityAccess.install(new Access());
    }

    private SecurityContextHolder() {
    }

    /// The thread's context; an empty one is made when there is none.
    public static SecurityContext getContext() {
        SecurityContext context = CONTEXT.get();
        if (context == null) {
            context = createEmptyContext();
            CONTEXT.set(context);
        }
        return context;
    }

    /// Replaces the thread's context.
    public static void setContext(SecurityContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Only non-null SecurityContext instances are "
                    + "permitted");
        }
        CONTEXT.set(context);
    }

    /// Forgets the thread's context.
    public static void clearContext() {
        CONTEXT.set(null);
    }

    /// A context with no authentication in it.
    public static SecurityContext createEmptyContext() {
        return new SecurityContextImpl();
    }

    /// The thread's context without making one.
    static SecurityContext peek() {
        return CONTEXT.get();
    }

    static SecurityContext testContext() {
        return TEST.get();
    }

    static void testContext(SecurityContext context) {
        TEST.set(context);
    }
}
