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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Where a bean stands among the beans of its type: the lower the value, the
/// earlier it comes in a `List` an injection point receives, and the earlier a
/// `SecurityFilterChain` is asked whether a request is its own.
///
/// ```java
/// @Bean
/// @Order(1)
/// SecurityFilterChain api(HttpSecurity http) { ... }
///
/// @Bean
/// @Order(2)
/// SecurityFilterChain pages(HttpSecurity http) { ... }
/// ```
///
/// A bean without one comes last, in the order the build found it. Beans with
/// the same value keep that order among themselves. The order says nothing about
/// which bean a single injection point receives -- that is `@Primary` and
/// `@Qualifier` -- or about the order beans are constructed in.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Order {
    /// The position: [#HIGHEST_PRECEDENCE] comes first, [#LOWEST_PRECEDENCE] last.
    int value() default LOWEST_PRECEDENCE;

    /// The value that comes before every other.
    int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /// The value of a bean that carries no `@Order`.
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;
}
