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
import org.gradle.api.tasks.StopExecutionException;
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
/// Without a Unity project the task is skipped, unless an earlier build of one
/// left output behind: then it runs once more to remove it (see
/// [#hasWork(File, File...)]).
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
        boolean built;
        try {
            built = build(new UnityProjectBuilder(getUnityDirectory().get().getAsFile(),
                    getOutputDirectory().get().getAsFile(), getClassesDirectory().get().getAsFile(),
                    getStateDirectory().get().getAsFile(), runtimeJar(getRuntimeJar().getFiles()),
                    single(getReferencesJar().getFiles()), tool, getDotnet().getOrNull(),
                    getMainPackage().getOrNull(), getMainClass().getOrNull(), roots, log()), owned());
        } catch (BuildException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
        if (!built) {
            // Nothing was translated, so there is nothing for the actions
            // after this one -- the compliance check of the translated classes
            // -- to read. The task has still succeeded.
            throw new StopExecutionException();
        }
    }

    /// The three directories only this task writes.
    private File[] owned() {
        return new File[] {getOutputDirectory().get().getAsFile(), getClassesDirectory().get().getAsFile(),
                getStateDirectory().get().getAsFile()};
    }

    /// Whether the task has anything to do: a Unity project to build, or the
    /// output of one that is no longer there to remove. See [#hasWork(File, File...)].
    public boolean hasWork() {
        return hasWork(getUnityDirectory().get().getAsFile(), owned());
    }

    /// What `onlyIf` asks. A project that is there is built (or found up to
    /// date). One that is not there leaves the task with work only while a
    /// directory the task owns still holds something: the generated sources
    /// are a source root of the main source set and the classes directory is
    /// part of its output, so what a deleted project left in them would be
    /// compiled and packaged until `clean`. Skipping on the sources alone, as
    /// this did, is what left it there.
    ///
    /// For an application that never had a Unity project the answer is three
    /// directory listings of directories that do not exist, and the task is
    /// skipped before Gradle snapshots anything or creates them.
    public static boolean hasWork(File unityDir, File... owned) {
        if (UnityProjectBuilder.isUnityProject(unityDir)) {
            return true;
        }
        for (File dir : owned) {
            String[] children = dir.list();
            if (children != null && children.length > 0) {
                return true;
            }
        }
        return false;
    }

    /// Runs the builder, and when there is no Unity project empties `owned`.
    /// The builder has by then removed what its own record names, as the
    /// Maven goal has it do in directories it shares with javac; here the
    /// directories are this task's alone, so whatever else is in them is a
    /// leftover of the same build and goes too -- and they are left empty, not
    /// merely free of files, which is what [#hasWork(File, File...)] reads.
    /// Returns whether a project was built.
    static boolean build(UnityProjectBuilder builder, File... owned) throws BuildException {
        if (builder.run()) {
            return true;
        }
        for (File dir : owned) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    delete(child);
                }
            }
        }
        return false;
    }

    private static void delete(File f) throws BuildException {
        File[] children = f.listFiles();
        if (children != null) {
            for (File child : children) {
                delete(child);
            }
        }
        try {
            java.nio.file.Files.deleteIfExists(f.toPath());
        } catch (java.io.IOException ex) {
            // Not quietly: what stays here is compiled or shipped.
            throw new BuildException("Cannot remove " + f + ", left by a Unity project that is no longer in"
                    + " src/main/unity: " + ex.getMessage(), ex);
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
