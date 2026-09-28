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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/// Fails the build when the Java and Kotlin classes directories hold the same
/// path.
///
/// Gradle compiles Kotlin and Java into separate directories, and the
/// annotation processors run over each one after its compiler. They cannot
/// share one: a Java full recompilation empties its destination, which would
/// delete Kotlin's classes. So a processor that emits one fixed registry --
/// `@AppIntent` handlers, ORM data access bootstraps -- writes a partial copy
/// into each directory when both languages declare that annotation, and only
/// one copy survives on the classpath. Maven compiles both into one directory
/// and never meets this. Rather than ship a registry missing one language's
/// declarations, the build stops and says so.
///
/// Runs after javac, which in a mixed project runs after the Kotlin compiler.
public final class SplitOutputCheck implements Action<Task> {
    /// The ORM enhancer's bookkeeping, which each directory keeps for itself.
    static final String PER_DIRECTORY = "META-INF/cn1/orm-enhanced-dependencies.list";

    private final File javaClasses;
    private final File kotlinClasses;

    /// @param javaClasses javac's destination
    /// @param kotlinClasses the Kotlin compiler's destination
    public SplitOutputCheck(File javaClasses, File kotlinClasses) {
        this.javaClasses = javaClasses;
        this.kotlinClasses = kotlinClasses;
    }

    @Override
    public void execute(Task task) {
        List<String> both = collisions(javaClasses, kotlinClasses);
        if (both.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("The Java and Kotlin classes both produced ");
        sb.append(both.size() == 1 ? "this file" : "these files").append(", and only one copy can be used:\n");
        for (String path : both) {
            sb.append("  - ").append(path).append('\n');
        }
        sb.append("This happens when both languages declare annotations that generate one shared registry "
                + "(for example @AppIntent handlers or ORM entities). Declare each kind in one language, "
                + "or move those classes to the same language.");
        throw new GradleException(sb.toString());
    }

    /// The relative paths present under both directories, bookkeeping aside.
    static List<String> collisions(File a, File b) {
        Set<String> inA = new TreeSet<String>();
        collect(a, "", inA);
        List<String> out = new ArrayList<String>();
        Set<String> inB = new TreeSet<String>();
        collect(b, "", inB);
        for (String path : inA) {
            if (inB.contains(path) && !PER_DIRECTORY.equals(path)) {
                out.add(path);
            }
        }
        return out;
    }

    private static void collect(File dir, String prefix, Set<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c.isDirectory()) {
                collect(c, prefix + c.getName() + "/", out);
            } else {
                out.add(prefix + c.getName());
            }
        }
    }
}
