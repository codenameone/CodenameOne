package com.codename1.location;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;

class LocationExtrasTest extends UITestBase {

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
