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
/// Companion-device association: telling the operating system which
/// accessory is yours, and getting privileges back for it.
///
/// Start at [CompanionDevices], which explains what association buys that an
/// ordinary Bluetooth scan does not -- OS-run presence watching, scanning
/// without location permission, and one honest consent prompt naming one
/// device.
///
/// An association is a durable relationship: it survives app restarts and
/// reboots, and ends only when the app drops it, the user revokes it in
/// system settings, or the app is uninstalled. Persist
/// [CompanionDevice#getId()] and look the device up again on the next
/// launch instead of asking the user to pick it twice.
package com.codename1.nearby.companion;
