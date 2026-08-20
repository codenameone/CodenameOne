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
package com.codename1.impl.home;

import com.codename1.home.AccessoryCategory;
import com.codename1.home.AlarmState;
import com.codename1.home.ChargingState;
import com.codename1.home.HeatingCoolingMode;
import com.codename1.home.LockState;
import com.codename1.home.PositionState;
import com.codename1.home.SceneType;
import com.codename1.home.ServiceType;
import com.codename1.home.Trait;
import com.codename1.home.TraitConstraint;
import com.codename1.home.TraitUnit;
import com.codename1.home.TraitValue;

/// Furnishes a [LocalHomeBridge] with a plausible house.
///
/// #### Chosen to be awkward, not to be tidy
///
/// A simulated home whose accessories all behave would let an app be written
/// that only works when everything behaves. So this one deliberately includes
/// the shapes that break naive code:
///
/// - a **two-gang wall switch**: one accessory with two services, both
///   exposing [Trait#ON_OFF], so code that writes to "the accessory" is
///   ambiguous and finds out here rather than in someone's hallway;
/// - a **bridged** pair of lights behind a hub, so an app that never surfaces
///   [com.codename1.home.Accessory#isBridged()] cannot explain why twelve
///   things went offline at once;
/// - an **unreachable** accessory, present in the graph and failing every
///   operation;
/// - a **thermostat in auto mode**, where [Trait#TARGET_TEMPERATURE] reports
///   no value and the two threshold traits are the only honest answer;
/// - a **sensor with no reading yet**, so
///   [com.codename1.home.TraitReading#hasValue()] is exercised on something
///   other than a failure;
/// - a **light with a real dimming floor**, so a slider built from
///   [TraitConstraint] differs from one built from the trait's nominal range.
public final class SyntheticHome {

    /// The identifier of the home this builds.
    public static final String STRUCTURE_ID = "home-1";

    private SyntheticHome() {
    }

    /// Fills an empty bridge with the house described in the class
    /// documentation.
    ///
    /// #### Parameters
    ///
    /// - `home`: the bridge to furnish
    public static void populate(LocalHomeBridge home) {
        home.addStructure(STRUCTURE_ID, "Home", true);
        home.addRoom(STRUCTURE_ID, "room-living", "Living Room");
        home.addRoom(STRUCTURE_ID, "room-kitchen", "Kitchen");
        home.addRoom(STRUCTURE_ID, "room-bedroom", "Bedroom");
        home.addRoom(STRUCTURE_ID, "room-hall", "Hall");

        addDimmableLight(home, "lamp-living", "Floor Lamp", "room-living");
        addColourLight(home, "lamp-bedroom", "Bedside Light", "room-bedroom");
        addTwoGangSwitch(home);
        addThermostat(home);
        addLock(home);
        addBlind(home);
        addMotionSensor(home);
        addAirQualitySensor(home);
        addBridgeAndItsLights(home);
        addUnreachableOutlet(home);

        home.addScene(STRUCTURE_ID, "scene-evening", "Evening",
                SceneType.USER_DEFINED.ordinal());
        home.addSceneAction(STRUCTURE_ID, "scene-evening", "lamp-living", "1",
                Trait.ON_OFF, TraitValue.of(true));
        home.addSceneAction(STRUCTURE_ID, "scene-evening", "lamp-living", "1",
                Trait.BRIGHTNESS, TraitValue.of(30, TraitUnit.PERCENT));
        home.addSceneAction(STRUCTURE_ID, "scene-evening", "blind-living", "1",
                Trait.TARGET_COVERING_POSITION,
                TraitValue.of(0, TraitUnit.PERCENT));

        home.addScene(STRUCTURE_ID, "scene-away", "Leaving",
                SceneType.DEPARTURE.ordinal());
        home.addSceneAction(STRUCTURE_ID, "scene-away", "lamp-living", "1",
                Trait.ON_OFF, TraitValue.of(false));
        home.addSceneAction(STRUCTURE_ID, "scene-away", "lock-front", "1",
                Trait.TARGET_LOCK_STATE, TraitValue.ofEnum(LockState.SECURED));
    }

