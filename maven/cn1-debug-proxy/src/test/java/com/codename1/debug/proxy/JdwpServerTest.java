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
package com.codename1.debug.proxy;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class JdwpServerTest {

    private static final byte[] HANDSHAKE = "JDWP-Handshake".getBytes(StandardCharsets.US_ASCII);

    @Test
    public void debuggerCanAttachAndQueryBeforeDeviceStreamsSymbols() throws Exception {
        int port = freePort();
        JdwpServer server = new JdwpServer(port);
        Thread serverThread = new Thread(() -> {
            try {
                server.acceptAndServe();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "jdwp-server-test");
        serverThread.setDaemon(true);
        serverThread.start();

        try (Socket client = connect(port)) {
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            out.write(HANDSHAKE);
            out.flush();
            byte[] handshakeReply = new byte[HANDSHAKE.length];
            in.readFully(handshakeReply);
            assertEquals("JDWP-Handshake", new String(handshakeReply, StandardCharsets.US_ASCII));

            int requestId = 41;
            byte[] signature = "Lcom/example/Test$1;".getBytes(StandardCharsets.UTF_8);
            out.writeInt(11 + 4 + signature.length);
            out.writeInt(requestId);
            out.writeByte(0);
            out.writeByte(1); // VirtualMachine command set
            out.writeByte(2); // ClassesBySignature
            out.writeInt(signature.length);
            out.write(signature);
            out.flush();

            client.setSoTimeout(250);
            try {
                in.readInt();
                fail("Class query should wait until the device streams symbols");
            } catch (SocketTimeoutException expected) {
                // Expected: the request remains queued instead of throwing.
            }

            String table = "version\t1\n"
                    + "class\t0\tcom_example_Test_1\tTest.java\t-1\tcom/example/Test$1\n";
            server.onSymbols(SymbolTable.load(new ByteArrayInputStream(
                    table.getBytes(StandardCharsets.UTF_8))));
            server.onHello(1);

            client.setSoTimeout(2000);
            Reply reply = readReply(in, requestId);
            assertEquals(0, reply.errorCode);
            DataInputStream body = new DataInputStream(new ByteArrayInputStream(reply.body));
            assertEquals(1, body.readInt());
            assertEquals(1, body.readUnsignedByte());
            assertEquals(1L, body.readLong());
            assertEquals(7, body.readInt());
        }
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static Socket connect(int port) throws Exception {
        Exception last = null;
        for (int i = 0; i < 50; i++) {
            try {
                return new Socket("127.0.0.1", port);
            } catch (Exception e) {
                last = e;
                Thread.sleep(20);
            }
        }
        throw last;
    }

    private static Reply readReply(DataInputStream in, int requestId) throws Exception {
        while (true) {
            int length = in.readInt();
            int id = in.readInt();
            int flags = in.readUnsignedByte();
            if ((flags & 0x80) != 0) {
                int errorCode = in.readUnsignedShort();
                byte[] body = new byte[length - 11];
                in.readFully(body);
                if (id == requestId) {
                    return new Reply(errorCode, body);
                }
            } else {
                in.readUnsignedByte();
                in.readUnsignedByte();
                byte[] body = new byte[length - 11];
                in.readFully(body);
            }
        }
    }

    private static final class Reply {
        final int errorCode;
        final byte[] body;

        Reply(int errorCode, byte[] body) {
            this.errorCode = errorCode;
            this.body = body;
        }
    }
}
