/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

/// Serves a web application's `dispatch` callable over HTTP: what
/// `_cn1server.serve`, and so `Flask.run` and `uvicorn.run`, reach.
///
/// A backend build installs one over the backend's `HttpServer` from its
/// generated registry; an application without a server has none, and
/// serving raises `OSError`. `dispatch` takes `(method, path, query,
/// headers, body)`, the headers a list of pairs and the body bytes, and
/// answers `(status, headers, body)` the same way, or
/// `(status, headers, body, after)` when work is to run once the response
/// has been sent (FastAPI's background tasks): the server calls
/// [#runAfter(Response)] when the last byte is out.
///
/// Requests are served concurrently, on several threads. Each holds the
/// [Gil] only while it runs Python -- [#call], [#nextChunk], [#runAfter] --
/// and never while it writes to a socket or waits for a chunk to drain, so a
/// slow streamed response does not hold up the requests beside it.
public abstract class PyServer {
    private static PyServer current;

    public static PyServer get() {
        if (current == null) {
            throw Py.error(Py.OS_ERROR, "no HTTP server: build the application as a backend to serve it");
        }
        return current;
    }

    public static void install(PyServer server) {
        current = server;
    }

    /// Serves until the server stops.
    public abstract void serve(Object dispatch, String host, int port);

    /// Serves `dispatch`, and websocket connections through `webSockets`
    /// when it is not null. `webSockets` is the callable `uvicorn.run`
    /// passes: `(path, query, headers, send)` answering a connection object
    /// with `text(str)`, `binary(bytes)` and `closed(code, reason)`. A server
    /// that cannot upgrade a connection refuses to start rather than serving
    /// an application whose websocket routes nothing could reach.
    public void serve(Object dispatch, String host, int port, Object webSockets) {
        if (webSockets != null) {
            throw Py.error(Py.OS_ERROR, "this HTTP server does not support websockets");
        }
        serve(dispatch, host, port);
    }

    /// What a server does with the messages a Python websocket endpoint sends.
    public interface WebSocketSink {
        /// The endpoint accepted. `headers` are the extra handshake response
        /// headers it asked for, name then value, never null.
        void accept(String subprotocol, String[] headers);

        void text(String value);

        void binary(byte[] value);

        void close(int code, String reason);

        /// The endpoint refused the handshake before accepting; `status` is
        /// the HTTP status it answered with (403 when it simply closed, 500
        /// when it failed), `headers` the response's headers, name then value,
        /// and `body` its body. Neither is ever null.
        void deny(int status, String[] headers, byte[] body);
    }

    /// Opens one websocket connection in Python; answers the connection
    /// object [#webSocketText], [#webSocketBinary] and [#webSocketClosed]
    /// take. The endpoint runs on Python's websocket event loop thread, so
    /// this returns at once and `sink` is called later, from that thread:
    /// first `accept` or `deny`, then the messages the endpoint sends.
    public static Object openWebSocket(Object webSockets, String target, String[] headers,
                                       final WebSocketSink sink) {
        Object[] req = request("GET", target, headers, null);
        PyBuiltin send = PyBuiltin.positional("send", a -> {
            String kind = Py.toStr(a[0]);
            if ("accept".equals(kind)) {
                sink.accept(a[1] == Py.NONE ? null : Py.toStr(a[1]), strings(a[2])); //NOPMD CompareObjectsWithEquals - identity is meant
            } else if ("text".equals(kind)) {
                sink.text(Py.toStr(a[1]));
            } else if ("bytes".equals(kind)) {
                sink.binary(PyBytes.like(a[1]));
            } else if ("deny".equals(kind)) {
                Object detail = a[2];
                Object[] parts = detail instanceof PyList ? ((PyList) detail).toArray() : null;
                sink.deny((int) Py.asLong(a[1]), parts == null ? new String[0] : strings(parts[0]),
                        parts == null ? new byte[0] : PyBytes.like(parts[1]));
            } else {
                sink.close((int) Py.asLong(a[1]), a[2] == Py.NONE ? "" : Py.toStr(a[2])); //NOPMD CompareObjectsWithEquals - identity is meant
            }
            return Py.NONE;
        });
        Gil.enter();
        try {
            return Py.obj(webSockets).call(new Object[] {req[1], req[2], req[3], send}, Py.NO_KW);
        } finally {
            Gil.exit();
        }
    }

