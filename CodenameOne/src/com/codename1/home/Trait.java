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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// One capability an accessory can have: a light's brightness, a lock's state,
/// a sensor's reading. Instances are interned constants, so `==` is a valid
/// identity test and is used throughout the API.
///
/// A trait is **canonical**, not a platform identifier. [#BRIGHTNESS] is the
/// same constant whether the accessory is behind HomeKit or Matter, and the
/// port maps it to `HMCharacteristicTypeBrightness` or to Level Control's
/// `CurrentLevel`. No `HMCharacteristicType` string and no Matter cluster id
/// ever reaches Java; the mapping tables live in the native bridges, where the
/// platform types are, and a test parses both of them to make sure neither has
/// drifted from this list.
///
/// #### Read the constant before you use it
///
/// The javadoc on each constant below is the specification, and most of them
/// carry a caveat that matters. Three kinds recur:
///
/// **Polarity.** Some values run in opposite directions on the two backends.
/// [#COVERING_POSITION] is the worst: HomeKit's 100 is fully open and Matter's
/// 0 is. This API picks one convention, the port inverts, and the constant
/// says which way it runs. Do not assume.
///
/// **Range and quantization.** Everything proportional is normalized to
/// percent here, but Matter carries brightness as 0 to 254 and covering
/// position as 0 to 10000, so a round trip is not the identity. Where an
/// accessory declares a real range or step, it is on the
/// [TraitConstraint], not here.
///
/// **Absence.** Several traits genuinely do not exist on one backend --
/// [#OUTLET_IN_USE] and [#TARGET_HUMIDITY] have no Matter equivalent at all.
/// Those report [HomeError#TRAIT_NOT_SUPPORTED] rather than a fabricated
/// value. The constant says so.
///
/// #### Why this is a class and not an enum
///
/// The same reason `com.codename1.health.HealthDataType` is: the table grows,
/// each constant carries metadata, and [#forId(java.lang.String)] has to stay
/// total across versions so a port from a newer build naming a trait this one
/// does not have degrades to "unknown trait" rather than throwing.
///
/// There is deliberately no way to make a custom trait. A trait only means
/// something because both ports agree on how to map it; one this API does not
/// know is one no port can serve.
public final class Trait {

    private static final Map<String, Trait> BY_ID = new HashMap<String, Trait>();
    private static final List<Trait> ALL = new ArrayList<Trait>();

    private final String id;
    private final TraitValueKind kind;
    private final TraitUnit unit;
    private final boolean readOnly;
    private final double nominalMin;
    private final double nominalMax;
    private final boolean hasNominalRange;

    private Trait(String id, TraitValueKind kind, TraitUnit unit,
            boolean readOnly, double nominalMin, double nominalMax,
            boolean hasNominalRange) {
        this.id = id;
        this.kind = kind;
        this.unit = unit;
        this.readOnly = readOnly;
        this.nominalMin = nominalMin;
        this.nominalMax = nominalMax;
        this.hasNominalRange = hasNominalRange;
    }

    private static Trait define(String id, TraitValueKind kind, TraitUnit unit,
            boolean readOnly, double min, double max, boolean ranged) {
        Trait t = new Trait(id, kind, unit, readOnly, min, max, ranged);
        BY_ID.put(id, t);
        ALL.add(t);
        return t;
    }

    /// A read-only or writable boolean.
    private static Trait flag(String id, boolean readOnly) {
        return define(id, TraitValueKind.BOOLEAN, TraitUnit.NONE, readOnly, 0,
                0, false);
    }

    /// A read-only or writable member of one of this package's domain enums.
    private static Trait choice(String id, boolean readOnly) {
        return define(id, TraitValueKind.ENUM, TraitUnit.NONE, readOnly, 0, 0,
                false);
    }

    /// A proportion, 0 to 100 percent.
    private static Trait percent(String id, boolean readOnly) {
        return define(id, TraitValueKind.DOUBLE, TraitUnit.PERCENT, readOnly,
                0, 100, true);
    }

