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
package com.codename1.mcp;

import java.io.IOException;

/// A bidirectional line delimited transport for MCP JSON-RPC messages.
///
/// The MCP stdio transport frames messages as newline delimited JSON, one complete
/// JSON-RPC object per line with no embedded newlines. Implementations therefore
/// exchange whole message strings and never deal with partial frames.
public interface MCPTransport {
    /// Opens the transport, blocking until it is ready to exchange messages. For a
    /// socket transport this waits for the first client connection.
    void open() throws IOException;

    /// Reads the next complete JSON-RPC message. Returns null at end of stream.
    String readMessage() throws IOException;

    /// Writes a complete JSON-RPC message and flushes it. The value must not contain
    /// embedded newlines.
    void writeMessage(String message) throws IOException;

    /// Closes the transport and releases any underlying resources.
    void close();
}
