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
import com.codename1.ui.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class LocationManagerTest extends UITestBase {
    private TestLocationManager manager;

    @BeforeEach
    void initManager() throws Exception {
        manager = new TestLocationManager();
        implementation.setLocationManager(manager);
    }

    @AfterEach
    void cleanupManager() throws Exception {
        manager.notifyOnBind = true;
        manager.setLocationListener(null);
        manager.setBackgroundLocationListener(null);
    }

    @FormTest
    void getCurrentLocationSyncWithoutListenerBindsAndReturnsResult() {
        Location expected = new Location(1.0, 2.0);
        manager.setCurrentLocation(expected);

        Location result = manager.getCurrentLocationSync(1000);

        assertSame(expected, result);
        assertEquals(1, manager.bindCount);
        assertEquals(LocationManager.TEMPORARILY_UNAVAILABLE, manager.getStatus());
        assertNull(manager.getStoredRequest());
    }

    @FormTest
    void getCurrentLocationSyncWithExistingListenerUsesCurrentLocationDirectly() throws IOException {
        manager.notifyOnBind = false;
        Location expected = new Location(4.0, 5.0);
        manager.setCurrentLocation(expected);
        manager.setLocationListener(new DummyLocationListener());
        manager.notifyOnBind = true;

        Location result = manager.getCurrentLocationSync(500);

        assertSame(expected, result);
        assertEquals(1, manager.getCurrentLocationCalls);
    }

    @FormTest
    void setLocationListenerWithRequestStoresRequest() {
        manager.notifyOnBind = false;
        LocationRequest request = new LocationRequest();

        manager.setLocationListener(new DummyLocationListener(), request);

        assertSame(request, manager.getStoredRequest());
        assertNotNull(manager.getCurrentListener());
    }

    @FormTest
    void getLastKnownLocationReturnsStoredLocation() {
        Location expected = new Location(9.0, 10.0);
        manager.setLastLocation(expected);

        assertSame(expected, manager.getLastKnownLocation());
    }

    @FormTest
    void setLocationListenerNullClearsRequestAndStatus() {
        manager.notifyOnBind = false;
        LocationRequest request = new LocationRequest();
        manager.setInternalStatus(LocationManager.AVAILABLE);
        manager.setLocationListener(new DummyLocationListener(), request);

        manager.setLocationListener(null);

        assertNull(manager.getStoredRequest());
        assertEquals(LocationManager.TEMPORARILY_UNAVAILABLE, manager.getStatus());
        assertEquals(1, manager.clearCount);
        assertNull(manager.getCurrentListener());
    }

    @FormTest
    void replacingLocationListenerClearsPreviousListener() {
        manager.notifyOnBind = false;
        manager.setLocationListener(new DummyLocationListener(), new LocationRequest());

        manager.setLocationListener(new DummyLocationListener());

        assertEquals(1, manager.clearCount);
        assertNotNull(manager.getCurrentListener());
    }

    @FormTest
    void backgroundLocationListenerBindsAndClears() {
        manager.setBackgroundLocationListener(DummyLocationListener.class);
        assertTrue(manager.backgroundBound);
        assertEquals(DummyLocationListener.class, manager.getCurrentBackgroundListener());

        manager.setBackgroundLocationListener(null);
        assertTrue(manager.backgroundCleared);
        assertNull(manager.getCurrentBackgroundListener());
    }

    @FormTest
    void isGPSEnabledThrowsByDefault() {
        assertThrows(RuntimeException.class, () -> manager.isGPSEnabled());
    }

    private Object getDisplayField(String name) throws Exception {
        Field field = Display.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(Display.getInstance());
    }

    @Test
    void aTimedOutRequestReleasesTheSlotWithoutClearingThePlatform() {
        // A timed-out request used to stay installed forever, so the NEXT
        // getCurrentLocationSync saw a non-null listener and took the
        // getCurrentLocation() path instead of starting a fresh timed request.
        //
        // Releasing the field fixes that. Calling the port's clear would fix
        // it too and cost more: on Android with Play Services clearListener()
        // is ASYNCHRONOUS, and a retry arriving as connectivity returns can
        // bind first, after which the late clear removes the retry's own
        // subscription. Nothing in core can serialize against that, so core
        // does not start it -- the next bind replaces the listener inside one
        // port call instead.
        TestLocationManager manager = new TestLocationManager();
        manager.notifyOnBind = false;
        manager.setCurrentLocation(null);

        Location timedOut = manager.getCurrentLocationSync(50);
        assertNull(timedOut, "sanity: the request must time out");
        assertEquals(1, manager.bindCount, "sanity: it bound once");
        assertEquals(0, manager.clearCount,
                "the timeout must not start a platform clear");
        assertNull(manager.getLocationListener(),
                "but the slot is free, which is what the routing reads");

        // And the retry starts a fresh timed request rather than falling
        // through to getCurrentLocation().
        manager.getCurrentLocationSync(50);
        assertEquals(2, manager.bindCount, "the retry binds again");
        assertEquals(0, manager.getCurrentLocationCalls,
                "and does not take the already-listening path");
    }

    private static class DummyLocationListener implements LocationListener {
        @Override
        public void locationUpdated(Location location) {
        }

        @Override
        public void providerStateChanged(int newState) {
        }
    }

    private static class TestLocationManager extends LocationManager {
        private Location currentLocation;
        private Location lastLocation;
        private Class backgroundListenerClass;
        int bindCount;
        int clearCount;
        int getCurrentLocationCalls;
        boolean notifyOnBind = true;
        boolean backgroundBound;
        boolean backgroundCleared;

        void setCurrentLocation(Location currentLocation) {
            this.currentLocation = currentLocation;
        }

        void setLastLocation(Location lastLocation) {
            this.lastLocation = lastLocation;
        }

        @Override
        public Location getCurrentLocation() throws IOException {
            getCurrentLocationCalls++;
            return currentLocation;
        }

        @Override
        public Location getLastKnownLocation() {
            return lastLocation;
        }

        @Override
        protected void bindListener() {
            bindCount++;
            if (notifyOnBind) {
                LocationListener l = getLocationListener();
                if (l != null && currentLocation != null) {
                    setStatus(LocationManager.AVAILABLE);
                    l.locationUpdated(currentLocation);
                }
            }
        }

        @Override
        protected void clearListener() {
            clearCount++;
        }

        @Override
        protected void bindBackgroundListener() {
            backgroundBound = true;
            backgroundCleared = false;
            backgroundListenerClass = getBackgroundLocationListener();
        }

        @Override
        protected void clearBackgroundListener() {
            backgroundCleared = true;
            backgroundBound = false;
            backgroundListenerClass = null;
        }

        LocationListener getCurrentListener() {
            return getLocationListener();
        }

        LocationRequest getStoredRequest() {
            return getRequest();
        }

        void setInternalStatus(int status) {
            setStatus(status);
        }

        Class getCurrentBackgroundListener() {
            return backgroundListenerClass;
        }
    }
}
