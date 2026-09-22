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

/**
 * What an application implements to serve a websocket route.
 *
 * One instance per ROUTE, not per connection -- the same shape a `@RestController`
 * has. Everything that belongs to one client lives on the {@link WebSocketSession}
 * handed to each callback, so an endpoint can be stateless and a stateful one puts
 * its state in {@link WebSocketSession#setAttachment}.
 *
 * ```java
 * @WebSocketMapping("/echo")
 * public class Echo implements WebSocket {
 *     public void onOpen(WebSocketSession session) { }
 *     public void onText(WebSocketSession session, String message) throws Exception {
 *         session.sendText(message);
 *     }
 *     public void onBinary(WebSocketSession session, byte[] message, int offset, int length)
 *             throws Exception {
 *         session.sendBinary(message, offset, length);
 *     }
 * }
 * ```
 *
 * The build finds that and registers it. A server assembled by hand gets the same
 * thing through a callback rather than a setter -- see
 * {@link Backend.WebSocketEndpoints} -- because this runtime is asked for its
 * configuration while it starts and never reconfigured once it is running.
 *
 * Threading is the part worth reading twice. A callback runs on the thread that
 * owns the connection -- a virtual thread where the server has them, a pool worker
 * otherwise -- and the server reads nothing more from that connection until the
 * callback returns. So an endpoint may block, and a slow endpoint slows down its
 * own client and nobody else's. What it must not do is assume it is alone: a
 * session may be sent to from another thread entirely (that is what makes
 * broadcast possible), and {@link WebSocketSession} is written to allow it.
 *
 * The optional callbacks have empty defaults, so an endpoint overrides only what
 * it cares about. PING is answered automatically before {@link #onPing} is called;
 * an endpoint never has to write the PONG itself.
 */
public interface WebSocket {
    /**
     * The handshake is done and the 101 is on the wire. Sending here is allowed
     * and reaches the client after the handshake, which is the earliest anything
     * can.
     */
    void onOpen(WebSocketSession session) throws Exception;

    /**
     * A whole text message, reassembled from however many frames carried it, and
     * already validated as UTF-8.
     */
    void onText(WebSocketSession session, String message) throws Exception;

    /**
     * A whole binary message.
     *
     * The array is the session's own reassembly buffer and is valid only until
     * this call returns -- the same contract `HttpServer.Request` carries, and for
     * the same reason: it is what lets a 10MB message arrive without a copy. An
     * endpoint that needs to keep the bytes copies the range it wants.
     */
    void onBinary(WebSocketSession session, byte[] message, int offset, int length)
            throws Exception;

    /** A PING arrived. The PONG has already been sent. The payload is borrowed. */
    default void onPing(WebSocketSession session, byte[] payload, int offset, int length)
            throws Exception {
    }

    /** A PONG arrived, answering a {@link WebSocketSession#sendPing}. Borrowed. */
    default void onPong(WebSocketSession session, byte[] payload, int offset, int length)
            throws Exception {
    }

    /**
     * The connection is finished, for any reason, exactly once per session.
     *
     * `code` is the close code, or 1006 when the connection dropped without one --
     * which is normal, not exceptional: a process killed mid-suite, a network that
     * went away, a browser tab closed. `reason` may be empty and is never null.
     * Sending from here does nothing; the socket is already going.
     */
    default void onClose(WebSocketSession session, int code, String reason) throws Exception {
    }

    /**
     * Something went wrong: a protocol violation by the peer, an I/O failure, or
     * an exception thrown by one of the callbacks above.
     *
     * Reported rather than thrown, because by the time a websocket fails there is
     * nobody left to throw to -- the request that started it returned long ago.
     * {@link #onClose} still follows.
     */
    default void onError(WebSocketSession session, Exception error) {
    }

    /**
     * The subprotocols this endpoint speaks, best first, or null for none.
     *
     * The SERVER's order decides, not the client's. RFC 6455 leaves the choice
     * open, and honouring the client's order would let a peer select a deprecated
     * protocol over a current one simply by listing it first.
     */
    default String[] getSubprotocols() {
        return null;
    }
}
