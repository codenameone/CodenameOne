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
package com.demo;

import com.codename1.backend.Crypto;
import com.codename1.backend.Tcp;

import java.io.File;
import java.io.IOException;

/**
 * Proves this binary really serves websockets, in about a second, before a CI leg
 * spends forty minutes finding out otherwise.
 *
 * The readiness line the runner waits for only says the process bound a port. It
 * does not say the handshake works, that the output directory is writable, that
 * continuation reassembly is intact, or -- on the translated arm -- that the
 * binary can execute on this machine at all. Every one of those has a failure mode
 * that looks, from the runner's side, exactly like a device that captured no
 * screenshots.
 *
 * So this opens a websocket to itself, sends a META frame and a binary message
 * SPLIT ACROSS TWO FRAMES (the common path, since the hand-rolled clients fragment
 * at 64KB), waits for the ACK, and checks the file landed.
 */
final class SelfCheck {
    private static final String TEST_NAME = "__cn1ss_selfcheck__";
    /** RFC 6455 1.3's worked example, and the accept value it prints. */
    private static final String RFC_KEY = "dGhlIHNhbXBsZSBub25jZQ==";
    private static final String RFC_ACCEPT = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";

    private SelfCheck() {
    }

    static boolean run(int port, File outDir) {
        Tcp connection = null;
        try {
            connection = Tcp.connect("127.0.0.1", port, 5000);
            // RFC 6455 1.3's own key, so the expected accept value is the one the
            // standard prints. A random key would work too, but then this check
            // could only compare the server against itself -- and the thing most
            // worth catching here is a translated SHA-1 binding that is missing or
            // wrong, which self-consistency cannot see.
            String key = RFC_KEY;
            String request = "GET / HTTP/1.1\r\nHost: 127.0.0.1\r\n"
                    + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                    + "Sec-WebSocket-Version: 13\r\nSec-WebSocket-Key: " + key + "\r\n\r\n";
            byte[] requestBytes = ascii(request);
            connection.write(requestBytes, 0, requestBytes.length);

            String head = readUntilBlankLine(connection);
            if(!head.startsWith("HTTP/1.1 101")) {
                System.err.println("[cn1ss] selfcheck: no 101, got " + firstLine(head));
                return false;
            }
            // THE ACCEPT VALUE, not just the status. A 101 says the server took
            // the upgrade; it does not say the handshake is one a browser will
            // accept. If Crypto.sha1's native went missing -- which is what
            // CN1_BACKEND_HTTPS=0 does, and what a mistyped symbol does silently
            // -- this server would still answer 101, every conforming client
            // would reject it, and the leg would find out only after the
            // forty-minute screenshot suite.
            if(head.indexOf("Sec-WebSocket-Accept: " + RFC_ACCEPT) < 0) {
                System.err.println("[cn1ss] selfcheck: wrong Sec-WebSocket-Accept; expected "
                        + RFC_ACCEPT + " for the RFC 6455 key. The SHA-1 binding is "
                        + "missing or wrong.");
                return false;
            }

            byte[] png = {(byte)0x89, 'P', 'N', 'G'};
            String hash = Cn1ssScreenshotServer.fnv1a64Hex(png, 0, png.length);
            String meta = "META {\"test\":\"" + TEST_NAME + "\",\"png_bytes\":" + png.length
                    + ",\"png_fnv1a64\":\"" + hash + "\"}";
            writeFrame(connection, true, 0x1, ascii(meta));
            // Two frames for four bytes, on purpose: this is the reassembly path
            // every real screenshot takes.
            writeFrame(connection, false, 0x2, new byte[]{png[0], png[1]});
            writeFrame(connection, true, 0x0, new byte[]{png[2], png[3]});

            String ack = readTextFrame(connection);
            if(ack == null || !ack.startsWith("ACK " + TEST_NAME)) {
                System.err.println("[cn1ss] selfcheck: expected an ACK, got " + ack);
                return false;
            }
            if(ack.indexOf("status=ok") < 0) {
                System.err.println("[cn1ss] selfcheck: " + ack);
                return false;
            }
            File written = new File(outDir, TEST_NAME + ".png");
            if(!written.isFile() || written.length() != png.length) {
                System.err.println("[cn1ss] selfcheck: " + written + " was not written");
                return false;
            }
            if(!written.delete()) {
                System.err.println("[cn1ss] selfcheck: could not remove " + written);
                return false;
            }
            System.out.println("CN1SS:INFO:selfcheck=ok");
            return true;
        } catch (Exception err) {
            System.err.println("[cn1ss] selfcheck failed: " + err);
            return false;
        } finally {
            if(connection != null) {
                connection.close();
            }
        }
    }

    /** A masked client frame, which is the only kind a server will accept. */
    private static void writeFrame(Tcp connection, boolean fin, int opcode, byte[] payload)
            throws IOException {
        byte[] mask = Crypto.randomBytes(4);
        byte[] frame = new byte[2 + 4 + payload.length];
        frame[0] = (byte)((fin ? 0x80 : 0) | opcode);
        frame[1] = (byte)(0x80 | payload.length);      // selfcheck payloads are tiny
        System.arraycopy(mask, 0, frame, 2, 4);
        for(int iter = 0 ; iter < payload.length ; iter++) {
            frame[6 + iter] = (byte)(payload[iter] ^ mask[iter & 3]);
        }
        connection.write(frame, 0, frame.length);
    }

    private static String readTextFrame(Tcp connection) throws IOException {
        int first = readByte(connection);
        if(first < 0) {
            return null;
        }
        int second = readByte(connection);
        int length = second & 0x7f;
        if(length == 126) {
            length = (readByte(connection) << 8) | readByte(connection);
        }
        byte[] payload = new byte[length];
        readFully(connection, payload, length);
        return new String(payload, 0, length, "UTF-8");
    }

    private static String readUntilBlankLine(Tcp connection) throws IOException {
        StringBuilder out = new StringBuilder();
        while(out.length() < 8192) {
            int c = readByte(connection);
            if(c < 0) {
                break;
            }
            out.append((char)c);
            int length = out.length();
            if(length >= 4 && out.charAt(length - 4) == '\r' && out.charAt(length - 3) == '\n'
                    && out.charAt(length - 2) == '\r' && out.charAt(length - 1) == '\n') {
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

    private static void readFully(Tcp connection, byte[] buffer, int length) throws IOException {
        int got = 0;
        while(got < length) {
            int read = connection.read(buffer, got, length - got);
            if(read <= 0) {
                throw new IOException("the connection ended after " + got + " of " + length);
            }
            got += read;
        }
    }

    private static String firstLine(String value) {
        int end = value.indexOf('\r');
        return end < 0 ? value : value.substring(0, end);
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte)value.charAt(iter);
        }
        return out;
    }
}
