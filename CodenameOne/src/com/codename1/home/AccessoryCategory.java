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

/// Roughly what an [Accessory] is, for picking an icon and grouping a list.
///
/// **Do not branch capability on this.** What an accessory can actually do is
/// its [AccessoryService]s and their [Trait]s; a device that calls itself a
/// switch may well dim, and a light strip may report temperature. This is a
/// label for the user interface, and the two platforms' own categorizations
/// are only approximately alike.
public enum AccessoryCategory {

    /// A lamp, bulb or light strip.
    LIGHT,

    /// A wall switch or relay.
    SWITCH,

    /// A smart plug or socket.
    OUTLET,

    /// A thermostat.
    THERMOSTAT,

    /// A door lock.
    LOCK,

    /// A garage door opener.
    GARAGE_DOOR_OPENER,

    /// A blind, shade, curtain or shutter.
    WINDOW_COVERING,

    /// A sensor of any kind -- motion, contact, temperature, leak, air
    /// quality. Read the services to find out which.
    SENSOR,

    /// A fan.
    FAN,

    /// An air purifier.
    AIR_PURIFIER,

    /// A speaker. See [Trait#VOLUME] for why very few real devices land here.
    SPEAKER,

    /// A television or set-top box.
    TELEVISION,

    /// A camera. This release exposes no stream, snapshot or recording API;
    /// the category exists so a camera is visible in the graph rather than
    /// missing from it.
    CAMERA,

    /// A video or audio doorbell.
    DOORBELL,

    /// A hub that other accessories sit behind. Its children report
    /// [Accessory#isBridged()] and name it through
    /// [Accessory#getBridgeAccessoryId()].
    BRIDGE,

    /// An alarm panel. This release exposes no arm or disarm API; a
    /// half-working alarm is a safety problem rather than a feature gap.
    SECURITY_SYSTEM,

    /// Anything else, including a category one platform has and this API does
    /// not.
    OTHER
}
