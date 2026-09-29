/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a core source file that the server-side backend runtime (`vm/backend`)
/// compiles too. Internal: it is a build instruction, not an API.
///
/// A class that both halves of an application need -- the ORM, the entity
/// annotations -- lives here in `CodenameOne/src` and nowhere else. Every backend
/// build (`maven/backend`, `vm/backend/build.sh`, `run-javase.sh`, ...) collects
/// the files carrying this marker and compiles them alongside the backend's own
/// sources, so there is exactly one copy to edit and nothing to keep in sync.
///
/// The builds find the marker by TEXT, not by reflection: it has to be written on
/// a line of its own, starting in column zero, directly above the type
/// declaration. Write it fully qualified, which needs no import:
///
/// ```java
/// @com.codename1.impl.SharedWithBackend
/// public final class SessionImpl implements Session {
/// ```
///
/// `@SharedWithBackend` with an import matches too; that is the form this file
/// uses, because PMD rejects a fully qualified name for a type in its own
/// package.
///
/// `vm/backend/shared-sources.sh` is the list the shell builds use, and
/// `maven/backend/pom.xml` applies the same line match. Both refuse a
/// `vm/backend/src` file that shadows a core file, which is what used to happen
/// instead of this marker.
///
/// Consequences for a marked class:
///
/// - It may only depend on the JDK subset the backend compiles against and on
///   other marked classes. Anything that is genuinely different on the server is
///   a separately named class in `vm/backend` behind an interface the shared
///   code declares (`BackendSqlAccess` implements the shared
///   `com.codename1.impl.orm.SqlAccess`), never a second copy of the shared
///   class.
/// - It is still an ordinary core class: the marker has `SOURCE` retention, so it
///   leaves nothing in bytecode on any port.
///
/// Not usable on `package-info.java`: javac emits a `package-info.class` for any
/// package annotation, and that is not a class a translated port should see.
@SharedWithBackend
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface SharedWithBackend {
}
