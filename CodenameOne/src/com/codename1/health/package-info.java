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

/// Cross-platform health data: reading and writing samples, aggregating
/// them over time, watching the store for changes, and recording workouts.
///
/// Start at [com.codename1.health.Health], which is never null on any
/// port. Backed by HealthKit on iOS and watchOS, by Health Connect on
/// Android, by a scriptable virtual store in the simulator, and by a local
/// store on the desktop and JavaScript ports.
///
/// #### Two platform truths this API does not hide
///
/// **Read authorization is unknowable on iOS.** HealthKit reports a denied
/// read as an empty result, deliberately, so that an app cannot infer what
/// a user is choosing to hide. There is therefore no `hasReadPermission`
/// anywhere in this package, and your UI must say "no data available"
/// rather than "you denied access".
///
/// **Android never wakes your app for new data.** Health Connect has no
/// push mechanism, so subscriptions there are polled when your app runs.
///
/// #### Related packages
///
/// - `com.codename1.health.workout` -- live and recorded workout sessions.
/// - `com.codename1.health.sensors` -- Bluetooth GATT health sensors,
///   which work on every port with Bluetooth LE regardless of whether a
///   health store exists.
package com.codename1.health;
