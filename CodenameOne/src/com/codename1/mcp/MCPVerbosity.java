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

/// How much of the MCP conversation the server echoes to the Codename One log, so a
/// developer can watch and debug what an agent is doing.
///
/// The levels are ordered from quietest to loudest. Each includes everything the level
/// below it prints.
public enum MCPVerbosity {
    /// No logging.
    OFF,
    /// Only failed calls (JSON-RPC errors and tools that report `isError`).
    ERRORS,
    /// One concise line per call: the method, the tool name, and whether it succeeded.
    SUMMARY,
    /// The full JSON-RPC request and response for every message.
    FULL;

    /// Whether this level prints at least as much as the given level.
    public boolean includes(MCPVerbosity level) {
        return ordinal() >= level.ordinal();
    }
}
