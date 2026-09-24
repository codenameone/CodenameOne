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

import com.codename1.backend.Backend;
import com.codename1.backend.DataSource;
import com.codename1.backend.orm.EntityManager;
import com.codename1.backend.HttpServer;
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The Backend chapter's websocket examples, compiled so they cannot drift. */
public final class WebSocketSnippets {

    private WebSocketSnippets() {
    }

// tag::backend-websocket-echo[]
public static final class Echo implements WebSocket {
    public void onOpen(WebSocketSession session) throws IOException {
        session.sendText("welcome");
    }

    public void onText(WebSocketSession session, String message) throws IOException {
        session.sendText(message);
    }

    public void onBinary(WebSocketSession session, byte[] message, int offset, int length)
            throws IOException {
        session.sendBinary(message, offset, length);
    }
}
// end::backend-websocket-echo[]

// tag::backend-websocket-register[]
public static void main(String[] args) throws Exception {
    Backend.builder()
            .webSockets(new Backend.WebSocketEndpoints() {
                public void register(HttpServer.WebSocketRegistry registry,
                                     DataSource dataSource, EntityManager entities) {
                    registry.route("/echo", new Echo());
                }
            })
            .run();
}
// end::backend-websocket-register[]

// tag::backend-websocket-annotated[]
@WebSocketMapping("/chat")
public static final class ChatEndpoint implements WebSocket {
    public void onOpen(WebSocketSession session) { }

    public void onText(WebSocketSession session, String message) { }

    public void onBinary(WebSocketSession session, byte[] message, int offset, int length) { }
}
// end::backend-websocket-annotated[]

// tag::backend-websocket-broadcast[]
public static final class Room implements WebSocket {
    // Every open session in this room. A websocket endpoint is one object shared
    // by every connection, so anything per-room lives here and anything per-client
    // lives on the session.
    private final List sessions = Collections.synchronizedList(new ArrayList());

    public void onOpen(WebSocketSession session) {
        sessions.add(session);
    }

    public void onText(WebSocketSession session, String message) {
        // Sending from a thread other than the one that owns a connection is
        // supported, and this is why: a broadcast reaches every session but one
        // from the thread that received the message.
        Object[] open = sessions.toArray();
        for(int iter = 0 ; iter < open.length ; iter++) {
            WebSocketSession other = (WebSocketSession)open[iter];
            if(other == session) {
                continue;
            }
            try {
                other.sendText(message);
            } catch (IOException err) {
                // A send fails when that peer has gone. It is already being torn
                // down; onClose will take it out of the list.
                other.abort();
            }
        }
    }

    public void onBinary(WebSocketSession session, byte[] message, int offset, int length) {
    }

    public void onClose(WebSocketSession session, int code, String reason) {
        sessions.remove(session);
    }
}
// end::backend-websocket-broadcast[]

// tag::backend-websocket-subprotocol[]
public static final class Graph implements WebSocket {
    public String[] getSubprotocols() {
        // The server's order decides, not the client's.
        return new String[]{"graphql-transport-ws", "graphql-ws"};
    }

    public void onOpen(WebSocketSession session) {
        if("graphql-ws".equals(session.getSubprotocol())) {
            // The legacy protocol; answer in its shape.
        }
    }

    public void onText(WebSocketSession session, String message) {
    }

    public void onBinary(WebSocketSession session, byte[] message, int offset, int length) {
    }
}
// end::backend-websocket-subprotocol[]

// tag::backend-websocket-raw[]
public static void serveForever() throws Exception {
    HttpServer server = HttpServer.start(null, 8080, 512, 16, new HttpServer.Handler() {
        public HttpServer.Response handle(HttpServer.Request request) {
            return HttpServer.Response.text(200, "ok");
        }
    }, null, new HttpServer.WebSocketRoutes() {
        public void register(HttpServer.WebSocketRegistry registry) {
            registry.route("/echo", new Echo());
        }
    });
    server.awaitTermination();
}
// end::backend-websocket-raw[]
}
