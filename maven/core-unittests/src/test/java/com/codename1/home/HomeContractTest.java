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
package com.codename1.home;

import com.codename1.impl.home.LocalHomeBridge;
import com.codename1.impl.home.SubscriptionState;
import com.codename1.impl.home.SyntheticHome;

import com.codename1.util.AsyncResource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The parts of the contract that are invisible from the happy path: what
 * reaches the bridge, and what stops reaching a listener once it has been
 * stopped.
 *
 * <p>Both of these shipped wrong once. They are the kind of defect that a
 * feature test cannot see, because the operation still succeeds -- it just
 * succeeds against the wrong accessory, or against a form that is gone.</p>
 */
class HomeContractTest {

    /**
     * A batch can hold two locks with different PINs.
     *
     * <p>The credential used to be a single slot for the whole batch, filled
     * by whichever write in the batch had one last. Two locks in one call
     * therefore sent the second lock's PIN to the first, which fails, and
     * discarded the credential the caller actually supplied for it. Nothing
     * in the result distinguishes that from a wrong PIN, so it would have
     * read as the user's fault.</p>
     */
    @Test
    void eachWriteInABatchKeepsItsOwnCredential() {
        CapturingBridge bridge = new CapturingBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory lock = home.findAccessory("lock-front");
        Accessory lamp = home.findAccessory("lamp-living");
        List<TraitWrite> writes = new ArrayList<TraitWrite>();
        writes.add(new TraitWrite(lock, lock.getPrimaryService(),
                Trait.TARGET_LOCK_STATE, TraitValue.ofEnum(LockState.SECURED))
                .setAuthorizationData("1234"));
        writes.add(new TraitWrite(lamp, lamp.getPrimaryService(),
                Trait.ON_OFF, TraitValue.of(true)));
        writes.add(new TraitWrite(lock, lock.getPrimaryService(),
                Trait.TARGET_LOCK_STATE, TraitValue.ofEnum(LockState.UNSECURED))
                .setAuthorizationData("9999"));
        HomeAwait.settled(home.write(writes));

        assertArrayEquals(new String[] {"1234", "", "9999"},
                bridge.authorization,
                "each write's credential must arrive in its own slot");
    }

    /**
     * A quantity reaches the bridge in the trait's own unit.
     *
     * <p>The wire carries a number and a unit id, and the one backend that
     * ignores the unit is HomeKit -- so 68 Fahrenheit handed straight through
     * sets a thermostat to 68 Celsius. Converting once here means no bridge
     * has to remember to.</p>
     */
    @Test
    void aWriteReachesTheBridgeInTheTraitsOwnUnit() {
        CapturingBridge bridge = new CapturingBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory thermostat = home.findAccessory("thermostat");
        AccessoryService svc = thermostat.getPrimaryService();
        HomeAwait.settled(home.write(new TraitWrite(thermostat, svc,
                Trait.TARGET_HEATING_TEMPERATURE,
                TraitValue.of(68, TraitUnit.FAHRENHEIT))));

        assertEquals(20.0, bridge.numeric[0], 0.0001,
                "68F must reach the bridge as 20C, not as 68");
        assertEquals(TraitUnit.CELSIUS.getWireId(), bridge.units[0],
                "and the unit slot must say so");
    }

