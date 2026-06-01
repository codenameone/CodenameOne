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
package com.codename1.io.graphql;

import java.util.List;
import java.util.Map;

/// One entry from a GraphQL response `errors` array. A GraphQL server
/// may return errors alongside partial `data`, so an error here does
/// not necessarily mean the whole request failed -- inspect
/// [GraphQLResponse#getData()] as well.
///
/// The spec-defined keys are surfaced directly: `message` (always
/// present), `path` (the response field path the error applies to),
/// `locations` (line/column positions in the request document), and
/// the open-ended `extensions` object. Unknown keys are ignored.
public final class GraphQLError {

    private final String message;
    private final List<Object> path;
    private final List<Map<String, Object>> locations;
    private final Map<String, Object> extensions;

    public GraphQLError(String message, List<Object> path,
                        List<Map<String, Object>> locations,
                        Map<String, Object> extensions) {
        this.message = message;
        this.path = path;
        this.locations = locations;
        this.extensions = extensions;
    }

    /// The human-readable error description. Never null in a
    /// spec-compliant response, but defaults to an empty string when
    /// the server omits it.
    public String getMessage() {
        return message;
    }

    /// The response path the error applies to (string field names and
    /// integer list indices), or null when the server did not supply
    /// one.
    public List<Object> getPath() {
        return path;
    }

    /// Source `{line, column}` locations in the request document, or
    /// null when absent.
    public List<Map<String, Object>> getLocations() {
        return locations;
    }

    /// The open-ended `extensions` object (often carries an error
    /// `code`), or null when absent.
    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public String toString() {
        return message == null ? "GraphQLError" : message;
    }
}
