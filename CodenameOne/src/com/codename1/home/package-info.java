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

/// A cross-platform smart-home API: read the home's accessories, read and
/// write what they can do, watch them for change, run scenes, and add new
/// Matter accessories.
///
/// [com.codename1.home.SmartHome#getInstance()] is the single entry point and
/// never returns `null`. Ports without smart-home support return a fallback
/// whose operations fail fast with [com.codename1.home.HomeError#NOT_SUPPORTED]
/// and whose graph accessors return empty lists, so calling code needs no
/// platform-specific `if`.
///
/// #### The model
///
/// A [com.codename1.home.HomeStructure] holds [com.codename1.home.HomeRoom]s
/// and [com.codename1.home.Accessory]s. An accessory has one or more
/// [com.codename1.home.AccessoryService]s -- a two-gang wall switch is one
/// accessory with two services -- and a service exposes
/// [com.codename1.home.Trait]s. A trait is a canonical capability, not a
/// platform identifier: [com.codename1.home.Trait#BRIGHTNESS] is the same
/// constant whether the accessory is behind HomeKit or Matter, and the port
/// maps it to `HMCharacteristicTypeBrightness` or Level Control's
/// `CurrentLevel` on your behalf.
///
/// Graph objects are immutable snapshots. Nothing on an
/// [com.codename1.home.Accessory] crosses into the platform when you call a
/// getter; when the topology moves, a
/// [com.codename1.home.HomeStructureListener] tells you to fetch it again.
///
/// #### Three things that will surprise you
///
/// **Android's default answer is not "available".** With no extra setup an
/// Android app can commission a Matter accessory into the user's Google Home
/// and can do nothing else -- the graph is empty and no trait can be read or
/// written. That state is
/// [com.codename1.home.HomeAvailability#COMMISSIONING_ONLY], and it is
/// reported honestly rather than dressed up as a working home. The full
/// accessory graph on Android needs the Google Home APIs, which need a Google
/// Cloud project and a Home Developer Console registration that only you can
/// create. See [com.codename1.home.SmartHome#getConfigurationProblems()].
///
/// **Nothing wakes your app for an accessory change.** HomeKit delivers
/// changes only while your app is running in the foreground, and the Google
/// Home APIs need a live signed-in client. The home hub, not your app, is what
/// runs automations while the phone sleeps. Ask
/// [com.codename1.home.TraitSubscription#isPushDelivery()] rather than
/// assuming; where it answers `false`, changes arrive when you call
/// [com.codename1.home.SmartHome#drainChanges()] and at no other time.
///
/// **Commissioning may not give you a device you can control.** Adding an
/// accessory through Google Play services puts it in the user's Google Home
/// and tells your app nothing more. Check
/// [com.codename1.home.commissioning.CommissioningResult#wasCommissionedToThisApp()]
/// instead of assuming the returned accessory id is usable.
///
/// #### Threading
///
/// Every method may be called from the EDT and returns immediately. Every
/// result and every listener delivery arrives on the EDT, on every platform --
/// including the desktop, simulator and JavaScript ports, which marshal rather
/// than answering on whichever thread happened to ask. A callback may touch
/// components directly.
///
/// #### Not claimed in this release
///
/// Automations, triggers and conditions (scenes only); topology writes
/// (creating homes, renaming rooms, moving accessories); cameras and video;
/// security and alarm panels; Matter events, which is why
/// [com.codename1.home.LockState#JAMMED] is unreachable outside HomeKit;
/// energy, appliance and diagnostic clusters. Codename One is not a Matter
/// controller: everything Matter goes through the OS ecosystem, so the Apple
/// Home or Google Home app has to be installed and set up.
package com.codename1.home;
