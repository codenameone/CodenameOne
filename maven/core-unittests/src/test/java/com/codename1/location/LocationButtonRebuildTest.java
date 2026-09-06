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

import com.codename1.junit.UITestBase;
import com.codename1.location.spi.LocationButtonBridge;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Button;
import com.codename1.ui.Display;
import com.codename1.ui.PeerComponent;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A setter that changes the label or the colours REPLACES the platform control,
 * because it takes those at construction. The native view that leaves the screen
 * keeps the listeners it was built with -- they point at the component, not at
 * the peer -- and nothing unregisters them.
 *
 * <p>So a session that died after its button had already been replaced still
 * retired the healthy control that took its place, and a permission result from
 * the retired view was served as though the visible button had produced it.</p>
 */
class LocationButtonRebuildTest extends UITestBase {

    /** Stands in for the platform's view: a peer, with nothing behind it. */
    private static final class FakePeer extends PeerComponent {
        private FakePeer() {
            super(new Object());
        }
    }

    /** One built control: the callbacks it was handed, kept for the test. */
    private static final class Session {
        private SuccessCallback<Boolean> onResult;
        private Runnable onUnavailable;
    }

    /** A bridge that builds peers and remembers what each one was given. */
    private static final class RecordingBridge implements LocationButtonBridge {
        private final List<Session> sessions = new ArrayList<Session>();

        public boolean isSupported() {
            return true;
        }

        public void setButtonEnabled(PeerComponent button, boolean enabled) {
        }

        /** Whatever the test installed, or null: a platform with nothing. */
        private LocationManager granted;

        /** How often the GRANTED path was taken, which is the thing tested. */
        private int grantedAsks;

        public LocationManager getGrantedLocationManager() {
            grantedAsks++;
            return granted;
        }

        /** When false the bridge answers null, so the caller falls back. */
        private boolean building = true;

        /** When true createButton throws, as a failing port would. */
        private boolean throwing;

        public PeerComponent createButton(int textType, int backgroundColor,
                int textColor, SuccessCallback<Boolean> onResult,
                Runnable onUnavailable) {
            if (throwing) {
                throw new IllegalStateException("no native view");
            }
            if (!building) {
                return null;
            }
            Session session = new Session();
            session.onResult = onResult;
            session.onUnavailable = onUnavailable;
            sessions.add(session);
            // NOT PeerComponent.create: that asks the implementation to make a
            // native peer, this module's implementation does not support one,
            // and createSystemButton catches the throw and quietly falls back
            // to the ordinary button. The component then holds no peer at all
            // and the test measures the fallback path instead of the one it
            // names -- which is what it did, silently, until the generation
            // counter came out two higher than there were controls.
            return new FakePeer();
        }
    }

    /**
     * A manager whose lookup PARKS until the test releases it, the way a real
     * one does while the platform is finding a fix.
     *
     * <p>Through {@code invokeAndBlock}, not a plain wait: the request runs on
     * the EDT, and stopping the EDT dead would keep the second button's own
     * callback from ever being delivered -- which is the state this test needs
     * to reach.</p>
     */
    private static final class ParkingManager extends LocationManager {
        private final Object lock = new Object();
        private boolean released;
        /** Once open, every parked and future lookup runs straight through. */
        private boolean open;
        private int lookups;

        /** What a completed lookup hands back. */
        private Location answer;

