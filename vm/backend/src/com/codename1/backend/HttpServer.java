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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An HTTP/1.1 server built around a reactor and a bounded worker pool.
 *
 * The split is the whole design. A parked ParparVM thread costs about 243KB on
 * musl (see vm/benchmarks ThreadCost), so ten thousand connections cannot each
 * have one. Here an IDLE connection is a descriptor the reactor watches, and only
 * a connection with a request in flight occupies a worker. Workers are bounded, so
 * overload sheds as queueing rather than as memory exhaustion.
 *
 * Once a worker owns a connection its descriptor is switched to BLOCKING mode, so
 * request parsing is a straight read loop rather than a resumable state machine.
 * That spends a worker per in-flight request to avoid a large amount of
 * complexity, and in-flight requests are the thing there are few of.
 */
public final class HttpServer {
    /**
     * What a handler receives. Header names are matched case-insensitively.
     *
     * The headers are NOT copied out of the request. They stay as offsets into
     * the buffer the kernel filled, and {@link #getHeader} compares against those
     * bytes -- so a handler that reads two headers allocates nothing, where
     * building a map of Strings cost about 2.6KB per request and made char[] the
     * single largest allocation in the server. {@link #getHeaders} still returns
     * a Map, built on first call, for callers that want one.
     *
     * A Request is valid for the duration of {@link Handler#handle} and NOT
     * beyond it. Both the byte array it was parsed from and the slice table that
     * indexes it are reused -- the array by the reading thread, the table by the
     * connection -- so a Request held past the handler describes whatever arrived
     * next, not what it was built from.
     *
     * This corrects a claim that used to stand here, that the slices "stay valid
     * for as long as the Request is held". That was never true: the table is
     * `conn.slices`, reused by the very next request on the same connection. The
     * zero-copy read added a second way for it to be false, which is what prompted
     * reading the sentence carefully enough to notice it had always been wrong.
     *
     * The synchronous handler signature already makes the call the natural
     * lifetime, so this documents the contract rather than narrowing one -- but
     * anything that needs to outlive the handler must copy what it needs, and
     * {@link #getHeaders} or {@link #getHeader} give Strings that are safe to keep.
     */
    public static final class Request {
        // Not final because one Request is REUSED for every request on a
        // connection, which is the same trade the buffer and the slice table
        // already make and the reason a Request is documented as valid only for
        // the duration of Handler.handle. "Immutable to its handler" is the
        // property that matters and it still holds exactly: reset() runs while a
        // request is being PARSED, which is strictly before the handler is called
        // and strictly after the previous one returned.
        private String method;
        private String target;
        private String version;
        private String body;
        /** The connection this request arrived on; null for HTTP/2, see respond. */
        private Conn conn;
        /** The bytes the header block was read from. */
        private byte[] raw;
        /** nameStart, nameLength, valueStart, valueLength per header, in order. */
        private int[] slices;
        private int headerCount;
        private Map headers;
        /**
         * Where the request target sits inside {@link #raw}.
         *
         * Kept so a router can match on the bytes the parser already has. Matching
         * on getTarget() means comparing Strings, and the generated router is the
         * one caller that runs for every request on every route, so it is worth not
         * asking it to.
         */
        private int targetStart;
        private int targetLength;
        /** Computed on first use; -1 until then. Reset with the rest of the Request. */
        private int pathLength = -1;
        /**
         * The target with percent-encoded UNRESERVED octets resolved, or null when
         * it carried none and the raw bytes are already canonical.
         *
         * <p>RFC 3986 calls %6D and "m" the same character, so /users/%6De and
         * /users/me are one URI spelled two ways. Route selection compares bytes,
         * so without this the literal route missed and a sibling pattern route
         * caught it instead -- /users/me and /users/{id} are different handlers,
         * and choosing between them by spelling is how a check on one of them gets
         * walked around. An encoded SLASH is deliberately left alone: %2F is not a
         * segment boundary, and resolving it would invent one.
         *
         * <p>Built only when such an escape is actually there, so an ordinary
         * target stays on the zero-allocation path the comment above describes.
         */
        private byte[] canonicalTarget;
        private int canonicalLength;
        private boolean canonicalChecked;

        Request(String method, String target, String version, byte[] raw, int[] slices,
                int headerCount, String body) {
            this(method, target, version, raw, slices, headerCount, body, 0, 0);
        }

        Request(String method, String target, String version, byte[] raw, int[] slices,
                int headerCount, String body, int targetStart, int targetLength) {
            this.method = method;
            this.target = target;
            this.version = version;
            this.raw = raw;
            this.slices = slices;
            this.headerCount = headerCount;
            this.body = body;
            this.targetStart = targetStart;
            this.targetLength = targetLength;
            this.pathLength = -1;
        }

        /**
         * True when the request PATH is exactly these bytes.
         *
         * The path, not the target: everything from `?` onwards is the query string
         * and is not part of the route. A router that compared the whole target
         * would match `/healthz` and miss `/healthz?probe=1`, which is the same
         * request.
         *
         * For the generated router, which holds each route as a byte[] constant. No
         * String is built and nothing is hashed: it is a length test and a compare
         * against the buffer the request was parsed from. Falls back to comparing
         * the target String when the slice is not available, which is the HTTP/2
         * path -- there the target came from HPACK rather than from a byte range.
         */
        public boolean pathIs(byte[] path) {
            if(path == null) {
                return false;
            }
            int length = pathByteLength();
            if(path.length != length) {
                return false;
            }
            return regionEquals(path, 0, length);
        }

        /** As {@link #pathIs}, for a route that continues into a path variable. */
        public boolean pathStartsWith(byte[] prefix) {
            if(prefix == null || prefix.length > pathByteLength()) {
                return false;
            }
            return regionEquals(prefix, 0, prefix.length);
        }

        /**
         * The path from `from` onwards, as text. Allocates, so a matched route only.
         *
         * Percent escapes are left alone. The router decodes the segments it binds,
         * because decoding first would let an encoded `/` invent a segment boundary
         * that the client never sent.
         */
        public String pathFrom(int from) {
            int length = pathByteLength();
            if(from >= length) {
                return "";
            }
            // The canonical bytes when there are any: the offsets a caller has are
            // positions in the path THIS returns, and pathIs and pathStartsWith
            // compare against the same bytes. Reading raw here instead would hand
            // back a slice measured in one spelling and indexed in the other.
            canonicalize();
            if(canonicalTarget != null) {
                return asciiString(canonicalTarget, from, length - from);
            }
            if(targetLength <= 0 || raw == null) {
                return target.substring(from, length);
            }
            return asciiString(raw, targetStart + from, length - from);
        }

        /** The path's length in bytes -- the target up to `?` -- without building it. */
        public int pathByteLength() {
            if(pathLength >= 0) {
                return pathLength;
            }
            int length = targetByteLength();
            int found = length;
            for(int iter = 0 ; iter < length ; iter++) {
                if(byteAt(iter) == '?') {
                    found = iter;
                    break;
                }
            }
            pathLength = found;
            return found;
        }

        /**
         * A query parameter's decoded value, or null when the request did not send
         * it. An empty `?flag=` is present with an empty value, which is not the
         * same as absent, and callers that offer a default depend on the difference.
         */
        public String queryParam(String name) {
            int length = targetByteLength();
            int pos = pathByteLength();
            if(pos >= length || name == null) {
                return null;
            }
            pos++; // the '?' itself
            while(pos <= length) {
                int end = pos;
                while(end < length && byteAt(end) != '&') {
                    end++;
                }
                int eq = pos;
                while(eq < end && byteAt(eq) != '=') {
                    eq++;
                }
                if(nameEquals(name, pos, eq)) {
                    return percentDecode(eq < end ? eq + 1 : end, end);
                }
                pos = end + 1;
            }
            return null;
        }

        /**
         * One byte of the request target, from whichever form this Request holds.
         *
         * <p>The String form is one char per OCTET -- the h2 natives build it with
         * newStringFromAsciiLen, as the HTTP/1 parser reads its bytes -- so
         * narrowing here recovers exactly what arrived. It is not a UTF-16 string
         * being truncated: decoding those octets into characters anywhere upstream
         * would make this return bytes the client never sent, which is what a
         * UTF-8 decode of :path did.
         */
        private int byteAt(int index) {
            canonicalize();
            if(canonicalTarget != null) {
                return canonicalTarget[index] & 0xff;
            }
            return rawByteAt(index);
        }

        private int rawByteAt(int index) {
            if(targetLength > 0 && raw != null) {
                return raw[targetStart + index] & 0xff;
            }
            return target.charAt(index) & 0xff;
        }

        private int rawTargetLength() {
            return targetLength > 0 ? targetLength
                                    : (target == null ? 0 : target.length());
        }

        /** The target's length in bytes, after normalizing if it needed it. */
        private int targetByteLength() {
            canonicalize();
            return canonicalTarget != null ? canonicalLength : rawTargetLength();
        }

        /**
         * Resolves percent-encoded unreserved octets, once, and only when there is
         * at least one. See canonicalTarget.
         */
        private void canonicalize() {
            if(canonicalChecked) {
                return;
            }
            canonicalChecked = true;
            int length = rawTargetLength();
            boolean needed = false;
            for(int iter = 0 ; iter + 2 < length ; iter++) {
                if(rawByteAt(iter) != '%') {
                    continue;
                }
                int hi = hexDigit(rawByteAt(iter + 1));
                int lo = hexDigit(rawByteAt(iter + 2));
                if(hi >= 0 && lo >= 0 && isUnreservedByte((hi << 4) | lo)) {
                    needed = true;
                    break;
                }
            }
            if(!needed) {
                return;             // already canonical; nothing allocated
            }
            byte[] out = new byte[length];
            int count = 0;
            int pos = 0;
            while(pos < length) {
                int c = rawByteAt(pos);
                if(c == '%' && pos + 2 < length) {
                    int hi = hexDigit(rawByteAt(pos + 1));
                    int lo = hexDigit(rawByteAt(pos + 2));
                    if(hi >= 0 && lo >= 0) {
                        int decoded = (hi << 4) | lo;
                        if(isUnreservedByte(decoded)) {
                            out[count++] = (byte)decoded;
                            pos += 3;
                            continue;
                        }
                    }
                }
                out[count++] = (byte)c;
                pos++;
            }
            canonicalTarget = out;
            canonicalLength = count;
        }

        /** ALPHA / DIGIT / "-" / "." / "_" / "~", the RFC 3986 unreserved set. */
        private static boolean isUnreservedByte(int c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '.' || c == '_' || c == '~';
        }

