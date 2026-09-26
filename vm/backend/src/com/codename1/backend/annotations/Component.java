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

/// Marks a class the build constructs and injects, once per server.
///
/// The build finds the class, works out what its constructor and its
/// `@Autowired` members need, and writes the `new` into the entry point it
/// generates. Nothing is looked up at run time: there is no container, no
/// registry and no reflection, and a dependency that cannot be satisfied is a
/// build error that names the injection point.
///
/// ```java
/// @Component
/// public class Clock {
///     public long now() { return System.currentTimeMillis(); }
/// }
/// ```
///
/// [Service] and [Repository] mean the same thing and exist so a class can say
/// which layer it belongs to, as they do in Spring.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Component {
    /// The bean's name, for [Qualifier]. Empty means the class's simple name with
    /// its first letter lower-cased, which is Spring's rule.
    String value() default "";
}
