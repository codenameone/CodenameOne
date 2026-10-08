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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codename1.backend.HttpServer;
import org.junit.jupiter.api.Test;

class RequestMatcherAuthorizationTest {
    @Test
    void allOfCarriesEveryVariableToTheAuthorizationManager() throws Exception {
        RequestMatcher match = RequestMatchers.allOf(RequestMatchers.method("GET"),
                AntPathRequestMatcher.antMatcher("/owners/{owner}/**"),
                AntPathRequestMatcher.antMatcher("/owners/*/notes/{id}"));
        try (SecuredServer guarded = SecuredServer.start(
                request -> HttpServer.Response.text(200, "allowed"),
                http -> http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth
                        .requestMatchers(match).access((authentication, context) ->
                                new AuthorizationDecision("ada".equals(context.getVariables().get("owner"))
                                        && "42".equals(context.getVariables().get("id"))))
                        .anyRequest().denyAll()).build())) {
            assertEquals(200, guarded.get("/owners/ada/notes/42").status);
            assertEquals(403, guarded.get("/owners/eve/notes/42").status);
            assertEquals(403, guarded.call("POST", "/owners/ada/notes/42", "", null).status);
        }
    }

}
