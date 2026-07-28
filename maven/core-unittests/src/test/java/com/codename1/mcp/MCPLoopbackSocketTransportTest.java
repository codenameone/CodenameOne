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
package com.codename1.mcp;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Framing and session handling for the portable MCP socket transport - the one that
/// lets an agent attach to an app running on a device, not only on the desktop.
///
/// Drives the transport's own streams directly rather than through
/// {@code Socket.listenLoopback}: what is under test here is the message framing and the
/// attach / detach / close handshake. Going through the socket layer would instead be
/// exercising the platform's thread scheduling.
class MCPLoopbackSocketTransportTest {

    private MCPLoopbackSocketTransport transport;

    @AfterEach
    void closeTransport() {
        // The transport is process-wide; leaving one open would fail the next test.
        if (transport != null) {
            transport.close();
            transport = null;
        }
    }

    /// Waits for a condition instead of sleeping for a guessed interval. A fixed sleep
    /// either wastes time or, on a loaded machine, fires before the thread it is waiting
    /// on has got anywhere - which is how timing-based tests turn flaky.
    private static void await(String what, java.util.concurrent.Callable<Boolean> condition)
            throws Exception {
        // nanoTime, not currentTimeMillis: a wall clock can be stepped by NTP mid-wait,
        // which would either cut the wait short or extend it, and a test that fails when
        // the clock is adjusted is worse than one that sleeps.
        long deadline = System.nanoTime() + 10000L * 1000000L;
        while (System.nanoTime() < deadline) {
            if (condition.call()) {
                return;
            }
            Thread.sleep(5);
        }
        fail("timed out waiting for " + what);
    }

    private MCPLoopbackSocketTransport attached(String clientBytes) throws IOException {
        transport = new MCPLoopbackSocketTransport(47811);
        transport.attach(new ByteArrayInputStream(clientBytes.getBytes("UTF-8")),
                new ByteArrayOutputStream());
        return transport;
    }

    @Test
    void readsWholeLineDelimitedMessages() throws Exception {
        MCPLoopbackSocketTransport t = attached("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}\n");
        String message = t.readMessage();
        assertNotNull(message);
        assertEquals("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}", message,
                "the newline delimits the frame and is not part of the message");
    }

    @Test
    void splitsConsecutiveMessagesArrivingTogether() throws Exception {
        // Both messages in one chunk: framing must not depend on packet boundaries.
        MCPLoopbackSocketTransport t = attached("{\"id\":1}\n{\"id\":2}\n");
        assertEquals("{\"id\":1}", t.readMessage());
        assertEquals("{\"id\":2}", t.readMessage());
    }

    @Test
    void toleratesCarriageReturnsBeforeTheDelimiter() throws Exception {
        MCPLoopbackSocketTransport t = attached("{\"id\":1}\r\n");
        assertEquals("{\"id\":1}", t.readMessage(),
                "a CRLF-writing client must not leave a stray carriage return in the JSON");
    }

    @Test
    void keepsACarriageReturnThatIsNotPartOfTheDelimiter() throws Exception {
        // Only CR immediately before LF is delimiter punctuation. A CR anywhere else is
        // payload, and a transport must hand up the bytes it was given rather than edit
        // them, however unlikely the sender.
        MCPLoopbackSocketTransport t = attached("{\"a\":\"x\ry\"}\n");
        assertEquals("{\"a\":\"x\ry\"}", t.readMessage(),
                "a carriage return inside the payload must survive the framing");
    }

    @Test
    void treatsOnlyTheFinalCarriageReturnAsDelimiterPunctuation() throws Exception {
        // "\r\r\n" is one payload CR followed by a CRLF delimiter; consuming both CRs
        // would corrupt the message, and consuming neither would leave a stray byte.
        MCPLoopbackSocketTransport t = attached("ab\r\r\n");
        assertEquals("ab\r", t.readMessage(),
                "the trailing CRLF is the delimiter; the CR before it is content");
    }

    @Test
    void writesMessagesWithATrailingDelimiter() throws Exception {
        transport = new MCPLoopbackSocketTransport(47811);
        ByteArrayOutputStream toClient = new ByteArrayOutputStream();
        transport.attach(new ByteArrayInputStream(new byte[0]), toClient);
        transport.writeMessage("{\"id\":7}");
        assertEquals("{\"id\":7}\n", new String(toClient.toByteArray(), "UTF-8"),
                "every reply must be one line so the peer can frame it");
    }

