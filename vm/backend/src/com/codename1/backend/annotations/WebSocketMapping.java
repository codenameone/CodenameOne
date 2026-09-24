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

/// Marks a `com.codename1.backend.WebSocket` as the endpoint for a path.
///
/// ```java
/// @WebSocketMapping("/chat")
/// public class ChatEndpoint implements WebSocket {
///     public void onOpen(WebSocketSession session) { }
///     public void onText(WebSocketSession session, String message) { }
///     public void onBinary(WebSocketSession session, byte[] m, int off, int len) { }
/// }
/// ```
///
/// The build finds these and registers them in the generated entry point, so a
/// websocket endpoint needs no more start-up code than a `@RestController` does.
///
/// ## Why this is on the TYPE and the HTTP mappings are on methods
///
/// A `@GetMapping` marks a call: one request in, one response out, and the method
/// signature is the whole contract. A websocket is a CONNECTION, and its contract
/// is seven callbacks that share per-connection state -- which is an object, not a
/// method. Putting the annotation on a method would mean either that the method
/// runs per connection (a factory pretending to be a handler) or that it runs once
/// and the annotation is on the wrong element.
///
/// What it keeps from `@GetMapping` is everything that matters to a reader: the
/// path is relative to a class-level `@RequestMapping` if there is one, a
/// constructor taking a `DataSource` or an `EntityManager` is injected the same
/// way, and two endpoints claiming one path is a build error rather than a
/// race decided by registration order.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface WebSocketMapping {
    /// The path this endpoint serves. One value, or several for aliases.
    String[] value() default {};
}
