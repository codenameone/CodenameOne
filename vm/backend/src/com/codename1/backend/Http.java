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

    public static Response get(String host, int port, String path) throws IOException {
        return request(host, port, "GET", path, null);
    }

    public static Response post(String host, int port, String path, byte[] body) throws IOException {
        return request(host, port, "POST", path, body);
    }

    public static Response request(String host, int port, String method, String path, byte[] body) throws IOException {
        Tcp socket = Tcp.connect(host, port, 0);
        try {
            StringBuilder head = new StringBuilder();
            head.append(method).append(' ').append(path).append(" HTTP/1.1\r\n");
            head.append("Host: ").append(host).append(':').append(port).append("\r\n");
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
        return new Response(status, names, values, bodyBytes);
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
