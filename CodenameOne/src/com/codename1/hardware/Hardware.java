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


/// Entry point to the buses and pins of the device the application runs on:
/// GPIO lines, I2C, SPI, serial ports, PWM, CAN, the hardware watchdog and the
/// display backlight.
///
/// This is the API for boards such as the Raspberry Pi, i.MX, Rockchip and
/// other embedded Linux devices. Every method is available on every platform;
/// a port without the hardware reports it through [#isSupported(int)] and its
/// `open` methods throw [HardwareException], so the same application runs on a
/// phone, a desktop or the simulator.
///
/// ```java
/// Hardware hw = Hardware.getInstance();
/// try {
///     if (hw.isSupported(Hardware.FEATURE_GPIO)) {
///         final GpioPin led = hw.openGpioOutput("gpiochip0", 17, false);
///         led.setHigh(true);
///
///         GpioPin button = hw.openGpioInput("gpiochip0", 27, GpioPin.BIAS_PULL_UP, GpioPin.EDGE_FALLING);
///         button.setListener(new GpioListener() {
///             public void edgeDetected(GpioPin pin, boolean rising, long timestampNanos) {
///                 try {
///                     led.setHigh(!led.isHigh());
///                 } catch (HardwareException e) {
///                     Log.e(e);
///                 }
///             }
///         });
///     }
/// } catch (HardwareException e) {
///     Log.e(e);
/// }
/// ```
///
/// #### Threading
///
/// The transfer methods block for the duration of the bus transaction, which
/// is microseconds for GPIO, I2C and SPI. Serial and CAN reads can wait for
/// data; call them off the event dispatch thread, or use their timeout
/// arguments. Listener callbacks always arrive on the event dispatch thread.
///
/// #### For port implementors
///
/// A port subclasses this class and overrides the protected primitives, which
/// take and return integer handles. The device classes ([GpioPin],
/// [I2cDevice], ...) are written once over those primitives, so a port never
/// subclasses them. Every primitive's default throws [HardwareException].
@com.codename1.impl.SharedWithBackend
public class Hardware {

    /// GPIO lines through the GPIO character device.
    public static final int FEATURE_GPIO = 1;
    /// I2C buses.
    public static final int FEATURE_I2C = 2;
    /// SPI buses.
    public static final int FEATURE_SPI = 4;
    /// Serial ports (UART, USB serial adapters).
    public static final int FEATURE_SERIAL = 8;
    /// Hardware PWM channels.
    public static final int FEATURE_PWM = 16;
    /// CAN buses.
    public static final int FEATURE_CAN = 32;
    /// The hardware watchdog.
    public static final int FEATURE_WATCHDOG = 64;
    /// Display backlight brightness control.
    public static final int FEATURE_BACKLIGHT = 128;

    /// Edge-wait result: the wait timed out.
    protected static final int EDGE_TIMEOUT = -1;
    /// Edge-wait result: a falling edge.
    protected static final int EDGE_RESULT_FALLING = 0;
    /// Edge-wait result: a rising edge.
    protected static final int EDGE_RESULT_RISING = 1;

    private static final Hardware DEFAULT = new Hardware();
    private static Hardware installed;

    /// Ports construct subclasses; applications call [#getInstance()].
    protected Hardware() {
    }

    /// Returns the hardware entry point of the current port. On a port without
    /// hardware access this is a base instance that supports nothing, so
    /// callers never need a `null` check.
    public static Hardware getInstance() {
        Hardware h = installed;
        return h != null ? h : DEFAULT;
    }

    /// Makes `hardware` the instance [#getInstance()] returns; `null` restores
    /// the unsupported default. `Display` installs the port's instance when it
    /// starts, and the backend runtime installs its own, which is why this
    /// class never names `Display` itself.
    public static void install(Hardware hardware) {
        installed = hardware;
    }

    /// Hands a listener callback to the thread listeners run on. The client
    /// ports post it to the event dispatch thread; the default, used by the
    /// headless backend, runs it on the calling thread.
    protected void deliver(Runnable callback) {
        callback.run();
    }

    /// `true` when the port supports at least one hardware feature.
    public boolean isSupported() {
        return getSupportedFeatures() != 0;
    }

    /// `true` when the port supports the given feature, one of the `FEATURE_`
    /// constants. Support means the port can drive the bus; whether the board
    /// exposes one is answered by the matching `list` method.
    public boolean isSupported(int feature) {
        return (getSupportedFeatures() & feature) == feature;
    }

    /// Bit mask of the `FEATURE_` constants this port implements. Ports
    /// override this; the default supports nothing.
    protected int getSupportedFeatures() {
        return 0;
    }

    // ------------------------------------------------------------ discovery

    /// Names of the GPIO controllers on this device, for example `gpiochip0`.
    /// Empty when there are none or GPIO is unsupported.
    public String[] listGpioChips() {
        return new String[0];
    }

