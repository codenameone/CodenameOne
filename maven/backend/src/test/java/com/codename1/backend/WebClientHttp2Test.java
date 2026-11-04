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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;

/// A WebClient that requires HTTP/2 goes through the JDK's java.net.http client
/// on this arm, and a server that answers in HTTP/1.1 fails the request instead
/// of serving it over the protocol the caller ruled out.
class WebClientHttp2Test {
    @Test
    void anHttp11ServerIsRefusedWhenHttp2IsRequired() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "one-one".getBytes("UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        WebClient client = new WebClient().setRequireHttp2(true);
        try {
            client.get("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            fail("an HTTP/1.1 answer must not satisfy a client that requires HTTP/2");
        } catch (IOException err) {
            String message = String.valueOf(err.getMessage());
            boolean java8 = System.getProperty("java.version").startsWith("1.");
            assertTrue(java8 ? message.indexOf("Java 11 or newer") >= 0
                    : message.indexOf("did not negotiate HTTP/2") >= 0, message);
        } finally {
            client.close();
            server.stop(0);
        }
    }
}
