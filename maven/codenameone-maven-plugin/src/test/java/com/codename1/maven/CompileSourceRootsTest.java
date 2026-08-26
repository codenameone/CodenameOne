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
import java.util.Properties;

import static org.junit.Assert.assertEquals;
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
        assertTrue(roots.toString(), contains(roots, basedir, "src/extra/kotlin"));
        // NOT the conventional Kotlin root: this project says where its Kotlin
        // sources are, and a configured <sourceDirs> replaces the default. See
        // theConventionalKotlinRootYieldsToAConfiguredOne.
        assertFalse(roots.toString(), contains(roots, basedir, "src/main/kotlin"));
        // NOT the plugin-level list: the compile execution supplies its own, and
        // Maven merges by element rather than appending. See
        // aKotlinExecutionsSourceDirsReplaceThePluginLevelOnes.
        assertFalse(roots.toString(), contains(roots, basedir, "src/shared/kotlin"));

        // NOT the test execution's: a same-named test fixture would then make a
        // deleted production class look like it still has a source.
        assertFalse(roots.toString(), contains(roots, basedir, "src/test/kotlin"));
    }

    /**
     * A module with no Kotlin plugin does not compile `src/main/kotlin`, however
     * many .kt files are sitting in it.
     *
     * <p>That is the shape a project is left in when Kotlin support is removed
     * and the tree is not deleted. Counting the directory as a root made a stale
     * class in `target/classes` look LIVE, because its old source was still
     * there -- and a build hint annotation on that class then failed the
     * placement check on every incremental build, a hard error nothing in the
     * project could clear except deleting files.</p>
     */
    @Test
    public void theConventionalKotlinRootNeedsAKotlinPlugin() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/main/kotlin"));
    }

    /**
     * ...and neither does one whose Kotlin plugin only compiles the TEST tree.
     * That execution compiles `src/test/kotlin`, which this list must not
     * contain either.
     */
    @Test
    public void aTestOnlyKotlinExecutionDoesNotClaimTheMainRoot() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.addExecution(execution("test-compile", "<jvmTarget>17</jvmTarget>"));
        project.getBuild().addPlugin(kotlin);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/main/kotlin"));
    }

    /**
     * `<extensions>true</extensions>` compiles Kotlin with no execution written.
     *
     * <p>It is the documented way to let the plugin contribute its own
     * lifecycle, and then there is nothing in `<executions>` to find. Reading
     * only the executions called such a module Kotlin-less, so an existing
     * `src/main/kotlin` was left out and a Kotlin main class living there could
     * not be found -- which is what makes the migration give up and Settings
     * offer an annotation-owned hint for editing.</p>
     */
    @Test
    public void anExtensionsEnabledKotlinPluginCompilesTheConventionalRoot() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setExtensions(true);
        project.getBuild().addPlugin(kotlin);

        assertTrue(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/main/kotlin"));
    }

    /** ...and a configured `<sourceDirs>` still replaces the convention. */
    @Test
    public void anExtensionsEnabledPluginStillYieldsToConfiguredSourceDirs() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setExtensions(true);
        kotlin.setConfiguration(config("<sourceDirs><d>src/app/kotlin</d></sourceDirs>"));
        project.getBuild().addPlugin(kotlin);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/main/kotlin"));
    }

    /**
     * `<phase>none</phase>` switches an inherited execution off.
     *
     * <p>It is the conventional way to disable an execution a parent declares
     * while leaving its goal in place, so the goal alone does not mean the build
     * runs it. Counting a disabled one made a stale class in `target/classes`
     * look current because its source is still on disk, and an annotation on
     * that class then failed the placement check on every incremental build.</p>
     */
    @Test
    public void aDisabledKotlinExecutionDoesNotClaimTheRoot() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        PluginExecution off = execution("compile", "<jvmTarget>17</jvmTarget>");
        off.setId("default-compile");
        off.setPhase("none");
        kotlin.addExecution(off);
        project.getBuild().addPlugin(kotlin);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/main/kotlin"));
    }

    /**
     * ...and a disabled execution contributes no configuration either, without
     * hiding the plugin-level configuration that still applies to the goal.
     */
    @Test
    public void aDisabledExecutionDoesNotSupplyOrHideConfiguration() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources><a>gen/plugin-level</a></sources>"));
        PluginExecution off = execution("add-source",
                "<sources><b>gen/disabled</b></sources>");
        off.setId("inherited");
        off.setPhase("none");
        helper.addExecution(off);
        // A live execution beside it, so the plugin-level configuration is
        // genuinely in effect and the assertion below says something: with only
        // the disabled one, add-source never runs and NOTHING is contributed.
        helper.addExecution(execution("add-source", "<skip>false</skip>"));
        project.getBuild().addPlugin(helper);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project);
        assertFalse(roots.toString(), contains(roots, basedir, "gen/disabled"));
        assertTrue(roots.toString(), contains(roots, basedir, "gen/plugin-level"));
    }

    /**
     * Plugin-level `<sources>` with no execution is dormant configuration.
     *
     * <p>build-helper's `add-source` runs only where an execution says so, so a
     * plugin declared with sources and no execution adds nothing. Treating that
     * as a compiled root made a stale class in `target/classes` look live
     * because its source sits there -- failing the placement check on every
     * incremental build over a directory Maven never reads.</p>
     */
    @Test
    public void pluginLevelSourcesNeedABoundGoal() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources><a>gen/dormant</a></sources>"));
        project.getBuild().addPlugin(helper);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir, "gen/dormant"));
    }

    /** ...and the same for a Kotlin plugin that binds nothing. */
    @Test
    public void pluginLevelSourceDirsNeedABoundGoal() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setConfiguration(config("<sourceDirs><d>src/dormant/kotlin</d></sourceDirs>"));
        project.getBuild().addPlugin(kotlin);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/dormant/kotlin"));
    }

    /**
     * ...but `<extensions>true</extensions>` DOES bind it, so the plugin-level
     * list is in effect with no execution written.
     */
    @Test
    public void anExtensionsEnabledPluginsSourceDirsAreInEffect() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setExtensions(true);
        kotlin.setConfiguration(config("<sourceDirs><d>src/app/kotlin</d></sourceDirs>"));
        project.getBuild().addPlugin(kotlin);

        assertTrue(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir, "src/app/kotlin"));
    }

    /**
     * An extensions-enabled plugin whose `default-compile` is disabled compiles
     * nothing.
     *
     * <p>The lifecycle binding is what `<extensions>true</extensions>` buys, and
     * a POM switches that off the way it switches off any inherited execution.
     * Claiming the root anyway kept a stale annotated class alive over a tree
     * Kotlin compilation was explicitly turned off for.</p>
     */
    @Test
    public void anExtensionsEnabledPluginHonoursADisabledCompile() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setExtensions(true);
        PluginExecution off = new PluginExecution();
        off.setId("default-compile");
        off.setPhase("none");
        kotlin.addExecution(off);
        project.getBuild().addPlugin(kotlin);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/main/kotlin"));
    }

    /**
     * A disabled extension lifecycle takes the plugin-level `<sourceDirs>` with
     * it.
     *
     * <p>`<extensions>true</extensions>` is what binds compile with no execution
     * written, so switching that binding off with
     * `<id>default-compile</id><phase>none</phase>` leaves nothing bound -- not
     * the conventional root, and not the configured list either. Returning the
     * plugin-level configuration anyway put a directory Maven does not compile
     * into the roots, which is where a stale annotated class keeps a source.</p>
     */
    @Test
    public void aDisabledExtensionLifecycleDropsItsConfiguredRoots() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setExtensions(true);
        kotlin.setConfiguration(config("<sourceDirs><d>src/app/kotlin</d></sourceDirs>"));
        PluginExecution off = new PluginExecution();
        off.setId("default-compile");
        off.setPhase("none");
        kotlin.addExecution(off);
        project.getBuild().addPlugin(kotlin);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir, "src/app/kotlin"));
    }

    /**
     * Disabling a DIFFERENTLY named compile execution does not cancel the
     * extension lifecycle.
     *
     * <p>The execution `<extensions>true</extensions>` contributes is
     * `default-compile`, so switching off a custom one that happens to bind
     * `compile` switches off only that execution. Reading any disabled compile
     * execution as cancellation dropped the Kotlin roots of a module that still
     * compiles Kotlin -- which classifies a LIVE class as stale, the opposite
     * mistake and the one that loses a real annotation.</p>
     */
    @Test
    public void aDisabledCustomExecutionLeavesTheExtensionLifecycleAlone() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.setExtensions(true);
        PluginExecution off = execution("compile", "<jvmTarget>17</jvmTarget>");
        off.setId("extra-compile");
        off.setPhase("none");
        kotlin.addExecution(off);
        project.getBuild().addPlugin(kotlin);

        assertTrue(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/main/kotlin"));
    }

    /**
     * `default-add-source` is not a binding.
     *
     * <p>`default-<goal>` is Maven's name for a LIFECYCLE-injected execution,
     * and build-helper never gets one -- it is not part of any packaging
     * lifecycle. So an execution with that id and no `<goal>` runs nothing, and
     * counting it pulled in a directory Maven does not compile, where a stale
     * annotated class keeps a source and fails the placement check.</p>
     */
    @Test
    public void anIdAloneDoesNotBindAGoalTheLifecycleNeverInjects() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        PluginExecution named = new PluginExecution();
        named.setId("default-add-source");
        named.setConfiguration(config("<sources><a>gen/not-really-bound</a></sources>"));
        helper.addExecution(named);
        project.getBuild().addPlugin(helper);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "gen/not-really-bound"));
    }

    /**
     * ...and neither for a Kotlin plugin that never asked for the lifecycle.
     * With `<extensions>true</extensions>` the same execution IS the binding.
     */
    @Test
    public void theDefaultCompileIdNeedsTheExtensionLifecycle() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject plain = projectAt(basedir);
        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        PluginExecution byId = new PluginExecution();
        byId.setId("default-compile");
        kotlin.addExecution(byId);
        plain.getBuild().addPlugin(kotlin);
        assertFalse(AbstractCN1Mojo.compileSourceRoots(plain).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(plain), basedir, "src/main/kotlin"));

        MavenProject withExtensions = projectAt(basedir);
        Plugin extended = new Plugin();
        extended.setArtifactId("kotlin-maven-plugin");
        extended.setExtensions(true);
        PluginExecution sameId = new PluginExecution();
        sameId.setId("default-compile");
        extended.addExecution(sameId);
        withExtensions.getBuild().addPlugin(extended);
        assertTrue(AbstractCN1Mojo.compileSourceRoots(withExtensions).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(withExtensions), basedir,
                        "src/main/kotlin"));
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
        // Bound, because plugin-level configuration is dormant without an
        // execution -- Maven never runs the goal that would read it.
        kotlin.addExecution(execution("compile", "<jvmTarget>17</jvmTarget>"));
        project.getBuild().addPlugin(kotlin);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project);
        assertTrue(roots.toString(), contains(roots, basedir, "appsrc"));
        assertTrue(roots.toString(), contains(roots, basedir, "out/generated-sources"));
        // What it cannot resolve it still leaves alone rather than guessing.
        assertFalse(roots.toString(), roots.toString().contains("nobody.knows"));
    }

    /**
     * A root written as an ordinary property -- `${generated.sources}` defined
     * in the POM, or handed in with `-D` -- is one Maven compiles from. Only
     * the project's own expressions used to be resolved, so such a root was
     * discarded, and on a direct `cn1:settings` or `cn1:migrate-build-hints`
     * invocation no lifecycle has added it back: a main class living there was
     * undiscoverable, and Settings would then offer its annotation-owned hints
     * as properties to set a second time.
     */
    @Test
    public void anOrdinaryPropertyInAConfiguredRootIsResolved() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);
        project.getProperties().setProperty("generated.sources", "gen/from-pom");
        // A property written in terms of another one resolves too.
        project.getProperties().setProperty("shared.root", "${generated.sources}/nested");

        Properties user = new Properties();
        user.setProperty("extra.sources", "gen/from-command-line");

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources>"
                + "<a>${generated.sources}</a>"
                + "<b>${extra.sources}</b>"
                + "<c>${shared.root}</c>"
                + "</sources>"));
        helper.addExecution(execution("add-source", "<skip>false</skip>"));
        project.getBuild().addPlugin(helper);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project, user);
        assertTrue(roots.toString(), contains(roots, basedir, "gen/from-pom"));
        assertTrue(roots.toString(), contains(roots, basedir, "gen/from-command-line"));
        assertTrue(roots.toString(), contains(roots, basedir, "gen/from-pom/nested"));
    }

    /**
     * `-D` wins over the POM's own value, the way it does everywhere else, and
     * a name nothing defines is still dropped rather than guessed at.
     */
    @Test
    public void commandLinePropertiesOverrideThePomsAndUnknownOnesAreStillDropped()
            throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);
        project.getProperties().setProperty("generated.sources", "gen/from-pom");

        Properties user = new Properties();
        user.setProperty("generated.sources", "gen/overridden");

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources>"
                + "<a>${generated.sources}</a>"
                + "<b>${nobody.defines.this}/x</b>"
                + "</sources>"));
        helper.addExecution(execution("add-source", "<skip>false</skip>"));
        project.getBuild().addPlugin(helper);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project, user);
        assertTrue(roots.toString(), contains(roots, basedir, "gen/overridden"));
        assertFalse(roots.toString(), contains(roots, basedir, "gen/from-pom"));
        assertFalse(roots.toString(), roots.toString().contains("nobody.defines.this"));
    }

    /**
     * A `$` that opens nothing is an ordinary character in a path, not an
     * expression -- dropping such a root lost a real source directory.
     */
    @Test
    public void aDollarThatOpensNothingIsPartOfThePath() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources><a>gen/dollar$dir</a></sources>"));
        helper.addExecution(execution("add-source", "<skip>false</skip>"));
        project.getBuild().addPlugin(helper);

        assertTrue(contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                "gen/dollar$dir"));
    }

    /**
     * The compiler's `<encoding>${source.charset}</encoding>` is the charset
     * Maven really compiles with. Discarding it fell back to an inherited
     * `project.build.sourceEncoding` that is not the one in force, and the
     * migration then decoded a non-ASCII package or main-class name with the
     * wrong charset -- missing the annotated source altogether.
     */
    @Test
    public void anEncodingWrittenAsAPropertyIsResolved() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);
        project.getProperties().setProperty("project.build.sourceEncoding", "UTF-8");
        project.getProperties().setProperty("source.charset", "Shift_JIS");

        Plugin compiler = new Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        compiler.setConfiguration(config("<encoding>${source.charset}</encoding>"));
        project.getBuild().addPlugin(compiler);

        assertEquals("Shift_JIS", AbstractCN1Mojo.sourceEncodingOf(project));

        Properties user = new Properties();
        user.setProperty("source.charset", "ISO-8859-1");
        assertEquals("ISO-8859-1", AbstractCN1Mojo.sourceEncodingOf(project, user));
    }

    /**
     * ...and one nothing defines is still not an encoding, so the caller falls
     * back rather than compiling the name of a property as a charset.
     */
    @Test
    public void anEncodingExpressionNothingDefinesIsNotAnEncoding() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);
        project.getProperties().setProperty("project.build.sourceEncoding", "UTF-8");

        Plugin compiler = new Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        compiler.setConfiguration(config("<encoding>${nobody.defines.this}</encoding>"));
        project.getBuild().addPlugin(compiler);

        assertEquals("UTF-8", AbstractCN1Mojo.sourceEncodingOf(project));
    }

    /**
     * `project.build.sourceEncoding` itself can be written as a property, and
     * can be supplied with `-D`.
     */
    @Test
    public void theSourceEncodingPropertyIsResolvedToo() throws Exception {
        File basedir = tmp.newFolder();
        MavenProject project = projectAt(basedir);
        project.getProperties().setProperty("project.build.sourceEncoding", "${my.charset}");
        project.getProperties().setProperty("my.charset", "Shift_JIS");

        assertEquals("Shift_JIS", AbstractCN1Mojo.sourceEncodingOf(project));

        Properties user = new Properties();
        user.setProperty("project.build.sourceEncoding", "ISO-8859-1");
        assertEquals("ISO-8859-1", AbstractCN1Mojo.sourceEncodingOf(project, user));
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

    /**
     * A configured `<sourceDirs>` REPLACES the default, so an existing
     * `src/main/kotlin` beside one is a tree the build does not compile. Adding
     * it made a stale class whose source still sits there look live, so the
     * orphan filter kept it and the placement error it carries fired on every
     * build.
     */
    @Test
    public void theConventionalKotlinRootYieldsToAConfiguredOne() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.addExecution(execution("compile", "<sourceDirs><d>src/app/kotlin</d></sourceDirs>"));
        project.getBuild().addPlugin(kotlin);

        List<String> roots = AbstractCN1Mojo.compileSourceRoots(project);
        assertTrue(roots.toString(), contains(roots, basedir, "src/app/kotlin"));
        assertFalse(roots.toString(), contains(roots, basedir, "src/main/kotlin"));
    }

    /** With nothing configured, the convention is the best answer there is. */
    @Test
    public void theConventionalKotlinRootCountsWhenNothingReplacesIt() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/main/kotlin").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin kotlin = new Plugin();
        kotlin.setArtifactId("kotlin-maven-plugin");
        kotlin.addExecution(execution("compile", "<jvmTarget>17</jvmTarget>"));
        project.getBuild().addPlugin(kotlin);

        assertTrue(contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                "src/main/kotlin"));
    }

    /**
     * `combine.self="override"` discards the inherited configuration wholesale,
     * so an execution that says it and omits the element has none -- reporting
     * the plugin-level value there named a setting the build does not use.
     */
    @Test
    public void anOverridingExecutionInheritsNothing() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/plugin-level").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources><a>src/plugin-level</a></sources>"));
        PluginExecution execution = new PluginExecution();
        execution.setGoals(Arrays.asList("add-source"));
        execution.setConfiguration(Xpp3DomBuilder.build(new StringReader(
                "<configuration combine.self=\"override\"><skip>false</skip></configuration>")));
        helper.addExecution(execution);
        project.getBuild().addPlugin(helper);

        assertFalse(AbstractCN1Mojo.compileSourceRoots(project).toString(),
                contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                        "src/plugin-level"));
    }

    /** Without the attribute the plugin-level configuration still applies. */
    @Test
    public void anExecutionWithoutTheElementInheritsIt() throws Exception {
        File basedir = tmp.newFolder();
        new File(basedir, "src/plugin-level").mkdirs();
        MavenProject project = projectAt(basedir);

        Plugin helper = new Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(config("<sources><a>src/plugin-level</a></sources>"));
        PluginExecution execution = new PluginExecution();
        execution.setGoals(Arrays.asList("add-source"));
        execution.setConfiguration(Xpp3DomBuilder.build(new StringReader(
                "<configuration><skip>false</skip></configuration>")));
        helper.addExecution(execution);
        project.getBuild().addPlugin(helper);

        assertTrue(contains(AbstractCN1Mojo.compileSourceRoots(project), basedir,
                "src/plugin-level"));
    }
}
