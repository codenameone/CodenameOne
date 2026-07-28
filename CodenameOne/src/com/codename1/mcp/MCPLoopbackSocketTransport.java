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

import com.codename1.io.Log;
import com.codename1.io.Socket;
import com.codename1.io.SocketConnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Loopback socket MCP transport built on the portable [com.codename1.io.Socket] API, so
/// an agent can attach to a running app on ANY port that can bind the loopback interface -
/// a device or simulator, not only the desktop.
///
/// The channel is bound to loopback only. That is the security boundary: an attaching
/// agent can read the screen and drive the UI, so it must already be on the device (or on
/// a host forwarding into it), never merely on the same network. There is no wildcard
/// fallback; if the port cannot bind loopback this transport refuses to open.
///
/// The listening socket stays bound across client sessions, so an agent may disconnect and
/// reconnect - each drive call is typically its own short-lived connection - without the
/// server thread exiting.
public final class MCPLoopbackSocketTransport implements MCPTransport {
    /// Lets [Connection], which the socket API instantiates reflectively via a public
    /// no-argument constructor, find the transport it belongs to. A single reference
    /// suffices because MCP runs one server per process.
    private static MCPLoopbackSocketTransport active;

    /// Ceiling on one incoming frame. What arrives here are requests - tool calls and UI
    /// actions - while the bulky payloads, screenshots among them, travel the other way,
    /// so this sits far above any legitimate message. It exists to bound what an ill
    /// behaved or hostile client can make the server allocate.
    static final int MAX_FRAME_BYTES = 8 * 1024 * 1024;

    /// Bulk reads land here and are consumed a byte at a time. Whatever follows a
    /// delimiter stays put for the next message, which is what makes buffering safe: the
    /// bytes are held rather than swallowed.
    ///
    /// Touched only by the reader thread, and reset in readMessage whenever the attached
    /// stream is not the one that filled it, so a new client never inherits a dead
    /// client's tail.
    private final byte[] chunk = new byte[8192];
    private int chunkPos;
    private int chunkLen;
    private InputStream chunkSource; // NOPMD borrowed identity, never read from or closed

    private final int port;
    private final Object lock = new Object();
    private Socket.StopListening listening;
    private InputStream in;
    private OutputStream out;
    private boolean closed;

