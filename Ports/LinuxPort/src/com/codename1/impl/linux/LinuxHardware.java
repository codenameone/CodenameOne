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
package com.codename1.impl.linux;

import com.codename1.impl.KernelHardware;

/// The Linux port's [KernelHardware], shared by the desktop and the embedded
/// Linux targets: a desktop has no GPIO controller, but a USB serial adapter or
/// a USB-to-I2C bridge works the same on either. The natives are in
/// `cn1_linux_hardware.c`.
class LinuxHardware extends KernelHardware {

    /// Listeners run on the event dispatch thread.
    @Override
    protected void deliver(Runnable callback) {
        com.codename1.ui.Display.getInstance().callSerially(callback);
    }

    @Override
    protected String rawList(int kind) {
        return LinuxNative.hwList(kind);
    }

    @Override
    protected int rawBacklight(int op, int value) {
        return LinuxNative.hwBacklight(op, value);
    }

    @Override
    protected int rawGpioOpen(String chip, int line, boolean output, boolean high, int bias, int edges) {
        // C reads the name up to its first NUL; a name holding one would open
        // a device other than the one named. -22 is EINVAL.
        if (chip == null || chip.indexOf('\0') >= 0) {
            return -22;
        }
        return LinuxNative.hwGpioOpen(chip, line, output, high, bias, edges);
    }

    @Override
    protected int rawGpioRead(int handle) {
        return LinuxNative.hwGpioRead(handle);
    }

    @Override
    protected int rawGpioWrite(int handle, boolean high) {
        return LinuxNative.hwGpioWrite(handle, high);
    }

    @Override
    protected int rawGpioWaitEdge(int handle, int timeoutMs, long[] timestampOut) {
        return LinuxNative.hwGpioWaitEdge(handle, timeoutMs, timestampOut);
    }

    @Override
    protected int rawI2cOpen(int bus, int address) {
        return LinuxNative.hwI2cOpen(bus, address);
    }

    @Override
    protected int rawI2cTransfer(int handle, byte[] write, int writeLen, byte[] read, int readLen) {
        return LinuxNative.hwI2cTransfer(handle, write, writeLen, read, readLen);
    }

    @Override
    protected int rawSpiOpen(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord) {
        return LinuxNative.hwSpiOpen(bus, chipSelect, mode, speedHz, bitsPerWord);
    }

    @Override
    protected int rawSpiTransfer(int handle, byte[] tx, byte[] rx, int len) {
        return LinuxNative.hwSpiTransfer(handle, tx, rx, len);
    }

    @Override
    protected int rawSerialOpen(String device, int baud, int dataBits, int parity, int stopBits) {
        // C reads the name up to its first NUL; a name holding one would open
        // a device other than the one named. -22 is EINVAL.
        if (device == null || device.indexOf('\0') >= 0) {
            return -22;
        }
        return LinuxNative.hwSerialOpen(device, baud, dataBits, parity, stopBits);
    }

    @Override
    protected int rawSerialRead(int handle, byte[] buf, int off, int len, int timeoutMs) {
        return LinuxNative.hwSerialRead(handle, buf, off, len, timeoutMs);
    }

    @Override
    protected int rawSerialWrite(int handle, byte[] buf, int off, int len) {
        return LinuxNative.hwSerialWrite(handle, buf, off, len);
    }

    @Override
    protected int rawPwmOpen(int chip, int channel) {
        return LinuxNative.hwPwmOpen(chip, channel);
    }

    @Override
    protected int rawPwmConfigure(int handle, long periodNanos, long dutyNanos, boolean enabled) {
        return LinuxNative.hwPwmConfigure(handle, periodNanos, dutyNanos, enabled);
    }

    @Override
    protected int rawCanOpen(String iface) {
        // C reads the name up to its first NUL; a name holding one would open
        // a device other than the one named. -22 is EINVAL.
        if (iface == null || iface.indexOf('\0') >= 0) {
            return -22;
        }
        return LinuxNative.hwCanOpen(iface);
    }

    @Override
    protected int rawCanSend(int handle, int id, boolean extended, byte[] data, int len) {
        return LinuxNative.hwCanSend(handle, id, extended, data, len);
    }

    @Override
    protected int rawCanReceive(int handle, int[] idOut, byte[] data, int timeoutMs) {
        return LinuxNative.hwCanReceive(handle, idOut, data, timeoutMs);
    }

    @Override
    protected int rawWatchdogOpen(int timeoutSeconds) {
        return LinuxNative.hwWatchdogOpen(timeoutSeconds);
    }

    @Override
    protected int rawWatchdogKick(int handle) {
        return LinuxNative.hwWatchdogKick(handle);
    }

    @Override
    protected void rawClose(int handle, boolean disarm) {
        LinuxNative.hwClose(handle, disarm);
    }
}
