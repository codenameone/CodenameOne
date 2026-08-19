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

/// What one [AccessoryService] is: HomeKit's `HMService` type, Matter's device
/// type on an endpoint.
///
/// As with [AccessoryCategory], this is for labelling rather than for deciding
/// what to call. Ask [AccessoryService#supports(Trait)].
public enum ServiceType {

    /// A dimmable or colour light.
    LIGHTBULB,

    /// An on-off switch.
    SWITCH,

    /// A mains socket.
    OUTLET,

    /// A thermostat.
    THERMOSTAT,

    /// A lock mechanism.
    LOCK_MECHANISM,

    /// A powered door.
    DOOR,

    /// A garage door opener.
    GARAGE_DOOR_OPENER,

    /// A blind, shade or curtain.
    WINDOW_COVERING,

    /// A fan.
    FAN,

    /// An air purifier.
    AIR_PURIFIER,

    /// A motion sensor.
    MOTION_SENSOR,

    /// An occupancy sensor. On Matter this and [#MOTION_SENSOR] are the same
    /// device type; see [Trait#MOTION_DETECTED].
    OCCUPANCY_SENSOR,

    /// A door or window contact sensor.
    CONTACT_SENSOR,

    /// A temperature sensor.
    TEMPERATURE_SENSOR,

    /// A humidity sensor.
    HUMIDITY_SENSOR,

    /// An ambient light sensor.
    LIGHT_SENSOR,

    /// A smoke alarm.
    SMOKE_SENSOR,

    /// A carbon monoxide alarm.
    CARBON_MONOXIDE_SENSOR,

    /// A water leak sensor.
    LEAK_SENSOR,

    /// An air quality sensor.
    AIR_QUALITY_SENSOR,

    /// A battery, reported as a service of its own so
    /// [Trait#BATTERY_LEVEL] and its siblings have somewhere to live.
    BATTERY,

    /// A speaker. See [Trait#VOLUME].
    SPEAKER,

    /// Anything else, including a service type one platform has and this API
    /// does not.
    OTHER
}
