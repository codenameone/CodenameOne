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

import java.io.Closeable;
import java.io.IOException;

/// A broker connection: what sends messages and runs subscriptions. One per
/// server, opened by [Messaging#current()] from the configuration; application
/// code sends through [MessageTemplate] and receives through `@MessageListener`
/// methods rather than calling this directly.
///
/// Delivery is at least once. A handler that returns acknowledges its message;
/// one that throws has it delivered again after a backoff, and after
/// `cn1.messaging.retry.max-attempts` deliveries it goes to the destination's
/// dead-letter destination, `<destination>.dlq`, with the failure in its
/// [Message#ERROR] header.
public abstract class MessageTransport implements Closeable {
    /// The scheme this transport was opened for: `memory`, `db`, `redis`,
    /// `amqp`, `kafka`.
    public abstract String getName();

    /// Sends `message`. With a database transport inside a transaction the
    /// message is written in that transaction and is seen only if it commits.
    public abstract void send(Message message) throws IOException;

    /// Starts `concurrency` consumers of `destination` in `group`.
    ///
    /// - `group`: subscriptions of one group share the messages between them,
    ///   and each group sees every message -- on a transport with groups (Redis
    ///   Streams, Kafka, AMQP through one queue per group). On `memory:` and
    ///   `db:` a destination is a queue and every subscription competes for it.
    public abstract Subscription subscribe(String destination, String group, int concurrency,
                                           MessageHandler handler) throws IOException;

    /// Whether a send is part of the calling thread's database transaction by
    /// itself, so it needs no outbox.
    public boolean isTransactional() {
        return false;
    }

    /// Whether the broker answers, for the health endpoint. Never throws.
    public boolean isHealthy() {
        return true;
    }

    /// Stops every subscription and closes the connection. Never throws.
    @Override
    public abstract void close();
}
