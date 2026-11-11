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

import com.codename1.impl.KernelHardware;

/// The Windows port's [com.codename1.hardware.Hardware]. It reuses
/// [KernelHardware]'s error and edge protocol -- the natives in
/// `cn1_windows_hardware.cpp` answer negative Linux errno values -- and
/// replaces the parts that name Linux device nodes:
///
/// - Serial ports are `COM1`, `COM2`, ... on every Windows.
/// - GPIO, I2C, SPI and PWM go through `Windows.Devices.*`, which answer on a
///   Windows IoT Enterprise board whose BSP ships the bus drivers; an ordinary
///   PC lists none. There is one GPIO controller, listed as `gpio0`.
/// - The backlight is DDC/CI or the integrated panel's WMI brightness, on a
///   0..100 scale for the latter.
/// - CAN and the watchdog are vendor SDKs on Windows and are not offered.
class WindowsHardware extends KernelHardware {

    @Override
    protected int getSupportedFeatures() {
        return FEATURE_GPIO | FEATURE_I2C | FEATURE_SPI | FEATURE_SERIAL | FEATURE_PWM | FEATURE_BACKLIGHT;
    }

    /// Listeners run on the event dispatch thread.
    @Override
    protected void deliver(Runnable callback) {
        com.codename1.ui.Display.getInstance().callSerially(callback);
    }

    @Override
    public String[] listSerialPorts() {
        return lines(rawList(3));
    }

    @Override
    public String[] listSpiDevices() {
        // "spi-N" per bus; the chip select is chosen when a device is opened.
        return lines(rawList(2));
    }

    @Override
    public String[] listCanInterfaces() {
        return new String[0];
    }

    private static String[] lines(String s) {
        if (s == null || s.length() == 0) {
            return new String[0];
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int start = 0;
        int len = s.length();
        for (int i = 0; i <= len; i++) {
            if (i == len || s.charAt(i) == '\n') {
                if (i > start) {
                    out.add(s.substring(start, i));
                }
                start = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    @Override
    protected String rawList(int kind) {
        return WindowsNative.hwList(kind);
    }

    @Override
    protected int rawBacklight(int op, int value) {
        return WindowsNative.hwBacklight(op, value);
    }

    @Override
    protected int rawGpioOpen(String chip, int line, boolean output, boolean high, int bias, int edges) {
        return WindowsNative.hwGpioOpen(chip, line, output, high, bias, edges);
    }

    @Override
    protected int rawGpioRead(int handle) {
        return WindowsNative.hwGpioRead(handle);
    }

    @Override
    protected int rawGpioWrite(int handle, boolean high) {
        return WindowsNative.hwGpioWrite(handle, high);
    }

    @Override
    protected int rawGpioWaitEdge(int handle, int timeoutMs, long[] timestampOut) {
        return WindowsNative.hwGpioWaitEdge(handle, timeoutMs, timestampOut);
    }

    @Override
    protected int rawI2cOpen(int bus, int address) {
        return WindowsNative.hwI2cOpen(bus, address);
    }

    @Override
    protected int rawI2cTransfer(int handle, byte[] write, int writeLen, byte[] read, int readLen) {
        return WindowsNative.hwI2cTransfer(handle, write, writeLen, read, readLen);
    }

    @Override
    protected int rawSpiOpen(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord) {
        return WindowsNative.hwSpiOpen(bus, chipSelect, mode, speedHz, bitsPerWord);
    }

    @Override
    protected int rawSpiTransfer(int handle, byte[] tx, byte[] rx, int len) {
        return WindowsNative.hwSpiTransfer(handle, tx, rx, len);
    }

    @Override
    protected int rawSerialOpen(String device, int baud, int dataBits, int parity, int stopBits) {
        // C reads the name up to its first NUL; a name holding one would open
        // a device other than the one named. -22 is EINVAL.
        if (device == null || device.indexOf('\0') >= 0) {
            return -22;
        }
        return WindowsNative.hwSerialOpen(device, baud, dataBits, parity, stopBits);
    }

    @Override
    protected int rawSerialRead(int handle, byte[] buf, int off, int len, int timeoutMs) {
        return WindowsNative.hwSerialRead(handle, buf, off, len, timeoutMs);
    }

    @Override
    protected int rawSerialWrite(int handle, byte[] buf, int off, int len) {
        return WindowsNative.hwSerialWrite(handle, buf, off, len);
    }

    @Override
    protected int rawPwmOpen(int chip, int channel) {
        return WindowsNative.hwPwmOpen(chip, channel);
    }

    @Override
    protected int rawPwmConfigure(int handle, long periodNanos, long dutyNanos, boolean enabled) {
        return WindowsNative.hwPwmConfigure(handle, periodNanos, dutyNanos, enabled);
    }

    @Override
    protected int rawCanOpen(String iface) {
        return WindowsNative.hwCanOpen(iface);
    }

    @Override
    protected int rawCanSend(int handle, int id, boolean extended, byte[] data, int len) {
        return WindowsNative.hwCanSend(handle, id, extended, data, len);
    }

    @Override
    protected int rawCanReceive(int handle, int[] idOut, byte[] data, int timeoutMs) {
        return WindowsNative.hwCanReceive(handle, idOut, data, timeoutMs);
    }

    @Override
    protected int rawWatchdogOpen(int timeoutSeconds) {
        return WindowsNative.hwWatchdogOpen(timeoutSeconds);
    }

    @Override
    protected int rawWatchdogKick(int handle) {
        return WindowsNative.hwWatchdogKick(handle);
    }

    @Override
    protected void rawClose(int handle, boolean disarm) {
        WindowsNative.hwClose(handle, disarm);
    }
}
