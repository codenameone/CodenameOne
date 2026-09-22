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
package com.demo;

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The host-side screenshot receiver for the on-device test suites, on the
 * Codename One backend.
 *
 * Every UI-test leg in this repository -- iOS, watchOS, tvOS, macOS, Catalyst,
 * Android, Java SE and the browser -- runs its suite on a device and streams each
 * captured PNG to this server over a websocket. Four independent client stacks
 * reach it: NSURLSessionWebSocketTask on the Apple platforms, the hand-rolled
 * Android and Java SE clients, and the browser's own WebSocket. That makes this
 * the most thoroughly exercised websocket server in the tree, which is exactly why
 * it is the one built on the backend: masked, fragmented, multi-hundred-kilobyte
 * binary messages under ACK-paced flow control, on every CI run.
 *
 * ## The protocol, which is a contract with the device side
 *
 * Per screenshot the device sends a text frame
 *
 *     META {"test":"&lt;name&gt;","png_bytes":&lt;n&gt;,"png_fnv1a64":"&lt;hex&gt;"}
 *
 * followed by the PNG as a binary message, and then blocks until this server
 * answers `ACK &lt;name&gt; status=&lt;ok|length_mismatch|hash_mismatch&gt;`. The ACK is
 * sent AFTER the bytes reach disk, and that ordering is the device's flow
 * control -- sending it earlier lets the next test overwrite a file this one has
 * not finished writing.
 *
 * `png_fnv1a64` is optional: the browser client omits it.
 *
 * ## Why the name is sanitised twice
 *
 * The name arrives over the wire, so it cannot be trusted even though the device
 * helper already restricts it. It is folded to `[A-Za-z0-9_.-]`, refused outright
 * if it holds a separator or is `.` or `..`, and the resolved path is then checked
 * to be inside the output directory -- guarding the exact File that is written
 * rather than a separately derived value, which is what CodeQL's
 * java/path-injection asks for.
 */
public class Cn1ssScreenshotServer {
    /** The port both ends hardcode. Nothing is injected per run. */
    private static final int DEFAULT_PORT = 8765;

    private static File outDir;
    /** png hash to the first test that produced it, for the duplicate warning. */
    private static final Map HASHES = new HashMap();
    private static int received;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        String out = "cn1ss-out";
        boolean selfCheck = false;
        for(int iter = 0 ; iter < args.length ; iter++) {
            if("--port".equals(args[iter]) && iter + 1 < args.length) {
                port = Integer.parseInt(args[++iter]);
            } else if("--out".equals(args[iter]) && iter + 1 < args.length) {
                out = args[++iter];
            } else if("--selfcheck".equals(args[iter])) {
                selfCheck = true;
            }
        }
        outDir = new File(out).getCanonicalFile();
        if(!outDir.isDirectory() && !outDir.mkdirs()) {
            System.err.println("[cn1ss] cannot create " + outDir);
            System.exit(2);
        }

        HttpServer server = HttpServer.start("127.0.0.1", port, 64, 4, null, null,
                new HttpServer.WebSocketRoutes() {
            public void register(HttpServer.WebSocketRegistry registry) {
                // Any path: the clients dial ws://host:8765 with no path, and the
                // one the server sees depends on which client substitutes what. A
                // fallback keeps that from being something four client stacks have
                // to agree on.
                registry.fallback(new HttpServer.WebSocketHandler() {
                    public WebSocket open(HttpServer.Request request) {
                        return new Receiver();
                    }
                });
            }
        });

        // The FIRST line, and the runner script polls for it before it starts the
        // simulator. On the translated arm stdout is unbuffered already; the flush
        // is for the JVM arm, so both behave alike.
        System.out.println("CN1SS_SERVER_PORT=" + server.getPort());
        System.out.flush();

