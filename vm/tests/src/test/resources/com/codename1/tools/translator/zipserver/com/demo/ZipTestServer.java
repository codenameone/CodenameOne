/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;
import com.codename1.io.zip.ZipArchiveOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;

/** Socket-test fixture: lazy sources expose cleanup without creating temporary files. */
public final class ZipTestServer {
    private static final AtomicInteger opened = new AtomicInteger();
    private static final AtomicInteger closed = new AtomicInteger();
    private static final AtomicInteger readBlocks = new AtomicInteger();

    private static final class Source extends InputStream {
        private final Random random = new Random(5998);
        private long remaining;
        Source(long length) { remaining = length; opened.incrementAndGet(); }
        public int read() throws IOException { throw new IOException("Use block reads"); }
        public int read(byte[] bytes, int offset, int length) {
            if (remaining == 0) return -1;
            int n = (int) Math.min(remaining, length);
            for (int i = 0; i < n; i++) bytes[offset + i] = (byte) random.nextInt();
            remaining -= n;
            readBlocks.incrementAndGet();
            return n;
        }
        public void close() { closed.incrementAndGet(); }
    }

    private static void archive(OutputStream destination, long length) throws IOException {
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(destination)) {
            for (String name : new String[] {"project/", "project/run.sh", "project/empty", "project/caf\u00e9.bin"}) {
                ZipEntry entry = new ZipEntry(name);
                entry.setTime(0);
                zip.putNextEntry(entry, name.endsWith("/") || name.endsWith(".sh") ? 0755 : 0644);
                if (name.endsWith(".sh")) zip.write("#!/bin/sh\necho ZIP_STREAM_OK\n".getBytes("UTF-8"));
                if (name.endsWith(".bin")) {
                    try (InputStream source = new Source(length)) {
                        byte[] buffer = new byte[8192];
                        int n;
                        while ((n = source.read(buffer)) != -1) zip.write(buffer, 0, n);
                    }
                }
                zip.closeEntry();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Backend.Builder builder = Backend.builder().host(System.getenv("ZIP_TEST_HOST") == null
                ? "127.0.0.1" : System.getenv("ZIP_TEST_HOST")).port(Integer.parseInt(args[0]))
                .handler(new HttpServer.Handler() {
                    public HttpServer.Response handle(HttpServer.Request request) {
                        String path = request.getTarget();
                        if ("/metrics".equals(path)) return HttpServer.Response.text(200,
                                opened.get() + " " + closed.get() + " " + readBlocks.get());
                        final long length = "/cancel.zip".equals(path) ? 1024L * 1024 * 1024
                                : "/large.zip".equals(path) ? 64L * 1024 * 1024 : 1024;
                        final boolean fail = "/fail".equals(path) || "/fail-runtime".equals(path);
                        final boolean runtimeFailure = "/fail-runtime".equals(path);
                        return HttpServer.Response.stream("/no-body".equals(path) ? 204 : 200,
                                "application/zip", new HttpServer.BodyWriter() {
                                    public void writeTo(OutputStream destination) throws IOException {
                                        if (fail) {
                                            try (InputStream source = new Source(32768)) {
                                                byte[] bytes = new byte[8192];
                                                int n;
                                                while ((n = source.read(bytes)) != -1) destination.write(bytes, 0, n);
                                                if (runtimeFailure) throw new IllegalStateException("deliberate producer failure");
                                                throw new IOException("deliberate producer failure");
                                            }
                                        }
                                        archive(destination, length);
                                    }
                                }).header("Content-Disposition", "attachment; filename=project.zip");
                    }
                });
        if (args.length == 3) builder.tls(args[1], args[2]);
        Backend server = builder.start();
        try { server.awaitTermination(); } finally { server.stop(); }
    }
}
