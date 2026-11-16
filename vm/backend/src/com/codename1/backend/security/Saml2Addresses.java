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
import com.codename1.backend.security.saml2.RelyingPartyRegistration;

/// This server's SAML addresses for a registration, with `{baseUrl}` and
/// `{registrationId}` filled in.
final class Saml2Addresses {
    private Saml2Addresses() {
    }

    static String entityId(HttpServer.Request request, RelyingPartyRegistration r) {
        return resolve(request, r, r.getEntityId());
    }

    static String acs(HttpServer.Request request, RelyingPartyRegistration r) {
        return resolve(request, r, r.getAssertionConsumerServiceLocation());
    }

    static String slo(HttpServer.Request request, RelyingPartyRegistration r) {
        return resolve(request, r, r.getSingleLogoutServiceLocation());
    }

    static String sloResponse(HttpServer.Request request, RelyingPartyRegistration r) {
        return resolve(request, r, r.getSingleLogoutServiceResponseLocation());
    }

    private static String resolve(HttpServer.Request request, RelyingPartyRegistration r,
                                  String template) {
        String out = replace(template, "{registrationId}", r.getRegistrationId());
        if (out.indexOf("{baseUrl}") >= 0) {
            String host = request.getHeader("Host");
            if (host == null || !isAuthority(host)) {
                throw new IllegalArgumentException("The request names no host to build the "
                        + "SAML addresses of " + r.getRegistrationId() + " from; give the "
                        + "registration a whole entity-id and acs.location");
            }
            out = replace(out, "{baseUrl}", (request.isSecure() ? "https://" : "http://") + host);
        }
        return out;
    }

    private static boolean isAuthority(String host) {
        if (host.length() == 0 || host.length() > 255) {
            return false;
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == ':' || c == '[' || c == ']')) {
                return false;
            }
        }
        return true;
    }

    private static String replace(String text, String what, String with) {
        int at = text.indexOf(what);
        return at < 0 ? text : text.substring(0, at) + with + text.substring(at + what.length());
    }
}
