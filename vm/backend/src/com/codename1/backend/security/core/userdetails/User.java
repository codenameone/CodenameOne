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

import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.crypto.PasswordEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/// The [UserDetails] the runtime provides. Immutable but for its password,
/// which is dropped once it has been checked.
///
/// ```java
/// UserDetails admin = User.withUsername("ada")
///         .password("{pbkdf2-sha256}pbkdf2$210000$...")
///         .roles("ADMIN")
///         .build();
/// ```
///
/// Two users are equal when their names are.
public class User implements UserDetails {
    private String password;
    private final String username;
    private final List<GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    public User(String username, String password,
                Collection<? extends GrantedAuthority> authorities) {
        this(username, password, true, true, true, true, authorities);
    }

    public User(String username, String password, boolean enabled, boolean accountNonExpired,
                boolean credentialsNonExpired, boolean accountNonLocked,
                Collection<? extends GrantedAuthority> authorities) {
        if (username == null || username.length() == 0 || password == null) {
            throw new IllegalArgumentException("Cannot pass null or empty values to constructor");
        }
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.accountNonLocked = accountNonLocked;
        List<GrantedAuthority> copy = new ArrayList<GrantedAuthority>();
        if (authorities != null) {
            for (GrantedAuthority authority : authorities) {
                if (authority == null) {
                    throw new IllegalArgumentException("GrantedAuthority list cannot contain "
                            + "any null elements");
                }
                if (!copy.contains(authority)) {
                    copy.add(authority);
                }
            }
        }
        this.authorities = Collections.unmodifiableList(copy);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public synchronized String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    /// Drops the password, once it has been checked.
    public synchronized void eraseCredentials() {
        password = null;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof User && username.equals(((User) other).username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [Username=" + username + ", Password=[PROTECTED], "
                + "Enabled=" + enabled + ", AccountNonExpired=" + accountNonExpired
                + ", CredentialsNonExpired=" + credentialsNonExpired + ", AccountNonLocked="
                + accountNonLocked + ", Granted Authorities=" + authorities + "]";
    }

    /// A builder for a user of this name.
    public static UserBuilder withUsername(String username) {
        return builder().username(username);
    }

    /// A builder with nothing set.
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    /// A builder starting from an existing user.
    public static UserBuilder withUserDetails(UserDetails user) {
        return withUsername(user.getUsername()).password(user.getPassword())
                .accountExpired(!user.isAccountNonExpired())
                .accountLocked(!user.isAccountNonLocked())
                .authorities(user.getAuthorities())
                .credentialsExpired(!user.isCredentialsNonExpired())
                .disabled(!user.isEnabled());
    }

    /// Builds a [User].
    public static final class UserBuilder {
        private String username;
        private String password;
        private List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        private boolean accountExpired;
        private boolean accountLocked;
        private boolean credentialsExpired;
        private boolean disabled;
        private PasswordEncoder encoder;

        private UserBuilder() {
        }

        public UserBuilder username(String username) {
            if (username == null) {
                throw new IllegalArgumentException("username cannot be null");
            }
            this.username = username;
            return this;
        }

        /// The password as it is to be stored, unless [#passwordEncoder] says to
        /// encode it.
        public UserBuilder password(String password) {
            if (password == null) {
                throw new IllegalArgumentException("password cannot be null");
            }
            this.password = password;
            return this;
        }

        /// Encodes the password with `encoder` when the user is built.
        public UserBuilder passwordEncoder(PasswordEncoder encoder) {
            if (encoder == null) {
                throw new IllegalArgumentException("encoder cannot be null");
            }
            this.encoder = encoder;
            return this;
        }

        /// Grants roles: each name gets the `ROLE_` prefix, so `roles("ADMIN")` is
        /// `authorities("ROLE_ADMIN")`. Replaces what was granted before.
        public UserBuilder roles(String... roles) {
            List<GrantedAuthority> granted = new ArrayList<GrantedAuthority>(roles.length);
            for (String role : roles) {
                if (role.startsWith("ROLE_")) {
                    throw new IllegalArgumentException(role + " cannot start with ROLE_ (it is "
                            + "automatically added)");
                }
                granted.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            this.authorities = granted;
            return this;
        }

        /// Grants authorities by name. Replaces what was granted before.
        public UserBuilder authorities(String... authorities) {
            List<GrantedAuthority> granted = new ArrayList<GrantedAuthority>(authorities.length);
            for (String authority : authorities) {
                granted.add(new SimpleGrantedAuthority(authority));
            }
            this.authorities = granted;
            return this;
        }

        /// Grants authorities. Replaces what was granted before.
        public UserBuilder authorities(Collection<? extends GrantedAuthority> authorities) {
            this.authorities = new ArrayList<GrantedAuthority>(authorities);
            return this;
        }

        public UserBuilder accountExpired(boolean accountExpired) {
            this.accountExpired = accountExpired;
            return this;
        }

        public UserBuilder accountLocked(boolean accountLocked) {
            this.accountLocked = accountLocked;
            return this;
        }

        public UserBuilder credentialsExpired(boolean credentialsExpired) {
            this.credentialsExpired = credentialsExpired;
            return this;
        }

        public UserBuilder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public UserDetails build() {
            if (username == null || password == null) {
                throw new IllegalArgumentException("A user needs a username and a password");
            }
            String stored = encoder == null ? password : encoder.encode(password);
            return new User(username, stored, !disabled, !accountExpired, !credentialsExpired,
                    !accountLocked, authorities);
        }
    }
}
