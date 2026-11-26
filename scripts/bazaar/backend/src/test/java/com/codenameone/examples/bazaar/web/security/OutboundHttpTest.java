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
package com.codenameone.examples.bazaar.web.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Calls to URLs someone else chose: capped while read, timed as a whole, and
/// refused for a private address. The far end is a stream the test controls
/// rather than a socket, so the same checks run on the compiled server, whose
/// class library has no server sockets.
class OutboundHttpTest {
    /// Hands out `chunk`-byte reads forever (or `total` bytes, then the end),
    /// sleeping `pauseMillis` before each, and counts what it handed out.
    static final class Far extends InputStream {
        final long total;
        final int chunk;
        final long pauseMillis;
        long given;

        Far(long total, int chunk, long pauseMillis) {
            this.total = total;
            this.chunk = chunk;
            this.pauseMillis = pauseMillis;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (pauseMillis > 0) {
                try {
                    Thread.sleep(pauseMillis);
                } catch (InterruptedException e) {
                    throw new IOException(e.getMessage());
                }
            }
            if (total >= 0 && given >= total) {
                return -1;
            }
            int n = Math.min(len, chunk);
            if (total >= 0) {
                n = (int) Math.min(n, total - given);
            }
            for (int i = 0; i < n; i++) {
                b[off + i] = (byte) 'x';
            }
            given += n;
            return n;
        }
    }

    private static long later(long millis) {
        return System.currentTimeMillis() + millis;
    }

    @Test
    void anEndlessAnswerIsCutOffAtTheCapNotBuffered() {
        Far far = new Far(-1, 4096, 0);
        assertThrows(IOException.class,
                () -> OutboundHttp.readCapped(far, 64 * 1024, later(5000), 5000));
        // Refused at the first read past the cap, not after reading on.
        assertTrue(far.given <= 64 * 1024 + 4096, "read " + far.given);
    }

    @Test
    void anAnswerWithinTheCapArrivesWhole() throws Exception {
        byte[] got = OutboundHttp.readCapped(new Far(10000, 4096, 0), 10000, later(5000), 5000);
        assertEquals(10000, got.length);
    }

    @Test
    void aSlowDripPastTheBudgetFails() {
        long started = System.currentTimeMillis();
        Far far = new Far(-1, 1, 50);
        assertThrows(IOException.class,
                () -> OutboundHttp.readCapped(far, 1 << 20, later(300), 300));
        assertTrue(System.currentTimeMillis() - started < 5000);
    }

    @Test
    void aPrivateAddressIsRefusedBeforeAnythingIsSent() {
        // Nothing listens there; the guard refuses before a connection is tried.
        assertThrows(IOException.class, () -> OutboundHttp.send("GET",
                "http://127.0.0.1:9/", null, null, 1000, 1024));
        assertThrows(IOException.class, () -> OutboundHttp.send("GET",
                "http://169.254.169.254/latest/meta-data", null, null, 1000, 1024));
    }
}
