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

        Path buildClient = findBuildClientJar();
        if (buildClient == null) {
            // Every goal in the generated project's compile runs out of this jar, so
            // without it there is nothing to test. Skip rather than fail: it is installed
            // by setup-workspace.sh, not by this repository's build.
            System.out.println("[WARN] Skipping integration build checks. No "
                    + ".codenameone/CodeNameOneBuildClient.jar found under the user home.");
            return true;
        }

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
