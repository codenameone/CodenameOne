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

/// Resolves one field: what the build generates for each `@QueryMapping`,
/// `@MutationMapping`, `@SubscriptionMapping` and `@SchemaMapping` method, and
/// what a program registers by hand with [GraphQL.Builder#fetcher].
///
/// A field with no fetcher reads the property of its parent value with the
/// field's name: an entry of a `Map`, or a property of an object the JSON
/// codecs can write -- the way Spring for GraphQL falls back to a getter.
public interface DataFetcher {
    /// The field's value. A `List`, array or `Iterable` for a list field; a
    /// [GraphQLStream] for a subscription field.
    ///
    /// #### Throws
    ///
    /// - `Exception`: the field resolves to null and an error is reported; see
    /// [GraphQLException] for an error with a message of its own
    Object get(DataFetchingEnvironment environment) throws Exception;
}
