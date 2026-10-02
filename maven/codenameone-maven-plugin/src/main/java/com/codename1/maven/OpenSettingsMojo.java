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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import java.util.UUID;

/**
 * Opens the standalone Codename One Settings tool.
 *
 * <pre>mvn cn1:settings</pre>
 */
@Mojo(name = "settings")
public class OpenSettingsMojo extends AbstractCN1Mojo {
    /// How the tool starts, shared with the Gradle plugin's `settings` task.
    private static final DesktopTool TOOL = DesktopTool.SETTINGS;

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
        File inputFile = new File(TOOL.runtimeDir(), "settings-" + UUID.randomUUID() + ".input");
        writeBinding(inputFile, projectDir);

        List<File> classpath = resolveDesktopTool(TOOL, pluginVersion(),
                "To work on the Settings tool itself, run:\n"
                + "    cd scripts/settings && mvn -Pexecutable-jar -pl javase -am package -Dcodename1.platform=javase\n"
                + "    java -cp \"javase/target/codenameone-settings-*.jar:javase/target/libs/*\" "
                + "com.codename1.settings.CodenameOneSettingsLauncher");
        getLog().info("Launching Codename One Settings bound to " + projectDir);
        launchDesktopTool(TOOL, classpath, inputFile, projectDir, shouldSpawn(), null);
    }

    /**
     * Forwards {@code settings.*} system properties (screenshot capture, forced
     * section/dark-mode, etc.) from the Maven invocation to the spawned Settings
     * JVM so {@code mvn cn1:settings -Dsettings.screenshot=out.png} works. The
     * {@code settings.input} binding and {@code settings.spawn} launch flag are
     * owned by this mojo and never forwarded.
     */
    List<String> forwardedSettingsProperties() {
        return TOOL.forwardedProperties();
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

    List<String> desktopIdentityArgs(File jar, File runtimeDir) {
        return TOOL.identityArgs(jar, runtimeDir, MavenLog.of(getLog()));
    }

    File namedJavaLauncher(File runtimeDir) {
        return TOOL.javaLauncher(runtimeDir, MavenLog.of(getLog()));
    }

    File extractSettingsIcon(File jar, File runtimeDir) {
        return TOOL.extractIcon(jar, runtimeDir, MavenLog.of(getLog()));
    }

    void writeBinding(File inputFile, File projectDir) throws MojoExecutionException {
        File root = multimoduleRoot(projectDir);
        // No buildHintsDoc: the Settings tool used to scrape the developer guide's
        // AsciiDoc table at runtime and guess each hint's type from its description
        // prose. It now reads com.codename1.build.shared.BuildHints, which carries
        // the catalog's hints and the ones the annotations declare.
        String content = "# Codename One Settings project binding\n"
                + "projectDir=" + projectDir.getAbsolutePath() + "\n"
                + "settings=" + new File(projectDir, "codenameone_settings.properties").getAbsolutePath() + "\n"
                + "pom=" + new File(projectDir, "pom.xml").getAbsolutePath() + "\n"
                + "multimoduleRoot=" + root.getAbsolutePath() + "\n"
                // What Maven RESOLVED, so the tool does not have to infer it
                // from POM text. It has no model: it cannot evaluate a profile
                // activation, follow an inherited <sourceDirectory> or expand a
                // property, and every one of those has been a way for it to miss
                // the main class and then offer an annotation-owned hint for
                // editing. Its own reading stays as the fallback for a Settings
                // launched without these -- an older plugin, or the standalone
                // app.
                + bindingLines("sourceRoot",
                        compileSourceRoots(moduleAt(projectDir), userProperties()))
                + bindingValue("sourceEncoding",
                        sourceEncodingOf(moduleAt(projectDir), userProperties()))
                // The EFFECTIVE entry point, which is what process-annotations
                // stamps the manifest with and what the build hint merge expects.
                // `properties` carries any -Dcodename1.mainName; the settings
                // file does not, so a tool reading the file looked at a different
                // class than the build and reported the annotations on the
                // selected one as absent.
                + bindingValue("mainName", effective("codename1.mainName"))
                + bindingValue("packageName", effective("codename1.packageName"));
        try {
            FileUtils.write(inputFile, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write Settings binding", ex);
        }
    }

    /// The reactor module whose directory is `projectDir`, or the project being
    /// built when the reactor has no such module.
    ///
    /// `cn1:settings` is normally run from the root of a multi-module project
    /// while the module being EDITED is common, so the project in scope is not
    /// the one whose sources matter.
    private MavenProject moduleAt(File projectDir) {
        if (projectDir == null) {
            return null;
        }
        MavenSession session = getSession();
        List<MavenProject> projects = session == null ? null : session.getProjects();
        if (projects != null) {
            for (MavenProject candidate : projects) {
                if (isAt(candidate, projectDir)) {
                    return candidate;
                }
            }
        }
        // The project being built, but only when it IS this directory. Falling
        // back to it regardless published the platform module's compile roots as
        // the common module's -- `cn1:settings` run from javase or android
        // resolves the sibling common directory, which the reactor need not
        // contain -- and Settings takes resolved roots as authoritative, so it
        // never looked at the real POM and missed the annotated main source.
        // Saying nothing sends it back to its own reading, which is right.
        return isAt(project, projectDir) ? project : null;
    }

    /// Canonical, not merely absolute. `getCN1ProjectDir()` hands back paths
    /// shaped like `javase/../common`, which an absolute-path comparison never
    /// matches against a reactor module's own basedir -- so the module WAS in
    /// the session and the binding still said nothing about it, sending the tool
    /// back to a POM reading that cannot see an activated profile.
    private static boolean isAt(MavenProject candidate, File dir) {
        return candidate != null && candidate.getBasedir() != null
                && canonical(candidate.getBasedir()).equals(canonical(dir));
    }

    private static String canonical(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException ex) {
            return f.getAbsolutePath();
        }
    }

    /// One key of the settings Maven resolved, or null before `execute()` has
    /// loaded them -- which is how the tests drive `writeBinding` directly.
    private String effective(String key) {
        return properties == null ? null : properties.getProperty(key);
    }

    private static String bindingValue(String key, String value) {
        return value == null ? "" : key + "=" + value + "\n";
    }

    /// One line per value.
    ///
    /// Not a delimited list: every delimiter is legal in a path -- a colon in a
    /// Unix directory name, a semicolon in either -- so joining them means
    /// choosing an escaping scheme, and a path that happened to contain the
    /// separator would have been split into two roots that exist nowhere.
    private static String bindingLines(String key, List<String> values) {
        if (values == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                out.append(key).append('=').append(value.trim()).append('\n');
            }
        }
        return out.toString();
    }

    File multimoduleRoot(File projectDir) {
        File parent = projectDir == null ? null : projectDir.getParentFile();
        if (parent != null && "common".equals(projectDir.getName())) {
            return parent;
        }
        return projectDir == null ? new File(".") : projectDir;
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

}
