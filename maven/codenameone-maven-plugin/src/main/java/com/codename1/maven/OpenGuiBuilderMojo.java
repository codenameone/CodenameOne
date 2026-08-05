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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Opens the modern standalone Codename One GUI Builder for every GUI form in the project.
 * The editor is resolved from Maven using the same distribution model as Codename One
 * Settings; no downloaded {@code ~/.codenameone/guibuilder.jar} is used.
 *
 * <pre>mvn cn1:guibuilder [-DclassName=com.example.MyForm]</pre>
 */
@Mojo(name = "guibuilder")
public class OpenGuiBuilderMojo extends AbstractCN1Mojo {
    private static final String LAUNCHED_PROPERTY = "com.codename1.maven.OpenGuiBuilderMojo.launched";

    /** The editor is compiled for this Java release, so an older forked JVM cannot load it. */
    private static final int REQUIRED_JAVA_VERSION = 17;

    /** Optional fully-qualified form to select initially. */
    @Parameter(property = "className", required = false)
    private String className;

    @Parameter(property = "guibuilder.spawn", required = false, defaultValue = "true")
    private boolean spawn;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (Boolean.getBoolean(LAUNCHED_PROPERTY)) {
            getLog().debug("Skipping guibuilder: already launched in this Maven invocation");
            return;
        }
        if (!isCN1ProjectDir()) {
            getLog().debug("Skipping guibuilder: not a Codename One project directory");
            return;
        }
        requireModernJdk();
        System.setProperty(LAUNCHED_PROPERTY, "true");

        File projectDir = getCN1ProjectDir();
        File guiDir = new File(projectDir, "src" + File.separator + "main" + File.separator + "guibuilder");
        File sourceDir = new File(projectDir, "src" + File.separator + "main" + File.separator + "java");
        File cssFile = new File(projectDir, "src" + File.separator + "main" + File.separator + "css" + File.separator + "theme.css");
        guiDir.mkdirs();

        File runtimeDir = new File(System.getProperty("user.home"), ".codenameoneGUIBuilder");
        runtimeDir.mkdirs();
        File input = new File(runtimeDir, "guibuilder-" + UUID.randomUUID() + ".input");
        writeBinding(input, projectDir, guiDir, sourceDir, cssFile);

