/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.javase.simulator.proxy;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.CodenameOneImplementationDecorator;
import com.codename1.impl.javase.NetworkMonitor;
import com.codename1.impl.javase.NetworkRequestObject;
import com.codename1.impl.javase.simulator.tools.SimulatorTools;
import com.codename1.io.ConnectionRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decorator feeding the simulator's network monitor from the implementation's
 * connection entry points. Works against any backend (Swing or native) because
 * it never inspects the opaque connection object - all information is captured
 * from the CodenameOneImplementation call parameters and return values.
 *
 * <p>The monitor window itself is owned by {@link SimulatorTools}; when it is
 * closed this decorator is a near-no-op. Request/response capture mirrors the
 * historical inline hooks that lived in JavaSEPort's networking methods.</p>
 */
public class NetworkMonitorProxy extends CodenameOneImplementationDecorator {
    /**
     * Request headers accumulated per connection from setHeader calls; keyed
     * by the opaque connection object, purged when its response stream closes.
     */
    private final Map<Object, Map<String, List<String>>> requestHeaders
            = new IdentityHashMap<Object, Map<String, List<String>>>();

    public NetworkMonitorProxy(CodenameOneImplementation delegate) {
        super(delegate);
    }

    private static NetworkMonitor monitor() {
        return SimulatorTools.getNetworkMonitor();
    }

    private static NetworkRequestObject byConnection(Object connection) {
        NetworkMonitor m = monitor();
        if (m != null) {
            return m.getByConnection(connection);
        }
        return null;
    }

    @Override
    public void addConnectionToQueue(ConnectionRequest req) {
        delegate.addConnectionToQueue(req);
        NetworkMonitor m = monitor();
        if (m != null) {
            NetworkRequestObject o = new NetworkRequestObject();
            o.setTimeQueued(System.currentTimeMillis());
            m.addQueuedRequest(req, o);
        }
    }

