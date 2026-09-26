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
package com.codename1.backend.mcp;

import java.util.Map;

/**
 * One tool on the server's MCP endpoint.
 *
 * <p>An {@code @McpTool} method becomes one of these through a class the build
 * generates, whose schema is a constant and whose {@link #call} converts the
 * arguments and invokes the method directly. Write one by hand for a tool that is
 * not a bean method, and pass it to {@link McpServer#register}.
 */
public interface McpTool {
    /** The tool's name, unique on the server. */
    String name();

    /** What it does, for the agent choosing between tools. */
    String description();

    /** The JSON Schema of its arguments: an object schema, as a map. */
    Map inputSchema();

    /**
     * Runs the tool. The result is written with {@link com.codename1.backend.Json}
     * as the call's text content.
     *
     * @throws IllegalArgumentException for arguments the tool cannot use, which
     *         the agent is told as a tool error it can correct
     */
    Object call(Map arguments) throws Exception;
}
