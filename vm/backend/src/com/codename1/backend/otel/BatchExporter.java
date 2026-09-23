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
    private long requested;
    private long completed;
    private boolean stopping;
    private boolean stopped;
    private Thread thread;

    // Counters for the metrics snapshot. Written under the lock.
    private long exportedSpans;
    private long droppedSpans;
    private long failedExports;
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
            if(stopped || queue.size() >= maxQueue) {
                droppedSpans++;
                return;
            }
            queue.add(span);
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
            if(stopped || relayedBytes + body.length > maxRelayBytes) {
                droppedRelayed++;
                return false;
            }
            relayed.add(new Object[] {body, contentType});
            relayedBytes += body.length;
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
            long target = ++requested;
            lock.notifyAll();
            while(completed < target && !stopped) {
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

    /** Flushes, then stops the thread. */
    void shutdown(int timeoutMillis) {
        flush(timeoutMillis);
        synchronized(lock) {
            stopping = true;
            lock.notifyAll();
        }
    }

    void metrics(Map out) {
        synchronized(lock) {
            out.put("tracing", stopped ? "stopped" : "on");
            out.put("spansExported", Long.valueOf(exportedSpans));
            out.put("spansDropped", Long.valueOf(droppedSpans));
            out.put("spansQueued", Integer.valueOf(queue.size()));
            out.put("traceExportsFailed", Long.valueOf(failedExports));
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
            long answering;
            synchronized(lock) {
                while(!stopping && queue.size() < maxBatch && relayed.isEmpty()
                        && requested == completed) {
                    long left = nextTick - System.currentTimeMillis();
                    if(left <= 0) {
                        break;
                    }
                    try {
                        lock.wait(left);
                    } catch (InterruptedException err) {
                        // Only a shutdown interrupts this thread; carry on to it.
                    }
                }
                answering = requested;
                batch = take(queue, maxBatch);
                payloads = relayed;
                relayed = new ArrayList();
                relayedBytes = 0;
            }
            if(System.currentTimeMillis() >= nextTick) {
                nextTick = System.currentTimeMillis() + delayMillis;
            }
            if(!batch.isEmpty()) {
                exportSpans(batch);
            }
            for(int iter = 0 ; iter < payloads.size() ; iter++) {
                Object[] payload = (Object[])payloads.get(iter);
                boolean sent = post((byte[])payload[0], (String)payload[1]);
                synchronized(lock) {
                    if(sent) {
                        relayedPayloads++;
                    } else {
                        droppedRelayed++;
                    }
                }
            }
            synchronized(lock) {
                // A flush is answered only once everything queued BEFORE it has
                // gone -- a batch cap smaller than the queue means that can take
                // several rounds, and answering on the first would let a Lambda
                // invocation return with spans still waiting.
                if(queue.isEmpty() && relayed.isEmpty() && completed < answering) {
                    completed = answering;
                    lock.notifyAll();
                }
                if(stopping && queue.isEmpty() && relayed.isEmpty()) {
                    stopped = true;
                    completed = requested;
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

    private void exportSpans(List batch) {
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
                lastError = "encode: " + err.getMessage();
            }
            return;
        }
        boolean sent = post(body, protobuf ? "application/x-protobuf" : "application/json");
        synchronized(lock) {
            if(sent) {
                exportedSpans += batch.size();
            } else {
                droppedSpans += batch.size();
            }
        }
    }

    /**
     * One POST, retried once for the answers OTLP/HTTP defines as retryable. Never
     * more than once: a collector that is overloaded is not helped by this server
     * hammering it, and the next batch is already queued behind this one.
     */
    private boolean post(byte[] body, String contentType) {
        for(int attempt = 0 ; attempt < 2 ; attempt++) {
            List lines = new ArrayList(headers.size() + 1);
            lines.add("Content-Type: " + contentType);
            lines.addAll(headers);
            int status;
            try {
                Web.Result result = Web.request("POST", endpoint, lines, body);
                status = result.getStatus();
            } catch (Exception err) {
                recordFailure("could not reach the collector: " + err.getMessage());
                status = -1;
            }
            if(status >= 200 && status < 300) {
                return true;
            }
            if(status > 0) {
                recordFailure("the collector answered " + status);
            }
            boolean retryable = status < 0 || status == 429 || status == 502 || status == 503
                    || status == 504;
            if(!retryable || attempt == 1) {
                return false;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException err) {
                return false;
            }
        }
        return false;
    }

    /**
     * Counted always, printed once per hundred. A missing collector is common --
     * a developer laptop, a misconfigured deployment -- and a line per batch would
     * bury everything else the server logs.
     */
    private void recordFailure(String message) {
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

    /** The endpoint without its query, which is where a token would travel. */
    static String redact(String url) {
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query) + "?<redacted>";
    }
}
