/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.play;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.Tcp;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.LocalServerPort;
import com.codenameone.examples.brickyard.Api;
import com.codenameone.examples.brickyard.account.Accounts;
import com.codenameone.examples.brickyard.api.PlaceDto;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The game socket over a real connection: a ticket seats the player and
/// replication starts flowing; a spent ticket and a text frame are refused.
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)
class PlaySocketTest {
    private static final int TEXT = 0x1;
    private static final int BINARY = 0x2;
    private static final int CLOSE = 0x8;

    @LocalServerPort
    private int port;
    @Autowired
    private PlayServers servers;
    @Autowired
    private PlayTickets tickets;
    @Autowired
    private Accounts accounts;

    private int opcode;
    private byte[] payload;

    @Test
    void aTicketSeatsThePlayerOnceAndReplicationFlows() throws Exception {
        String who = Api.player(accounts, "wsplayer");
        PlayServers.Placement at = servers.seat(who, "exp-ws", 4, "", new PlayServers.PlaceSource() {
            @Override
            public PlaceDto place() throws IOException {
                return new PlaceDto(1, Base64.encode(Api.sample()));
            }
        });
        String ticket = tickets.issue(who, at.serverId);

        Tcp c = open("/ws/play?ticket=" + ticket);
        readFrame(c);
        assertEquals(BINARY, opcode, "the room replicates to the new seat");
        assertTrue(payload.length > 0);
        // Text is not a replication message.
        writeFrame(c, TEXT, new byte[] {'h', 'i'});
        while (opcode != CLOSE) {
            readFrame(c);
        }
        assertEquals(1003, closeCode());
        c.close();

        // The ticket was spent by the first connection.
        Tcp again = open("/ws/play?ticket=" + ticket);
        readFrame(again);
        assertEquals(CLOSE, opcode);
        assertEquals(4401, closeCode());
        again.close();

        assertEquals("abc", PlaySocket.ticket("x=1&ticket=abc&y=2"));
    }

    private Tcp open(String target) throws IOException {
        Tcp connection = Tcp.connect("127.0.0.1", port, 5000);
        byte[] request = ascii("GET " + target + " HTTP/1.1\r\nHost: 127.0.0.1\r\n"
                + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                + "Sec-WebSocket-Version: 13\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n");
        connection.write(request, 0, request.length);
        StringBuilder head = new StringBuilder();
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

    private int closeCode() {
        return payload.length < 2 ? -1 : ((payload[0] & 0xff) << 8) | (payload[1] & 0xff);
    }

    private void readFrame(Tcp connection) throws IOException {
        int first = readByte(connection);
        if (first < 0) {
            throw new IOException("the connection ended with no frame");
        }
        opcode = first & 0xf;
        long length = readByte(connection) & 0x7f;
        if (length == 126) {
            length = (readByte(connection) << 8) | readByte(connection);
        } else if (length == 127) {
            length = 0;
            for (int i = 0; i < 8; i++) {
                length = (length << 8) | readByte(connection);
            }
        }
        payload = new byte[(int) length];
        int got = 0;
        while (got < length) {
            int read = connection.read(payload, got, (int) length - got);
            if (read <= 0) {
                throw new IOException("the connection ended after " + got + " of " + length);
            }
            got += read;
        }
    }

    /// A client's frames are masked; a server refuses one that is not.
    private static void writeFrame(Tcp connection, int op, byte[] body) throws IOException {
        byte[] mask = Crypto.randomBytes(4);
        byte[] frame = new byte[6 + body.length];
        frame[0] = (byte) (0x80 | op);
        frame[1] = (byte) (0x80 | body.length);
        System.arraycopy(mask, 0, frame, 2, 4);
        for (int i = 0; i < body.length; i++) {
            frame[6 + i] = (byte) (body[i] ^ mask[i & 3]);
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
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) value.charAt(i);
        }
        return out;
    }
}
