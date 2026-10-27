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
package com.codename1.backend;

import com.codename1.hardware.Hardware;
import com.codename1.impl.KernelHardware;

/// The headless backend's [Hardware]: the same kernel interfaces as the Linux
/// port, through the same C (`cn1_hardware_impl.h`, wrapped in
/// `cn1_backend_hardware.c`). A gateway or sensor hub serves its REST API and
/// drives its buses from one binary. Listener callbacks run on the pin's own
/// watcher thread, since a server has no event dispatch thread. Built for a
/// host other than Linux every primitive answers `-ENOSYS`.
public final class BoardHardware extends KernelHardware {
    private static boolean installed;

    private BoardHardware() {
    }

    /// Makes this the instance `Hardware.getInstance()` returns. The server
    /// calls it as it starts; calling it again does nothing.
    public static void install() {
        if (!installed) {
            installed = true;
            Hardware.install(new BoardHardware());
        }
    }

    @Override
    protected String rawList(int kind) {
        return nList(kind);
    }

    private static native String nList(int kind);

    @Override
    protected int rawBacklight(int op, int value) {
        return nBacklight(op, value);
    }

    private static native int nBacklight(int op, int value);

    @Override
    protected int rawGpioOpen(String chip, int line, boolean output, boolean high, int bias, int edges) {
        // C reads the name up to its first NUL; a name holding one would open
        // a device other than the one named. -22 is EINVAL.
        if (chip == null || chip.indexOf('\0') >= 0) {
            return -22;
        }
        return nGpioOpen(chip, line, output, high, bias, edges);
    }

    private static native int nGpioOpen(String chip, int line, boolean output, boolean high, int bias, int edges);

    @Override
    protected int rawGpioRead(int handle) {
        return nGpioRead(handle);
    }

    private static native int nGpioRead(int handle);

    @Override
    protected int rawGpioWrite(int handle, boolean high) {
        return nGpioWrite(handle, high);
    }

    private static native int nGpioWrite(int handle, boolean high);

    @Override
    protected int rawGpioWaitEdge(int handle, int timeoutMs, long[] timestampOut) {
        return nGpioWaitEdge(handle, timeoutMs, timestampOut);
    }

    private static native int nGpioWaitEdge(int handle, int timeoutMs, long[] timestampOut);

    @Override
    protected int rawI2cOpen(int bus, int address) {
        return nI2cOpen(bus, address);
    }

    private static native int nI2cOpen(int bus, int address);

    @Override
    protected int rawI2cTransfer(int handle, byte[] write, int writeLen, byte[] read, int readLen) {
        return nI2cTransfer(handle, write, writeLen, read, readLen);
    }

    private static native int nI2cTransfer(int handle, byte[] write, int writeLen, byte[] read, int readLen);

    @Override
    protected int rawSpiOpen(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord) {
        return nSpiOpen(bus, chipSelect, mode, speedHz, bitsPerWord);
    }

    private static native int nSpiOpen(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord);

    @Override
    protected int rawSpiTransfer(int handle, byte[] tx, byte[] rx, int len) {
        return nSpiTransfer(handle, tx, rx, len);
    }

    private static native int nSpiTransfer(int handle, byte[] tx, byte[] rx, int len);

    @Override
    protected int rawSerialOpen(String device, int baud, int dataBits, int parity, int stopBits) {
        // C reads the name up to its first NUL; a name holding one would open
        // a device other than the one named. -22 is EINVAL.
        if (device == null || device.indexOf('\0') >= 0) {
            return -22;
        }
        return nSerialOpen(device, baud, dataBits, parity, stopBits);
    }

    private static native int nSerialOpen(String device, int baud, int dataBits, int parity, int stopBits);

    @Override
    protected int rawSerialRead(int handle, byte[] buf, int off, int len, int timeoutMs) {
        return nSerialRead(handle, buf, off, len, timeoutMs);
    }

    private static native int nSerialRead(int handle, byte[] buf, int off, int len, int timeoutMs);

    @Override
    protected int rawSerialWrite(int handle, byte[] buf, int off, int len) {
        return nSerialWrite(handle, buf, off, len);
    }

    private static native int nSerialWrite(int handle, byte[] buf, int off, int len);

    @Override
    protected int rawPwmOpen(int chip, int channel) {
        return nPwmOpen(chip, channel);
    }

    private static native int nPwmOpen(int chip, int channel);

    @Override
    protected int rawPwmConfigure(int handle, long periodNanos, long dutyNanos, boolean enabled) {
        return nPwmConfigure(handle, periodNanos, dutyNanos, enabled);
    }

    private static native int nPwmConfigure(int handle, long periodNanos, long dutyNanos, boolean enabled);

    @Override
    protected int rawCanOpen(String iface) {
        // C reads the name up to its first NUL; a name holding one would open
        // a device other than the one named. -22 is EINVAL.
        if (iface == null || iface.indexOf('\0') >= 0) {
            return -22;
        }
        return nCanOpen(iface);
    }

    private static native int nCanOpen(String iface);

    @Override
    protected int rawCanSend(int handle, int id, boolean extended, byte[] data, int len) {
        return nCanSend(handle, id, extended, data, len);
    }

    private static native int nCanSend(int handle, int id, boolean extended, byte[] data, int len);

    @Override
    protected int rawCanReceive(int handle, int[] idOut, byte[] data, int timeoutMs) {
        return nCanReceive(handle, idOut, data, timeoutMs);
    }

    private static native int nCanReceive(int handle, int[] idOut, byte[] data, int timeoutMs);

    @Override
    protected int rawWatchdogOpen(int timeoutSeconds) {
        return nWatchdogOpen(timeoutSeconds);
    }

    private static native int nWatchdogOpen(int timeoutSeconds);

    @Override
    protected int rawWatchdogKick(int handle) {
        return nWatchdogKick(handle);
    }

    private static native int nWatchdogKick(int handle);

    @Override
    protected void rawClose(int handle, boolean disarm) {
        nClose(handle, disarm);
    }

    private static native void nClose(int handle, boolean disarm);
}
