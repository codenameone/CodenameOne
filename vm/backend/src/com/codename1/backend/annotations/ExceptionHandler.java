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

/// Marks a method that answers for an exception a controller method threw.
///
/// On a `@RestController` or `@Controller` it handles that controller's
/// routes only, and is consulted first; on a [ControllerAdvice] bean it handles
/// every controller's. Among the handlers of one class, the one naming the
/// closest superclass of the thrown exception is chosen. The order, and the
/// security failures no handler ever sees, are described on
/// `com.codename1.backend.web.ControllerHooks`.
///
/// ```java
/// @ExceptionHandler(NotFound.class)
/// @ResponseStatus(404)
/// public Map notFound(NotFound e) {
///     return Collections.singletonMap("error", e.getMessage());
/// }
/// ```
///
/// The method is public and may take, in any order, the exception (any type
/// the declared exceptions are assignable to), the `HttpServer.Request` and the
/// `com.codename1.backend.web.HandlerMethod`. It returns an `HttpServer.Response`
/// -- complete control of status, headers and body -- or a `String` (text), a
/// `Map`, a `List` or a `Json.Writable` (JSON), or nothing; any of those but a
/// Response is sent with [ResponseStatus]'s status, 200 without one, as in
/// Spring.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface ExceptionHandler {
    /// The exceptions handled. Empty means the method's exception parameter's type.
    Class<?>[] value() default {};
}
