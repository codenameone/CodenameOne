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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

/** A streaming project download. No archive buffer or temporary files are created. */
public final class ZipDownload {
    private static void archive(OutputStream destination) throws IOException {
        // Closing the ZIP owns destination. For a caller-owned stream use
        // finish(), then release the writer with a non-closing destination wrapper.
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(destination)) {
            ZipEntry dir = new ZipEntry("project/");
            dir.setTime(0);
            zip.putNextEntry(dir, 0755);
            zip.closeEntry();
            ZipEntry launcher = new ZipEntry("project/run.sh");
            launcher.setTime(0);
            zip.putNextEntry(launcher, 0755);
            // Open sources only inside the callback and close even when the client cancels.
            try (InputStream source = new ByteArrayInputStream(
                    "#!/bin/sh\nprintf 'Hello from the archive\\n'\n".getBytes("UTF-8"))) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = source.read(buffer)) != -1) {
                    zip.write(buffer, 0, count);
                }
            }
            zip.closeEntry();
            ZipEntry readme = new ZipEntry("project/README.txt");
            readme.setTime(0);
            zip.putNextEntry(readme, 0644);
            zip.write("Run ./run.sh after extraction.\n".getBytes("UTF-8"));
            zip.closeEntry();
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--write".equals(args[0])) {
            archive(new FileOutputStream(args[1]));
            System.out.println("ZIP_DOWNLOAD_OK");
            return;
        }
        Backend server =
                Backend.builder()
                        .host("127.0.0.1")
                        .port(8099)
                        .handler(new HttpServer.Handler() {
                            public HttpServer.Response handle(HttpServer.Request request) {
                                if ((!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod()))
                                        || !"/project.zip".equals(request.getTarget()))
                                    return HttpServer.Response.text(404, "Not found");
                                return HttpServer.Response.stream(200, "application/zip", new HttpServer.BodyWriter() {
                                    public void writeTo(OutputStream destination) throws IOException {
                                        archive(destination);
                                    }
                                })
                                        .header("Content-Disposition", "attachment; filename=project.zip");
                            }
                        })
                        .start();
        try {
            server.awaitTermination();
        } finally {
            server.stop();
        }
    }
}
