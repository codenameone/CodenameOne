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
package com.codename1.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Binds a `Form` class -- or a static method that returns a `Form` -- to a
/// URL path so the framework can show it in response to a deep link.
///
/// `@Route` is the only annotation an application needs in order to make a
/// form reachable from a Universal Link, an Android App Link, a custom-scheme
/// URL, a push-notification payload, or any other URL the platform delivers
/// to the app. Path variables flow into constructor or method parameters
/// through `RouteParam`.
///
/// ```java
/// @Route("/users/:id")
/// public class ProfileForm extends Form {
///     public ProfileForm(@RouteParam("id") String id) { ... }
/// }
///
/// public class Routes {
///     @Route("/home")
///     public static Form home() {
///         return new HomeForm();
///     }
///
///     @Route("/users/:id")
///     public static Form profile(@RouteParam("id") String id) {
///         return new ProfileForm(id);
///     }
/// }
/// ```
///
/// **At build time** the Codename One Maven plugin scans the project's
/// compiled bytecode, validates every `@Route` (extends `Form`, accessible
/// constructor or static factory, no duplicate patterns, every parameter
/// bound), then generates an internal dispatch class that the framework wires
/// to the platform's deep-link plumbing under the hood. There is no
/// reflection at runtime and no router API for the application to call --
/// `new MyForm().show()` is still the way to navigate inside the app; URL
/// routing only handles links coming from outside.
///
/// #### Path syntax
///
/// - **Literal segments** -- `/about`
/// - **Named parameters** -- `/users/:id`, bound via `@RouteParam("id")`
/// - **Single-segment wildcard** -- `/files/*`
/// - **Catch-all wildcard** -- `/files/**`
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Route {

    /// The path pattern. Always starts with `/`. Required.
    String value();

    /// Container annotation for binding several path patterns to the same
    /// target. Use it when a single Form should be reachable from multiple
    /// URLs without repeating the body.
    @Retention(RetentionPolicy.CLASS)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @interface Routes {
        Route[] value();
    }
}