    /// A measured quantity with no fixed range.
    private static Trait measure(String id, TraitUnit unit, boolean readOnly) {
        return define(id, TraitValueKind.DOUBLE, unit, readOnly, 0, 0, false);
    }

    // ------------------------------------------------------------------
    // power
    // ------------------------------------------------------------------

    /// Whether the accessory is on.
    ///
    /// HomeKit spells this two ways -- `PowerState` on a lightbulb or switch,
    /// and `Active` on a fan v2, air purifier or heater-cooler -- and this one
    /// constant covers both; the port uses whichever characteristic the
    /// service actually has. Two traits here would be a HomeKit implementation
    /// detail leaking into portable code.
    ///
    /// Matter: On/Off cluster `0x0006`, `OnOff` attribute.
    ///
    /// Note that on Matter this is genuinely independent of
    /// [#BRIGHTNESS]: level 0 does not reliably mean off, and turning a light
    /// off does not zero its level. Write this trait to turn something off.
    public static final Trait ON_OFF = flag("on_off", false);

    /// Whether something is plugged into an outlet and drawing power.
    ///
    /// HomeKit: `OutletInUse`.
    ///
    /// **Absent on Matter and Google Home.** Matter's Electrical Power
    /// Measurement cluster answers a different question -- how much current is
    /// flowing -- and it is optional and rare besides. A lamp that is plugged
    /// in but switched off draws nothing, so deriving this from power draw
    /// would report an empty socket. The port answers
    /// [HomeError#TRAIT_NOT_SUPPORTED] rather than guessing.
    public static final Trait OUTLET_IN_USE = flag("outlet_in_use", true);

    // ------------------------------------------------------------------
    // lighting
    // ------------------------------------------------------------------

    /// How bright a light is, as a percentage.
    ///
    /// HomeKit: `Brightness`, already a percentage.
    ///
    /// Matter: Level Control `0x0008`, `CurrentLevel`, which runs 1 to 254
    /// with the real bounds in `MinLevel` and `MaxLevel`; written through
    /// `MoveToLevel`. **254 steps do not divide into 100**, so setting 33
    /// percent and reading it back can answer 33 or something adjacent. Do not
    /// compare a read against a value you wrote.
    public static final Trait BRIGHTNESS = percent("brightness", false);

    /// The hue of a colour light, in degrees around the colour wheel.
    ///
    /// HomeKit: `Hue`, already in arcdegrees.
    ///
    /// Matter: Color Control `0x0300`, `CurrentHue` -- a single byte covering
    /// the whole circle, so about 1.4 degrees per step. Where the accessory
    /// supports the enhanced-hue feature the port reads
    /// `EnhancedCurrentHue` instead and the resolution is much better.
    ///
    /// **A Matter light in XY or colour-temperature mode reports a stale hue.**
    /// The port checks `ColorMode` and reports no value rather than passing on
    /// a number that does not describe the light you are looking at.
    public static final Trait HUE =
            define("hue", TraitValueKind.DOUBLE, TraitUnit.ARC_DEGREE, false,
                    0, 360, true);

    /// How saturated a colour light is, as a percentage.
    ///
    /// HomeKit: `Saturation`. Matter: Color Control `0x0300`,
    /// `CurrentSaturation`, 0 to 254. Same quantization and the same
    /// colour-mode caveat as [#HUE].
    public static final Trait SATURATION = percent("saturation", false);

    /// The colour temperature of a white light, in mireds.
    ///
    /// HomeKit: `ColorTemperature`. Matter: Color Control `0x0300`,
    /// `ColorTemperatureMireds`, bounded by the accessory's own
    /// `ColorTempPhysicalMinMireds` and `ColorTempPhysicalMaxMireds`, which
    /// arrive on the [TraitConstraint].
    ///
    /// Mireds because that is what **both** platforms use natively, so nothing
    /// is converted on the way through. And the trap that catches everyone
    /// once: **a higher mired value is a warmer light.** 153 is a cold
    /// blue-white and 400 is candlelight. Use
    /// [TraitValue#getColorTemperatureKelvin()] if you would rather think in
    /// Kelvin.
    public static final Trait COLOR_TEMPERATURE =
            measure("color_temperature", TraitUnit.MIRED, false);

