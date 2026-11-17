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

/// Resolves a field of an object type of the schema with this method of a
/// `@Controller` bean, as in Spring for GraphQL. [QueryMapping],
/// [MutationMapping] and [SubscriptionMapping] are this with the root type
/// filled in.
///
/// ```java
/// @Controller
/// public class BookController {
///     @QueryMapping
///     public Book bookById(@Argument("id") String id) { ... }
///
///     @SchemaMapping(typeName = "Book")
///     public Author author(Book book) { ... }
/// }
/// ```
///
/// A parameter is one of:
///
/// - an [Argument], one argument of the field, converted to the parameter's
///   type -- a scalar, an enum, a list or a class of the application's own
/// - [Arguments], all of them, as a `Map` or converted to a class
/// - a `DataFetchingEnvironment`, for everything else
/// - an `HttpServer.Request` or a `WebSocketSession`: the transport the
///   operation arrived on, or null on the other one
/// - otherwise, on a field of a type other than the root types, the source:
///   the object whose field this is
///
/// The result is the field's value: a scalar, a `Map`, a `List`, or a class of
/// the application's own, whose fields are read through its JSON codec. A
/// method that throws nulls the field and reports the error; see
/// [com.codename1.backend.graphql.GraphQL] for how exceptions are classified.
/// `@PreAuthorize` and `@Secured` apply as on any bean method.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SchemaMapping {
    /// The object type the field belongs to. On a method, empty means the
    /// class's `@SchemaMapping` type; on a class, the type of every
    /// `@SchemaMapping` and `@BatchMapping` method that names none.
    String typeName() default "";
    /// The field's name; empty means the method's name. Same as [#field()].
    String value() default "";
    /// The field's name; empty means the method's name. Same as [#value()].
    String field() default "";
}
