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
        // NULL IS GET HERE TOO. requireMethod tolerates null because the sibling
        // Web API takes it and means GET -- libcurl is simply left to its default
        // on one implementation and normalises it on the other -- but this client
        // writes the request line itself, and appending a null reference to a
        // StringBuilder writes the four characters "null". The peer was being
        // asked for an undefined verb, which a conforming server answers 501 and a
        // lenient one may treat as something else entirely: one API, three answers
        // to the same argument.
        String verb = method == null ? "GET" : method;
        Tcp socket = Tcp.connect(host, port, 0);
        try {
            StringBuilder head = new StringBuilder();
            head.append(verb).append(' ').append(path).append(" HTTP/1.1\r\n");
            head.append("Host: ").append(authority(host, port)).append("\r\n");
            head.append("Connection: close\r\n");
            head.append("Content-Length: ").append(body == null ? 0 : body.length).append("\r\n");
            head.append("\r\n");
            byte[] headBytes = head.toString().getBytes("UTF-8");
            socket.write(headBytes, 0, headBytes.length);
            if(body != null && body.length > 0) {
                socket.write(body, 0, body.length);
            }
            return readResponse(socket, verb);
        } finally {
            socket.close();
        }
    }

    /**
     * How much of one response this client will hold, head and body together.
     *
     * <p>Mirrors CN1_WEB_MAX_RESPONSE_MB on the other client, including its
     * default of 64, so the two agree about what is too much to read.
     */
    private static final long MAX_RESPONSE_BYTES =
            (long)envMegabytes("CN1_HTTP_MAX_RESPONSE_MB", 64) * 1024L * 1024L;

    /** 1..2047 MB, as the other client's reader bounds the same setting. */
    private static int envMegabytes(String name, int fallback) {
        String value = System.getenv(name);
        if(value == null || value.length() == 0) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 1 && parsed <= 2047 ? parsed : fallback;
        } catch (NumberFormatException err) {
            return fallback;
        }
    }

    private static Response readResponse(Tcp socket, String verb) throws IOException {
        // "Connection: close" is requested above, so a conforming peer ends the
        // message by closing, and the whole of it can be parsed in memory at once.
        //
        // ENDING IS NOT ONLY CLOSING, though. A peer that keeps the connection open
        // regardless -- one that answers keep-alive whatever was asked, or a
        // control endpoint that simply never hangs up -- left this blocked in
        // read() with a COMPLETE response already in the buffer and nothing looking
        // at it. There is no read deadline on this socket, so "blocked" means for
        // as long as the process lives: in LambdaRuntime that strands the
        // invocation loop for the lifetime of the instance. The message says where
        // it ends, RFC 9112 6.3, so this now stops when its framing says so and
        // waits for end-of-stream only when nothing else frames it.
        //
        // A READ DEADLINE WOULD HAVE BEEN THE WRONG TOOL, though it is the other
        // obvious one: the Lambda protocol's GET /invocation/next is a long poll
        // that is SUPPOSED to block until work arrives, so a timeout short enough
        // to catch a stalled peer would break the loop it was meant to protect.
        // What remains unbounded is a peer that accepts, sends nothing or half a
        // message, and stalls -- that one needs a socket deadline, which needs a
        // native this runtime does not have yet.
        byte[] buffer = new byte[8192];
        int size = 0;
        // The framing, once the final header block has arrived: a whole-message
        // length, or a chunk walk, or neither -- in which case end-of-stream is
        // the framing and this reads exactly as it used to.
        boolean framingKnown = false;
        // A LONG for the same reason chunkedEnd uses one: a peer may declare a
        // Content-Length up to Integer.MAX_VALUE, and bodyStart + declared then
        // wrapped negative -- which did not spin, but did leave total < 0, so the
        // framing this loop had just worked out was silently ignored and the read
        // went back to waiting for end of stream.
        long total = -1;
        boolean chunked = false;
        int[] chunkCursor = new int[1];
        int frameStart = 0;
        int headerScan = 0;
        while(true) {
            if(!framingKnown) {
                int headerEnd = indexOfHeaderEnd(buffer, headerScan, size);
                if(headerEnd < 0) {
                    // Resume three bytes back, because the terminator can straddle
                    // two reads. Never before the block being examined.
                    headerScan = Math.max(frameStart, size - 3);
                } else {
                    String headerText = new String(buffer, frameStart,
                            headerEnd - frameStart, "UTF-8");
                    String[] lines = split(headerText, "\r\n");
                    int status = lines.length == 0 ? -1 : parseStatus(lines[0]);
                    if(status >= 100 && status < 200) {
                        // An interim response frames nothing; the real one follows
                        // it on the same connection. frameStart only moves forward.
                        frameStart = headerEnd + 4;
                        headerScan = frameStart;
                        continue;
                    }
                    List headNames = new ArrayList();
                    List headValues = new ArrayList();
                    for(int iter = 1 ; iter < lines.length ; iter++) {
                        int colon = lines[iter].indexOf(':');
                        if(colon > 0) {
                            headNames.add(lines[iter].substring(0, colon).trim());
                            headValues.add(lines[iter].substring(colon + 1).trim());
                        }
                    }
                    int bodyStart = headerEnd + 4;
                    framingKnown = true;
                    if(!carriesBody(verb, status)) {
                        total = bodyStart;
                    } else if(joinedHeader(headNames, headValues,
                            "Transfer-Encoding") != null) {
                        chunked = true;
                        chunkCursor[0] = bodyStart;
                    } else {
                        int declared = declaredLength(headNames, headValues);
                        total = declared >= 0 ? (long)bodyStart + (long)declared : -1;
                    }
                }
            }
            if(total >= 0 && size >= total) {
                break;
            }
            if(chunked) {
                int end = chunkedEnd(buffer, size, chunkCursor);
                if(end >= 0) {
                    break;
                }
            }
            if(size == buffer.length) {
                // DOUBLING, BUT NOT PAST THE CEILING. The byte count below refuses
                // a response over MAX_RESPONSE_BYTES -- after this ran, so the
                // buffer had already doubled to twice the configured limit to hold
                // bytes that were about to be refused: 128 MB of allocation to
                // reject a 64 MB response, and an OutOfMemoryError rather than the
                // IOException that was promised. At a large configured limit the
                // doubling overflows instead, and a negative array size is not an
                // answer at all.
                //
                // One byte past the ceiling is exactly enough: the read below can
                // then still deliver the byte that proves the response is too
                // long, which is what the count wants to see. A buffer already
                // there has nowhere left to go, and the response is over the limit
                // by definition, so it is refused here in the same words.
                long ceiling = MAX_RESPONSE_BYTES + 1;
                if(ceiling > Integer.MAX_VALUE - 8) {
                    ceiling = Integer.MAX_VALUE - 8;
                }
                long want = (long)buffer.length * 2L;
                if(want > ceiling) {
                    want = ceiling;
                }
                if(want <= buffer.length) {
                    throw new IOException("The response passed the "
                            + (MAX_RESPONSE_BYTES / (1024 * 1024)) + " MB this client "
                            + "will read; set CN1_HTTP_MAX_RESPONSE_MB higher if the "
                            + "endpoint really answers that much");
                }
                byte[] grown = new byte[(int)want];
                System.arraycopy(buffer, 0, grown, 0, size);
                buffer = grown;
            }
            int n = socket.read(buffer, size, buffer.length - size);
            if(n <= 0) {
                break;
            }
            // BOUNDED WHILE IT IS READ, not judged once it is over. This reads to
            // EOF, so a peer that never stops sending is a peer this process grows
            // a buffer for until the heap is gone -- and nothing about the framing
            // is even looked at until the connection closes. The endpoint does not
            // have to be hostile to do it; a misconfigured one that streams is
            // enough, and the failure lands on the whole process rather than on
            // the request that caused it.
            //
            // The sibling client already had this: libcurl's write callback stops
            // the transfer past CN1_WEB_MAX_RESPONSE_MB, default 64. This is the
            // same bound with the name its own family uses, so the two clients
            // answer alike.
            if(size + n > MAX_RESPONSE_BYTES) {
                throw new IOException("The response passed the "
                        + (MAX_RESPONSE_BYTES / (1024 * 1024)) + " MB this client "
                        + "will read; set CN1_HTTP_MAX_RESPONSE_MB higher if the "
                        + "endpoint really answers that much");
            }
            size += n;
        }
        byte[] all = new byte[size];
        System.arraycopy(buffer, 0, all, 0, size);
        if(all.length == 0) {
            // The peer closed without sending anything. Reporting this as malformed
            // HTTP sent every "the host went away" shutdown to the wrong diagnosis.
            throw new IOException("Connection closed before any response was sent");
        }
        // PAST THE INTERIM ONES. A 1xx is a complete response with its own status
        // line and header block and no body, and the final response follows it on
        // the same connection -- RFC 9110 15.2. Taking the first header terminator
        // handed back "103 Early Hints", or the "100 Continue" this very server
        // sends when a request carries Expect, as though it were the answer, with
        // the real status line and headers delivered to the caller as the body of
        // it. A client that checks the status then reads 103 and treats a
        // successful request as a failed one, or the reverse.
        int blockStart = 0;
        int headerEnd;
        String[] lines;
        int status;
        while(true) {
            headerEnd = indexOfHeaderEnd(all, blockStart);
            if(headerEnd < 0) {
                throw new IOException("Malformed HTTP response: no header terminator");
            }
            String headerText = new String(all, blockStart, headerEnd - blockStart, "UTF-8");
            lines = split(headerText, "\r\n");
            if(lines.length == 0) {
                throw new IOException("Malformed HTTP response: empty");
            }
            status = parseStatus(lines[0]);
            if(status < 100 || status >= 200) {
                break;
            }
            // blockStart only ever moves forward, so a peer sending nothing but
            // interim responses runs out of bytes and is reported as malformed
            // rather than looping here.
            blockStart = headerEnd + 4;
        }
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
        // ... EXCEPT where the declared length does not describe a body that was
        // sent. RFC 9110 6.4.1: a response to HEAD, and any 1xx, 204 or 304, ends
        // at the blank line whatever the header fields say -- and a conforming
        // server answers HEAD with the Content-Length the GET would have carried.
        // Comparing that against the zero bytes it correctly sent reported every
        // such response as a truncated one, so this client could not make a HEAD
        // request against a conforming server at all.
        // TRANSFER-ENCODING WINS. RFC 9112 6.1: when a message carries both, the
        // transfer coding overrides the length, and a RECIPIENT of a response
        // ignores the Content-Length -- it describes the decoded body at best,
        // and at worst it is the smuggling attempt that rule exists for. Applying
        // it to the raw chunk framing measured the wrong thing entirely: a
        // declared 5 cut "5\r\nhello\r\n0\r\n\r\n" to five wire bytes, and the
        // decoder then called a complete response truncated.
        //
        // The server's REQUEST reader refuses both together instead, which is the
        // other half of the same rule: a request that frames itself twice is
        // refused, a response that does is read by its coding.
        boolean coded = joinedHeader(names, values, "Transfer-Encoding") != null;
        int declared = carriesBody(verb, status) && !coded
                ? declaredLength(names, values) : -1;
        if(declared >= 0 && bodyBytes.length < declared) {
            throw new IOException("The response body stopped after " + bodyBytes.length
                    + " of the " + declared + " byte(s) its Content-Length declared, so "
                    + "the connection failed part way through it");
        }
        // AND THE MESSAGE ENDS THERE. Content-Length is the whole of the framing:
        // whatever follows the declared bytes is not part of this response, and
        // handing it back as body meant a caller acted on the peer's answer with
        // something else appended to it -- a second response, or bytes an
        // attacker put after a short one. A short body is a transport failure and
        // is reported above; a long one is the peer contradicting its own framing,
        // and the framed answer is still perfectly readable, so this keeps exactly
        // what was declared rather than refusing what was asked for.
        if(declared >= 0 && bodyBytes.length > declared) {
            byte[] framed = new byte[declared];
            System.arraycopy(bodyBytes, 0, framed, 0, declared);
            bodyBytes = framed;
        }
        // AND NOTHING IS DECODED WHEN NOTHING WAS SENT. The same RFC 9110 6.4.1
        // rule that stops the length check applies to the transfer coding: a HEAD
        // response may carry "Transfer-Encoding: chunked" to describe how the GET
        // would have been framed, and a 304 may carry the same metadata, while
        // both correctly end at the blank line. Handing those zero bytes to the
        // chunked decoder made it report a truncated message -- the strictness
        // added for a real truncation, applied to a response that is complete.
        byte[] framedBody = carriesBody(verb, status) ? bodyBytes : new byte[0];
        return new Response(status, names, values,
                carriesBody(verb, status) ? decodeBody(framedBody, names, values)
                                          : framedBody);
    }

    /**
     * Whether a response with this status, to this verb, is one whose
     * Content-Length describes bytes that follow the header block.
     *
     * <p>RFC 9110 6.4.1, and the list is exhaustive: a response to HEAD, and any
     * 1xx, 204 or 304, is terminated by the first empty line after the header
     * fields regardless of the fields present.
     */
    private static boolean carriesBody(String verb, int status) {
        if("HEAD".equals(verb)) {
            return false;
        }
        return !(status >= 100 && status < 200) && status != 204 && status != 304;
    }

    /**
     * The Content-Length the peer declared, or -1 when it declared none or the
     * value is not a number. A chunked response has no Content-Length, so the
     * check above simply does not apply to one.
     */
    private static int declaredLength(List names, List values) throws IOException {
        // EVERY occurrence, and they have to agree. Taking the first and treating
        // an unparseable one as absent made the framing depend on field ORDER:
        // "Content-Length: 5" before "Content-Length: 10" with ten bytes was
        // accepted and cut to five, while the same two fields the other way round
        // came back a transport error. RFC 9112 6.3 is that a message with an
        // invalid or conflicting length is invalid -- and rejecting a repeated
        // list is explicitly one of the two allowed answers.
        //
        // -1 still means ABSENT, which is a different thing from unreadable: a
        // response with no Content-Length is ordinary (chunked, or ended by the
        // close), and only the ones that make a claim are held to it.
        int declared = -1;
        for(int iter = 0 ; iter < names.size() ; iter++) {
            if(!"content-length".equalsIgnoreCase(String.valueOf(names.get(iter)))) {
                continue;
            }
            String raw = String.valueOf(values.get(iter)).trim();
            int parsed;
            try {
                parsed = Integer.parseInt(raw);
            } catch (NumberFormatException err) {
                throw new IOException("The response declares a Content-Length that "
                        + "is not a number, so where its body ends cannot be known: "
                        + raw);
            }
            if(parsed < 0) {
                throw new IOException("The response declares a negative "
                        + "Content-Length: " + raw);
            }
            if(declared >= 0 && declared != parsed) {
                throw new IOException("The response declares two different "
                        + "Content-Lengths, " + declared + " and " + parsed
                        + ", so where its body ends depends on which is believed");
            }
            declared = parsed;
        }
        return declared;
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
                // AND THE MESSAGE ENDS WHERE THE FRAMING SAYS. RFC 9112 7.1:
                // last-chunk, then the trailer section, then a final CRLF. Returning
                // here as soon as the zero-size line arrived accepted a response
                // that stopped mid-message -- a peer that died right after "0" was
                // reported as a complete body -- and accepted whatever bytes
                // happened to follow as a trailer nobody looked at. A truncated
                // response is the one case a client must not round up to success,
                // since the caller then acts on a body it believes is whole.
                //
                // The trailers themselves are still dropped: they are header
                // fields, and this client has no caller that reads them. What
                // changes is that they have to BE header fields, and the section
                // has to be terminated.
                while(true) {
                    int lineEnd = indexOfCrLf(data, pos);
                    if(lineEnd < 0) {
                        throw new IOException("Truncated chunked response: the last "
                                + "chunk is not followed by a terminated trailer "
                                + "section, so the message stopped part way through");
                    }
                    if(lineEnd == pos) {
                        return out.toByteArray();       // the empty line ends it
                    }
                    int colon = pos;
                    while(colon < lineEnd && data[colon] != ':') {
                        colon++;
                    }
                    if(colon == pos || colon == lineEnd) {
                        throw new IOException("Malformed chunked response: the trailer "
                                + "section holds a line that is not a header field");
                    }
                    pos = lineEnd + 2;
                }
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
        return indexOfCrLf(data, from, data.length);
    }

    /** The same search, bounded by how much of a partly filled buffer is real. */
    private static int indexOfCrLf(byte[] data, int from, int limit) {
        for(int iter = Math.max(from, 0) ; iter + 1 < limit ; iter++) {
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
        return indexOfHeaderEnd(data, 0);
    }

    private static int indexOfHeaderEnd(byte[] data, int from) {
        return indexOfHeaderEnd(data, from, data.length);
    }

    /** The same search, bounded by how much of a partly filled buffer is real. */
    private static int indexOfHeaderEnd(byte[] data, int from, int limit) {
        for(int iter = Math.max(from, 0) ; iter + 3 < limit ; iter++) {
            if(data[iter] == '\r' && data[iter + 1] == '\n' && data[iter + 2] == '\r' && data[iter + 3] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    /**
     * Walks chunk framing from the cursor, returning the index one past the end of
     * the message once the last chunk and its trailer section have arrived, or -1
     * while the message is still coming.
     *
     * <p>The cursor only ever moves forward, to the last chunk boundary this has
     * fully parsed, so a body delivered in a thousand reads is walked once rather
     * than a thousand times. What it does NOT do is decode: dechunk() does that
     * afterwards, from the start, and this only has to agree with it about where
     * the message stops.
     */
    private static int chunkedEnd(byte[] data, int size, int[] cursor)
            throws IOException {
        int pos = cursor[0];
        while(true) {
            int eol = indexOfCrLf(data, pos, size);
            if(eol < 0) {
                return -1;
            }
            int end = pos;
            while(end < eol && data[end] != ';') {
                end++;
            }
            int chunk = parseChunkSize(data, pos, end);
            int after = eol + 2;
            if(chunk == 0) {
                // last-chunk, then a trailer section ending at an empty line.
                int scan = after;
                while(true) {
                    int lineEnd = indexOfCrLf(data, scan, size);
                    if(lineEnd < 0) {
                        return -1;
                    }
                    if(lineEnd == scan) {
                        return scan + 2;
                    }
                    scan = lineEnd + 2;
                }
            }
            // IN A WIDER TYPE, because the size is the PEER's number and it goes up
            // to Integer.MAX_VALUE -- parseChunkSize refuses only what overflows
            // its own accumulator. "7fffffff" made after + chunk + 2 wrap negative,
            // which passed the bound below as though the chunk had already arrived
            // and put a NEGATIVE cursor back for the next pass. Measured from
            // there: the scan resumes at zero, because indexOfCrLf clamps its
            // start, but `end` below begins at the unclamped cursor, so the walk
            // indexes the buffer at -2147483590 and the caller is handed a failure
            // whose whole message is that number. Without the clamp it is the
            // endless re-parse of one size line instead. Neither is a reading of
            // the peer's framing, and the arithmetic is what is wrong with both: a
            // long cannot wrap here, and a chunk that big simply never fits, so
            // this answers "not yet" until the read loop's response ceiling
            // refuses it.
            long next = (long)after + (long)chunk + 2L;   // the chunk and its CRLF
            if(next > size) {
                return -1;
            }
            pos = (int)next;
            cursor[0] = pos;
        }
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
