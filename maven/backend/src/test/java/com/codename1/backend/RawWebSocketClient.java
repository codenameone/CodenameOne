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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A websocket client built to be WRONG on purpose.
 *
 * The functional half of a websocket server is covered by any real client at all
 * talking to it -- and the tree has four of those, pointed at this server by every
 * on-device screenshot run. What none of them can do is send a frame that violates
 * the protocol, because they are all conformant. So the protocol half is covered
 * by nothing unless a client exists that will send an unmasked frame, a reserved
 * opcode, a non-minimal length or a fragmented PING.
 *
 * This is that client. Every field of a frame header is a parameter, including the
 * ones RFC 6455 fixes, and {@link #dribble} puts a frame on the wire one byte at a
 * time so the decoder has to resume mid-header, mid-mask and mid-payload.
 */
final class RawWebSocketClient implements Closeable {
    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;
    private final Random random = new Random(20260921L);

    private String statusLine;
    private Map responseHeaders;
    private byte[] lastPayload;
    private int lastOpcode;
    private boolean lastFin;

    RawWebSocketClient(int port) throws IOException {
        this(port, "/echo", null);
    }

    RawWebSocketClient(int port, String path, String extraHeaders) throws IOException {
        socket = new Socket("127.0.0.1", port);
        socket.setSoTimeout(10000);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        byte[] nonce = new byte[16];
        random.nextBytes(nonce);
        String key = Base64.encode(nonce);
        StringBuilder request = new StringBuilder();
        request.append("GET ").append(path).append(" HTTP/1.1\r\n")
               .append("Host: 127.0.0.1\r\n")
               .append("Upgrade: websocket\r\n")
               // A comma list, which is what a browser sends and what a naive
               // equals("Upgrade") test would miss.
               .append("Connection: keep-alive, Upgrade\r\n")
               .append("Sec-WebSocket-Version: 13\r\n")
               .append("Sec-WebSocket-Key: ").append(key).append("\r\n");
        if(extraHeaders != null) {
            request.append(extraHeaders);
        }
        request.append("\r\n");
        out.write(ascii(request.toString()));
        out.flush();

        statusLine = readLine();
        responseHeaders = new HashMap();
        for(String line = readLine() ; line.length() > 0 ; line = readLine()) {
            int colon = line.indexOf(':');
            if(colon > 0) {
                responseHeaders.put(lower(line.substring(0, colon).trim()),
                        line.substring(colon + 1).trim());
            }
        }
        if(statusLine.startsWith("HTTP/1.1 101")) {
            String expected = WebSocketHandshake.accept(key);
            Object actual = responseHeaders.get("sec-websocket-accept");
            if(!expected.equals(actual)) {
                throw new IOException("Sec-WebSocket-Accept was " + actual + ", expected " + expected);
            }
        }
    }

    String getStatusLine() {
        return statusLine;
    }

    String getResponseHeader(String name) {
        Object value = responseHeaders.get(lower(name));
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Builds a frame with every header field under the caller's control.
     *
     * `lengthWidth` is 0 for the shortest legal form, or 2 or 8 to force a wider
     * one -- which is how a non-minimal length gets onto the wire, since nothing
     * conformant produces one.
     */
    byte[] buildFrame(boolean fin, int rsv, int opcode, boolean masked, byte[] payload,
                      int lengthWidth) {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write((fin ? 0x80 : 0) | ((rsv & 0x7) << 4) | (opcode & 0x0f));
        int length = payload.length;
        int maskBit = masked ? 0x80 : 0;
        if(lengthWidth == 2 || (lengthWidth == 0 && length >= 126 && length <= 0xffff)) {
            frame.write(maskBit | 126);
            frame.write((length >> 8) & 0xff);
            frame.write(length & 0xff);
        } else if(lengthWidth == 8 || (lengthWidth == 0 && length > 0xffff)) {
            frame.write(maskBit | 127);
            for(int iter = 7 ; iter >= 0 ; iter--) {
                frame.write((int)(((long)length >>> (iter * 8)) & 0xff));
            }
        } else {
            frame.write(maskBit | length);
        }
        byte[] key = new byte[4];
        if(masked) {
            random.nextBytes(key);
            frame.write(key, 0, 4);
        }
        byte[] body = new byte[payload.length];
        for(int iter = 0 ; iter < payload.length ; iter++) {
            body[iter] = masked ? (byte)(payload[iter] ^ key[iter & 3]) : payload[iter];
        }
        frame.write(body, 0, body.length);
        return frame.toByteArray();
    }

    void send(boolean fin, int rsv, int opcode, boolean masked, byte[] payload, int lengthWidth)
            throws IOException {
        out.write(buildFrame(fin, rsv, opcode, masked, payload, lengthWidth));
        out.flush();
    }

    void sendText(String value) throws IOException {
        send(true, 0, WebSocketFrames.OP_TEXT, true, Utf8.encode(value), 0);
    }

    void sendBinary(byte[] value) throws IOException {
        send(true, 0, WebSocketFrames.OP_BINARY, true, value, 0);
    }

    void sendRaw(byte[] bytes) throws IOException {
        out.write(bytes);
        out.flush();
    }

    /** One byte per write, so every field of the header spans a read boundary. */
    void dribble(byte[] bytes) throws IOException {
        for(int iter = 0 ; iter < bytes.length ; iter++) {
            out.write(bytes[iter]);
            out.flush();
        }
    }

    /** Reads one frame. Answers false at end of stream. */
    boolean readFrame() throws IOException {
        int first = in.read();
        if(first < 0) {
            return false;
        }
        int second = in.read();
        long length = second & 0x7f;
        if(length == 126) {
            length = (in.read() << 8) | in.read();
        } else if(length == 127) {
            length = 0;
            for(int iter = 0 ; iter < 8 ; iter++) {
                length = (length << 8) | in.read();
            }
        }
        lastPayload = new byte[(int)length];
        int got = 0;
        while(got < lastPayload.length) {
            int read = in.read(lastPayload, got, lastPayload.length - got);
            if(read < 0) {
                break;
            }
            got += read;
        }
        lastOpcode = first & 0x0f;
        lastFin = (first & 0x80) != 0;
        return true;
    }

    int getLastOpcode() {
        return lastOpcode;
    }

    boolean isLastFin() {
        return lastFin;
    }

    byte[] getLastPayload() {
        return lastPayload;
    }

    String getLastText() {
        return Utf8.decode(lastPayload, 0, lastPayload.length);
    }

    /** The close code of the frame just read, or -1 when it carried none. */
    int getLastCloseCode() {
        if(lastOpcode != WebSocketFrames.OP_CLOSE || lastPayload.length < 2) {
            return -1;
        }
        return ((lastPayload[0] & 0xff) << 8) | (lastPayload[1] & 0xff);
    }

    /** Reads frames until a CLOSE arrives, and answers its code. */
    int readUntilClose() throws IOException {
        while(readFrame()) {
            if(lastOpcode == WebSocketFrames.OP_CLOSE) {
                return getLastCloseCode();
            }
        }
        return -1;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        while(true) {
            int c = in.read();
            if(c < 0) {
                return buffer.size() == 0 ? "" : latin1(buffer.toByteArray(), buffer.size());
            }
            if(previous == '\r' && c == '\n') {
                return latin1(buffer.toByteArray(), buffer.size() - 1);
            }
            buffer.write(c);
            previous = c;
        }
    }

    private static String latin1(byte[] bytes, int length) {
        StringBuilder out = new StringBuilder(length);
        for(int iter = 0 ; iter < length ; iter++) {
            out.append((char)(bytes[iter] & 0xff));
        }
        return out.toString();
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte)value.charAt(iter);
        }
        return out;
    }

    /** ASCII fold; see the note on WebSocketSession.asciiLower. */
    private static String lower(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char)(c + 32) : c);
        }
        return out.toString();
    }
}
