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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serves files out of a document root, on the kernel's zero-copy path.
 *
 * The body goes out with sendfile() where the platform has it: the bytes move from
 * the page cache to the socket inside the kernel, never entering this process. For
 * a file server that is the difference between two copies per byte and none. TLS
 * is the exception and always will be -- encrypted bytes have to be produced in
 * user space, so that path reads and writes like anything else.
 *
 * Correctness this does NOT cut corners on:
 *
 *   - the resolved file must be inside the root, proven with realpath() rather
 *     than by inspecting the request string. "../" is only the obvious attack;
 *     percent-encoding and a symlink pointing out of the tree are the other two,
 *     and only resolution catches all three
 *   - the descriptor is opened FIRST and stat'd from the open fd, so the length in
 *     the header and the bytes in the body describe the same file even if it is
 *     replaced mid-request
 *   - conditional requests (If-None-Match, If-Modified-Since) and ranges, because
 *     a static server without them re-sends whole files to clients that already
 *     have them
 */
public final class StaticFiles implements HttpServer.Handler {
    private static final boolean HAVE_SENDFILE = FileIo.hasSendFile();

    private final String root;
    private final String prefix;
    private final String indexFile;
    private final String cacheControl;

    /**
     * - `root`: the document root; resolved once, and every request must land inside it
     * - `prefix`: URL prefix to strip, "" or "/" for none
     * - `cacheControl`: the Cache-Control value, or null to omit it
     */
    public StaticFiles(String root, String prefix, String indexFile, String cacheControl) throws IOException {
        String resolved = FileIo.realPath(root);
        if(resolved == null) {
            throw new IOException("Document root does not exist: " + root);
        }
        this.root = resolved;
        this.prefix = prefix == null || "/".equals(prefix) ? "" : stripTrailingSlash(prefix);
        this.indexFile = indexFile == null ? "index.html" : indexFile;
        this.cacheControl = cacheControl;
    }

    /** True when the body is sent by the kernel rather than copied through here. */
    public static boolean isZeroCopy() {
        return HAVE_SENDFILE;
    }

    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        // THE CANONICAL PATH, not getTarget(). getTarget hands back the target
        // exactly as it arrived, so the mount was matched on the spelling the
        // client chose while every generated router matching the same URI compares
        // it with percent-encoded unreserved octets already resolved. /assets was
        // therefore ours and /%61ssets was not -- the same path, and in a chain of
        // handlers, two different answers to whose it is. pathFrom stops at the
        // query, which is what the split this replaces was for, and it leaves an
        // encoded slash encoded, so %2F still cannot invent a segment boundary.
        String target = request.pathFrom(0);
        if(prefix.length() > 0) {
            // The prefix has to end on a segment boundary. startsWith alone let
            // /assets2/logo.png match a prefix of /assets, strip to /2/logo.png and
            // be served from the document root, which is a different URL namespace
            // than the one this handler was mounted on.
            if(!target.startsWith(prefix)
                    || (target.length() > prefix.length()
                        && target.charAt(prefix.length()) != '/')) {
                return null; // not ours; let the caller 404 it
            }
            target = target.substring(prefix.length());
        }
        // The method is checked only once the target is known to be OURS. This
        // handler is one link in a chain -- the caller tries it and falls back --
        // so refusing a verb for a path outside the mount answers on behalf of
        // whoever was going to handle it: a POST to an unrelated path came back
        // 405 instead of reaching the 404 the caller meant, and in a chain that
        // tries files first it would shadow a later dynamic handler entirely.
        String method = request.getMethod();
        if(!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return HttpServer.Response.text(405, "method not allowed");
        }
        if(target.length() == 0) {
            // The request named the mount EXACTLY, with no trailing slash. Left
            // alone it becomes "/" and then "/" + indexFile, which resolves to a
            // FILE -- so the directory branch further down, which exists to issue
            // this very redirect, never runs and the index is served as 200 at
            // /assets. A browser then resolves "style.css" in it against /, not
            // /assets/, and every relative reference in the site points outside
            // the mount. Redirect to the directory form the way that branch does,
            // carrying the query because it was addressed to this resource.
            String raw = request.getTarget() == null ? "" : request.getTarget();
            int at = raw.indexOf('?');
            Map here = new LinkedHashMap();
            here.put("Location", prefix + "/" + (at < 0 ? "" : raw.substring(at)));
            return HttpServer.Response.empty(301, "text/plain", here);
        }
        String decoded = decode(target);
        if(decoded == null) {
            return HttpServer.Response.text(400, "bad path");
        }
        if(decoded.indexOf('\0') >= 0) {
            // A NUL truncates the path in every C call underneath this.
            return HttpServer.Response.text(400, "bad path");
        }
        if(!decoded.startsWith("/")) {
            decoded = "/" + decoded;
        }
        if(decoded.endsWith("/")) {
            decoded = decoded + indexFile;
        }

