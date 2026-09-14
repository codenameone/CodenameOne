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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One HTTP/2 connection, on nghttp2.
 *
 * The framing is not implemented here and should not be: HPACK alone is a static
 * table, a dynamic table with eviction and Huffman coding, and flow control,
 * stream state, CONTINUATION reassembly and GOAWAY are all their own problems.
 * nghttp2 owns those. This class owns the shape of the boundary.
 *
 * Java PULLS from the session rather than being called back into. nghttp2 is
 * callback-driven, but a C callback that reaches into the VM has to survive
 * dead-code elimination and must not run while the collector is moving; the
 * callbacks instead accumulate completed requests and this class takes them.
 */
public final class Http2 {
    /** The ALPN identifier. There is no upgrade handshake for h2 over TLS. */
    public static final String ALPN = "h2";

    private long session;

    private Http2(long session) {
        this.session = session;
    }

    /** A new server session, with the SETTINGS preface already queued. */
    public static Http2 create() throws IOException {
        long s = createImpl();
        if(s == 0) {
            throw new IOException("Could not create an HTTP/2 session");
        }
        return new Http2(s);
    }

    /** One request, once the client has finished sending it. */
    public static final class Stream {
        final int id;
        final String method;
        final String path;
        final String authority;
        final Map headers;
        final byte[] body;

        Stream(int id, String method, String path, String authority, Map headers, byte[] body) {
            this.id = id;
            this.method = method;
            this.path = path;
            this.authority = authority;
            this.headers = headers;
            this.body = body;
        }

        public int getId() {
            return id;
        }

        public String getMethod() {
            return method;
        }

        /** Path and query, from the :path pseudo-header. */
        public String getPath() {
            return path;
        }

        /** From :authority, which is what Host is in HTTP/1.1. */
        public String getAuthority() {
            return authority;
        }

        /** Lower-cased names, as HTTP/2 requires them on the wire. */
        public Map getHeaders() {
            return headers;
        }

        /** The body as it ARRIVED, so the caller can check it before decoding. */
        public byte[] getBody() {
            return body;
        }

        public String getBodyAsString() {
            if(body == null || body.length == 0) {
                return null;
            }
            try {
                return new String(body, "UTF-8");
            } catch (IOException err) {
                return new String(body);
            }
        }
    }

    /** Feeds received bytes to the session. */
    public void receive(byte[] buffer, int offset, int length) throws IOException {
        checkRange(buffer, offset, length);
        if(receiveImpl(session, buffer, offset, length) < 0) {
            throw new IOException("HTTP/2 framing error");
        }
    }

    /**
     * The next completed request, or null. A stream is complete only when
     * END_STREAM arrives -- on the HEADERS frame for a request with no body, on
     * the last DATA frame otherwise.
     */
    public Stream nextRequest() {
        int id = nextRequestImpl(session);
        if(id < 0) {
            return null;
        }
        Map headers = new LinkedHashMap();
        int count = headerCountImpl(session);
        for(int iter = 0 ; iter < count ; iter++) {
            String name = headerNameImpl(session, iter);
            if(name != null) {
                String value = headerValueImpl(session, iter);
                Object existing = headers.get(name);
                if(existing == null) {
                    headers.put(name, value);
                } else {
                    // A repeated field is COMBINED, not replaced. HTTP/2 lets a client
                    // split its cookies across several fields for better compression,
                    // and overwriting meant a session cookie sent in an earlier field
                    // vanished -- an authenticated request answered as anonymous.
                    // Cookie joins on "; " and everything else on ",", which is what
                    // RFC 9110 says a repeated field line means.
                    String separator = name.equalsIgnoreCase("cookie") ? "; " : ",";
                    headers.put(name, String.valueOf(existing) + separator + value);
                }
            }
        }
        return new Stream(id, methodImpl(session), pathImpl(session),
                authorityImpl(session), headers, bodyImpl(session));
    }

