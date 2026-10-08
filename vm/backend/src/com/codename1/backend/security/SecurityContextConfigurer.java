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

/// Where a chain keeps who is signed in between requests.
public final class SecurityContextConfigurer extends SecurityConfigurer {
    private SecurityContextRepository repository;

    SecurityContextConfigurer() {
    }

    /// The repository; the HTTP session unless set, and none for a chain with
    /// [SessionCreationPolicy#STATELESS].
    public SecurityContextConfigurer securityContextRepository(
            SecurityContextRepository securityContextRepository) {
        this.repository = securityContextRepository;
        return this;
    }

    SecurityContextRepository resolve(SessionCreationPolicy policy,
                                      java.util.List<AuthenticationCodec> codecs) {
        SecurityContextRepository resolved = resolve(policy);
        // The application's own repository is given them too, when it is the
        // session one or a subclass of it.
        if (resolved instanceof HttpSessionSecurityContextRepository) {
            for (AuthenticationCodec codec : codecs) {
                ((HttpSessionSecurityContextRepository) resolved).addAuthenticationCodec(codec);
            }
        }
        return resolved;
    }

    private SecurityContextRepository resolve(SessionCreationPolicy policy) {
        if (repository != null) {
            return repository;
        }
        if (policy == SessionCreationPolicy.STATELESS) {
            repository = new NullSecurityContextRepository();
        } else {
            HttpSessionSecurityContextRepository session = new HttpSessionSecurityContextRepository();
            session.setAllowSessionCreation(policy != SessionCreationPolicy.NEVER);
            repository = session;
        }
        return repository;
    }

    @Override
    public void configure(HttpSecurity http) {
        http.addFilter(new SecurityContextHolderFilter(http.resolveSecurityContextRepository(),
                http.sessionCreationPolicy() == SessionCreationPolicy.ALWAYS),
                HttpSecurity.ORDER_SECURITY_CONTEXT);
    }
}
