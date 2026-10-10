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
package com.codename1.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Set;

/// Which modules of a reactor the server goals apply to.
final class BackendModules {

    private BackendModules() {
    }

    /// Whether a server goal invoked from the command line should pass over
    /// `project` without doing anything.
    ///
    /// A goal named on the command line runs in every module of the reactor.
    /// A project whose server shares a library with its app has to build that
    /// library first, so the natural command is
    /// `mvn -pl backend -am cn1:backend-package` -- and that used to stop in the
    /// shared module, which has no server in it, with an instruction to add a
    /// dependency it must not have. The only way round was to install the
    /// library separately before every build.
    ///
    /// So a module without the backend runtime is skipped when it is one of
    /// several. On its own it is still an error: a single module asked to
    /// package a server it does not contain is a mistake worth reporting.
    static boolean passOver(MavenProject project, List<MavenProject> reactor, Log log, String goal) {
        if (reactor == null || reactor.size() < 2 || dependsOnBackend(project)) {
            return false;
        }
        log.info("cn1:" + goal + " skipped for " + project.getArtifactId()
                + ": it does not depend on codenameone-backend, so it is not a server.");
        return true;
    }

    private static boolean dependsOnBackend(MavenProject project) {
        Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts == null) {
            return false;
        }
        for (Artifact artifact : artifacts) {
            if ("com.codenameone".equals(artifact.getGroupId())
                    && "codenameone-backend".equals(artifact.getArtifactId())) {
                return true;
            }
        }
        return false;
    }
}
