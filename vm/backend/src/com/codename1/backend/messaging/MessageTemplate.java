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
package com.codename1.backend.messaging;

import com.codename1.backend.Json;
import com.codename1.backend.Transactions;
import com.codename1.impl.backend.WireBuffer;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// Sends messages through the server's transport: what an application injects
/// to publish, as it would Spring's `KafkaTemplate` or `RabbitTemplate`.
///
/// ```java
/// @Service
/// public class Orders {
///     private final MessageTemplate messages;
///     public Orders(MessageTemplate messages) { this.messages = messages; }
///
///     @Transactional
///     public void place(Order order) {
///         repository.save(order);
///         messages.send("orders.placed", order);
///     }
/// }
/// ```
///
/// A payload is sent as it is when it is a `byte[]`, as UTF-8 when it is a
/// `String`, and otherwise as JSON written by the codec the build generated
/// for its type.
///
/// Inside a database transaction a send happens only if the transaction
/// commits. The `db:` transport writes the message in the transaction itself;
/// a broker transport writes it to an outbox table in the transaction and
/// relays it after the commit, so a rollback sends nothing and a crash between
/// the commit and the relay sends it when the server comes back. `memory:`
/// holds the message until the commit.
public final class MessageTemplate {
    /// Sends `payload` to `destination`.
    public void send(String destination, Object payload) throws IOException {
        send(destination, null, payload);
    }

    /// Sends `payload` to `destination` with an ordering `key`, which may be null.
    public void send(String destination, String key, Object payload) throws IOException {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        byte[] body = encode(payload, headers);
        send(new Message(destination, key, body, headers));
    }

    /// Sends `message` as it is.
    public void send(final Message message) throws IOException {
        final MessageTransport t = Messaging.current(); // NOPMD CloseResource - the server's transport, closed when it stops
        if (t.isTransactional() || !Transactions.isActive()) {
            t.send(message);
            return;
        }
        if (t instanceof OutboxTransport) {
            ((OutboxTransport) t).stage(message);
            return;
        }
        Transactions.registerSynchronization(new Transactions.Synchronization() {
            @Override
            public void afterCommit() {
                try {
                    t.send(message);
                } catch (IOException err) {
                    System.err.println("cn1: a message to " + message.getDestination()
                            + " could not be sent after its transaction committed: "
                            + err.getMessage());
                }
            }
        });
    }

    /// The bytes `payload` is sent as, recording its content type in `headers`.
    static byte[] encode(Object payload, Map<String, String> headers) {
        if (payload instanceof byte[]) {
            headers.put(Message.CONTENT_TYPE, "application/octet-stream");
            return (byte[]) payload;
        }
        if (payload instanceof String) {
            headers.put(Message.CONTENT_TYPE, "text/plain");
            return WireBuffer.utf8((String) payload);
        }
        headers.put(Message.CONTENT_TYPE, "application/json");
        return WireBuffer.utf8(Json.write(payload));
    }
}
