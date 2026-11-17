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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.BadCredentialsException;
import com.codename1.backend.security.CredentialsExpiredException;
import com.codename1.backend.security.LockedException;
import com.codename1.backend.security.UsernamePasswordAuthenticationToken;
import com.codename1.backend.security.pki.Asn1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// The LDAP client and providers against [FakeLdapServer].
class LdapAuthenticationTest {
    private static final String ADA = "uid=ada,ou=people,dc=corp,dc=example";
    private FakeLdapServer server;
    private LdapContextSource source;

    @BeforeEach
    void start() throws Exception {
        server = new FakeLdapServer();
        server.entries.add(new FakeLdapServer.Entry(ADA, "uid", "ada", "cn", "Ada Lovelace",
                "mail", "ada@corp.example"));
        server.entries.add(new FakeLdapServer.Entry("uid=bob,ou=people,dc=corp,dc=example",
                "uid", "bob"));
        server.entries.add(new FakeLdapServer.Entry("cn=admins,ou=groups,dc=corp,dc=example",
                "cn", "admins", "member", ADA));
        server.entries.add(new FakeLdapServer.Entry("cn=staff,ou=groups,dc=corp,dc=example",
                "cn", "staff", "member", ADA, "member", "uid=bob,ou=people,dc=corp,dc=example"));
        server.passwords.put(ADA, "s3cret");
        source = new LdapContextSource(server.url() + "/dc=corp,dc=example");
    }

    @AfterEach
    void stop() throws Exception {
        server.close();
    }

    private LdapAuthenticationProvider patternProvider() {
        BindAuthenticator bind = new BindAuthenticator(source);
        bind.setUserDnPatterns("uid={0},ou=people");
        return new LdapAuthenticationProvider(bind,
                new DefaultLdapAuthoritiesPopulator(source, "ou=groups"));
    }

    private LdapAuthenticationProvider searchProvider() {
        BindAuthenticator bind = new BindAuthenticator(source);
        bind.setUserSearch(new FilterBasedLdapUserSearch("ou=people", "(uid={0})", source));
        return new LdapAuthenticationProvider(bind);
    }

    private static Authentication login(String user, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(user, password);
    }

    @Test
    void signsInWithDnPatternAndGroups() {
        Authentication a = patternProvider().authenticate(login("ada", "s3cret"));
        assertTrue(a.isAuthenticated());
        assertEquals("ada", a.getName());
        assertEquals("[ROLE_ADMINS, ROLE_STAFF]", a.getAuthorities().toString());
        LdapUserDetails user = (LdapUserDetails) a.getPrincipal();
        assertEquals(ADA, user.getDn());
        assertEquals("ada@corp.example", user.getEntry().getValue("MAIL"));
    }

    @Test
    void signsInWithUserSearch() {
        assertEquals("ada", searchProvider().authenticate(login("ada", "s3cret")).getName());
    }

    @Test
    void refusesWrongPassword() {
        assertThrows(BadCredentialsException.class,
                () -> patternProvider().authenticate(login("ada", "nope")));
    }

    @Test
    void neverSendsAnEmptyPassword() {
        // The fake accepts a bind with no password, as many directories do.
        assertThrows(BadCredentialsException.class,
                () -> patternProvider().authenticate(login("ada", "")));
        assertThrows(BadCredentialsException.class,
                () -> searchProvider().authenticate(login("ada", "")));
        assertTrue(server.binds.isEmpty(), server.binds.toString());
    }

    @Test
    void escapesTheUserNameInFilters() throws Exception {
        assertThrows(BadCredentialsException.class,
                () -> searchProvider().authenticate(login("*", "s3cret")));
        assertThrows(BadCredentialsException.class,
                () -> searchProvider().authenticate(login("x)(uid=*", "s3cret")));
        // (uid=*) would be a presence filter, 0x87; the escaped one is an
        // equality match whose value is the one byte '*'.
        assertEquals("a30804037569640401" + "2a", server.filters.get(0));
        assertTrue(server.binds.isEmpty(), server.binds.toString());
    }

    @Test
    void escapesTheUserNameInDns() {
        assertThrows(BadCredentialsException.class,
                () -> patternProvider().authenticate(login("ada,ou=people", "s3cret")));
        assertEquals("uid=ada\\,ou\\=people,ou=people,dc=corp,dc=example / s3cret",
                server.binds.get(0));
    }

    @Test
    void activeDirectory() {
        String upn = "ada@corp.example";
        server.entries.add(new FakeLdapServer.Entry("CN=Ada,CN=Users,DC=corp,DC=example",
                "objectClass", "user", "userPrincipalName", upn, "memberOf",
                "CN=Domain Admins,CN=Users,DC=corp,DC=example", "memberOf",
                "CN=R\\, D,OU=Groups,DC=corp,DC=example"));
        server.passwords.put(upn, "s3cret");
        ActiveDirectoryLdapAuthenticationProvider ad =
                new ActiveDirectoryLdapAuthenticationProvider("corp.example", server.url());
        Authentication a = ad.authenticate(login("ada", "s3cret"));
        assertEquals("[Domain Admins, R, D]", a.getAuthorities().toString());
        assertEquals(upn + " / s3cret", server.binds.get(0));
        ad.authenticate(login("ada@corp.example", "s3cret"));
        int binds = server.binds.size();
        // another domain of the forest is never asked
        assertThrows(BadCredentialsException.class,
                () -> ad.authenticate(login("ada@trusted.example", "s3cret")));
        assertEquals(binds, server.binds.size());

        server.bindDiagnostics.put("bob@corp.example",
                "80090308: LdapErr: DSID-0C09042A, comment: AcceptSecurityContext error, "
                        + "data 775, v3839");
        assertThrows(LockedException.class, () -> ad.authenticate(login("bob", "x")));
        server.bindDiagnostics.put("bob@corp.example", "AcceptSecurityContext error, data 532, v");
        assertThrows(CredentialsExpiredException.class, () -> ad.authenticate(login("bob", "x")));
        server.bindDiagnostics.put("bob@corp.example", "AcceptSecurityContext error, data 52e, v");
        assertThrows(BadCredentialsException.class, () -> ad.authenticate(login("bob", "x")));
    }

