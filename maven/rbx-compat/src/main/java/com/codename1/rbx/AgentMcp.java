/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codename1.rbx;

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.mcp.MCP;
import com.codename1.ui.CN;

/// Publishes [AgentTools] on the app's built-in MCP server, so an agent
/// (Claude Code, Codex, any MCP host) can build an experience in the
/// studio: lay out parts, write and test scripts, add effects, publish.
///
/// Every tool runs on the EDT, where the engine lives, and the agent waits
/// for its answer. Names are prefixed `brickyard_` to keep them apart from
/// the server's own UI tools.
public final class AgentMcp {
    private AgentMcp() {
    }

    /// Registers every tool against `studio`.
    public static void install(AgentTools.Studio studio) {
        final AgentTools tools = new AgentTools(studio);
        for (int i = 0; i < AgentTools.toolCount(); i++) {
            String[] t = AgentTools.tool(i);
            final String name = t[0];
            MCP.addTool(new Tool("brickyard_" + name, t[1], t[2], new ToolHandler() {
                @Override
                public String invoke(final String args) {
                    return onEdt(tools, name, args);
                }
            }));
        }
    }

    static String onEdt(final AgentTools tools, final String name, final String args) {
        if (CN.isEdt()) {
            return tools.call(name, args);
        }
        final String[] out = new String[1];
        final RuntimeException[] err = new RuntimeException[1];
        CN.callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    out[0] = tools.call(name, args);
                } catch (RuntimeException ex) {
                    err[0] = ex;
                }
            }
        });
        if (err[0] != null) {
            throw err[0];
        }
        return out[0];
    }
}