        @Override
        public Location getCurrentLocationSync(long timeout) {
            lookups++;
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    synchronized (lock) {
                        while (!released && !open) {
                            try {
                                lock.wait(50);
                            } catch (InterruptedException ignored) {
                                return;
                            }
                        }
                        // Consumed, so release() is one permit rather than an
                        // open gate: a test that needs the NEXT request to
                        // park as well would otherwise find it running free.
                        released = false;
                    }
                }
            });
            return answer;
        }

        private void release() {
            synchronized (lock) {
                released = true;
                lock.notifyAll();
            }
        }

        /** Teardown: let everything parked go, and stay open. */
        private void openGate() {
            synchronized (lock) {
                open = true;
                lock.notifyAll();
            }
        }

        @Override
        public Location getCurrentLocation() {
            return null;
        }

        @Override
        public Location getLastKnownLocation() {
            return null;
        }

        @Override
        protected void bindListener() {
        }

        @Override
        protected void clearListener() {
        }

        @Override
        protected void bindBackgroundListener() {
        }

        @Override
        protected void clearBackgroundListener() {
        }
    }

    @Test
    void aQueuedGrantWhoseButtonWasRebuiltIsAnsweredRatherThanServed() {
        RecordingBridge bridge = install();
        ParkingManager manager = parkingManager();
        bridge.granted = manager;

        LocationButton first = new LocationButton();
        LocationButton second = new LocationButton();
        final int[] secondAnswers = new int[1];
        second.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                secondAnswers[0]++;
            }
        });
        assertEquals(2, bridge.sessions.size());

        // The first button is granted and its lookup parks, holding the slot.
        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(1, manager.lookups, "the first lookup should be running");

        // The second is granted while that one is in flight, so it QUEUES.
        bridge.sessions.get(1).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(0, secondAnswers[0], "queued, so not answered yet");

        // And now a setter replaces the second button while its grant waits.
        second.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);

        // Releasing the first drains the queue.
        manager.release();
        drain();
        drain();

        assertEquals(1, manager.lookups,
                "the queued grant belonged to a control that is gone, so it "
                + "must not start a lookup against a session nobody granted");
        assertEquals(1, secondAnswers[0],
                "but the tap still happened, so it is ANSWERED -- a grant that "
                + "is silently dropped is the failure this queue exists to "
                + "prevent");
    }

    @Test
    void aFallbackTapKeepsThePromptingManagerAcrossAnUpgrade() {
        RecordingBridge bridge = install();
        ParkingManager granted = parkingManager();
        bridge.granted = granted;
        // The ordinary manager, which is what a fallback tap is entitled to:
        // it never had a platform grant, so its answer comes through the
        // prompting path like any other Codename One location request.
        ParkingManager ordinary = parkingManager();
        ordinary.release();
        implementation.setLocationManager(ordinary);

        // One real system button, which will hold the in-flight slot.
        LocationButton holder = new LocationButton();
        assertEquals(1, bridge.sessions.size());

        // And one built while the bridge answers null, so it carries the
        // ordinary fallback button rather than a platform control.
        bridge.building = false;
        LocationButton fallbackButton = new LocationButton();
        Button tapTarget = (Button) fallbackButton.getComponentAt(0);

        // The holder is granted and its lookup parks, so anything else queues.
        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(1, granted.lookups, "the holder's lookup is running");

        // The user taps the fallback, and the tap queues behind the holder.
        tapTarget.pressed();
        tapTarget.released();
        drain();

        // Now a system button becomes available and replaces the fallback,
        // which is what initComponent does on every attach where the fallback
        // is showing.
        bridge.building = true;
        fallbackButton.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(2, bridge.sessions.size(), "the upgrade built a control");

        granted.release();
        drain();
        drain();

        // The tap must still be served -- it has no session to be superseded
        // -- and served through the ORDINARY manager. Reading the body at
        // service time sent it down the granted path instead, asking the
        // platform for a session nobody opened.
        waitUntil("the fallback tap reached the prompting manager",
                new Settled() {
                    public boolean isSo() {
                        return ordinary.lookups == 1;
                    }
                });
        assertEquals(1, ordinary.lookups,
                "a queued fallback tap is served through the prompting "
                + "manager, however the component was rebuilt while it waited");
        assertEquals(1, bridge.grantedAsks,
                "and the granted path is asked once, for the holder alone");
    }

    /**
     * Whether a stale wake is pending.
     *
     * <p>Read by reflection, and deliberately: the alternative is to WAIT for
     * the wake, and its delay is the in-flight deadline plus STALE_MARGIN --
     * five seconds of sleeping in a unit test to observe a boolean. The field
     * is the mechanism this test is about, so a rename should fail it.</p>
     */
    @Test
    void twoGrantsForOneButtonBothGetAnAnswer() throws Exception {
        RecordingBridge bridge = install();
        ParkingManager manager = parkingManager();
        bridge.granted = manager;

        LocationButton holder = new LocationButton();
        LocationButton second = new LocationButton();
        final int[] answers = new int[1];
        second.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                answers[0]++;
            }
        });
        assertEquals(2, bridge.sessions.size());

        // The holder parks, so anything else queues.
        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();

        // The user taps the second button and its grant waits.
        bridge.sessions.get(1).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(0, answers[0], "queued, so not answered yet");

        // A setter replaces that control, and the user taps the replacement
        // before the queue has moved.
        second.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(3, bridge.sessions.size(), "the replacement control");
        bridge.sessions.get(2).onResult.onSucess(Boolean.TRUE);
        drain();

        manager.release();
        drain();
        manager.release();
        drain();
        drain();

        // TWO taps, two answers. The first is the null its retired control has
        // earned, the second is the served lookup. Overwriting the stamp under
        // a contains() test gave the first tap nothing at all.
        waitUntil("both grants answered", new Settled() {
            public boolean isSo() {
                return answers[0] == 2;
            }
        });
        assertEquals(2, answers[0],
                "each grant is answered: the superseded one with null, the "
                + "current one by being served");
        assertEquals(2, manager.lookups,
                "and only the current one runs a lookup");
    }

    @Test
    void aNewPeerFailingDoesNotSwallowTheRunningRequestsResult()
            throws Exception {
        RecordingBridge bridge = install();
        ParkingManager manager = parkingManager();
        manager.answer = new Location();
        bridge.granted = manager;

        LocationButton button = new LocationButton();
        final List<Location> heard = new ArrayList<Location>();
        button.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                heard.add(location);
            }
        });

        // The user taps, and the lookup parks with a real fix waiting for it.
        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(1, manager.lookups, "the tap's lookup is running");

        // A setter replaces the control while that request is still running,
        // and the REPLACEMENT's session dies without anyone touching it.
        button.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(2, bridge.sessions.size());
        bridge.sessions.get(1).onUnavailable.run();
        drain();
        assertTrue(button.isUnavailable(), "the current control did fail");

        manager.release();
        drain();
        drain();

        // Two things happened, so the listener hears two: the failure's null,
        // and the location the tap actually obtained. Suppressing on the
        // component-wide flag threw the second away and left the user's tap
        // answered by a session it had nothing to do with.
        waitUntil("failure and result both reported", new Settled() {
            public boolean isSo() {
                return heard.size() == 2;
            }
        });
        assertEquals(2, heard.size(), "both the failure and the result: "
                + heard);
        assertNull(heard.get(0), "the failure reports first");
        assertNotNull(heard.get(1),
                "and the running request still reports the fix it obtained");
    }

    @Test
    void aQueuedFallbackTapSurvivesAPeerFailure() throws Exception {
        RecordingBridge bridge = install();
        ParkingManager granted = parkingManager();
        bridge.granted = granted;
        ParkingManager ordinary = parkingManager();
        ordinary.answer = new Location();
        ordinary.release();
        implementation.setLocationManager(ordinary);

        // A system button holds the slot, and a fallback button beside it.
        LocationButton holder = new LocationButton();
        bridge.building = false;
        LocationButton fallbackButton = new LocationButton();
        Button tapTarget = (Button) fallbackButton.getComponentAt(0);
        final List<Location> heard = new ArrayList<Location>();
        fallbackButton.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                heard.add(location);
            }
        });

        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(1, granted.lookups, "the holder's lookup is running");

        // The user taps the fallback; its grant queues behind the holder.
        tapTarget.pressed();
        tapTarget.released();
        drain();
        assertEquals(0, heard.size(), "queued, so not answered yet");

        // The component upgrades to a system button, and THAT session dies
        // without anyone having touched it.
        bridge.building = true;
        fallbackButton.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(2, bridge.sessions.size());
        bridge.sessions.get(1).onUnavailable.run();
        drain();

        granted.release();
        drain();
        drain();

        // The queued tap had no session behind it -- it was going through the
        // prompting manager, which this failure never touched. Dropping it
        // from the queue answered the user with the null of a session their
        // tap never used.
        waitUntil("the fallback tap ran its lookup", new Settled() {
            public boolean isSo() {
                return ordinary.lookups == 1;
            }
        });
        assertEquals(1, ordinary.lookups,
                "the queued fallback tap still runs its own lookup");
        assertTrue(heard.contains(ordinary.answer),
                "and reports the location it obtained: " + heard);
    }

    @Test
    void twoTapsOnOneFallbackAreTwoRequests() throws Exception {
        RecordingBridge bridge = install();
        ParkingManager granted = parkingManager();
        bridge.granted = granted;
        ParkingManager ordinary = parkingManager();
        ordinary.answer = new Location();
        ordinary.openGate();
        implementation.setLocationManager(ordinary);

        LocationButton holder = new LocationButton();
        bridge.building = false;
        LocationButton fallbackButton = new LocationButton();
        Button tapTarget = (Button) fallbackButton.getComponentAt(0);
        final List<Location> heard = new ArrayList<Location>();
        fallbackButton.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                heard.add(location);
            }
        });

        // The holder parks, so everything else queues.
        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();

        // The user taps the fallback TWICE before the queue moves. Both taps
        // carry NO_SESSION and the same button, which a stamp-on-the-button
        // queue could not tell apart: the second overwrote the first and was
        // refused a slot, so two taps produced one answer.
        tapTarget.pressed();
        tapTarget.released();
        drain();
        tapTarget.pressed();
        tapTarget.released();
        drain();
        assertEquals(0, heard.size(), "both queued, neither answered yet");

        granted.release();
        drain();
        drain();
        drain();

        waitUntil("both taps run their own lookup",
                new Settled() {
                    public boolean isSo() {
                        return ordinary.lookups == 2 && heard.size() == 2;
                    }
                });
        assertEquals(2, ordinary.lookups, "each tap runs its own lookup");
        assertEquals(2, heard.size(), "and each is answered: " + heard);
    }

    @Test
    void aFailedSessionRetiresOnlyItsOwnQueuedGrant() throws Exception {
        RecordingBridge bridge = install();
        ParkingManager manager = parkingManager();
        bridge.granted = manager;

        LocationButton holder = new LocationButton();
        LocationButton second = new LocationButton();
        final List<Location> heard = new ArrayList<Location>();
        second.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                heard.add(location);
            }
        });

        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();

        // A tap on the second button queues, its control is replaced, and the
        // replacement is tapped too: two grants from two different sessions,
        // both waiting.
        bridge.sessions.get(1).onResult.onSucess(Boolean.TRUE);
        drain();
        second.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(3, bridge.sessions.size());
        bridge.sessions.get(2).onResult.onSucess(Boolean.TRUE);
        drain();

        // The CURRENT session dies. Retiring every queued grant of this button
        // took the earlier tap's with it, and systemButtonFailed fires one
        // null -- so two taps got one answer.
        bridge.sessions.get(2).onUnavailable.run();
        drain();

        manager.release();
        drain();
        drain();

        waitUntil("both grants answered", new Settled() {
            public boolean isSo() {
                return heard.size() == 2;
            }
        });
        assertEquals(2, heard.size(),
                "the failed session retires its own grant; the earlier one is "
                + "still answered by the drain: " + heard);
    }

    @Test
    void anEnormousTimeoutDoesNotMakeEveryRequestStale() throws Exception {
        RecordingBridge bridge = install();
        ParkingManager manager = parkingManager();
        bridge.granted = manager;

        LocationButton holder = new LocationButton();
        // Long.MAX_VALUE is how a caller says "wait as long as it takes".
        // Adding the stale margin to it wrapped negative, so the deadline was
        // already behind us and the request counted as stale the moment it
        // began -- which let the next grant start beside it and share
        // LocationManager's single listener slot.
        holder.setTimeout(Long.MAX_VALUE);
        LocationButton second = new LocationButton();
        assertEquals(2, bridge.sessions.size());

        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(1, manager.lookups, "the holder's lookup is running");

        // The second is granted while that one is genuinely still in flight.
        // It must QUEUE, not start a concurrent lookup.
        bridge.sessions.get(1).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(1, manager.lookups,
                "a request with an enormous timeout is not stale, so the next "
                + "grant waits instead of running beside it");

        manager.release();
        drain();
        drain();
        waitUntil("the queued grant is served", new Settled() {
            public boolean isSo() {
                return manager.lookups == 2;
            }
        });
        assertEquals(2, manager.lookups, "and is served once the first ends");
    }

    /** A condition the EDT and its invokeAndBlock workers will reach. */
    private interface Settled {
        boolean isSo();
    }

    /**
     * Drains until {@code condition} holds, or fails saying it never did.
     *
     * <p>A fixed number of drains is not enough for anything that has been
     * through {@code getCurrentLocationSync}: that parks in invokeAndBlock, so
     * the request finishes on another thread and posts its completion back.
     * The sentinel in {@link #drain()} proves the EDT ran what was queued WHEN
     * IT WAS POSTED, which is a weaker thing, and asserting a count straight
     * after it made these tests depend on how loaded the machine was. One of
     * them failed exactly that way in a full-suite run while passing alone.</p>
     */
    private static void waitUntil(String what, Settled condition) {
        for (int guard = 0; guard < 200; guard++) {
            if (condition.isSo()) {
                return;
            }
            drain();
        }
        assertTrue(condition.isSo(), "never settled: " + what);
    }

    @Test
    void anAttachTimeFailureRetiresTheFallbackToo() {
        RecordingBridge bridge = install();
        // No control at first, so the component carries the ordinary fallback.
        bridge.building = false;
        LocationButton button = new LocationButton();
        assertFalse(button.isUnavailable());
        assertTrue(button.getComponentAt(0).isEnabled(), "the fallback is up");

        // The attach retry then meets a bridge that says it supports the
        // control and throws building it -- a failed session, not an absent
        // feature. rebuild() installs the placeholder for that; the retry has
        // to as well, or the component reports itself unavailable while a live
        // fallback goes on offering the ordinary permission prompt.
        bridge.building = true;
        bridge.throwing = true;
        button.initComponent();

        assertTrue(button.isUnavailable(), "the failure is recorded");
        assertFalse(button.getComponentAt(0).isEnabled(),
                "and what it shows is the disabled placeholder, not a live "
                + "button that prompts");
    }

    @Test
    void aRetainedPeerStillOwnsItsCallbacks() {
        RecordingBridge bridge = install();
        LocationButton button = new LocationButton();
        assertEquals(1, bridge.sessions.size());

        // A setter rebuilds and the platform declines, so the peer already on
        // screen is kept. Advancing the stamp anyway retired ITS callbacks
        // while it was still the visible control, so a tap or a failure from
        // the button the user can see was discarded as stale.
        bridge.building = false;
        button.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertTrue(button.getComponentAt(0) instanceof PeerComponent,
                "the peer was kept");

        bridge.sessions.get(0).onUnavailable.run();
        drain();
        assertTrue(button.isUnavailable(),
                "the retained control's session failing is still this "
                + "component's business");
    }

    @Test
    void aDeclinedRebuildKeepsTheControlItAlreadyHas() {
        RecordingBridge bridge = install();
        LocationButton button = new LocationButton();
        assertTrue(button.getComponentAt(0) instanceof PeerComponent,
                "the system control is up");

        // A setter rebuilds while the platform cannot make one right now --
        // the Android bridge needs the current Activity and answers null
        // without it.
        bridge.building = false;
        button.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);

        // The peer stays. Downgrading to the fallback here loses the
        // session-scoped path for good: initComponent's retry runs on ATTACH,
        // and replacing a child does not re-attach an initialised component.
        assertTrue(button.getComponentAt(0) instanceof PeerComponent,
                "a control already on screen is not given up because a "
                + "rebuild was declined");
        assertFalse(button.isUnavailable(), "and this is not a failure");

        // The rebuild is owed, and the next attach settles it.
        bridge.building = true;
        button.initComponent();
        assertEquals(2, bridge.sessions.size(),
                "the pending rebuild is retried on attach");
    }

    @Test
    void aSupportedControlThatThrowsIsUnavailableNotFallback() {
        RecordingBridge bridge = install();
        bridge.throwing = true;

        // isSupported() says yes and construction then fails. That is a failed
        // session, not a platform without the control: the ordinary fallback
        // asks for persistent location, which an exclusive build refuses
        // outright and a transactional one is trying not to need.
        LocationButton button = new LocationButton();
        assertTrue(button.isUnavailable(),
                "a supported control that threw leaves the component "
                + "unavailable, not falling back");
        assertFalse(button.getComponentAt(0).isEnabled(),
                "and what it shows is the disabled placeholder");
    }

    @Test
    void aBridgeThatDeclinesStillFallsBack() {
        // The other two outcomes are NOT failures. isSupported() false and a
        // createButton that returns null are the platform saying "not here,
        // not now", and both keep the ordinary fallback and its retry.
        RecordingBridge bridge = install();
        bridge.building = false;
        LocationButton button = new LocationButton();
        assertFalse(button.isUnavailable(),
                "a null from createButton is not a failure");
        assertTrue(button.getComponentAt(0).isEnabled(),
                "and the fallback is live");
    }

    private static boolean wakePending() throws Exception {
        return field("staleWake").get(null) != null;
    }

    @Test
    void servingOneQueuedButtonLeavesAWakeForTheRest() throws Exception {
        RecordingBridge bridge = install();
        ParkingManager manager = parkingManager();
        bridge.granted = manager;

        LocationButton first = new LocationButton();
        LocationButton second = new LocationButton();
        LocationButton third = new LocationButton();
        assertEquals(3, bridge.sessions.size());

        // The first is granted and parks, holding the slot.
        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(1, manager.lookups);

        // The other two are granted while it runs, so both queue.
        bridge.sessions.get(1).onResult.onSucess(Boolean.TRUE);
        bridge.sessions.get(2).onResult.onSucess(Boolean.TRUE);
        drain();
        assertTrue(wakePending(), "a queued button schedules a wake");

        // The first finishes, so the queue is drained: serveNextWaiting
        // cancels the wake on its way in, takes the second, and starts it --
        // and the second parks too, because ParkingManager is not released
        // again. The third is still waiting.
        manager.release();
        drain();
        drain();
        assertEquals(2, manager.lookups, "the second request is running");

        // That request can outlive its deadline without bound --
        // getCurrentLocationSync ignores its timeout entirely once somebody
        // holds LocationManager's listener slot. Without a wake, the third
        // button waits on it returning, or on another tap, neither of which
        // has to happen.
        assertTrue(wakePending(),
                "starting a request while the queue is not empty must leave a "
                + "wake for whoever is behind it");
    }

    /**
     * Every parking manager a test builds, so teardown can open all of them.
     *
     * <p>A test that deliberately leaves a lookup parked -- and one of these
     * does, because that is the state it is about -- would otherwise leave an
     * invokeAndBlock worker blocked forever, holding inFlight, with entries
     * still in WAITING and a timer pending. The next test in the same fork
     * would then find its own request queued behind a button that no longer
     * exists.</p>
     */
    private final List<ParkingManager> parkers = new ArrayList<ParkingManager>();

    private ParkingManager parkingManager() {
        ParkingManager manager = new ParkingManager();
        parkers.add(manager);
        return manager;
    }

    private RecordingBridge install() {
        RecordingBridge bridge = new RecordingBridge();
        implementation.setLocationButtonBridge(bridge);
        return bridge;
    }

    @AfterEach
    void removeBridge() throws Exception {
        // The implementation is a singleton shared with every other test class
        // in this module, so a bridge left installed would make an unrelated
        // component believe this device renders the button itself.
        implementation.setLocationButtonBridge(null);
        implementation.setLocationManager(null);
        // Let every parked lookup finish, then drain what that releases.
        for (ParkingManager manager : parkers) {
            manager.openGate();
        }
        parkers.clear();
        drain();
        drain();
        // And then say so plainly. The statics are shared by every
        // LocationButton in the process, so a queue or a flag left set here is
        // a failure delivered to some other test class -- which is the worst
        // kind, because it is reported against code that did nothing wrong.
        clearStatics();
    }

    @BeforeEach
    void theSharedStateMustBeCleanBeforeEachTest() throws Exception {
        // In @BeforeEach rather than in a test of its own, because a test can
        // only observe what ran BEFORE it and JUnit promises no order: as a
        // test it would pass for free whenever it happened to run first. Here
        // it runs after every one of its siblings in turn, so whichever of
        // them leaked is the one it reports.
        //
        // These are statics shared by every LocationButton in the process, so
        // a queue or a flag left set is a failure delivered to some other test
        // class -- the worst kind, because it is reported against code that
        // did nothing wrong.
        assertFalse(field("inFlight").getBoolean(null),
                "a previous test left a request in flight");
        assertTrue(((java.util.List<?>) field("WAITING").get(null)).isEmpty(),
                "a previous test left buttons queued");
        assertFalse(wakePending(), "a previous test left a timer pending");
    }

    /** Puts the shared request state back to what a fresh process has. */
    private static void clearStatics() throws Exception {
        field("inFlight").setBoolean(null, false);
        ((java.util.List<?>) field("WAITING").get(null)).clear();
        java.lang.reflect.Field wake = field("staleWake");
        java.util.Timer pending = (java.util.Timer) wake.get(null);
        if (pending != null) {
            pending.cancel();
            wake.set(null, null);
        }
    }

    private static java.lang.reflect.Field field(String name) throws Exception {
        java.lang.reflect.Field f =
                LocationButton.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /**
     * Lets everything already queued on the EDT run, and PROVES it did.
     *
     * <p>A sentinel of its own goes on the queue behind whatever the test just
     * posted, so FIFO ordering makes "the sentinel ran" mean "the callback ran".
     * Sleeping a fixed interval instead is how the negative assertion in these
     * tests passed while the EDT had not run a single one of them: nothing had
     * happened yet, and "nothing happened" is exactly what it was asserting.</p>
     */
    private static void drain() {
        final boolean[] ran = new boolean[1];
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                ran[0] = true;
            }
        });
        for (int guard = 0; !ran[0] && guard < 400; guard++) {
            TestUtils.waitFor(5);
        }
        assertTrue(ran[0],
                "the EDT never drained its queue, so this test asserted "
                + "nothing at all");
    }

    @Test
    void aFailedSessionDoesNotRetireTheButtonThatReplacedIt() {
        RecordingBridge bridge = install();
        LocationButton button = new LocationButton();
        assertEquals(1, bridge.sessions.size(), "the platform control");
        assertFalse(button.isUnavailable());

        // A setter has to build a new control, because the platform takes the
        // label at construction.
        button.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(2, bridge.sessions.size(), "the replacement control");

        // Now the FIRST session dies, which is the whole shape of this bug: the
        // view is off screen but its error listener still points here.
        bridge.sessions.get(0).onUnavailable.run();
        drain();
        assertFalse(button.isUnavailable(),
                "a session that died after its button was replaced must not "
                + "retire the healthy control that took its place");

        // The one on screen failing is a real failure and must still land.
        bridge.sessions.get(1).onUnavailable.run();
        drain();
        assertTrue(button.isUnavailable(),
                "the CURRENT control failing is what unavailable means");
    }

    @Test
    void aGrantFromAReplacedControlIsNotServed() {
        RecordingBridge bridge = install();
        LocationButton button = new LocationButton();
        final int[] answers = new int[1];
        button.addLocationSharedListener(new LocationSharedListener() {
            public void locationShared(Location location) {
                answers[0]++;
            }
        });
        button.setTextType(LocationButton.TEXT_USE_PRECISE_LOCATION);
        assertEquals(2, bridge.sessions.size());

        // The retired view reports a grant. Serving it would fire the listeners
        // of a button the user has not touched.
        bridge.sessions.get(0).onResult.onSucess(Boolean.TRUE);
        drain();
        assertEquals(0, answers[0],
                "a permission result from a replaced control is not this "
                + "button's answer");
    }
}
