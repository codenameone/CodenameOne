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

import com.codename1.backend.Base64;
import com.codename1.backend.security.AnonymousAuthenticationToken;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SecurityContext;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecurityContextImpl;
import com.codename1.backend.security.SimpleGrantedAuthority;
import com.codename1.backend.security.UsernamePasswordAuthenticationToken;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.impl.backend.security.SecurityAccess;
import java.util.ArrayList;
import java.util.List;

/// What a test says about one request's security, for static import:
///
/// ```java
/// mvc.perform(post("/notes").with(user("ada").roles("EDITOR")).with(csrf())
///         .content("{}"));
/// mvc.perform(get("/api/orders").with(httpBasic("svc", "secret")));
/// ```
///
/// [#user] and [#authentication] say who the request is from, without asking a
/// user store; [#csrf] gives a state-changing request the token its chain
/// demands; [#httpBasic] sends real credentials for the chain to check.
public final class SecurityMockMvcRequestPostProcessors {
    private SecurityMockMvcRequestPostProcessors() {
    }

    /// The request is from a user of this name, with the role `USER` unless
    /// told otherwise.
    public static UserRequestPostProcessor user(String username) {
        return new UserRequestPostProcessor(username);
    }

    /// The request is from this user.
    public static RequestPostProcessor user(UserDetails user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(user,
                user.getPassword(), user.getAuthorities()));
    }

    /// The request is from this authentication.
    public static RequestPostProcessor authentication(final Authentication authentication) {
        return new RequestPostProcessor() {
            @Override
            public MockRequestBuilder postProcessRequest(MockRequestBuilder request) {
                as(request, authentication);
                return request;
            }
        };
    }

    /// The request is from nobody, whoever the test runs as.
    public static RequestPostProcessor anonymous() {
        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ANONYMOUS"));
        return authentication(new AnonymousAuthenticationToken("key", "anonymous", authorities));
    }

    /// The request carries a valid CSRF token, in the `_csrf` parameter.
    public static CsrfRequestPostProcessor csrf() {
        return new CsrfRequestPostProcessor();
    }

    /// The request carries these credentials in an `Authorization: Basic` header.
    public static RequestPostProcessor httpBasic(final String username, final String password) {
        return new RequestPostProcessor() {
            @Override
            public MockRequestBuilder postProcessRequest(MockRequestBuilder request) {
                request.replaceHeader("Authorization", "Basic "
                        + Base64.encode(MockRequestBuilder.utf8(username + ":" + password)));
                return request;
            }
        };
    }

    /// Makes `request` one from `authentication`, for as long as it is being
    /// sent: what the thread ran as before is put back afterwards.
    private static void as(MockRequestBuilder request, final Authentication authentication) {
        final SecurityContext[] previous = new SecurityContext[1];
        request.around(new Runnable() {
            @Override
            public void run() {
                previous[0] = SecurityAccess.get().testContext();
                SecurityAccess.get().testContext(new SecurityContextImpl(authentication));
            }
        }, new Runnable() {
            @Override
            public void run() {
                SecurityAccess.get().testContext(previous[0]);
                if (previous[0] == null) {
                    SecurityContextHolder.clearContext();
                } else {
                    SecurityContextHolder.setContext(
                            new SecurityContextImpl(previous[0].getAuthentication()));
                }
            }
        });
    }

    /// [SecurityMockMvcRequestPostProcessors#user(String)], to refine.
    public static final class UserRequestPostProcessor implements RequestPostProcessor {
        private final String username;
        private String password = "password";
        private List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        private UserRequestPostProcessor(String username) {
            if (username == null || username.length() == 0) {
                throw new IllegalArgumentException("username cannot be empty");
            }
            this.username = username;
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        /// The user's roles, each granted as `ROLE_` and the name, in place of
        /// what was granted before.
        public UserRequestPostProcessor roles(String... roles) {
            List<GrantedAuthority> granted = new ArrayList<GrantedAuthority>();
            for (String role : roles) {
                if (role.startsWith("ROLE_")) {
                    throw new IllegalArgumentException("Role should not start with ROLE_ since "
                            + "this method automatically prefixes with this value. Got " + role);
                }
                granted.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            this.authorities = granted;
            return this;
        }

        /// The user's authorities, as they are written, in place of what was
        /// granted before.
        public UserRequestPostProcessor authorities(String... authorities) {
            List<GrantedAuthority> granted = new ArrayList<GrantedAuthority>();
            for (String authority : authorities) {
                granted.add(new SimpleGrantedAuthority(authority));
            }
            this.authorities = granted;
            return this;
        }

        public UserRequestPostProcessor password(String password) {
            this.password = password;
            return this;
        }

        @Override
        public MockRequestBuilder postProcessRequest(MockRequestBuilder request) {
            User user = new User(username, password, authorities);
            as(request, UsernamePasswordAuthenticationToken.authenticated(user, password,
                    authorities));
            return request;
        }
    }

    /// [SecurityMockMvcRequestPostProcessors#csrf()], to refine.
    ///
    /// The chain is told what token to expect for this one request, so no page
    /// has to be fetched first. The names are the defaults -- the `_csrf`
    /// parameter and the `X-CSRF-TOKEN` header -- whichever repository the chain
    /// uses.
    public static final class CsrfRequestPostProcessor implements RequestPostProcessor {
        private boolean asHeader;
        private boolean invalid;

        private CsrfRequestPostProcessor() {
        }

        /// Sends the token in the `X-CSRF-TOKEN` header instead of a parameter.
        public CsrfRequestPostProcessor asHeader() {
            this.asHeader = true;
            return this;
        }

        /// Sends a token that is not the expected one, to see it refused.
        public CsrfRequestPostProcessor useInvalidToken() {
            this.invalid = true;
            return this;
        }

        @Override
        public MockRequestBuilder postProcessRequest(MockRequestBuilder request) {
            request.around(new Arm(request, asHeader, invalid), new Disarm());
            return request;
        }

        /// Tells the chain what token to expect and puts it on the request.
        private static final class Arm implements Runnable {
            private final MockRequestBuilder request;
            private final boolean header;
            private final boolean wrong;

            Arm(MockRequestBuilder request, boolean header, boolean wrong) {
                this.request = request;
                this.header = header;
                this.wrong = wrong;
            }

            @Override
            public void run() {
                String token = SecurityAccess.get().testCsrf(!wrong);
                if (header) {
                    request.replaceHeader("X-CSRF-TOKEN", token);
                } else {
                    request.replaceParam("_csrf", token);
                }
            }
        }

        /// Ends the expectation once the request has been sent.
        private static final class Disarm implements Runnable {
            @Override
            public void run() {
                SecurityAccess.get().clearTestCsrf();
            }
        }
    }
}
