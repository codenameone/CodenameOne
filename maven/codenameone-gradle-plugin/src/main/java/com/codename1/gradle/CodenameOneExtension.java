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
package com.codename1.gradle;

import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/// The `codenameone { }` block.
///
/// Everything has a sensible default, so most projects never write one:
///
/// ```kotlin
/// codenameone {
///     version = "8.1.0"                      // the framework version; defaults to the plugin's
///     buildHints.put("ios.newStorageLocation", "true")
/// }
/// ```
public abstract class CodenameOneExtension {
    /// The Codename One version the project builds against. Defaults to the
    /// plugin's own version, so the version in `settings.gradle.kts` is the only
    /// number to bump.
    public abstract Property<String> getVersion();

    /// The application's main class, fully qualified. Defaults to
    /// `codename1.packageName` + `codename1.mainName` from
    /// `codenameone_settings.properties`.
    public abstract Property<String> getMainClass();

    /// Build hints applied to every build of this project, on top of the ones in
    /// `codenameone_settings.properties` (without the `codename1.arg.` prefix).
    /// A `-Pcodename1.arg.<hint>=...` on the command line still wins.
    public abstract MapProperty<String, String> getBuildHints();

    /// Whether a generated Xcode or Android Studio project is opened when a
    /// `*-source` build finishes. Defaults to true; CI sets `-Pcodename1.open=false`.
    public abstract Property<Boolean> getOpenGeneratedProjects();
}
