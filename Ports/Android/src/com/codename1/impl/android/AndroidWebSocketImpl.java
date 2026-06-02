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

import com.codename1.impl.WebSocketImpl;
import com.codename1.io.WebSocketState;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import android.util.Base64;
import javax.net.ssl.SSLSocketFactory;

/**
 * Hand-rolled RFC 6455 WebSocket client used by the Android port. Runs over
 * java.net.Socket / javax.net.ssl.SSLSocket. The same logic is duplicated in
 * JavaSEWebSocketImpl -- the two ports cannot share source because they're
 * separate Maven modules with different source levels; if you change one,
 * change the other.
 *
 * Compiled at Java 6 source level -- no diamond operator, no try-with-resources,
 * no multi-catch.
 */
class AndroidWebSocketImpl extends WebSocketImpl {

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset ASCII = Charset.forName("ISO-8859-1");
    private static final int OP_CONTINUATION = 0x0;
    private static final int OP_TEXT = 0x1;
    private static final int OP_BINARY = 0x2;
    private static final int OP_CLOSE = 0x8;
    private static final int OP_PING = 0x9;
    private static final int OP_PONG = 0xA;
    private static final int MAX_FRAME_PAYLOAD = 65536;

    private final SecureRandom random = new SecureRandom();
    private final AtomicReference<WebSocketState> state =
            new AtomicReference<WebSocketState>(WebSocketState.CONNECTING);
    private final Object writeLock = new Object();

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    AndroidWebSocketImpl(String url) {
        super(url);
    }

