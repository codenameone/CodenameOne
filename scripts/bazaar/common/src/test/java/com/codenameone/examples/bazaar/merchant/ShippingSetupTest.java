/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.codenameone.examples.bazaar.api.RoutingRuleDto;
import com.codenameone.examples.bazaar.api.ShippingProfileDto;
import com.codenameone.examples.bazaar.api.ShippingRateDto;
import com.codenameone.examples.bazaar.api.ShippingZoneDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/// The shipping editor's profile and routing arithmetic.
class ShippingSetupTest {
    private static ShippingZoneDto zone(String id, String profile) {
        return new ShippingZoneDto(id, id, "US", 0, new ArrayList<ShippingRateDto>(), profile);
    }

    @Test
    void zonesBelongToTheirProfile() {
        List<ShippingZoneDto> zones = Arrays.asList(zone("a", null), zone("b", "big"),
                zone("c", ""));
        assertEquals(2, ShippingSetup.zonesOf(zones, null).size());
        assertEquals(2, ShippingSetup.zonesOf(zones, "").size());
        assertEquals("b", ShippingSetup.zonesOf(zones, "big").get(0).id());
        assertEquals(0, ShippingSetup.zonesOf(null, "big").size());
    }

    @Test
    void aProductsProfile() {
        List<ShippingProfileDto> profiles = Arrays.asList(
                new ShippingProfileDto("big", "Oversized", 0, Arrays.asList("p1", "p2")),
                new ShippingProfileDto("glass", "Fragile", 1, null));
        assertEquals("big", ShippingSetup.profileOf(profiles, "p2").id());
        assertNull(ShippingSetup.profileOf(profiles, "p3"));
        assertNull(ShippingSetup.profileOf(null, "p1"));
    }

    @Test
    void locationListsRoundTrip() {
        assertEquals(Arrays.asList("a", "b"), ShippingSetup.ids(" a, ,b,a,"));
        assertEquals(0, ShippingSetup.ids(null).size());
        assertEquals(0, ShippingSetup.ids("").size());
        assertEquals("a,b", ShippingSetup.join(Arrays.asList("a", "b")));
        assertEquals("", ShippingSetup.join(null));
        assertEquals(Arrays.asList("c", "a", "d"), ShippingSetup.keepOrder(
                Arrays.asList("c", "b", "a"), Arrays.asList("a", "c", "d")));
    }

    @Test
    void aNewRuleGoesLast() {
        assertEquals(0, ShippingSetup.nextPosition(null));
        assertEquals(6, ShippingSetup.nextPosition(Arrays.asList(
                new RoutingRuleDto("x", "CLOSEST", "", 5),
                new RoutingRuleDto("y", "PRIORITY", "a", 2))));
    }
}
