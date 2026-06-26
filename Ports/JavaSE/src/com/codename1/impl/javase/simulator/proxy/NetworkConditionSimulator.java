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
import com.codename1.impl.javase.simulator.tools.SimulatorTools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Decorator simulating degraded network conditions over any backend
 * implementation: the Tools/Network menu items Slow Connection and
 * Disconnected toggle flags in {@link SimulatorTools} and this decorator
 * applies them to the connection streams.
 *
 * <p>Slow mode injects sleeps mirroring the historical JavaSEPort behavior
 * (1000ms before the response, 250ms per request-body chunk and response-code
 * query, response-length-proportional delays while reading). Disconnected mode
 * throws IOException("Unreachable") at every network touch point. File
 * connections (the implementation contract passes those as String paths) are
 * never affected.</p>
 */
public class NetworkConditionSimulator extends CodenameOneImplementationDecorator {
    public NetworkConditionSimulator(CodenameOneImplementation delegate) {
        super(delegate);
    }

    private static boolean slow() {
        return SimulatorTools.isSlowConnectionMode();
    }

    private static boolean disconnected() {
        return SimulatorTools.isDisconnectedMode();
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {
        }
    }

    private static void failIfDisconnected() throws IOException {
        if (disconnected()) {
            throw new IOException("Unreachable");
        }
    }

    @Override
    public Object connect(String url, boolean read, boolean write) throws IOException {
        if (disconnected() && url.toLowerCase().startsWith("http")) {
            throw new IOException("Unreachable");
        }
        return delegate.connect(url, read, write);
    }

    @Override
    public Object connect(String url, boolean read, boolean write, int timeout) throws IOException {
        if (disconnected() && url.toLowerCase().startsWith("http")) {
            throw new IOException("Unreachable");
        }
        return delegate.connect(url, read, write, timeout);
    }

    @Override
    public OutputStream openOutputStream(Object connection) throws IOException {
        if (connection instanceof String || !(slow() || disconnected())) {
            return delegate.openOutputStream(connection);
        }
        failIfDisconnected();
        final OutputStream out = delegate.openOutputStream(connection);
        return new OutputStream() {
            public void write(int b) throws IOException {
                out.write(b);
                degrade(250);
            }

            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
                degrade(250);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public void close() throws IOException {
                out.close();
            }

            private void degrade(int ms) throws IOException {
                if (slow()) {
                    sleep(ms);
                }
                failIfDisconnected();
            }
        };
    }

    @Override
    public InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String || !(slow() || disconnected())) {
            return delegate.openInputStream(connection);
        }
        if (slow()) {
            sleep(1000);
        }
        failIfDisconnected();
        final InputStream in = delegate.openInputStream(connection);
        return new InputStream() {
            public int read() throws IOException {
                int b = in.read();
                degrade(1);
                return b;
            }

            public int read(byte[] b, int off, int len) throws IOException {
                int s = in.read(b, off, len);
                degrade(len);
                return s;
            }

            public int available() throws IOException {
                return in.available();
            }

            public void close() throws IOException {
                in.close();
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

            private void degrade(int ms) throws IOException {
                if (slow()) {
                    sleep(ms);
                }
                failIfDisconnected();
            }
        };
    }

    @Override
    public int getResponseCode(Object connection) throws IOException {
        int code = delegate.getResponseCode(connection);
        if (slow()) {
            sleep(250);
        }
        failIfDisconnected();
        return code;
    }
}
