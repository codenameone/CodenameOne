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

import com.codename1.home.commissioning.CommissioningRequest;
import com.codename1.home.commissioning.CommissioningResult;
import com.codename1.impl.home.LocalHomeBridge;
import com.codename1.impl.home.SyntheticHome;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole stack against the simulated home: the facade, the wire codec, and
 * the local bridge that backs the simulator, the desktop ports and the
 * JavaScript port.
 *
 * <p>The synthetic house is deliberately awkward -- a two-gang switch, a
 * bridged pair of lights, an unreachable socket, a thermostat in auto mode and
 * a sensor that has never reported -- so these tests exercise the shapes that
 * break naive code rather than the happy path.</p>
 */
class LocalHomeTest {

    private LocalHomeBridge bridge;
    private SmartHome home;

    @BeforeEach
    void furnishAHome() {
        bridge = new LocalHomeBridge();
        SyntheticHome.populate(bridge);
        SmartHome.resetForTest(bridge);
        home = SmartHome.getInstance();
        settled(home.refresh());
    }

    // ------------------------------------------------------------------
    // graph
    // ------------------------------------------------------------------

    @Test
    void theGraphLoadsAndReportsItselfAsLocalOnly() {
        assertTrue(home.isSupported());
        assertSame(HomeAvailability.LOCAL_ONLY, home.getAvailability());
        assertSame(HomeBackend.LOCAL, home.getBackend());
        assertEquals(1, home.getStructures().size());
        assertNotNull(home.getPrimaryStructure());
        assertEquals(4, home.getPrimaryStructure().getRooms().size());
    }

    /**
     * Zones are a HomeKit concept, and a simulated home that invented some
     * would let an app be built around groupings that exist nowhere it will
     * actually run.
     */
    @Test
    void thereAreNoZonesOutsideHomeKit() {
        assertTrue(home.getPrimaryStructure().getZones().isEmpty());
    }

    @Test
    void accessoriesAreFoundByIdAndByRoom() {
        HomeStructure h = home.getPrimaryStructure();
        assertNotNull(h.getAccessory("lamp-living"));
        assertNotNull(home.findAccessory("lamp-living"));
        assertNull(home.findAccessory("no-such-thing"));
        assertFalse(h.getAccessoriesInRoom("room-living").isEmpty());
        // The hub and the garage socket are in no room, which is a real state
        // on both backends and an easy place to lose a device.
        assertFalse(h.getAccessoriesInRoom(null).isEmpty(),
                "accessories with no room have to be reachable, or they are"
                        + " invisible in a room-organized UI");
    }

    /**
     * One accessory, two services, both switchable. Code that writes ON_OFF
     * to "the accessory" is ambiguous, and this is where it finds out.
     */
    @Test
    void aTwoGangSwitchIsOneAccessoryWithTwoSwitchableServices() {
        Accessory sw = home.findAccessory("switch-hall");
        assertNotNull(sw);
        assertEquals(2, sw.getServices().size());
        assertEquals(2, sw.getServicesSupporting(Trait.ON_OFF).size());
        assertTrue(sw.supports(Trait.ON_OFF));
        assertSame(sw.getService("1"), sw.getPrimaryService());
    }

    @Test
    void bridgedAccessoriesNameTheirBridge() {
        Accessory light = home.findAccessory("lamp-kitchen-1");
        assertNotNull(light);
        assertTrue(light.isBridged());
        assertEquals("hub", light.getBridgeAccessoryId());
        assertFalse(home.findAccessory("lamp-living").isBridged());
    }

    /**
     * The trait's nominal range is 0..100 and this lamp's real floor is 10. A
     * slider built from the trait rather than from the constraint would offer
     * values the accessory refuses.
     */
    @Test
    void theAccessorysOwnRangeIsNarrowerThanTheTraitsNominalOne() {
        AccessoryService svc =
                home.findAccessory("lamp-living").getPrimaryService();
        TraitConstraint c = svc.getConstraint(Trait.BRIGHTNESS);
        assertNotNull(c);
        assertTrue(c.hasRange());
        assertEquals(10.0, c.getMinimum(), 0.0001);
        assertEquals(0.0, Trait.BRIGHTNESS.getNominalMinimum(), 0.0001);
    }

    // ------------------------------------------------------------------
    // reads
    // ------------------------------------------------------------------

    @Test
    void readingATraitReturnsItsValue() {
        Accessory lamp = home.findAccessory("lamp-living");
        AsyncResource<TraitReading> r = settled(home.read(lamp,
                lamp.getPrimaryService(), Trait.BRIGHTNESS));
        TraitReading reading = r.get();
        assertTrue(reading.hasValue());
        assertEquals(60.0, reading.getValue().getDouble(TraitUnit.PERCENT),
                0.0001);
    }

