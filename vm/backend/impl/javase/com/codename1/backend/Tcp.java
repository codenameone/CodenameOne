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
        // THE DEADLINE COMES ACROSS. A TLS upgrade replaces the socket, and a
        // read deadline set before it would have been left on the plaintext one
        // that nothing reads from again -- so a connection that asked to be
        // bounded silently stopped being, at exactly the point it started
        // carrying anything worth protecting. Carried here rather than re-applied
        // by each caller, so a caller that upgrades cannot forget.
        int deadline = socket == null ? 0 : socket.getSoTimeout();
        this.socket = replacement;
        if(deadline > 0) {
            replacement.setSoTimeout(deadline);
        }
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
    /**
     * How long an outbound handshake may take, in milliseconds.
     *
     * <p>CN1_TLS_HANDSHAKE_MS, default 15000, 0 or less for no bound. The
     * packaged arm reads the same name in C, so a deployment tunes one knob.
     */
    private static int handshakeBudgetMillis() {
        String raw = System.getenv("CN1_TLS_HANDSHAKE_MS");
        if(raw != null && raw.length() > 0) {
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException malformed) {
                // The default is a better answer than refusing to connect.
            }
        }
        return 15000;
    }

    public void startTls(String host, String caFile) throws IOException {
        // BOTH STRINGS, and the name is the one that decides who the peer is
        // allowed to be. It crosses to a native as a C string, so a NUL inside it
        // ends the name there and OpenSSL verifies the certificate against the
        // prefix alone: "attacker.example\0.trusted.example" satisfies a caller's
        // own endsWith(".trusted.example") check and then verifies as
        // "attacker.example". connect() validating the address it dialled does not
        // cover this, because the verification name is supplied separately and may
        // come from somewhere else entirely.
        Urls.requireHostName(host);
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
        // BOUNDED, for the reason the packaged arm's comment gives at its own
        // SSL_connect: connectTimeout was spent reaching the port, and a peer that
        // accepts TCP and then stops talking holds this thread for as long as it
        // likes. CN1_TLS_HANDSHAKE_MS is read by both arms from the same name so
        // they agree; 0 or less leaves it unbounded.
        //
        // A READ timeout rather than a total one, which is the weaker of the two
        // and is what this arm can express: an SSLSocket handshake is several
        // reads and a peer answering just inside each window stretches the whole.
        // The packaged arm bounds the total because its loop is written against
        // poll(); saying so here is better than implying the two are identical.
        int handshakeMillis = handshakeBudgetMillis();
        int previousTimeout = upgraded.getSoTimeout();
        if(handshakeMillis > 0) {
            upgraded.setSoTimeout(handshakeMillis);
        }
        try {
            upgraded.startHandshake();
        } finally {
            if(handshakeMillis > 0) {
                // Back to what it was, or an idle connection's first read after
                // the handshake would inherit a deadline nobody asked for.
                upgraded.setSoTimeout(previousTimeout);
            }
        }
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

    /**
     * A receive deadline for this connection, in milliseconds; 0 for none.
     *
     * <p>The packaged arm sets SO_RCVTIMEO and SO_SNDTIMEO; an SSLSocket here
     * expresses only the receive half, through setSoTimeout, and that is said
     * rather than implied. Without one, a peer that finishes connecting and then
     * stops answering holds the calling thread indefinitely.
     */
    public void setReadTimeout(int millis) throws IOException {
        if(millis < 0) {
            throw new IllegalArgumentException("read timeout must not be negative: "
                    + millis);
        }
        socket.setSoTimeout(millis);
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
