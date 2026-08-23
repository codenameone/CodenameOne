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
/// Ultra-wideband precision ranging: how far away another device is, and in
/// which direction.
///
/// UWB measures distance by timing a radio round trip, which is worth about
/// ten centimetres. That is a different kind of answer from a Bluetooth
/// signal-strength estimate, which is worth a few metres on a good day and
/// swings wildly when someone puts a hand over the phone -- so this is what
/// makes "unlock as I walk up to the door" and "point me at my bag" work at
/// all.
///
/// Start at [Ranging]. The shape of a session, and why it takes two steps,
/// is documented there.
///
/// #### What it costs to reference this package
///
/// On iOS the build links NearbyInteraction.framework and injects the two
/// Nearby Interaction privacy strings. On Android it adds the
/// `androidx.core.uwb` dependency and the `UWB_RANGING` permission, and
/// declares the UWB hardware feature as optional so the app still installs
/// on devices without the radio.
///
/// #### Hardware, not just platform
///
/// [Ranging#isSupported()] answers `false` on plenty of current phones --
/// iPhones before the 11, and most Android devices. Treat ranging as an
/// enhancement to a feature that also works without it rather than as the
/// feature itself.
package com.codename1.nearby.ranging;
