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
/// #### Example
///
/// ```
/// Tool t = new Tool(
///     "get_weather",
///     "Returns the current weather for a location",
///     "{\"type\":\"object\",\"properties\":{" +
///        "\"location\":{\"type\":\"string\"}}," +
///        "\"required\":[\"location\"]}");
/// ```
public final class Tool {
    private final String name;
    private final String description;
    private final String parametersJsonSchema;

    public Tool(String name, String description, String parametersJsonSchema) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name is required");
        }
        this.name = name;
        this.description = description == null ? "" : description;
        this.parametersJsonSchema = parametersJsonSchema == null
                ? "{\"type\":\"object\",\"properties\":{}}"
                : parametersJsonSchema;
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
}
