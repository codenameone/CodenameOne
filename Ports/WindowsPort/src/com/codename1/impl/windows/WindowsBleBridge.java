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
package com.codename1.impl.windows;

/**
 * Windows native port binding to the in-process {@code libcn1ble} BLE engine
 * (btleplug over WinRT). Every method is a {@code native} that ParparVM
 * translates to a C function in {@code nativeSources/cn1_windows_ble.c}, which
 * forwards straight to the corresponding {@code cn1ble_*} shared-library entry
 * point. This is the desktop-native counterpart of the JavaSE simulator's JNI
 * shim; both feed {@link com.codename1.impl.bluetooth.NativeBleBackend}.
 *
 * <p>The engine owns all state (adapter, peripherals, the event queue), so the
 * bridge instance itself is stateless -- the native functions ignore the
 * {@code this} object and act on the process-wide engine singleton.</p>
 */
public class WindowsBleBridge implements com.codename1.impl.bluetooth.NativeBleBridge {

    @Override
    public native boolean start();

    @Override
    public native boolean isAlive();

    @Override
    public native String pollEvent(long timeoutMillis);

    @Override
    public native void scanStart(long id, String serviceCsv);

    @Override
    public native void scanStop(long id);

    @Override
    public native void connect(long id, String address);

    @Override
    public native void disconnect(long id, String address);

    @Override
    public native void discover(long id, String address);

    @Override
    public native void read(long id, String address, String service, String characteristic);

    @Override
    public native void write(long id, String address, String service, String characteristic,
            byte[] value, boolean noResponse);

    @Override
    public native void subscribe(long id, String address, String service,
            String characteristic, boolean enable);

    @Override
    public native void readDescriptor(long id, String address, String service,
            String characteristic, String descriptor);

    @Override
    public native void writeDescriptor(long id, String address, String service,
            String characteristic, String descriptor, byte[] value);

    @Override
    public native void readRssi(long id, String address);

    @Override
    public native void close();
}
