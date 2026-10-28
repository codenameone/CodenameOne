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
package com.codename1.io.mqtt;

import java.io.IOException;

/// A byte pipe to a broker. Implementations deliver every [Handler] callback
/// on the thread the client runs on (the EDT); the client never locks.
interface MqttTransport {
    /// Starts connecting. Exactly one of `opened` or `closed` follows.
    void open(Handler handler);

    /// Sends bytes; only valid between `opened` and `closed`.
    void send(byte[] data) throws IOException;

    /// Closes the pipe. No callback follows a call to this method.
    void close();

    /// Receives what the pipe produces.
    interface Handler {
        /// The pipe is ready for `send`.
        void opened();

        /// Bytes arrived; packet boundaries are not preserved.
        void received(byte[] data, int len);

        /// The pipe ended or failed.
        void closed(String reason);
    }
}
