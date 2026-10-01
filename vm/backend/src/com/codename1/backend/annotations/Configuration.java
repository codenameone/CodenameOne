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

/// A class whose [Bean] methods produce beans.
///
/// The class itself is a bean too, so its own constructor and fields can be
/// injected before its factory methods are called. The build calls each factory
/// method exactly once, directly, from the entry point it generates -- so unlike
/// Spring there is no proxy, and one `@Bean` method calling another simply gets a
/// second object. Take the other bean as a PARAMETER instead, which is what the
/// build wires:
///
/// ```java
/// @Configuration
/// public class Clients {
///     @Bean
///     public Mailer mailer(@Value("${mail.host:localhost}") String host) {
///         return new Mailer(host);
///     }
///     @Bean
///     public Notifier notifier(Mailer mailer) {
///         return new Notifier(mailer);
///     }
/// }
/// ```
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Configuration {
    /// The bean's name, for [Qualifier]. Empty means the default name.
    String value() default "";
}
