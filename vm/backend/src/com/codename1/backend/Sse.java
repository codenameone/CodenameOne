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
import java.io.OutputStream;

/// Server-Sent Events (`text/event-stream`) from a handler: a response that stays
/// open and delivers each event to the client the moment it is written.
///
/// ```java
/// return Sse.stream(request, events -> {
///     for (String token : model.tokens(prompt)) {
///         events.data(token);                 // on the wire now, not at the end
///     }
///     events.event("done").data("{}");
/// });
/// ```
///
/// The producer runs on the worker that served the request, after the headers
/// are sent, and the stream ends when it returns. It blocks on its own source --
/// an upstream stream, a queue it polls -- and should call [Events#ping()] when
/// it has had nothing to send for a while: proxies and load balancers close a
/// connection that is idle for about a minute. Wait with `Thread.sleep`, never
/// `Object.wait`, which blocks the whole host under virtual threads.
///
/// A client that goes away makes the next write throw `IOException`; let it
/// propagate so the producer's try-with-resources cleanup runs. A producer that
/// opens something before it runs should also implement `java.io.Closeable`: the
/// server calls `close()` instead of the producer when no body is sent (HEAD).
///
/// To relay an upstream event stream unchanged, [HttpProxy] already streams it.
///
/// This is the blocking-producer model, not Spring's `SseEmitter`: nothing here
/// hands events from another thread to the connection.
public final class Sse {
    /// The media type of an event stream.
    public static final String CONTENT_TYPE = "text/event-stream; charset=utf-8";

    private Sse() {
    }

    /// Writes the events of one stream.
    public interface Producer {
        void produce(Events events) throws IOException;
    }

    /// An event stream answered with status 200.
    public static HttpServer.Response stream(HttpServer.Request request, Producer producer) {
        if (producer == null) {
            throw new NullPointerException("producer");
        }
        // Read now: a body writer must not keep the Request.
        String lastEventId = request == null ? null : request.getHeader("Last-Event-ID");
        return HttpServer.Response.stream(200, CONTENT_TYPE, new Writer(producer, lastEventId))
                .header("Cache-Control", "no-cache")
                // nginx buffers a proxied response by default, which holds events back.
                .header("X-Accel-Buffering", "no");
    }

    private static final class Writer implements HttpServer.BodyWriter, java.io.Closeable {
        private final Producer producer;
        private final String lastEventId;

        Writer(Producer producer, String lastEventId) {
            this.producer = producer;
            this.lastEventId = lastEventId;
        }

        @Override
        public void writeTo(OutputStream output) throws IOException {
            producer.produce(new Events(output, lastEventId));
        }

        @Override
        public void close() throws IOException {
            if (producer instanceof java.io.Closeable) {
                ((java.io.Closeable) producer).close();
            }
        }
    }

    /// The open stream. `event`, `id` and `retry` set fields of the NEXT event,
    /// which [#data(String)], [#json(Object)] or [#send()] then writes and flushes.
    public static final class Events {
        private final OutputStream out;
        private final String lastEventId;
        private final ByteSink sink = new ByteSink(256);
        private final ByteSink json = new ByteSink(256);
        private String event;
        private String id;
        private long retry = -1;

        Events(OutputStream out, String lastEventId) {
            this.out = out;
            this.lastEventId = lastEventId;
        }

        /// The `Last-Event-ID` the client reconnected with, or null on a first
        /// connection. Resume after it.
        public String lastEventId() {
            return lastEventId;
        }

        /// Names the next event; a client listens for it with
        /// `addEventListener(name, ...)`. Unnamed events are `message`.
        public Events event(String name) {
            event = checkField("event", name);
            return this;
        }

        /// The id of the next event, which the client sends back as
        /// `Last-Event-ID` when it reconnects.
        public Events id(String eventId) {
            String checked = checkField("id", eventId);
            if (checked != null && checked.indexOf('\0') >= 0) {
                // A browser ignores an id containing NUL, so it would never come back.
                throw new IllegalArgumentException("an event id cannot contain NUL");
            }
            id = checked;
            return this;
        }

        /// How long the client waits before reconnecting, sent with the next event.
        public Events retry(long millis) {
            if (millis < 0) {
                throw new IllegalArgumentException("retry " + millis + " is negative");
            }
            retry = millis;
            return this;
        }

        /// Sends one event with this data. Each line of a multi-line value
        /// becomes its own `data:` field, which the client joins back with `\n`.
        public void data(String data) throws IOException {
            sink.reset();
            putFields();
            putData(data == null ? "" : data);
            finish();
        }

        /// Sends one event whose data is `value` as JSON, encoded straight into
        /// the buffer. JSON has no raw line breaks, so it is always one line.
        public void json(Object value) throws IOException {
            sink.reset();
            putFields();
            json.reset();
            Json.write(value, json);
            sink.putAscii("data: ");
            sink.put(json.bytes(), 0, json.length());
            sink.put('\n');
            finish();
        }

        /// Sends the pending `event`/`id`/`retry` fields with no data. A browser
        /// dispatches nothing for it, but records the id and the retry.
        public void send() throws IOException {
            sink.reset();
            putFields();
            finish();
        }

        /// A comment line, which every client ignores.
        public void comment(String text) throws IOException {
            sink.reset();
            putLines(":", text == null ? "" : text);
            sink.put('\n');
            write();
        }

        /// Keeps an idle stream open through proxies; the client sees nothing.
        public void ping() throws IOException {
            sink.reset();
            sink.putAscii(":\n\n");
            write();
        }

        private void putFields() {
            if (event != null) {
                sink.putAscii("event: ");
                sink.putUtf8(event);
                sink.put('\n');
            }
            if (id != null) {
                sink.putAscii("id: ");
                sink.putUtf8(id);
                sink.put('\n');
            }
            if (retry >= 0) {
                sink.putAscii("retry: ");
                sink.putNumber(retry);
                sink.put('\n');
            }
            event = null;
            id = null;
            retry = -1;
        }

        private void putData(String data) {
            putLines("data:", data);
        }

        /// One `prefix value` line per line of `text`, split at CR, LF or CRLF
        /// -- every line ending the event stream format recognises.
        private void putLines(String prefix, String text) {
            int start = 0;
            int n = text.length();
            for (int i = 0; i <= n; i++) {
                char c = i < n ? text.charAt(i) : '\n';
                if (c == '\n' || c == '\r') {
                    sink.putAscii(prefix);
                    if (i > start) {
                        sink.put(' ');
                        sink.putUtf8(text.substring(start, i));
                    }
                    sink.put('\n');
                    if (c == '\r' && i + 1 < n && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    start = i + 1;
                }
            }
        }

        private void finish() throws IOException {
            sink.put('\n');
            write();
        }

        private void write() throws IOException {
            out.write(sink.bytes(), 0, sink.length());
            out.flush();
        }

        private static String checkField(String field, String value) {
            if (value != null && (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)) {
                // A line break would end the field and let the rest forge another.
                throw new IllegalArgumentException("an event " + field + " cannot contain a line break");
            }
            return value;
        }
    }
}