        private boolean regionEquals(byte[] expected, int from, int length) {
            for(int iter = 0 ; iter < length ; iter++) {
                if(byteAt(from + iter) != (expected[iter] & 0xff)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Compares a parameter name against the raw bytes, decoding escapes in the
         * request as it goes. Names are rarely encoded, but comparing an encoded
         * name against a plain one would silently miss the parameter.
         */
        private boolean nameEquals(String name, int from, int to) {
            // A DECODED OCTET is compared below, so the thing it is compared
            // against has to be an octet too. For a non-ASCII name it is not:
            // "cafe" with an acute e arrives as caf%C3%A9, whose octets are 0xC3
            // 0xA9, while the Java char is 0xE9 -- no octet ever equals it, and
            // the parameter reads as absent even though the client sent it
            // exactly as every client encodes it. Such a name is compared
            // against its UTF-8 bytes instead. ASCII names, which is nearly all
            // of them, keep the character path: it is identical for them and
            // allocates nothing on a per-request code path.
            byte[] utf8 = null;
            for(int iter = 0 ; iter < name.length() ; iter++) {
                if(name.charAt(iter) > 0x7f) {
                    try {
                        utf8 = name.getBytes("UTF-8");
                    } catch (IOException err) {
                        return false;   // it cannot be encoded, so it cannot match
                    }
                    break;
                }
            }
            int wanted = utf8 == null ? name.length() : utf8.length;
            int index = 0;
            int pos = from;
            while(pos < to) {
                int c = byteAt(pos);
                int width = 1;
                if(c == '%' && pos + 2 < to) {
                    int hi = hexDigit(byteAt(pos + 1));
                    int lo = hexDigit(byteAt(pos + 2));
                    if(hi >= 0 && lo >= 0) {
                        c = (hi << 4) | lo;
                        width = 3;
                    }
                } else if(c == '+') {
                    c = ' ';
                }
                int want = index >= wanted ? -1
                        : (utf8 == null ? (name.charAt(index) & 0xff) : (utf8[index] & 0xff));
                if(want != c) {
                    return false;
                }
                index++;
                pos += width;
            }
            return index == wanted;
        }

        /**
         * Decodes one query value. The octets are gathered and decoded as a run,
         * because a percent escape carries one byte of UTF-8 and a character built
         * from a single byte at a time is mojibake for everything above ASCII.
         */
        private String percentDecode(int from, int to) {
            byte[] out = new byte[to - from];
            int length = 0;
            int pos = from;
            while(pos < to) {
                int c = byteAt(pos);
                if(c == '%' && pos + 2 < to) {
                    int hi = hexDigit(byteAt(pos + 1));
                    int lo = hexDigit(byteAt(pos + 2));
                    if(hi >= 0 && lo >= 0) {
                        out[length++] = (byte)((hi << 4) | lo);
                        pos += 3;
                        continue;
                    }
                } else if(c == '+') {
                    c = ' ';
                }
                out[length++] = (byte)c;
                pos++;
            }
            try {
                return new String(out, 0, length, "UTF-8");
            } catch (java.io.UnsupportedEncodingException err) {
                // UTF-8 is required of every VM this runs on; the checked exception
                // is the API's, not a case that can happen.
                return new String(out, 0, length);
            }
        }

        private static int hexDigit(int c) {
            if(c >= '0' && c <= '9') {
                return c - '0';
            }
            if(c >= 'a' && c <= 'f') {
                return c - 'a' + 10;
            }
            if(c >= 'A' && c <= 'F') {
                return c - 'A' + 10;
            }
            return -1;
        }

        /**
         * A Response for this request WITHOUT allocating one.
         *
         * Returns the connection's single Response, re-pointed to these values. It
         * is valid for the duration of {@link Handler#handle} and not beyond it --
         * the same contract this Request already carries, and for the same reason:
         * the next request on this connection reuses it.
         *
         * Why it exists: on a route that allocates nothing else, the Response was
         * the last per-request allocation, and allocation is what drives both the
         * collector's frequency and its footprint. Removing it measured a 15x
         * better p99 and an 8x smaller resident set at the same throughput.
         *
         * {@code new Response(...)} still works and still allocates; a handler that
         * needs its Response to outlive the call must use it.
         */
        public Response respond(int status, String contentType, byte[] body) {
            if(conn == null) {
                return new Response(status, contentType, body);   // HTTP/2 path
            }
            if(conn.pooledResponse == null) {
                conn.pooledResponse = new Response(status, contentType, body);
            } else {
                conn.pooledResponse.reset(status, contentType,
                        body == null ? EMPTY_BODY : body, -1, 0, 0, null);
            }
            return conn.pooledResponse;
        }

        /**
         * A JSON response on the connection's pooled Response, serialised straight
         * from the value.
         *
         * The deferred-JSON path already avoided every copy on the body side --
         * Json.write goes into the connection's reusable ByteSink, so nothing
         * materialises a byte[] or a String -- but Response.jsonValue is a static
         * that allocates a fresh Response per call, and that was the ONLY thing the
         * route allocated. Profiled over 10.8M requests: 88.1 bytes each, all of it
         * one HttpServer.Response, count 10789601 against 10789541 requests. The
         * plaintext route had already been pooled and sat at 0.1 bytes per request.
         *
         * That is worth removing because of what allocation costs HERE rather than
         * what it costs to allocate: the collector shares the server's cores, so a
         * route that allocates pays for cycles in its tail. fasthttp on the same
         * body allocates about 16 bytes per request and collects three times a
         * second; this route was collecting thirteen to eighteen times a second.
         *
         * reset() clears deferredJson and hasDeferredJson, so a pooled Response
         * reused for a plain body cannot carry a stale value into the next
         * response -- which is the failure this would otherwise invite.
         */
        public Response respondJson(int status, Object value) {
            if(conn == null) {
                return Response.jsonValue(status, value);   // HTTP/2 path, as respond() does
            }
            if(conn.pooledResponse == null) {
                conn.pooledResponse = new Response(status, JSON_CONTENT_TYPE,
                        EMPTY_BODY, -1, 0, 0, null);
            } else {
                conn.pooledResponse.reset(status, JSON_CONTENT_TYPE,
                        EMPTY_BODY, -1, 0, 0, null);
            }
            conn.pooledResponse.deferredJson = value;
            conn.pooledResponse.hasDeferredJson = true;
            return conn.pooledResponse;
        }

        /**
         * DIAGNOSTIC: the connection's Response exactly as the last request left
         * it, or null the first time. Separates the allocation pooling saves from
         * the field writes it adds -- see the bench demo's RESPONSE_MODE.
         */
        public Response presetResponse() {
            return conn == null ? null : conn.pooledResponse;
        }

        /**
         * Re-points this Request at a freshly parsed request. Every field is
         * assigned, with no "unchanged" case: a field left behind describes the
         * PREVIOUS request on this connection, and headers is the one that would
         * hurt -- it caches a Map built on demand by getHeaders, so carrying it
         * over would answer one request's header lookups with another's. That is
         * a wrong answer rather than a crash, which is why it is assigned here
         * unconditionally instead of being cleared at some later point.
         */
        void reset(Conn conn, String method, String target, String version, byte[] raw,
                   int[] slices, int headerCount, String body) {
            reset(conn, method, target, version, raw, slices, headerCount, body, 0, 0);
        }

        /**
         * Drops what this request pointed at, once it has been answered.
         *
         * This object is pooled per connection and re-pointed by reset() for the
         * next request, so between the two it goes on referencing the LAST one:
         * the buffer the headers were sliced from, and the decoded body, which for
         * an upload is the whole of it. Every field cleared here is assigned again
         * by reset() before anything reads it.
         */
        void releaseRetained() {
            this.raw = null;
            this.slices = null;
            this.body = null;
            this.headers = null;
        }

        void reset(Conn conn, String method, String target, String version, byte[] raw,
                   int[] slices, int headerCount, String body,
                   int targetStart, int targetLength) {
            this.conn = conn;
            this.method = method;
            this.target = target;
            this.version = version;
            this.raw = raw;
            this.slices = slices;
            this.headerCount = headerCount;
            this.body = body;
            this.headers = null;
            this.targetStart = targetStart;
            this.targetLength = targetLength;
            // Recomputed for this request. A stale value would give the next request
            // on this connection the previous one's path length.
            this.pathLength = -1;
            this.canonicalTarget = null;
            this.canonicalLength = 0;
            this.canonicalChecked = false;
        }

        /**
         * For HTTP/2, whose headers arrive already decoded from the HPACK state --
         * there is no request buffer to slice into, so the map IS the
         * representation and every lookup below falls back to it.
         */
        Request(String method, String target, String version, Map headers, String body) {
            // pathLength is NOT set here, and does not need to be: it carries a
            // field initializer (= -1) and javac copies those into every
            // constructor -- this one's bytecode opens with iconst_m1/putfield.
            // The sibling constructor assigns it again explicitly, which makes
            // this one look like it forgot; a review has already read it that way
            // once and filed it as "every HTTP/2 route 404s", which it does not.
            this.method = method;
            this.target = target;
            this.version = version;
            this.raw = null;
            this.slices = null;
            this.headerCount = 0;
            this.headers = headers;
            this.body = body;
        }

        /** "HTTP/1.1" or "HTTP/1.0". The two differ on whether keep-alive is the default. */
        public String getVersion() {
            return version;
        }

        public String getMethod() {
            return method;
        }

        /** Path plus query string, exactly as it arrived. */
        public String getTarget() {
            return target;
        }

        /**
         * The headers as a Map, lower-cased names to values.
         *
         * Built on the first call and cached. Prefer {@link #getHeader}: this
         * allocates a String per name and per value, which is the cost the slice
         * representation exists to avoid.
         */
        public Map getHeaders() {
            if(headers == null) {
                Map out = new LinkedHashMap();
                for(int iter = 0 ; iter < headerCount ; iter++) {
                    int base = iter * 4;
                    String name = lowerCaseString(raw, slices[base], slices[base + 1]);
                    String value = asciiString(raw, slices[base + 2], slices[base + 3]);
                    Object existing = out.get(name);
                    if(existing == null) {
                        out.put(name, value);
                    } else {
                        // Combined in arrival order, as the HTTP/2 path does. Replacing
                        // meant getHeader() answered with the FIRST occurrence while
                        // this map held the last, so a cookie split across two fields
                        // was visible through one API and gone from the other -- and a
                        // generated dispatcher reads this map.
                        out.put(name, String.valueOf(existing)
                                + ("cookie".equals(name) ? "; " : ",") + value);
                    }
                }
                headers = out;
            }
            return headers;
        }

        /** One header by name, matched case-insensitively. Allocates only the value. */
        public String getHeader(String name) {
            if(name == null) {
                return null;
            }
            if(raw == null) {
                Object v = headers.get(asciiLower(name));
                return v == null ? null : String.valueOf(v);
            }
            int at = indexOfHeader(name);
            if(at < 0) {
                return null;
            }
            if(countHeader(name) == 1) {
                return asciiString(raw, slices[at + 2], slices[at + 3]);
            }
            // Repeated field. getHeaders() combines these and the HTTP/2 path does
            // too; returning only the first meant a handler reading getHeader saw
            // less than one reading getHeaders, and cookies split across two Cookie
            // fields -- which is legal on the wire -- simply vanished from the
            // second one. Authentication that reads a cookie could then differ by
            // which API it used, or by protocol.
            //
            // Cookie joins with "; " because that is its own delimiter (RFC 6265);
            // everything else with "," as RFC 9110 5.3 defines for a list field.
            String separator = "cookie".equalsIgnoreCase(name) ? "; " : ", ";
            StringBuilder joined = new StringBuilder();
            for(int iter = 0 ; iter < headerCount ; iter++) {
                int base = iter * 4;
                if(!sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], name)) {
                    continue;
                }
                if(joined.length() > 0) {
                    joined.append(separator);
                }
                joined.append(asciiString(raw, slices[base + 2], slices[base + 3]));
            }
            return joined.toString();
        }

        /**
         * Lowercases an ASCII header name.
         *
         * NOT String.toLowerCase(), which is locale sensitive and has no overload
         * here that takes a Locale: on a Turkish default the I of "COOKIE" folds to
         * a dotless i and the lookup misses a header that is present. A field name
         * is ASCII by specification, so it folds by hand. Six lines, copied rather
         * than shared, as the other folds in this tree are.
         */
        private static String asciiLower(String name) {
            int length = name.length();
            StringBuilder out = new StringBuilder(length);
            for(int iter = 0 ; iter < length ; iter++) {
                char c = name.charAt(iter);
                out.append(c >= 'A' && c <= 'Z' ? (char)(c + 32) : c);
            }
            return out.toString();
        }

        /** The slice index of a header, or -1. No allocation on either path. */
        int indexOfHeader(String name) {
            // Callers guard on raw != null; headerCount is 0 for the map form, so
            // this returns -1 there rather than reading a null slices array.
            //
            // The name is folded ONCE, not once per header. sliceEqualsIgnoreCase
            // reads it with charAt and case-folds it on every comparison, so a
            // four-header request folded the same needle four times over -- and the
            // generated code shows why that is not free: each character costs a
            // cn1InlStrCharAt (which re-checks the string's coder) plus a foldAscii
            // call, against a plain array read on the other side.
            byte[] needle = foldedBytes(name);
            if(needle == null) {
                // Not ASCII-foldable, so the general path is the only correct one.
                for(int iter = 0 ; iter < headerCount ; iter++) {
                    int base = iter * 4;
                    if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], name)) {
                        return base;
                    }
                }
                return -1;
            }
            for(int iter = 0 ; iter < headerCount ; iter++) {
                int base = iter * 4;
                if(sliceEqualsFolded(raw, slices[base], slices[base + 1], needle)) {
                    return base;
                }
            }
            return -1;
        }

        /**
         * Whether a header's value contains a token, case-insensitively. Used for
         * "connection: keep-alive" and friends without materialising the value.
         */
        /**
         * Whether a comma-separated field lists this token.
         *
         * A WHOLE token, not a substring. "Connection: disclose" contains "close"
         * and "not-keep-alive" contains "keep-alive", and a substring test read
         * both as the option itself -- so an extension token nobody here has heard
         * of decided whether the connection stays open, which is a framing
         * decision made on an unrelated name. Every occurrence of the field is
         * searched, because a repeated one is as legal as a repeated Cookie.
         */
        boolean headerContains(String name, String token) {
            if(raw == null) {
                Object v = headers == null ? null : headers.get(asciiLower(name));
                return v != null && listHasToken(String.valueOf(v), token);
            }
            for(int iter = 0 ; iter < headerCount ; iter++) {
                int base = iter * 4;
                if(!sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], name)) {
                    continue;
                }
                if(sliceHasToken(raw, slices[base + 2], slices[base + 3], token)) {
                    return true;
                }
            }
            return false;
        }

        /** The slice form: no String is built for the field or for its tokens. */
        private boolean sliceHasToken(byte[] data, int start, int length, String token) {
            int end = start + length;
            int at = start;
            while(at < end) {
                while(at < end && (data[at] == ' ' || data[at] == '\t' || data[at] == ',')) {
                    at++;
                }
                int tokenStart = at;
                while(at < end && data[at] != ',') {
                    at++;
                }
                int tokenEnd = at;
                while(tokenEnd > tokenStart
                        && (data[tokenEnd - 1] == ' ' || data[tokenEnd - 1] == '\t')) {
                    tokenEnd--;
                }
                if(tokenEnd - tokenStart == token.length()
                        && sliceEqualsIgnoreCase(data, tokenStart, tokenEnd - tokenStart, token)) {
                    return true;
                }
            }
            return false;
        }

        /** The String form, for a Request built from a map rather than a socket. */
        private boolean listHasToken(String value, String token) {
            int at = 0;
            while(at <= value.length()) {
                int comma = value.indexOf(',', at);
                int end = comma < 0 ? value.length() : comma;
                int start = at;
                while(start < end && (value.charAt(start) == ' ' || value.charAt(start) == '\t')) {
                    start++;
                }
                int trimmed = end;
                while(trimmed > start
                        && (value.charAt(trimmed - 1) == ' ' || value.charAt(trimmed - 1) == '\t')) {
                    trimmed--;
                }
                // regionMatches(true, ...) compares character by character and is
                // locale independent, unlike folding both sides with toLowerCase().
                if(trimmed - start == token.length()
                        && value.regionMatches(true, start, token, 0, token.length())) {
                    return true;
                }
                if(comma < 0) {
                    return false;
                }
                at = comma + 1;
            }
            return false;
        }

        /** How many headers arrived, so a duplicate can be detected. */
        int countHeader(String name) {
            int found = 0;
            for(int iter = 0 ; iter < headerCount ; iter++) {
                int base = iter * 4;
                if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], name)) {
                    found++;
                }
            }
            return found;
        }

        public String getBody() {
            return body;
        }
    }

    /** What a handler returns. */
    public static final class Response {
        // Not final because Request.respond hands back ONE Response per connection,
        // re-pointed per request. The same trade the Request beside it already
        // makes: valid for the duration of Handler.handle and not beyond it, which
        // is the whole window in which a handler can see it. A handler that would
        // rather own its Response still writes new Response(...) and pays for it.
        int status;
        String contentType;
        byte[] body;
        /** When >= 0 the body is this descriptor, and the server owns closing it. */
        int fileFd;
        long fileOffset;
        long fileLength;
        Map extraHeaders;
        /** Serialised into the connection buffer at write time; see jsonValue. */
        Object deferredJson;
        boolean hasDeferredJson;

        /**
         * Re-points this Response. Every field is assigned with no "unchanged"
         * case: a field left behind describes the PREVIOUS response on this
         * connection, and deferredJson is the one that would hurt -- it makes the
         * writer serialise an object the handler never returned.
         */
        /**
         * Drops what this response pointed at, once it has been written.
         *
         * Pooled per connection like the Request, so between responses it goes on
         * referencing the last one -- and deferredJson is a handler's object
         * graph, which for a large result is the largest thing either side holds.
         * reset() assigns every field below before anything reads it.
         */
        void releaseRetained() {
            this.deferredJson = null;
            this.hasDeferredJson = false;
            this.body = EMPTY_BODY;
            this.extraHeaders = null;
        }

        void reset(int status, String contentType, byte[] body, int fileFd,
                   long fileOffset, long fileLength, Map extraHeaders) {
            this.status = status;
            this.contentType = contentType;
            this.body = body == null ? EMPTY_BODY : body;
            this.fileFd = fileFd;
            this.fileOffset = fileOffset;
            this.fileLength = fileLength;
            this.extraHeaders = extraHeaders;
            this.deferredJson = null;
            this.hasDeferredJson = false;
        }

        public Response(int status, String contentType, byte[] body) {
            this(status, contentType, body == null ? new byte[0] : body, -1, 0, 0, null);
        }

        /**
         * A body AND application headers, which nothing public could express.
         *
         * The constructor above always passed null for them, empty() takes headers
         * but discards the body, and file() wants a descriptor -- so a handler
         * returning JSON with a Set-Cookie, a CORS header or a cache directive had
         * no supported way to say so, even though both protocol writers send extra
         * headers. Server-owned names are still refused at write time.
         */
        public Response(int status, String contentType, byte[] body, Map extraHeaders) {
            this(status, contentType, body == null ? new byte[0] : body, -1, 0, 0,
                    extraHeaders);
        }

        Response(int status, String contentType, byte[] body,
                 int fileFd, long fileOffset, long fileLength, Map extraHeaders) {
            this.status = status;
            this.contentType = contentType;
            this.body = body;
            this.fileFd = fileFd;
            this.fileOffset = fileOffset;
            this.fileLength = fileLength;
            this.extraHeaders = extraHeaders;
        }

        /**
         * A response whose body is a file. The server sends it with sendfile where
         * the platform has it, so the bytes never enter user space, and CLOSES the
         * descriptor when it is done -- a handler that returned one must not.
         */
        public static Response file(int status, String contentType, int fd,
                                    long offset, long length, Map extraHeaders) {
            return new Response(status, contentType, null, fd, offset, length, extraHeaders);
        }

        public static Response text(int status, String body) {
            return new Response(status, "text/plain; charset=utf-8", bytes(body));
        }

        public static Response json(int status, String body) {
            return new Response(status, "application/json; charset=utf-8", bytes(body));
        }

        /**
         * A JSON response serialised straight into the connection's write buffer.
         *
         * Prefer this to json(status, Json.write(value)): that builds a
         * StringBuilder, grows its char[], copies it into a String and encodes
         * that to bytes, for a document about to go to a socket and be discarded.
         * Deliberately a DIFFERENT NAME rather than an overload taking Object --
         * an overload would bind a String-typed variable to this method and
         * double-encode it, which is exactly the kind of thing that is found in
         * production rather than in review.
         */
        public static Response jsonValue(int status, Object value) {
            Response r = new Response(status, "application/json; charset=utf-8",
                    EMPTY_BODY, -1, 0, 0, null);
            r.deferredJson = value;
            r.hasDeferredJson = true;
            return r;
        }

        /** A response with headers but no body, for 304 and for HEAD. */
        public static Response empty(int status, String contentType, Map extraHeaders) {
            return new Response(status, contentType, new byte[0], -1, 0, 0, extraHeaders);
        }

        public int getStatus() {
            return status;
        }

        private static byte[] bytes(String s) {
            try {
                return s == null ? new byte[0] : s.getBytes("UTF-8");
            } catch (IOException err) {
                return new byte[0];
            }
        }
    }

    public interface Handler {
        Response handle(Request request) throws Exception;
    }

    /**
     * How long stop() waits, after closing the sockets, for workers to unwind before
     * it releases any session they might still have been inside.
     */
    private static final int SESSION_RELEASE_GRACE_MILLIS = 2000;

    private static final int MAX_HEADER_BYTES = 64 * 1024;

    /**
     * How much response body one HTTP/2 turn may hold before it drains.
     *
     * Not a limit on any response, which MAX_BODY_BYTES governs: a limit on how
     * many of them may sit copied into native buffers at once while this loop
     * keeps answering the next ready stream.
     */
    /**
     * What a request body buffer starts at, and doubles from as bytes arrive. Not
     * the declared Content-Length: see fillTo for why believing that number before
     * the body exists is what lets a client allocate memory it never has to send.
     */
    private static final int BODY_CHUNK_BYTES = 16 * 1024;

    /**
     * Request-body bytes held by uploads IN PROGRESS, across the process.
     *
     * Growing with the data removed the case where a client allocates 8MB by
     * declaring it and sending nothing. It does not bound the case where the
     * client really sends nearly all of it on many connections and pauses before
     * the last byte: that memory is real, it is held until the rate allowance
     * expires, and nothing counted it. The connection ceiling is in the
     * thousands, so a modest number of near-complete uploads is the machine.
     *
     * Scoped to the read, which is what makes it safe to account at all: the
     * charge is taken as the buffer grows and given back in a finally on every
     * path out of fillTo. A reservation that outlived the call would have to be
     * threaded through borrowed thread buffers, owned copies and every failure
     * path, and ONE leaked reservation wedges the server for good -- a worse
     * failure than the one it fixes. What this bounds is uploads in flight,
     * which is the shape of the attack.
     */
    private static final java.util.concurrent.atomic.AtomicLong http1UploadBytes =
            new java.util.concurrent.atomic.AtomicLong();

    private static final long MAX_HTTP1_UPLOAD_BYTES =
            envInt("CN1_HTTP_MAX_UPLOAD_MB", 64) * 1024L * 1024L;

    private static final long MAX_QUEUED_H2_BODY_BYTES = 4L * 1024 * 1024;

    /**
     * File-backed HTTP/2 responses that may be outstanding across the process.
     * A separate limit from the byte one because it is a separate resource: such
     * a response holds a DESCRIPTOR and no heap, so the byte figure never sees
     * it, and a peer that keeps its flow-control window shut holds one per
     * stream for as long as it likes. Descriptors run out process-wide, and when
     * they do the server stops accepting sockets and opening files entirely --
     * a failure with nothing to do with whoever caused it.
     */
    private static final int MAX_OPEN_H2_FILES = envInt("CN1_HTTP_MAX_H2_FILES", 128);

    /**
     * Response-body heap that may be outstanding across the PROCESS. The limit
     * beside it is per turn and per session, which bounds one connection -- and
     * the connection ceiling is in the thousands, so a body per connection is
     * still gigabytes. Memory runs out process-wide, so it is counted that way,
     * exactly like the descriptors above.
     */
    private static final long MAX_OPEN_H2_BODY_BYTES =
            envInt("CN1_HTTP_MAX_H2_BODY_MB", 64) * 1024L * 1024L;
    private static final int MAX_BODY_BYTES = 8 * 1024 * 1024;
    private static final int READY_CAPACITY = 256;

    /**
     * Bodies at or below this are sent together with the headers in one write.
     * Sized so an ordinary JSON response fits and a page-sized payload does not;
     * beyond it the copy costs more than the syscall it saves.
     */
    private static final int COMBINED_WRITE_LIMIT = 8192;

    private static final byte[] EMPTY_BODY = new byte[0];
    /** One instance, so the pooled JSON path does not intern a literal per call. */
    static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    /** What a Response with no content type is sent as, on either protocol. */
    static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    /** "Sat, 29 Aug 2026 07:11:02 GMT" -- RFC 9110 fixes the width. */
    private static final int HTTP_DATE_LENGTH = 29;

    /**
     * How long a worker waits, still holding the connection, for the NEXT request
     * before handing it back to the poller.
     *
     * This is the difference between a poller that is re-armed per request and one
     * that is not. Handing the descriptor back costs an epoll_ctl pair, two
     * blocking-mode flips and a cross-thread handoff -- measured against Go, whose
     * netpoller registers a connection once: 2 epoll_ctl, 4 fcntl and 3.9 futex
     * per request against its 0.00, 0.00 and 0.05, with the futex traffic alone
     * 80% of our syscall time.
     *
     * A client that is going to send another request usually sends it within
     * microseconds, so a few milliseconds captures nearly all of them. Set to 0 to
     * hand back immediately, which is the behaviour this replaces.
     *
     * The timeout alone does NOT bound how long a worker keeps a connection: a
     * client that keeps sending is readable every time, so the worker goes round
     * again and holds it indefinitely. Under continuous load that made the pool
     * the limit on concurrent clients -- exactly what the reactor exists to
     * prevent -- and it was invisible in a throughput number, because the
     * connections that DID hold a worker were served at full speed while the rest
     * starved. A fresh connection got no response in five seconds while the
     * benchmark reported 234k requests a second. What bounds it is
     * {@link #pendingWork} below, plus the burst cap.
     */
    /**
     * Which thread takes a ready descriptor from the poller.
     *
     *   0  A dedicated reactor thread calls the poller and DISPATCHES: it
     *      deregisters the descriptor, allocates a task, queues it and wakes a
     *      worker. That wake is a futex and a context switch, and the descriptor
     *      has to be registered again afterwards, so an ordinary request costs
     *      two epoll_ctl and a cross-thread handoff on top of its own read and
     *      write.
     *   1  The WORKERS call the poller themselves. A worker with nothing to do
     *      waits on the same set and serves the first descriptor it is given, on
     *      the thread that polled -- no queue, no wake, no task object. The
     *      descriptor is armed {@link Reactor#ONESHOT} so the kernel hands it to
     *      exactly one waiter, and re-arming afterwards is a single epoll_ctl.
     *
     * Mode 1 is what Go's scheduler does. `netpoll()` is called from
     * `findRunnable()` on whatever thread has run out of work, and the result is
     * `gp := list.pop(); injectglist(&list); return gp` -- it runs the first
     * ready goroutine ON THE POLLING THREAD and only queues the remainder. A
     * syscall census of the two servers under the same load put us at 6.9x Go's
     * futex rate and 41x its epoll_ctl rate while the read and write counts
     * matched to within 5%, which says the gap is coordination rather than work.
     */
    /**
     *   2  ONE worker polls at a time. It serves the first ready descriptor on
     *      its own thread and queues the remainder for the others, so a lone
     *      event -- the common case -- costs no handoff at all, while a burst
     *      pays one wake per SURPLUS descriptor rather than one per request.
     *
     * Mode 2 is what Go actually does, and mode 1 is what it looks like from a
     * distance. The difference is a guard in `findRunnable` that mode 1 has no
     * equivalent of: "we can safely skip it if there are no waiters or A THREAD
     * IS BLOCKED IN NETPOLL ALREADY". Go never has two threads in the poller.
     * Mode 1 puts every worker in `epoll_wait` on one set, so a single arriving
     * event wakes all of them; ONESHOT still guarantees only one RECEIVES the
     * descriptor, but the other wakeups happen anyway and cost more the more
     * workers there are. Measured, that is exactly what mode 1 does: +40% on two
     * workers, +24% on four, and -25% on eight.
     */
    /**
     *   3  A VIRTUAL THREAD per connection. Host threads poll and resume; a
     *      connection's virtual thread runs until it finishes or asks for bytes
     *      that have not arrived, and parks inside the ordinary blocking read.
     *      There is no handoff at all, and no thread per connection either.
     *
     * The numbers that motivate mode 3 rather than more tuning of 0 to 2: moving
     * a request between OS threads measured 21181ns on the machine this was built
     * on, switching a virtual thread measured 2.6ns, and the paired experiment
     * over modes 0 to 2 showed the handoff is worth about a third of throughput
     * at four workers while REMOVING it costs about a third at sixteen -- because
     * a pool large enough to hide the handoff is a pool large enough to lose to
     * the OS scheduler. A virtual thread is how a context per connection stops
     * implying an OS thread per connection, which is the assumption that made
     * those two facts irreconcilable.
     */
    /**
     * 0 the reactor thread dispatches to a pool; 3 a virtual thread per connection.
     *
     * Modes 1 and 2 were two ways of letting the WORKERS poll, and the paired
     * experiment killed both: removing the dispatch is worth about a third of
     * throughput at four workers and costs about a third at sixteen, because a
     * pool big enough to hide the handoff is a pool big enough to lose to the OS
     * scheduler. Their numbers are in the benchmarks README; the code is gone
     * rather than left to rot, since a mode nobody selects is a mode nobody
     * tests.
     */
    /*
     * The DEFAULT is virtual threads wherever the build has them, and the pool
     * only where it does not.
     *
     * Virtual threads are not a tuning option here, they are the mode that
     * matches Go on the TAIL: measured p99 1.58 ms against the pool's 59.70 ms on
     * the same host, and a corrected syscall census puts their futex traffic at
     * 0.000 per request against the pool's 0.265. Leaving the pool as the default
     * shipped the worse tail to everyone who did not know to set an environment
     * variable, and left the better path exercised only by benchmarks.
     *
     * It is a TRADE, not a free win, and the cost is throughput. Four arms
     * interleaved on a quiet host, /plaintext at 64 connections, medians of four
     * steady-state reps (spread 0.4-4.9%):
     *
     *     go                243,161 req/s
     *     pool, 64 workers  239,155        0.972 of go
     *     virtual threads   207,808        0.854 of go
     *
     * So the pool is within 3% of Go on throughput and virtual threads are 13%
     * behind it. That gap is NOT syscalls -- the census has virtual threads at
     * 3.02 per request against the pool's 3.29 -- so it is user-space switch and
     * scheduling cost, which is where to look next if this default is to stop
     * costing anything.
     *
     * Conditioned on VirtualThread.supported() rather than assumed: the context
     * switch is compiled in only on non-Windows aarch64/x86_64, and elsewhere
     * create() can only return 0, which would drop every connection instead of
     * falling back. Setting CN1_HTTP_POLL_MODE explicitly still overrides this
     * in either direction.
     */
    private static final int POLL_MODE =
            envInt("CN1_HTTP_POLL_MODE", VirtualThread.supported() ? 3 : 0);
    private static final boolean VIRTUAL_THREADS = POLL_MODE == 3;

    /**
     * C stack per connection. Java locals and the operand stack are NOT here --
     * they live in the virtual thread's own VM state, mapped lazily -- so this
     * buys call depth rather than data. 64KB holds a few hundred nested Java
     * frames, well past what an HTTP handler needs, and is mapped lazily too.
     */
    private static final int VT_STACK_BYTES = envIntAtLeast("CN1_HTTP_VT_STACK", 64 * 1024, 1);

    /**
     * How often a virtual-thread host sweeps its deadlines, however busy it is.
     * The same 250ms the idle poll waits, so a quiet host behaves exactly as
     * before and a busy one stops being exempt.
     */
    private static final long SWEEP_INTERVAL_MILLIS = 250;

    private static final int KEEPALIVE_LINGER_MILLIS =
            envInt("CN1_HTTP_KEEPALIVE_LINGER_MS", 5);

    /**
     * How long a worker waits for a request, and for the client to take the
     * response. A connection that opens and says nothing would otherwise hold a
     * worker forever, and the pool is bounded on purpose -- open as many silent
     * connections as there are workers and the server stops answering anyone.
     */
    private static final int SOCKET_TIMEOUT_MILLIS =
            envIntAtLeast("CN1_HTTP_TIMEOUT_MS", 15000, 1);

    /**
     * The slowest upload this server will wait for, in bytes per second.
     *
     * 8 KB/s is well under any real link and still bounds a body: 8 MiB has about
     * seventeen minutes to arrive, and a client sending a byte at a time does not
     * get them. Set CN1_HTTP_MIN_BODY_RATE to change it.
     */
    private static final int MIN_BODY_BYTES_PER_SECOND =
            envIntAtLeast("CN1_HTTP_MIN_BODY_RATE", 8192, 1);

    /**
     * Ceiling on open connections. Past it a connection is accepted and closed
     * immediately rather than left in the backlog: refusing is a fast, legible
     * answer, while a full backlog looks to a client like a server that hangs. Set
     * to 0 for no ceiling.
     */
    private static final int MAX_CONNECTIONS = envInt("CN1_HTTP_MAX_CONNECTIONS", 4096);

    /**
     * A tunable that must be at least `minimum`, or the default is used instead.
     *
     * Some of these settings are DIVISORS or array sizes, and a zero reaches very
     * different places on the two runtimes: Java SE throws ArithmeticException,
     * which the reader catches as an ordinary read failure and drops the
     * connection with no response, while ParparVM answers 0 for an integer
     * division by zero -- so the packaged binary silently loses the rate part of
     * its own deadline instead. Neither is what anyone typed 0 hoping for, and a
     * setting that behaves differently in the dev loop than in production is the
     * exact divergence this backend keeps being reviewed for.
     *
     * Package-visible so the runtime self-test can check the clamp itself on both
     * arms; the values it guards are read once at class initialisation, which no
     * test can reach.
     */
    static int atLeast(String name, int value, int minimum) {
        if(value >= minimum) {
            return value;
        }
        System.err.println(name + "=" + value + " is below the minimum of " + minimum
                + "; using the default instead");
        return -1;
    }

    private static int envIntAtLeast(String name, int fallback, int minimum) {
        int value = envInt(name, fallback);
        return atLeast(name, value, minimum) < 0 ? fallback : value;
    }

    private static int envInt(String name, int fallback) {
        String v = System.getenv(name);
        if(v == null || v.length() == 0) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException err) {
            return fallback;
        }
    }

    /**
     * Set CN1_HTTP_TRACE=1 to print what the reactor sees. A reactor that is not
     * reporting readiness looks exactly like a handler that is not responding, and
     * the only way to tell them apart from outside is to ask which one is silent.
     */
    private static final boolean TRACE = "1".equals(System.getenv("CN1_HTTP_TRACE"));

    private static void trace(String message) {
        if(TRACE) {
            System.err.println("[http] " + message);
        }
    }

    private final ServerSocket listener;
    private final Reactor reactor;
    private final ExecutorService workers;
    private final Handler handler;
    private final Tls tls;
    /**
     * fd to SSL session. Only written when a connection is established or closed,
     * never per request. A TLS connection genuinely costs an object; the plain
     * server allocates nothing per idle connection and this map is why that
     * property does not carry over to TLS.
     */
    private final Map sessions = java.util.Collections.synchronizedMap(new java.util.HashMap());
    /** fd to HTTP/2 session, for connections where ALPN settled on h2. */
    private final Map http2Sessions = java.util.Collections.synchronizedMap(new java.util.HashMap());
    /**
     * Every accepted descriptor that has not been dropped yet.
     *
     * The TLS and HTTP/2 maps only hold the connections that have one of those, so
     * a plaintext connection appeared in neither and stop() had nothing to close it
     * with. It would stay open past the drain deadline while the server reported
     * itself fully stopped.
     */
    private final Map liveConnections = java.util.Collections.synchronizedMap(new java.util.HashMap());

    /**
     * When each PARKED pooled connection stops being worth keeping.
     *
     * The virtual-thread path has this on its hosts, keyed by descriptor and swept
     * by the poller. The pooled reactor had nothing: it registered the descriptor
     * and left, and SO_RCVTIMEO cannot expire a socket while no thread is inside
     * recv, so an accepted connection that said nothing -- or a keep-alive one
     * re-armed and then abandoned -- stayed in liveConnections for ever. Enough of
     * them reach MAX_CONNECTIONS and every later client is refused, which is the
     * cheapest denial of service there is. Every Java SE run and every TLS server
     * takes this path.
     *
     * Only while PARKED: handOff removes the entry, because a connection a worker
     * is serving is bounded by the request deadlines instead.
     */
    private final Map pooledDeadlines =
            java.util.Collections.synchronizedMap(new java.util.HashMap());
    private volatile boolean running = true;
    private Thread loop;
    /** Released only when stop() has finished draining. See awaitTermination. */
    private final Object stopped = new Object();
    private boolean fullyStopped;
    private final java.util.concurrent.atomic.AtomicInteger openConnections =
            new java.util.concurrent.atomic.AtomicInteger();
    /**
     * Requests actually being served, as opposed to connections being held.
     *
     * activeRequests counts a worker's whole stay on a connection, which the pool
     * sizing below genuinely wants -- but in virtual-thread mode a worker owns a
     * keep-alive connection for its lifetime and parks between requests, so that
     * number stays positive while the client sits idle. Reported as saturation it is
     * wrong, and stop() waiting on it meant one idle keep-alive client held shutdown
     * for the entire drain window.
     */
    /**
     * HTTP/2 turns inside nghttp2, which stop() has to wait for as well.
     *
     * Separate from inFlightRequests rather than folded into it: that one is what
     * getMetrics reports as active requests, and a connection pumping control
     * frames or flushing after its last stream is not a request in flight -- but
     * it IS a reason not to free the session under it.
     */
    private final java.util.concurrent.atomic.AtomicInteger http2Turns =
            new java.util.concurrent.atomic.AtomicInteger();

    private final java.util.concurrent.atomic.AtomicInteger inFlightRequests =
            new java.util.concurrent.atomic.AtomicInteger();

    private final java.util.concurrent.atomic.AtomicInteger activeRequests =
            new java.util.concurrent.atomic.AtomicInteger();
    /**
     * Requests answered, striped one slot per host thread.
     *
     * A profile put AtomicLong.incrementAndGet among the hottest symbols in this
     * server: requestsServed was one CONTENDED atomic per request, and with the
     * host threads pinned to two cores every increment moved a cache line between
     * them. Each slot here has a single writer -- the host thread that owns the
     * connection -- so the increment is a plain add, and the health endpoint sums
     * the stripes. Slots are 8 longs apart so two hosts never share a cache line,
     * which is the whole point of striping and easy to leave out by accident.
     *
     * Kept alongside requestsServed rather than replacing it: the reactor mode has
     * no hosts to stripe by, and still uses the atomic.
     */
    private static final int SERVED_STRIPE_STRIDE = 8;
    private long[] servedStripes = new long[0];

    private long servedTotal() {
        long total = requestsServed.get();
        long[] st = servedStripes;
        for(int i = 0 ; i < st.length ; i += SERVED_STRIPE_STRIDE) {
            total += st[i];
        }
        return total;
    }

    private final java.util.concurrent.atomic.AtomicLong requestsServed =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong connectionsAccepted =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong connectionsRefused =
            new java.util.concurrent.atomic.AtomicLong();
    private final long startedAt = System.currentTimeMillis();


    /**
     * How many requests one worker may serve on one connection before handing it
     * back even when nothing else is waiting.
     *
     * A backstop under the pendingWork check rather than the main mechanism: it
     * bounds the damage if that check is ever wrong. In virtual-thread mode the
     * cap still applies but its ACTION is to step aside rather than to close --
     * see where it is used.
     */
    private static final int KEEPALIVE_BURST_LIMIT =
            envInt("CN1_HTTP_KEEPALIVE_BURST", 256);

    /** How many requests may be in flight at once; the pool size. */
    private final int workerCount;

    /**
     * Whether THIS server runs on virtual threads, which is not the same question as
     * whether the build supports them.
     *
     * A TLS server does not, however POLL_MODE is set. Tls.readImpl maps
     * SSL_ERROR_WANT_READ to a hard error rather than parking, so a TLS descriptor
     * has to stay blocking -- and a blocking descriptor on a virtual thread holds
     * its host OS thread for the whole read. With one host per core, one idle TLS
     * client per core occupies every host and unrelated connections stop being
     * served. A thread pool has a worse ceiling and an honest one; virtual threads
     * here have a better ceiling that a single slow client removes.
     *
     * So TLS falls back to the pool until the TLS layer can park. This is decided
     * once, here, rather than tested at each use, because half a server in each mode
     * is neither.
     */
    private final boolean virtualThreads;

    private HttpServer(ServerSocket listener, Reactor reactor, ExecutorService workers,
                       int workerCount, Handler handler, Tls tls) {
        this.listener = listener;
        this.reactor = reactor;
        this.workers = workers;
        this.workerCount = workerCount;
        this.handler = handler;
        this.tls = tls;
        // Derived from the decision the caller actually made, not recomputed from
        // the statics behind it. Recomputing was right while "plaintext" was the
        // only condition, but a server can now fall back to the pool for a second
        // reason -- another server already holds the single virtual-thread slot --
        // and a server that recomputed would have run on this pool while believing
        // itself virtual, which changes parking, keep-alive linger, ownership and
        // teardown. No pool means virtual threads; that is the whole of it.
        this.virtualThreads = workers == null;
    }

    /**
     * Arming used for connection descriptors.
     *
     * Plain level-triggered READ, with no ONESHOT and so no re-arm, and affinity
     * is what makes that safe. A descriptor lives in exactly ONE host's epoll
     * set, and that host is not polling while it is inside advance() running the
     * virtual thread, so no second thread can ever be handed a descriptor whose
     * virtual thread is already running. ONESHOT was guarding against a hazard
     * that only exists when several threads share a poller.
     *
     * What it cost to keep it was an epoll_ctl on every park, which is the exact
     * syscall Go does not pay: it registers each descriptor once and never
     * touches epoll again for the life of the connection. A profile of the
     * plaintext benchmark put epoll_ctl at 4.65% of in-binary self time, so the
     * re-arm is now gone and a descriptor stays armed for the whole connection.
     *
     * ONESHOT was doing one more thing than the hazard above, and dropping it
     * without replacing that is a use-after-free. The kernel DISARMS on delivery,
     * so a descriptor whose virtual thread returned RUNNABLE -- queued in the
     * ring, neither running nor parked -- could not be reported again while it
     * sat there. Left armed it can be, and advance() would resume a handle the
     * ring is also about to resume. That invariant is now explicit: the RUNNABLE
     * path disarms with a remove() and VtHost.armedByFd remembers it, which costs
     * a syscall on the yield path instead of on every request.
     */
    private static final int CONN_EVENTS = Reactor.READ;

    /**
     * Ready descriptors handed to the pool and not yet picked up.
     *
     * The keep-alive linger is bounded by this rather than by its timeout: a
     * client that keeps sending is readable every time, so a worker would hold
     * one connection for ever and the pool would become the limit on concurrent
     * clients. A fresh connection got no response in five seconds while the
     * benchmark reported 234k requests a second.
     */
    private final java.util.concurrent.atomic.AtomicInteger pendingWork =
            new java.util.concurrent.atomic.AtomicInteger(0);

    /** Set only when a poller-per-worker mode is on; the threads that poll. */
    private Thread[] pollers;

    private final java.util.concurrent.atomic.AtomicInteger vtAccepts =
            new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger vtDispatched =
            new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger vtCreateFailures =
            new java.util.concurrent.atomic.AtomicInteger(0);

    public static HttpServer start(String host, int port, int backlog, int workerCount, Handler handler)
            throws IOException {
        return start(host, port, backlog, workerCount, handler, null);
    }

    /**
     * - `tls`: terminate TLS here, or null to serve plaintext (correct behind a
     *   load balancer that already terminated it)
     */
    public static HttpServer start(String host, int port, int backlog, int workerCount,
                                   Handler handler, Tls tls) throws IOException {
        // BEFORE THE BIND, because the two arms failed this differently and both
        // badly. Java SE's Executors.newFixedThreadPool throws for a non-positive
        // count -- but only after the listener and the reactor are open, so the
        // port stayed bound and a caller retrying on it met "address already in
        // use" instead of the argument error. The packaged runtime's thread pool
        // simply created no workers and handed back a server that accepts
        // connections and queues them forever, which is worse than either. One
        // check here and neither can happen.
        if(workerCount < 1) {
            throw new IOException("workerCount must be at least 1, not " + workerCount);
        }
        ServerSocket listener = ServerSocket.bind(host, port, backlog);
        Reactor reactor;
        try {
            reactor = Reactor.create();
        } catch (IOException err) {
            listener.close();
            throw err;
        }
        try {
            ServerSocket.setBlocking(listener.getFd(), false);
            reactor.add(listener.getFd(), CONN_EVENTS);
        } catch (IOException err) {
            // Both of these can fail -- descriptor exhaustion reaches epoll_ctl as
            // readily as anything else -- and they sit AFTER the bind and the
            // reactor, with no HttpServer in existence yet for the caller to close.
            // Left uncovered, the port stayed bound so a retry met "address already
            // in use", and each attempt leaked another poller descriptor. The same
            // cleanup the Reactor.create() above and the virtual-thread setup below
            // already get.
            reactor.close();
            listener.close();
            throw err;
        }

        // No worker pool in virtual-thread mode. workers.execute() is reached only
        // from handOff(), which is reached only from pump(), which runs only in
        // the branch below this one -- so in this mode every pooled thread is
        // created, parked, and never given anything to do.
        //
        // They are not free. Each is a Java thread of control: the collector
        // conservatively scans its native stack and its 258KB object stack on
        // every cycle, and stop-the-world sets threadBlockedByGC on each and
        // spins `while(t->threadActive)` waiting for it. Measured on two pinned
        // cores with the host count held equal by the clamp above -- so the pool
        // size was the only variable -- WORKERS=64 segfaulted 2 runs in 6 and
        // WORKERS=4 survived 6 of 6. Throughput was unaffected when it did not
        // crash (265k either way), so this buys robustness rather than speed.
        boolean useVirtualThreads = VIRTUAL_THREADS && tls == null;
        // The native virtual thread carries the accepted descriptor and nothing
        // else, so the Java side finds its server through one process-global. A
        // second virtual-thread server would replace it, and every connection the
        // FIRST listener had accepted would then be served by the second one's
        // handler and ownership maps -- an administrative port answering public
        // requests, and neither able to shut down what it owns. Only one server
        // can hold that slot; the next takes the pool, which is per instance and
        // has no such ambiguity. Claimed before the server is built so two
        // starting at once cannot both win it.
        if(useVirtualThreads && !VT_SLOT_TAKEN.compareAndSet(false, true)) {
            System.out.println("another virtual-thread server is already running in "
                    + "this process, so this one runs on a thread pool: an accepted "
                    + "descriptor is all a virtual thread carries, and it cannot say "
                    + "which server to hand it to.");
            useVirtualThreads = false;
        }
        if(VIRTUAL_THREADS && tls != null) {
            System.out.println("TLS is configured, so this server runs on a thread "
                    + "pool rather than virtual threads: the TLS layer cannot park a "
                    + "read yet, and a blocking read on a virtual thread holds its "
                    + "host for the duration.");
        }
        final HttpServer server = new HttpServer(listener, reactor,
                useVirtualThreads ? null : Executors.newFixedThreadPool(workerCount),
                workerCount, handler, tls);
        if(useVirtualThreads) {
            ACTIVE_SERVER = server;
            // A poller PER HOST, because affinity is enforced by the poller: a
            // descriptor registered in one host's set can only ever be reported
            // to that host, so its virtual thread cannot run anywhere else. The
            // listener lives in host 0's set, so exactly one host accepts and
            // hands each new connection to its permanent owner.
            // HOSTS TRACK CORES, not the caller's expected concurrency.
            //
            // In this mode workerCount stops meaning "how many requests may be in
            // flight" -- the virtual threads supply that, one per connection --
            // and a host thread is only useful while there is a core free to run
            // it on. Past that they contend for the cores the server needs:
            // measured on two pinned cores, 16 hosts served 117 requests where 2
            // served 257297. Unpinned, with cores to spare, 2 through 32 all
            // behave, so the ceiling has to come from the machine at runtime.
            //
            // Clamped rather than obeyed, because a caller asking for 64 workers
            // is asking for concurrency, and in this mode that request is
            // answered by the virtual threads instead.
            int hostCount = workerCount;
            int cores = ServerSocket.availableProcessors();
            if(hostCount > cores) {
                hostCount = cores;
            }
            if(hostCount < 1) {
                hostCount = 1;
            }
            server.vtHosts = new VtHost[hostCount];
            // One cache line per host, so the stripes never share one.
            server.servedStripes = new long[hostCount * SERVED_STRIPE_STRIDE];
            // UNDER CLEANUP, because everything from here can fail and the caller
            // gets no HttpServer to close when it does. Reactor.create() is a
            // descriptor and Thread.start() is a thread, so descriptor or memory
            // exhaustion throws HERE -- after the listener is bound, after the
            // reactor is open, and after ACTIVE_SERVER and VT_SLOT_TAKEN were
            // claimed above. Left as it was, the port stayed bound so a retry met
            // "address already in use", and the slot stayed taken so every later
            // server in the process fell back off virtual threads permanently --
            // from a failure that was transient.
            try {
                for(int iter = 0 ; iter < hostCount ; iter++) {
                    server.vtHosts[iter] = new VtHost(iter == 0 ? reactor : Reactor.create());
                }
                server.pollers = new Thread[hostCount];
                for(int iter = 0 ; iter < hostCount ; iter++) {
                    final int index = iter;
                    server.pollers[iter] = new Thread(new Runnable() {
                        public void run() {
                            server.runVirtualThreadHost(index);
                        }
                    });
                    server.pollers[iter].start();
                }
            } catch (IOException err) {
                abandonStart(listener, server);
                throw err;
            } catch (RuntimeException err) {
                // Thread.start() answers with one of these, and OutOfMemoryError
                // is not caught on purpose: there is nothing left to clean up
                // with.
                abandonStart(listener, server);
                throw err;
            }
        } else {
            server.loop = new Thread(new Runnable() {
                public void run() {
                    server.pump();
                }
            });
            server.loop.start();
        }
        return server;
    }

    /**
     * Undoes a start that failed after the listener was bound.
     *
     * <p>The order matters and is the same as stop()'s: stop the loops, wait for
     * them to leave their reactors, then close. A poller already started is
     * inside its reactor, and closing that under it is the use-after-free stop()
     * goes out of its way to avoid -- a failed start is not a reason to take the
     * process down with it.
     */
    private static void abandonStart(ServerSocket listener, HttpServer server) {
        server.running = false;
        if(server.pollLoopsEnded(POLL_LOOP_JOIN_MILLIS)) {
            server.closePollers();
        }
        listener.close();
        // Last, so a server starting concurrently cannot take the slot while this
        // one is still closing the reactors it claimed with it.
        server.releaseVirtualThreadSlot();
    }

    public int getPort() {
        return listener.getPort();
    }

    /** Connections currently open. */
    public int getOpenConnections() {
        return openConnections.get();
    }

    /** Requests being handled right now. This is what saturation looks like. */
    public int getActiveRequests() {
        return inFlightRequests.get();
    }

    /**
     * A snapshot for a health or metrics endpoint. "draining" is what a load
     * balancer needs to see to take this instance out of rotation before it stops
     * answering.
     */
    public Map getMetrics() {
        Map out = new LinkedHashMap();
        out.put("status", running ? "ok" : "draining");
        out.put("uptimeSeconds", new Long((System.currentTimeMillis() - startedAt) / 1000L));
        out.put("openConnections", new Integer(openConnections.get()));
        out.put("activeRequests", new Integer(inFlightRequests.get()));
        out.put("requestsServed", new Long(servedTotal()));
        out.put("connectionsAccepted", new Long(connectionsAccepted.get()));
        out.put("connectionsRefused", new Long(connectionsRefused.get()));
        out.put("tls", tls == null ? "off" : "on");
        out.put("http2Connections", new Integer(http2Sessions.size()));
        // A descriptor handed to a Response and not yet closed. Reported
        // because nothing else can see one that escapes: the process limit is
        // enormous, so a leak surfaces hours later as a server that cannot
        // accept sockets, with nothing pointing at the cause.
        out.put("openStaticFiles", new Integer(StaticFiles.openFileCount()));
        return out;
    }

    /**
     * Blocks until the server has fully stopped, draining included.
     *
     * A caller's main() must do this or something equivalent: the reactor and the
     * workers run on threads ParparVM creates DETACHED, so when main returns the
     * process exits and takes them with it -- silently, with status 0, which from
     * outside looks exactly like a server that refuses connections.
     *
     * Waiting on the reactor THREAD is not enough, and that was a real bug: stop()
     * clears the running flag, the loop returns on its next timeout, main wakes up
     * and the process ends while a worker is still writing a response. This waits
     * on the drain finishing instead.
     */
    public void awaitTermination() {
        synchronized(stopped) {
            while(!fullyStopped) {
                try {
                    stopped.wait();
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    /**
     * Stops accepting, lets in-flight requests finish, then closes what is left.
     *
     * The order matters. Closing the listener first means no new work arrives while
     * the pool drains; draining before closing connections means a request already
     * being served gets to produce its response instead of having the socket pulled
     * out from under it, which is what a client sees as a truncated reply.
     */
    public void stop(int drainMillis) {
        running = false;
        reactor.remove(listener.getFd());
        listener.close();
        if(workers != null) {           // null in virtual-thread mode; see start()
            workers.shutdown();
        }
        long deadline = System.currentTimeMillis() + drainMillis;
        // Waits on requests IN FLIGHT, not on open connections: an idle keep-alive
        // connection has nothing to finish and would otherwise hold the shutdown
        // open for the whole window for no reason.
        //
        // http2Turns as well, which this loop was missing when that counter was
        // added: a turn holds the session and is not a request in flight, so a
        // connection pumping frames was not waited for here at all.
        //
        // NOT covered, deliberately: a response already handed to nghttp2 whose
        // DATA frames are still waiting on the peer's flow-control window. No
        // worker is inside that connection, so nothing here can see it -- and the
        // way to see it, asking the session whether it still wants to write, means
        // calling into nghttp2 from THIS thread while a worker may be inside the
        // same session, which is the race the descriptor-first teardown below
        // exists to avoid. Answering it safely needs the worker to record the
        // answer at the end of its own turn; until then such a response can still
        // be cut short by a stop(), and that is a smaller fault than a native data
        // race during shutdown.
        while(System.currentTimeMillis() < deadline && workOutstanding()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Whatever is still open at the deadline is an idle keep-alive connection or
        // a request that overran; both have to be closed rather than held forever.
        //
        // The DESCRIPTOR first, and only the descriptor. A worker past the deadline
        // may be sitting inside SSL_read or nghttp2 on this very connection, and
        // freeing the session under it is a native use-after-free -- a crash during
        // shutdown, which is exactly when the remaining work is least recoverable.
        // Closing the socket instead unblocks that worker: its next read fails, and
        // it takes its own connection down through drop(), which frees the session on
        // the thread that was using it.
        java.util.Iterator live = new java.util.ArrayList(liveConnections.keySet()).iterator();
        while(live.hasNext()) {
            ServerSocket.closeFd(((Integer)live.next()).intValue());
        }
        // Then give those workers a moment to notice and unwind. Freeing a session
        // while one is still inside it is the thing being avoided, so the sweep below
        // waits for the count to reach zero rather than assuming it has.
        long freeBy = System.currentTimeMillis() + SESSION_RELEASE_GRACE_MILLIS;
        while(System.currentTimeMillis() < freeBy && workOutstanding()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // The wait above ends on the count reaching zero OR on the grace period
        // expiring, and only the first of those means nothing is running. A
        // handler still inside a request holds the very TLS and HTTP/2 sessions
        // the sweeps below free, and would then write its response through freed
        // native memory -- so when the window expired with work outstanding, the
        // sessions are left alone. That leaks one per live connection, which a
        // process about to exit does not care about and a use-after-free is not
        // a trade for.
        if(workOutstanding()) {
            releaseVirtualThreadSlot();
            synchronized(stopped) {
                fullyStopped = true;
                stopped.notifyAll();
            }
            return;
        }
        // Anything still registered had no worker to take it down -- an idle
        // connection in reactor mode, where nothing runs for it once its descriptor
        // is gone. With no request in flight there is no one left to race, so these
        // are safe to release here, and leaving them would leak a native session per
        // connection for the life of the process.
        // The parked VIRTUAL THREADS first, because drop() cannot reclaim them.
        // A connection parked between requests holds a handle in its host's table
        // and no request in flight, so the wait above finds nothing to wait for and
        // comes straight here. drop() then closes the descriptor and returns,
        // leaving the handle -- and its native stack and VM thread registration --
        // allocated. sweepDeadlines is what normally frees those, and it runs from
        // the poll loop, which `running = false` has already ended. The process
        // keeps every one of them, which matters precisely because
        // releaseVirtualThreadSlot() exists so a server CAN be started again here.
        freeParkedVirtualThreads();
        java.util.Iterator stranded = new java.util.ArrayList(liveConnections.keySet()).iterator();
        while(stranded.hasNext()) {
            drop(((Integer)stranded.next()).intValue());
        }
        // Belt and braces: a session recorded for a descriptor that was already
        // dropped would otherwise never be freed.
        synchronized(sessions) {
            java.util.Iterator it = new java.util.ArrayList(sessions.keySet()).iterator();
            while(it.hasNext()) {
                Object key = it.next();
                Object session = sessions.remove(key);
                if(session != null) {
                    Tls.closeSession(((Long)session).longValue());
                }
            }
        }
        synchronized(http2Sessions) {
            java.util.Iterator it = new java.util.ArrayList(http2Sessions.keySet()).iterator();
            while(it.hasNext()) {
                Object h2 = http2Sessions.remove(it.next());
                if(h2 != null) {
                    ((Http2)h2).close();
                }
            }
        }
        if(tls != null) {
            tls.close();
        }
        // THE POLLERS, which nothing closed. Each Reactor owns an epoll or kqueue
        // descriptor -- a Selector on Java SE -- so a process that stops and starts
        // a server, which releaseVirtualThreadSlot() exists precisely to allow,
        // leaked one per cycle until it ran out of descriptors.
        //
        // Only on THIS path, and only once the loops have left them. The early
        // return above leaves workers running and they reach the reactor; closing
        // it under one is the same use-after-free the session sweeps go out of
        // their way to avoid, and the same trade is taken there -- a process about
        // to exit can afford a descriptor, and cannot afford a crash.
        if(pollLoopsEnded(POLL_LOOP_JOIN_MILLIS)) {
            closePollers();
        }
        releaseVirtualThreadSlot();
        synchronized(stopped) {
            fullyStopped = true;
            stopped.notifyAll();
        }
    }

    /**
     * Whether any worker is queued, running, or inside a request or an h2 turn.
     *
     * <p>ALL FOUR counters, which is the point of having one predicate. The drain
     * loops waited on inFlightRequests and http2Turns alone, and both of those are
     * still zero while a task sits in the pool's queue or a worker is handshaking
     * or parsing a request line -- so a stop() could decide nothing was running,
     * free the TLS and HTTP/2 sessions, and let a task ExecutorService.shutdown()
     * still permits run straight into them. pendingWork covers the queued window
     * and activeRequests the worker's whole stay on a connection; both already
     * existed and neither was consulted here.
     *
     * <p>The comment on the first drain loop records http2Turns being added to one
     * loop and not the others, which is this same drift once already. One method
     * is what stops it happening a third time.
     */
    private boolean workOutstanding() {
        return inFlightRequests.get() > 0 || http2Turns.get() > 0
                || pendingWork.get() > 0 || activeRequests.get() > 0;
    }

    /** How long stop() waits for the poll loops before giving up on closing them. */
    private static final int POLL_LOOP_JOIN_MILLIS = 2000;

    /** Waits for every poll loop to end, and answers whether they all did. */
    private boolean pollLoopsEnded(long millis) {
        long deadline = System.currentTimeMillis() + millis;
        boolean all = true;
        Thread[] threads = pollers;
        if(threads != null) {
            for(int iter = 0 ; iter < threads.length ; iter++) {
                // Every one of them, not just until the first that outstays its
                // welcome: a poller still inside its reactor is exactly the one
                // whose reactor must be left alone.
                all = endedBy(threads[iter], deadline) && all;
            }
        }
        return endedBy(loop, deadline) && all;
    }

    private boolean endedBy(Thread thread, long deadline) {
        if(thread == null) {
            return true;
        }
        long left = deadline - System.currentTimeMillis();
        try {
            thread.join(left > 0 ? left : 1);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
        return !thread.isAlive();
    }

    /** Closes every reactor exactly once. */
    private void closePollers() {
        VtHost[] hosts = vtHosts;
        if(hosts != null) {
            for(int iter = 0 ; iter < hosts.length ; iter++) {
                // Host 0 SHARES the main reactor (see start()), so closing every
                // host's poller and then the reactor would close that one twice --
                // a double free of one descriptor, not the release of two.
                if(hosts[iter] != null && hosts[iter].poller != reactor) {
                    hosts[iter].poller.close();
                }
            }
        }
        reactor.close();
    }

    /**
     * Hands the single virtual-thread slot back, so a server started later in this
     * process can have it. Only the holder releases it: a second server that fell
     * back to the pool must not free the running one's claim when it stops.
     */
    /**
     * Frees every virtual thread still parked on a connection, at shutdown.
     *
     * Only safe because nothing is running by the time it is called: the poll loop
     * has stopped, so no host can resume one of these handles, and a handle that is
     * freed while its thread could still be resumed is a use-after-free -- the same
     * hazard the RUNNABLE path guards with poller.remove().
     */
    private void freeParkedVirtualThreads() {
        VtHost[] hosts = vtHosts;
        if(hosts == null) {
            return;
        }
        for(int h = 0 ; h < hosts.length ; h++) {
            VtHost host = hosts[h];
            if(host == null) {
                continue;
            }
            for(int fd = 0 ; fd < host.vtByFd.length ; fd++) {
                long handle = host.handleFor(fd);
                if(handle != 0) {
                    host.setHandle(fd, 0);
                    host.setDeadline(fd, 0);
                    VirtualThread.free(handle);
                }
            }
        }
    }

    private void releaseVirtualThreadSlot() {
        if(virtualThreads) {
            ACTIVE_SERVER = null;
            VT_SLOT_TAKEN.set(false);
        }
    }

    /** Stops with a default drain window. */
    public void stop() {
        stop(10000);
    }

    private void pump() {
        trace("reactor thread started");
        int[] ready = new int[READY_CAPACITY];
        int listenFd = listener.getFd();
        while(running) {
            int n;
            try {
                // A timeout rather than an infinite wait, so stop() is noticed even
                // when no connection ever arrives.
                n = reactor.await(ready, 250);
            } catch (IOException err) {
                if(running) {
                    System.err.println("reactor failed: " + err);
                }
                return;
            }
            if(n > 0) {
                trace("ready=" + n);
            }
            for(int iter = 0 ; iter < n ; iter++) {
                int fd = ready[iter];
                if(fd == listenFd) {
                    acceptAll();
                } else {
                    handOff(fd);
                }
            }
            sweepIdlePooledConnections();
        }
    }

    /**
     * Closes parked pooled connections whose idle deadline has passed.
     *
     * On the reactor thread, which is the only one that parks them, and after the
     * ready set has been dispatched so a descriptor that just became readable is
     * never swept on the same turn. await() returns at least every 250ms, so this
     * runs often enough without a timer of its own.
     */
    private void sweepIdlePooledConnections() {
        if(virtualThreads || pooledDeadlines.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        java.util.Iterator it =
                new java.util.ArrayList(pooledDeadlines.entrySet()).iterator();
        while(it.hasNext()) {
            java.util.Map.Entry entry = (java.util.Map.Entry)it.next();
            if(((Long)entry.getValue()).longValue() > now) {
                continue;
            }
            int fd = ((Integer)entry.getKey()).intValue();
            pooledDeadlines.remove(entry.getKey());
            trace("idle deadline reached, dropping fd=" + fd);
            drop(fd);
        }
    }

    /**
     * The server a virtual thread belongs to.
     *
     * A virtual thread's body is a C function and cannot carry a Java receiver,
     * so it arrives at serveVirtual with a descriptor and nothing else. One
     * server per process is the shape every backend binary has.
     */
    private static volatile HttpServer ACTIVE_SERVER;

    /** Guards ACTIVE_SERVER: exactly one server per process may use virtual threads. */
    private static final java.util.concurrent.atomic.AtomicBoolean VT_SLOT_TAKEN =
            new java.util.concurrent.atomic.AtomicBoolean();

    /**
     * What a connection's virtual thread runs. Reached from native code only,
     * which is also what keeps it from being dead-code eliminated.
     *
     * Deliberately just serve(): the existing connection handling, written in
     * the blocking style, unchanged. That it now runs on a virtual thread is
     * invisible to it, which is the property that makes virtual threads worth
     * having rather than a rewrite into callbacks.
     */
    static void serveVirtual(int fd) {
        HttpServer server = ACTIVE_SERVER;
        if(server == null) {
            ServerSocket.closeFd(fd);
            return;
        }
        server.serve(fd);
    }

    /**
     * One host thread's private world: its poller, its connections, its virtual
     * threads, its run queue.
     *
     * NOTHING here is shared, and that is the point. A virtual thread runs on
     * the host that accepted its connection and on no other, for its whole life.
     *
     * WHY AFFINITY IS NOT A TUNING CHOICE. The VM keeps real state per OS
     * THREAD: the BiBOP allocator's current page (`bibopCurrent`), the pacing
     * claim (`cn1MyPacingClaim`), the mark buffer, `cn1TlsSelf`, and this
     * backend's own zero-copy read buffer. A virtual thread that parks on one
     * host and resumes on another continues against a different thread's copy of
     * all of it. The first version had no affinity and crashed as soon as the
     * collector's backpressure started parking virtual threads mid-allocation.
     *
     * Auditing each of those for migration-safety would be a list that grows
     * every time somebody adds a __thread; pinning the virtual thread to its
     * host makes every one of them correct by construction, including the ones
     * nobody has written yet.
     */
    private static final class VtHost {
        final Reactor poller;

        /**
         * Virtual threads ready to run, as a preallocated ring of raw handles.
         *
         * NOT a LinkedList of boxed Longs, and this is the single most important
         * line in the scheduler. That version allocated twice per yield -- the
         * box and the list node -- ON THE HOST THREAD, and a host thread has no
         * virtual thread to hand back when the collector's backpressure stops it:
         * it just sleeps. Caught with a debugger, the accepting host was sitting
         * in cn1PacingPark underneath LinkedList.addLast underneath advance(),
         * which is this queue. Nothing was accepted after that, and the failure
         * amplifies itself -- the more the collector is behind, the more the
         * scheduler allocates trying to cope.
         *
         * A scheduler's hot path must not allocate, or it becomes a customer of
         * the very backpressure it is supposed to be relieving.
         */
        long[] ring = new long[256];
        int ringHead = 0;
        int ringCount = 0;

        boolean ringEmpty() {
            return ringCount == 0;
        }

        void ringAdd(long handle) {
            if(ringCount == ring.length) {
                // Growth allocates, which is why the ring starts big enough that
                // it does not happen in steady state: it is bounded by how many
                // virtual threads can be mid-yield at once on ONE host.
                long[] grown = new long[ring.length * 2];
                for(int iter = 0 ; iter < ringCount ; iter++) {
                    grown[iter] = ring[(ringHead + iter) % ring.length];
                }
                ring = grown;
                ringHead = 0;
            }
            ring[(ringHead + ringCount) % ring.length] = handle;
            ringCount++;
        }

        /**
         * Head first, and NOT the FILO order fasthttp uses.
         *
         * fasthttp hands a new connection to its most recently released worker --
         * "such a scheme keeps CPU caches hot" -- and the same idea looks like it
         * should apply here. It does not, because it is not the same queue.
         * fasthttp is choosing among IDLE, INTERCHANGEABLE workers, where nothing
         * can starve: whichever it picks, every connection still has a worker.
         * This queue holds PENDING RUNNABLE CONTEXTS, and the order decides
         * whether a connection runs at all -- work keeps arriving at the tail, so
         * taking from the tail lets the head sit.
         *
         * Measured rather than reasoned, tail-first against head-first, two reps
         * at each of 64 and 256 connections:
         *
         *     64  conns   head-first 277152/176814 rps, 1.48/1.39 cores
         *                 tail-first  92603/ 73980 rps, 0.56/0.55 cores
         *     256 conns   head-first 274451/270569 rps, 1.52/1.51 cores
         *                 tail-first 131821/120500 rps, 0.81/0.74 cores
         *
         * Tail-first costs two thirds of the throughput and half the machine,
         * four readings out of four. Its p99 looks better only because it is
         * serving a third of the traffic. Do not re-import this from fasthttp
         * without re-reading which queue it applies to.
         */
        long ringTake() {
            if(ringCount == 0) {
                return 0;
            }
            long handle = ring[ringHead];
            ringHead = (ringHead + 1) % ring.length;
            ringCount--;
            return handle;
        }
        /** Descriptor to virtual-thread handle. Only this host touches it. */
        long[] vtByFd = new long[1024];

        /**
         * Whether each descriptor is currently registered with this host's poller.
         *
         * Without ONESHOT the kernel no longer disarms on delivery, so this is the
         * only record of it. Only the owning host reads or writes it, which is the
         * same single-writer rule the tables beside it follow.
         */
        boolean[] armedByFd = new boolean[1024];

        /**
         * When each parked connection stops being worth waiting for, or 0.
         *
         * A parked virtual thread is resumed only when its descriptor becomes
         * readable, so a client that connects, sends half a request and then goes
         * quiet is never resumed and never shed: the connection lives for ever
         * and holds a virtual thread and its stacks. That is slowloris, and
         * BackendHttpIntegrationTest.shedsIdleConnections tests for it. The
         * dispatching path inherits the behaviour from the socket deadline; this
         * path has to enforce it, because nothing else will.
         */
        long[] deadlineByFd = new long[1024];

        /** When this host last swept, so a busy one still sheds stale work. */
        long lastSweep;

        VtHost(Reactor poller) {
            this.poller = poller;
        }

        long handleFor(int fd) {
            return fd < vtByFd.length ? vtByFd[fd] : 0;
        }

        /**
         * Makes room for this descriptor in all three tables.
         *
         * Every writer calls it, not just setHandle. The tables start at 1024
         * and a process serving the advertised connection ceiling opens numbers
         * far past that, so a write that only bounds-CHECKED was a write that
         * silently did nothing: an accepted connection above 1024 recorded no
         * deadline, and a client that then sent nothing was never swept, because
         * the growth happened in setHandle and setHandle only runs once the
         * connection has spoken. Silence was the one case it had to cover.
         */
        void ensureCapacity(int fd) {
            if(fd < vtByFd.length) {
                return;
            }
            int size = vtByFd.length;
            while(size <= fd) {
                size = size * 2;
            }
            long[] grown = new long[size];
            System.arraycopy(vtByFd, 0, grown, 0, vtByFd.length);
            vtByFd = grown;
            long[] grownDeadlines = new long[size];
            System.arraycopy(deadlineByFd, 0, grownDeadlines, 0, deadlineByFd.length);
            deadlineByFd = grownDeadlines;
            boolean[] grownArmed = new boolean[size];
            System.arraycopy(armedByFd, 0, grownArmed, 0, armedByFd.length);
            armedByFd = grownArmed;
        }

        void setHandle(int fd, long handle) {
            ensureCapacity(fd);
            vtByFd[fd] = handle;
            if(handle == 0) {
                deadlineByFd[fd] = 0;
                // The descriptor is being closed, and close() takes it out of the
                // epoll set on its own. Clearing here keeps the flag from claiming
                // a registration that the next connection to reuse this number
                // would not have.
                armedByFd[fd] = false;
            }
        }

        void setDeadline(int fd, long at) {
            if(fd < 0) {
                return;
            }
            ensureCapacity(fd);
            deadlineByFd[fd] = at;
        }

        boolean isArmed(int fd) {
            return fd >= 0 && fd < armedByFd.length && armedByFd[fd];
        }

        void setArmed(int fd, boolean armed) {
            if(fd >= 0) {
                ensureCapacity(fd);
                armedByFd[fd] = armed;
            }
        }
    }

    private VtHost[] vtHosts;

    /**
     * Which host owns each descriptor, so a connection can be re-armed on the
     * poller it actually lives in.
     *
     * Written only by the accept loop, which runs on host 0 alone, and read only
     * by the owning host -- which cannot learn the descriptor exists until the
     * registration syscall below has already happened, so the write is published
     * before any reader can reach it.
     */
    private int[] vtOwnerByFd = new int[1024];

    /** Round-robin cursor for handing new connections out. Accept thread only. */
    private int nextVtHost;

    private void setVtOwner(int fd, int host) {
        if(fd >= vtOwnerByFd.length) {
            int size = vtOwnerByFd.length;
            while(size <= fd) {
                size = size * 2;
            }
            int[] grown = new int[size];
            System.arraycopy(vtOwnerByFd, 0, grown, 0, vtOwnerByFd.length);
            vtOwnerByFd = grown;
        }
        vtOwnerByFd[fd] = host;
    }

    private VtHost ownerOf(int fd) {
        int index = fd < vtOwnerByFd.length ? vtOwnerByFd[fd] : 0;
        if(index < 0 || index >= vtHosts.length) {
            index = 0;
        }
        return vtHosts[index];
    }
    /** Round robin over the hosts, used only by whoever is accepting. */
    private int vtNextHost = 0;

    /**
     * One host thread: run whoever is ready, then poll for more.
     *
     * Everything it touches belongs to it. The run queue is a plain LinkedList
     * because no other thread can reach it, and the descriptor table is a plain
     * long[] for the same reason -- affinity is what buys that, and it is worth
     * more than the lock it saves, because it is also what makes the VM's
     * per-thread allocator state correct under a parked virtual thread.
     */
    private void runVirtualThreadHost(int index) {
        VtHost me = vtHosts[index];
        int[] ready = new int[READY_CAPACITY];
        int listenFd = listener.getFd();
        boolean owner = (index == 0);       // only one host accepts
        while(running) {
            // Runnable virtual threads first: they wait for a turn, not for the
            // network, so polling before running them would delay them by the
            // whole poll timeout.
            boolean ranSome = drainRunnable(me);
            int n;
            try {
                n = me.poller.await(ready, (ranSome || !me.ringEmpty()) ? 0 : 250);
                // On ELAPSED TIME, not on an idle poll. Sweeping only when a poll
                // came back empty meant a host that always had at least one event
                // never swept at all -- and a client can keep that true with a
                // trickle of traffic while its other connections sit silent, so
                // the deadline that exists to shed them never runs and they
                // accumulate to the process ceiling. Busy is exactly when the
                // sweep matters.
                long now = System.currentTimeMillis();
                if(n == 0 || now - me.lastSweep >= SWEEP_INTERVAL_MILLIS) {
                    me.lastSweep = now;
                    sweepDeadlines(me);
                }
            } catch (IOException err) {
                if(running) {
                    System.err.println("poller failed: " + err);
                }
                return;
            }
            for(int iter = 0 ; iter < n ; iter++) {
                int fd = ready[iter];
                if(owner && fd == listenFd) {
                    acceptAll();
                    try {
                        me.poller.modify(listenFd, CONN_EVENTS);
                    } catch (IOException err) {
                        if(running) {
                            System.err.println("could not re-arm the listener: " + err);
                        }
                        return;
                    }
                    continue;
                }
                advance(me, fd, me.handleFor(fd));
            }
        }
    }

    /**
     * Run the virtual threads that are ready. True if any were.
     */
    private boolean drainRunnable(VtHost me) {
        boolean any = false;
        int budget = me.ringCount;          // one pass, so a busy one cannot starve the poller
        while(budget-- > 0 && !me.ringEmpty()) {
            long handle = me.ringTake();
            any = true;
            advance(me, VirtualThread.descriptorOf(handle), handle);
        }
        return any;
    }

    /**
     * Give a connection's virtual thread its turn, creating it on first sight,
     * and do whatever its answer asks for.
     *
     * The three answers are the whole scheduler. FINISHED means the connection is
     * over. PARKED_IO means it wants bytes, so the descriptor goes back to this
     * host's poller. RUNNABLE means it gave up its turn but is ready now -- it is
     * waiting on the collector, not on its socket -- and handing that one to the
     * poller would wait for a client that is itself waiting for the response this
     * virtual thread still owes it.
     */
    private void advance(VtHost me, int fd, long handle) {
        if(fd < 0) {
            return;
        }
        if(handle == 0) {
            handle = VirtualThread.create(fd, VT_STACK_BYTES);
            if(handle == 0) {
                // No stack. Serving it on this thread is not an option here: the
                // keep-alive wait is indefinite because it expects to park, so
                // this host would never poll again. Refuse instead, and say so
                // once -- a server quietly dropping to zero is far worse.
                if(vtCreateFailures.incrementAndGet() == 1) {
                    System.err.println("virtual thread creation failed; "
                            + "refusing connections rather than pinning a host");
                }
                drop(fd);
                return;
            }
            me.setHandle(fd, handle);
        }
        me.setDeadline(fd, 0);      // it is running, so it is not idle
        int state = VirtualThread.resume(handle);
        if(state == VirtualThread.FINISHED) {
            me.setHandle(fd, 0);
            VirtualThread.free(handle);
            return;
        }
        if(state == VirtualThread.RUNNABLE) {
            // Take it out of the poller for as long as it sits in the ring. It is
            // neither running nor parked, so a readable descriptor would otherwise
            // be reported and resumed here while the ring is about to resume it
            // too -- and the second resume of a handle the first one finished and
            // freed is a use-after-free. ONESHOT used to make this impossible by
            // disarming as it delivered.
            me.poller.remove(fd);
            me.setArmed(fd, false);
            me.ringAdd(handle);
            return;
        }
        // Parked on I/O: start its clock. Nothing else will, and without it a
        // half-sent request parks a virtual thread for ever.
        me.setDeadline(fd, System.currentTimeMillis() + SOCKET_TIMEOUT_MILLIS);
        try {
            // Normally already armed and this is no syscall at all, which is the
            // point: a keep-alive connection is registered once at accept and
            // parks for every later request without touching epoll again. Only a
            // descriptor the RUNNABLE path disarmed has to come back, and it comes
            // back as an ADD because remove() really deregistered it.
            if(!me.isArmed(fd)) {
                me.poller.add(fd, CONN_EVENTS);
                me.setArmed(fd, true);
            }
        } catch (IOException err) {
            me.setHandle(fd, 0);
            VirtualThread.free(handle);
            drop(fd);
        }
    }

    /**
     * Close connections whose deadline passed while they were parked.
     *
     * Swept on the poll timeout rather than per event: a connection making
     * progress is resumed by readability long before this runs, so the only
     * descriptors it ever finds are the silent ones.
     */
    private void sweepDeadlines(VtHost me) {
        long now = System.currentTimeMillis();
        for(int fd = 0 ; fd < me.deadlineByFd.length ; fd++) {
            long at = me.deadlineByFd[fd];
            if(at == 0 || at > now) {
                continue;
            }
            long handle = me.handleFor(fd);
            me.setDeadline(fd, 0);
            if(handle != 0) {
                me.setHandle(fd, 0);
                VirtualThread.free(handle);
            }
            drop(fd);
        }
    }

    /**
     * Arm a connection descriptor for its next request.
     *
     * The two modes need different calls and getting it wrong fails quietly in
     * both directions, which is why this is one place. Under ONESHOT the kernel
     * DISARMS a descriptor as it delivers it but leaves it registered, so coming
     * back is EPOLL_CTL_MOD; an EPOLL_CTL_ADD would fail with EEXIST and the
     * connection would hang for ever. The dispatching path removed the
     * descriptor before handing it over, so there it has to be an ADD.
     *
     * @param fresh true for a descriptor the poller has never seen -- one just
     *              accepted -- which is an ADD either way.
     */
    /**
     * Hand a connection to a host and keep it there.
     *
     * Every accepted descriptor used to be registered with `reactor`, which IS
     * host 0's poller, so every connection lived on host 0 and the other hosts
     * polled empty sets for the life of the process. Virtual-thread mode was
     * therefore single threaded: measured 0.75 of two pinned cores against the
     * pool's 1.61 and Go's 1.49, while costing the LEAST cpu per request of the
     * three (5.64 us against 9.78 and 6.87). It was not slower, it was narrower.
     *
     * Affinity is enforced by the poller -- a descriptor registered in one host's
     * set is only ever reported to that host -- so choosing the set at accept
     * time is what assigns the owner, and the virtual thread is then created by
     * whichever host first sees it. The accept loop never touches another host's
     * descriptor table, so that table stays single-writer.
     */
    private void armConnection(int fd, boolean fresh) throws IOException {
        if(!virtualThreads) {
            pooledDeadlines.put(new Integer(fd),
                    new Long(System.currentTimeMillis() + SOCKET_TIMEOUT_MILLIS));
            reactor.add(fd, CONN_EVENTS);
            return;
        }
        if(fresh) {
            int host = nextVtHost;
            nextVtHost = host + 1 >= vtHosts.length ? 0 : host + 1;
            setVtOwner(fd, host);
            vtHosts[host].poller.add(fd, CONN_EVENTS);
            vtHosts[host].setArmed(fd, true);
            // Its clock starts NOW, not when it first parks. A connection that sends
            // nothing never becomes readable, so advance() never runs for it and the
            // deadline it would have set never exists -- and SO_RCVTIMEO does not
            // close a socket that is only sitting in a poller. Without this, opening
            // connections and saying nothing fills MAX_CONNECTIONS and the server
            // starts refusing real ones.
            vtHosts[host].setDeadline(fd, System.currentTimeMillis() + SOCKET_TIMEOUT_MILLIS);
            return;
        }
        // Re-arm has to name the SAME poller: an epoll set that does not hold
        // this descriptor answers a modify with ENOENT, and before connections
        // were distributed this was reached through `reactor` and happened to be
        // right for every fd.
        ownerOf(fd).poller.modify(fd, CONN_EVENTS);
    }

    private void acceptAll() {
        while(running) {
            int fd = listener.accept();
            if(fd < 0) {
                return; // drained
            }
            trace("accepted fd=" + fd);
            if(MAX_CONNECTIONS > 0 && openConnections.get() >= MAX_CONNECTIONS) {
                // Accept-and-close rather than stop accepting: leaving it in the
                // backlog looks to the client like a server that hangs.
                trace("at the connection ceiling, refusing fd=" + fd);
                connectionsRefused.incrementAndGet();
                ServerSocket.closeFd(fd);
                continue;
            }
            try {
                ServerSocket.setBlocking(fd, false);
                ServerSocket.setTimeout(fd, SOCKET_TIMEOUT_MILLIS);
            } catch (IOException err) {
                // CLOSED HERE, not through drop(). Nothing has been registered for
                // this descriptor yet, and drop() declines to close what it does not
                // own -- so routing this failure there returned without closing and
                // leaked a descriptor per accept, which is one per connection for as
                // long as the option keeps failing. That is exactly the shape a bad
                // CN1_HTTP_TIMEOUT_MS used to produce before the clamp above, and it
                // stays reachable for any other setsockopt failure.
                trace("fd=" + fd + " rejected at accept: " + err);
                ServerSocket.closeFd(fd);
                continue;
            }
            try {
                // Registered BEFORE the poller can report it. Arming first would let
                // another host thread reach drop() for a descriptor this map has not
                // heard of yet, and drop declines to close what it does not own.
                liveConnections.put(new Integer(fd), Boolean.TRUE);
                openConnections.incrementAndGet();
                armConnection(fd, true);
                vtAccepts.incrementAndGet();
                connectionsAccepted.incrementAndGet();
            } catch (IOException err) {
                // Through drop() so the count it just incremented comes back down.
                drop(fd);
            }
        }
    }

    /**
     * Takes the descriptor away from the reactor and gives it to a worker. It has
     * to leave the poller BEFORE the worker starts reading: this is level
     * triggered, so an fd left registered is reported ready again on the next turn
     * and two workers end up on one connection.
     */
    /**
     * How many ready descriptors one wake may carry.
     *
     * The dispatching path costs a task object and a WAKE per descriptor, and a
     * wake is a futex -- a kernel operation whose cost is the same whichever
     * language issues it. Measured against Go under the same load this server
     * does 6.9x the futex traffic per request while doing the same number of
     * reads and writes, so the coordination is the gap rather than the work.
     *
     * A poller turn that finds N ready descriptors does not need N wakes: one
     * worker can be handed the batch and walk it. That divides the dominant cost
     * by the batch size, and batches are BIGGEST under exactly the load where
     * this server is furthest behind.
     *
     * 1 restores the old behaviour exactly, which is what makes the comparison
     * an A/B rather than a rewrite.
     */
    private static final int HANDOFF_BATCH = envInt("CN1_HTTP_HANDOFF_BATCH", 1);

    /**
     * Give a whole batch of ready descriptors to ONE worker, in one wake.
     *
     * Every descriptor still leaves the poller before any of them is read, for
     * the same reason the single handoff does it: level-triggered, an fd left
     * registered is reported ready again on the next turn and a second worker
     * lands on a connection this batch already owns.
     */
    private void handOffBatch(final int[] fds, final int count) {
        for(int iter = 0 ; iter < count ; iter++) {
            reactor.remove(fds[iter]);
            pooledDeadlines.remove(new Integer(fds[iter]));
        }
        pendingWork.addAndGet(count);
        try {
            workers.execute(new Runnable() {
                public void run() {
                    for(int iter = 0 ; iter < count ; iter++) {
                        pendingWork.decrementAndGet();
                        serve(fds[iter]);
                    }
                }
            });
        } catch (RuntimeException err) {
            for(int iter = 0 ; iter < count ; iter++) {
                pendingWork.decrementAndGet();
                drop(fds[iter]);
            }
        }
    }

    private void handOff(final int fd) {
        // It is about to be served, so the idle deadline no longer applies; the
        // request deadlines take over from here.
        pooledDeadlines.remove(new Integer(fd));
        trace("handOff fd=" + fd);
        reactor.remove(fd);
        pendingWork.incrementAndGet();
        try {
            workers.execute(new Runnable() {
                public void run() {
                    pendingWork.decrementAndGet();
                    serve(fd);
                }
            });
        } catch (RuntimeException err) {
            pendingWork.decrementAndGet();
            // The pool rejected it (shutting down). Closing is the honest answer;
            // holding the connection open would promise service that is not coming.
            drop(fd);
        }
    }

    /**
     * The only place a served connection is closed, so the count stays honest.
     *
     * Idempotent, and it has to be: the stop() deadline closes what is still open
     * while a worker may be using that same connection, and that worker calls here
     * again on its next failed read. Closing twice decrements the count a second
     * time and hands close() a descriptor number the OS may already have reused for
     * something else, so the second call would shut down unrelated I/O. Winning the
     * removal is what decides which call owns the teardown.
     */
    private void drop(int fd) {
        if(liveConnections.remove(new Integer(fd)) == null) {
            return;
        }
        // Before anything else: a descriptor number is reused as soon as it is
        // closed, so an entry left behind here would time out the NEXT connection
        // to be handed that number.
        pooledDeadlines.remove(new Integer(fd));
        Object h2 = http2Sessions.remove(new Integer(fd));
        if(h2 != null) {
            ((Http2)h2).close();
        }
        Object session = sessions.remove(new Integer(fd));
        if(session != null) {
            Tls.closeSession(((Long)session).longValue());
        }
        ServerSocket.closeFd(fd);
        openConnections.decrementAndGet();
    }

    /** 0 when this connection is plaintext. */
    private long sessionOf(int fd) {
        Object session = sessions.get(new Integer(fd));
        return session == null ? 0 : ((Long)session).longValue();
    }

    private static int readFrom(int fd, long session, byte[] buffer, int offset, int length)
            throws IOException {
        return session == 0 ? ServerSocket.read(fd, buffer, offset, length)
                            : Tls.read(session, buffer, offset, length);
    }

    private static void writeTo(int fd, long session, byte[] buffer, int offset, int length)
            throws IOException {
        if(session == 0) {
            ServerSocket.write(fd, buffer, offset, length);
        } else {
            Tls.write(session, buffer, offset, length);
        }
    }

    private void serve(int fd) {
        trace("serve fd=" + fd);
        activeRequests.incrementAndGet();
        try {
            serveOne(fd);
        } finally {
            activeRequests.decrementAndGet();
        }
    }

    /** A malformed request that deserves a specific status before the close. */
    private static final class ProtocolException extends IOException {
        final int status;

        ProtocolException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    /**
     * A connection plus whatever has been read from it and not yet consumed.
     *
     * The leftover is the point. A client may send a second request before reading
     * the reply to the first, and both arrive in one read; a parser that keeps only
     * the request it wanted silently drops the rest. That is not an exotic case --
     * it is what pipelining is, and what a proxy does when it coalesces.
     */
    private final class Conn {
        final int fd;
        final long session;
        byte[] buffer = new byte[0];
        int pos;
        /** True while `buffer` is the thread's shared buffer rather than ours. */
        boolean borrowed;
        /**
         * True once this request's header slices name positions in `buffer`.
         *
         * "Is anything still pointing at this buffer" is the question fill() has to
         * answer before it lets go of a borrow, and "is there anything left to
         * read" is NOT the same question -- see the comment there.
         */
        boolean parsedFromBuffer;

        /** Memoised request targets for this connection. See internTarget. */
        private final String[] targetCache = new String[TARGET_CACHE_SLOTS];

        String internTarget(byte[] data, int start, int length) {
            // NOTHING LONG GOES IN. The cache exists for a route asked for over and
            // over on one connection -- "/plaintext", "/json", "/api/notes/42" --
            // and every one of those is short. A target may be nearly
            // MAX_HEADER_BYTES though, and with one slot per hash a client sending
            // 64 distinct near-limit query strings fills all of them and the
            // connection then holds megabytes for as long as it stays open. That
            // needs no body at all, which is what makes it worse than the upload
            // retention releaseIdleMemory deals with: it is reached by a keep-alive
            // client that only ever sends request lines.
            //
            // Capped rather than cleared when idle: clearing would throw the
            // memoisation away on every keep-alive request, which is the case it
            // was measured to help.
            if(targetCache.length == 0 || length > MAX_CACHED_TARGET_BYTES) {
                // Cache disabled (CN1_HTTP_TARGET_CACHE=0), for A/B measurement.
                // Guarded because the slot arithmetic below is a modulo, and a zero
                // size would divide by it rather than politely doing nothing.
                return asciiString(data, start, length);
            }
            int hash = 0;
            for(int iter = 0 ; iter < length ; iter++) {
                hash = hash * 31 + data[start + iter];
            }
            int slot = (hash & 0x7fffffff) % TARGET_CACHE_SLOTS;
            String cached = targetCache[slot];
            if(cached != null && cached.length() == length) {
                int iter = 0;
                while(iter < length
                        && cached.charAt(iter) == (char)(data[start + iter] & 0xff)) {
                    iter++;
                }
                if(iter == length) {
                    return cached;
                }
            }
            String fresh = asciiString(data, start, length);
            // One entry per slot, overwritten on collision rather than chained:
            // two hot targets that collide would otherwise both miss forever, and
            // overwriting lets whichever is currently hot keep the slot.
            targetCache[slot] = fresh;
            return fresh;
        }
        /**
         * Set when a read returned end-of-stream. The linger above has to tell a
         * client that WENT AWAY from one that has merely gone quiet: the first
         * must be closed, and handing the second to the poller is the whole point.
         */
        boolean closedByPeer;

        /**
         * Where a response is assembled, reused for the life of the connection.
         *
         * The head used to be built with a StringBuilder, turned into a String and
         * then encoded to bytes, and the body copied in after that -- four
         * allocations per response, and the StringBuilder reallocating its char[]
         * as it grew. An allocation census put char[] at 47% of ALL allocation in
         * this server, and this path was most of it. Bytes go in directly now:
         * the header field names are ASCII constants, and a status or a length is
         * digits.
         */
        byte[] out = new byte[1024];
        int outLength;
        /**
         * Reused header slices: nameStart, nameLength, valueStart, valueLength.
         *
         * Handed to a Request BY REFERENCE, not copied -- and that is only safe
         * because `buffer` is a fresh, exactly-sized array per fill, so each
         * Request's `raw` is privately owned and immutable once parsed. The two are
         * a pair: the slices name absolute offsets into that particular array.
         *
         * Anything that makes the buffer REUSABLE breaks the pair. Measured, with a
         * capacity-plus-limit buffer in place of the per-fill array: a Request came
         * back holding `raw.length=45` while its own slices named offsets 212 and
         * 232, i.e. raw from one request and the slice table from a larger later
         * one -- the shared table had been re-parsed under a Request still using it.
         * The result was an ArrayIndexOutOfBoundsException in getHeader, thrown
         * outside any try block, which killed the connection with no response
         * written (~1 suite run in 2).
         *
         * TWO WRONG ANSWERS, so that a third attempt does not re-buy them. It is
         * NOT two workers on one connection: a probe that reports a second thread
         * entering serve() for a descriptor already inside it fired ZERO times on
         * the build that fails (the same probe on the passing build proves nothing,
         * which is how it was nearly mis-read). And it is not the slice table
         * overflowing: slices.length was 64 against a headerCount of 3.
         *
         * THE ACTUAL CAUSE, and it is a property of the VM rather than of this
         * class: a zero-copy buffer's LENGTH IS NOT STABLE. readIntoThreadBufferImpl
         * hands back the same array object every call and mutates it in place --
         * `a->length = (int)n` -- because a ParparVM array's length is a field in a
         * struct the runtime owns. So the array reports ~100 bytes while its headers
         * are parsed (slices at 39, 59, 69) and reports 40 after the same thread's
         * next read, with the already-parsed slices left naming positions past the
         * end. That is the 40-against-69 reading, and nothing moved: the length did.
         *
         * `available()` is written as `buffer.length - pos` for exactly this reason.
         * It re-reads the length every time and therefore self-corrects. Caching it
         * in a `limit` field -- which is what a reusable buffer needs -- is what
         * breaks, and it breaks silently, as a truncated response rather than a
         * wrong one.
         *
         * So a reusable buffer has to stop borrowing first: take a private array
         * (whose length really is immutable) before anything caches a length or
         * parses slices out of it. Sizing that copy is itself subject to the same
         * trap, since buffer.length must be read before the next read mutates it.
         *
         * THAT WAS BUILT, AND IT IS NOT WORTH IT. With the length handled correctly
         * the reusable buffer is correct -- 4 default plus 2 virtual-thread suite
         * runs clean, against a naive version that failed within two -- and it buys
         * NOTHING. Measured in virtual-thread mode, same 12s window and load:
         *
         *   cycles per window   reuse 40, 40, 38     no reuse 41, 42, 41
         *   requests            2.69M, 2.63M, 2.56M  3.08M, 3.01M, 2.70M
         *
         * The collection RATE does not move, so this array is not a meaningful part
         * of the ~340 bytes a request allocates, and the throughput came out lower
         * in all three reps (arms were not interleaved, so treat that half loosely).
         * The per-request read buffer is simply not where the allocation is: look
         * for the bytes before removing an allocation on the assumption it matters.
         *
         * So removing the per-request byte[] is not just a capacity field: a Request
         * has to own a consistent (raw, slices, headerCount) triple, re-based
         * together or not at all.
         */
        int[] slices = new int[64];
        /**
         * Where a deferred JSON body is serialised, so its length is known before
         * the head that must declare it is written. Reused like everything else
         * here; the copy into the head buffer afterwards is a memcpy of a body
         * small enough to share a packet with its headers.
         */
        // Not final: an oversized one is REPLACED rather than carried, see
        // releaseIdleMemory. reset() only rewinds the length, which is the right
        // thing per request and the wrong thing across an idle wait.
        ByteSink bodySink = new ByteSink(512);

        void reset() {
            outLength = 0;
        }

        void ensure(int extra) {
            if(outLength + extra <= out.length) {
                return;
            }
            int size = out.length * 2;
            while(size < outLength + extra) {
                size *= 2;
            }
            byte[] grown = new byte[size];
            System.arraycopy(out, 0, grown, 0, outLength);
            out = grown;
        }

        /** ASCII only. Every caller passes a header name or a constant. */
        void put(String ascii) {
            int n = ascii.length();
            ensure(n);
            for(int iter = 0 ; iter < n ; iter++) {
                out[outLength++] = (byte)ascii.charAt(iter);
            }
        }

        /**
         * The content type, encoded once per connection rather than per response.
         *
         * put(String) walks charAt by charAt, and a handler hands back the same
         * String instance every time -- a literal, or a constant on Response --
         * so after the first response the bytes are already there. Identity, not
         * equals: a handler that builds a fresh String per response simply keeps
         * missing and pays what it paid before, and the cache is filled ONCE so
         * that case cannot allocate per request either.
         */
        private String ctKey;
        private byte[] ctBytes;

        void putContentType(String ct) {
            if(ct == ctKey) {
                System.arraycopy(ctBytes, 0, out, ensureAt(ctBytes.length), ctBytes.length);
                outLength += ctBytes.length;
                return;
            }
            put(ct);
            if(ctKey == null && ct != null) {
                ctKey = ct;
                ctBytes = asciiBytes(ct);
            }
        }

        /** Reserves {@code n} bytes and answers the offset they start at. */
        private int ensureAt(int n) {
            ensure(n);
            return outLength;
        }

        void put(byte[] data, int offset, int length) {
            ensure(length);
            System.arraycopy(data, offset, out, outLength, length);
            outLength += length;
        }

        void put(int b) {
            ensure(1);
            out[outLength++] = (byte)b;
        }

        /**
         * A non-negative number as ASCII digits, written in place.
         * Integer.toString would allocate a String and its char[] -- per response,
         * twice (the status and the content length).
         */
        void putNumber(long value) {
            if(value < 0) {
                put("-");
                value = -value;
            }
            if(value == 0) {
                put('0');
                return;
            }
            int start = outLength;
            // Tried and REVERTED: an int fast path that sized the number with a
            // ternary chain instead of dividing to count digits. It looked like a
            // clear win on paper -- this was 2.1% of on-CPU time and a 64-bit
            // divide is tens of cycles -- but measured 6 of 8 interleaved reps
            // SLOWER, median about -3.7%. The likely reason is that a ten-way
            // ternary compiles to branchy operand-stack code here, which costs more
            // than the divisions it removes. Do not re-attempt without measuring on
            // an idle machine; the reps above were taken at load 17 and are weak
            // evidence, but there was no sign of a gain in any of them.
            long v = value;
            int digits = 0;
            while(v > 0) {
                digits++;
                v /= 10;
            }
            ensure(digits);
            outLength += digits;
            int at = outLength;
            while(value > 0) {
                out[--at] = (byte)('0' + (int)(value % 10));
                value /= 10;
            }
            if(at != start) {
                // Unreachable unless the digit count and the loop disagree; the
                // buffer would be left with a hole rather than a short write.
                throw new IllegalStateException("digit count mismatch");
            }
        }

        Conn(int fd, long session) {
            this.fd = fd;
            this.session = session;
        }

        int available() {
            return buffer.length - pos;
        }

        /**
         * The one Response handed to Request.respond on this connection. Null until
         * a handler asks for it, so a handler that never does pays nothing.
         */
        Response pooledResponse;

        /**
         * Requests answered on this connection since the last fold into the
         * server's striped counter. Plain: one virtual thread owns a connection
         * for its whole life, so this field has a single writer.
         */
        long servedPending;

        /** Index into servedStripes for the host that owns this connection, or -1. */
        int stripe = -1;

        /**
         * The one Request served on this connection, re-pointed per request rather
         * than reallocated. Response and Request were the whole of what /plaintext
         * still allocated once the borrowed-buffer copy went: 88 and 80 bytes, one
         * of each, every request.
         */
        Request pooledRequest;

        /** Reads more. False at end of stream. */
        boolean fill(byte[] scratch) throws IOException {
            if(borrowed && available() == 0 && !parsedFromBuffer) {
                // Nothing is left unread AND nothing has been parsed out of this
                // buffer, so this is the START of a new request on a kept-alive
                // connection and there is nothing to preserve: the previous request
                // was answered before the loop came back here. Just let go.
                //
                // The parsedFromBuffer half is load-bearing and was missing. An
                // empty buffer does NOT mean no request is in flight: a POST whose
                // headers arrive in one segment and whose body arrives in the next
                // reaches the body loop with the header block fully consumed, so
                // available() is 0 while the Request's slices still name positions
                // in this very buffer. Taking this branch there dropped the borrow,
                // skipped detachPreservingOffsets below, and let the next zero-copy
                // read overwrite the headers with the body -- after which
                // getHeader("connection") walked off the end of the array and the
                // AIOOBE killed the connection with no response written at all.
                //
                // That is a truncated reply, not a wrong one, so it showed up only
                // as a client-side timeout: transactionRollsBack failing 15.05s
                // (its own setSoTimeout) with status -1, and authGuardsMutatingRoutes
                // reading an empty body, about 2 full-suite runs in 6. It never
                // appeared under virtual threads because ZERO_COPY_READ is off
                // there, which is also why it survived the whole reactor rewrite.
                //
                // Copying here instead is what a first version did, and it put the
                // per-request byte[] straight back -- the linger means a worker
                // loops without handing the descriptor back, so `borrowed` was still
                // set on every subsequent request and each one copied the whole
                // buffer. The census said 223 bytes per request where it should have
                // said none, which is the only reason it was noticed.
                buffer = EMPTY_BODY;
                pos = 0;
                borrowed = false;
            } else if(borrowed) {
                // A second read WITHIN one request is about to overwrite the
                // thread buffer, and a Request parsed out of it holds SLICES into
                // exactly that memory. Copy first, preserving absolute offsets so
                // those slices stay valid.
                //
                // Found by BackendHttpIntegrationTest.transactionRollsBack, which
                // sends a body big enough to need two reads along with an
                // Authorization header: the second read landed on top of the header
                // and the request came back 401 instead of 400. A corrupted header
                // is the good version of this bug -- the same overwrite could just
                // as easily have served one request's bytes inside another's.
                detachPreservingOffsets();
            }
            if(ZERO_COPY_READ && available() == 0 && session == 0) {
                // The common case by far: nothing left over, so the bytes are read
                // into this thread's reusable buffer and parsed where they land --
                // no array allocated, nothing copied. The request is parsed out of
                // the same memory the kernel wrote into.
                //
                // The returned array's length is exactly what was read, so every
                // parser below that scans to buffer.length keeps working untouched.
                // That is only possible because the length of a ParparVM array is a
                // field in a struct we own; introducing a separate limit instead
                // would have meant auditing fifteen call sites, and one missed site
                // reads a previous request's bytes into this one's response.
                //
                // Plaintext only (session == 0): a TLS read decrypts through its
                // own path and does not hand back a buffer we own.
                byte[] direct = ServerSocket.readIntoThreadBuffer(fd, scratch.length);
                if(direct == null) {
                    closedByPeer = true;
                    return false;
                }
                if(direct.length == 0) {
                    // Nothing ready on a non-blocking descriptor, which means two
                    // different things and only one of them is trouble.
                    //
                    // MIDWAY THROUGH a message it is not the peer leaving: the
                    // headers arrived in one packet and the body is still coming,
                    // and answering "closed" here dropped a valid upload. Those
                    // reads go to the copying path, which parks on EAGAIN and owns
                    // its buffer -- this one cannot park, because the storage is
                    // per HOST thread and another virtual thread's read would
                    // overwrite what this one is about to return.
                    //
                    // BETWEEN messages it is the ordinary quiet of a kept-alive
                    // connection, and reporting it as closed is how a worker is
                    // freed. Sending those to a parking read instead made the
                    // suite 2.4x slower and stopped shedsIdleConnections shedding
                    // anything -- the partial request sat until a deadline and was
                    // answered 408 rather than dropped.
                    //
                    // parsedFromBuffer is precisely that distinction, and it is
                    // already maintained for fill()'s benefit. Note a SPLIT header
                    // block needs nothing here: after the first partial read
                    // available() is non-zero, so it never takes this branch.
                    if(!parsedFromBuffer) {
                        closedByPeer = true;
                        return false;
                    }
                    return fillCopying(scratch);
                }
                if(ZERO_COPY_MODE == 2) {
                    // Diagnostic bisection only -- see ZERO_COPY_MODE. Same read as
                    // mode 1, same heap array as mode 0, so whichever of the two the
                    // throughput follows is the one that costs.
                    byte[] owned = new byte[direct.length];
                    System.arraycopy(direct, 0, owned, 0, direct.length);
                    buffer = owned;
                    pos = 0;
                    borrowed = false;
                    return true;
                }
                buffer = direct;
                pos = 0;
                borrowed = true;
                return true;
            }
            return fillCopying(scratch);
        }

        /**
         * The copying read: into this connection's own scratch, then into a buffer
         * sized for what is kept plus what arrived.
         *
         * Split out of fill() so the zero-copy path can defer to it when the
         * descriptor has nothing ready. readFrom parks on EAGAIN for a virtual
         * thread, which is the behaviour the shared-buffer read cannot safely have.
         */
        private boolean fillCopying(byte[] scratch) throws IOException {
            int n = readFrom(fd, session, scratch, 0, scratch.length);
            if(n <= 0) {
                closedByPeer = true;
                return false;
            }
            int keep = available();
            byte[] grown = new byte[keep + n];
            System.arraycopy(buffer, pos, grown, 0, keep);
            System.arraycopy(scratch, 0, grown, keep, n);
            buffer = grown;
            pos = 0;
            borrowed = false;
            return true;
        }

        /**
         * Reads until `needed` bytes are buffered, into ONE array sized for them.
         *
         * fill() grows by exactly what it just read, so a body arriving in
         * scratch-sized pieces reallocated and recopied everything once per read:
         * an 8MB upload over an 8KB buffer is about a thousand resizes and some 4GB
         * of copying before the handler is even called, which a few concurrent
         * uploads turn into the whole machine.
         *
         * When the total is known -- and for Content-Length it is -- the destination
         * can be grown toward it in doublings, which is one copy of what was already
         * buffered and an amortised one of the body.
         *
         * It is NOT allocated at `needed` up front, which is what this did first.
         * Content-Length is a client's CLAIM, and believing it before a byte of the
         * body has arrived means an unauthenticated client can make the server
         * allocate 8MB by sending a header and then nothing at all: this loop holds
         * that memory until the rate allowance below expires, and the connection
         * ceiling is in the thousands, so a few dozen such requests are gigabytes.
         * Growing as the bytes ARRIVE makes the memory track what was actually sent,
         * which is the only figure a client cannot lie about. The doubling is what
         * keeps that affordable -- growing by each read's size instead was the
         * original defect here, about a thousand resizes and 4GB of copying for one
         * 8MB upload.
         *
         * The invariant the rest of this class depends on is kept, because every
         * growth is capped at `needed`: the last one allocates exactly that, so the
         * array handed over is exactly `needed` long with every byte valid, and
         * `buffer.length` still means "bytes readable". See the class comment for
         * why a `limit` field is not the answer here.
         */
        boolean fillTo(int needed) throws IOException {
            int keep = available();
            if(keep >= needed) {
                return true;
            }
            long charged = 0;
            try {
            // RESERVED before allocated, not after. The charge is what bounds
            // concurrent uploads, and a budget checked after the allocation
            // bounds nothing: every thread that reaches a growth boundary at the
            // same moment takes its memory first and finds out it was over the
            // limit second, so the peak is the number of threads times their
            // step, whatever the limit says. Reserving first makes the refusal
            // happen while the memory is still hypothetical. `charged` is
            // incremented in the same breath, so the finally below rolls the
            // reservation back even if the allocation itself fails.
            int first = Math.max(keep, Math.min(needed, BODY_CHUNK_BYTES));
            charged += first;
            if(http1UploadBytes.addAndGet(first) > MAX_HTTP1_UPLOAD_BYTES) {
                throw new ProtocolException(503, "too many uploads in flight");
            }
            byte[] grown = new byte[first];
            System.arraycopy(buffer, pos, grown, 0, keep);
            int at = keep;
            // A RATE, not a deadline. The head gets a flat bound because it is small;
            // a body cannot, since 8 MiB over a slow mobile link is a real client and
            // any fixed wall-clock limit refuses it. But SO_RCVTIMEO restarts on
            // every successful read, so without something here a client declaring a
            // large Content-Length and sending one byte inside each window holds its
            // worker for as long as it likes -- and in pool mode, which is what TLS
            // uses, enough of those are the whole server. The allowance is what this
            // many bytes take at the floor rate, plus one socket timeout of slack, so
            // a slow upload that keeps making progress finishes and a dribble does not.
            long started = System.currentTimeMillis();
            long allowed = SOCKET_TIMEOUT_MILLIS
                    + (long)(needed - keep) * 1000L / MIN_BODY_BYTES_PER_SECOND;
            while(at < needed) {
                if(System.currentTimeMillis() - started > allowed) {
                    throw new ProtocolException(408, "the request body did not arrive in time");
                }
                if(at == grown.length) {
                    // Doubling, capped at what was declared -- so the final growth
                    // lands exactly on `needed` and the invariant above holds.
                    int next = (int)Math.min((long)needed, (long)grown.length * 2);
                    // Reserved before allocated, for the reason above.
                    long delta$ = (long)next - grown.length;
                    charged += delta$;
                    if(http1UploadBytes.addAndGet(delta$) > MAX_HTTP1_UPLOAD_BYTES) {
                        throw new ProtocolException(503, "too many uploads in flight");
                    }
                    byte[] bigger = new byte[next];
                    System.arraycopy(grown, 0, bigger, 0, at);
                    grown = bigger;
                }
                // Exactly the shortfall, so a pipelined request behind this body stays
                // in the socket for the next parse rather than being read into it.
                int n = readFrom(fd, session, grown, at, grown.length - at);
                if(n <= 0) {
                    closedByPeer = true;
                    return false;
                }
                at += n;
            }
            buffer = grown;
            pos = 0;
            borrowed = false;
            return true;
            } finally {
                // Every path out: the body arrived, the peer went away, the
                // deadline passed, or the process was full. The charge covers
                // the READ -- on success the buffer becomes the connection's and
                // the request goes on to a handler, which is ordinary server
                // memory rather than an upload being held open.
                http1UploadBytes.addAndGet(-charged);
            }
        }

        /**
         * Give up the shared thread buffer before this connection can be taken by a
         * different worker.
         *
         * The buffer belongs to the THREAD, not the connection. Anything still
         * unread has to be copied somewhere this connection owns before the
         * descriptor goes back to the reactor: the next worker runs on another
         * thread whose buffer is different memory, and the thread that read these
         * bytes overwrites them on its next request.
         *
         * The copy happens only when bytes are actually left over -- the pipelining
         * case. The ordinary request-per-read path copies nothing.
         */
        /**
         * Take a private copy of the whole borrowed buffer, keeping every index the
         * same, so anything already parsed out of it (a Request's header slices)
         * keeps pointing at the right bytes.
         *
         * Compacting here instead would be a subtle disaster: it moves the content
         * to offset zero while the slices still name the old positions.
         */
        void detachPreservingOffsets() {
            byte[] owned = new byte[buffer.length];
            System.arraycopy(buffer, 0, owned, 0, buffer.length);
            buffer = owned;
            borrowed = false;
        }

        /**
         * Lets go of an oversized buffer this connection OWNS, and of the request
         * that pointed into it, before it waits for the next one.
         *
         * releaseBorrowed below deals with the THREAD's buffer. This is the other
         * half, and the one an upload reaches: a body bigger than what is already
         * buffered grows a private array to fit, and the connection then keeps it.
         *
         * CN1_HTTP_MAX_UPLOAD_MB bounds what is IN FLIGHT, and the charge is
         * dropped when the read completes -- correct, because by then it is this
         * connection's memory rather than an upload still arriving, but it does
         * mean the bound stops describing it. A kept-alive connection holds the
         * whole body while it waits, and under virtual threads it waits for as long
         * as the client cares to take, so connections that have each uploaded once
         * and gone quiet hold far more than the in-flight bound ever allowed and
         * nothing counts it.
         *
         * Holding the CHARGE across the wait instead would let idle connections
         * refuse other people's uploads, which trades a memory problem for a
         * liveness one. The memory is not needed: the response has been written,
         * so nothing points into the buffer any more -- the same fact that lets
         * parsedFromBuffer be cleared at the wait -- and fill() borrows the
         * thread's buffer for the next request. This is the state a connection
         * starts in.
         *
         * Nothing is dropped while bytes are still unread: a pipelined request
         * sitting in this buffer is the next request, not residue.
         */
        void releaseIdleMemory() {
            if(!borrowed && buffer.length > 0 && available() == 0) {
                buffer = EMPTY_BODY;
                pos = 0;
            }
            if(pooledRequest != null) {
                pooledRequest.releaseRetained();
            }
            // The RESPONSE side keeps peaks of its own. Both of these grow to fit
            // and never shrink, which is what makes them cheap per request and
            // expensive across an idle wait: one large JSON answer leaves the
            // connection holding it in the sink it was serialised into and again
            // in the head buffer it was copied to. Anything up to the combine
            // limit is the working size and is kept; past that it belonged to one
            // response that has already gone out.
            if(out.length > MAX_IDLE_BUFFER_BYTES) {
                out = new byte[1024];
                outLength = 0;
            }
            if(bodySink.bytes().length > MAX_IDLE_BUFFER_BYTES) {
                bodySink = new ByteSink(512);
            }
            if(pooledResponse != null) {
                pooledResponse.releaseRetained();
            }
        }

        void releaseBorrowed() {
            if(!borrowed) {
                return;
            }
            int keep = available();
            if(keep > 0) {
                byte[] owned = new byte[keep];
                System.arraycopy(buffer, pos, owned, 0, keep);
                buffer = owned;
            } else {
                buffer = EMPTY_BODY;
            }
            pos = 0;
            borrowed = false;
        }

        void write(byte[] data) throws IOException {
            writeTo(fd, session, data, 0, data.length);
        }
    }

    /**
     * Serves one connection, releasing it even if the failure is an ERROR.
     *
     * Every catch below is `catch (Exception)`, and an Error is not one: a
     * StackOverflowError out of a recursive parser, or an AssertionError from a
     * handler, walks past all of them and leaves serveOne without reaching any
     * drop(). By then the descriptor has been removed from its poller and is
     * still in liveConnections, so nothing will ever close it -- one stranded
     * socket per occurrence, and the process runs out of them. The Error itself
     * is rethrown: this releases the connection, it does not pretend the failure
     * did not happen.
     *
     * A wrapper rather than a try around the body, because the body has many
     * returns and the point is that EVERY one of them is covered.
     */
    private void serveOne(int fd) {
        try {
            serveOneRelease(fd);
        } catch (Error err) {
            drop(fd);
            throw err;
        }
    }

    private void serveOneRelease(int fd) {
        long session;
        try {
            // A POOL worker owns its descriptor and blocks on it: there is no one to
            // hand its host thread to. A virtual thread is the opposite, and blocking
            // here defeated the whole design -- readImpl reaches its park path only
            // when recv returns EAGAIN, which a blocking descriptor never does. So the
            // virtual thread never parked and the host OS thread sat in the kernel
            // until SO_RCVTIMEO. A load generator never shows this, because the bytes
            // are always already there; a client sending one byte per timeout pins a
            // host and starves every connection scheduled on it.
            //
            // TLS is the exception, and stays blocking below: Tls.readImpl maps
            // SSL_ERROR_WANT_READ to a hard error rather than parking, so a
            // non-blocking descriptor would break TLS reads outright. Giving the TLS
            // layer a park path is the real fix and is not this change.
            boolean parking = virtualThreads;
            if(!parking) {
                ServerSocket.setBlocking(fd, true);
            }
            if(tls != null && sessionOf(fd) == 0) {
                // The handshake runs here, on the worker, because the descriptor is
                // blocking here and a handshake is several round trips. On the
                // reactor thread it would stall every other connection.
                long fresh = tls.accept(fd);
                if(fresh == 0) {
                    // Not a TLS client, or no common cipher. Ordinary traffic.
                    drop(fd);
                    return;
                }
                sessions.put(new Integer(fd), new Long(fresh));
            }
            session = sessionOf(fd);
            if(tls != null && Http2.ALPN.equals(Tls.negotiatedProtocol(session))) {
                // ALPN settled on h2, so this connection is framed, not textual,
                // for its whole life. There is no downgrade from here.
                serveHttp2(fd, session, null, 0);
                return;
            }
        } catch (Exception err) {
            drop(fd);
            return;
        }

        Conn conn = new Conn(fd, session);
        // Which stripe this connection's requests count into. Resolved once here
        // rather than per request: the owner cannot change for a live descriptor.
        if(virtualThreads && fd >= 0 && fd < vtOwnerByFd.length && servedStripes.length > 0) {
            int host = vtOwnerByFd[fd];
            if(host >= 0 && host * SERVED_STRIPE_STRIDE < servedStripes.length) {
                conn.stripe = host * SERVED_STRIPE_STRIDE;
            }
        }
        byte[] scratch = new byte[8192];
        int served = 0;

        // Cleartext HTTP/2 by prior knowledge: a client that already knows the
        // server speaks h2 opens with the connection preface instead of a request
        // line. This is how gRPC talks over cleartext and how a load balancer that
        // terminated TLS talks to an origin, and it is the only way to reach h2
        // without ALPN.
        if(http2Sessions.containsKey(new Integer(fd))) {
            serveHttp2(fd, session, null, 0);
            return;
        }
        // CLEARTEXT ONLY, which is what "prior knowledge" means: over TLS the
        // protocol is ALPN's to decide and it was decided above. Sniffing the
        // preface on a TLS connection as well let a client negotiate HTTP/1.1 --
        // or offer no ALPN at all -- and then send the preface anyway, so a server
        // built with offerHttp2=false spoke h2 to whoever asked in the one way the
        // operator had turned off. RFC 9113 puts prior knowledge on the cleartext
        // side for exactly this reason.
        if(session == 0) {
            try {
                while(conn.available() < HTTP2_PREFACE.length) {
                    if(!conn.fill(scratch)) {
                        drop(fd);
                        return;
                    }
                    if(!startsWithPrefacePrefix(conn)) {
                        break; // definitely not h2; parse it as HTTP/1.1
                    }
                }
                if(conn.available() >= HTTP2_PREFACE.length && matchesPreface(conn)) {
                    byte[] rest = new byte[conn.available()];
                    System.arraycopy(conn.buffer, conn.pos, rest, 0, rest.length);
                    serveHttp2(fd, session, rest, rest.length);
                    return;
                }
            } catch (Exception err) {
                drop(fd);
                return;
            }
        }

        while(true) {
            Request request;
            try {
                request = readRequest(conn, scratch);
            } catch (ProtocolException err) {
                trace("fd=" + fd + " rejected: " + err.getMessage());
                writeStatusOnly(conn, err.status, err.getMessage());
                drop(fd);
                return;
            } catch (ServerSocket.TimeoutException err) {
                // An idle client, not a fault. Shedding it is the point of the deadline.
                trace("fd=" + fd + " timed out");
                drop(fd);
                return;
            } catch (Exception err) {
                trace("fd=" + fd + " read failed: " + err);
                drop(fd);
                return;
            }
            if(request == null) {
                drop(fd); // the peer closed
                return;
            }

            boolean keepAlive = wantsKeepAlive(request);
            // Methods are case-sensitive, so this is an exact comparison.
            boolean headOnly = "HEAD".equals(request.getMethod());
            Response response;
            // From here to the end of the write is the request being in flight. Not
            // the whole of serveOne: that is the CONNECTION, which outlives this.
            inFlightRequests.incrementAndGet();
            try {
                try {
                    response = handler.handle(request);
                    if(response == null) {
                        response = Response.text(404, "not found");
                    }
                } catch (Exception err) {
                    System.err.println("handler failed: " + err);
                    response = Response.text(500, "internal error");
                }
                try {
                    writeResponse(conn, fd, session, response, keepAlive, headOnly);
                    if(conn.stripe >= 0) {
                        servedStripes[conn.stripe]++;      // single writer: this host
                    } else {
                        requestsServed.incrementAndGet();  // reactor mode, no stripes
                    }
                } catch (Exception err) {
                    trace("fd=" + fd + " write failed: " + err);
                    drop(fd);
                    return;
                }
            } finally {
                inFlightRequests.decrementAndGet();
            }
            if(!keepAlive) {
                drop(fd);
                return;
            }
            if(conn.available() > 0) {
                continue; // a pipelined request is already in the buffer
            }
            // The burst cap is a fairness backstop for a POOL: it stops one worker
            // monopolising a shared thread. A virtual thread owns its connection
            // and parking costs nobody anything, so there is nothing to be fair
            // to -- and breaking here would be worse than pointless, because in
            // this mode the loop's exit path closes the connection. That is a
            // healthy keep-alive connection dropped every 256 requests, which the
            // client sees as a mid-stream close: 446833 write errors against
            // 164307 requests at four connections, and it made every earlier
            // virtual-thread measurement an underestimate.
            if(++served >= KEEPALIVE_BURST_LIMIT) {
                if(virtualThreads) {
                    // Step aside rather than close. A virtual thread under a load
                    // generator never runs out of bytes, so it never parks on its
                    // own and would hold this host thread for as long as the
                    // client kept talking -- with fewer hosts than connections the
                    // rest starve, measured as two hosts serving two of sixty four.
                    // Breaking here instead is worse still, because in this mode
                    // the exit path CLOSES the connection: 446833 write errors
                    // against 164307 requests, a healthy keep-alive connection
                    // dropped every 256 requests.
                    served = 0;
                    conn.releaseBorrowed();
                    VirtualThread.yieldNow();
                    continue;
                }
                break;                          // fairness backstop; see the constant
            }
            // On a virtual thread there is nothing to be fair TO: parking releases
            // the host thread immediately, so holding the connection costs no one
            // anything and handing it back would only add a poller round trip per
            // request.
            if(!virtualThreads && pendingWork.get() > 0
                    && workerCount - activeRequests.get() <= pendingWork.get()) {
                // Hand back only when something is actually waiting AND there are
                // not enough idle workers for it -- the case where holding this
                // connection denies service to another. With nothing waiting, or
                // with a spare worker for whoever is, holding costs nobody
                // anything. Testing the idle count alone is wrong in the most
                // common configuration of all: with connections == workers every
                // worker is busy and nothing is queued, so "idle <= pending" reads
                // 0 <= 0 and hands the connection back on every single request,
                // which is the behaviour the linger exists to avoid.
                //
                // Breaking on "anything is waiting at all" was the first attempt and
                // it is too blunt: with 128 workers and 64 connections every worker
                // handed its connection back on every request even though half the
                // pool was idle, and throughput did not move (123k at 16 workers,
                // 126k at 128). The pool size only buys anything if a spare worker
                // actually lets a connection stay put.
                break;
            }
            // Wait briefly for the next request rather than going round the poller
            // for it. See KEEPALIVE_LINGER_MILLIS.
            //
            // With the workers polling, the linger waits ZERO milliseconds: it
            // still asks whether the next request has already arrived, because
            // answering a pipelined request on the spot is free, but it never
            // BLOCKS waiting for one. Two reasons, and they are the reasons the
            // linger exists at all:
            //
            //  - What it buys is avoiding the handback, and in this mode the
            //    handback is one epoll_ctl with no wake and no queue. There is
            //    almost nothing left to avoid.
            //  - What it costs is much higher here. A lingering worker is not
            //    polling, so with fewer workers than connections it withholds the
            //    poller itself. In the dispatching mode a lingering worker only
            //    withheld itself, because a separate thread went on polling.
            //
            // This is what Go does: read optimistically, park on EAGAIN
            // (internal/poll.FD.Read). The park is what re-arming is here.
            // -1 on a virtual thread: wait for the next request for as long as the
            // client cares to take. That is not a blocked thread, it is a parked
            // virtual thread costing a stack and nothing else, which is exactly
            // the resource an idle keep-alive connection should cost.
            int linger = virtualThreads ? -1 : KEEPALIVE_LINGER_MILLIS;
            if(virtualThreads || linger > 0) {
                boolean more;
                // BEFORE THE WAIT, not after it. Under virtual threads this parks
                // until the client sends something, which may be never, and an
                // upload's buffer would sit here for all of it.
                conn.releaseIdleMemory();
                try {
                    // A readiness wait rather than a timed read: it is one syscall
                    // and it leaves the receive deadline alone, so the request this
                    // is waiting for still gets the full one when it arrives.
                    // Setting and restoring SO_RCVTIMEO around each wait did work,
                    // and cost four setsockopt per request -- 15% of syscall time.
                    if(!ServerSocket.awaitReadable(fd, linger)) {
                        break;                  // quiet client; the poller can have it
                    }
                    // The request this buffer was parsed from has been ANSWERED,
                    // so nothing points into it any more and the borrow can be
                    // dropped rather than copied.
                    //
                    // Without this, fill() below sees parsedFromBuffer still set
                    // from the request just served, reads that as "midway through a
                    // request", and takes detachPreservingOffsets -- a full copy of
                    // the borrowed buffer on EVERY keep-alive request. Measured on
                    // /plaintext under virtual threads: 1989689 detaches against
                    // 2000000 reads, one 97-byte array per request, 37% of
                    // everything the route allocated. The zero-copy read was
                    // working perfectly and handing the saving straight back here.
                    //
                    // The flag's real job is the SECOND read within one request (a
                    // body arriving after its headers), where slices into this
                    // array are live and the copy is required. That case is
                    // untouched: readRequest clears the flag on entry and raises it
                    // once the header block is parsed, so it is set exactly across
                    // the window where a Request exists. This point is outside that
                    // window by construction -- the handler has returned and the
                    // response is on the wire.
                    conn.parsedFromBuffer = false;
                    more = conn.fill(scratch);
                } catch (IOException err) {
                    drop(fd);
                    return;
                }
                if(more) {
                    continue;
                }
                // Readable but nothing came: the peer closed.
                drop(fd);
                return;
            }
            break;
        }
        // Before the descriptor can be taken by another worker: the read buffer
        // belongs to THIS thread and the next request on it will overwrite these
        // bytes. Must come before reactor.add, not after -- the moment the fd is
        // registered, another worker can pick it up.
        conn.releaseBorrowed();
        // The same reason, for the other way a connection goes quiet: handed back
        // to the poller, it waits there with everything it was holding.
        conn.releaseIdleMemory();
        try {
            // Back to the poller for the next request on this connection. Both
            // epoll_ctl and kevent are safe to call from this thread.
            if(virtualThreads) {
                // Reached only when the connection itself is finished: a virtual
                // thread does not come back here to wait, it parks where it waits.
                // Re-arming now would hand the poller a descriptor nobody owns.
                drop(fd);
            } else {
                ServerSocket.setBlocking(fd, false);
                armConnection(fd, false);
            }
        } catch (IOException err) {
            drop(fd);
        }
    }

    /**
     * One turn of an HTTP/2 connection: read what is available, answer every
     * request that completed, flush, and hand the descriptor back to the reactor.
     *
     * Deliberately the same shape as the HTTP/1.1 path rather than a worker that
     * owns the connection for its lifetime. h2 connections are long-lived by
     * design, so pinning a worker to each would mean the pool size is the limit on
     * concurrent clients -- the exact thing the reactor exists to avoid.
     */
    private void serveHttp2(int fd, long session, byte[] pending, int pendingLength) {
        Http2 h2;
        // Held for the WHOLE turn, not just while a handler runs. The per-stream
        // count below drops to zero as the last response is submitted, and the
        // flush and the liveness check after the loop still call into nghttp2 and
        // the TLS session -- so stop() could see nothing in flight and free both
        // underneath this thread. A turn with no completed request at all, one
        // that only pumped control frames, was never counted by anything.
        http2Turns.incrementAndGet();
        try {
            Object existing = http2Sessions.get(new Integer(fd));
            if(existing == null) {
                // Told to the native side once, where the reservation happens.
                // Idempotent, so doing it per session rather than finding a
                // startup hook costs an atomic store on a path that is already
                // creating a session.
                Http2.setMaxBodyBytes(MAX_OPEN_H2_BODY_BYTES);
                Http2.setMaxFileBodies(MAX_OPEN_H2_FILES);
                h2 = Http2.create();
                http2Sessions.put(new Integer(fd), h2);
                // The SETTINGS preface has to reach the client before anything else.
                flushHttp2(fd, session, h2);
            } else {
                h2 = (Http2)existing;
            }

            if(pending != null && pendingLength > 0) {
                // Bytes already read while deciding this was h2, preface included.
                h2.receive(pending, 0, pendingLength);
            } else {
                byte[] scratch = new byte[16384];
                int n = readFrom(fd, session, scratch, 0, scratch.length);
                if(n <= 0) {
                    drop(fd);
                    return;
                }
                h2.receive(scratch, 0, n);
            }

            // Every submitted body is COPIED into a native buffer that lives until
            // the flush after this loop, so a connection completing many streams at
            // once holds all of them at the same time: with the concurrency this
            // server advertises and an endpoint returning a large body, one client
            // could hold hundreds of megabytes of native response buffers on top of
            // the Java ones. Draining when enough has piled up bounds that without
            // paying a syscall per response.
            long queuedBodyBytes = 0;
            Http2.Stream stream;
            while((stream = h2.nextRequest()) != null) {
                // :authority is what Host is in HTTP/1.1, so the handler sees a
                // request shaped exactly like an HTTP/1.1 one. Which means it has
                // to be held to the same rules: a handler reading getHeader("host")
                // cannot tell which protocol carried the request, so an authority
                // refused on one side and served on the other is one server giving
                // two answers. RFC 9113 8.3.1 also requires :authority and a Host
                // field to agree when both are sent.
                Map headers = new LinkedHashMap(stream.getHeaders());
                Object carriedHost = headers.get("host");
                String authority = stream.getAuthority();
                if(authority != null) {
                    if(carriedHost != null
                            && !String.valueOf(carriedHost).equalsIgnoreCase(authority)) {
                        if(!h2.respond(stream.getId(), 400, "text/plain", new ArrayList(),
                                asciiBytes("the authority and the Host header disagree"))) {
                            h2.respond(stream.getId(), 400, "text/plain",
                                       new ArrayList(), null);
                        }
                        requestsServed.incrementAndGet();
                        continue;
                    }
                    headers.put("host", authority);
                }
                Object effectiveHost = headers.get("host");
                // AT LEAST ONE OF THEM, which is what the HTTP/1.1 parser requires
                // of Host. A stream carrying neither :authority nor Host used to
                // skip this check entirely and reach the handler with no authority
                // at all, so anything routing or authorising on the host saw a null
                // over h2 and a 400 over h1 for the same request. RFC 9113 calls
                // that stream malformed.
                if(effectiveHost == null) {
                    if(!h2.respond(stream.getId(), 400, "text/plain", new ArrayList(),
                            asciiBytes("the request carries neither :authority nor Host"))) {
                        h2.respond(stream.getId(), 400, "text/plain", new ArrayList(), null);
                    }
                    requestsServed.incrementAndGet();
                    continue;
                }
                if(!isAuthority(String.valueOf(effectiveHost))) {
                    if(!h2.respond(stream.getId(), 400, "text/plain", new ArrayList(),
                            asciiBytes("the authority is not a valid authority"))) {
                        h2.respond(stream.getId(), 400, "text/plain", new ArrayList(), null);
                    }
                    requestsServed.incrementAndGet();
                    continue;
                }
                byte[] h2RequestBody = stream.getBody();
                if(h2RequestBody != null && h2RequestBody.length > 0
                        && !Utf8.isValid(h2RequestBody, 0, h2RequestBody.length)) {
                    // Decided here rather than in getBodyAsString, because this is
                    // where a status code can be produced: the decoder has no way
                    // to answer 400, and returning null there would have made a
                    // malformed body indistinguishable from an absent one.
                    if(!h2.respond(stream.getId(), 400, "text/plain", new ArrayList(),
                            asciiBytes("the request body is not valid UTF-8"))) {
                        // The explanation is itself a body, and under a full
                        // process budget respond() takes nothing and says so. This
                        // path ignored that and moved on, so the stream was left
                        // unanswered until the connection timed out -- a client
                        // that sent bad bytes under load simply hung. The status
                        // still has to arrive; only the sentence is optional.
                        h2.respond(stream.getId(), 400, "text/plain", new ArrayList(), null);
                    }
                    requestsServed.incrementAndGet();
                    continue;
                }
                if(!targetDecodesToUtf8(stream.getPath())) {
                    // The same rule the HTTP/1 request line takes, because the
                    // handler cannot tell which protocol carried it. Fixing this in
                    // the parser alone left "?name=%C3%28" refused over HTTP/1 and
                    // served over h2 -- one server, two answers, which is the shape
                    // of defect this whole file keeps closing.
                    if(!h2.respond(stream.getId(), 400, "text/plain", new ArrayList(),
                            asciiBytes("the request target is not valid UTF-8"))) {
                        h2.respond(stream.getId(), 400, "text/plain", new ArrayList(), null);
                    }
                    requestsServed.incrementAndGet();
                    continue;
                }
                if(!isKnownMethod(stream.getMethod())) {
                    // 501 before the handler, for the same reason the HTTP/1
                    // request line answers 501: the path may well exist, the verb
                    // is what is unknown. Without this the two protocols
                    // DISAGREED on the same server -- "BREW", or a lowercase
                    // "get", was refused over HTTP/1 and handed to application
                    // code over h2, where a generated router turns it into a 404
                    // and a hand-written handler may treat anything that is not a
                    // GET as a write.
                    if(!h2.respond(stream.getId(), 501, "text/plain", new ArrayList(),
                            asciiBytes("unsupported method"))) {
                        h2.respond(stream.getId(), 501, "text/plain", new ArrayList(), null);
                    }
                    requestsServed.incrementAndGet();
                    continue;
                }
                Request request = new Request(stream.getMethod(), stream.getPath(),
                        "HTTP/2", headers, stream.getBodyAsString());
                Response response;
                inFlightRequests.incrementAndGet();
                try {
                    response = handler.handle(request);
                    if(response == null) {
                        response = Response.text(404, "not found");
                    }
                } catch (Exception err) {
                    System.err.println("handler failed: " + err);
                    response = Response.text(500, "internal error");
                }
                try {
                boolean headOnly = "HEAD".equals(stream.getMethod());
                List extra = new java.util.ArrayList();
                // RFC 9110 6.6.1: an origin server with a clock MUST send Date, and
                // the HTTP/1 writer does. This path sent only the content type and
                // whatever the handler added -- and a handler cannot make up for it,
                // because "date" is refused as server-owned. Caches were left without
                // the timestamp they compute freshness and age from.
                // ONE entry, and a complete line: Http2.headerLines() treats every
                // element as "name: value" and the native parser drops anything
                // without a colon. Added as two elements this produced two lines it
                // ignored, so the header was still absent and nothing failed -- no
                // test asserted it, which is why the first attempt looked right.
                extra.add("date: " + currentHttpDate());
                if(response.extraHeaders != null) {
                    java.util.Iterator it = response.extraHeaders.keySet().iterator();
                    while(it.hasNext()) {
                        Object key = it.next();
                        Object value = response.extraHeaders.get(key);
                        if(key != null && value != null) {
                            String name = String.valueOf(key);
                            String text = String.valueOf(value);
                            // The native side splits this block on '\n', so a newline
                            // here is another field exactly as it is over HTTP/1.1.
                            if(isServerOwnedHeader(name)) {
                                System.err.println("dropped a response header the "
                                        + "server owns: " + sanitizeForLog(name));
                            } else if(isHeaderName(name) && isHeaderSafe(text)) {
                                extra.add(name + ": " + text);
                            } else {
                                System.err.println("dropped a response header whose name "
                                        + "is not a token or whose value carries a control "
                                        + "character: " + sanitizeForLog(name));
                            }
                        }
                    }
                }
                String contentType = safeContentType(response.contentType);
                // The same rule as HTTP/1: a 204, 304 or 1xx carries no body, so
                // a DATA frame must not follow the headers here either.
                boolean noBody = headOnly || statusForbidsBody(response.status);
                if(response.fileFd >= 0 && !noBody
                        && Http2.pendingBodyFiles() >= MAX_OPEN_H2_FILES) {
                    // BEFORE submitting, not after. The turn check below stops
                    // this session, but every other session wakes on a control
                    // frame and submits one more first, so the cap was really
                    // "the cap plus one per connection" -- and a peer holding its
                    // window shut can keep waking them. Descriptors are a process
                    // resource and running out stops the server accepting sockets
                    // at all, which is a failure for every client rather than the
                    // one that caused it.
                    //
                    // Answered rather than deferred: the handler has ALREADY
                    // opened the descriptor, so holding the response holds the
                    // very thing being rationed. Closing it and saying so is the
                    // honest answer, and 503 is what it is.
                    StaticFiles.closeFile(response.fileFd);
                    // Even this small explanation is a body, and a body is what
                    // the ceiling refuses. If there is no room for it, the status
                    // alone still has to reach the client -- dropping the whole
                    // response would leave the stream hanging.
                    if(!h2.respond(stream.getId(), 503, "text/plain", extra,
                            asciiBytes("too many files in flight"))) {
                        h2.respond(stream.getId(), 503, "text/plain", extra, null);
                    }
                } else if(response.fileFd >= 0 && !noBody) {
                    // Streamed frame by frame out of the descriptor. Reading the file
                    // in first cost its whole size in the heap plus the same again in
                    // the native copy, so a large enough public file turned one request
                    // into an OutOfMemoryError -- which the catch above does not catch,
                    // because it is an Error. The descriptor belongs to the session
                    // from here, so nothing on this side closes it.
                    if(h2.respondFile(stream.getId(), response.status, contentType,
                            extra, response.fileFd, response.fileOffset, response.fileLength)) {
                        // The session owns it from here and frees it natively.
                        StaticFiles.handOverFile(response.fileFd);
                    } else {
                        // Refused by the descriptor ceiling, which means the
                        // session took NOTHING -- the fd is still ours to close.
                        // The Java-side check above is now an early-out rather
                        // than the enforcement; this is the enforcement, and it
                        // happens in the same step that takes the descriptor.
                        StaticFiles.closeFile(response.fileFd);
                        h2.respond(stream.getId(), 503, "text/plain", extra, null);
                    }
                } else {
                    // A HEAD describes the representation it is not sending, and
                    // that is the whole point of asking: over HTTP/1 this server
                    // reports the real length, so over HTTP/2 it has to as well,
                    // or the same static file answers a size on one protocol and
                    // nothing on the other from one handler. Only for a HEAD --
                    // a bodiless STATUS has no representation to describe, which
                    // is the distinction the HTTP/1 writer already makes.
                    // ... and only where the status permits a length at all. A
                    // HEAD of a 204 must not carry one, which statusForbidsLength
                    // already knows and the HTTP/1 writer already honours -- so
                    // adding it here unconditionally made the SAME response valid
                    // over one protocol and invalid over the other, which is the
                    // exact divergence this fix existed to remove.
                    // The descriptor is NOT closed here, and a review that says
                    // it leaks is reading one branch short: responseBodyFor()
                    // below closes it in a finally, which is the entire reason it
                    // is called on a path that wants no body. Closing it here as
                    // well was measured at exactly one extra close per request --
                    // openStaticFiles ran to -10 over ten HEADs -- and a double
                    // close is worse than the leak it was meant to fix, because
                    // the number is reusable the instant the first close returns
                    // and the second one then lands on whatever took it.
                    if(headOnly && !statusForbidsLength(response.status)) {
                        long described;
                        if(response.fileFd >= 0) {
                            described = response.fileLength;
                        } else if(response.hasDeferredJson) {
                            // respondJson leaves the value UNSERIALISED so the
                            // HTTP/1 writer can render it straight into the
                            // connection's buffer, which means response.body is
                            // empty and measuring it reports zero for a
                            // representation that is not. Rendering it is the only
                            // way to know the length, and describing the
                            // representation is the entire purpose of a HEAD.
                            described = responseBodyFor(response, false).length;
                        } else {
                            described = response.body == null ? 0 : response.body.length;
                        }
                        extra.add("content-length: " + described);
                    }
                    byte[] h2Body = responseBodyFor(response, noBody);
                    int bodyBytes = h2Body == null ? 0 : h2Body.length;
                    // The RESERVATION is the check. Testing the counter here and
                    // allocating inside respond() is two steps with a gap: two
                    // sessions being processed at once both read the total below
                    // the ceiling and then both allocate, so the real peak was the
                    // limit plus a body for every concurrent responder. respond()
                    // reserves and allocates in the same step natively, and
                    // answers false having taken nothing when the body would
                    // cross the ceiling.
                    if(!h2.respond(stream.getId(), response.status, contentType, extra,
                            h2Body)) {
                        // Bodiless, because the reason for refusing is that there
                        // is no room for bodies. An explanatory body here is the
                        // one allocation that must not be attempted.
                        h2.respond(stream.getId(), 503, "text/plain", extra, null);
                    } else {
                        queuedBodyBytes += bodyBytes;
                    }
                }
                requestsServed.incrementAndGet();
                if(queuedBodyBytes > MAX_QUEUED_H2_BODY_BYTES
                        || Http2.pendingBodyFiles() > MAX_OPEN_H2_FILES
                        || Http2.pendingBodyBytesAll() > MAX_OPEN_H2_BODY_BYTES) {
                    flushHttp2(fd, session, h2);
                    // What the flush could NOT write, not zero. nghttp2 pulls
                    // from a submitted body only as the peer's flow-control
                    // window allows, so a client that simply stops sending
                    // WINDOW_UPDATE makes every flush a no-op while the bodies
                    // stay retained. Zeroing a turn-local counter against that
                    // bounds nothing: the advertised stream concurrency times a
                    // large endpoint is hundreds of megabytes of native buffers
                    // held for a client that is reading none of it.
                    queuedBodyBytes = h2.pendingBodyBytes();
                    if(queuedBodyBytes > MAX_QUEUED_H2_BODY_BYTES
                            || Http2.pendingBodyFiles() > MAX_OPEN_H2_FILES
                            || Http2.pendingBodyBytesAll() > MAX_OPEN_H2_BODY_BYTES) {
                        // Still over after a real attempt to write, so the peer
                        // is not draining. Leave the rest of the ready requests
                        // where they are -- their inbound bodies are already
                        // capped by the session limit -- and end the turn. The
                        // WINDOW_UPDATE that unblocks this connection wakes it
                        // again, and a peer that sends nothing at all is closed
                        // by the idle deadline rather than held forever.
                        break;
                    }
                }
                } finally {
                    // Held until the response has been SUBMITTED, not merely produced.
                    // Releasing it after the handler let stop() see no work in flight
                    // while this thread was still about to call into nghttp2 -- so the
                    // deadline sweep could close the descriptor and free the session
                    // underneath it, which truncates the response at best.
                    inFlightRequests.decrementAndGet();
                }
            }
            flushHttp2(fd, session, h2);
            if(!h2.isAlive()) {
                drop(fd);
                return;
            }
            ServerSocket.setBlocking(fd, false);
            armConnection(fd, false);
        } catch (Exception err) {
            trace("fd=" + fd + " http/2 failed: " + err);
            drop(fd);
        } finally {
            http2Turns.decrementAndGet();
        }
    }

    /** The HTTP/2 connection preface, sent by a client that opens with h2. */
    private static final byte[] HTTP2_PREFACE = prefaceBytes();

    private static byte[] prefaceBytes() {
        try {
            return "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes("UTF-8");
        } catch (IOException err) {
            return new byte[0];
        }
    }

    /**
     * True while what has arrived is still consistent with the preface. Lets the
     * read loop stop early on an ordinary request rather than waiting for 24 bytes
     * that will never match -- "GET / HTTP/1.1" diverges at the second character.
     */
    private static boolean startsWithPrefacePrefix(Conn conn) {
        int have = Math.min(conn.available(), HTTP2_PREFACE.length);
        for(int iter = 0 ; iter < have ; iter++) {
            if(conn.buffer[conn.pos + iter] != HTTP2_PREFACE[iter]) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesPreface(Conn conn) {
        for(int iter = 0 ; iter < HTTP2_PREFACE.length ; iter++) {
            if(conn.buffer[conn.pos + iter] != HTTP2_PREFACE[iter]) {
                return false;
            }
        }
        return true;
    }

    private void flushHttp2(int fd, long session, Http2 h2) throws IOException {
        byte[] out = h2.drain();
        while(out != null && out.length > 0) {
            writeTo(fd, session, out, 0, out.length);
            out = h2.drain();
        }
    }

    /**
     * The response body as bytes. A file-backed response cannot use sendfile on an
     * HTTP/2 connection -- the bytes have to become DATA frames, which means they
     * have to be produced here -- so it is read in, and the descriptor is released
     * either way.
     */
    /**
     * Whether this status ends the response at the header section.
     *
     * RFC 9110: a 1xx, 204 or 304 response carries no body, and a client stops
     * reading at the blank line. Writing one anyway does not merely waste bytes
     * -- on a keep-alive connection the client reads those bytes as the start of
     * the NEXT response, and everything after that on the connection is
     * misframed. Only HEAD used to be treated this way.
     */
    static boolean statusForbidsBody(int status) {
        // 205 belongs here with 204: RFC 9110 15.3.6 says a Reset Content
        // response cannot contain content and is terminated by the first empty
        // line, so a handler that returns bytes with it desynchronises a
        // keep-alive connection exactly the way a 204 with bytes does.
        return status == 204 || status == 205 || status == 304
                || (status >= 100 && status < 200);
    }

    /**
     * Whether this status must not carry Content-Length at all.
     *
     * RFC 9110 6.4.1 makes that a MUST NOT for 1xx and 204.
     *
     * 304 is here too, which is a correction. The rule for one is not that it
     * carries no length but that any length it carries must describe the
     * SELECTED REPRESENTATION -- what a 200 for the same request would have
     * sent. Nothing here knows that: a 304 is built by StaticFiles as
     * Response.empty, so the only figure available is zero, and sending
     * "Content-Length: 0" tells the cache the file it just validated is empty.
     * The header is optional on a 304, so omitting it is both correct and the
     * only honest answer available.
     */
    static boolean statusForbidsLength(int status) {
        return status == 204 || status == 304 || (status >= 100 && status < 200);
    }

    private byte[] responseBodyFor(Response response, boolean headOnly) throws IOException {
        if(response.fileFd < 0) {
            if(headOnly) {
                return new byte[0];
            }
            if(response.hasDeferredJson) {
                // respondJson and jsonValue leave the value unserialised so the HTTP/1.1
                // writer can render it straight into the connection's reusable buffer.
                // There is no such buffer here -- the bytes have to become DATA frames --
                // so they are built as their own array. Without this the body is empty,
                // and the same handler that works over HTTP/1.1 answers HTTP/2 with
                // nothing at all.
                return Response.bytes(Json.write(response.deferredJson));
            }
            return response.body;
        }
        try {
            if(headOnly) {
                return new byte[0];
            }
            // Reached only when the caller has no streaming path to offer. The HTTP/2
            // caller does -- see respondFile -- and comes here for HEAD alone, where
            // the point of this branch is the close below.
            return StaticFiles.readAll(response.fileFd, response.fileOffset, response.fileLength);
        } finally {
            StaticFiles.closeFile(response.fileFd);
        }
    }

    /**
     * True for the fields whose values this server decides.
     *
     * A handler that sets Content-Length or Transfer-Encoding through extraHeaders
     * gets it serialised AFTER the server's own, so the response carries two
     * answers to "where does the body end". A client and a proxy may pick
     * different ones, which desynchronises everything after it on that connection
     * -- request smuggling, and cache poisoning when the map came from the request.
     * Connection is the same: the server decides keep-alive from the request and
     * the framing follows from that.
     *
     * Dropped rather than merged. There is no sensible merge of two lengths, and a
     * handler wanting a different body should return a different body.
     */
    private static boolean isServerOwnedHeader(String name) {
        return name.equalsIgnoreCase("content-length")
                || name.equalsIgnoreCase("transfer-encoding")
                || name.equalsIgnoreCase("connection")
                || name.equalsIgnoreCase("content-type")
                || name.equalsIgnoreCase("date");
    }

    /**
     * The content type to serialise: the handler's, or the default.
     *
     * Validated with the same rule as every other header value. Response.respond and
     * the public Response constructor both take this from the handler, so it can
     * carry request-derived text just as extraHeaders can -- guarding one and not
     * the other left the same response-splitting hole open through a different
     * argument. A rejected type falls back rather than being dropped, because a
     * response without Content-Type is its own problem.
     */
    private static String safeContentType(String contentType) {
        if(contentType == null) {
            return DEFAULT_CONTENT_TYPE;
        }
        if(isHeaderSafe(contentType)) {
            return contentType;
        }
        System.err.println("replaced a content type containing a control character: "
                + sanitizeForLog(contentType));
        return DEFAULT_CONTENT_TYPE;
    }

    /**
     * True when this text can go into a response head as it stands.
     *
     * CR and LF end a field; NUL truncates it in every C call underneath. None of
     * the three can appear in a header name or value, and a header carrying one is
     * either a bug or an injection attempt -- neither is worth serialising.
     */
    /**
     * True when this is a field NAME as HTTP defines one: a non-empty run of
     * tchar (RFC 9110 5.6.2). isHeaderSafe is the right rule for a value and
     * the wrong one for a name -- a space, tab or colon passes it and still
     * produces a field line no peer reads the way the handler meant. A leading
     * space is worse than merely malformed: over HTTP/1 that is obsolete line
     * folding, so the name and value are appended to the PREVIOUS header
     * instead of forming their own. Over HTTP/2 nghttp2 rejects the name, and
     * that can cost the whole response rather than the one header.
     */
    private static boolean isHeaderName(String name) {
        if(name.length() == 0) {
            return false;
        }
        for(int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean tchar = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '!' || c == '#' || c == '$' || c == '%' || c == '&'
                    || c == '\'' || c == '*' || c == '+' || c == '-' || c == '.'
                    || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
            if(!tchar) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether this value may be written as a field value.
     *
     * RFC 9110's field-value carries HTAB, SP, VCHAR (0x21-0x7E) and obs-text
     * (0x80-0xFF). Every other C0 character, and DEL, is a delimiter to somebody:
     * over HTTP/1 they produce a malformed field, and an HTTP/2 submission
     * carrying one can be rejected outright and take the whole response with it.
     *
     * THE TEST IS ON THE BYTE THAT WILL BE EMITTED, not on the char, and that is
     * the part worth reading twice. Both writers narrow with a plain cast --
     * Buffer.put does out[n] = (byte)charAt(i), and asciiBytes the same -- so
     * '\u010A' is not '\n' to a char comparison and is byte 0x0A on the wire.
     * The old rule tested \r, \n and NUL as CHARS and so passed it. A handler
     * reflecting a query parameter into a header -- exactly the shape this guard
     * exists for -- therefore turned ?v=%C4%8A into a real newline in the header
     * block, which is response splitting and a cache-poisoning primitive: the
     * defect the comment beside the caller says is being prevented. Anything
     * above 0xFF cannot be spelled in one byte at all and is refused for the same
     * reason rather than being narrowed into whatever it happens to alias.
     */
    private static boolean isHeaderSafe(String value) {
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c == '\t') {
                continue;
            }
            if(c < 0x20 || c == 0x7f || c > 0xff) {
                return false;
            }
        }
        return true;
    }

    /**
     * The same question for an h2 :path, which arrives already decoded into a
     * String rather than as a slice of the read buffer.
     *
     * <p>Re-encoding to UTF-8 first is what makes the two agree: a literal
     * non-ASCII character in the path is text that came from valid bytes and
     * re-encodes to valid bytes, while the percent escapes -- the only part that
     * can be malformed -- decode and are checked exactly as they are on the
     * HTTP/1 side.
     */
    private static boolean targetDecodesToUtf8(String target) {
        if(target == null) {
            return true;
        }
        // NO "%" FAST PATH HERE. The byte version's was corrected to test for pure
        // ASCII rather than for the absence of an escape, and this one was left
        // behind -- so a raw malformed octet in an h2 :path was refused on the
        // HTTP/1 side and served on this one, which is the divergence the whole
        // check exists to close. The byte version below does the deciding; this
        // only has to hand it the right bytes.

        // NARROWED, not re-encoded. The h2 natives build this String one char per
        // OCTET, exactly as the HTTP/1 parser reads its bytes, and byteAt() takes
        // it back apart the same way -- so getBytes("UTF-8") would have re-encoded
        // each of those chars and validated something the server never received.
        byte[] raw = new byte[target.length()];
        for(int iter = 0 ; iter < raw.length ; iter++) {
            char c = target.charAt(iter);
            if(c > 0xff) {
                // Not a byte any wire could have delivered under that
                // representation, so it cannot be a target this server parsed.
                return false;
            }
            raw[iter] = (byte)c;
        }
        return targetDecodesToUtf8(raw, 0, raw.length);
    }

    /**
     * Whether the request target's percent escapes decode to valid UTF-8.
     *
     * <p>Decodes exactly as Request.percentDecode does, so the two cannot disagree
     * about what the handler will see. '+' is not folded to a space here because
     * both are ASCII and neither changes whether the run is valid UTF-8.
     *
     * <p>A target with no '%' in it cannot decode to anything but itself, so the
     * scan for one is the whole cost on the overwhelming majority of requests and
     * nothing is allocated for them.
     */
    private static boolean targetDecodesToUtf8(byte[] raw, int from, int to) {
        boolean inspect = false;
        for(int iter = from ; iter < to ; iter++) {
            int c = raw[iter] & 0xff;
            // A RAW CONTROL BYTE IS NOT A TARGET. RFC 9110 excludes CTL from a
            // URI, so anything below 0x20 or DEL arrives only from a client that
            // means something by it -- and this runs after the request line was
            // split on spaces, so CR and LF cannot reach here anyway.
            if(c < 0x20 || c == 0x7f) {
                return false;
            }
            // The fast path is PURE ASCII, not merely unescaped. The version this
            // replaces returned true for any target with no '%' in it, reasoning
            // that nothing without an escape can decode to anything but itself --
            // true, and beside the point, because "itself" is not necessarily
            // valid UTF-8. A raw 0xC3 followed by '(' is a truncated sequence
            // sitting on the wire with no escape anywhere, and it decoded to
            // U+FFFD exactly as "%C3%28" did, aliasing the same legitimate
            // spelling this check exists to keep distinct.
            if(c == '%' || c >= 0x80) {
                inspect = true;
            }
        }
        if(!inspect) {
            return true;
        }
        byte[] out = new byte[to - from];
        int length = 0;
        int pos = from;
        while(pos < to) {
            int c = raw[pos] & 0xff;
            if(c == '%') {
                // EVERY '%' INTRODUCES TWO HEX DIGITS, or the target is malformed.
                // RFC 3986 leaves no other reading, and this used to fall through
                // and copy the '%' as a literal byte -- which is valid UTF-8, so
                // /?name=%ZZ and /x%2 were accepted here and handed to a
                // hand-written handler's queryParam() as themselves. The generated
                // routers and StaticFiles both refuse them, so one target was
                // rejected or served depending on which kind of handler sat behind
                // it, and an intermediary that normalises escapes could disagree
                // with all three.
                if(pos + 2 >= to) {
                    return false;
                }
                int hi = Request.hexDigit(raw[pos + 1] & 0xff);
                int lo = Request.hexDigit(raw[pos + 2] & 0xff);
                if(hi < 0 || lo < 0) {
                    return false;
                }
                out[length++] = (byte)((hi << 4) | lo);
                pos += 3;
                continue;
            }
            out[length++] = (byte)c;
            pos++;
        }
        return Utf8.isValid(out, 0, length);
    }

    /**
     * Whether {@code [from, to)} is an RFC 3986 IPv6address.
     *
     * <p>Eight groups of one to four hex digits, at most one "::" standing for a
     * run of zero groups, and an optional dotted-quad tail that counts as the
     * last two. IPvFuture ("v1.xyz") is refused: nothing sends it, and accepting
     * a form this server cannot route is how the whitelist got here.
     *
     * <p>An RFC 6874 zone id is refused too, for the same reason -- "%25eth0"
     * after the address is not something a Host field carries to an origin
     * server, and the escape-shaped syntax made it the one place a percent could
     * appear inside brackets.
     */
    private static boolean isIpv6Literal(String value, int from, int to) {
        if(to <= from) {
            return false;
        }
        int pos = from;
        int groups = 0;
        boolean compressed = false;
        if(value.charAt(pos) == ':') {
            // A leading colon is only legal as the first half of "::".
            if(pos + 1 >= to || value.charAt(pos + 1) != ':') {
                return false;
            }
            compressed = true;
            pos += 2;
            if(pos == to) {
                return true;                       // "::" alone is the any address
            }
        }
        while(pos < to) {
            int start = pos;
            int digits = 0;
            while(pos < to && digits < 4 && Hex.digit(value.charAt(pos)) >= 0) {
                pos++;
                digits++;
            }
            if(pos < to && value.charAt(pos) == '.') {
                // A dotted-quad tail ends the address and fills two groups.
                if(!isIpv4Literal(value, start, to)) {
                    return false;
                }
                groups += 2;
                pos = to;
                break;
            }
            if(digits == 0) {
                return false;
            }
            groups++;
            if(pos == to) {
                break;
            }
            if(value.charAt(pos) != ':') {
                return false;                      // a fifth hex digit, or junk
            }
            pos++;
            if(pos < to && value.charAt(pos) == ':') {
                if(compressed) {
                    return false;                  // only one "::" may appear
                }
                compressed = true;
                pos++;
                if(pos == to) {
                    break;                         // a trailing "::" is legal
                }
            } else if(pos == to) {
                return false;                      // a trailing single colon is not
            }
        }
        return compressed ? groups < 8 : groups == 8;
    }

    /** Whether {@code [from, to)} is a dotted quad, each part 0-255 and unpadded. */
    private static boolean isIpv4Literal(String value, int from, int to) {
        int parts = 0;
        int pos = from;
        while(pos < to) {
            int start = pos;
            int n = 0;
            while(pos < to && value.charAt(pos) >= '0' && value.charAt(pos) <= '9') {
                n = n * 10 + (value.charAt(pos) - '0');
                pos++;
            }
            int digits = pos - start;
            // "01" is not a dec-octet: RFC 3986 spells the leading-zero forms out
            // and none of them has one, which is also what stops an octal reading.
            if(digits < 1 || digits > 3 || n > 255
                    || (digits > 1 && value.charAt(start) == '0')) {
                return false;
            }
            parts++;
            if(pos == to) {
                break;
            }
            if(value.charAt(pos) != '.') {
                return false;
            }
            pos++;
            if(pos == to) {
                return false;                      // a trailing dot
            }
        }
        return parts == 4;
    }

    /** Whether {@code at} is a '%' followed by two hex digits. */
    private static boolean isPercentTriplet(String value, int at) {
        return at + 2 < value.length()
                && Hex.digit(value.charAt(at + 1)) >= 0
                && Hex.digit(value.charAt(at + 2)) >= 0;
    }

    /**
     * Whether this is a legal HTTP authority: a host, bracketed if it is an IPv6
     * literal, optionally followed by ":" and a port.
     *
     * <p>Userinfo is the part that matters. RFC 9110 excludes it from an HTTP
     * authority, and "Host: user@internal" is how one request is made to mean one
     * thing to a parser that takes the whole string and another to a parser that
     * reads only what follows the '@'.
     */
    private static boolean isAuthority(String value) {
        if(value.length() == 0) {
            return false;
        }
        int hostEnd;
        if(value.charAt(0) == '[') {
            int close = value.indexOf(']');
            if(close < 2) {
                return false;
            }
            // PARSED, not character-whitelisted. Accepting any run of hex digits,
            // colons and dots took "[.]" and "[1:]" for IPv6 literals -- neither
            // is one, and a frontend that parses them rejects the request this
            // server then routed on.
            if(!isIpv6Literal(value, 1, close)) {
                return false;
            }
            hostEnd = close + 1;
        } else {
            hostEnd = value.indexOf(':');
            if(hostEnd < 0) {
                hostEnd = value.length();
            }
            if(hostEnd == 0) {
                return false;
            }
            for(int iter = 0 ; iter < hostEnd ; iter++) {
                char c = value.charAt(iter);
                if(c == '%') {
                    // pct-encoded is "%" HEXDIG HEXDIG, and accepting a bare '%'
                    // as an ordinary character meant "bad%zz.example" passed here
                    // -- an authority a conforming frontend rejects or normalises,
                    // which is the same proxy-versus-origin disagreement this
                    // validator exists to close, reintroduced one level down.
                    if(!isPercentTriplet(value, iter)) {
                        return false;
                    }
                    iter += 2;
                    continue;
                }
                // RFC 3986 reg-name: unreserved / pct-encoded / sub-delims. Not
                // '@', not a space, not a control character -- and the point of
                // spelling the set out is that everything absent from it is
                // refused rather than tolerated.
                // RFC 3986 reg-name minus the comma. The comma is a sub-delim and
                // so legal in a generic reg-name, but no host has one -- and it is
                // exactly what a REPEATED field turns into over HTTP/2, where
                // Http2 combines repeats on "," as RFC 9110 says a repeated field
                // line means. A client sending "host: a" twice therefore arrived
                // as the single value "a,a", which this accepted and dispatched,
                // while the HTTP/1 parser answers 400 for a duplicate Host. One
                // character, and the two protocols agree again.
                boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9')
                        || c == '-' || c == '.' || c == '_' || c == '~'
                        || c == '!' || c == '$' || c == '&' || c == '\''
                        || c == '(' || c == ')' || c == '*' || c == '+'
                        || c == ';' || c == '=';
                if(!ok) {
                    return false;
                }
            }
        }
        if(hostEnd == value.length()) {
            return true;
        }
        if(value.charAt(hostEnd) != ':') {
            return false;
        }
        int digits = value.length() - hostEnd - 1;
        if(digits < 1 || digits > 5) {
            return false;
        }
        int port = 0;
        for(int iter = hostEnd + 1 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c < '0' || c > '9') {
                return false;
            }
            port = port * 10 + (c - '0');
        }
        return port >= 1 && port <= 65535;
    }

    /**
     * Whether this is one of the methods this server routes. Anything else is
     * 501, not a 404 -- the path may well exist, the verb is what is unknown.
     *
     * Derived from KNOWN_METHODS rather than spelling the seven out again. The
     * version this replaced did spell them out, and had no callers at all, so a
     * method added to KNOWN_METHODS would have been routed by the HTTP/1 parser
     * and refused here with nothing to notice the disagreement.
     */
    private static boolean isKnownMethod(String method) {
        for(int iter = 0 ; iter < KNOWN_METHODS.length ; iter++) {
            if(KNOWN_METHODS[iter].equals(method)) {
                return true;
            }
        }
        return false;
    }

    /** The same token rule as isHeaderName, over a slice of the read buffer. */
    private static boolean isRequestHeaderName(byte[] raw, int from, int to) {
        if(to <= from) {
            return false;
        }
        for(int iter = from ; iter < to ; iter++) {
            int c = raw[iter] & 0xff;
            boolean tchar = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '!' || c == '#' || c == '$' || c == '%' || c == '&'
                    || c == '\'' || c == '*' || c == '+' || c == '-' || c == '.'
                    || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
            if(!tchar) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether this slice holds a byte no field value may carry. HTAB is allowed
     * because RFC 9110 permits it inside a value; everything else below 0x20, and
     * DEL, is a delimiter to somebody.
     */
    private static boolean hasControlByte(byte[] raw, int from, int to) {
        for(int iter = from ; iter < to ; iter++) {
            int c = raw[iter] & 0xff;
            if((c < 0x20 && c != '\t') || c == 0x7f) {
                return true;
            }
        }
        return false;
    }

    /** The same characters would break the log line they are reported on. */
    private static String sanitizeForLog(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out.append(c == '\r' || c == '\n' || c < 0x20 ? '?' : c);
        }
        return out.toString();
    }

    /**
     * HTTP/1.1 keeps the connection alive unless asked not to; HTTP/1.0 closes
     * unless asked to keep it. Treating a 1.0 client as keep-alive leaves it
     * waiting for a close that never comes.
     */
    private static boolean wantsKeepAlive(Request request) {
        // headerContains rather than getHeader, and this is the hot path.
        //
        // getHeader materialises the value: asciiString allocates a char[] AND a
        // String, and .toLowerCase() allocates a second String -- four objects per
        // request to answer a question about a fixed token. The request already
        // holds its headers as positions into the read buffer, and
        // sliceContainsIgnoreCase answers straight off those bytes, so this is the
        // one call site that was throwing that away. Measured at 662 bytes
        // allocated per /plaintext request against a 4MB trigger, which is 60-90
        // collections a second, and the collector is what costs the tail.
        //
        // Same answers as before on both branches: headerContains is false when
        // the header is absent, so 1.0 still needs an explicit keep-alive and 1.1
        // still defaults to keeping the connection.
        if("HTTP/1.0".equals(request.getVersion())) {
            return request.headerContains("connection", "keep-alive");
        }
        return !request.headerContains("connection", "close");
    }

    private void writeStatusOnly(Conn conn, int status, String message) {
        try {
            byte[] body = (message == null ? reason(status) : message)
                    .getBytes("UTF-8");
            StringBuilder head = new StringBuilder();
            head.append("HTTP/1.1 ").append(status).append(' ').append(reason(status)).append("\r\n");
            head.append("Content-Type: text/plain; charset=utf-8\r\n");
            head.append("Date: ").append(currentHttpDate()).append("\r\n");
            head.append("Content-Length: ").append(body.length).append("\r\n");
            head.append("Connection: close\r\n\r\n");
            conn.write(head.toString().getBytes("UTF-8"));
            conn.write(body);
        } catch (IOException err) {
            // The peer is already gone; there is nowhere to report this.
        }
    }

    /**
     * The methods this server routes. Anything else is 501, not a 404.
     *
     * Held as constants so a parsed method can BE one of them rather than a fresh
     * String per request.
     */
    private static final String[] KNOWN_METHODS = {
        "GET", "POST", "HEAD", "PUT", "DELETE", "PATCH", "OPTIONS"
    };

    /**
     * The same constants as bytes, because comparing against the String walks it a
     * character at a time and String.charAt is a call.
     *
     * Every one of these is matched against raw buffer bytes on the request path,
     * and the comparison was reaching into a String for each character: a profile
     * of the plaintext benchmark put String.charInternal at 4.92% of in-binary self
     * time, third behind syscall dispatch and serveOne itself. A request line costs
     * about eleven of those calls -- eight for the version and three for the method
     * -- before a single header is looked at. Held as bytes the same comparison is
     * a byte load, and the constants are built once at class initialisation.
     *
     * The IGNORE-CASE constants are stored already folded, so only the data side is
     * folded at comparison time rather than both sides on every character.
     */
    private static final byte[] HTTP_1_1_BYTES = asciiConstant("HTTP/1.1");
    private static final byte[] HTTP_1_0_BYTES = asciiConstant("HTTP/1.0");
    private static final byte[] CONTENT_LENGTH_BYTES = asciiConstant("content-length");
    private static final byte[] TRANSFER_ENCODING_BYTES = asciiConstant("transfer-encoding");
    private static final byte[] HOST_BYTES = asciiConstant("host");
    private static final byte[][] KNOWN_METHOD_BYTES = asciiConstants(KNOWN_METHODS);

    private static byte[] asciiConstant(String ascii) {
        byte[] out = new byte[ascii.length()];
        for(int iter = 0 ; iter < ascii.length() ; iter++) {
            out[iter] = (byte)ascii.charAt(iter);
        }
        return out;
    }

    private static byte[][] asciiConstants(String[] values) {
        byte[][] out = new byte[values.length][];
        for(int iter = 0 ; iter < values.length ; iter++) {
            out[iter] = asciiConstant(values[iter]);
        }
        return out;
    }

    /**
     * Reads one request. Null when the peer closed; ProtocolException when what
     * arrived is not a request this server will act on.
     */
    private Request readRequest(Conn conn, byte[] scratch) throws IOException {
        // Cleared before the header block is read and raised once it has been
        // parsed, so fill() can tell "start of a request" from "midway through
        // one" -- which an empty buffer alone cannot say.
        conn.parsedFromBuffer = false;
        int headerEnd = indexOfHeaderEnd(conn.buffer, conn.pos);
        // An ABSOLUTE bound on the head, not a per-read one. SO_RCVTIMEO restarts
        // on every successful read, so a client sending one byte just inside each
        // window holds its worker for as long as it likes -- and in pool mode,
        // which is what TLS falls back to, the default sixteen such connections
        // are the whole server. Armed by the first byte rather than on entry: a
        // kept-alive connection may legitimately sit idle between requests, and
        // that idleness is the socket timeout's business, not this one.
        //
        // The head only. A body is bounded by MAX_BODY_BYTES and by the socket
        // timeout between reads, and a wall-clock bound on it would refuse a
        // large upload over a slow link, which is a real client rather than an
        // attack.
        long headDeadline = 0;
        while(headerEnd < 0) {
            if(conn.available() > MAX_HEADER_BYTES) {
                throw new ProtocolException(431, "request head too large");
            }
            if(conn.available() > 0 && headDeadline == 0) {
                headDeadline = System.currentTimeMillis() + SOCKET_TIMEOUT_MILLIS;
            }
            if(headDeadline != 0 && System.currentTimeMillis() > headDeadline) {
                throw new ProtocolException(408, "the request head did not arrive in time");
            }
            if(!conn.fill(scratch)) {
                return null;
            }
            headerEnd = indexOfHeaderEnd(conn.buffer, conn.pos);
        }
        // Parsed IN PLACE, out of the array the kernel filled. Nothing here builds
        // a String for a name or a value: the header block used to become a
        // String, be split into lines, each line split again and each half
        // substring'd, lower-cased and trimmed -- about 2.6KB per request, and the
        // largest single source of allocation in this server. Names and tokens are
        // ASCII by definition, so a byte comparison with an ASCII fold is exact.
        // From here the slices below name positions in THIS array, so fill() must
        // preserve it rather than let go of the borrow.
        conn.parsedFromBuffer = true;
        byte[] raw = conn.buffer;
        int blockStart = conn.pos;
        int blockEnd = headerEnd;
        conn.pos = headerEnd + 4;

        int lineEnd = indexOfCrLfWithin(raw, blockStart, blockEnd);
        if(lineEnd < 0) {
            lineEnd = blockEnd;         // a single request line with no headers
        }
        if(lineEnd == blockStart) {
            throw new ProtocolException(400, "empty request");
        }

        int firstSpace = indexOfByte(raw, blockStart, lineEnd, (byte)' ');
        int secondSpace = firstSpace < 0 ? -1
                : indexOfByte(raw, firstSpace + 1, lineEnd, (byte)' ');
        if(firstSpace < 0 || secondSpace < 0
                || indexOfByte(raw, secondSpace + 1, lineEnd, (byte)' ') >= 0) {
            throw new ProtocolException(400, "malformed request line");
        }

        String version;
        int versionStart = secondSpace + 1;
        int versionLength = lineEnd - versionStart;
        if(sliceEquals(raw, versionStart, versionLength, HTTP_1_1_BYTES)) {
            version = "HTTP/1.1";
        } else if(sliceEquals(raw, versionStart, versionLength, HTTP_1_0_BYTES)) {
            version = "HTTP/1.0";
        } else {
            throw new ProtocolException(505, "unsupported HTTP version");
        }

        String method = knownMethod(raw, blockStart, firstSpace - blockStart);
        if(method == null) {
            // 501, not 404: the path may well exist, the verb is what is unknown.
            throw new ProtocolException(501, "unsupported method");
        }

        int targetStart = firstSpace + 1;
        int targetLength = secondSpace - targetStart;
        // Valid hex that is NOT valid UTF-8, which is a different thing from a bad
        // escape and was the one still unchecked here. "?name=%C3%28" is a
        // truncated two-byte sequence, and new String(_, "UTF-8") answers U+FFFD
        // rather than failing -- so the handler received "\uFFFD(" and the request
        // became indistinguishable from "?name=%EF%BF%BD%28", which spells that
        // value legitimately. Two request spellings, one handler input, and a
        // frontend that validates UTF-8 rejects only one of them: the same
        // proxy-versus-origin disagreement as a malformed Host.
        //
        // Refused here rather than in queryParam, which returns a String and has
        // no way to say "malformed" -- answering null there would make a bad
        // parameter look absent, which is worse than either. The same decision
        // this PR already took for a request BODY and for a static file path; the
        // target was the remaining hole.
        //
        // Note this does not reject a malformed ESCAPE. percentDecode passes "%zz"
        // through as literal bytes, browsers do send a bare '%', and tightening
        // that is a separate question from whether what DID decode is text.
        if(!targetDecodesToUtf8(raw, targetStart, targetStart + targetLength)) {
            throw new ProtocolException(400, "the request target is not valid UTF-8");
        }
        // The origin-form target when it had to be built rather than pointed at.
        String synthesized = null;
        // The authority of an absolute-form target. RFC 9112 3.2.2 says a server
        // receiving one MUST use it and IGNORE the Host field, so keeping it lets
        // the two be compared: a proxy sending "GET http://public.example/p" with
        // "Host: internal.example" would otherwise leave getHeader("host") saying
        // internal.example to whatever routes or authorizes on it.
        String absoluteAuthority = null;
        // Absolute-form ("GET http://host/path"), which a request through a proxy
        // uses and RFC 9112 requires a server to accept.
        if(sliceStartsWithIgnoreCase(raw, targetStart, targetLength, "http://")
                || sliceStartsWithIgnoreCase(raw, targetStart, targetLength, "https://")) {
            int schemeEnd = indexOfByte(raw, targetStart, targetStart + targetLength, (byte)':');
            int authority = schemeEnd + 3;      // past "://"
            int end = targetStart + targetLength;
            int slash = indexOfByte(raw, authority, end, (byte)'/');
            int question = indexOfByte(raw, authority, end, (byte)'?');
            // Whichever comes first ends the authority. Looking only for '/' drops the
            // query of "http://host?a=b" on the floor, and reads a '/' INSIDE a query
            // value as the start of the path.
            int authorityEnd = end;
            if(slash >= 0 && (question < 0 || slash < question)) {
                authorityEnd = slash;
            } else if(question >= 0) {
                authorityEnd = question;
            }
            absoluteAuthority = asciiString(raw, authority, authorityEnd - authority);
            if(slash >= 0 && (question < 0 || slash < question)) {
                targetLength = end - slash;
                targetStart = slash;
            } else if(question >= 0) {
                // No path but a query. The origin-form is "/" followed by that query,
                // which is not a range of this buffer, so it has to be built.
                synthesized = "/" + asciiString(raw, question, end - question);
                targetStart = -1;
            } else {
                targetStart = -1;               // origin-form is just "/"
            }
        }
        String target;
        if(targetStart < 0) {
            target = synthesized == null ? "/" : synthesized;
        } else {
            if(targetLength == 0
                    || (raw[targetStart] != '/'
                        && !(targetLength == 1 && raw[targetStart] == '*'
                             && "OPTIONS".equals(method)))) {
                throw new ProtocolException(400, "malformed request target");
            }
            target = conn.internTarget(raw, targetStart, targetLength);
        }
        // The slice a generated router matches on, so it compares the bytes the
        // parser already has instead of the String it just built. Zero when the
        // target had to be BUILT rather than pointed at -- absolute-form with no
        // path -- because then no range of this buffer holds it and the String is
        // the only representation. Passing 0,0 for every request, which is what
        // this did, left the byte path unreachable and every route matched as a
        // String: correct, and none of the point.
        int sliceStart = targetStart < 0 ? 0 : targetStart;
        int sliceLength = targetStart < 0 ? 0 : targetLength;

        // Four ints per header, into a buffer the connection reuses.
        int[] slices = conn.slices;
        int headerCount = 0;
        int at = lineEnd + 2;
        while(at < blockEnd) {
            int end = indexOfCrLfWithin(raw, at, blockEnd);
            if(end < 0) {
                end = blockEnd;
            }
            if(end == at) {
                at = end + 2;
                continue;
            }
            int first = raw[at] & 0xff;
            if(first == ' ' || first == '\t') {
                // Obsolete line folding. Two parsers disagreeing about where a
                // header ends is how a request is smuggled; RFC 9112 says reject.
                throw new ProtocolException(400, "obsolete line folding");
            }
            int colon = indexOfByte(raw, at, end, (byte)':');
            if(colon <= at) {
                throw new ProtocolException(400, "malformed header");
            }
            int nameStart = at;
            int nameEnd = colon;
            // RFC 9112 5.1: no whitespace between the field name and the colon, and
            // a server MUST reject a message that has it. Trimming it instead made
            // "Content-Length : 5" a valid Content-Length here while an intermediary
            // in front either rejects that line or reads it as a different field.
            // Two parsers disagreeing about which headers a request carries is how a
            // request is smuggled, which is why the obsolete line folding above is
            // refused rather than joined up.
            if(nameEnd > nameStart && isSpace(raw[nameEnd - 1])) {
                throw new ProtocolException(400, "whitespace before header colon");
            }
            int valueStart = colon + 1;
            int valueEnd = end;
            while(valueStart < valueEnd && isSpace(raw[valueStart])) {
                valueStart++;
            }
            while(valueEnd > valueStart && isSpace(raw[valueEnd - 1])) {
                valueEnd--;
            }
            if(headerCount * 4 + 4 > slices.length) {
                int[] grown = new int[slices.length * 2];
                System.arraycopy(slices, 0, grown, 0, slices.length);
                slices = grown;
                conn.slices = grown;
            }
            // The name must be a TOKEN and the value must carry no control
            // character. Both are smuggling defences, the same one the folding
            // and whitespace-before-colon rules above are: this parser finds the
            // end of a field by scanning for CRLF, so a bare LF inside a value is
            // just a byte to it -- while an intermediary that accepts bare LF as
            // a delimiter reads "X: v\nContent-Length: 5" as TWO fields and frames
            // the body by that length. One connection, two readings, and the next
            // request on it is whatever the attacker put after the body. The
            // response side already refuses exactly this shape (isHeaderName); a
            // request is the direction that matters more.
            if(!isRequestHeaderName(raw, nameStart, nameEnd)) {
                throw new ProtocolException(400, "malformed header name");
            }
            if(hasControlByte(raw, valueStart, valueEnd)) {
                throw new ProtocolException(400, "control character in a header value");
            }
            int base = headerCount * 4;
            slices[base] = nameStart;
            slices[base + 1] = nameEnd - nameStart;
            slices[base + 2] = valueStart;
            slices[base + 3] = valueEnd - valueStart;
            headerCount++;
            at = end + 2;
        }

        Request request;
        if(POOL_REQUEST) {
            if(conn.pooledRequest == null) {
                conn.pooledRequest = new Request(method, target, version, raw, slices,
                                                 headerCount, null, sliceStart, sliceLength);
            } else {
                conn.pooledRequest.reset(conn, method, target, version, raw, slices, headerCount,
                        null, sliceStart, sliceLength);
            }
            request = conn.pooledRequest;
        } else {
            request = new Request(method, target, version, raw, slices, headerCount, null,
                    sliceStart, sliceLength);
        }

        int contentLengthAt = -1;
        boolean chunked = false;
        String transferEncoding = null;
        int hostCount = 0;
        String hostValue = null;
        for(int iter = 0 ; iter < headerCount ; iter++) {
            int base = iter * 4;
            if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], CONTENT_LENGTH_BYTES)) {
                // Two different lengths means two readings of where this request
                // ends. Refuse rather than pick one.
                if(contentLengthAt >= 0
                        && !slicesEqual(raw, slices[contentLengthAt + 2], slices[contentLengthAt + 3],
                                        slices[base + 2], slices[base + 3])) {
                    throw new ProtocolException(400, "conflicting Content-Length");
                }
                contentLengthAt = base;
            } else if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1],
                                            TRANSFER_ENCODING_BYTES)) {
                // Every instance, in order, joined with commas. RFC 9110 5.3 makes
                // repeated fields mean the same as one field holding the joined
                // list, and framing has to be decided on the whole list: assigning
                // per field let a second Transfer-Encoding overwrite the first, so
                // "chunked" followed by anything else fell back to Content-Length
                // here while a proxy in front still framed it as chunked.
                String value = asciiString(raw, slices[base + 2], slices[base + 3]);
                transferEncoding = transferEncoding == null ? value
                        : transferEncoding + "," + value;
            } else if(sliceEqualsIgnoreCase(raw, slices[base], slices[base + 1], HOST_BYTES)) {
                hostCount++;
                if(hostValue == null) {
                    hostValue = asciiString(raw, slices[base + 2], slices[base + 3]);
                }
            }
        }
        if(transferEncoding != null) {
            // RFC 9112 6.1: chunked MUST be the final coding, and a server that
            // cannot decode the rest MUST NOT guess at the framing. An unsupported
            // coding, chunked twice, or chunked in the middle all mean this server
            // and the next hop could choose different message boundaries.
            chunked = requireChunkedIsFinalCoding(transferEncoding);
        }
        // RFC 9112: an HTTP/1.1 request MUST carry Host, and a server MUST reject
        // one that does not. Routing on a name the client never sent is how a
        // request reaches the wrong virtual host.
        if("HTTP/1.1".equals(version) && hostCount == 0) {
            throw new ProtocolException(400, "missing Host header");
        }
        // Refused rather than silently preferred one of the two. The authority is
        // what this server must act on, but a handler reading getHeader("host")
        // would still see the other, and a request that carries two different
        // answers to "which host did you mean" has no honest interpretation.
        if(absoluteAuthority != null && hostValue != null
                && !absoluteAuthority.equalsIgnoreCase(hostValue)) {
            throw new ProtocolException(400,
                    "the request target's authority and the Host header disagree");
        }
        // RFC 9112 3.2: more than one Host is a 400. Accepting it lets the two
        // request APIs disagree -- getHeader returns the first, getHeaders keeps the
        // last -- so a handler and whatever authorized it can read different
        // authorities out of the same request.
        if(hostCount > 1) {
            throw new ProtocolException(400, "duplicate Host header");
        }
        // And the authority has to BE one. Everything above settles which answer
        // to "which host did you mean" the request carries; none of it looks at
        // whether the answer is well formed, so "Host: user@internal" and
        // "Host: example.com:not-a-port" reached the handler intact. An
        // application routing or authorizing on getHeader("host") then acts on a
        // value a conforming parser in front of it would have rejected, and the
        // proxy and the origin disagreeing about the authority is the same class
        // of defect as the two framings below -- it is just spelled with a Host
        // instead of a Content-Length.
        //
        // The absolute-form authority is checked as well rather than relying on
        // the equality test above to carry the verdict across: that test runs only
        // when BOTH are present, and an HTTP/1.0 absolute-form request has no Host
        // to compare against.
        if(hostValue != null && !isAuthority(hostValue)) {
            throw new ProtocolException(400, "the Host header is not a valid authority");
        }
        if(absoluteAuthority != null && !isAuthority(absoluteAuthority)) {
            throw new ProtocolException(400,
                    "the request target's authority is not a valid authority");
        }

        String contentLength = contentLengthAt < 0 ? null : "set";
        int declaredLength = contentLengthAt < 0 ? -1
                : sliceToInt(raw, slices[contentLengthAt + 2], slices[contentLengthAt + 3]);

        if(chunked && contentLength != null) {
            // Both framings in one request is precisely how a request is smuggled
            // past a proxy that believes one and a server that believes the other.
            throw new ProtocolException(400, "both Content-Length and Transfer-Encoding");
        }

        String expectation = request.getHeader("Expect");
        if(expectation != null) {
            // EVERY token, not just whether the one we know is among them. An
            // earlier version asked headerContains for 100-continue and answered
            // it, so "Expect: 100-continue, custom-extension" got its interim
            // response and the extension nobody can satisfy was ignored -- leniency
            // chosen on purpose and wrong, because the field is a list of things
            // the client expects to hold and this server cannot make one of them
            // true.
            //
            // An expectation this server does not know is answered NOW rather than
            // ignored, whichever way it arrives. The mechanism exists so the client
            // waits to be told before sending, so ignoring the field and dropping
            // into the body read leaves both sides waiting for each other until the
            // receive deadline -- the client for a reply it was invited to expect,
            // the server for a body that is not coming. 417 is what RFC 9110
            // provides, and it costs one round trip instead of a timeout.
            if(!onlyExpects100Continue(expectation)) {
                throw new ProtocolException(417, "unsupported expectation");
            }
            // The client is entitled to this before sending the body. A server
            // that stays silent makes every such client pay its whole timeout
            // first.
            conn.write(CONTINUE_100);
        }

        String body = null;
        if(chunked) {
            byte[] decoded = readChunked(conn, scratch);
            if(decoded == null) {
                return null;
            }
            if(decoded.length > 0 && !Utf8.isValid(decoded, 0, decoded.length)) {
                throw new ProtocolException(400, "the request body is not valid UTF-8");
            }
            body = decoded.length == 0 ? null : new String(decoded, "UTF-8");
        } else if(contentLength != null) {
            // sliceToInt returns -1 for anything that is not a plain non-negative
            // decimal, which covers the malformed and the negative cases the two
            // separate checks here used to make after parsing.
            if(declaredLength < 0) {
                throw new ProtocolException(400, "malformed Content-Length");
            }
            if(declaredLength > MAX_BODY_BYTES) {
                throw new ProtocolException(413, "request body too large");
            }
            if(!conn.fillTo(declaredLength)) {
                return null;
            }
            if(declaredLength > 0) {
                // Checked before it is decoded. new String replaces a malformed
                // sequence with U+FFFD rather than failing, so without this the
                // handler is handed text the client never sent -- and whatever
                // validated it validated the replacement. Note the query-string
                // decoder above does the same thing with percent-decoded bytes;
                // that one is left alone deliberately, because refusing a query
                // parameter is a different policy from refusing a body, and no
                // report has been made against it.
                if(!Utf8.isValid(conn.buffer, conn.pos, declaredLength)) {
                    throw new ProtocolException(400, "the request body is not valid UTF-8");
                }
                body = new String(conn.buffer, conn.pos, declaredLength, "UTF-8");
                conn.pos += declaredLength;
            }
        }
        // The body is the only field not known when the header block was parsed.
        // Both branches below run during PARSING -- before this Request is handed
        // to a handler -- so neither one mutates anything a handler can see, and
        // "immutable to its handler" is preserved either way. The slices and the
        // array are shared, not copied.
        if(body == null) {
            return request;
        }
        if(POOL_REQUEST) {
            request.reset(conn, method, target, version, raw, slices, headerCount, body,
                    sliceStart, sliceLength);
            return request;
        }
        return new Request(method, target, version, raw, slices, headerCount, body,
                sliceStart, sliceLength);
    }

    /**
     * Decodes a chunked body: a size in hex, CRLF, that many bytes, CRLF, until a
     * zero-length chunk. The trailer section after it is consumed and discarded --
     * ignoring trailers is allowed, but leaving them in the stream would
     * desynchronise the next request on a keep-alive connection.
     */
    private byte[] readChunked(Conn conn, byte[] scratch) throws IOException {
        // Charged against the SAME process-wide budget the fixed-length path uses.
        // Bounding only that path left this one open: a chunked body is capped per
        // request at MAX_BODY_BYTES and by nothing at all across requests, so
        // enough unauthenticated clients sending almost 8 MiB each and pausing
        // before the terminating chunk retain gigabytes until their rate deadlines
        // expire, with CN1_HTTP_MAX_UPLOAD_MB looking on.
        //
        // The figure charged is what this read is RETAINING: the chunks already
        // accumulated plus what is buffered on the connection for the chunk in
        // progress. Charging only the first would miss the second, which grows to
        // a whole chunk -- the same "fixed one of the two" that made this comment
        // necessary in the first place.
        long[] charged = { 0 };
        try {
        // A ByteSink rather than a ByteArrayOutputStream, because the reservation
        // below has to follow the ALLOCATION and a stream does not say how big its
        // array is. Both double when they grow, so charging the logical body left
        // roughly half of a grown buffer uncounted: a client crossing a growth
        // boundary and then pausing held about 8MB against a 4MB charge, and enough
        // connections doing it hold close to twice CN1_HTTP_MAX_UPLOAD_MB while the
        // guard believes it is inside the limit. bytes().length is the capacity, so
        // it can be charged for what it really is.
        ByteSink body = new ByteSink(1024);
        // The same floor rate the fixed-length path got, over the WHOLE chunked
        // read: the size lines, the data and the trailers. Each of the three fill
        // loops below restarts the socket timeout on every successful read, so a
        // client sending a one-byte chunk just inside each window could hold a
        // worker for years before the 8 MiB cap ever came into view -- and in pool
        // mode, which is what TLS uses, enough of those are the server. Bounding
        // only the fixed-length path left this one open.
        long started = System.currentTimeMillis();
        while(true) {
            int lineEnd = indexOfCrLf(conn.buffer, conn.pos);
            while(lineEnd < 0) {
                // A chunk-size line with no CRLF would otherwise be read for ever:
                // fill() reallocates and copies what is already buffered, and none
                // of it counts toward MAX_BODY_BYTES because no body byte has been
                // framed yet. Metadata gets the same ceiling the header block has.
                if(conn.available() > MAX_HEADER_BYTES) {
                    throw new ProtocolException(400, "chunk size line too long");
                }
                requireChunkedProgress(started, body.length() + conn.available());
                reserveUploadUpTo(charged, body.length() + conn.available());
                if(!conn.fill(scratch)) {
                    return null;
                }
                lineEnd = indexOfCrLf(conn.buffer, conn.pos);
            }
            // NO CONTROL BYTE ANYWHERE ON THE LINE, checked before any of it is
            // discarded. The extension after ';' is thrown away unread, so a bare
            // LF inside one was swallowed with it -- this parser scans on to the
            // next CRLF, while an intermediary that ends a chunk line at LF stops
            // there and reads the rest as chunk data or as the next request. That
            // is the same disagreement the trailer loop below refuses, and the
            // comment under this one already claims this line refuses it too.
            if(hasControlByte(conn.buffer, conn.pos, lineEnd)) {
                throw new ProtocolException(400, "control character in a chunk size line");
            }
            String sizeLine = new String(conn.buffer, conn.pos, lineEnd - conn.pos, "UTF-8");
            // A chunk-size may carry extensions after a ';'; the size is before it.
            int semi = sizeLine.indexOf(';');
            if(semi >= 0) {
                sizeLine = sizeLine.substring(0, semi);
            }
            // 1*HEXDIG, on the raw text. Integer.parseInt(_, 16) took a sign and
            // any Unicode digit, so "+1" and U+0661 both framed a one-byte chunk
            // and "-0" framed the TERMINATING one -- while the intermediary in
            // front rejects all three. A server that frames a message differently
            // from the proxy ahead of it is the whole of request smuggling, and
            // this parser refuses bare LF and obsolete folding for exactly that
            // reason. No trim either: HTTP does not allow space around the size.
            int size = Hex.parse(sizeLine, 0, sizeLine.length());
            if(size < 0) {
                throw new ProtocolException(400, "malformed chunk size");
            }
            conn.pos = lineEnd + 2;
            if(size == 0) {
                // Trailers, terminated by a bare CRLF. Bounded in total, not per
                // line: an endless run of short well-formed trailers costs exactly
                // as much memory as one endless line.
                int trailerBytes = 0;
                while(true) {
                    int trailerEnd = indexOfCrLf(conn.buffer, conn.pos);
                    while(trailerEnd < 0) {
                        if(conn.available() > MAX_HEADER_BYTES) {
                            throw new ProtocolException(400, "chunk trailer too long");
                        }
                        requireChunkedProgress(started, body.length() + conn.available());
                        reserveUploadUpTo(charged, body.length() + conn.available());
                        if(!conn.fill(scratch)) {
                            // EOF before the blank line that ends the trailers: the
                            // chunked framing never finished, so this is a truncated
                            // message, not a complete one. Returning the body here
                            // ran the handler on it -- and for a mutating request
                            // that means committing half a message. The fixed-length
                            // and chunk-data paths both return null; so does this.
                            return null;
                        }
                        trailerEnd = indexOfCrLf(conn.buffer, conn.pos);
                    }
                    trailerBytes += (trailerEnd - conn.pos) + 2;
                    if(trailerBytes > MAX_HEADER_BYTES) {
                        throw new ProtocolException(400, "chunk trailers too large");
                    }
                    boolean blank = trailerEnd == conn.pos;
                    if(!blank) {
                        // THE SAME CHECKS THE HEADER BLOCK GETS, and for the same
                        // reason. This loop finds the end of a trailer by scanning
                        // for CRLF, so a bare LF inside one was just a byte to it:
                        // a zero chunk followed by "X: v\n\nGET /next ..." ended
                        // the trailer section at an intermediary that treats LF as
                        // a delimiter, while this consumed the whole thing --
                        // including the next request -- as opaque trailer text. The
                        // two then disagree about where the next request starts on
                        // a reused connection, which is the whole of smuggling. The
                        // header block above refuses exactly this shape; a trailer
                        // is a header field and gets the same treatment.
                        int colon = -1;
                        for(int scan = conn.pos ; scan < trailerEnd ; scan++) {
                            if(conn.buffer[scan] == ':') {
                                colon = scan;
                                break;
                            }
                        }
                        // No colon covers obsolete folding too: a continuation line
                        // begins with space and carries none.
                        if(colon < 0 || !isRequestHeaderName(conn.buffer, conn.pos, colon)) {
                            throw new ProtocolException(400, "malformed trailer name");
                        }
                        int valueStart = colon + 1;
                        while(valueStart < trailerEnd && (conn.buffer[valueStart] == ' '
                                || conn.buffer[valueStart] == '\t')) {
                            valueStart++;
                        }
                        if(hasControlByte(conn.buffer, valueStart, trailerEnd)) {
                            throw new ProtocolException(400,
                                    "control character in a trailer value");
                        }
                    }
                    conn.pos = trailerEnd + 2;
                    if(blank) {
                        return usedBytes(body);
                    }
                }
            }
            // Subtraction, not addition: body.length() + size overflows to a negative
            // for a chunk size near Integer.MAX_VALUE and sails past the cap, after
            // which the loop below grows the buffer toward the declared multi-gigabyte
            // chunk. Four bytes and a "7ffffffd" header was enough for an
            // unauthenticated client to take the process out. Both sides here are
            // non-negative, so there is nothing left to overflow.
            if(size > MAX_BODY_BYTES - body.length()) {
                throw new ProtocolException(413, "chunked body too large");
            }
            // The chunk and its trailing CRLF must both be present before it is taken.
            while(conn.available() < size + 2) {
                requireChunkedProgress(started, body.length() + conn.available());
                reserveUploadUpTo(charged, body.length() + conn.available());
                if(!conn.fill(scratch)) {
                    return null;
                }
            }
            // Reserved for the copy BEFORE it is made, like every other growth
            // point: a budget checked afterwards has already spent what it meant
            // to withhold.
            // Reserved against the capacity this write will leave behind, not the
            // bytes it adds: ensure() doubles, and the doubling is the memory.
            body.ensure(size);
            reserveUploadUpTo(charged, body.bytes().length + conn.available());
            body.put(conn.buffer, conn.pos, size);
            conn.pos += size;
            if(conn.buffer[conn.pos] != '\r' || conn.buffer[conn.pos + 1] != '\n') {
                throw new ProtocolException(400, "malformed chunk terminator");
            }
            conn.pos += 2;
        }
        } finally {
            // Every path out, exactly like the fixed-length reader: the body
            // arrived, the peer went away, the deadline passed or the process was
            // full. On success the bytes become the request's and stop being an
            // upload in flight.
            http1UploadBytes.addAndGet(-charged[0]);
        }
    }

    /**
     * Tops a reservation up to what the caller is now holding.
     *
     * The running total is in the array so that the charge is recorded BEFORE the
     * ceiling is tested: if this throws, the caller's finally still releases what
     * was just taken. Recording it afterwards leaks the last reservation of every
     * refused upload, which is the slowest possible way to run a server out of
     * budget.
     */
    /**
     * The filled prefix of a sink, copied out.
     *
     * ByteSink.bytes() is the whole backing array -- capacity, not content -- so
     * handing it to a caller that reads buffer.length would hand it the padding
     * too.
     */
    private static byte[] usedBytes(ByteSink sink) {
        byte[] out = new byte[sink.length()];
        System.arraycopy(sink.bytes(), 0, out, 0, out.length);
        return out;
    }

    /**
     * Whether an Expect field asks for nothing but 100-continue.
     *
     * <p>It is a comma-separated list, and one member this server cannot satisfy
     * makes the whole field unsatisfiable -- there is no partial answer to give.
     * Compared without case folding a token by hand: equalsIgnoreCase is
     * locale-independent, which toLowerCase is not.
     */
    private static boolean onlyExpects100Continue(String value) {
        int at = 0;
        boolean any = false;
        while(at <= value.length()) {
            int comma = value.indexOf(',', at);
            int end = comma < 0 ? value.length() : comma;
            String token = value.substring(at, end).trim();
            if(token.length() > 0) {
                any = true;
                if(!"100-continue".equalsIgnoreCase(token)) {
                    return false;
                }
            }
            if(comma < 0) {
                break;
            }
            at = comma + 1;
        }
        return any;
    }

    private static void reserveUploadUpTo(long[] charged, long needed)
            throws ProtocolException {
        if(needed <= charged[0]) {
            return;
        }
        long delta = needed - charged[0];
        charged[0] = needed;
        if(http1UploadBytes.addAndGet(delta) > MAX_HTTP1_UPLOAD_BYTES) {
            throw new ProtocolException(503, "too many uploads in flight");
        }
    }

    /**
     * Refuses a chunked body that is not arriving at the floor rate.
     *
     * The total is not declared, so the allowance is computed from what has
     * ARRIVED: at any moment the elapsed time may be one socket timeout plus what
     * those bytes take at MIN_BODY_BYTES_PER_SECOND. A slow but progressing upload
     * keeps earning time; one that has stopped delivering does not.
     *
     * "Arrived" includes what is BUFFERED for the chunk in progress, not just the
     * chunks already complete. Counting only completed chunks meant one legal
     * large chunk earned no time at all while it streamed: a 1 MiB chunk at four
     * times the floor rate was cut off with a 408 after about fifteen seconds,
     * because the total stayed zero until the whole of it had landed.
     */
    private static void requireChunkedProgress(long started, int received)
            throws ProtocolException {
        long allowed = SOCKET_TIMEOUT_MILLIS
                + (long)received * 1000L / MIN_BODY_BYTES_PER_SECOND;
        if(System.currentTimeMillis() - started > allowed) {
            throw new ProtocolException(408, "the chunked body did not arrive in time");
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
     * The Date header value, formatted at most once a second.
     *
     * The header has one-second resolution, so formatting it per response is work
     * whose result is identical for every request in the same second -- and at
     * these rates that is thousands of them. Two threads racing here both compute
     * the same string for the same second, so the only cost of the race is a
     * duplicated format, never a wrong value.
     */
    private static volatile long dateStampSecond = -1;
    private static volatile String dateStampValue;
    /**
     * The same stamp as bytes, so writing it costs a copy rather than a
     * per-character conversion. An HTTP date is fixed width and ASCII, which is
     * what makes the length a constant.
     */
    private static volatile byte[] dateStampBytes = new byte[HTTP_DATE_LENGTH];

    static String currentHttpDate() {
        refreshHttpDate();
        return dateStampValue;
    }

    static byte[] currentHttpDateBytes() {
        refreshHttpDate();
        return dateStampBytes;
    }

    private static void refreshHttpDate() {
        long millis = System.currentTimeMillis();
        long second = millis / 1000L;
        if(second != dateStampSecond) {
            String formatted = Http1Date.format(second * 1000L);
            byte[] bytes = new byte[HTTP_DATE_LENGTH];
            // Fixed width by construction; a formatter that ever returned another
            // length would otherwise write a short or truncated date silently.
            if(formatted.length() != HTTP_DATE_LENGTH) {
                throw new IllegalStateException("HTTP date is not "
                        + HTTP_DATE_LENGTH + " characters: " + formatted);
            }
            for(int iter = 0 ; iter < HTTP_DATE_LENGTH ; iter++) {
                bytes[iter] = (byte)formatted.charAt(iter);
            }
            dateStampValue = formatted;
            dateStampBytes = bytes;
            dateStampSecond = second;
        }
    }

    private void writeResponse(Conn conn, int fd, long session, Response response,
            boolean keepAlive, boolean headOnly) throws IOException {
        // A deferred JSON body is serialised FIRST: Content-Length has to be
        // written before it, and the only honest way to know it is to have the
        // bytes. Into a second reusable buffer rather than the head's, because
        // the head is not built yet.
        byte[] deferred = null;
        int deferredLength = 0;
        if(response.hasDeferredJson) {
            conn.bodySink.reset();
            Json.write(response.deferredJson, conn.bodySink);
            deferred = conn.bodySink.bytes();
            deferredLength = conn.bodySink.length();
        }
        long bodyLength = response.fileFd >= 0 ? response.fileLength
                : (deferred != null ? deferredLength : response.body.length);
        // HEAD is not the only thing that suppresses a body; see statusForbidsBody.
        boolean noBody = headOnly || statusForbidsBody(response.status);
        boolean noLength = statusForbidsLength(response.status);
        // A HEAD and a bodiless STATUS are suppressed for different reasons and
        // must advertise different lengths. HEAD describes the representation it
        // is not sending, so it keeps the real figure. A 205 has no
        // representation to describe -- it tells the client to clear its form --
        // so it advertises zero. Reporting the suppressed body's length there
        // would leave a keep-alive client waiting for bytes that never come.
        if(noBody && !headOnly) {
            bodyLength = 0;
        }

        // Assembled into the connection's own buffer, as bytes, with no
        // intermediate String. See Conn.out: the StringBuilder-to-String-to-bytes
        // chain this replaces was the largest single source of allocation in the
        // server, and the buffer is reused for the life of the connection.
        conn.reset();
        // Pre-encoded, not re-encoded per response. put(String) walks the string
        // one charAt at a time; these literals are 82 of the ~97 characters a
        // plaintext 200 emits, so writing them that way spent 51 MILLION charAt
        // calls a second at this server's throughput to reproduce bytes that never
        // change. put(byte[]) is a System.arraycopy. Measured before this: 1.36us
        // of user CPU per request against fasthttp's 0.74us, with system time at
        // parity -- the gap was all in our own code, and this is the largest
        // identifiable piece of it.
        if(FAST_HEADERS) {
            if(response.status == 200) {
                conn.put(H_STATUS_200, 0, H_STATUS_200.length);   // overwhelmingly the common case
            } else {
                conn.put(H_VERSION, 0, H_VERSION.length);
                conn.putNumber(response.status);
                conn.put(' ');
                conn.put(reason(response.status));
            }
            conn.put(H_CTYPE, 0, H_CTYPE.length);
            // The same default HTTP/2 applies. The public Response constructor lets a
            // handler pass null, and reaching putContentType with it threw an NPE that
            // dropped the connection without a response -- so one handler behaved two
            // ways depending on the protocol it happened to be answering.
            conn.putContentType(safeContentType(response.contentType));
            // RFC 9110 6.6.1: an origin server with a clock MUST send Date.
            conn.put(H_DATE, 0, H_DATE.length);
            conn.put(currentHttpDateBytes(), 0, HTTP_DATE_LENGTH);
            // Always an explicit length: without it a keep-alive client waits for
            // a close that is not coming. The exception is a status the spec says
            // must not carry one, where the absent header IS the framing.
            if(!noLength) {
                conn.put(H_CLEN, 0, H_CLEN.length);
                conn.putNumber(bodyLength);
            }
            if(keepAlive) {
                conn.put(H_KEEPALIVE, 0, H_KEEPALIVE.length);
            } else {
                conn.put(H_CLOSE, 0, H_CLOSE.length);
            }
        } else {
            // The per-character path this replaces, kept so the two can be
            // measured against each other in one binary.
            conn.put("HTTP/1.1 ");
            conn.putNumber(response.status);
            conn.put(' ');
            conn.put(reason(response.status));
            conn.put("\r\nContent-Type: ");
            // Through the same guard as the fast path above. This branch is the
            // measurement copy, and it had BOTH defects that branch was fixed for: a
            // null content type reached it as an NPE, and a CR/LF one as a second
            // header. A path kept for comparison is still a path that serves.
            conn.put(safeContentType(response.contentType));
            conn.put("\r\nDate: ");
            conn.put(currentHttpDateBytes(), 0, HTTP_DATE_LENGTH);
            if(!noLength) {
                conn.put("\r\nContent-Length: ");
                conn.putNumber(bodyLength);
            }
            conn.put(keepAlive ? "\r\nConnection: keep-alive" : "\r\nConnection: close");
        }
        if(response.extraHeaders != null) {
            java.util.Iterator it = response.extraHeaders.keySet().iterator();
            while(it.hasNext()) {
                Object key = it.next();
                Object value = response.extraHeaders.get(key);
                if(key != null && value != null) {
                    String name = String.valueOf(key);
                    String text = String.valueOf(value);
                    // A CR or LF here ENDS the field and starts another, so a value
                    // built from request data -- a decoded query parameter reaches a
                    // handler with real CRLF in it if the client sent %0d%0a -- lets
                    // the client write its own headers, or a second response. That is
                    // response splitting, and it is a cache-poisoning primitive.
                    // Dropped rather than escaped: there is no correct escaping, and a
                    // header the handler could not have meant is not worth sending.
                    if(isServerOwnedHeader(name)) {
                        System.err.println("dropped a response header the server owns: "
                                + sanitizeForLog(name));
                    } else if(isHeaderName(name) && isHeaderSafe(text)) {
                        conn.put("\r\n");
                        conn.put(name);
                        conn.put(": ");
                        conn.put(text);
                    } else {
                        System.err.println("dropped a response header whose name is "
                                + "not a token or whose value carries a control character: "
                                + sanitizeForLog(name));
                    }
                }
            }
        }
        if(FAST_HEADERS) {
            conn.put(H_END, 0, H_END.length);
        } else {
            conn.put("\r\n\r\n");
        }

        // Head and body in ONE write when the body is small and already in memory.
        // Two writes are two syscalls and, on a fresh connection, two segments: the
        // client sees the headers, acknowledges, and only then gets the body.
        // Measured against Go, which does one write per response, this was half of
        // our remaining syscall count per request. Above the threshold the copy
        // would cost more than the syscall it saves, and a file body never enters
        // user space at all -- both keep the two-write path.
        if(deferred != null) {
            // THE SAME LIMIT THE ORDINARY BODY GETS. This path used to copy any
            // deferred body into the head buffer, however large: a big JSON result
            // was then held twice, once in bodySink where it was serialised and
            // again in an "out" that had grown to fit it, and both keep their peak
            // for the life of the connection. Above the limit the copy costs more
            // than the syscall it saves anyway, which is why the branch below
            // stops there.
            if(!noBody && deferredLength > 0 && deferredLength <= COMBINED_WRITE_LIMIT) {
                conn.put(deferred, 0, deferredLength);
                writeTo(fd, session, conn.out, 0, conn.outLength);
                return;
            }
            writeTo(fd, session, conn.out, 0, conn.outLength);
            if(!noBody && deferredLength > 0) {
                writeTo(fd, session, deferred, 0, deferredLength);
            }
            return;
        }
        if(response.fileFd < 0 && !noBody
                && response.body.length > 0
                && response.body.length <= COMBINED_WRITE_LIMIT) {
            conn.put(response.body, 0, response.body.length);
            writeTo(fd, session, conn.out, 0, conn.outLength);
            return;
        }
        writeTo(fd, session, conn.out, 0, conn.outLength);

        if(response.fileFd >= 0) {
            try {
                if(!noBody) {
                    StaticFiles.sendBody(fd, session, response.fileFd, response.fileOffset, response.fileLength);
                }
            } finally {
                // The server owns the descriptor once a handler hands it over, so
                // this is the only place it is closed -- including when the send
                // failed halfway.
                StaticFiles.closeFile(response.fileFd);
            }
            return;
        }
        if(!noBody && response.body.length > 0) {
            writeTo(fd, session, response.body, 0, response.body.length);
        }
    }

    /** Pre-encoded: this goes out on the body path of every expecting client. */
    private static final byte[] CONTINUE_100 = asciiBytes("HTTP/1.1 100 Continue\r\n\r\n");

    /**
     * The response head's fixed bytes, encoded once at class init instead of
     * character by character per response. CN1_HTTP_FAST_HEADERS=0 restores the
     * per-character path, which is what the measurement compares against.
     */
    private static final boolean FAST_HEADERS = envInt("CN1_HTTP_FAST_HEADERS", 1) != 0;
    private static final byte[] H_STATUS_200 = asciiBytes("HTTP/1.1 200 OK");
    private static final byte[] H_VERSION    = asciiBytes("HTTP/1.1 ");
    private static final byte[] H_CTYPE      = asciiBytes("\r\nContent-Type: ");
    private static final byte[] H_DATE       = asciiBytes("\r\nDate: ");
    private static final byte[] H_CLEN       = asciiBytes("\r\nContent-Length: ");
    private static final byte[] H_KEEPALIVE  = asciiBytes("\r\nConnection: keep-alive");
    private static final byte[] H_CLOSE      = asciiBytes("\r\nConnection: close");
    private static final byte[] H_END        = asciiBytes("\r\n\r\n");

    private static byte[] asciiBytes(String value) {
        byte[] out = new byte[value.length()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte)value.charAt(iter);
        }
        return out;
    }

    /**
     * True when the joined Transfer-Encoding list ends in `chunked` and carries
     * nothing this server cannot decode.
     *
     * Throws rather than returning false for a list it will not act on: silently
     * ignoring a coding leaves the body to be read as the next request on the
     * connection, which is the smuggling case this exists to close.
     */
    private static boolean requireChunkedIsFinalCoding(String value)
            throws ProtocolException {
        String[] codings = splitOn(value, ',');
        int seen = 0;
        for(int iter = 0 ; iter < codings.length ; iter++) {
            String coding = codings[iter].trim();
            // A transfer coding may carry parameters after a semicolon; the coding
            // itself is what decides the framing.
            int semi = coding.indexOf(';');
            if(semi >= 0) {
                coding = coding.substring(0, semi).trim();
            }
            if(coding.length() == 0) {
                continue;
            }
            if(!"chunked".equalsIgnoreCase(coding)) {
                throw new ProtocolException(501, "unsupported transfer coding");
            }
            if(iter != codings.length - 1) {
                throw new ProtocolException(400, "chunked is not the final transfer coding");
            }
            seen++;
        }
        if(seen == 0) {
            throw new ProtocolException(400, "empty Transfer-Encoding");
        }
        return true;
    }

    private static boolean isSpace(byte b) {
        return b == ' ' || b == '\t';
    }

    static int indexOfByte(byte[] data, int from, int to, byte wanted) {
        for(int iter = from ; iter < to ; iter++) {
            if(data[iter] == wanted) {
                return iter;
            }
        }
        return -1;
    }

    static int indexOfCrLfWithin(byte[] data, int from, int to) {
        for(int iter = from ; iter + 1 < to ; iter++) {
            if(data[iter] == '\r' && data[iter + 1] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    static boolean sliceStartsWithIgnoreCase(byte[] data, int start, int length, String ascii) {
        return length >= ascii.length()
                && sliceEqualsIgnoreCase(data, start, ascii.length(), ascii);
    }

    static boolean slicesEqual(byte[] data, int aStart, int aLength, int bStart, int bLength) {
        if(aLength != bLength) {
            return false;
        }
        for(int iter = 0 ; iter < aLength ; iter++) {
            if(data[aStart + iter] != data[bStart + iter]) {
                return false;
            }
        }
        return true;
    }

    // ---- byte-slice helpers -------------------------------------------------
    //
    // Header names and the tokens compared against them are ASCII by definition
    // (RFC 9110 field-name is a token), so a byte-wise comparison with an ASCII
    // fold is exact -- no locale, no decoding, no allocation. These are what let
    // the request path answer "is this connection keep-alive" without building a
    // String.

    private static int foldAscii(int c) {
        return c >= 'A' && c <= 'Z' ? c + ('a' - 'A') : c;
    }

    /**
     * The case-folded bytes of an ASCII string, or null if it is not ASCII.
     *
     * Cached per String IDENTITY, because the callers pass literals: "connection"
     * at a given call site is the same object every time, so the fold happens once
     * for the life of the process rather than once per header per request. A miss
     * simply folds again -- the cache is a hint, never a correctness dependency,
     * which is what lets it stay lock-free.
     */
    /**
     * Case-folded header names, packed END TO END in ONE byte[].
     *
     * Flat on purpose. An array of arrays scatters every entry across the heap and
     * costs a pointer chase per lookup; this holds all of them contiguously, so a
     * comparison walks memory the prefetcher already has. It is also one object for
     * the collector to mark instead of seventeen.
     *
     * Keyed by String IDENTITY, because the callers pass literals -- "connection" at
     * a given call site is the same object every time, so the fold happens once for
     * the life of the process rather than once per header per request. A miss simply
     * folds again: the cache is a hint, never a correctness dependency, which is
     * what lets it stay lock-free.
     */
    private static final int FOLD_CACHE_SLOTS = 16;

    /**
     * One cached fold. Both fields are final, which is the whole point.
     *
     * The cache used to be three parallel static arrays and a rotating index, and
     * a lookup returned the SLOT it had matched. The caller then compared against
     * that slot while walking the request's headers -- a window in which another
     * worker could retire the slot and write a different name into its bytes. Two
     * names of equal length are then indistinguishable, so getHeader answered
     * with the wrong field, or reported a header that was sent as absent, and
     * nothing threw. Clearing the key first does not help a reader that already
     * holds the index.
     *
     * Handing back an immutable entry closes that by construction: what the
     * caller compares against cannot be rewritten, because nothing ever writes to
     * a published entry.
     */
    private static final class Folded {
        final String key;
        final byte[] bytes;

        Folded(String key, byte[] bytes) {
            this.key = key;
            this.bytes = bytes;
        }
    }

    /**
     * Published by replacement, never by mutation, so a reader either sees an
     * entry complete or does not see it at all. Two threads that fold the same
     * name at once may lose one of the two writes; that costs a later refold and
     * nothing else, which is what keeps this lock free.
     */
    private static volatile Folded[] foldCache = new Folded[0];

    /**
     * The folded bytes of `ascii`, or null when it cannot be cached -- not ASCII,
     * or the cache is full. Null means the caller takes the general path, which
     * is only slower.
     */
    static byte[] foldedBytes(String ascii) {
        Folded[] snapshot = foldCache;
        for(int iter = 0 ; iter < snapshot.length ; iter++) {
            // Identity, not equals: a given call site hands over the same constant
            // every time, so this is a pointer compare and the fold happens once
            // for the life of the process.
            if(snapshot[iter].key == ascii) {
                return snapshot[iter].bytes;
            }
        }
        if(snapshot.length >= FOLD_CACHE_SLOTS) {
            return null;
        }
        int length = ascii.length();
        for(int iter = 0 ; iter < length ; iter++) {
            if(ascii.charAt(iter) > 127) {
                return null;
            }
        }
        byte[] bytes = new byte[length];
        for(int iter = 0 ; iter < length ; iter++) {
            bytes[iter] = (byte) foldAscii(ascii.charAt(iter));
        }
        Folded[] grown = new Folded[snapshot.length + 1];
        System.arraycopy(snapshot, 0, grown, 0, snapshot.length);
        grown[snapshot.length] = new Folded(ascii, bytes);
        foldCache = grown;
        return bytes;
    }

    /** Case-insensitive compare of a slice against already-folded needle bytes. */
    static boolean sliceEqualsFolded(byte[] data, int start, int length, byte[] needle) {
        if(length != needle.length) {
            return false;
        }
        for(int iter = 0 ; iter < length ; iter++) {
            if(foldAscii(data[start + iter] & 0xff) != needle[iter]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Folded compare against a constant that is ALREADY folded, so only the bytes
     * that arrived off the socket have to be folded here.
     */
    static boolean sliceEqualsIgnoreCase(byte[] data, int start, int length, byte[] asciiLower) {
        if(length != asciiLower.length) {
            return false;
        }
        for(int iter = 0 ; iter < length ; iter++) {
            if(foldAscii(data[start + iter] & 0xff) != (asciiLower[iter] & 0xff)) {
                return false;
            }
        }
        return true;
    }

    static boolean sliceEqualsIgnoreCase(byte[] data, int start, int length, String ascii) {
        if(length != ascii.length()) {
            return false;
        }
        for(int iter = 0 ; iter < length ; iter++) {
            if(foldAscii(data[start + iter] & 0xff) != foldAscii(ascii.charAt(iter))) {
                return false;
            }
        }
        return true;
    }

    static boolean sliceContainsIgnoreCase(byte[] data, int start, int length, String ascii) {
        int needle = ascii.length();
        if(needle == 0 || needle > length) {
            return needle == 0;
        }
        int last = start + length - needle;
        for(int at = start ; at <= last ; at++) {
            int iter = 0;
            while(iter < needle
                    && foldAscii(data[at + iter] & 0xff) == foldAscii(ascii.charAt(iter))) {
                iter++;
            }
            if(iter == needle) {
                return true;
            }
        }
        return false;
    }

    /**
     * A non-negative decimal from a slice, or -1 when it is not one.
     *
     * Integer.parseInt would need a String first, which is the allocation this
     * whole representation exists to avoid -- and it is on the path of every
     * request that carries a body.
     */
    static int sliceToInt(byte[] data, int start, int length) {
        if(length <= 0 || length > 10) {
            return -1;
        }
        long value = 0;
        for(int iter = 0 ; iter < length ; iter++) {
            int c = data[start + iter] & 0xff;
            if(c < '0' || c > '9') {
                return -1;
            }
            value = value * 10 + (c - '0');
            if(value > Integer.MAX_VALUE) {
                return -1;
            }
        }
        return (int)value;
    }

    /**
     * The request target as a String, memoised PER CONNECTION.
     *
     * A connection asks for the same handful of targets over and over, so this is a
     * hit almost every time and the steady state allocates nothing. A miss does
     * exactly what the code did before and is only slower, never wrong.
     *
     * Worth doing because the target was the last per-request String on the
     * plaintext path, and it cost three objects rather than one: asciiString builds
     * a char[] and String's public constructor copies it into a second.
     *
     * PER CONNECTION rather than one shared static table, and that is a
     * correctness requirement rather than a preference. `java.lang.String.value` is
     * NOT final in this runtime (only offset and count are), so a String published
     * through an unsynchronised static array can be observed by another worker with
     * a null value -- on arm64 that is a real reordering, not a theoretical one. A
     * Conn reaches its next worker through the executor, which gives the
     * happens-before edge this needs for free.
     *
     * Bounded, because targets are attacker controlled: a query string or path
     * parameter makes every request unique. Past the cap it stops inserting and
     * every miss allocates as before -- a performance cliff, never a memory one.
     */
    // DEFAULT OFF. Measured against the same binary at 16 connections, the cache
    // is worth +24% when the zero-copy read is on (159,291 vs 128,111) and -6%
    // when it is off (172,681 vs 183,406). Since the zero-copy read is itself off
    // by default, the case that applies is the one where this costs throughput.
    // Kept behind a switch rather than deleted because the allocation it removes is
    // real -- 2 char[] and a String per request -- and a cheaper lookup might yet
    // win; what is NOT supported is turning it on without re-measuring.
    /**
     * On by default. Sixty-four slots per connection, one reference each.
     *
     * With this at 0 internTarget takes its disabled path and calls asciiString
     * for EVERY request, which allocates a char[], a String and the String's own
     * storage. A per-class allocation profile of /plaintext at 64 connections put
     * char[] + String + byte[] at 57% of all bytes allocated -- 424MB, 123MB and
     * 302MB against a 1.49GB total -- and the request target is the only thing
     * left materialising on that path once the keep-alive check stopped doing it.
     *
     * A benchmark client sends a handful of distinct targets, and a real service
     * has a bounded route set, so the slot array is small and the hit rate is
     * high. 64 references per connection is 512 bytes, against the ~180 bytes per
     * REQUEST the miss path was costing.
     *
     * MEASURED: paired A/B, arms alternated inside each rep, six readings at 64
     * and 256 connections -- +7% to +13% throughput, 6 of 6 in favour, p99 better
     * in 5 of 6, and allocation 639 -> 411 bytes per request with String
     * allocations falling from 2,575,425 to 542. The backend suite passed twice.
     *
     * NOT MEASURED: a workload whose targets are all DISTINCT, which is what
     * query strings produce and what an attacker can force. Five attempts to
     * measure it failed for harness reasons rather than server ones -- 404s reply
     * Connection: close so varied targets tore down the connection, and driving
     * wrk from Lua produced 1.5M write errors against 20k requests. The arm is
     * still worth building if this ever looks suspect.
     *
     * What the MISS path costs, from the code rather than a measurement: a hash
     * over the target, a length compare that fails immediately, then exactly the
     * asciiString the disabled path performs, plus a reference store. So a miss
     * adds one pass over a short byte range and allocates nothing extra -- it
     * cannot allocate MORE than the cache being off, because it stores the very
     * String that path would have created. That bounds the worst case to a small
     * constant, which is why this ships on rather than off.
     *
     * CN1_HTTP_TARGET_CACHE=0 restores the old behaviour for A/B.
     */
    private static final int TARGET_CACHE_SLOTS =
            envIntAtLeast("CN1_HTTP_TARGET_CACHE", 64, 0);

    /**
     * The longest target worth memoising, and worth HOLDING.
     *
     * A route repeated on one connection is short; a target near MAX_HEADER_BYTES
     * is not a route, it is a query string, and caching sixty-four of those would
     * let a client that never sends a body hold megabytes per connection for as
     * long as it keeps the connection open. 512 leaves ample room for a real path
     * with parameters and none for that.
     */
    private static final int MAX_CACHED_TARGET_BYTES = 512;

    /**
     * The largest per-connection buffer worth carrying across an idle wait.
     *
     * The head buffer and the body sink both grow to fit and never shrink, which
     * is what makes them cheap per request. Across a wait that may last as long as
     * the client likes, a buffer sized by one big response is just retention, so
     * anything past this is dropped and rebuilt. Comfortably above
     * COMBINED_WRITE_LIMIT, so the steady-state buffers survive.
     */
    private static final int MAX_IDLE_BUFFER_BYTES = 16 * 1024;

    /**
     * Read straight into the thread's reusable buffer instead of a fresh array.
     * A switch because it is the kind of change that has to be A/B measurable
     * against the allocation it removes -- an optimisation that costs more than it
     * saves looks exactly like one that works until somebody measures the thing it
     * was supposed to improve.
     */
    /**
     * Read straight into the thread's reusable buffer instead of a fresh array.
     *
     * ON by default. It removes 95% of the per-request byte[] allocations
     * (1.04 to 0.054 per request).
     *
     * MODES, because the throughput answer turned out to depend on the route and
     * two earlier readings here were both wrong:
     *
     *   0  read with recv() into the worker's reusable scratch, then copy into a
     *      fresh byte[] sized to the read. Allocates once per request.
     *   1  read straight into this thread's foreign (off-heap) buffer and parse
     *      where the bytes land. Allocates nothing.
     *   2  DIAGNOSTIC. Identical native to mode 1, identical Java to mode 0: read
     *      into the foreign buffer and immediately copy it into a heap array. It
     *      exists to split mode 1's two differences from mode 0 -- the syscall and
     *      the off-heap object living in a Java field -- because measuring only 0
     *      against 1 cannot say which of them moved the number.
     *
     * Paired measurement on an idle Linux box, same binary, 3 reps, median req/s:
     *
     *   /plaintext   mode 0 = 262961   mode 1 = 233800   mode 1 is 11% SLOWER
     *   /json        mode 0 = 167851   mode 1 = 176876   mode 1 is  5% FASTER
     *
     * That split is the whole reason the modes are here. Earlier comments in this
     * spot claimed first a flat 30% loss and then no cost at all; the first was
     * measured against a machine running a compile, the second was an A/B too
     * noisy to resolve an 11% effect and should have been reported as a failed
     * measurement rather than a result.
     *
     * WHAT THE BISECTION FOUND. Mode 2 lands on mode 0 on BOTH routes -- 250962
     * against 249524 on /plaintext, 159168 against 158972 on /json -- so the
     * native read costs nothing and is not what moved either number. Since mode 2
     * differs from mode 1 only in copying the bytes into a heap array, the whole
     * effect, in both directions, is the cost of keeping a FOREIGN off-heap array
     * in a Java field:
     *
     *   /json        mode 1 gains 5.3% over mode 2 -- the route is allocation
     *                bound, so not allocating a per-request array is worth more
     *                than the collector's extra work.
     *   /plaintext   mode 1 loses 4.2% to mode 2 -- little GC pressure here, so
     *                the saved allocation buys little while the off-heap cost is
     *                paid on every traversal: `Conn.buffer` points outside the
     *                heap, so the fast range check in the mark path fails and the
     *                object has to be resolved as an immortal root instead.
     *
     * The default is therefore a judgement about the workload rather than a fact
     * about the code, which is why the switch is left in place.
     */
    private static final int ZERO_COPY_MODE = envInt("CN1_HTTP_ZERO_COPY", 1);
    /**
     * On under virtual threads too, since the reason it was not is gone.
     *
     * WHAT THIS USED TO SAY, AND WHY IT WAS WRONG. It read that combining the two
     * was unsafe because the zero-copy read hands back a HOST thread's buffer and
     * a virtual thread could resume elsewhere, and it cited an attempt that
     * "passed the virtual-thread suite 21/21 and then produced a TRUNCATED
     * RESPONSE on the dispatching path: authGuardsMutatingRoutes read a reply it
     * could not parse, once".
     *
     * That truncation was not the buffer refactor. It was the missing
     * parsedFromBuffer guard in fill() -- see the comment there, which records
     * the same two symptoms (transactionRollsBack timing out at 15.05s with
     * status -1, authGuardsMutatingRoutes reading an empty body, about 2 runs in
     * 6) and says in as many words that it "never appeared under virtual threads
     * because ZERO_COPY_READ is off there". Turning zero-copy on under virtual
     * threads is exactly what first exposed that bug; the refactor was blamed,
     * reverted, and the real defect found and fixed afterwards without anyone
     * going back to correct the verdict.
     *
     * The sharing hazard is handled by that same guard rather than by keeping the
     * paths apart. Several virtual threads do multiplex onto one host thread, but
     * fill() either releases the borrow when nothing has been parsed out of it or
     * calls detachPreservingOffsets before any second read, so a virtual thread
     * that parks mid-request already owns a private copy and no other thread's
     * read can overwrite live slices.
     *
     * What it costs to leave off: a per-request byte[] copy on every request. The
     * census measured 223 bytes per request when this path was copying and none
     * when it was not, against 662 bytes per request total on /plaintext.
     *
     * CN1_HTTP_ZERO_COPY=0 still disables it entirely.
     */
    private static final boolean ZERO_COPY_READ = ZERO_COPY_MODE != 0;

    /**
     * Reuse one Request per connection instead of allocating one per request.
     *
     * Request and Response were the whole of what /plaintext still allocated once
     * the borrowed-buffer copy went -- 80 and 88 bytes, one of each, every
     * request -- so this is half of what was left. The allocation half is exact
     * and was measured directly: Request disappears from the profile and the
     * route falls from 168.2 to 88.2 bytes per request. Throughput, thirteen
     * interleaved pairs in one binary with the arm order rotating, is a median
     * +13.6% and ahead in 12 of 13, p99 better in 10.
     *
     * A profiled build shows only +2.3% for the same change, and that is not a
     * contradiction: the profiler taxes every allocation, so the server is slower,
     * allocates less per second, and the collector it is being spared matters
     * less. The non-profiled figure is the one that describes a real deployment.
     *
     * CN1_HTTP_POOL_REQUEST=0 restores the allocating path -- kept for the same
     * reason ZERO_COPY_MODE keeps its switch, so the comparison stays runnable
     * rather than having to be rebuilt.
     */
    private static final boolean POOL_REQUEST = envInt("CN1_HTTP_POOL_REQUEST", 1) != 0;

    static String asciiString(byte[] data, int start, int length) {
        char[] chars = new char[length];
        for(int iter = 0 ; iter < length ; iter++) {
            chars[iter] = (char)(data[start + iter] & 0xff);
        }
        return new String(chars);
    }

    static String lowerCaseString(byte[] data, int start, int length) {
        char[] chars = new char[length];
        for(int iter = 0 ; iter < length ; iter++) {
            chars[iter] = (char)foldAscii(data[start + iter] & 0xff);
        }
        return new String(chars);
    }

    /**
     * The interned constant for a known method, or null.
     *
     * Returning a constant rather than a fresh String means the common methods
     * cost nothing, and it makes the identity comparisons elsewhere in this file
     * safe as well as the equals ones.
     */
    static String knownMethod(byte[] data, int start, int length) {
        // The folded compare that used to guard this one was redundant: an EXACT
        // match implies a folded match, so it could only ever agree with the test
        // below it, at the cost of a second walk of the same bytes -- with a
        // foldAscii call per character on both sides -- for every request.
        for(int iter = 0 ; iter < KNOWN_METHOD_BYTES.length ; iter++) {
            if(sliceEquals(data, start, length, KNOWN_METHOD_BYTES[iter])) {
                return KNOWN_METHODS[iter];
            }
        }
        return null;
    }

    /** Exact, not folded: HTTP methods are case SENSITIVE. */
    /** Exact compare against a constant already held as bytes. */
    private static boolean sliceEquals(byte[] data, int start, int length, byte[] ascii) {
        if(length != ascii.length) {
            return false;
        }
        for(int iter = 0 ; iter < length ; iter++) {
            if(data[start + iter] != ascii[iter]) {
                return false;
            }
        }
        return true;
    }

    private static boolean sliceEquals(byte[] data, int start, int length, String ascii) {
        if(length != ascii.length()) {
            return false;
        }
        for(int iter = 0 ; iter < length ; iter++) {
            if((data[start + iter] & 0xff) != ascii.charAt(iter)) {
                return false;
            }
        }
        return true;
    }

    private static String reason(int status) {
        switch(status) {
            case 200: return "OK";
            case 201: return "Created";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 413: return "Payload Too Large";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            default: return status < 400 ? "OK" : "Error";
        }
    }

    private static int indexOfHeaderEnd(byte[] data, int from) {
        for(int iter = from ; iter + 3 < data.length ; iter++) {
            if(data[iter] == '\r' && data[iter + 1] == '\n'
                    && data[iter + 2] == '\r' && data[iter + 3] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    private static String[] splitLines(String value) {
        List parts = new ArrayList();
        int pos = 0;
        while(true) {
            int next = value.indexOf("\r\n", pos);
            if(next < 0) {
                if(pos < value.length()) {
                    parts.add(value.substring(pos));
                }
                break;
            }
            parts.add(value.substring(pos, next));
            pos = next + 2;
        }
        String[] out = new String[parts.size()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (String)parts.get(iter);
        }
        return out;
    }

    private static String[] splitOn(String value, char sep) {
        List parts = new ArrayList();
        int pos = 0;
        while(true) {
            int next = value.indexOf(sep, pos);
            if(next < 0) {
                parts.add(value.substring(pos));
                break;
            }
            parts.add(value.substring(pos, next));
            pos = next + 1;
        }
        String[] out = new String[parts.size()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (String)parts.get(iter);
        }
        return out;
    }
}
