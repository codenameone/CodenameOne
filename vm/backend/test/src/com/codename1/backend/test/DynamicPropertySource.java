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
package com.codename1.backend.test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a static method of a test class that adds settings whose values are
/// only known when the test starts -- the port of a database container, a
/// temporary directory:
///
/// ```java
/// @DynamicPropertySource
/// static void database(DynamicPropertyRegistry registry) {
///     registry.add("cn1.datasource.url", () -> container.getJdbcUrl());
/// }
/// ```
///
/// The build calls the method from the test's generated context, so it must be
/// static, take one [DynamicPropertyRegistry] and be visible from the test's
/// package. The values are read once, when the application starts, and win over
/// the test annotation's `properties`. A class with such a method gets an
/// application of its own rather than sharing one with other test classes,
/// because what it adds is not known until it runs.
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface DynamicPropertySource {
}
