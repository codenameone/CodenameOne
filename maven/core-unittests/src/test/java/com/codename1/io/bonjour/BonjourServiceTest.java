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
package com.codename1.io.bonjour;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic coverage for the immutable {@link BonjourService} value object:
 * field exposure, the defensively-copied / unmodifiable TXT map (including the
 * null-to-empty rule), and {@code toString}.
 */
class BonjourServiceTest {

    @Test
    void fieldsAreExposed() {
        Map<String, String> txt = new HashMap<String, String>();
        txt.put("path", "/api");
        BonjourService s = new BonjourService("Srv", "_http._tcp.", "192.168.1.5", 8080, txt);
        assertEquals("Srv", s.getName());
        assertEquals("_http._tcp.", s.getType());
        assertEquals("192.168.1.5", s.getHost());
        assertEquals(8080, s.getPort());
        assertEquals("/api", s.getTxt().get("path"));
    }

    @Test
    void txtMapIsADefensiveCopy() {
        Map<String, String> txt = new HashMap<String, String>();
        txt.put("a", "1");
        BonjourService s = new BonjourService("S", "_t._tcp.", "h", 1, txt);
        // Mutating the source after construction must not change the service.
        txt.put("b", "2");
        assertFalse(s.getTxt().containsKey("b"));
    }

    @Test
    void txtMapIsUnmodifiable() {
        final BonjourService s = new BonjourService("S", "_t._tcp.", "h", 1, null);
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                s.getTxt().put("x", "y");
            }
        });
    }

    @Test
    void nullTxtBecomesEmptyMap() {
        BonjourService s = new BonjourService("S", "_t._tcp.", "h", 1, null);
        assertNotNull(s.getTxt());
        assertTrue(s.getTxt().isEmpty());
    }

    @Test
    void nullHostIsAllowedForUnresolvedServices() {
        BonjourService s = new BonjourService("S", "_t._tcp.", null, 1, null);
        assertNull(s.getHost());
    }

    @Test
    void toStringIncludesNameTypeHostAndPort() {
        BonjourService s = new BonjourService("Srv", "_http._tcp.", "h", 80, null);
        String str = s.toString();
        assertTrue(str.contains("Srv"));
        assertTrue(str.contains("_http._tcp."));
        assertTrue(str.contains("h"));
        assertTrue(str.contains("80"));
    }
}
