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
package com.codename1.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/// The files that make a directory a Codename One Gradle project, from the one
/// template every generator shares.
///
/// The templates live beside this class under
/// `com/codename1/project/templates/gradle`. The Maven plugin writes them when
/// it generates or migrates a project with `-Dcn1.buildTool=gradle`, the Gradle
/// plugin writes the backend skeleton for `addBackend`, and the initializr
/// packages the same directory -- so a project looks the same whichever of them
/// made it. Placeholders are `__NAME__` tokens rather than `${...}`, which
/// Kotlin build scripts already use.
public final class GradleProjectTemplate {
    /// What the project is.
    public enum Shape {
        /// An application.
        APP,
        /// An application with a `backend/` subproject.
        APP_WITH_BACKEND,
        /// A backend with no client, at the root.
        BACKEND_ONLY
    }

    private static final String ROOT = "/com/codename1/project/templates/gradle/";

    private GradleProjectTemplate() {
    }

    /// The template at `path` (relative to the template root), as text.
    public static String text(String path) throws IOException {
        return new String(bytes(path), StandardCharsets.UTF_8);
    }

    private static byte[] bytes(String path) throws IOException {
        InputStream in = GradleProjectTemplate.class.getResourceAsStream(ROOT + path);
        if (in == null) {
            throw new IOException("Missing Gradle project template " + path);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /// Writes the build scaffolding into `dir`: the settings script, the build
    /// script, the Gradle wrapper, `gradle.properties` and `.gitignore`. Sources,
    /// `codenameone_settings.properties` and the icon are the caller's business.
    /// An existing file is left alone, so this can run over a project that has
    /// some of them already.
    ///
    /// @param projectName the root project's name
    /// @param cn1Version the plugin (and framework) version to declare
    /// `value` as the inside of a Kotlin string literal: a main class may hold a
    /// `$` (`Price$Tracker`), which Kotlin would read as a template.
    static String kotlinString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$");
    }

    public static void writeScaffolding(File dir, String projectName, String cn1Version, Shape shape)
            throws IOException {
        write(new File(dir, "settings.gradle.kts"), text("settings.gradle.kts.txt")
                .replace("__CN1_VERSION__", cn1Version).replace("__PROJECT_NAME__", kotlinString(projectName)));
        write(new File(dir, "build.gradle.kts"),
                text(shape == Shape.BACKEND_ONLY ? "backend/build.gradle.kts.txt" : "app/build.gradle.kts.txt"));
        write(new File(dir, "gradle.properties"), text("gradle.properties.txt"));
        write(new File(dir, ".gitignore"), text("gitignore.txt"));
        writeBinary(new File(dir, "gradlew"), bytes("gradlew"));
        writeBinary(new File(dir, "gradlew.bat"), bytes("gradlew.bat"));
        writeBinary(new File(dir, "gradle/wrapper/gradle-wrapper.jar"), bytes("gradle/wrapper/gradle-wrapper.jar"));
        writeBinary(new File(dir, "gradle/wrapper/gradle-wrapper.properties"),
                bytes("gradle/wrapper/gradle-wrapper.properties"));
        File gradlew = new File(dir, "gradlew");
        if (!gradlew.setExecutable(true, false) && !gradlew.canExecute()) {
            throw new IOException("Could not make " + gradlew + " executable");
        }
        if (shape == Shape.BACKEND_ONLY) {
            writeBackendFiles(dir, null, "");
        }
    }

    /// Writes a backend skeleton into `backendDir`: its `application*.properties`
    /// and, when `packageName` is not null, an `Api` controller, the `Greeter`
    /// service it is given and the tests of both, in that package.
    ///
    /// @param taskPrefix how the backend's tasks are addressed from the root:
    ///        `:backend:` for a subproject, empty for a backend-only project
    public static void writeBackendFiles(File backendDir, String packageName, String taskPrefix) throws IOException {
        write(new File(backendDir, "application.properties"),
                text("backend/application.properties.txt").replace("__BACKEND__", taskPrefix));
        write(new File(backendDir, "application-dev.properties"),
                text("backend/application-dev.properties.txt").replace("__BACKEND__", taskPrefix));
        if (packageName != null) {
            write(new File(backendDir, "src/main/java/" + packageName.replace('.', '/') + "/Api.java"),
                    text("backend/Api.java.txt").replace("${package}", packageName)
                            .replace("__BACKEND__", taskPrefix));
            write(new File(backendDir, "src/main/java/" + packageName.replace('.', '/') + "/Greeter.java"),
                    text("backend/Greeter.java.txt").replace("${package}", packageName));
            for (String test : new String[] {"ApiTest", "ServedApiTest"}) {
                write(new File(backendDir, "src/test/java/" + packageName.replace('.', '/') + "/" + test + ".java"),
                        text("backend/" + test + ".java.txt").replace("${package}", packageName)
                                .replace("__BACKEND__", taskPrefix));
            }
        }
    }

    private static void write(File f, String text) throws IOException {
        writeBinary(f, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBinary(File f, byte[] content) throws IOException {
        if (f.exists()) {
            return;
        }
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Could not create " + parent);
        }
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(content);
        } finally {
            out.close();
        }
    }
}
