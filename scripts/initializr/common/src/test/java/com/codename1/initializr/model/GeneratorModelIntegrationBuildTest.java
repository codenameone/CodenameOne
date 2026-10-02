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
package com.codename1.initializr.model;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.util.Resources;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Integration-oriented test that generates real projects and attempts a Maven compile
 * using selected JDK homes.
 */
public class GeneratorModelIntegrationBuildTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        Path java8Or11 = findJava8Or11Home();
        Path java17 = findJavaHomeForMajor(17);

        // The Gradle half needs no build client (nothing is sent to the build server),
        // so it runs before the Maven half's early return.
        buildGeneratedGradleProjects(java17);

        Path buildClient = findBuildClientJar();
        if (buildClient == null) {
            // Every goal in the generated project's compile runs out of this jar, so
            // without it there is nothing to test. Skip rather than fail: it is installed
            // by setup-workspace.sh, not by this repository's build.
            System.out.println("[WARN] Skipping integration build checks. No "
                    + ".codenameone/CodeNameOneBuildClient.jar found under the user home.");
            return true;
        }

        buildGeneratedMavenLayouts(java17, buildClient);

        if (java8Or11 == null) {
            System.out.println("[WARN] Skipping Java 8/11 integration build check. No JDK 8 or 11 found.");
        } else {
            buildGeneratedProject(ProjectOptions.JavaVersion.JAVA_8, java8Or11, buildClient, "java8-or-11");
        }

        if (java17 == null) {
            System.out.println("[WARN] Skipping Java 17 integration build check. No JDK 17 found.");
        } else {
            buildGeneratedProject(ProjectOptions.JavaVersion.JAVA_17, java17, buildClient, "java17");
        }

        return true;
    }

    private void buildGeneratedProject(ProjectOptions.JavaVersion version, Path javaHome,
                                       Path buildClient, String suffix) throws Exception {
        String appName = "Integration" + suffix.replace("-", "") + "App";
        String packageName = "com.acme.initializr." + suffix.replace("-", "");

        ProjectOptions options = new ProjectOptions(
                ProjectOptions.ThemeMode.LIGHT,
                ProjectOptions.Accent.DEFAULT,
                true,
                true,
                ProjectOptions.PreviewLanguage.ENGLISH,
                version
        );

        byte[] zip = createProjectZip(options, appName, packageName);
        Path projectDir = Files.createTempDirectory("initializr-integration-" + suffix + "-");
        Path homeDir = Files.createTempDirectory("initializr-home-" + suffix + "-");
        ensureCodenameOneHome(homeDir, buildClient);
        unzipProject(zip, projectDir);

        int exitCode = runMavenCompile(projectDir, homeDir, javaHome);
        assertTrue(exitCode == 0, "Generated project should build with selected JDK. Version=" + version.label + " | exitCode=" + exitCode);

        // Localization bundles were requested -- they must end up baked into theme.res so
        // that Resources.getGlobalResources().getL10N("messages", lang) resolves at runtime.
        // This is the regression test for the NPE in MyAppName.init() reported when bundles
        // were generated under common/src/main/resources instead of common/src/main/l10n.
        assertLocalizationBakedIntoThemeRes(projectDir, version);
    }

    /// Compiles generated Gradle projects (App, App + backend, Backend only) with
    /// `./gradlew classes`, against the Maven repository named by the
    /// CN1_GRADLE_PLUGIN_REPO environment variable -- a directory holding the
    /// `com.codenameone` plugin (its marker and jar) and the framework, e.g. a
    /// checkout's `maven/.m2-repo` after `mvn install`. The generated settings name
    /// a released plugin version that only exists on repo.codenameone.com, so
    /// without that repository there is nothing to build against and the check is
    /// skipped, as the Maven half skips without a build client.
    /// CN1_GRADLE_PLUGIN_VERSION, when set, replaces the plugin version in the
    /// copy under test (the repository usually holds a SNAPSHOT).
    private void buildGeneratedGradleProjects(Path java17) throws Exception {
        String repo = System.getenv("CN1_GRADLE_PLUGIN_REPO");
        if (repo == null || repo.length() == 0) {
            System.out.println("[WARN] Skipping Gradle integration build checks. Set CN1_GRADLE_PLUGIN_REPO to a "
                    + "Maven repository holding the com.codenameone Gradle plugin.");
            return;
        }
        if (java17 == null) {
            System.out.println("[WARN] Skipping Gradle integration build checks. No JDK 17 found.");
            return;
        }
        Path repoDir = Paths.get(repo).toAbsolutePath();
        String version = System.getenv("CN1_GRADLE_PLUGIN_VERSION");
        // Every project type with the Java template, plus each template whose build
        // script adds something of its own (the Kotlin plugin, Tweet's cn1libs).
        Object[][] cases = new Object[][] {
                {Template.BAREBONES, ProjectOptions.ProjectType.APP},
                {Template.BAREBONES, ProjectOptions.ProjectType.APP_WITH_BACKEND},
                {Template.BAREBONES, ProjectOptions.ProjectType.BACKEND_ONLY},
                {Template.KOTLIN, ProjectOptions.ProjectType.APP},
                // cn1libs by coordinates, CodeRAD XML views and an annotation processor.
                {Template.TWEET, ProjectOptions.ProjectType.APP}
        };
        for (int i = 0; i < cases.length; i++) {
            Template template = (Template) cases[i][0];
            ProjectOptions.ProjectType type = (ProjectOptions.ProjectType) cases[i][1];
            if (!template.supportsGradle()) {
                // Listed so it is built as soon as the template is enabled for Gradle.
                System.out.println("[WARN] Skipping " + template + ": " + template.GRADLE_UNSUPPORTED_REASON);
                continue;
            }
            String suffix = "gradle" + i;
            ProjectOptions options = new ProjectOptions(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.DEFAULT,
                    true, true, ProjectOptions.PreviewLanguage.ENGLISH, ProjectOptions.JavaVersion.JAVA_17, null,
                    ProjectOptions.BuildTool.GRADLE, type);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            GeneratorModel.create(IDE.INTELLIJ, template, "IntegrationGradle" + i + "App",
                    "com.acme.initializr." + suffix, options).writeProjectZip(output);
            Path projectDir = Files.createTempDirectory("initializr-integration-" + suffix + "-");
            unzipProject(output.toByteArray(), projectDir);
            injectPluginRepository(projectDir.resolve("settings.gradle.kts"), repoDir, version);

            int exit = runGradleClasses(projectDir, repoDir, java17);
            assertTrue(exit == 0, "Generated Gradle project should compile. Template=" + template + " Type="
                    + type.label + " | exitCode=" + exit);
        }
    }

    /// Builds the Maven layouts a download gets once the plugin is 7.0.275 or newer --
    /// the minimal app, which builds every platform from common, and the backend-only
    /// project -- against the plugin named by CN1_MAVEN_PLUGIN_VERSION (typically a
    /// locally installed SNAPSHOT), resolved from CN1_MAVEN_REPO_LOCAL when set. The
    /// generated poms name a released version that may not exist yet, so without the
    /// variable the check is skipped, as the Gradle half is.
    private void buildGeneratedMavenLayouts(Path java17, Path buildClient) throws Exception {
        String version = System.getenv("CN1_MAVEN_PLUGIN_VERSION");
        if (version == null || version.length() == 0) {
            System.out.println("[WARN] Skipping Maven layout build checks. Set CN1_MAVEN_PLUGIN_VERSION to a "
                    + "codenameone-maven-plugin version the build can resolve.");
            return;
        }
        if (java17 == null) {
            System.out.println("[WARN] Skipping Maven layout build checks. No JDK 17 found.");
            return;
        }
        String repoLocal = System.getenv("CN1_MAVEN_REPO_LOCAL");
        Path homeDir = Files.createTempDirectory("initializr-home-layouts-");
        ensureCodenameOneHome(homeDir, buildClient);

        ProjectOptions backendOnly = layoutOptions(ProjectOptions.ProjectType.BACKEND_ONLY);
        Path server = generateLayoutProject(backendOnly, "LayoutServerApp", "com.acme.initializr.server", version);
        int exit = runMaven(server, homeDir, java17, repoLocal, "process-classes");
        assertTrue(exit == 0, "The backend-only Maven project should build | exitCode=" + exit);
        assertTrue(Files.isRegularFile(server.resolve("target/classes/META-INF/cn1-backend-main")),
                "The backend-only build should generate its entry point");

        Path app = generateLayoutProject(layoutOptions(ProjectOptions.ProjectType.APP), "LayoutMinimalApp",
                "com.acme.initializr.minimal", version);
        assertTrue(!Files.exists(app.resolve("javase")) && !Files.exists(app.resolve("android")),
                "The minimal layout should have no platform modules");
        exit = runMaven(app, homeDir, java17, repoLocal, "package", "-DskipTests=true",
                "-Dcodename1.platform=android", "-Dcodename1.buildTarget=android-device",
                "-Dcodename1.stageOnly=true");
        assertTrue(exit == 0, "The minimal app should stage an Android build from common | exitCode=" + exit);
        File[] staged = app.resolve("common/target").toFile().listFiles(
                (d, n) -> n.endsWith("-android-device-jar-with-dependencies.jar"));
        assertTrue(staged != null && staged.length == 1, "common should stage the Android upload");
        exit = runMaven(app, homeDir, java17, repoLocal, "package", "-DskipTests=true", "-Pexecutable-jar",
                "-Dcodename1.platform=javase");
        assertTrue(exit == 0, "The minimal app should package the desktop jar from common | exitCode=" + exit);
        File[] desktop = app.resolve("common/target").toFile().listFiles(
                (d, n) -> n.endsWith(".jar") && n.indexOf("-javase-") > 0);
        assertTrue(desktop != null && desktop.length == 1, "common should write the desktop jar");
    }

    private ProjectOptions layoutOptions(ProjectOptions.ProjectType type) {
        return new ProjectOptions(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.DEFAULT,
                true, false, ProjectOptions.PreviewLanguage.ENGLISH, ProjectOptions.JavaVersion.JAVA_17, null,
                ProjectOptions.BuildTool.MAVEN, type);
    }

    /// A project generated on the layouts side of the gate, with the poms pointed at `version`.
    private Path generateLayoutProject(ProjectOptions options, String appName, String packageName, String version)
            throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GeneratorModel.createForPluginVersion(IDE.INTELLIJ, Template.BAREBONES, appName, packageName, options,
                GeneratorModel.MAVEN_LAYOUTS_SINCE).writeProjectZip(output);
        Path dir = Files.createTempDirectory("initializr-layout-" + appName + "-");
        unzipProject(output.toByteArray(), dir);
        Path pom = dir.resolve("pom.xml");
        String text = new String(Files.readAllBytes(pom), "UTF-8");
        text = text.replaceAll("<cn1\\.plugin\\.version>[^<]*</cn1\\.plugin\\.version>",
                "<cn1.plugin.version>" + version + "</cn1.plugin.version>");
        text = text.replaceAll("<cn1\\.version>[^<]*</cn1\\.version>", "<cn1.version>" + version + "</cn1.version>");
        Files.write(pom, text.getBytes("UTF-8"));
        return dir;
    }

    private int runMaven(Path projectDir, Path homeDir, Path javaHome, String repoLocal, String... args)
            throws Exception {
        List<String> command = new ArrayList<String>();
        command.add("mvn");
        command.add("-B");
        command.add("-Duser.home=" + homeDir.toString());
        if (repoLocal != null && repoLocal.length() > 0) {
            command.add("-Dmaven.repo.local=" + repoLocal);
        }
        for (String a : args) {
            command.add(a);
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put("JAVA_HOME", javaHome.toString());
        env.put("PATH", javaHome.resolve("bin") + File.pathSeparator + env.get("PATH"));
        return runAndReport(pb);
    }

    /// Puts `repoDir` first in the copy's pluginManagement repositories, so the
    /// plugin resolves from it; -Pcodename1.repository does the same for the
    /// framework the plugin adds.
    private void injectPluginRepository(Path settings, Path repoDir, String version) throws IOException {
        String text = new String(Files.readAllBytes(settings), "UTF-8");
        String marker = "    repositories {\n";
        assertTrue(text.indexOf(marker) >= 0, "settings.gradle.kts should declare pluginManagement repositories");
        text = text.replace(marker, marker + "        maven(uri(\"" + repoDir.toUri() + "\"))\n");
        if (version != null && version.length() > 0) {
            text = text.replaceAll("id\\(\"com\\.codenameone\"\\) version \"[^\"]*\"",
                    "id(\"com.codenameone\") version \"" + version + "\"");
        }
        Files.write(settings, text.getBytes("UTF-8"));
    }

    private int runGradleClasses(Path projectDir, Path repoDir, Path javaHome) throws Exception {
        Path gradlew = projectDir.resolve("gradlew");
        gradlew.toFile().setExecutable(true);
        boolean windows = File.separatorChar == '\\';
        List<String> command = new ArrayList<String>();
        if (windows) {
            command.add("cmd");
            command.add("/c");
            command.add(projectDir.resolve("gradlew.bat").toString());
        } else {
            command.add(gradlew.toString());
        }
        command.add("--no-daemon");
        command.add("--stacktrace");
        command.add("classes");
        command.add("-Pcodename1.repository=" + repoDir);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);
        Map<String, String> env = pb.environment();
        env.put("JAVA_HOME", javaHome.toString());
        env.put("PATH", javaHome.resolve("bin") + File.pathSeparator + env.get("PATH"));
        return runAndReport(pb);
    }

    private void assertLocalizationBakedIntoThemeRes(Path projectDir, ProjectOptions.JavaVersion version) throws Exception {
        Path themeRes = projectDir.resolve("common/target/classes/theme.res");
        assertTrue(Files.isRegularFile(themeRes),
                "theme.res should exist after the build. Version=" + version.label + " | path=" + themeRes);

        Resources res;
        try (FileInputStream in = new FileInputStream(themeRes.toFile())) {
            res = Resources.open(in);
        }

        Hashtable<String, String> defaultBundle = res.getL10N("messages", "");
        assertNotNull(defaultBundle,
                "theme.res should contain a 'messages' L10N bundle for the default locale (\"\"). "
                        + "If null, bundles were not picked up by the CN1 css compiler -- check that "
                        + "they are placed under common/src/main/l10n. Version=" + version.label);
        assertTrue(defaultBundle.size() > 0,
                "Default 'messages' bundle should not be empty. Version=" + version.label);

        Hashtable<String, String> hebrew = res.getL10N("messages", "he");
        assertNotNull(hebrew,
                "theme.res should contain a Hebrew 'messages' bundle when localization bundles are requested. "
                        + "Version=" + version.label);
        assertTrue(hebrew.size() > 0,
                "Hebrew 'messages' bundle should not be empty. Version=" + version.label);
    }

    private byte[] createProjectZip(ProjectOptions options, String appName, String packageName) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GeneratorModel.create(IDE.INTELLIJ, Template.BAREBONES, appName, packageName, options).writeProjectZip(output);
        return output.toByteArray();
    }

    private int runMavenCompile(Path projectDir, Path homeDir, Path javaHome) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "mvn",
                "-f", "common/pom.xml",
                "-DskipTests=true",
                "-Dcodename1.platform=javase",
                "-Duser.home=" + homeDir.toString(),
                // process-classes, not compile: the generated pom binds the cn1 css goal
                // (theme.css -> theme.res, localization bundles and all) to that phase, so
                // stopping at compile leaves nothing for assertLocalizationBakedIntoThemeRes
                // to read.
                "process-classes"
        );
        pb.directory(projectDir.toFile());
        pb.redirectErrorStream(true);

        Map<String, String> env = pb.environment();
        env.put("JAVA_HOME", javaHome.toString());
        env.put("PATH", javaHome.resolve("bin") + File.pathSeparator + env.get("PATH"));

        return runAndReport(pb);
    }

    /// Runs the process, and prints its output when it fails.
    private int runAndReport(ProcessBuilder pb) throws Exception {
        List<String> output = new ArrayList<String>();
        Process process = pb.start();
        try (InputStream in = process.getInputStream()) {
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(in));
            String line;
            while ((line = r.readLine()) != null) {
                output.add(line);
            }
        }
        int exit = process.waitFor();
        if (exit != 0) {
            StringBuilder sb = new StringBuilder();
            for (String line : output) {
                if (sb.length() > 12000) {
                    sb.append("\n...[truncated]");
                    break;
                }
                sb.append(line).append('\n');
            }
            System.out.println(sb.toString());
        }
        return exit;
    }

    /// Populates the throwaway home the generated build runs against. Both jars belong in
    /// `.codenameone/`: that is where the generated pom's systemPath points and where
    /// `generate-gui-sources` looks. The build client has to be the real one - the mojo
    /// loads `com.codename1.build.client.GenerateGuiSources` out of it, so an empty
    /// placeholder fails the build before it compiles a line.
    private void ensureCodenameOneHome(Path homeDir, Path buildClient) throws IOException {
        Path cn1Dir = homeDir.resolve(".codenameone");
        Files.createDirectories(cn1Dir);
        Files.write(cn1Dir.resolve("guibuilder.jar"), new byte[0]);
        Files.copy(buildClient, cn1Dir.resolve("CodeNameOneBuildClient.jar"));
    }

    /// The build client installed on this machine, or null when there is none. The test
    /// overrides `user.home` for the child build only, so the real home still holds it.
    private Path findBuildClientJar() {
        String home = System.getProperty("user.home");
        if (home == null || home.length() == 0) {
            return null;
        }
        Path jar = Paths.get(home, ".codenameone", "CodeNameOneBuildClient.jar");
        return Files.isRegularFile(jar) ? jar : null;
    }

    private void unzipProject(byte[] zipData, Path destination) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(zipData);
        ZipInputStream zis = new ZipInputStream(input);
        try {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    Path target = destination.resolve(entry.getName());
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    FileOutputStream fos = new FileOutputStream(target.toFile());
                    try {
                        Util.copyNoClose(zis, fos, 8192);
                    } finally {
                        fos.close();
                    }
                }
                zis.closeEntry();
                entry = zis.getNextEntry();
            }
        } finally {
            zis.close();
            input.close();
        }
    }

    private Path findJava8Or11Home() throws Exception {
        Path java11 = findJavaHomeForMajor(11);
        if (java11 != null) {
            return java11;
        }
        return findJavaHomeForMajor(8);
    }

    private Path findJavaHomeForMajor(int major) throws Exception {
        String envName = "INITIALIZR_JDK" + major + "_HOME";
        String envValue = System.getenv(envName);
        if (envValue != null && envValue.length() > 0) {
            Path candidate = Paths.get(envValue);
            if (looksLikeJdkHome(candidate) && javaMajor(candidate) == major) {
                return candidate;
            }
        }

        List<Path> candidates = new ArrayList<Path>();
        String currentJavaHome = System.getProperty("java.home");
        if (currentJavaHome != null) {
            candidates.add(Paths.get(currentJavaHome).getParent());
            candidates.add(Paths.get(currentJavaHome));
        }
        collectJvmCandidates(candidates, "/usr/lib/jvm");
        collectJvmCandidates(candidates, "/Library/Java/JavaVirtualMachines");
        collectJvmCandidates(candidates, "C:\\Program Files\\Java");

        for (Path candidate : candidates) {
            if (!looksLikeJdkHome(candidate)) {
                continue;
            }
            if (javaMajor(candidate) == major) {
                return candidate;
            }
            Path nestedHome = candidate.resolve("Contents/Home");
            if (looksLikeJdkHome(nestedHome) && javaMajor(nestedHome) == major) {
                return nestedHome;
            }
        }

        return null;
    }

    private void collectJvmCandidates(List<Path> out, String directory) throws IOException {
        Path root = Paths.get(directory);
        if (!Files.isDirectory(root)) {
            return;
        }
        out.add(root);
        try (java.util.stream.Stream<Path> stream = Files.list(root)) {
            stream.forEach(out::add);
        }
    }

    private boolean looksLikeJdkHome(Path candidate) {
        return candidate != null && Files.isRegularFile(candidate.resolve("bin").resolve("java"));
    }

    private int javaMajor(Path javaHome) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(javaHome.resolve("bin").resolve("java").toString(), "-version");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder out = new StringBuilder();
        try (InputStream in = process.getInputStream()) {
            int b;
            while ((b = in.read()) != -1) {
                out.append((char) b);
            }
        }
        process.waitFor();

        String text = out.toString().toLowerCase(Locale.ROOT);
        if (text.indexOf(" version \"1.8") >= 0) {
            return 8;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("version \\\"([0-9]+)").matcher(text);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }
}
