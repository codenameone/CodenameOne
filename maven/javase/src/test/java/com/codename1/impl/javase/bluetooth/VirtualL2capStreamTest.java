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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.time.Duration;

/**
 * L2CAP over the virtual stack's piped streams: the app as a central
 * against a peripheral's PSM endpoint, and the app as a peripheral
 * publishing a PSM that a virtual client connects to.
 */
public class VirtualL2capStreamTest extends AbstractVirtualStackTest {

    private static final Duration GUARD = Duration.ofSeconds(30);
    private static final String ADDR = "AA:BB:CC:DD:EE:22";
    private static final int PSM = 0x25;

    @Test
    public void appAsCentralEchoRoundTripIsByteExact() {
        stack.addPeripheral(new VirtualPeripheral(ADDR)
                .withL2capEndpoint(PSM, echoHandler()));
        Result<SimStreamChannel> open = new Result<>();
        stack.openL2capChannel(ADDR, PSM, open);
        settle();
        open.assertSuccess();
        final SimStreamChannel channel = open.value;

        Assertions.assertTimeoutPreemptively(GUARD, () -> {
            byte[] payload = new byte[300];
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (255 - (i % 251));
            }
            OutputStream out = channel.getOutputStream();
            out.write(payload);
            out.flush();
            Assertions.assertArrayEquals(payload,
                    readFully(channel.getInputStream(), payload.length));
        });
        channel.close();
    }

    @Test
    public void openWithoutEndpointFails() {
        stack.addPeripheral(new VirtualPeripheral(ADDR));
        Result<SimStreamChannel> open = new Result<>();
        stack.openL2capChannel(ADDR, PSM, open);
        settle();
        open.assertFailure(BluetoothError.IO_ERROR);
    }

    @Test
    public void appAsServerAcceptsVirtualClientOnPublishedPsm() {
        Result<Integer> publish = new Result<>();
        stack.publishAppL2capServer(publish);
        settle();
        publish.assertSuccess();
        int psm = publish.value.intValue();
        Assertions.assertTrue(psm > 0);

        Result<SimStreamChannel> accept = new Result<>();
        stack.acceptAppL2cap(psm, accept);
        settle();
        Assertions.assertEquals(0, accept.successCount,
                "accept should wait until a client arrives");

        SimStreamChannel client = stack.connectVirtualL2capClient(psm);
        settle();
        accept.assertSuccess();
        final SimStreamChannel server = accept.value;

        Assertions.assertTimeoutPreemptively(GUARD, () -> {
            client.getOutputStream().write(new byte[] {4, 5, 6});
            client.getOutputStream().flush();
            Assertions.assertArrayEquals(new byte[] {4, 5, 6},
                    readFully(server.getInputStream(), 3));
            server.getOutputStream().write(9);
            server.getOutputStream().flush();
            Assertions.assertEquals(9, client.getInputStream().read());
        });
        client.close();
        server.close();
    }

    @Test
    public void serverCloseFailsPendingAcceptsAndEofPropagates() {
        Result<Integer> publish = new Result<>();
        stack.publishAppL2capServer(publish);
        settle();
        int psm = publish.value.intValue();

        Result<SimStreamChannel> accept = new Result<>();
        stack.acceptAppL2cap(psm, accept);
        SimStreamChannel client = stack.connectVirtualL2capClient(psm);
        settle();
        accept.assertSuccess();
        final SimStreamChannel server = accept.value;

        // server side closes; the virtual client observes EOF
        server.close();
        Assertions.assertTimeoutPreemptively(GUARD, () ->
                Assertions.assertEquals(-1,
                        client.getInputStream().read()));

        Result<SimStreamChannel> pending = new Result<>();
        stack.acceptAppL2cap(psm, pending);
        settle();
        stack.closeAppL2capServer(psm);
        settle();
        pending.assertFailure(BluetoothError.IO_ERROR);
        client.close();
    }

    @Test
    public void virtualClientWithoutListenerObservesImmediateEof() {
        SimStreamChannel client = stack.connectVirtualL2capClient(0x99);
        settle();
        Assertions.assertTimeoutPreemptively(GUARD, () ->
                Assertions.assertEquals(-1,
                        client.getInputStream().read()));
    }
}
