/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
/// GraphQL client runtime. [GraphQL] sends queries and mutations over
/// HTTP `POST` and parses the `{"data":...,"errors":[...]}` envelope;
/// [GraphQLResponse] is the typed result wrapper (data and errors can
/// co-exist as a partial result); [GraphQLError] is one entry of the
/// errors array; [GraphQLSubscription] streams subscription payloads
/// over a WebSocket; [GraphQLClients] is the per-`@GraphQLClient`
/// factory registry that the build-time-generated
/// `cn1app.GraphQLClientBootstrap` populates.
///
/// End-to-end usage is documented on
/// [com.codename1.annotations.graphql] -- the user-facing entry point
/// is the generated `<Name>.of(endpoint)` factory rather than any
/// class in this package directly.
package com.codename1.io.graphql;
