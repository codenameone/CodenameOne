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
package com.codename1.backend;

import java.io.IOException;
import java.util.Map;

/**
 * One websocket connection: the decoder that turns bytes into messages, and the
 * handle an application sends through.
 *
 * ## Why the decoder is a state machine rather than a loop over whole frames
 *
 * A frame arrives in as many reads as the network chooses. The header may be split
 * across two of them, the mask key across four, and a 10MB payload across
 * thousands -- so every piece of state that spans a read boundary lives in a field
 * here: how much of the current payload is still owed, how far into the mask key
 * the next byte lands, whether a message is open and which opcode started it, and
 * how far a UTF-8 character got. {@link #pump} consumes whatever has arrived and
 * stops cleanly when it wants more, which is what lets the same decoder be driven
 * by a blocking loop on a virtual thread and by one reactor turn on a pool worker.
 *
 * ## Why this buffer is its own
 *
 * The HTTP read path borrows a per-host-thread array and parses in place. That is
 * free for a request, which is read, served and written without stopping -- and
 * exactly wrong for a websocket, which stops between every message by design. A
 * borrowed buffer held across a park has to be copied at every park, so this class
 * owns its buffer from the moment of upgrade and never touches `Conn.buffer`
 * again. See {@link HttpServer#tryUpgrade}.
 *
 * ## Why sending takes a lock and checks a flag three times
 *
 * Broadcast is the normal reason to run a websocket server, so a send can come
 * from any thread. Two writes to one descriptor would interleave frames, hence
 * `writeLock`. Worse, the descriptor can be closed under a sender and its NUMBER
 * reused immediately -- so `dead` is set before the close and checked on the way
 * in, after registering as a writer, and again under the lock. A handle whose
 * connection has gone is inert rather than dangerous.
 */
public final class WebSocketSession {
    /** What a close reports when the peer never sent one. */
    static final int NO_CLOSE_CODE = WebSocketFrames.CLOSE_ABNORMAL;

    private final HttpServer server;
    private final int fd;
    private final long tlsSession;
    private final WebSocket endpoint;
    private final String path;
    private final String query;
    private final Map handshakeHeaders;
    private final String subprotocol;
    private final long id;

    /** Serialises writers. Not the connection's read side, which is single threaded. */
    private final Object writeLock = new Object();
    /**
     * Counts threads between the entry check and the end of their write, so
     * teardown can wait for them instead of closing the descriptor underneath one.
     */
    private final java.util.concurrent.atomic.AtomicInteger writers =
            new java.util.concurrent.atomic.AtomicInteger();
    /** Sticky, and set BEFORE the descriptor is closed. */
    private volatile boolean dead;
    private boolean closeSent;
    /**
     * Raised the moment a Close frame is sent, which is earlier than `dead`.
     *
     * RFC 6455 5.5.1: after sending a Close the endpoint must not send any more
     * DATA frames. `dead` is not that point -- it is set at teardown, and between
     * the two an echo-style callback answering a message the peer had already
     * queued would put a data frame AFTER the Close, which a conforming peer reads
     * as a protocol error on an otherwise orderly shutdown.
     */
    private volatile boolean closing;

    private Object attachment;

    // ---- read side, touched only by the connection's own thread ----

    private byte[] in;
    private int inStart;
    private int inEnd;

    /** Set once the current frame's header has been consumed. */
    private boolean inFrame;
    private long frameRemaining;
    private int frameOpcode;
    private boolean frameFin;
    private final byte[] maskKey = new byte[4];
    private int maskPhase;

    /** The opcode that opened the message in progress, or -1 between messages. */
    private int messageOpcode = -1;
    private byte[] message = new byte[0];
    private int messageLength;
    /** How much of the process-wide reassembly budget this session is holding. */
    private int reserved;
    private final Utf8Stream text = new Utf8Stream();

    /** Control payloads are at most 125 bytes, so one small buffer always fits. */
    private final byte[] control = new byte[125];
    private int controlLength;

    private final byte[] header = new byte[WebSocketFrames.MAX_SERVER_HEADER];
    private byte[] scratch = new byte[0];

