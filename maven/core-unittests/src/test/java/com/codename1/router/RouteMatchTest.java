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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Package-private to access `RouteMatch` directly.
class RouteMatchTest {

    @Test
    void literalMatches() {
        RouteMatch r = new RouteMatch("/about", null);
        assertNotNull(r.match("/about"));
        assertNotNull(r.match("/about/")); // trailing slash tolerated
        assertNull(r.match("/about/x"));
        assertNull(r.match("/other"));
    }

    @Test
    void namedParamExtraction() {
        RouteMatch r = new RouteMatch("/users/:id", null);
        Map<String, String> m = r.match("/users/42");
        assertNotNull(m);
        assertEquals("42", m.get("id"));
    }

    @Test
    void singleSegmentWildcard() {
        RouteMatch r = new RouteMatch("/files/*", null);
        assertNotNull(r.match("/files/foo.png"));
        assertNull(r.match("/files/sub/foo.png"));
    }

    @Test
    void catchAllWildcardMatchesEmptyAndDeep() {
        RouteMatch r = new RouteMatch("/files/**", null);
        Map<String, String> m1 = r.match("/files/");
        Map<String, String> m2 = r.match("/files/a/b/c");
        assertNotNull(m1);
        assertNotNull(m2);
        assertEquals("a/b/c", m2.get("*"));
    }

    @Test
    void catchAllWildcardMatchesBarePrefix() {
        // `/admin/**` should also match `/admin` (without trailing slash) —
        // Ant-style catch-all semantics. Real apps register guards as
        // `/admin/**` and expect the bare entry to be guarded too.
        RouteMatch r = new RouteMatch("/admin/**", null);
        Map<String, String> m = r.match("/admin");
        assertNotNull(m);
        assertEquals("", m.get("*"));
    }

    @Test
    void specificityFavorsLiteralsOverParams() {
        RouteMatch literal = new RouteMatch("/users/me", null);
        RouteMatch param = new RouteMatch("/users/:id", null);
        assertTrue(literal.specificity() > param.specificity(),
                "literal segment must outscore named param");
    }

    @Test
    void specificityFavorsParamOverWildcard() {
        RouteMatch param = new RouteMatch("/files/:name", null);
        RouteMatch wildcard = new RouteMatch("/files/**", null);
        assertTrue(param.specificity() > wildcard.specificity());
    }

    @Test
    void patternMustStartWithSlash() {
        RouteMatch r = new RouteMatch("about", null);
        // Constructor normalizes by prepending '/' — accept both forms.
        assertNotNull(r.match("/about"));
    }

    @Test
    void emptyPatternThrows() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            @Override public void execute() { new RouteMatch("", null); }
        });
    }
}
