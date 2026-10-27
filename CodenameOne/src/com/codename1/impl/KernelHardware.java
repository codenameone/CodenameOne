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
package com.codename1.impl;

import com.codename1.hardware.Hardware;
import com.codename1.hardware.HardwareException;
import java.util.ArrayList;

/// [Hardware] over the Linux kernel's own interfaces: the GPIO character
/// device, i2c-dev, spidev, termios, sysfs PWM and backlight, SocketCAN and
/// `/dev/watchdog`. The Linux port and the headless backend both extend it and
/// supply only the `raw` primitives, which they implement over one shared C
/// body (`cn1_hardware_impl.h`), so the two cannot drift apart.
@SharedWithBackend
public abstract class KernelHardware extends Hardware {

    /// Linux `ETIMEDOUT`.
    private static final int ETIMEDOUT = 110;

    @Override
    protected int getSupportedFeatures() {
        return FEATURE_GPIO | FEATURE_I2C | FEATURE_SPI | FEATURE_SERIAL | FEATURE_PWM
                | FEATURE_CAN | FEATURE_WATCHDOG | FEATURE_BACKLIGHT;
    }

    private static int check(int r, String what) throws HardwareException {
        if (r < 0) {
            throw new HardwareException(what + " failed (errno " + (-r) + ")", -r);
        }
        return r;
    }

    private static String[] split(String s) {
        ArrayList<String> out = new ArrayList<String>();
        if (s != null) {
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
        }
        return out.toArray(new String[out.size()]);
    }

    @Override
    public String[] listGpioChips() {
        return split(rawList(0));
    }

    @Override
    public int[] listI2cBuses() {
        String[] names = split(rawList(1));
        int[] out = new int[names.length];
        int count = 0;
        for (String name : names) {
            try {
                out[count] = Integer.parseInt(name.substring(4));
                count++;
            } catch (NumberFormatException ignored) {
                // A non-numeric i2c- entry is not a bus node.
            }
        }
        int[] trimmed = new int[count];
        System.arraycopy(out, 0, trimmed, 0, count);
        return trimmed;
    }

    @Override
    public String[] listSpiDevices() {
        String[] names = split(rawList(2));
        for (int i = 0; i < names.length; i++) {
            names[i] = names[i].substring(6);
        }
        return names;
    }

    @Override
    public String[] listSerialPorts() {
        String[] names = split(rawList(3));
        for (int i = 0; i < names.length; i++) {
            names[i] = "/dev/" + names[i];
        }
        return names;
    }

    @Override
    public String[] listCanInterfaces() {
        return split(rawList(4));
    }

    @Override
    public int getBacklightBrightness() {
        int r = rawBacklight(0, 0);
        return r < 0 ? -1 : r;
    }

    @Override
    public int getMaxBacklightBrightness() {
        int r = rawBacklight(1, 0);
        return r < 0 ? -1 : r;
    }

    @Override
    public void setBacklightBrightness(int value) throws HardwareException {
        check(rawBacklight(2, value), "Backlight");
    }

    @Override
    protected int gpioOpen(String chip, int line, boolean output, boolean initialHigh, int bias, int edges) throws HardwareException {
        return check(rawGpioOpen(chip, line, output, initialHigh, bias, edges), "GPIO " + chip + "/" + line);
    }

    @Override
    protected boolean gpioRead(int handle) throws HardwareException {
        return check(rawGpioRead(handle), "GPIO read") != 0;
    }

    @Override
    protected void gpioWrite(int handle, boolean high) throws HardwareException {
        check(rawGpioWrite(handle, high), "GPIO write");
    }

    @Override
    protected int gpioWaitEdge(int handle, int timeoutMs, long[] timestampOut) throws HardwareException {
        int r = rawGpioWaitEdge(handle, timeoutMs, timestampOut);
        if (r == -ETIMEDOUT) {
            return EDGE_TIMEOUT;
        }
        return check(r, "GPIO edge wait") == 1 ? EDGE_RESULT_RISING : EDGE_RESULT_FALLING;
    }

    @Override
    protected int i2cOpen(int bus, int address) throws HardwareException {
        return check(rawI2cOpen(bus, address), "I2C bus " + bus);
    }

    @Override
    protected void i2cTransfer(int handle, byte[] write, int writeLen, byte[] read, int readLen) throws HardwareException {
        check(rawI2cTransfer(handle, write, writeLen, read, readLen), "I2C transfer");
    }