    /// Ambient light, in lux.
    ///
    /// HomeKit: `CurrentLightLevel`, already in lux.
    ///
    /// Matter: Illuminance Measurement `0x0400`, `MeasuredValue`, which is
    /// **logarithmically encoded** as `10000 * log10(lux) + 1` in a single
    /// 16-bit field -- so precision falls away sharply at the bright end. Its
    /// two sentinels, 0 for "too dark to measure" and 0xFFFF for "unknown",
    /// become no value rather than a reading of zero lux.
    public static final Trait CURRENT_LIGHT_LEVEL =
            measure("current_light_level", TraitUnit.LUX, true);

    // ------------------------------------------------------------------
    // climate
    // ------------------------------------------------------------------

    /// The temperature an accessory is measuring.
    ///
    /// HomeKit: `CurrentTemperature` on a sensor, or the thermostat's own.
    ///
    /// Matter: Temperature Measurement `0x0402` `MeasuredValue` for a sensor,
    /// or Thermostat `0x0201` `LocalTemperature` for a thermostat. Both are
    /// hundredths of a degree with 0x8000 meaning null, which becomes no
    /// value.
    public static final Trait CURRENT_TEMPERATURE =
            measure("current_temperature", TraitUnit.CELSIUS, true);

    /// The single setpoint that applies in the thermostat's current mode.
    ///
    /// #### The largest structural mismatch in this table
    ///
    /// HomeKit has one `TargetTemperature` characteristic. **Matter has no
    /// such thing** -- it has `OccupiedHeatingSetpoint` and
    /// `OccupiedCoolingSetpoint` and nothing that unifies them.
    ///
    /// So this trait is defined as *the setpoint that applies right now*: when
    /// [#TARGET_HEATING_COOLING] is [HeatingCoolingMode#HEAT] it is the
    /// heating setpoint, when it is [HeatingCoolingMode#COOL] it is the
    /// cooling one, and **when the mode is [HeatingCoolingMode#AUTO] this
    /// trait reports no value on either backend.** In auto there are two
    /// setpoints and answering with either one would silently be the wrong one
    /// half the time; use [#TARGET_HEATING_TEMPERATURE] and
    /// [#TARGET_COOLING_TEMPERATURE], which always mean exactly what they say.
    ///
    /// A thermostat UI that only ever offers one number will be wrong on an
    /// auto-mode thermostat no matter what this API does. Offer two.
    public static final Trait TARGET_TEMPERATURE =
            measure("target_temperature", TraitUnit.CELSIUS, false);

    /// The temperature below which heating runs.
    ///
    /// HomeKit: `HeatingThreshold`. Matter: Thermostat `0x0201`,
    /// `OccupiedHeatingSetpoint`. Clean on both sides.
    public static final Trait TARGET_HEATING_TEMPERATURE =
            measure("target_heating_temperature", TraitUnit.CELSIUS, false);

    /// The temperature above which cooling runs.
    ///
    /// HomeKit: `CoolingThreshold`. Matter: Thermostat `0x0201`,
    /// `OccupiedCoolingSetpoint`. Clean on both sides.
    public static final Trait TARGET_COOLING_TEMPERATURE =
            measure("target_cooling_temperature", TraitUnit.CELSIUS, false);

    /// What the thermostat is doing right now, as a [HeatingCoolingMode].
    ///
    /// HomeKit: `CurrentHeatingCooling`. Matter: Thermostat `0x0201`,
    /// `ThermostatRunningMode` where the accessory has it, falling back to
    /// `SystemMode`.
    ///
    /// **Never [HeatingCoolingMode#AUTO]**: at any instant a thermostat is
    /// heating, cooling or idle. Auto is a policy, and it lives on
    /// [#TARGET_HEATING_COOLING].
    public static final Trait CURRENT_HEATING_COOLING =
            choice("current_heating_cooling", true);