        ToolClasspath classpath = getGuiBuilderClasspath();
        getLog().info("Launching Codename One GUI Builder bound to " + projectDir);
        if (shouldSpawn()) {
            launchDetached(classpath, runtimeDir, input, projectDir);
            return;
        }
        Java java = createJava();
        java.setFork(true);
        java.setClassname("com.codename1.guibuilder.CodenameOneGUIBuilderLauncher");
        java.createClasspath().setPath(joinClasspath(classpath.files));
        for (String arg : desktopIdentityArgs()) {
            java.createJvmarg().setValue(arg);
        }
        java.createJvmarg().setValue("-Dguibuilder.input=" + input.getAbsolutePath());
        for (String arg : forwardedGuiBuilderProperties()) {
            java.createJvmarg().setValue(arg);
        }
        java.executeJava();
    }

    /**
     * The GUI Builder is a Java 17 artifact. The spawned process writes only to its log file, so
     * without this check an older Maven JVM fails with an UnsupportedClassVersionError that never
     * reaches the console.
     */
    private void requireModernJdk() throws MojoFailureException {
        int version = javaFeatureVersion();
        if (version >= REQUIRED_JAVA_VERSION) {
            return;
        }
        throw new MojoFailureException("The Codename One GUI Builder needs JDK "
                + REQUIRED_JAVA_VERSION + " or newer, but Maven is running on "
                + System.getProperty("java.version", String.valueOf(version)) + " ("
                + System.getProperty("java.home") + ").\n"
                + "Point JAVA_HOME at a JDK " + REQUIRED_JAVA_VERSION
                + "+ installation (for example Eclipse Temurin from https://adoptium.net) and run "
                + "mvn cn1:guibuilder again.");
    }

    static int javaFeatureVersion() {
        String version = System.getProperty("java.specification.version", "");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dot = version.indexOf('.');
        if (dot > 0) {
            version = version.substring(0, dot);
        }
        try {
            return Integer.parseInt(version.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * Forwards {@code guibuilder.*} system properties (MCP port, canvas mode, dark mode, initial
     * selection, editor to open) from the Maven invocation to the editor JVM, so
     * {@code mvn cn1:guibuilder -Dguibuilder.mcp.port=18349} works. The {@code guibuilder.input}
     * binding and the {@code guibuilder.spawn} launch flag are owned by this mojo and never
     * forwarded.
     */
    List<String> forwardedGuiBuilderProperties() {
        List<String> args = new ArrayList<String>();
        for (String key : System.getProperties().stringPropertyNames()) {
            if (key.startsWith("guibuilder.")
                    && !key.equals("guibuilder.input")
                    && !key.equals("guibuilder.spawn")) {
                args.add("-D" + key + "=" + System.getProperty(key));
            }
        }
        return args;
    }

    /**
     * Names the process for the dock, taskbar and window manager, and opens the JDK packages the
     * JavaSE port needs on Java 9 and newer, matching cn1:settings and cn1:certificate-wizard.
     */
    List<String> desktopIdentityArgs() {
        List<String> args = new ArrayList<String>();
        args.add("-Dapple.awt.application.name=Codename One GUI Builder");
        args.add("-Dcom.apple.mrj.application.apple.menu.about.name=Codename One GUI Builder");
        args.add("-Dsun.awt.application.name=Codename One GUI Builder");
        args.add("-Dsun.awt.X11.XWMClass=CodenameOneGUIBuilder");
        args.add("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED");
        args.add("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED");
        if (isMacOs()) {
            args.add("-Xdock:name=Codename One GUI Builder");
        }
        return args;
    }

    private static boolean isMacOs() {
        return System.getProperty("os.name", "").toLowerCase().contains("mac");
    }

    @Override
    protected boolean isCN1ProjectDir() {
        File cn1 = getCN1ProjectDir();
        if (cn1 == null || project == null || project.getBasedir() == null) return false;
        try {
            File current = project.getBasedir().getCanonicalFile();
            File projectDir = cn1.getCanonicalFile();
            return projectDir.equals(current) || projectDir.equals(new File(current, "common").getCanonicalFile());
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean shouldSpawn() {
        String legacy = System.getProperty("spawn");
        return legacy == null ? spawn : Boolean.parseBoolean(legacy);
    }

    void writeBinding(File input, File projectDir, File guiDir, File sourceDir, File cssFile)
            throws MojoExecutionException {
        StringBuilder content = new StringBuilder();
        content.append("# Codename One GUI Builder project binding\n");
        content.append("projectDir=").append(projectDir.getAbsolutePath()).append('\n');
        content.append("guiDir=").append(guiDir.getAbsolutePath()).append('\n');
        content.append("sourceDir=").append(sourceDir.getAbsolutePath()).append('\n');
        content.append("cssFile=").append(cssFile.getAbsolutePath()).append('\n');
        if (className != null && className.trim().length() > 0) {
            content.append("initialForm=").append(className.trim()).append('\n');
        }
        try {
            FileUtils.write(input, content.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write GUI Builder project binding", ex);
        }
    }

    private void launchDetached(ToolClasspath classpath, File runtimeDir, File input, File projectDir)
            throws MojoExecutionException {
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable());
        command.addAll(desktopIdentityArgs());
        command.add("-Dguibuilder.input=" + input.getAbsolutePath());
        command.addAll(forwardedGuiBuilderProperties());
        command.add("-cp");
        command.add(joinClasspath(classpath.files));
        command.add("com.codename1.guibuilder.CodenameOneGUIBuilderLauncher");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectDir);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(runtimeDir, "guibuilder.log")));
        try {
            builder.start();
            getLog().info("GUI Builder launched in the background. Log: " + new File(runtimeDir, "guibuilder.log"));
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to launch Codename One GUI Builder", ex);
        }
    }

    private ToolClasspath getGuiBuilderClasspath() throws MojoExecutionException, MojoFailureException {
        Artifact artifact = getArtifact("com.codenameone", "codenameone-guibuilder");
        if (artifact == null) {
            artifact = repositorySystem.createArtifact("com.codenameone", "codenameone-guibuilder", pluginVersion(), "jar");
        }
        List<File> files = new ArrayList<File>();
        ArtifactResolutionResult result = repositorySystem.resolve(new ArtifactResolutionRequest()
                .setLocalRepository(localRepository)
                .setRemoteRepositories(new ArrayList<ArtifactRepository>(remoteRepositories))
                .setResolveTransitively(true)
                .setArtifact(artifact));
        addArtifact(files, artifact);
        if (result != null && result.getArtifacts() != null) {
            for (Artifact resolved : result.getArtifacts()) addArtifact(files, resolved);
        }
        if (files.isEmpty()) {
            throw new MojoFailureException("Could not resolve the GUI Builder (com.codenameone:codenameone-guibuilder:"
                    + pluginVersion() + "). It is distributed through Maven Central alongside the Codename One plugin.\n"
                    + "To work on the editor, run:\n"
                    + "    cd scripts/guibuilder && mvn -Pexecutable-jar -pl javase -am package -Dcodename1.platform=javase");
        }
        return new ToolClasspath(files);
    }

    private static void addArtifact(List<File> files, Artifact artifact) {
        if (artifact == null || artifact.getFile() == null || !"jar".equals(artifact.getType())) return;
        File file = artifact.getFile().getAbsoluteFile();
        if (file.exists() && !files.contains(file)) files.add(file);
    }

    private String pluginVersion() {
        if (pluginArtifacts != null) {
            for (Artifact artifact : pluginArtifacts) {
                if ("com.codenameone".equals(artifact.getGroupId())
                        && "codenameone-maven-plugin".equals(artifact.getArtifactId())) return artifact.getVersion();
            }
        }
        return project.getProperties().getProperty("cn1.plugin.version",
                project.getProperties().getProperty("cn1.version", "8.0-SNAPSHOT"));
    }

    private String javaExecutable() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return new File(new File(System.getProperty("java.home"), "bin"), windows ? "javaw.exe" : "java").getAbsolutePath();
    }

    private static String joinClasspath(List<File> files) {
        StringBuilder value = new StringBuilder();
        for (File file : files) {
            if (value.length() > 0) value.append(File.pathSeparator);
            value.append(file.getAbsolutePath());
        }
        return value.toString();
    }

    private static final class ToolClasspath {
        final List<File> files;
        ToolClasspath(List<File> files) { this.files = files; }
    }
}
