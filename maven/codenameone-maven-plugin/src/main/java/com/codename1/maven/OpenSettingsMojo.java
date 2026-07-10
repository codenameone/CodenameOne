/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Opens the standalone Codename One Settings tool.
 *
 * <pre>mvn cn1:settings</pre>
 */
@Mojo(name = "settings")
public class OpenSettingsMojo extends AbstractCN1Mojo {
    private static final String LAUNCHED_PROPERTY =
            "com.codename1.maven.OpenSettingsMojo.launched";

    @Parameter(property = "settings.spawn", required = false, defaultValue = "true")
    private boolean spawn;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (Boolean.getBoolean(LAUNCHED_PROPERTY)) {
            getLog().debug("Skipping settings: already launched in this Maven invocation");
            return;
        }
        if (!isCN1ProjectDir()) {
            getLog().debug("Skipping settings: not a CN1 project dir");
            return;
        }
        System.setProperty(LAUNCHED_PROPERTY, "true");

        File projectDir = getCN1ProjectDir();
        File runtimeDir = new File(System.getProperty("user.home"), ".codenameoneSettings");
        runtimeDir.mkdirs();
        File inputFile = new File(runtimeDir, "settings-" + UUID.randomUUID() + ".input");
        writeBinding(inputFile, projectDir);

        ToolClasspath toolClasspath = getSettingsClasspath();
        getLog().info("Launching Codename One Settings bound to " + projectDir);
        if (shouldSpawn()) {
            launchDetached(toolClasspath, runtimeDir, inputFile, projectDir);
            return;
        }

