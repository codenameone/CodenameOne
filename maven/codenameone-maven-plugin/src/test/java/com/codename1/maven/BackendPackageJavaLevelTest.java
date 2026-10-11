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
