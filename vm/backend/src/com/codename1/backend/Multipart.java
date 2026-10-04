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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Splits a `multipart/form-data` body (RFC 7578) into [HttpServer.Part]s, and
/// decodes an `application/x-www-form-urlencoded` one.
///
/// Bounded the way the header parser is: a body of a thousand empty parts, or one
/// part whose header block never ends, is refused rather than walked. Refusals are
/// IllegalArgumentException, which the generated routers answer with a 400; a
/// handler calling getParts() itself sees the same exception.
final class Multipart {
    /// Parts in one body. A form is a few fields and a file or two.
    static final int MAX_PARTS = 1000;
    /// The header block of one part: Content-Disposition and Content-Type.
    static final int MAX_PART_HEADER_BYTES = 8192;

    private Multipart() {
    }

    /// The boundary a multipart Content-Type names, or null when it is not
    /// `multipart/form-data` or names none.
    static String boundary(String contentType) {
        if (contentType == null) {
            return null;
        }
        int semi = contentType.indexOf(';');
        String type = (semi < 0 ? contentType : contentType.substring(0, semi)).trim();
        if (!"multipart/form-data".equalsIgnoreCase(type) || semi < 0) {
            return null;
        }
        String value = parameter(contentType.substring(semi + 1), "boundary");
        return value == null || value.length() == 0 || value.length() > 70 ? null : value;
    }

    /// Whether a Content-Type declares `multipart/form-data`, whatever its
    /// boundary -- so a declaration with a missing or invalid one can be told
    /// from a body that is not multipart at all.
    static boolean isMultipart(String contentType) {
        if (contentType == null) {
            return false;
        }
        int semi = contentType.indexOf(';');
        String type = (semi < 0 ? contentType : contentType.substring(0, semi)).trim();
        return "multipart/form-data".equalsIgnoreCase(type);
    }

    /// Whether a Content-Type is a urlencoded form.
    static boolean isForm(String contentType) {
        if (contentType == null) {
            return false;
        }
        int semi = contentType.indexOf(';');
        String type = (semi < 0 ? contentType : contentType.substring(0, semi)).trim();
        return "application/x-www-form-urlencoded".equalsIgnoreCase(type);
    }

    /// The parts of `body`, in order.
    static List parse(byte[] body, String boundary) {
        byte[] delimiter = ascii("--" + boundary);
        List parts = new ArrayList();
        int pos = openingDelimiter(body, delimiter);
        if (pos < 0) {
            throw new IllegalArgumentException("the multipart body has no boundary");
        }
        while (true) {
            pos += delimiter.length;
            if (pos + 1 < body.length && body[pos] == '-' && body[pos + 1] == '-') {
                if (!closeEnds(body, pos + 2)) {
                    throw new IllegalArgumentException("the closing multipart boundary is "
                            + "followed by more than padding and CRLF");
                }
                return parts;
            }
            // Transport padding (RFC 2046) is allowed between a boundary and its CRLF.
            while (pos < body.length && (body[pos] == ' ' || body[pos] == '\t')) {
                pos++;
            }
            if (!crlfAt(body, pos)) {
                throw new IllegalArgumentException("a multipart boundary is not followed by CRLF");
            }
            pos += 2;
            if (parts.size() == MAX_PARTS) {
                throw new IllegalArgumentException("the multipart body has more than "
                        + MAX_PARTS + " parts");
            }
            Map headers = new LinkedHashMap();
            // The limit is on the whole header block, counted from its start. Per
            // line, it let one part carry hundreds of thousands of short headers --
            // each line parsed and stored -- inside the body limit.
            int headersStart = pos;
            while (!crlfAt(body, pos)) {
                int end = indexOfCrLf(body, pos);
                if (end < 0 || end - headersStart > MAX_PART_HEADER_BYTES) {
                    throw new IllegalArgumentException("a multipart part's headers never end");
                }
                String line = latin1(body, pos, end - pos);
                int colon = line.indexOf(':');
                if (colon <= 0) {
                    throw new IllegalArgumentException("a multipart part has a malformed header");
                }
                headers.put(asciiLower(line.substring(0, colon).trim()),
                        line.substring(colon + 1).trim());
                pos = end + 2;
            }
            pos += 2;
            byte[] closing = new byte[delimiter.length + 2];
            closing[0] = '\r';
            closing[1] = '\n';
            System.arraycopy(delimiter, 0, closing, 2, delimiter.length);
            int next = closingDelimiter(body, closing, pos);
            if (next < 0) {
                throw new IllegalArgumentException("a multipart part is never closed");
            }
            byte[] data = new byte[next - pos];
            System.arraycopy(body, pos, data, 0, data.length);
            String disposition = (String) headers.get("content-disposition");
            // RFC 7578 4.2: every part of a form says Content-Disposition: form-data
            // and names its field. One that does not is a malformed form, not a
            // nameless part that an optional binding quietly ignores.
            if (disposition == null || !isFormData(disposition)) {
                throw new IllegalArgumentException("a multipart/form-data part has no "
                        + "Content-Disposition: form-data");
            }
            String name = parameter(afterType(disposition), "name");
            if (name == null) {
                throw new IllegalArgumentException("a multipart/form-data part names no field");
            }
            String filename = parameter(afterType(disposition), "filename");
            parts.add(new HttpServer.Part(name, filename, (String) headers.get("content-type"),
                    headers, data));
            pos = next + 2;
        }
    }

