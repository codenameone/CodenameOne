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
package android.location;

import android.content.Context;
import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LocationManagerTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    /// A Codename One location manager whose fixes the test supplies.
    static final class FakeLocations extends com.codename1.location.LocationManager {
        com.codename1.location.Location last;
        int binds;
        int clears;

        @Override
        public com.codename1.location.Location getCurrentLocation() {
            return last;
        }

        @Override
        public com.codename1.location.Location getLastKnownLocation() {
            return last;
        }

        @Override
        protected void bindListener() {
            binds++;
        }

        @Override
        protected void clearListener() {
            clears++;
        }

        void fix(double lat, double lon) {
            com.codename1.location.Location l = new com.codename1.location.Location();
            l.setLatitude(lat);
            l.setLongitude(lon);
            l.setAccuracy(5);
            l.setTimeStamp(System.currentTimeMillis());
            last = l;
            if (getListener() != null) {
                getListener().locationUpdated(l);
            }
        }
    }

    /// One for the whole class: the shim is a per-application singleton
    /// that binds its platform manager once, as the real one never changes.
    private static final FakeLocations fake = new FakeLocations();

    @Before
    public void setUp() {
        AndroidTestSupport.context();
        LocationManager.override = fake;
    }

    @After
    public void tearDown() {
        LocationManager.override = null;
    }

    @Test
    public void systemServiceIsTheShim() {
        Context c = AndroidTestSupport.context();
        assertSame(LocationManager.getInstance(), c.getSystemService(Context.LOCATION_SERVICE));
    }

    @Test
    public void updatesFanOutAndHonourMinDistance() {
        LocationManager lm = LocationManager.getInstance();
        final List<Location> all = new ArrayList<Location>();
        final List<Location> far = new ArrayList<Location>();
        LocationListener a = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                all.add(location);
            }
        };
        LocationListener b = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                far.add(location);
            }
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, a);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1000, b);
        MainThreadRule.drain();
        assertTrue(fake.binds > 0);
        fake.fix(32.0, 34.0);
        fake.fix(32.0001, 34.0);
        fake.fix(33.0, 34.0);
        MainThreadRule.drain();
        assertEquals(3, all.size());
        assertEquals(2, far.size());
        assertEquals(LocationManager.GPS_PROVIDER, all.get(0).getProvider());
        assertEquals(32.0, all.get(0).getLatitude(), 1e-9);
        lm.removeUpdates(a);
        lm.removeUpdates(b);
        MainThreadRule.drain();
        fake.fix(34.0, 34.0);
        MainThreadRule.drain();
        assertEquals(3, all.size());
    }

    /// A listener removed by another listener's callback hears nothing more,
    /// not even the fix being delivered when it was removed.
    @Test
    public void aListenerRemovedDuringDeliveryHearsNothing() {
        final LocationManager lm = LocationManager.getInstance();
        final List<Location> heardByB = new ArrayList<Location>();
        final LocationListener b = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                heardByB.add(location);
            }
        };
        LocationListener a = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lm.removeUpdates(b);
            }
        };
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, a);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, b);
        MainThreadRule.drain();
        fake.fix(32.0, 34.0);
        MainThreadRule.drain();
        assertEquals(0, heardByB.size());
        lm.removeUpdates(a);
        MainThreadRule.drain();
    }

    @Test(expected = IllegalArgumentException.class)
    public void theExecutorOverloadRefusesAnUnknownProvider() {
        LocationManager.getInstance().requestLocationUpdates("nope", 0, 0, new java.util.concurrent.Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        }, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }
        });
    }

    @Test
    public void lastKnownLocationAndDistance() {
        fake.fix(0, 0);
        Location l = LocationManager.getInstance().getLastKnownLocation(LocationManager.GPS_PROVIDER);
        assertNotNull(l);
        assertTrue(l.hasAccuracy());
        float[] out = new float[1];
        Location.distanceBetween(0, 0, 0, 1, out);
        // One degree of longitude at the equator: within 0.2% of the answer
        // Android computes on the WGS84 ellipsoid.
        assertEquals(111319.5f, out[0], 200f);
    }
}
