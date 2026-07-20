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
package com.codename1.impl.javase.bluetooth;

import com.codename1.impl.bluetooth.NativeBleBridge;

/**
 * The JavaSE simulator's {@link NativeBleBridge}: every method binds straight
 * to the bundled {@code libcn1ble} shared library (a btleplug engine) through
 * JNI. The library also exports these {@code Java_..._JniBleBridge_*} entry
 * points, so the JavaSE port needs no separate C shim -- it just
 * {@link BleLibraryResolver#load() System.load}s the library.
 *
 * <p>Call {@link #isLibraryAvailable()} before constructing/using an instance;
 * when it returns {@code false} the host has no bundled library or no radio
 * and the caller must stay on the simulator backend. The heavy protocol/GATT
 * logic lives in {@code com.codename1.impl.bluetooth.NativeBleBackend}, which
 * wraps this bridge.</p>
 */
final class JniBleBridge implements NativeBleBridge {

    /** True once {@code libcn1ble} has been located and loaded for this host. */
    static boolean isLibraryAvailable() {
        return BleLibraryResolver.load();
    }

    /** Human-readable trace of the library locations that were tried. */
    static String describeResolution() {
        return BleLibraryResolver.describeResolution();
    }

    /**
     * Test-only: switches the loaded engine into a deterministic scripted
     * responder that services one virtual peripheral entirely in-library --
     * no real radio, no BlueZ/D-Bus, no CoreBluetooth. It lets an integration
     * test drive the WHOLE native stack (JNI marshalling, the engine's event
     * channel, the JSON event shapes) through scan / connect / discover /
     * read / write / subscribe / notify deterministically on any host. Must
     * be called before {@link #start()}. Inert in production -- nothing in the
     * shipped framework calls it.
     */
    static native void enableTestMode();

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
    public native void read(long id, String address, String service,
            String characteristic);

    @Override
    public native void write(long id, String address, String service,
            String characteristic, byte[] value, boolean noResponse);

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
