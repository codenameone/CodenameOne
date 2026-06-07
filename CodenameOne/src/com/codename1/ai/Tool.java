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

/// A function the model can call. `parametersJsonSchema` is a raw
/// JSON-Schema string; each provider wraps it differently on the wire
/// (OpenAI `{type:"function",function:{...}}`, Anthropic
/// `{name,description,input_schema}`, Gemini `functionDeclarations`),
/// but the inner schema shape is the same across all of them, so we
/// hand it through as a string and let the provider client wrap.
///
/// #### Linking a tool to its executor
///
/// Pass an optional [ToolHandler] at construction time and the
/// matching [ToolCall] can dispatch through it without the caller
/// having to match names by hand:
///
/// ```
/// Tool weather = new Tool(
///     "get_weather",
///     "Returns the current weather for a location",
///     "{\"type\":\"object\",\"properties\":{" +
///        "\"location\":{\"type\":\"string\"}}," +
///        "\"required\":[\"location\"]}",
///     argumentsJson -> {
///         Map args = JSONParser.parseJSON(argumentsJson);
///         return "{\"temp\":21,\"city\":\""
///                 + JSONParser.getString(args, "location") + "\"}";
///     });
///
/// // Later, when the model returns a ToolCall:
/// for (ToolCall call : response.getToolCalls()) {
///     String resultJson = call.execute(Arrays.asList(weather));
///     conversation.add(ChatMessage.toolResult(call.getId(), resultJson));
/// }
/// ```
///
/// The handler is optional -- a `Tool` constructed without one is a
/// pure description for the model, and the caller can dispatch
/// however they like via the raw [ToolCall#getName] /
/// [ToolCall#getArgumentsJson] accessors.
public final class Tool {
    private final String name;
    private final String description;
    private final String parametersJsonSchema;
    private final ToolHandler handler;

    public Tool(String name, String description, String parametersJsonSchema) {
        this(name, description, parametersJsonSchema, null);
    }

    public Tool(String name, String description, String parametersJsonSchema,
                ToolHandler handler) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name is required");
        }
        this.name = name;
        this.description = description == null ? "" : description;
        this.parametersJsonSchema = parametersJsonSchema == null
                ? "{\"type\":\"object\",\"properties\":{}}"
                : parametersJsonSchema;
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getParametersJsonSchema() {
        return parametersJsonSchema;
    }

    /// The optional executor wired up by the constructor. Returns
    /// `null` for description-only tools.
    public ToolHandler getHandler() {
        return handler;
    }

    /// Invokes the handler with the given arguments JSON. Throws
    /// `IllegalStateException` when no handler was registered.
    public String invoke(String argumentsJson) throws Exception {
        if (handler == null) {
            throw new IllegalStateException(
                    "No handler registered for tool '" + name + "'. "
                  + "Either register a ToolHandler when constructing the Tool, "
                  + "or call ToolCall.getArgumentsJson() and dispatch by hand.");
        }
        return handler.invoke(argumentsJson);
    }
}