    /// What the thermostat has been asked to do, as a [HeatingCoolingMode].
    ///
    /// HomeKit: `TargetHeatingCooling`, four values. Matter: Thermostat
    /// `0x0201`, `SystemMode`, nine.
    ///
    /// The five Matter modes HomeKit cannot express -- emergency heat,
    /// precooling, fan-only, dry and sleep -- arrive as
    /// [HeatingCoolingMode#OTHER] with the platform's own ordinal on
    /// [TraitValue#getRawPlatformValue()], rather than being flattened into
    /// [HeatingCoolingMode#OFF]. Writing `OTHER` is refused.
    public static final Trait TARGET_HEATING_COOLING =
            choice("target_heating_cooling", false);

    /// The relative humidity an accessory is measuring, as a percentage.
    ///
    /// HomeKit: `CurrentRelativeHumidity`. Matter: Relative Humidity
    /// Measurement `0x0405`, `MeasuredValue`, in hundredths of a percent with
    /// 0xFFFF meaning null.
    public static final Trait CURRENT_HUMIDITY = percent("current_humidity",
            true);

    /// The relative humidity a humidifier or dehumidifier is aiming for.
    ///
    /// HomeKit: `TargetRelativeHumidity`.
    ///
    /// **Absent on Matter and Google Home.** There is no standard
    /// target-humidity cluster in the Matter revisions the shipping ecosystems
    /// support, so the port answers [HomeError#TRAIT_NOT_SUPPORTED].
    public static final Trait TARGET_HUMIDITY = percent("target_humidity",
            false);

    // ------------------------------------------------------------------
    // locks
    // ------------------------------------------------------------------

    /// What a door lock is doing, as a [LockState].
    ///
    /// HomeKit: `CurrentLockMechanismState`. Matter: Door Lock `0x0101`,
    /// `LockState`, which is nullable and whose null becomes no value.
    ///
    /// Note [LockState#JAMMED] is unreachable outside HomeKit and
    /// [LockState#PARTIALLY_LOCKED] is unreachable on HomeKit; the enum
    /// explains both.
    public static final Trait LOCK_STATE = choice("lock_state", true);

    /// Lock or unlock a door lock. Only [LockState#SECURED] and
    /// [LockState#UNSECURED] may be written.
    ///
    /// HomeKit: `TargetLockMechanismState`, an ordinary characteristic write.
    ///
    /// **Matter has no such attribute** -- locking is the `LockDoor` command
    /// and unlocking is `UnlockDoor` on Door Lock `0x0101`. That difference is
    /// invisible from here by design, with one exception that is not: a Matter
    /// lock configured with `RequirePINforRemoteOperation` will refuse an
    /// unlock without a credential and the write fails with
    /// [HomeError#PIN_REQUIRED]. Supply one through
    /// [TraitWrite#setAuthorizationData(java.lang.String)]. HomeKit never
    /// takes a PIN and ignores the field.
    public static final Trait TARGET_LOCK_STATE = choice("target_lock_state",
            false);

    // ------------------------------------------------------------------
    // doors and garage
    // ------------------------------------------------------------------

    /// What a door or garage door is doing, as a [DoorState].
    ///
    /// HomeKit: `CurrentDoorState`. Google Home: its own garage-door trait.
    /// **Matter has no standard garage cluster**, so this is unavailable on an
    /// Android build limited to [HomeAvailability#COMMISSIONING_ONLY].
    public static final Trait DOOR_STATE = choice("door_state", true);

    /// Open or close a door. Only [DoorState#OPEN] and [DoorState#CLOSED] may
    /// be written.
    ///
    /// HomeKit: `TargetDoorState`. Same backend availability as
    /// [#DOOR_STATE].
    public static final Trait TARGET_DOOR_STATE = choice("target_door_state",
            false);