    private static void addDimmableLight(LocalHomeBridge home, String id,
            String name, String roomId) {
        home.addAccessory(STRUCTURE_ID, id, name, roomId,
                AccessoryCategory.LIGHT.ordinal());
        home.addService(id, "1", name, ServiceType.LIGHTBULB.ordinal(), true);
        home.addTrait(id, "1",
                TraitConstraint.of(Trait.ON_OFF, true, true, true),
                TraitValue.of(false));
        // A floor of 10 percent, because real dimmers have one and a slider
        // built from the trait's nominal 0..100 would offer values this lamp
        // refuses.
        home.addTrait(id, "1",
                TraitConstraint.ranged(Trait.BRIGHTNESS, true, true, true, 10,
                        100, 1),
                TraitValue.of(60, TraitUnit.PERCENT));
    }

    private static void addColourLight(LocalHomeBridge home, String id,
            String name, String roomId) {
        home.addAccessory(STRUCTURE_ID, id, name, roomId,
                AccessoryCategory.LIGHT.ordinal());
        home.addService(id, "1", name, ServiceType.LIGHTBULB.ordinal(), true);
        home.addTrait(id, "1",
                TraitConstraint.of(Trait.ON_OFF, true, true, true),
                TraitValue.of(true));
        home.addTrait(id, "1",
                TraitConstraint.ranged(Trait.BRIGHTNESS, true, true, true, 1,
                        100, 1),
                TraitValue.of(85, TraitUnit.PERCENT));
        home.addTrait(id, "1",
                TraitConstraint.ranged(Trait.HUE, true, true, true, 0, 360, 1),
                TraitValue.of(38, TraitUnit.ARC_DEGREE));
        home.addTrait(id, "1",
                TraitConstraint.ranged(Trait.SATURATION, true, true, true, 0,
                        100, 1),
                TraitValue.of(45, TraitUnit.PERCENT));
        // 153 is a cold blue-white and 500 is candlelight; the value below is
        // a warm white, and it is worth remembering the number goes UP as the
        // light gets warmer.
        home.addTrait(id, "1",
                TraitConstraint.ranged(Trait.COLOR_TEMPERATURE, true, true,
                        true, 153, 500, 1),
                TraitValue.of(370, TraitUnit.MIRED));
    }

    /// One accessory, two services, both switchable -- the shape that makes
    /// "write ON_OFF to this accessory" ambiguous.
    private static void addTwoGangSwitch(LocalHomeBridge home) {
        home.addAccessory(STRUCTURE_ID, "switch-hall", "Hall Switch",
                "room-hall", AccessoryCategory.SWITCH.ordinal());
        home.addService("switch-hall", "1", "Ceiling",
                ServiceType.SWITCH.ordinal(), true);
        home.addTrait("switch-hall", "1",
                TraitConstraint.of(Trait.ON_OFF, true, true, true),
                TraitValue.of(false));
        home.addService("switch-hall", "2", "Porch",
                ServiceType.SWITCH.ordinal(), false);
        home.addTrait("switch-hall", "2",
                TraitConstraint.of(Trait.ON_OFF, true, true, true),
                TraitValue.of(true));
    }

