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

/// Receives the messages of a destination: the method runs once per message,
/// on a consumer thread of the server, and the message is acknowledged when it
/// returns.
///
/// ```java
/// @Component
/// public class Shipping {
///     @MessageListener(value = "orders.placed", group = "shipping", concurrency = 4)
///     public void placed(Order order) { ... }
/// }
/// ```
///
/// The method takes one parameter: the payload, read as JSON by a codec the
/// build generates for its type -- or a `String`, a `byte[]`, or the whole
/// [com.codename1.backend.messaging.Message]. A method that throws has the
/// message delivered again after a backoff, and after
/// `cn1.messaging.retry.max-attempts` deliveries it goes to
/// `<destination>.dlq`. Delivery is at least once, so a listener should be
/// safe to run twice for one message.
///
/// The build finds every listener and writes the code that subscribes it when
/// the server starts and stops it when the server stops; there is no
/// reflection. A destination may name a setting, `${orders.topic}`, which is
/// read from the configuration when the server starts.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface MessageListener {
    /// The destinations: queues, topics or streams. At least one.
    String[] value() default {};
    /// The same as [#value].
    String[] destinations() default {};
    /// The consumer group; subscriptions of one group share the messages, and
    /// each group gets every one. Empty means `spring.application.name`, or
    /// `default` without one.
    String group() default "";
    /// Consumer threads.
    int concurrency() default 1;
}