    /// Where the delimiter that closes a part starts, searching from `from`, or -1.
    /// A delimiter is complete only when `--` follows it (the last one) or
    /// optional transport padding and CRLF (RFC 2046 5.1.1). A part's own bytes
    /// may contain `CRLF--boundary` followed by anything else -- a binary file
    /// can -- and taking that for the end of the part refused a legal upload.
    private static int closingDelimiter(byte[] body, byte[] closing, int from) {
        int at = indexOf(body, closing, from);
        while (at >= 0) {
            int after = at + closing.length;
            if (after + 1 < body.length && body[after] == '-' && body[after + 1] == '-'
                    && closeEnds(body, after + 2)) {
                return at;
            }
            int p = after;
            while (p < body.length && (body[p] == ' ' || body[p] == '\t')) {
                p++;
            }
            if (crlfAt(body, p)) {
                return at;
            }
            at = indexOf(body, closing, at + 1);
        }
        return -1;
    }

    /// Where the first delimiter starts: at the start of the body or of a line of
    /// the preamble, and complete -- `--` after it, or padding and CRLF (RFC 2046
    /// 5.1.1). Any other `--boundary` is preamble text: taking the first substring
    /// match accepted `prefix--b` as an opening and refused a valid preamble that
    /// merely mentioned the boundary before the real one.
    private static int openingDelimiter(byte[] body, byte[] delimiter) {
        int at = indexOf(body, delimiter, 0);
        while (at >= 0) {
            boolean lineStart = at == 0 || (at >= 2 && body[at - 2] == '\r' && body[at - 1] == '\n');
            int after = at + delimiter.length;
            boolean complete;
            if (after + 1 < body.length && body[after] == '-' && body[after + 1] == '-') {
                complete = closeEnds(body, after + 2);
            } else {
                int p = after;
                while (p < body.length && (body[p] == ' ' || body[p] == '\t')) {
                    p++;
                }
                complete = crlfAt(body, p);
            }
            if (lineStart && complete) {
                return at;
            }
            at = indexOf(body, delimiter, at + 1);
        }
        return -1;
    }

    private static boolean isFormData(String disposition) {
        int semi = disposition.indexOf(';');
        return "form-data".equalsIgnoreCase((semi < 0 ? disposition : disposition.substring(0, semi)).trim());
    }

    /// Whether the close delimiter whose `--` ends just before `pos` really ends
    /// there: transport padding, then the end of the body or the CRLF that starts
    /// the epilogue (RFC 2046 5.1.1). `--b--garbage` is not a close delimiter.
    private static boolean closeEnds(byte[] body, int pos) {
        int p = pos;
        while (p < body.length && (body[p] == ' ' || body[p] == '\t')) {
            p++;
        }
        return p == body.length || crlfAt(body, p);
    }

    /// The value of `name` in a urlencoded form, or null. `+` is a space, as the
    /// form encoding defines; a malformed escape is kept as written.
    static String formValue(String form, String name) {
        if (form == null || name == null) {
            return null;
        }
        int pos = 0;
        while (pos <= form.length()) {
            int end = form.indexOf('&', pos);
            if (end < 0) {
                end = form.length();
            }
            int eq = form.indexOf('=', pos);
            if (eq < 0 || eq > end) {
                eq = end;
            }
            if (name.equals(decode(form.substring(pos, eq)))) {
                return decode(eq < end ? form.substring(eq + 1, end) : "");
            }
            pos = end + 1;
        }
        return null;
    }