    /// Whether something is blocking a door or covering.
    ///
    /// HomeKit: `ObstructionDetected`, on both doors and coverings.
    ///
    /// Matter: the obstacle bit of Window Covering `0x0102` `SafetyStatus` --
    /// **coverings only**, since there is no garage cluster to carry it.
    public static final Trait OBSTRUCTION_DETECTED =
            flag("obstruction_detected", true);

    // ------------------------------------------------------------------
    // window coverings
    // ------------------------------------------------------------------

    /// How far open a blind or shade is. **100 is fully open, 0 is fully
    /// closed.**
    ///
    /// #### The inversion to watch
    ///
    /// This is the likeliest source of a shipped bug in the whole package.
    /// HomeKit's `CurrentPosition` runs the way this constant does -- 100 is
    /// open. Matter's `CurrentPositionLiftPercent100ths` runs the **other**
    /// way, where 0 is fully open, and it is in hundredths of a percent
    /// besides, so the raw numbers differ by both a flip and a factor of a
    /// hundred.
    ///
    /// This API takes the HomeKit convention because it is what a slider
    /// labelled "open" wants, and the Android bridge inverts. Nothing in
    /// application code should ever see Matter's polarity -- but if you are
    /// reading a Matter trace next to a Codename One log, that is why the two
    /// disagree.
    public static final Trait COVERING_POSITION =
            percent("covering_position", true);

    /// Move a blind or shade. **100 is fully open**, matching
    /// [#COVERING_POSITION]; read that constant's note.
    ///
    /// HomeKit: `TargetPosition`. Matter: `TargetPositionLiftPercent100ths`
    /// to read back what was asked for, and the `GoToLiftPercentage` command
    /// to write.
    public static final Trait TARGET_COVERING_POSITION =
            percent("target_covering_position", false);

    /// How far the slats of a blind are tilted, as a percentage.
    ///
    /// HomeKit models tilt as **two** characteristics in arcdegrees,
    /// `CurrentHorizontalTilt` and `CurrentVerticalTilt`, running -90 to 90.
    /// Matter models it as **one** percentage,
    /// `CurrentPositionTiltPercent100ths`.
    ///
    /// This API takes Matter's single-axis percentage. On iOS the port uses
    /// whichever axis the service exposes and maps the accessory's real degree
    /// range onto 0 to 100, publishing the degrees on the [TraitConstraint] so
    /// a UI can still label them. **Two-axis tilt is not supported**: an
    /// accessory offering both gets its horizontal axis, and there is no way
    /// to reach the other one from portable code.
    public static final Trait COVERING_TILT = percent("covering_tilt", true);

    /// Tilt the slats of a blind. Same single-axis model as
    /// [#COVERING_TILT]; read that constant's note.
    ///
    /// HomeKit: `TargetHorizontalTilt` or `TargetVerticalTilt`. Matter:
    /// `TargetPositionTiltPercent100ths` and the `GoToTiltPercentage` command.
    public static final Trait TARGET_COVERING_TILT =
            percent("target_covering_tilt", false);

    /// Which way a covering is moving, as a [PositionState].
    ///
    /// HomeKit: `PositionState`. Matter: the global bits of Window Covering
    /// `0x0102` `OperationalStatus`.
    ///
    /// Reported in terms of opening and closing, not of the position number
    /// rising or falling, because the two backends disagree about which
    /// direction that number runs.
    public static final Trait COVERING_MOTION = choice("covering_motion",
            true);

    // ------------------------------------------------------------------
    // sensors
    // ------------------------------------------------------------------

