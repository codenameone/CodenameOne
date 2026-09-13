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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimal HTTP/1.1 client over [Tcp]. Enough to speak a host runtime's control
 * protocol - the AWS Lambda Runtime API, for instance - without pulling in a
 * platform layer or a TLS stack. Plaintext only: the Lambda Runtime API is
 * plaintext on the loopback interface, and anything facing the public internet
 * terminates TLS in front of the process.
 */
public final class Http {
    private Http() {
    }

    /** One HTTP response: status line, headers and body. */
    public static final class Response {
        private final int status;
        private final List headerNames;
        private final List headerValues;
        private final byte[] body;

        Response(int status, List headerNames, List headerValues, byte[] body) {
            this.status = status;
            this.headerNames = headerNames;
            this.headerValues = headerValues;
            this.body = body;
        }

        public int getStatus() {
            return status;
        }

        public byte[] getBody() {
            return body;
        }

        public String getBodyAsString() {
            try {
                return new String(body, "UTF-8");
            } catch (IOException err) {
                return new String(body);
            }
        }

        /** Case-insensitive, because header case is not guaranteed by anything. */
        public String getHeader(String name) {
            for(int iter = 0 ; iter < headerNames.size() ; iter++) {
                if(((String)headerNames.get(iter)).equalsIgnoreCase(name)) {
                    return (String)headerValues.get(iter);
                }
            }
            return null;
        }
    }

    /**
     * The Host field's value: "[::1]:8080" for an IPv6 literal, "host:8080"
     * otherwise.
     *
     * An address literal with colons in it has to be bracketed -- RFC 3986 gives
     * the authority no other way to say where the address ends and the port
     * begins -- and Tcp.connect accepts "::1" quite happily, so an otherwise
     * valid request over IPv6 went out as "Host: ::1:8080" and a conforming
     * server refused it. A colon cannot appear in a registered name, so it is
     * what tells the two apart.
     */
    static String authority(String host, int port) {
        String named = host != null && host.indexOf(':') >= 0
                && host.charAt(0) != '[' ? "[" + host + "]" : host;
        return named + ":" + port;
    }

    public static Response get(String host, int port, String path) throws IOException {
        return request(host, port, "GET", path, null);
    }

    public static Response post(String host, int port, String path, byte[] body) throws IOException {
        return request(host, port, "POST", path, body);
    }

    public static Response request(String host, int port, String method, String path, byte[] body) throws IOException {
        // BEFORE THE SOCKET, and not only in Web: this is a public client of its
        // own, and both fields of the request line are written into it verbatim.
        // Tcp.connect checks the host; these two were nobody's.
        HeaderLines.requireMethod(method);
        HeaderLines.requireOriginForm(path);
        Tcp socket = Tcp.connect(host, port, 0);
        try {
            StringBuilder head = new StringBuilder();
            head.append(method).append(' ').append(path).append(" HTTP/1.1\r\n");
            head.append("Host: ").append(authority(host, port)).append("\r\n");
            head.append("Connection: close\r\n");
            head.append("Content-Length: ").append(body == null ? 0 : body.length).append("\r\n");
            head.append("\r\n");
            byte[] headBytes = head.toString().getBytes("UTF-8");
            socket.write(headBytes, 0, headBytes.length);
            if(body != null && body.length > 0) {
                socket.write(body, 0, body.length);
            }
            return readResponse(socket);
        } finally {
            socket.close();
        }
    }

