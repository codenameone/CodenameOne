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
package com.codename1.backend.mail;

import com.codename1.backend.ByteSink;
import com.codename1.backend.Http1Date;

/// Writes a [Mail] as the bytes a mail server takes: RFC 5322 headers and a
/// MIME body.
///
/// The message is encoded once, into one buffer, and those bytes are what
/// every attempt to send it uses. Nothing here builds a String of the body:
/// text is turned to UTF-8 once and encoded from the bytes, an attachment is
/// encoded from its array straight into the output.
///
/// The structure is the smallest that holds the mail:
///
/// - text alone is one `text/plain` part
/// - text and HTML are `multipart/alternative`
/// - HTML with inline images is `multipart/related` in the HTML's place
/// - attachments wrap all of it in `multipart/mixed`
///
/// A text body goes as `7bit` when it is ASCII in lines of at most 78
/// characters, and as quoted-printable otherwise; everything else is Base64
/// in 76 character lines. A header that is not ASCII is written as RFC 2047
/// encoded words, and every header is folded at 78 characters.
///
/// Line endings are CRLF throughout and no line begins a dot-stuffed pair:
/// doubling a leading dot is SMTP's transparency, done by the SMTP transport
/// while it copies these bytes, since a provider's HTTP API takes them as they
/// are.
final class MimeWriter {
    private static final String BASE64 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final String HEX = "0123456789ABCDEF";
    /// The longest line a header is folded to, without its CRLF.
    private static final int FOLD = 78;
    /// The longest line that may hold an encoded word.
    private static final int ENCODED_LINE = 76;

    private final ByteSink out;
    private final ByteSink scratch = new ByteSink(256);
    private int col;

    private MimeWriter(ByteSink out) {
        this.out = out;
    }

    /// Encodes `mail`, which must be resolved: it has a sender and no
    /// template left to render.
    ///
    /// - `messageId`: without the angle brackets
    /// - `boundary`: a string that cannot occur in the mail, which random
    ///   bytes make certain enough; each level adds a letter to it
    /// - `millis`: the time for the Date header
    static byte[] write(Mail mail, String messageId, String boundary, long millis) {
        int size = 1024;
        if (mail.text != null) {
            size += mail.text.length() * 2;
        }
        if (mail.html != null) {
            size += mail.html.length() * 2;
        }
        for (Mail.Part p : mail.attachments) {
            size += p.data.length / 3 * 4 + p.data.length / 28 + 512;
        }
        for (Mail.Part p : mail.inlines) {
            size += p.data.length / 3 * 4 + p.data.length / 28 + 512;
        }
        ByteSink sink = new ByteSink(size);
        new MimeWriter(sink).message(mail, messageId, boundary, millis);
        byte[] exact = new byte[sink.length()];
        System.arraycopy(sink.bytes(), 0, exact, 0, exact.length);
        return exact;
    }

    private void message(Mail m, String messageId, String boundary, long millis) {
        String date = Http1Date.format(millis);
        // "Sun, 06 Nov 1994 08:49:37 GMT": RFC 5322 wants a numeric zone.
        raw("Date: " + date.substring(0, date.length() - 3) + "+0000");
        addresses("From", new Address[] {m.from});
        if (m.replyTo != null) {
            addresses("Reply-To", new Address[] {m.replyTo});
        }
        if (m.to.length > 0) {
            addresses("To", m.to);
        }
        if (m.cc.length > 0) {
            addresses("Cc", m.cc);
        }
        // Bcc is in the envelope only: writing it would tell every recipient.
        if (m.subject != null) {
            text("Subject", m.subject);
        }
        raw("Message-ID: <" + messageId + ">");
        raw("MIME-Version: 1.0");
        for (int i = 0; i < m.headers.length; i += 2) {
            text(m.headers[i], m.headers[i + 1]);
        }
        if (m.attachments.length == 0) {
            content(m, boundary);
            return;
        }
        String b = boundary + "m";
        multipart("mixed", null, b);
        open(b);
        content(m, boundary);
        for (Mail.Part p : m.attachments) {
            open(b);
            part(p);
        }
        close(b);
    }

