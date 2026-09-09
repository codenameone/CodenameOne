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

/// Binds a request header to this parameter.
///
/// A missing value binds to the default below, or to null when the parameter is
/// not required. It is never a server error unless `required` says so.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface RequestHeader {
    /// The name to bind from. REQUIRED, and deliberately so: it cannot default to
    /// the parameter's own name because a Java parameter name only survives
    /// compilation when the application is built with -parameters, which is the
    /// application's build to decide and not this one's. An annotation that
    /// promised the default would compile fine and then fail at packaging, for
    /// every developer who took it at its word.
    String value();
    /// Whether a request without it is rejected.
    boolean required() default true;
    /// Used when the request omits it.
    String defaultValue() default "";
}
