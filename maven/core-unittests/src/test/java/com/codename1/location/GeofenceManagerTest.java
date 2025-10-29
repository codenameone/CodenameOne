package com.codename1.location;

import com.codename1.io.Storage;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeofenceManagerTest extends UITestBase {
    private Storage originalStorage;
    private InMemoryStorage storage;
    private RecordingLocationManager locationManager;
    private GeofenceManager manager;

    @BeforeEach
    void setupManager() throws Exception {
        originalStorage = Storage.getInstance();
        storage = new InMemoryStorage();
        Storage.setStorageInstance(storage);
        locationManager = new RecordingLocationManager();
        Location origin = new Location(0.0, 0.0);
        locationManager.setCurrentLocation(origin);
        locationManager.setLastLocation(origin);
        implementation.setLocationManager(locationManager);
        resetSingleton();
        manager = GeofenceManager.getInstance();
        locationManager.clearRecords();
    }

    @AfterEach
    void tearDownManager() throws Exception {
        if (manager != null) {
            manager.clear();
        }
        Storage.setStorageInstance(originalStorage);
        resetSingleton();
    }

    @FormTest
    void addStoresGeofencesAndUpdatesSize() {
        Geofence first = createGeofence("first", 0.001, 0.0, 120, -1L);
        Geofence second = createGeofence("second", 0.002, 0.0, 80, -1L);

        manager.add(first, second);

        assertEquals(2, manager.size());
        assertTrue(manager.asMap().containsKey("first"));
        assertTrue(manager.asMap().containsKey("second"));
    }

    @FormTest
    void removeAndClearDeleteTrackedGeofences() {
        Geofence first = createGeofence("one", 0.0, 0.001, 100, -1L);
        Geofence second = createGeofence("two", 0.0, 0.002, 100, -1L);
        manager.add(first, second);

        manager.remove("one");
        assertEquals(1, manager.size());
        assertFalse(manager.asMap().containsKey("one"));

        manager.clear();
        assertEquals(0, manager.size());
    }

    @FormTest
    void updateActivatesGeofencesWithinBubble() {
        Geofence near = createGeofence("near", 0.001, 0.001, 50, -1L);
        Geofence far = createGeofence("far", 2.0, 2.0, 100, -1L);
        manager.add(near, far);
        locationManager.clearRecords();

        manager.update(1000);

        assertTrue(locationManager.addedIds.contains("near"));
        assertTrue(manager.isCurrentlyActive("near"));
        assertFalse(manager.isCurrentlyActive("far"));
        assertFalse(locationManager.removedIds.contains("near"));
    }

    @FormTest
    void updateWithNullLocationRegistersBackgroundListener() {
        locationManager.setCurrentLocation(null);
        locationManager.clearRecords();

        manager.update(5000);

        assertEquals(GeofenceManager.Listener.class, locationManager.lastBackgroundListener);
        assertTrue(locationManager.backgroundBound);
        assertTrue(locationManager.addedIds.isEmpty());
    }

    @FormTest
    void listenerClassPersistsAndClears() {
        manager.setListenerClass(TestGeofenceListener.class);
        assertSame(TestGeofenceListener.class, manager.getListenerClass());

        manager.setListenerClass(null);
        assertNull(manager.getListenerClass());
    }

    @FormTest
    void asSortedListOrdersByProximity() {
        Location reference = new Location(0.0, 0.0);
        locationManager.setLastLocation(reference);
        Geofence close = createGeofence("close", 0.001, 0.0, 30, -1L);
        Geofence far = createGeofence("farther", 0.01, 0.01, 30, -1L);
        manager.add(close, far);

        List<Geofence> sorted = manager.asSortedList();

        assertEquals("close", sorted.get(0).getId());
        assertEquals(2, sorted.size());
    }

    @FormTest
    void isBubbleRecognizesBubbleId() {
        assertTrue(manager.isBubble("$AsyncGeoStreamer.bubble"));
        assertFalse(manager.isBubble("not-bubble"));
    }

    private Geofence createGeofence(String id, double lat, double lng, int radius, long expiration) {
        Location location = new Location(lat, lng);
        return new Geofence(id, location, radius, expiration);
    }

    private void resetSingleton() throws Exception {
        Field field = GeofenceManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    private static class InMemoryStorage extends Storage {
        private final Map<String, Object> values = new HashMap<String, Object>();

        @Override
        public Object readObject(String name) {
            return values.get(name);
        }

        @Override
        public boolean writeObject(String name, Object value) {
            values.put(name, value);
            return true;
        }

        @Override
        public void deleteStorageFile(String name) {
            values.remove(name);
        }

        @Override
        public boolean exists(String name) {
            return values.containsKey(name);
        }
    }

    private static class RecordingLocationManager extends LocationManager {
        private Location currentLocation;
        private Location lastLocation;
        private final List<String> addedIds = new ArrayList<String>();
        private final List<String> removedIds = new ArrayList<String>();
        private final List<Class> backgroundChanges = new ArrayList<Class>();
        private Class lastBackgroundListener;
        private boolean backgroundBound;
        private boolean backgroundCleared;

        void setCurrentLocation(Location currentLocation) {
            this.currentLocation = currentLocation;
        }

        void setLastLocation(Location lastLocation) {
            this.lastLocation = lastLocation;
        }

        void clearRecords() {
            addedIds.clear();
            removedIds.clear();
            backgroundChanges.clear();
            backgroundBound = false;
            backgroundCleared = false;
            lastBackgroundListener = null;
        }

        @Override
        public Location getCurrentLocation() {
            return currentLocation;
        }

        @Override
        public Location getCurrentLocationSync(long timeout) {
            return currentLocation;
        }

        @Override
        public Location getLastKnownLocation() {
            return lastLocation;
        }

        @Override
        protected void bindListener() {
        }

        @Override
        protected void clearListener() {
        }

        @Override
        public void addGeoFencing(Class GeofenceListenerClass, Geofence gf) {
            addedIds.add(gf.getId());
        }

        @Override
        public void removeGeoFencing(String id) {
            removedIds.add(id);
        }

        @Override
        public void setBackgroundLocationListener(Class locationListener) {
            super.setBackgroundLocationListener(locationListener);
            backgroundChanges.add(locationListener);
            lastBackgroundListener = locationListener;
        }

        @Override
        protected void bindBackgroundListener() {
            backgroundBound = true;
            backgroundCleared = false;
        }

        @Override
        protected void clearBackgroundListener() {
            backgroundCleared = true;
            backgroundBound = false;
        }
    }

    public static class TestGeofenceListener implements GeofenceListener {
        @Override
        public void onExit(String id) {
        }

        @Override
        public void onEntered(String id) {
        }
    }
}