    /// Numbers of the I2C buses on this device (`/dev/i2c-N` on Linux).
    public int[] listI2cBuses() {
        return new int[0];
    }

    /// SPI devices as `bus.chipSelect` strings, for example `0.1`.
    public String[] listSpiDevices() {
        return new String[0];
    }

    /// Device paths of the serial ports on this device, for example
    /// `/dev/ttyAMA0` or `/dev/ttyUSB0`.
    public String[] listSerialPorts() {
        return new String[0];
    }

    /// Names of the CAN network interfaces, for example `can0`.
    public String[] listCanInterfaces() {
        return new String[0];
    }

    // ------------------------------------------------------------ factories

    /// Claims a GPIO line as an output.
    ///
    /// #### Parameters
    ///
    /// - `chip`: the controller name from [#listGpioChips()]
    ///
    /// - `line`: the line offset within the controller (the BCM number on a
    ///   Raspberry Pi's `gpiochip0`)
    ///
    /// - `initialHigh`: the level the line is driven to when claimed
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the line does not exist, is claimed by
    ///   another consumer, or GPIO is unsupported
    public GpioPin openGpioOutput(String chip, int line, boolean initialHigh) throws HardwareException {
        int h = gpioOpen(chip, line, true, initialHigh, GpioPin.BIAS_DEFAULT, GpioPin.EDGE_NONE);
        return new GpioPin(this, h, chip, line, true, initialHigh);
    }

    /// Claims a GPIO line as an input.
    ///
    /// #### Parameters
    ///
    /// - `chip`: the controller name from [#listGpioChips()]
    ///
    /// - `line`: the line offset within the controller
    ///
    /// - `bias`: one of the `GpioPin.BIAS_` constants
    ///
    /// - `edges`: which edges [GpioPin#setListener(GpioListener)] reports, one
    ///   of the `GpioPin.EDGE_` constants
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the line cannot be claimed
    public GpioPin openGpioInput(String chip, int line, int bias, int edges) throws HardwareException {
        int h = gpioOpen(chip, line, false, false, bias, edges);
        return new GpioPin(this, h, chip, line, false, false);
    }

    /// Opens the device at a 7-bit address on an I2C bus.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the bus does not exist or I2C is unsupported
    public I2cDevice openI2c(int bus, int address) throws HardwareException {
        if (address < 0 || address > 0x7f) {
            throw new HardwareException("I2C address out of range: " + address);
        }
        return new I2cDevice(this, i2cOpen(bus, address), bus, address);
    }

    /// Opens an SPI device.
    ///
    /// #### Parameters
    ///
    /// - `bus`: the SPI controller number
    ///
    /// - `chipSelect`: the chip-select line
    ///
    /// - `mode`: SPI mode 0 to 3 (clock polarity and phase)
    ///
    /// - `speedHz`: the clock rate
    ///
    /// - `bitsPerWord`: the word size, usually 8
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the device does not exist or rejects the
    ///   configuration
    public SpiDevice openSpi(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord) throws HardwareException {
        if (mode < 0 || mode > 3) {
            throw new HardwareException("SPI mode must be 0..3: " + mode);
        }
        return new SpiDevice(this, spiOpen(bus, chipSelect, mode, speedHz, bitsPerWord), bus, chipSelect);
    }

    /// Opens a serial port in raw mode.
    ///
    /// #### Parameters
    ///
    /// - `device`: the port, from [#listSerialPorts()]
    ///
    /// - `baud`: the baud rate, for example 9600 or 115200
    ///
    /// - `dataBits`: 5 to 8
    ///
    /// - `parity`: one of the `SerialPort.PARITY_` constants
    ///
    /// - `stopBits`: 1 or 2
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the port cannot be opened or the rate is
    ///   not supported
    public SerialPort openSerial(String device, int baud, int dataBits, int parity, int stopBits) throws HardwareException {
        return new SerialPort(this, serialOpen(device, baud, dataBits, parity, stopBits), device);
    }

    /// Opens a hardware PWM channel. The channel starts disabled.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the channel does not exist
    public PwmChannel openPwm(int chip, int channel) throws HardwareException {
        return new PwmChannel(this, pwmOpen(chip, channel), chip, channel);
    }

    /// Opens a CAN network interface. The interface must already be up and
    /// configured with its bit rate (`ip link set can0 up type can bitrate
    /// 500000`), which needs privileges an application normally lacks.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when the interface does not exist
    public CanBus openCan(String iface) throws HardwareException {
        return new CanBus(this, canOpen(iface), iface);
    }

    /// Opens and starts the hardware watchdog. From this call on the device
    /// resets unless [Watchdog#kick()] is called within the timeout.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when there is no watchdog or it is in use
    public Watchdog openWatchdog(int timeoutSeconds) throws HardwareException {
        return new Watchdog(this, watchdogOpen(timeoutSeconds));
    }

    /// The current backlight brightness, from `0` to
    /// [#getMaxBacklightBrightness()], or `-1` when not supported.
    public int getBacklightBrightness() {
        return -1;
    }

