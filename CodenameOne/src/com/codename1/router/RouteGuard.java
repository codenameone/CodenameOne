/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.router;

/// Runs before a route's builder. Can permit, redirect, or block a navigation.
///
/// Guards are evaluated in registration order. The first guard to return a
/// non-`PROCEED` decision short-circuits.
///
/// #### Example: redirect to login if unauthenticated
///
/// ```java
/// Router.getInstance().guard("/account/**", new RouteGuard() {
///     public Decision check(RouteContext ctx) {
///         if (!UserSession.isLoggedIn()) return Decision.redirect("/login");
///         return Decision.PROCEED;
///     }
/// });
/// ```
///
/// #### Since 8.0
public interface RouteGuard {

    /// Guard decision returned by `RouteGuard#check`.
    final class Decision {
        /// Allow the navigation to proceed to the route builder.
        public static final Decision PROCEED = new Decision(Kind.PROCEED, null);

        /// Block the navigation entirely without showing anything new.
        public static final Decision BLOCK = new Decision(Kind.BLOCK, null);

        private final Kind kind;
        private final String redirectTo;

        private Decision(Kind k, String to) {
            this.kind = k; this.redirectTo = to;
        }

        /// Redirect the navigation to a different in-app path.
        public static Decision redirect(String path) {
            return new Decision(Kind.REDIRECT, path);
        }

        public Kind getKind() {
            return kind;
        }
        public String getRedirectTo() {
            return redirectTo;
        }

        public enum Kind { PROCEED, BLOCK, REDIRECT }
    }

    /// Inspect the context and decide what to do.
    Decision check(RouteContext ctx);
}
