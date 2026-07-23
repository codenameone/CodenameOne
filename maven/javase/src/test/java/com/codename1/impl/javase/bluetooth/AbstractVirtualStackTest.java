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
package com.codename1.impl.javase.bluetooth;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.bluetooth.gatt.GattCharacteristic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Shared fixture of the virtual-stack unit tests: one deterministic stack
 * on a {@link ManualScheduler} (no Codename One Display, no wall-clock
 * time) plus recording callbacks and the heart-rate demo peripheral.
 */
abstract class AbstractVirtualStackTest {

    static final BluetoothUuid HR_SERVICE = BluetoothUuid.fromShort(0x180D);
    static final BluetoothUuid HR_MEASUREMENT =
            BluetoothUuid.fromShort(0x2A37);
    static final BluetoothUuid HR_CONTROL = BluetoothUuid.fromShort(0x2A39);
    static final BluetoothUuid USER_DESCRIPTION =
            BluetoothUuid.fromShort(0x2901);

    protected ManualScheduler scheduler;
    protected SimulatedBluetoothStack stack;

    @BeforeEach
    void setUpStack() {
        scheduler = new ManualScheduler();
        stack = new SimulatedBluetoothStack(scheduler);
    }

    /** Advances the virtual clock far enough for everything to settle. */
    protected void settle() {
        scheduler.advance(10000);
    }

    /** Callback recorder; assertions read it after {@link #settle()}. */
    static final class Result<T> implements SimulatedBluetoothStack.Callback<T> {
        T value;
        BluetoothError error;
        String message;
        int successCount;
        int errorCount;

        public void onSuccess(T value) {
            this.value = value;
            successCount++;
        }

        public void onError(BluetoothError error, String message) {
            this.error = error;
            this.message = message;
            errorCount++;
        }

        boolean succeeded() {
            return successCount == 1 && errorCount == 0;
        }

        boolean failed() {
            return errorCount == 1 && successCount == 0;
        }

        void assertSuccess() {
            Assertions.assertTrue(succeeded(), "expected success but was "
                    + describe());
        }

        void assertFailure(BluetoothError expected) {
            Assertions.assertTrue(failed(), "expected failure but was "
                    + describe());
            Assertions.assertEquals(expected, error);
        }

        private String describe() {
            return "successCount=" + successCount + " errorCount="
                    + errorCount + " error=" + error + " message=" + message;
        }
    }

    /** A heart-rate style peripheral with one service, 2 chars, 1 desc. */
    protected VirtualPeripheral heartRatePeripheral(String address) {
        return new VirtualPeripheral(address)
                .setName("HR-" + address)
                .setRssi(-42)
                .addAdvertisedServiceUuid(HR_SERVICE)
                .addManufacturerData(0x004C, new byte[] {1, 2, 3})
                .withService(new VirtualService(HR_SERVICE)
                        .withCharacteristic(new VirtualCharacteristic(
                                HR_MEASUREMENT,
                                GattCharacteristic.PROPERTY_READ
                                        | GattCharacteristic.PROPERTY_NOTIFY,
                                new byte[] {0, 72})
                                .withDescriptor(new VirtualDescriptor(
                                        USER_DESCRIPTION,
                                        new byte[] {'h', 'r'})))
                        .withCharacteristic(new VirtualCharacteristic(
                                HR_CONTROL,
                                GattCharacteristic.PROPERTY_READ
                                        | GattCharacteristic.PROPERTY_WRITE,
                                new byte[] {0})));
    }

    /** Registers, connects and discovers the given peripheral. */
    protected void connectAndDiscover(String address) {
        Result<Boolean> connect = new Result<Boolean>();
        stack.connect(address, connect);
        Result<java.util.List<VirtualService>> discover =
                new Result<java.util.List<VirtualService>>();
        stack.discoverServices(address, discover);
        settle();
        connect.assertSuccess();
        discover.assertSuccess();
    }

    /**
     * An echo endpoint: pumps every received byte straight back on a
     * dedicated daemon thread until EOF.
     */
    static SimStreamHandler echoHandler() {
        return new SimStreamHandler() {
            public void onConnection(final SimStreamChannel channel) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            InputStream in = channel.getInputStream();
                            OutputStream out = channel.getOutputStream();
                            byte[] buf = new byte[256];
                            int n;
                            while ((n = in.read(buf)) != -1) {
                                out.write(buf, 0, n);
                                out.flush();
                            }
                        } catch (IOException ignored) {
                            // closed under us -- fine
                        } finally {
                            channel.close();
                        }
                    }
                }, "sim-echo");
                t.setDaemon(true);
                t.start();
            }
        };
    }

    /** Reads exactly {@code len} bytes from the stream. */
    static byte[] readFully(InputStream in, int len) throws IOException {
        byte[] out = new byte[len];
        new DataInputStream(in).readFully(out);
        return out;
    }
}
