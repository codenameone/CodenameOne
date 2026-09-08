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
        String method = request.getMethod();
        if(!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return HttpServer.Response.text(405, "method not allowed");
        }
        String target = request.getTarget();
        int q = target.indexOf('?');
        if(q >= 0) {
            target = target.substring(0, q);
        }
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
            long[] info = new long[3];
            if(FileIo.stat(fd, info) != 0) {
                return HttpServer.Response.text(404, "not found");
            }
            if(info[2] != 0) {
                // A directory: retry at its index file rather than listing it.
                // Directory listings leak names nobody asked to publish.
                FileIo.close(fd);
                release = false;
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
            // JavaAPI's Long has neither toHexString nor a radix toString. An
            // ETag only has to be stable and opaque, so size-mtime in decimal is
            // exactly as good a validator.
            String etag = "\"" + size + "-" + modified + "\"";

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
            if(range != null) {
                long[] parsed = parseRange(range, size);
                if(parsed == null) {
                    headers.put("Content-Range", "bytes */" + size);
                    FileIo.close(fd);
                    release = false;
                    return HttpServer.Response.empty(416, contentType(decoded), headers);
                }
                offset = parsed[0];
                length = parsed[1];
                status = 206;
                headers.put("Content-Range", "bytes " + offset + "-" + (offset + length - 1) + "/" + size);
            }

            release = false; // the server owns the descriptor from here
            return HttpServer.Response.file(status, contentType(decoded), fd, offset, length, headers);
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
        // not inside it.
        return real.startsWith(root.endsWith("/") ? root : root + "/");
    }

    private static boolean isNotModified(HttpServer.Request request, String etag, long modified) {
        String ifNoneMatch = request.getHeader("if-none-match");
        if(ifNoneMatch != null) {
            // An ETag match wins outright; a date is only consulted when there is
            // no ETag to compare, as HTTP requires.
            return ifNoneMatch.indexOf(etag) >= 0 || "*".equals(ifNoneMatch.trim());
        }
        String ifModifiedSince = request.getHeader("if-modified-since");
        if(ifModifiedSince == null) {
            return false;
        }
        long since = Http1Date.parse(ifModifiedSince);
        // Second granularity on the wire, so compare at that resolution.
        return since >= 0 && modified / 1000 <= since / 1000;
    }

    /** Returns {offset, length}, or null when the range cannot be satisfied. */
    static long[] parseRange(String header, long size) {
        String value = header.trim();
        if(!value.startsWith("bytes=")) {
            return null;
        }
        value = value.substring("bytes=".length());
        if(value.indexOf(',') >= 0) {
            // Multi-range needs a multipart/byteranges body. Refusing is allowed
            // and honest; pretending to satisfy only the first range is not.
            return null;
        }
        int dash = value.indexOf('-');
        if(dash < 0) {
            return null;
        }
        String fromText = value.substring(0, dash).trim();
        String toText = value.substring(dash + 1).trim();
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
            return null;
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

    static void closeFile(int fd) {
        if(fd >= 0) {
            FileIo.close(fd);
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.length() > 1 && value.endsWith("/")
                ? value.substring(0, value.length() - 1) : value;
    }

    /** Null for a malformed escape rather than a partially decoded path. */
    static String decode(String value) {
        if(value.indexOf('%') < 0) {
            return value;
        }
        // A run of escapes is one UTF-8 sequence, not one character per octet.
        // Appending each octet as a char turned the %C3%A9 a client sends for an
        // accented letter into two characters, so the lookup missed a file that is
        // on disk and the request 404'd.
        StringBuilder out = new StringBuilder();
        byte[] pending = new byte[value.length()];
        int pendingLength = 0;
        for(int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            if(c != '%') {
                if(pendingLength > 0) {
                    out.append(utf8(pending, pendingLength));
                    pendingLength = 0;
                }
                out.append(c);
                continue;
            }
            if(iter + 2 >= value.length()) {
                return null;
            }
            try {
                pending[pendingLength++] =
                        (byte)Integer.parseInt(value.substring(iter + 1, iter + 3), 16);
            } catch (NumberFormatException err) {
                return null;
            }
            iter += 2;
        }
        if(pendingLength > 0) {
            out.append(utf8(pending, pendingLength));
        }
        return out.toString();
    }

    /** The gathered escape bytes as text; malformed input keeps its bytes. */
    private static String utf8(byte[] bytes, int length) {
        try {
            return new String(bytes, 0, length, "UTF-8");
        } catch (java.io.UnsupportedEncodingException err) {
            return new String(bytes, 0, length);
        }
    }

    static String contentType(String path) {
        int dot = path.lastIndexOf('.');
        String ext = dot < 0 ? "" : path.substring(dot + 1).toLowerCase();
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
