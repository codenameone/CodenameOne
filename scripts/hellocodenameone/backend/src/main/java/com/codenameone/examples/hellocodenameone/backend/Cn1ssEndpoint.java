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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.Json;
import com.codename1.backend.WebSocket;
import com.codename1.backend.WebSocketSession;
import com.codename1.backend.annotations.WebSocketMapping;

import java.io.IOException;
import java.util.Map;

/// The host-side screenshot receiver for the on-device test suites.
///
/// Every UI-test leg in this repository -- iOS, watchOS, tvOS, macOS, Catalyst,
/// Android, Java SE, the browser, Linux and Windows -- runs its suite on a device
/// and streams each captured PNG here over a websocket. Several independent client
/// stacks reach it: NSURLSessionWebSocketTask on the Apple platforms, the
/// hand-rolled Android and Java SE clients, the browser's own WebSocket and the
/// native desktop ports. That makes this the most thoroughly exercised websocket
/// endpoint in the tree: masked, fragmented, multi-hundred-kilobyte binary
/// messages under ACK-paced flow control, on every CI run.
///
/// ## The protocol, which is a contract with the device side
///
/// Per screenshot the device sends a text frame
///
///     META {"test":"<name>","png_bytes":<n>,"png_fnv1a64":"<hex>"}
///
/// followed by the PNG as a binary message, and then blocks until this endpoint
/// answers `ACK <name> status=<ok|length_mismatch|hash_mismatch>`. The ACK is
/// sent AFTER the bytes reach disk, and that ordering is the device's flow
/// control -- sending it earlier lets the next test overwrite a file this one has
/// not finished writing. `png_fnv1a64` is optional: the browser client omits it.
///
/// `/` as well as `/cn1ss`, because the clients dial `ws://host:8765` and the
/// path the server sees depends on which client substitutes what.
@WebSocketMapping({"/cn1ss", "/"})
public class Cn1ssEndpoint implements WebSocket {
    private final ScreenshotStore store;

    public Cn1ssEndpoint(ScreenshotStore store) {
        this.store = store;
    }

    @Override
    public void onOpen(WebSocketSession session) {
    }

    @Override
    public void onText(WebSocketSession session, String message) throws IOException {
        if (!message.startsWith("META ")) {
            return;
        }
        Map meta;
        try {
            meta = Json.parseObject(message.substring(5));
        } catch (IOException err) {
            System.err.println("[cn1ss] unparseable META: " + message);
            return;
        }
        // Per connection, not per endpoint: the endpoint is one object for every
        // client, and a device that reconnects must not inherit a stale META.
        session.setAttachment(new Pending(string(meta.get("test")), longOf(meta.get("png_bytes")),
                string(meta.get("png_fnv1a64"))));
    }

    @Override
    public void onBinary(WebSocketSession session, byte[] payload, int offset, int length)
            throws IOException {
        Object attached = session.getAttachment();
        if (!(attached instanceof Pending)) {
            System.err.println("[cn1ss] binary frame with no META; dropping " + length + " bytes");
            return;
        }
        Pending pending = (Pending) attached;
        session.setAttachment(null);
        String reply = store.write(pending.test, pending.bytes, pending.hash, payload, offset,
                length);
        // AFTER the bytes are on disk. The device blocks on this and moves to the
        // next test when it arrives, so an earlier ACK races the file write.
        session.sendText(reply);
    }

    @Override
    public void onError(WebSocketSession session, Exception error) {
        System.err.println("[cn1ss] session error: " + error);
    }

    /// The META a binary message is waiting to be matched with.
    static final class Pending {
        final String test;
        final long bytes;
        final String hash;

        Pending(String test, long bytes, String hash) {
            this.test = test;
            this.bytes = bytes;
            this.hash = hash;
        }
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long longOf(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return -1;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException err) {
            return -1;
        }
    }
}
