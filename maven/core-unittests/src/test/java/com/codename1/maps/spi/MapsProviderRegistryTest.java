/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.spi;

import com.codename1.maps.WebMapProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Provider registry fallback-chain resolution and WebMapProvider semantics. */
class MapsProviderRegistryTest {

    @BeforeEach
    @AfterEach
    void reset() {
        MapProviderRegistry.resetForTest();
    }

    // A WebMapProvider doubles as a configurable test provider: a non-empty key
    // makes isAvailable() true, an empty one false.
    private static MapProvider provider(String id, boolean available) {
        return new WebMapProvider(id, available ? "k" : "", "<html></html>");
    }

    @Test
    void emptyRegistryHasNoProvider() {
        assertNull(MapProviderRegistry.getProvider());
        assertFalse(MapProviderRegistry.hasProvider());
    }

    @Test
    void registerAndResolveSingle() {
        MapProvider p = provider("google", true);
        MapProviderRegistry.register(p);
        assertSame(p, MapProviderRegistry.getProvider());
        assertTrue(MapProviderRegistry.hasProvider());
    }

    @Test
    void preferredOrderWins() {
        MapProvider g = provider("google", true);
        MapProvider h = provider("huawei", true);
        MapProviderRegistry.register(g);
        MapProviderRegistry.register(h);
        MapProviderRegistry.setProviderOrder(new String[]{"huawei", "google"});
        assertSame(h, MapProviderRegistry.getProvider());
        MapProviderRegistry.setProviderOrder(new String[]{"google", "huawei"});
        assertSame(g, MapProviderRegistry.getProvider());
    }

    @Test
    void unavailablePreferredFallsThroughChain() {
        MapProvider g = provider("google", false);
        MapProvider h = provider("huawei", true);
        MapProviderRegistry.register(g);
        MapProviderRegistry.register(h);
        MapProviderRegistry.setProviderOrder(new String[]{"google", "huawei", "vector"});
        assertSame(h, MapProviderRegistry.getProvider());
    }

    @Test
    void vectorTerminatorForcesFallback() {
        MapProviderRegistry.register(provider("google", true));
        MapProviderRegistry.setProviderOrder(new String[]{"vector"});
        assertNull(MapProviderRegistry.getProvider());
        // An available provider before the terminator still wins.
        MapProviderRegistry.setProviderOrder(new String[]{"google", "vector"});
        assertNotNull(MapProviderRegistry.getProvider());
    }

    @Test
    void anyAvailableUsedWhenChainNamesNone() {
        MapProvider g = provider("google", true);
        MapProviderRegistry.register(g);
        MapProviderRegistry.setProviderOrder(new String[]{"nonexistent"});
        assertSame(g, MapProviderRegistry.getProvider());
    }

    @Test
    void registerReplacesSameId() {
        MapProvider a = provider("google", true);
        MapProvider b = provider("google", true);
        MapProviderRegistry.register(a);
        MapProviderRegistry.register(b);
        assertSame(b, MapProviderRegistry.getProvider());
    }

    @Test
    void setPreferredProviderIsOneElementChain() {
        MapProviderRegistry.register(provider("google", true));
        MapProviderRegistry.register(provider("huawei", true));
        MapProviderRegistry.setPreferredProvider("huawei");
        assertEquals("huawei", MapProviderRegistry.getProvider().getId());
    }

    @Test
    void webMapProviderGoogleIdAndAvailability() {
        WebMapProvider p = WebMapProvider.google("AIzaSyExampleKey");
        assertEquals("web", p.getId());
        assertTrue(p.isAvailable());
        assertFalse(WebMapProvider.google("").isAvailable());
        // An unsubstituted build placeholder ("{...}") must not count as a key.
        assertFalse(WebMapProvider.google("{GOOGLE_MAPS_API_KEY}").isAvailable());
    }
}
