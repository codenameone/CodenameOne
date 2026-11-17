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

/// The coercion of a custom scalar, such as `scalar Date`: Spring for GraphQL's
/// `Coercing`. Register one with [GraphQL.Builder#scalar].
///
/// A custom scalar with none passes values through as they are: what a
/// resolver returns is written as JSON, and what a client sends is handed to
/// the resolver as the JSON value (a string, number, boolean, list or map).
public interface GraphQLScalar {
    /// The value to write for `value`, which a resolver returned: a string,
    /// number, boolean, list or map.
    ///
    /// #### Throws
    ///
    /// - `GraphQLException`: when `value` cannot be represented
    Object serialize(Object value);

    /// The value a resolver receives for `input`, which the client sent, either
    /// as a variable or as a literal in the document.
    ///
    /// #### Throws
    ///
    /// - `GraphQLException`: when `input` is not a valid value
    Object parseValue(Object input);
}
