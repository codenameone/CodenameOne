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

/// Model Context Protocol (MCP) headless API for Codename One applications.
///
/// This package lets any Codename One application expose itself to an LLM agent
/// (Claude Desktop/Code, Codex, opencode, and other MCP hosts) so the agent can
/// read the current screen, drive the user interface, and invoke domain specific
/// tools the application publishes. It is supported on desktop style targets that
/// own a standard input/output stream: the JavaSE port and ParparVM native desktop
/// builds.
///
/// The server is intentionally thin. It reuses the portable accessibility semantics
/// tree from {@link com.codename1.ui.accessibility} for automatic user interface
/// driving and the {@link com.codename1.ai.Tool} contract for developer defined
/// tools, so no new tool abstraction is introduced.
///
/// #### Getting started
///
/// ```
/// // From the application init/start, or via the desktop launcher --mcp-stdio flag:
/// MCP.startStdioServer();
///
/// // Publish an application specific tool:
/// MCP.addTool(new Tool("current_user", "Returns the signed in user",
///         "{\"type\":\"object\",\"properties\":{}}",
///         new ToolHandler() {
///             public String invoke(String argumentsJson) {
///                 return "{\"name\":\"" + session.getUser().getName() + "\"}";
///             }
///         }));
/// ```
package com.codename1.mcp;
