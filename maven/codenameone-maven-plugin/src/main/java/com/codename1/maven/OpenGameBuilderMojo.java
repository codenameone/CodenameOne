/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Launches the standalone Codename One game builder, bound to this project, the same
 * way {@code cn1:guibuilder} launches the GUI builder.
 *
 * <p>It writes a binding descriptor to {@code ~/.gameBuilder/gamebuilder.input} (the
 * project's games directory, settings and an output channel) and forks the editor app.
 * The editor is delivered through Maven &mdash; the artifact
 * {@code com.codenameone:codenameone-gamebuilder:jar-with-dependencies} is resolved from
 * Maven Central (the same mechanism that ships the Designer/CSS-compiler jar), so there
 * is no separate install step. The editor reads/writes
 * {@code src/main/resources/games/*.game} in this project.</p>
 *
 * <p>The goal only applies to Java 17 Codename One projects (the editor uses Java 17
 * language features and the generated scenes target the 8.x gaming APIs).</p>
 *
 * <pre>mvn cn1:gamebuilder</pre>
 */
@Mojo(name = "gamebuilder")
public class OpenGameBuilderMojo extends AbstractCN1Mojo {

    /** Optional scene class to open initially (e.g. com.example.Level1). */
    @Parameter(property = "className", required = false)
    private String className;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            getLog().debug("Skipping gamebuilder: not a CN1 project dir");
            return;
        }
        requireJava17();

        File projectDir = getCN1ProjectDir();
        File gamesDir = new File(projectDir, "src" + File.separator + "main" + File.separator
                + "resources" + File.separator + "games");
        gamesDir.mkdirs();

        File runtimeDir = new File(System.getProperty("user.home"), ".gameBuilder");
        runtimeDir.mkdirs();
        String uuid = UUID.randomUUID().toString();
        File outputFile = new File(runtimeDir, uuid + ".output");
        writeBinding(new File(runtimeDir, "gamebuilder.input"), projectDir, gamesDir, outputFile);

        File jar = getGameBuilderJar();

        getLog().info("Launching game builder bound to " + projectDir);
        Java java = createJava();
        java.setFork(true);
        if ("true".equals(System.getProperty("spawn", "true"))) {
            java.setSpawn(true);
        }
        java.setJar(jar);
        // The forked editor reads the binding path from this system property.
        java.createJvmarg().setValue("-Dgamebuilder.input="
                + new File(runtimeDir, "gamebuilder.input").getAbsolutePath());
        if (className != null && !className.trim().isEmpty()) {
            java.createJvmarg().setValue("-Dgamebuilder.scene=" + className.trim());
        }
        java.executeJava();
    }

    private void requireJava17() throws MojoFailureException, MojoExecutionException {
        String v;
        try {
            v = getProjectProperties().getProperty("codename1.arg.java.version",
                    getProjectProperties().getProperty("codename1.java.version", "8"));
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to read codenameone_settings.properties", ex);
        }
        if (!"17".equals(v.trim())) {
            throw new MojoFailureException("The game builder requires a Java 17 Codename One project "
                    + "(codename1.arg.java.version=17). This project reports '" + v + "'.");
        }
    }

    private void writeBinding(File inputFile, File projectDir, File gamesDir, File outputFile)
            throws MojoExecutionException {
        String pkg = "";
        try {
            pkg = getProjectProperties().getProperty("codename1.packageName", "");
        } catch (IOException ignore) {
            // optional
        }
        String content = "# Codename One game builder project binding\n"
                + "projectDir=" + projectDir.getAbsolutePath() + "\n"
                + "gamesDir=" + gamesDir.getAbsolutePath() + "\n"
                + "settings=" + new File(projectDir, "codenameone_settings.properties").getAbsolutePath() + "\n"
                + "sourceDir=" + new File(projectDir, "src/main/java").getAbsolutePath() + "\n"
                + "packageName=" + pkg + "\n"
                + "output=" + outputFile.getAbsolutePath() + "\n";
        try {
            FileUtils.write(inputFile, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write gamebuilder binding", ex);
        }
    }

    /**
     * Resolves the game-builder editor through Maven &mdash; the self-contained artifact
     * {@code com.codenameone:codenameone-gamebuilder:jar-with-dependencies}. This mirrors
     * {@link AbstractCN1Mojo#getDesignerJar()}: in a released build the artifact is a
     * declared dependency of the plugin (found via {@link #getArtifact}); otherwise it is
     * resolved from the configured repositories at the plugin's own version. There is no
     * install script &mdash; the editor ships with the plugin from Maven Central.
     */
    private File getGameBuilderJar() throws MojoExecutionException, MojoFailureException {
        Artifact artifact = getArtifact("com.codenameone", "codenameone-gamebuilder", "jar-with-dependencies");
        if (artifact == null) {
            artifact = repositorySystem.createArtifactWithClassifier(
                    "com.codenameone", "codenameone-gamebuilder", pluginVersion(), "jar", "jar-with-dependencies");
        }
        File jar = getJar(artifact);
        if (jar == null || !jar.exists()) {
            throw new MojoFailureException(
                    "Could not resolve the game builder editor "
                    + "(com.codenameone:codenameone-gamebuilder:" + pluginVersion() + ":jar-with-dependencies).\n"
                    + "It is distributed through Maven Central alongside the Codename One plugin; make sure "
                    + "your build can reach Maven Central and is using a released plugin version.\n"
                    + "To work on the editor itself, run it straight from its module:\n"
                    + "    cd scripts/gamebuilder && mvn -Pexecutable-jar -pl javase -am package -Dcodename1.platform=javase\n"
                    + "    java -jar javase/target/codenameone-gamebuilder-*-jar-with-dependencies.jar");
        }
        return jar;
    }

    /** The Codename One plugin's own version, used as the game-builder editor's version. */
    private String pluginVersion() {
        if (pluginArtifacts != null) {
            for (Artifact a : pluginArtifacts) {
                if ("codenameone-maven-plugin".equals(a.getArtifactId())
                        && "com.codenameone".equals(a.getGroupId())) {
                    return a.getVersion();
                }
            }
        }
        return project.getProperties().getProperty("cn1.plugin.version",
                project.getProperties().getProperty("cn1.version", "8.0-SNAPSHOT"));
    }
}
