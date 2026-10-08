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
    private File desktopEntry;
    private String applicationMain;
    private FileCollection shipWhenEmpty;
    private FileCollection libraries;

    public RemapCompatAction(File classesDir, FileCollection compileClasspath, File onClickNames,
                             boolean relocateOnly, FileCollection handlerDirs) {
        this.classesDir = classesDir;
        this.compileClasspath = compileClasspath;
        this.onClickNames = onClickNames;
        this.relocateOnly = relocateOnly;
        this.handlerDirs = handlerDirs;
    }

    /// The record of the desktop application's entry point
    /// (`src/main/desktop/cn1-desktop.properties`), for the entry point
    /// generators.
    public RemapCompatAction withDesktopEntryRecord(File record) {
        this.desktopEntry = record;
        return this;
    }

    /// The project's main class (`codename1.packageName` and
    /// `codename1.mainName`), which a desktop application's entry point is
    /// generated as.
    public RemapCompatAction withApplicationMain(String className) {
        this.applicationMain = className;
        return this;
    }

    /// The jars the application ships (`runtimeClasspath`), as opposed to
    /// those it is only compiled against (`compileOnly`). The ones written
    /// against Swing or JavaFX are unpacked into the classes directory and
    /// relocated with the application.
    public RemapCompatAction withApplicationLibraries(FileCollection jars) {
        this.libraries = jars;
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
        Set<File> classpath = compileClasspath.getFiles();
        // By the jars alone, as the first test: with none there is nothing to
        // read the classes for.
        if (CompatLayers.active(classpath).isEmpty() || !classesDir.isDirectory()) {
            return;
        }
        Log log = new GradleLog(task.getLogger());
        CompatRemapper r = new CompatRemapper(classesDir, classpath, onClickNames, log)
                .withDesktopEntryRecord(desktopEntry)
                .withApplicationMain(applicationMain);
        if (libraries != null) {
            List<File> jars = new ArrayList<File>();
            for (File f : libraries.getFiles()) {
                if (f.isFile() && f.getName().endsWith(".jar")) {
                    jars.add(f);
                }
            }
            r.withApplicationLibraries(jars);
        }
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
