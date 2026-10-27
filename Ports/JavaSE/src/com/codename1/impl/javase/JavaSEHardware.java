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

import com.codename1.hardware.Hardware;
import com.codename1.hardware.HardwareException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/// A virtual board for the simulator, so an application that drives hardware
/// can be developed and tested with none attached. It answers every feature:
///
/// - GPIO: `gpiochip0` with 32 lines. An output reads back what was written;
///   an input reads what [#setInput(String, int, boolean)] last set, which also
///   delivers an edge to a waiting listener.
/// - I2C bus 1: a TMP102-style temperature sensor at `0x48` (register 0 holds
///   the temperature, [#setTemperature(double)]) and a 256-byte EEPROM at `0x50`
///   (write the address byte, then data; or write the address and read).
/// - SPI `0.0`: a loopback, MISO wired to MOSI.
/// - Serial `/dev/ttySIM0`: a loopback, everything written is read back.
/// - PWM chip 0, channels 0 and 1, and CAN `vcan0`, which loops frames back.
/// - A watchdog that logs a simulated reset when it is not kicked in time.
/// - A backlight from 0 to 255.
///
/// Tests reach the same state through the static methods.
public class JavaSEHardware extends Hardware {

    /// Listeners run on the event dispatch thread.
    @Override
    protected void deliver(Runnable callback) {
        com.codename1.ui.Display.getInstance().callSerially(callback);
    }

    private static final int LINES = 32;
    private static final Object LOCK = new Object();
    private static final boolean[] PIN_LEVEL = new boolean[LINES];
    private static final ArrayDeque<long[]> EDGES = new ArrayDeque<long[]>();
    private static double temperature = 22.5;
    private static int backlight = 255;

    private final Map<Integer, Object[]> handles = new HashMap<Integer, Object[]>();
    private final byte[] eeprom = new byte[256];
    private int eepromPointer;
    private final ArrayDeque<Byte> serialLoop = new ArrayDeque<Byte>();
    private final ArrayDeque<Object[]> canLoop = new ArrayDeque<Object[]>();
    private int nextHandle = 1;

    /// Drives a simulated input line, delivering an edge to any listener.
    public static void setInput(String chip, int line, boolean high) {
        if (line < 0 || line >= LINES) {
            return;
        }
        synchronized (LOCK) {
            if (PIN_LEVEL[line] != high) {
                PIN_LEVEL[line] = high;
                EDGES.add(new long[]{line, high ? 1 : 0, System.nanoTime()});
                LOCK.notifyAll();
            }
        }
    }

    /// The level a simulated line is at, input or output.
    public static boolean getLevel(int line) {
        synchronized (LOCK) {
            return line >= 0 && line < LINES && PIN_LEVEL[line];
        }
    }

    /// Sets what the simulated temperature sensor at I2C `0x48` reports.
    public static void setTemperature(double celsius) {
        synchronized (LOCK) {
            temperature = celsius;
        }
    }

    @Override
    protected int getSupportedFeatures() {
        return FEATURE_GPIO | FEATURE_I2C | FEATURE_SPI | FEATURE_SERIAL | FEATURE_PWM | FEATURE_CAN
                | FEATURE_WATCHDOG | FEATURE_BACKLIGHT;
    }

    @Override
    public String[] listGpioChips() {
        return new String[]{"gpiochip0"};
    }

    @Override
    public int[] listI2cBuses() {
        return new int[]{1};
    }

    @Override
    public String[] listSpiDevices() {
        return new String[]{"0.0"};
    }

    @Override
    public String[] listSerialPorts() {
        return new String[]{"/dev/ttySIM0"};
    }

    @Override
    public String[] listCanInterfaces() {
        return new String[]{"vcan0"};
    }

    @Override
    public int getBacklightBrightness() {
        synchronized (LOCK) {
            return backlight;
        }
    }

    @Override
    public int getMaxBacklightBrightness() {
        return 255;
    }

    @Override
    public void setBacklightBrightness(int value) {
        synchronized (LOCK) {
            backlight = Math.max(0, Math.min(255, value));
        }
    }

