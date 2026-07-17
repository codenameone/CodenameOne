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

import com.codename1.bluetooth.helper.HelperTransport;
import com.codename1.bluetooth.helper.HelperTransportFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds {@link ProcessTransport}s for the JavaSE simulator's native
 * Bluetooth backend. Resolves the bundled {@code cn1-ble-helper} binary for
 * this host through the shared {@link HelperBinaryResolver} (so the
 * simulator and the native ports resolve identically), and exposes
 * {@link #isAvailable()} / {@link #describeResolution()} so the port can
 * refuse the switch with a helpful trace when no helper is found.
 */
class ProcessTransportFactory implements HelperTransportFactory {

    /** The resolved launch command, or {@code null} when unavailable. */
    private final List<String> command;
    private final String description;

    /** Resolves the helper binary for this host. */
    ProcessTransportFactory() {
        List<String> attempted = new ArrayList<String>();
        File helperBinary = HelperBinaryResolver.resolveHelperBinary(
                System.getProperty(HelperBinaryResolver.HELPER_PATH_PROPERTY),
                System.getProperty("os.name", ""),
                System.getProperty("os.arch", ""),
                System.getenv("PATH"), attempted);
        this.command = helperBinary != null
                ? Collections.singletonList(helperBinary.getAbsolutePath())
                : null;
        this.description = HelperBinaryResolver.join(attempted);
    }

    /**
     * Unit-test seam: runs the given command line (a fake helper speaking
     * the wire protocol) instead of resolving a real binary.
     */
    ProcessTransportFactory(List<String> launchCommand) {
        this.command = launchCommand;
        this.description = "explicit launch command (test)";
    }

    public HelperTransport create() {
        return new ProcessTransport(command);
    }

    /** True when a helper binary (or explicit command) was located. */
    boolean isAvailable() {
        return command != null;
    }

    /** Human-readable trace of the binary locations that were tried. */
    String describeResolution() {
        return description;
    }
}