    @Override
    public void setConnectionId(Object connection, int id) {
        delegate.setConnectionId(connection, id);
        NetworkMonitor m = monitor();
        if (m != null) {
            NetworkRequestObject queuedRequest = m.findQueuedRequest(id);
            if (queuedRequest != null) {
                NetworkRequestObject existingRequest = m.getByConnection(connection);
                if (existingRequest != null) {
                    existingRequest.setTimeQueued(queuedRequest.getTimeQueued());
                } else {
                    m.addRequest(connection, queuedRequest);
                }
                m.removeQueuedRequest(queuedRequest);
            }
        }
    }

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        Object con = delegate.connect(url, read, write);
        register(con, url);
        return con;
    }

    @Override
    public Object connect(String url, boolean read, boolean write, int timeout) throws IOException {
        Object con = delegate.connect(url, read, write, timeout);
        register(con, url);
        return con;
    }

    private void register(Object con, String url) {
        NetworkMonitor m = monitor();
        if (m != null) {
            NetworkRequestObject nr = new NetworkRequestObject();
            nr.setUrl(url);
            nr.setTimeSent(System.currentTimeMillis());
            m.addRequest(con, nr);
        }
    }

    @Override
    public void setHeader(Object connection, String key, String val) {
        delegate.setHeader(connection, key, val);
        NetworkRequestObject nr = byConnection(connection);
        if (nr != null) {
            Map<String, List<String>> headers;
            synchronized (requestHeaders) {
                headers = requestHeaders.get(connection);
                if (headers == null) {
                    headers = new LinkedHashMap<String, List<String>>();
                    requestHeaders.put(connection, headers);
                }
            }
            List<String> values = headers.get(key);
            if (values == null) {
                values = new ArrayList<String>();
                headers.put(key, values);
            }
            values.add(val);
            StringBuilder b = new StringBuilder();
            for (Map.Entry<String, List<String>> e : headers.entrySet()) {
                b.append(e.getKey()).append("=").append(e.getValue()).append("\n");
            }
            nr.setHeaders(b.toString());
        }
    }

    @Override
    public void setHttpMethod(Object connection, String method) throws IOException {
        delegate.setHttpMethod(connection, method);
        NetworkRequestObject nr = byConnection(connection);
        if (nr != null) {
            nr.setMethod(method.toUpperCase());
        }
    }

    @Override
    public void setPostRequest(Object connection, boolean p) {
        delegate.setPostRequest(connection, p);
        NetworkRequestObject nr = byConnection(connection);
        if (nr != null) {
            nr.setMethod(p ? "POST" : "GET");
        }
    }

    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        OutputStream out = delegate.openOutputStream(connection);
        if (connection instanceof String) {
            return out;
        }
        final NetworkRequestObject nr = byConnection(connection);
        if (nr == null) {
            return out;
        }
        nr.setRequestBody("");
        final OutputStream o = out;
        return new OutputStream() {
            public void write(int b) throws IOException {
                o.write(b);
                nr.setRequestBody(nr.getRequestBody() + (char) b);
            }

            public void write(byte[] b, int off, int len) throws IOException {
                o.write(b, off, len);
                nr.setRequestBody(nr.getRequestBody() + new String(b, off, len));
            }

            public void flush() throws IOException {
                o.flush();
            }

            public void close() throws IOException {
                o.close();
            }
        };
    }

    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            return delegate.openInputStream(connection);
        }
        final NetworkRequestObject nr = byConnection(connection);
        if (nr == null) {
            return delegate.openInputStream(connection);
        }
        nr.setTimeServerResponse(System.currentTimeMillis());
        boolean isText = false;
        try {
            StringBuilder b = new StringBuilder();
            String[] names = delegate.getHeaderFieldNames(connection);
            if (names != null) {
                for (String name : names) {
                    String[] values = delegate.getHeaderFields(name, connection);
                    if (values != null) {
                        List<String> l = new ArrayList<String>();
                        for (String v : values) {
                            l.add(v);
                        }
                        b.append(name).append("=").append(l).append("\n");
                        if ("content-type".equalsIgnoreCase(name) && values.length > 0) {
                            String contentType = values[0];
                            if (contentType.startsWith("text/") || contentType.contains("json")
                                    || contentType.contains("css") || contentType.contains("javascript")) {
                                isText = true;
                            }
                        }
                    }
                }
            }
            nr.setResponseHeaders(b.toString());
        } catch (IOException ignored) {
            // header capture must never break the actual connection
        }
        nr.setResponseBody("");
        final boolean fIsText = isText;
        final Object fConnection = connection;
        final InputStream in = delegate.openInputStream(connection);
        return new InputStream() {
            public int read() throws IOException {
                int b = in.read();
                if (fIsText && b > -1) {
                    nr.setResponseBody(nr.getResponseBody() + (char) b);
                }
                return b;
            }

            public synchronized int read(byte[] b, int off, int len) throws IOException {
                int s = in.read(b, off, len);
                if (fIsText && s > 0) {
                    nr.setResponseBody(nr.getResponseBody() + new String(b, off, s));
                }
                return s;
            }

            public int available() throws IOException {
                return in.available();
            }

            public long skip(long n) throws IOException {
                return in.skip(n);
            }

            public boolean markSupported() {
                return in.markSupported();
            }

            public void mark(int readlimit) {
                in.mark(readlimit);
            }

            public void reset() throws IOException {
                in.reset();
            }

            public void close() throws IOException {
                in.close();
                nr.setTimeComplete(System.currentTimeMillis());
                synchronized (requestHeaders) {
                    requestHeaders.remove(fConnection);
                }
            }
        };
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        int code = delegate.getResponseCode(connection);
        NetworkRequestObject nr = byConnection(connection);
        if (nr != null) {
            nr.setResponseCode("" + code);
        }
        return code;
    }

    @Override
    public int getContentLength(Object connection) {
        int contentLength = delegate.getContentLength(connection);
        NetworkRequestObject nr = byConnection(connection);
        if (nr != null) {
            nr.setContentLength("" + contentLength);
        }
        return contentLength;
    }
}