    /**
     * A value whose unit measures something else entirely is a caller bug,
     * and applying the number regardless would move a real accessory.
     */
    @Test
    void aWriteInAUnitOfTheWrongDimensionIsRefused() {
        CapturingBridge bridge = new CapturingBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory thermostat = home.findAccessory("thermostat");
        final AccessoryService svc = thermostat.getPrimaryService();
        final TraitWrite bad = new TraitWrite(thermostat, svc,
                Trait.TARGET_HEATING_TEMPERATURE,
                TraitValue.of(50, TraitUnit.PERCENT));
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                SmartHome.getInstance().write(bad);
            }
        });
    }

    /**
     * Two refreshes before the first one has connected.
     *
     * <p>Ordinary rather than contrived: startup code and a foreground
     * handler both call it. The second used to be sent to the bridge's
     * refresh() against a backend that had not loaded, and on iOS that
     * resolves with an empty house.</p>
     */
    @Test
    void aRefreshDuringStartupGetsTheStartsAnswer() {
        CapturingBridge bridge = new CapturingBridge();
        bridge.holdStart = true;
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();

        AsyncResource<List<HomeStructure>> first = home.refresh();
        AsyncResource<List<HomeStructure>> second = home.refresh();
        assertEquals(0, bridge.refreshCalls,
                "the second refresh must not reach a backend that is still"
                        + " starting");

        bridge.releaseStart();
        HomeAwait.settled(first);
        HomeAwait.settled(second);
        assertEquals(0, bridge.refreshCalls,
                "and it must still not have gone to the backend afterwards");
        assertEquals(first.get().size(), second.get().size(),
                "it is answered with the start's own graph");
        assertFalse(second.get().isEmpty(),
                "an empty answer is the bug this guards");
    }

    /**
     * The initial delivery is produced by a read that is in flight while the
     * caller is free to stop the subscription, so it has to be checked at the
     * point of delivery like every other batch.
     */
    @Test
    void aStoppedSubscriptionDeliversNothingEvenUpFront() {
        final AtomicReference<TraitChangeBatch> seen =
                new AtomicReference<TraitChangeBatch>();
        SubscriptionState state = new SubscriptionState("sub-1",
                new HomeChangeListener() {
                    @Override
                    public void traitsChanged(TraitChangeBatch batch) {
                        seen.set(batch);
                    }
                }, 0);
        List<TraitReading> readings = new ArrayList<TraitReading>();
        readings.add(TraitReading.of("lamp-living", "1", Trait.ON_OFF,
                TraitValue.of(true), 1L));

        state.dispose();
        state.offer(readings, true);
        assertNull(seen.get(),
                "an initial batch must not reach a stopped subscription");

        state.offer(readings, false);
        assertNull(seen.get(),
                "nor must an ordinary one");
    }

    /**
     * A change that lands while the up-front read is in flight arrives
     * after it, not before.
     *
     * <p>The read snapshots its values when it starts. A change delivered
     * before it finishes would be overwritten on screen by that older
     * snapshot -- a light that turns on and then shows itself off, from the
     * subscription whose job is to keep the screen true.</p>
     */
    @Test
    void aChangeDuringTheInitialReadIsDeliveredAfterIt() {
        final List<TraitChangeBatch> seen = new ArrayList<TraitChangeBatch>();
        SubscriptionState state = new SubscriptionState("sub-3",
                new HomeChangeListener() {
                    @Override
                    public void traitsChanged(TraitChangeBatch batch) {
                        seen.add(batch);
                    }
                }, 0, true);

        state.offer(readingOf(true, 2L), false);
        assertEquals(0, seen.size(),
                "a live change must wait for the read it would race");

        state.offer(readingOf(false, 1L), true);
        assertEquals(2, seen.size(), "both batches must arrive");
        assertTrue(seen.get(0).isInitialDelivery(),
                "the snapshot first");
        assertFalse(seen.get(1).isInitialDelivery(),
                "then the change that outdates it");
        assertTrue(seen.get(1).getReadings().get(0).getValue().getBoolean(),
                "and the value the screen keeps is the newer one");
        state.dispose();
    }

    /**
     * And a read that comes back with nothing must not hold them forever.
     */
    @Test
    void aFailedInitialReadReleasesWhatItWasHolding() {
        final List<TraitChangeBatch> seen = new ArrayList<TraitChangeBatch>();
        SubscriptionState state = new SubscriptionState("sub-4",
                new HomeChangeListener() {
                    @Override
                    public void traitsChanged(TraitChangeBatch batch) {
                        seen.add(batch);
                    }
                }, 0, true);

        state.offer(readingOf(true, 2L), false);
        assertEquals(0, seen.size());
        state.initialDeliveryUnavailable();
        assertEquals(1, seen.size(),
                "the held change must be released, not lost");
        state.dispose();
    }

    private static List<TraitReading> readingOf(boolean on, long at) {
        List<TraitReading> readings = new ArrayList<TraitReading>();
        readings.add(TraitReading.of("lamp-living", "1", Trait.ON_OFF,
                TraitValue.of(on), at));
        return readings;
    }

    /**
     * The same state before it is stopped, so the test above is proving the
     * dispose check rather than a listener that never fires.
     */
    @Test
    void aLiveSubscriptionStillDeliversUpFront() {
        final AtomicReference<TraitChangeBatch> seen =
                new AtomicReference<TraitChangeBatch>();
        SubscriptionState state = new SubscriptionState("sub-2",
                new HomeChangeListener() {
                    @Override
                    public void traitsChanged(TraitChangeBatch batch) {
                        seen.set(batch);
                    }
                }, 0);
        List<TraitReading> readings = new ArrayList<TraitReading>();
        readings.add(TraitReading.of("lamp-living", "1", Trait.ON_OFF,
                TraitValue.of(true), 1L));

        state.offer(readings, true);
        assertEquals(1, seen.get().getReadings().size());
        state.dispose();
    }

    /**
     * The local bridge, with the one array this test cares about kept.
     *
     * <p>A subclass rather than a hand-written fake: the point is what the
     * facade hands a bridge, and everything else about the call has to stay
     * real for the write to reach here at all.</p>
     */
    private static final class CapturingBridge extends LocalHomeBridge {

        private String[] authorization;
        private double[] numeric;
        private int[] units;
        private int refreshCalls;
        /// Holds start() open, so a test can stand inside the window between
        /// asking a backend to connect and it having connected. The local
        /// bridge is otherwise far too quick to be in it.
        private boolean holdStart;
        private int heldStartId;

        @Override
        public void start(int requestId) {
            if (holdStart) {
                heldStartId = requestId;
                return;
            }
            super.start(requestId);
        }

        void releaseStart() {
            holdStart = false;
            super.start(heldStartId);
        }

        @Override
        public void refresh(int requestId) {
            refreshCalls++;
            super.refresh(requestId);
        }

        @Override
        public void writeTraits(int requestId, String[] accessoryIds,
                String[] serviceIds, String[] traitIds, int[] kinds,
                double[] numericValues, String[] stringValues,
                int[] unitWireIds, String[] authorizationData) {
            authorization = authorizationData;
            numeric = numericValues;
            units = unitWireIds;
            super.writeTraits(requestId, accessoryIds, serviceIds, traitIds,
                    kinds, numericValues, stringValues, unitWireIds,
                    authorizationData);
        }
    }
}