    private int register(String kind, Object state) {
        synchronized (LOCK) {
            int h = nextHandle++;
            handles.put(h, new Object[]{kind, state});
            return h;
        }
    }

    private Object state(int handle, String kind) throws HardwareException {
        synchronized (LOCK) {
            Object[] h = handles.get(handle);
            if (h == null || !kind.equals(h[0])) {
                throw new HardwareException("Closed or invalid " + kind + " handle");
            }
            return h[1];
        }
    }

    @Override
    protected int gpioOpen(String chip, int line, boolean output, boolean initialHigh, int bias, int edges)
            throws HardwareException {
        if (!"gpiochip0".equals(chip) || line < 0 || line >= LINES) {
            throw new HardwareException("No such GPIO line " + chip + ":" + line);
        }
        if (output) {
            synchronized (LOCK) {
                PIN_LEVEL[line] = initialHigh;
            }
        }
        return register("gpio", Integer.valueOf(line));
    }

    @Override
    protected boolean gpioRead(int handle) throws HardwareException {
        return getLevel(((Integer) state(handle, "gpio")).intValue());
    }

    @Override
    protected void gpioWrite(int handle, boolean high) throws HardwareException {
        int line = ((Integer) state(handle, "gpio")).intValue();
        synchronized (LOCK) {
            PIN_LEVEL[line] = high;
        }
    }