        if(selfCheck) {
            System.exit(SelfCheck.run(server.getPort(), outDir) ? 0 : 1);
        }
        server.awaitTermination();
    }

    /** One connection. There is normally exactly one, from the device under test. */
    static final class Receiver implements WebSocket {
        private String pendingTest;
        private long pendingBytes = -1;
        private String pendingHash;

        public void onOpen(WebSocketSession session) {
        }

        public void onText(WebSocketSession session, String message) throws IOException {
            if(!message.startsWith("META ")) {
                return;
            }
            Map meta;
            try {
                meta = Json.parseObject(message.substring(5));
            } catch (IOException err) {
                System.err.println("[cn1ss] unparseable META: " + message);
                return;
            }
            pendingTest = string(meta.get("test"));
            pendingBytes = longOf(meta.get("png_bytes"));
            pendingHash = string(meta.get("png_fnv1a64"));
        }

        public void onBinary(WebSocketSession session, byte[] payload, int offset, int length)
                throws IOException {
            if(pendingTest == null) {
                System.err.println("[cn1ss] binary frame with no META; dropping " + length + " bytes");
                return;
            }
            String test = pendingTest;
            long declared = pendingBytes;
            String declaredHash = pendingHash;
            pendingTest = null;
            pendingBytes = -1;
            pendingHash = null;
            writeAndAck(session, test, declared, declaredHash, payload, offset, length);
        }

        public void onClose(WebSocketSession session, int code, String reason) {
        }

        public void onError(WebSocketSession session, Exception error) {
            System.err.println("[cn1ss] session error: " + error);
        }
    }

    static void writeAndAck(WebSocketSession session, String name, long declaredBytes,
                            String declaredHash, byte[] payload, int offset, int length)
            throws IOException {
        String safe = sanitise(name);
        if(safe == null) {
            System.err.println("[cn1ss] rejected unsafe test name: " + name);
            session.sendText("NACK rejected status=invalid_test_name");
            return;
        }
        File target = new File(outDir, safe + ".png").getCanonicalFile();
        // Guarding the exact File the stream below opens. Checking a value derived
        // separately from the one that is written leaves the sink unguarded as far
        // as dataflow analysis is concerned, which is the shape CodeQL flags.
        if(!isInside(outDir, target)) {
            System.err.println("[cn1ss] rejected out-of-base path: " + name + " -> " + target);
            session.sendText("NACK rejected status=path_escape");
            return;
        }

        String status = "ok";
        StringBuilder warn = new StringBuilder();
        if(declaredBytes >= 0 && declaredBytes != length) {
            status = "length_mismatch";
            warn.append("expected=").append(declaredBytes).append(",got=").append(length);
        }
        String actualHash = fnv1a64Hex(payload, offset, length);
        if(declaredHash != null && !declaredHash.equals(actualHash)) {
            if("ok".equals(status)) {
                status = "hash_mismatch";
            }
            if(warn.length() > 0) {
                warn.append(';');
            }
            warn.append("expected_hash=").append(declaredHash)
                .append(",actual_hash=").append(actualHash);
        }
        Object previous = HASHES.get(actualHash);
        if(previous == null) {
            HASHES.put(actualHash, safe);
        } else if(!previous.equals(safe)) {
            // Two tests that produced pixel-identical images. This is what catches
            // a stale frame being captured for a test that never rendered.
            System.out.println("CN1SS:WARN:test=" + safe + " duplicate_image_with=" + previous
                    + " png_fnv1a64=" + actualHash);
        }

        OutputStream stream = new FileOutputStream(target);
        try {
            stream.write(payload, offset, length);
        } finally {
            stream.close();
        }
        received++;
        System.out.println("CN1SS:INFO:test=" + safe + " png_bytes=" + length
                + " png_fnv1a64=" + actualHash + " status=" + status
                + (warn.length() == 0 ? "" : " warn=" + warn));
        // AFTER the bytes are on disk. The device blocks on this and moves to the
        // next test when it arrives, so an earlier ACK races the file write.
        session.sendText("ACK " + safe + " status=" + status);
    }

    static int getReceived() {
        return received;
    }

    /**
     * The name with every character outside `[A-Za-z0-9_.-]` replaced by `_`, or
     * null when it cannot be a file name at all.
     */
    static String sanitise(String name) {
        if(name == null || name.length() == 0) {
            return null;
        }
        if(name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.indexOf('\0') >= 0) {
            return null;
        }
        StringBuilder out = new StringBuilder(name.length());
        for(int iter = 0 ; iter < name.length() ; iter++) {
            char c = name.charAt(iter);
            boolean alphanumeric = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9');
            out.append(alphanumeric || c == '_' || c == '.' || c == '-' ? c : '_');
        }
        String result = out.toString();
        return ".".equals(result) || "..".equals(result) ? null : result;
    }

    /** Whether `target` really sits under `base`, both already canonical. */
    static boolean isInside(File base, File target) {
        String basePath = base.getPath();
        String targetPath = target.getPath();
        if(!targetPath.startsWith(basePath)) {
            return false;
        }
        // "/out" must not be read as a prefix of "/output".
        return targetPath.length() == basePath.length()
                || targetPath.charAt(basePath.length()) == File.separatorChar;
    }

    /** The same FNV-1a-64 the device computes, so the two can be compared. */
    static String fnv1a64Hex(byte[] bytes, int offset, int length) {
        long hash = 0xcbf29ce484222325L;
        long prime = 0x100000001b3L;
        for(int iter = 0 ; iter < length ; iter++) {
            hash ^= bytes[offset + iter] & 0xff;
            hash *= prime;
        }
        StringBuilder out = new StringBuilder(16);
        for(int shift = 60 ; shift >= 0 ; shift -= 4) {
            int nibble = (int)((hash >>> shift) & 0xf);
            out.append((char)(nibble < 10 ? '0' + nibble : 'a' + (nibble - 10)));
        }
        return out.toString();
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long longOf(Object value) {
        if(value instanceof Number) {
            return ((Number)value).longValue();
        }
        if(value == null) {
            return -1;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException err) {
            return -1;
        }
    }
}
