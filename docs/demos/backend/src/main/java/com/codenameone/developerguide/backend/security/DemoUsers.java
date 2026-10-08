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
package com.codenameone.developerguide.backend.security;

import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.annotations.Profile;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetailsManager;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.crypto.PasswordEncoder;

/** The Backend security chapter's user store examples. */
@Configuration
@Profile("dev")
public class DemoUsers {
// tag::backend-security-users-memory[]
    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}secret").roles("ADMIN").build(),
                User.withUsername("grace").password("{noop}secret").roles("USER").build());
    }
// end::backend-security-users-memory[]

    public static void register(UserDetailsManager users, PasswordEncoder encoder,
                                String name, String password) {
// tag::backend-security-users-create[]
users.createUser(User.withUsername(name)
        .password(encoder.encode(password))
        .roles("USER")
        .build());
// end::backend-security-users-create[]
    }
}
