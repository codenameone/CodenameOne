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

import com.codename1.build.FirstBuildTelemetry;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Properties;

/// Reports the start and the end of a Maven build of a project that opted in to
/// build progress reporting -- see [FirstBuildTelemetry] for what is sent and when.
///
/// A lifecycle participant rather than a goal, because only the participant sees the
/// build end: a goal that never runs (the compiler failed first, a dependency never
/// downloaded) cannot report why, and those are exactly the first builds that die
/// silently. It runs because the generated project declares this plugin with
/// `<extensions>true</extensions>`; a project that does not, or that lacks the two
/// properties, reports nothing.
///
/// Registered in META-INF/plexus/components.xml.
public class FirstBuildTelemetryParticipant extends AbstractMavenLifecycleParticipant {
    /// Claimed by the one participant that reports a build. Every module that inherits
    /// the plugin declaration loads the extension again, in its own class realm, so
    /// each gets its own instance of this class -- and its own statics. A system
    /// property is the one thing they all share, so it is what decides which of them
    /// reports, and a build is counted once however many modules it has.
    static final String CLAIM_PROPERTY = "cn1.telemetry.claim";

    private FirstBuildTelemetry telemetry;
    private String claim;

    @Override
    public void afterProjectsRead(MavenSession session) {
        try {
            MavenProject top = session.getTopLevelProject();
            if (top == null) {
                top = session.getCurrentProject();
            }
            if (top == null) {
                return;
            }
            Properties model = top.getProperties();
            String endpoint = property(session, model, FirstBuildTelemetry.EVENTS_PROPERTY);
            String project = property(session, model, FirstBuildTelemetry.PROJECT_PROPERTY);
            String buildTarget = property(session, model, "codename1.buildTarget");
            List<String> goals = session.getGoals();
            if (goals == null || goals.isEmpty()) {
                // No goal is no build: an IDE importing or reloading the project
                // reads it through the same session, and reporting that as a
                // successful build would mark first builds done before any ran.
                return;
            }
            FirstBuildTelemetry t = FirstBuildTelemetry.start(endpoint, project,
                    FirstBuildTelemetry.target(buildTarget, goals), System.getenv(), System.getProperties());
            if (t == null) {
                return;
            }
            String mine = Integer.toHexString(System.identityHashCode(session)) + "-" + System.nanoTime();
            if (System.getProperties().putIfAbsent(CLAIM_PROPERTY, mine) != null) {
                return;
            }
            claim = mine;
            telemetry = t;
            telemetry.launched();
        } catch (RuntimeException ignored) {
            // Reporting must never be the reason a build fails.
            telemetry = null;
        }
    }

    @Override
    public void afterSessionEnd(MavenSession session) {
        FirstBuildTelemetry t = telemetry;
        telemetry = null;
        if (t == null) {
            return;
        }
        // Released for the next build in the same JVM (an IDE's embedded Maven).
        System.getProperties().remove(CLAIM_PROPERTY, claim);
        claim = null;
        try {
            List<Throwable> failures = session.getResult() == null ? null : session.getResult().getExceptions();
            Throwable failure = null;
            if (failures != null && !failures.isEmpty()) {
                failure = failures.get(0);
                for (int i = 1; i < failures.size(); i++) {
                    failure.addSuppressed(failures.get(i));
                }
            }
            t.finished(failure);
        } catch (RuntimeException ignored) {
            // As above.
        }
    }

    /// A command-line `-D` wins over the pom, as it does for every other property.
    private static String property(MavenSession session, Properties model, String name) {
        String v = session.getUserProperties() == null ? null : session.getUserProperties().getProperty(name);
        if (v == null && session.getSystemProperties() != null) {
            v = session.getSystemProperties().getProperty(name);
        }
        if (v == null && model != null) {
            v = model.getProperty(name);
        }
        return v;
    }
}
