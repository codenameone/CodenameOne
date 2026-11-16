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

import com.codename1.backend.Base64;
import com.codename1.backend.HttpServer;
import com.codename1.backend.security.ldap.BindAuthenticator;
import com.codename1.backend.security.ldap.DefaultLdapAuthoritiesPopulator;
import com.codename1.backend.security.ldap.FakeLdapServer;
import com.codename1.backend.security.ldap.LdapAuthenticationProvider;
import com.codename1.backend.security.ldap.LdapContextSource;
import com.codename1.impl.backend.security.SecuritySupport;
import org.junit.jupiter.api.Test;

/// A directory behind HTTP Basic, through the whole request path.
class LdapLoginTest {
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            return HttpServer.Response.text(200, who.getName() + " " + who.getAuthorities());
        }
    };

    private static String basic(String user, String password) throws Exception {
        return "Basic " + Base64.encode((user + ":" + password).getBytes("UTF-8"));
    }

    @Test
    void httpBasicAgainstTheDirectory() throws Exception {
        try (FakeLdapServer ldap = new FakeLdapServer()) {
            String ada = "uid=ada,ou=people,dc=corp,dc=example";
            ldap.entries.add(new FakeLdapServer.Entry(ada, "uid", "ada"));
            ldap.entries.add(new FakeLdapServer.Entry("cn=admins,ou=groups,dc=corp,dc=example",
                    "cn", "admins", "member", ada));
            ldap.passwords.put(ada, "s3cret");
            LdapContextSource source = new LdapContextSource(ldap.url() + "/dc=corp,dc=example");
            BindAuthenticator bind = new BindAuthenticator(source);
            bind.setUserDnPatterns("uid={0},ou=people");
            LdapAuthenticationProvider provider = new LdapAuthenticationProvider(bind,
                    new DefaultLdapAuthoritiesPopulator(source, "ou=groups"));
            try (SecuredServer server = SecuredServer.start(APP, http -> http
                    .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMINS"))
                    .authenticationProvider(provider)
                    .httpBasic(Customizer.withDefaults()).build())) {
                SecuredServer.Reply ok = server.get("/x", "Authorization", basic("ada", "s3cret"));
                assertEquals(200, ok.status);
                assertEquals("ada [ROLE_ADMINS]", ok.body);
                assertEquals(401, server.get("/x", "Authorization", basic("ada", "no")).status);
                assertEquals(401, server.get("/x", "Authorization", basic("ada", "")).status);
            }
        }
    }
}
