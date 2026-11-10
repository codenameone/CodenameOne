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

import com.codename1.impl.backend.WireBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// One message: a destination, an optional key, headers, and the payload as
/// bytes. What a listener is handed when it asks for more than the payload, and
/// what [MessageTemplate#send(Message)] sends as it is.
///
/// The key is what a partitioned transport (Kafka) orders by; the others carry it
/// as a header. Header names starting with `cn1-` are the runtime's own: the
/// delivery attempt, the error a dead-lettered message failed with, and the
/// destination it came from.
public final class Message {
    /// The delivery attempt, counted from 1.
    public static final String ATTEMPT = "cn1-attempt";
    /// The text of the failure that dead-lettered a message.
    public static final String ERROR = "cn1-error";
    /// The destination a dead-lettered message was sent to first.
    public static final String ORIGIN = "cn1-origin";
    /// The time, in epoch milliseconds, before which a retried message is not
    /// handled again.
    public static final String NOT_BEFORE = "cn1-not-before";
    /// The content type of the payload: `application/json`, `text/plain` or
    /// `application/octet-stream`.
    public static final String CONTENT_TYPE = "content-type";

    private final String destination;
    private final String key;
    private final byte[] payload;
    private final Map<String, String> headers;
    private String id;

    /// A message to `destination` with no key.
    public Message(String destination, byte[] payload) {
        this(destination, null, payload, null);
    }

    /// A message to `destination`; `key` and `headers` may be null.
    public Message(String destination, String key, byte[] payload, Map<String, String> headers) {
        if (destination == null || destination.length() == 0) {
            throw new IllegalArgumentException("A message needs a destination");
        }
        this.destination = destination;
        this.key = key;
        this.payload = payload == null ? new byte[0] : payload;
        this.headers = headers == null ? new LinkedHashMap<String, String>()
                : new LinkedHashMap<String, String>(headers);
    }

    /// Where it is sent: a queue, a topic or a stream, whichever the transport has.
    public String getDestination() {
        return destination;
    }

    /// The ordering key, or null.
    public String getKey() {
        return key;
    }

    /// The payload. Not copied; do not change it.
    public byte[] getPayload() {
        return payload;
    }

    /// The payload as UTF-8 text.
    public String getText() {
        return WireBuffer.fromUtf8(payload);
    }

    /// The headers, which a listener may read but not change.
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /// One header, or null.
    public String getHeader(String name) {
        return headers.get(name);
    }

    /// Which delivery of this message this is, counted from 1.
    public int getAttempt() {
        String a = headers.get(ATTEMPT);
        if (a == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(a));
        } catch (NumberFormatException err) {
            return 1;
        }
    }

    /// The transport's identifier of this delivery, or null before it is sent.
    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    /// A copy with `name` set to `value`, or removed when `value` is null.
    public Message withHeader(String name, String value) {
        Map<String, String> h = new LinkedHashMap<String, String>(headers);
        if (value == null) {
            h.remove(name);
        } else {
            h.put(name, value);
        }
        return new Message(destination, key, payload, h);
    }

    /// A copy sent to `to` instead.
    public Message to(String to) {
        return new Message(to, key, payload, headers);
    }

    Map<String, String> headersForTransport() {
        return headers;
    }

    @Override
    public String toString() {
        return "Message[" + destination + (key == null ? "" : " key=" + key)
                + " " + payload.length + " bytes]";
    }
}
