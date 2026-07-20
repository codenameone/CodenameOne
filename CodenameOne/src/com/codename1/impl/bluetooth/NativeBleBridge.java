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
package com.codename1.impl.bluetooth;

/// The in-process seam between {@link NativeBleBackend} and the native
/// `libcn1ble` engine (a [btleplug](https://github.com/deviceplug/btleplug)
/// bridge: CoreBluetooth on macOS, BlueZ on Linux, WinRT on Windows). Each
/// port supplies an implementation whose methods bind straight to the shared
/// library -- a JNI shim on the JavaSE simulator, ParparVM `nativeSources`
/// on the C-translated Windows/Linux ports.
///
/// Commands are fire-and-forget: each carries a positive, monotonic `id` and
/// is answered by exactly one terminal event that echoes it as `requestId`,
/// delivered on the {@link #pollEvent} stream (never by a callback on a
/// foreign thread -- the backend owns a single reader thread that drains
/// {@code pollEvent}, so the engine never re-enters the VM from its own
/// worker threads).
///
/// Internal to the native-backend implementation -- not a public API.
public interface NativeBleBridge {

    /// Brings the engine up (opens the shared library and the OS adapter
    /// manager) and returns an adapter-availability hint: {@code true} when a
    /// radio is present, {@code false} otherwise. Either way the engine is
    /// pollable and emits its capabilities + adapter-state handshake, so the
    /// backend always drains {@link #pollEvent} afterwards -- a radioless host
    /// reports {@code UNSUPPORTED} rather than a start failure. Called once
    /// before any command.
    boolean start();

    /// True while the engine is running and able to accept commands.
    boolean isAlive();

    /// Blocks up to {@code timeoutMillis} for the next event and returns it
    /// as a JSON object string; {@code null} on timeout (poll again), or an
    /// empty string once the engine has closed.
    String pollEvent(long timeoutMillis);

    // ------------------------------------------------------------------
    // commands (fire-and-forget; terminal event arrives via pollEvent)
    // ------------------------------------------------------------------

    /// Starts a scan; {@code serviceCsv} is a comma-separated list of service
    /// UUIDs to filter by advertised service (empty scans for all) -- a plain
    /// string keeps the native marshalling trivial on both JNI and ParparVM.
    void scanStart(long id, String serviceCsv);

    void scanStop(long id);

    void connect(long id, String address);

    void disconnect(long id, String address);

    void discover(long id, String address);

    void read(long id, String address, String service, String characteristic);

    /// Writes {@code value} raw (no Base64 -- the array crosses the native
    /// boundary directly); {@code noResponse} selects write-without-response.
    void write(long id, String address, String service, String characteristic,
            byte[] value, boolean noResponse);

    /// Subscribes ({@code enable}) or unsubscribes to notifications/indications.
    void subscribe(long id, String address, String service,
            String characteristic, boolean enable);

    void readDescriptor(long id, String address, String service,
            String characteristic, String descriptor);

    void writeDescriptor(long id, String address, String service,
            String characteristic, String descriptor, byte[] value);

    void readRssi(long id, String address);

    /// Signals the engine to shut down and releases it. After this
    /// {@link #pollEvent} returns the empty string.
    void close();
}