    /**
     * - `extraHeaders`: "name: value" strings. Connection-specific headers are
     *   dropped, because HTTP/2 forbids them, and names are lower-cased, because a
     *   capital letter is a protocol error the peer resets the stream over.
     */
    public boolean respond(int streamId, int status, String contentType, List extraHeaders, byte[] body)
            throws IOException {
        int rc = respondImpl(session, streamId, String.valueOf(status),
                headerBytes(contentType, extraHeaders), body);
        if(rc == OVER_BODY_BUDGET) {
            // Not a failure: the body was refused because submitting it would
            // cross the process-wide ceiling, and NOTHING was allocated or
            // charged. The caller answers 503 instead. Reported rather than
            // thrown because it is an ordinary load condition, and because the
            // reservation has to be the same step as the allocation -- a limit
            // the caller tests beforehand is two steps with a gap in the middle,
            // which is how two sessions both passed a 64MB check and then held
            // 80MB between them.
            return false;
        }
        if(rc != 0) {
            throw new IOException("Could not submit an HTTP/2 response on stream " + streamId);
        }
        return true;
    }

    /** respondImpl's answer when the body would cross the ceiling. */
    static final int OVER_BODY_BUDGET = -2;

    /**
     * The ceiling for outstanding response bodies across the process.
     *
     * Set once, and enforced natively where the memory is actually taken.
     */
    public static void setMaxBodyBytes(long limit) {
        setMaxBodyBytesImpl(limit);
    }

    /**
     * The ceiling for outstanding FILE-backed bodies across the process.
     *
     * Enforced natively for the same reason as the byte ceiling: the slot has to
     * be taken in the same step as the descriptor, or every worker finishing at
     * once passes the check before any of them counts.
     */
    public static void setMaxFileBodies(int limit) {
        setMaxFileBodiesImpl(limit);
    }

    /**
     * Responds with a range of an open file, without reading it into the heap.
     *
     * HTTP/2 cannot use sendfile -- the bytes have to become DATA frames -- but that
     * is not a reason to materialise the whole file first. Reading it in cost the
     * file's size in Java plus the same again in the native copy, so a large enough
     * public file turned one request into an OutOfMemoryError, which the handler's
     * `catch (Exception)` does not catch. The provider reads each frame straight out
     * of the descriptor instead, so the memory is one frame regardless of size, and
     * nghttp2's flow control decides the pace.
     *
     * The descriptor is owned by the session from here: it is closed when the stream
     * reaches EOF, when it is reset early, and when the session is torn down.
     */
    public boolean respondFile(int streamId, int status, String contentType, List extraHeaders,
            int fd, long offset, long length) throws IOException {
        int rc = respondFileImpl(session, streamId, String.valueOf(status),
                headerBytes(contentType, extraHeaders), fd, offset, length);
        if(rc == OVER_BODY_BUDGET) {
            // The descriptor ceiling was reached and NOTHING was taken -- the
            // caller still owns the fd and has to close it. Reported rather than
            // thrown because it is an ordinary load condition.
            return false;
        }
        if(rc != 0) {
            throw new IOException("Could not submit an HTTP/2 file response on stream "
                    + streamId);
        }
        return true;
    }

    /**
     * Runs the session's output side and returns the bytes to put on the wire.
     * Empty when there is nothing pending.
     */
    public byte[] drain() throws IOException {
        if(pumpImpl(session) != 0) {
            throw new IOException("HTTP/2 session failed");
        }
        return drainImpl(session);
    }

    /** False once the session is finished and the connection can be closed. */
    public boolean isAlive() {
        return wantsMoreImpl(session);
    }

    /**
     * Heap held by response bodies that have been submitted and not yet fully
     * written, which is NOT what drain() empties: that buffer is what nghttp2
     * has already serialised. nghttp2 pulls from a submitted body only as the
     * peer's flow-control window allows, so a client that stops sending
     * WINDOW_UPDATE leaves every body it asked for sitting here. A caller that
     * keeps submitting has to look at this figure rather than at what it just
     * handed over, because a flush that could write nothing frees nothing.
     */
    public long pendingBodyBytes() {
        return session == 0 ? 0 : pendingBodyBytesImpl(session);
    }

