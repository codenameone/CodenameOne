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

import java.util.ArrayList;
import java.util.List;

/// What a chain's sign-out does, kept where a mechanism that ends sessions by
/// its own route -- SAML Single Logout -- can reach it: the handlers the
/// [LogoutConfigurer] runs, the address it sends a signed-out user to, and the
/// wrappers that may answer a sign-out in its place.
///
/// Generic on purpose: [LogoutConfigurer] names no mechanism, so a server that
/// signs out without SAML links none of SAML.
final class LogoutHooks {
    /// Answers a sign-out in place of `next`, or hands it on.
    interface Wrapper {
        LogoutSuccessHandler wrap(LogoutSuccessHandler next);
    }

    /// Filled in by [LogoutConfigurer#configure]; read at request time.
    final List<LogoutHandler> handlers = new ArrayList<LogoutHandler>();
    final List<Wrapper> wrappers = new ArrayList<Wrapper>();
    /// `last`, behind every wrapper -- resolved at the first sign-out, since a
    /// configurer that adds a wrapper may configure after [LogoutConfigurer].
    LogoutSuccessHandler wrapped(final LogoutSuccessHandler last) {
        return new LogoutSuccessHandler() {
            private LogoutSuccessHandler chain;

            @Override
            public com.codename1.backend.HttpServer.Response onLogoutSuccess(
                    com.codename1.backend.HttpServer.Request request,
                    Authentication authentication) throws Exception {
                LogoutSuccessHandler c = chain;
                if (c == null) {
                    c = last;
                    for (Wrapper wrapper : wrappers) {
                        c = wrapper.wrap(c);
                    }
                    chain = c;
                }
                return c.onLogoutSuccess(request, authentication);
            }
        };
    }

    /// Where a signed-out user goes; null when the application answers sign-out itself.
    String successUrl;
}
