/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Builds the application for the browser and stages it where this server
 * serves it from: `mvn cn1:backend-webapp`.
 *
 * A server that finds an application in its `webapp` directory answers with it
 * at `/`, so one address is both the API and the app and a person can use it
 * with nothing installed. This goal is what fills that directory. It runs the
 * project's own JavaScript build -- the one `mvn package
 * -Dcodename1.platform=javascript -Dcodename1.buildTarget=local-javascript`
 * runs -- finds the bundle, and unpacks it into `target/webapp` with a
 * compressed copy beside each text file.
 *
 * `cn1:backend` then serves it without being told to, and `cn1:backend-package`
 * leaves it beside the binary, which is where a deployed server looks.
 *
 * It is a goal of its own rather than part of every build because the browser
 * build takes about a minute and not every server publishes its app: a server
 * edited and restarted all afternoon should not pay for a translation each
 * time. Run it when the app changed; what it staged stays until `mvn clean`.
 */
// No dependency resolution on purpose. This goal is useful before anything is
// compiled, and resolving a server module's classpath from a clean checkout
// fails on the sibling module it shares with the app.
@Mojo(name = "backend-webapp", threadSafe = false)
public class BackendWebAppMojo extends AbstractMojo {

    /** How much of a quiet browser build's output is kept to show if it fails. */
    private static final int QUIET_TAIL = 200;

    /** Passed to the nested build, so a build that runs this goal cannot start itself. */
    static final String NESTED = "cn1.backend.webapp.nested";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true)
    private List<MavenProject> reactorProjects;

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${localRepository}", readonly = true)
    private ArtifactRepository localRepository;

    /**
     * A browser build to stage instead of building one: the archive a
     * JavaScript build produced, or a directory unpacked from it. This is how a
     * bundle built somewhere else -- by a build server, or an earlier CI job --
     * is hosted.
     */
    @Parameter(property = "cn1.backend.webapp.bundle")
    private File bundle;

    /** Where the application is staged. The server looks here when run with cn1:backend. */
    @Parameter(property = "cn1.backend.webapp.output", defaultValue = "${project.build.directory}/webapp")
    private File output;

    /**
     * The application's root project, the one whose modules include the client.
     * Defaults to the directory above the server module.
     */
    @Parameter(property = "cn1.backend.webapp.appRoot")
    private File appRoot;

    /** Skips the goal, for a build that binds it and sometimes has no use for it. */
    @Parameter(property = "cn1.backend.webapp.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(property = NESTED, defaultValue = "false", readonly = true)
    private boolean nested;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || nested) {
            return;
        }
        if (passOver()) {
            return;
        }
        try {
            File source = bundle;
            if (source == null) {
                source = build();
            }
            new BackendWebApp(MavenLog.of(getLog())).stage(source, output);
        } catch (BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        } catch (BuildExecutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /**
     * As {@link BackendModules#passOver}, from the declared dependencies: this
     * goal resolves none, so there is no resolved set to ask.
     */
    private boolean passOver() {
        if (reactorProjects == null || reactorProjects.size() < 2) {
            return false;
        }
        List<Dependency> declared = project.getDependencies();
        for (Dependency dependency : declared == null ? Collections.<Dependency>emptyList() : declared) {
            if ("com.codenameone".equals(dependency.getGroupId())
                    && "codenameone-backend".equals(dependency.getArtifactId())) {
                return false;
            }
        }
        getLog().info("cn1:backend-webapp skipped for " + project.getArtifactId()
                + ": it does not depend on codenameone-backend, so it is not a server.");
        return true;
    }

    /** Runs the application's browser build and answers the bundle it produced. */
    private File build() throws BuildFailureException, BuildExecutionException {
        File root = appRoot != null ? appRoot : project.getBasedir().getParentFile();
        if (root == null || !new File(root, "pom.xml").isFile() || !new File(root, "common").isDirectory()) {
            throw new BuildFailureException("cn1:backend-webapp builds the application this server "
                    + "belongs to, and " + root + " is not a Codename One project (no pom.xml with a "
                    + "common module). Name it with -Dcn1.backend.webapp.appRoot=<dir>, or pass a "
                    + "bundle that is already built with -Dcn1.backend.webapp.bundle=<zip or dir>.");
        }
        long started = System.currentTimeMillis();
        Properties props = new Properties();
        Properties user = session == null ? null : session.getUserProperties();
        if (user != null) {
            // The application's own build settings: hints given on the command
            // line, the version overrides a snapshot build is run with.
            for (String key : user.stringPropertyNames()) {
                if (key.startsWith("codename1.")
                        || (key.startsWith("cn1.") && !key.startsWith("cn1.backend."))) {
                    props.setProperty(key, user.getProperty(key));
                }
            }
        }
        props.setProperty("codename1.platform", "javascript");
        props.setProperty("codename1.buildTarget", "local-javascript");
        props.setProperty("skipTests", "true");
        props.setProperty(NESTED, "true");
        // The JavaScript build sends cross-origin requests through a proxy
        // servlet it expects beside the page. This server is not a servlet
        // container and has no such path, and an app talking to the server that
        // hosts it is same-origin and never proxied anyway -- so the build is
        // told not to wire one in, unless the project has chosen for itself.
        if (props.getProperty("codename1.arg.javascript.inject_proxy") == null
                && props.getProperty("codename1.arg.javascript.proxy.url") == null
                && !BackendWebApp.choosesProxy(new File(root, "common/codenameone_settings.properties"))) {
            props.setProperty("codename1.arg.javascript.inject_proxy", "false");
        }
        // Or the nested build resolves from a different repository than this one.
        if (localRepository != null && localRepository.getBasedir() != null) {
            props.setProperty("maven.repo.local", localRepository.getBasedir());
        }

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(root);
        request.setGoals(Collections.singletonList("package"));
        request.setProperties(props);
        request.setBatchMode(true);
        // A build asked to be quiet means all of it: the browser build logs a
        // line per translated class. Its last lines are kept, because a failure
        // with nothing above it says nothing.
        final java.util.ArrayDeque<String> tail = new java.util.ArrayDeque<String>();
        if (!getLog().isInfoEnabled()) {
            org.apache.maven.shared.invoker.InvocationOutputHandler quiet =
                    new org.apache.maven.shared.invoker.InvocationOutputHandler() {
                @Override
                public void consumeLine(String line) {
                    if (tail.size() >= QUIET_TAIL) {
                        tail.removeFirst();
                    }
                    tail.addLast(line);
                }
            };
            request.setOutputHandler(quiet);
            request.setErrorHandler(quiet);
        }
        if (session != null) {
            request.setOffline(session.isOffline());
            File settings = session.getRequest() == null ? null : session.getRequest().getUserSettingsFile();
            if (settings != null && settings.isFile()) {
                request.setUserSettingsFile(settings);
            }
        }
        getLog().info("Building " + root.getName() + " for the browser");
        try {
            InvocationResult result = new DefaultInvoker().execute(request);
            if (result.getExitCode() != 0) {
                for (String line : tail) {
                    getLog().error(line);
                }
                throw new BuildFailureException("The browser build of " + root + " failed (exit "
                        + result.getExitCode() + "); its output is above. Run `mvn package "
                        + "-Dcodename1.platform=javascript -Dcodename1.buildTarget=local-javascript` "
                        + "there to see it on its own.");
            }
        } catch (MavenInvocationException ex) {
            throw new BuildExecutionException("Could not run the browser build of " + root, ex);
        }
        File built = BackendWebApp.locateBundle(root);
        // One from an earlier build is not this build's answer.
        if (built == null || built.lastModified() < started - 2000) {
            throw new BuildFailureException("The browser build of " + root + " succeeded and left no "
                    + "bundle in javascript/target or common/target. A project whose browser "
                    + "build is made elsewhere can stage it with -Dcn1.backend.webapp.bundle=<zip or dir>.");
        }
        return built;
    }
}
