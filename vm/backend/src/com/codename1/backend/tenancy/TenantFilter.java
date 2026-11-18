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
package com.codename1.backend.tenancy;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.FilterChain;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecurityFilter;
import com.codename1.impl.orm.TenantSource;

/// Sets [TenantContext] for the length of each request and clears it after,
/// whatever the handler does.
///
/// A request that names no tenant passes through with none set, so a
/// tenant-partitioned entity touched while serving it fails with
/// `MissingTenantException`; construct the filter with `required` true to
/// answer 400 at the door instead. A malformed identifier is always a 400.
///
/// Trust: a header is whatever the client sent. Resolve the tenant from
/// something the server vouches for -- the authenticated principal, a host
/// name the proxy routes -- unless a gateway in front strips and sets the
/// header.
public final class TenantFilter implements SecurityFilter {
    private final TenantResolver resolver;
    private final boolean required;

    /// @param resolver how the tenant is found
    /// @param required whether a request without a tenant is refused
    public TenantFilter(TenantResolver resolver, boolean required) {
        if (resolver == null) {
            throw new IllegalArgumentException("resolver is null");
        }
        this.resolver = resolver;
        this.required = required;
    }

    /// A resolver reading a header, conventionally `X-Tenant-ID`.
    /// @param name the header name
    /// @return the resolver
    public static TenantResolver header(final String name) {
        return new TenantResolver() {
            @Override
            public String resolve(HttpServer.Request request) {
                return request.getHeader(name);
            }
        };
    }

    /// A resolver taking the first label of the `Host` header below `domain`:
    /// `acme.example.com` under `example.com` is tenant `acme`.
    /// @param domain the shared parent domain
    /// @return the resolver
    public static TenantResolver subdomain(final String domain) {
        return new TenantResolver() {
            @Override
            public String resolve(HttpServer.Request request) {
                String host = request.getHeader("Host");
                if (host == null) {
                    return null;
                }
                int colon = host.indexOf(':');
                if (colon >= 0) {
                    host = host.substring(0, colon);
                }
                String suffix = "." + domain;
                if (host.length() <= suffix.length()
                        || !host.regionMatches(true, host.length() - suffix.length(), suffix, 0, suffix.length())) {
                    return null;
                }
                String label = host.substring(0, host.length() - suffix.length());
                return label.indexOf('.') >= 0 ? null : label;
            }
        };
    }

    /// A resolver asking `rule` for the tenant of the authenticated principal.
    /// Install the filter after authentication, so the principal is set; a
    /// request that is not authenticated resolves no tenant.
    /// @param rule how a principal maps to its tenant, such as a claim lookup
    /// @return the resolver
    public static TenantResolver authenticated(final PrincipalTenant rule) {
        return new TenantResolver() {
            @Override
            public String resolve(HttpServer.Request request) throws Exception {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated()) {
                    return null;
                }
                return rule.tenantOf(auth);
            }
        };
    }

    /// Maps an authenticated principal to its tenant, for [#authenticated].
    public interface PrincipalTenant {
        /// The tenant of `authentication`, or null when it has none.
        /// @param authentication the authenticated principal
        /// @return the tenant identifier
        /// @throws Exception when the lookup fails
        String tenantOf(Authentication authentication) throws Exception;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain) throws Exception {
        String tenant = resolver.resolve(request);
        if (tenant == null || tenant.length() == 0) {
            if (required) {
                return HttpServer.Response.text(400, "No tenant");
            }
            return chain.doFilter(request);
        }
        try {
            TenantSource.Ids.check(tenant);
        } catch (IllegalArgumentException e) {
            return HttpServer.Response.text(400, "Invalid tenant");
        }
        String previous = TenantContext.get();
        TenantContext.set(tenant);
        try {
            return chain.doFilter(request);
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }
}
