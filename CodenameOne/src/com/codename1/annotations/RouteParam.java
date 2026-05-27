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

/// Binds a constructor or static-factory parameter to a path variable or query
/// parameter from an incoming deep link.
///
/// Used together with `Route`. The build-time route processor inspects each
/// annotated parameter and generates dispatch code that pulls the value out of
/// the matched URL before invoking the constructor or factory.
///
/// ```java
/// @Route("/users/:id")
/// public class ProfileForm extends Form {
///     public ProfileForm(@RouteParam("id") String id) { ... }
/// }
///
/// @Route("/search")
/// public static Form search(@RouteParam("q") String query,
///                           @RouteParam(value = "page", required = false) String page) { ... }
/// ```
///
/// The `value` is matched first against named path variables (`:name`) and then
/// against query-string keys. The annotation is required on every parameter the
/// framework should bind; unannotated parameters are an error at build time.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface RouteParam {

    /// The name of the path variable or query parameter to bind. Required.
    String value();

    /// When true (the default) the build fails if the deep link cannot supply a
    /// value. When false a missing value is passed in as null.
    boolean required() default true;
}
