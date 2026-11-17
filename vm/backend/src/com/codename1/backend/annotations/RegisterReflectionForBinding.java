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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares classes the build must write JSON codecs for, read and write,
/// although no call site names them -- a class reached only through an
/// `Object`-typed `Json.write`, a `Map<String, Object>` response or a message
/// payload. Spring's annotation of the same name registers the same classes for
/// its ahead-of-time image, which is what this is here: codecs are generated at
/// build time, and `Json.write` refuses a class it has none for.
///
/// It may sit on any class or method of the application:
///
/// ```java
/// @RegisterReflectionForBinding({Invoice.class, Refund.class})
/// @Component
/// public class Webhooks { ... }
/// ```
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterReflectionForBinding {
    /// The classes to write codecs for.
    Class<?>[] value() default {};

    /// The same, as Spring also accepts it.
    Class<?>[] classes() default {};
}
