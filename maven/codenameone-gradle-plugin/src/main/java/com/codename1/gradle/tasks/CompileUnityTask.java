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

import com.codename1.builders.BuildException;
import com.codename1.maven.UnityProjectBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.LocalState;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Builds `src/main/unity` -- a Unity project's `Assets` and
/// `ProjectSettings` -- into the scripts' class files, the images the scenes
/// draw, and the scenes as Java source. The same [UnityProjectBuilder] as the
/// Maven plugin's `compile-unity` goal.
///
/// Where the Maven goal installs the translated classes in `target/classes`,
/// this task keeps them in a directory of its own. `compileJava` owns its
/// destination and empties it whenever it compiles from scratch, so classes
/// written there by another task would be lost on the next full compile. The
/// directory is an output directory of the main source set instead: on javac's
/// classpath for the generated scene source, and part of everything the
/// source set's output goes into -- the simulator's classpath, the jar and
/// every target's upload.
///
/// Not cacheable: the classes come out of whichever .NET SDK this machine has,
/// and the generated main class depends on whether the project has its own.
/// Gradle's up-to-date check still skips the task while nothing it reads has
/// changed, and then no .NET SDK is looked for at all.
@DisableCachingByDefault(because = "Compiles C# with the .NET SDK installed on this machine")
public abstract class CompileUnityTask extends Cn1Task {

    /// `src/main/unity`. Not an input by itself: only the two directories a
    /// build reads are, in [#getSources()], so the `Library` or `Temp` a Unity
    /// editor leaves beside them cannot make the task run again.
    @Internal
    public abstract DirectoryProperty getUnityDirectory();

    /// `Assets` and `ProjectSettings`.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /// The `codenameone-unity-compat` jar the scripts are translated against.
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getRuntimeJar();

    /// Its `references` classifier: the assemblies C# compiles against.
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getReferencesJar();

    /// The translator and scene compiler, with what they run on.
    @Classpath
    public abstract ConfigurableFileCollection getToolClasspath();

    @Input
    @Optional
    public abstract Property<String> getMainPackage();

    @Input
    @Optional
    public abstract Property<String> getMainClass();

    /// Source roots in which an existing main class suppresses the generated one.
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceRoots();

    /// The `dotnet` executable or its directory. Not an input: which SDK
    /// compiled the scripts is not something a build should repeat for.
    @Internal
    public abstract Property<String> getDotnet();

    /// The generated Java source.
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /// The translated classes and the images.
    @OutputDirectory
    public abstract DirectoryProperty getClassesDirectory();

    /// Everything else the build writes: the generated C# project and its
    /// intermediate files, the staged classes, the logs. Local state rather
    /// than an output, because it holds absolute paths and the SDK's own
    /// first-run files, none of which another machine could use.
    @LocalState
    public abstract DirectoryProperty getStateDirectory();

    @TaskAction
    public void compile() {
        List<File> roots = new ArrayList<File>(getSourceRoots().getFiles());
        List<File> tool = new ArrayList<File>(getToolClasspath().getFiles());
        try {
            new UnityProjectBuilder(getUnityDirectory().get().getAsFile(), getOutputDirectory().get().getAsFile(),
                    getClassesDirectory().get().getAsFile(), getStateDirectory().get().getAsFile(),
                    runtimeJar(getRuntimeJar().getFiles()), single(getReferencesJar().getFiles()), tool,
                    getDotnet().getOrNull(), getMainPackage().getOrNull(), getMainClass().getOrNull(), roots,
                    log()).run();
        } catch (BuildException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /// Whether a file of the compile classpath is the runtime jar. The
    /// `references` classifier is published under the same artifact id, and a
    /// build that declares it would otherwise be translated against a jar of
    /// assemblies.
    public static boolean isRuntimeJar(String fileName) {
        return fileName.startsWith(UnityProjectBuilder.RUNTIME_ARTIFACT + "-") && fileName.endsWith(".jar")
                && !fileName.endsWith("-" + UnityProjectBuilder.REFERENCES_CLASSIFIER + ".jar")
                && !fileName.endsWith("-sources.jar") && !fileName.endsWith("-javadoc.jar");
    }

    /// The runtime jar among `files`, or null.
    static File runtimeJar(Iterable<File> files) {
        for (File f : files) {
            if (isRuntimeJar(f.getName())) {
                return f;
            }
        }
        return null;
    }

    private static File single(Iterable<File> files) {
        java.util.Iterator<File> it = files.iterator();
        return it.hasNext() ? it.next() : null;
    }
}
