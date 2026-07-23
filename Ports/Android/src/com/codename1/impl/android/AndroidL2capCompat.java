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
package com.codename1.impl.android;

import android.os.Build;

import com.codename1.bluetooth.BluetoothError;
import com.codename1.bluetooth.BluetoothException;
import com.codename1.bluetooth.le.L2capChannel;
import com.codename1.bluetooth.le.L2capServer;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflection bridge to the Android 10 (API 29) L2CAP connection-oriented
 * channel APIs. The Android port compiles against the API 27 android.jar
 * from cn1-binaries, so the API 29 symbols
 * ({@code BluetoothDevice.createL2capChannel},
 * {@code BluetoothAdapter.listenUsingL2capChannel},
 * {@code BluetoothServerSocket.getPsm}) cannot be referenced directly and
 * are looked up reflectively here. All lookups are cached; on devices below
 * API 29 the feature reports itself as unsupported.
 */
final class AndroidL2capCompat {

    private static boolean initialized;
    private static Method createSecureChannel;
    private static Method createInsecureChannel;
    private static Method listenSecure;
    private static Method listenInsecure;
    private static Method getPsm;

    private AndroidL2capCompat() {
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (Build.VERSION.SDK_INT < 29) {
            return;
        }
        try {
            createSecureChannel = android.bluetooth.BluetoothDevice.class
                    .getMethod("createL2capChannel", int.class);
            createInsecureChannel = android.bluetooth.BluetoothDevice.class
                    .getMethod("createInsecureL2capChannel", int.class);
            listenSecure = android.bluetooth.BluetoothAdapter.class
                    .getMethod("listenUsingL2capChannel");
            listenInsecure = android.bluetooth.BluetoothAdapter.class
                    .getMethod("listenUsingInsecureL2capChannel");
            getPsm = android.bluetooth.BluetoothServerSocket.class
                    .getMethod("getPsm");
        } catch (Throwable t) {
            createSecureChannel = null;
            createInsecureChannel = null;
            listenSecure = null;
            listenInsecure = null;
            getPsm = null;
        }
    }

    /**
     * True when this device exposes the L2CAP channel APIs (Android 10+).
     */
    static boolean isSupported() {
        init();
        return createSecureChannel != null && listenSecure != null
                && getPsm != null;
    }

    /**
     * Opens an outgoing (client) L2CAP channel socket to the given PSM.
     * The returned socket is not yet connected.
     */
    static android.bluetooth.BluetoothSocket openChannel(
            android.bluetooth.BluetoothDevice device, int psm,
            boolean secure) throws IOException {
        init();
        Method m = secure ? createSecureChannel : createInsecureChannel;
        if (m == null) {
            throw new IOException(
                    "L2CAP channels require Android 10 (API 29) or newer");
        }
        return (android.bluetooth.BluetoothSocket) invoke(m, device,
                new Object[]{Integer.valueOf(psm)});
    }

    /**
     * Opens a listening L2CAP server socket with a dynamically assigned PSM.
     */
    static android.bluetooth.BluetoothServerSocket listen(
            android.bluetooth.BluetoothAdapter adapter, boolean secure)
            throws IOException {
        init();
        Method m = secure ? listenSecure : listenInsecure;
        if (m == null) {
            throw new IOException(
                    "L2CAP channels require Android 10 (API 29) or newer");
        }
        return (android.bluetooth.BluetoothServerSocket) invoke(m, adapter,
                new Object[0]);
    }

    /**
     * Returns the PSM the stack assigned to the given listening socket.
     */
    static int psmOf(android.bluetooth.BluetoothServerSocket serverSocket)
            throws IOException {
        init();
        if (getPsm == null) {
            throw new IOException(
                    "L2CAP channels require Android 10 (API 29) or newer");
        }
        Object v = invoke(getPsm, serverSocket, new Object[0]);
        return ((Integer) v).intValue();
    }

    private static Object invoke(Method m, Object target, Object[] args)
            throws IOException {
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IOException("L2CAP call failed: " + cause);
        } catch (IllegalAccessException iae) {
            throw new IOException("L2CAP call failed: " + iae);
        }
    }
}

/**
 * L2CAP channel over a platform BluetoothSocket (client-opened or accepted
 * from an {@link AndroidL2capServer}).
 */
class AndroidL2capChannel extends L2capChannel {

    private final android.bluetooth.BluetoothSocket socket;

    AndroidL2capChannel(int psm, android.bluetooth.BluetoothSocket socket) {
        super(psm);
        this.socket = socket;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    @Override
    public boolean isOpen() {
        return socket.isConnected();
    }
}

/**
 * Listening L2CAP endpoint wrapping a platform BluetoothServerSocket. The
 * blocking {@code accept()} runs on a dedicated daemon thread -- never on
 * the EDT.
 */
class AndroidL2capServer extends L2capServer {

    private final int psm;
    private final android.bluetooth.BluetoothServerSocket serverSocket;
    private volatile boolean closed;

    AndroidL2capServer(int psm,
            android.bluetooth.BluetoothServerSocket serverSocket) {
        this.psm = psm;
        this.serverSocket = serverSocket;
    }

    @Override
    public int getPsm() {
        return psm;
    }

    @Override
    public AsyncResource<L2capChannel> accept() {
        final AsyncResource<L2capChannel> out =
                new AsyncResource<L2capChannel>();
        if (closed) {
            out.error(new BluetoothException(BluetoothError.IO_ERROR,
                    "The L2CAP server is closed"));
            return out;
        }
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    android.bluetooth.BluetoothSocket s = serverSocket.accept();
                    out.complete(new AndroidL2capChannel(psm, s));
                } catch (IOException ioe) {
                    out.error(new BluetoothException(BluetoothError.IO_ERROR,
                            "L2CAP accept failed: " + ioe.getMessage(), ioe));
                } catch (Throwable ex) {
                    out.error(new BluetoothException(BluetoothError.UNKNOWN,
                            "L2CAP accept failed: " + ex, ex));
                }
            }
        }, "CN1-L2CAP-accept");
        t.setDaemon(true);
        t.start();
        return out;
    }

    @Override
    public void close() {
        closed = true;
        try {
            serverSocket.close();
        } catch (Throwable ignore) {
        }
    }
}