    private boolean finished;
    private int closeCode = NO_CLOSE_CODE;
    private String closeReason = "";

    WebSocketSession(HttpServer server, int fd, long tlsSession, WebSocket endpoint,
                     String path, String query, Map handshakeHeaders, String subprotocol,
                     long id, byte[] pending, int pendingLength) {
        this.server = server;
        this.fd = fd;
        this.tlsSession = tlsSession;
        this.endpoint = endpoint;
        this.path = path;
        this.query = query;
        this.handshakeHeaders = handshakeHeaders;
        this.subprotocol = subprotocol;
        this.id = id;
        // Whatever the client pipelined behind its handshake. Dropping these would
        // hang any client that puts its first frame in the same segment as the GET,
        // which is what a browser does.
        this.in = pendingLength > 0 ? pending : new byte[0];
        this.inEnd = pendingLength;
    }

    // ---------------------------------------------------------------- public API

    /** A number unique within this process, for logging and for keying a registry. */
    public long getId() {
        return id;
    }

    /** The path the client upgraded on, with no query string. */
    public String getPath() {
        return path;
    }

    /** The query string, or null when there was none. */
    public String getQuery() {
        return query;
    }

    /**
     * A header from the handshake request, case-insensitively.
     *
     * Snapshotted at upgrade, because the Request it came from stops being valid
     * the moment the handshake finishes.
     */
    public String getHandshakeHeader(String name) {
        if(handshakeHeaders == null || name == null) {
            return null;
        }
        Object value = handshakeHeaders.get(asciiLower(name));
        return value == null ? null : String.valueOf(value);
    }

