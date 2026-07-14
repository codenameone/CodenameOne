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

import com.codename1.io.JSONParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/// JSON helpers that preserve the JSON-RPC type model. The default
/// {@link JSONParser} turns booleans into strings, integers into doubles, and drops
/// null values; that silently rewrites tool arguments and, when editing a host config,
/// the user's other settings. These helpers keep booleans, integers (as longs) and
/// nulls, and serialize them back faithfully.
final class MCPJson {
    private MCPJson() {
    }

    /// Parses a JSON object preserving booleans, longs and nulls.
    static Map<String, Object> parse(String json) throws IOException {
        JSONParser parser = new JSONParser();
        parser.setUseBooleanInstance(true);
        parser.setUseLongsInstance(true);
        parser.setIncludeNullsInstance(true);
        return parser.parseJSON(new InputStreamReader(
                new ByteArrayInputStream(json.getBytes("UTF-8")), "UTF-8"));
    }

    /// Serializes a value back to JSON, keeping null-valued map entries. Maps go through
    /// {@link JSONParser#mapToJson} (which preserves nulls); other values through
    /// {@link JSONParser#toJson}.
    @SuppressWarnings("unchecked")
    static String toJson(Object value) {
        if (value instanceof Map) {
            return JSONParser.mapToJson((Map<String, ?>) value);
        }
        return JSONParser.toJson(value);
    }
}
