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
/// Nearby devices: how far away one is, which one is yours, and how to send
/// it something.
///
/// The three questions are answered by three sub-packages, and they are
/// separate packages rather than one because **referencing a package is the
/// only opt-in there is**. The build server decides what native machinery an
/// app gets by scanning bytecode for these prefixes, and it has no way to
/// express an exclusion -- so an app that only wants to know how far away
/// its keyring tag is must not pay for the Play Services dependency and the
/// Wi-Fi permissions that device-to-device transport costs.
///
/// - [com.codename1.nearby.ranging] -- ultra-wideband precision ranging.
///   Distance to within about ten centimeters, and direction on hardware
///   that has the antennas for it.
/// - [com.codename1.nearby.companion] -- the OS-managed association between
///   this app and one particular accessory, which buys background presence
///   notifications and scanning that does not need location permission.
/// - [com.codename1.nearby.transport] -- moving bytes and files to a device
///   in the same room, with no access point and no internet. Same-ecosystem
///   only; the package documentation says why and what to use instead.
///
/// This package itself holds only what all three share: [NearbyError],
/// [NearbyException], [NearbyAvailability] and [NearbyPermission].
/// Referencing it alone costs nothing.
///
/// #### How this relates to what was already here
///
/// Ranging is not a replacement for `com.codename1.bluetooth` -- it needs
/// it. Both platforms require the two devices to swap a token over some
/// channel they already share before any radio ranging can start, and a GATT
/// characteristic is the usual channel. The two APIs are designed to be used
/// together.
///
/// Nor does any of this replace RSSI-based proximity: an app that only needs
/// "near or far" can read the signal strength of a
/// `com.codename1.bluetooth.le` advertisement on every device ever made,
/// where UWB needs hardware from 2019 onward.
package com.codename1.nearby;
