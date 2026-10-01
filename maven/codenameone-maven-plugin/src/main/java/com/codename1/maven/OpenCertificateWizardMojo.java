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

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Launches the standalone Certificate Wizard for a Codename One project.
 *
 * <pre>mvn cn1:certificatewizard</pre>
 */
@Mojo(name = "certificatewizard")
public class OpenCertificateWizardMojo extends AbstractCN1Mojo {
    /// How the wizard starts, shared with the Gradle plugin's `certificateWizard` task.
    private static final DesktopTool TOOL = DesktopTool.CERTIFICATE_WIZARD;

    private static final String LAUNCHED_PROPERTY =
            "com.codename1.maven.OpenCertificateWizardMojo.launched";

    @Parameter(property = "token", required = false)
    private String token;

    @Parameter(property = "user", required = false)
    private String user;

    @Parameter(property = "baseUrl", required = false, defaultValue = "https://cloud.codenameone.com")
    private String baseUrl;

    @Parameter(property = "outputDir", required = false)
    private File outputDir;

    @Parameter(property = "certificatewizard.login", required = false, defaultValue = "true")
    private boolean login;

    @Parameter(property = "certificatewizard.loginTimeoutSeconds", required = false, defaultValue = "180")
    private int loginTimeoutSeconds;

    @Parameter(property = "certificatewizard.spawn", required = false, defaultValue = "true")
    private boolean spawn;

    @Override
    protected String helpStep() {
        return "configure";
    }

    @Override
    protected String helpAction() {
        return "mvn cn1:certificatewizard";
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (Boolean.getBoolean(LAUNCHED_PROPERTY)) {
            getLog().debug("Skipping certificatewizard: already launched in this Maven invocation");
            return;
        }
        if (!isCN1ProjectDir()) {
            getLog().debug("Skipping certificatewizard: not a CN1 project dir");
            return;
        }
        System.setProperty(LAUNCHED_PROPERTY, "true");

        File projectDir = getCN1ProjectDir();
        if (outputDir == null) {
            outputDir = new File(projectDir, "iosCerts");
        }
        outputDir.mkdirs();

        // See CodenameOneLogin, shared with the Gradle plugin.
        CodenameOneLogin.LoginResult signIn = new CodenameOneLogin(MavenLog.of(getLog()))
                .resolve(token, user, login, loginTimeoutSeconds, baseUrl);
        String effectiveToken = signIn.token;
        String effectiveUser = signIn.user;
        File runtimeDir = TOOL.runtimeDir();
        File inputFile = new File(runtimeDir, "certificatewizard.input");
        File outputFile = new File(runtimeDir, UUID.randomUUID().toString() + ".output");
        writeBinding(inputFile, projectDir, outputDir, outputFile, effectiveUser, effectiveToken);

        List<File> classpath = resolveDesktopTool(TOOL, pluginVersion(),
                "To work on the wizard itself, run:\n"
                + "    cd scripts/certificatewizard && mvn -Pexecutable-jar -pl javase -am package -Dcodename1.platform=javase\n"
                + "    java -cp \"javase/target/codenameone-certificatewizard-*.jar:javase/target/libs/*\" "
                + "com.codename1.certificatewizard.CertificateWizardLauncher");
        getLog().info("Launching certificate wizard bound to " + projectDir);
        if (effectiveToken.length() == 0) {
            getLog().warn("No Codename One bearer token was found. The wizard will open in offline mode unless "
                    + "you pass -Dtoken=<keycloak-jwt> or allow -Dcertificatewizard.login=true.");
        }

        launchDesktopTool(TOOL, classpath, inputFile, projectDir, shouldSpawn(), null);
    }

    @Override
    protected boolean isCN1ProjectDir() {
        File cn1ProjectDir = getCN1ProjectDir();
        if (cn1ProjectDir == null || project == null || project.getBasedir() == null) {
            getLog().debug("Skipping certificatewizard: not a CN1 project dir");
            return false;
        }
        try {
            File current = project.getBasedir().getCanonicalFile();
            File cn1 = cn1ProjectDir.getCanonicalFile();
            if (cn1.equals(current)) {
                return true;
            }
            File rootCommon = new File(current, "common").getCanonicalFile();
            if (cn1.equals(rootCommon)) {
                return true;
            }
        } catch (IOException ex) {
            getLog().error("Failed to get canonical paths for project dir", ex);
        }
        getLog().debug("Skipping certificatewizard: not a CN1 project dir");
        return false;
    }

    private boolean shouldSpawn() {
        String legacySpawn = System.getProperty("spawn");
        if (legacySpawn != null) {
            return Boolean.parseBoolean(legacySpawn);
        }
        return spawn;
    }

    File namedJavaLauncher(File runtimeDir) {
        return TOOL.javaLauncher(runtimeDir, MavenLog.of(getLog()));
    }

    List<String> desktopIdentityArgs(File jar, File runtimeDir) {
        return TOOL.identityArgs(jar, runtimeDir, MavenLog.of(getLog()));
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

    File extractWizardIcon(File jar, File runtimeDir) {
        return TOOL.extractIcon(jar, runtimeDir, MavenLog.of(getLog()));
    }

    private void writeBinding(File inputFile, File projectDir, File outDir, File outputFile,
                              String effectiveUser, String effectiveToken) throws MojoExecutionException {
        String content = "# Codename One certificate wizard project binding\n"
                + "projectDir=" + projectDir.getAbsolutePath() + "\n"
                + "settings=" + new File(projectDir, "codenameone_settings.properties").getAbsolutePath() + "\n"
                + "outputDir=" + outDir.getAbsolutePath() + "\n"
                + "output=" + outputFile.getAbsolutePath() + "\n"
                + "user=" + effectiveUser + "\n"
                + "token=" + effectiveToken + "\n"
                + "baseUrl=" + baseUrl + "\n";
        try {
            FileUtils.write(inputFile, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write certificatewizard binding", ex);
        }
    }


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

    static boolean isUsableJwt(String token) {
        return CodenameOneLogin.isUsableJwt(token);
    }
}
