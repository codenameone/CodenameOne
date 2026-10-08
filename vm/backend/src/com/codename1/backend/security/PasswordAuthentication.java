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
package com.codename1.backend.security;

import com.codename1.backend.Config;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetailsPasswordService;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import java.util.ArrayList;
import java.util.List;

/// Builds what a chain checks passwords with: its [AuthenticationManager], from
/// the application's user store, password encoder and providers.
///
/// Apart from [HttpSecurity] on purpose. Only the parts of a chain that take a
/// password -- the form login, HTTP Basic -- call this, so a server whose
/// chains take none has no user store, no DAO provider and no password hashing
/// in it.
final class PasswordAuthentication {
    private PasswordAuthentication() {
    }

    /// The manager a sign-in filter of `http` authenticates with.
    static AuthenticationManager require(HttpSecurity http, String what) {
        AuthenticationManager manager = resolve(http);
        if (manager == null) {
            throw new IllegalStateException(what + " needs something to check credentials "
                    + "against, and this application has none. Declare a UserDetailsService "
                    + "bean, an AuthenticationProvider bean or an AuthenticationManager bean, "
                    + "call userDetailsService(...) on the HttpSecurity, or set "
                    + HttpSecurity.USER_PASSWORD + ".");
        }
        return manager;
    }

    /// The users a part of `http` that needs them by name looks up: the chain's
    /// own, the application's unique or primary [UserDetailsService] bean, or
    /// the user the configuration describes. Ambiguous beans are refused.
    static UserDetailsService users(HttpSecurity http) {
        UserDetailsService users = http.chosenUserDetailsService();
        if (users == null) {
            users = http.uniqueSharedObject(UserDetailsService.class,
                    "Password authentication", "userDetailsService(...)");
        }
        if (users == null) {
            users = configuredUser(http.getConfig());
        }
        return users;
    }

    private static AuthenticationManager resolve(HttpSecurity http) {
        if (http.authenticationManagerResolved()) {
            return http.resolvedAuthenticationManager();
        }
        AuthenticationManager manager = build(http);
        http.resolvedAuthenticationManager(manager);
        return manager;
    }

    private static AuthenticationManager build(HttpSecurity http) {
        if (http.explicitAuthenticationManager() != null) {
            return http.explicitAuthenticationManager();
        }
        Object[] beans = http.beans();
        List<AuthenticationProvider> all = new ArrayList<AuthenticationProvider>(http.providers());
        for (Object bean : beans) {
            if (bean instanceof AuthenticationProvider && !all.contains(bean)) {
                all.add((AuthenticationProvider) bean);
            }
        }
        AuthenticationManager parent = null;
        int managers = 0;
        for (Object bean : beans) {
            if (bean instanceof AuthenticationManager) {
                parent = (AuthenticationManager) bean;
                managers++;
            }
        }
        if (managers != 1) {
            parent = null;
        }
        UserDetailsService users = http.chosenUserDetailsService();
        if (users == null && all.isEmpty() && parent == null) {
            // The application's unique or primary user store when it has no
            // provider of its own. Ambiguity must not enable a configured fallback.
            users = http.uniqueSharedObject(UserDetailsService.class,
                    "Password authentication", "userDetailsService(...)");
            if (users == null) {
                users = configuredUser(http.getConfig());
            }
        }
        if (users != null) {
            DaoAuthenticationProvider dao = new DaoAuthenticationProvider(users);
            dao.setMaxConcurrentPasswordChecks(maxConcurrentPasswordChecks(http.getConfig()));
            PasswordEncoder encoder = http.uniqueSharedObject(PasswordEncoder.class,
                    "Password authentication", "setSharedObject(PasswordEncoder.class, ...)");
            dao.setPasswordEncoder(encoder != null ? encoder
                    : PasswordEncoderFactories.createDelegatingPasswordEncoder());
            UserDetailsPasswordService passwords = http.getSharedObject(
                    UserDetailsPasswordService.class);
            if (passwords == null && users instanceof UserDetailsPasswordService) {
                passwords = (UserDetailsPasswordService) users;
            }
            dao.setUserDetailsPasswordService(passwords);
            all.add(dao);
        }
        return all.isEmpty() ? parent : new ProviderManager(all, parent);
    }

    private static int maxConcurrentPasswordChecks(Config config) {
        if (config == null) {
            return 0;
        }
        try {
            return config.getInt(HttpSecurity.PASSWORD_MAX_CONCURRENT, 0);
        } catch (java.io.IOException err) {
            throw new IllegalStateException(err.getMessage(), err);
        }
    }

    /// The one user `cn1.security.user.*` describes, or null.
    private static UserDetailsService configuredUser(Config config) {
        if (config == null) {
            return null;
        }
        String password;
        String name;
        String roles;
        try {
            password = config.get(HttpSecurity.USER_PASSWORD);
            name = config.get(HttpSecurity.USER_NAME, "user");
            roles = config.get(HttpSecurity.USER_ROLES, "");
        } catch (java.io.IOException err) {
            throw new IllegalStateException(err.getMessage(), err);
        }
        if (password == null || password.length() == 0) {
            return null;
        }
        if (!(password.startsWith("{") && password.indexOf('}') > 1)) {
            password = "{noop}" + password;
        }
        List<String> granted = new ArrayList<String>();
        int start = 0;
        while (start <= roles.length()) {
            int comma = roles.indexOf(',', start);
            int end = comma < 0 ? roles.length() : comma;
            String role = roles.substring(start, end).trim();
            if (role.length() > 0) {
                granted.add(role);
            }
            if (comma < 0) {
                break;
            }
            start = comma + 1;
        }
        return new InMemoryUserDetailsManager(User.withUsername(name).password(password)
                .roles(granted.toArray(new String[granted.size()])).build());
    }
}