    /// A Python list of strings as an array; anything else is empty.
    private static String[] strings(Object list) {
        if (!(list instanceof PyList)) {
            return new String[0];
        }
        Object[] items = ((PyList) list).toArray();
        String[] out = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            out[i] = Py.toStr(items[i]);
        }
        return out;
    }

    /// Delivers a text message to a connection [#openWebSocket] opened.
    public static void webSocketText(Object connection, String value) {
        event(connection, "text", value);
    }

    /// Delivers a binary message to a connection [#openWebSocket] opened.
    public static void webSocketBinary(Object connection, byte[] value) {
        event(connection, "binary", new PyBytes(value));
    }

    /// Tells a connection [#openWebSocket] opened that the peer has gone.
    public static void webSocketClosed(Object connection, int code, String reason) {
        Gil.enter();
        try {
            Py.callMethod(connection, "closed", Py.of(code), reason == null ? "" : reason);
        } finally {
            Gil.exit();
        }
    }

    private static void event(Object connection, String name, Object value) {
        Gil.enter();
        try {
            Py.callMethod(connection, name, value);
        } finally {
            Gil.exit();
        }
    }

    /// The request as `dispatch` takes it.
    public static Object[] request(String method, String target, String[] headers, byte[] body) {
        int q = target.indexOf('?');
        String path = q < 0 ? target : target.substring(0, q);
        String query = q < 0 ? "" : target.substring(q + 1);
        PyList hs = new PyList(headers.length / 2);
        for (int i = 0; i + 1 < headers.length; i += 2) {
            hs.append(new PyTuple(new Object[] {headers[i], headers[i + 1]}));
        }
        return new Object[] {method, path, query, hs, new PyBytes(body == null ? new byte[0] : body)};
    }

    /// Calls `dispatch` and checks what it answered.
    public static Response call(Object dispatch, Object[] request) {
        Gil.enter();
        try {
            return call0(dispatch, request);
        } finally {
            Gil.exit();
        }
    }

    private static Response call0(Object dispatch, Object[] request) {
        Object r = Py.obj(dispatch).call(request, Py.NO_KW);
        if (!(r instanceof PyTuple) || ((PyTuple) r).size() < 3 || ((PyTuple) r).size() > 4) {
            throw Py.typeError("dispatch must return (status, headers, body[, after])");
        }
        PyTuple t = (PyTuple) r;
        Object after = t.size() == 4 && t.get(3) != Py.NONE ? t.get(3) : null; //NOPMD CompareObjectsWithEquals - identity is meant
        Object hl = t.get(1);
        Object[] pairs = hl instanceof PyList ? ((PyList) hl).toArray()
                : hl instanceof PyTuple ? ((PyTuple) hl).items : null;
        if (pairs == null) {
            throw Py.typeError("dispatch headers must be a list of (name, value) pairs");
        }
        String[] headers = new String[pairs.length * 2];
        for (int i = 0; i < pairs.length; i++) {
            if (!(pairs[i] instanceof PyTuple) || ((PyTuple) pairs[i]).size() != 2) {
                throw Py.typeError("dispatch headers must be a list of (name, value) pairs");
            }
            PyTuple p = (PyTuple) pairs[i];
            headers[2 * i] = Py.toStr(p.get(0));
            headers[2 * i + 1] = Py.toStr(p.get(1));
        }
        Object body = t.get(2);
        byte[] bytes = PyBytes.like(body);
        if (bytes != null || body == Py.NONE) { //NOPMD CompareObjectsWithEquals - identity is meant
            return new Response((int) Py.asLong(t.get(0)), headers, bytes == null ? new byte[0] : bytes, null, after);
        }
        // Anything else is an iterable of chunks, sent as each is produced.
        return new Response((int) Py.asLong(t.get(0)), headers, new byte[0], Py.iter(body), after);
    }

    /// Runs the work `dispatch` asked to run after the response, at most
    /// once, under the GIL. A failure is printed: the response is already
    /// out, so there is nobody left to answer it to, as in Starlette.
    public static void runAfter(Response response) {
        Object after = response.after.getAndSet(null);
        if (after == null) {
            return;
        }
        Gil.enter();
        try {
            Py.obj(after).call(new Object[0], Py.NO_KW);
        } catch (PyException e) {
            e.printStackTrace();
        } finally {
            Gil.exit();
        }
    }

    /// Drops the work to run after a response that was never completed --
    /// the client went away -- as Starlette skips its background tasks when
    /// a send fails.
    public static void cancelAfter(Response response) {
        response.after.set(null);
    }

    /// [#runAfter(Response)] on a thread of its own, for a server that has
    /// no hook for "the response is written": the caller returns and the
    /// response goes out without waiting for the work, which then waits only
    /// for the GIL. Does nothing when there is no such work.
    public static void runAfterLater(final Response response) {
        if (response.after.get() == null) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                runAfter(response);
            }
        });
        t.start();
    }

    /// The next chunk of a streamed response, or an empty array when it is
    /// done. Runs the application's generator, under the GIL, up to its next
    /// `yield`; empty chunks are skipped, so empty means the end and never
    /// "nothing yet".
    public static byte[] nextChunk(Response response) {
        if (response.stream == null) {
            return new byte[0];
        }
        Gil.enter();
        try {
            while (true) {
                Object item = Py.next(response.stream);
                if (item == null) {
                    response.stream = null;
                    return new byte[0];
                }
                byte[] chunk = PyBytes.like(item);
                if (chunk == null) {
                    chunk = Py.toStr(item).getBytes("UTF-8");
                }
                if (chunk.length > 0) {
                    return chunk;
                }
            }
        } catch (java.io.UnsupportedEncodingException e) {
            throw Py.error(Py.OS_ERROR, "UTF-8 is unsupported");
        } finally {
            Gil.exit();
        }
    }

    /// Stops a streamed response early -- the client went away -- by closing
    /// the application's generator, so its `finally` blocks run.
    public static void closeStream(Response response) {
        Object it = response.stream;
        response.stream = null;
        if (it == null) {
            return;
        }
        Gil.enter();
        try {
            Object close = Py.obj(it).getAttrOrNull("close");
            if (close != null) {
                Py.obj(close).call(new Object[0], Py.NO_KW);
            }
        } catch (PyException e) {
            // The client is gone; a generator that fails while closing has
            // nobody left to answer.
            e.printStackTrace();
        } finally {
            Gil.exit();
        }
    }

    /// What `dispatch` answered.
    public static final class Response {
        public final int status;
        /// Name, value, name, value...
        public final String[] headers;
        /// Empty when the body is streamed.
        public final byte[] body;
        /// The chunk iterator of a streamed body, until it is exhausted.
        Object stream;
        /// What [#runAfter(Response)] calls, until it has.
        final java.util.concurrent.atomic.AtomicReference<Object> after;

        Response(int status, String[] headers, byte[] body, Object stream, Object after) {
            this.status = status;
            this.headers = headers;
            this.body = body;
            this.stream = stream;
            this.after = new java.util.concurrent.atomic.AtomicReference<Object>(after);
        }

        /// Whether there is work to run once this response has been sent.
        public boolean hasAfter() {
            return after.get() != null;
        }

        /// Whether the body comes from [#nextChunk] rather than [#body].
        public boolean isStreamed() {
            return stream != null;
        }

        /// The value of the first header named `name`, ignoring case.
        public String header(String name) {
            for (int i = 0; i + 1 < headers.length; i += 2) {
                if (headers[i].equalsIgnoreCase(name)) {
                    return headers[i + 1];
                }
            }
            return null;
        }
    }
}