    MCPLoopbackSocketTransport(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /// Registers this transport as the socket factory when the platform has not supplied
    /// one of its own. The JavaSE port keeps its native implementation (it can use
    /// java.net directly); every other port with loopback support gets this one.
    static void registerIfPlatformHasNone() {
        if (MCP.hasPlatformSocketTransport()) {
            return;
        }
        if (!Socket.isLoopbackServerSocketSupported()) {
            // Registering regardless would be worse than not registering: the caller would
            // see a transport, believe the server started, and only find out on the
            // server's own thread that nothing can bind. Leaving the factory unset lets
            // startSocketServer fail on the calling thread, where it can be handled.
            return;
        }
        MCP.setSocketTransportFactory(new MCP.SocketTransportFactory() {
            @Override
            public MCPTransport createSocketTransport(int port) {
                return new MCPLoopbackSocketTransport(port);
            }
        });
    }

    @Override
    public void open() throws IOException {
        if (!Socket.isLoopbackServerSocketSupported()) {
            throw new IOException("This platform cannot bind a loopback server socket, so an "
                    + "MCP agent cannot attach to it");
        }
        synchronized (MCPLoopbackSocketTransport.class) {
            // Identity, not equality: is the open transport a DIFFERENT instance?
            if (active != null && active != this) { // NOPMD identity is the question
                throw new IOException("Another MCP socket transport is already open on port "
                        + active.port);
            }
            active = this;
        }
        try {
            listening = Socket.listenLoopback(port, Connection.class);
        } catch (RuntimeException ex) {
            // Two things go wrong if this escapes. The process-wide registration would stay
            // pointing at a transport that never started listening, so every later open()
            // would refuse, believing an agent is already served. And the server's reader
            // thread only handles IOException around open(), so a runtime exception would
            // kill that thread before it could clear its running flag, leaving the server
            // permanently "running" with nothing behind it.
            clearActiveIfOurs();
            IOException failure = new IOException("Failed to listen on loopback port " + port);
            failure.initCause(ex);
            throw failure;
        }
    }

    /// Releases the process-wide registration, but only when it is still this transport's.
    private void clearActiveIfOurs() {
        synchronized (MCPLoopbackSocketTransport.class) {
            if (active == this) { // NOPMD identity: is the slot still ours?
                active = null;
            }
        }
    }

    /// Called from the connection callback thread when a client attaches. The previous
    /// client, if any, is dropped: one agent drives at a time.
    void attach(InputStream is, OutputStream os) {
        InputStream previousIn; // NOPMD closed below, deliberately outside the lock
        OutputStream previousOut; // NOPMD closed below, outside the lock
        synchronized (lock) {
            previousIn = in;
            previousOut = out;
            in = is;
            out = os;
            lock.notifyAll();
        }
        // Dropping the previous client means closing its streams, not just forgetting
        // them. A reader parked in read() on the old stream is not watching this field,
        // so a client that holds its connection open and sends nothing would keep the
        // server serving a session that has already been replaced.
        //
        // Outside the lock, because closing is what wakes that reader and it takes the
        // lock on its way out.
        if (previousIn != is) { // NOPMD identity: the same stream re-attached is not a swap
            closeQuietly(previousIn);
        }
        if (previousOut != os) { // NOPMD identity: as above
            closeQuietly(previousOut);
        }
    }

    /// Closes a dropped client's stream. Failure is uninteresting: the stream is being
    /// discarded either way, and closing is best effort by nature.
    private static void closeQuietly(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // discarding this stream regardless
            }
        }
    }

    private static void closeQuietly(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // discarding this stream regardless
            }
        }
    }

    /// Called when a client's session ends.
    void detach(InputStream is) {
        synchronized (lock) {
            // Detach only if this is still the very same stream we attached, so a newer
            // client is not torn down by a stale one finishing late.
            if (in == is) { // NOPMD identity: same stream we attached?
                in = null;
                out = null;
                lock.notifyAll();
            }
        }
    }

    @Override
    public String readMessage() throws IOException {
        while (true) {
            // Borrowed reference: read the field under the lock, use it outside. The
            // stream is owned by the connection callback and closed in close(); closing
            // it here would end the session after a single message.
            InputStream stream; // NOPMD borrowed, not owned by this method
            synchronized (lock) {
                while (in == null && !closed) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                        // fall through: re-check the loop conditions
                    }
                }
                if (closed) {
                    return null;
                }
                stream = in;
            }
            if (stream != chunkSource) { // NOPMD identity: is this the stream we buffered?
                // A different client. Anything left from the previous one belongs to a
                // session that has ended and must not be read as this client's first
                // message.
                chunkSource = stream;
                chunkPos = 0;
                chunkLen = 0;
            }
            String line = readLine(stream);
            if (line != null) {
                return line;
            }
            // client disconnected; wait for the next one rather than ending the server
            detach(stream);
            if (isClosed()) {
                return null;
            }
        }
    }

    /// Reads one newline-delimited UTF-8 message. Returns null at end of stream, which
    /// includes a frame cut short by the client going away.
    ///
    /// Reads in blocks rather than a byte at a time - a frame near the ceiling would
    /// otherwise cost millions of single-byte reads. Buffering is safe here because
    /// whatever follows the delimiter is KEPT in the chunk for the next call, so no bytes
    /// belonging to the next message are swallowed.
    private String readLine(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        // A carriage return is only part of the delimiter when a newline follows it, so it
        // is held back one byte rather than dropped. Dropping every CR would silently edit
        // a payload that happened to contain one.
        boolean pendingCarriageReturn = false;
        while (true) {
            if (chunkPos >= chunkLen) {
                int read;
                try {
                    read = stream.read(chunk, 0, chunk.length);
                } catch (IOException ex) {
                    return null;   // a read error on the client is a disconnect
                }
                if (read < 0) {
                    // End of stream with no delimiter: the frame is truncated, so this is
                    // a disconnect and not a message, however many bytes arrived first.
                    // Handing the partial frame up would have the server fail to parse it,
                    // then fail to answer it (the peer is already gone), and end the loop -
                    // when the whole point of this transport is to stay bound and await a
                    // reconnect.
                    return null;
                }
                chunkPos = 0;
                chunkLen = read;
                if (read == 0) {
                    continue;      // nothing this time; ask again rather than call it EOF
                }
            }
            int b = chunk[chunkPos++] & 0xff;
            if (pendingCarriageReturn) {
                pendingCarriageReturn = false;
                if (b == '\n') {
                    return toUtf8(buffer);          // the CR belonged to a CRLF delimiter
                }
                if (!append(buffer, '\r')) {        // it belonged to the payload after all
                    return null;
                }
            }
            if (b == '\r') {
                pendingCarriageReturn = true;
                continue;
            }
            if (b == '\n') {
                return toUtf8(buffer);
            }
            if (!append(buffer, b)) {
                return null;
            }
        }
    }

    /// Appends one payload byte, refusing once the frame would pass [#MAX_FRAME_BYTES].
    /// Enforced here rather than at the top of the read loop so the ceiling is exact: a
    /// frame of precisely the limit is accepted and its delimiter still read, and one byte
    /// beyond is refused before it is ever buffered.
    ///
    /// A refusal is reported to the caller as end of stream. A client sending no delimiter
    /// would otherwise grow this buffer until the process ran out of memory, and on a
    /// device any other app installed alongside can open this connection.
    private static boolean append(ByteArrayOutputStream buffer, int b) {
        if (buffer.size() >= MAX_FRAME_BYTES) {
            return false;
        }
        buffer.write(b);
        return true;
    }

    /// Note for anyone tempted by ByteArrayOutputStream.toString(String), which would
    /// decode without toByteArray()'s copy: core is compiled against CLDC11
    /// (Ports/CLDC11), whose ByteArrayOutputStream declares only the no-argument
    /// toString(). The overload exists on the JDK and in the ParparVM runtime, so a Maven
    /// build of core compiles it happily and the Ant build then fails.
    private static String toUtf8(ByteArrayOutputStream buffer) throws IOException {
        return new String(buffer.toByteArray(), "UTF-8");
    }

    @Override
    public void writeMessage(String message) throws IOException {
        // Borrowed reference (see readMessage): this writes one message, it does not own
        // the session and must not close the stream.
        OutputStream stream; // NOPMD borrowed, not owned by this method
        synchronized (lock) {
            stream = out;
        }
        if (stream == null) {
            throw new IOException("No MCP client is attached");
        }
        stream.write(message.getBytes("UTF-8"));
        stream.write('\n');
        stream.flush();
    }

    private boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    @Override
    public void close() {
        Socket.StopListening l;
        // Both streams come out of their fields under the lock so they can be closed
        // below, outside it: closing is what unblocks a reader parked in read(), and it
        // takes the lock on its way out.
        InputStream is; // NOPMD closed below, deliberately outside the lock
        OutputStream os; // NOPMD closed below, outside the lock
        synchronized (lock) {
            closed = true;
            l = listening;
            listening = null;
            is = in;
            os = out;
            in = null;
            out = null;
            lock.notifyAll();
        }
        // Only if it is still ours: a transport opened after this one keeps its slot.
        clearActiveIfOurs();
        if (l != null) {
            l.stop();
        }
        // Closing the output as well as the input: forgetting the field is not enough,
        // because a writer that already captured it would go on writing to a session that
        // has ended, and the socket would stay open until the connection callback unwound.
        closeQuietly(is);
        closeQuietly(os);
    }

    /// The per-connection callback the socket API instantiates. Public with a no-argument
    /// constructor because [Socket#listenLoopback] creates it reflectively.
    public static final class Connection extends SocketConnection {
        /// The last failure reported, so a listener that cannot bind at all says so once
        /// instead of once per retry. Only ever touched from the listener thread.
        private static String lastReportedError;

        @Override
        public void connectionError(int errorCode, String message) {
            // Without this the failure is silent: startSocketServer returns, the server
            // reports itself running, and nothing can ever attach - a port already in use
            // being the ordinary way that happens. Logged rather than thrown because this
            // arrives on the listener thread, long after the caller has gone.
            String description = "MCP socket listener failed: " + message + " (" + errorCode + ")";
            if (description.equals(lastReportedError)) {
                return;   // the same failure retrying; saying so once is enough
            }
            lastReportedError = description;
            try {
                Log.p(description, Log.ERROR);
            } catch (Throwable loggingFailed) {
                // The log routes through the platform implementation, which may not be
                // registered yet when a server is started early in initialization.
                System.err.println("[cn1.mcp] " + description);
            }
        }

        @Override
        public void connectionEstablished(InputStream is, OutputStream os) {
            MCPLoopbackSocketTransport transport;
            synchronized (MCPLoopbackSocketTransport.class) {
                transport = active;
            }
            if (transport == null) {
                return;
            }
            transport.attach(is, os);
            // Hold this callback thread for the life of the session: the socket API closes
            // the streams as soon as it returns, and the server reads them from its own
            // thread.
            synchronized (transport.lock) {
                while (transport.in == is && !transport.closed) {
                    try {
                        transport.lock.wait();
                    } catch (InterruptedException ex) {
                        break;
                    }
                }
            }
        }
    }
}