    @Test
    void writeFailsWhenNoAgentIsAttached() {
        transport = new MCPLoopbackSocketTransport(47811);
        assertThrows(IOException.class, () -> transport.writeMessage("{\"id\":1}"),
                "writing with nobody attached is an error, not a silent no-op");
    }

    @Test
    void aDisconnectedClientDoesNotEndTheServer() throws Exception {
        // The first client's stream ends; the transport must wait for the next agent
        // instead of reporting end-of-transport, so reconnects keep working.
        final MCPLoopbackSocketTransport t = new MCPLoopbackSocketTransport(47811);
        transport = t;
        // Counts the end-of-stream read, so the test can wait for the reader to have SEEN
        // the disconnect rather than sleeping and hoping.
        final AtomicLong endOfStreamReads = new AtomicLong();
        t.attach(new InputStream() {
            @Override
            public int read() {
                endOfStreamReads.incrementAndGet();
                return -1;
            }
        }, new ByteArrayOutputStream());

        final String[] out = new String[1];
        Thread reader = new Thread(() -> {
            try {
                out[0] = t.readMessage();
            } catch (IOException ignored) {
                // surfaces as a null message below
            }
        });
        reader.start();
        await("the reader to consume the disconnect", () -> endOfStreamReads.get() > 0);
        assertTrue(reader.isAlive(), "the reader should be waiting for the next client");

        t.attach(new ByteArrayInputStream("{\"id\":9}\n".getBytes("UTF-8")),
                new ByteArrayOutputStream());
        reader.join(3000);
        assertEquals("{\"id\":9}", out[0], "the reconnecting agent's message should arrive");
    }

    @Test
    void aClientThatNeverDelimitsItsFrameCannotExhaustMemory() throws Exception {
        // A stream that never ends and never delimits: without a ceiling on the frame the
        // reader would accumulate until the process died, and on a device any other
        // installed app can open this connection. It must be dropped like any other bad
        // client, leaving the server available for the next session.
        final MCPLoopbackSocketTransport t = new MCPLoopbackSocketTransport(47811);
        transport = t;
        // Counting what the client is asked for is what distinguishes a ceiling from no
        // ceiling here: the thread stays alive either way, but an uncapped reader never
        // stops asking for more.
        final AtomicLong served = new AtomicLong();
        t.attach(new InputStream() {
            @Override
            public int read() {
                served.incrementAndGet();
                return 'x';
            }
        }, new ByteArrayOutputStream());

        final String[] out = new String[1];
        Thread reader = new Thread(() -> {
            try {
                out[0] = t.readMessage();
            } catch (IOException ignored) {
                // surfaces as a null message below
            }
        });
        reader.start();
        // Wait for consumption to stop rather than for a fixed interval: the ceiling is
        // what makes it stop, and how long 8MB takes depends on the machine.
        await("the reader to stop consuming at the frame ceiling", () -> {
            long before = served.get();
            Thread.sleep(50);
            return before == served.get();
        });

        t.attach(new ByteArrayInputStream("{\"id\":11}\n".getBytes("UTF-8")),
                new ByteArrayOutputStream());
        reader.join(5000);
        assertEquals("{\"id\":11}", out[0],
                "the next client's message should arrive normally");
    }

    @Test
    void aFrameOfExactlyTheCeilingIsStillDelivered() throws Exception {
        // The ceiling is on the payload, so a frame of exactly that size is legal and its
        // delimiter still has to be read. Rejecting at >= would have made the largest
        // permitted message unsendable, which is the classic off-by-one on a limit.
        final int size = MCPLoopbackSocketTransport.MAX_FRAME_BYTES;
        final MCPLoopbackSocketTransport t = new MCPLoopbackSocketTransport(47811);
        transport = t;
        t.attach(new InputStream() {
            private int pos;

            @Override
            public int read() {
                if (pos < size) {
                    pos++;
                    return 'x';
                }
                if (pos == size) {
                    pos++;
                    return '\n';
                }
                return -1;
            }
        }, new ByteArrayOutputStream());

        // Read on another thread with a deadline. Should the ceiling ever go back to
        // rejecting at exactly the limit, this frame is discarded as a disconnect and the
        // reader parks waiting for the next client -- so a direct call here would hang CI
        // instead of failing it.
        final String[] out = new String[1];
        Thread reader = new Thread(() -> {
            try {
                out[0] = t.readMessage();
            } catch (IOException ignored) {
                // surfaces as a null message below
            }
        });
        reader.start();
        reader.join(30000);
        assertNotNull(out[0], "a frame of exactly the ceiling must be delivered");
        assertEquals(size, out[0].length(),
                "the whole payload should arrive, without the delimiter");
    }

