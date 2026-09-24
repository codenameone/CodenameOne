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
package com.codename1.tools.translator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The websocket server on the arm that actually ships.
 *
 * `maven/backend` covers the same protocol on a JVM, and that is not the same
 * test. The Java SE arm is always POOL mode -- `VirtualThread.supported()` is
 * false there -- and it never reaches a single native. This one builds the real
 * binary through the real `build.sh`, so the frames go through the translated
 * decoder, the reads and writes go through `cn1_backend_server.c`, and the
 * connection is served by a virtual thread on a host poller.
 *
 * Three things only this arm can show:
 *
 * - **Descriptor reuse.** The Java SE arm hands out monotonically increasing
 *   descriptor numbers and never recycles one, so every use-after-close bug in
 *   the upgrade and teardown paths is structurally invisible there. The kernel
 *   recycles immediately.
 * - **The borrowed read buffer.** `readIntoThreadBuffer` returns storage shared
 *   by every virtual thread on a host; on the JVM the buffer is per real thread,
 *   so a session that kept reading from it would still be correct there.
 * - **Virtual-thread parking.** A websocket parks between messages, which on this
 *   arm is a real park-and-resume through the native scheduler.
 *
 * Every assertion goes through a raw socket, for the reason
 * {@link BackendHttpIntegrationTest} gives for the same choice: no conformant
 * client sends the frames a server has to get right.
 */
class BackendWebSocketIntegrationTest {

    private static Process server;
    private static int port;
    private static Path work;
    private static Path outDir;
    private static String skipReason;

    @BeforeAll
    static void startServer() throws Exception {
        if (CompilerHelper.isWindows()) {
            skipReason = "the server-side backend is POSIX-only for now";
            BackendTestSupport.skipOrFail(skipReason);
            return;
        }
        Path backend = Paths.get("..", "backend").normalize().toAbsolutePath();
        BackendTestSupport.require(Files.isDirectory(backend), "vm/backend is not present");
        Path jdk8 = BackendTestSupport.findJdk8();
        BackendTestSupport.require(jdk8 != null, "no JDK 8 available to compile the backend");

        work = Files.createTempDirectory("backend-websocket");
        outDir = Files.createDirectories(work.resolve("screenshots"));
        Path binary = work.resolve("cn1ss");

        // The screenshot server rather than a purpose-built fixture: it is what CI
        // actually runs, so a break here is a break in the transport every
        // on-device suite depends on, and the application protocol gets covered
        // alongside the frame layer.
        String failure = BackendTestSupport.build("Cn1ssScreenshotServer", "demo/cn1ss",
                binary, jdk8);
        if (failure != null) {
            BackendTestSupport.skipOrFail(failure);
            return;
        }

        // Proves the binary works before any test blames the protocol for a
        // toolchain problem.
        ProcessBuilder check = new ProcessBuilder(binary.toString(), "--port", "0",
                "--out", outDir.toString(), "--selfcheck");
        check.redirectErrorStream(true);
        Process checked = check.start();
        assertTrue(checked.waitFor(60, java.util.concurrent.TimeUnit.SECONDS),
                "the self-check did not finish");
        assertEquals(0, checked.exitValue(), "the translated server failed its own self-check");

        port = BackendTestSupport.freePort();
        // Started directly rather than through BackendTestSupport.start, which
        // takes no arguments: --port and --out reach the translated main through
        // cn1MainArgs, and this test is partly about that being true.
        ProcessBuilder run = new ProcessBuilder(binary.toString(), "--port",
                String.valueOf(port), "--out", outDir.toString());
        // A short HTTP read deadline, so idleLongerThanTheRequestTimeout can wait
        // past it in seconds rather than in the default fifteen. It also makes the
        // test sharper: the websocket allowance it must NOT inherit is now four
        // times the wait.
        run.environment().put("CN1_HTTP_TIMEOUT_MS", "1500");
        run.redirectErrorStream(true);
        run.redirectOutput(work.resolve("server.log").toFile());
        server = run.start();
        assertTrue(BackendTestSupport.waitForPort(port, 30000),
                "the translated server never bound " + port);
    }

    @AfterAll
    static void stopServer() {
        BackendTestSupport.stop(server);
    }

