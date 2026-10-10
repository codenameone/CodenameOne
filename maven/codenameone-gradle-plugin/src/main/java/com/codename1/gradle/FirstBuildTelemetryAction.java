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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

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
/// the start time in its parameters is the one from when the configuration was
/// cached, so such a run leaves the duration out rather than send a wrong one; a run
/// that configured itself reports it (see [#CONFIGURED]).
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

        /// Identifies the run that configured this build; see [#CONFIGURED].
        @Input
        Property<String> getConfiguredBy();
    }

    /// The run that applied the settings plugin in this JVM, until its flow action
    /// reads it. A run that reuses a cached configuration never applies the plugin, so
    /// its action finds nothing here -- or another run's token -- and knows its start
    /// time is stale. Statics are shared because the plugin's class loader is.
    private static final AtomicReference<String> CONFIGURED = new AtomicReference<String>();

    /// Tasks an IDE runs to sync a project rather than to build it. IntelliJ IDEA
    /// runs the first to load .kts build scripts on every Gradle sync; the others are
    /// the IDE-file generators. A request made only of these is not a build.
    private static final Set<String> IDE_SYNC_TASKS = new HashSet<String>(Arrays.asList(
            "prepareKotlinBuildScriptModel", "eclipse", "eclipseClasspath", "eclipseProject", "eclipseJdt",
            "cleanEclipse", "idea", "ideaModule", "ideaProject", "ideaWorkspace", "cleanIdea"));

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
            boolean configuredByThisRun = parameters.getConfiguredBy().get().equals(CONFIGURED.getAndSet(null));
            FirstBuildTelemetry.resume(parameters.getEndpoint().get(), parameters.getProjectId().get(),
                    parameters.getTarget().getOrNull(), parameters.getClient().get(),
                    parameters.getStartedAt().get())
                    .finishedWith(reason.isEmpty() ? null : reason, configuredByThisRun);
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
            List<String> tasks = buildTasks(settings.getGradle().getStartParameter().getTaskNames());
            if (tasks.isEmpty()) {
                // No task is no build: an IDE importing or syncing the project asks
                // Gradle for its model, and reporting that as a successful build would
                // mark first builds done before any ran.
                return;
            }
            final FirstBuildTelemetry t = FirstBuildTelemetry.start(
                    providers.gradleProperty(FirstBuildTelemetry.EVENTS_PROPERTY).getOrNull(),
                    providers.gradleProperty(FirstBuildTelemetry.PROJECT_PROPERTY).getOrNull(),
                    FirstBuildTelemetry.target(providers.gradleProperty("codename1.buildTarget").getOrNull(), tasks),
                    env, sys);
            if (t == null) {
                return;
            }
            final String token = t.startedAt() + "-" + System.nanoTime();
            CONFIGURED.set(token);
            t.launched();
            flowScope.always(FirstBuildTelemetryAction.class, spec -> {
                Parameters p = spec.getParameters();
                p.getEndpoint().set(t.endpoint());
                p.getProjectId().set(t.project());
                p.getTarget().set(t.target());
                p.getClient().set(t.client());
                p.getStartedAt().set(t.startedAt());
                p.getFailureReason().set(flowProviders.getBuildWorkResult().map(FirstBuildTelemetryAction::reasonOf));
                p.getConfiguredBy().set(token);
            });
        } catch (RuntimeException ignored) {
            // As above.
        }
    }

    /// The requested tasks that build something: options and IDE sync tasks removed.
    static List<String> buildTasks(List<String> requested) {
        List<String> out = new ArrayList<String>();
        if (requested == null) {
            return out;
        }
        for (String task : requested) {
            if (task == null || task.isEmpty() || task.startsWith("-")) {
                continue;
            }
            String name = task.substring(task.lastIndexOf(':') + 1);
            if (!IDE_SYNC_TASKS.contains(name)) {
                out.add(task);
            }
        }
        return out;
    }

    private static String reasonOf(BuildWorkResult result) {
        return result.getFailure().isPresent() ? FirstBuildTelemetry.reason(result.getFailure().get()) : "";
    }
}
