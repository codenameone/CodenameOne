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
