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

import com.codename1.build.Log;
import com.codename1.builders.BuildException;
import com.codename1.gradle.GradleLog;
import com.codename1.maven.CompatLayers;
import com.codename1.maven.CompatRemapper;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// Relocates a compiled class directory onto whichever compatibility layers
/// the application has switched on -- Android, Swing, JavaFX -- and ships
/// their runtimes; see [CompatRemapper]. Attached to the compile task before
/// the compliance check, as the Maven build binds `remap-compat` before
/// `bytecode-compliance`.
///
/// For an application with only Android sources it does what the Android
/// step alone does, file for file; `CompatRemapperTest` holds that.
public class RemapCompatAction implements Action<Task> {

    private final File classesDir;
    private final FileCollection compileClasspath;
    private final File onClickNames;
    private final boolean relocateOnly;
    private final FileCollection handlerDirs;
    private File desktopDir;
    private FileCollection shipWhenEmpty;
    private File fxmlSources;

    public RemapCompatAction(File classesDir, FileCollection compileClasspath, File onClickNames,
                             boolean relocateOnly, FileCollection handlerDirs) {
        this.classesDir = classesDir;
        this.compileClasspath = compileClasspath;
        this.onClickNames = onClickNames;
        this.relocateOnly = relocateOnly;
        this.handlerDirs = handlerDirs;
    }

    /// The module's desktop source directory (`src/main/desktop`), whose
    /// `resources` the desktop runtime indexes. The application's main
    /// class is a source file of the project and is relocated like any other
    /// class; nothing here generates one.
    public RemapCompatAction withDesktopSourceDirectory(File dir) {
        this.desktopDir = dir;
        return this;
    }

    /// Where the Java generated from the application's FXML documents is
    /// kept. The documents are compiled by this action, before it relocates
    /// anything: javac has just compiled the application, so a document can
    /// name one of its classes, and Kotlin's classes -- compiled earlier, into
    /// a directory among the handler directories -- are still as Kotlin
    /// wrote them. See [com.codename1.fxml.FxmlClassCompiler].
    public RemapCompatAction withFxmlSourceDirectory(File dir) {
        this.fxmlSources = dir;
        return this;
    }

    /// Makes a relocate-only action a full one when `javaSources` turns out
    /// to be empty. Kotlin's directory is relocated only, because javac's
    /// pass ships the runtimes and generates what has to be generated -- but
    /// `compileJava` does nothing at all for a module with no Java source,
    /// and then this directory is the only one there is to ship them in.
    public RemapCompatAction shippingWhenEmpty(FileCollection javaSources) {
        this.shipWhenEmpty = javaSources;
        return this;
    }

    @Override
    public void execute(Task task) {
        // Classes translated from Python get the Python file and lines, so
        // a debugger and a stack trace show the Python source.
        try {
            com.codename1.maven.PythonLineMapper.apply(classesDir);
        } catch (java.io.IOException ex) {
            throw new GradleException("Could not map Python line numbers in " + classesDir, ex);
        }
        Set<File> classpath = compileClasspath.getFiles();
        // By the jars alone, as the first test: with none there is nothing to
        // read the classes for.
        if (CompatLayers.active(classpath).isEmpty() || !classesDir.isDirectory()) {
            return;
        }
        Log log = new GradleLog(task.getLogger());
        CompatRemapper r = new CompatRemapper(classesDir, classpath, onClickNames, log)
                .withDesktopSourceDirectory(desktopDir)
                .withFxmlSourceDirectory(fxmlSources);
        if (relocateOnly && !(shipWhenEmpty != null && shipWhenEmpty.isEmpty())) {
            r.relocateOnly();
        } else if (handlerDirs != null) {
            List<File> dirs = new ArrayList<File>(handlerDirs.getFiles());
            r.withHandlerDirectories(dirs);
        }
        try {
            r.run();
        } catch (BuildException ex) {
            throw new GradleException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }
}
