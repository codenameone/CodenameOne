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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Guards the completeness check that stops the registrar from overwriting a truncated or
/// corrupt host config. Codename One's JSON parser is lenient and returns a partial map
/// for malformed input rather than throwing, so this structural check is what protects the
/// user's other MCP servers.
class MCPClientRegistrarTest {

    @Test
    void acceptsWellFormedObjects() {
        assertTrue(MCPClientRegistrar.isCompleteJsonObject("{}"));
        assertTrue(MCPClientRegistrar.isCompleteJsonObject(
                "{\"mcpServers\":{\"a\":{\"command\":\"x\",\"args\":[\"1\",\"2\"]}}}"));
        // Braces and quotes inside strings must not confuse the scanner.
        assertTrue(MCPClientRegistrar.isCompleteJsonObject("{\"a\":\"}{[]\\\"\"}"));
    }

    @Test
    void rejectsTruncatedOrMalformedObjects() {
        assertFalse(MCPClientRegistrar.isCompleteJsonObject(""));
        assertFalse(MCPClientRegistrar.isCompleteJsonObject("{"));
        assertFalse(MCPClientRegistrar.isCompleteJsonObject("{\"a\":1"));
        assertFalse(MCPClientRegistrar.isCompleteJsonObject("{\"a\":{\"b\":1}"));
        assertFalse(MCPClientRegistrar.isCompleteJsonObject("{\"a\":\"unterminated}"));
        assertFalse(MCPClientRegistrar.isCompleteJsonObject("[1,2,3]"));
        assertFalse(MCPClientRegistrar.isCompleteJsonObject("null"));
    }
}
