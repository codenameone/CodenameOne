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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An HTTP/1.1 server built around a reactor and a bounded worker pool.
 *
 * The split is the whole design. A parked ParparVM thread costs about 243KB on
 * musl (see vm/benchmarks ThreadCost), so ten thousand connections cannot each
 * have one. Here an IDLE connection is a descriptor the reactor watches, and only
 * a connection with a request in flight occupies a worker. Workers are bounded, so
 * overload sheds as queueing rather than as memory exhaustion.
 *
 * Once a worker owns a connection its descriptor is switched to BLOCKING mode, so
 * request parsing is a straight read loop rather than a resumable state machine.
 * That spends a worker per in-flight request to avoid a large amount of
 * complexity, and in-flight requests are the thing there are few of.
 */
public final class HttpServer {
    /**
     * What a handler receives. Header names are matched case-insensitively.
     *
     * The headers are NOT copied out of the request. They stay as offsets into
     * the buffer the kernel filled, and {@link #getHeader} compares against those
     * bytes -- so a handler that reads two headers allocates nothing, where
     * building a map of Strings cost about 2.6KB per request and made char[] the
     * single largest allocation in the server. {@link #getHeaders} still returns
     * a Map, built on first call, for callers that want one.
     *
     * A Request is valid for the duration of {@link Handler#handle} and NOT
     * beyond it. Both the byte array it was parsed from and the slice table that
     * indexes it are reused -- the array by the reading thread, the table by the
     * connection -- so a Request held past the handler describes whatever arrived
     * next, not what it was built from.
     *
     * This corrects a claim that used to stand here, that the slices "stay valid
     * for as long as the Request is held". That was never true: the table is
     * `conn.slices`, reused by the very next request on the same connection. The
     * zero-copy read added a second way for it to be false, which is what prompted
     * reading the sentence carefully enough to notice it had always been wrong.
     *
     * The synchronous handler signature already makes the call the natural
     * lifetime, so this documents the contract rather than narrowing one -- but
     * anything that needs to outlive the handler must copy what it needs, and
     * {@link #getHeaders} or {@link #getHeader} give Strings that are safe to keep.
     */
    public static final class Request {
        // Not final because one Request is REUSED for every request on a
        // connection, which is the same trade the buffer and the slice table
        // already make and the reason a Request is documented as valid only for
        // the duration of Handler.handle. "Immutable to its handler" is the
        // property that matters and it still holds exactly: reset() runs while a
        // request is being PARSED, which is strictly before the handler is called
        // and strictly after the previous one returned.
        private String method;
        private String target;
        private String version;
        private String body;
        /** The connection this request arrived on; null for HTTP/2, see respond. */
        private Conn conn;
        /** The bytes the header block was read from. */
        private byte[] raw;
        /** nameStart, nameLength, valueStart, valueLength per header, in order. */
        private int[] slices;
        private int headerCount;
        private Map headers;

        Request(String method, String target, String version, byte[] raw, int[] slices,
                int headerCount, String body) {
            this.method = method;
            this.target = target;
            this.version = version;
            this.raw = raw;
            this.slices = slices;
            this.headerCount = headerCount;
            this.body = body;
        }

        /**
         * A Response for this request WITHOUT allocating one.
         *
         * Returns the connection's single Response, re-pointed to these values. It
         * is valid for the duration of {@link Handler#handle} and not beyond it --
         * the same contract this Request already carries, and for the same reason:
         * the next request on this connection reuses it.
         *
         * Why it exists: on a route that allocates nothing else, the Response was
         * the last per-request allocation, and allocation is what drives both the
         * collector's frequency and its footprint. Removing it measured a 15x
         * better p99 and an 8x smaller resident set at the same throughput.
         *
         * {@code new Response(...)} still works and still allocates; a handler that
         * needs its Response to outlive the call must use it.
         */
        public Response respond(int status, String contentType, byte[] body) {
            if(conn == null) {
                return new Response(status, contentType, body);   // HTTP/2 path
            }
            if(conn.pooledResponse == null) {
                conn.pooledResponse = new Response(status, contentType, body);
            } else {
                conn.pooledResponse.reset(status, contentType,
                        body == null ? EMPTY_BODY : body, -1, 0, 0, null);
            }
            return conn.pooledResponse;
        }

        /**
         * Re-points this Request at a freshly parsed request. Every field is
         * assigned, with no "unchanged" case: a field left behind describes the
         * PREVIOUS request on this connection, and headers is the one that would
         * hurt -- it caches a Map built on demand by getHeaders, so carrying it
         * over would answer one request's header lookups with another's. That is
         * a wrong answer rather than a crash, which is why it is assigned here
         * unconditionally instead of being cleared at some later point.
         */
        void reset(Conn conn, String method, String target, String version, byte[] raw,
                   int[] slices, int headerCount, String body) {
            this.conn = conn;
            this.method = method;
            this.target = target;
            this.version = version;
            this.raw = raw;
            this.slices = slices;
            this.headerCount = headerCount;
            this.body = body;
            this.headers = null;
        }

        /**
         * For HTTP/2, whose headers arrive already decoded from the HPACK state --
         * there is no request buffer to slice into, so the map IS the
         * representation and every lookup below falls back to it.
         */
        Request(String method, String target, String version, Map headers, String body) {
            this.method = method;
            this.target = target;
            this.version = version;
            this.raw = null;
            this.slices = null;
            this.headerCount = 0;
            this.headers = headers;
            this.body = body;
        }

        /** "HTTP/1.1" or "HTTP/1.0". The two differ on whether keep-alive is the default. */
        public String getVersion() {
            return version;
        }

        public String getMethod() {
            return method;
        }

        /** Path plus query string, exactly as it arrived. */
        public String getTarget() {
            return target;
        }

        /**
         * The headers as a Map, lower-cased names to values.
         *
         * Built on the first call and cached. Prefer {@link #getHeader}: this
         * allocates a String per name and per value, which is the cost the slice
         * representation exists to avoid.
         */
        public Map getHeaders() {
            if(headers == null) {
                Map out = new LinkedHashMap();
                for(int iter = 0 ; iter < headerCount ; iter++) {
                    int base = iter * 4;
                    out.put(lowerCaseString(raw, slices[base], slices[base + 1]),
                            asciiString(raw, slices[base + 2], slices[base + 3]));
                }
                headers = out;
            }
            return headers;
        }

        /** One header by name, matched case-insensitively. Allocates only the value. */
        public String getHeader(String name) {
            if(name == null) {
                return null;
            }
            if(raw == null) {
                Object v = headers.get(name.toLowerCase());
                return v == null ? null : String.valueOf(v);
            }
            int at = indexOfHeader(name);
            if(at < 0) {
                return null;
            }
            return asciiString(raw, slices[at + 2], slices[at + 3]);
        }

