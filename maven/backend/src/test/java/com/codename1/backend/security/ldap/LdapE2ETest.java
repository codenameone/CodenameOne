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
package com.codename1.backend.security.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.AuthenticationServiceException;
import com.codename1.backend.security.BadCredentialsException;
import com.codename1.backend.security.UsernamePasswordAuthenticationToken;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/// The LDAP providers against a real OpenLDAP, plain and over verified TLS.
/// Runs under `maven/integration-tests/backend-identity/run.sh`, which starts
/// the server; skipped otherwise.
class LdapE2ETest {
    private static String plain;
    private static String tls;
    private static String ca;

    @BeforeAll
    static void server() {
        plain = System.getenv("CN1_E2E_LDAP_URL");
        tls = System.getenv("CN1_E2E_LDAPS_URL");
        ca = System.getenv("CN1_E2E_LDAP_CA");
        Assumptions.assumeTrue(plain != null && tls != null && ca != null,
                "no LDAP server; run maven/integration-tests/backend-identity/run.sh");
    }

    private static LdapAuthenticationProvider provider(LdapContextSource source) {
        BindAuthenticator bind = new BindAuthenticator(source);
        bind.setUserSearch(new FilterBasedLdapUserSearch("ou=people", "(uid={0})", source));
        DefaultLdapAuthoritiesPopulator groups =
                new DefaultLdapAuthoritiesPopulator(source, "ou=groups");
        return new LdapAuthenticationProvider(bind, groups);
    }

    private static LdapContextSource source(String url, boolean startTls) {
        LdapContextSource s = new LdapContextSource(url + "/dc=corp,dc=example");
        s.setStartTls(startTls);
        if (url.startsWith("ldaps") || startTls) {
            s.setCaFile(ca);
        }
        return s;
    }

    private static Authentication login(LdapContextSource s, String user, String password) {
        return provider(s).authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(user, password));
    }

    @Test
    void signsInOverEveryTransport() {
        LdapContextSource[] sources = {source(plain, false), source(tls, false),
            source(plain, true)};
        for (LdapContextSource s : sources) {
            Authentication a = login(s, "ada", "s3cret");
            assertEquals("ada", a.getName());
            java.util.Set<String> roles = new java.util.TreeSet<String>();
            for (Object r : a.getAuthorities()) {
                roles.add(r.toString());
            }
            assertEquals("[ROLE_ADMINS, ROLE_STAFF]", roles.toString(), s.getUrls().toString());
        }
    }

    @Test
    void refusesWrongEmptyAndInjectedPasswords() {
        LdapContextSource s = source(tls, false);
        assertThrows(BadCredentialsException.class, () -> login(s, "ada", "wrong"));
        assertThrows(BadCredentialsException.class, () -> login(s, "ada", ""));
        assertThrows(BadCredentialsException.class, () -> login(s, "*", "s3cret"));
        assertThrows(BadCredentialsException.class, () -> login(s, "ada)(uid=*", "s3cret"));
        assertThrows(BadCredentialsException.class, () -> login(s, "nobody", "s3cret"));
    }

    @Test
    void refusesAServerTheCaDidNotSign() throws Exception {
        java.io.File other = java.io.File.createTempFile("cn1-other-ca", ".pem");
        other.deleteOnExit();
        // any valid certificate that is not the e2e CA
        try (java.io.FileWriter w = new java.io.FileWriter(other)) {
            w.write(new com.codename1.backend.security.saml2.SamlFixtures().certificate.toPem());
        }
        LdapContextSource s = new LdapContextSource(tls + "/dc=corp,dc=example");
        s.setCaFile(other.getAbsolutePath());
        assertThrows(AuthenticationServiceException.class, () -> login(s, "ada", "s3cret"));
    }

    @Test
    void searchesWithExtensibleMatch() throws Exception {
        try (LdapConnection c = source(tls, false).connect()) {
            assertTrue(c.search("dc=corp,dc=example", LdapConnection.SCOPE_SUBTREE,
                    "(ou:dn:=people)", 0, 0).size() >= 3);
        }
    }
}
