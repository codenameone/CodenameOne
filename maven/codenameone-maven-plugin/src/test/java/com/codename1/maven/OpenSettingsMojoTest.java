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
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpenSettingsMojoTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void extractsPackagedIconForDesktopLaunchers() throws Exception {
        File jar = jarWithIcon("settings.jar");
        File runtimeDir = tmp.newFolder("runtime");

        File extracted = new OpenSettingsMojo().extractSettingsIcon(jar, runtimeDir);

        assertTrue(extracted.isFile());
        assertEquals("settings-icon.png", extracted.getName());
        assertEquals("fake-png", new String(Files.readAllBytes(extracted.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void desktopIdentityArgsUseSettingsName() throws Exception {
        File runtimeDir = tmp.newFolder("runtime");
        List<String> args = new OpenSettingsMojo().desktopIdentityArgs(jarWithIcon("settings.jar"), runtimeDir);

        assertTrue(args.contains("-Dapple.awt.application.name=Codename One Settings"));
        assertTrue(args.contains("-Dcom.apple.mrj.application.apple.menu.about.name=Codename One Settings"));
        assertTrue(args.contains("-Dsun.awt.application.name=Codename One Settings"));
        assertTrue(args.contains("-Dsun.awt.X11.XWMClass=CodenameOneSettings"));
        if (OpenSettingsMojo.isJava9OrNewer()) {
            assertTrue(args.contains("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"));
            assertTrue(args.contains("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED"));
        }
    }

    @Test
    public void namedJavaLauncherNeverCopiesTheWindowsJvmLauncher() throws Exception {
        File runtimeDir = tmp.newFolder("runtime");
        File launcher = new OpenSettingsMojo().namedJavaLauncher(runtimeDir);

        if (OpenSettingsMojo.isWindows()) {
            // A javaw.exe copied out of the JDK loses its DLL search anchor and
            // breaks font/native rendering (issue #5443) - the real launcher
            // must be used and nothing may be materialized in the runtime dir.
            assertEquals("javaw.exe", launcher.getName());
            assertFalse(new File(runtimeDir, "CodenameOneSettings.exe").exists());
        } else {
            assertEquals("Codename One Settings", launcher.getName());
            assertTrue(Files.isSymbolicLink(launcher.toPath()) || launcher.exists());
        }
    }

    @Test
    public void forwardsSettingsSystemPropertiesButNotLaunchInternals() {
        System.setProperty("settings.screenshot", "/tmp/out.png");
        System.setProperty("settings.screenshot.delay", "5000");
        System.setProperty("settings.input", "/tmp/should-not-forward.input");
        System.setProperty("settings.spawn", "false");
        try {
            List<String> args = new OpenSettingsMojo().forwardedSettingsProperties();
            assertTrue(args.contains("-Dsettings.screenshot=/tmp/out.png"));
            assertTrue(args.contains("-Dsettings.screenshot.delay=5000"));
            for (String arg : args) {
                assertFalse(arg.startsWith("-Dsettings.input="));
                assertFalse(arg.startsWith("-Dsettings.spawn="));
            }
        } finally {
            System.clearProperty("settings.screenshot");
            System.clearProperty("settings.screenshot.delay");
            System.clearProperty("settings.input");
            System.clearProperty("settings.spawn");
        }
    }

    @Test
    public void settingsCanLaunchFromProjectRootWithCommonModule() throws Exception {
        File root = tmp.newFolder("cn1app");
        File common = new File(root, "common");
        assertTrue(common.mkdirs());
        Files.write(new File(root, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "codenameone_settings.properties").toPath(),
                "codename1.packageName=com.example.app\n".getBytes(StandardCharsets.UTF_8));

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(root);

        assertTrue(mojo.isCN1ProjectDir());
        assertEquals(common.getCanonicalFile(), mojo.getCN1ProjectDir().getCanonicalFile());
        assertEquals(root.getCanonicalFile(), mojo.multimoduleRoot(common).getCanonicalFile());
    }

    @Test
    public void settingsCanLaunchFromCommonModule() throws Exception {
        File common = tmp.newFolder("common");
        Files.write(new File(common, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "codenameone_settings.properties").toPath(),
                "codename1.packageName=com.example.app\n".getBytes(StandardCharsets.UTF_8));

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(common);

        assertTrue(mojo.isCN1ProjectDir());
        assertEquals(common.getCanonicalFile(), mojo.getCN1ProjectDir().getCanonicalFile());
    }

    @Test
    public void bindingContainsProjectFilesAndMultimoduleRoot() throws Exception {
        File root = tmp.newFolder("project");
        File common = new File(root, "common");
        assertTrue(common.mkdirs());
        File input = tmp.newFile("settings.input");

        new OpenSettingsMojo().writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding.contains("projectDir=" + common.getAbsolutePath()));
        assertTrue(binding.contains("settings=" + new File(common, "codenameone_settings.properties").getAbsolutePath()));
        assertTrue(binding.contains("pom=" + new File(common, "pom.xml").getAbsolutePath()));
        assertTrue(binding.contains("multimoduleRoot=" + root.getAbsolutePath()));
    }

    @Test
    public void pluginVersionUsesCodenameOneVersionInsteadOfApplicationVersion() {
        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(new File("."));
        mojo.project.setVersion("1.0-SNAPSHOT");
        mojo.project.getProperties().setProperty("cn1.version", "7.0.258");

        assertEquals("7.0.258", mojo.pluginVersion());

        mojo.project.getProperties().setProperty("cn1.plugin.version", "7.0.259");
        assertEquals("7.0.259", mojo.pluginVersion());
    }

    @Test
    public void platformDetectionHandlesMacWindowsAndJavaVersions() {
        String os = System.getProperty("os.name");
        String java = System.getProperty("java.specification.version");
        try {
            System.setProperty("os.name", "Mac OS X");
            assertTrue(OpenSettingsMojo.isMacOs());
            assertFalse(OpenSettingsMojo.isWindows());
            System.setProperty("os.name", "Windows 11");
            assertTrue(OpenSettingsMojo.isWindows());
            System.setProperty("java.specification.version", "17");
            assertTrue(OpenSettingsMojo.isJava9OrNewer());
            System.setProperty("java.specification.version", "1.8");
            assertFalse(OpenSettingsMojo.isJava9OrNewer());
        } finally {
            restoreProperty("os.name", os);
            restoreProperty("java.specification.version", java);
        }
    }

    private File jarWithIcon(String name) throws Exception {
        File jar = tmp.newFile(name);
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry("icon.png"));
            out.write("fake-png".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

    private MavenProject projectAt(File basedir) {
        MavenProject project = new MavenProject();
        project.setFile(new File(basedir, "pom.xml"));
        project.addCompileSourceRoot(new File(basedir, "src/main/java").getAbsolutePath());
        return project;
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