        /** The slice index of a header, or -1. No allocation on either path. */
        int indexOfHeader(String name) {
            // Callers guard on raw != null; headerCount is 0 for the map form, so
            // this returns -1 there rather than reading a null slices array.
            //
            // The name is folded ONCE, not once per header. sliceEqualsIgnoreCase
            // reads it with charAt and case-folds it on every comparison, so a
            // four-header request folded the same needle four times over -- and the
            // generated code shows why that is not free: each character costs a
            // cn1InlStrCharAt (which re-checks the string's coder) plus a foldAscii
            // call, against a plain array read on the other side.
            int needle = foldedSlot(name);
            if(needle < 0) {
                // Not ASCII-foldable, so the general path is the only correct one.
                for(int iter = 0 ; iter < headerCount ; iter++) {
                    int base = iter * 4;
                    if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], name)) {
                        return base;
                    }
                }
                return -1;
            }
            for(int iter = 0 ; iter < headerCount ; iter++) {
                int base = iter * 4;
                if(sliceEqualsFolded(raw, slices[base], slices[base + 1], needle)) {
                    return base;
                }
            }
            return -1;
        }

        /**
         * Whether a header's value contains a token, case-insensitively. Used for
         * "connection: keep-alive" and friends without materialising the value.
         */
        boolean headerContains(String name, String token) {
            if(raw == null) {
                Object v = headers == null ? null : headers.get(name.toLowerCase());
                return v != null
                        && String.valueOf(v).toLowerCase().indexOf(token.toLowerCase()) >= 0;
            }
            int at = indexOfHeader(name);
            return at >= 0
                    && sliceContainsIgnoreCase(raw, slices[at + 2], slices[at + 3], token);
        }

        /** How many headers arrived, so a duplicate can be detected. */
        int countHeader(String name) {
            int found = 0;
            for(int iter = 0 ; iter < headerCount ; iter++) {
                int base = iter * 4;
                if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], name)) {
                    found++;
                }
            }
            return found;
        }

        public String getBody() {
            return body;
        }
    }

    /** What a handler returns. */
    public static final class Response {
        // Not final because Request.respond hands back ONE Response per connection,
        // re-pointed per request. The same trade the Request beside it already
        // makes: valid for the duration of Handler.handle and not beyond it, which
        // is the whole window in which a handler can see it. A handler that would
        // rather own its Response still writes new Response(...) and pays for it.
        int status;
        String contentType;
        byte[] body;
        /** When >= 0 the body is this descriptor, and the server owns closing it. */
        int fileFd;
        long fileOffset;
        long fileLength;
        Map extraHeaders;
        /** Serialised into the connection buffer at write time; see jsonValue. */
        Object deferredJson;
        boolean hasDeferredJson;

        /**
         * Re-points this Response. Every field is assigned with no "unchanged"
         * case: a field left behind describes the PREVIOUS response on this
         * connection, and deferredJson is the one that would hurt -- it makes the
         * writer serialise an object the handler never returned.
         */
        void reset(int status, String contentType, byte[] body, int fileFd,
                   long fileOffset, long fileLength, Map extraHeaders) {
            this.status = status;
            this.contentType = contentType;
            this.body = body == null ? EMPTY_BODY : body;
            this.fileFd = fileFd;
            this.fileOffset = fileOffset;
            this.fileLength = fileLength;
            this.extraHeaders = extraHeaders;
            this.deferredJson = null;
            this.hasDeferredJson = false;
        }

        public Response(int status, String contentType, byte[] body) {
            this(status, contentType, body == null ? new byte[0] : body, -1, 0, 0, null);
        }

        Response(int status, String contentType, byte[] body,
                 int fileFd, long fileOffset, long fileLength, Map extraHeaders) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
            this.fileFd = fileFd;
            this.fileOffset = fileOffset;
            this.fileLength = fileLength;
            this.extraHeaders = extraHeaders;
        }

        /**
         * A response whose body is a file. The server sends it with sendfile where
         * the platform has it, so the bytes never enter user space, and CLOSES the
         * descriptor when it is done -- a handler that returned one must not.
         */
        public static Response file(int status, String contentType, int fd,
                                    long offset, long length, Map extraHeaders) {
            return new Response(status, contentType, null, fd, offset, length, extraHeaders);
        }

        public static Response text(int status, String body) {
            return new Response(status, "text/plain; charset=utf-8", bytes(body));
        }

        public static Response json(int status, String body) {
            return new Response(status, "application/json; charset=utf-8", bytes(body));
        }

        /**
         * A JSON response serialised straight into the connection's write buffer.
         *
         * Prefer this to json(status, Json.write(value)): that builds a
         * StringBuilder, grows its char[], copies it into a String and encodes
         * that to bytes, for a document about to go to a socket and be discarded.
         * Deliberately a DIFFERENT NAME rather than an overload taking Object --
         * an overload would bind a String-typed variable to this method and
         * double-encode it, which is exactly the kind of thing that is found in
         * production rather than in review.
         */
        public static Response jsonValue(int status, Object value) {
            Response r = new Response(status, "application/json; charset=utf-8",
                    EMPTY_BODY, -1, 0, 0, null);
            r.deferredJson = value;
            r.hasDeferredJson = true;
            return r;
        }

        /** A response with headers but no body, for 304 and for HEAD. */
        public static Response empty(int status, String contentType, Map extraHeaders) {
            return new Response(status, contentType, new byte[0], -1, 0, 0, extraHeaders);
        }

        public int getStatus() {
            return status;
        }

        private static byte[] bytes(String s) {
            try {
                return s == null ? new byte[0] : s.getBytes("UTF-8");
            } catch (IOException err) {
                return new byte[0];
            }
        }
    }

    public interface Handler {
        Response handle(Request request) throws Exception;
    }

    private static final int MAX_HEADER_BYTES = 64 * 1024;
    private static final int MAX_BODY_BYTES = 8 * 1024 * 1024;
    private static final int READY_CAPACITY = 256;

    /**
     * Bodies at or below this are sent together with the headers in one write.
     * Sized so an ordinary JSON response fits and a page-sized payload does not;
     * beyond it the copy costs more than the syscall it saves.
     */
    private static final int COMBINED_WRITE_LIMIT = 8192;

    private static final byte[] EMPTY_BODY = new byte[0];

    /** "Sat, 29 Aug 2026 07:11:02 GMT" -- RFC 9110 fixes the width. */
    private static final int HTTP_DATE_LENGTH = 29;

    /**
     * How long a worker waits, still holding the connection, for the NEXT request
     * before handing it back to the poller.
     *
     * This is the difference between a poller that is re-armed per request and one
     * that is not. Handing the descriptor back costs an epoll_ctl pair, two
     * blocking-mode flips and a cross-thread handoff -- measured against Go, whose
     * netpoller registers a connection once: 2 epoll_ctl, 4 fcntl and 3.9 futex
     * per request against its 0.00, 0.00 and 0.05, with the futex traffic alone
     * 80% of our syscall time.
     *
     * A client that is going to send another request usually sends it within
     * microseconds, so a few milliseconds captures nearly all of them. Set to 0 to
     * hand back immediately, which is the behaviour this replaces.
     *
     * The timeout alone does NOT bound how long a worker keeps a connection: a
     * client that keeps sending is readable every time, so the worker goes round
     * again and holds it indefinitely. Under continuous load that made the pool
     * the limit on concurrent clients -- exactly what the reactor exists to
     * prevent -- and it was invisible in a throughput number, because the
     * connections that DID hold a worker were served at full speed while the rest
     * starved. A fresh connection got no response in five seconds while the
     * benchmark reported 234k requests a second. What bounds it is
     * {@link #pendingWork} below, plus the burst cap.
     */
    /**
     * Which thread takes a ready descriptor from the poller.
     *
     *   0  A dedicated reactor thread calls the poller and DISPATCHES: it
     *      deregisters the descriptor, allocates a task, queues it and wakes a
     *      worker. That wake is a futex and a context switch, and the descriptor
     *      has to be registered again afterwards, so an ordinary request costs
     *      two epoll_ctl and a cross-thread handoff on top of its own read and
     *      write.
     *   1  The WORKERS call the poller themselves. A worker with nothing to do
     *      waits on the same set and serves the first descriptor it is given, on
     *      the thread that polled -- no queue, no wake, no task object. The
     *      descriptor is armed {@link Reactor#ONESHOT} so the kernel hands it to
     *      exactly one waiter, and re-arming afterwards is a single epoll_ctl.
     *
     * Mode 1 is what Go's scheduler does. `netpoll()` is called from
     * `findRunnable()` on whatever thread has run out of work, and the result is
     * `gp := list.pop(); injectglist(&list); return gp` -- it runs the first
     * ready goroutine ON THE POLLING THREAD and only queues the remainder. A
     * syscall census of the two servers under the same load put us at 6.9x Go's
     * futex rate and 41x its epoll_ctl rate while the read and write counts
     * matched to within 5%, which says the gap is coordination rather than work.
     */
    /**
     *   2  ONE worker polls at a time. It serves the first ready descriptor on
     *      its own thread and queues the remainder for the others, so a lone
     *      event -- the common case -- costs no handoff at all, while a burst
     *      pays one wake per SURPLUS descriptor rather than one per request.
     *
     * Mode 2 is what Go actually does, and mode 1 is what it looks like from a
     * distance. The difference is a guard in `findRunnable` that mode 1 has no
     * equivalent of: "we can safely skip it if there are no waiters or A THREAD
     * IS BLOCKED IN NETPOLL ALREADY". Go never has two threads in the poller.
     * Mode 1 puts every worker in `epoll_wait` on one set, so a single arriving
     * event wakes all of them; ONESHOT still guarantees only one RECEIVES the
     * descriptor, but the other wakeups happen anyway and cost more the more
     * workers there are. Measured, that is exactly what mode 1 does: +40% on two
     * workers, +24% on four, and -25% on eight.
     */
    /**
     *   3  A VIRTUAL THREAD per connection. Host threads poll and resume; a
     *      connection's virtual thread runs until it finishes or asks for bytes
     *      that have not arrived, and parks inside the ordinary blocking read.
     *      There is no handoff at all, and no thread per connection either.
     *
     * The numbers that motivate mode 3 rather than more tuning of 0 to 2: moving
     * a request between OS threads measured 21181ns on the machine this was built
     * on, switching a virtual thread measured 2.6ns, and the paired experiment
     * over modes 0 to 2 showed the handoff is worth about a third of throughput
     * at four workers while REMOVING it costs about a third at sixteen -- because
     * a pool large enough to hide the handoff is a pool large enough to lose to
     * the OS scheduler. A virtual thread is how a context per connection stops
     * implying an OS thread per connection, which is the assumption that made
     * those two facts irreconcilable.
     */
    /**
     * 0 the reactor thread dispatches to a pool; 3 a virtual thread per connection.
     *
     * Modes 1 and 2 were two ways of letting the WORKERS poll, and the paired
     * experiment killed both: removing the dispatch is worth about a third of
     * throughput at four workers and costs about a third at sixteen, because a
     * pool big enough to hide the handoff is a pool big enough to lose to the OS
     * scheduler. Their numbers are in the benchmarks README; the code is gone
     * rather than left to rot, since a mode nobody selects is a mode nobody
     * tests.
     */
    /*
     * The DEFAULT is virtual threads wherever the build has them, and the pool
     * only where it does not.
     *
     * Virtual threads are not a tuning option here, they are the mode that
     * matches Go on the TAIL: measured p99 1.58 ms against the pool's 59.70 ms on
     * the same host, and a corrected syscall census puts their futex traffic at
     * 0.000 per request against the pool's 0.265. Leaving the pool as the default
     * shipped the worse tail to everyone who did not know to set an environment
     * variable, and left the better path exercised only by benchmarks.
     *
     * It is a TRADE, not a free win, and the cost is throughput. Four arms
     * interleaved on a quiet host, /plaintext at 64 connections, medians of four
     * steady-state reps (spread 0.4-4.9%):
     *
     *     go                243,161 req/s
     *     pool, 64 workers  239,155        0.972 of go
     *     virtual threads   207,808        0.854 of go
     *
     * So the pool is within 3% of Go on throughput and virtual threads are 13%
     * behind it. That gap is NOT syscalls -- the census has virtual threads at
     * 3.02 per request against the pool's 3.29 -- so it is user-space switch and
     * scheduling cost, which is where to look next if this default is to stop
     * costing anything.
     *
     * Conditioned on VirtualThread.supported() rather than assumed: the context
     * switch is compiled in only on non-Windows aarch64/x86_64, and elsewhere
     * create() can only return 0, which would drop every connection instead of
     * falling back. Setting CN1_HTTP_POLL_MODE explicitly still overrides this
     * in either direction.
     */
    private static final int POLL_MODE =
            envInt("CN1_HTTP_POLL_MODE", VirtualThread.supported() ? 3 : 0);
    private static final boolean VIRTUAL_THREADS = POLL_MODE == 3;

    /**
     * C stack per connection. Java locals and the operand stack are NOT here --
     * they live in the virtual thread's own VM state, mapped lazily -- so this
     * buys call depth rather than data. 64KB holds a few hundred nested Java
     * frames, well past what an HTTP handler needs, and is mapped lazily too.
     */
    private static final int VT_STACK_BYTES = envInt("CN1_HTTP_VT_STACK", 64 * 1024);

    private static final int KEEPALIVE_LINGER_MILLIS =
            envInt("CN1_HTTP_KEEPALIVE_LINGER_MS", 5);

    /**
     * How long a worker waits for a request, and for the client to take the
     * response. A connection that opens and says nothing would otherwise hold a
     * worker forever, and the pool is bounded on purpose -- open as many silent
     * connections as there are workers and the server stops answering anyone.
     */
    private static final int SOCKET_TIMEOUT_MILLIS = envInt("CN1_HTTP_TIMEOUT_MS", 15000);

    /**
     * Ceiling on open connections. Past it a connection is accepted and closed
     * immediately rather than left in the backlog: refusing is a fast, legible
     * answer, while a full backlog looks to a client like a server that hangs. Set
     * to 0 for no ceiling.
     */
    private static final int MAX_CONNECTIONS = envInt("CN1_HTTP_MAX_CONNECTIONS", 4096);

    private static int envInt(String name, int fallback) {
        String v = System.getenv(name);
        if(v == null || v.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException err) {
            return fallback;
        }
    }

    /**
     * Set CN1_HTTP_TRACE=1 to print what the reactor sees. A reactor that is not
     * reporting readiness looks exactly like a handler that is not responding, and
     * the only way to tell them apart from outside is to ask which one is silent.
     */
    private static final boolean TRACE = "1".equals(System.getenv("CN1_HTTP_TRACE"));

    private static void trace(String message) {
        if(TRACE) {
            System.err.println("[http] " + message);
        }
    }

    private final ServerSocket listener;
    private final Reactor reactor;
    private final ExecutorService workers;
    private final Handler handler;
    private final Tls tls;
    /**
     * fd to SSL session. Only written when a connection is established or closed,
     * never per request. A TLS connection genuinely costs an object; the plain
     * server allocates nothing per idle connection and this map is why that
     * property does not carry over to TLS.
     */
    private final Map sessions = java.util.Collections.synchronizedMap(new java.util.HashMap());
    /** fd to HTTP/2 session, for connections where ALPN settled on h2. */
    private final Map http2Sessions = java.util.Collections.synchronizedMap(new java.util.HashMap());
    private volatile boolean running = true;
    private Thread loop;
    /** Released only when stop() has finished draining. See awaitTermination. */
    private final Object stopped = new Object();
    private boolean fullyStopped;
    private final java.util.concurrent.atomic.AtomicInteger openConnections =
            new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger activeRequests =
            new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicLong requestsServed =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong connectionsAccepted =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong connectionsRefused =
            new java.util.concurrent.atomic.AtomicLong();
    private final long startedAt = System.currentTimeMillis();


    /**
     * How many requests one worker may serve on one connection before handing it
     * back even when nothing else is waiting.
     *
     * A backstop under the pendingWork check rather than the main mechanism: it
     * bounds the damage if that check is ever wrong. In virtual-thread mode the
     * cap still applies but its ACTION is to step aside rather than to close --
     * see where it is used.
     */
    private static final int KEEPALIVE_BURST_LIMIT =
            envInt("CN1_HTTP_KEEPALIVE_BURST", 256);

    /** How many requests may be in flight at once; the pool size. */
    private final int workerCount;

    private HttpServer(ServerSocket listener, Reactor reactor, ExecutorService workers,
                       int workerCount, Handler handler, Tls tls) {
        this.listener = listener;
        this.reactor = reactor;
        this.workers = workers;
        this.workerCount = workerCount;
        this.handler = handler;
        this.tls = tls;
    }

    /**
     * Arming used for connection descriptors.
     *
     * Plain level-triggered READ, with no ONESHOT and so no re-arm, and affinity
     * is what makes that safe. A descriptor lives in exactly ONE host's epoll
     * set, and that host is not polling while it is inside advance() running the
     * virtual thread, so no second thread can ever be handed a descriptor whose
     * virtual thread is already running. ONESHOT was guarding against a hazard
     * that only exists when several threads share a poller.
     *
     * What it costs to keep it is an epoll_ctl on every park, which is the exact
     * syscall Go does not pay: it registers each descriptor once, edge-triggered,
     * and never touches epoll again for the life of the connection.
     */
    private static final int CONN_EVENTS =
            VIRTUAL_THREADS ? (Reactor.READ | Reactor.ONESHOT) : Reactor.READ;

    /**
     * Ready descriptors handed to the pool and not yet picked up.
     *
     * The keep-alive linger is bounded by this rather than by its timeout: a
     * client that keeps sending is readable every time, so a worker would hold
     * one connection for ever and the pool would become the limit on concurrent
     * clients. A fresh connection got no response in five seconds while the
     * benchmark reported 234k requests a second.
     */
    private final java.util.concurrent.atomic.AtomicInteger pendingWork =
            new java.util.concurrent.atomic.AtomicInteger(0);

    /** Set only when a poller-per-worker mode is on; the threads that poll. */
    private Thread[] pollers;

    private final java.util.concurrent.atomic.AtomicInteger vtAccepts =
            new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger vtDispatched =
            new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger vtCreateFailures =
            new java.util.concurrent.atomic.AtomicInteger(0);

    public static HttpServer start(String host, int port, int backlog, int workerCount, Handler handler)
            throws IOException {
        return start(host, port, backlog, workerCount, handler, null);
    }

    /**
     * - `tls`: terminate TLS here, or null to serve plaintext (correct behind a
     *   load balancer that already terminated it)
     */
    public static HttpServer start(String host, int port, int backlog, int workerCount,
                                   Handler handler, Tls tls) throws IOException {
        ServerSocket listener = ServerSocket.bind(host, port, backlog);
        Reactor reactor;
        try {
            reactor = Reactor.create();
        } catch (IOException err) {
            listener.close();
            throw err;
        }
        ServerSocket.setBlocking(listener.getFd(), false);
        reactor.add(listener.getFd(), CONN_EVENTS);

        // No worker pool in virtual-thread mode. workers.execute() is reached only
        // from handOff(), which is reached only from pump(), which runs only in
        // the branch below this one -- so in this mode every pooled thread is
        // created, parked, and never given anything to do.
        //
        // They are not free. Each is a Java thread of control: the collector
        // conservatively scans its native stack and its 258KB object stack on
        // every cycle, and stop-the-world sets threadBlockedByGC on each and
        // spins `while(t->threadActive)` waiting for it. Measured on two pinned
        // cores with the host count held equal by the clamp above -- so the pool
        // size was the only variable -- WORKERS=64 segfaulted 2 runs in 6 and
        // WORKERS=4 survived 6 of 6. Throughput was unaffected when it did not
        // crash (265k either way), so this buys robustness rather than speed.
        final HttpServer server = new HttpServer(listener, reactor,
                VIRTUAL_THREADS ? null : Executors.newFixedThreadPool(workerCount),
                workerCount, handler, tls);
        if(VIRTUAL_THREADS) {
            ACTIVE_SERVER = server;
            // A poller PER HOST, because affinity is enforced by the poller: a
            // descriptor registered in one host's set can only ever be reported
            // to that host, so its virtual thread cannot run anywhere else. The
            // listener lives in host 0's set, so exactly one host accepts and
            // hands each new connection to its permanent owner.
            // HOSTS TRACK CORES, not the caller's expected concurrency.
            //
            // In this mode workerCount stops meaning "how many requests may be in
            // flight" -- the virtual threads supply that, one per connection --
            // and a host thread is only useful while there is a core free to run
            // it on. Past that they contend for the cores the server needs:
            // measured on two pinned cores, 16 hosts served 117 requests where 2
            // served 257297. Unpinned, with cores to spare, 2 through 32 all
            // behave, so the ceiling has to come from the machine at runtime.
            //
            // Clamped rather than obeyed, because a caller asking for 64 workers
            // is asking for concurrency, and in this mode that request is
            // answered by the virtual threads instead.
            int hostCount = workerCount;
            int cores = ServerSocket.availableProcessors();
            if(hostCount > cores) {
                hostCount = cores;
            }
            if(hostCount < 1) {
                hostCount = 1;
            }
            server.vtHosts = new VtHost[hostCount];
            for(int iter = 0 ; iter < hostCount ; iter++) {
                server.vtHosts[iter] = new VtHost(iter == 0 ? reactor : Reactor.create());
            }
            server.pollers = new Thread[hostCount];
            for(int iter = 0 ; iter < hostCount ; iter++) {
                final int index = iter;
                server.pollers[iter] = new Thread(new Runnable() {
                    public void run() {
                        server.runVirtualThreadHost(index);
                    }
                });
                server.pollers[iter].start();
            }
        } else {
            server.loop = new Thread(new Runnable() {
                public void run() {
                    server.pump();
                }
            });
            server.loop.start();
        }
        return server;
    }

    public int getPort() {
        return listener.getPort();
    }

    /** Connections currently open. */
    public int getOpenConnections() {
        return openConnections.get();
    }

    /** Requests being handled right now. This is what saturation looks like. */
    public int getActiveRequests() {
        return activeRequests.get();
    }

    /**
     * A snapshot for a health or metrics endpoint. "draining" is what a load
     * balancer needs to see to take this instance out of rotation before it stops
     * answering.
     */
    public Map getMetrics() {
        Map out = new LinkedHashMap();
        out.put("status", running ? "ok" : "draining");
        out.put("uptimeSeconds", new Long((System.currentTimeMillis() - startedAt) / 1000L));
        out.put("openConnections", new Integer(openConnections.get()));
        out.put("activeRequests", new Integer(activeRequests.get()));
        out.put("requestsServed", new Long(requestsServed.get()));
        out.put("connectionsAccepted", new Long(connectionsAccepted.get()));
        out.put("connectionsRefused", new Long(connectionsRefused.get()));
        out.put("tls", tls == null ? "off" : "on");
        out.put("http2Connections", new Integer(http2Sessions.size()));
        return out;
    }

    /**
     * Blocks until the server has fully stopped, draining included.
     *
     * A caller's main() must do this or something equivalent: the reactor and the
     * workers run on threads ParparVM creates DETACHED, so when main returns the
     * process exits and takes them with it -- silently, with status 0, which from
     * outside looks exactly like a server that refuses connections.
     *
     * Waiting on the reactor THREAD is not enough, and that was a real bug: stop()
     * clears the running flag, the loop returns on its next timeout, main wakes up
     * and the process ends while a worker is still writing a response. This waits
     * on the drain finishing instead.
     */
    public void awaitTermination() {
        synchronized(stopped) {
            while(!fullyStopped) {
                try {
                    stopped.wait();
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Stops accepting, lets in-flight requests finish, then closes what is left.
     *
     * The order matters. Closing the listener first means no new work arrives while
     * the pool drains; draining before closing connections means a request already
     * being served gets to produce its response instead of having the socket pulled
     * out from under it, which is what a client sees as a truncated reply.
     */
    public void stop(int drainMillis) {
        running = false;
        reactor.remove(listener.getFd());
        listener.close();
        if(workers != null) {           // null in virtual-thread mode; see start()
            workers.shutdown();
        }
        long deadline = System.currentTimeMillis() + drainMillis;
        // Waits on requests IN FLIGHT, not on open connections: an idle keep-alive
        // connection has nothing to finish and would otherwise hold the shutdown
        // open for the whole window for no reason.
        while(System.currentTimeMillis() < deadline && activeRequests.get() > 0) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Whatever is still open at the deadline is an idle keep-alive connection or
        // a request that overran; both get closed rather than held forever.
        synchronized(sessions) {
            java.util.Iterator it = new java.util.ArrayList(sessions.keySet()).iterator();
            while(it.hasNext()) {
                Object key = it.next();
                Object session = sessions.remove(key);
                if(session != null) {
                    Tls.closeSession(((Long)session).longValue());
                }
            }
        }
        synchronized(http2Sessions) {
            java.util.Iterator it = new java.util.ArrayList(http2Sessions.keySet()).iterator();
            while(it.hasNext()) {
                Object h2 = http2Sessions.remove(it.next());
                if(h2 != null) {
                    ((Http2)h2).close();
                }
            }
        }
        if(tls != null) {
            tls.close();
        }
        synchronized(stopped) {
            fullyStopped = true;
            stopped.notifyAll();
        }
    }

    /** Stops with a default drain window. */
    public void stop() {
        stop(10000);
    }

    private void pump() {
        trace("reactor thread started");
        int[] ready = new int[READY_CAPACITY];
        int listenFd = listener.getFd();
        while(running) {
            int n;
            try {
                // A timeout rather than an infinite wait, so stop() is noticed even
                // when no connection ever arrives.
                n = reactor.await(ready, 250);
            } catch (IOException err) {
                if(running) {
                    System.err.println("reactor failed: " + err);
                }
                return;
            }
            if(n > 0) {
                trace("ready=" + n);
            }
            for(int iter = 0 ; iter < n ; iter++) {
                int fd = ready[iter];
                if(fd == listenFd) {
                    acceptAll();
                } else {
                    handOff(fd);
                }
            }
        }
    }

    /** The methods this server routes. Anything else is 501, not a 404. */
    private static boolean isKnownMethod(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "POST".equals(method)
                || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method)
                || "OPTIONS".equals(method);
    }

    /**
     * The server a virtual thread belongs to.
     *
     * A virtual thread's body is a C function and cannot carry a Java receiver,
     * so it arrives at serveVirtual with a descriptor and nothing else. One
     * server per process is the shape every backend binary has.
     */
    private static volatile HttpServer ACTIVE_SERVER;

    /**
     * What a connection's virtual thread runs. Reached from native code only,
     * which is also what keeps it from being dead-code eliminated.
     *
     * Deliberately just serve(): the existing connection handling, written in
     * the blocking style, unchanged. That it now runs on a virtual thread is
     * invisible to it, which is the property that makes virtual threads worth
     * having rather than a rewrite into callbacks.
     */
    static void serveVirtual(int fd) {
        HttpServer server = ACTIVE_SERVER;
        if(server == null) {
            ServerSocket.closeFd(fd);
            return;
        }
        server.serve(fd);
    }

    /**
     * One host thread's private world: its poller, its connections, its virtual
     * threads, its run queue.
     *
     * NOTHING here is shared, and that is the point. A virtual thread runs on
     * the host that accepted its connection and on no other, for its whole life.
     *
     * WHY AFFINITY IS NOT A TUNING CHOICE. The VM keeps real state per OS
     * THREAD: the BiBOP allocator's current page (`bibopCurrent`), the pacing
     * claim (`cn1MyPacingClaim`), the mark buffer, `cn1TlsSelf`, and this
     * backend's own zero-copy read buffer. A virtual thread that parks on one
     * host and resumes on another continues against a different thread's copy of
     * all of it. The first version had no affinity and crashed as soon as the
     * collector's backpressure started parking virtual threads mid-allocation.
     *
     * Auditing each of those for migration-safety would be a list that grows
     * every time somebody adds a __thread; pinning the virtual thread to its
     * host makes every one of them correct by construction, including the ones
     * nobody has written yet.
     */
    private static final class VtHost {
        final Reactor poller;

        /**
         * Virtual threads ready to run, as a preallocated ring of raw handles.
         *
         * NOT a LinkedList of boxed Longs, and this is the single most important
         * line in the scheduler. That version allocated twice per yield -- the
         * box and the list node -- ON THE HOST THREAD, and a host thread has no
         * virtual thread to hand back when the collector's backpressure stops it:
         * it just sleeps. Caught with a debugger, the accepting host was sitting
         * in cn1PacingPark underneath LinkedList.addLast underneath advance(),
         * which is this queue. Nothing was accepted after that, and the failure
         * amplifies itself -- the more the collector is behind, the more the
         * scheduler allocates trying to cope.
         *
         * A scheduler's hot path must not allocate, or it becomes a customer of
         * the very backpressure it is supposed to be relieving.
         */
        long[] ring = new long[256];
        int ringHead = 0;
        int ringCount = 0;

        boolean ringEmpty() {
            return ringCount == 0;
        }

        void ringAdd(long handle) {
            if(ringCount == ring.length) {
                // Growth allocates, which is why the ring starts big enough that
                // it does not happen in steady state: it is bounded by how many
                // virtual threads can be mid-yield at once on ONE host.
                long[] grown = new long[ring.length * 2];
                for(int iter = 0 ; iter < ringCount ; iter++) {
                    grown[iter] = ring[(ringHead + iter) % ring.length];
                }
                ring = grown;
                ringHead = 0;
            }
            ring[(ringHead + ringCount) % ring.length] = handle;
            ringCount++;
        }

        /**
         * Head first, and NOT the FILO order fasthttp uses.
         *
         * fasthttp hands a new connection to its most recently released worker --
         * "such a scheme keeps CPU caches hot" -- and the same idea looks like it
         * should apply here. It does not, because it is not the same queue.
         * fasthttp is choosing among IDLE, INTERCHANGEABLE workers, where nothing
         * can starve: whichever it picks, every connection still has a worker.
         * This queue holds PENDING RUNNABLE CONTEXTS, and the order decides
         * whether a connection runs at all -- work keeps arriving at the tail, so
         * taking from the tail lets the head sit.
         *
         * Measured rather than reasoned, tail-first against head-first, two reps
         * at each of 64 and 256 connections:
         *
         *     64  conns   head-first 277152/176814 rps, 1.48/1.39 cores
         *                 tail-first  92603/ 73980 rps, 0.56/0.55 cores
         *     256 conns   head-first 274451/270569 rps, 1.52/1.51 cores
         *                 tail-first 131821/120500 rps, 0.81/0.74 cores
         *
         * Tail-first costs two thirds of the throughput and half the machine,
         * four readings out of four. Its p99 looks better only because it is
         * serving a third of the traffic. Do not re-import this from fasthttp
         * without re-reading which queue it applies to.
         */
        long ringTake() {
            if(ringCount == 0) {
                return 0;
            }
            long handle = ring[ringHead];
            ringHead = (ringHead + 1) % ring.length;
            ringCount--;
            return handle;
        }
        /** Descriptor to virtual-thread handle. Only this host touches it. */
        long[] vtByFd = new long[1024];

        /**
         * When each parked connection stops being worth waiting for, or 0.
         *
         * A parked virtual thread is resumed only when its descriptor becomes
         * readable, so a client that connects, sends half a request and then goes
         * quiet is never resumed and never shed: the connection lives for ever
         * and holds a virtual thread and its stacks. That is slowloris, and
         * BackendHttpIntegrationTest.shedsIdleConnections tests for it. The
         * dispatching path inherits the behaviour from the socket deadline; this
         * path has to enforce it, because nothing else will.
         */
        long[] deadlineByFd = new long[1024];

        VtHost(Reactor poller) {
            this.poller = poller;
        }

        long handleFor(int fd) {
            return fd < vtByFd.length ? vtByFd[fd] : 0;
        }

        void setHandle(int fd, long handle) {
            if(fd >= vtByFd.length) {
                int size = vtByFd.length;
                while(size <= fd) {
                    size = size * 2;
                }
                long[] grown = new long[size];
                System.arraycopy(vtByFd, 0, grown, 0, vtByFd.length);
                vtByFd = grown;
                long[] grownDeadlines = new long[size];
                System.arraycopy(deadlineByFd, 0, grownDeadlines, 0, deadlineByFd.length);
                deadlineByFd = grownDeadlines;
            }
            vtByFd[fd] = handle;
            if(handle == 0) {
                deadlineByFd[fd] = 0;
            }
        }

        void setDeadline(int fd, long at) {
            if(fd < deadlineByFd.length) {
                deadlineByFd[fd] = at;
            }
        }
    }

    private VtHost[] vtHosts;

    /**
     * Which host owns each descriptor, so a connection can be re-armed on the
     * poller it actually lives in.
     *
     * Written only by the accept loop, which runs on host 0 alone, and read only
     * by the owning host -- which cannot learn the descriptor exists until the
     * registration syscall below has already happened, so the write is published
     * before any reader can reach it.
     */
    private int[] vtOwnerByFd = new int[1024];

    /** Round-robin cursor for handing new connections out. Accept thread only. */
    private int nextVtHost;

    private void setVtOwner(int fd, int host) {
        if(fd >= vtOwnerByFd.length) {
            int size = vtOwnerByFd.length;
            while(size <= fd) {
                size = size * 2;
            }
            int[] grown = new int[size];
            System.arraycopy(vtOwnerByFd, 0, grown, 0, vtOwnerByFd.length);
            vtOwnerByFd = grown;
        }
        vtOwnerByFd[fd] = host;
    }

    private VtHost ownerOf(int fd) {
        int index = fd < vtOwnerByFd.length ? vtOwnerByFd[fd] : 0;
        if(index < 0 || index >= vtHosts.length) {
            index = 0;
        }
        return vtHosts[index];
    }
    /** Round robin over the hosts, used only by whoever is accepting. */
    private int vtNextHost = 0;

    /**
     * One host thread: run whoever is ready, then poll for more.
     *
     * Everything it touches belongs to it. The run queue is a plain LinkedList
     * because no other thread can reach it, and the descriptor table is a plain
     * long[] for the same reason -- affinity is what buys that, and it is worth
     * more than the lock it saves, because it is also what makes the VM's
     * per-thread allocator state correct under a parked virtual thread.
     */
    private void runVirtualThreadHost(int index) {
        VtHost me = vtHosts[index];
        int[] ready = new int[READY_CAPACITY];
        int listenFd = listener.getFd();
        boolean owner = (index == 0);       // only one host accepts
        while(running) {
            // Runnable virtual threads first: they wait for a turn, not for the
            // network, so polling before running them would delay them by the
            // whole poll timeout.
            boolean ranSome = drainRunnable(me);
            int n;
            try {
                n = me.poller.await(ready, (ranSome || !me.ringEmpty()) ? 0 : 250);
                if(n == 0) {
                    sweepDeadlines(me);
                }
            } catch (IOException err) {
                if(running) {
                    System.err.println("poller failed: " + err);
                }
                return;
            }
            for(int iter = 0 ; iter < n ; iter++) {
                int fd = ready[iter];
                if(owner && fd == listenFd) {
                    acceptAll();
                    try {
                        me.poller.modify(listenFd, CONN_EVENTS);
                    } catch (IOException err) {
                        if(running) {
                            System.err.println("could not re-arm the listener: " + err);
                        }
                        return;
                    }
                    continue;
                }
                advance(me, fd, me.handleFor(fd));
            }
        }
    }

    /**
     * Run the virtual threads that are ready. True if any were.
     */
    private boolean drainRunnable(VtHost me) {
        boolean any = false;
        int budget = me.ringCount;          // one pass, so a busy one cannot starve the poller
        while(budget-- > 0 && !me.ringEmpty()) {
            long handle = me.ringTake();
            any = true;
            advance(me, VirtualThread.descriptorOf(handle), handle);
        }
        return any;
    }

    /**
     * Give a connection's virtual thread its turn, creating it on first sight,
     * and do whatever its answer asks for.
     *
     * The three answers are the whole scheduler. FINISHED means the connection is
     * over. PARKED_IO means it wants bytes, so the descriptor goes back to this
     * host's poller. RUNNABLE means it gave up its turn but is ready now -- it is
     * waiting on the collector, not on its socket -- and handing that one to the
     * poller would wait for a client that is itself waiting for the response this
     * virtual thread still owes it.
     */
    private void advance(VtHost me, int fd, long handle) {
        if(fd < 0) {
            return;
        }
        if(handle == 0) {
            handle = VirtualThread.create(fd, VT_STACK_BYTES);
            if(handle == 0) {
                // No stack. Serving it on this thread is not an option here: the
                // keep-alive wait is indefinite because it expects to park, so
                // this host would never poll again. Refuse instead, and say so
                // once -- a server quietly dropping to zero is far worse.
                if(vtCreateFailures.incrementAndGet() == 1) {
                    System.err.println("virtual thread creation failed; "
                            + "refusing connections rather than pinning a host");
                }
                drop(fd);
                return;
            }
            me.setHandle(fd, handle);
        }
        me.setDeadline(fd, 0);      // it is running, so it is not idle
        int state = VirtualThread.resume(handle);
        if(state == VirtualThread.FINISHED) {
            me.setHandle(fd, 0);
            VirtualThread.free(handle);
            return;
        }
        if(state == VirtualThread.RUNNABLE) {
            me.ringAdd(handle);
            return;
        }
        // Parked on I/O: start its clock. Nothing else will, and without it a
        // half-sent request parks a virtual thread for ever.
        me.setDeadline(fd, System.currentTimeMillis() + SOCKET_TIMEOUT_MILLIS);
        try {
            me.poller.modify(fd, CONN_EVENTS);
        } catch (IOException err) {
            me.setHandle(fd, 0);
            VirtualThread.free(handle);
            drop(fd);
        }
    }

    /**
     * Close connections whose deadline passed while they were parked.
     *
     * Swept on the poll timeout rather than per event: a connection making
     * progress is resumed by readability long before this runs, so the only
     * descriptors it ever finds are the silent ones.
     */
    private void sweepDeadlines(VtHost me) {
        long now = System.currentTimeMillis();
        for(int fd = 0 ; fd < me.deadlineByFd.length ; fd++) {
            long at = me.deadlineByFd[fd];
            if(at == 0 || at > now) {
                continue;
            }
            long handle = me.handleFor(fd);
            me.setDeadline(fd, 0);
            if(handle != 0) {
                me.setHandle(fd, 0);
                VirtualThread.free(handle);
            }
            drop(fd);
        }
    }

    /**
     * Arm a connection descriptor for its next request.
     *
     * The two modes need different calls and getting it wrong fails quietly in
     * both directions, which is why this is one place. Under ONESHOT the kernel
     * DISARMS a descriptor as it delivers it but leaves it registered, so coming
     * back is EPOLL_CTL_MOD; an EPOLL_CTL_ADD would fail with EEXIST and the
     * connection would hang for ever. The dispatching path removed the
     * descriptor before handing it over, so there it has to be an ADD.
     *
     * @param fresh true for a descriptor the poller has never seen -- one just
     *              accepted -- which is an ADD either way.
     */
    /**
     * Hand a connection to a host and keep it there.
     *
     * Every accepted descriptor used to be registered with `reactor`, which IS
     * host 0's poller, so every connection lived on host 0 and the other hosts
     * polled empty sets for the life of the process. Virtual-thread mode was
     * therefore single threaded: measured 0.75 of two pinned cores against the
     * pool's 1.61 and Go's 1.49, while costing the LEAST cpu per request of the
     * three (5.64 us against 9.78 and 6.87). It was not slower, it was narrower.
     *
     * Affinity is enforced by the poller -- a descriptor registered in one host's
     * set is only ever reported to that host -- so choosing the set at accept
     * time is what assigns the owner, and the virtual thread is then created by
     * whichever host first sees it. The accept loop never touches another host's
     * descriptor table, so that table stays single-writer.
     */
    private void armConnection(int fd, boolean fresh) throws IOException {
        if(!VIRTUAL_THREADS) {
            reactor.add(fd, CONN_EVENTS);
            return;
        }
        if(fresh) {
            int host = nextVtHost;
            nextVtHost = host + 1 >= vtHosts.length ? 0 : host + 1;
            setVtOwner(fd, host);
            vtHosts[host].poller.add(fd, CONN_EVENTS);
            return;
        }
        // Re-arm has to name the SAME poller: an epoll set that does not hold
        // this descriptor answers a modify with ENOENT, and before connections
        // were distributed this was reached through `reactor` and happened to be
        // right for every fd.
        ownerOf(fd).poller.modify(fd, CONN_EVENTS);
    }

    private void acceptAll() {
        while(running) {
            int fd = listener.accept();
            if(fd < 0) {
                return; // drained
            }
            trace("accepted fd=" + fd);
            if(MAX_CONNECTIONS > 0 && openConnections.get() >= MAX_CONNECTIONS) {
                // Accept-and-close rather than stop accepting: leaving it in the
                // backlog looks to the client like a server that hangs.
                trace("at the connection ceiling, refusing fd=" + fd);
                connectionsRefused.incrementAndGet();
                ServerSocket.closeFd(fd);
                continue;
            }
            try {
                ServerSocket.setBlocking(fd, false);
                ServerSocket.setTimeout(fd, SOCKET_TIMEOUT_MILLIS);
                armConnection(fd, true);
                vtAccepts.incrementAndGet();
                openConnections.incrementAndGet();
                connectionsAccepted.incrementAndGet();
            } catch (IOException err) {
                ServerSocket.closeFd(fd);
            }
        }
    }

    /**
     * Takes the descriptor away from the reactor and gives it to a worker. It has
     * to leave the poller BEFORE the worker starts reading: this is level
     * triggered, so an fd left registered is reported ready again on the next turn
     * and two workers end up on one connection.
     */
    /**
     * How many ready descriptors one wake may carry.
     *
     * The dispatching path costs a task object and a WAKE per descriptor, and a
     * wake is a futex -- a kernel operation whose cost is the same whichever
     * language issues it. Measured against Go under the same load this server
     * does 6.9x the futex traffic per request while doing the same number of
     * reads and writes, so the coordination is the gap rather than the work.
     *
     * A poller turn that finds N ready descriptors does not need N wakes: one
     * worker can be handed the batch and walk it. That divides the dominant cost
     * by the batch size, and batches are BIGGEST under exactly the load where
     * this server is furthest behind.
     *
     * 1 restores the old behaviour exactly, which is what makes the comparison
     * an A/B rather than a rewrite.
     */
    private static final int HANDOFF_BATCH = envInt("CN1_HTTP_HANDOFF_BATCH", 1);

    /**
     * Give a whole batch of ready descriptors to ONE worker, in one wake.
     *
     * Every descriptor still leaves the poller before any of them is read, for
     * the same reason the single handoff does it: level-triggered, an fd left
     * registered is reported ready again on the next turn and a second worker
     * lands on a connection this batch already owns.
     */
    private void handOffBatch(final int[] fds, final int count) {
        for(int iter = 0 ; iter < count ; iter++) {
            reactor.remove(fds[iter]);
        }
        pendingWork.addAndGet(count);
        try {
            workers.execute(new Runnable() {
                public void run() {
                    for(int iter = 0 ; iter < count ; iter++) {
                        pendingWork.decrementAndGet();
                        serve(fds[iter]);
                    }
                }
            });
        } catch (RuntimeException err) {
            for(int iter = 0 ; iter < count ; iter++) {
                pendingWork.decrementAndGet();
                drop(fds[iter]);
            }
        }
    }

    private void handOff(final int fd) {
        trace("handOff fd=" + fd);
        reactor.remove(fd);
        pendingWork.incrementAndGet();
        try {
            workers.execute(new Runnable() {
                public void run() {
                    pendingWork.decrementAndGet();
                    serve(fd);
                }
            });
        } catch (RuntimeException err) {
            pendingWork.decrementAndGet();
            // The pool rejected it (shutting down). Closing is the honest answer;
            // holding the connection open would promise service that is not coming.
            drop(fd);
        }
    }

    /** The only place a served connection is closed, so the count stays honest. */
    private void drop(int fd) {
        Object h2 = http2Sessions.remove(new Integer(fd));
        if(h2 != null) {
            ((Http2)h2).close();
        }
        Object session = sessions.remove(new Integer(fd));
        if(session != null) {
            Tls.closeSession(((Long)session).longValue());
        }
        ServerSocket.closeFd(fd);
        openConnections.decrementAndGet();
    }

    /** 0 when this connection is plaintext. */
    private long sessionOf(int fd) {
        Object session = sessions.get(new Integer(fd));
        return session == null ? 0 : ((Long)session).longValue();
    }

    private static int readFrom(int fd, long session, byte[] buffer, int offset, int length)
            throws IOException {
        return session == 0 ? ServerSocket.read(fd, buffer, offset, length)
                            : Tls.read(session, buffer, offset, length);
    }

    private static void writeTo(int fd, long session, byte[] buffer, int offset, int length)
            throws IOException {
        if(session == 0) {
            ServerSocket.write(fd, buffer, offset, length);
        } else {
            Tls.write(session, buffer, offset, length);
        }
    }

    private void serve(int fd) {
        trace("serve fd=" + fd);
        activeRequests.incrementAndGet();
        try {
            serveOne(fd);
        } finally {
            activeRequests.decrementAndGet();
        }
    }

    /** A malformed request that deserves a specific status before the close. */
    private static final class ProtocolException extends IOException {
        final int status;

        ProtocolException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    /**
     * A connection plus whatever has been read from it and not yet consumed.
     *
     * The leftover is the point. A client may send a second request before reading
     * the reply to the first, and both arrive in one read; a parser that keeps only
     * the request it wanted silently drops the rest. That is not an exotic case --
     * it is what pipelining is, and what a proxy does when it coalesces.
     */
    private final class Conn {
        final int fd;
        final long session;
        byte[] buffer = new byte[0];
        int pos;
        /** True while `buffer` is the thread's shared buffer rather than ours. */
        boolean borrowed;
        /**
         * True once this request's header slices name positions in `buffer`.
         *
         * "Is anything still pointing at this buffer" is the question fill() has to
         * answer before it lets go of a borrow, and "is there anything left to
         * read" is NOT the same question -- see the comment there.
         */
        boolean parsedFromBuffer;

        /** Memoised request targets for this connection. See internTarget. */
        private final String[] targetCache = new String[TARGET_CACHE_SLOTS];

        String internTarget(byte[] data, int start, int length) {
            if(targetCache.length == 0) {
                // Cache disabled (CN1_HTTP_TARGET_CACHE=0), for A/B measurement.
                // Guarded because the slot arithmetic below is a modulo, and a zero
                // size would divide by it rather than politely doing nothing.
                return asciiString(data, start, length);
            }
            int hash = 0;
            for(int iter = 0 ; iter < length ; iter++) {
                hash = hash * 31 + data[start + iter];
            }
            int slot = (hash & 0x7fffffff) % TARGET_CACHE_SLOTS;
            String cached = targetCache[slot];
            if(cached != null && cached.length() == length) {
                int iter = 0;
                while(iter < length
                        && cached.charAt(iter) == (char)(data[start + iter] & 0xff)) {
                    iter++;
                }
                if(iter == length) {
                    return cached;
                }
            }
            String fresh = asciiString(data, start, length);
            // One entry per slot, overwritten on collision rather than chained:
            // two hot targets that collide would otherwise both miss forever, and
            // overwriting lets whichever is currently hot keep the slot.
            targetCache[slot] = fresh;
            return fresh;
        }
        /**
         * Set when a read returned end-of-stream. The linger above has to tell a
         * client that WENT AWAY from one that has merely gone quiet: the first
         * must be closed, and handing the second to the poller is the whole point.
         */
        boolean closedByPeer;

        /**
         * Where a response is assembled, reused for the life of the connection.
         *
         * The head used to be built with a StringBuilder, turned into a String and
         * then encoded to bytes, and the body copied in after that -- four
         * allocations per response, and the StringBuilder reallocating its char[]
         * as it grew. An allocation census put char[] at 47% of ALL allocation in
         * this server, and this path was most of it. Bytes go in directly now:
         * the header field names are ASCII constants, and a status or a length is
         * digits.
         */
        byte[] out = new byte[1024];
        int outLength;
        /**
         * Reused header slices: nameStart, nameLength, valueStart, valueLength.
         *
         * Handed to a Request BY REFERENCE, not copied -- and that is only safe
         * because `buffer` is a fresh, exactly-sized array per fill, so each
         * Request's `raw` is privately owned and immutable once parsed. The two are
         * a pair: the slices name absolute offsets into that particular array.
         *
         * Anything that makes the buffer REUSABLE breaks the pair. Measured, with a
         * capacity-plus-limit buffer in place of the per-fill array: a Request came
         * back holding `raw.length=45` while its own slices named offsets 212 and
         * 232, i.e. raw from one request and the slice table from a larger later
         * one -- the shared table had been re-parsed under a Request still using it.
         * The result was an ArrayIndexOutOfBoundsException in getHeader, thrown
         * outside any try block, which killed the connection with no response
         * written (~1 suite run in 2).
         *
         * TWO WRONG ANSWERS, so that a third attempt does not re-buy them. It is
         * NOT two workers on one connection: a probe that reports a second thread
         * entering serve() for a descriptor already inside it fired ZERO times on
         * the build that fails (the same probe on the passing build proves nothing,
         * which is how it was nearly mis-read). And it is not the slice table
         * overflowing: slices.length was 64 against a headerCount of 3.
         *
         * THE ACTUAL CAUSE, and it is a property of the VM rather than of this
         * class: a zero-copy buffer's LENGTH IS NOT STABLE. readIntoThreadBufferImpl
         * hands back the same array object every call and mutates it in place --
         * `a->length = (int)n` -- because a ParparVM array's length is a field in a
         * struct the runtime owns. So the array reports ~100 bytes while its headers
         * are parsed (slices at 39, 59, 69) and reports 40 after the same thread's
         * next read, with the already-parsed slices left naming positions past the
         * end. That is the 40-against-69 reading, and nothing moved: the length did.
         *
         * `available()` is written as `buffer.length - pos` for exactly this reason.
         * It re-reads the length every time and therefore self-corrects. Caching it
         * in a `limit` field -- which is what a reusable buffer needs -- is what
         * breaks, and it breaks silently, as a truncated response rather than a
         * wrong one.
         *
         * So a reusable buffer has to stop borrowing first: take a private array
         * (whose length really is immutable) before anything caches a length or
         * parses slices out of it. Sizing that copy is itself subject to the same
         * trap, since buffer.length must be read before the next read mutates it.
         *
         * THAT WAS BUILT, AND IT IS NOT WORTH IT. With the length handled correctly
         * the reusable buffer is correct -- 4 default plus 2 virtual-thread suite
         * runs clean, against a naive version that failed within two -- and it buys
         * NOTHING. Measured in virtual-thread mode, same 12s window and load:
         *
         *   cycles per window   reuse 40, 40, 38     no reuse 41, 42, 41
         *   requests            2.69M, 2.63M, 2.56M  3.08M, 3.01M, 2.70M
         *
         * The collection RATE does not move, so this array is not a meaningful part
         * of the ~340 bytes a request allocates, and the throughput came out lower
         * in all three reps (arms were not interleaved, so treat that half loosely).
         * The per-request read buffer is simply not where the allocation is: look
         * for the bytes before removing an allocation on the assumption it matters.
         *
         * So removing the per-request byte[] is not just a capacity field: a Request
         * has to own a consistent (raw, slices, headerCount) triple, re-based
         * together or not at all.
         */
        int[] slices = new int[64];
        /**
         * Where a deferred JSON body is serialised, so its length is known before
         * the head that must declare it is written. Reused like everything else
         * here; the copy into the head buffer afterwards is a memcpy of a body
         * small enough to share a packet with its headers.
         */
        final ByteSink bodySink = new ByteSink(512);

        void reset() {
            outLength = 0;
        }

        void ensure(int extra) {
            if(outLength + extra <= out.length) {
                return;
            }
            int size = out.length * 2;
            while(size < outLength + extra) {
                size *= 2;
            }
            byte[] grown = new byte[size];
            System.arraycopy(out, 0, grown, 0, outLength);
            out = grown;
        }

        /** ASCII only. Every caller passes a header name or a constant. */
        void put(String ascii) {
            int n = ascii.length();
            ensure(n);
            for(int iter = 0 ; iter < n ; iter++) {
                out[outLength++] = (byte)ascii.charAt(iter);
            }
        }

        /**
         * The content type, encoded once per connection rather than per response.
         *
         * put(String) walks charAt by charAt, and a handler hands back the same
         * String instance every time -- a literal, or a constant on Response --
         * so after the first response the bytes are already there. Identity, not
         * equals: a handler that builds a fresh String per response simply keeps
         * missing and pays what it paid before, and the cache is filled ONCE so
         * that case cannot allocate per request either.
         */
        private String ctKey;
        private byte[] ctBytes;

        void putContentType(String ct) {
            if(ct == ctKey) {
                System.arraycopy(ctBytes, 0, out, ensureAt(ctBytes.length), ctBytes.length);
                outLength += ctBytes.length;
                return;
            }
            put(ct);
            if(ctKey == null && ct != null) {
                ctKey = ct;
                ctBytes = asciiBytes(ct);
            }
        }

        /** Reserves {@code n} bytes and answers the offset they start at. */
        private int ensureAt(int n) {
            ensure(n);
            return outLength;
        }

        void put(byte[] data, int offset, int length) {
            ensure(length);
            System.arraycopy(data, offset, out, outLength, length);
            outLength += length;
        }

        void put(int b) {
            ensure(1);
            out[outLength++] = (byte)b;
        }

        /**
         * A non-negative number as ASCII digits, written in place.
         * Integer.toString would allocate a String and its char[] -- per response,
         * twice (the status and the content length).
         */
        void putNumber(long value) {
            if(value < 0) {
                put("-");
                value = -value;
            }
            if(value == 0) {
                put('0');
                return;
            }
            int start = outLength;
            // Tried and REVERTED: an int fast path that sized the number with a
            // ternary chain instead of dividing to count digits. It looked like a
            // clear win on paper -- this was 2.1% of on-CPU time and a 64-bit
            // divide is tens of cycles -- but measured 6 of 8 interleaved reps
            // SLOWER, median about -3.7%. The likely reason is that a ten-way
            // ternary compiles to branchy operand-stack code here, which costs more
            // than the divisions it removes. Do not re-attempt without measuring on
            // an idle machine; the reps above were taken at load 17 and are weak
            // evidence, but there was no sign of a gain in any of them.
            long v = value;
            int digits = 0;
            while(v > 0) {
                digits++;
                v /= 10;
            }
            ensure(digits);
            outLength += digits;
            int at = outLength;
            while(value > 0) {
                out[--at] = (byte)('0' + (int)(value % 10));
                value /= 10;
            }
            if(at != start) {
                // Unreachable unless the digit count and the loop disagree; the
                // buffer would be left with a hole rather than a short write.
                throw new IllegalStateException("digit count mismatch");
            }
        }

        Conn(int fd, long session) {
            this.fd = fd;
            this.session = session;
        }

        int available() {
            return buffer.length - pos;
        }

        /**
         * The one Response handed to Request.respond on this connection. Null until
         * a handler asks for it, so a handler that never does pays nothing.
         */
        Response pooledResponse;

        /**
         * The one Request served on this connection, re-pointed per request rather
         * than reallocated. Response and Request were the whole of what /plaintext
         * still allocated once the borrowed-buffer copy went: 88 and 80 bytes, one
         * of each, every request.
         */
        Request pooledRequest;

        /** Reads more. False at end of stream. */
        boolean fill(byte[] scratch) throws IOException {
            if(borrowed && available() == 0 && !parsedFromBuffer) {
                // Nothing is left unread AND nothing has been parsed out of this
                // buffer, so this is the START of a new request on a kept-alive
                // connection and there is nothing to preserve: the previous request
                // was answered before the loop came back here. Just let go.
                //
                // The parsedFromBuffer half is load-bearing and was missing. An
                // empty buffer does NOT mean no request is in flight: a POST whose
                // headers arrive in one segment and whose body arrives in the next
                // reaches the body loop with the header block fully consumed, so
                // available() is 0 while the Request's slices still name positions
                // in this very buffer. Taking this branch there dropped the borrow,
                // skipped detachPreservingOffsets below, and let the next zero-copy
                // read overwrite the headers with the body -- after which
                // getHeader("connection") walked off the end of the array and the
                // AIOOBE killed the connection with no response written at all.
                //
                // That is a truncated reply, not a wrong one, so it showed up only
                // as a client-side timeout: transactionRollsBack failing 15.05s
                // (its own setSoTimeout) with status -1, and authGuardsMutatingRoutes
                // reading an empty body, about 2 full-suite runs in 6. It never
                // appeared under virtual threads because ZERO_COPY_READ is off
                // there, which is also why it survived the whole reactor rewrite.
                //
                // Copying here instead is what a first version did, and it put the
                // per-request byte[] straight back -- the linger means a worker
                // loops without handing the descriptor back, so `borrowed` was still
                // set on every subsequent request and each one copied the whole
                // buffer. The census said 223 bytes per request where it should have
                // said none, which is the only reason it was noticed.
                buffer = EMPTY_BODY;
                pos = 0;
                borrowed = false;
            } else if(borrowed) {
                // A second read WITHIN one request is about to overwrite the
                // thread buffer, and a Request parsed out of it holds SLICES into
                // exactly that memory. Copy first, preserving absolute offsets so
                // those slices stay valid.
                //
                // Found by BackendHttpIntegrationTest.transactionRollsBack, which
                // sends a body big enough to need two reads along with an
                // Authorization header: the second read landed on top of the header
                // and the request came back 401 instead of 400. A corrupted header
                // is the good version of this bug -- the same overwrite could just
                // as easily have served one request's bytes inside another's.
                detachPreservingOffsets();
            }
            if(ZERO_COPY_READ && available() == 0 && session == 0) {
                // The common case by far: nothing left over, so the bytes are read
                // into this thread's reusable buffer and parsed where they land --
                // no array allocated, nothing copied. The request is parsed out of
                // the same memory the kernel wrote into.
                //
                // The returned array's length is exactly what was read, so every
                // parser below that scans to buffer.length keeps working untouched.
                // That is only possible because the length of a ParparVM array is a
                // field in a struct we own; introducing a separate limit instead
                // would have meant auditing fifteen call sites, and one missed site
                // reads a previous request's bytes into this one's response.
                //
                // Plaintext only (session == 0): a TLS read decrypts through its
                // own path and does not hand back a buffer we own.
                byte[] direct = ServerSocket.readIntoThreadBuffer(fd, scratch.length);
                if(direct == null) {
                    closedByPeer = true;
                    return false;
                }
                if(ZERO_COPY_MODE == 2) {
                    // Diagnostic bisection only -- see ZERO_COPY_MODE. Same read as
                    // mode 1, same heap array as mode 0, so whichever of the two the
                    // throughput follows is the one that costs.
                    byte[] owned = new byte[direct.length];
                    System.arraycopy(direct, 0, owned, 0, direct.length);
                    buffer = owned;
                    pos = 0;
                    borrowed = false;
                    return true;
                }
                buffer = direct;
                pos = 0;
                borrowed = true;
                return true;
            }
            int n = readFrom(fd, session, scratch, 0, scratch.length);
            if(n <= 0) {
                closedByPeer = true;
                return false;
            }
            int keep = available();
            byte[] grown = new byte[keep + n];
            System.arraycopy(buffer, pos, grown, 0, keep);
            System.arraycopy(scratch, 0, grown, keep, n);
            buffer = grown;
            pos = 0;
            borrowed = false;
            return true;
        }

        /**
         * Give up the shared thread buffer before this connection can be taken by a
         * different worker.
         *
         * The buffer belongs to the THREAD, not the connection. Anything still
         * unread has to be copied somewhere this connection owns before the
         * descriptor goes back to the reactor: the next worker runs on another
         * thread whose buffer is different memory, and the thread that read these
         * bytes overwrites them on its next request.
         *
         * The copy happens only when bytes are actually left over -- the pipelining
         * case. The ordinary request-per-read path copies nothing.
         */
        /**
         * Take a private copy of the whole borrowed buffer, keeping every index the
         * same, so anything already parsed out of it (a Request's header slices)
         * keeps pointing at the right bytes.
         *
         * Compacting here instead would be a subtle disaster: it moves the content
         * to offset zero while the slices still name the old positions.
         */
        void detachPreservingOffsets() {
            byte[] owned = new byte[buffer.length];
            System.arraycopy(buffer, 0, owned, 0, buffer.length);
            buffer = owned;
            borrowed = false;
        }

        void releaseBorrowed() {
            if(!borrowed) {
                return;
            }
            int keep = available();
            if(keep > 0) {
                byte[] owned = new byte[keep];
                System.arraycopy(buffer, pos, owned, 0, keep);
                buffer = owned;
            } else {
                buffer = EMPTY_BODY;
            }
            pos = 0;
            borrowed = false;
        }

        void write(byte[] data) throws IOException {
            writeTo(fd, session, data, 0, data.length);
        }
    }

    private void serveOne(int fd) {
        long session;
        try {
            ServerSocket.setBlocking(fd, true);
            if(tls != null && sessionOf(fd) == 0) {
                // The handshake runs here, on the worker, because the descriptor is
                // blocking here and a handshake is several round trips. On the
                // reactor thread it would stall every other connection.
                long fresh = tls.accept(fd);
                if(fresh == 0) {
                    // Not a TLS client, or no common cipher. Ordinary traffic.
                    drop(fd);
                    return;
                }
                sessions.put(new Integer(fd), new Long(fresh));
            }
            session = sessionOf(fd);
            if(tls != null && Http2.ALPN.equals(Tls.negotiatedProtocol(session))) {
                // ALPN settled on h2, so this connection is framed, not textual,
                // for its whole life. There is no downgrade from here.
                serveHttp2(fd, session, null, 0);
                return;
            }
        } catch (Exception err) {
            drop(fd);
            return;
        }

        Conn conn = new Conn(fd, session);
        byte[] scratch = new byte[8192];
        int served = 0;

        // Cleartext HTTP/2 by prior knowledge: a client that already knows the
        // server speaks h2 opens with the connection preface instead of a request
        // line. This is how gRPC talks over cleartext and how a load balancer that
        // terminated TLS talks to an origin, and it is the only way to reach h2
        // without ALPN.
        if(http2Sessions.containsKey(new Integer(fd))) {
            serveHttp2(fd, session, null, 0);
            return;
        }
        try {
            while(conn.available() < HTTP2_PREFACE.length) {
                if(!conn.fill(scratch)) {
                    drop(fd);
                    return;
                }
                if(!startsWithPrefacePrefix(conn)) {
                    break; // definitely not h2; parse it as HTTP/1.1
                }
            }
            if(conn.available() >= HTTP2_PREFACE.length && matchesPreface(conn)) {
                byte[] rest = new byte[conn.available()];
                System.arraycopy(conn.buffer, conn.pos, rest, 0, rest.length);
                serveHttp2(fd, session, rest, rest.length);
                return;
            }
        } catch (Exception err) {
            drop(fd);
            return;
        }

        while(true) {
            Request request;
            try {
                request = readRequest(conn, scratch);
            } catch (ProtocolException err) {
                trace("fd=" + fd + " rejected: " + err.getMessage());
                writeStatusOnly(conn, err.status, err.getMessage());
                drop(fd);
                return;
            } catch (ServerSocket.TimeoutException err) {
                // An idle client, not a fault. Shedding it is the point of the deadline.
                trace("fd=" + fd + " timed out");
                drop(fd);
                return;
            } catch (Exception err) {
                trace("fd=" + fd + " read failed: " + err);
                drop(fd);
                return;
            }
            if(request == null) {
                drop(fd); // the peer closed
                return;
            }

            boolean keepAlive = wantsKeepAlive(request);
            // Methods are case-sensitive, so this is an exact comparison.
            boolean headOnly = "HEAD".equals(request.getMethod());
            Response response;
            try {
                response = handler.handle(request);
                if(response == null) {
                    response = Response.text(404, "not found");
                }
            } catch (Exception err) {
                System.err.println("handler failed: " + err);
                response = Response.text(500, "internal error");
            }
            try {
                writeResponse(conn, fd, session, response, keepAlive, headOnly);
                requestsServed.incrementAndGet();
            } catch (Exception err) {
                trace("fd=" + fd + " write failed: " + err);
                drop(fd);
                return;
            }
            if(!keepAlive) {
                drop(fd);
                return;
            }
            if(conn.available() > 0) {
                continue; // a pipelined request is already in the buffer
            }
            // The burst cap is a fairness backstop for a POOL: it stops one worker
            // monopolising a shared thread. A virtual thread owns its connection
            // and parking costs nobody anything, so there is nothing to be fair
            // to -- and breaking here would be worse than pointless, because in
            // this mode the loop's exit path closes the connection. That is a
            // healthy keep-alive connection dropped every 256 requests, which the
            // client sees as a mid-stream close: 446833 write errors against
            // 164307 requests at four connections, and it made every earlier
            // virtual-thread measurement an underestimate.
            if(++served >= KEEPALIVE_BURST_LIMIT) {
                if(VIRTUAL_THREADS) {
                    // Step aside rather than close. A virtual thread under a load
                    // generator never runs out of bytes, so it never parks on its
                    // own and would hold this host thread for as long as the
                    // client kept talking -- with fewer hosts than connections the
                    // rest starve, measured as two hosts serving two of sixty four.
                    // Breaking here instead is worse still, because in this mode
                    // the exit path CLOSES the connection: 446833 write errors
                    // against 164307 requests, a healthy keep-alive connection
                    // dropped every 256 requests.
                    served = 0;
                    conn.releaseBorrowed();
                    VirtualThread.yieldNow();
                    continue;
                }
                break;                          // fairness backstop; see the constant
            }
            // On a virtual thread there is nothing to be fair TO: parking releases
            // the host thread immediately, so holding the connection costs no one
            // anything and handing it back would only add a poller round trip per
            // request.
            if(!VIRTUAL_THREADS && pendingWork.get() > 0
                    && workerCount - activeRequests.get() <= pendingWork.get()) {
                // Hand back only when something is actually waiting AND there are
                // not enough idle workers for it -- the case where holding this
                // connection denies service to another. With nothing waiting, or
                // with a spare worker for whoever is, holding costs nobody
                // anything. Testing the idle count alone is wrong in the most
                // common configuration of all: with connections == workers every
                // worker is busy and nothing is queued, so "idle <= pending" reads
                // 0 <= 0 and hands the connection back on every single request,
                // which is the behaviour the linger exists to avoid.
                //
                // Breaking on "anything is waiting at all" was the first attempt and
                // it is too blunt: with 128 workers and 64 connections every worker
                // handed its connection back on every request even though half the
                // pool was idle, and throughput did not move (123k at 16 workers,
                // 126k at 128). The pool size only buys anything if a spare worker
                // actually lets a connection stay put.
                break;
            }
            // Wait briefly for the next request rather than going round the poller
            // for it. See KEEPALIVE_LINGER_MILLIS.
            //
            // With the workers polling, the linger waits ZERO milliseconds: it
            // still asks whether the next request has already arrived, because
            // answering a pipelined request on the spot is free, but it never
            // BLOCKS waiting for one. Two reasons, and they are the reasons the
            // linger exists at all:
            //
            //  - What it buys is avoiding the handback, and in this mode the
            //    handback is one epoll_ctl with no wake and no queue. There is
            //    almost nothing left to avoid.
            //  - What it costs is much higher here. A lingering worker is not
            //    polling, so with fewer workers than connections it withholds the
            //    poller itself. In the dispatching mode a lingering worker only
            //    withheld itself, because a separate thread went on polling.
            //
            // This is what Go does: read optimistically, park on EAGAIN
            // (internal/poll.FD.Read). The park is what re-arming is here.
            // -1 on a virtual thread: wait for the next request for as long as the
            // client cares to take. That is not a blocked thread, it is a parked
            // virtual thread costing a stack and nothing else, which is exactly
            // the resource an idle keep-alive connection should cost.
            int linger = VIRTUAL_THREADS ? -1 : KEEPALIVE_LINGER_MILLIS;
            if(VIRTUAL_THREADS || linger > 0) {
                boolean more;
                try {
                    // A readiness wait rather than a timed read: it is one syscall
                    // and it leaves the receive deadline alone, so the request this
                    // is waiting for still gets the full one when it arrives.
                    // Setting and restoring SO_RCVTIMEO around each wait did work,
                    // and cost four setsockopt per request -- 15% of syscall time.
                    if(!ServerSocket.awaitReadable(fd, linger)) {
                        break;                  // quiet client; the poller can have it
                    }
                    // The request this buffer was parsed from has been ANSWERED,
                    // so nothing points into it any more and the borrow can be
                    // dropped rather than copied.
                    //
                    // Without this, fill() below sees parsedFromBuffer still set
                    // from the request just served, reads that as "midway through a
                    // request", and takes detachPreservingOffsets -- a full copy of
                    // the borrowed buffer on EVERY keep-alive request. Measured on
                    // /plaintext under virtual threads: 1989689 detaches against
                    // 2000000 reads, one 97-byte array per request, 37% of
                    // everything the route allocated. The zero-copy read was
                    // working perfectly and handing the saving straight back here.
                    //
                    // The flag's real job is the SECOND read within one request (a
                    // body arriving after its headers), where slices into this
                    // array are live and the copy is required. That case is
                    // untouched: readRequest clears the flag on entry and raises it
                    // once the header block is parsed, so it is set exactly across
                    // the window where a Request exists. This point is outside that
                    // window by construction -- the handler has returned and the
                    // response is on the wire.
                    conn.parsedFromBuffer = false;
                    more = conn.fill(scratch);
                } catch (IOException err) {
                    drop(fd);
                    return;
                }
                if(more) {
                    continue;
                }
                // Readable but nothing came: the peer closed.
                drop(fd);
                return;
            }
            break;
        }
        // Before the descriptor can be taken by another worker: the read buffer
        // belongs to THIS thread and the next request on it will overwrite these
        // bytes. Must come before reactor.add, not after -- the moment the fd is
        // registered, another worker can pick it up.
        conn.releaseBorrowed();
        try {
            // Back to the poller for the next request on this connection. Both
            // epoll_ctl and kevent are safe to call from this thread.
            if(VIRTUAL_THREADS) {
                // Reached only when the connection itself is finished: a virtual
                // thread does not come back here to wait, it parks where it waits.
                // Re-arming now would hand the poller a descriptor nobody owns.
                drop(fd);
            } else {
                ServerSocket.setBlocking(fd, false);
                armConnection(fd, false);
            }
        } catch (IOException err) {
            drop(fd);
        }
    }

    /**
     * One turn of an HTTP/2 connection: read what is available, answer every
     * request that completed, flush, and hand the descriptor back to the reactor.
     *
     * Deliberately the same shape as the HTTP/1.1 path rather than a worker that
     * owns the connection for its lifetime. h2 connections are long-lived by
     * design, so pinning a worker to each would mean the pool size is the limit on
     * concurrent clients -- the exact thing the reactor exists to avoid.
     */
    private void serveHttp2(int fd, long session, byte[] pending, int pendingLength) {
        Http2 h2;
        try {
            Object existing = http2Sessions.get(new Integer(fd));
            if(existing == null) {
                h2 = Http2.create();
                http2Sessions.put(new Integer(fd), h2);
                // The SETTINGS preface has to reach the client before anything else.
                flushHttp2(fd, session, h2);
            } else {
                h2 = (Http2)existing;
            }

            if(pending != null && pendingLength > 0) {
                // Bytes already read while deciding this was h2, preface included.
                h2.receive(pending, 0, pendingLength);
            } else {
                byte[] scratch = new byte[16384];
                int n = readFrom(fd, session, scratch, 0, scratch.length);
                if(n <= 0) {
                    drop(fd);
                    return;
                }
                h2.receive(scratch, 0, n);
            }

            Http2.Stream stream;
            while((stream = h2.nextRequest()) != null) {
                // :authority is what Host is in HTTP/1.1, so the handler sees a
                // request shaped exactly like an HTTP/1.1 one.
                Map headers = new LinkedHashMap(stream.getHeaders());
                if(stream.getAuthority() != null) {
                    headers.put("host", stream.getAuthority());
                }
                Request request = new Request(stream.getMethod(), stream.getPath(),
                        "HTTP/2", headers, stream.getBodyAsString());
                Response response;
                try {
                    response = handler.handle(request);
                    if(response == null) {
                        response = Response.text(404, "not found");
                    }
                } catch (Exception err) {
                    System.err.println("handler failed: " + err);
                    response = Response.text(500, "internal error");
                }
                byte[] body = responseBodyFor(response,
                        "HEAD".equals(stream.getMethod()));
                List extra = new java.util.ArrayList();
                if(response.extraHeaders != null) {
                    java.util.Iterator it = response.extraHeaders.keySet().iterator();
                    while(it.hasNext()) {
                        Object key = it.next();
                        Object value = response.extraHeaders.get(key);
                        if(key != null && value != null) {
                            extra.add(String.valueOf(key) + ": " + String.valueOf(value));
                        }
                    }
                }
                h2.respond(stream.getId(), response.status, response.contentType, extra, body);
                requestsServed.incrementAndGet();
            }
            flushHttp2(fd, session, h2);
            if(!h2.isAlive()) {
                drop(fd);
                return;
            }
            ServerSocket.setBlocking(fd, false);
            armConnection(fd, false);
        } catch (Exception err) {
            trace("fd=" + fd + " http/2 failed: " + err);
            drop(fd);
        }
    }

    /** The HTTP/2 connection preface, sent by a client that opens with h2. */
    private static final byte[] HTTP2_PREFACE = prefaceBytes();

    private static byte[] prefaceBytes() {
        try {
            return "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes("UTF-8");
        } catch (IOException err) {
            return new byte[0];
        }
    }

    /**
     * True while what has arrived is still consistent with the preface. Lets the
     * read loop stop early on an ordinary request rather than waiting for 24 bytes
     * that will never match -- "GET / HTTP/1.1" diverges at the second character.
     */
    private static boolean startsWithPrefacePrefix(Conn conn) {
        int have = Math.min(conn.available(), HTTP2_PREFACE.length);
        for(int iter = 0 ; iter < have ; iter++) {
            if(conn.buffer[conn.pos + iter] != HTTP2_PREFACE[iter]) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesPreface(Conn conn) {
        for(int iter = 0 ; iter < HTTP2_PREFACE.length ; iter++) {
            if(conn.buffer[conn.pos + iter] != HTTP2_PREFACE[iter]) {
                return false;
            }
        }
        return true;
    }

    private void flushHttp2(int fd, long session, Http2 h2) throws IOException {
        byte[] out = h2.drain();
        while(out != null && out.length > 0) {
            writeTo(fd, session, out, 0, out.length);
            out = h2.drain();
        }
    }

    /**
     * The response body as bytes. A file-backed response cannot use sendfile on an
     * HTTP/2 connection -- the bytes have to become DATA frames, which means they
     * have to be produced here -- so it is read in, and the descriptor is released
     * either way.
     */
    private byte[] responseBodyFor(Response response, boolean headOnly) throws IOException {
        if(response.fileFd < 0) {
            return headOnly ? new byte[0] : response.body;
        }
        try {
            if(headOnly) {
                return new byte[0];
            }
            return StaticFiles.readAll(response.fileFd, response.fileOffset, response.fileLength);
        } finally {
            StaticFiles.closeFile(response.fileFd);
        }
    }

    /**
     * HTTP/1.1 keeps the connection alive unless asked not to; HTTP/1.0 closes
     * unless asked to keep it. Treating a 1.0 client as keep-alive leaves it
     * waiting for a close that never comes.
     */
    private static boolean wantsKeepAlive(Request request) {
        // headerContains rather than getHeader, and this is the hot path.
        //
        // getHeader materialises the value: asciiString allocates a char[] AND a
        // String, and .toLowerCase() allocates a second String -- four objects per
        // request to answer a question about a fixed token. The request already
        // holds its headers as positions into the read buffer, and
        // sliceContainsIgnoreCase answers straight off those bytes, so this is the
        // one call site that was throwing that away. Measured at 662 bytes
        // allocated per /plaintext request against a 4MB trigger, which is 60-90
        // collections a second, and the collector is what costs the tail.
        //
        // Same answers as before on both branches: headerContains is false when
        // the header is absent, so 1.0 still needs an explicit keep-alive and 1.1
        // still defaults to keeping the connection.
        if("HTTP/1.0".equals(request.getVersion())) {
            return request.headerContains("connection", "keep-alive");
        }
        return !request.headerContains("connection", "close");
    }

    private void writeStatusOnly(Conn conn, int status, String message) {
        try {
            byte[] body = (message == null ? reason(status) : message)
                    .getBytes("UTF-8");
            StringBuilder head = new StringBuilder();
            head.append("HTTP/1.1 ").append(status).append(' ').append(reason(status)).append("\r\n");
            head.append("Content-Type: text/plain; charset=utf-8\r\n");
            head.append("Date: ").append(currentHttpDate()).append("\r\n");
            head.append("Content-Length: ").append(body.length).append("\r\n");
            head.append("Connection: close\r\n\r\n");
            conn.write(head.toString().getBytes("UTF-8"));
            conn.write(body);
        } catch (IOException err) {
            // The peer is already gone; there is nowhere to report this.
        }
    }

    /**
     * The methods this server routes. Anything else is 501, not a 404.
     *
     * Held as constants so a parsed method can BE one of them rather than a fresh
     * String per request.
     */
    private static final String[] KNOWN_METHODS = {
        "GET", "POST", "HEAD", "PUT", "DELETE", "PATCH", "OPTIONS"
    };

    /**
     * Reads one request. Null when the peer closed; ProtocolException when what
     * arrived is not a request this server will act on.
     */
    private Request readRequest(Conn conn, byte[] scratch) throws IOException {
        // Cleared before the header block is read and raised once it has been
        // parsed, so fill() can tell "start of a request" from "midway through
        // one" -- which an empty buffer alone cannot say.
        conn.parsedFromBuffer = false;
        int headerEnd = indexOfHeaderEnd(conn.buffer, conn.pos);
        while(headerEnd < 0) {
            if(conn.available() > MAX_HEADER_BYTES) {
                throw new ProtocolException(431, "request head too large");
            }
            if(!conn.fill(scratch)) {
                return null;
            }
            headerEnd = indexOfHeaderEnd(conn.buffer, conn.pos);
        }
        // Parsed IN PLACE, out of the array the kernel filled. Nothing here builds
        // a String for a name or a value: the header block used to become a
        // String, be split into lines, each line split again and each half
        // substring'd, lower-cased and trimmed -- about 2.6KB per request, and the
        // largest single source of allocation in this server. Names and tokens are
        // ASCII by definition, so a byte comparison with an ASCII fold is exact.
        // From here the slices below name positions in THIS array, so fill() must
        // preserve it rather than let go of the borrow.
        conn.parsedFromBuffer = true;
        byte[] raw = conn.buffer;
        int blockStart = conn.pos;
        int blockEnd = headerEnd;
        conn.pos = headerEnd + 4;

        int lineEnd = indexOfCrLfWithin(raw, blockStart, blockEnd);
        if(lineEnd < 0) {
            lineEnd = blockEnd;         // a single request line with no headers
        }
        if(lineEnd == blockStart) {
            throw new ProtocolException(400, "empty request");
        }

        int firstSpace = indexOfByte(raw, blockStart, lineEnd, (byte)' ');
        int secondSpace = firstSpace < 0 ? -1
                : indexOfByte(raw, firstSpace + 1, lineEnd, (byte)' ');
        if(firstSpace < 0 || secondSpace < 0
                || indexOfByte(raw, secondSpace + 1, lineEnd, (byte)' ') >= 0) {
            throw new ProtocolException(400, "malformed request line");
        }

        String version;
        int versionStart = secondSpace + 1;
        int versionLength = lineEnd - versionStart;
        if(sliceEquals(raw, versionStart, versionLength, "HTTP/1.1")) {
            version = "HTTP/1.1";
        } else if(sliceEquals(raw, versionStart, versionLength, "HTTP/1.0")) {
            version = "HTTP/1.0";
        } else {
            throw new ProtocolException(505, "unsupported HTTP version");
        }

        String method = knownMethod(raw, blockStart, firstSpace - blockStart);
        if(method == null) {
            // 501, not 404: the path may well exist, the verb is what is unknown.
            throw new ProtocolException(501, "unsupported method");
        }

        int targetStart = firstSpace + 1;
        int targetLength = secondSpace - targetStart;
        // Absolute-form ("GET http://host/path"), which a request through a proxy
        // uses and RFC 9112 requires a server to accept.
        if(sliceStartsWithIgnoreCase(raw, targetStart, targetLength, "http://")
                || sliceStartsWithIgnoreCase(raw, targetStart, targetLength, "https://")) {
            int schemeEnd = indexOfByte(raw, targetStart, targetStart + targetLength, (byte)':');
            int authority = schemeEnd + 3;      // past "://"
            int slash = indexOfByte(raw, authority, targetStart + targetLength, (byte)'/');
            if(slash < 0) {
                targetStart = -1;               // origin-form is just "/"
            } else {
                targetLength = targetStart + targetLength - slash;
                targetStart = slash;
            }
        }
        String target;
        if(targetStart < 0) {
            target = "/";
        } else {
            if(targetLength == 0
                    || (raw[targetStart] != '/'
                        && !(targetLength == 1 && raw[targetStart] == '*'
                             && "OPTIONS".equals(method)))) {
                throw new ProtocolException(400, "malformed request target");
            }
            target = conn.internTarget(raw, targetStart, targetLength);
        }

        // Four ints per header, into a buffer the connection reuses.
        int[] slices = conn.slices;
        int headerCount = 0;
        int at = lineEnd + 2;
        while(at < blockEnd) {
            int end = indexOfCrLfWithin(raw, at, blockEnd);
            if(end < 0) {
                end = blockEnd;
            }
            if(end == at) {
                at = end + 2;
                continue;
            }
            int first = raw[at] & 0xff;
            if(first == ' ' || first == '\t') {
                // Obsolete line folding. Two parsers disagreeing about where a
                // header ends is how a request is smuggled; RFC 9112 says reject.
                throw new ProtocolException(400, "obsolete line folding");
            }
            int colon = indexOfByte(raw, at, end, (byte)':');
            if(colon <= at) {
                throw new ProtocolException(400, "malformed header");
            }
            int nameStart = at;
            int nameEnd = colon;
            while(nameEnd > nameStart && isSpace(raw[nameEnd - 1])) {
                nameEnd--;
            }
            int valueStart = colon + 1;
            int valueEnd = end;
            while(valueStart < valueEnd && isSpace(raw[valueStart])) {
                valueStart++;
            }
            while(valueEnd > valueStart && isSpace(raw[valueEnd - 1])) {
                valueEnd--;
            }
            if(headerCount * 4 + 4 > slices.length) {
                int[] grown = new int[slices.length * 2];
                System.arraycopy(slices, 0, grown, 0, slices.length);
                slices = grown;
                conn.slices = grown;
            }
            int base = headerCount * 4;
            slices[base] = nameStart;
            slices[base + 1] = nameEnd - nameStart;
            slices[base + 2] = valueStart;
            slices[base + 3] = valueEnd - valueStart;
            headerCount++;
            at = end + 2;
        }

        Request request;
        if(POOL_REQUEST) {
            if(conn.pooledRequest == null) {
                conn.pooledRequest = new Request(method, target, version, raw, slices,
                                                 headerCount, null);
            } else {
                conn.pooledRequest.reset(conn, method, target, version, raw, slices, headerCount, null);
            }
            request = conn.pooledRequest;
        } else {
            request = new Request(method, target, version, raw, slices, headerCount, null);
        }

        int contentLengthAt = -1;
        boolean chunked = false;
        for(int iter = 0 ; iter < headerCount ; iter++) {
            int base = iter * 4;
            if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], "content-length")) {
                // Two different lengths means two readings of where this request
                // ends. Refuse rather than pick one.
                if(contentLengthAt >= 0
                        && !slicesEqual(raw, slices[contentLengthAt + 2], slices[contentLengthAt + 3],
                                        slices[base + 2], slices[base + 3])) {
                    throw new ProtocolException(400, "conflicting Content-Length");
                }
                contentLengthAt = base;
            } else if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1],
                                            "transfer-encoding")) {
                chunked = sliceContainsIgnoreCase(raw, slices[base + 2], slices[base + 3],
                                                  "chunked");
            }
        }
        // RFC 9112: an HTTP/1.1 request MUST carry Host, and a server MUST reject
        // one that does not. Routing on a name the client never sent is how a
        // request reaches the wrong virtual host.
        if("HTTP/1.1".equals(version) && request.indexOfHeader("host") < 0) {
            throw new ProtocolException(400, "missing Host header");
        }

        String contentLength = contentLengthAt < 0 ? null : "set";
        int declaredLength = contentLengthAt < 0 ? -1
                : sliceToInt(raw, slices[contentLengthAt + 2], slices[contentLengthAt + 3]);

        if(chunked && contentLength != null) {
            // Both framings in one request is precisely how a request is smuggled
            // past a proxy that believes one and a server that believes the other.
            throw new ProtocolException(400, "both Content-Length and Transfer-Encoding");
        }

        if(request.headerContains("expect", "100-continue")) {
            // The client is entitled to wait for this before sending the body. A
            // server that stays silent makes every such client pay its whole
            // timeout first.
            conn.write(CONTINUE_100);
        }

        String body = null;
        if(chunked) {
            byte[] decoded = readChunked(conn, scratch);
            if(decoded == null) {
                return null;
            }
            body = decoded.length == 0 ? null : new String(decoded, "UTF-8");
        } else if(contentLength != null) {
            // sliceToInt returns -1 for anything that is not a plain non-negative
            // decimal, which covers the malformed and the negative cases the two
            // separate checks here used to make after parsing.
            if(declaredLength < 0) {
                throw new ProtocolException(400, "malformed Content-Length");
            }
            if(declaredLength > MAX_BODY_BYTES) {
                throw new ProtocolException(413, "request body too large");
            }
            while(conn.available() < declaredLength) {
                if(!conn.fill(scratch)) {
                    return null;
                }
            }
            if(declaredLength > 0) {
                body = new String(conn.buffer, conn.pos, declaredLength, "UTF-8");
                conn.pos += declaredLength;
            }
        }
        // The body is the only field not known when the header block was parsed.
        // Both branches below run during PARSING -- before this Request is handed
        // to a handler -- so neither one mutates anything a handler can see, and
        // "immutable to its handler" is preserved either way. The slices and the
        // array are shared, not copied.
        if(body == null) {
            return request;
        }
        if(POOL_REQUEST) {
            request.reset(conn, method, target, version, raw, slices, headerCount, body);
            return request;
        }
        return new Request(method, target, version, raw, slices, headerCount, body);
    }

    /**
     * Decodes a chunked body: a size in hex, CRLF, that many bytes, CRLF, until a
     * zero-length chunk. The trailer section after it is consumed and discarded --
     * ignoring trailers is allowed, but leaving them in the stream would
     * desynchronise the next request on a keep-alive connection.
     */
    private byte[] readChunked(Conn conn, byte[] scratch) throws IOException {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        while(true) {
            int lineEnd = indexOfCrLf(conn.buffer, conn.pos);
            while(lineEnd < 0) {
                if(!conn.fill(scratch)) {
                    return null;
                }
                lineEnd = indexOfCrLf(conn.buffer, conn.pos);
            }
            String sizeLine = new String(conn.buffer, conn.pos, lineEnd - conn.pos, "UTF-8");
            // A chunk-size may carry extensions after a ';'; the size is before it.
            int semi = sizeLine.indexOf(';');
            if(semi >= 0) {
                sizeLine = sizeLine.substring(0, semi);
            }
            int size;
            try {
                size = Integer.parseInt(sizeLine.trim(), 16);
            } catch (NumberFormatException err) {
                throw new ProtocolException(400, "malformed chunk size");
            }
            if(size < 0) {
                throw new ProtocolException(400, "negative chunk size");
            }
            conn.pos = lineEnd + 2;
            if(size == 0) {
                // Trailers, terminated by a bare CRLF.
                while(true) {
                    int trailerEnd = indexOfCrLf(conn.buffer, conn.pos);
                    while(trailerEnd < 0) {
                        if(!conn.fill(scratch)) {
                            return body.toByteArray();
                        }
                        trailerEnd = indexOfCrLf(conn.buffer, conn.pos);
                    }
                    boolean blank = trailerEnd == conn.pos;
                    conn.pos = trailerEnd + 2;
                    if(blank) {
                        return body.toByteArray();
                    }
                }
            }
            if(body.size() + size > MAX_BODY_BYTES) {
                throw new ProtocolException(413, "chunked body too large");
            }
            // The chunk and its trailing CRLF must both be present before it is taken.
            while(conn.available() < size + 2) {
                if(!conn.fill(scratch)) {
                    return null;
                }
            }
            body.write(conn.buffer, conn.pos, size);
            conn.pos += size;
            if(conn.buffer[conn.pos] != '\r' || conn.buffer[conn.pos + 1] != '\n') {
                throw new ProtocolException(400, "malformed chunk terminator");
            }
            conn.pos += 2;
        }
    }

    private static int indexOfCrLf(byte[] data, int from) {
        for(int iter = from ; iter + 1 < data.length ; iter++) {
            if(data[iter] == '\r' && data[iter + 1] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    /**
     * The Date header value, formatted at most once a second.
     *
     * The header has one-second resolution, so formatting it per response is work
     * whose result is identical for every request in the same second -- and at
     * these rates that is thousands of them. Two threads racing here both compute
     * the same string for the same second, so the only cost of the race is a
     * duplicated format, never a wrong value.
     */
    private static volatile long dateStampSecond = -1;
    private static volatile String dateStampValue;
    /**
     * The same stamp as bytes, so writing it costs a copy rather than a
     * per-character conversion. An HTTP date is fixed width and ASCII, which is
     * what makes the length a constant.
     */
    private static volatile byte[] dateStampBytes = new byte[HTTP_DATE_LENGTH];

    static String currentHttpDate() {
        refreshHttpDate();
        return dateStampValue;
    }

    static byte[] currentHttpDateBytes() {
        refreshHttpDate();
        return dateStampBytes;
    }

    private static void refreshHttpDate() {
        long millis = System.currentTimeMillis();
        long second = millis / 1000L;
        if(second != dateStampSecond) {
            String formatted = Http1Date.format(second * 1000L);
            byte[] bytes = new byte[HTTP_DATE_LENGTH];
            // Fixed width by construction; a formatter that ever returned another
            // length would otherwise write a short or truncated date silently.
            if(formatted.length() != HTTP_DATE_LENGTH) {
                throw new IllegalStateException("HTTP date is not "
                        + HTTP_DATE_LENGTH + " characters: " + formatted);
            }
            for(int iter = 0 ; iter < HTTP_DATE_LENGTH ; iter++) {
                bytes[iter] = (byte)formatted.charAt(iter);
            }
            dateStampValue = formatted;
            dateStampBytes = bytes;
            dateStampSecond = second;
        }
    }

    private void writeResponse(Conn conn, int fd, long session, Response response,
            boolean keepAlive, boolean headOnly) throws IOException {
        // A deferred JSON body is serialised FIRST: Content-Length has to be
        // written before it, and the only honest way to know it is to have the
        // bytes. Into a second reusable buffer rather than the head's, because
        // the head is not built yet.
        byte[] deferred = null;
        int deferredLength = 0;
        if(response.hasDeferredJson) {
            conn.bodySink.reset();
            Json.write(response.deferredJson, conn.bodySink);
            deferred = conn.bodySink.bytes();
            deferredLength = conn.bodySink.length();
        }
        long bodyLength = response.fileFd >= 0 ? response.fileLength
                : (deferred != null ? deferredLength : response.body.length);

        // Assembled into the connection's own buffer, as bytes, with no
        // intermediate String. See Conn.out: the StringBuilder-to-String-to-bytes
        // chain this replaces was the largest single source of allocation in the
        // server, and the buffer is reused for the life of the connection.
        conn.reset();
        // Pre-encoded, not re-encoded per response. put(String) walks the string
        // one charAt at a time; these literals are 82 of the ~97 characters a
        // plaintext 200 emits, so writing them that way spent 51 MILLION charAt
        // calls a second at this server's throughput to reproduce bytes that never
        // change. put(byte[]) is a System.arraycopy. Measured before this: 1.36us
        // of user CPU per request against fasthttp's 0.74us, with system time at
        // parity -- the gap was all in our own code, and this is the largest
        // identifiable piece of it.
        if(FAST_HEADERS) {
            if(response.status == 200) {
                conn.put(H_STATUS_200, 0, H_STATUS_200.length);   // overwhelmingly the common case
            } else {
                conn.put(H_VERSION, 0, H_VERSION.length);
                conn.putNumber(response.status);
                conn.put(' ');
                conn.put(reason(response.status));
            }
            conn.put(H_CTYPE, 0, H_CTYPE.length);
            conn.putContentType(response.contentType);
            // RFC 9110 6.6.1: an origin server with a clock MUST send Date.
            conn.put(H_DATE, 0, H_DATE.length);
            conn.put(currentHttpDateBytes(), 0, HTTP_DATE_LENGTH);
            // Always an explicit length: without it a keep-alive client waits for
            // a close that is not coming.
            conn.put(H_CLEN, 0, H_CLEN.length);
            conn.putNumber(bodyLength);
            if(keepAlive) {
                conn.put(H_KEEPALIVE, 0, H_KEEPALIVE.length);
            } else {
                conn.put(H_CLOSE, 0, H_CLOSE.length);
            }
        } else {
            // The per-character path this replaces, kept so the two can be
            // measured against each other in one binary.
            conn.put("HTTP/1.1 ");
            conn.putNumber(response.status);
            conn.put(' ');
            conn.put(reason(response.status));
            conn.put("\r\nContent-Type: ");
            conn.put(response.contentType);
            conn.put("\r\nDate: ");
            conn.put(currentHttpDateBytes(), 0, HTTP_DATE_LENGTH);
            conn.put("\r\nContent-Length: ");
            conn.putNumber(bodyLength);
            conn.put(keepAlive ? "\r\nConnection: keep-alive" : "\r\nConnection: close");
        }
        if(response.extraHeaders != null) {
            java.util.Iterator it = response.extraHeaders.keySet().iterator();
            while(it.hasNext()) {
                Object key = it.next();
                Object value = response.extraHeaders.get(key);
                if(key != null && value != null) {
                    conn.put("\r\n");
                    conn.put(String.valueOf(key));
                    conn.put(": ");
                    conn.put(String.valueOf(value));
                }
            }
        }
        if(FAST_HEADERS) {
            conn.put(H_END, 0, H_END.length);
        } else {
            conn.put("\r\n\r\n");
        }

        // Head and body in ONE write when the body is small and already in memory.
        // Two writes are two syscalls and, on a fresh connection, two segments: the
        // client sees the headers, acknowledges, and only then gets the body.
        // Measured against Go, which does one write per response, this was half of
        // our remaining syscall count per request. Above the threshold the copy
        // would cost more than the syscall it saves, and a file body never enters
        // user space at all -- both keep the two-write path.
        if(deferred != null) {
            if(!headOnly && deferredLength > 0) {
                conn.put(deferred, 0, deferredLength);
            }
            writeTo(fd, session, conn.out, 0, conn.outLength);
            return;
        }
        if(response.fileFd < 0 && !headOnly
                && response.body.length > 0
                && response.body.length <= COMBINED_WRITE_LIMIT) {
            conn.put(response.body, 0, response.body.length);
            writeTo(fd, session, conn.out, 0, conn.outLength);
            return;
        }
        writeTo(fd, session, conn.out, 0, conn.outLength);

        if(response.fileFd >= 0) {
            try {
                if(!headOnly) {
                    StaticFiles.sendBody(fd, session, response.fileFd, response.fileOffset, response.fileLength);
                }
            } finally {
                // The server owns the descriptor once a handler hands it over, so
                // this is the only place it is closed -- including when the send
                // failed halfway.
                StaticFiles.closeFile(response.fileFd);
            }
            return;
        }
        if(!headOnly && response.body.length > 0) {
            writeTo(fd, session, response.body, 0, response.body.length);
        }
    }

    /** Pre-encoded: this goes out on the body path of every expecting client. */
    private static final byte[] CONTINUE_100 = asciiBytes("HTTP/1.1 100 Continue\r\n\r\n");

    /**
     * The response head's fixed bytes, encoded once at class init instead of
     * character by character per response. CN1_HTTP_FAST_HEADERS=0 restores the
     * per-character path, which is what the measurement compares against.
     */
    private static final boolean FAST_HEADERS = envInt("CN1_HTTP_FAST_HEADERS", 1) != 0;
    private static final byte[] H_STATUS_200 = asciiBytes("HTTP/1.1 200 OK");
    private static final byte[] H_VERSION    = asciiBytes("HTTP/1.1 ");
    private static final byte[] H_CTYPE      = asciiBytes("\r\nContent-Type: ");
    private static final byte[] H_DATE       = asciiBytes("\r\nDate: ");
    private static final byte[] H_CLEN       = asciiBytes("\r\nContent-Length: ");
    private static final byte[] H_KEEPALIVE  = asciiBytes("\r\nConnection: keep-alive");
    private static final byte[] H_CLOSE      = asciiBytes("\r\nConnection: close");
    private static final byte[] H_END        = asciiBytes("\r\n\r\n");

    private static byte[] asciiBytes(String value) {
        byte[] out = new byte[value.length()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte)value.charAt(iter);
        }
        return out;
    }

    private static boolean isSpace(byte b) {
        return b == ' ' || b == '\t';
    }

    static int indexOfByte(byte[] data, int from, int to, byte wanted) {
        for(int iter = from ; iter < to ; iter++) {
            if(data[iter] == wanted) {
                return iter;
            }
        }
        return -1;
    }

    static int indexOfCrLfWithin(byte[] data, int from, int to) {
        for(int iter = from ; iter + 1 < to ; iter++) {
            if(data[iter] == '\r' && data[iter + 1] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    static boolean sliceStartsWithIgnoreCase(byte[] data, int start, int length, String ascii) {
        return length >= ascii.length()
                && sliceEqualsIgnoreCase(data, start, ascii.length(), ascii);
    }

    static boolean slicesEqual(byte[] data, int aStart, int aLength, int bStart, int bLength) {
        if(aLength != bLength) {
            return false;
        }
        for(int iter = 0 ; iter < aLength ; iter++) {
            if(data[aStart + iter] != data[bStart + iter]) {
                return false;
            }
        }
        return true;
    }

    // ---- byte-slice helpers -------------------------------------------------
    //
    // Header names and the tokens compared against them are ASCII by definition
    // (RFC 9110 field-name is a token), so a byte-wise comparison with an ASCII
    // fold is exact -- no locale, no decoding, no allocation. These are what let
    // the request path answer "is this connection keep-alive" without building a
    // String.

    private static int foldAscii(int c) {
        return c >= 'A' && c <= 'Z' ? c + ('a' - 'A') : c;
    }

    /**
     * The case-folded bytes of an ASCII string, or null if it is not ASCII.
     *
     * Cached per String IDENTITY, because the callers pass literals: "connection"
     * at a given call site is the same object every time, so the fold happens once
     * for the life of the process rather than once per header per request. A miss
     * simply folds again -- the cache is a hint, never a correctness dependency,
     * which is what lets it stay lock-free.
     */
    /**
     * Case-folded header names, packed END TO END in ONE byte[].
     *
     * Flat on purpose. An array of arrays scatters every entry across the heap and
     * costs a pointer chase per lookup; this holds all of them contiguously, so a
     * comparison walks memory the prefetcher already has. It is also one object for
     * the collector to mark instead of seventeen.
     *
     * Keyed by String IDENTITY, because the callers pass literals -- "connection" at
     * a given call site is the same object every time, so the fold happens once for
     * the life of the process rather than once per header per request. A miss simply
     * folds again: the cache is a hint, never a correctness dependency, which is
     * what lets it stay lock-free.
     */
    private static final int FOLD_CACHE_SLOTS = 16;
    private static final int FOLD_STORE_BYTES = 512;
    private static final String[] foldKeys = new String[FOLD_CACHE_SLOTS];
    private static final int[] foldStart = new int[FOLD_CACHE_SLOTS];
    private static final int[] foldLength = new int[FOLD_CACHE_SLOTS];
    private static final byte[] foldStore = new byte[FOLD_STORE_BYTES];
    private static int foldNext;
    private static int foldUsed;

    /**
     * Folds `ascii` into {@link #foldStore} and returns its slot, or -1 when the
     * name is not ASCII (the caller then takes the general path).
     */
    static int foldedSlot(String ascii) {
        for(int iter = 0 ; iter < FOLD_CACHE_SLOTS ; iter++) {
            if(foldKeys[iter] == ascii) {
                return iter;
            }
        }
        int length = ascii.length();
        if(length > FOLD_STORE_BYTES) {
            return -1;
        }
        if(foldUsed + length > FOLD_STORE_BYTES) {
            foldUsed = 0;                    // wrap; stale slots are re-folded on miss
        }
        int at = foldUsed;
        for(int iter = 0 ; iter < length ; iter++) {
            char c = ascii.charAt(iter);
            if(c > 127) {
                return -1;
            }
            foldStore[at + iter] = (byte) foldAscii(c);
        }
        foldUsed = at + length;
        int slot = foldNext;
        foldNext = (slot + 1) % FOLD_CACHE_SLOTS;
        // Bounds before key: a reader that matches the key must see them complete.
        foldStart[slot] = at;
        foldLength[slot] = length;
        foldKeys[slot] = ascii;
        return slot;
    }

    /** Case-insensitive compare of a slice against an already-folded cache slot. */
    static boolean sliceEqualsFolded(byte[] data, int start, int length, int slot) {
        int needle = foldLength[slot];
        if(length != needle) {
            return false;
        }
        int at = foldStart[slot];
        for(int iter = 0 ; iter < length ; iter++) {
            if(foldAscii(data[start + iter] & 0xff) != foldStore[at + iter]) {
                return false;
            }
        }
        return true;
    }

    static boolean sliceEqualsIgnoreCase(byte[] data, int start, int length, String ascii) {
        if(length != ascii.length()) {
            return false;
        }
        for(int iter = 0 ; iter < length ; iter++) {
            if(foldAscii(data[start + iter] & 0xff) != foldAscii(ascii.charAt(iter))) {
                return false;
            }
        }
        return true;
    }

    static boolean sliceContainsIgnoreCase(byte[] data, int start, int length, String ascii) {
        int needle = ascii.length();
        if(needle == 0 || needle > length) {
            return needle == 0;
        }
        int last = start + length - needle;
        for(int at = start ; at <= last ; at++) {
            int iter = 0;
            while(iter < needle
                    && foldAscii(data[at + iter] & 0xff) == foldAscii(ascii.charAt(iter))) {
                iter++;
            }
            if(iter == needle) {
                return true;
            }
        }
        return false;
    }

    /**
     * A non-negative decimal from a slice, or -1 when it is not one.
     *
     * Integer.parseInt would need a String first, which is the allocation this
     * whole representation exists to avoid -- and it is on the path of every
     * request that carries a body.
     */
    static int sliceToInt(byte[] data, int start, int length) {
        if(length <= 0 || length > 10) {
            return -1;
        }
        long value = 0;
        for(int iter = 0 ; iter < length ; iter++) {
            int c = data[start + iter] & 0xff;
            if(c < '0' || c > '9') {
                return -1;
            }
            value = value * 10 + (c - '0');
            if(value > Integer.MAX_VALUE) {
                return -1;
            }
        }
        return (int)value;
    }

    /**
     * The request target as a String, memoised PER CONNECTION.
     *
     * A connection asks for the same handful of targets over and over, so this is a
     * hit almost every time and the steady state allocates nothing. A miss does
     * exactly what the code did before and is only slower, never wrong.
     *
     * Worth doing because the target was the last per-request String on the
     * plaintext path, and it cost three objects rather than one: asciiString builds
     * a char[] and String's public constructor copies it into a second.
     *
     * PER CONNECTION rather than one shared static table, and that is a
     * correctness requirement rather than a preference. `java.lang.String.value` is
     * NOT final in this runtime (only offset and count are), so a String published
     * through an unsynchronised static array can be observed by another worker with
     * a null value -- on arm64 that is a real reordering, not a theoretical one. A
     * Conn reaches its next worker through the executor, which gives the
     * happens-before edge this needs for free.
     *
     * Bounded, because targets are attacker controlled: a query string or path
     * parameter makes every request unique. Past the cap it stops inserting and
     * every miss allocates as before -- a performance cliff, never a memory one.
     */
    // DEFAULT OFF. Measured against the same binary at 16 connections, the cache
    // is worth +24% when the zero-copy read is on (159,291 vs 128,111) and -6%
    // when it is off (172,681 vs 183,406). Since the zero-copy read is itself off
    // by default, the case that applies is the one where this costs throughput.
    // Kept behind a switch rather than deleted because the allocation it removes is
    // real -- 2 char[] and a String per request -- and a cheaper lookup might yet
    // win; what is NOT supported is turning it on without re-measuring.
    /**
     * On by default. Sixty-four slots per connection, one reference each.
     *
     * With this at 0 internTarget takes its disabled path and calls asciiString
     * for EVERY request, which allocates a char[], a String and the String's own
     * storage. A per-class allocation profile of /plaintext at 64 connections put
     * char[] + String + byte[] at 57% of all bytes allocated -- 424MB, 123MB and
     * 302MB against a 1.49GB total -- and the request target is the only thing
     * left materialising on that path once the keep-alive check stopped doing it.
     *
     * A benchmark client sends a handful of distinct targets, and a real service
     * has a bounded route set, so the slot array is small and the hit rate is
     * high. 64 references per connection is 512 bytes, against the ~180 bytes per
     * REQUEST the miss path was costing.
     *
     * MEASURED: paired A/B, arms alternated inside each rep, six readings at 64
     * and 256 connections -- +7% to +13% throughput, 6 of 6 in favour, p99 better
     * in 5 of 6, and allocation 639 -> 411 bytes per request with String
     * allocations falling from 2,575,425 to 542. The backend suite passed twice.
     *
     * NOT MEASURED: a workload whose targets are all DISTINCT, which is what
     * query strings produce and what an attacker can force. Five attempts to
     * measure it failed for harness reasons rather than server ones -- 404s reply
     * Connection: close so varied targets tore down the connection, and driving
     * wrk from Lua produced 1.5M write errors against 20k requests. The arm is
     * still worth building if this ever looks suspect.
     *
     * What the MISS path costs, from the code rather than a measurement: a hash
     * over the target, a length compare that fails immediately, then exactly the
     * asciiString the disabled path performs, plus a reference store. So a miss
     * adds one pass over a short byte range and allocates nothing extra -- it
     * cannot allocate MORE than the cache being off, because it stores the very
     * String that path would have created. That bounds the worst case to a small
     * constant, which is why this ships on rather than off.
     *
     * CN1_HTTP_TARGET_CACHE=0 restores the old behaviour for A/B.
     */
    private static final int TARGET_CACHE_SLOTS =
            envInt("CN1_HTTP_TARGET_CACHE", 64);

    /**
     * Read straight into the thread's reusable buffer instead of a fresh array.
     * A switch because it is the kind of change that has to be A/B measurable
     * against the allocation it removes -- an optimisation that costs more than it
     * saves looks exactly like one that works until somebody measures the thing it
     * was supposed to improve.
     */
    /**
     * Read straight into the thread's reusable buffer instead of a fresh array.
     *
     * ON by default. It removes 95% of the per-request byte[] allocations
     * (1.04 to 0.054 per request).
     *
     * MODES, because the throughput answer turned out to depend on the route and
     * two earlier readings here were both wrong:
     *
     *   0  read with recv() into the worker's reusable scratch, then copy into a
     *      fresh byte[] sized to the read. Allocates once per request.
     *   1  read straight into this thread's foreign (off-heap) buffer and parse
     *      where the bytes land. Allocates nothing.
     *   2  DIAGNOSTIC. Identical native to mode 1, identical Java to mode 0: read
     *      into the foreign buffer and immediately copy it into a heap array. It
     *      exists to split mode 1's two differences from mode 0 -- the syscall and
     *      the off-heap object living in a Java field -- because measuring only 0
     *      against 1 cannot say which of them moved the number.
     *
     * Paired measurement on an idle Linux box, same binary, 3 reps, median req/s:
     *
     *   /plaintext   mode 0 = 262961   mode 1 = 233800   mode 1 is 11% SLOWER
     *   /json        mode 0 = 167851   mode 1 = 176876   mode 1 is  5% FASTER
     *
     * That split is the whole reason the modes are here. Earlier comments in this
     * spot claimed first a flat 30% loss and then no cost at all; the first was
     * measured against a machine running a compile, the second was an A/B too
     * noisy to resolve an 11% effect and should have been reported as a failed
     * measurement rather than a result.
     *
     * WHAT THE BISECTION FOUND. Mode 2 lands on mode 0 on BOTH routes -- 250962
     * against 249524 on /plaintext, 159168 against 158972 on /json -- so the
     * native read costs nothing and is not what moved either number. Since mode 2
     * differs from mode 1 only in copying the bytes into a heap array, the whole
     * effect, in both directions, is the cost of keeping a FOREIGN off-heap array
     * in a Java field:
     *
     *   /json        mode 1 gains 5.3% over mode 2 -- the route is allocation
     *                bound, so not allocating a per-request array is worth more
     *                than the collector's extra work.
     *   /plaintext   mode 1 loses 4.2% to mode 2 -- little GC pressure here, so
     *                the saved allocation buys little while the off-heap cost is
     *                paid on every traversal: `Conn.buffer` points outside the
     *                heap, so the fast range check in the mark path fails and the
     *                object has to be resolved as an immortal root instead.
     *
     * The default is therefore a judgement about the workload rather than a fact
     * about the code, which is why the switch is left in place.
     */
    private static final int ZERO_COPY_MODE = envInt("CN1_HTTP_ZERO_COPY", 1);
    /**
     * On under virtual threads too, since the reason it was not is gone.
     *
     * WHAT THIS USED TO SAY, AND WHY IT WAS WRONG. It read that combining the two
     * was unsafe because the zero-copy read hands back a HOST thread's buffer and
     * a virtual thread could resume elsewhere, and it cited an attempt that
     * "passed the virtual-thread suite 21/21 and then produced a TRUNCATED
     * RESPONSE on the dispatching path: authGuardsMutatingRoutes read a reply it
     * could not parse, once".
     *
     * That truncation was not the buffer refactor. It was the missing
     * parsedFromBuffer guard in fill() -- see the comment there, which records
     * the same two symptoms (transactionRollsBack timing out at 15.05s with
     * status -1, authGuardsMutatingRoutes reading an empty body, about 2 runs in
     * 6) and says in as many words that it "never appeared under virtual threads
     * because ZERO_COPY_READ is off there". Turning zero-copy on under virtual
     * threads is exactly what first exposed that bug; the refactor was blamed,
     * reverted, and the real defect found and fixed afterwards without anyone
     * going back to correct the verdict.
     *
     * The sharing hazard is handled by that same guard rather than by keeping the
     * paths apart. Several virtual threads do multiplex onto one host thread, but
     * fill() either releases the borrow when nothing has been parsed out of it or
     * calls detachPreservingOffsets before any second read, so a virtual thread
     * that parks mid-request already owns a private copy and no other thread's
     * read can overwrite live slices.
     *
     * What it costs to leave off: a per-request byte[] copy on every request. The
     * census measured 223 bytes per request when this path was copying and none
     * when it was not, against 662 bytes per request total on /plaintext.
     *
     * CN1_HTTP_ZERO_COPY=0 still disables it entirely.
     */
    private static final boolean ZERO_COPY_READ = ZERO_COPY_MODE != 0;

    /**
     * Reuse one Request per connection instead of allocating one per request.
     *
     * Request and Response were the whole of what /plaintext still allocated once
     * the borrowed-buffer copy went -- 80 and 88 bytes, one of each, every
     * request -- so this is half of what was left. The allocation half is exact
     * and was measured directly: Request disappears from the profile and the
     * route falls from 168.2 to 88.2 bytes per request. Throughput, thirteen
     * interleaved pairs in one binary with the arm order rotating, is a median
     * +13.6% and ahead in 12 of 13, p99 better in 10.
     *
     * A profiled build shows only +2.3% for the same change, and that is not a
     * contradiction: the profiler taxes every allocation, so the server is slower,
     * allocates less per second, and the collector it is being spared matters
     * less. The non-profiled figure is the one that describes a real deployment.
     *
     * CN1_HTTP_POOL_REQUEST=0 restores the allocating path -- kept for the same
     * reason ZERO_COPY_MODE keeps its switch, so the comparison stays runnable
     * rather than having to be rebuilt.
     */
    private static final boolean POOL_REQUEST = envInt("CN1_HTTP_POOL_REQUEST", 1) != 0;

    static String asciiString(byte[] data, int start, int length) {
        char[] chars = new char[length];
        for(int iter = 0 ; iter < length ; iter++) {
            chars[iter] = (char)(data[start + iter] & 0xff);
        }
        return new String(chars);
    }

    static String lowerCaseString(byte[] data, int start, int length) {
        char[] chars = new char[length];
        for(int iter = 0 ; iter < length ; iter++) {
            chars[iter] = (char)foldAscii(data[start + iter] & 0xff);
        }
        return new String(chars);
    }

    /**
     * The interned constant for a known method, or null.
     *
     * Returning a constant rather than a fresh String means the common methods
     * cost nothing, and it makes the identity comparisons elsewhere in this file
     * safe as well as the equals ones.
     */
    static String knownMethod(byte[] data, int start, int length) {
        for(int iter = 0 ; iter < KNOWN_METHODS.length ; iter++) {
            if(sliceEqualsIgnoreCase(data, start, length, KNOWN_METHODS[iter])
                    && sliceEquals(data, start, length, KNOWN_METHODS[iter])) {
                return KNOWN_METHODS[iter];
            }
        }
        return null;
    }

    /** Exact, not folded: HTTP methods are case SENSITIVE. */
    private static boolean sliceEquals(byte[] data, int start, int length, String ascii) {
        if(length != ascii.length()) {
            return false;
        }
        for(int iter = 0 ; iter < length ; iter++) {
            if((data[start + iter] & 0xff) != ascii.charAt(iter)) {
                return false;
            }
        }
        return true;
    }

    private static String reason(int status) {
        switch(status) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 413: return "Payload Too Large";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            default: return status < 400 ? "OK" : "Error";
        }
    }

    private static int indexOfHeaderEnd(byte[] data, int from) {
        for(int iter = from ; iter + 3 < data.length ; iter++) {
            if(data[iter] == '\r' && data[iter + 1] == '\n'
                    && data[iter + 2] == '\r' && data[iter + 3] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    private static String[] splitLines(String value) {
        List parts = new ArrayList();
        int pos = 0;
        while(true) {
            int next = value.indexOf("\r\n", pos);
            if(next < 0) {
                if(pos < value.length()) {
                    parts.add(value.substring(pos));
                }
                break;
            }
            parts.add(value.substring(pos, next));
            pos = next + 2;
        }
        String[] out = new String[parts.size()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (String)parts.get(iter);
        }
        return out;
    }

    private static String[] splitOn(String value, char sep) {
        List parts = new ArrayList();
        int pos = 0;
        while(true) {
            int next = value.indexOf(sep, pos);
            if(next < 0) {
                parts.add(value.substring(pos));
                break;
            }
            parts.add(value.substring(pos, next));
            pos = next + 1;
        }
        String[] out = new String[parts.size()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (String)parts.get(iter);
        }
        return out;
    }
}
