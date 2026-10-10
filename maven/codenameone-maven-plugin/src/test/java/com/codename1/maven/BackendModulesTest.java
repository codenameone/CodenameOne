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
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// `mvn -pl backend -am cn1:backend-package` runs the goal in the shared
/// library too, which has no server in it. That module is passed over; a module
/// built on its own that is not a server is still told so.
class BackendModulesTest {

    @Test
    void aModuleWithoutTheBackendIsPassedOverInAReactor() {
        MavenProject shared = module("shared", artifact("com.example", "notes"));
        MavenProject server = module("backend",
                artifact("com.example", "shared"), artifact("com.codenameone", "codenameone-backend"));
        List<MavenProject> reactor = Arrays.asList(shared, server);
        Recorder log = new Recorder();

        assertTrue(BackendModules.passOver(shared, reactor, log, "backend-package"));
        assertFalse(BackendModules.passOver(server, reactor, log, "backend-package"));

        assertEquals(1, log.lines.size(), "only the skipped module is mentioned: " + log.lines);
        assertTrue(log.lines.get(0).contains("cn1:backend-package skipped for shared"),
                log.lines.get(0));
    }

    /// On its own the module is the one the developer asked for, and the goal
    /// goes on to report what is wrong with it.
    @Test
    void aModuleOnItsOwnIsNeverPassedOver() {
        MavenProject shared = module("shared");
        Recorder log = new Recorder();

        assertFalse(BackendModules.passOver(shared, Collections.singletonList(shared), log, "backend"));
        assertFalse(BackendModules.passOver(shared, Collections.<MavenProject>emptyList(), log, "backend"));
        assertFalse(BackendModules.passOver(shared, null, log, "backend"));
        assertTrue(log.lines.isEmpty(), log.lines.toString());
    }

    /// The artifact id alone is not the runtime: somebody else's
    /// `codenameone-backend` does not make a module a server.
    @Test
    void theBackendIsRecognizedByGroupAndArtifact() {
        MavenProject lookalike = module("lookalike", artifact("org.other", "codenameone-backend"),
                artifact("com.codenameone", "codenameone-core"));
        MavenProject unresolved = new MavenProject();
        unresolved.setArtifactId("unresolved");
        unresolved.setArtifacts(null);
        MavenProject server = module("backend", artifact("com.codenameone", "codenameone-backend"));
        List<MavenProject> reactor = Arrays.asList(lookalike, unresolved, server);
        Recorder log = new Recorder();

        assertTrue(BackendModules.passOver(lookalike, reactor, log, "backend"));
        assertTrue(BackendModules.passOver(unresolved, reactor, log, "backend"));
        assertFalse(BackendModules.passOver(server, reactor, log, "backend"));
    }

    private static MavenProject module(String artifactId, Artifact... dependencies) {
        MavenProject project = new MavenProject();
        project.setArtifactId(artifactId);
        Set<Artifact> artifacts = new LinkedHashSet<Artifact>(Arrays.asList(dependencies));
        project.setArtifacts(artifacts);
        return project;
    }

    private static Artifact artifact(String groupId, String artifactId) {
        return new DefaultArtifact(groupId, artifactId, "1.0", Artifact.SCOPE_COMPILE, "jar", null,
                new DefaultArtifactHandler("jar"));
    }

    private static final class Recorder extends SystemStreamLog {
        final List<String> lines = new java.util.ArrayList<String>();

        @Override
        public void info(CharSequence content) {
            lines.add(String.valueOf(content));
        }
    }
}