    /// Whether motion is being detected.
    ///
    /// HomeKit: `MotionDetected`, a characteristic of its own.
    ///
    /// **Matter has no motion cluster.** A Matter motion sensor is the
    /// Occupancy Sensor device type with a PIR sensor type, so on Matter and
    /// Google Home this trait and [#OCCUPANCY_DETECTED] read the same bit and
    /// always agree.
    ///
    /// Both are exposed anyway, rather than hiding one, because an app written
    /// against HomeKit's motion sensor should not have its feature vanish on
    /// Android. Just do not treat the two as independent signals.
    public static final Trait MOTION_DETECTED = flag("motion_detected", true);

    /// Whether a space is occupied.
    ///
    /// HomeKit: `OccupancyDetected`. Matter: Occupancy Sensing `0x0406`,
    /// bit 0 of `Occupancy`. See [#MOTION_DETECTED] for why the two coincide
    /// on Matter.
    public static final Trait OCCUPANCY_DETECTED =
            flag("occupancy_detected", true);

    /// Whether a contact sensor is **closed**. True means the door or window
    /// is shut.
    ///
    /// The polarity is worth stating twice because the platforms differ:
    /// HomeKit's `ContactState` is an inverted integer where 0 means closed,
    /// and Matter's Boolean State `0x0045` `StateValue` is a boolean where
    /// true means closed. This API follows Matter and the iOS port inverts.
    public static final Trait CONTACT_DETECTED = flag("contact_detected", true);

    /// Whether water is being detected.
    ///
    /// HomeKit: `LeakDetected`. Matter: the Water Leak Detector device type
    /// over Boolean State `0x0045`, where true means a leak.
    ///
    /// Note that Boolean State means the **opposite** thing here than it does
    /// for [#CONTACT_DETECTED], because its polarity is fixed by the device
    /// type rather than by the cluster. That is why the ports' mapping tables
    /// key on device type, cluster and attribute together, never on the
    /// cluster alone.
    public static final Trait LEAK_DETECTED = flag("leak_detected", true);

    /// Whether smoke is being detected, as an [AlarmState].
    ///
    /// HomeKit: `SmokeDetected`, two-state, so it can only ever report
    /// [AlarmState#NORMAL] or [AlarmState#CRITICAL].
    ///
    /// Matter: Smoke CO Alarm `0x005C`, `SmokeState`, three-state, so
    /// [AlarmState#WARNING] is reachable there.
    ///
    /// Read [AlarmState]'s note before building anything on this. It is not a
    /// fire-alarm channel.
    public static final Trait SMOKE_DETECTED = choice("smoke_detected", true);

    /// Whether carbon monoxide is being detected, as an [AlarmState].
    ///
    /// HomeKit: `CarbonMonoxideDetected`, two-state. Matter: Smoke CO Alarm
    /// `0x005C`, `COState`, three-state. Same asymmetry as
    /// [#SMOKE_DETECTED], and the same warning.
    public static final Trait CO_DETECTED = choice("co_detected", true);

    /// Measured carbon monoxide, in parts per million.
    ///
    /// HomeKit: `CarbonMonoxideLevel`, already ppm.
    ///
    /// Matter: Carbon Monoxide Concentration Measurement `0x040C`, whose
    /// `MeasurementUnit` is chosen by the accessory and may be a mass
    /// concentration. **Where the accessory reports mass per volume the port
    /// reports no value**, because converting needs the gas's molar mass and
    /// the ambient temperature and pressure, and inventing those would produce
    /// a number that looks like a measurement.
    public static final Trait CO_LEVEL = measure("co_level", TraitUnit.PPM,
            true);

    /// Measured carbon dioxide, in parts per million.
    ///
    /// HomeKit: `CarbonDioxideLevel`. Matter: Carbon Dioxide Concentration
    /// Measurement `0x040D`. Same unit caveat as [#CO_LEVEL].
    public static final Trait CO2_LEVEL = measure("co2_level", TraitUnit.PPM,
            true);

