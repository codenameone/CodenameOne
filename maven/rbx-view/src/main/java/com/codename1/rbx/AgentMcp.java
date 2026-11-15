/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
    /// Hears every tool call an agent makes, after it ran, so an editor can
    /// show the agent's work as it happens.
    public interface Activity {
        /// `result` is the tool's answer, or the error's message when `failed`.
        void called(String tool, String args, String result, boolean failed);
    }

    private static Activity activity;

    private AgentMcp() {
    }

    /// The listener told about each call; null for none.
    public static void setActivity(Activity a) {
        activity = a;
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
        String out;
        try {
            out = run(tools, name, args);
        } catch (RuntimeException ex) {
            tell(name, args, String.valueOf(ex.getMessage()), true);
            throw ex;
        }
        tell(name, args, out, false);
        return out;
    }

    private static void tell(final String name, final String args, final String result, final boolean failed) {
        final Activity a = activity;
        if (a != null) {
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    a.called(name, args, result, failed);
                }
            });
        }
    }

    private static String run(final AgentTools tools, final String name, final String args) {
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
