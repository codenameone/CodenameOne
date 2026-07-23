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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;

/**
 * RFCOMM over the virtual stack's piped streams: the app as a client
 * against a registered endpoint, the app as a server accepting a virtual
 * client, and EOF propagation on close. Stream I/O uses real threads, so
 * every blocking section runs under a preemptive timeout hang-guard.
 */
public class VirtualRfcommStreamTest extends AbstractVirtualStackTest {

    private static final Duration GUARD = Duration.ofSeconds(30);

    @Test
    public void appAsClientEchoRoundTripIsByteExact() {
        stack.addRfcommEndpoint(BluetoothUuid.SPP, echoHandler());
        Result<SimStreamChannel> connect = new Result<>();
        stack.connectRfcomm(BluetoothUuid.SPP, connect);
        settle();
        connect.assertSuccess();
        final SimStreamChannel channel = connect.value;

        Assertions.assertTimeoutPreemptively(GUARD, () -> {
            byte[] payload = new byte[512];
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (i * 31);
            }
            OutputStream out = channel.getOutputStream();
            out.write(payload);
            out.flush();
            byte[] echoed = readFully(channel.getInputStream(),
                    payload.length);
            Assertions.assertArrayEquals(payload, echoed);
        });
        channel.close();
    }

    @Test
    public void connectWithoutEndpointFails() {
        Result<SimStreamChannel> connect = new Result<>();
        stack.connectRfcomm(BluetoothUuid.SPP, connect);
        settle();
        connect.assertFailure(BluetoothError.IO_ERROR);
    }

    @Test
    public void appAsServerAcceptsVirtualClient() {
        Result<Boolean> listen = new Result<>();
        stack.listenRfcomm(BluetoothUuid.SPP, listen);
        Result<SimStreamChannel> accept = new Result<>();
        stack.acceptRfcomm(BluetoothUuid.SPP, accept);
        settle();
        listen.assertSuccess();
        Assertions.assertEquals(0, accept.successCount,
                "accept should wait until a client arrives");

        SimStreamChannel client =
                stack.connectVirtualRfcommClient(BluetoothUuid.SPP);
        settle();
        accept.assertSuccess();
        final SimStreamChannel server = accept.value;

        Assertions.assertTimeoutPreemptively(GUARD, () -> {
            // client -> server
            client.getOutputStream().write(new byte[] {10, 20, 30});
            client.getOutputStream().flush();
            Assertions.assertArrayEquals(new byte[] {10, 20, 30},
                    readFully(server.getInputStream(), 3));
            // server -> client
            server.getOutputStream().write(new byte[] {77});
            server.getOutputStream().flush();
            Assertions.assertArrayEquals(new byte[] {77},
                    readFully(client.getInputStream(), 1));
        });
        client.close();
        server.close();
    }

    @Test
    public void virtualClientQueuedBeforeAcceptIsHandedOver() {
        Result<Boolean> listen = new Result<>();
        stack.listenRfcomm(BluetoothUuid.SPP, listen);
        settle();
        listen.assertSuccess();

        SimStreamChannel client =
                stack.connectVirtualRfcommClient(BluetoothUuid.SPP);
        settle();

        Result<SimStreamChannel> accept = new Result<>();
        stack.acceptRfcomm(BluetoothUuid.SPP, accept);
        settle();
        accept.assertSuccess();

        final SimStreamChannel server = accept.value;
        Assertions.assertTimeoutPreemptively(GUARD, () -> {
            client.getOutputStream().write(5);
            client.getOutputStream().flush();
            Assertions.assertEquals(5, server.getInputStream().read());
        });
        client.close();
        server.close();
    }

    @Test
    public void closePropagatesEofToThePeer() {
        Result<Boolean> listen = new Result<>();
        stack.listenRfcomm(BluetoothUuid.SPP, listen);
        Result<SimStreamChannel> accept = new Result<>();
        stack.acceptRfcomm(BluetoothUuid.SPP, accept);
        settle();
        SimStreamChannel client =
                stack.connectVirtualRfcommClient(BluetoothUuid.SPP);
        settle();
        accept.assertSuccess();
        SimStreamChannel server = accept.value;

        client.close();
        Assertions.assertFalse(client.isOpen());
        Assertions.assertTimeoutPreemptively(GUARD, () -> {
            InputStream in = server.getInputStream();
            Assertions.assertEquals(-1, in.read(),
                    "server must observe EOF after the client closed");
        });
        server.close();
    }

    @Test
    public void closingTheServerFailsPendingAccepts() {
        Result<Boolean> listen = new Result<>();
        stack.listenRfcomm(BluetoothUuid.SPP, listen);
        Result<SimStreamChannel> accept = new Result<>();
        stack.acceptRfcomm(BluetoothUuid.SPP, accept);
        settle();

        stack.closeRfcommServer(BluetoothUuid.SPP);
        settle();
        accept.assertFailure(BluetoothError.IO_ERROR);
    }
}