    /// A summarized air-quality rating, as an [AirQualityLevel].
    ///
    /// HomeKit: `AirQuality`, six levels. Matter: Air Quality `0x005B`, seven.
    /// The enum documents how they are reconciled and which constant HomeKit
    /// can never produce. Do not hard-code thresholds against it and expect
    /// them to describe the same air on both platforms.
    public static final Trait AIR_QUALITY = choice("air_quality", true);

    /// Fine particulate matter, in micrograms per cubic metre.
    ///
    /// HomeKit: `PM2_5Density`. Matter: PM2.5 Concentration `0x042A`, where
    /// the accessory's own `MeasurementUnit` has to match; see [#CO_LEVEL].
    public static final Trait PM2_5_DENSITY =
            measure("pm2_5_density", TraitUnit.MICROGRAM_PER_CUBIC_METER,
                    true);

    /// Coarse particulate matter, in micrograms per cubic metre.
    ///
    /// HomeKit: `PM10Density`. Matter: PM10 Concentration `0x042D`.
    public static final Trait PM10_DENSITY =
            measure("pm10_density", TraitUnit.MICROGRAM_PER_CUBIC_METER, true);

    /// Total volatile organic compounds.
    ///
    /// HomeKit reports these in micrograms per cubic metre; Matter's Total VOC
    /// Concentration `0x042E` is commonly in parts per billion. **The unit
    /// therefore varies by accessory**, which is exactly why
    /// [TraitValue#getDouble(TraitUnit)] makes you name the one you expect and
    /// refuses to convert between the two dimensions. Read
    /// [TraitValue#getUnit()] before you render this one.
    public static final Trait VOC_DENSITY =
            measure("voc_density", TraitUnit.MICROGRAM_PER_CUBIC_METER, true);

    // ------------------------------------------------------------------
    // power source
    // ------------------------------------------------------------------

    /// How much battery an accessory has left, as a percentage.
    ///
    /// HomeKit: `BatteryLevel`, already 0 to 100. Matter: Power Source
    /// `0x002F`, `BatPercentRemaining`, which is in **half percent** and runs
    /// 0 to 200. The port halves it; application code sees percent.
    public static final Trait BATTERY_LEVEL = percent("battery_level", true);

    /// Whether the battery is charging, as a [ChargingState].
    ///
    /// HomeKit: `ChargingState`. Matter: Power Source `0x002F`,
    /// `BatChargeState`. The enum documents which constants each backend can
    /// never produce -- notably that HomeKit cannot tell a full battery on a
    /// charger from one that is running down.
    public static final Trait BATTERY_CHARGING = choice("battery_charging",
            true);

    /// Whether the battery is low enough to want attention.
    ///
    /// HomeKit: `StatusLowBattery`, two-state. Matter: Power Source `0x002F`,
    /// `BatChargeLevel`, whose warning and critical levels both arrive here as
    /// true -- a small loss, and the only one on this trait.
    public static final Trait BATTERY_LOW = flag("battery_low", true);

    // ------------------------------------------------------------------
    // fans
    // ------------------------------------------------------------------

    /// How fast a fan is running, as a percentage.
    ///
    /// HomeKit: `RotationSpeed`, one characteristic for both reading and
    /// writing.
    ///
    /// Matter splits it: Fan Control `0x0202` has `PercentSetting` for what
    /// was asked and `PercentCurrent` for what the motor is actually doing.
    /// Writes go to the former and reads come from the latter, so **a read
    /// immediately after a write can still show the old speed** while the fan
    /// ramps. That is the accessory being honest, not a stale cache.
    public static final Trait FAN_SPEED = percent("fan_speed", false);

    /// How a fan is running, as a [FanMode].
    ///
    /// Matter: Fan Control `0x0202`, `FanMode`, seven values. HomeKit has no
    /// real equivalent, and the enum documents the translation -- including
    /// that low, medium and high are written as speeds on iOS and never read
    /// back. If your UI shows a speed, read [#FAN_SPEED] instead; it behaves
    /// the same everywhere.
    public static final Trait FAN_MODE = choice("fan_mode", false);

