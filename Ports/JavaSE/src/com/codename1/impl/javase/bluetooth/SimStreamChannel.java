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
package com.codename1.impl.javase.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * One endpoint of a bidirectional in-memory byte channel built from
 * {@code java.io} piped streams -- the simulated stack's stand-in for an
 * RFCOMM connection or an L2CAP channel. Channels come in cross-connected
 * pairs from {@link #createPair()}: what one side writes the other side
 * reads. Closing either side propagates EOF to the peer's input stream.
 *
 * <p>The streams block, exactly like the real transports -- consume them
 * off the EDT.</p>
 */
public final class SimStreamChannel {

    /**
     * Generous pipe buffer so tests exchanging a few KiB never deadlock on
     * a full pipe.
     */
    private static final int PIPE_BUFFER = 1 << 16;

    private final PipedInputStream in;
    private final PipedOutputStream out;
    private volatile boolean open = true;

    private SimStreamChannel(PipedInputStream in, PipedOutputStream out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Creates a cross-connected channel pair: bytes written to
     * {@code pair[0]} are read from {@code pair[1]} and vice versa.
     */
    public static SimStreamChannel[] createPair() {
        try {
            PipedInputStream aIn = new PipedInputStream(PIPE_BUFFER);
            PipedOutputStream bOut = new PipedOutputStream(aIn);
            PipedInputStream bIn = new PipedInputStream(PIPE_BUFFER);
            PipedOutputStream aOut = new PipedOutputStream(bIn);
            return new SimStreamChannel[] {
                    new SimStreamChannel(aIn, aOut),
                    new SimStreamChannel(bIn, bOut)
            };
        } catch (IOException ex) {
            // piped stream construction cannot fail in practice
            throw new IllegalStateException("Failed to create pipe pair: "
                    + ex, ex);
        }
    }

    /** The blocking input stream of this side of the channel. */
    public InputStream getInputStream() {
        return in;
    }

    /** The blocking output stream of this side of the channel. */
    public OutputStream getOutputStream() {
        return out;
    }

    /** {@code true} until {@link #close()} was called on this side. */
    public boolean isOpen() {
        return open;
    }

    /**
     * Closes both streams of this side; the peer's reads observe EOF.
     * Never throws; idempotent.
     */
    public void close() {
        open = false;
        try {
            out.close();
        } catch (IOException ignored) {
        }
        try {
            in.close();
        } catch (IOException ignored) {
        }
    }
}
