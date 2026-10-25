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
package com.codenameone.developerguide.backend;

import com.codename1.backend.HttpProxy;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Web;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RestController;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;

/** Streaming request and response bodies, and a reverse proxy. */
@RestController
public class Streaming {
    private final HttpProxy images;

    public Streaming() throws IOException {
        // tag::backend-web-proxy-create[]
        images = new HttpProxy("https://images.internal:8443")
                .allowHost("cdn.internal");
        // end::backend-web-proxy-create[]
    }

    // tag::backend-web-upload-stream[]
    @PostMapping("/archive")
    public Map<String, Long> archive(HttpServer.Request request) throws IOException {
        long total = 0;
        byte[] buffer = new byte[64 * 1024];
        InputStream in = request.getBodyStream();
        try {
            for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
                total += n;                 // store, hash or forward each block
            }
        } finally {
            in.close();
        }
        return Collections.singletonMap("bytes", total);
    }
    // end::backend-web-upload-stream[]

    // tag::backend-web-download-stream[]
    @GetMapping("/report.csv")
    public HttpServer.Response report() {
        return HttpServer.Response.stream(200, "text/csv", new HttpServer.BodyWriter() {
            public void writeTo(OutputStream out) throws IOException {
                for (int row = 0; row < 1000000; row++) {
                    out.write((row + "," + row * row + "\n").getBytes("UTF-8"));
                }
            }
        });
    }
    // end::backend-web-download-stream[]

    // tag::backend-web-exchange[]
    static long upload(InputStream file, long length) throws IOException {
        Web.Exchange exchange = Web.open("PUT", "https://storage.internal/blobs/1",
                Collections.singletonList("Content-Type: application/octet-stream"), length);
        try {
            OutputStream up = exchange.getRequestBody();
            byte[] buffer = new byte[64 * 1024];
            for (int n = file.read(buffer); n > 0; n = file.read(buffer)) {
                up.write(buffer, 0, n);
            }
            up.close();
            if (exchange.getStatus() >= 300) {
                throw new IOException("storage answered " + exchange.getStatus());
            }
            InputStream down = exchange.getResponseBody();
            long answered = 0;
            while (down.read() >= 0) {
                answered++;
            }
            return answered;
        } finally {
            exchange.close();               // before the end, this cancels it
        }
    }
    // end::backend-web-exchange[]

    // tag::backend-web-proxy-forward[]
    @RequestMapping("/images/{name}")
    public HttpServer.Response image(HttpServer.Request request) {
        return images.forward(request);  // the same path and query, upstream
    }
    // end::backend-web-proxy-forward[]
}