    // ------------------------------------------------------------------
    // speakers
    // ------------------------------------------------------------------

    /// Speaker volume, as a percentage.
    ///
    /// HomeKit: `Volume` on a speaker service. Matter: the speaker device
    /// type's Level Control, 0 to 254, with the same quantization as
    /// [#BRIGHTNESS].
    ///
    /// **Expect this to be unsupported on almost every real accessory.**
    /// HomeKit's speaker service is in practice only exposed by cameras and
    /// doorbells -- AirPlay speakers and HomePods are not HomeKit accessories
    /// and do not appear in the graph at all -- and Matter speakers barely
    /// exist in the field. This is not a media-playback API.
    public static final Trait VOLUME = percent("volume", false);

    /// Whether a speaker is muted.
    ///
    /// HomeKit: `Mute`, a plain boolean. Matter: the speaker device type uses
    /// On/Off, where **off means muted** -- inverted. The port flips it.
    ///
    /// Same availability caveat as [#VOLUME].
    public static final Trait MUTE = flag("mute", false);

    /// The token this trait crosses the native boundary as, and the stable
    /// name to use in build hints and persisted state.
    ///
    /// #### Returns
    ///
    /// the identifier, never `null`
    public String getId() {
        return id;
    }

    /// What sort of value this trait carries, and therefore which
    /// [TraitValue] getter reads it.
    ///
    /// #### Returns
    ///
    /// the kind, never `null`
    public TraitValueKind getValueKind() {
        return kind;
    }

    /// The unit a [TraitValueKind#DOUBLE] trait is expressed in.
    /// [TraitUnit#NONE] for every other kind.
    ///
    /// #### Returns
    ///
    /// the canonical unit, never `null`
    public TraitUnit getUnit() {
        return unit;
    }

    /// Whether this trait can only ever be read.
    ///
    /// A trait that is writable in principle may still be read-only on a
    /// particular accessory; that is a property of the
    /// [TraitConstraint], not of the trait. This answers the stronger
    /// question of whether writing it could ever mean anything.
    ///
    /// #### Returns
    ///
    /// `true` when no accessory can accept a write of this trait
    public boolean isReadOnly() {
        return readOnly;
    }

    /// Whether [#getNominalMinimum()] and [#getNominalMaximum()] mean
    /// anything.
    ///
    /// #### Returns
    ///
    /// `true` when this trait has a documented range
    public boolean hasNominalRange() {
        return hasNominalRange;
    }

    /// The bottom of this trait's documented range, in [#getUnit()].
    ///
    /// Documentation, not validation. What a given accessory will actually
    /// accept is on its [TraitConstraint], which is where a write is checked;
    /// a dimmer whose real floor is 10 percent says so there.
    ///
    /// #### Returns
    ///
    /// the nominal minimum, or zero when [#hasNominalRange()] is `false`
    public double getNominalMinimum() {
        return nominalMin;
    }

    /// The top of this trait's documented range, in [#getUnit()]. See
    /// [#getNominalMinimum()].
    ///
    /// #### Returns
    ///
    /// the nominal maximum, or zero when [#hasNominalRange()] is `false`
    public double getNominalMaximum() {
        return nominalMax;
    }

    /// Every trait this release knows, in declaration order.
    ///
    /// #### Returns
    ///
    /// an immutable list
    public static List<Trait> all() {
        return Collections.unmodifiableList(ALL);
    }

    /// Resolves a trait by its [#getId()], total: an unknown or `null` id
    /// answers `null` rather than throwing.
    ///
    /// Total because ids arrive from outside -- a persisted favourite, a
    /// build hint, a port built against a newer version of this table. An
    /// unrecognized one is a value to skip, not a reason to fail a decode that
    /// still has good rows in it.
    ///
    /// #### Parameters
    ///
    /// - `id`: a trait identifier, or `null`
    ///
    /// #### Returns
    ///
    /// the trait, or `null` when this release has no such trait
    public static Trait forId(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