    /// The text, the HTML, or both as alternatives.
    private void content(Mail m, String boundary) {
        if (m.html == null) {
            textPart("plain", m.text == null ? "" : m.text);
            return;
        }
        if (m.text == null) {
            htmlContent(m, boundary);
            return;
        }
        String b = boundary + "a";
        multipart("alternative", null, b);
        open(b);
        textPart("plain", m.text);
        // The richest form goes last: a reader shows the last one it can.
        open(b);
        htmlContent(m, boundary);
        close(b);
    }

    /// The HTML, with the images it shows beside it when it has any.
    private void htmlContent(Mail m, String boundary) {
        if (m.inlines.length == 0) {
            textPart("html", m.html);
            return;
        }
        String b = boundary + "r";
        multipart("related", "text/html", b);
        open(b);
        textPart("html", m.html);
        for (Mail.Part p : m.inlines) {
            open(b);
            part(p);
        }
        close(b);
    }

    private void multipart(String subtype, String type, String boundary) {
        out.putAscii("Content-Type: multipart/");
        out.putAscii(subtype);
        out.putAscii(";\r\n");
        if (type != null) {
            out.putAscii(" type=\"");
            out.putAscii(type);
            out.putAscii("\";\r\n");
        }
        out.putAscii(" boundary=\"");
        out.putAscii(boundary);
        out.putAscii("\"\r\n\r\n");
    }

    /// Every part ends with a CRLF, which is the one the delimiter owns.
    private void open(String boundary) {
        out.putAscii("--");
        out.putAscii(boundary);
        out.putAscii("\r\n");
    }

    private void close(String boundary) {
        out.putAscii("--");
        out.putAscii(boundary);
        out.putAscii("--\r\n");
    }

    private void raw(String line) {
        out.putAscii(line);
        out.putAscii("\r\n");
    }

    // ---- headers ----

    private void begin(String name) {
        out.putAscii(name);
        out.putAscii(": ");
        col = name.length() + 2;
    }

    private void fold() {
        out.putAscii("\r\n ");
        col = 1;
    }

    /// Writes one unbreakable piece of a header, on a new line when it does
    /// not fit this one. `space` says whether whitespace separates it from
    /// what came before, which is the only place a header may be folded.
    private void token(String value, boolean space, boolean ascii) {
        if (space) {
            if (col + 1 + value.length() > FOLD) {
                fold();
            } else {
                out.put(' ');
                col++;
            }
        }
        if (ascii) {
            out.putAscii(value);
        } else {
            out.putUtf8(value);
        }
        col += value.length();
    }

