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
package com.codename1.impl.backend.security;

import com.codename1.backend.Config;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.CsrfToken;
import com.codename1.backend.security.HttpSecurity;
import com.codename1.backend.security.SecurityContext;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.impl.backend.RequestSecurity;
import java.util.List;

/// The security layer's internals, for the code the build generates, the server
/// and the test support: the same arrangement as
/// [com.codename1.impl.backend.BackendAccess], and for the same reason.
/// `com.codename1.backend.security` is what an application is written against;
/// what builds an [HttpSecurity] for a bean method, or stands the layer up for
/// a server, is not part of that and lives behind this door. The implementation
/// is in that package and installs itself when [SecurityContextHolder] is
/// initialized.
///
/// Nothing here is a contract with applications.
public abstract class SecurityAccess {
    private static SecurityAccess instance;

    /// Called once, by the implementation.
    public static synchronized void install(SecurityAccess access) {
        if (instance != null) {
            throw new IllegalStateException("SecurityAccess is already installed");
        }
        instance = access;
    }

    private static synchronized SecurityAccess installed() {
        return instance;
    }

    /// The implementation, initializing the security package first if nothing
    /// has yet.
    public static SecurityAccess get() {
        SecurityAccess a = installed();
        if (a == null) {
            // A static call is what makes a class initialize, on the JVM and
            // under ParparVM alike. It has no other effect.
            SecurityContextHolder.createEmptyContext();
            a = installed();
            if (a == null) {
                throw new IllegalStateException("The security layer did not install its access");
            }
        }
        return a;
    }

    /// A new [HttpSecurity] for one `SecurityFilterChain` bean method.
    ///
    /// @param beans the application's beans the layer picks its collaborators
    /// from by type -- a user store, a password encoder, authentication
    /// providers; a null element is a conditional bean that is off
    public abstract HttpSecurity httpSecurity(Config config, Object[] beans);

    /// [#httpSecurity(Config, Object[])], with what the build knows about each bean and
    /// the layer cannot read off the object: its name, and whether it is `@Primary`.
    /// That is what lets a chain choose between two beans of one type the way an
    /// injection point would, and name them when it cannot.
    /// @param config the server's configuration
    /// @param beans the beans; a null element is a conditional bean that is off
    /// @param names the name of each bean, by position
    /// @param primary whether each bean is `@Primary`, by position
    public abstract HttpSecurity httpSecurity(Config config, Object[] beans, String[] names,
            boolean[] primary);

    /// The layer for a server's chains, in the order they are asked.
    public abstract RequestSecurity runtime(Config config, List chains, boolean tls);

    /// Who the calling thread's request is from, or null when nobody signed in.
    public abstract Authentication authentication();

    /// The authentication of the calling thread's request as its context holds
    /// it, the anonymous one included; null when the thread has no context.
    public abstract Authentication current();

    /// The principal of the calling thread's request, the anonymous one included.
    public abstract Object principal();

    /// The CSRF token of `request`, or null when its chain has none.
    public abstract CsrfToken csrfToken(HttpServer.Request request);

    /// The context a test put in place for this thread's requests, or null.
    public abstract SecurityContext testContext();

    /// Puts a context in place for the requests a test sends from this thread;
    /// null removes it.
    public abstract void testContext(SecurityContext context);

    /// Makes this thread's next requests expect a CSRF token, and returns the
    /// value to send: the expected one when `valid`, another otherwise.
    public abstract String testCsrf(boolean valid);

    /// Undoes [#testCsrf].
    public abstract void clearTestCsrf();
}
