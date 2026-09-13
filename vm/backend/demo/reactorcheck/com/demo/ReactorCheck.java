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

import com.codename1.backend.Reactor;
import com.codename1.backend.ServerSocket;
import com.codename1.backend.Tcp;

/**
 * Smoke test for the poller: bind, register, accept, register the accepted
 * connection, read from it. Small on purpose -- when the HTTP server went silent
 * this is what separated "the reactor never reports readiness" from "the server
 * has a bug", and the answer was the latter.
 *
 * Registration happens on the main thread and polling on another, because that is
 * how HttpServer uses it.
 */
public class ReactorCheck {
    public static void main(String[] args) throws Exception {
        final ServerSocket listener = ServerSocket.bind(null, 0, 16);
        final int port = listener.getPort();
        final Reactor reactor = Reactor.create();
        ServerSocket.setBlocking(listener.getFd(), false);
        reactor.add(listener.getFd(), Reactor.READ);
        System.out.println("listening on " + port + " fd=" + listener.getFd());

        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(300);
                    Tcp t = Tcp.connect("127.0.0.1", port, 0);
                    byte[] hello = "GET /x HTTP/1.1\r\n\r\n".getBytes("UTF-8");
                    t.write(hello, 0, hello.length);
                    Thread.sleep(2000);
                    t.close();
                } catch (Exception err) {
                    System.out.println("client failed: " + err);
                }
            }
        }).start();

        int[] ready = new int[16];
        for (int round = 0; round < 8; round++) {
            int n = reactor.await(ready, 1000);
            if (n <= 0) {
                continue;
            }
            if (ready[0] == listener.getFd()) {
                int client = listener.accept();
                if (client >= 0) {
                    ServerSocket.setBlocking(client, false);
                    reactor.add(client, Reactor.READ);
                    System.out.println("accepted fd=" + client);
                }
                continue;
            }
            byte[] buf = new byte[256];
            ServerSocket.setBlocking(ready[0], true);
            int got = ServerSocket.read(ready[0], buf, 0, buf.length);
            System.out.println("read " + got + " bytes: "
                    + (got > 0 ? new String(buf, 0, got, "UTF-8").trim() : ""));
            System.out.println(got > 0 ? "REACTOR OK" : "REACTOR FAILED: empty read");
            return;
        }
        System.out.println("REACTOR FAILED: no readiness reported");
    }
}
