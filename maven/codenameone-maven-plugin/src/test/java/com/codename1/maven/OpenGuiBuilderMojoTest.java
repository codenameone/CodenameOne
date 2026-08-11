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

import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import static org.junit.Assert.*;

public class OpenGuiBuilderMojoTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void bindingIncludesProjectWideGuiAndCssLocations() throws Exception {
        File common = tmp.newFolder("common");
        File input = tmp.newFile("guibuilder.input");
        File gui = new File(common, "src/main/guibuilder");
        File source = new File(common, "src/main/java");
        File css = new File(common, "src/main/css/theme.css");
        new OpenGuiBuilderMojo().writeBinding(input, common, gui, source, css);
        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding.contains("projectDir=" + common.getAbsolutePath()));
        assertTrue(binding.contains("guiDir=" + gui.getAbsolutePath()));
        assertTrue(binding.contains("cssFile=" + css.getAbsolutePath()));
    }

    @Test
    public void forwardsGuiBuilderPropertiesButNotTheBindingOrLaunchFlag() {
        System.setProperty("guibuilder.mcp.port", "18349");
        System.setProperty("guibuilder.canvasMode", "desktop");
        System.setProperty("guibuilder.input", "/tmp/should-not-be-forwarded.input");
        System.setProperty("guibuilder.spawn", "false");
        try {
            List<String> args = new OpenGuiBuilderMojo().forwardedGuiBuilderProperties();
            assertTrue(args.contains("-Dguibuilder.mcp.port=18349"));
            assertTrue(args.contains("-Dguibuilder.canvasMode=desktop"));
            for (String arg : args) {
                assertFalse(arg.startsWith("-Dguibuilder.input="));
                assertFalse(arg.startsWith("-Dguibuilder.spawn="));
            }
        } finally {
            System.clearProperty("guibuilder.mcp.port");
            System.clearProperty("guibuilder.canvasMode");
            System.clearProperty("guibuilder.input");
            System.clearProperty("guibuilder.spawn");
        }
    }

    @Test
    public void desktopIdentityOpensThePackagesTheJavaseRuntimeNeeds() {
        List<String> args = new OpenGuiBuilderMojo().desktopIdentityArgs();
        assertTrue(args.contains("-Dsun.awt.application.name=Codename One GUI Builder"));
        // --add-exports is a Java 9 option. Passing it to an 8 JVM stops that JVM before it can
        // print anything, and the editor is spawned detached, so the user would see nothing at all.
        boolean modular = OpenGuiBuilderMojo.javaFeatureVersion() >= 9;
        boolean exported = args.contains("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED")
                && args.contains("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED");
        if (modular) {
            assertTrue("a Java 9+ fork needs the desktop packages opened", exported);
        } else {
            assertFalse("a Java 8 fork rejects --add-exports and never starts", exported);
        }
    }

    @Test
    public void detectsTheRunningJavaFeatureVersion() {
        assertTrue("the plugin itself runs on JDK 8 or newer",
                OpenGuiBuilderMojo.javaFeatureVersion() >= 8);
    }

    @Test
    public void launchesFromAggregatorOrCommonModule() throws Exception {
        File root = tmp.newFolder("app");
        File common = new File(root, "common");
        assertTrue(common.mkdirs());
        Files.write(new File(root, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "codenameone_settings.properties").toPath(), "codename1.packageName=com.example\n".getBytes(StandardCharsets.UTF_8));
        OpenGuiBuilderMojo mojo = new OpenGuiBuilderMojo();
        MavenProject project = new MavenProject();
        project.setFile(new File(root, "pom.xml"));
        project.addCompileSourceRoot(new File(root, "src/main/java").getAbsolutePath());
        mojo.project = project;
        assertTrue(mojo.isCN1ProjectDir());
    }
    /**
     * Two reactor executions, one guard. getPluginContext() is indexed by the current project and
     * isCN1ProjectDir() accepts both the aggregator and the app module, so running the goal from a
     * generated multi-module root gave each execution its own empty map and launched a second
     * editor over the same files.
     */
    @Test
    public void theLaunchGuardIsSharedAcrossTheReactor() {
        final java.util.Map<String, Object> reactor = new java.util.HashMap<String, Object>();
        OpenGuiBuilderMojo aggregator = new OpenGuiBuilderMojo() {
            @Override java.util.Map<String, Object> sharedReactorContext() { return reactor; }
        };
        OpenGuiBuilderMojo module = new OpenGuiBuilderMojo() {
            @Override java.util.Map<String, Object> sharedReactorContext() { return reactor; }
        };
        java.util.Map<String, Object> perProject = new java.util.HashMap<String, Object>();
        aggregator.setPluginContext(perProject);
        module.setPluginContext(new java.util.HashMap<String, Object>());

        assertSame("both executions must resolve to one map",
                aggregator.launchGuardContext(), module.launchGuardContext());
        aggregator.launchGuardContext().put("launched", Boolean.TRUE);
        assertEquals("the second execution must see the first one's mark",
                Boolean.TRUE, module.launchGuardContext().get("launched"));
        assertTrue("the per-project context is not what was written to", perProject.isEmpty());
    }

    /** Without a session -- a single module build -- the mojo's own context still serves. */
    @Test
    public void theGuardFallsBackToThePerProjectContext() {
        OpenGuiBuilderMojo mojo = new OpenGuiBuilderMojo() {
            @Override java.util.Map<String, Object> sharedReactorContext() { return null; }
        };
        java.util.Map<String, Object> perProject = new java.util.HashMap<String, Object>();
        mojo.setPluginContext(perProject);
        assertSame(perProject, mojo.launchGuardContext());
    }

}