    private static Response readResponse(Tcp socket) throws IOException {
        // "Connection: close" is requested above, so the whole response can be read
        // to end-of-stream and parsed in memory. That keeps the parser free of the
        // chunked/keep-alive state machine, at the cost of one connection per call --
        // which on loopback is cheaper than the code it saves.
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        while(true) {
            int n = socket.read(chunk, 0, chunk.length);
            if(n <= 0) {
                break;
            }
            raw.write(chunk, 0, n);
        }
        byte[] all = raw.toByteArray();
        if(all.length == 0) {
            // The peer closed without sending anything. Reporting this as malformed
            // HTTP sent every "the host went away" shutdown to the wrong diagnosis.
            throw new IOException("Connection closed before any response was sent");
        }
        int headerEnd = indexOfHeaderEnd(all);
        if(headerEnd < 0) {
            throw new IOException("Malformed HTTP response: no header terminator");
        }
        String headerText = new String(all, 0, headerEnd, "UTF-8");
        String[] lines = split(headerText, "\r\n");
        if(lines.length == 0) {
            throw new IOException("Malformed HTTP response: empty");
        }
        int status = parseStatus(lines[0]);
        List names = new ArrayList();
        List values = new ArrayList();
        for(int iter = 1 ; iter < lines.length ; iter++) {
            int colon = lines[iter].indexOf(':');
            if(colon > 0) {
                names.add(lines[iter].substring(0, colon).trim());
                values.add(lines[iter].substring(colon + 1).trim());
            }
        }
        int bodyStart = headerEnd + 4;
        byte[] bodyBytes = new byte[all.length - bodyStart];
        System.arraycopy(all, bodyStart, bodyBytes, 0, bodyBytes.length);
        // Everything before EOF is not the same as the whole body. A connection
        // that dies mid-payload leaves a SHORT one, and handing that back as a
        // complete response is how a Lambda handler is invoked on half an event
        // and produces side effects from input the caller never sent. The
        // declared length is the peer's own statement of what it owed, so a
        // shortfall is a transport failure and is reported as one.
        int declared = declaredLength(names, values);
        if(declared >= 0 && bodyBytes.length < declared) {
            throw new IOException("The response body stopped after " + bodyBytes.length
                    + " of the " + declared + " byte(s) its Content-Length declared, so "
                    + "the connection failed part way through it");
        }
        return new Response(status, names, values, decodeBody(bodyBytes, names, values));
    }

    /**
     * The Content-Length the peer declared, or -1 when it declared none or the
     * value is not a number. A chunked response has no Content-Length, so the
     * check above simply does not apply to one.
     */
    private static int declaredLength(List names, List values) {
        for(int iter = 0 ; iter < names.size() ; iter++) {
            if("content-length".equalsIgnoreCase(String.valueOf(names.get(iter)))) {
                try {
                    return Integer.parseInt(String.valueOf(values.get(iter)).trim());
                } catch (NumberFormatException err) {
                    return -1;
                }
            }
        }
        return -1;
    }

    /**
     * Strips whatever Transfer-Encoding the peer applied, which is usually none.
     *
     * Reading to EOF is not the same as reading the body: `Connection: close` ends
     * the message but does not remove chunk framing, so a server that answers
     * chunked hands back size lines and terminators mixed into the payload. The
     * Lambda Runtime API sends Content-Length today, which is the reason to handle
     * this rather than a reason not to -- nothing here fails until the day it does.
     *
     * A coding this client cannot undo is an error rather than a pass-through. The
     * one outcome worth ruling out is returning framed bytes as though they were
     * the body, because the handler then parses garbage and blames its own input.
     */
    private static byte[] decodeBody(byte[] body, List names, List values) throws IOException {
        String encoding = joinedHeader(names, values, "Transfer-Encoding");
        if(encoding == null) {
            return body;
        }
        String[] codings = split(encoding, ",");
        boolean chunked = false;
        for(int iter = 0 ; iter < codings.length ; iter++) {
            String coding = codings[iter].trim();
            if(coding.length() == 0 || coding.equalsIgnoreCase("identity")) {
                continue;
            }
            // Case folding a protocol token with toLowerCase() is locale sensitive and
            // wrong on a Turkish device; equalsIgnoreCase compares character by
            // character and is not.
            if(coding.equalsIgnoreCase("chunked") && iter == codings.length - 1) {
                chunked = true;
                continue;
            }
            throw new IOException("Unsupported Transfer-Encoding: " + encoding);
        }
        return chunked ? dechunk(body) : body;
    }

