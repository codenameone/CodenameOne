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
import com.codename1.maven.AndroidRemapper;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/// Relocates Android code in a compiled class directory onto the
/// compatibility runtime; see [AndroidRemapper]. Attached to the compile task
/// before the compliance check, as the Maven build binds `remap-android`
/// before `bytecode-compliance`.
public class RemapAndroidAction implements Action<Task> {

    private final File classesDir;
    private final FileCollection compileClasspath;
    private final File onClickNames;
    private final boolean relocateOnly;
    private final FileCollection handlerDirs;

    public RemapAndroidAction(File classesDir, FileCollection compileClasspath, File onClickNames,
                              boolean relocateOnly, FileCollection handlerDirs) {
        this.classesDir = classesDir;
        this.compileClasspath = compileClasspath;
        this.onClickNames = onClickNames;
        this.relocateOnly = relocateOnly;
        this.handlerDirs = handlerDirs;
    }

    @Override
    public void execute(Task task) {
        File jar = null;
        for (File f : compileClasspath.getFiles()) {
            if (f.getName().startsWith(com.codename1.maven.AndroidResourceRunner.COMPAT_ARTIFACT + "-")
                    && f.getName().endsWith(".jar")) {
                jar = f;
            }
        }
        if (jar == null || !classesDir.isDirectory()) {
            return;
        }
        Log log = new GradleLog(task.getLogger());
        AndroidRemapper r = new AndroidRemapper(classesDir, jar, onClickNames, log);
        if (relocateOnly) {
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
