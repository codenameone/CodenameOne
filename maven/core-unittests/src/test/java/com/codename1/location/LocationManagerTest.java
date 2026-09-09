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

    /// A request that times out has to leave the manager as it found it. Both
    /// of LL's callbacks clear the listener; run() only breaks its wait loop, so
    /// without this the platform keeps its location updates registered and the
    /// next call answers from getCurrentLocation() instead of waiting for a
    /// fresh fix.
    @FormTest
    void getCurrentLocationSyncClearsTheListenerWhenItTimesOut() {
        manager.notifyOnBind = false;
        manager.setCurrentLocation(null);

        Location result = manager.getCurrentLocationSync(50);

        assertNull(result, "a request that never got a fix has no location");
        assertNull(manager.getCurrentListener(),
                "the timed-out listener must not stay installed");
        assertEquals(1, manager.clearCount, "and the platform must be told once");
    }

    /// The follow-up the leak would have broken: a second request still binds a
    /// fresh listener rather than taking the already-listening shortcut.
    @FormTest
    void aRequestAfterATimeoutStillWaitsForAFreshFix() {
        manager.notifyOnBind = false;
        manager.getCurrentLocationSync(50);
        int bindsAfterTimeout = manager.bindCount;

        manager.notifyOnBind = true;
        Location expected = new Location(5.0, 6.0);
        manager.setCurrentLocation(expected);

        assertSame(expected, manager.getCurrentLocationSync(1000));
        assertEquals(bindsAfterTimeout + 1, manager.bindCount,
                "the second request must bind again, not reuse a stale listener");
    }

    /// A one-shot request that times out must not take a subscription with it.
    /// invokeAndBlock keeps the EDT running while it waits, so application code
    /// is free to start tracking in the middle of one.
    @FormTest
    void aTimeoutDoesNotClearAListenerInstalledWhileItWaited() {
        manager.notifyOnBind = false;
        final DummyLocationListener tracker = new DummyLocationListener();
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                manager.setLocationListener(tracker);
            }
        });

        Location result = manager.getCurrentLocationSync(500);

        assertNull(result, "the one-shot request still timed out");
        assertSame(tracker, manager.getCurrentListener(),
                "the subscription installed during the wait must survive");
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
