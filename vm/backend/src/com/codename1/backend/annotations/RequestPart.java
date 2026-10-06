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

/// Binds one part of a `multipart/form-data` request body to this parameter.
///
/// ```java
/// @PostMapping("/avatar")
/// public String upload(@RequestPart("file") HttpServer.Part file) {
///     store(file.getFilename(), file.getBytes());
///     return "stored " + file.getSize() + " bytes";
/// }
/// ```
///
/// The parameter is a `com.codename1.backend.HttpServer.Part` -- name, filename,
/// content type and bytes -- or `byte[]` for the content alone, or `String` for
/// a text field. A body that is not well-formed multipart is answered 400 before
/// the method runs, as is a missing required part.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface RequestPart {
    /// The part's name, from its Content-Disposition. Required for the same reason
    /// [RequestParam]'s is: a parameter's own name does not survive compilation.
    String value();
    /// Whether a request without the part is rejected.
    boolean required() default true;
}
