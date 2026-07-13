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
package com.codenameone.developerguide.snippets;

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.mcp.MCP;

/** Compiled source snippets for the MCP headless API guide chapter. */
public class McpSnippets {

    public void startSocketServer() {
        // tag::mcp-start-socket[]
        MCP.startSocketServer(8765);
        // end::mcp-start-socket[]
    }

    public void startStdioServer() {
        // tag::mcp-start-stdio[]
        MCP.startStdioServer();
        // end::mcp-start-stdio[]
    }

    public void publishTool(final String signedInUser) {
        // tag::mcp-add-tool[]
        MCP.addTool(new Tool(
                "current_user",
                "Returns the signed in user",
                "{\"type\":\"object\",\"properties\":{}}",
                new ToolHandler() {
                    public String invoke(String argumentsJson) {
                        return "{\"name\":\"" + signedInUser + "\"}";
                    }
                }));
        // end::mcp-add-tool[]
    }
}
