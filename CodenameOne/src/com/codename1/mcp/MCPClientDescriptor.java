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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Describes the MCP server entry to register with a host: the stable server name it
/// appears under and the command line that launches the application in stdio MCP mode.
public final class MCPClientDescriptor {
    private final String serverName;
    private final String command;
    private final List<String> args;
    private final Map<String, String> env;

    public MCPClientDescriptor(String serverName, String command, List<String> args) {
        this(serverName, command, args, null);
    }

    public MCPClientDescriptor(String serverName, String command, List<String> args,
                               Map<String, String> env) {
        if (serverName == null || serverName.length() == 0) {
            throw new IllegalArgumentException("serverName is required");
        }
        if (command == null || command.length() == 0) {
            throw new IllegalArgumentException("command is required");
        }
        this.serverName = serverName;
        this.command = command;
        this.args = args == null ? new ArrayList<String>() : new ArrayList<String>(args);
        this.env = env == null ? null : new LinkedHashMap<String, String>(env);
    }

    public String getServerName() {
        return serverName;
    }

    public String getCommand() {
        return command;
    }

    public List<String> getArgs() {
        return args;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    /// Builds the JSON object stored under `mcpServers.<serverName>` in a host config.
    Map<String, Object> toServerEntry() {
        Map<String, Object> entry = new LinkedHashMap<String, Object>();
        entry.put("command", command);
        entry.put("args", new ArrayList<Object>(args));
        if (env != null && !env.isEmpty()) {
            entry.put("env", new LinkedHashMap<String, Object>(env));
        }
        return entry;
    }
}
