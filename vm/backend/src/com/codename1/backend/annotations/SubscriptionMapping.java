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
package com.codename1.backend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Resolves a field of the schema's `Subscription` type with this method of a
/// `@Controller` bean, as in Spring for GraphQL.
///
/// The method returns a [com.codename1.backend.graphql.GraphQLStream]; each
/// value it emits is resolved against the subscription's selection set and
/// sent to the client as one result, over WebSocket with the
/// `graphql-transport-ws` protocol. Parameters are as for [SchemaMapping].
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface SubscriptionMapping {
    /// The field's name; empty means the method's name. Same as [#name()].
    String value() default "";
    /// The field's name; empty means the method's name. Same as [#value()].
    String name() default "";
}
