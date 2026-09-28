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
package com.codename1.gradle.tasks;

import com.codename1.build.BuildExecutionException;
import com.codename1.maven.NativeInterfaces;
import com.codename1.project.NativePlatform;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// `generateNativeInterfaces` and `verifyNativeInterfaces`.
///
/// Generating writes a stub for every platform into `src/<platform>/<lang>`,
/// creating the directories only then; nothing in the build file changes,
/// because the platform source sets always exist. Verifying runs before every
/// platform build and fails early, naming the file to create, when an
/// interface has no implementation for that platform -- which a cloud build
/// would otherwise only report minutes later.
@DisableCachingByDefault(because = "Writes into src/, or is a check")
public abstract class NativeInterfacesTask extends Cn1Task {
    /// The compiled classes to scan: every classes directory of the main source
    /// set, since Gradle compiles Kotlin apart from Java. A directory that does
    /// not exist (javac's, in a pure Kotlin project) is simply empty.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getClassesDirectories();

    /// The compile classpath the interfaces load against.
    @Classpath
    public abstract ConfigurableFileCollection getCompileClasspath();

    /// When set, check this platform instead of generating.
    @Input
    @Optional
    public abstract Property<String> getVerifyPlatform();

    /// Generate for this interface only (simple or binary name).
    @Input
    @Optional
    public abstract Property<String> getOnly();

    /// Also write a Swift implementation for iOS.
    @Input
    public abstract Property<Boolean> getSwift();

    /// Also write a Kotlin implementation for Android.
    @Input
    public abstract Property<Boolean> getKotlin();

    /// Replace existing implementation files.
    @Input
    public abstract Property<Boolean> getOverwrite();

    @TaskAction
    public void run() {
        List<String> cp = new ArrayList<String>();
        for (File f : getCompileClasspath()) {
            cp.add(f.getAbsolutePath());
        }
        NativeInterfaces ni = new NativeInterfaces(log(),
                new ArrayList<File>(getClassesDirectories().getFiles()), cp);
        try {
            if (getVerifyPlatform().isPresent()) {
                NativePlatform platform = NativePlatform.fromId(getVerifyPlatform().get());
                if (platform == null) {
                    return;
                }
                List<String> missing = ni.missingImplementations(layout(), platform);
                if (!missing.isEmpty()) {
                    StringBuilder sb = new StringBuilder("Native interfaces are missing their ")
                            .append(platform.id()).append(" implementation:\n");
                    for (String m : missing) {
                        sb.append("  - ").append(m).append('\n');
                    }
                    sb.append("Run ./gradlew generateNativeInterfaces to write stubs, then implement them.");
                    throw new GradleException(sb.toString());
                }
                return;
            }
            List<File> written = ni.generate(layout(), getOnly().getOrNull(), getSwift().get(), getKotlin().get(),
                    getOverwrite().get(), "./gradlew generateNativeInterfaces -Pcn1.overwrite=true");
            for (File f : written) {
                getLogger().lifecycle("Wrote " + f);
            }
        } catch (BuildExecutionException ex) {
            throw new GradleException(ex.getMessage(), ex);
        }
    }
}
