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
package com.codename1.impl.javase;

import com.codename1.hardware.CanBus;
import com.codename1.hardware.CanFrame;
import com.codename1.hardware.GpioPin;
import com.codename1.hardware.Hardware;
import com.codename1.hardware.HardwareException;
import com.codename1.hardware.I2cDevice;
import com.codename1.hardware.SerialPort;
import com.codename1.hardware.SpiDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The simulator's virtual board, driven through the public hardware API the
/// way an application drives a real one.
class JavaSEHardwareTest {
    private final Hardware hw = new JavaSEHardware();

    @Test
    void reportsEveryFeature() {
        assertTrue(hw.isSupported(Hardware.FEATURE_GPIO | Hardware.FEATURE_I2C | Hardware.FEATURE_CAN));
        assertArrayEquals(new String[]{"gpiochip0"}, hw.listGpioChips());
    }

    @Test
    void outputReadsBackAndInputSeesSimulatedLevel() throws HardwareException {
        GpioPin out = hw.openGpioOutput("gpiochip0", 17, true);
        assertTrue(out.isHigh());
        out.setHigh(false);
        assertFalse(JavaSEHardware.getLevel(17));
        out.close();

        GpioPin in = hw.openGpioInput("gpiochip0", 5, GpioPin.BIAS_PULL_UP, GpioPin.EDGE_BOTH);
        JavaSEHardware.setInput("gpiochip0", 5, true);
        assertTrue(in.isHigh());
        JavaSEHardware.setInput("gpiochip0", 5, false);
        assertFalse(in.isHigh());
        in.close();
        assertThrows(HardwareException.class, () -> hw.openGpioOutput("gpiochip9", 1, false));
    }

    @Test
    void temperatureSensorAndEeprom() throws HardwareException {
        JavaSEHardware.setTemperature(25.0);
        I2cDevice tmp = hw.openI2c(1, 0x48);
        byte[] raw = tmp.readRegisters(0, 2);
        int value = ((raw[0] << 8) | (raw[1] & 0xff)) >> 4;
        assertEquals(25.0, value * 0.0625, 0.0001);
        tmp.close();

        I2cDevice eeprom = hw.openI2c(1, 0x50);
        eeprom.write(new byte[]{0x10, 1, 2, 3});
        assertArrayEquals(new byte[]{1, 2, 3}, eeprom.writeRead(new byte[]{0x10}, 3));
        eeprom.close();
        assertThrows(HardwareException.class, () -> hw.openI2c(1, 0x20));
    }

    @Test
    void loopbacks() throws HardwareException {
        SpiDevice spi = hw.openSpi(0, 0, 0, 1000000, 8);
        assertArrayEquals(new byte[]{9, 8, 7}, spi.transfer(new byte[]{9, 8, 7}));
        spi.close();

        SerialPort serial = hw.openSerial("/dev/ttySIM0", 115200, 8, SerialPort.PARITY_NONE, 1);
        serial.write("hi".getBytes(), 0, 2);
        byte[] buf = new byte[8];
        assertEquals(2, serial.read(buf, 0, buf.length, 1000));
        assertEquals("hi", new String(buf, 0, 2));
        assertEquals(0, serial.read(buf, 0, buf.length, 10));
        serial.close();

        CanBus can = hw.openCan("vcan0");
        can.send(new CanFrame(0x123, false, new byte[]{4, 5}));
        CanFrame f = can.receive(1000);
        assertEquals(0x123, f.getId());
        assertArrayEquals(new byte[]{4, 5}, f.getData());
        assertNull(can.receive(10));
        can.close();
    }

    @Test
    void backlightClamps() throws HardwareException {
        hw.setBacklightBrightness(400);
        assertEquals(255, hw.getBacklightBrightness());
        hw.setBacklightBrightness(-3);
        assertEquals(0, hw.getBacklightBrightness());
        hw.setBacklightBrightness(255);
    }
}
