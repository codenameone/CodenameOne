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
/// GraphQL for the backend, in the shape of Spring for GraphQL: a schema
/// written in `.graphqls` files under `src/main/resources/graphql`, resolved by
/// `@QueryMapping`, `@MutationMapping`, `@SubscriptionMapping` and
/// `@SchemaMapping` methods of `@Controller` beans, and served at `/graphql`.
///
/// The build reads the schema, matches every resolver against it -- a method
/// naming a field the schema lacks is a build error -- and generates the
/// wiring, so nothing is scanned or reflected at run time.
/// [com.codename1.backend.graphql.GraphQL] is the engine and
/// [com.codename1.backend.graphql.GraphQLEndpoint] serves it over HTTP and,
/// for subscriptions, over WebSocket with the `graphql-transport-ws` protocol
/// the client's `GraphQLSubscription` speaks.
package com.codename1.backend.graphql;
