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

/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
    /**
     * Marks the editor as launched for this reactor. Kept in the plugin context, which Maven scopes
     * to the session: a JVM-wide system property survived in a long lived Maven -- an IDE Maven
     * server or mvnd -- so every later invocation returned at the guard and the run configuration
     * appeared to do nothing until that process was restarted.
     */
    private static final String LAUNCHED_KEY = "com.codename1.maven.OpenGuiBuilderMojo.launched";

    /** The editor is compiled for this Java release, so an older forked JVM cannot load it. */
    private static final int REQUIRED_JAVA_VERSION = 8;

    /// How the editor starts, shared with the Gradle plugin's `guibuilder` task.
    /// It passes `--add-exports` only on Java 9 and newer: the option stops an 8
    /// JVM before it prints anything the user would see.
    private static final DesktopTool TOOL = DesktopTool.GUI_BUILDER;

    /** The invocation the guard belongs to. */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession guardSession;

    /** Identifies this plugin when asking the session for a context shared across the reactor. */
    @Parameter(defaultValue = "${plugin}", readonly = true)
    private PluginDescriptor guardPlugin;

    /** Optional fully-qualified form to select initially. */
    @Parameter(property = "className", required = false)
    private String className;

    @Parameter(property = "guibuilder.spawn", required = false, defaultValue = "true")
    private boolean spawn;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (alreadyLaunchedInThisSession()) {
            getLog().debug("Skipping guibuilder: already launched in this Maven invocation");
            return;
        }
        if (!isCN1ProjectDir()) {
            getLog().debug("Skipping guibuilder: not a Codename One project directory");
            return;
        }
        requireModernJdk();
        markLaunchedInThisSession();

        File projectDir = getCN1ProjectDir();
        File guiDir = new File(projectDir, "src" + File.separator + "main" + File.separator + "guibuilder");
        File sourceDir = new File(projectDir, "src" + File.separator + "main" + File.separator + "java");
        File cssFile = new File(projectDir, "src" + File.separator + "main" + File.separator + "css" + File.separator + "theme.css");
        guiDir.mkdirs();

        File input = new File(TOOL.runtimeDir(), "guibuilder-" + UUID.randomUUID() + ".input");
        writeBinding(input, projectDir, guiDir, sourceDir, cssFile);

        List<File> classpath = resolveDesktopTool(TOOL, pluginVersion(),
                "To work on the editor, run:\n"
                + "    cd scripts/guibuilder && mvn -Pexecutable-jar -pl javase -am package -Dcodename1.platform=javase");
        getLog().info("Launching Codename One GUI Builder bound to " + projectDir);
        launchDesktopTool(TOOL, classpath, input, projectDir, shouldSpawn(), null);
    }

    /**
     * The guard's home: one map per Maven invocation, shared by every project in the reactor.
     *
     * <p>getPluginContext() is indexed by the current project, and isCN1ProjectDir() accepts both
     * the aggregator and the app module, so running the goal from a generated multi-module root
     * gave each execution its own empty map and launched a second editor on the same files.
     * Keyed on the top level project instead, which is one map for the invocation and still not a
     * JVM-wide flag -- that was the previous bug, where a long lived Maven never launched again.
     *
     * @return the context to record the launch in, or null when there is none
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> launchGuardContext() {
        Map<String, Object> shared = sharedReactorContext();
        return shared != null ? shared : getPluginContext();
    }

    /**
     * The session's context for the top level project, which every project in the reactor resolves
     * to the same map.
     *
     * <p>Its own method so a test can supply one without building a MavenSession.
     *
     * @return the shared map, or null when there is no session to ask
     */
    Map<String, Object> sharedReactorContext() {
        if (guardSession == null || guardPlugin == null) return null;
        MavenProject top = guardSession.getTopLevelProject();
        return top == null ? null : guardSession.getPluginContext(guardPlugin, top);
    }

    /**
     * @return true when this reactor has already opened the editor
     */
    private boolean alreadyLaunchedInThisSession() {
        Map<String, Object> context = launchGuardContext();
        return context != null && Boolean.TRUE.equals(context.get(LAUNCHED_KEY));
    }

    private void markLaunchedInThisSession() {
        Map<String, Object> context = launchGuardContext();
        if (context != null) context.put(LAUNCHED_KEY, Boolean.TRUE);
    }

    /**
     * The GUI Builder is a Java 8 artifact. The spawned process writes only to its log file, so
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
        return TOOL.forwardedProperties();
    }

    /**
     * Names the process for the dock, taskbar and window manager, and opens the JDK packages the
     * JavaSE port needs on Java 9 and newer, matching cn1:settings and cn1:certificate-wizard.
     */
    List<String> desktopIdentityArgs() {
        // The GUI Builder ships no dock icon, so no jar or runtime directory is needed.
        return TOOL.identityArgs(null, null, MavenLog.of(getLog()));
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

}
