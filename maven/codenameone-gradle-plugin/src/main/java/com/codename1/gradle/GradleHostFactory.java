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

import com.codename1.build.Log;
import com.codename1.build.ProjectHost;
import com.codename1.project.ProjectLayout;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Builds a [ProjectHost] from a task's resolved inputs.
public final class GradleHostFactory {
    private GradleHostFactory() {
    }

    /// A resolved dependency as a task input string; see [decode(String)].
    public static String encode(org.gradle.api.artifacts.result.ResolvedArtifactResult result, String scope) {
        return GradleProjectHost.encode(result, scope);
    }

    /// The dependency [encode] wrote, or null.
    public static com.codename1.build.BuildArtifact decode(String encoded) {
        return GradleProjectHost.decode(encoded);
    }

    /// A host over the given inputs.
    ///
    /// @param task the task the host serves (for its name in messages)
    /// @param classpath the ordered classpath the build stages
    /// @param frameworkJars `codenameone-core` / `java-runtime` and friends,
    ///        recognised by file name
    public static ProjectHost create(Task task, Log log, ProjectLayout layout, String finalName, String groupId,
                                     Map<String, String> projectProperties, Map<String, String> userProperties,
                                     FileCollection classpath, List<String> sourceRoots, List<String> artifacts,
                                     FileCollection frameworkJars, String codenameOneVersion) {
        List<String> elements = new ArrayList<String>();
        for (File f : classpath) {
            elements.add(f.getAbsolutePath());
        }
        Map<String, File> framework = new LinkedHashMap<String, File>();
        if (frameworkJars != null) {
            for (File f : frameworkJars) {
                for (String artifactId : new String[] {"codenameone-core", "java-runtime", "codenameone-javase"}) {
                    if (f.getName().startsWith(artifactId + "-") && f.getName().endsWith(".jar")) {
                        framework.put(PluginInfo.GROUP + ":" + artifactId, f);
                    }
                }
            }
        }
        return new GradleProjectHost(log, layout, finalName, groupId, projectProperties, userProperties, elements,
                sourceRoots, artifacts, framework, codenameOneVersion);
    }
}
