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
package com.codename1.router;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeepLinkTest {

    @Test
    void parsesFullHttpsUrl() {
        DeepLink l = DeepLink.parse("https://example.com/users/42?tab=posts&sort=new#bio");
        assertEquals("https", l.getScheme());
        assertEquals("example.com", l.getHost());
        assertEquals("/users/42", l.getPath());
        assertEquals("bio", l.getFragment());
        Map<String, String> q = l.getQueryParameters();
        assertEquals("posts", q.get("tab"));
        assertEquals("new", q.get("sort"));
        List<String> segs = l.getSegments();
        assertEquals(2, segs.size());
        assertEquals("users", segs.get(0));
        assertEquals("42", segs.get(1));
    }

    @Test
    void parsesCustomSchemeWithoutHost() {
        DeepLink l = DeepLink.parse("myapp:profile/42");
        assertEquals("myapp", l.getScheme());
        assertEquals("", l.getHost());
        assertEquals("/profile/42", l.getPath());
    }

    @Test
    void parsesCustomSchemeWithDoubleSlashAndHost() {
        DeepLink l = DeepLink.parse("myapp://chat/room?id=5");
        assertEquals("myapp", l.getScheme());
        assertEquals("chat", l.getHost());
        assertEquals("/room", l.getPath());
        assertEquals("5", l.getQueryParameter("id"));
    }

    @Test
    void parsesBarePathAsScheme0Host0() {
        DeepLink l = DeepLink.parse("/users/42");
        assertEquals("", l.getScheme());
        assertEquals("", l.getHost());
        assertEquals("/users/42", l.getPath());
    }

    @Test
    void normalizesMissingLeadingSlash() {
        DeepLink l = DeepLink.parse("users/42");
        assertEquals("/users/42", l.getPath());
    }

    @Test
    void rootPathIsAlwaysSlash() {
        DeepLink l = DeepLink.parse("https://example.com/");
        assertEquals("/", l.getPath());
        assertTrue(l.getSegments().isEmpty());
    }

    @Test
    void hostIsLowercased() {
        DeepLink l = DeepLink.parse("HTTPS://Example.COM/foo");
        assertEquals("https", l.getScheme());
        assertEquals("example.com", l.getHost());
    }

    @Test
    void portAndUserInfoStrippedFromHost() {
        DeepLink l = DeepLink.parse("https://user:pass@example.com:8443/foo");
        assertEquals("example.com", l.getHost());
        assertEquals("/foo", l.getPath());
    }

    @Test
    void emptyForNull() {
        DeepLink l = DeepLink.parse(null);
        assertTrue(l.isEmpty());
        assertEquals("/", l.getPath());
        assertEquals("", l.getRaw());
    }

    @Test
    void percentDecodesSegmentsAndQuery() {
        DeepLink l = DeepLink.parse("https://x.com/hello%20world?name=Ana%20Lima");
        assertEquals("hello world", l.getSegments().get(0));
        assertEquals("Ana Lima", l.getQueryParameter("name"));
    }

    @Test
    void withPathReplacesOnlyThePath() {
        DeepLink l = DeepLink.parse("https://example.com/old?x=1");
        DeepLink l2 = l.withPath("/new");
        assertEquals("/new", l2.getPath());
        assertEquals("example.com", l2.getHost());
        assertEquals("1", l2.getQueryParameter("x"));
    }

    @Test
    void equalsByRaw() {
        DeepLink a = DeepLink.parse("https://example.com/x");
        DeepLink b = DeepLink.parse("https://example.com/x");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void hashColonInPathIsNotMistakenForScheme() {
        // "/v1:install" should parse as a path-only link, not a scheme.
        DeepLink l = DeepLink.parse("/v1:install");
        assertEquals("", l.getScheme());
        assertEquals("/v1:install", l.getPath());
    }
}
