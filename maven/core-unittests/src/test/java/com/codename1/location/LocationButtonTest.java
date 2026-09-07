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
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The two halves of [LocationButton]: the platform's own control where there is
/// one, and the ordinary button everywhere else. The fake implementation decides
/// which, so both are reachable from a unit test.
class LocationButtonTest extends UITestBase {

    private RecordingLocationManager manager;

    @BeforeEach
    void initManager() {
        manager = new RecordingLocationManager();
        implementation.setLocationManager(manager);
        implementation.setLocationButtonSupported(false);
    }

    @AfterEach
    void resetImplementation() {
        implementation.setLocationButtonSupported(false);
    }

    @FormTest
    void withoutAPlatformControlTheComponentIsAnOrdinaryButton() {
        LocationButton button = showButton(new LocationButton());

        assertFalse(LocationButton.isSystemRendered());
        assertNotNull(findButton(button), "the fallback button should be in place");
        assertNull(findPeer(button));
    }

    @FormTest
    void pressingTheFallbackButtonDeliversTheLocation() {
        Location expected = new Location(1.5, 2.5);
        manager.currentLocation = expected;
        LocationButton button = showButton(new LocationButton());
        List<Location> shared = record(button);

        findButton(button).released();

        assertEquals(1, shared.size());
        assertSame(expected, shared.get(0));
    }

    @FormTest
    void aFallbackWithNoFixStillAnswers() {
        manager.currentLocation = null;
        LocationButton button = new LocationButton();
        // A manager that never reports a fix is what a real timeout looks like;
        // shortened so the test measures the answer rather than the wait.
        button.setTimeout(50);
        showButton(button);
        List<Location> shared = record(button);

        findButton(button).released();

        assertEquals(1, shared.size());
        assertNull(shared.get(0));
    }

    @FormTest
    void noLocationManagerAtAllStillAnswers() {
        implementation.setLocationManager(null);
        LocationButton button = showButton(new LocationButton());
        List<Location> shared = record(button);

        findButton(button).released();

        assertEquals(1, shared.size());
        assertNull(shared.get(0));
    }

    @FormTest
    void aRemovedListenerHearsNothing() {
        manager.currentLocation = new Location(3.0, 4.0);
        LocationButton button = showButton(new LocationButton());
        final List<Location> shared = new ArrayList<Location>();
        LocationSharedListener listener = new LocationSharedListener() {
            public void locationShared(Location location) {
                shared.add(location);
            }
        };
        button.addLocationSharedListener(listener);
        button.removeLocationSharedListener(listener);

        findButton(button).released();

        assertTrue(shared.isEmpty());
    }

    @FormTest
    void whereThePlatformDrawsItTheComponentIsThePeer() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = showButton(
                new LocationButton(LocationButton.TEXT_SHARE_PRECISE_LOCATION));

