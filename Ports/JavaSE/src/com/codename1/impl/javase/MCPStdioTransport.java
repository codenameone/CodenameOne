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
package com.codename1.impl.javase;

import com.codename1.mcp.MCP;
import com.codename1.mcp.MCPTransport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;

/// The standard MCP stdio transport for the JavaSE desktop port: newline delimited
/// JSON-RPC over the process standard input and output. It lives in the JavaSE port
/// rather than the portable core because it depends on {@link System#in} and
/// {@link System#setOut(PrintStream)}, which are not available on every Codename One
/// target (notably the ParparVM Java runtime).
///
/// While open, the process standard output is reserved exclusively for the protocol, so
/// {@link System#out} is redirected to {@link System#err} to keep application logging
/// from corrupting the JSON-RPC stream. The original stream is retained for writes.
public class MCPStdioTransport implements MCPTransport {
    private final InputStream in;
    private final PrintStream protocolOut;
    private final boolean redirectStandardOut;
    private BufferedReader reader;
    private Writer writer;
    private PrintStream previousOut;

    public MCPStdioTransport() {
        this(System.in, System.out, true);
    }

    public MCPStdioTransport(InputStream input, OutputStream output, boolean redirectStandardOut) {
        this.in = input;
        this.protocolOut = output instanceof PrintStream ? (PrintStream) output : new PrintStream(output);
        this.redirectStandardOut = redirectStandardOut;
    }

    /// Registers this transport as the platform stdio factory so
    /// {@link MCP#startStdioServer()} works on the desktop. Safe to call more than once.
    public static void register() {
        MCP.setStdioTransportFactory(new MCP.StdioTransportFactory() {
            @Override
            public MCPTransport createStdioTransport() {
                return new MCPStdioTransport();
            }
        });
    }

    @Override
    public void open() throws IOException {
        reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        writer = new OutputStreamWriter(protocolOut, "UTF-8");
        if (redirectStandardOut) {
            previousOut = System.out;
            System.setOut(System.err);
        }
    }

    @Override
    public String readMessage() throws IOException {
        if (reader == null) {
            return null;
        }
        return reader.readLine();
    }

    @Override
    public void writeMessage(String message) throws IOException {
        writer.write(message);
        writer.write('\n');
        writer.flush();
    }

    @Override
    public void close() {
        if (redirectStandardOut && previousOut != null) {
            System.setOut(previousOut);
            previousOut = null;
        }
        try {
            if (writer != null) {
                writer.flush();
            }
        } catch (IOException ignored) {
            // best effort flush on shutdown
        }
    }
}