    @Override
    public void connect(final int connectTimeoutMs) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    doHandshake(connectTimeoutMs);
                    state.set(WebSocketState.OPEN);
                    sink().onConnect();
                    readLoop();
                } catch (Exception ex) {
                    fail(ex);
                }
            }
        }, "WebSocket-" + getUrl());
        t.setDaemon(true);
        t.start();
    }

    private void doHandshake(int connectTimeoutMs) throws IOException {
        URI uri = URI.create(getUrl());
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IOException("Missing scheme in WebSocket URL");
        }
        boolean secure = scheme.equalsIgnoreCase("wss") || scheme.equalsIgnoreCase("https");
        boolean plain = scheme.equalsIgnoreCase("ws") || scheme.equalsIgnoreCase("http");
        if (!secure && !plain) {
            throw new IOException("Unsupported scheme: " + scheme);
        }
        String host = uri.getHost();
        if (host == null) {
            throw new IOException("Missing host in WebSocket URL");
        }
        int port = uri.getPort();
        if (port < 0) {
            port = secure ? 443 : 80;
        }
        String path = uri.getRawPath();
        if (path == null || path.length() == 0) {
            path = "/";
        }
        if (uri.getRawQuery() != null) {
            path = path + "?" + uri.getRawQuery();
        }

        Socket s;
        if (secure) {
            s = SSLSocketFactory.getDefault().createSocket();
        } else {
            s = new Socket();
        }
        s.connect(new InetSocketAddress(host, port), connectTimeoutMs);
        this.socket = s;
        this.in = s.getInputStream();
        this.out = s.getOutputStream();

        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        String key = Base64.encodeToString(keyBytes, Base64.NO_WRAP);

        StringBuilder req = new StringBuilder();
        req.append("GET ").append(path).append(" HTTP/1.1\r\n");
        req.append("Host: ").append(host);
        if ((secure && port != 443) || (!secure && port != 80)) {
            req.append(":").append(port);
        }
        req.append("\r\n");
        req.append("Upgrade: websocket\r\n");
        req.append("Connection: Upgrade\r\n");
        req.append("Sec-WebSocket-Key: ").append(key).append("\r\n");
        req.append("Sec-WebSocket-Version: 13\r\n");
        String[] subs = requestedSubprotocols();
        if (subs != null && subs.length > 0) {
            req.append("Sec-WebSocket-Protocol: ");
            for (int i = 0; i < subs.length; i++) {
                if (i > 0) req.append(", ");
                req.append(subs[i]);
            }
            req.append("\r\n");
        }
        req.append("\r\n");
        out.write(req.toString().getBytes(ASCII));
        out.flush();

        String statusLine = readHeaderLine(in);
        if (statusLine == null) {
            throw new IOException("Server closed before handshake response");
        }
        if (!statusLine.startsWith("HTTP/1.1 101") && !statusLine.startsWith("HTTP/1.0 101")) {
            throw new IOException("Unexpected handshake response: " + statusLine);
        }
        Map<String, String> headers = new HashMap<String, String>();
        while (true) {
            String line = readHeaderLine(in);
            if (line == null) {
                throw new IOException("Server closed during handshake response");
            }
            if (line.length() == 0) {
                break;
            }
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colon + 1).trim();
                headers.put(name, value);
            }
        }

        String upgrade = headers.get("upgrade");
        String connection = headers.get("connection");
        String accept = headers.get("sec-websocket-accept");
        if (upgrade == null || !"websocket".equalsIgnoreCase(upgrade)) {
            throw new IOException("Missing Upgrade: websocket in handshake response");
        }
        if (connection == null || connection.toLowerCase(Locale.ROOT).indexOf("upgrade") < 0) {
            throw new IOException("Missing Connection: upgrade in handshake response");
        }
        String expected = expectedAccept(key);
        if (accept == null || !accept.equals(expected)) {
            throw new IOException("Sec-WebSocket-Accept mismatch");
        }
        setSelectedSubprotocol(headers.get("sec-websocket-protocol"));
    }

    private static String readHeaderLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(64);
        int prev = -1;
        while (true) {
            int b = in.read();
            if (b < 0) {
                if (buf.size() == 0) {
                    return null;
                }
                throw new EOFException("EOF in HTTP header");
            }
            if (prev == '\r' && b == '\n') {
                byte[] bytes = buf.toByteArray();
                return new String(bytes, 0, bytes.length - 1, ASCII);
            }
            buf.write(b);
            prev = b;
        }
    }

    private static String expectedAccept(String key) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest((key + GUID).getBytes(ASCII));
            return Base64.encodeToString(digest, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void readLoop() {
        ByteArrayOutputStream fragmentBuffer = null;
        int fragmentOpcode = -1;
        DataInputStream dis = new DataInputStream(in);
        try {
            while (state.get() == WebSocketState.OPEN || state.get() == WebSocketState.CLOSING) {
                int b1 = dis.read();
                if (b1 < 0) {
                    throw new EOFException("Server closed connection");
                }
                int b2 = dis.read();
                if (b2 < 0) {
                    throw new EOFException("Server closed connection");
                }
                boolean fin = (b1 & 0x80) != 0;
                int opcode = b1 & 0x0F;
                boolean masked = (b2 & 0x80) != 0;
                long payloadLen = b2 & 0x7F;
                if (payloadLen == 126) {
                    payloadLen = dis.readUnsignedShort();
                } else if (payloadLen == 127) {
                    payloadLen = dis.readLong();
                    if (payloadLen < 0) {
                        throw new IOException("Payload too large");
                    }
                }
                byte[] mask = null;
                if (masked) {
                    mask = new byte[4];
                    dis.readFully(mask);
                }
                if (payloadLen > Integer.MAX_VALUE) {
                    throw new IOException("Payload exceeds int max");
                }
                byte[] payload = new byte[(int) payloadLen];
                dis.readFully(payload);
                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ mask[i & 3]);
                    }
                }

                switch (opcode) {
                    case OP_CONTINUATION:
                        if (fragmentBuffer == null) {
                            throw new IOException("CONTINUATION without prior fragment");
                        }
                        fragmentBuffer.write(payload);
                        if (fin) {
                            deliverMessage(fragmentOpcode, fragmentBuffer.toByteArray());
                            fragmentBuffer = null;
                            fragmentOpcode = -1;
                        }
                        break;
                    case OP_TEXT:
                    case OP_BINARY:
                        if (fin) {
                            deliverMessage(opcode, payload);
                        } else {
                            fragmentBuffer = new ByteArrayOutputStream();
                            fragmentBuffer.write(payload);
                            fragmentOpcode = opcode;
                        }
                        break;
                    case OP_PING:
                        sendFrame(OP_PONG, payload, true);
                        break;
                    case OP_PONG:
                        // ignore
                        break;
                    case OP_CLOSE:
                        handleClose(payload);
                        return;
                    default:
                        throw new IOException("Unsupported opcode: " + opcode);
                }
            }
        } catch (Exception ex) {
            fail(ex);
        }
    }

    private void deliverMessage(int opcode, byte[] payload) {
        if (opcode == OP_TEXT) {
            sink().onTextMessage(new String(payload, UTF8));
        } else {
            sink().onBinaryMessage(payload);
        }
    }

    private void handleClose(byte[] payload) {
        int code = 1005;
        String reason = "";
        if (payload.length >= 2) {
            code = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
            if (payload.length > 2) {
                reason = new String(payload, 2, payload.length - 2, UTF8);
            }
        }
        if (state.compareAndSet(WebSocketState.OPEN, WebSocketState.CLOSING)) {
            try {
                sendFrame(OP_CLOSE, payload, true);
            } catch (IOException ignore) {
            }
        }
        closeSocketQuietly();
        state.set(WebSocketState.CLOSED);
        sink().onClose(code, reason);
    }

    private void fail(Exception ex) {
        WebSocketState prev = state.getAndSet(WebSocketState.CLOSED);
        closeSocketQuietly();
        if (prev != WebSocketState.CLOSED) {
            sink().onError(ex);
        }
    }

    @Override
    public void close() {
        if (!state.compareAndSet(WebSocketState.OPEN, WebSocketState.CLOSING)) {
            return;
        }
        try {
            byte[] codeBytes = new byte[] { 0x03, (byte) 0xE8 }; // 1000
            sendFrame(OP_CLOSE, codeBytes, true);
        } catch (IOException ignore) {
        }
    }

    @Override
    public void sendText(String message) {
        if (state.get() != WebSocketState.OPEN) {
            throw new IllegalStateException("WebSocket is not open");
        }
        try {
            sendFrame(OP_TEXT, message.getBytes(UTF8), true);
        } catch (IOException ex) {
            fail(ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void sendBinary(byte[] message) {
        if (state.get() != WebSocketState.OPEN) {
            throw new IllegalStateException("WebSocket is not open");
        }
        try {
            sendFrame(OP_BINARY, message, true);
        } catch (IOException ex) {
            fail(ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public WebSocketState getReadyState() {
        return state.get();
    }

    private void sendFrame(int opcode, byte[] payload, boolean mask) throws IOException {
        synchronized (writeLock) {
            if (out == null) {
                throw new IOException("Connection not established");
            }
            boolean control = (opcode & 0x08) != 0;
            if (control || payload.length <= MAX_FRAME_PAYLOAD) {
                writeSingleFrame(opcode, payload, 0, payload.length, true, mask);
                return;
            }
            int off = 0;
            boolean first = true;
            while (off < payload.length) {
                int len = Math.min(MAX_FRAME_PAYLOAD, payload.length - off);
                int frameOp = first ? opcode : OP_CONTINUATION;
                boolean fin = (off + len) == payload.length;
                writeSingleFrame(frameOp, payload, off, len, fin, mask);
                off += len;
                first = false;
            }
        }
    }

    private void writeSingleFrame(int opcode, byte[] payload, int off, int len,
                                  boolean fin, boolean mask) throws IOException {
        int b1 = (fin ? 0x80 : 0) | (opcode & 0x0F);
        out.write(b1);
        int maskBit = mask ? 0x80 : 0;
        if (len <= 125) {
            out.write(maskBit | len);
        } else if (len <= 0xFFFF) {
            out.write(maskBit | 126);
            out.write((len >>> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(maskBit | 127);
            for (int i = 7; i >= 0; i--) {
                out.write((int) (((long) len >>> (i * 8)) & 0xFF));
            }
        }
        if (mask) {
            byte[] m = new byte[4];
            random.nextBytes(m);
            out.write(m);
            byte[] masked = new byte[len];
            for (int i = 0; i < len; i++) {
                masked[i] = (byte) (payload[off + i] ^ m[i & 3]);
            }
            out.write(masked);
        } else if (len > 0) {
            out.write(payload, off, len);
        }
        out.flush();
    }

    private void closeSocketQuietly() {
        Socket s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignore) {
            }
        }
    }
}