        assertTrue(LocationButton.isSystemRendered());
        assertNotNull(findPeer(button), "the platform control should be in place");
        assertNull(findButton(button));
        assertEquals(LocationButton.TEXT_SHARE_PRECISE_LOCATION,
                implementation.getLocationButtonTextType());
    }

    @FormTest
    void aGrantedSystemButtonDeliversTheLocation() {
        implementation.setLocationButtonSupported(true);
        Location expected = new Location(6.0, 7.0);
        manager.currentLocation = expected;
        LocationButton button = showButton(new LocationButton());
        List<Location> shared = record(button);

        grant(Boolean.TRUE);

        assertEquals(1, shared.size());
        assertSame(expected, shared.get(0));
    }

    @FormTest
    void aDeclinedSystemButtonDeliversNull() {
        implementation.setLocationButtonSupported(true);
        manager.currentLocation = new Location(6.0, 7.0);
        LocationButton button = showButton(new LocationButton());
        List<Location> shared = record(button);

        grant(Boolean.FALSE);

        assertEquals(1, shared.size());
        assertNull(shared.get(0), "a decline must not reach the location manager");
        assertEquals(0, manager.bindCount);
    }

    @FormTest
    void aFailedSessionFallsBackToAnOrdinaryButton() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = showButton(new LocationButton());
        assertNotNull(findPeer(button));

        // null is the platform saying its own control will not work here, which
        // can arrive without a tap because the session opens on attach.
        grant(null);

        assertNull(findPeer(button));
        assertNotNull(findButton(button), "a dead control must be replaced");

        Location expected = new Location(8.0, 9.0);
        manager.currentLocation = expected;
        List<Location> shared = record(button);
        findButton(button).released();
        assertEquals(1, shared.size());
        assertSame(expected, shared.get(0));
    }

    @FormTest
    void colorsReachThePlatform() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = new LocationButton();
        button.setButtonBackgroundColor(0x112233);
        button.setButtonTextColor(0x445566);
        showButton(button);

        assertEquals(0x112233, implementation.getLocationButtonBackgroundColor());
        assertEquals(0x445566, implementation.getLocationButtonTextColor());
    }

    @FormTest
    void unsetColorsAreLeftToThePlatform() {
        implementation.setLocationButtonSupported(true);
        showButton(new LocationButton());

        assertEquals(-1, implementation.getLocationButtonBackgroundColor());
        assertEquals(-1, implementation.getLocationButtonTextColor());
    }

    @FormTest
    void theTextTypeIsValidated() {
        assertThrows(IllegalArgumentException.class, () -> new LocationButton(-1));
        assertThrows(IllegalArgumentException.class, () -> new LocationButton(6));
        LocationButton button = new LocationButton();
        assertThrows(IllegalArgumentException.class, () -> button.setTextType(99));
        assertEquals(LocationButton.TEXT_USE_PRECISE_LOCATION, button.getTextType());
    }

    @FormTest
    void everyTextTypeLabelsTheFallback() {
        for (int type = LocationButton.TEXT_NONE;
                type <= LocationButton.TEXT_NEAR_YOUR_PRECISE_LOCATION; type++) {
            LocationButton button = showButton(new LocationButton(type));
            Button fallback = findButton(button);
            assertNotNull(fallback);
            if (type == LocationButton.TEXT_NONE) {
                assertEquals("", fallback.getText());
            } else {
                assertFalse(fallback.getText().isEmpty(),
                        "text type " + type + " should carry a label");
            }
        }
    }

    private void grant(Boolean granted) {
        SuccessCallback<Boolean> callback = implementation.getLocationButtonCallback();
        assertNotNull(callback, "the component should have handed the platform a callback");
        callback.onSucess(granted);
        // The component hops to the EDT before it acts on the platform's answer.
        flushSerialCalls();
    }

    private LocationButton showButton(LocationButton button) {
        Form f = new Form("LocationButton");
        f.add(button);
        f.show();
        flushSerialCalls();
        return button;
    }

    private List<Location> record(LocationButton button) {
        final List<Location> shared = new ArrayList<Location>();
        button.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                shared.add(location);
            }
        });
        return shared;
    }

    private static Button findButton(LocationButton button) {
        for (int iter = 0; iter < button.getComponentCount(); iter++) {
            Component c = button.getComponentAt(iter);
            if (c instanceof Button) {
                return (Button) c;
            }
        }
        return null;
    }

    private static PeerComponent findPeer(LocationButton button) {
        for (int iter = 0; iter < button.getComponentCount(); iter++) {
            Component c = button.getComponentAt(iter);
            if (c instanceof PeerComponent) {
                return (PeerComponent) c;
            }
        }
        return null;
    }

    private static class RecordingLocationManager extends LocationManager {
        Location currentLocation;
        int bindCount;

        @Override
        public Location getCurrentLocation() throws IOException {
            return currentLocation;
        }

        @Override
        public Location getLastKnownLocation() {
            return currentLocation;
        }

        @Override
        protected void bindListener() {
            bindCount++;
            LocationListener l = getLocationListener();
            if (l != null && currentLocation != null) {
                setStatus(LocationManager.AVAILABLE);
                l.locationUpdated(currentLocation);
            }
        }

        @Override
        protected void clearListener() {
        }
    }
}
