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
package com.codename1.project;

/// The build tool a Codename One project is driven by.
///
/// The layout rules differ per build tool, so everything that has to find a
/// project file asks [ProjectLayout] instead of assuming one of these.
public enum BuildSystem {
    /// The legacy NetBeans-style project: `build.xml`, `src/`, `native/`,
    /// `lib/` and `codenameone_settings.properties` in one directory.
    ANT,
    /// The multi-module project the `cn1app-archetype` generates: a root
    /// `pom.xml` with `common/` holding the application and one module per
    /// platform.
    MAVEN,
    /// The single-project build the `com.codenameone` Gradle plugin drives,
    /// with the application at the root and platform sources under
    /// `src/<platform>/` only once they exist.
    GRADLE
}
