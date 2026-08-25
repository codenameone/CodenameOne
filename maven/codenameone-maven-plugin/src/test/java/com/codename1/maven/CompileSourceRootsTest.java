/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Where a source could be compiled from.
 *
 * <p>This list is used to decide that a source is ABSENT -- an annotated class
 * with no source behind it is dropped as an orphan, taking its placement error
 * with it -- so a list that is merely incomplete must not be read as that, and
 * one that is too wide is just as wrong in the other direction.</p>
 */
public class CompileSourceRootsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** The Kotlin plugin compiles its own sourceDirs without adding them back. */
    @Test
    public void theKotlinPluginsConfiguredDirectoriesCount() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);
        project.addCompileSourceRoot(new File(basedir, "src/main/java").getAbsolutePath());

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setConfiguration(config("<sourceDirs><d>src/shared/kotlin</d></sourceDirs>"));
        kotlin.addExecution(execution("compile", "<sourceDirs><d>src/extra/kotlin</d></sourceDirs>"));
        kotlin.addExecution(
                execution("test-compile", "<sourceDirs><d>src/test/kotlin</d></sourceDirs>"));
        project.getBuild().addPlugin(kotlin);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project);

        // What Maven listed, the conventional Kotlin root, and the dirs the
        // plugin compiles.
        assertTrue(roots.toString(), contains(roots, basedir, "src/main/java"));
        assertTrue(roots.toString(), contains(roots, basedir, "src/main/kotlin"));
        assertTrue(roots.toString(), contains(roots, basedir, "src/extra/kotlin"));
        // NOT the plugin-level list: the compile execution supplies its own, and
        // Maven merges by element rather than appending. See
        // aKotlinExecutionsSourceDirsReplaceThePluginLevelOnes.
        assertFalse(roots.toString(), contains(roots, basedir, "src/shared/kotlin"));

        // NOT the test execution's: a same-named test fixture would then make a
        // deleted production class look like it still has a source.
        assertFalse(roots.toString(), contains(roots, basedir, "src/test/kotlin"));
    }

    /** A conventional root that does not exist is not invented. */
    @Test
    public void anAbsentKotlinRootIsNotAdded() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);
        assertFalse(contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                "src/main/kotlin"));
    }

    /** No project is "not told", which the callers read as inconclusive. */
    @Test
    public void noProjectIsNoAnswer() {
        org.junit.Assert.assertNull(AbstractCN1Mojo.compileSourceRoots(null));
    }

    private MavenProject projectAt(File basedir) {
        MavenProject project = new MavenProject();
        project.setBuild(new Build());
        project.setFile(new File(basedir, "pom.xml"));
        return project;
    }

    private static PluginExecution execution(String goal, String configuration) throws Exception {
        PluginExecution execution = new PluginExecution();
        execution.setGoals(Arrays.asList(goal));
        execution.setConfiguration(config(configuration));
        return execution;
    }

    private static Xpp3Dom config(String inner) throws Exception {
        return Xpp3DomBuilder.build(new StringReader("<configuration>" + inner
                + "</configuration>"));
    }

    private static boolean contains(List<String> roots, File basedir, String relative) {
        return roots != null
                && roots.contains(new File(basedir, relative).getAbsolutePath());
    }

    /**
     * Maven merges configuration by element, so a `compile` execution's
     * `<sourceDirs>` replaces the plugin-level list. Adding both put a
     * directory the build does not compile into the roots, where a source left
     * behind can make a stale annotated class look live.
     */
    @Test
    public void aKotlinExecutionsSourceDirsReplaceThePluginLevelOnes() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setConfiguration(config("<sourceDirs><d>src/plugin-level</d></sourceDirs>"));
        kotlin.addExecution(execution("compile", "<sourceDirs><d>src/execution</d></sourceDirs>"));
        project.getBuild().addPlugin(kotlin);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project);
        assertTrue(roots.toString(), contains(roots, basedir, "src/execution"));
        assertFalse(roots.toString(), contains(roots, basedir, "src/plugin-level"));
    }

    /**
     * `${project.basedir}/appsrc` is an ordinary way to write a root, and
     * dropping it outright is a main class the migration cannot find. Maven
     * usually interpolates these while building the model, so this is normally
     * a no-op -- but a value that arrives unexpanded must not be discarded.
     */
    @Test
    public void projectExpressionsInAConfiguredRootAreResolved() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);
        project.getBuild().setDirectory(new File(basedir, "out").getAbsolutePath());

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setConfiguration(config("<sourceDirs>"
                + "<a>${project.basedir}/appsrc</a>"
                + "<b>${project.build.directory}/generated-sources</b>"
                + "<c>${nobody.knows}/x</c>"
                + "</sourceDirs>"));
        project.getBuild().addPlugin(kotlin);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project);
        assertTrue(roots.toString(), contains(roots, basedir, "appsrc"));
        assertTrue(roots.toString(), contains(roots, basedir, "out/generated-sources"));
        // What it cannot resolve it still leaves alone rather than guessing.
        assertFalse(roots.toString(), roots.toString().contains("nobody.knows"));
    }

    /**
     * `combine.children="append"` is how a POM asks for both lists, and then
     * both are in effect. Treating the execution element as a replacement
     * regardless omitted the plugin-level root, so a main class living there
     * could not be found.
     */
    @Test
    public void anAppendedListKeepsBothLevels() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources><a>gen/plugin-level</a></sources>"));
        helper.addExecution(execution("add-source",
                "<sources combine.children=\"append\"><b>gen/execution</b></sources>"));
        project.getBuild().addPlugin(helper);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project);
        assertTrue(roots.toString(), contains(roots, basedir, "gen/execution"));
        assertTrue(roots.toString(), contains(roots, basedir, "gen/plugin-level"));
    }

    /** ...and without the attribute it is still a replacement. */
    @Test
    public void aPlainListStillReplaces() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources><a>gen/plugin-level</a></sources>"));
        helper.addExecution(execution("add-source", "<sources><b>gen/execution</b></sources>"));
        project.getBuild().addPlugin(helper);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project);
        assertTrue(roots.toString(), contains(roots, basedir, "gen/execution"));
        assertFalse(roots.toString(), contains(roots, basedir, "gen/plugin-level"));
    }
}
