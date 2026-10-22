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
package com.codenameone.examples.skirmish;

import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.Tcp;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.LocalServerPort;
import com.codenameone.examples.skirmish.account.Accounts;
import com.codenameone.examples.skirmish.game.Challenges;
import com.codenameone.examples.skirmish.game.GameTickets;
import java.io.IOException;
import com.codenameone.examples.skirmish.rules.Cards;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.rules.Match;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The game over a real socket: a ticket opens it, a challenge seats two
/// players, plays come back stamped for both, and leaving ends the match.
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)
class GameSocketTest {
    private static final int TEXT = 0x1;
    private static final int CLOSE = 0x8;

    @LocalServerPort
    private int port;
    @Autowired
    private GameTickets tickets;
    @Autowired
    private Challenges challenges;
    @Autowired
    private Accounts accounts;

    private int opcode;
    private byte[] payload;

    @Test
    void twoPlayersMeetThroughAChallengeAndSeeTheSamePlays() throws Exception {
        String host = Api.player(accounts, "sockethost");
        String guest = Api.player(accounts, "socketguest");
        String code = challenges.create(host).code();
        Tcp a = open("/ws/game?ticket=" + tickets.issue(host).ticket());
        Tcp b = open("/ws/game?ticket=" + tickets.issue(guest).ticket());
        try {
            assertEquals("ready", next(a).get("type"));
            assertEquals("ready", next(b).get("type"));
            writeText(a, "{\"type\":\"challenge\",\"code\":\"" + code + "\"}");
            assertEquals("queued", next(a).get("type"));
            writeText(b, "{\"type\":\"challenge\",\"code\":\"" + code + "\"}");
            Map startA = until(a, "start");
            Map startB = until(b, "start");
            assertEquals(startA.get("match"), startB.get("match"));
            assertEquals(startA.get("seed"), startB.get("seed"));
            assertEquals(0L, ((Number) startA.get("you")).longValue());
            assertEquals(1L, ((Number) startB.get("you")).longValue());

            // The host's play reaches the guest, stamped for a tick to come.
            // The client builds the same match from the start message, so it knows
            // its hand without asking.
            Match local = new Match(Long.parseLong((String) startA.get("seed")),
                    split((String) startA.get("deck0")), split((String) startA.get("deck1")));
            String card = local.player(0).hand(0);
            for (int i = 1; i < Deck.HAND; i++) {
                if (Cards.get(local.player(0).hand(i)).cost < Cards.get(card).cost) {
                    card = local.player(0).hand(i);
                }
            }
            writeText(a, "{\"type\":\"play\",\"card\":\"" + card + "\",\"x\":3500,\"y\":24000}");
            String stamped = firstCommand(b);
            assertTrue(stamped.indexOf(card) > 0, stamped);
            assertEquals(stamped, firstCommand(a));

            writeText(b, "{\"type\":\"leave\"}");
            Map end = until(a, "end");
            assertEquals(0L, ((Number) end.get("result")).longValue());
        } finally {
            a.close();
            b.close();
        }
    }

    @Test
    void noTicketNoGame() throws Exception {
        String[] paths = {"/ws/game", "/ws/game?ticket=", "/ws/game?ticket=0123456789abcdef"};
        for (String path : paths) {
            Tcp stranger = open(path);
            try {
                readFrame(stranger);
                assertEquals(CLOSE, opcode, path);
                assertEquals(4401, closeCode());
            } finally {
                stranger.close();
            }
        }
    }

    private static String[] split(String csv) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        int from = 0;
        for (int i = 0; i <= csv.length(); i++) {
            if (i == csv.length() || csv.charAt(i) == ',') {
                out.add(csv.substring(from, i));
                from = i + 1;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private Map next(Tcp connection) throws IOException {
        readFrame(connection);
        return Json.parseObject(text());
    }

    /// Reads messages until one of `type`, skipping the tick stream.
    private Map until(Tcp connection, String type) throws IOException {
        for (int i = 0; i < 2000; i++) {
            Map m = next(connection);
            if (type.equals(m.get("type"))) {
                return m;
            }
        }
        throw new IOException("no " + type + " message");
    }

    /// The first play a `ticks` message carries.
    private String firstCommand(Tcp connection) throws IOException {
        for (int i = 0; i < 2000; i++) {
            Map m = until(connection, "ticks");
            String c = (String) m.get("c");
            if (c != null && c.length() > 0) {
                return c;
            }
        }
        throw new IOException("no play arrived");
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
