/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/// Standard Bluetooth SIG health sensors -- heart-rate straps, power
/// meters, speed and cadence sensors, foot pods, thermometers, scales,
/// blood-pressure cuffs and glucose meters.
///
/// Start at [com.codename1.health.sensors.HealthSensors], reached from
/// [com.codename1.health.Health#getSensors()].
///
/// This layer is built entirely on `com.codename1.bluetooth.le` and needs
/// no platform health store, so it behaves identically on every port with
/// Bluetooth LE -- including the desktop and JavaScript ports, where no
/// health store exists at all. It needs Bluetooth permissions rather than
/// health ones, and an app that uses only this package is not treated as a
/// health-data app by the build server.
///
/// The measurement parsers are public and static so they can be unit
/// tested without hardware and reused by apps doing their own GATT work.
/// Each returns null rather than throwing on a malformed payload.
package com.codename1.health.sensors;
