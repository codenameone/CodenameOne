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
import java.security.cert.CertificateEncodingException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/// Java SE twin of the translated Tcp.
///
/// The public surface is identical on purpose. Everything above this -- Http,
/// HttpServer, the Lambda runtime, the database and S3 clients -- is compiled from
/// ONE shared source tree against whichever of the two implementations is on the
/// path. Nothing above knows which target it is on, and there is no runtime lookup
/// to get wrong. Divergence is caught by the runtime self-test, which runs against
/// both.
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
        if (deadline > 0) {
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
            throw new IOException("Connection to " + host + ":" + port + " failed", err);
        }
    }

    /// Upgrades this connection to TLS. See the translated twin for why this is an
    /// upgrade rather than a flag on connect.
    ///
    /// HTTPS endpoint identification is asked for explicitly: an SSLSocket made
    /// this way verifies the certificate chain by default but NOT that the name on
    /// it is the host we asked for, which is most of the protection.
    public void startTls(String host) throws IOException {
        startTls(host, null);
    }

    /// How long an outbound handshake may take, in milliseconds.
    ///
    /// CN1_TLS_HANDSHAKE_MS, default 15000, 0 or less for no bound. The
    /// packaged arm reads the same name in C, so a deployment tunes one knob.
    private static int handshakeBudgetMillis() {
        String raw = System.getenv("CN1_TLS_HANDSHAKE_MS");
        if (raw != null && raw.length() > 0) {
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException malformed) {
                // The default is a better answer than refusing to connect.
            }
        }
        return 15000;
    }

    /// As [#startTls(String)], verifying against the PEM bundle at `caFile`
    /// INSTEAD of the system trust store. See the translated twin for why.
    public void startTls(String host, String caFile) throws IOException {
        startTls(host, caFile, true);
    }

    /// As [#startTls(String,String)], with the name check optional.
    ///
    /// `verifyHost` false still verifies the chain against `caFile` and skips
    /// only the match between the certificate and `host`: what
    /// `sslmode=verify-ca` asks for. It is refused without a `caFile`, because
    /// a public root vouches for names, and a chain to one without the name is
    /// a certificate for anybody.
    public void startTls(String host, String caFile, boolean verifyHost)
            throws IOException {
        upgrade(host, caFile, verifyHost, null, null);
    }

    /// [#startTls(String, String, boolean)] offering one ALPN protocol, which
    /// TDS 8.0 asks for as `tds/8.0`.
    public void startTls(String host, String caFile, boolean verifyHost, String alpn)
            throws IOException {
        upgrade(host, caFile, verifyHost, null, alpn);
    }

    /// Starts TLS with the handshake carried INSIDE TDS packets, which is how
    /// SQL Server negotiates encryption before TDS 8.0: every handshake flight
    /// travels as the payload of packets of type `packetType` (PRELOGIN), each
    /// under the eight-byte TDS header, and once the handshake completes the
    /// connection is ordinary TLS with no framing at all.
    ///
    /// The certificate and host name rules are [#startTls(String, String, boolean)]'s.
    /// The handshake is held to TLS 1.2: a server that tunnels it does not
    /// speak 1.3 this way, and TDS 8.0, which does, starts TLS before anything
    /// else and uses [#startTls(String, String, boolean)].
    public void startTlsTunnelled(String host, String caFile, boolean verifyHost,
                                  int packetType) throws IOException {
        upgrade(host, caFile, verifyHost, new TunnelSocket(socket, packetType), null);
    }

    private void upgrade(String host, String caFile, boolean verifyHost, TunnelSocket tunnel, String alpn)
            throws IOException {
        if (!verifyHost && caFile == null) {
            throw new IOException("Skipping the host name check needs a trust "
                    + "root of its own (sslrootcert)");
        }
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
        if (secure) {
            return;
        }
        SSLSocketFactory factory = caFile == null
                ? (SSLSocketFactory) SSLSocketFactory.getDefault()
                : factoryTrusting(caFile);
        // Checked rather than cast: an SSLSocketFactory that answered with a plain
        // Socket would otherwise surface as a ClassCastException, not as the
        // IOException every caller of startTls handles, and the socket would leak.
        Socket created = factory.createSocket(tunnel == null ? socket : tunnel, host, socket.getPort(), true); //NOPMD CloseResource - closed below unless it is TLS, and then it becomes this connection's socket
        if (!(created instanceof SSLSocket)) {
            created.close();
            throw new IOException("The TLS socket factory did not produce a TLS socket");
        }
        SSLSocket upgraded = (SSLSocket) created; //NOPMD CloseResource - becomes this connection's socket in rebind()
        SSLParameters parameters = upgraded.getSSLParameters();
        if (verifyHost) {
            parameters.setEndpointIdentificationAlgorithm("HTTPS");
        }
        if (alpn != null) {
            parameters.setApplicationProtocols(new String[] {alpn});
        }
        upgraded.setSSLParameters(parameters);
        if (tunnel != null) {
            upgraded.setEnabledProtocols(new String[] {"TLSv1.2"});
        }
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
        if (handshakeMillis > 0) {
            upgraded.setSoTimeout(handshakeMillis);
        }
        try {
            upgraded.startHandshake();
        } finally {
            if (handshakeMillis > 0) {
                // Back to what it was, or an idle connection's first read after
                // the handshake would inherit a deadline nobody asked for.
                try {
                    upgraded.setSoTimeout(previousTimeout);
                } catch (IOException closedByFailure) { //NOPMD EmptyCatchBlock - see below
                    // A handshake that failed has closed the socket, and the
                    // complaint about that would replace the reason it failed:
                    // the certificate error the caller needs to read.
                }
            }
        }
        if (alpn != null && !alpn.equals(upgraded.getApplicationProtocol())) {
            // An endpoint that ignored ALPN (a TLS terminator, a server that
            // does not speak the protocol) is not what this connection is for.
            upgraded.close();
            throw new IOException("The server did not select ALPN protocol " + alpn);
        }
        if (tunnel != null) {
            // The handshake is over, and so is the framing: from here the TLS
            // records go to the socket as they are.
            tunnel.passThrough();
        }
        rebind(upgraded);
        secure = true;
    }

    /// The socket a tunnelled handshake is layered over: the real one, except
    /// that while the handshake runs its streams wrap what is written in TDS
    /// packets and unwrap what is read.
    private static final class TunnelSocket extends Socket {
        private static final int PACKET = 4096;
        private final Socket real;
        private final int packetType;
        private final InputStream realIn;
        private final OutputStream realOut;
        private boolean framed = true;
        private int remaining;
        private final java.io.ByteArrayOutputStream pending = new java.io.ByteArrayOutputStream();

        TunnelSocket(Socket real, int packetType) throws IOException {
            this.real = real;
            this.packetType = packetType;
            this.realIn = real.getInputStream();
            this.realOut = real.getOutputStream();
        }

        void passThrough() {
            framed = false;
        }

        private final InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                byte[] one = new byte[1];
                int got = read(one, 0, 1);
                return got < 0 ? -1 : one[0] & 0xff;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) throws IOException {
                if (!framed) {
                    return realIn.read(buffer, offset, length);
                }
                while (remaining == 0) {
                    byte[] header = new byte[8];
                    int have = 0;
                    while (have < 8) {
                        int got = realIn.read(header, have, 8 - have);
                        if (got < 0) {
                            return -1;
                        }
                        have += got;
                    }
                    int total = ((header[2] & 0xff) << 8) | (header[3] & 0xff);
                    if (total < 8) {
                        throw new IOException("A malformed packet arrived during the TLS handshake");
                    }
                    remaining = total - 8;
                }
                int got = realIn.read(buffer, offset, Math.min(length, remaining));
                if (got > 0) {
                    remaining -= got;
                }
                return got;
            }
        };

        private final OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                write(new byte[] {(byte) b}, 0, 1);
            }

            @Override
            public void write(byte[] buffer, int offset, int length) throws IOException {
                if (framed) {
                    pending.write(buffer, offset, length);
                } else {
                    realOut.write(buffer, offset, length);
                }
            }

            /// A flight is complete when the TLS layer flushes it, and that is
            /// when it goes out, as one message of as many packets as it needs.
            @Override
            public void flush() throws IOException {
                if (framed && pending.size() > 0) {
                    byte[] data = pending.toByteArray();
                    pending.reset();
                    int at = 0;
                    int id = 1;
                    while (at < data.length) {
                        int chunk = Math.min(PACKET - 8, data.length - at);
                        byte[] header = new byte[8];
                        header[0] = (byte) packetType;
                        header[1] = (byte) (at + chunk == data.length ? 1 : 0);
                        header[2] = (byte) ((chunk + 8) >> 8);
                        header[3] = (byte) (chunk + 8);
                        header[6] = (byte) id++;
                        realOut.write(header, 0, 8);
                        realOut.write(data, at, chunk);
                        at += chunk;
                    }
                }
                realOut.flush();
            }
        };

        @Override
        public InputStream getInputStream() {
            return in;
        }

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public void close() throws IOException {
            real.close();
        }

        @Override
        public boolean isClosed() {
            return real.isClosed();
        }

        @Override
        public boolean isConnected() {
            return real.isConnected();
        }

        @Override
        public void setSoTimeout(int timeout) throws java.net.SocketException {
            real.setSoTimeout(timeout);
        }

        @Override
        public int getSoTimeout() throws java.net.SocketException {
            return real.getSoTimeout();
        }

        @Override
        public java.net.InetAddress getInetAddress() {
            return real.getInetAddress();
        }

        @Override
        public int getPort() {
            return real.getPort();
        }

        @Override
        public int getLocalPort() {
            return real.getLocalPort();
        }

        @Override
        public java.net.SocketAddress getRemoteSocketAddress() {
            return real.getRemoteSocketAddress();
        }

        @Override
        public void setTcpNoDelay(boolean on) throws java.net.SocketException {
            real.setTcpNoDelay(on);
        }

        @Override
        public void shutdownInput() throws IOException {
            real.shutdownInput();
        }

        @Override
        public void shutdownOutput() throws IOException {
            real.shutdownOutput();
        }

        @Override
        public boolean isInputShutdown() {
            return real.isInputShutdown();
        }

        @Override
        public boolean isOutputShutdown() {
            return real.isOutputShutdown();
        }
    }

    /// Whether this connection is encrypted.
    public boolean isSecure() {
        return secure;
    }

    /// The certificates the server presented in the TLS handshake, leaf first,
    /// each as DER; empty while the connection is plaintext. For pinning a
    /// server's key or reading its expiry -- what `openssl s_client -showcerts`
    /// prints. The chain has already been verified when this answers: an
    /// untrusted one failed [#startTls(String)].
    public byte[][] getPeerCertificates() throws IOException {
        if (!secure || !(socket instanceof SSLSocket)) {
            return new byte[0][];
        }
        try {
            Certificate[] chain = ((SSLSocket) socket).getSession().getPeerCertificates();
            byte[][] out = new byte[chain.length][];
            for (int iter = 0 ; iter < chain.length ; iter++) {
                out[iter] = chain[iter].getEncoded();
            }
            return out;
        } catch (CertificateEncodingException err) {
            throw new IOException("A peer certificate could not be encoded", err);
        }
    }

    /// A factory that trusts exactly the certificates in one PEM bundle. The
    /// default trust store is deliberately NOT included: the caller named the
    /// roots it wants, and quietly adding more would defeat the point of naming
    /// them.
    private static SSLSocketFactory factoryTrusting(String caFile) throws IOException {
        try {
            KeyStore trust = KeyStore.getInstance(KeyStore.getDefaultType());
            trust.load(null, null);
            CertificateFactory certificates = CertificateFactory.getInstance("X.509");
            FileInputStream in = new FileInputStream(caFile);
            try {
                Collection<? extends Certificate> loaded = certificates.generateCertificates(in);
                if (loaded.isEmpty()) {
                    throw new IOException("No certificates in " + caFile);
                }
                int index = 0;
                Iterator<? extends Certificate> it = loaded.iterator();
                while (it.hasNext()) {
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
                    + ": " + err.getMessage(), err);
        }
    }

    /// A receive deadline for this connection, in milliseconds; 0 for none.
    ///
    /// The packaged arm sets SO_RCVTIMEO and SO_SNDTIMEO; an SSLSocket here
    /// expresses only the receive half, through setSoTimeout, and that is said
    /// rather than implied. Without one, a peer that finishes connecting and then
    /// stops answering holds the calling thread indefinitely.
    public void setReadTimeout(int millis) throws IOException {
        if (millis < 0) {
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