        Java java = createJava();
        java.setFork(true);
        java.setJvm(namedJavaLauncher(runtimeDir).getAbsolutePath());
        java.setClassname("com.codename1.settings.CodenameOneSettingsLauncher");
        java.createClasspath().setPath(joinClasspath(toolClasspath.files));
        configureDesktopIdentity(java, toolClasspath.primaryJar, runtimeDir);
        java.createJvmarg().setValue("-Dsettings.input=" + inputFile.getAbsolutePath());
        java.executeJava();
    }

    @Override
    protected boolean isCN1ProjectDir() {
        File cn1ProjectDir = getCN1ProjectDir();
        if (cn1ProjectDir == null || project == null || project.getBasedir() == null) {
            return false;
        }
        try {
            File current = project.getBasedir().getCanonicalFile();
            File cn1 = cn1ProjectDir.getCanonicalFile();
            if (cn1.equals(current)) {
                return true;
            }
            File rootCommon = new File(current, "common").getCanonicalFile();
            return cn1.equals(rootCommon);
        } catch (IOException ex) {
            getLog().error("Failed to get canonical paths for project dir", ex);
            return false;
        }
    }

    private boolean shouldSpawn() {
        String legacySpawn = System.getProperty("spawn");
        if (legacySpawn != null) {
            return Boolean.parseBoolean(legacySpawn);
        }
        return spawn;
    }

    private void launchDetached(ToolClasspath toolClasspath, File runtimeDir, File inputFile, File projectDir)
            throws MojoExecutionException {
        File log = new File(runtimeDir, "settings.log");
        List<String> command = new ArrayList<String>();
        command.add(namedJavaLauncher(runtimeDir).getAbsolutePath());
        command.addAll(desktopIdentityArgs(toolClasspath.primaryJar, runtimeDir));
        command.add("-Dsettings.input=" + inputFile.getAbsolutePath());
        command.add("-cp");
        command.add(joinClasspath(toolClasspath.files));
        command.add("com.codename1.settings.CodenameOneSettingsLauncher");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        configureLauncherEnvironment(pb);
        try {
            pb.start();
            getLog().info("Codename One Settings launched in the background. Log: " + log.getAbsolutePath());
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to launch Codename One Settings", ex);
        }
    }

    private void configureDesktopIdentity(Java java, File jar, File runtimeDir) {
        for (String arg : desktopIdentityArgs(jar, runtimeDir)) {
            java.createJvmarg().setValue(arg);
        }
    }

    List<String> desktopIdentityArgs(File jar, File runtimeDir) {
        List<String> args = new ArrayList<String>();
        args.add("-Dapple.awt.application.name=Codename One Settings");
        args.add("-Dcom.apple.mrj.application.apple.menu.about.name=Codename One Settings");
        args.add("-Dsun.awt.application.name=Codename One Settings");
        args.add("-Dsun.awt.X11.XWMClass=CodenameOneSettings");
        if (isJava9OrNewer()) {
            args.add("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED");
            args.add("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED");
        }
        if (isMacOs()) {
            args.add("-Xdock:name=Codename One Settings");
            File icon = extractSettingsIcon(jar, runtimeDir);
            if (icon != null && icon.isFile()) {
                args.add("-Xdock:icon=" + icon.getAbsolutePath());
            }
        }
        return args;
    }

    File namedJavaLauncher(File runtimeDir) {
        File java = new File(javaExecutable());
        if (isWindows()) {
            File launcher = new File(runtimeDir, "CodenameOneSettings.exe");
            try {
                FileUtils.copyFile(java, launcher);
                return launcher;
            } catch (IOException ex) {
                getLog().debug("Unable to create Settings launcher executable: " + ex.getMessage());
                return java;
            }
        }
        File launcher = new File(runtimeDir, "Codename One Settings");
        try {
            Files.deleteIfExists(launcher.toPath());
            Files.createSymbolicLink(launcher.toPath(), java.toPath());
            return launcher;
        } catch (IOException | UnsupportedOperationException | SecurityException ex) {
            getLog().debug("Unable to create Settings launcher symlink: " + ex.getMessage());
            return java;
        }
    }

    File extractSettingsIcon(File jar, File runtimeDir) {
        File iconFile = new File(runtimeDir, "settings-icon.png");
        try (JarFile jf = new JarFile(jar)) {
            JarEntry entry = jf.getJarEntry("icon.png");
            if (entry == null) {
                return null;
            }
            try (InputStream in = jf.getInputStream(entry)) {
                FileUtils.copyInputStreamToFile(in, iconFile);
            }
            return iconFile;
        } catch (IOException ex) {
            getLog().debug("Unable to extract Settings dock icon: " + ex.getMessage());
            return null;
        }
    }

    private void configureLauncherEnvironment(ProcessBuilder pb) {
        if (!isWindows()) {
            return;
        }
        String javaBin = new File(System.getProperty("java.home"), "bin").getAbsolutePath();
        String path = pb.environment().get("PATH");
        pb.environment().put("PATH", path == null || path.length() == 0
                ? javaBin : javaBin + File.pathSeparator + path);
    }

    void writeBinding(File inputFile, File projectDir) throws MojoExecutionException {
        File root = multimoduleRoot(projectDir);
        File buildHints = new File(root, "docs/developer-guide/Advanced-Topics-Under-The-Hood.asciidoc");
        String content = "# Codename One Settings project binding\n"
                + "projectDir=" + projectDir.getAbsolutePath() + "\n"
                + "settings=" + new File(projectDir, "codenameone_settings.properties").getAbsolutePath() + "\n"
                + "pom=" + new File(projectDir, "pom.xml").getAbsolutePath() + "\n"
                + "multimoduleRoot=" + root.getAbsolutePath() + "\n"
                + (buildHints.isFile() ? "buildHintsDoc=" + buildHints.getAbsolutePath() + "\n" : "");
        try {
            FileUtils.write(inputFile, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write Settings binding", ex);
        }
    }

    File multimoduleRoot(File projectDir) {
        File parent = projectDir == null ? null : projectDir.getParentFile();
        if (parent != null && "common".equals(projectDir.getName())) {
            return parent;
        }
        return projectDir == null ? new File(".") : projectDir;
    }

    private ToolClasspath getSettingsClasspath() throws MojoExecutionException, MojoFailureException {
        Artifact artifact = getArtifact("com.codenameone", "codenameone-settings");
        if (artifact == null) {
            artifact = repositorySystem.createArtifact(
                    "com.codenameone", "codenameone-settings", pluginVersion(), "jar");
        }
        ToolClasspath classpath = resolveToolClasspath(artifact);
        if (classpath.primaryJar == null || classpath.files.isEmpty()) {
            throw new MojoFailureException(
                    "Could not resolve Codename One Settings "
                    + "(com.codenameone:codenameone-settings:" + pluginVersion()
                    + ").\n"
                    + "It is distributed through Maven Central alongside the Codename One plugin.\n"
                    + "To work on the Settings tool itself, run:\n"
                    + "    cd scripts/settings && mvn -Pexecutable-jar -pl javase -am package -Dcodename1.platform=javase\n"
                    + "    java -cp \"javase/target/codenameone-settings-*.jar:javase/target/libs/*\" "
                    + "com.codename1.settings.CodenameOneSettingsLauncher");
        }
        return classpath;
    }

    private ToolClasspath resolveToolClasspath(Artifact artifact) {
        List<File> files = new ArrayList<File>();
        ArtifactResolutionResult result = repositorySystem.resolve(new ArtifactResolutionRequest()
                .setLocalRepository(localRepository)
                .setRemoteRepositories(new ArrayList<ArtifactRepository>(remoteRepositories))
                .setResolveTransitively(true)
                .setArtifact(artifact));
        File primary = addArtifactFile(files, artifact);
        if (result != null && result.getArtifacts() != null) {
            for (Artifact resolved : result.getArtifacts()) {
                File file = addArtifactFile(files, resolved);
                if (primary == null && resolved != null
                        && "com.codenameone".equals(resolved.getGroupId())
                        && "codenameone-settings".equals(resolved.getArtifactId())) {
                    primary = file;
                }
            }
        }
        return new ToolClasspath(primary, files);
    }

    private static File addArtifactFile(List<File> files, Artifact artifact) {
        if (artifact == null || artifact.getFile() == null || !"jar".equals(artifact.getType())) {
            return null;
        }
        File file = artifact.getFile().getAbsoluteFile();
        if (!file.exists()) {
            return null;
        }
        if (!files.contains(file)) {
            files.add(file);
        }
        return file;
    }

    private static String joinClasspath(List<File> files) {
        StringBuilder out = new StringBuilder();
        for (File file : files) {
            if (out.length() > 0) {
                out.append(File.pathSeparator);
            }
            out.append(file.getAbsolutePath());
        }
        return out.toString();
    }

    private String javaExecutable() {
        String executable = isWindows() ? "javaw.exe" : "java";
        return new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath();
    }

    static boolean isMacOs() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    static boolean isJava9OrNewer() {
        String version = System.getProperty("java.specification.version", "");
        return version.length() > 0 && !version.startsWith("1.");
    }

    String pluginVersion() {
        if (pluginArtifacts != null) {
            for (Artifact a : pluginArtifacts) {
                if ("codenameone-maven-plugin".equals(a.getArtifactId())
                        && "com.codenameone".equals(a.getGroupId())) {
                    return a.getVersion();
                }
            }
        }
        if (project == null) {
            return "8.0-SNAPSHOT";
        }
        return project.getProperties().getProperty("cn1.plugin.version",
                project.getProperties().getProperty("cn1.version", "8.0-SNAPSHOT"));
    }

    private static final class ToolClasspath {
        final File primaryJar;
        final List<File> files;

        ToolClasspath(File primaryJar, List<File> files) {
            this.primaryJar = primaryJar;
            this.files = files;
        }
    }
}
