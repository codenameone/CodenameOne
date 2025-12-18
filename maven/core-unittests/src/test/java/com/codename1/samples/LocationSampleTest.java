package com.codename1.samples;

import com.codename1.location.Location;
import com.codename1.location.LocationManager;
import com.codename1.ui.*;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.junit.UITestBase;
import com.codename1.junit.FormTest;
import static org.junit.jupiter.api.Assertions.*;

public class LocationSampleTest extends UITestBase {

    @FormTest
    public void testLocationSample() {
        MockLocationManager mockLoc = new MockLocationManager();
        implementation.setLocationManager(mockLoc);

        Form hi = new Form("Hi World", BoxLayout.y());
        Button checkIfDetectionAvailable = new Button("Check if GPS Detection Available");
        checkIfDetectionAvailable.addActionListener(e -> {
            String message = LocationManager.getLocationManager().isGPSDetectionSupported() ? "GPS Detection IS supported" : "GPS Detection is NOT supported";
            checkIfDetectionAvailable.putClientProperty("lastMessage", message);
        });

        Button isGPSEnabled = new Button("GPS Status");
        isGPSEnabled.addActionListener(e -> {
            String message = LocationManager.getLocationManager().isGPSEnabled() ? "GPS IS enabled" : "GPS is NOT enabled";
            isGPSEnabled.putClientProperty("lastMessage", message);
        });

        Button getLocation = new Button("Get Location");
        getLocation.addActionListener(e -> {
            Location loc = LocationManager.getLocationManager().getCurrentLocationSync(3000);
            String message = loc != null ? "Location is " + loc : "Location not found";
            getLocation.putClientProperty("lastMessage", message);
        });
        hi.addAll(checkIfDetectionAvailable, isGPSEnabled, getLocation);
        hi.show();
        waitForFormTitle("Hi World");

        tapComponent(checkIfDetectionAvailable);
        assertEquals("GPS Detection IS supported", checkIfDetectionAvailable.getClientProperty("lastMessage"));

        tapComponent(isGPSEnabled);
        assertEquals("GPS IS enabled", isGPSEnabled.getClientProperty("lastMessage"));

        tapComponent(getLocation);
        String locMsg = (String) getLocation.getClientProperty("lastMessage");
        assertTrue(locMsg != null && locMsg.contains("Location is"), "Should find location");
    }

    private void waitForFormTitle(String title) {
        long start = System.currentTimeMillis();
        while(System.currentTimeMillis() - start < 5000) {
            Form f = CN.getCurrentForm();
            if (f != null && title.equals(f.getTitle())) {
                return;
            }
            try { Thread.sleep(50); } catch(Exception e){}
        }
    }

    class MockLocationManager extends LocationManager {
        @Override
        public boolean isGPSDetectionSupported() {
            return true;
        }

        @Override
        public boolean isGPSEnabled() {
            return true;
        }

        @Override
        public Location getCurrentLocationSync(long timeout) {
            Location loc = new Location();
            loc.setLatitude(10.0);
            loc.setLongitude(20.0);
            return loc;
        }

        public void bindGPSListener(com.codename1.location.LocationListener listener) {}

        public void unbindGPSListener(com.codename1.location.LocationListener listener) {}

        @Override
        public Location getCurrentLocation() { return getCurrentLocationSync(-1); }
        @Override
        public Location getLastKnownLocation() { return getCurrentLocation(); }

        @Override
        public void clearListener() {}

        @Override
        public void bindListener() {}
    }
}
