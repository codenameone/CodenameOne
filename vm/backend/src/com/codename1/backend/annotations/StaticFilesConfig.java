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

/// Static file settings, compiled in.
///
/// Each attribute is the `cn1.static.*` key named beside it, and sets that key's
/// value at the bottom of the configuration, so a properties file or the
/// environment still override it. An attribute left at its default sets nothing.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface StaticFilesConfig {
    /// `cn1.static.root`: the directory to serve.
    String root() default "";

    /// `cn1.static.prefix`: the path they're served under.
    String prefix() default "";

    /// `cn1.static.index`: the file a directory answers with.
    String index() default "";

    /// `cn1.static.cacheControl`: their `Cache-Control` header.
    String cacheControl() default "";
}
