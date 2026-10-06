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

/// Whether a chain remembers where an anonymous request was going; see
/// [RequestCache]. A chain that sends such a request to a login page remembers
/// it in the session; one that only challenges -- HTTP Basic -- has nothing to
/// come back from and remembers nothing, unless a cache is set here.
public final class RequestCacheConfigurer extends SecurityConfigurer
        implements HttpSecurity.RequestCacheSource {
    private RequestCache requestCache;

    RequestCacheConfigurer() {
    }

    public RequestCacheConfigurer requestCache(RequestCache requestCache) {
        this.requestCache = requestCache;
        return this;
    }

    /// @param redirects whether the chain answers a request that must sign in
    /// by sending it to a login page, which is when there is somewhere to come
    /// back from
    @Override
    public RequestCache resolve(SessionCreationPolicy policy, boolean redirects) {
        if (requestCache != null) {
            return requestCache;
        }
        // Spring remembers the address under every chain. A chain whose only
        // way in is a challenge -- HTTP Basic, a token -- never redirects back
        // to it, so here that would be a session started, and written to the
        // session store, for every anonymous request an API refuses.
        if (policy == SessionCreationPolicy.STATELESS || !redirects) {
            requestCache = new NullRequestCache();
        } else {
            HttpSessionRequestCache session = new HttpSessionRequestCache();
            session.setCreateSessionAllowed(policy != SessionCreationPolicy.NEVER);
            requestCache = session;
        }
        return requestCache;
    }
}