    /**
     * An ASCII fold, by hand.
     *
     * Not toLowerCase(): it is locale sensitive, this runtime has no
     * java.util.Locale to ask for the root locale, and on a device set to Turkish
     * the `I` of `Sec-WebSocket-Key` folds to a dotless i -- so the lookup misses
     * and the header reads as absent. A header name is ASCII by specification, so
     * folding it by hand is both correct and cheaper. Copied rather than shared,
     * the way the other five copies of these six lines in this tree are.
     */
    private static String asciiLower(String name) {
        int length = name.length();
        StringBuilder out = new StringBuilder(length);
        for(int iter = 0 ; iter < length ; iter++) {
            char c = name.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char)(c + 32) : c);
        }
        return out.toString();
    }

    /** The negotiated subprotocol, or null when none was. */
    public String getSubprotocol() {
        return subprotocol;
    }

    /** Whether this connection is still usable. */
    public boolean isOpen() {
        return !dead;
    }

    public Object getAttachment() {
        return attachment;
    }

    /** Anything the endpoint wants to keep per connection. */
    public void setAttachment(Object value) {
        this.attachment = value;
    }

    public void sendText(String value) throws IOException {
        byte[] bytes = Utf8.encode(value);
        send(WebSocketFrames.OP_TEXT, bytes, 0, bytes.length);
    }

    public void sendBinary(byte[] value, int offset, int length) throws IOException {
        send(WebSocketFrames.OP_BINARY, value, offset, length);
    }

    public void sendBinary(byte[] value) throws IOException {
        send(WebSocketFrames.OP_BINARY, value, 0, value == null ? 0 : value.length);
    }

    public void sendPing(byte[] payload, int offset, int length) throws IOException {
        requireControlSize(length);
        send(WebSocketFrames.OP_PING, payload, offset, length);
    }

    public void sendPong(byte[] payload, int offset, int length) throws IOException {
        requireControlSize(length);
        send(WebSocketFrames.OP_PONG, payload, offset, length);
    }

    /** A normal close, code 1000, no reason. */
    public void close() {
        close(WebSocketFrames.CLOSE_NORMAL, "");
    }

    /**
     * Starts the closing handshake.
     *
     * Sends a Close frame and then waits for the peer's, which is what lets the
     * last message already in flight arrive. A peer that never answers is shed by
     * the connection's read timeout rather than held forever.
     */
    public void close(int code, String reason) {
        if(!WebSocketFrames.isValidCloseCode(code)) {
            // 1005, 1006 and 1015 are what a LOCAL implementation reports and no
            // endpoint may put on the wire; 1004 and anything outside the ranges
            // has no agreed meaning. Emitting one turns a requested orderly close
            // into a protocol error at the peer, so the request is corrected
            // rather than honoured literally.
            System.out.println("[http] websocket close code " + code
                    + " cannot be sent on the wire; closing with "
                    + WebSocketFrames.CLOSE_NORMAL + " instead");
            code = WebSocketFrames.CLOSE_NORMAL;
        }
        try {
            sendClose(code, reason == null ? "" : reason);
        } catch (IOException err) {
            // The peer is already gone. Nothing left to tell it.
            abort();
        }
    }

    /** Drops the connection with no closing handshake. For a peer already lost. */
    public void abort() {
        finished = true;
        server.dropWebSocket(fd);
    }

    // ------------------------------------------------------------- the send path

    private static void requireControlSize(int length) throws IOException {
        if(length > 125) {
            // RFC 6455 5.5: a control frame carries at most 125 bytes, because a
            // receiver must be able to handle one without buffering -- which is the
            // whole point of a PING arriving mid-message.
            throw new IOException("a control frame payload is at most 125 bytes, not " + length);
        }
    }

    private void send(int opcode, byte[] payload, int offset, int length) throws IOException {
        if(payload == null) {
            payload = HttpServer.EMPTY_BODY;
            offset = 0;
            length = 0;
        }
        if(dead) {
            throw new IOException("websocket " + id + " is closed");
        }
        if(closing && opcode != WebSocketFrames.OP_CLOSE) {
            // The Close is already on the wire; anything after it is a protocol
            // error for the peer to report.
            throw new IOException("websocket " + id + " is closing; no more data frames");
        }
        writers.incrementAndGet();
        try {
            // Re-read after registering. retire() sets `dead` and THEN reads
            // `writers`, so a writer that registered before reading the flag has to
            // look again or it slips between the two.
            if(dead) {
                throw new IOException("websocket " + id + " is closed");
            }
            synchronized(writeLock) {
                if(dead) {
                    throw new IOException("websocket " + id + " is closed");
                }
                // AGAIN, under the lock. The check before this one is outside it,
                // so a sender can pass it, pause, and take the lock after another
                // thread has written the Close -- and then put a data frame after
                // it, which is the exact thing the flag exists to prevent.
                if(closing && opcode != WebSocketFrames.OP_CLOSE) {
                    throw new IOException("websocket " + id + " is closing; no more data frames");
                }
                int headerLength = WebSocketFrames.writeHeader(header, 0, opcode, true, false,
                        length);
                // One write where it fits. On a fresh connection two writes are two
                // segments and the client waits a round trip for the second, which
                // for a small text frame is the whole message. The same reasoning
                // as writeHeadAndBody's combined write.
                if(headerLength + length <= HttpServer.COMBINED_WRITE_LIMIT) {
                    if(scratch.length < headerLength + length) {
                        scratch = new byte[headerLength + length];
                    }
                    System.arraycopy(header, 0, scratch, 0, headerLength);
                    System.arraycopy(payload, offset, scratch, headerLength, length);
                    HttpServer.writeTo(fd, tlsSession, scratch, 0, headerLength + length);
                } else {
                    // Both writes inside the lock: a second sender between them
                    // would put its header after this header and before this
                    // payload, and the peer would read the two as one frame.
                    HttpServer.writeTo(fd, tlsSession, header, 0, headerLength);
                    HttpServer.writeTo(fd, tlsSession, payload, offset, length);
                }
            }
        } catch (IOException err) {
            // THE STREAM IS NOW UNTRUSTWORTHY. By the time a write fails the
            // header, or part of the payload, may already be on the wire -- so the
            // peer is waiting for bytes that will never come, and the next send
            // would be read as the remainder of that frame rather than as a new
            // one. A send inside a callback gets endpointFailed to sort this out;
            // one from a broadcast thread has no such wrapper, and used to just
            // rethrow and leave the session open.
            dead = true;
            throw err;
        } finally {
            writers.decrementAndGet();
        }
    }

    private void sendClose(int code, String reason) throws IOException {
        synchronized(writeLock) {
            if(closeSent) {
                return;                      // one Close per connection, ever
            }
            closeSent = true;
            closing = true;
        }
        byte[] reasonBytes = Utf8.encode(reason);
        // RFC 6455 5.5.1: code and reason together are a control payload, so 123
        // bytes of reason at most. Truncating the reason is better than failing to
        // close, but it has to be truncated on a CHARACTER boundary or the close
        // frame carries invalid UTF-8 and the peer answers 1007 to our 1000.
        int reasonLength = reasonBytes.length > 123 ? Utf8.truncateAt(reasonBytes, 123)
                                                    : reasonBytes.length;
        byte[] payload = new byte[2 + reasonLength];
        payload[0] = (byte)((code >> 8) & 0xff);
        payload[1] = (byte)(code & 0xff);
        System.arraycopy(reasonBytes, 0, payload, 2, reasonLength);
        send(WebSocketFrames.OP_CLOSE, payload, 0, payload.length);
    }

    // ------------------------------------------------------------- the read side

    /** True once this session is finished and the descriptor should be dropped. */
    boolean isFinished() {
        return finished;
    }

    int getCloseCode() {
        return closeCode;
    }

    String getCloseReason() {
        return closeReason;
    }

    int getFd() {
        return fd;
    }

    WebSocket getEndpoint() {
        return endpoint;
    }

    /**
     * Marks this session unusable and waits, briefly, for any thread inside a
     * write to leave.
     *
     * Answers false when one is still there, and the caller must then NOT close the
     * descriptor: the number is reused immediately, so closing it under a sender
     * writes into whatever connection was handed that number next. A deferred
     * close leaks one descriptor for a bounded time; the alternative corrupts an
     * unrelated connection.
     */
    boolean retire() {
        dead = true;
        // Whatever this session was still holding. A reservation that outlives its
        // session shrinks the budget for every later one, permanently.
        releaseReservation();
        // Make a parked writer fail NOW rather than after SO_SNDTIMEO. A sender
        // blocked on a slow peer is waiting for POLLOUT bounded by the socket's
        // send timeout, and on a host thread that is the whole poller stalled.
        ServerSocket.shutdown(fd);
        long deadline = System.currentTimeMillis() + RETIRE_WAIT_MILLIS;
        while(writers.get() > 0 && System.currentTimeMillis() < deadline) {
            Thread.yield();
        }
        return writers.get() == 0;
    }

    /**
     * Whether every writer has left, asked WITHOUT waiting.
     *
     * {@link #retire} may wait, and that is right where it is called from -- the
     * connection's own thread, which has nothing else to do. It is wrong from the
     * deferred-close sweep, which runs on the REACTOR thread: that thread is also
     * the one that accepts, so a sweep that waits 100ms per deferred session stops
     * the server accepting for as long as it takes, the listen backlog fills, and
     * new connections are REFUSED while the process looks perfectly healthy.
     *
     * Measured: the Autobahn suite reached case 9.4.4 and then every remaining
     * case failed to connect, with the server still running and `kill -0` still
     * reporting it alive.
     */
    boolean isQuiescent() {
        if(!dead) {
            dead = true;
            ServerSocket.shutdown(fd);
        }
        return writers.get() == 0;
    }

    private static final long RETIRE_WAIT_MILLIS = 100;

    /** Reads more bytes. Answers false at end of stream. */
    boolean fill() throws IOException {
        compact();
        if(inEnd == in.length) {
            int grown = in.length == 0 ? READ_BUFFER_FLOOR : in.length * 2;
            byte[] bigger = new byte[grown];
            System.arraycopy(in, inStart, bigger, 0, inEnd - inStart);
            in = bigger;
            inEnd -= inStart;
            inStart = 0;
        }
        int read = HttpServer.readFrom(fd, tlsSession, in, inEnd, in.length - inEnd);
        if(read <= 0) {
            return false;
        }
        inEnd += read;
        return true;
    }

    private static final int READ_BUFFER_FLOOR = 2048;

    /** Whether anything is left to decode without reading again. */
    boolean hasBuffered() {
        return inEnd > inStart;
    }

    private void compact() {
        if(inStart == 0) {
            return;
        }
        int keep = inEnd - inStart;
        if(keep > 0) {
            System.arraycopy(in, inStart, in, 0, keep);
        }
        inStart = 0;
        inEnd = keep;
    }

    /**
     * Decodes and dispatches every complete frame that has arrived.
     *
     * Returns when it needs more bytes or when the session is finished. Every exit
     * leaves the fields describing exactly how far it got, so the next call picks
     * up mid-header, mid-mask or mid-payload without noticing.
     */
    void pump() throws IOException {
        while(!finished) {
            if(!inFrame) {
                int available = inEnd - inStart;
                int headerLength = WebSocketFrames.headerLength(in, inStart, available);
                if(headerLength == WebSocketFrames.NEED_MORE) {
                    return;
                }
                if(!readHeader(headerLength)) {
                    return;
                }
                inStart += headerLength;
                if(frameRemaining == 0) {
                    inFrame = false;
                    completeFrame();
                    continue;
                }
                inFrame = true;
            }
            int available = inEnd - inStart;
            if(available == 0) {
                return;
            }
            int take = available;
            if(take > frameRemaining) {
                take = (int)frameRemaining;
            }
            maskPhase = WebSocketFrames.unmask(in, inStart, take, maskKey, maskPhase);
            if(!acceptPayload(in, inStart, take)) {
                return;
            }
            inStart += take;
            frameRemaining -= take;
            if(frameRemaining == 0) {
                inFrame = false;
                completeFrame();
            }
        }
    }

    /**
     * Validates a frame header and takes its fields. False means the session has
     * been failed and pump must stop.
     */
    private boolean readHeader(int headerLength) throws IOException {
        int opcode = WebSocketFrames.opcode(in, inStart);
        boolean fin = WebSocketFrames.fin(in, inStart);
        int rsv = WebSocketFrames.rsv(in, inStart);
        boolean masked = WebSocketFrames.masked(in, inStart);
        long length = WebSocketFrames.payloadLength(in, inStart);

        if(rsv != 0) {
            // Nothing negotiated an extension, so a reserved bit means the peer is
            // speaking a protocol this server did not agree to.
            return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "reserved bit set with no extension");
        }
        if(!masked) {
            // RFC 6455 5.1. Unmasked client data is the cache-poisoning case
            // masking exists to prevent, so it is a close rather than a warning.
            return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "a client frame must be masked");
        }
        if(length < 0) {
            return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "the payload length has its high bit set");
        }
        if(!WebSocketFrames.lengthIsMinimal(in, inStart)) {
            return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "the payload length is not minimally encoded");
        }
        if(WebSocketFrames.isControl(opcode)) {
            if(opcode != WebSocketFrames.OP_CLOSE && opcode != WebSocketFrames.OP_PING
                    && opcode != WebSocketFrames.OP_PONG) {
                return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "reserved control opcode " + opcode);
            }
            if(!fin) {
                return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "a control frame is never fragmented");
            }
            if(length > 125) {
                return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "a control frame carries at most 125 bytes");
            }
            controlLength = 0;
        } else if(opcode == WebSocketFrames.OP_CONTINUATION) {
            if(messageOpcode < 0) {
                return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "a continuation with no message open");
            }
        } else if(opcode == WebSocketFrames.OP_TEXT || opcode == WebSocketFrames.OP_BINARY) {
            if(messageOpcode >= 0) {
                return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR,
                        "a new data frame while a message is still open");
            }
            messageOpcode = opcode;
            messageLength = 0;
            if(opcode == WebSocketFrames.OP_TEXT) {
                text.reset();
            }
        } else {
            return fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "reserved data opcode " + opcode);
        }

        if(!WebSocketFrames.isControl(opcode)) {
            long total = messageLength + length;
            if(total > server.getMaxWebSocketMessageBytes()) {
                return fail(WebSocketFrames.CLOSE_TOO_BIG, "message larger than the configured limit");
            }
        }

        int maskAt = WebSocketFrames.maskOffset(in, inStart);
        for(int iter = 0 ; iter < 4 ; iter++) {
            maskKey[iter] = in[maskAt + iter];
        }
        maskPhase = 0;
        frameOpcode = opcode;
        frameFin = fin;
        frameRemaining = length;
        return true;
    }

    /** Appends decoded payload. False means the session has been failed. */
    private boolean acceptPayload(byte[] data, int offset, int length) throws IOException {
        if(WebSocketFrames.isControl(frameOpcode)) {
            System.arraycopy(data, offset, control, controlLength, length);
            controlLength += length;
            return true;
        }
        if(messageOpcode == WebSocketFrames.OP_TEXT) {
            // Incrementally, so an invalid sequence fails HERE rather than after
            // however many more megabytes the peer chose to send.
            if(!text.accept(data, offset, length)) {
                return fail(WebSocketFrames.CLOSE_BAD_PAYLOAD, "the text message is not valid UTF-8");
            }
        }
        if(message.length < messageLength + length) {
            int grown = message.length == 0 ? READ_BUFFER_FLOOR : message.length * 2;
            while(grown < messageLength + length) {
                grown *= 2;
            }
            // RESERVED BEFORE IT IS ALLOCATED, and against a process-wide total --
            // the per-message cap bounds ONE session, and a peer may open as many
            // as MAX_CONNECTIONS allows. A hundred connections each parking a
            // fragmented message just under an 8MB limit is 800MB of live heap,
            // held for as long as the peer keeps sending control frames. The same
            // reasoning, and the same reserve-then-allocate order, as the HTTP
            // upload accounting in Conn.fillTo.
            if(!server.reserveWebSocketMemory(grown - reserved)) {
                return fail(WebSocketFrames.CLOSE_TOO_BIG,
                        "the server is already holding its reassembly budget");
            }
            reserved = grown;
            byte[] bigger = new byte[grown];
            System.arraycopy(message, 0, bigger, 0, messageLength);
            message = bigger;
        }
        System.arraycopy(data, offset, message, messageLength, length);
        messageLength += length;
        return true;
    }

    /** A frame has arrived whole. Dispatches it, or the message it completes. */
    private void completeFrame() throws IOException {
        if(WebSocketFrames.isControl(frameOpcode)) {
            dispatchControl();
            return;
        }
        if(!frameFin) {
            return;                          // more fragments to come
        }
        int opcode = messageOpcode;
        messageOpcode = -1;
        if(opcode == WebSocketFrames.OP_TEXT) {
            if(!text.isComplete()) {
                fail(WebSocketFrames.CLOSE_BAD_PAYLOAD, "the text message ends mid-character");
                return;
            }
            deliverText();
            return;
        }
        try {
            endpoint.onBinary(this, message, 0, messageLength);
        } catch (Exception err) {
            endpointFailed(err);
        }
        releaseMessageBuffer();
    }

    private void deliverText() {
        try {
            endpoint.onText(this, Utf8.decode(message, 0, messageLength));
        } catch (Exception err) {
            endpointFailed(err);
        }
        releaseMessageBuffer();
    }

    /**
     * Gives back a message buffer that grew past the working size.
     *
     * One large message on an otherwise idle connection would otherwise be held
     * for as long as the connection lives, times however many connections saw one.
     * The same reasoning as Conn.releaseIdleMemory.
     */
    private void releaseMessageBuffer() {
        if(message.length > HttpServer.MAX_IDLE_BUFFER_BYTES) {
            message = new byte[0];
            releaseReservation();
        }
        messageLength = 0;
    }

    /** Gives the process-wide reassembly budget back. Idempotent. */
    private void releaseReservation() {
        if(reserved > 0) {
            server.releaseWebSocketMemory(reserved);
            reserved = 0;
        }
    }

    private void dispatchControl() throws IOException {
        if(frameOpcode == WebSocketFrames.OP_PING) {
            // Answered before the endpoint is told, so an endpoint that does
            // nothing still keeps the connection alive.
            try {
                sendPong(control, 0, controlLength);
            } catch (IOException err) {
                abort();
                return;
            }
            try {
                endpoint.onPing(this, control, 0, controlLength);
            } catch (Exception err) {
                endpointFailed(err);
            }
            return;
        }
        if(frameOpcode == WebSocketFrames.OP_PONG) {
            try {
                endpoint.onPong(this, control, 0, controlLength);
            } catch (Exception err) {
                endpointFailed(err);
            }
            return;
        }
        // CLOSE
        int code = WebSocketFrames.CLOSE_NO_STATUS;
        String reason = "";
        if(controlLength == 1) {
            fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "a close payload is either empty or at least two bytes");
            return;
        }
        if(controlLength >= 2) {
            code = ((control[0] & 0xff) << 8) | (control[1] & 0xff);
            if(!WebSocketFrames.isValidCloseCode(code)) {
                fail(WebSocketFrames.CLOSE_PROTOCOL_ERROR, "close code " + code + " is not one a peer may send");
                return;
            }
            if(!Utf8.isValid(control, 2, controlLength - 2)) {
                fail(WebSocketFrames.CLOSE_BAD_PAYLOAD, "the close reason is not valid UTF-8");
                return;
            }
            reason = Utf8.decode(control, 2, controlLength - 2);
        }
        closeCode = code;
        closeReason = reason;
        // Echo it back, which is what completes the handshake, then stop.
        try {
            sendClose(code == WebSocketFrames.CLOSE_NO_STATUS ? WebSocketFrames.CLOSE_NORMAL : code, "");
        } catch (IOException err) {
            // The peer closed without waiting for the echo. Normal.
        }
        finished = true;
    }

    /**
     * Refuses the connection with a close code, and stops the decoder.
     *
     * Always answers false so a caller can `return fail(...)`.
     */
    private boolean fail(int code, String why) {
        closeCode = code;
        closeReason = why;
        // The public contract on WebSocket.onError names "a protocol violation by
        // the peer" first, and every one of those arrives here. Without this the
        // only failures an endpoint ever hears about are its own, so a malformed
        // client is invisible to whatever the application logs or counts.
        try {
            endpoint.onError(this, new IOException(why));
        } catch (RuntimeException ignored) {
            // An endpoint whose error handler also throws does not get to stop the
            // close frame going out.
        }
        try {
            sendClose(code, why);
        } catch (IOException err) {
            // Already gone; the close below is what matters.
        }
        finished = true;
        return false;
    }

    private void endpointFailed(Exception err) {
        try {
            endpoint.onError(this, err);
        } catch (RuntimeException ignored) {
            // An endpoint whose error handler also throws does not get to take the
            // server with it.
        }
        closeCode = WebSocketFrames.CLOSE_INTERNAL_ERROR;
        closeReason = "endpoint failed";
        try {
            sendClose(WebSocketFrames.CLOSE_INTERNAL_ERROR, "endpoint failed");
        } catch (IOException ignored) {
        }
        finished = true;
    }

    /**
     * Ends a session whose onOpen threw, with the same answer a failing onText
     * gets.
     */
    void failOnOpen() {
        closeCode = WebSocketFrames.CLOSE_INTERNAL_ERROR;
        closeReason = "the endpoint failed to open";
        try {
            sendClose(WebSocketFrames.CLOSE_INTERNAL_ERROR, "the endpoint failed to open");
        } catch (IOException ignored) {
        }
        finished = true;
    }

    /** Sends a 1001 and stops, for a server that is shutting down. */
    void closeForShutdown() {
        // RECORDED, not just sent. The read loop stops without decoding the peer's
        // echo, so onClose would otherwise report 1006 -- an abnormal close -- for
        // a shutdown the server performed deliberately, and every endpoint's
        // metrics would count a clean deployment as a network failure.
        closeCode = WebSocketFrames.CLOSE_GOING_AWAY;
        closeReason = "going away";
        try {
            sendClose(WebSocketFrames.CLOSE_GOING_AWAY, "going away");
        } catch (IOException ignored) {
        }
        finished = true;
    }
}
