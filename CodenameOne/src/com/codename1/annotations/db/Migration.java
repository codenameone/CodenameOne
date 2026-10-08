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
package com.codename1.annotations.db;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a `com.codename1.migration.JavaMigration` the build should register beside the
/// project's SQL migration files.
///
/// The class needs a public no-argument constructor. It runs once, in version order with the
/// scripts, and its version must not be one a script already uses.
///
/// ```java
/// @Migration(version = "4", description = "normalize phone numbers")
/// public class NormalizePhones implements JavaMigration {
///     public void migrate(MigrationContext context) throws IOException {
///         // ...
///     }
/// }
/// ```
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@com.codename1.impl.SharedWithBackend
public @interface Migration {
    /// The version, such as `4` or `2026.05.21.1`.
    String version();

    /// What the migration does; recorded in the schema history.
    String description();
}