    @Test
    void activeDirectoryNestedGroups() {
        String upn = "ada@corp.example";
        String ada = "CN=Ada,CN=Users,DC=corp,DC=example";
        server.entries.add(new FakeLdapServer.Entry(ada, "objectClass", "user",
                "userPrincipalName", upn, "memberOf", "CN=Engineers,OU=Groups,DC=corp,DC=example"));
        server.entries.add(new FakeLdapServer.Entry("CN=Engineers,OU=Groups,DC=corp,DC=example",
                "objectClass", "group", "cn", "Engineers", "member", ada));
        server.entries.add(new FakeLdapServer.Entry("CN=Staff,OU=Groups,DC=corp,DC=example",
                "objectClass", "group", "cn", "Staff", "member",
                "CN=Engineers,OU=Groups,DC=corp,DC=example"));
        server.passwords.put(upn, "s3cret");
        ActiveDirectoryLdapAuthenticationProvider ad =
                new ActiveDirectoryLdapAuthenticationProvider("corp.example", server.url());
        assertEquals("[Engineers]", ad.authenticate(login("ada", "s3cret"))
                .getAuthorities().toString());
        ad.setSearchNestedGroups(true);
        assertEquals("[Engineers, Staff]", ad.authenticate(login("ada", "s3cret"))
                .getAuthorities().toString());
        assertEquals("1.2.840.113556.1.4.1941 member " + ada, FakeLdapServer.lastExtensible);
    }

    @Test
    void ambiguousUserIsBadCredentialsNotAnOutage() {
        String upn = "ada@corp.example";
        server.entries.add(new FakeLdapServer.Entry("CN=Ada,CN=Users,DC=corp,DC=example",
                "objectClass", "user", "userPrincipalName", upn));
        server.entries.add(new FakeLdapServer.Entry("CN=Ada2,CN=Users,DC=corp,DC=example",
                "objectClass", "user", "userPrincipalName", upn));
        server.entries.add(new FakeLdapServer.Entry("CN=Ada3,CN=Users,DC=corp,DC=example",
                "objectClass", "user", "userPrincipalName", upn));
        server.passwords.put(upn, "s3cret");
        ActiveDirectoryLdapAuthenticationProvider ad =
                new ActiveDirectoryLdapAuthenticationProvider("corp.example", server.url());
        assertThrows(BadCredentialsException.class, () -> ad.authenticate(login("ada", "s3cret")));
    }

    @Test
    void compilesExtensibleMatch() throws Exception {
        // RFC 4515's own examples
        assertEquals("a9158202636e830f467265642046726564657269636b73", hex(LdapFilter.compile("(cn:=Fred Fredericks)")));
        assertEquals("a91482016f830c41636520496e6475737472798401ff", hex(LdapFilter.compile("(o:dn:=Ace Industry)")));
        assertEquals("a912810c322e342e362e382e392e313083024a6f", hex(LdapFilter.compile("(:2.4.6.8.9.10:=Jo)")));
        assertThrows(java.io.IOException.class, () -> LdapFilter.compile("(:=x)"));
        assertThrows(java.io.IOException.class, () -> LdapFilter.compile("(cn:a b:=x)"));
    }

    @Test
    void compilesFilters() throws Exception {
        assertEquals("a40c0402636e3006800161820162", hex(LdapFilter.compile("(cn=a*b)")));
        assertEquals("8702636e", hex(LdapFilter.compile("cn=*")));
        assertEquals("a20aa30804026f75040262c3", hex(LdapFilter.compile("(!(ou=b\\c3))")));
        byte[] and = LdapFilter.compile("(&(objectClass=person)(|(uid=a)(uid=b)))");
        assertEquals(0xa0, new Asn1.Reader(and).peek());
        assertThrows(java.io.IOException.class, () -> LdapFilter.compile("(uid=a"));
        assertThrows(java.io.IOException.class, () -> LdapFilter.compile("(uid=a)(x=y)"));
        assertThrows(java.io.IOException.class, () -> LdapFilter.compile("(uid=\\zz)"));
    }

    @Test
    void encodes() {
        assertEquals("\\2a\\28\\29\\5c\\00", LdapEncoder.filterEncode("*()\\\u0000"));
        assertEquals("\\ a\\,b\\+c\\=\\ ", LdapEncoder.nameEncode(" a,b+c= "));
        assertEquals("Domain Admins", ActiveDirectoryLdapAuthenticationProvider.firstValue(
                "CN=Domain Admins,CN=Users,DC=x"));
        assertFalse(LdapEncoder.asciiLower("TITLE").contains("\u0131"));
    }

    private static String hex(byte[] b) {
        StringBuilder out = new StringBuilder();
        for (byte x : b) {
            out.append(String.format("%02x", x));
        }
        return out.toString();
    }
}
