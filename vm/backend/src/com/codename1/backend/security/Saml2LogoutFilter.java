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

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.crypto.SignedTokens;
import com.codename1.backend.security.saml2.RelyingPartyRegistration;
import com.codename1.backend.security.saml2.RelyingPartyRegistrationRepository;
import com.codename1.backend.security.saml2.Saml2AuthenticatedPrincipal;
import com.codename1.backend.security.saml2.Saml2Authentication;
import com.codename1.backend.security.saml2.Saml2AuthenticationException;
import com.codename1.backend.security.saml2.Saml2Logout;
import com.codename1.backend.security.saml2.Saml2Message;

/// SAML Single Logout's two ends; see [Saml2LogoutConfigurer].
///
/// At `{base}/{registrationId}` it takes the identity provider's
/// `LogoutRequest` (ends the named user's session, answers with a
/// `LogoutResponse`) and its `LogoutResponse` to a request this server sent
/// (held to the signed cookie naming that request). Through
/// [#relyingPartyInitiated] it turns the chain's own sign-out of a SAML user
/// into a `LogoutRequest` to the identity provider.
final class Saml2LogoutFilter implements SecurityFilter {
    /// Holds "registrationId requestId" of the LogoutRequest this browser was sent with.
    static final String COOKIE = "__Host-cn1_saml2_logout_request";
    static final String PURPOSE = "saml2-logout-request";

    private final String prefix;
    private final RelyingPartyRegistrationRepository registrations;
    private final SignedTokens tokens;
    private final LogoutHooks hooks;
    private final String fallbackSuccessUrl;

    Saml2LogoutFilter(String prefix, RelyingPartyRegistrationRepository registrations,
            SignedTokens tokens, LogoutHooks hooks, String fallbackSuccessUrl) {
        this.prefix = prefix;
        this.registrations = registrations;
        this.tokens = tokens;
        this.hooks = hooks;
        this.fallbackSuccessUrl = fallbackSuccessUrl;
    }

    private String successUrl() {
        return hooks.successUrl != null ? hooks.successUrl : fallbackSuccessUrl;
    }

    @Override
    public HttpServer.Response doFilter(HttpServer.Request request, FilterChain chain)
            throws Exception {
        String path = SecurityExchange.path(request);
        String method = request.getMethod();
        if (!path.startsWith(prefix) || path.indexOf('/', prefix.length()) >= 0
                || !("GET".equals(method) || "POST".equals(method))) {
            return chain.doFilter(request);
        }
        RelyingPartyRegistration r = registrations.findByRegistrationId(
                path.substring(prefix.length()));
        if (r == null || r.getSigningKey() == null
                || r.getAssertingPartySingleLogoutServiceLocation() == null) {
            return Responses.status(404, "Not Found");
        }
        Saml2Logout.Received message;
        try {
            message = read(request, r);
        } catch (Saml2AuthenticationException refused) {
            return Responses.status(400, "The logout message was refused: "
                    + refused.getErrorCode());
        } catch (IllegalArgumentException malformed) {
            return Responses.status(400, "The logout message was refused");
        }
        if (message == null) {
            return chain.doFilter(request);
        }
        return message.isRequest() ? assertingPartyInitiated(request, r, message)
                : answered(request, r, message);
    }

    private Saml2Logout.Received read(HttpServer.Request request, RelyingPartyRegistration r) {
        String slo = Saml2Addresses.slo(request, r);
        long now = System.currentTimeMillis();
        if ("GET".equals(request.getMethod())) {
            String target = request.getTarget();
            int q = target.indexOf('?');
            if (q < 0) {
                return null;
            }
            String query = target.substring(q + 1);
            if (query.indexOf("SAMLRequest=") < 0 && query.indexOf("SAMLResponse=") < 0) {
                return null;
            }
            // A LogoutResponse is sent to the response location, which may
            // differ from the request location; both are this filter's.
            try {
                return Saml2Logout.readRedirect(r, query, slo, now);
            } catch (Saml2AuthenticationException wrongPlace) {
                String responseLocation = Saml2Addresses.sloResponse(request, r);
                if (!responseLocation.equals(slo)
                        && "invalid_destination".equals(wrongPlace.getErrorCode())) {
                    return Saml2Logout.readRedirect(r, query, responseLocation, now);
                }
                throw wrongPlace;
            }
        }
        String parameter = "SAMLRequest";
        String value = Responses.param(request, parameter);
        if (value == null) {
            parameter = "SAMLResponse";
            value = Responses.param(request, parameter);
        }
        if (value == null) {
            return null;
        }
        String relayState = Responses.param(request, "RelayState");
        try {
            return Saml2Logout.readPost(r, parameter, value, relayState, slo, now);
        } catch (Saml2AuthenticationException wrongPlace) {
            String responseLocation = Saml2Addresses.sloResponse(request, r);
            if (!responseLocation.equals(slo)
                    && "invalid_destination".equals(wrongPlace.getErrorCode())) {
                return Saml2Logout.readPost(r, parameter, value, relayState, responseLocation,
                        now);
            }
            throw wrongPlace;
        }
    }