    private static void addThermostat(LocalHomeBridge home) {
        home.addAccessory(STRUCTURE_ID, "thermostat", "Thermostat",
                "room-living", AccessoryCategory.THERMOSTAT.ordinal());
        home.addService("thermostat", "1", "Thermostat",
                ServiceType.THERMOSTAT.ordinal(), true);
        home.addTrait("thermostat", "1",
                TraitConstraint.of(Trait.CURRENT_TEMPERATURE, true, false,
                        true),
                TraitValue.of(20.5, TraitUnit.CELSIUS));
        home.addTrait("thermostat", "1",
                TraitConstraint.of(Trait.CURRENT_HUMIDITY, true, false, true),
                TraitValue.of(48, TraitUnit.PERCENT));
        home.addTrait("thermostat", "1",
                TraitConstraint.of(Trait.CURRENT_HEATING_COOLING, true, false,
                        true),
                TraitValue.ofEnum(HeatingCoolingMode.HEAT));
        home.addTrait("thermostat", "1",
                TraitConstraint.of(Trait.TARGET_HEATING_COOLING, true, true,
                        true),
                TraitValue.ofEnum(HeatingCoolingMode.AUTO));
        // In AUTO there is no single setpoint, so this trait is present and
        // reports no value -- which is the case a one-number thermostat UI
        // gets wrong. The two thresholds below are the honest answer.
        home.addTrait("thermostat", "1",
                TraitConstraint.ranged(Trait.TARGET_TEMPERATURE, true, true,
                        true, 10, 38, 0.5),
                null);
        home.addTrait("thermostat", "1",
                TraitConstraint.ranged(Trait.TARGET_HEATING_TEMPERATURE, true,
                        true, true, 10, 25, 0.5),
                TraitValue.of(19, TraitUnit.CELSIUS));
        home.addTrait("thermostat", "1",
                TraitConstraint.ranged(Trait.TARGET_COOLING_TEMPERATURE, true,
                        true, true, 18, 35, 0.5),
                TraitValue.of(24, TraitUnit.CELSIUS));
    }

    private static void addLock(LocalHomeBridge home) {
        home.addAccessory(STRUCTURE_ID, "lock-front", "Front Door",
                "room-hall", AccessoryCategory.LOCK.ordinal());
        home.addService("lock-front", "1", "Front Door",
                ServiceType.LOCK_MECHANISM.ordinal(), true);
        home.addTrait("lock-front", "1",
                TraitConstraint.of(Trait.LOCK_STATE, true, false, true),
                TraitValue.ofEnum(LockState.SECURED));
        home.addTrait("lock-front", "1",
                TraitConstraint.choices(Trait.TARGET_LOCK_STATE, true, true,
                        true, new int[] {LockState.SECURED.ordinal(),
                            LockState.UNSECURED.ordinal()}),
                TraitValue.ofEnum(LockState.SECURED));
        home.addService("lock-front", "2", "Battery",
                ServiceType.BATTERY.ordinal(), false);
        home.addTrait("lock-front", "2",
                TraitConstraint.ranged(Trait.BATTERY_LEVEL, true, false, true,
                        0, 100, 1),
                TraitValue.of(72, TraitUnit.PERCENT));
        home.addTrait("lock-front", "2",
                TraitConstraint.of(Trait.BATTERY_CHARGING, true, false, true),
                TraitValue.ofEnum(ChargingState.NOT_CHARGEABLE));
        home.addTrait("lock-front", "2",
                TraitConstraint.of(Trait.BATTERY_LOW, true, false, true),
                TraitValue.of(false));
    }

    private static void addBlind(LocalHomeBridge home) {
        home.addAccessory(STRUCTURE_ID, "blind-living", "Living Room Blind",
                "room-living", AccessoryCategory.WINDOW_COVERING.ordinal());
        home.addService("blind-living", "1", "Blind",
                ServiceType.WINDOW_COVERING.ordinal(), true);
        // 100 is fully open here, matching the trait's documented convention.
        home.addTrait("blind-living", "1",
                TraitConstraint.ranged(Trait.COVERING_POSITION, true, false,
                        true, 0, 100, 1),
                TraitValue.of(100, TraitUnit.PERCENT));
        home.addTrait("blind-living", "1",
                TraitConstraint.ranged(Trait.TARGET_COVERING_POSITION, true,
                        true, true, 0, 100, 1),
                TraitValue.of(100, TraitUnit.PERCENT));
        home.addTrait("blind-living", "1",
                TraitConstraint.of(Trait.COVERING_MOTION, true, false, true),
                TraitValue.ofEnum(PositionState.STOPPED));
        home.addTrait("blind-living", "1",
                TraitConstraint.of(Trait.OBSTRUCTION_DETECTED, true, false,
                        true),
                TraitValue.of(false));
    }

