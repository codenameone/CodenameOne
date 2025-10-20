package com.codename1.location;

import com.codename1.test.UITestBase;
import com.codename1.ui.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class LocationManagerTest extends UITestBase {
    private TestLocationManager manager;
    private Object originalEdt;

    @BeforeEach
    void initManager() throws Exception {
        manager = new TestLocationManager();
        when(implementation.getLocationManager()).thenReturn(manager);
        originalEdt = getDisplayField("edt");
        setDisplayField("edt", new Thread());
    }

    @AfterEach
    void cleanupManager() throws Exception {
        manager.notifyOnBind = true;
        manager.setLocationListener(null);
        manager.setBackgroundLocationListener(null);
        setDisplayField("edt", originalEdt);
    }

    @Test
    void getCurrentLocationSyncWithoutListenerBindsAndReturnsResult() {
        Location expected = new Location(1.0, 2.0);
        manager.setCurrentLocation(expected);

        Location result = manager.getCurrentLocationSync(1000);

        assertSame(expected, result);
        assertEquals(1, manager.bindCount);
        assertEquals(LocationManager.TEMPORARILY_UNAVAILABLE, manager.getStatus());
        assertNull(manager.getStoredRequest());
    }

    @Test
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

    @Test
    void setLocationListenerWithRequestStoresRequest() {
        manager.notifyOnBind = false;
        LocationRequest request = new LocationRequest();

        manager.setLocationListener(new DummyLocationListener(), request);

        assertSame(request, manager.getStoredRequest());
        assertNotNull(manager.getCurrentListener());
    }

    @Test
    void getLastKnownLocationReturnsStoredLocation() {
        Location expected = new Location(9.0, 10.0);
        manager.setLastLocation(expected);

        assertSame(expected, manager.getLastKnownLocation());
    }

    @Test
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

    @Test
    void replacingLocationListenerClearsPreviousListener() {
        manager.notifyOnBind = false;
        manager.setLocationListener(new DummyLocationListener(), new LocationRequest());

        manager.setLocationListener(new DummyLocationListener());

        assertEquals(1, manager.clearCount);
        assertNotNull(manager.getCurrentListener());
    }

    @Test
    void backgroundLocationListenerBindsAndClears() {
        manager.setBackgroundLocationListener(DummyLocationListener.class);
        assertTrue(manager.backgroundBound);
        assertEquals(DummyLocationListener.class, manager.getCurrentBackgroundListener());

        manager.setBackgroundLocationListener(null);
        assertTrue(manager.backgroundCleared);
        assertNull(manager.getCurrentBackgroundListener());
    }

    @Test
    void isGPSEnabledThrowsByDefault() {
        assertThrows(RuntimeException.class, () -> manager.isGPSEnabled());
    }

    private Object getDisplayField(String name) throws Exception {
        Field field = Display.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(Display.getInstance());
    }

    private void setDisplayField(String name, Object value) throws Exception {
        Field field = Display.class.getDeclaredField(name);
        field.setAccessible(true);
        if ((field.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) {
            field.set(null, value);
        } else {
            field.set(Display.getInstance(), value);
        }
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
