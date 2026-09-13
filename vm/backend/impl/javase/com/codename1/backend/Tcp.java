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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.Iterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Java SE twin of the translated Tcp.
 *
 * The public surface is identical on purpose. Everything above this -- Http,
 * HttpServer, the Lambda runtime, the database and S3 clients -- is compiled from
 * ONE shared source tree against whichever of the two implementations is on the
 * path. Nothing above knows which target it is on, and there is no runtime lookup
 * to get wrong. Divergence is caught by the runtime self-test, which runs against
 * both.
 */
public final class Tcp {
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private boolean secure;

    private Tcp(Socket socket) throws IOException {
        rebind(socket);
    }

    private void rebind(Socket replacement) throws IOException {
        this.socket = replacement;
        this.in = replacement.getInputStream();
        this.out = replacement.getOutputStream();
    }

    public static Tcp connect(String host, int port, int timeoutMillis) throws IOException {
        // The name reaches a resolver; see Urls.requireHostName for what a NUL
        // in it does to the packaged arm, and why both arms refuse it.
        Urls.requireHostName(host);
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress(host, port), timeoutMillis);
            // Nagle batches small writes, so on a request/response protocol a
            // header would wait for its body.
            s.setTcpNoDelay(true);
            return new Tcp(s);
        } catch (IOException err) {
            try {
                s.close();
            } catch (IOException ignored) {
                // closing a socket that never connected
            }
            throw new IOException("Connection to " + host + ":" + port + " failed");
        }
    }

    /**
     * Upgrades this connection to TLS. See the translated twin for why this is an
     * upgrade rather than a flag on connect.
     *
     * HTTPS endpoint identification is asked for explicitly: an SSLSocket made
     * this way verifies the certificate chain by default but NOT that the name on
     * it is the host we asked for, which is most of the protection.
     */
    public void startTls(String host) throws IOException {
        startTls(host, null);
    }

    /**
     * As {@link #startTls(String)}, verifying against the PEM bundle at `caFile`
     * INSTEAD of the system trust store. See the translated twin for why.
     */
    public void startTls(String host, String caFile) throws IOException {
        // The trust root is a file name that crosses to a native; see
        // Urls.requireNoNul for what a NUL in it loads instead.
        Urls.requireNoNul("An sslrootcert path", caFile);
        if(secure) {
            return;
        }
        SSLSocketFactory factory = caFile == null
                ? (SSLSocketFactory)SSLSocketFactory.getDefault()
                : factoryTrusting(caFile);
        SSLSocket upgraded = (SSLSocket)factory
                .createSocket(socket, host, socket.getPort(), true);
        SSLParameters parameters = upgraded.getSSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        upgraded.setSSLParameters(parameters);
        upgraded.startHandshake();
        rebind(upgraded);
        secure = true;
    }

    /** Whether this connection is encrypted. */
    public boolean isSecure() {
        return secure;
    }

    /**
     * A factory that trusts exactly the certificates in one PEM bundle. The
     * default trust store is deliberately NOT included: the caller named the
     * roots it wants, and quietly adding more would defeat the point of naming
     * them.
     */
    private static SSLSocketFactory factoryTrusting(String caFile) throws IOException {
        try {
            KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
            trust.load(null, null);
            CertificateFactory certificates = CertificateFactory.getInstance("X.509");
            FileInputStream in = new FileInputStream(caFile);
            try {
                Collection<? extends Certificate> loaded = certificates.generateCertificates(in);
                if(loaded.isEmpty()) {
                    throw new IOException("No certificates in " + caFile);
                }
                int index = 0;
                Iterator<? extends Certificate> it = loaded.iterator();
                while(it.hasNext()) {
                    trust.setCertificateEntry("ca" + (index++), it.next());
                }
            } finally {
                in.close();
            }
            TrustManagerFactory managers = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            managers.init(trust);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, managers.getTrustManagers(), null);
            return context.getSocketFactory();
        } catch (IOException err) {
            throw err;
        } catch (Exception err) {
            throw new IOException("Could not build a trust store from " + caFile
                    + ": " + err.getMessage());
        }
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        return in.read(buffer, offset, length);
    }

    public void write(byte[] buffer, int offset, int length) throws IOException {
        out.write(buffer, offset, length);
        out.flush();
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // already gone
        }
    }
}