    /**
     * Every value sent under one header name, joined the way a single line would
     * have read. A field may legally arrive split across repeated lines.
     */
    private static String joinedHeader(List names, List values, String name) {
        StringBuilder joined = null;
        for(int iter = 0 ; iter < names.size() ; iter++) {
            if(((String)names.get(iter)).equalsIgnoreCase(name)) {
                if(joined == null) {
                    joined = new StringBuilder();
                } else {
                    joined.append(',');
                }
                joined.append((String)values.get(iter));
            }
        }
        return joined == null ? null : joined.toString();
    }

    /** Reassembles a chunked body, dropping the framing and any trailer section. */
    private static byte[] dechunk(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = 0;
        while(true) {
            int eol = indexOfCrLf(data, pos);
            if(eol < 0) {
                throw new IOException("Truncated chunked response: no chunk size line");
            }
            int end = pos;
            while(end < eol && data[end] != ';') {
                end++;
            }
            int size = parseChunkSize(data, pos, end);
            pos = eol + 2;
            if(size == 0) {
                // Trailers may follow. They are header fields, not body bytes, and
                // this client has no caller that reads them.
                return out.toByteArray();
            }
            if(size > data.length - pos) {
                throw new IOException("Truncated chunked response: chunk runs past the body");
            }
            out.write(data, pos, size);
            pos += size;
            if(pos + 2 > data.length || data[pos] != '\r' || data[pos + 1] != '\n') {
                throw new IOException("Malformed chunked response: chunk not terminated");
            }
            pos += 2;
        }
    }

    private static int indexOfCrLf(byte[] data, int from) {
        for(int iter = from ; iter + 1 < data.length ; iter++) {
            if(data[iter] == '\r' && data[iter + 1] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    /**
     * A chunk size is hexadecimal and unsigned. Parsed by hand because the sizes
     * this guards against are exactly the ones that overflow a signed parse.
     */
    private static int parseChunkSize(byte[] data, int from, int to) throws IOException {
        int size = 0;
        int digits = 0;
        for(int iter = from ; iter < to ; iter++) {
            int c = data[iter] & 0xff;
            int digit;
            if(c >= '0' && c <= '9') {
                digit = c - '0';
            } else if(c >= 'a' && c <= 'f') {
                digit = c - 'a' + 10;
            } else if(c >= 'A' && c <= 'F') {
                digit = c - 'A' + 10;
            } else if((c == ' ' || c == '\t') && digits > 0) {
                break;
            } else {
                throw new IOException("Malformed chunk size");
            }
            if(size > (Integer.MAX_VALUE - digit) / 16) {
                throw new IOException("Chunk size out of range");
            }
            size = size * 16 + digit;
            digits++;
        }
        if(digits == 0) {
            throw new IOException("Malformed chunk size: empty");
        }
        return size;
    }

    private static int indexOfHeaderEnd(byte[] data) {
        for(int iter = 0 ; iter + 3 < data.length ; iter++) {
            if(data[iter] == '\r' && data[iter + 1] == '\n' && data[iter + 2] == '\r' && data[iter + 3] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    private static int parseStatus(String statusLine) throws IOException {
        int first = statusLine.indexOf(' ');
        if(first < 0) {
            throw new IOException("Malformed status line: " + statusLine);
        }
        int second = statusLine.indexOf(' ', first + 1);
        String code = second < 0 ? statusLine.substring(first + 1) : statusLine.substring(first + 1, second);
        try {
            return Integer.parseInt(code.trim());
        } catch (NumberFormatException err) {
            throw new IOException("Malformed status code: " + statusLine);
        }
    }

    private static String[] split(String value, String separator) {
        List parts = new ArrayList();
        int pos = 0;
        while(true) {
            int next = value.indexOf(separator, pos);
            if(next < 0) {
                parts.add(value.substring(pos));
                break;
            }
            parts.add(value.substring(pos, next));
            pos = next + separator.length();
        }
        String[] result = new String[parts.size()];
        for(int iter = 0 ; iter < result.length ; iter++) {
            result[iter] = (String)parts.get(iter);
        }
        return result;
    }
}
