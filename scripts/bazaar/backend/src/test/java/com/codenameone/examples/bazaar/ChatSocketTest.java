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
package com.codenameone.examples.bazaar;

import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.Tcp;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.LocalServerPort;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.live.LiveTickets;
import com.codenameone.examples.bazaar.ops.ChatLive;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The floating chat over a real socket: a visitor's message reaches the
/// store's staff, the reply reaches the visitor under the store's name, and
/// neither a ticket from another store nor a page from another site gets in.
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)
class ChatSocketTest {
    private static final int TEXT = 0x1;
    private static final int CLOSE = 0x8;

    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private LiveTickets tickets;
    @Autowired
    private ChatLive live;

    private int opcode;
    private byte[] payload;

    private Shop shop(String name) throws Exception {
        return new Shop(name, 10, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    @Test
    void aVisitorAndTheStoreTalkLive() throws Exception {
        Shop s = shop("chat-live");
        Tcp staff = open("/ws/chat?staff=" + tickets.issue(s.owner).ticket() + "&store=" + s.storeId,
                null);
        Tcp visitor = null;
        try {
            assertTrue(until(staff, "hello").contains("\"online\":1"), text());
            visitor = open("/ws/chat?ticket=" + live.issue(s.storeId, "Chat Live", "", null,
                    "test-client"), null);
            String hello = until(visitor, "hello");
            assertTrue(hello.contains("\"online\":1") && hello.contains("\"messages\":[]"), hello);

            writeText(visitor, "{\"op\":\"send\",\"body\":\"Is this in stock?\",\"ref\":\"r1\"}");
            assertTrue(until(visitor, "token").contains("\"token\":\""), text());
            assertTrue(until(visitor, "sent").contains("\"ref\":\"r1\""), text());
            String heard = until(staff, "message");
            assertTrue(heard.contains("Is this in stock?"), heard);
            String chat = field(heard, "chat");

            writeText(staff, "{\"op\":\"send\",\"chat\":\"" + chat + "\",\"body\":\"Yes, ten left\"}");
            String reply = until(visitor, "message");
            assertTrue(reply.contains("Yes, ten left") && reply.contains("\"staff\":true"), reply);
            assertTrue(!reply.contains(s.owner), "the visitor never sees a staff account: " + reply);

            writeText(visitor, "{\"op\":\"typing\"}");
            assertTrue(until(staff, "typing").contains(chat), text());

            // Another store's owner reaches neither the store nor its thread.
            Shop other = shop("chat-live-o");
            Tcp intruder = open("/ws/chat?staff=" + tickets.issue(other.owner).ticket() + "&store="
                    + s.storeId, null);
            readFrame(intruder);
            assertEquals(CLOSE, opcode);
            assertEquals(4403, closeCode());
            intruder.close();
            Tcp own = open("/ws/chat?staff=" + tickets.issue(other.owner).ticket() + "&store="
                    + other.storeId, null);
            until(own, "hello");
            writeText(own, "{\"op\":\"watch\",\"chat\":\"" + chat + "\"}");
            assertTrue(until(own, "error").contains("\"status\":404"), text());
            own.close();
        } finally {
            staff.close();
            if (visitor != null) {
                visitor.close();
            }
        }
    }

    /// The call path the merchant app answers on: a handshake with no
    /// `Origin` (a native client), the ring and the offer fanned out to every
    /// staff connection, the first ANSWER winning, and the candidate travelling
    /// as the object WebRTC produces.
    @Test
    void theAppAnswersAShoppersCall() throws Exception {
        Shop s = shop("chat-call");
        Tcp app = open("/ws/chat?staff=" + tickets.issue(s.owner).ticket() + "&store=" + s.storeId,
                null);
        Tcp web = open("/ws/chat?staff=" + tickets.issue(s.owner).ticket() + "&store=" + s.storeId,
                null);
        Tcp visitor = null;
        try {
            assertTrue(until(app, "hello").contains("\"ice\":["), text());
            until(web, "hello");
            visitor = open("/ws/chat?ticket=" + live.issue(s.storeId, "Chat Call", "", null,
                    "call-client"), null);
            until(visitor, "hello");
            writeText(visitor, "{\"op\":\"send\",\"body\":\"Can I call?\",\"ref\":\"r1\"}");
            String chat = field(until(app, "message"), "chat");
            until(web, "message");

            writeText(visitor, "{\"op\":\"call\",\"kind\":\"RING\",\"video\":true}");
            String ring = until(app, "call");
            assertTrue(ring.contains("\"RING\"") && ring.contains("\"video\":true")
                    && ring.contains(chat), ring);
            until(web, "call");
            writeText(visitor, "{\"op\":\"call\",\"kind\":\"OFFER\",\"sdp\":\"v=0\\r\\n\",\"video\":true}");
            assertTrue(until(app, "call").contains("\"OFFER\""), text());
            until(web, "call");

            writeText(app, "{\"op\":\"call\",\"chat\":\"" + chat
                    + "\",\"kind\":\"ANSWER\",\"sdp\":\"v=0\\r\\n\"}");
            assertTrue(until(visitor, "call").contains("\"ANSWER\""), text());
            assertTrue(until(web, "call").contains("\"TAKEN\""), "the other staff hear it was taken");
            writeText(app, "{\"op\":\"call\",\"chat\":\"" + chat + "\",\"kind\":\"ICE\","
                    + "\"candidate\":{\"candidate\":\"candidate:1 1 udp 1 10.0.0.1 9 typ host\","
                    + "\"sdpMid\":\"0\",\"sdpMLineIndex\":0}}");
            String ice = until(visitor, "call");
            assertTrue(ice.contains("\"ICE\"") && ice.contains("\"sdpMid\":\"0\""), ice);

            writeText(app, "{\"op\":\"call\",\"chat\":\"" + chat + "\",\"kind\":\"END\"}");
            assertTrue(until(visitor, "call").contains("\"END\""), text());
        } finally {
            app.close();
            web.close();
            if (visitor != null) {
                visitor.close();
            }
        }
    }

    @Test
    void aPageFromAnotherSiteIsRefused() throws Exception {
        Shop s = shop("chat-origin");
        Tcp evil = open("/ws/chat?ticket=" + live.issue(s.storeId, "Chat Origin", "", null, "x"),
                "https://evil.example");
        readFrame(evil);
        assertEquals(CLOSE, opcode);
        assertEquals(4403, closeCode());
        evil.close();
        Tcp none = open("/ws/chat?ticket=nope", null);
        readFrame(none);
        assertEquals(4401, closeCode());
        none.close();
    }

    /// Reads frames until one of `type` arrives, and answers it.
    private String until(Tcp connection, String type) throws IOException {
        for (int iter = 0; iter < 20; iter++) {
            readFrame(connection);
            if (opcode == TEXT && text().contains("\"type\":\"" + type + "\"")) {
                return text();
            }
        }
        throw new IOException("no " + type + " frame; the last was " + text());
    }

    private static String field(String json, String name) {
        int at = json.indexOf("\"" + name + "\":\"") + name.length() + 4;
        return json.substring(at, json.indexOf('"', at));
    }

    private Tcp open(String target, String origin) throws IOException {
        Tcp connection = Tcp.connect("127.0.0.1", port, 5000);
        byte[] request = ascii("GET " + target + " HTTP/1.1\r\nHost: 127.0.0.1:" + port + "\r\n"
                + (origin == null ? "" : "Origin: " + origin + "\r\n")
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

    private String text() throws IOException {
        return payload == null ? "" : new String(payload, "UTF-8");
    }

    private int closeCode() {
        return payload.length < 2 ? -1 : ((payload[0] & 0xff) << 8) | (payload[1] & 0xff);
    }

    private void readFrame(Tcp connection) throws IOException {
        int first = readByte(connection);
        if (first < 0) {
            throw new IOException("the connection ended with no frame; the last: " + text());
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
        // A call signal passes 125 bytes, so the 16-bit length form is needed.
        int head = body.length < 126 ? 2 : 4;
        byte[] frame = new byte[head + 4 + body.length];
        frame[0] = (byte) (0x80 | TEXT);
        if (head == 2) {
            frame[1] = (byte) (0x80 | body.length);
        } else {
            frame[1] = (byte) (0x80 | 126);
            frame[2] = (byte) (body.length >> 8);
            frame[3] = (byte) body.length;
        }
        System.arraycopy(mask, 0, frame, head, 4);
        for (int iter = 0; iter < body.length; iter++) {
            frame[head + 4 + iter] = (byte) (body[iter] ^ mask[iter & 3]);
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
