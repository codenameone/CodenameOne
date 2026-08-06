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
        assertTrue(args.contains("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED"));
        assertTrue(args.contains("-Dsun.awt.application.name=Codename One GUI Builder"));
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
}
