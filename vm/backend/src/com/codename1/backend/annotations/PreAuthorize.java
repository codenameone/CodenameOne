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

/// Lets this method -- or every public method of this class -- run only when an
/// expression about the caller holds.
///
/// ```java
/// @PreAuthorize("hasRole('ADMIN') or #owner == authentication.name")
/// public Report read(String owner) { ... }
///
/// @PreAuthorize("@documents.canEdit(authentication, #id)")
/// public void rename(long id, String title) { ... }
/// ```
///
/// A caller who has not signed in gets an
/// [com.codename1.backend.security.InsufficientAuthenticationException], which
/// a chain answers with its sign-in challenge; one who has signed in and is
/// refused gets an [com.codename1.backend.security.AccessDeniedException],
/// which is a 403.
///
/// The expression is compiled by the build into plain Java -- there is nothing
/// to interpret at run time -- and a mistake in it is a build error. What it
/// may contain:
///
/// - `hasRole('X')`, `hasAnyRole('X', 'Y')`, `hasAuthority('X')`,
///   `hasAnyAuthority('X', 'Y')`. A role is the authority `ROLE_X`.
/// - `isAuthenticated()`, `isAnonymous()`, `isFullyAuthenticated()` -- signed
///   in during this session rather than remembered from an earlier one --
///   `isRememberMe()`, `permitAll` and `denyAll`.
/// - `and`, `or`, `not`, also written `&&`, `||` and `!`, and parentheses.
/// - `authentication.name` and `principal.username`, compared with `==` or
///   `!=` to a string literal or to a `String` parameter written `#name`.
/// - A call on a bean, `@beanName.method(...)`, whose arguments are
///   `authentication`, `principal`, a parameter `#name`, or a string, whole
///   number or boolean literal. The method must be one the build can find,
///   visible to this class, and return `boolean`. The bean is a singleton of
///   the server the calling thread works for.
///
/// A parameter is named as the source names it, when the class was compiled
/// with debug information or `-parameters`, or by a [P] annotation on it.
///
/// Not supported, each refused by the build: `hasPermission`, `returnObject`,
/// `T(...)`, property chains such as `#dto.owner.id`, and the post-invocation
/// and filtering annotations of Spring Security. Put what those would do in a
/// bean method and call it.
///
/// An annotation on a method replaces the one on its class. The build rewrites
/// the method itself rather than wrapping the object in a proxy, so the check
/// runs however the method is called: from another bean, from `this`, on a
/// private method, or on an object built with `new`. In Spring a call through
/// `this` skips it. For an [Async] method the check runs on the caller's
/// thread, before the work is handed off.
///
/// A module that uses this needs a
/// [com.codename1.backend.security.SecurityFilterChain] bean: without one
/// nobody ever signs in, and the build refuses the annotation.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface PreAuthorize {
    /// The expression.
    String value();
}