    /// The highest backlight brightness value, or `-1` when not supported.
    public int getMaxBacklightBrightness() {
        return -1;
    }

    /// Sets the backlight brightness, clamped to the valid range.
    ///
    /// #### Throws
    ///
    /// - `HardwareException`: when not supported or not permitted
    public void setBacklightBrightness(int value) throws HardwareException {
        throw unsupported("backlight");
    }

    // ----------------------------------------------------------- primitives

    /// Creates the exception the default primitives throw.
    protected static HardwareException unsupported(String what) {
        return new HardwareException(what + " is not supported on this platform");
    }

    /// Claims a GPIO line and returns its handle.
    protected int gpioOpen(String chip, int line, boolean output, boolean initialHigh, int bias, int edges) throws HardwareException {
        throw unsupported("GPIO");
    }

    /// Reads a GPIO line: `true` for high.
    protected boolean gpioRead(int handle) throws HardwareException {
        throw unsupported("GPIO");
    }

    /// Drives an output GPIO line.
    protected void gpioWrite(int handle, boolean high) throws HardwareException {
        throw unsupported("GPIO");
    }

    /// Waits up to `timeoutMs` for an edge on an input line. Returns one of
    /// [#EDGE_TIMEOUT], [#EDGE_RESULT_FALLING] or [#EDGE_RESULT_RISING] and
    /// stores the edge's timestamp in nanoseconds in `timestampOut[0]`.
    /// Called from a background thread, never the event dispatch thread.
    protected int gpioWaitEdge(int handle, int timeoutMs, long[] timestampOut) throws HardwareException {
        throw unsupported("GPIO");
    }

    /// Opens an I2C device and returns its handle.
    protected int i2cOpen(int bus, int address) throws HardwareException {
        throw unsupported("I2C");
    }

    /// Performs one combined I2C transaction: writes `writeLen` bytes from
    /// `write`, then, with a repeated start and no stop between, reads
    /// `readLen` bytes into `read`. Either length may be zero.
    protected void i2cTransfer(int handle, byte[] write, int writeLen, byte[] read, int readLen) throws HardwareException {
        throw unsupported("I2C");
    }

    /// Opens and configures an SPI device and returns its handle.
    protected int spiOpen(int bus, int chipSelect, int mode, int speedHz, int bitsPerWord) throws HardwareException {
        throw unsupported("SPI");
    }

    /// Performs one full-duplex SPI transfer of `len` bytes. `rx` may be the
    /// same array as `tx`.
    protected void spiTransfer(int handle, byte[] tx, byte[] rx, int len) throws HardwareException {
        throw unsupported("SPI");
    }

    /// Opens and configures a serial port and returns its handle.
    protected int serialOpen(String device, int baud, int dataBits, int parity, int stopBits) throws HardwareException {
        throw unsupported("Serial");
    }

    /// Reads up to `len` bytes, waiting at most `timeoutMs` (negative waits
    /// forever) for the first one. Returns the count read, `0` on timeout.
    protected int serialRead(int handle, byte[] buf, int off, int len, int timeoutMs) throws HardwareException {
        throw unsupported("Serial");
    }

    /// Writes all `len` bytes.
    protected void serialWrite(int handle, byte[] buf, int off, int len) throws HardwareException {
        throw unsupported("Serial");
    }

    /// Opens a PWM channel and returns its handle.
    protected int pwmOpen(int chip, int channel) throws HardwareException {
        throw unsupported("PWM");
    }

    /// Configures a PWM channel. Duty may not exceed the period.
    protected void pwmConfigure(int handle, long periodNanos, long dutyNanos, boolean enabled) throws HardwareException {
        throw unsupported("PWM");
    }

    /// Opens a CAN interface and returns its handle.
    protected int canOpen(String iface) throws HardwareException {
        throw unsupported("CAN");
    }

    /// Sends one CAN frame.
    protected void canSend(int handle, int id, boolean extended, byte[] data, int len) throws HardwareException {
        throw unsupported("CAN");
    }

    /// Waits up to `timeoutMs` for a CAN frame. Returns the payload length,
    /// or `-1` on timeout; the identifier goes into `idOut[0]` and `idOut[1]`
    /// is `1` for an extended identifier.
    protected int canReceive(int handle, int[] idOut, byte[] data, int timeoutMs) throws HardwareException {
        throw unsupported("CAN");
    }

    /// Opens the watchdog with a timeout and returns its handle.
    protected int watchdogOpen(int timeoutSeconds) throws HardwareException {
        throw unsupported("Watchdog");
    }

    /// Resets the watchdog timer.
    protected void watchdogKick(int handle) throws HardwareException {
        throw unsupported("Watchdog");
    }

    /// Closes any handle. `disarm` applies to a watchdog only: `true` stops it
    /// where the driver allows, `false` leaves it running so the device resets.
    protected void close(int handle, boolean disarm) {
    }
}
