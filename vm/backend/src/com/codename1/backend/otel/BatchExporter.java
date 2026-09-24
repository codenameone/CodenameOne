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
package com.codename1.backend.otel;

import com.codename1.backend.Tracing;
import com.codename1.backend.Web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Batches ended spans and posts them to the collector, on a thread of its own.
 *
 * <p>A PLATFORM THREAD, never a virtual one, and never the request's. Outbound
 * HTTP on the packaged runtime is libcurl, which blocks the OS thread it runs on
 * -- and a virtual thread's OS thread is the host carrying every other connection
 * pinned to it. An export done inline would stall all of them for as long as the
 * collector took to answer; one done on a virtual thread would do the same to
 * whichever host it landed on. Here a slow or absent collector costs this thread
 * and nothing else.
 *
 * <p>BOUNDED. The queue holds at most {@code maxQueue} spans, and a span that
 * arrives when it is full is dropped and counted. A collector that is down must
 * degrade to missing traces, never to a server that runs out of memory holding
 * them. Relayed client payloads are bounded the same way, by bytes.
 *
 * <p>The request path only ever takes the lock for an append.
 */
final class BatchExporter implements Runnable {
    /** Relayed payloads posted per round before the server's own spans get a turn. */
    static final int RELAY_PER_ROUND = 8;

    private final Object lock = new Object();
    private final String endpoint;
    private final List headers;
    private final boolean protobuf;
    private final Map resource;
    private final int maxQueue;
    private final int maxBatch;
    private final long delayMillis;
    private final long maxRelayBytes;

    private ArrayList queue = new ArrayList();
    /** Each an Object[] {byte[] body, String contentType}: payloads the relay accepted. */
    private ArrayList relayed = new ArrayList();
    private long relayedBytes;
    /*
     * Watermarks for flush(). Items leave each queue in order, so "everything
     * queued before the flush has gone" is exactly "the drained count has reached
     * the enqueued count the flush saw" -- which stays true on a busy server whose
     * queue is never empty, where waiting for an empty queue would not.
     */
    private long enqueuedSpans;
    private long drainedSpans;
    private long enqueuedRelayed;
    private long drainedRelayed;
    /** The furthest watermarks any waiting flush asked for; the thread exports promptly until it reaches them. */
    private long flushSpans;
    private long flushRelayed;
    private boolean stopping;
    private boolean stopped;
    /**
     * Nothing is posted before this time: set after a retryable failure (429, a
     * 5xx gateway answer, no connection) so the retry waits out a backoff. Waited
     * for on the lock, never slept through -- shutdown's notifyAll ends it at once.
     */
    private long retryAt;
    private Thread thread;

    // Counters for the metrics snapshot. Written under the lock.
    private long exportedSpans;
    private long droppedSpans;
    private long failedExports;
    private long rejectedSpans;
    /** Spans the collector rejected in the last successful POST; see post(). */
    private long lastRejected;
    private long relayedPayloads;
    private long droppedRelayed;
    private String lastError;

    BatchExporter(String endpoint, List headers, boolean protobuf, Map resource,
                  int maxQueue, int maxBatch, long delayMillis, long maxRelayBytes) {
        this.endpoint = endpoint;
        this.headers = headers;
        this.protobuf = protobuf;
        this.resource = resource;
        this.maxQueue = maxQueue;
        this.maxBatch = maxBatch;
        this.delayMillis = delayMillis;
        this.maxRelayBytes = maxRelayBytes;
    }

    boolean isProtobuf() {
        return protobuf;
    }

