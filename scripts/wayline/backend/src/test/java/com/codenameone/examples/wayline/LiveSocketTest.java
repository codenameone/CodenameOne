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
package com.codenameone.examples.wayline;

import com.codename1.backend.Crypto;
import com.codename1.backend.Tcp;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.LocalServerPort;
import com.codenameone.examples.wayline.account.DemoAccounts;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.live.LiveTickets;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The live channel over a real socket: a ticket opens it once, and what the
/// server pushes arrives.
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)
class LiveSocketTest {
    private static final int TEXT = 0x1;
    private static final int CLOSE = 0x8;

    @LocalServerPort
    private int port;
    @Autowired
    private LiveTickets tickets;
    @Autowired
    private LiveHub hub;

    /// The last frame read: its opcode, and its payload as text.
    private int opcode;
    private byte[] payload;

    @Test
    void aTicketOpensTheChannelOnceAndPushesArrive() throws Exception {
        String ticket = tickets.issue(DemoAccounts.RIDER).ticket;
        Tcp rider = open("/ws/live?ticket=" + ticket);
        try {
            readFrame(rider);
            assertEquals("{\"type\":\"ready\"}", text());

            writeText(rider, "{\"type\":\"ping\"}");
            readFrame(rider);
            assertEquals("{\"type\":\"pong\"}", text());

            // Something happens to a ride of theirs.
            hub.rideChanged("ride-1", "ACCEPTED", DemoAccounts.RIDER, DemoAccounts.DRIVER, "");
            readFrame(rider);
            assertEquals("{\"type\":\"ride\",\"id\":\"ride-1\",\"state\":\"ACCEPTED\"}", text());
            hub.driverMoved(DemoAccounts.RIDER, "ride-1", 37.8, -122.4, 90);
            readFrame(rider);
            assertTrue(text().startsWith("{\"type\":\"driver\",\"rideId\":\"ride-1\",\"lat\":37.8"),
                    text());
            // And to somebody else's, which is none of their business.
            hub.rideChanged("ride-2", "ACCEPTED", "other@test.example", DemoAccounts.DRIVER, "");
            hub.rideChanged("ride-3", "COMPLETED", DemoAccounts.RIDER, "", "");
            readFrame(rider);
            assertTrue(text().indexOf("ride-3") > 0, text());

            // The same ticket again opens nothing.
            Tcp again = open("/ws/live?ticket=" + ticket);
            try {
                readFrame(again);
                assertEquals(CLOSE, opcode);
                assertEquals(4401, closeCode());
            } finally {
                again.close();
            }

            // Suspending the account closes what it has open.
            hub.drop(DemoAccounts.RIDER);
            readFrame(rider);
            assertEquals(CLOSE, opcode);
            assertEquals(4403, closeCode());
        } finally {
            rider.close();
        }
    }

    @Test
    void noTicketNoChannel() throws Exception {
        String[] paths = {"/ws/live", "/ws/live?ticket=", "/ws/live?ticket=0123456789abcdef"};
        for (int iter = 0; iter < paths.length; iter++) {
            Tcp stranger = open(paths[iter]);
            try {
                readFrame(stranger);
                assertEquals(CLOSE, opcode, paths[iter]);
                assertEquals(4401, closeCode());
            } finally {
                stranger.close();
            }
        }
    }

    @Test
    void anAdminHearsAboutEveryRideAndTheFleet() throws Exception {
        Tcp admin = open("/ws/live?ticket=" + tickets.issue(DemoAccounts.ADMIN).ticket);
        try {
            readFrame(admin);
            assertEquals("{\"type\":\"ready\"}", text());
            hub.rideChanged("ride-9", "REQUESTED", "someone@test.example", "", "");
            readFrame(admin);
            assertTrue(text().indexOf("ride-9") > 0, text());
            hub.fleetChanged(DemoAccounts.DRIVER, true, 37.8, -122.4, 0);
            readFrame(admin);
            assertTrue(text().startsWith("{\"type\":\"fleet\",\"username\":\"" + DemoAccounts.DRIVER
                    + "\",\"online\":true"), text());
        } finally {
            admin.close();
        }
    }

    private Tcp open(String target) throws IOException {
        Tcp connection = Tcp.connect("127.0.0.1", port, 5000);
        byte[] request = ascii("GET " + target + " HTTP/1.1\r\nHost: 127.0.0.1\r\n"
                + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                + "Sec-WebSocket-Version: 13\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n");
        connection.write(request, 0, request.length);
        StringBuilder head = new StringBuilder();
        // The head ends at the first empty line: CR LF CR LF.
        int ending = 0;
        while (head.length() < 8192 && ending < 4) {
            int c = readByte(connection);
            if (c < 0) {
                break;
            }
            head.append((char) c);
            ending = c == (ending % 2 == 0 ? '\r' : '\n') ? ending + 1 : (c == '\r' ? 1 : 0);
        }
        assertTrue(head.toString().startsWith("HTTP/1.1 101"), head.toString());
        return connection;
    }

    private String text() throws IOException {
        assertEquals(TEXT, opcode);
        return new String(payload, "UTF-8");
    }

    private int closeCode() {
        return payload.length < 2 ? -1 : ((payload[0] & 0xff) << 8) | (payload[1] & 0xff);
    }

    private void readFrame(Tcp connection) throws IOException {
        int first = readByte(connection);
        if (first < 0) {
            throw new IOException("the connection ended with no frame");
        }
        opcode = first & 0xf;
        int length = readByte(connection) & 0x7f;
        if (length == 126) {
            length = (readByte(connection) << 8) | readByte(connection);
        }
        payload = new byte[length];
        int got = 0;
        while (got < length) {
            int read = connection.read(payload, got, length - got);
            if (read <= 0) {
                throw new IOException("the connection ended after " + got + " of " + length);
            }
            got += read;
        }
    }

    /// A client's frames are masked; a server refuses one that is not.
    private static void writeText(Tcp connection, String text) throws IOException {
        byte[] body = ascii(text);
        byte[] mask = Crypto.randomBytes(4);
        byte[] frame = new byte[6 + body.length];
        frame[0] = (byte) (0x80 | TEXT);
        frame[1] = (byte) (0x80 | body.length);
        System.arraycopy(mask, 0, frame, 2, 4);
        for (int iter = 0; iter < body.length; iter++) {
            frame[6 + iter] = (byte) (body[iter] ^ mask[iter & 3]);
        }
        connection.write(frame, 0, frame.length);
    }

    private static int readByte(Tcp connection) throws IOException {
        byte[] one = new byte[1];
        int read = connection.read(one, 0, 1);
        return read <= 0 ? -1 : one[0] & 0xff;
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = (byte) value.charAt(iter);
        }
        return out;
    }
}