    private static String decode(String value) {
        if (value.indexOf('%') < 0 && value.indexOf('+') < 0) {
            return value;
        }
        byte[] out = new byte[value.length()];
        int n = 0;
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if (c == '+') {
                out[n++] = ' ';
            } else if (c == '%' && iter + 2 < value.length()
                    && hex(value.charAt(iter + 1)) >= 0 && hex(value.charAt(iter + 2)) >= 0) {
                out[n++] = (byte) (hex(value.charAt(iter + 1)) * 16 + hex(value.charAt(iter + 2)));
                iter += 2;
            } else if (c < 0x80) {
                out[n++] = (byte) c;
            } else {
                // Already text: re-encode it so the decode below reads it back. A
                // surrogate pair is one character and is encoded together; one
                // surrogate at a time came out as two replacement characters.
                boolean pair = c >= 0xD800 && c <= 0xDBFF && iter + 1 < value.length()
                        && value.charAt(iter + 1) >= 0xDC00 && value.charAt(iter + 1) <= 0xDFFF;
                byte[] utf = utf8(pair ? value.substring(iter, iter + 2) : String.valueOf(c));
                if (pair) {
                    iter++;
                }
                if (n + utf.length > out.length) {
                    byte[] grown = new byte[out.length + utf.length + 16];
                    System.arraycopy(out, 0, grown, 0, n);
                    out = grown;
                }
                System.arraycopy(utf, 0, out, n, utf.length);
                n += utf.length;
            }
        }
        try {
            return new String(out, 0, n, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException(err.toString(), err);
        }
    }

    private static byte[] utf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            throw new IllegalStateException(err.toString(), err);
        }
    }

    private static int hex(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }

    /// What follows the type of a header value such as `form-data; name="x"`.
    private static String afterType(String value) {
        int semi = value.indexOf(';');
        return semi < 0 ? "" : value.substring(semi + 1);
    }

    /// The value of `name` among `; key=value` parameters, a quoted string
    /// unquoted. Names compare without case, values as written.
    static String parameter(String params, String name) {
        int pos = 0;
        int length = params.length();
        while (pos < length) {
            while (pos < length && (params.charAt(pos) == ' ' || params.charAt(pos) == '\t'
                    || params.charAt(pos) == ';')) {
                pos++;
            }
            int eq = params.indexOf('=', pos);
            if (eq < 0) {
                return null;
            }
            String key = params.substring(pos, eq).trim();
            pos = eq + 1;
            String value;
            if (pos < length && params.charAt(pos) == '"') {
                StringBuilder sb = new StringBuilder();
                pos++;
                while (pos < length && params.charAt(pos) != '"') {
                    char c = params.charAt(pos);
                    if (c == '\\' && pos + 1 < length) {
                        pos++;
                        c = params.charAt(pos);
                    }
                    sb.append(c);
                    pos++;
                }
                if (pos >= length) {
                    // No closing quote: the header is malformed, and what it would
                    // have said is unknown. Answered as absent, which both callers
                    // refuse -- no valid boundary, a part naming no field -- where
                    // boundary="b used to be accepted as b.
                    return null;
                }
                pos++;
                value = sb.toString();
            } else {
                int end = params.indexOf(';', pos);
                if (end < 0) {
                    end = length;
                }
                value = params.substring(pos, end).trim();
                pos = end;
            }
            if (key.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    private static boolean crlfAt(byte[] data, int pos) {
        return pos + 1 < data.length && data[pos] == '\r' && data[pos + 1] == '\n';
    }

    private static int indexOfCrLf(byte[] data, int from) {
        for (int iter = from ; iter + 1 < data.length ; iter++) {
            if (data[iter] == '\r' && data[iter + 1] == '\n') {
                return iter;
            }
        }
        return -1;
    }

    /// Where `needle` first occurs in `data` at or after `from`, or -1, in linear
    /// time (Knuth-Morris-Pratt). The boundary is the client's, up to 70 bytes:
    /// a naive search restarted the comparison at every matching first byte, so a
    /// boundary of dashes over a body of dashes cost hundreds of millions of
    /// comparisons per request before it was refused.
    private static int indexOf(byte[] data, byte[] needle, int from) {
        int[] fallback = new int[needle.length];
        int k = 0;
        for (int iter = 1 ; iter < needle.length ; iter++) {
            while (k > 0 && needle[iter] != needle[k]) {
                k = fallback[k - 1];
            }
            if (needle[iter] == needle[k]) {
                k++;
            }
            fallback[iter] = k;
        }
        k = 0;
        for (int iter = Math.max(from, 0) ; iter < data.length ; iter++) {
            while (k > 0 && data[iter] != needle[k]) {
                k = fallback[k - 1];
            }
            if (data[iter] == needle[k]) {
                k++;
            }
            if (k == needle.length) {
                return iter - needle.length + 1;
            }
        }
        return -1;
    }

    private static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) value.charAt(iter);
        }
        return out;
    }

    private static String latin1(byte[] data, int offset, int length) {
        char[] out = new char[length];
        for (int iter = 0 ; iter < length ; iter++) {
            out[iter] = (char) (data[offset + iter] & 0xff);
        }
        return new String(out);
    }

    private static String asciiLower(String value) {
        char[] chars = value.toCharArray();
        for (int iter = 0 ; iter < chars.length ; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }
}