    private static void addMotionSensor(LocalHomeBridge home) {
        home.addAccessory(STRUCTURE_ID, "sensor-hall", "Hall Sensor",
                "room-hall", AccessoryCategory.SENSOR.ordinal());
        home.addService("sensor-hall", "1", "Motion",
                ServiceType.MOTION_SENSOR.ordinal(), true);
        home.addTrait("sensor-hall", "1",
                TraitConstraint.of(Trait.MOTION_DETECTED, true, false, true),
                TraitValue.of(false));
        home.addService("sensor-hall", "2", "Temperature",
                ServiceType.TEMPERATURE_SENSOR.ordinal(), false);
        // No starting value: a sensor that has not reported yet is a real
        // state, and it is the one that gets rendered as "0 degrees" by code
        // that never checks hasValue().
        home.addTrait("sensor-hall", "2",
                TraitConstraint.of(Trait.CURRENT_TEMPERATURE, true, false,
                        true),
                null);
    }

    private static void addAirQualitySensor(LocalHomeBridge home) {
        home.addAccessory(STRUCTURE_ID, "sensor-air", "Air Monitor",
                "room-kitchen", AccessoryCategory.SENSOR.ordinal());
        home.addService("sensor-air", "1", "Air Quality",
                ServiceType.AIR_QUALITY_SENSOR.ordinal(), true);
        home.addTrait("sensor-air", "1",
                TraitConstraint.of(Trait.AIR_QUALITY, true, false, true),
                TraitValue.ofEnum(
                        com.codename1.home.AirQualityLevel.FAIR));
        home.addTrait("sensor-air", "1",
                TraitConstraint.of(Trait.PM2_5_DENSITY, true, false, true),
                TraitValue.of(14,
                        TraitUnit.MICROGRAM_PER_CUBIC_METER));
        home.addTrait("sensor-air", "1",
                TraitConstraint.of(Trait.CO2_LEVEL, true, false, true),
                TraitValue.of(620, TraitUnit.PPM));
        home.addService("sensor-air", "2", "Smoke",
                ServiceType.SMOKE_SENSOR.ordinal(), false);
        home.addTrait("sensor-air", "2",
                TraitConstraint.of(Trait.SMOKE_DETECTED, true, false, true),
                TraitValue.ofEnum(AlarmState.NORMAL));
    }

    /// A hub and the two lights behind it, so an app that never surfaces
    /// bridging cannot explain a hub going offline.
    private static void addBridgeAndItsLights(LocalHomeBridge home) {
        home.addAccessory(STRUCTURE_ID, "hub", "Lighting Hub", null,
                AccessoryCategory.BRIDGE.ordinal());
        home.addService("hub", "1", "Hub", ServiceType.OTHER.ordinal(), true);
        addDimmableLight(home, "lamp-kitchen-1", "Kitchen Downlight 1",
                "room-kitchen");
        addDimmableLight(home, "lamp-kitchen-2", "Kitchen Downlight 2",
                "room-kitchen");
        home.setBridge("lamp-kitchen-1", "hub");
        home.setBridge("lamp-kitchen-2", "hub");
    }

    /// Present in the graph and failing every operation, which is a state an
    /// app has to render and almost never gets tested against.
    private static void addUnreachableOutlet(LocalHomeBridge home) {
        home.addAccessory(STRUCTURE_ID, "outlet-garage", "Garage Socket", null,
                AccessoryCategory.OUTLET.ordinal());
        home.addService("outlet-garage", "1", "Socket",
                ServiceType.OUTLET.ordinal(), true);
        home.addTrait("outlet-garage", "1",
                TraitConstraint.of(Trait.ON_OFF, true, true, true),
                TraitValue.of(false));
        home.setReachable("outlet-garage", false);
    }
}
