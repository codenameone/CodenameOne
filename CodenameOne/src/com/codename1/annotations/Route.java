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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares a `com.codename1.router.Router` route on a `Form` class.
///
/// At build time the Codename One Maven plugin scans `.class` files for `@Route`
/// annotations and generates a single `RoutesIndex` class that registers every
/// annotated form with the Router. App startup calls `RoutesIndex.register()`
/// once, before showing the first form:
///
/// ```java
/// @Route("/profile/:id")
/// public class ProfileForm extends Form {
///     public ProfileForm() { setTitle("Profile"); /* ... */ }
///
///     // Optional: builder-aware constructor. The generated RoutesIndex
///     // prefers this constructor over the no-arg one when both exist.
///     public ProfileForm(RouteContext ctx) {
///         this();
///         setTitle("Profile of " + ctx.param("id"));
///     }
/// }
/// ```
///
/// `@Route` is a build-time hint only — there is no reflection at runtime. Pure
/// Java code generation keeps the contract portable across iOS (ParparVM),
/// Android, JavaSE, and the JavaScript port without changes.
///
/// Multiple paths can be assigned to a single Form by stacking annotations using
/// `@Route.Routes` or by repeating the annotation when the project targets a
/// language version that supports `@Repeatable`.
///
/// #### Path syntax
///
/// - **Literal segments** — `/about`
/// - **Named parameters** — `/users/:id`, accessible as `ctx.param("id")`
/// - **Single-segment wildcard** — `/files/*`
/// - **Catch-all wildcard** — `/files/**`
///
/// #### Since 8.0
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Route {

    /// The route pattern (always starts with `/`). Required.
    String value();

    /// Optional name used by reverse-routing utilities (`Router.named("home")`).
    /// Defaults to the empty string, which means "unnamed".
    String name() default "";

    /// Container annotation for multiple routes on the same class. Pre-Java-8
    /// classes can express `@Route.Routes({@Route("/a"), @Route("/b")})` until
    /// the surrounding project moves to a JDK that supports `@Repeatable`.
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface Routes {
        Route[] value();
    }
}
