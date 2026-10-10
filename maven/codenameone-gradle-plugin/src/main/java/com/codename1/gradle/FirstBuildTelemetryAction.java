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

import com.codename1.build.FirstBuildTelemetry;
import org.gradle.api.flow.BuildWorkResult;
import org.gradle.api.flow.FlowAction;
import org.gradle.api.flow.FlowParameters;
import org.gradle.api.flow.FlowProviders;
import org.gradle.api.flow.FlowScope;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.util.HashMap;
import java.util.Map;

/// Reports how a Gradle build of an opted-in project ended -- the Gradle half of
/// [FirstBuildTelemetry], which says what is sent and when.
///
/// A flow action because that is how a Gradle build is told it has finished while
/// staying compatible with the configuration cache, which generated projects turn on:
/// `buildFinished` listeners are deprecated and refuse to run with it. The parameters
/// carry the reporter's parts rather than the reporter, because they are serialized
/// with the build.
///
/// Two consequences of the cache. A run that reuses a cached configuration does not
/// apply the settings plugin, so it reports how it ended but not that it started --
/// the first build of a project, the one that matters here, is never such a run. And
/// the start time in the parameters is the one from when the configuration was
/// cached, so the exit report leaves the duration out rather than send a wrong one.
public abstract class FirstBuildTelemetryAction implements FlowAction<FirstBuildTelemetryAction.Parameters> {

    /// What the action needs, all of it plain values.
    public interface Parameters extends FlowParameters {
        @Input
        Property<String> getEndpoint();

        @Input
        Property<String> getProjectId();

        @Input
        @Optional
        Property<String> getTarget();

        @Input
        Property<String> getClient();

        @Input
        Property<Long> getStartedAt();

        /// Empty when the build succeeded, otherwise the reason it failed.
        @Input
        Property<String> getFailureReason();
    }

    /// The environment variables the reporter reads, and nothing else: reading the
    /// whole environment would make every variable a configuration cache input.
    private static final String[] ENV = {
        FirstBuildTelemetry.OPT_OUT_ENV, FirstBuildTelemetry.LAUNCHER_ENV,
        "IDEA_INITIAL_DIRECTORY", "TERMINAL_EMULATOR", "ECLIPSE_HOME",
        "TERM_PROGRAM", "VSCODE_PID", "VSCODE_IPC_HOOK_CLI"
    };
    private static final String[] SYSTEM_PROPERTIES = {
        "cn1.telemetry", "idea.version", "idea.active", "netbeans.execution", "netbeans.home",
        "osgi.framework", "eclipse.home.location"
    };

    @Override
    public void execute(Parameters parameters) {
        try {
            String reason = parameters.getFailureReason().getOrElse("");
            FirstBuildTelemetry.resume(parameters.getEndpoint().get(), parameters.getProjectId().get(),
                    parameters.getTarget().getOrNull(), parameters.getClient().get(),
                    parameters.getStartedAt().get())
                    .finishedWith(reason.isEmpty() ? null : reason, false);
        } catch (RuntimeException ignored) {
            // Reporting must never be the reason a build fails.
        }
    }

    /// Reports the start of this build and arranges for its end to be reported, when
    /// the project opted in. Called from the settings plugin, which is applied before
    /// anything is configured -- so a build that fails while configuring is reported
    /// too.
    static void register(Settings settings, FlowScope flowScope, FlowProviders flowProviders) {
        try {
            ProviderFactory providers = settings.getProviders();
            Map<String, String> env = new HashMap<String, String>();
            for (String name : ENV) {
                Provider<String> v = providers.environmentVariable(name);
                if (v.isPresent()) {
                    env.put(name, v.get());
                }
            }
            Map<String, String> sys = new HashMap<String, String>();
            for (String name : SYSTEM_PROPERTIES) {
                Provider<String> v = providers.systemProperty(name);
                if (v.isPresent()) {
                    sys.put(name, v.get());
                }
            }
            final FirstBuildTelemetry t = FirstBuildTelemetry.start(
                    providers.gradleProperty(FirstBuildTelemetry.EVENTS_PROPERTY).getOrNull(),
                    providers.gradleProperty(FirstBuildTelemetry.PROJECT_PROPERTY).getOrNull(),
                    FirstBuildTelemetry.target(providers.gradleProperty("codename1.buildTarget").getOrNull(),
                            settings.getGradle().getStartParameter().getTaskNames()),
                    env, sys);
            if (t == null) {
                return;
            }
            t.launched();
            flowScope.always(FirstBuildTelemetryAction.class, spec -> {
                Parameters p = spec.getParameters();
                p.getEndpoint().set(t.endpoint());
                p.getProjectId().set(t.project());
                p.getTarget().set(t.target());
                p.getClient().set(t.client());
                p.getStartedAt().set(t.startedAt());
                p.getFailureReason().set(flowProviders.getBuildWorkResult().map(FirstBuildTelemetryAction::reasonOf));
            });
        } catch (RuntimeException ignored) {
            // As above.
        }
    }

    private static String reasonOf(BuildWorkResult result) {
        return result.getFailure().isPresent() ? FirstBuildTelemetry.reason(result.getFailure().get()) : "";
    }
}
