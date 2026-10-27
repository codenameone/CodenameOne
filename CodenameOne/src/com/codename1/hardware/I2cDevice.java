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
package com.codename1.hardware;

/// A device on an I2C bus, opened through [Hardware#openI2c(int, int)].
///
/// The register helpers cover the common sensor convention: write the register
/// address, then read with a repeated start.
///
/// ```java
/// I2cDevice bme280 = Hardware.getInstance().openI2c(1, 0x76);
/// int chipId = bme280.readRegister(0xd0);   // 0x60 on a BME280
/// ```
@com.codename1.impl.SharedWithBackend
public final class I2cDevice {

    private final Hardware hw;
    private final int handle;
    private final int bus;
    private final int address;
    private boolean closed;

    I2cDevice(Hardware hw, int handle, int bus, int address) {
        this.hw = hw;
        this.handle = handle;
        this.bus = bus;
        this.address = address;
    }

    /// The bus number.
    public int getBus() {
        return bus;
    }

    /// The 7-bit device address.
    public int getAddress() {
        return address;
    }

    /// Writes `data` in one transaction.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the device does not acknowledge
    public void write(byte[] data) throws HardwareException {
        checkOpen();
        hw.i2cTransfer(handle, data, data.length, null, 0);
    }

    /// Reads `len` bytes in one transaction.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the device does not acknowledge
    public byte[] read(int len) throws HardwareException {
        checkOpen();
        byte[] out = new byte[len];
        hw.i2cTransfer(handle, null, 0, out, len);
        return out;
    }

    /// Writes `write`, then reads `readLen` bytes after a repeated start, as a
    /// single transaction with no stop condition between them.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the device does not acknowledge
    public byte[] writeRead(byte[] write, int readLen) throws HardwareException {
        checkOpen();
        byte[] out = new byte[readLen];
        hw.i2cTransfer(handle, write, write.length, out, readLen);
        return out;
    }

    /// Reads one 8-bit register.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the device does not acknowledge
    public int readRegister(int register) throws HardwareException {
        return writeRead(new byte[] {(byte) register}, 1)[0] & 0xff;
    }

    /// Reads `len` consecutive registers starting at `register`.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the device does not acknowledge
    public byte[] readRegisters(int register, int len) throws HardwareException {
        return writeRead(new byte[] {(byte) register}, len);
    }

    /// Writes one 8-bit register.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the device does not acknowledge
    public void writeRegister(int register, int value) throws HardwareException {
        write(new byte[] {(byte) register, (byte) value});
    }

    /// Releases the device. Closing twice is harmless.
    public void close() {
        if (!closed) {
            closed = true;
            hw.close(handle, false);
        }
    }

    private void checkOpen() throws HardwareException {
        if (closed) {
            throw new HardwareException("I2C device 0x" + Integer.toHexString(address) + " is closed");
        }
    }
}
