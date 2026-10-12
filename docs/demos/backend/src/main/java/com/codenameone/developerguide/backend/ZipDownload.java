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
package com.codenameone.developerguide.backend;

import com.codename1.backend.HttpServer;
import com.codename1.io.zip.ZipArchiveOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;

public final class ZipDownload {
    private static InputStream openLauncher() throws IOException {
        return new ByteArrayInputStream("#!/bin/sh\necho Hello\n".getBytes("UTF-8"));
    }

    // tag::backend-zip-download[]
    public static HttpServer.Response projectDownload() {
        return HttpServer.Response.stream(200, "application/zip", new HttpServer.BodyWriter() {
            public void writeTo(OutputStream destination) throws IOException {
                try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(destination)) {
                    ZipEntry directory = new ZipEntry("project/");
                    directory.setTime(0);
                    zip.putNextEntry(directory, 0755);
                    zip.closeEntry();

                    ZipEntry launcher = new ZipEntry("project/run.sh");
                    launcher.setTime(0);
                    zip.putNextEntry(launcher, 0755);
                    try (InputStream source = openLauncher()) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = source.read(buffer)) != -1) {
                            zip.write(buffer, 0, count);
                        }
                    }
                    zip.closeEntry();
                }
            }
        }).header("Content-Disposition", "attachment; filename=project.zip");
    }
    // end::backend-zip-download[]
}
