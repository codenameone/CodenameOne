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
import java.util.Arrays;
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

    /// What Maven RESOLVED, so the Settings tool does not have to infer it from
    /// POM text. It has no model: it cannot evaluate a profile activation,
    /// follow an inherited `<sourceDirectory>` or expand a property, and each of
    /// those has been a way for it to miss the main class and then offer an
    /// annotation-owned hint for editing.
    @Test
    public void bindingCarriesTheResolvedSourceRootsAndEncoding() throws Exception {
        File root = tmp.newFolder("resolved");
        File common = new File(root, "common");
        assertTrue(new File(common, "src/main/java").mkdirs());
        assertTrue(new File(common, "appsrc").mkdirs());
        File input = tmp.newFile("resolved.input");

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(common);
        mojo.project.addCompileSourceRoot(new File(common, "appsrc").getAbsolutePath());
        mojo.project.getProperties().setProperty("project.build.sourceEncoding", "Shift_JIS");

        mojo.writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        // One line per root, so a path containing a separator survives.
        assertTrue(binding, binding.contains(
                "sourceRoot=" + new File(common, "src/main/java").getAbsolutePath() + "\n"));
        assertTrue(binding, binding.contains(
                "sourceRoot=" + new File(common, "appsrc").getAbsolutePath() + "\n"));
        assertTrue(binding, binding.contains("sourceEncoding=Shift_JIS"));
    }

    /// Maven does not copy a plugin parameter into the project's properties, so
    /// a POM that sets `<encoding>` inside maven-compiler-plugin -- in a profile,
    /// say -- published nothing and left the tool guessing.
    @Test
    public void theCompilerPluginsEncodingIsPublishedToo() throws Exception {
        File root = tmp.newFolder("plugin-encoding");
        File common = new File(root, "common");
        assertTrue(new File(common, "src/main/java").mkdirs());
        File input = tmp.newFile("plugin-encoding.input");

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(common);
        org.apache.maven.model.Plugin compiler = new org.apache.maven.model.Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        compiler.setConfiguration(org.codehaus.plexus.util.xml.Xpp3DomBuilder.build(
                new java.io.StringReader(
                        "<configuration><encoding>Shift_JIS</encoding></configuration>")));
        mojo.project.getBuild().addPlugin(compiler);

        mojo.writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding, binding.contains("sourceEncoding=Shift_JIS"));
    }

    /// The compiler plugin's `<encoding>` parameter DEFAULTS to
    /// `${project.build.sourceEncoding}`, so an explicit one overrides the
    /// property. Reading the property first gave a module that sets the plugin
    /// parameter its parent's value instead of its own.
    @Test
    public void anExplicitPluginEncodingBeatsTheProperty() throws Exception {
        File root = tmp.newFolder("precedence");
        File common = new File(root, "common");
        assertTrue(new File(common, "src/main/java").mkdirs());
        File input = tmp.newFile("precedence.input");

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(common);
        mojo.project.getProperties().setProperty("project.build.sourceEncoding", "UTF-8");
        org.apache.maven.model.Plugin compiler = new org.apache.maven.model.Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        compiler.setConfiguration(org.codehaus.plexus.util.xml.Xpp3DomBuilder.build(
                new java.io.StringReader(
                        "<configuration><encoding>Shift_JIS</encoding></configuration>")));
        mojo.project.getBuild().addPlugin(compiler);

        mojo.writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding, binding.contains("sourceEncoding=Shift_JIS"));
        assertFalse(binding, binding.contains("sourceEncoding=UTF-8"));
    }

    /// `add-source` runs at generate-sources and adds its directories to the
    /// project, so a mojo bound after it sees them. A goal invoked DIRECTLY --
    /// `mvn cn1:settings` -- runs no lifecycle at all, so they are missing and a
    /// main class living only in an added root looked absent.
    @Test
    public void buildHelperSourcesAreResolvedWithoutTheLifecycle() throws Exception {
        File root = tmp.newFolder("helper-roots");
        File common = new File(root, "common");
        assertTrue(new File(common, "src/main/java").mkdirs());
        assertTrue(new File(common, "gen/main").mkdirs());
        assertTrue(new File(common, "gen/fixtures").mkdirs());
        File input = tmp.newFile("helper-roots.input");

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(common);
        org.apache.maven.model.Plugin helper = new org.apache.maven.model.Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.addExecution(sourceExecution("add-source", "gen/main"));
        helper.addExecution(sourceExecution("add-test-source", "gen/fixtures"));
        mojo.project.getBuild().addPlugin(helper);

        mojo.writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding, binding.contains(
                "sourceRoot=" + new File(common, "gen/main").getAbsolutePath() + "\n"));
        // The test goal's directories are not main sources.
        assertFalse(binding, binding.contains(new File(common, "gen/fixtures").getAbsolutePath()));
    }

    private static org.apache.maven.model.PluginExecution sourceExecution(String goal, String dir)
            throws Exception {
        org.apache.maven.model.PluginExecution execution =
                new org.apache.maven.model.PluginExecution();
        execution.setGoals(Arrays.asList(goal));
        execution.setConfiguration(org.codehaus.plexus.util.xml.Xpp3DomBuilder.build(
                new java.io.StringReader("<configuration><sources><source>" + dir
                        + "</source></sources></configuration>")));
        return execution;
    }

    /// Maven merges plugin-level configuration into every execution, so a
    /// `<sources>` written once outside them applies to the `add-source`
    /// execution too -- and an execution's own value overrides a plugin-level
    /// `<encoding>`. Reading only one of the two got both halves wrong in turn.
    @Test
    public void configurationAtEitherLevelIsApplied() throws Exception {
        File root = tmp.newFolder("levels");
        File common = new File(root, "common");
        assertTrue(new File(common, "src/main/java").mkdirs());
        assertTrue(new File(common, "gen/shared").mkdirs());
        File input = tmp.newFile("levels.input");

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(common);

        // build-helper: the root is named at PLUGIN level, the goal in the
        // execution.
        org.apache.maven.model.Plugin helper = new org.apache.maven.model.Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(org.codehaus.plexus.util.xml.Xpp3DomBuilder.build(
                new java.io.StringReader(
                        "<configuration><sources><source>gen/shared</source></sources>"
                                + "</configuration>")));
        org.apache.maven.model.PluginExecution add =
                new org.apache.maven.model.PluginExecution();
        add.setGoals(Arrays.asList("add-source"));
        helper.addExecution(add);
        mojo.project.getBuild().addPlugin(helper);

        // compiler: the EXECUTION overrides the plugin-level encoding, and it
        // names the goal only through Maven's own execution id.
        org.apache.maven.model.Plugin compiler = new org.apache.maven.model.Plugin();
        compiler.setArtifactId("maven-compiler-plugin");
        compiler.setConfiguration(org.codehaus.plexus.util.xml.Xpp3DomBuilder.build(
                new java.io.StringReader(
                        "<configuration><encoding>UTF-8</encoding></configuration>")));
        org.apache.maven.model.PluginExecution defaultCompile =
                new org.apache.maven.model.PluginExecution();
        defaultCompile.setId("default-compile");
        defaultCompile.setConfiguration(org.codehaus.plexus.util.xml.Xpp3DomBuilder.build(
                new java.io.StringReader(
                        "<configuration><encoding>Shift_JIS</encoding></configuration>")));
        compiler.addExecution(defaultCompile);
        mojo.project.getBuild().addPlugin(compiler);

        mojo.writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding, binding.contains(
                "sourceRoot=" + new File(common, "gen/shared").getAbsolutePath() + "\n"));
        assertTrue(binding, binding.contains("sourceEncoding=Shift_JIS"));
        assertFalse(binding, binding.contains("sourceEncoding=UTF-8"));
    }

    /// Within one execution the levels do NOT accumulate: Maven merges by
    /// element, and a repeated list is replaced rather than appended unless the
    /// POM says otherwise. Taking both added a directory the build does not
    /// compile -- a phantom root for everything downstream to scan.
    @Test
    public void anExecutionsSourcesReplaceThePluginLevelOnes() throws Exception {
        File root = tmp.newFolder("override");
        File common = new File(root, "common");
        assertTrue(new File(common, "src/main/java").mkdirs());
        assertTrue(new File(common, "gen/plugin-level").mkdirs());
        assertTrue(new File(common, "gen/execution").mkdirs());
        File input = tmp.newFile("override.input");

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(common);
        org.apache.maven.model.Plugin helper = new org.apache.maven.model.Plugin();
        helper.setArtifactId("build-helper-maven-plugin");
        helper.setConfiguration(org.codehaus.plexus.util.xml.Xpp3DomBuilder.build(
                new java.io.StringReader(
                        "<configuration><sources><source>gen/plugin-level</source></sources>"
                                + "</configuration>")));
        helper.addExecution(sourceExecution("add-source", "gen/execution"));
        mojo.project.getBuild().addPlugin(helper);

        mojo.writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding, binding.contains(
                "sourceRoot=" + new File(common, "gen/execution").getAbsolutePath() + "\n"));
        assertFalse(binding,
                binding.contains(new File(common, "gen/plugin-level").getAbsolutePath()));
    }

    /// `cn1:settings` run from a platform module resolves the sibling common
    /// directory, which the reactor need not contain. Publishing the platform
    /// module's roots as common's sent Settings an authoritative-looking answer
    /// about the wrong module, so it never read the real POM and missed the
    /// annotated main source.
    @Test
    public void aModuleWeDoNotHaveIsNotDescribed() throws Exception {
        File root = tmp.newFolder("platform");
        File common = new File(root, "common");
        File javase = new File(root, "javase");
        assertTrue(new File(common, "src/main/java").mkdirs());
        assertTrue(new File(javase, "src/main/java").mkdirs());
        File input = tmp.newFile("platform.input");

        // The project being built is javase; the directory being edited is
        // common, and no reactor module describes it.
        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(javase);
        mojo.project.getProperties().setProperty("project.build.sourceEncoding", "Shift_JIS");

        mojo.writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding, binding.contains("projectDir=" + common.getAbsolutePath()));
        assertFalse(binding, binding.contains("sourceRoot="));
        assertFalse(binding, binding.contains("sourceEncoding="));
        assertFalse(binding, binding.contains(javase.getAbsolutePath() + "/src"));
    }

    /// A project that resolves neither says neither, and the tool falls back to
    /// reading the POM itself rather than being handed an empty answer.
    @Test
    public void bindingOmitsWhatItCannotResolve() throws Exception {
        File root = tmp.newFolder("unresolved");
        File common = new File(root, "common");
        assertTrue(common.mkdirs());
        File input = tmp.newFile("unresolved.input");

        new OpenSettingsMojo().writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertFalse(binding, binding.contains("sourceRoot="));
        assertFalse(binding, binding.contains("sourceEncoding="));
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
        project.setBuild(new org.apache.maven.model.Build());
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
