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

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.util.GradleVersion;

/// The `com.codenameone` Gradle plugin.
///
/// One id for every use, applied wherever it is declared:
///
/// - **in `settings.gradle.kts`** it adds the Codename One repository, includes
///   a `backend/` subproject when there is one, and applies itself to the root
///   project and that backend -- so a project needs no `build.gradle.kts` until
///   it has dependencies to declare;
/// - **in a project** it builds whatever the directory holds: an application
///   (`codenameone_settings.properties`), a cn1lib
///   (`codenameone_library_appended.properties`) or a backend
///   (`application.properties`). The `codename1.kind` Gradle property
///   overrides the detection.
///
/// The work itself is the build engine the Maven plugin also runs, so the same
/// application builds the same way under either tool.
public class CodenameOnePlugin implements Plugin<Object> {
    /// The oldest Gradle the plugin supports.
    static final String MINIMUM_GRADLE = "8.5";

    @Override
    public void apply(Object target) {
        if (GradleVersion.current().compareTo(GradleVersion.version(MINIMUM_GRADLE)) < 0) {
            throw new GradleException("The Codename One Gradle plugin needs Gradle " + MINIMUM_GRADLE
                    + " or newer; this build runs " + GradleVersion.current().getVersion()
                    + ". Update the wrapper with: gradle wrapper --gradle-version " + MINIMUM_GRADLE);
        }
        if (target instanceof Settings) {
            SettingsSupport.apply((Settings) target);
        } else if (target instanceof Project) {
            ProjectSupport.apply((Project) target);
        } else {
            throw new GradleException("The com.codenameone plugin is applied from settings.gradle.kts or from a "
                    + "project's build.gradle.kts, not to " + target);
        }
    }
}