    /**
     * A sensor that has not reported is not an error and not a zero. Code that
     * skips hasValue() renders this as a freezing room.
     */
    @Test
    void aSensorWithNothingToReportSaysSoRatherThanReadingZero() {
        Accessory sensor = home.findAccessory("sensor-hall");
        AsyncResource<TraitReading> r = settled(home.read(sensor,
                sensor.getService("2"), Trait.CURRENT_TEMPERATURE));
        TraitReading reading = r.get();
        assertFalse(reading.hasValue());
        assertNull(reading.getValue());
        assertFalse(reading.isFailed(),
                "no reading yet is not a failure");
    }

    /**
     * Three values and one unreachable accessory is a successful read with
     * one failed row, not a failed operation.
     */
    @Test
    void aBatchReadPartlySucceeds() {
        Accessory lamp = home.findAccessory("lamp-living");
        Accessory dead = home.findAccessory("outlet-garage");
        TraitReadRequest request = new TraitReadRequest()
                .add(lamp, lamp.getPrimaryService(), Trait.ON_OFF)
                .add(lamp, lamp.getPrimaryService(), Trait.BRIGHTNESS)
                .add(dead, dead.getPrimaryService(), Trait.ON_OFF);
        AsyncResource<List<TraitReading>> r = settled(home.read(request));
        assertTrue(r.isReady(),
                "one unreachable accessory must not fail the whole read");
        List<TraitReading> readings = r.get();
        assertEquals(3, readings.size());
        assertTrue(readings.get(0).hasValue());
        assertTrue(readings.get(1).hasValue());
        assertTrue(readings.get(2).isFailed());
        assertSame(HomeError.ACCESSORY_UNREACHABLE,
                readings.get(2).getError());
    }

    @Test
    void readingATraitAServiceDoesNotHaveIsReportedPerTrait() {
        Accessory lamp = home.findAccessory("lamp-living");
        AsyncResource<TraitReading> r = settled(home.read(lamp,
                lamp.getPrimaryService(), Trait.LOCK_STATE));
        assertTrue(r.get().isFailed());
        assertSame(HomeError.TRAIT_NOT_SUPPORTED, r.get().getError());
    }

    // ------------------------------------------------------------------
    // writes
    // ------------------------------------------------------------------

    @Test
    void writingATraitChangesIt() {
        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        AsyncResource<TraitWriteResult> w = settled(home.write(
                new TraitWrite(lamp, svc, Trait.ON_OFF,
                        TraitValue.of(true))));
        assertTrue(w.get().isApplied());
        assertTrue(settled(home.read(lamp, svc, Trait.ON_OFF)).get()
                .getValue().getBoolean());
    }

    /**
     * Refused rather than clamped. An app that asked for 5 percent and
     * silently got 10 never learns it was wrong, and the bug reaches the user
     * as a lamp that "does not go dim enough".
     */
    @Test
    void aWriteBelowTheAccessorysFloorIsRefusedRatherThanClamped() {
        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        AsyncResource<TraitWriteResult> w = settled(home.write(
                new TraitWrite(lamp, svc, Trait.BRIGHTNESS,
                        TraitValue.of(5, TraitUnit.PERCENT))));
        assertFalse(w.get().isApplied());
        assertSame(HomeError.VALUE_OUT_OF_RANGE, w.get().getError());
        assertEquals(60.0, settled(home.read(lamp, svc, Trait.BRIGHTNESS))
                .get().getValue().getDouble(TraitUnit.PERCENT), 0.0001,
                "a refused write must leave the accessory alone");
    }

    /**
     * "Turn off every light" against a home with a dead socket mostly worked,
     * and failing the whole operation would have the caller retry and flicker
     * the house.
     */
    @Test
    void aBatchWritePartlySucceeds() {
        Accessory lamp = home.findAccessory("lamp-living");
        Accessory dead = home.findAccessory("outlet-garage");
        List<TraitWrite> writes = new ArrayList<TraitWrite>();
        writes.add(new TraitWrite(lamp, lamp.getPrimaryService(),
                Trait.ON_OFF, TraitValue.of(true)));
        writes.add(new TraitWrite(dead, dead.getPrimaryService(),
                Trait.ON_OFF, TraitValue.of(true)));
        AsyncResource<List<TraitWriteResult>> w =
                settled(home.write(writes));
        assertTrue(w.isReady());
        assertEquals(2, w.get().size());
        assertTrue(w.get().get(0).isApplied());
        assertFalse(w.get().get(1).isApplied());
        assertSame(HomeError.ACCESSORY_UNREACHABLE, w.get().get(1).getError());
    }