    @Override
    protected int spiOpen(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord) throws HardwareException {
        return check(rawSpiOpen(bus, chipSelect, mode, speedHz, bitsPerWord), "SPI " + bus + "." + chipSelect);
    }

    @Override
    protected void spiTransfer(int handle, byte[] tx, byte[] rx, int len) throws HardwareException {
        check(rawSpiTransfer(handle, tx, rx, len), "SPI transfer");
    }

    @Override
    protected int serialOpen(String device, int baud, int dataBits, int parity, int stopBits) throws HardwareException {
        return check(rawSerialOpen(device, baud, dataBits, parity, stopBits), "Serial " + device);
    }

    @Override
    protected int serialRead(int handle, byte[] buf, int off, int len, int timeoutMs) throws HardwareException {
        return check(rawSerialRead(handle, buf, off, len, timeoutMs), "Serial read");
    }

    @Override
    protected void serialWrite(int handle, byte[] buf, int off, int len) throws HardwareException {
        check(rawSerialWrite(handle, buf, off, len), "Serial write");
    }

    @Override
    protected int pwmOpen(int chip, int channel) throws HardwareException {
        return check(rawPwmOpen(chip, channel), "PWM " + chip + "/" + channel);
    }

    @Override
    protected void pwmConfigure(int handle, long periodNanos, long dutyNanos, boolean enabled) throws HardwareException {
        check(rawPwmConfigure(handle, periodNanos, dutyNanos, enabled), "PWM configure");
    }

    @Override
    protected int canOpen(String iface) throws HardwareException {
        return check(rawCanOpen(iface), "CAN " + iface);
    }

    @Override
    protected void canSend(int handle, int id, boolean extended, byte[] data, int len) throws HardwareException {
        check(rawCanSend(handle, id, extended, data, len), "CAN send");
    }

    @Override
    protected int canReceive(int handle, int[] idOut, byte[] data, int timeoutMs) throws HardwareException {
        int r = rawCanReceive(handle, idOut, data, timeoutMs);
        if (r == -ETIMEDOUT) {
            return -1;
        }
        return check(r, "CAN receive");
    }

    @Override
    protected int watchdogOpen(int timeoutSeconds) throws HardwareException {
        return check(rawWatchdogOpen(timeoutSeconds), "Watchdog");
    }

    @Override
    protected void watchdogKick(int handle) throws HardwareException {
        check(rawWatchdogKick(handle), "Watchdog kick");
    }

    @Override
    protected void close(int handle, boolean disarm) {
        rawClose(handle, disarm);
    }

    // The kernel primitives. Each answers a handle or a result >= 0, or -errno;
    // the runtime implements them over the shared C in cn1_hardware_impl.h.

    /// `kind`: 0 GPIO chips, 1 I2C buses, 2 SPI devices, 3 serial ports,
    /// 4 CAN interfaces. Names separated by newlines.
    protected abstract String rawList(int kind);

    /// `op`: 0 read, 1 maximum, 2 write `value`.
    protected abstract int rawBacklight(int op, int value);

    protected abstract int rawGpioOpen(String chip, int line, boolean output, boolean high, int bias, int edges);

    protected abstract int rawGpioRead(int handle);

    protected abstract int rawGpioWrite(int handle, boolean high);

    protected abstract int rawGpioWaitEdge(int handle, int timeoutMs, long[] timestampOut);

    protected abstract int rawI2cOpen(int bus, int address);

    protected abstract int rawI2cTransfer(int handle, byte[] write, int writeLen, byte[] read, int readLen);

    protected abstract int rawSpiOpen(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord);

    protected abstract int rawSpiTransfer(int handle, byte[] tx, byte[] rx, int len);

    protected abstract int rawSerialOpen(String device, int baud, int dataBits, int parity, int stopBits);

    protected abstract int rawSerialRead(int handle, byte[] buf, int off, int len, int timeoutMs);

    protected abstract int rawSerialWrite(int handle, byte[] buf, int off, int len);

    protected abstract int rawPwmOpen(int chip, int channel);

    protected abstract int rawPwmConfigure(int handle, long periodNanos, long dutyNanos, boolean enabled);

    protected abstract int rawCanOpen(String iface);

    protected abstract int rawCanSend(int handle, int id, boolean extended, byte[] data, int len);

    protected abstract int rawCanReceive(int handle, int[] idOut, byte[] data, int timeoutMs);

    protected abstract int rawWatchdogOpen(int timeoutSeconds);

    protected abstract int rawWatchdogKick(int handle);

    protected abstract void rawClose(int handle, boolean disarm);
}
