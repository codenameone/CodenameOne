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

/// Builds the management endpoints into the server: health, metrics, the
/// Prometheus view, scheduled jobs and managed beans, under `/manage`.
///
/// ```java
/// @EnableManagement
/// @Configuration
/// public class Settings { }
/// ```
///
/// THIS IS A BUILD-TIME SWITCH. Without it -- or `cn1.management.enabled=true`
/// in `application.properties` or a profile's file -- the entry point never names
/// the endpoints and the translator leaves their code out of the binary, so a
/// packaged server has no `/manage` to find, whatever its profile. The development
/// run always has them.
///
/// With it, the endpoints are on unless `cn1.management.enabled=false` turns them
/// off at start-up, and outside a development profile they refuse to start
/// without `cn1.management.token`, which belongs in the environment rather than
/// in source.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface EnableManagement {
    /// Where the endpoints are served; `cn1.management.path`. Empty means
    /// `/manage`.
    String path() default "";
}
