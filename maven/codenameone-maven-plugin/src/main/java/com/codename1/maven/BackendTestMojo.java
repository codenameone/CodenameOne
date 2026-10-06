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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a backend module's tests as a native binary: `mvn test
 * -Dcn1.backend.compiledTests=true`.
 *
 * The same test classes Surefire runs on the JVM, translated with the application
 * and linked the way {@link BackendPackageMojo} links a server. The work is
 * {@link BackendTestPackager}, in the build engine; this goal supplies the Maven
 * answers, as BackendPackageMojo does for BackendPackager.
 *
 * Off unless asked for, because it needs the native toolchain
 * {@code cn1:backend-package} needs, and takes as long as a package does. On
 * Windows, which has no native backend, it says so and does nothing; the JVM run is
 * the coverage there.
 */
// No @Execute: bound into the test phase, it must not fork the lifecycle again.
@Mojo(name = "backend-test", defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class BackendTestMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(property = "cn1.backend.compiledTests", defaultValue = "false")
    private boolean enabled;

    /** Fail on a test class left out of the run, or a test skipped in it. */
    @Parameter(property = "cn1.backend.compiledTests.strict", defaultValue = "false")
    private boolean strict;

    @Parameter(property = "cn1.backend.compiledTests.timeoutSeconds", defaultValue = "900")
    private int timeoutSeconds;

    @Parameter(property = "skipTests", defaultValue = "false")
    private boolean skipTests;

    @Parameter(property = "maven.test.skip", defaultValue = "false")
    private boolean skip;

    /** As for cn1:backend-package. */
    @Parameter(property = "cn1.backend.jdk")
    private String jdkHome;

    /** As for cn1:backend-package. */
    @Parameter(property = "cn1.backend.jdk8", defaultValue = "${env.JDK_8_HOME}")
    private String jdk8Home;

    /** As for cn1:backend-package. */
    @Parameter(property = "cn1.backend.cflags")
    private String cflags;

    /** Surefire's -Dtest, which selects the compiled run's classes as it selects the JVM run's. */
    @Parameter(property = "test")
    private String testSelection;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // Whatever an earlier compiled run reported is not this build's result,
        // however this one ends: disabled, on Windows, or with no tests left.
        for (java.io.File f : BackendTestPackager.clearCompiledReports(
                new java.io.File(project.getBuild().getDirectory()))) {
            getLog().warn("cn1: could not remove the stale report " + f);
        }
        if (!enabled || skip || skipTests) {
            return;
        }
        String os = System.getProperty("os.name", "");
        if (os.regionMatches(true, 0, "windows", 0, 7)) {
            getLog().warn("cn1: compiled backend tests need the native backend, which does "
                    + "not build on Windows; the JVM run is this platform's coverage");
            return;
        }
        BackendTestPackager packager = packager();
        if (!packager.hasTests()) {
            getLog().info("cn1: no test sources, so no compiled backend tests");
            return;
        }
        try {
            packager.execute();
        } catch (BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        } catch (BuildExecutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    private BackendTestPackager packager() {
        final SurefireSelection selection = SurefireSelection.of(project, testSelection);
        BackendTestPackager p = new BackendTestPackager(
                new MavenModuleHost(project, MavenLog.of(getLog()))) {
            @Override
            protected String binaryName() {
                return project.getArtifactId();
            }

            @Override
            protected File processedResourcesDirectory() {
                return new File(project.getBuild().getOutputDirectory());
            }

            @Override
            protected boolean declaresResources() {
                return !project.getBuild().getResources().isEmpty();
            }

            @Override
            protected String sourceEncoding() {
                return BackendPackageMojo.sourceEncodingOf(project);
            }

            @Override
            protected File resolve(String groupId, String artifactId, String version, String classifier)
                    throws BuildExecutionException {
                try {
                    return BackendPackageMojo.resolve(repositorySystem, localRepository, project,
                            groupId, artifactId, version, classifier);
                } catch (MojoExecutionException ex) {
                    throw new BuildExecutionException(ex.getMessage(), ex);
                }
            }

            @Override
            protected List<String> testSourceRoots() {
                List<String> roots = new ArrayList<String>();
                for (Object root : project.getTestCompileSourceRoots()) {
                    roots.add(String.valueOf(root));
                }
                return roots;
            }

            @Override
            protected File testOutputDirectory() {
                return new File(project.getBuild().getTestOutputDirectory());
            }

            @Override
            protected List<String> testClasspathElements() {
                // The test-SCOPED artifacts alone: the compile classpath already
                // has the compile ones, and runtime-scoped ones (a JDBC driver) are
                // JVM-only, as the main translation treats them.
                List<String> out = new ArrayList<String>();
                for (org.apache.maven.artifact.Artifact a : project.getArtifacts()) {
                    if (org.apache.maven.artifact.Artifact.SCOPE_TEST.equals(a.getScope()) && a.getFile() != null) {
                        out.add(a.getFile().getAbsolutePath());
                    }
                }
                return out;
            }

            @Override
            protected boolean selectsTestClass(String binaryName) {
                return selection.test(binaryName);
            }

            @Override
            protected boolean selectsTestMethod(String binaryName, String method) {
                return selection.test(binaryName + "#" + method);
            }
        };
        p.strict(strict).timeoutSeconds(timeoutSeconds);
        p.jdk(jdkHome, jdk8Home).cflags(cflags);
        return p;
    }
}