    void start() {
        Thread t = new Thread(this, "cn1-otel-exporter");
        // So a JVM run of the server can exit: the exporter must never be the
        // reason a process that finished stays up.
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    /** From the request path, as a span ends. */
    void add(OtelSpan span) {
        synchronized(lock) {
            // STOPPING counts as closed, not just stopped: shutdown() has already
            // dropped the queue, and a span that ends after it -- a request still
            // in flight when its tracer was replaced -- would otherwise be taken
            // and exported by a worker that is meant to be finishing.
            if(stopped || stopping || queue.size() >= maxQueue) {
                droppedSpans++;
                return;
            }
            queue.add(span);
            enqueuedSpans++;
            if(queue.size() >= maxBatch) {
                lock.notifyAll();
            }
        }
    }

    /**
     * From the relay: a client's export, already re-encoded. False when the
     * relay's byte budget is spent, which the relay answers with 503 so the client
     * backs off rather than resending into a full queue.
     */
    boolean addRelayed(byte[] body, String contentType) {
        synchronized(lock) {
            if(stopped || stopping || relayedBytes + body.length > maxRelayBytes) {
                droppedRelayed++;
                return false;
            }
            // The third slot marks a payload that has had its one retry.
            relayed.add(new Object[] {body, contentType, null});
            relayedBytes += body.length;
            enqueuedRelayed++;
            lock.notifyAll();
            return true;
        }
    }

    /** Blocks until everything queued before the call is exported, or the time is up. */
    void flush(int timeoutMillis) {
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMillis);
        synchronized(lock) {
            if(thread == null || stopped) {
                return;
            }
            long spans = enqueuedSpans;
            long payloads = enqueuedRelayed;
            flushSpans = Math.max(flushSpans, spans);
            flushRelayed = Math.max(flushRelayed, payloads);
            lock.notifyAll();
            while((drainedSpans < spans || drainedRelayed < payloads) && !stopped) {
                long left = deadline - System.currentTimeMillis();
                if(left <= 0) {
                    return;
                }
                try {
                    lock.wait(left);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Flushes for at most {@code timeoutMillis}, then stops the thread. What the
     * flush could not export in that time is DROPPED and counted, not drained:
     * waiting for an empty queue kept a replaced tracer posting every remaining
     * batch -- retries included -- with its old credentials for as long as a slow
     * or absent collector took, while its caller had been promised a bounded stop.
     */
    void shutdown(int timeoutMillis) {
        flush(timeoutMillis);
        synchronized(lock) {
            stopping = true;
            droppedSpans += queue.size();
            queue.clear();
            droppedRelayed += relayed.size();
            relayed.clear();
            relayedBytes = 0;
            lock.notifyAll();
        }
    }

    private boolean isStopping() {
        synchronized(lock) {
            return stopping;
        }
    }

    void metrics(Map out) {
        synchronized(lock) {
            out.put("tracing", stopped ? "stopped" : "on");
            out.put("spansExported", Long.valueOf(exportedSpans));
            out.put("spansDropped", Long.valueOf(droppedSpans));
            out.put("spansQueued", Integer.valueOf(queue.size()));
            out.put("traceExportsFailed", Long.valueOf(failedExports));
            if(rejectedSpans > 0) {
                out.put("spansRejected", Long.valueOf(rejectedSpans));
            }
            if(relayedPayloads > 0 || droppedRelayed > 0) {
                out.put("clientExportsRelayed", Long.valueOf(relayedPayloads));
                out.put("clientExportsDropped", Long.valueOf(droppedRelayed));
            }
            if(lastError != null) {
                out.put("traceExportLastError", lastError);
            }
        }
    }

    public void run() {
        // The exporter's own requests are not traced: each would be a span, and
        // exporting that span another.
        Tracing.setSuppressed(true);
        long nextTick = System.currentTimeMillis() + delayMillis;
        while(true) {
            List batch;
            List payloads;
            synchronized(lock) {
                while(!stopping) {
                    long now = System.currentTimeMillis();
                    long until;
                    if(now < retryAt) {
                        // Backing off: nothing goes before retryAt, a pending
                        // flush included, or a flush would hammer the collector
                        // that just asked for less.
                        until = retryAt;
                    } else if(queue.size() >= maxBatch || !relayed.isEmpty()
                            || drainedSpans < flushSpans || drainedRelayed < flushRelayed
                            || now >= nextTick) {
                        break;
                    } else {
                        until = nextTick;
                    }
                    try {
                        lock.wait(Math.max(1, until - now));
                    } catch (InterruptedException err) {
                        // Only a shutdown interrupts this thread; carry on to it.
                    }
                }
                batch = take(queue, maxBatch);
                // A bounded share of the relay per round, between local batches.
                // Draining it all at once let clients posting many tiny payloads
                // to a slow collector hold this thread for as long as that took,
                // while the server's own spans overflowed their queue and were
                // dropped.
                payloads = take(relayed, RELAY_PER_ROUND);
                for(int iter = 0 ; iter < payloads.size() ; iter++) {
                    relayedBytes -= ((byte[])((Object[])payloads.get(iter))[0]).length;
                }
            }
            if(System.currentTimeMillis() >= nextTick) {
                nextTick = System.currentTimeMillis() + delayMillis;
            }
            int requeuedSpans = batch.isEmpty() ? 0 : exportSpans(batch);
            int requeuedPayloads = 0;
            for(int iter = 0 ; iter < payloads.size() ; iter++) {
                if(isStopping()) {
                    // The rest of this round goes the way shutdown() sends the
                    // queue: dropped, so the stop stays bounded.
                    synchronized(lock) {
                        droppedRelayed += payloads.size() - iter;
                    }
                    break;
                }
                Object[] payload = (Object[])payloads.get(iter);
                int result = post((byte[])payload[0], (String)payload[1]);
                synchronized(lock) {
                    if(result == SENT) {
                        relayedPayloads++;
                    } else if(result == RETRY) {
                        // The collector asked for less: the backoff starts whatever
                        // becomes of this payload, and the rest of the round waits
                        // it out rather than being posted into the same trouble.
                        retryAt = System.currentTimeMillis() + delayMillis;
                        boolean retryThis = payload[2] == null;
                        if(!retryThis) {
                            droppedRelayed++;       // its one retry is spent
                        }
                        int rest = retryThis ? iter : iter + 1;
                        if(requeueRelayed(payloads, rest, retryThis)) {
                            requeuedPayloads = payloads.size() - rest;
                        } else {
                            droppedRelayed += payloads.size() - rest;
                        }
                        break;
                    } else {
                        droppedRelayed++;
                    }
                }
            }
            synchronized(lock) {
                // A requeued span is not drained: flush() is waiting for it to be
                // exported or dropped, which happens on its retry.
                drainedSpans += batch.size() - requeuedSpans;
                drainedRelayed += payloads.size() - requeuedPayloads;
                lock.notifyAll();
                if(stopping && queue.isEmpty() && relayed.isEmpty()) {
                    stopped = true;
                    lock.notifyAll();
                    return;
                }
            }
        }
    }

    private static List take(ArrayList from, int max) {
        int n = Math.min(from.size(), max);
        List out = new ArrayList(n);
        for(int iter = 0 ; iter < n ; iter++) {
            out.add(from.get(iter));
        }
        // Removed from the FRONT in one step; a remove(0) per span would shift the
        // rest of the queue every time.
        from.subList(0, n).clear();
        return out;
    }

    /**
     * Posts one batch. A retryable failure puts the spans that have not been
     * retried yet back at the head of the queue for one more try after the
     * backoff; the rest are dropped.
     *
     * @return how many spans went back on the queue
     */
    private int exportSpans(List batch) {
        byte[] body;
        try {
            Map request = OtlpTracer.exportRequest(resource, batch);
            body = protobuf ? OtlpSchema.protobuf(request) : OtlpSchema.json(request);
        } catch (Exception err) {
            // A span this code built and cannot encode is a bug here, not in the
            // collector; count it and keep exporting the rest.
            synchronized(lock) {
                failedExports++;
                droppedSpans += batch.size();
                lastError = bounded("encode: " + err.getMessage());
            }
            return 0;
        }
        int result = post(body, protobuf ? "application/x-protobuf" : "application/json");
        synchronized(lock) {
            if(result == SENT) {
                // A 200 can still carry a partial success: the collector names how
                // many spans it dropped, and those were not exported.
                long rejected = Math.min(lastRejected, batch.size());
                exportedSpans += batch.size() - rejected;
                rejectedSpans += rejected;
                return 0;
            }
            if(result == RETRY) {
                // On EVERY retryable answer, whether or not these spans can go
                // back: dropping a batch without it let the next one go straight
                // out into the same 429, unthrottled.
                retryAt = System.currentTimeMillis() + delayMillis;
            }
            if(result != RETRY || stopping) {
                droppedSpans += batch.size();
                return 0;
            }
            // Once per span, never more: an overloaded collector is not helped by
            // this server hammering it, and what is queued behind these keeps
            // arriving. Room permitting -- a full queue is the bound that holds.
            List again = new ArrayList();
            for(int iter = 0 ; iter < batch.size() ; iter++) {
                OtelSpan span = (OtelSpan)batch.get(iter);
                if(!span.exportRetried) {
                    span.exportRetried = true;
                    again.add(span);
                }
            }
            if(again.isEmpty() || queue.size() + again.size() > maxQueue) {
                droppedSpans += batch.size();
                return 0;
            }
            droppedSpans += batch.size() - again.size();
            queue.addAll(0, again);
            return again.size();
        }
    }

    /**
     * Puts payloads[from..] back at the head of the relay queue -- payloads[from]
     * marked as retried when {@code markFirst}, since its POST was the one that
     * failed -- or answers false, having changed nothing, when the relay's byte
     * budget has no room for them. The caller starts the backoff. Called holding
     * the lock.
     */
    private boolean requeueRelayed(List payloads, int from, boolean markFirst) {
        if(stopping) {
            return false;
        }
        long bytes = 0;
        for(int iter = from ; iter < payloads.size() ; iter++) {
            bytes += ((byte[])((Object[])payloads.get(iter))[0]).length;
        }
        if(relayedBytes + bytes > maxRelayBytes) {
            return false;
        }
        List again = new ArrayList();
        for(int iter = from ; iter < payloads.size() ; iter++) {
            Object[] payload = (Object[])payloads.get(iter);
            // Only the one that was POSTED has used its retry. The rest of the
            // round were never sent, and marking them too dropped each of them on
            // its first real failure.
            again.add(markFirst && iter == from
                    ? new Object[] {payload[0], payload[1], Boolean.TRUE} : payload);
        }
        relayed.addAll(0, again);
        relayedBytes += bytes;
        return true;
    }

    private static final int SENT = 0;
    /** A failure OTLP/HTTP defines as retryable: 429, 502-504, or no connection. */
    private static final int RETRY = 1;
    private static final int FAILED = 2;

    /**
     * One POST, and only one. There used to be a second, after a one-second
     * Thread.sleep in here: a thread parked doing nothing, and a window in which
     * a shutdown that had already given up waiting watched it post again with the
     * old credentials. The retry now goes back on the queue instead -- see
     * exportSpans -- and waits out its backoff on the lock, which shutdown wakes.
     */
    private int post(byte[] body, String contentType) {
        lastRejected = 0;
        List lines = new ArrayList(headers.size() + 1);
        lines.add("Content-Type: " + contentType);
        lines.addAll(headers);
        int status;
        try {
            Web.Result result = Web.request("POST", endpoint, lines, body);
            status = result.getStatus();
            if(status >= 200 && status < 300) {
                lastRejected = partialSuccess(result, contentType);
            }
        } catch (Exception err) {
            recordFailure("could not reach the collector: " + err.getMessage());
            status = -1;
        }
        if(status >= 200 && status < 300) {
            return SENT;
        }
        if(status > 0) {
            recordFailure("the collector answered " + status);
        }
        boolean retryable = status < 0 || status == 429 || status == 502 || status == 503
                || status == 504;
        return retryable ? RETRY : FAILED;
    }

    /**
     * The OTLP partial-success answer: an ExportTraceServiceResponse whose
     * partial_success reports rejected spans and why. A collector answers 200 for a
     * batch it only partly accepted, so without reading this every dropped span was
     * counted as exported and nobody could tell. Decoded in whichever encoding the
     * collector answered in; anything unreadable is taken as full success, which is
     * what a 200 without the field means.
     *
     * @return the number of spans rejected, 0 for none
     */
    private long partialSuccess(Web.Result result, String requestType) {
        byte[] body = result.getBody();
        if(body == null || body.length == 0) {
            return 0;
        }
        String type = result.getHeader("content-type");
        boolean json = type != null ? type.regionMatches(true, 0, "application/json", 0, 16)
                : !requestType.startsWith("application/x-protobuf");
        long[] rejected = new long[1];
        String[] message = new String[1];
        try {
            if(json) {
                OtlpSchema.jsonPartialSuccess(result.getBodyAsString(), rejected, message);
            } else {
                OtlpSchema.protobufPartialSuccess(body, rejected, message);
            }
        } catch (Exception err) {
            return 0;
        }
        if(rejected[0] > 0 || (message[0] != null && message[0].length() > 0)) {
            recordFailure("the collector rejected " + rejected[0] + " span(s)"
                    + (message[0] == null || message[0].length() == 0 ? "" : ": " + message[0]));
        }
        return rejected[0] < 0 ? 0 : rejected[0];
    }

    /**
     * Counted always, printed once per hundred. A missing collector is common --
     * a developer laptop, a misconfigured deployment -- and a line per batch would
     * bury everything else the server logs.
     */
    /** The longest diagnostic kept or logged. */
    static final int MAX_ERROR_CHARS = 512;

    /**
     * {@code message} cut to {@link #MAX_ERROR_CHARS}. Much of it is the
     * collector's own text -- a partial-success errorMessage, an exception naming
     * what it read -- and a response may be megabytes, which lastError then held
     * for the exporter's lifetime and the log printed as one line.
     */
    static String bounded(String message) {
        if(message == null || message.length() <= MAX_ERROR_CHARS) {
            return message;
        }
        int end = MAX_ERROR_CHARS;
        if(Character.isHighSurrogate(message.charAt(end - 1))) {
            end--;
        }
        return message.substring(0, end) + "... (" + message.length() + " chars)";
    }

    private void recordFailure(String message) {
        message = bounded(message);
        long count;
        synchronized(lock) {
            failedExports++;
            lastError = message;
            count = failedExports;
        }
        if(count == 1 || count % 100 == 0) {
            System.err.println("trace export to " + redact(endpoint) + " failed (" + count
                    + " so far): " + message);
        }
    }

    /**
     * The endpoint as it may be logged: no query or fragment, where a token would travel, and
     * no userinfo, where a password would. Both are how collectors are commonly
     * authenticated, and this string goes into every failure line and every
     * refused-configuration message.
     */
    static String redact(String url) {
        // The query AND the fragment, whichever comes first: an OAuth token rides
        // in a fragment as readily as in a query, and cutting only at '?' logged it.
        int cut = url.length();
        int query = url.indexOf('?');
        int fragment = url.indexOf('#');
        if(query >= 0) {
            cut = query;
        }
        if(fragment >= 0 && fragment < cut) {
            cut = fragment;
        }
        String out = url.substring(0, cut);
        int scheme = out.indexOf("://");
        if(scheme >= 0) {
            int at = out.indexOf('@', scheme + 3);
            int slash = out.indexOf('/', scheme + 3);
            if(at >= 0 && (slash < 0 || at < slash)) {
                out = out.substring(0, scheme + 3) + "<redacted>@" + out.substring(at + 1);
            }
        }
        // Says something was there, and which part, without saying what.
        return cut < url.length() ? out + url.charAt(cut) + "<redacted>" : out;
    }
}
