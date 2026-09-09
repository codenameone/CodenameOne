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
import com.codename1.ui.Display;
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
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
        // LocationManager's listener is static, so a test that installs one --
        // playing an application that tracks -- would otherwise leave every
        // later test on the tracked path, where nothing binds at all.
        manager.setLocationListener(null);
    }

    @AfterEach
    void resetImplementation() {
        implementation.setLocationButtonSupported(false);
        implementation.setLocationButtonReady(true);
        manager.setLocationListener(null);
    }

    @FormTest
    void withoutAPlatformControlTheComponentIsAnOrdinaryButton() {
        LocationButton button = showButton(new LocationButton());

        assertFalse(button.isSystemRendered());
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

        assertTrue(button.isSystemRendered());
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

    /// A setter that arrives after the first show has to reach the platform's
    /// control, which is configured when its session opens. Without a rebuild
    /// it would move the field and change nothing on screen.
    @FormTest
    void changingTheTextTypeAfterShowingRebuildsThePlatformControl() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = showButton(new LocationButton());
        assertEquals(LocationButton.TEXT_USE_PRECISE_LOCATION,
                implementation.getLocationButtonTextType());
        PeerComponent first = findPeer(button);
        assertNotNull(first);

        button.setTextType(LocationButton.TEXT_NEAR_MY_PRECISE_LOCATION);

        assertEquals(LocationButton.TEXT_NEAR_MY_PRECISE_LOCATION,
                implementation.getLocationButtonTextType());
        PeerComponent second = findPeer(button);
        assertNotNull(second, "the control should still be in place");
        assertNotSame(first, second, "the control should have been rebuilt");
    }

    @FormTest
    void changingAColorAfterShowingRebuildsThePlatformControl() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = showButton(new LocationButton());
        assertEquals(-1, implementation.getLocationButtonBackgroundColor());

        button.setButtonBackgroundColor(0x00ff00);

        assertEquals(0x00ff00, implementation.getLocationButtonBackgroundColor());
        assertNotNull(findPeer(button));
    }

    /// Setting the same value is not a reason to tear a live control down.
    @FormTest
    void settingTheSameTextTypeChangesNothing() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = showButton(new LocationButton());
        PeerComponent first = findPeer(button);

        button.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);

        assertSame(first, findPeer(button));
    }

    /// The platform already said its control does not work here; a later setter
    /// must not put the dead control back.
    @FormTest
    void aSetterAfterAFailedSessionKeepsTheFallback() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = showButton(new LocationButton());
        grant(null);
        assertNull(findPeer(button));

        button.setTextType(LocationButton.TEXT_SHARE_PRECISE_LOCATION);

        assertNull(findPeer(button), "a failed platform must not be asked again");
        assertNotNull(findButton(button));
    }

    /// The whole point of making this an instance question: on a platform that
    /// HAS the control, a failed session means this button is not it.
    @FormTest
    void isSystemRenderedFollowsTheControlNotThePlatform() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = showButton(new LocationButton());
        assertTrue(button.isSystemRendered());

        grant(null);

        assertTrue(implementation.isLocationButtonSupported(),
                "the platform still claims the control");
        assertFalse(button.isSystemRendered(),
                "but this button is no longer showing it");
    }

    /// A control that was created but whose session never opened is present and
    /// blank, and nothing reports a failure to fall back from. The component
    /// must not call that the system's control.
    @FormTest
    void aControlWithNoSessionIsNotSystemRendered() {
        implementation.setLocationButtonSupported(true);
        implementation.setLocationButtonReady(false);
        LocationButton button = showButton(new LocationButton());

        assertNotNull(findPeer(button), "the peer is built before its session opens");
        assertFalse(button.isSystemRendered(),
                "a blank control is not the system rendering one");

        implementation.setLocationButtonReady(true);
        assertTrue(button.isSystemRendered(),
                "and it is once the platform starts drawing");
    }

    @FormTest
    void isSystemRenderedIsFalseBeforeTheComponentIsShown() {
        implementation.setLocationButtonSupported(true);
        assertFalse(new LocationButton().isSystemRendered());
    }

    /// A control a setter replaced can still have a callback queued on the
    /// platform's side. It must not act on the control that took its place.
    @FormTest
    void aCallbackFromAReplacedControlIsIgnored() {
        implementation.setLocationButtonSupported(true);
        manager.currentLocation = new Location(1.0, 2.0);
        LocationButton button = showButton(new LocationButton());
        SuccessCallback<Boolean> stale = implementation.getLocationButtonCallback();
        assertNotNull(stale);

        button.setTextType(LocationButton.TEXT_SHARE_PRECISE_LOCATION);
        PeerComponent live = findPeer(button);
        assertNotNull(live);
        List<Location> shared = record(button);

        // A late failure from the control that was replaced.
        stale.onSucess(null);
        flushSerialCalls();
        assertSame(live, findPeer(button),
                "a replaced control must not tear down the one that replaced it");
        assertTrue(button.isSystemRendered());

        // And a late grant from it must not acquire anything either.
        stale.onSucess(Boolean.TRUE);
        flushSerialCalls();
        assertTrue(shared.isEmpty(), "a replaced control must not deliver a location");
    }

    /// The floor is a minimum touch target, which has two axes. TEXT_NONE is
    /// the icon-only form and the one with the least natural width.
    @FormTest
    void neitherAxisFallsBelowTheTouchTarget() {
        int floor = Display.getInstance().convertToPixels(8f);
        for (int type = LocationButton.TEXT_NONE;
                type <= LocationButton.TEXT_NEAR_YOUR_PRECISE_LOCATION; type++) {
            LocationButton button = new LocationButton(type);
            assertTrue(button.getPreferredSize().getWidth() >= floor,
                    "text type " + type + " is narrower than the touch target");
            assertTrue(button.getPreferredSize().getHeight() >= floor,
                    "text type " + type + " is shorter than the touch target");
        }
    }

    /// A grant that lands after the component has left the form belongs to a
    /// control the user has navigated away from.
    @FormTest
    void aGrantThatLandsAfterTheComponentLeftTheFormIsDropped() {
        implementation.setLocationButtonSupported(true);
        manager.currentLocation = new Location(11.0, 12.0);
        LocationButton button = showButton(new LocationButton());
        SuccessCallback<Boolean> callback = implementation.getLocationButtonCallback();
        List<Location> shared = record(button);

        // Off the form, exactly as a navigation would leave it.
        button.remove();
        flushSerialCalls();

        callback.onSucess(Boolean.TRUE);
        flushSerialCalls();

        assertTrue(shared.isEmpty(),
                "a control the user navigated away from must not deliver a location");
        assertEquals(0, manager.bindCount, "and must not have asked for one");
    }

    /// setEnabled before the first show reached a container with no children,
    /// so the button built afterwards came up live and still acquired on a tap.
    @FormTest
    void aButtonDisabledBeforeItIsShownComesUpDisabled() {
        manager.currentLocation = new Location(1.0, 2.0);
        LocationButton button = new LocationButton();
        button.setEnabled(false);
        showButton(button);
        List<Location> shared = record(button);

        Button fallback = findButton(button);
        assertNotNull(fallback);
        assertFalse(fallback.isEnabled(), "the fallback must come up disabled");

        // Through the pointer routing rather than Button.released(), which is a
        // direct call that never consults the enabled flag -- the check lives in
        // event delivery, so only a real tap exercises it.
        tapComponent(fallback);
        assertTrue(shared.isEmpty(), "a disabled button must not acquire");
    }

    /// A component disabled while the platform control was up must not come
    /// back live when a failed session swaps in the ordinary button.
    @FormTest
    void aDisabledButtonStaysDisabledThroughTheFallback() {
        implementation.setLocationButtonSupported(true);
        manager.currentLocation = new Location(3.0, 4.0);
        LocationButton button = showButton(new LocationButton());
        button.setEnabled(false);
        List<Location> shared = record(button);

        grant(null);

        Button fallback = findButton(button);
        assertNotNull(fallback, "the failed control is replaced by the button");
        assertFalse(fallback.isEnabled(),
                "and it inherits the disabled state the peer was carrying");

        tapComponent(fallback);
        assertTrue(shared.isEmpty(), "a disabled fallback must not acquire");
    }

    /// The system's control is drawn by another process, so a disabled component
    /// cannot stop the tap or the consent flow. It can refuse to act on the
    /// answer, and that is the part the application asked for: no location, no
    /// listener.
    @FormTest
    void aDisabledSystemRenderedButtonDoesNotActOnAGrant() {
        implementation.setLocationButtonSupported(true);
        manager.currentLocation = new Location(9.0, 10.0);
        LocationButton button = showButton(new LocationButton());
        assertTrue(button.isSystemRendered(),
                "the platform control is what this test is about");
        button.setEnabled(false);
        List<Location> shared = record(button);

        // The platform granted: the user tapped a control this component could
        // not withhold and answered the system's prompt.
        grant(Boolean.TRUE);

        assertTrue(shared.isEmpty(),
                "a disabled button must not deliver a location it was told not to get");
    }

    /// The same for a decline, which would otherwise report a null location and
    /// look to the application like a real answer from a live button.
    @FormTest
    void aDisabledSystemRenderedButtonDoesNotReportADecline() {
        implementation.setLocationButtonSupported(true);
        LocationButton button = showButton(new LocationButton());
        button.setEnabled(false);
        List<Location> shared = record(button);

        grant(Boolean.FALSE);

        assertTrue(shared.isEmpty(), "a disabled button reports nothing at all");
    }

    /// And the ordinary case still works, so the guard is not disabling
    /// everything.
    @FormTest
    void aButtonThatWasNeverDisabledComesUpEnabled() {
        LocationButton button = showButton(new LocationButton());
        assertTrue(findButton(button).isEnabled());
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

    /// Two buttons on one form, both granted, the second while the first is
    /// still waiting. invokeAndBlock keeps the EDT pumping, so this interleaving
    /// is ordinary rather than exotic -- and the one-shot wait is not reentrant:
    /// the first request holds LocationManager's listener, so the second's
    /// getCurrentLocationSync would answer from the last known location and
    /// report it as the fresh fix its own tap authorised.
    @FormTest
    void twoButtonsGrantedTogetherShareTheOneFix() {
        implementation.setLocationButtonSupported(true);
        Location expected = new Location(11.0, 12.0);
        manager.currentLocation = expected;
        // What the second button gets if it asks on its own: the listener the
        // first request installed sends it down getCurrentLocation() instead of
        // a wait. Same object as the fix would make the two indistinguishable
        // and the test would pass either way -- it did, before this line.
        Location stale = new Location(1.0, 2.0);
        manager.staleLocation = stale;

        Form f = new Form("two");
        LocationButton first = new LocationButton();
        LocationButton second = new LocationButton();
        f.add(first);
        f.add(second);
        f.show();
        flushSerialCalls();

        List<Location> firstShared = record(first);
        List<Location> secondShared = record(second);
        List<SuccessCallback<Boolean>> callbacks =
                implementation.getLocationButtonCallbacks();
        assertEquals(2, callbacks.size(), "each button gets its own callback");

        final SuccessCallback<Boolean> secondCallback = callbacks.get(1);
        manager.duringBind = new Runnable() {
            public void run() {
                secondCallback.onSucess(Boolean.TRUE);
                flushSerialCalls();
            }
        };

        callbacks.get(0).onSucess(Boolean.TRUE);
        flushSerialCalls();

        assertEquals(1, manager.bindCount,
                "one fix is fetched, not one per button");
        assertEquals(1, firstShared.size(), "the first button is told once");
        assertEquals(1, secondShared.size(), "and so is the second");
        assertSame(expected, firstShared.get(0));
        assertSame(expected, secondShared.get(0),
                "the joiner gets the fix, not the last-known stand-in "
                        + "getCurrentLocationSync would have handed it");
        assertNotSame(stale, secondShared.get(0));
    }

    /// A button that joins an acquisition keeps its own timeout. Here the one
    /// that starts the request gives up quickly and the one that joins is
    /// willing to wait much longer: the short one must not take the long one's
    /// patience, and the long one must not be handed the short one's null and
    /// told it is an answer.
    ///
    /// The bind count is what tells the two apart. One shared wait binds once
    /// and answers everybody with the leader's result; honouring each deadline
    /// means a second round for whoever still has time left.
    @FormTest
    void aJoiningButtonKeepsItsOwnTimeout() {
        implementation.setLocationButtonSupported(true);
        // Nothing ever arrives, so every round runs to its deadline.
        manager.currentLocation = null;

        Form f = new Form("timeouts");
        LocationButton quick = new LocationButton();
        quick.setTimeout(50);
        LocationButton patient = new LocationButton();
        patient.setTimeout(900);
        f.add(quick);
        f.add(patient);
        f.show();
        flushSerialCalls();

        List<Location> quickShared = record(quick);
        List<Location> patientShared = record(patient);
        List<SuccessCallback<Boolean>> callbacks =
                implementation.getLocationButtonCallbacks();
        assertEquals(2, callbacks.size());

        final SuccessCallback<Boolean> patientCallback = callbacks.get(1);
        manager.duringBind = new Runnable() {
            public void run() {
                patientCallback.onSucess(Boolean.TRUE);
                flushSerialCalls();
            }
        };

        callbacks.get(0).onSucess(Boolean.TRUE);
        flushSerialCalls();

        assertEquals(2, manager.bindCount,
                "the patient button gets a round of its own after the quick "
                        + "one's deadline, rather than the quick one's answer");
        assertEquals(1, quickShared.size(), "the quick button is answered once");
        assertNull(quickShared.get(0), "and with nothing, which is its timeout");
        assertEquals(1, patientShared.size(), "so is the patient one");
        assertNull(patientShared.get(0));
    }

    /// The case a shared wait cannot serve at all: the button that starts the
    /// request never times out, so the round it owns never ends on its own. A
    /// button that joins it wanting to give up after a moment used to wait for
    /// ever -- the round is not bounded, so being served "at the end of the
    /// round" was no answer. Its own deadline is kept by a timer, which asks
    /// nothing of the request in flight.
    @FormTest
    void aFiniteJoinerIsNotHeldByALeaderThatNeverTimesOut() {
        implementation.setLocationButtonSupported(true);
        // Nothing arrives, so the leader's -1 round would run for ever.
        manager.currentLocation = null;

        Form f = new Form("forever");
        LocationButton forever = new LocationButton();
        forever.setTimeout(-1);
        LocationButton brief = new LocationButton();
        brief.setTimeout(60);
        f.add(forever);
        f.add(brief);
        f.show();
        flushSerialCalls();

        List<Location> briefShared = record(brief);
        List<SuccessCallback<Boolean>> callbacks =
                implementation.getLocationButtonCallbacks();

        final SuccessCallback<Boolean> briefCallback = callbacks.get(1);
        manager.duringBind = new Runnable() {
            public void run() {
                briefCallback.onSucess(Boolean.TRUE);
                flushSerialCalls();
                // The leader is still inside its endless wait here; the EDT
                // pumping under invokeAndBlock is what lets the timer fire.
                long until = System.currentTimeMillis() + 3000;
                while (briefShared.isEmpty() && System.currentTimeMillis() < until) {
                    flushSerialCalls();
                }
                // Let the leader's round end so the test can finish.
                manager.currentLocation = new Location(1.0, 1.0);
            }
        };

        callbacks.get(0).onSucess(Boolean.TRUE);
        flushSerialCalls();

        assertEquals(1, briefShared.size(),
                "the brief button is answered on its own deadline, not the "
                        + "leader's, which never arrives");
        assertNull(briefShared.get(0), "and its answer is its timeout");
    }

    /// An application that is already tracking owns LocationManager's single
    /// listener, so getCurrentLocationSync does not wait at all: it answers from
    /// getCurrentLocation(), which hands back whatever the tracker cached. That
    /// is the right answer to "where are we" and the wrong one to a tap, which
    /// asks where the user is NOW. The button waits for the cache to move.
    @FormTest
    void aStaleTrackedFixIsNotTheAnswerToATap() {
        implementation.setLocationButtonSupported(true);
        Location old = new Location(1.0, 2.0);
        old.setTimeStamp(System.currentTimeMillis() - 600000);
        Location fresh = new Location(7.0, 8.0);
        fresh.setTimeStamp(System.currentTimeMillis());
        manager.currentLocation = old;
        // The application is tracking, which is what sends the button down the
        // cached-answer path.
        manager.setLocationListener(new LocationListener() {
            public void locationUpdated(Location location) {
            }

            public void providerStateChanged(int newState) {
            }
        });

        LocationButton button = new LocationButton();
        button.setTimeout(3000);
        Form f = new Form("tracked");
        f.add(button);
        f.show();
        flushSerialCalls();
        List<Location> shared = record(button);

        // The tracker delivers a new fix a moment after the tap.
        manager.duringCurrentLocation = new Runnable() {
            public void run() {
                manager.currentLocation = fresh;
            }
        };

        grant(Boolean.TRUE);

        assertEquals(1, shared.size(), "the button is answered");
        assertSame(fresh, shared.get(0),
                "with the fix that arrived after the tap, not the one the "
                        + "tracker had cached ten minutes earlier");
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

        /// What getCurrentLocation() answers when it differs from the fix that
        /// bindListener delivers. LocationManager serves a request made while
        /// another one holds its listener from here instead of waiting, so a
        /// test can only tell the two apart when they are different objects.
        Location staleLocation;

        /// Runs on each read of the cache, so a test can play the tracker
        /// delivering an update while the button waits.
        Runnable duringCurrentLocation;

        @Override
        public Location getCurrentLocation() throws IOException {
            // The answer is decided BEFORE the hook runs, so the hook models a
            // tracker delivering an update after this read rather than during
            // it. Running it first made the very first read return the new fix,
            // which no amount of waiting is needed for -- and a test that could
            // not tell whether any waiting happened at all.
            Location answer = staleLocation != null ? staleLocation : currentLocation;
            if (duringCurrentLocation != null) {
                Runnable r = duringCurrentLocation;
                duringCurrentLocation = null;
                r.run();
            }
            return answer;
        }

        @Override
        public Location getLastKnownLocation() {
            return currentLocation;
        }

        /// Runs inside the one-shot wait, which is the only place a second
        /// button's grant can land while the first is still acquiring.
        Runnable duringBind;

        @Override
        protected void bindListener() {
            bindCount++;
            if (duringBind != null) {
                Runnable r = duringBind;
                duringBind = null;
                r.run();
            }
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
