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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.CustomDomainDto;
import org.junit.jupiter.api.Test;

/// The store clock and custom domain screens' text. Nothing here opens a
/// display or loads a bundle.
class StoreZonesTest {
    @Test
    void zonesAreNamedByCityAreaAndIanaName() {
        assertEquals("UTC", StoreZones.label("UTC"));
        assertEquals("UTC", StoreZones.label(null));
        assertEquals("New York (America) - America/New_York",
                StoreZones.label("America/New_York"));
        assertEquals("Buenos Aires (America) - America/Argentina/Buenos_Aires",
                StoreZones.label("America/Argentina/Buenos_Aires"));
        assertEquals(StoreZones.ZONES.length, StoreZones.labels(StoreZones.ZONES).length);
    }

    @Test
    void everyCuratedZoneIsAnIanaShapeTheServerTakes() {
        for (int iter = 0; iter < StoreZones.ZONES.length; iter++) {
            String z = StoreZones.ZONES[iter];
            assertTrue("UTC".equals(z) || z.indexOf('/') > 0, z);
            assertEquals(iter, StoreZones.indexOf(StoreZones.ZONES, z), "listed once: " + z);
        }
    }

    @Test
    void aSavedZoneOutsideTheListIsOfferedFirst() {
        assertSame(StoreZones.ZONES, StoreZones.choices("Europe/Paris"));
        assertSame(StoreZones.ZONES, StoreZones.choices(""));
        String[] more = StoreZones.choices("Asia/Kathmandu");
        assertEquals(StoreZones.ZONES.length + 1, more.length);
        assertEquals("Asia/Kathmandu", more[0]);
        assertEquals(0, StoreZones.indexOf(more, "Asia/Kathmandu"));
        assertEquals(-1, StoreZones.indexOf(StoreZones.ZONES, "Mars/Olympus"));
    }

    @Test
    void aTypedDomainBecomesAHostName() {
        assertEquals("shop.example.com", StoreZones.host("  https://Shop.Example.COM/path?q"));
        assertEquals("shop.example.com", StoreZones.host("shop.example.com."));
        assertEquals("shop.example.com", StoreZones.host("http://shop.example.com:8080"));
        assertEquals("", StoreZones.host(null));
        assertEquals("", StoreZones.host("   "));
    }

    @Test
    void aClaimReadsAsNonePendingOrVerified() {
        assertEquals(StoreZones.NONE, StoreZones.state(null));
        assertEquals(StoreZones.NONE, StoreZones.state(dto("", false)));
        assertEquals(StoreZones.PENDING, StoreZones.state(dto("shop.example.com", false)));
        assertEquals(StoreZones.VERIFIED, StoreZones.state(dto("shop.example.com", true)));
        assertEquals("_bazaar-verify.shop.example.com TXT \"bazaar-verify=tok\"",
                StoreZones.txtLine(dto("shop.example.com", false)));
        assertEquals("", StoreZones.txtLine(dto("", false)));
    }

    private static CustomDomainDto dto(String domain, boolean verified) {
        boolean any = domain.length() > 0;
        return new CustomDomainDto(domain, any ? "tok" : "", verified, verified ? 1L : 0L,
                any ? "https://" + domain + "/.well-known/bazaar-verify/tok" : "",
                any ? "_bazaar-verify." + domain : "", any ? "bazaar-verify=tok" : "", "");
    }
}
