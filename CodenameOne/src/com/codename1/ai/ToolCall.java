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
package com.codename1.ai;

import java.util.List;

/// A tool/function invocation produced by the model. The `id` round-
/// trips the call back to a [ToolResultPart] so the provider can
/// match the result to the original request. `argumentsJson` is the
/// raw JSON string the model produced -- parse it with
/// [com.codename1.io.JSONParser] if you need the structured fields.
///
/// Use [#execute(List)] to dispatch to the matching [Tool] handler
/// from the request's tool list. See the [Tool] class javadoc for
/// the full pattern.
public final class ToolCall {
    private final String id;
    private final String name;
    private final String argumentsJson;

    public ToolCall(String id, String name, String argumentsJson) {
        this.id = id;
        this.name = name;
        this.argumentsJson = argumentsJson == null ? "{}" : argumentsJson;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    /// Finds the [Tool] whose name matches this call and invokes its
    /// [ToolHandler] with this call's `argumentsJson`. Returns the
    /// JSON result the handler produced. Apps typically wrap that in
    /// a [ChatMessage#toolResult] and append it to the conversation
    /// before the next chat turn.
    ///
    /// Throws `IllegalArgumentException` when no tool in `tools`
    /// has a matching name, or `IllegalStateException` when the
    /// matching tool has no handler registered.
    public String execute(List<Tool> tools) throws Exception {
        Tool match = findTool(tools);
        if (match == null) {
            throw new IllegalArgumentException(
                    "No tool registered with name '" + name + "'. "
                  + "Add it to the request's tool list, or dispatch by hand "
                  + "via getName() / getArgumentsJson().");
        }
        return match.invoke(argumentsJson);
    }

    /// Looks up the matching [Tool] without invoking it. Useful when
    /// the caller wants to dispatch by hand but still benefit from
    /// the name-matching plumbing.
    public Tool findTool(List<Tool> tools) {
        if (tools == null) {
            return null;
        }
        for (Tool t : tools) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }
}
