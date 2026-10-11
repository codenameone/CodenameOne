/* Copyright (c) 2026 Codename One. GPLv2 with the Classpath exception. */
package com.demo;

import com.codename1.backend.Backend;
import com.codename1.backend.HttpServer;
import com.codename1.io.zip.ZipArchiveOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

/** A small, fixed-size downloadable project. No temporary files are created. */
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
            zip.write("#!/bin/sh\nprintf 'Hello from the archive\\n'\n".getBytes("UTF-8"));
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
        // This demo contains only the fixed literals above. Do not accumulate
        // arbitrary user files in memory: write those to an owned output stream.
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        archive(bytes);
        final byte[] download = bytes.toByteArray();
        Backend server =
                Backend.builder()
                        .host("127.0.0.1")
                        .port(8099)
                        .handler(new HttpServer.Handler() {
                            public HttpServer.Response handle(HttpServer.Request request) {
                                if (!"GET".equals(request.getMethod()) || !"/project.zip".equals(request.getTarget()))
                                    return HttpServer.Response.text(404, "Not found");
                                return new HttpServer.Response(200, "application/zip", download)
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
