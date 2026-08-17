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

import com.codename1.impl.home.HomeWire;
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
import static org.junit.jupiter.api.Assertions.assertSame;
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
     * A value out of the wrong domain enum is refused rather than applied by
     * ordinal.
     *
     * <p>Every enum crosses the wire as an ordinal, and an ordinal from the
     * wrong enum is a perfectly good number: AlarmState.WARNING is 1, and 1
     * in LockState is UNSECURED. Unguarded, asking a door lock for an alarm
     * state opens the door.</p>
     */
    @Test
    void aValueFromTheWrongEnumIsRefusedRatherThanUnlockingTheDoor() {
        CapturingBridge bridge = new CapturingBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory lock = home.findAccessory("lock-front");
        final TraitWrite wrong = new TraitWrite(lock,
                lock.getPrimaryService(), Trait.TARGET_LOCK_STATE,
                TraitValue.ofEnum(AlarmState.WARNING));
        assertEquals(LockState.UNSECURED.ordinal(),
                AlarmState.WARNING.ordinal(),
                "the test is only meaningful while the two ordinals collide");
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                SmartHome.getInstance().write(wrong);
            }
        });
        assertNull(bridge.authorization,
                "and nothing may reach the bridge");
    }

    /**
     * A backend that advertises a batch limit gets batches within it, and the
     * caller still gets one answer covering everything it asked for.
     */
    @Test
    void aReadLargerThanTheBackendsLimitIsSplitAndRecombined() {
        CapturingBridge bridge = new CapturingBridge();
        bridge.readBatchLimit = 2;
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory thermostat = home.findAccessory("thermostat");
        AccessoryService svc = thermostat.getPrimaryService();
        TraitReadRequest request = new TraitReadRequest();
        request.add(thermostat, svc, Trait.CURRENT_TEMPERATURE);
        request.add(thermostat, svc, Trait.CURRENT_HUMIDITY);
        request.add(thermostat, svc, Trait.TARGET_HEATING_TEMPERATURE);
        request.add(thermostat, svc, Trait.TARGET_COOLING_TEMPERATURE);
        request.add(thermostat, svc, Trait.TARGET_HEATING_COOLING);
        AsyncResource<List<TraitReading>> r =
                HomeAwait.settled(home.read(request));

        assertEquals(3, bridge.readBatches.size(),
                "five traits at two per call is three calls: "
                        + bridge.readBatches);
        for (Integer size : bridge.readBatches) {
            assertTrue(size.intValue() <= 2,
                    "no call may exceed the advertised limit: " + size);
        }
        assertEquals(5, r.get().size(),
                "and the caller gets one answer covering all five");
        assertSame(Trait.CURRENT_TEMPERATURE, r.get().get(0).getTrait(),
                "in the order they were asked for");
        assertSame(Trait.TARGET_HEATING_COOLING, r.get().get(4).getTrait());
    }

    /**
     * A record the codec cannot read keeps its place.
     *
     * <p>read() promises one reading per requested trait, in order. Dropping
     * an unreadable record shifts every later one up, and a caller lining
     * values up with the controls that asked for them puts the humidity on
     * the thermostat dial.</p>
     */
    @Test
    void anUnreadableRecordKeepsItsPlaceInTheAnswer() {
        CapturingBridge bridge = new CapturingBridge();
        bridge.corruptSecondReading = true;
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory thermostat = home.findAccessory("thermostat");
        AccessoryService svc = thermostat.getPrimaryService();
        TraitReadRequest request = new TraitReadRequest();
        request.add(thermostat, svc, Trait.CURRENT_TEMPERATURE);
        request.add(thermostat, svc, Trait.CURRENT_HUMIDITY);
        request.add(thermostat, svc, Trait.TARGET_HEATING_TEMPERATURE);
        List<TraitReading> readings =
                HomeAwait.settled(home.read(request)).get();

        assertEquals(3, readings.size(),
                "one reading per trait asked for");
        assertSame(Trait.CURRENT_HUMIDITY, readings.get(1).getTrait(),
                "the unreadable one holds its own position");
        assertSame(HomeError.INVALID_DATA, readings.get(1).getError(),
                "and says why it has no value");
        assertSame(Trait.TARGET_HEATING_TEMPERATURE,
                readings.get(2).getTrait(),
                "so the ones after it are not shifted up");
    }

    /**
     * A scene action against a target the home does not have fails the whole
     * scene.
     *
     * <p>This is the backend the simulator and the desktop run on, so a scene
     * it accepts is one a developer believes works. Saving an action against
     * an accessory that is not there lets the simulator approve a scene every
     * real backend rejects.</p>
     */
    @Test
    void aSceneActionAgainstAMissingAccessoryFailsTheScene() {
        LocalHomeBridge bridge = new LocalHomeBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        HomeStructure structure = home.getPrimaryStructure();
        List<SceneAction> actions = new ArrayList<SceneAction>();
        actions.add(new SceneAction("no-such-lamp", "1", Trait.ON_OFF,
                TraitValue.of(true)));
        AsyncResource<Scene> r = HomeAwait.settled(
                home.createScene(structure, "Good night", actions));

        assertFalse(r.isReady(), "the scene must not be created");
        int before = structure.getScenes().size();
        HomeAwait.settled(home.refresh());
        assertEquals(before,
                home.getPrimaryStructure().getScenes().size(),
                "and nothing may be left behind");
    }

    /**
     * A read is lined up against what was sent, not against a request the
     * caller has gone on editing.
     */
    @Test
    void aReadIsAnsweredAgainstTheRequestAsItWasSent() {
        CapturingBridge bridge = new CapturingBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory thermostat = home.findAccessory("thermostat");
        AccessoryService svc = thermostat.getPrimaryService();
        TraitReadRequest request = new TraitReadRequest();
        request.add(thermostat, svc, Trait.CURRENT_TEMPERATURE);
        AsyncResource<List<TraitReading>> r = home.read(request);
        request.add(thermostat, svc, Trait.CURRENT_HUMIDITY);
        HomeAwait.settled(r);

        assertEquals(1, r.get().size(),
                "the answer covers what was asked for, not what was added"
                        + " afterwards");
        assertSame(Trait.CURRENT_TEMPERATURE, r.get().get(0).getTrait());
    }

    /**
     * A state an accessory only reports is refused as a write.
     *
     * <p>Every target enum has some -- a door that is OPENING, a thermostat
     * mode HomeKit cannot express -- and each says so with its own
     * isWritable(). Stored and reported successful, the app believes it asked
     * for something no accessory can carry out.</p>
     */
    @Test
    void aReportOnlyEnumStateIsRefusedAsAWrite() {
        LocalHomeBridge bridge = new LocalHomeBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory thermostat = home.findAccessory("thermostat");
        final TraitWrite reportOnly = new TraitWrite(thermostat,
                thermostat.getPrimaryService(), Trait.TARGET_HEATING_COOLING,
                TraitValue.ofEnum(HeatingCoolingMode.OTHER));
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                SmartHome.getInstance().write(reportOnly);
            }
        });

        // And the mode the thermostat is actually in is untouched.
        assertSame(HeatingCoolingMode.AUTO, HeatingCoolingMode.of(
                HomeAwait.settled(home.read(thermostat,
                        thermostat.getPrimaryService(),
                        Trait.TARGET_HEATING_COOLING)).get().getValue()));
    }

    /**
     * A constraint's enum choices survive the trip through the graph.
     *
     * <p>Dropped, a thermostat the simulator restricts to HEAT and COOL came
     * back saying it accepts anything, so a UI built from the graph offered
     * modes the very same bridge then refused.</p>
     */
    @Test
    void enumChoicesSurviveTheGraph() {
        LocalHomeBridge bridge = new LocalHomeBridge();
        SyntheticHome.populate(bridge);
        bridge.addAccessory(SyntheticHome.STRUCTURE_ID, "stat-2", "Hall Stat",
                "room-hall", AccessoryCategory.THERMOSTAT.ordinal());
        bridge.addService("stat-2", "1", "Hall Stat",
                ServiceType.THERMOSTAT.ordinal(), true);
        bridge.addTrait("stat-2", "1",
                TraitConstraint.choices(Trait.TARGET_HEATING_COOLING, true,
                        true, true, new int[] {
                            HeatingCoolingMode.HEAT.ordinal(),
                            HeatingCoolingMode.COOL.ordinal()}),
                TraitValue.ofEnum(HeatingCoolingMode.HEAT));
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory stat = home.findAccessory("stat-2");
        TraitConstraint c = stat.getPrimaryService()
                .getConstraint(Trait.TARGET_HEATING_COOLING);
        assertEquals(2, c.getValidOrdinals().size(),
                "the two choices must come back: " + c.getValidOrdinals());
        assertTrue(c.getValidOrdinals().contains(
                Integer.valueOf(HeatingCoolingMode.HEAT.ordinal())));
        assertFalse(c.getValidOrdinals().contains(
                Integer.valueOf(HeatingCoolingMode.AUTO.ordinal())),
                "and a mode it does not offer must not appear");
    }

    /**
     * identify() on an unreachable accessory fails, as it does on iOS. The
     * local model's promise is that an unreachable accessory fails
     * operations, and the synthetic outlet exists to be unreachable.
     */
    @Test
    void identifyingAnUnreachableAccessoryFails() {
        LocalHomeBridge bridge = new LocalHomeBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        SmartHome home = SmartHome.getInstance();
        HomeAwait.settled(home.refresh());

        Accessory dead = home.findAccessory("outlet-garage");
        AsyncResource<?> r = HomeAwait.settled(home.identify(dead));
        assertFalse(r.isReady(), "an unreachable accessory cannot identify");

        Accessory lamp = home.findAccessory("lamp-living");
        assertTrue(HomeAwait.settled(home.identify(lamp)).isReady(),
                "a reachable one still can");
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
     * A resync raised while the initial read is in flight still reaches the
     * listener.
     *
     * <p>markResyncRequired deliberately holds the flag during that window,
     * so if nothing else is pending the initial delivery is the only chance
     * to hand it over. Lost, a subscription whose notification registration
     * failed delivered its initial values looking perfectly healthy and never
     * said they could not be trusted.</p>
     */
    @Test
    void aResyncRaisedDuringTheInitialReadIsStillDelivered() {
        final List<TraitChangeBatch> seen = new ArrayList<TraitChangeBatch>();
        SubscriptionState state = new SubscriptionState("sub-5",
                new HomeChangeListener() {
                    @Override
                    public void traitsChanged(TraitChangeBatch batch) {
                        seen.add(batch);
                    }
                }, 0, true);

        state.markResyncRequired();
        assertEquals(0, seen.size(), "held while the read is in flight");

        state.offer(readingOf(true, 1L), true);
        boolean flagged = false;
        for (TraitChangeBatch batch : seen) {
            flagged = flagged || batch.isResyncRequired();
        }
        assertTrue(flagged,
                "the resync flag must reach the listener: " + seen.size()
                        + " batch(es)");
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
        private int readBatchLimit;
        private boolean corruptSecondReading;
        private final List<Integer> readBatches = new ArrayList<Integer>();

        @Override
        public int getMaxReadBatchSize() {
            return readBatchLimit;
        }

        @Override
        public void readTraits(int requestId, String[] accessoryIds,
                String[] serviceIds, String[] traitIds, boolean allowCached) {
            readBatches.add(Integer.valueOf(traitIds.length));
            if (!corruptSecondReading) {
                super.readTraits(requestId, accessoryIds, serviceIds, traitIds,
                        allowCached);
                return;
            }
            // A port a version ahead, or one that truncated a line. Answered
            // straight rather than through the local store, because the
            // record has to be broken on the wire and that is where the wire
            // is.
            String[] lines = new String[traitIds.length];
            for (int i = 0; i < traitIds.length; i++) {
                lines[i] = i == 1 ? "not a record"
                        : HomeWire.encodeReading(TraitReading.of(
                                accessoryIds[i], serviceIds[i],
                                Trait.forId(traitIds[i]),
                                TraitValue.of(1, TraitUnit.PERCENT), 1L));
            }
            SmartHome.deliverReadings(requestId, lines, null);
        }

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