    @Test
    @DisplayName("the handshake and an echo-shaped exchange work on the translated arm")
    void handshakeAndDelivery() throws Exception {
        Ws client = new Ws(port);
        try {
            byte[] png = new byte[4096];
            new Random(1L).nextBytes(png);
            deliver(client, "translated", png, true);
            assertTrue(Files.isRegularFile(outDir.resolve("translated.png")));
            assertArrayEquals(png, Files.readAllBytes(outDir.resolve("translated.png")));
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a fragmented multi-hundred-kilobyte message reassembles")
    void largeFragmentedMessage() throws Exception {
        // The real device shape: the hand-rolled clients split at 64KB, so a
        // screenshot is always a continuation sequence.
        Ws client = new Ws(port);
        try {
            byte[] png = new byte[320000];
            new Random(2L).nextBytes(png);
            deliver(client, "large", png, true);
            assertArrayEquals(png, Files.readAllBytes(outDir.resolve("large.png")));
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a protocol violation is refused with the code the RFC names")
    void protocolViolationCloses() throws Exception {
        Ws client = new Ws(port);
        try {
            // Unmasked, which RFC 6455 5.1 forbids a client to send.
            client.frame(true, 0, 1, false, "x".getBytes("UTF-8"));
            assertEquals(1002, client.readUntilClose());
        } finally {
            client.close();
        }
        Ws utf8 = new Ws(port);
        try {
            utf8.frame(true, 0, 1, true, new byte[]{(byte) 0xc0, (byte) 0x80});
            assertEquals(1007, utf8.readUntilClose());
        } finally {
            utf8.close();
        }
    }

    @Test
    @DisplayName("a connection served and closed does not break the next one on a reused descriptor")
    void descriptorReuse() throws Exception {
        // This is the test the JVM arm cannot run: Descriptors hands out
        // increasing ids and never recycles, so a use-after-close on a descriptor
        // NUMBER is invisible there. Here the kernel hands the number straight
        // back, so a session that outlived its close would be writing into the
        // connection opened on the line below.
        for (int iter = 0; iter < 12; iter++) {
            Ws client = new Ws(port);
            try {
                byte[] png = new byte[1024 + iter];
                new Random(iter).nextBytes(png);
                deliver(client, "reuse" + iter, png, false);
                assertArrayEquals(png, Files.readAllBytes(outDir.resolve("reuse" + iter + ".png")),
                        "round " + iter + " read back the wrong bytes");
            } finally {
                client.close();
            }
        }
    }

    @Test
    @DisplayName("a session that parks between messages resumes correctly")
    void parkAndResume() throws Exception {
        // On this arm the gap is a real virtual-thread park through the native
        // scheduler, and the resume has to come back to the same host with the
        // session's own buffer intact -- which is the whole reason the upgrade
        // stops borrowing the per-host read buffer.
        Ws client = new Ws(port);
        try {
            for (int iter = 0; iter < 4; iter++) {
                byte[] png = new byte[2048];
                new Random(100 + iter).nextBytes(png);
                deliver(client, "park" + iter, png, true);
                Thread.sleep(250);          // long enough to park
            }
            for (int iter = 0; iter < 4; iter++) {
                assertTrue(Files.isRegularFile(outDir.resolve("park" + iter + ".png")),
                        "message " + iter + " did not survive the park");
            }
        } finally {
            client.close();
        }
    }

    @Test
    @DisplayName("a session idle past the HTTP read timeout survives on the virtual-thread arm")
    void idleLongerThanTheRequestTimeout() throws Exception {
        // THE TEST THIS ARM EXISTS FOR. advance() re-arms SOCKET_TIMEOUT_MILLIS on
        // every park, which is right for a half-sent request and fatal for a
        // websocket parked between messages -- it closed any connection quiet for
        // more than fifteen seconds while both peers still believed it was open,
        // and logged nothing on either side.
        //
        // Invisible on the Java SE arm, which is pool mode and never goes through
        // advance(). It cost 150 of 181 screenshots on the browser leg before CI
        // found it.
        Ws client = new Ws(port);
        try {
            byte[] before = new byte[512];
            new Random(31L).nextBytes(before);
            deliver(client, "idlebefore", before, false);

            Thread.sleep(IDLE_PAST_REQUEST_TIMEOUT_MILLIS);

            byte[] after = new byte[512];
            new Random(32L).nextBytes(after);
            deliver(client, "idleafter", after, false);
            assertArrayEquals(after, Files.readAllBytes(outDir.resolve("idleafter.png")));
        } finally {
            client.close();
        }
    }

    /**
     * Past the default request timeout and far short of the websocket one, so this
     * measures the distinction rather than the wall clock. CN1_HTTP_TIMEOUT_MS is
     * lowered for the server this class starts so the wait stays short.
     */
    private static final long IDLE_PAST_REQUEST_TIMEOUT_MILLIS = 6000;

    @Test
    @DisplayName("a PING between fragments is answered without joining the message")
    void pingBetweenFragments() throws Exception {
        Ws client = new Ws(port);
        try {
            byte[] png = new byte[600];
            new Random(7L).nextBytes(png);
            client.frame(true, 0, 1, true, meta("interleaved", png).getBytes("UTF-8"));
            client.frame(false, 0, 2, true, slice(png, 0, 300));
            client.frame(true, 0, 9, true, "mid".getBytes("UTF-8"));
            assertTrue(client.readFrame());
            assertEquals(0xA, client.opcode, "the PING was not answered");
            client.frame(true, 0, 0, true, slice(png, 300, 300));
            assertTrue(client.readFrame());
            assertTrue(new String(client.payload, "UTF-8").startsWith("ACK interleaved"));
            assertArrayEquals(png, Files.readAllBytes(outDir.resolve("interleaved.png")),
                    "the control frame joined the message");
        } finally {
            client.close();
        }
    }

    // ------------------------------------------------------------------ helpers

    private static byte[] slice(byte[] source, int offset, int length) {
        byte[] out = new byte[length];
        System.arraycopy(source, offset, out, 0, length);
        return out;
    }

    private static String meta(String name, byte[] png) {
        return "META {\"test\":\"" + name + "\",\"png_bytes\":" + png.length
                + ",\"png_fnv1a64\":\"" + fnv1a64Hex(png) + "\"}";
    }

    /** Sends one screenshot the way a device does and waits for the ACK. */
    private static void deliver(Ws client, String name, byte[] png, boolean fragment)
            throws Exception {
        client.frame(true, 0, 1, true, meta(name, png).getBytes("UTF-8"));
        if (fragment) {
            int chunk = 65536;
            for (int offset = 0; offset < png.length; offset += chunk) {
                int length = Math.min(chunk, png.length - offset);
                client.frame(offset + length >= png.length, 0,
                        offset == 0 ? 2 : 0, true, slice(png, offset, length));
            }
        } else {
            client.frame(true, 0, 2, true, png);
        }
        assertTrue(client.readFrame(), "no answer to " + name);
        String ack = new String(client.payload, "UTF-8");
        assertTrue(ack.startsWith("ACK " + name + " status=ok"), "unexpected answer: " + ack);
    }

    static String fnv1a64Hex(byte[] bytes) {
        long hash = 0xcbf29ce484222325L;
        for (int iter = 0; iter < bytes.length; iter++) {
            hash ^= bytes[iter] & 0xff;
            hash *= 0x100000001b3L;
        }
        StringBuilder out = new StringBuilder(16);
        for (int shift = 60; shift >= 0; shift -= 4) {
            int nibble = (int) ((hash >>> shift) & 0xf);
            out.append((char) (nibble < 10 ? '0' + nibble : 'a' + (nibble - 10)));
        }
        return out.toString();
    }

    /** A raw websocket client that can also be wrong on purpose. */
    private static final class Ws implements Closeable {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private final Random random = new Random(9L);
        int opcode;
        byte[] payload;

        Ws(int port) throws Exception {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(30000);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            byte[] nonce = new byte[16];
            random.nextBytes(nonce);
            String key = Base64.getEncoder().encodeToString(nonce);
            out.write(("GET / HTTP/1.1\r\nHost: 127.0.0.1\r\nUpgrade: websocket\r\n"
                    + "Connection: Upgrade\r\nSec-WebSocket-Version: 13\r\n"
                    + "Sec-WebSocket-Key: " + key + "\r\n\r\n").getBytes("ISO-8859-1"));
            out.flush();
            StringBuilder head = new StringBuilder();
            while (!head.toString().endsWith("\r\n\r\n")) {
                int c = in.read();
                if (c < 0) {
                    throw new IOException("the server closed during the handshake");
                }
                head.append((char) c);
            }
            if (!head.toString().startsWith("HTTP/1.1 101")) {
                throw new IOException("no 101: " + head);
            }
            String expected = Base64.getEncoder().encodeToString(
                    MessageDigest.getInstance("SHA-1").digest(
                            (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("ISO-8859-1")));
            assertTrue(head.toString().contains("Sec-WebSocket-Accept: " + expected),
                    "the translated arm computed a different accept value:\n" + head);
        }

        void frame(boolean fin, int rsv, int op, boolean masked, byte[] body) throws IOException {
            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            frame.write((fin ? 0x80 : 0) | (rsv << 4) | op);
            int maskBit = masked ? 0x80 : 0;
            if (body.length < 126) {
                frame.write(maskBit | body.length);
            } else if (body.length <= 0xffff) {
                frame.write(maskBit | 126);
                frame.write((body.length >> 8) & 0xff);
                frame.write(body.length & 0xff);
            } else {
                frame.write(maskBit | 127);
                for (int iter = 7; iter >= 0; iter--) {
                    frame.write((int) (((long) body.length >>> (iter * 8)) & 0xff));
                }
            }
            byte[] key = new byte[4];
            if (masked) {
                random.nextBytes(key);
                frame.write(key, 0, 4);
            }
            for (int iter = 0; iter < body.length; iter++) {
                frame.write(masked ? (body[iter] ^ key[iter & 3]) : body[iter]);
            }
            out.write(frame.toByteArray());
            out.flush();
        }

        boolean readFrame() throws IOException {
            int first = in.read();
            if (first < 0) {
                return false;
            }
            int second = in.read();
            long length = second & 0x7f;
            if (length == 126) {
                length = (in.read() << 8) | in.read();
            } else if (length == 127) {
                length = 0;
                for (int iter = 0; iter < 8; iter++) {
                    length = (length << 8) | in.read();
                }
            }
            payload = new byte[(int) length];
            int got = 0;
            while (got < payload.length) {
                int read = in.read(payload, got, payload.length - got);
                if (read < 0) {
                    break;
                }
                got += read;
            }
            opcode = first & 0x0f;
            return true;
        }

        int readUntilClose() throws IOException {
            while (readFrame()) {
                if (opcode == 0x8) {
                    return payload.length >= 2
                            ? ((payload[0] & 0xff) << 8) | (payload[1] & 0xff) : -1;
                }
            }
            return -1;
        }

        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
