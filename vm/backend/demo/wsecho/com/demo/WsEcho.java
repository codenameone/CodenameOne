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
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;

import java.io.IOException;

/**
 * An echo server, and nothing else. The Autobahn|Testsuite fuzzing client drives
 * this one.
 *
 * Deliberately the smallest endpoint that can be written: Autobahn is testing the
 * FRAME layer, and every line of application logic here is a line that could fail
 * a case for a reason that has nothing to do with RFC 6455. Text comes back as
 * text and binary as binary, because a server that answered a text message with a
 * binary frame would pass the framing cases and fail the echo comparison.
 *
 * Ports and limits come from the environment so the harness can raise the message
 * ceiling for the 9.* cases, which send up to 16MB.
 */
public class WsEcho {
    public static void main(String[] args) throws Exception {
        int port = 9001;
        int workers = 8;
        String host = "127.0.0.1";
        for(int iter = 0 ; iter < args.length ; iter++) {
            if("--port".equals(args[iter]) && iter + 1 < args.length) {
                port = Integer.parseInt(args[++iter]);
            } else if("--host".equals(args[iter]) && iter + 1 < args.length) {
                host = args[++iter];
            } else if("--workers".equals(args[iter]) && iter + 1 < args.length) {
                workers = Integer.parseInt(args[++iter]);
            }
        }
        HttpServer server = HttpServer.start(host, port, 128, workers, null, null,
                new HttpServer.WebSocketRoutes() {
            public void register(HttpServer.WebSocketRegistry registry) {
                registry.fallback(new HttpServer.WebSocketHandler() {
                    public WebSocket open(HttpServer.Request request) {
                        return new Echo();
                    }
                });
            }
        });
        System.out.println("WSECHO_PORT=" + server.getPort());
        System.out.flush();
        server.awaitTermination();
    }

    static final class Echo implements WebSocket {
        public void onOpen(WebSocketSession session) {
        }

        public void onText(WebSocketSession session, String message) throws IOException {
            session.sendText(message);
        }

        public void onBinary(WebSocketSession session, byte[] message, int offset, int length)
                throws IOException {
            session.sendBinary(message, offset, length);
        }
    }
}
