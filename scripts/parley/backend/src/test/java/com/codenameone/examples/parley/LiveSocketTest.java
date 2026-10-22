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
package com.codenameone.examples.parley;

import com.codename1.backend.Crypto;
import com.codename1.backend.Tcp;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.LocalServerPort;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.parley.account.Accounts;
import com.codenameone.examples.parley.chat.Conversations;
import com.codenameone.examples.parley.live.LiveTickets;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The live channel over a real socket: a ticket opens it once, for one of
/// the account's own devices, and messages for that device arrive on it.
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)
class LiveSocketTest {
    private static final int TEXT = 0x1;
    private static final int CLOSE = 0x8;

    @LocalServerPort
    private int port;
    @Autowired
    private LiveTickets tickets;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    /// The last frame read: its opcode, and its payload as text.
    private int opcode;
    private byte[] payload;

    @Test
    void aMessageArrivesOnTheRecipientsChannel() throws Exception {
        String alice = People.user(accounts, "live-a");
        String bob = People.user(accounts, "live-b");
        String alices = Chat.device(mvc, alice);
        String bobs = Chat.device(mvc, bob);
        String chat = Chat.conversation(mvc, alice, Conversations.DIRECT, "", bob);
        Tcp channel = open("/ws/live?ticket=" + tickets.issue(bob).ticket() + "&device=" + bobs);
        try {
            readFrame(channel);
            assertEquals("{\"type\":\"ready\"}", text());
            writeText(channel, "{\"type\":\"ping\"}");
            readFrame(channel);
            assertEquals("{\"type\":\"pong\"}", text());

            Chat.send(mvc, alice, alices, chat, "live-1", Chat.copies(Chat.copy(bob, bobs)));
            readFrame(channel);
            assertTrue(text().startsWith("{\"type\":\"envelope\""), text());
            assertTrue(text().indexOf(Chat.sealed(bobs)) > 0, text());
        } finally {
            channel.close();
        }
    }

    @Test
    void aTicketIsGoodOnceAndOnlyForTheAccountsOwnDevice() throws Exception {
        String alice = People.user(accounts, "live-own-a");
        String bob = People.user(accounts, "live-own-b");
        String bobs = Chat.device(mvc, bob);
        String alices = Chat.device(mvc, alice);
        String ticket = tickets.issue(alice).ticket();
        Tcp other = open("/ws/live?ticket=" + ticket + "&device=" + bobs);
        try {
            readFrame(other);
            assertEquals(CLOSE, opcode);
            assertEquals(4401, closeCode());
        } finally {
            other.close();
        }
        // Spent, even though it opened nothing.
        Tcp again = open("/ws/live?ticket=" + ticket + "&device=" + alices);
        try {
            readFrame(again);
            assertEquals(CLOSE, opcode);
        } finally {
            again.close();
        }
    }

    @Test
    void unlinkingADeviceClosesItsChannel() throws Exception {
        String alice = People.user(accounts, "live-unlink");
        String alices = Chat.device(mvc, alice);
        Tcp channel = open("/ws/live?ticket=" + tickets.issue(alice).ticket()
                + "&device=" + alices);
        try {
            readFrame(channel);
            assertEquals("{\"type\":\"ready\"}", text());
            Chat.json(mvc, People.asUser(delete("/api/devices/" + alices), alice));
            readFrame(channel);
            assertEquals(CLOSE, opcode);
        } finally {
            channel.close();
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
