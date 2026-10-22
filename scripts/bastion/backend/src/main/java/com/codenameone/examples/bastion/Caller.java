/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationToken;

import java.util.Iterator;

/// Who is asking. Every endpoint reads the caller from here and never from a
/// parameter, so there is no id in a request to change to reach someone
/// else's money.
public final class Caller {
    private Caller() {
    }

    public static String name() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (who == null || !who.isAuthenticated() || who.getName() == null) {
            throw new ResponseStatusException(401, "Sign in first");
        }
        return who.getName();
    }

    public static boolean hasRole(String role) {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (who == null || who.getAuthorities() == null) {
            return false;
        }
        String wanted = "ROLE_" + role;
        Iterator<? extends GrantedAuthority> it = who.getAuthorities().iterator();
        while (it.hasNext()) {
            if (wanted.equals(it.next().getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /// The client's address as the server saw it, for the audit log; empty
    /// outside a request.
    /// When the caller's token says they last proved who they are -- the
    /// `auth_time` claim, epoch seconds, which [SecurityConfig] copies from
    /// the sign-in into every access token, refreshed ones included -- or -1
    /// when the caller has no such token.
    public static long authTime() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (!(who instanceof JwtAuthenticationToken)) {
            return -1L;
        }
        Long at = ((JwtAuthenticationToken) who).getToken().getClaimAsLong("auth_time");
        return at == null ? -1L : at.longValue();
    }

    /// How the caller's sign-in was made: the token's `amr` claim (RFC 8176
    /// names -- `pwd`, `otp`, `hwk`, `mfa`), empty when it carries none.
    public static java.util.List<String> authMethods() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (!(who instanceof JwtAuthenticationToken)) {
            return java.util.Collections.emptyList();
        }
        java.util.List<String> amr = ((JwtAuthenticationToken) who).getToken()
                .getClaimAsStringList("amr");
        return amr == null ? java.util.Collections.<String>emptyList() : amr;
    }

    public static String address() {
        SecurityExchange exchange = SecurityExchange.current();
        if (exchange == null) {
            return "";
        }
        String address = exchange.getRequest().getRemoteAddress();
        return address == null ? "" : address;
    }
}
