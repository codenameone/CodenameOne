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
            // A dependency declared <type>pom</type> -- how a cn1lib depends on
            // another cn1lib's -lib -- resolves in Gradle to the .pom file itself.
            // Maven never puts such a dependency on the classpath, only what it
            // brings in, and the build would try to open the pom as a jar.
            if (f.isFile() && f.getName().endsWith(".pom")) {
                continue;
            }
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

    /// A host for a task that may build a target locally, which reads port jars
    /// (`AppBuilder.portArtifacts`).
    ///
    /// @param ports what the task's build target reads, as `artifactId[:classifier]`
    /// @param portJars the files of exactly those, from a configuration of their own
    ///        that nothing resolves until this task runs
    public static ProjectHost create(Task task, Log log, ProjectLayout layout, String finalName, String groupId,
                                     Map<String, String> projectProperties, Map<String, String> userProperties,
                                     FileCollection classpath, List<String> sourceRoots, List<String> artifacts,
                                     FileCollection frameworkJars, String codenameOneVersion,
                                     List<String> ports, FileCollection portJars) {
        GradleProjectHost host = (GradleProjectHost) create(task, log, layout, finalName, groupId,
                projectProperties, userProperties, classpath, sourceRoots, artifacts, frameworkJars,
                codenameOneVersion);
        if (ports != null && portJars != null && !ports.isEmpty()) {
            Map<String, File> resolved = new LinkedHashMap<String, File>();
            for (File f : portJars) {
                String port = portOf(f.getName(), ports);
                if (port != null) {
                    resolved.put(port, f);
                }
            }
            host.portJars(resolved);
        }
        return host;
    }

    /// Which of `ports` the file `name` is. The most specific artifact id wins, so
    /// `codenameone-android-compat-1.jar` is never taken for `codenameone-android`.
    static String portOf(String name, List<String> ports) {
        String best = null;
        int bestLength = -1;
        for (String port : ports) {
            int colon = port.indexOf(':');
            String artifactId = colon < 0 ? port : port.substring(0, colon);
            String classifier = colon < 0 ? null : port.substring(colon + 1);
            if (!name.startsWith(artifactId + "-") || !name.endsWith(".jar")) {
                continue;
            }
            // What follows the artifact id is the version, then the classifier.
            // A version starts with a digit; "compat-1.0" does not.
            String rest = name.substring(artifactId.length() + 1, name.length() - ".jar".length());
            if (rest.length() == 0 || !Character.isDigit(rest.charAt(0))) {
                continue;
            }
            boolean classified = classifier != null && rest.endsWith("-" + classifier);
            if (classifier != null ? !classified : rest.endsWith("-bundle")) {
                continue;
            }
            if (artifactId.length() > bestLength) {
                best = port;
                bestLength = artifactId.length();
            }
        }
        return best;
    }
}
