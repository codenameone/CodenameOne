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

import com.codename1.bluetooth.helper.HelperBleBackend;

import java.util.List;

/**
 * The JavaSE simulator's binding of the shared, transport-agnostic
 * {@link HelperBleBackend} onto a real {@code cn1-ble-helper} subprocess.
 * All the protocol, GATT and lifecycle logic lives in the
 * {@code codenameone-native-ble} module; this thin class only supplies the
 * {@link ProcessTransportFactory} (the one place {@link ProcessBuilder}
 * lives on JavaSE) and re-exposes the host-specific binary-resolution
 * queries the port needs. {@code JavaSEBluetooth} constructs it unchanged
 * via {@code new NativeBleBackend()} / {@code switchBackend("native")}.
 */
class NativeBleBackend extends HelperBleBackend {

    private final ProcessTransportFactory transportFactory;

    /** Resolves the helper binary for this host; check {@link #isAvailable()}. */
    NativeBleBackend() {
        this(new ProcessTransportFactory());
    }

    /**
     * Unit-test seam: runs the given command line (a fake helper speaking
     * the wire protocol) instead of resolving a real binary.
     */
    NativeBleBackend(List<String> launchCommand) {
        this(new ProcessTransportFactory(launchCommand));
    }

    private NativeBleBackend(ProcessTransportFactory transportFactory) {
        super(transportFactory);
        this.transportFactory = transportFactory;
    }

    /** True when a helper binary was located for this host. */
    boolean isAvailable() {
        return transportFactory.isAvailable();
    }

    /** Human-readable trace of the binary locations that were tried. */
    String describeResolution() {
        return transportFactory.describeResolution();
    }
}