    /**
     * File-backed response bodies outstanding across the PROCESS, not this
     * session. Such a body holds a descriptor and no heap, so it is invisible to
     * pendingBodyBytes, and descriptors are a process resource: bounding them
     * per connection still multiplies by the connection count, and exhausting
     * them stops the process opening sockets or files at all.
     */
    public static int pendingBodyFiles() {
        return pendingBodyFilesImpl();
    }

    /**
     * Response-body heap outstanding across the PROCESS rather than this session.
     * The per-session figure says what one connection holds; a limit on that is a
     * limit per connection, and the connection ceiling is in the thousands, so it
     * bounds nothing about the machine.
     */
    public static long pendingBodyBytesAll() {
        return pendingBodyBytesAllImpl();
    }

    public void close() {
        if(session != 0) {
            long s = session;
            session = 0;
            destroyImpl(s);
        }
    }

    private static native long createImpl();

    /**
     * Refuses a slice that does not lie inside the array.
     *
     * The natives below index the array through the pointer they are handed and
     * ParparVM adds no bounds check of its own, so a bad offset is a native read
     * or write of whatever is next in the heap rather than an exception. The
     * JavaSE arm gets this free from its stream APIs, which is why such a bug is
     * invisible on the simulator and only appears once packaged. The subtraction
     * avoids the overflow that `offset + length` has.
     */
    private static void checkRange(byte[] buffer, int offset, int length) {
        if(buffer == null) {
            throw new NullPointerException("buffer");
        }
        if(offset < 0 || length < 0 || length > buffer.length - offset) {
            throw new IndexOutOfBoundsException("offset " + offset + ", length "
                    + length + ", buffer " + buffer.length);
        }
    }

    private static native int receiveImpl(long session, byte[] buffer, int offset, int length);
    private static native int pumpImpl(long session);
    private static native int pendingOutputImpl(long session);
    private static native byte[] drainImpl(long session);
    private static native int nextRequestImpl(long session);
    private static native String methodImpl(long session);
    private static native String pathImpl(long session);
    private static native String authorityImpl(long session);
    private static native int headerCountImpl(long session);
    private static native String headerNameImpl(long session, int index);
    private static native String headerValueImpl(long session, int index);
    private static native byte[] bodyImpl(long session);
    /**
     * The header block both response forms send, as "name: value" lines.
     *
     * <p>BYTES, one per character, and not a String for the native to encode. A
     * field value is octets: the server's own validation accepts anything from
     * 0x20 to 0xff except 0x7f -- obs-text included -- and says it tests "the
     * byte that will be emitted", which is exactly what the HTTP/1.1 writer does
     * when it narrows each char with a cast. Handing the native a String meant
     * stringToUTF8 encoded it instead, so a handler returning U+00E9 sent one
     * byte over HTTP/1.1 and two over h2: the same response, different octets,
     * decided by which protocol the client negotiated.
     *
     * <p>This is the exact inverse of newStringFromAsciiLen, which is how the
     * inbound natives turn a request's header bytes into chars -- so the two
     * directions agree again.
     */
    private static byte[] headerBytes(String contentType, List extraHeaders) {
        StringBuilder joined = new StringBuilder();
        joined.append("content-type: ").append(contentType == null
                ? "application/octet-stream" : contentType);
        if(extraHeaders != null) {
            for(int iter = 0 ; iter < extraHeaders.size() ; iter++) {
                joined.append('\n').append(String.valueOf(extraHeaders.get(iter)));
            }
        }
        return HeaderLines.narrowed(joined.toString());
    }

    private static native int respondFileImpl(long session, int streamId, String status,
            byte[] headerLines, int fd, long offset, long length);
    private static native void setMaxBodyBytesImpl(long limit);

    private static native void setMaxFileBodiesImpl(int limit);

    private static native int respondImpl(long session, int streamId, String status,
                                          byte[] headerLines, byte[] body);
    private static native boolean wantsMoreImpl(long session);
    private static native long pendingBodyBytesImpl(long session);
    private static native int pendingBodyFilesImpl();
    private static native long pendingBodyBytesAllImpl();
    private static native void destroyImpl(long session);
}
