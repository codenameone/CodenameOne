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
/// answers `(status, headers, body)` the same way.
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
        /// The endpoint accepted; the handshake has already completed.
        void accept(String subprotocol);

        void text(String value);

        void binary(byte[] value);

        void close(int code, String reason);
    }

    /// Opens one websocket connection in Python; answers the connection
    /// object [#webSocketText], [#webSocketBinary] and [#webSocketClosed]
    /// take. Messages the endpoint sends, including any it sends while
    /// opening, reach `sink`.
    public static Object openWebSocket(Object webSockets, String target, String[] headers,
                                       final WebSocketSink sink) {
        Object[] req = request("GET", target, headers, null);
        PyBuiltin send = PyBuiltin.positional("send", a -> {
            String kind = Py.toStr(a[0]);
            if ("accept".equals(kind)) {
                sink.accept(a[1] == Py.NONE ? null : Py.toStr(a[1])); //NOPMD CompareObjectsWithEquals - identity is meant
            } else if ("text".equals(kind)) {
                sink.text(Py.toStr(a[1]));
            } else if ("bytes".equals(kind)) {
                sink.binary(PyBytes.like(a[1]));
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
        if (!(r instanceof PyTuple) || ((PyTuple) r).size() != 3) {
            throw Py.typeError("dispatch must return (status, headers, body)");
        }
        PyTuple t = (PyTuple) r;
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
        return new Response((int) Py.asLong(t.get(0)), headers, PyBytes.like(t.get(2)));
    }

    /// What `dispatch` answered.
    public static final class Response {
        public final int status;
        /// Name, value, name, value...
        public final String[] headers;
        public final byte[] body;

        Response(int status, String[] headers, byte[] body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
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