    @Test
    void aWriteToATraitTheServiceDoesNotHaveIsRefused() {
        Accessory lamp = home.findAccessory("lamp-living");
        AsyncResource<TraitWriteResult> w = settled(home.write(
                new TraitWrite(lamp, lamp.getPrimaryService(),
                        Trait.TARGET_LOCK_STATE,
                        TraitValue.ofEnum(LockState.SECURED))));
        assertFalse(w.get().isApplied());
        assertSame(HomeError.TRAIT_NOT_SUPPORTED, w.get().getError());
    }

    // ------------------------------------------------------------------
    // subscriptions
    // ------------------------------------------------------------------

    /**
     * The local home does not push, matching every backend except HomeKit in
     * the foreground. An app that assumed push would look exactly like a
     * sensor that never triggers.
     */
    @Test
    void changesWaitForADrainRatherThanArrivingOnTheirOwn() {
        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        AtomicReference<TraitChangeBatch> seen =
                new AtomicReference<TraitChangeBatch>();
        TraitSubscription sub = home.subscribe(
                new SubscriptionRequest()
                        .add(lamp, svc, Trait.ON_OFF)
                        .setMinIntervalMillis(0),
                seen::set);
        assertFalse(sub.isPushDelivery());

        bridge.setValue("lamp-living", "1", Trait.ON_OFF,
                TraitValue.of(true));
        assertNull(seen.get(),
                "nothing must arrive before a drain on a backend that does"
                        + " not push");

        settled(home.drainChanges());
        assertNotNull(seen.get());
        assertEquals(1, seen.get().getReadings().size());
        assertTrue(seen.get().getReadings().get(0).getValue().getBoolean());
        sub.stop();
    }

    /**
     * A batch is a state update, not an event log: within the window the
     * newest value per trait is what survives.
     */
    @Test
    void changesAreCoalescedToTheNewestValuePerTrait() {
        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        AtomicReference<TraitChangeBatch> seen =
                new AtomicReference<TraitChangeBatch>();
        TraitSubscription sub = home.subscribe(
                new SubscriptionRequest()
                        .add(lamp, svc, Trait.BRIGHTNESS)
                        .setMinIntervalMillis(0),
                seen::set);
        for (int i = 20; i <= 90; i += 10) {
            bridge.setValue("lamp-living", "1", Trait.BRIGHTNESS,
                    TraitValue.of(i, TraitUnit.PERCENT));
        }
        settled(home.drainChanges());
        assertNotNull(seen.get());
        assertEquals(1, seen.get().getReadings().size(),
                "eight steps of a dimmer must arrive as one value, not eight");
        assertEquals(90.0, seen.get().getReadings().get(0).getValue()
                .getDouble(TraitUnit.PERCENT), 0.0001);
        sub.stop();
    }

    @Test
    void aStoppedSubscriptionStopsDelivering() {
        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        AtomicReference<TraitChangeBatch> seen =
                new AtomicReference<TraitChangeBatch>();
        TraitSubscription sub = home.subscribe(
                new SubscriptionRequest().add(lamp, svc, Trait.ON_OFF)
                        .setMinIntervalMillis(0),
                seen::set);
        sub.stop();
        bridge.setValue("lamp-living", "1", Trait.ON_OFF,
                TraitValue.of(true));
        settled(home.drainChanges());
        assertNull(seen.get());
    }

    /**
     * Without this an app shows values from before the gap indefinitely, with
     * nothing to indicate it.
     */
    @Test
    void aResyncIsAnnouncedEvenWithNothingToShow() {
        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        AtomicReference<TraitChangeBatch> seen =
                new AtomicReference<TraitChangeBatch>();
        TraitSubscription sub = home.subscribe(
                new SubscriptionRequest().add(lamp, svc, Trait.ON_OFF)
                        .setMinIntervalMillis(0),
                seen::set);
        bridge.forceResync(sub.getId());
        assertNotNull(seen.get());
        assertTrue(seen.get().isResyncRequired());
        assertTrue(seen.get().isEmpty());
        sub.stop();
    }

    // ------------------------------------------------------------------
    // scenes
    // ------------------------------------------------------------------

