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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Runs a test, or every test of a class, as a signed-in user who exists nowhere
/// but in the test: no user store is asked and no password checked.
///
/// ```java
/// @Test
/// @WithMockUser(username = "ada", roles = "ADMIN")
/// void anAdminSeesTheReport() throws Exception {
///     mvc.perform(get("/admin/report")).andExpect(status().isOk());
/// }
/// ```
///
/// Every request the test sends through [MockMvc] is from that user, whatever
/// session or credentials it carries, and
/// `SecurityContextHolder.getContext()` on the test's own thread names them too.
/// Requests sent over a socket with [TestRestTemplate] are not affected: they
/// are served on the server's threads and authenticate as a real client does.
///
/// On a method it replaces what the class says. The principal is a
/// [com.codename1.backend.security.core.userdetails.User].
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface WithMockUser {
    /// The user's name; the same as [#username], which wins when both are set.
    String value() default "user";

    /// The user's name; `user` unless set here or in [#value].
    String username() default "";

    /// The user's roles, each granted as `ROLE_` and the name. Ignored when
    /// [#authorities] is given.
    String[] roles() default {"USER"};

    /// The user's authorities, as they are written, in place of [#roles].
    String[] authorities() default {};

    /// The user's password, for code that reads it.
    String password() default "password";
}
