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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.Crypto;
import com.codename1.backend.Tcp;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.LocalServerPort;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Proves the screenshot endpoint works end to end before a CI leg spends forty
/// minutes finding out otherwise: a websocket to the served endpoint, a META frame
/// and a binary message SPLIT ACROSS TWO FRAMES (the common path, since the
/// hand-rolled clients fragment at 64KB), the ACK, and the file on disk.
///
/// Through the backend's own Tcp and Crypto, so a compiled run checks the
/// translated SHA-1 binding and websocket reassembly of the binary CI starts.
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT,
        properties = "cn1ss.out=target/cn1ss-test-out")
class Cn1ssEndpointTest {
    private static final String TEST_NAME = "__cn1ss_selfcheck__";
    /// RFC 6455 1.3's worked example, and the accept value it prints.
    private static final String RFC_KEY = "dGhlIHNhbXBsZSBub25jZQ==";
    private static final String RFC_ACCEPT = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";

    @LocalServerPort
    private int port;

    @Autowired
    private ScreenshotStore store;

    @Test
    void aFragmentedScreenshotIsStoredAndAcknowledged() throws Exception {
        for (String path : new String[] {"/cn1ss", "/"}) {
            Tcp connection = Tcp.connect("127.0.0.1", port, 5000);
            try {
                String request = "GET " + path + " HTTP/1.1\r\nHost: 127.0.0.1\r\n"
                        + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                        + "Sec-WebSocket-Version: 13\r\nSec-WebSocket-Key: " + RFC_KEY + "\r\n\r\n";
                byte[] requestBytes = ascii(request);
                connection.write(requestBytes, 0, requestBytes.length);
                String head = readUntilBlankLine(connection);
                assertTrue(head.startsWith("HTTP/1.1 101"), head);
                // The accept value, not just the status: a 101 with a wrong value is
                // a handshake every conforming client rejects.
                assertTrue(head.indexOf("Sec-WebSocket-Accept: " + RFC_ACCEPT) >= 0, head);

                byte[] png = {(byte) 0x89, 'P', 'N', 'G'};
                String hash = ScreenshotStore.fnv1a64Hex(png, 0, png.length);
                writeFrame(connection, true, 0x1, ascii("META {\"test\":\"" + TEST_NAME
                        + "\",\"png_bytes\":4,\"png_fnv1a64\":\"" + hash + "\"}"));
                writeFrame(connection, false, 0x2, new byte[] {png[0], png[1]});
                writeFrame(connection, true, 0x0, new byte[] {png[2], png[3]});
                assertEquals("ACK " + TEST_NAME + " status=ok", readTextFrame(connection));
                File written = new File(store.getDirectory(), TEST_NAME + ".png");
                assertTrue(written.isFile() && written.length() == 4, String.valueOf(written));
                assertTrue(written.delete());
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void anUnsafeNameIsRefused() {
        assertEquals(null, ScreenshotStore.sanitise("../escape"));
        assertEquals(null, ScreenshotStore.sanitise(".."));
        assertEquals("a_b.png", ScreenshotStore.sanitise("a b.png"));
    }

    /// A masked client frame, which is the only kind a server will accept.
    private static void writeFrame(Tcp connection, boolean fin, int opcode, byte[] payload)
            throws IOException {
        byte[] mask = Crypto.randomBytes(4);
        byte[] frame = new byte[2 + 4 + payload.length];
        frame[0] = (byte) ((fin ? 0x80 : 0) | opcode);
        frame[1] = (byte) (0x80 | payload.length);
        System.arraycopy(mask, 0, frame, 2, 4);
        for (int iter = 0 ; iter < payload.length ; iter++) {
            frame[6 + iter] = (byte) (payload[iter] ^ mask[iter & 3]);
        }
        connection.write(frame, 0, frame.length);
    }

    private static String readTextFrame(Tcp connection) throws IOException {
        int first = readByte(connection);
        if (first < 0) {
            return null;
        }
        int length = readByte(connection) & 0x7f;
        if (length == 126) {
            length = (readByte(connection) << 8) | readByte(connection);
        }
        byte[] payload = new byte[length];
        int got = 0;
        while (got < length) {
            int read = connection.read(payload, got, length - got);
            if (read <= 0) {
                throw new IOException("the connection ended after " + got + " of " + length);
            }
            got += read;
        }
        return new String(payload, 0, length, "UTF-8");
    }

    private static String readUntilBlankLine(Tcp connection) throws IOException {
        StringBuilder out = new StringBuilder();
        while (out.length() < 8192) {
            int c = readByte(connection);
            if (c < 0) {
                break;
            }
            out.append((char) c);
            int n = out.length();
            if (n >= 4 && out.charAt(n - 4) == '\r' && out.charAt(n - 3) == '\n'
                    && out.charAt(n - 2) == '\r' && out.charAt(n - 1) == '\n') {
                break;
            }
        }
        return out.toString();
    }

    private static int readByte(Tcp connection) throws IOException {
        byte[] one = new byte[1];
        int read = connection.read(one, 0, 1);
        return read <= 0 ? -1 : one[0] & 0xff;
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) value.charAt(iter);
        }
        return out;
    }
}
