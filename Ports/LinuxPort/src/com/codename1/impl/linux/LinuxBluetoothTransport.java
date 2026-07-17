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
package com.codename1.impl.linux;

import com.codename1.bluetooth.helper.NativeSubprocessTransport;
import com.codename1.bluetooth.helper.NativeSubprocessTransportFactory;

import java.util.List;

/// The Linux native port's {@code cn1-ble-helper} transport: it bridges the
/// shared {@link NativeSubprocessTransport} line framing onto
/// {@link LinuxNative}'s {@code posix_spawn}-based process primitives.
final class LinuxBluetoothTransport extends NativeSubprocessTransport {

    LinuxBluetoothTransport(List<String> command) {
        super(command);
    }

    protected long rawSpawn(String[] argv) {
        return LinuxNative.procSpawn(argv);
    }

    protected int rawRead(long handle, byte[] buf, int off, int len) {
        return LinuxNative.procRead(handle, buf, off, len);
    }

    protected int rawWrite(long handle, byte[] buf, int off, int len) {
        return LinuxNative.procWrite(handle, buf, off, len);
    }

    protected void rawCloseStdin(long handle) {
        LinuxNative.procCloseStdin(handle);
    }

    protected void rawClose(long handle) {
        LinuxNative.procClose(handle);
    }

    protected int rawIsAlive(long handle) {
        return LinuxNative.procIsAlive(handle);
    }

    /// The factory the port wires into {@code HelperBluetooth}.
    static final class Factory extends NativeSubprocessTransportFactory {
        protected NativeSubprocessTransport createTransport(
                List<String> command) {
            return new LinuxBluetoothTransport(command);
        }

        protected String executableName(String basename) {
            return basename;
        }
    }
}
