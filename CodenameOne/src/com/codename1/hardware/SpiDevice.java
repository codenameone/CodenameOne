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

/// A device on an SPI bus, opened through
/// [Hardware#openSpi(int, int, int, int, int)].
@com.codename1.impl.SharedWithBackend
public final class SpiDevice {

    private final Hardware hw;
    private final int handle;
    private final int bus;
    private final int chipSelect;
    private boolean closed;

    SpiDevice(Hardware hw, int handle, int bus, int chipSelect) {
        this.hw = hw;
        this.handle = handle;
        this.bus = bus;
        this.chipSelect = chipSelect;
    }

    /// The SPI controller number.
    public int getBus() {
        return bus;
    }

    /// The chip-select line.
    public int getChipSelect() {
        return chipSelect;
    }

    /// Clocks `tx` out and returns the bytes clocked in at the same time.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the transfer fails or the device is closed
    public byte[] transfer(byte[] tx) throws HardwareException {
        byte[] rx = new byte[tx.length];
        transfer(tx, rx);
        return rx;
    }

    /// Clocks `tx` out while filling `rx`; both must be the same length, and
    /// may be the same array.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the transfer fails or the device is closed
    public void transfer(byte[] tx, byte[] rx) throws HardwareException {
        if (closed) {
            throw new HardwareException("SPI device " + bus + "." + chipSelect + " is closed");
        }
        if (rx.length != tx.length) {
            throw new HardwareException("SPI tx and rx lengths differ");
        }
        hw.spiTransfer(handle, tx, rx, tx.length);
    }

    /// Releases the device. Closing twice is harmless.
    public void close() {
        if (!closed) {
            closed = true;
            hw.close(handle, false);
        }
    }
}
