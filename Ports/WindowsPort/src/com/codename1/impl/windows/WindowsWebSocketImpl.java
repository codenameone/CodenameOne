/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.windows;

import com.codename1.impl.WebSocketImpl;
import com.codename1.io.WebSocketState;
import com.codename1.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RFC 6455 WebSocket client for the Windows port, running over the port's
 * native {@link WindowsSocket} (WinSock). Modeled on the JavaSE/Android clients
 * but written against the Java APIs available in the translated ParparVM
 * runtime: it uses {@link Base64} and a manual URL parse instead of
 * {@code java.util.Base64}/{@code java.net.URI}, and skips the SHA-1
 * Sec-WebSocket-Accept check (no MessageDigest in the runtime) -- the screenshot
 * transport talks to a trusted local server, so validating the 101 status and
 * masking client frames is sufficient. Only the {@code ws://} scheme is
 * supported.
 */
public final class WindowsWebSocketImpl extends WebSocketImpl {

    private static final int OP_CONTINUATION = 0x0;
    private static final int OP_TEXT = 0x1;
    private static final int OP_BINARY = 0x2;
    private static final int OP_CLOSE = 0x8;
    private static final int OP_PING = 0x9;
    private static final int OP_PONG = 0xA;
    private static final int MAX_FRAME_PAYLOAD = 65536;

    private final Random random = new Random();
    private final AtomicReference<WebSocketState> state =
            new AtomicReference<WebSocketState>(WebSocketState.CONNECTING);
    private final Object writeLock = new Object();

    private WindowsSocket socket;
    private InputStream in;
    private OutputStream out;

    /// Strong-refs every impl whose reader thread is alive. The reader parks in a native
    /// blocking recv (socketRead yields the thread), during which the concurrent GC runs;
    /// the conservative native-stack scan can miss the frameless `this` receiver on that
    /// parked thread and sweep the whole impl -> in -> read-buffer chain, so read() then
    /// dereferences a freed buffer (the symbolized WindowsSocket.SocketInput.read
    /// 0xC0000005). A static (a guaranteed GC root) pins the chain regardless of the scan.
    private static final Set<WindowsWebSocketImpl> ACTIVE =
            Collections.synchronizedSet(new HashSet<WindowsWebSocketImpl>());

    public WindowsWebSocketImpl(String url) {
        super(url);
    }

    @Override
    public void connect(final int connectTimeoutMs) {
        ACTIVE.add(this);
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    doHandshake(connectTimeoutMs);
                    state.set(WebSocketState.OPEN);
                    sink().onConnect();
                    readLoop();
                } catch (Exception ex) {
                    fail(ex);
                } finally {
                    ACTIVE.remove(WindowsWebSocketImpl.this);
                }
            }
        }, "WindowsWebSocket");
        t.start();
    }

    private void doHandshake(int connectTimeoutMs) throws IOException {
        String url = getUrl();
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            throw new IOException("Malformed WebSocket URL: " + url);
        }
        String scheme = url.substring(0, schemeEnd).toLowerCase();
        if (!"ws".equals(scheme)) {
            throw new IOException("Unsupported scheme (only ws:// supported): " + scheme);
        }
        String rest = url.substring(schemeEnd + 3);
        int slash = rest.indexOf('/');
        String hostPort = slash < 0 ? rest : rest.substring(0, slash);
        String path = slash < 0 ? "/" : rest.substring(slash);
        if (path.length() == 0) {
            path = "/";
        }
        int colon = hostPort.indexOf(':');
        String host;
        int port;
        if (colon < 0) {
            host = hostPort;
            port = 80;
        } else {
            host = hostPort.substring(0, colon);
            port = Integer.parseInt(hostPort.substring(colon + 1));
        }

        WindowsSocket s = new WindowsSocket(host, port, connectTimeoutMs);
        if (!s.isConnected()) {
            throw new IOException("Failed to connect to " + host + ":" + port);
        }
        this.socket = s;
        this.in = s.getInputStream();
        this.out = s.getOutputStream();

        byte[] keyBytes = new byte[16];
        random.nextBytes(keyBytes);
        String key = Base64.encodeNoNewline(keyBytes);

        StringBuilder req = new StringBuilder();
        req.append("GET ").append(path).append(" HTTP/1.1\r\n");
        req.append("Host: ").append(host);
        if (port != 80) {
            req.append(":").append(port);
        }
        req.append("\r\n");
        req.append("Upgrade: websocket\r\n");
        req.append("Connection: Upgrade\r\n");
        req.append("Sec-WebSocket-Key: ").append(key).append("\r\n");
        req.append("Sec-WebSocket-Version: 13\r\n");
        req.append("\r\n");
        out.write(bytes(req.toString()));
        out.flush();

        String statusLine = readHeaderLine(in);
        if (statusLine == null) {
            throw new IOException("Server closed before handshake response");
        }
        if (statusLine.indexOf(" 101") < 0) {
            throw new IOException("Unexpected handshake response: " + statusLine);
        }
        // Drain the remaining header lines up to the blank separator.
        while (true) {
            String line = readHeaderLine(in);
            if (line == null) {
                throw new IOException("Server closed during handshake response");
            }
            if (line.length() == 0) {
                break;
            }
        }
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
                return str(bytes, 0, bytes.length - 1);
            }
            buf.write(b);
            prev = b;
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
                        sendFrame(OP_PONG, payload);
                        break;
                    case OP_PONG:
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
            sink().onTextMessage(str(payload, 0, payload.length));
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
                reason = str(payload, 2, payload.length - 2);
            }
        }
        if (state.compareAndSet(WebSocketState.OPEN, WebSocketState.CLOSING)) {
            try {
                sendFrame(OP_CLOSE, payload);
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
            sendFrame(OP_CLOSE, new byte[] { 0x03, (byte) 0xE8 }); // 1000
        } catch (IOException ignore) {
        }
    }

    @Override
    public void sendText(String message) {
        if (state.get() != WebSocketState.OPEN) {
            throw new IllegalStateException("WebSocket is not open");
        }
        try {
            sendFrame(OP_TEXT, bytes(message));
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
            sendFrame(OP_BINARY, message);
        } catch (IOException ex) {
            fail(ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public WebSocketState getReadyState() {
        return state.get();
    }

    private void sendFrame(int opcode, byte[] payload) throws IOException {
        synchronized (writeLock) {
            if (out == null) {
                throw new IOException("Connection not established");
            }
            boolean control = (opcode & 0x08) != 0;
            if (control || payload.length <= MAX_FRAME_PAYLOAD) {
                writeSingleFrame(opcode, payload, 0, payload.length, true);
                return;
            }
            int off = 0;
            boolean first = true;
            while (off < payload.length) {
                int len = Math.min(MAX_FRAME_PAYLOAD, payload.length - off);
                int frameOp = first ? opcode : OP_CONTINUATION;
                boolean fin = (off + len) == payload.length;
                writeSingleFrame(frameOp, payload, off, len, fin);
                off += len;
                first = false;
            }
        }
    }

    private void writeSingleFrame(int opcode, byte[] payload, int off, int len, boolean fin) throws IOException {
        int b1 = (fin ? 0x80 : 0) | (opcode & 0x0F);
        out.write(b1);
        // Client frames are always masked per RFC 6455.
        if (len <= 125) {
            out.write(0x80 | len);
        } else if (len <= 0xFFFF) {
            out.write(0x80 | 126);
            out.write((len >>> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(0x80 | 127);
            for (int i = 7; i >= 0; i--) {
                out.write((int) (((long) len >>> (i * 8)) & 0xFF));
            }
        }
        byte[] m = new byte[4];
        random.nextBytes(m);
        out.write(m);
        byte[] masked = new byte[len];
        for (int i = 0; i < len; i++) {
            masked[i] = (byte) (payload[off + i] ^ m[i & 3]);
        }
        out.write(masked);
        out.flush();
    }

    private void closeSocketQuietly() {
        WindowsSocket s = socket;
        if (s != null) {
            s.close();
        }
    }

    private static byte[] bytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return s.getBytes();
        }
    }

    private static String str(byte[] b, int off, int len) {
        try {
            return new String(b, off, len, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(b, off, len);
        }
    }
}
