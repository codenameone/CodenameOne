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
/// Sending bytes and files to a device in the same room, with no access
/// point, no pairing and no internet.
///
/// Start at [NearbyTransport].
///
/// #### Read the limitation before designing around this
///
/// **This transport does not cross ecosystems.** It is Google's Nearby
/// Connections on Android and Apple's MultipeerConnectivity on iOS, and the
/// two share no wire protocol, so an Android phone and an iPhone will never
/// discover each other here no matter how the app is written. The API does
/// not hide that, because an API that looked portable and silently never
/// found the peer would be worse.
///
/// When both ends of the conversation are not the same platform, the
/// framework already has two options that do work across the divide:
/// `com.codename1.bluetooth.le.L2capChannel` for a raw byte stream over BLE,
/// and `com.codename1.io.bonjour` plus sockets when both devices share a
/// Wi-Fi network.
package com.codename1.nearby.transport;
