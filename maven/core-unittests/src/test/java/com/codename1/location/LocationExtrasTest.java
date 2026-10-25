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
package com.codename1.location;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;

class LocationExtrasTest extends UITestBase {

    @FormTest
    void testAccuracyIsStoredAndNaNIsIgnored() {
        Location loc = new Location(0, 0);
        loc.setAccuracy(12.5f);
        assertEquals(12.5f, loc.getAccuracy());
        loc.setAccuracy(Float.NaN);
        assertEquals(12.5f, loc.getAccuracy());
    }

    @FormTest
    void testGeofenceComparator() {
        Location loc = new Location(0, 0);
        Geofence g1 = new Geofence("id1", new Location(10, 10), 100, 10000);
        Geofence g2 = new Geofence("id2", new Location(20, 20), 100, 10000);

        Comparator<Geofence> cmp = Geofence.createDistanceComparator(loc);
        assertTrue(cmp.compare(g1, g2) < 0);
        assertTrue(cmp.compare(g2, g1) > 0);
        assertEquals(0, cmp.compare(g1, g1));

        Geofence ref = new Geofence("ref", loc, 100, 10000);
        Comparator<Geofence> cmp2 = Geofence.createDistanceComparator(ref);
        assertTrue(cmp2.compare(g1, g2) < 0);
    }

    @FormTest
    void testLocationComparator() {
        Location ref = new Location(0, 0);
        Location l1 = new Location(10, 10);
        Location l2 = new Location(20, 20);

        // Note: Typo in method name in source might be 'createDistanceCompartor' or 'createDistanceComparator'
        // I'll try the correct spelling first, if fails I'll check the grep result again.
        // Grep said: createDistanceCompartor
        Comparator<Location> cmp = ref.createDistanceCompartor();
        assertTrue(cmp.compare(l1, l2) < 0);
        assertTrue(cmp.compare(l2, l1) > 0);
    }
}
