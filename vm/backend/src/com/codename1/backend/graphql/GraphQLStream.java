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

/// The events of a subscription field: what a `@SubscriptionMapping` method
/// returns, in place of Spring for GraphQL's `Flux`.
///
/// Each event is resolved against the subscription's selection set as though it
/// were the field's value, and sent to the client as one response.
///
/// ```java
/// @SubscriptionMapping
/// public GraphQLStream bookAdded() {
///     return emitter -> {
///         Subscription s = messaging.subscribe("books", m -> emitter.next(m.getBody()));
///         return s::cancel;
///     };
/// }
/// ```
public interface GraphQLStream {
    /// Starts the stream. Events, an error or completion go to `emitter`, from
    /// any thread.
    ///
    /// #### Returns
    ///
    /// what stops the stream when the client unsubscribes or disconnects; may be null
    Runnable open(Emitter emitter) throws Exception;

    /// Where a stream sends what it produces.
    interface Emitter {
        /// Sends one event.
        void next(Object event);

        /// Ends the stream with an error, reported to the client.
        void error(Throwable error);

        /// Ends the stream.
        void complete();
    }
}
