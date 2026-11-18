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
package com.codename1.backend.graphql;

import java.util.Map;

/// An error a resolver throws to put a message of its own in the response's
/// `errors`, as Spring for GraphQL's `GraphQLError` does.
///
/// Any other exception thrown by a resolver is reported as `INTERNAL_ERROR`
/// with no detail, so an exception message never leaks server internals to a
/// client. Throw this one for messages that are meant to be read.
///
/// The field that threw resolves to `null`, and the rest of the response is
/// still answered: GraphQL reports partial results.
public class GraphQLException extends RuntimeException {
    private final int line;
    private final int column;
    private final Map extensions;

    /// An error with this message for the client.
    public GraphQLException(String message) {
        this(message, (Map) null);
    }

    /// An error with this message and `extensions`, written as the error's
    /// `extensions` object -- `classification` or `code` by convention.
    public GraphQLException(String message, Map extensions) {
        super(message);
        this.line = 0;
        this.column = 0;
        this.extensions = extensions;
    }

    GraphQLException(String message, int line, int column) {
        super(message);
        this.line = line;
        this.column = column;
        this.extensions = null;
    }

    /// The line in the document the error refers to, or 0.
    public int getLine() {
        return line;
    }

    /// The column in the document the error refers to, or 0.
    public int getColumn() {
        return column;
    }

    /// The error's `extensions`, or null.
    public Map getExtensions() {
        return extensions;
    }
}
