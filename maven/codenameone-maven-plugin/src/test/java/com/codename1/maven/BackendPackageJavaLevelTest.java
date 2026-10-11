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

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.jupiter.api.Test;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class BackendPackageJavaLevelTest {
    @Test void generatedSourceDirectoriesFollowCompilerConfiguration() {
        MavenProject project = project();
        project.getBuild().setDirectory("custom-target");
        assertEquals(new java.io.File("custom-target/generated-sources/annotations"),
                BackendPackageMojo.generatedSourcesDirectory(project, new Properties(), false));
        assertEquals(new java.io.File("custom-target/generated-test-sources/test-annotations"),
                BackendPackageMojo.generatedSourcesDirectory(project, new Properties(), true));
        Plugin compiler = new Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        compiler.setConfiguration(configuration("generatedSourcesDirectory", "custom/main"));
        compiler.addExecution(execution("default-testCompile", null, "testCompile", "generatedTestSourcesDirectory", "custom/tests"));
        project.getBuild().addPlugin(compiler);
        project.getBuild().flushPluginMap();
        assertEquals(new java.io.File("custom/main"),
                BackendPackageMojo.generatedSourcesDirectory(project, new Properties(), false));
        assertEquals(new java.io.File("custom/tests"),
                BackendPackageMojo.generatedSourcesDirectory(project, new Properties(), true));
    }

    @Test void processorPathsResolveTransitivelyWithManagementAndExclusions() throws Exception {
        MavenProject project = project();
        Plugin compiler = new Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        Xpp3Dom config = configuration("annotationProcessorPathsUseDepMgmt", "true");
        Xpp3Dom paths = new Xpp3Dom("annotationProcessorPaths");
        Xpp3Dom path = new Xpp3Dom("path");
        path.addChild(configuration("groupId", "processors").getChild("groupId"));
        path.addChild(configuration("artifactId", "main").getChild("artifactId"));
        Xpp3Dom exclusions = new Xpp3Dom("exclusions"), exclusion = new Xpp3Dom("exclusion");
        exclusion.addChild(configuration("groupId", "excluded").getChild("groupId"));
        exclusion.addChild(configuration("artifactId", "helper").getChild("artifactId"));
        exclusions.addChild(exclusion); path.addChild(exclusions); paths.addChild(path); config.addChild(paths);
        Xpp3Dom processors = new Xpp3Dom("annotationProcessors");
        processors.addChild(configuration("annotationProcessor", "processors.Main").getChild("annotationProcessor"));
        config.addChild(processors);
        compiler.setConfiguration(config);
        PluginExecution test = execution("default-testCompile", null, "testCompile", "release", "25");
        Xpp3Dom testConfig = (Xpp3Dom) test.getConfiguration();
        Xpp3Dom testPaths = new Xpp3Dom(paths);
        testPaths.getChild("path").getChild("artifactId").setValue("tests");
        testPaths.getChild("path").addChild(configuration("version", "2").getChild("version"));
        testConfig.addChild(testPaths); compiler.addExecution(test);
        project.getBuild().addPlugin(compiler);
        org.apache.maven.model.Dependency managed = new org.apache.maven.model.Dependency();
        managed.setGroupId("processors"); managed.setArtifactId("main"); managed.setVersion("1");
        org.apache.maven.model.DependencyManagement management = new org.apache.maven.model.DependencyManagement();
        management.addDependency(managed); project.getModel().setDependencyManagement(management);
        org.eclipse.aether.DefaultRepositorySystemSession session = new org.eclipse.aether.DefaultRepositorySystemSession();
        session.setArtifactTypeRegistry(new org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry());
        for (boolean tests : new boolean[] {false, true}) {
            final boolean testCompilation = tests;
            org.eclipse.aether.RepositorySystem resolver = (org.eclipse.aether.RepositorySystem)
                    java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[] {org.eclipse.aether.RepositorySystem.class}, (proxy, method, args) -> {
                assertEquals("resolveDependencies", method.getName());
                org.eclipse.aether.resolution.DependencyRequest request = (org.eclipse.aether.resolution.DependencyRequest) args[1];
                org.eclipse.aether.graph.Dependency root = request.getCollectRequest().getDependencies().get(0);
                assertEquals(testCompilation ? "tests" : "main", root.getArtifact().getArtifactId());
                assertEquals(testCompilation ? "2" : "1", root.getArtifact().getVersion());
                assertEquals("excluded", root.getExclusions().iterator().next().getGroupId());
                assertEquals(1, request.getCollectRequest().getManagedDependencies().size());
                java.util.List<org.eclipse.aether.resolution.ArtifactResult> artifacts = new java.util.ArrayList<>();
                for (String name : new String[] {"processor", "transitive-helper"}) {
                    org.eclipse.aether.resolution.ArtifactResult result = new org.eclipse.aether.resolution.ArtifactResult(
                            new org.eclipse.aether.resolution.ArtifactRequest());
                    result.setArtifact(new org.eclipse.aether.artifact.DefaultArtifact("processors", name, "jar", "1")
                            .setFile(new java.io.File(name + ".jar")));
                    artifacts.add(result);
                }
                return new org.eclipse.aether.resolution.DependencyResult(request).setArtifactResults(artifacts);
            });
            java.util.List<String> options = BackendPackageMojo.resolvedCompilerOptions(project,
                    new Properties(), tests, resolver, session);
            assertEquals("processors.Main", options.get(options.indexOf("-processor") + 1));
            assertEquals(new java.io.File("processor.jar").getAbsolutePath() + java.io.File.pathSeparator
                    + new java.io.File("transitive-helper.jar").getAbsolutePath(), options.get(options.indexOf("-processorpath") + 1));
            assertFalse(options.contains("-classpath"), "processors must not become application dependencies");
        }
    }

    @Test void propertiesAndCommandLineRelease() {
        MavenProject project = project();
        Properties cli = new Properties();
        assertNull(option(project, cli, "release", false));
        project.getProperties().setProperty("maven.compiler.release", "17");
        assertEquals("17", option(project, cli, "release", false));
        cli.setProperty("maven.compiler.release", "25");
        assertEquals("25", option(project, cli, "release", false));
    }

    @Test void compilerConfigurationAndExecutionOverrides() {
        MavenProject project = project();
        Properties cli = new Properties();
        project.getProperties().setProperty("maven.compiler.release", "8");
        Plugin plugin = new Plugin();
        plugin.setGroupId("org.apache.maven.plugins");
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setConfiguration(configuration("release", "17"));
        PluginExecution main = new PluginExecution();
        main.setId("default-compile");
        main.setConfiguration(configuration("release", "${backend.java}"));
        plugin.addExecution(main);
        PluginExecution test = new PluginExecution();
        test.setId("default-testCompile");
        test.setConfiguration(configuration("testRelease", "25"));
        plugin.addExecution(test);
        project.getBuild().addPlugin(plugin);
        cli.setProperty("backend.java", "25");
        assertEquals("25", option(project, cli, "release", false));
        assertEquals("25", option(project, cli, "testRelease", true));
        assertEquals("17", option(project, cli, "release", true));
    }

    @Test void legacySourceAndTargetAndSeparateTestLevel() {
        MavenProject project = project();
        Properties cli = new Properties();
        project.getProperties().setProperty("maven.compiler.source", "17");
        project.getProperties().setProperty("maven.compiler.target", "17");
        project.getProperties().setProperty("maven.compiler.testRelease", "25");
        assertEquals("17", option(project, cli, "source", false));
        assertEquals("17", option(project, cli, "target", false));
        assertEquals("25", option(project, cli, "testRelease", true));
    }

    @Test void honorsCustomCompilerExecutionsAndSkipsDisabledDefaults() {
        MavenProject project = project();
        Plugin compiler = new Plugin();
        compiler.setGroupId("org.apache.maven.plugins");
        compiler.setArtifactId("maven-compiler-plugin");
        compiler.setConfiguration(configuration("release", "8"));
        compiler.addExecution(execution("default-compile", "none", "compile", "release", "8"));
        compiler.addExecution(execution("modern-main", "compile", "compile", "release", "25"));
        compiler.addExecution(execution("default-testCompile", "none", "testCompile", "testRelease", "8"));
        compiler.addExecution(execution("modern-tests", "test-compile", "testCompile", "testRelease", "17"));
        // An execution outside the lifecycle interval must not override the active one.
        compiler.addExecution(execution("later-main", "package", "compile", "release", "8"));
        project.getBuild().addPlugin(compiler);
        assertEquals("25", option(project, new Properties(), "release", false));
        assertEquals("17", option(project, new Properties(), "testRelease", true));
    }

    @Test void customExecutionUsesGoalDefaultPhaseAndMergesPluginSettings() {
        MavenProject project = project();
        Plugin compiler = new Plugin();
        compiler.setGroupId("org.apache.maven.plugins");
        compiler.setArtifactId("maven-compiler-plugin");
        compiler.setConfiguration(configuration("source", "17"));
        compiler.addExecution(execution("custom-main", null, "compile", "target", "25"));
        // Declaring a lifecycle default later in the model does not make it run last.
        compiler.addExecution(execution("default-compile", null, "compile", "target", "8"));
        project.getBuild().addPlugin(compiler);
        assertEquals("17", option(project, new Properties(), "source", false));
        assertEquals("25", option(project, new Properties(), "target", false));
    }

    @Test void impliedLifecycleDefaultsRunAfterEarlyCustomExecutions() {
        for (boolean tests : new boolean[] {false, true}) {
            MavenProject project = project();
            Plugin compiler = new Plugin();
            compiler.setGroupId("org.apache.maven.plugins");
            compiler.setArtifactId("maven-compiler-plugin");
            String goal = tests ? "testCompile" : "compile";
            String option = tests ? "testRelease" : "release";
            compiler.setConfiguration(configuration(option, "17"));
            compiler.addExecution(execution("early", "generate-sources", goal, option, "8"));
            project.getBuild().addPlugin(compiler);
            assertEquals("17", option(project, new Properties(), option, tests));

            PluginExecution defaultExecution = execution("default-" + goal, "none", goal, option, "25");
            compiler.addExecution(defaultExecution);
            assertEquals("8", option(project, new Properties(), option, tests));
            defaultExecution.setPhase("validate");
            assertEquals("8", option(project, new Properties(), option, tests));
            defaultExecution.setPhase("process-sources");
            assertEquals("25", option(project, new Properties(), option, tests));
        }
    }

    @Test void readsLifecycleCompilerSettingsFromPluginManagement() {
        MavenProject project = project();
        Plugin compiler = new Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        Xpp3Dom config = configuration("release", "${backend.java}");
        config.addChild(configuration("encoding", "UTF-16").getChild("encoding"));
        compiler.setConfiguration(config);
        compiler.addExecution(execution("default-testCompile", null, "testCompile", "testRelease", "25"));
        org.apache.maven.model.PluginManagement management = new org.apache.maven.model.PluginManagement();
        management.addPlugin(compiler);
        project.getBuild().setPluginManagement(management);
        project.getProperties().setProperty("backend.java", "17");
        assertEquals("17", option(project, new Properties(), "release", false));
        assertEquals("25", option(project, new Properties(), "testRelease", true));
        assertEquals("UTF-16", BackendPackageMojo.sourceEncodingOf(project));
        Properties cli = new Properties();
        cli.setProperty("backend.java", "25");
        assertEquals("25", option(project, cli, "release", false));
        Plugin active = new Plugin();
        active.setArtifactId("maven-compiler-plugin");
        active.setConfiguration(configuration("release", "8"));
        project.getBuild().addPlugin(active);
        project.getBuild().flushPluginMap();
        assertEquals("8", option(project, cli, "release", false));
    }

    @Test void previewAndProcessorSettingsFollowCompilerExecutions() {
        MavenProject project = project();
        project.getProperties().setProperty("maven.compiler.enablePreview", "true");
        assertEquals(java.util.Collections.singletonList("--enable-preview"),
                BackendPackageMojo.compilerOptions(project, new Properties(), false));
        Plugin compiler = new Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        Xpp3Dom config = configuration("enablePreview", "false");
        Xpp3Dom args = new Xpp3Dom("compilerArgs");
        args.addChild(configuration("arg", "-Asetting=value").getChild("arg"));
        config.addChild(args);
        compiler.setConfiguration(config);
        compiler.addExecution(execution("default-testCompile", null, "testCompile", "enablePreview", "true"));
        project.getBuild().addPlugin(compiler);
        project.getBuild().flushPluginMap();
        assertEquals(java.util.Collections.singletonList("-Asetting=value"),
                BackendPackageMojo.compilerOptions(project, new Properties(), false));
        assertEquals(java.util.Arrays.asList("-Asetting=value", "--enable-preview"),
                BackendPackageMojo.compilerOptions(project, new Properties(), true));
        config.addChild(configuration("proc", "none").getChild("proc"));
        assertTrue(BackendPackageMojo.compilerOptions(project, new Properties(), false).contains("-proc:none"));
    }

    private static PluginExecution execution(String id, String phase, String goal, String name, String value) {
        PluginExecution execution = new PluginExecution();
        execution.setId(id); execution.setPhase(phase); execution.addGoal(goal);
        execution.setConfiguration(configuration(name, value));
        return execution;
    }

    private static MavenProject project() {
        Model model = new Model();
        model.setBuild(new Build());
        return new MavenProject(model);
    }
    private static Xpp3Dom configuration(String name, String value) {
        Xpp3Dom result = new Xpp3Dom("configuration");
        Xpp3Dom child = new Xpp3Dom(name);
        child.setValue(value); result.addChild(child); return result;
    }
    private static String option(MavenProject project, Properties cli, String name, boolean tests) {
        return BackendPackageMojo.compilerOption(project, cli, name, tests);
    }
}