    @Test
    void runningASceneAppliesEveryActionInIt() {
        HomeStructure h = home.getPrimaryStructure();
        Scene evening = null;
        for (Scene s : h.getScenes()) {
            if ("scene-evening".equals(s.getId())) {
                evening = s;
            }
        }
        assertNotNull(evening);
        assertEquals(3, evening.getActions().size());
        settled(home.executeScene(evening));

        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        assertTrue(settled(home.read(lamp, svc, Trait.ON_OFF)).get()
                .getValue().getBoolean());
        assertEquals(30.0, settled(home.read(lamp, svc, Trait.BRIGHTNESS))
                .get().getValue().getDouble(TraitUnit.PERCENT), 0.0001);
    }

    @Test
    void aSceneCanBeCreatedFromWritesAndThenRun() {
        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        List<SceneAction> actions = new ArrayList<SceneAction>();
        actions.add(new TraitWrite(lamp, svc, Trait.BRIGHTNESS,
                TraitValue.of(15, TraitUnit.PERCENT)).toSceneAction());
        AsyncResource<Scene> created = settled(home.createScene(
                home.getPrimaryStructure(), "Reading", actions));
        assertTrue(created.isReady());
        assertNotNull(created.get());
        assertEquals("Reading", created.get().getName());

        settled(home.refresh());
        Scene reading = null;
        for (Scene s : home.getPrimaryStructure().getScenes()) {
            if ("Reading".equals(s.getName())) {
                reading = s;
            }
        }
        assertNotNull(reading, "a created scene must appear in the graph");
        settled(home.executeScene(reading));
        assertEquals(15.0, settled(home.read(lamp, svc, Trait.BRIGHTNESS))
                .get().getValue().getDouble(TraitUnit.PERCENT), 0.0001);
    }

    // ------------------------------------------------------------------
    // commissioning
    // ------------------------------------------------------------------

    @Test
    void commissioningAddsAnAccessoryTheAppCanThenUse() {
        AsyncResource<CommissioningResult> r =
                settled(home.getCommissioner().commission(
                        new CommissioningRequest()
                                .setStructure(home.getPrimaryStructure())
                                .setRoomId("room-kitchen")
                                .setSuggestedName("Kettle")));
        assertTrue(r.isReady());
        CommissioningResult result = r.get();
        assertTrue(result.wasCommissionedToThisApp());
        assertNotNull(result.getAccessoryId());
        assertEquals("Kettle", result.getAccessoryName());

        settled(home.refresh());
        assertNotNull(home.findAccessory(result.getAccessoryId()),
                "a commissioned accessory has to appear in the graph");
    }

    // ------------------------------------------------------------------
    // availability scripting
    // ------------------------------------------------------------------

    /**
     * The reason the simulator is worth having: an app's COMMISSIONING_ONLY
     * branch is otherwise only reachable on an Android device in exactly the
     * wrong configuration.
     */
    @Test
    void everyAvailabilityStateCanBeScripted() {
        bridge.setAvailability(HomeAvailability.PERMISSION_REQUIRED);
        assertSame(HomeAvailability.PERMISSION_REQUIRED,
                home.getAvailability());
        bridge.setAvailability(HomeAvailability.COMMISSIONING_ONLY);
        assertSame(HomeAvailability.COMMISSIONING_ONLY,
                home.getAvailability());
        bridge.setAvailability(HomeAvailability.LOCAL_ONLY);
    }

    @Test
    void anAccessoryCanBeTakenOfflineAndBroughtBack() {
        Accessory lamp = home.findAccessory("lamp-living");
        AccessoryService svc = lamp.getPrimaryService();
        bridge.setReachable("lamp-living", false);
        assertSame(HomeError.ACCESSORY_UNREACHABLE,
                settled(home.read(lamp, svc, Trait.ON_OFF)).get().getError());
        bridge.setReachable("lamp-living", true);
        assertTrue(settled(home.read(lamp, svc, Trait.ON_OFF)).get()
                .hasValue());
    }

    @Test
    void structureListenersHearAboutReachability() {
        AtomicReference<HomeStructureEvent> seen =
                new AtomicReference<HomeStructureEvent>();
        HomeStructureListener listener = seen::set;
        home.addStructureListener(listener);
        bridge.setReachable("lamp-living", false);
        assertNotNull(seen.get());
        assertSame(StructureChangeKind.REACHABILITY_CHANGED,
                seen.get().getKind());
        assertEquals("lamp-living", seen.get().getAccessoryId());
        home.removeStructureListener(listener);
        bridge.setReachable("lamp-living", true);
    }

    private static <T> AsyncResource<T> settled(AsyncResource<T> resource) {
        return HomeAwait.settled(resource);
    }
}