    /// The identity provider asks this server to sign a user out.
    private HttpServer.Response assertingPartyInitiated(HttpServer.Request request,
            RelyingPartyRegistration r, Saml2Logout.Received message) throws Exception {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        String status = Saml2Logout.SUCCESS;
        if (current instanceof Saml2Authentication) {
            Saml2AuthenticatedPrincipal p = ((Saml2Authentication) current).getPrincipal();
            if (matches(p, r, message)) {
                for (LogoutHandler handler : hooks.handlers) {
                    handler.logout(request, current);
                }
            } else {
                status = Saml2Logout.REQUESTER;
            }
        }
        // No session in this browser (or a POST that arrived without the
        // SameSite=Lax cookie) answers Success: there is nothing here to end.
        Saml2Message answer = Saml2Logout.response(r, message.getId(), status,
                Saml2Addresses.entityId(request, r), message.getRelayState(),
                System.currentTimeMillis());
        return send(answer);
    }

    private static boolean matches(Saml2AuthenticatedPrincipal p, RelyingPartyRegistration r,
                                   Saml2Logout.Received message) {
        if (!r.getRegistrationId().equals(p.getRelyingPartyRegistrationId())
                || !p.getName().equals(message.getNameId())) {
            return false;
        }
        if (message.getSessionIndexes().isEmpty() || p.getSessionIndexes().isEmpty()) {
            return true;
        }
        for (String index : message.getSessionIndexes()) {
            if (p.getSessionIndexes().contains(index)) {
                return true;
            }
        }
        return false;
    }

    /// The identity provider answers the LogoutRequest this server sent.
    private HttpServer.Response answered(HttpServer.Request request, RelyingPartyRegistration r,
            Saml2Logout.Received message) {
        String cookie = request.getCookie(COOKIE);
        String subject = cookie == null || cookie.length() == 0 ? null
                : tokens.verify(PURPOSE, cookie);
        String expected = r.getRegistrationId() + " " + message.getInResponseTo();
        if (subject == null || message.getInResponseTo() == null || !subject.equals(expected)) {
            return clear(Responses.status(400, "The logout response answers no request "
                    + "of this browser"));
        }
        // The session here already ended when the request was sent; a failed
        // status only means the identity provider kept its own.
        String target = successUrl();
        if (!Saml2Logout.SUCCESS.equals(message.getStatus())) {
            target = target + (target.indexOf('?') < 0 ? "?" : "&") + "saml2_logout_partial";
        }
        return clear(Responses.redirect(target));
    }

    private static HttpServer.Response clear(HttpServer.Response response) {
        return response.header("Set-Cookie", COOKIE
                + "=; Path=/; Max-Age=0; Secure; HttpOnly; SameSite=None");
    }

    private static HttpServer.Response send(Saml2Message message) {
        if (RelyingPartyRegistration.POST.equals(message.getBinding())) {
            return Responses.html(200, message.getPostPage())
                    .header("Cache-Control", "no-store");
        }
        return Responses.redirect(message.getRedirectUrl());
    }

    /// Wraps the chain's sign-out: a SAML user whose registration takes part
    /// is sent on to the identity provider; anyone else goes where `next` says.
    LogoutSuccessHandler relyingPartyInitiated(final LogoutSuccessHandler next) {
        return new LogoutSuccessHandler() {
            @Override
            public HttpServer.Response onLogoutSuccess(HttpServer.Request request,
                    Authentication authentication) throws Exception {
                if (!(authentication instanceof Saml2Authentication)) {
                    return next.onLogoutSuccess(request, authentication);
                }
                Saml2AuthenticatedPrincipal p =
                        ((Saml2Authentication) authentication).getPrincipal();
                RelyingPartyRegistration r = p.getRelyingPartyRegistrationId() == null ? null
                        : registrations.findByRegistrationId(p.getRelyingPartyRegistrationId());
                if (r == null || r.getSigningKey() == null
                        || r.getAssertingPartySingleLogoutServiceLocation() == null) {
                    return next.onLogoutSuccess(request, authentication);
                }
                Saml2Message out = Saml2Logout.request(r, p, Saml2Addresses.entityId(request, r),
                        null, System.currentTimeMillis());
                String token = tokens.create(PURPOSE, r.getRegistrationId() + " " + out.getId(),
                        600);
                return send(out).header("Set-Cookie", COOKIE + "=" + token
                        + "; Path=/; Max-Age=600; Secure; HttpOnly; SameSite=None");
            }
        };
    }
}