        // Open under the root so the kernel refuses an escape while it resolves.
        // The realPath check further down runs against a SECOND lookup, so on its
        // own it loses a race an attacker who can write symlinks into the document
        // root controls: point the link outside for this open, inside for the
        // check, and the descriptor served is the outside file. Where openBeneath
        // works, containment is already settled by the time the descriptor exists.
        boolean beneathProven = true;
        int fd = FileIo.openBeneath(root, decoded);
        if(fd == FileIo.BENEATH_UNSUPPORTED) {
            beneathProven = false;
            fd = FileIo.openRead(root + decoded);
        }
        if(fd < 0) {
            return HttpServer.Response.text(404, "not found");
        }
        boolean release = true;
        try {
            long[] info = new long[4];
            if(FileIo.stat(fd, info) != 0) {
                return HttpServer.Response.text(404, "not found");
            }
            if(info[2] != 0) {
                // A directory: retry at its index file rather than listing it.
                // Directory listings leak names nobody asked to publish.
                FileIo.close(fd);
                release = false;
                String rawTarget = request.getTarget() == null ? "" : request.getTarget();
                int queryAt = rawTarget.indexOf('?');
                String rawPath = queryAt < 0 ? rawTarget : rawTarget.substring(0, queryAt);
                if(!rawPath.endsWith("/")) {
                    // Redirect first. Serving the index at /static/docs makes a browser
                    // resolve "style.css" in it against /static/, not /static/docs/, so
                    // every relative reference in an otherwise valid site points one
                    // level too high. The query is carried across because it was
                    // addressed to this resource.
                    Map moved = new LinkedHashMap();
                    moved.put("Location", rawPath + "/"
                            + (queryAt < 0 ? "" : rawTarget.substring(queryAt)));
                    return HttpServer.Response.empty(301, "text/plain", moved);
                }
                String indexPath = stripTrailingSlash(decoded) + "/" + indexFile;
                int indexFd = beneathProven ? FileIo.openBeneath(root, indexPath)
                                            : FileIo.openRead(root + indexPath);
                if(indexFd == FileIo.BENEATH_UNSUPPORTED) {
                    beneathProven = false;
                    indexFd = FileIo.openRead(root + indexPath);
                }
                if(indexFd < 0) {
                    return HttpServer.Response.text(404, "not found");
                }
                fd = indexFd;
                release = true;
                if(FileIo.stat(fd, info) != 0 || info[2] != 0) {
                    return HttpServer.Response.text(404, "not found");
                }
                decoded = stripTrailingSlash(decoded) + "/" + indexFile;
            }

            // Only where the open could not prove it. Checking the request string
            // instead is defeated by an encoded traversal or by a symlink out of the
            // tree, so this resolves first -- but it is a second lookup, which is why
            // the open above is preferred wherever the platform supports it.
            if(!beneathProven) {
                String real = FileIo.realPath(root + decoded);
                if(real == null || !isInsideRoot(real)) {
                    return HttpServer.Response.text(403, "forbidden");
                }
            }

            long size = info[0];
            long modified = info[1];
            // JavaAPI's Long has neither toHexString nor a radix toString. An ETag
            // only has to be stable and opaque, so decimal is exactly as good.
            //
            // THREE COMPONENTS, the third being the file's identity -- its inode
            // where the platform has one. Size and mtime alone identify the
            // representation only as well as the timestamps do: a deployment that
            // copies files with timestamps preserved reproduces the same
            // millisecond, and if the new content is the same length the validator
            // does not change, so a client holding the old one is told 304 for as
            // long as it keeps asking. That copy makes a new file, which is what
            // the identity catches.
            //
            // What remains uncovered is an in-place rewrite that restores the
            // timestamp AND keeps the byte length. Closing that means deriving the
            // validator from the content, which means reading every byte of every
            // response on a path whose whole purpose is to avoid reading any of
            // them -- and a cache of those hashes could only be keyed on the
            // metadata that just collided. Operators who rewrite files that way
            // should touch them, or serve them with a Cache-Control that does not
            // invite revalidation.
            String etag = "\"" + size + "-" + modified + "-" + info[3] + "\"";

            Map headers = new LinkedHashMap();
            headers.put("ETag", etag);
            headers.put("Last-Modified", Http1Date.format(modified));
            headers.put("Accept-Ranges", "bytes");
            if(cacheControl != null) {
                headers.put("Cache-Control", cacheControl);
            }

            if(isNotModified(request, etag, modified)) {
                FileIo.close(fd);
                release = false;
                // 304 carries the validators and no body, by definition.
                return HttpServer.Response.empty(304, contentType(decoded), headers);
            }

            long offset = 0;
            long length = size;
            int status = 200;
            String range = request.getHeader("range");
            if(range != null && rangeIsFresh(request, etag, modified)) {
                long[] parsed = parseRange(range, size);
                if(parsed == IGNORE_RANGE) {
                    // Nothing wrong with the request; this server just cannot
                    // answer it as a range. Send the representation whole.
                    parsed = null;
                } else if(parsed == null) {
                    headers.put("Content-Range", "bytes */" + size);
                    FileIo.close(fd);
                    release = false;
                    return HttpServer.Response.empty(416, contentType(decoded), headers);
                }
                if(parsed != null) {
                    offset = parsed[0];
                    length = parsed[1];
                    status = 206;
                    headers.put("Content-Range",
                            "bytes " + offset + "-" + (offset + length - 1) + "/" + size);
                }
            }

            release = false; // the server owns the descriptor from here
            return HttpServer.Response.file(status, contentType(decoded), trackFile(fd), offset, length, headers);
        } finally {
            if(release) {
                FileIo.close(fd);
            }
        }
    }

    private boolean isInsideRoot(String real) {
        if(real.equals(root)) {
            return true;
        }
        // The separator matters: "/srv/wwwroot-evil" starts with "/srv/www" but is
        // not inside it. EITHER separator, though: FileIo.realPath answers
        // backslashes on Windows, so requiring root + "/" refused every ordinary
        // child there -- and since openBeneath() is unsupported in the Java SE
        // runtime, every request in a Windows dev loop reaches this fallback and
        // was answered 403. The packaged server is POSIX-only; the developer
        // running cn1:backend is not.
        String base = root;
        if(base.endsWith("/") || base.endsWith("\\")) {
            base = base.substring(0, base.length() - 1);
        }
        if(!real.startsWith(base) || real.length() <= base.length()) {
            return false;
        }
        char next = real.charAt(base.length());
        return next == '/' || next == '\\';
    }

    /**
     * True when a Range may be honoured: either the client sent no If-Range, or the
     * validator it sent still describes this file.
     *
     * A resumed download sends back the validator it received with the first part. If
     * the file has changed since, answering 206 out of the new one lets the client
     * staple fresh bytes onto a stale prefix and call the result a complete download.
     * HTTP's answer is to ignore the range and send the whole current representation,
     * which costs one download and saves a corrupt file.
     */
    private static boolean rangeIsFresh(HttpServer.Request request, String etag, long modified) {
        String ifRange = request.getHeader("if-range");
        if(ifRange == null) {
            return true;
        }
        String value = ifRange.trim();
        if(value.length() == 0) {
            return false;
        }
        if(value.charAt(0) == '"') {
            return value.equals(etag);
        }
        if(value.startsWith("W/") || value.startsWith("w/")) {
            // If-Range requires a strong comparison, and a weak tag cannot supply one.
            return false;
        }
        long parsed = Http1Date.parse(value);
        // Second granularity on the wire, as in isNotModified.
        return parsed >= 0 && parsed / 1000 == modified / 1000;
    }

    /**
     * Whether If-None-Match names this representation, read as the comma
     * separated list of validators it is.
     *
     * <p>Weak comparison, which is the one If-None-Match takes (RFC 9110 13.1.2):
     * W/"x" and "x" name the same representation, so the W/ marker is skipped
     * rather than being grounds to refuse.
     *
     * <p>REVIEW ASKED FOR THIS AGAINST AN EXAMPLE THAT DOES NOT ACTUALLY MATCH.
     * The reading was that the old `ifNoneMatch.indexOf(etag) >= 0` would take
     * "prefix10-20" for "10-20". It would not: etag is built WITH its quotes
     * three lines above the call, so the needle is the seven characters
     * {@code "10-20"} including both of them, and in {@code "prefix10-20"} the
     * opening quote is followed by 'p'. An ETag cannot contain a quote either
     * (RFC 9110 etagc excludes DQUOTE), so no other single validator's quoted
     * form can embed ours, and for a well formed field the substring test agreed
     * with list membership in every case.
     *
     * <p>It is replaced anyway, because being right for that reason means being
     * right only while the tag stays quoted. Whoever later writes an unquoted
     * validator -- or compares a tag this method does not build -- turns a
     * correct line into a cache-poisoning one without touching it. Reading the
     * list is the same work and does not depend on anything three lines away.
     */
    private static boolean etagListMatches(String header, String etag) {
        String value = header.trim();
        if("*".equals(value)) {
            return true;
        }
        int at = 0;
        while(at < value.length()) {
            char c = value.charAt(at);
            if(c == ' ' || c == '\t' || c == ',') {
                at++;
                continue;
            }
            if((c == 'W' || c == 'w') && at + 1 < value.length()
                    && value.charAt(at + 1) == '/') {
                at += 2;
            }
            if(at >= value.length() || value.charAt(at) != '"') {
                // Not a validator, so the field is malformed. Answering false
                // sends the representation, which is the safe direction: a wrong
                // 304 is a body the client never receives and cannot ask for
                // again until its cache expires.
                return false;
            }
            int close = value.indexOf('"', at + 1);
            if(close < 0) {
                return false;
            }
            if(value.substring(at, close + 1).equals(etag)) {
                return true;
            }
            at = close + 1;
        }
        return false;
    }

    private static boolean isNotModified(HttpServer.Request request, String etag, long modified) {
        String ifNoneMatch = request.getHeader("if-none-match");
        if(ifNoneMatch != null) {
            // An ETag match wins outright; a date is only consulted when there is
            // no ETag to compare, as HTTP requires.
            return etagListMatches(ifNoneMatch, etag);
        }
        String ifModifiedSince = request.getHeader("if-modified-since");
        if(ifModifiedSince == null) {
            return false;
        }
        long since = Http1Date.parse(ifModifiedSince);
        // Second granularity on the wire, so compare at that resolution.
        return since >= 0 && modified / 1000 <= since / 1000;
    }

    /**
     * Whether {@code text} is empty, or a run of ASCII digits and nothing else.
     * Empty means the bound was absent, which both range forms allow.
     */
    private static boolean isUnsignedDigits(String text) {
        for(int iter = 0 ; iter < text.length() ; iter++) {
            char c = text.charAt(iter);
            if(c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /** Returns {offset, length}, or null when the range cannot be satisfied. */
    /**
     * Returned when the Range field cannot be honoured but nothing about it is
     * wrong: the whole representation is sent, with a 200, exactly as if the
     * client had not asked. Distinct from null, which means every range asked
     * for is unsatisfiable and 416 is the answer.
     */
    static final long[] IGNORE_RANGE = new long[0];

    static long[] parseRange(String header, long size) {
        String value = header.trim();
        if(!value.startsWith("bytes=")) {
            return IGNORE_RANGE;
        }
        value = value.substring("bytes=".length());
        if(value.indexOf(',') >= 0) {
            // Multi-range needs a multipart/byteranges body, which this does not
            // build. But NOT satisfying a range is not the same as the range being
            // unsatisfiable, and 416 says the second: RFC 9110 15.5.17 is for the
            // case where none of what was asked for exists, and "bytes=0-99,200-299"
            // over a large enough file is entirely satisfiable -- this server simply
            // will not assemble it. The rule for a Range that cannot be honoured is
            // to IGNORE the field and send the whole representation, which every
            // client understands, rather than to refuse a request that is correct.
            return IGNORE_RANGE;
        }
        int dash = value.indexOf('-');
        if(dash < 0) {
            // Not a byte-range-spec at all. RFC 9110 14.2 says to IGNORE a Range
            // the server cannot parse, not to refuse the request over it -- 416
            // asserts that what was asked for does not exist, which is a claim
            // this cannot make about a field it did not understand.
            return IGNORE_RANGE;
        }
        String fromText = value.substring(0, dash);
        String toText = value.substring(dash + 1);
        // EACH BOUND IS AN UNSIGNED RUN OF ASCII DIGITS, or it is absent.
        // Long.parseLong accepts a sign and Character.digit's whole repertoire, so
        // "bytes=+0-1" parsed as 0-1 and answered 206 to a field that is not a
        // byte-range-spec at all -- as would Arabic-Indic digits. The .trim() that
        // used to be here was the same leniency once more: RFC 9112 allows no
        // whitespace inside the spec, and accepting it invents a range the client
        // did not write.
        //
        // Ignored rather than 416, which is what makes being strict here safe: the
        // client gets the WHOLE representation, which every client understands.
        // RFC 9110 14.2 says to ignore a Range that cannot be parsed; 416 asserts
        // that what was asked for does not exist, and that is a claim this cannot
        // make about a field it did not understand.
        if(!isUnsignedDigits(fromText) || !isUnsignedDigits(toText)
                || (fromText.length() == 0 && toText.length() == 0)) {
            return IGNORE_RANGE;
        }
        if(size == 0) {
            // No range over a zero-length representation can be satisfied, and the
            // suffix form quietly produced one: "bytes=-1" clamped to a length of 0
            // and answered 206 with "Content-Range: bytes 0--1/0", which is not a
            // header any client can read. 416 is the whole of the correct answer.
            return null;
        }
        try {
            if(fromText.length() == 0) {
                // "-N" is the last N bytes.
                long n = Long.parseLong(toText);
                if(n <= 0) {
                    return null;
                }
                if(n > size) {
                    n = size;
                }
                return new long[]{size - n, n};
            }
            long from = Long.parseLong(fromText);
            if(from < 0 || from >= size) {
                return null;
            }
            long to = toText.length() == 0 ? size - 1 : Long.parseLong(toText);
            if(to >= size) {
                to = size - 1;
            }
            if(to < from) {
                return null;
            }
            return new long[]{from, to - from + 1};
        } catch (NumberFormatException err) {
            // Digits that are not digits: unparseable, so ignored for the same
            // reason as above rather than answered 416.
            return IGNORE_RANGE;
        }
    }

    /**
     * Writes length bytes of fileFd to the socket. Loops because sendfile may move
     * less than asked, which is normal rather than an error.
     */
    static void sendBody(int socketFd, long session, int fileFd, long offset, long length) throws IOException {
        long remaining = length;
        long position = offset;
        // sendfile works because the kernel moves bytes it never has to look at.
        // TLS bytes have to be encrypted in user space first, so there is no
        // zero-copy path there and there never will be.
        if(HAVE_SENDFILE && session == 0) {
            while(remaining > 0) {
                long sent = FileIo.sendFile(socketFd, fileFd, position, remaining);
                if(sent < 0) {
                    throw new IOException("sendfile failed");
                }
                if(sent == 0) {
                    // No progress and no error: the peer is gone.
                    throw new IOException("connection closed while sending");
                }
                position += sent;
                remaining -= sent;
            }
            return;
        }
        copyBody(socketFd, session, fileFd, offset, length);
    }

    /** The read/write path: no sendfile on this platform, or the socket is TLS. */
    static void copyBody(int socketFd, long session, int fileFd, long offset, long length) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long remaining = length;
        // Only the plain path uses this with an offset; a fresh descriptor is at 0.
        long skipped = 0;
        while(skipped < offset) {
            int want = (int)Math.min(buffer.length, offset - skipped);
            int n = FileIo.read(fileFd, buffer, 0, want);
            if(n <= 0) {
                throw new IOException("Could not seek to the range start");
            }
            skipped += n;
        }
        while(remaining > 0) {
            int want = (int)Math.min(buffer.length, remaining);
            int n = FileIo.read(fileFd, buffer, 0, want);
            if(n <= 0) {
                throw new IOException("Unexpected end of file while sending");
            }
            if(session == 0) {
                ServerSocket.write(socketFd, buffer, 0, n);
            } else {
                Tls.write(session, buffer, 0, n);
            }
            remaining -= n;
        }
    }

    /**
     * Reads a range of the file into memory. Needed for HTTP/2, where the body has
     * to become DATA frames and so cannot take the sendfile path.
     */
    static byte[] readAll(int fd, long offset, long length) throws IOException {
        if(length > Integer.MAX_VALUE) {
            throw new IOException("File too large to buffer for HTTP/2");
        }
        byte[] out = new byte[(int)length];
        byte[] skip = new byte[64 * 1024];
        long skipped = 0;
        while(skipped < offset) {
            int want = (int)Math.min(skip.length, offset - skipped);
            int n = FileIo.read(fd, skip, 0, want);
            if(n <= 0) {
                throw new IOException("Could not seek to the range start");
            }
            skipped += n;
        }
        int filled = 0;
        while(filled < out.length) {
            int n = FileIo.read(fd, out, filled, out.length - filled);
            if(n <= 0) {
                throw new IOException("Unexpected end of file");
            }
            filled += n;
        }
        return out;
    }

    /**
     * Descriptors handed to a Response and not yet closed.
     *
     * Telemetry, and the only way a leak here is visible at all: a descriptor
     * that escapes is not counted by anything else, the process limit is in the
     * hundreds of thousands, and the failure arrives much later as a server that
     * cannot accept sockets. An HTTP/2 HEAD of a static file leaked one per
     * request precisely because nothing said so.
     */
    private static final java.util.concurrent.atomic.AtomicInteger OPEN_FILES =
            new java.util.concurrent.atomic.AtomicInteger();

    static int openFileCount() {
        return OPEN_FILES.get();
    }

    /** Counted where the descriptor becomes a Response's to own. */
    static int trackFile(int fd) {
        if(fd >= 0) {
            OPEN_FILES.incrementAndGet();
        }
        return fd;
    }

    /**
     * Gives up tracking WITHOUT closing: the HTTP/2 session owns this descriptor
     * now and frees it natively, so counting it here would climb for ever.
     *
     * The count means "descriptors this side still has to close" -- anything the
     * session holds is reported separately by Http2.pendingBodyFiles(). Mixing
     * the two made an ordinary h2 GET look like a leak, which is how this
     * distinction got noticed.
     */
    static void handOverFile(int fd) {
        if(fd >= 0) {
            OPEN_FILES.decrementAndGet();
        }
    }

    static void closeFile(int fd) {
        if(fd >= 0) {
            OPEN_FILES.decrementAndGet();
            FileIo.close(fd);
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.length() > 1 && value.endsWith("/")
                ? value.substring(0, value.length() - 1) : value;
    }

    /**
     * Null for a malformed escape rather than a partially decoded path.
     *
     * <p>EVERY character is an octet here, escaped or not, which is why they are
     * all gathered and the path is decoded once at the end. The request line
     * arrives as bytes and each becomes a char, so a client may send an accented
     * filename raw -- two characters, no escape anywhere -- and the earlier
     * version returned that untouched because it held no '%'. The name then went
     * to the open as UTF-8 of those two characters, which is four bytes and not
     * the file on disk, so the raw spelling 404'd while %C3%A9 found it. Same
     * request, two answers, and the fast path was the whole difference.
     */
    static String decode(String value) {
        byte[] octets = new byte[value.length()];
        int length = 0;
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c != '%') {
                // Anything wider than a byte did not come off the wire, and
                // narrowing it would invent an octet the client never sent.
                if(c > 0xff) {
                    return null;
                }
                octets[length++] = (byte)c;
                continue;
            }
            if(iter + 2 >= value.length()) {
                return null;
            }
            // Two HEX DIGITS, tested as digits. Integer.parseInt(_, 16) accepts a
            // sign, so "%+1" decoded to the byte 1 and "%-1" to -1 -- two more
            // spellings of an octet the client never wrote.
            int hi = Hex.digit(value.charAt(iter + 1));
            int lo = Hex.digit(value.charAt(iter + 2));
            if(hi < 0 || lo < 0) {
                return null;
            }
            octets[length++] = (byte)((hi << 4) | lo);
            iter += 2;
        }
        // The bytes have to BE UTF-8, not merely be spelled in valid hex. %C3%28
        // is a truncated two-byte sequence, and new String(_, "UTF-8") answers
        // U+FFFD rather than failing -- so that path resolved to the same file as
        // one genuinely containing U+FFFD, while a bad hex digit above was already
        // a 400. One file, two spellings, and only one of them checked.
        if(!Utf8.isValid(octets, 0, length)) {
            return null;
        }
        return utf8(octets, length);
    }

    /** The gathered escape bytes as text; malformed input keeps its bytes. */
    private static String utf8(byte[] bytes, int length) {
        try {
            return new String(bytes, 0, length, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            return new String(bytes, 0, length);
        }
    }

    /**
     * ASCII lower case, because String.toLowerCase() is LOCALE SENSITIVE and
     * this platform has no Locale to ask for the root one. On a device set to
     * Turkish the I of an ASCII token folds to a dotless i, so the result stops
     * equalling the constant it is compared against: nothing is thrown, nothing
     * is logged, and the feature is simply inert for those users. Every token
     * folded here -- a header name, a file extension -- is ASCII by
     * specification. Copied rather than shared; see CLAUDE.md.
     */
    private static String asciiLower(String value) {
        if(value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(value.length());
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char)(c + 32) : c);
        }
        return out.toString();
    }

    static String contentType(String path) {
        int dot = path.lastIndexOf('.');
        String ext = dot < 0 ? "" : asciiLower(path.substring(dot + 1));
        if("html".equals(ext) || "htm".equals(ext)) return "text/html; charset=utf-8";
        if("css".equals(ext)) return "text/css; charset=utf-8";
        if("js".equals(ext) || "mjs".equals(ext)) return "text/javascript; charset=utf-8";
        if("json".equals(ext)) return "application/json; charset=utf-8";
        if("svg".equals(ext)) return "image/svg+xml";
        if("png".equals(ext)) return "image/png";
        if("jpg".equals(ext) || "jpeg".equals(ext)) return "image/jpeg";
        if("gif".equals(ext)) return "image/gif";
        if("webp".equals(ext)) return "image/webp";
        if("ico".equals(ext)) return "image/x-icon";
        if("woff2".equals(ext)) return "font/woff2";
        if("woff".equals(ext)) return "font/woff";
        if("ttf".equals(ext)) return "font/ttf";
        if("wasm".equals(ext)) return "application/wasm";
        if("pdf".equals(ext)) return "application/pdf";
        if("txt".equals(ext) || "md".equals(ext)) return "text/plain; charset=utf-8";
        if("xml".equals(ext)) return "application/xml";
        if("mp4".equals(ext)) return "video/mp4";
        if("zip".equals(ext)) return "application/zip";
        return "application/octet-stream";
    }
}
