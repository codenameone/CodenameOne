/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A cross-origin policy for one controller or one handler, as Spring's
/// `@CrossOrigin` is. On a class it covers every route of the controller; on a
/// method it replaces the class's for that route.
///
/// The route's policy replaces the global `cn1.cors.*` policy for requests to
/// that route -- the preflight, the reject of an origin it does not list and the
/// response headers alike -- rather than being merged with it, so what a route
/// allows is read off its annotation alone. A preflight for such a route is
/// answered before the security chain, as the global policy's is, because a
/// browser sends no credentials with one.
///
/// ```java
/// @CrossOrigin(origins = "https://app.example.com", allowCredentials = true)
/// @GetMapping("/api/me")
/// public Profile me(@AuthenticationPrincipal User user) { ... }
/// ```
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface CrossOrigin {
    /// The origins allowed, each `scheme://host[:port]`, or `*` for any.
    String[] origins() default {"*"};
    /// The same as [#origins], as Spring spells it on its `value` attribute.
    String[] value() default {};
    /// The methods a preflight may ask for; empty allows the route's own.
    String[] methods() default {};
    /// The request headers a preflight may ask for; `*` allows any.
    String[] allowedHeaders() default {"*"};
    /// Response headers a script may read beyond the safelisted ones.
    String[] exposedHeaders() default {};
    /// Whether the browser may send cookies and read the response with them.
    /// Needs [#origins] to list the origins: a browser refuses `*` with credentials.
    boolean allowCredentials() default false;
    /// How long, in seconds, a browser may cache the preflight answer.
    int maxAge() default 1800;
}