    @Override
    protected int gpioWaitEdge(int handle, int timeoutMs, long[] timestampOut) throws HardwareException {
        int line = ((Integer) state(handle, "gpio")).intValue();
        long deadline = timeoutMs < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;
        synchronized (LOCK) {
            while (true) {
                for (java.util.Iterator<long[]> it = EDGES.iterator(); it.hasNext();) {
                    long[] e = it.next();
                    if (e[0] == line) {
                        it.remove();
                        if (timestampOut != null && timestampOut.length > 0) {
                            timestampOut[0] = e[2];
                        }
                        return e[1] == 1 ? EDGE_RESULT_RISING : EDGE_RESULT_FALLING;
                    }
                }
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    return EDGE_TIMEOUT;
                }
                try {
                    LOCK.wait(left);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return EDGE_TIMEOUT;
                }
            }
        }
    }

    @Override
    protected int i2cOpen(int bus, int address) throws HardwareException {
        if (bus != 1 || (address != 0x48 && address != 0x50)) {
            throw new HardwareException("No device at I2C " + bus + ":0x" + Integer.toHexString(address));
        }
        return register("i2c", Integer.valueOf(address));
    }

    @Override
    protected void i2cTransfer(int handle, byte[] write, int writeLen, byte[] read, int readLen)
            throws HardwareException {
        int address = ((Integer) state(handle, "i2c")).intValue();
        synchronized (LOCK) {
            if (address == 0x48) {
                // TMP102: 12-bit two's complement, 0.0625 C per bit, left aligned.
                int raw = (int) Math.round(temperature / 0.0625) << 4;
                for (int i = 0; i < readLen; i++) {
                    read[i] = (byte) (i == 0 ? raw >> 8 : i == 1 ? raw : 0);
                }
                return;
            }
            if (writeLen > 0) {
                eepromPointer = write[0] & 0xff;
                for (int i = 1; i < writeLen; i++) {
                    eeprom[eepromPointer] = write[i];
                    eepromPointer = (eepromPointer + 1) & 0xff;
                }
            }
            for (int i = 0; i < readLen; i++) {
                read[i] = eeprom[eepromPointer];
                eepromPointer = (eepromPointer + 1) & 0xff;
            }
        }
    }

    @Override
    protected int spiOpen(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord) throws HardwareException {
        if (bus != 0 || chipSelect != 0) {
            throw new HardwareException("No SPI device " + bus + "." + chipSelect);
        }
        return register("spi", null);
    }

    @Override
    protected void spiTransfer(int handle, byte[] tx, byte[] rx, int len) throws HardwareException {
        state(handle, "spi");
        if (rx != null && tx != null) {
            System.arraycopy(tx, 0, rx, 0, len);
        }
    }

    @Override
    protected int serialOpen(String device, int baud, int dataBits, int parity, int stopBits) throws HardwareException {
        if (!"/dev/ttySIM0".equals(device)) {
            throw new HardwareException("No serial port " + device);
        }
        return register("serial", null);
    }

    @Override
    protected int serialRead(int handle, byte[] buf, int off, int len, int timeoutMs) throws HardwareException {
        state(handle, "serial");
        long deadline = timeoutMs < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;
        synchronized (LOCK) {
            while (serialLoop.isEmpty()) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    return 0;
                }
                try {
                    LOCK.wait(left);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return 0;
                }
            }
            int n = 0;
            while (n < len && !serialLoop.isEmpty()) {
                buf[off + n++] = serialLoop.poll().byteValue();
            }
            return n;
        }
    }

    @Override
    protected void serialWrite(int handle, byte[] buf, int off, int len) throws HardwareException {
        state(handle, "serial");
        synchronized (LOCK) {
            for (int i = 0; i < len; i++) {
                serialLoop.add(Byte.valueOf(buf[off + i]));
            }
            LOCK.notifyAll();
        }
    }

    @Override
    protected int pwmOpen(int chip, int channel) throws HardwareException {
        if (chip != 0 || channel < 0 || channel > 1) {
            throw new HardwareException("No PWM channel " + chip + ":" + channel);
        }
        return register("pwm", null);
    }

    @Override
    protected void pwmConfigure(int handle, long periodNanos, long dutyNanos, boolean enabled) throws HardwareException {
        state(handle, "pwm");
    }

    @Override
    protected int canOpen(String iface) throws HardwareException {
        if (!"vcan0".equals(iface)) {
            throw new HardwareException("No CAN interface " + iface);
        }
        return register("can", null);
    }

    @Override
    protected void canSend(int handle, int id, boolean extended, byte[] data, int len) throws HardwareException {
        state(handle, "can");
        byte[] copy = new byte[len];
        System.arraycopy(data, 0, copy, 0, len);
        synchronized (LOCK) {
            canLoop.add(new Object[]{Integer.valueOf(id), Boolean.valueOf(extended), copy});
            LOCK.notifyAll();
        }
    }

    @Override
    protected int canReceive(int handle, int[] idOut, byte[] data, int timeoutMs) throws HardwareException {
        state(handle, "can");
        long deadline = timeoutMs < 0 ? Long.MAX_VALUE : System.currentTimeMillis() + timeoutMs;
        synchronized (LOCK) {
            while (canLoop.isEmpty()) {
                long left = deadline - System.currentTimeMillis();
                if (left <= 0) {
                    return -1;
                }
                try {
                    LOCK.wait(left);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
            Object[] f = canLoop.poll();
            byte[] payload = (byte[]) f[2];
            idOut[0] = ((Integer) f[0]).intValue();
            if (idOut.length > 1) {
                idOut[1] = ((Boolean) f[1]).booleanValue() ? 1 : 0;
            }
            System.arraycopy(payload, 0, data, 0, Math.min(payload.length, data.length));
            return payload.length;
        }
    }

    @Override
    protected int watchdogOpen(int timeoutSeconds) throws HardwareException {
        return register("watchdog", new long[]{System.currentTimeMillis(), Math.max(1, timeoutSeconds) * 1000L});
    }

    @Override
    protected void watchdogKick(int handle) throws HardwareException {
        long[] w = (long[]) state(handle, "watchdog");
        long now = System.currentTimeMillis();
        if (now - w[0] > w[1]) {
            System.out.println("[Hardware simulation] watchdog expired " + (now - w[0] - w[1])
                    + "ms before this kick: a real device would have reset");
        }
        w[0] = now;
    }

    @Override
    protected void close(int handle, boolean disarm) {
        synchronized (LOCK) {
            handles.remove(handle);
        }
    }
}
