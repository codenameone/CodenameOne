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
package com.codename1.backend.security.core.userdetails;

import com.codename1.backend.security.AccessDeniedException;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.SecurityContextHolder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/// Users kept in memory: for a demonstration, a test, or a server whose handful
/// of accounts is part of its configuration. Names are compared without regard
/// to ASCII case, and every lookup returns a copy, so erasing the credentials of
/// a signed-in user does not erase the stored ones.
///
/// ```java
/// @Bean
/// UserDetailsService users() {
///     return new InMemoryUserDetailsManager(
///             User.withUsername("ada").password("{noop}secret").roles("USER").build());
/// }
/// ```
///
/// The accounts live in this process only and are gone when it stops.
public class InMemoryUserDetailsManager implements UserDetailsManager, UserDetailsPasswordService {
    private final Map<String, UserDetails> users = new HashMap<String, UserDetails>();

    public InMemoryUserDetailsManager() {
    }

    public InMemoryUserDetailsManager(UserDetails... users) {
        for (UserDetails user : users) {
            createUser(user);
        }
    }

    public InMemoryUserDetailsManager(Collection<UserDetails> users) {
        for (UserDetails user : users) {
            createUser(user);
        }
    }

    @Override
    public synchronized void createUser(UserDetails user) {
        if (userExists(user.getUsername())) {
            throw new IllegalArgumentException("user should not exist");
        }
        users.put(key(user.getUsername()), User.withUserDetails(user).build());
    }

    @Override
    public synchronized void updateUser(UserDetails user) {
        if (!userExists(user.getUsername())) {
            throw new IllegalArgumentException("user should exist");
        }
        users.put(key(user.getUsername()), User.withUserDetails(user).build());
    }

    @Override
    public synchronized void deleteUser(String username) {
        users.remove(key(username));
    }

    @Override
    public synchronized boolean userExists(String username) {
        return users.containsKey(key(username));
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null) {
            throw new AccessDeniedException("Can't change password as no Authentication object "
                    + "found in context for current user.");
        }
        synchronized (this) {
            UserDetails user = users.get(key(current.getName()));
            if (user == null) {
                throw new IllegalStateException("Current user doesn't exist in database.");
            }
            users.put(key(user.getUsername()),
                    User.withUserDetails(user).password(newPassword).build());
        }
    }

    @Override
    public synchronized UserDetails updatePassword(UserDetails user, String newPassword) {
        UserDetails stored = users.get(key(user.getUsername()));
        if (stored == null) {
            return user;
        }
        UserDetails updated = User.withUserDetails(stored).password(newPassword).build();
        users.put(key(user.getUsername()), updated);
        return User.withUserDetails(updated).build();
    }

    @Override
    public synchronized UserDetails loadUserByUsername(String username) {
        UserDetails user = username == null ? null : users.get(key(username));
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return User.withUserDetails(user).build();
    }

    private static String key(String username) {
        return asciiLower(username);
    }

    /// Folded by hand: String.toLowerCase follows the device's locale, and a
    /// Turkish one maps "I" to a dotless i, so "ADMIN" and "admin" would be two
    /// accounts on one server and one on another.
    private static String asciiLower(String value) {
        char[] chars = value.toCharArray();
        for (int iter = 0 ; iter < chars.length ; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }
}