    @Test
    void aFrameCutShortByADisconnectIsNotDeliveredAsAMessage() throws Exception {
        // A client that dies mid-frame leaves bytes with no delimiter. Delivering those
        // would have the server parse a truncated request, fail to answer it (the peer is
        // gone) and end the loop; the transport must read it as a plain disconnect and
        // stay available, exactly as it does for a clean end of stream.
        final MCPLoopbackSocketTransport t = new MCPLoopbackSocketTransport(47811);
        transport = t;
        // Yields a partial frame then end of stream, counting the EOF so the test can wait
        // for the reader to have reached it.
        final AtomicLong truncatedReads = new AtomicLong();
        final byte[] partial = "{\"id\":1,\"method\":\"init".getBytes("UTF-8");
        t.attach(new InputStream() {
            private int pos;

            @Override
            public int read() {
                if (pos < partial.length) {
                    return partial[pos++] & 0xff;
                }
                truncatedReads.incrementAndGet();
                return -1;
            }
        }, new ByteArrayOutputStream());

        final String[] out = new String[1];
        Thread reader = new Thread(() -> {
            try {
                out[0] = t.readMessage();
            } catch (IOException ignored) {
                // surfaces as a null message below
            }
        });
        reader.start();
        await("the reader to consume the truncated frame", () -> truncatedReads.get() > 0);
        assertTrue(reader.isAlive(),
                "a truncated frame must leave the reader waiting for the next client, "
                        + "not hand up a partial message");

        t.attach(new ByteArrayInputStream("{\"id\":9}\n".getBytes("UTF-8")),
                new ByteArrayOutputStream());
        reader.join(3000);
        assertEquals("{\"id\":9}", out[0],
                "the next agent's whole message should be what finally arrives");
    }

    @Test
    void closeUnblocksAPendingRead() throws Exception {
        final MCPLoopbackSocketTransport t = new MCPLoopbackSocketTransport(47811);
        transport = t;
        final String[] out = new String[1];
        final boolean[] done = new boolean[1];
        final Thread reader = new Thread(() -> {
            try {
                out[0] = t.readMessage();
            } catch (IOException ignored) {
                // end of transport is reported as a null message
            }
            done[0] = true;
        });
        reader.start();
        // Park the reader before closing, so this exercises "close wakes a waiting reader"
        // rather than "close had already been called". WAITING is precisely the state
        // lock.wait() puts it in.
        // Polling for a parked state is not the same as sleeping and hoping: once the
        // reader reaches wait() it STAYS there until close() notifies it, so this cannot
        // miss the window. TIMED_WAITING is accepted too, so a future timed wait in the
        // transport would not silently turn this into a ten second stall.
        await("the reader to park waiting for a client", () -> {
            Thread.State state = reader.getState();
            return state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING;
        });
        t.close();
        reader.join(3000);

        assertTrue(done[0], "close must wake a reader that is waiting for a client");
        assertNull(out[0], "end of transport is signalled by a null message");
    }

    @Test
    void closingTheTransportClosesBothClientStreams() throws Exception {
        // Forgetting the fields is not enough. A writer that already captured the output
        // stream would go on writing into a session that has ended, and the socket would
        // stay open until the connection callback happened to unwind.
        final boolean[] inClosed = new boolean[1];
        final boolean[] outClosed = new boolean[1];
        transport = new MCPLoopbackSocketTransport(47811);
        transport.attach(new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() {
                inClosed[0] = true;
            }
        }, new ByteArrayOutputStream() {
            @Override
            public void close() {
                outClosed[0] = true;
            }
        });

        transport.close();
        transport = null;
        assertTrue(inClosed[0], "close must close the client's input stream");
        assertTrue(outClosed[0], "close must close the client's output stream too");
    }

    @Test
    void aPipedClientRoundTrips() throws Exception {
        // Closest thing to a real socket without one: the client end is a live pipe, so
        // the read blocks until bytes actually arrive rather than hitting a buffer's end.
        PipedOutputStream clientWrites = new PipedOutputStream();
        InputStream serverReads = new PipedInputStream(clientWrites, 4096);
        transport = new MCPLoopbackSocketTransport(47811);
        transport.attach(serverReads, new ByteArrayOutputStream());

        clientWrites.write("{\"id\":42}\n".getBytes("UTF-8"));
        clientWrites.flush();
        assertEquals("{\"id\":42}", transport.readMessage());
    }
}
