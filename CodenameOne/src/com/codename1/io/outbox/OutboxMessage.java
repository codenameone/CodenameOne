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
package com.codename1.io.outbox;

/// One message waiting in an [Outbox], as the transport receives it.
///
/// Delivery is at least once: a message whose acknowledgement was lost is sent
/// again. The receiver deduplicates on [#getKey()], which no other message of
/// the same outbox ever carries.
@com.codename1.impl.SharedWithBackend
public final class OutboxMessage {
    private final long id;
    private final String key;
    private final String topic;
    private final byte[] payload;
    private final int attempts;
    private final long created;

    OutboxMessage(long id, String key, String topic, byte[] payload, int attempts, long created) {
        this.id = id;
        this.key = key;
        this.topic = topic;
        this.payload = payload;
        this.attempts = attempts;
        this.created = created;
    }

    /// The position of this message in its outbox; later messages have larger ids.
    public long getId() {
        return id;
    }

    /// The outbox name and the id, `name/id`. Stable across retries, so a
    /// receiver that already applied it can acknowledge and drop the copy.
    public String getKey() {
        return key;
    }

    /// The topic the message was enqueued under: a path, an event name, whatever
    /// the transport routes on.
    public String getTopic() {
        return topic;
    }

    /// The bytes enqueued. The array is this message's own; do not modify it.
    public byte[] getPayload() {
        return payload;
    }

    /// How many deliveries were attempted before this one.
    public int getAttempts() {
        return attempts;
    }

    /// When the message was enqueued, in milliseconds since the epoch.
    public long getCreated() {
        return created;
    }
}
