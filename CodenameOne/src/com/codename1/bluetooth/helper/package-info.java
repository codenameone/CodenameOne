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
/// Shared implementation support for the ports that talk to the native
/// `cn1-ble-helper` subprocess (JavaSE real-hardware, Win32 and Linux).
/// Not part of the public Bluetooth API -- application code uses
/// `com.codename1.bluetooth` and its sub packages instead.
///
/// `HelperBleBackend` implements the BLE central protocol over a
/// line-delimited JSON transport, `HelperBluetooth`/`HelperBluetoothLE`
/// bridge it onto the core facades, and `Wire` is the codec. `HelperTransport`
/// abstracts the subprocess pipe so each port supplies its own launcher --
/// `ProcessBuilder` on JavaSE, a native `posix_spawn`/`CreateProcess` bridge
/// on the C-translated Win32/Linux ports (`NativeSubprocessTransport`).
package com.codename1.bluetooth.helper;
