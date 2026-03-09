package com.codename1.initializr.model;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

        if (java8Or11 == null) {
            System.out.println("[WARN] Skipping Java 8/11 integration build check. No JDK 8 or 11 found.");
        } else {
            buildGeneratedProject(ProjectOptions.JavaVersion.JAVA_8, java8Or11, "java8-or-11");
        }

        if (java17 == null) {
            System.out.println("[WARN] Skipping Java 17 integration build check. No JDK 17 found.");
        } else {
            buildGeneratedProject(ProjectOptions.JavaVersion.JAVA_17_EXPERIMENTAL, java17, "java17");
        }

        return true;
    }

    private void buildGeneratedProject(ProjectOptions.JavaVersion version, Path javaHome, String suffix) throws Exception {
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
        ensureCodenameOneHome(homeDir);
        unzipProject(zip, projectDir);

        int exitCode = runMavenCompile(projectDir, homeDir, javaHome);
        assertTrue(exitCode == 0, "Generated project should compile with selected JDK. Version=" + version.label + " | exitCode=" + exitCode);
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
                "compile"
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

    private void ensureCodenameOneHome(Path homeDir) throws IOException {
        Path cn1Dir = homeDir.resolve(".codenameone");
        Files.createDirectories(cn1Dir);
        Files.write(cn1Dir.resolve("guibuilder.jar"), new byte[0]);
        Files.write(homeDir.resolve("CodeNameOneBuildClient.jar"), new byte[0]);
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