    private static boolean needsEncoding(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < ' ' || c > 126) {
                return true;
            }
        }
        // ASCII that looks like an encoded word would be decoded as one.
        return value.indexOf("=?") >= 0;
    }

    /// Whether `value` has a run without a space that no folding could keep
    /// inside the 998 bytes a line may be; a server refuses a longer one.
    static boolean longWord(String value) {
        int run = 0;
        for (int i = 0; i < value.length(); i++) {
            run = value.charAt(i) == ' ' ? 0 : run + 1;
            if (run > 900) {
                return true;
            }
        }
        return false;
    }

    /// An unstructured header: Subject, or one of the application's own.
    private void text(String name, String value) {
        begin(name);
        if (needsEncoding(value) || longWord(value)) {
            encoded(value, false);
        } else {
            int from = 0;
            boolean first = true;
            while (from <= value.length()) {
                int space = value.indexOf(' ', from);
                if (space < 0) {
                    space = value.length();
                }
                token(value.substring(from, space), !first, true);
                first = false;
                from = space + 1;
            }
        }
        out.putAscii("\r\n");
    }

    private void addresses(String name, Address[] list) {
        begin(name);
        for (int i = 0; i < list.length; i++) {
            Address a = list[i];
            String tail = i < list.length - 1 ? "," : "";
            boolean space = i > 0;
            if (a.name == null) {
                token(a.spec + tail, space, a.ascii);
                continue;
            }
            // A name is one piece, spaces and all, so a long one is written
            // as encoded words, which fold.
            if (needsEncoding(a.name) || a.name.length() > 60) {
                encoded(a.name, space);
            } else {
                token(phrase(a.name), space, true);
            }
            token("<" + a.spec + ">" + tail, true, a.ascii);
        }
        out.putAscii("\r\n");
    }

    /// A display name as it may stand in a header: as it is when it is words,
    /// and as a quoted string when it holds a character with a meaning there.
    private static String phrase(String name) {
        boolean plain = true;
        for (int i = 0; plain && i < name.length(); i++) {
            char c = name.charAt(i);
            plain = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == ' ' || c == '-' || c == '_' || c == '\'' || c == '!' || c == '+';
        }
        return plain ? name : quoted(name);
    }

    private static String quoted(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 4);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.append('"').toString();
    }

    /// Writes `value` as RFC 2047 encoded words, each `=?UTF-8?B?...?=`, as
    /// many as it takes: an encoded word may be 75 characters and the line it
    /// is on 76, and a character's bytes may not be split between two words.
    private void encoded(String value, boolean space) {
        int i = 0;
        int n = value.length();
        boolean lead = space;
        while (i < n) {
            int budget = (ENCODED_LINE - col - (lead ? 1 : 0) - 12) / 4 * 3;
            if (budget < 4) {
                fold();
                budget = (ENCODED_LINE - col - 12) / 4 * 3;
            } else if (lead) {
                out.put(' ');
                col++;
            }
            scratch.reset();
            while (i < n) {
                int cp = value.charAt(i);
                int chars = 1;
                if (cp >= 0xD800 && cp <= 0xDBFF && i + 1 < n && value.charAt(i + 1) >= 0xDC00
                        && value.charAt(i + 1) <= 0xDFFF) {
                    cp = 0x10000 + ((cp - 0xD800) << 10) + (value.charAt(i + 1) - 0xDC00);
                    chars = 2;
                } else if (cp >= 0xD800 && cp <= 0xDFFF) {
                    // Half a pair is not a character; UTF-8 has no form for it.
                    cp = '?';
                }
                int bytes = cp < 0x80 ? 1 : cp < 0x800 ? 2 : cp < 0x10000 ? 3 : 4;
                if (scratch.length() + bytes > budget) {
                    break;
                }
                scratch.putCodePoint(cp);
                i += chars;
            }
            out.putAscii("=?UTF-8?B?");
            base64(scratch.bytes(), 0, scratch.length(), false);
            out.putAscii("?=");
            col += 12 + (scratch.length() + 2) / 3 * 4;
            // Whitespace between two encoded words is not part of the text.
            lead = true;
        }
    }

    // ---- parts ----

    private void textPart(String subtype, String value) {
        scratch.reset();
        scratch.putUtf8(value);
        byte[] data = scratch.bytes();
        int length = scratch.length();
        boolean sevenBit = true;
        int line = 0;
        for (int i = 0; sevenBit && i < length; i++) {
            int b = data[i];
            if (b == '\n' || b == '\r') {
                line = 0;
            } else {
                line++;
                sevenBit = b >= 0 && (b >= ' ' || b == '\t') && b != 127 && line <= FOLD;
            }
        }
        out.putAscii("Content-Type: text/");
        out.putAscii(subtype);
        out.putAscii("; charset=UTF-8\r\nContent-Transfer-Encoding: ");
        out.putAscii(sevenBit ? "7bit\r\n\r\n" : "quoted-printable\r\n\r\n");
        if (sevenBit) {
            plain(data, length);
        } else {
            quotedPrintable(data, length);
        }
    }

    /// Whether a line break starts at `i`; a CRLF, a lone LF and a lone CR
    /// are each one break.
    private static int lineBreak(byte[] data, int i, int length) {
        if (data[i] == '\n') {
            return 1;
        }
        if (data[i] == '\r') {
            return i + 1 < length && data[i + 1] == '\n' ? 2 : 1;
        }
        return 0;
    }

    private void plain(byte[] data, int length) {
        int i = 0;
        boolean ended = true;
        while (i < length) {
            int br = lineBreak(data, i, length);
            if (br > 0) {
                out.putAscii("\r\n");
                i += br;
                ended = true;
            } else {
                out.put(data[i++]);
                ended = false;
            }
        }
        if (!ended) {
            out.putAscii("\r\n");
        }
    }

    private void quotedPrintable(byte[] data, int length) {
        int i = 0;
        int width = 0;
        boolean ended = true;
        while (i < length) {
            int br = lineBreak(data, i, length);
            if (br > 0) {
                out.putAscii("\r\n");
                i += br;
                width = 0;
                ended = true;
                continue;
            }
            int b = data[i] & 0xff;
            boolean literal = b > ' ' && b < 127 && b != '=';
            if (b == ' ' || b == '\t') {
                // Whitespace at the end of a line is removed in transit unless
                // it is encoded.
                literal = i + 1 < length && lineBreak(data, i + 1, length) == 0;
            }
            int need = literal ? 1 : 3;
            // 76 with the soft break's own "=".
            if (width + need > 75) {
                out.putAscii("=\r\n");
                width = 0;
            }
            if (literal) {
                out.put(b);
            } else {
                out.put('=');
                out.put(HEX.charAt(b >> 4));
                out.put(HEX.charAt(b & 15));
            }
            width += need;
            ended = false;
            i++;
        }
        if (!ended) {
            out.putAscii("\r\n");
        }
    }

    private void part(Mail.Part p) {
        boolean asciiName = p.name != null && !needsEncoding(p.name);
        out.putAscii("Content-Type: ");
        out.putAscii(p.type);
        if (asciiName) {
            out.putAscii(";\r\n name=");
            out.putAscii(quoted(p.name));
        }
        out.putAscii("\r\nContent-Transfer-Encoding: base64\r\n");
        if (p.contentId != null) {
            out.putAscii("Content-ID: <");
            out.putAscii(p.contentId);
            out.putAscii(">\r\nContent-Disposition: inline\r\n\r\n");
        } else {
            out.putAscii("Content-Disposition: attachment;\r\n filename");
            if (asciiName) {
                out.put('=');
                out.putAscii(quoted(p.name));
            } else {
                // RFC 2231: the only form of a parameter that says which
                // charset its bytes are in.
                out.putAscii("*=UTF-8''");
                scratch.reset();
                scratch.putUtf8(p.name);
                byte[] raw = scratch.bytes();
                for (int i = 0; i < scratch.length(); i++) {
                    int b = raw[i] & 0xff;
                    if (b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z' || b >= '0' && b <= '9'
                            || b == '.' || b == '-' || b == '_') {
                        out.put(b);
                    } else {
                        out.put('%');
                        out.put(HEX.charAt(b >> 4));
                        out.put(HEX.charAt(b & 15));
                    }
                }
            }
            out.putAscii("\r\n\r\n");
        }
        base64(p.data, 0, p.data.length, true);
    }

    /// Base64 of `data` into the output; `wrap` breaks it into 76 character
    /// lines and ends it with a line break, which a body needs and an encoded
    /// word must not have.
    private void base64(byte[] data, int offset, int length, boolean wrap) {
        out.ensure(length / 3 * 4 + length / 28 + 8);
        int end = offset + length;
        int i = offset;
        int width = 0;
        while (i + 2 < end) {
            int v = (data[i] & 0xff) << 16 | (data[i + 1] & 0xff) << 8 | data[i + 2] & 0xff;
            out.put(BASE64.charAt(v >> 18));
            out.put(BASE64.charAt(v >> 12 & 63));
            out.put(BASE64.charAt(v >> 6 & 63));
            out.put(BASE64.charAt(v & 63));
            i += 3;
            width += 4;
            if (wrap && width == 76) {
                out.put('\r');
                out.put('\n');
                width = 0;
            }
        }
        if (end - i == 1) {
            int v = (data[i] & 0xff) << 16;
            out.put(BASE64.charAt(v >> 18));
            out.put(BASE64.charAt(v >> 12 & 63));
            out.put('=');
            out.put('=');
            width += 4;
        } else if (end - i == 2) {
            int v = (data[i] & 0xff) << 16 | (data[i + 1] & 0xff) << 8;
            out.put(BASE64.charAt(v >> 18));
            out.put(BASE64.charAt(v >> 12 & 63));
            out.put(BASE64.charAt(v >> 6 & 63));
            out.put('=');
            width += 4;
        }
        if (wrap && width > 0) {
            out.put('\r');
            out.put('\n');
        }
    }
}
