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

import com.codename1.backend.DataSource;
import com.codename1.backend.Database;
import com.codename1.backend.security.AccessDeniedException;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecuritySchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// Users kept in the server's database, in the `cn1_users` and
/// `cn1_authorities` tables of [SecuritySchema].
///
/// ```java
/// @Bean
/// UserDetailsService users(DataSource dataSource) {
///     return new JdbcUserDetailsManager(dataSource);
/// }
/// ```
///
/// A user is found whatever the case of the name it is asked for by, and keeps
/// the spelling it was created with; two names that differ only in the case of
/// `A` to `Z` are one user. A password is stored as it is given: encode it
/// first, as for any user store.
///
/// Every change to a user and its authorities is one transaction, and joins
/// the caller's when there is one.
public class JdbcUserDetailsManager implements UserDetailsManager, UserDetailsPasswordService {
    private final DataSource dataSource;

    public JdbcUserDetailsManager(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    private static Integer flag(boolean value) {
        return Integer.valueOf(value ? 1 : 0);
    }

    private static boolean truth(Object value) {
        return value instanceof Number && ((Number) value).longValue() != 0;
    }

    private static RuntimeException failed(String what, Exception err) {
        if (err instanceof RuntimeException) {
            return (RuntimeException) err;
        }
        return new AuthenticationServiceException("Could not " + what + ": " + err.getMessage(),
                err);
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        if (username == null) {
            throw new UsernameNotFoundException("null");
        }
        String key = SecuritySchema.usernameKey(username);
        Map row;
        List authorities;
        try {
            row = dataSource.queryOne("SELECT username, password, enabled, account_non_expired, "
                    + "account_non_locked, credentials_non_expired FROM cn1_users WHERE "
                    + "username_key = ?", new Object[] {key});
            if (row == null) {
                throw new UsernameNotFoundException(username);
            }
            authorities = dataSource.query("SELECT authority FROM cn1_authorities WHERE "
                    + "username_key = ?", new Object[] {key});
        } catch (java.io.IOException err) {
            throw failed("load the user", err);
        }
        List<String> granted = new ArrayList<String>();
        for (Object each : authorities) {
            granted.add(String.valueOf(((Map) each).get("authority")));
        }
        // Ordered here rather than by the database, whose idea of text order
        // follows its locale: the same user reads the same on every engine.
        java.util.Collections.sort(granted);
        return User.withUsername(String.valueOf(row.get("username")))
                .password(String.valueOf(row.get("password")))
                .authorities(granted.toArray(new String[granted.size()]))
                .disabled(!truth(row.get("enabled")))
                .accountExpired(!truth(row.get("account_non_expired")))
                .accountLocked(!truth(row.get("account_non_locked")))
                .credentialsExpired(!truth(row.get("credentials_non_expired"))).build();
    }

    private static void insertAuthorities(Database db, String key, UserDetails user)
            throws java.io.IOException {
        List<String> seen = new ArrayList<String>();
        for (GrantedAuthority authority : user.getAuthorities()) {
            String name = authority.getAuthority();
            if (name != null && !seen.contains(name)) {
                seen.add(name);
                db.execute("INSERT INTO cn1_authorities (username_key, authority) VALUES (?, ?)",
                        new Object[] {key, name});
            }
        }
    }

    @Override
    public void createUser(UserDetails user) {
        try {
            dataSource.inTransaction(new Change(Change.CREATE, user,
                    SecuritySchema.usernameKey(user.getUsername())));
        } catch (Exception err) {
            throw failed("create the user", err);
        }
    }

    @Override
    public void updateUser(UserDetails user) {
        try {
            dataSource.inTransaction(new Change(Change.UPDATE, user,
                    SecuritySchema.usernameKey(user.getUsername())));
        } catch (Exception err) {
            throw failed("update the user", err);
        }
    }

    @Override
    public void deleteUser(String username) {
        try {
            dataSource.inTransaction(new Change(Change.DELETE, null,
                    SecuritySchema.usernameKey(username)));
        } catch (Exception err) {
            throw failed("delete the user", err);
        }
    }

    /// One change to a user and its authorities, as the body of a transaction.
    private static final class Change implements DataSource.Work {
        static final int CREATE = 0;
        static final int UPDATE = 1;
        static final int DELETE = 2;
        private final int kind;
        private final UserDetails user;
        private final String key;

        Change(int kind, UserDetails user, String key) {
            this.kind = kind;
            this.user = user;
            this.key = key;
        }

        @Override
        public Object run(Database db) throws Exception {
            if (kind == CREATE) {
                if (db.queryOne("SELECT username_key FROM cn1_users WHERE username_key = ?",
                        new Object[] {key}) != null) {
                    throw new IllegalArgumentException("user should not exist");
                }
                db.execute("INSERT INTO cn1_users (username_key, username, password, enabled, "
                        + "account_non_expired, account_non_locked, credentials_non_expired) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)", new Object[] {key, user.getUsername(),
                            user.getPassword(), flag(user.isEnabled()),
                            flag(user.isAccountNonExpired()), flag(user.isAccountNonLocked()),
                            flag(user.isCredentialsNonExpired())});
                insertAuthorities(db, key, user);
                return null;
            }
            if (kind == UPDATE) {
                int changed = db.execute("UPDATE cn1_users SET username = ?, password = ?, "
                        + "enabled = ?, account_non_expired = ?, account_non_locked = ?, "
                        + "credentials_non_expired = ? WHERE username_key = ?",
                        new Object[] {user.getUsername(), user.getPassword(),
                            flag(user.isEnabled()), flag(user.isAccountNonExpired()),
                            flag(user.isAccountNonLocked()),
                            flag(user.isCredentialsNonExpired()), key});
                if (changed != 1) {
                    throw new IllegalArgumentException("user should exist");
                }
                db.execute("DELETE FROM cn1_authorities WHERE username_key = ?",
                        new Object[] {key});
                insertAuthorities(db, key, user);
                return null;
            }
            deleteCredentials(db, key);
            db.execute("DELETE FROM cn1_authorities WHERE username_key = ?", new Object[] {key});
            db.execute("DELETE FROM cn1_users WHERE username_key = ?", new Object[] {key});
            return null;
        }

        /// These rows must disappear in the same transaction as the account. Otherwise
        /// reusing a username lets the old account's durable credentials sign in again.
        private static void deleteCredentials(Database db, String key) throws java.io.IOException {
            db.execute("DELETE FROM cn1_persistent_logins WHERE username_key = ?", new Object[] {key});
            db.execute("DELETE FROM cn1_mfa_totp WHERE username_key = ?", new Object[] {key});
            db.execute("DELETE FROM cn1_mfa_recovery_code WHERE username_key = ?", new Object[] {key});
            db.execute("DELETE FROM cn1_federated_identity WHERE username_key = ?", new Object[] {key});
            db.execute("DELETE FROM cn1_webauthn_credential WHERE user_id IN "
                    + "(SELECT user_id FROM cn1_webauthn_user WHERE username_key = ?)", new Object[] {key});
            db.execute("DELETE FROM cn1_webauthn_user WHERE username_key = ?", new Object[] {key});
            // These two stores retain the principal's original spelling rather than
            // username_key. Use the same ASCII-only fold as the user store, independent
            // of the database's locale and collation. Client-credentials grants name a
            // client rather than a user and must survive a user with the same name.
            for (Object row : db.query("SELECT key_hash, owner FROM cn1_api_key", null)) {
                Map values = (Map) row;
                if (key.equals(SecuritySchema.usernameKey((String) values.get("owner")))) {
                    db.execute("DELETE FROM cn1_api_key WHERE key_hash = ?",
                            new Object[] {values.get("key_hash")});
                }
            }
            for (Object row : db.query("SELECT id, principal_name FROM cn1_oauth2_authorization "
                    + "WHERE grant_type <> 'client_credentials'", null)) {
                Map values = (Map) row;
                if (key.equals(SecuritySchema.usernameKey((String) values.get("principal_name")))) {
                    Object[] id = {values.get("id")};
                    db.execute("DELETE FROM cn1_oauth2_token WHERE authorization_id = ?", id);
                    db.execute("DELETE FROM cn1_oauth2_authorization WHERE id = ?", id);
                }
            }
        }
    }

    @Override
    public boolean userExists(String username) {
        try {
            return dataSource.queryOne("SELECT username_key FROM cn1_users WHERE username_key = ?",
                    new Object[] {SecuritySchema.usernameKey(username)}) != null;
        } catch (java.io.IOException err) {
            throw failed("look the user up", err);
        }
    }

    /// Sets the signed-in user's password to `newPassword`, which is stored as
    /// given. `oldPassword` is not checked here: re-authenticate before calling.
    @Override
    public void changePassword(String oldPassword, String newPassword) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null) {
            throw new AccessDeniedException("Can't change password as no Authentication object "
                    + "found in context for current user.");
        }
        if (!setPassword(current.getName(), newPassword)) {
            throw new IllegalStateException("Current user doesn't exist in database.");
        }
    }

    private boolean setPassword(String username, String password) {
        try {
            return dataSource.execute("UPDATE cn1_users SET password = ? WHERE username_key = ?",
                    new Object[] {password, SecuritySchema.usernameKey(username)}) == 1;
        } catch (java.io.IOException err) {
            throw failed("store the password", err);
        }
    }

    @Override
    public UserDetails updatePassword(UserDetails user, String newPassword) {
        if (!setPassword(user.getUsername(), newPassword)) {
            return user;
        }
        return User.withUserDetails(user).password(newPassword).build();
    }
}
