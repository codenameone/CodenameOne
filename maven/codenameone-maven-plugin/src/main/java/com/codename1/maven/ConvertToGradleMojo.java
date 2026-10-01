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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Copies this Maven (or Ant) Codename One application into a new directory as a
 * single-project Gradle build: {@code mvn cn1:convert-to-gradle}.
 *
 * <p>The original project is left untouched. The conversion is
 * {@link GradleConversion}: sources, resources and CSS move to {@code src/main},
 * each platform's native-interface implementations to {@code src/<platform>/<lang>},
 * a backend module to {@code backend/}, and the dependencies of
 * {@code common/pom.xml} into {@code build.gradle.kts}. A project that still uses
 * legacy {@code .cn1lib} files is refused with the list of them, since Gradle
 * projects only consume cn1libs from a Maven repository.</p>
 */
@Mojo(name = "convert-to-gradle", aggregator = true, requiresProject = false)
public class ConvertToGradleMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /** The project to convert; defaults to the one Maven runs in, else the working directory. */
    @Parameter(property = "cn1.sourceProject")
    private File sourceProject;

    /** Where the Gradle project goes; defaults to {@code <project>-gradle} beside it. */
    @Parameter(property = "cn1.outputDir")
    private File outputDir;

    /**
     * Carry over a backend module that is still the archetype's untouched skeleton.
     * Off by default; a backend with code of its own is always converted.
     */
    @Parameter(property = "cn1.includeBackend", defaultValue = "false")
    private boolean includeBackend;

    /** The plugin version the Gradle project declares; defaults to this plugin's. */
    @Parameter(property = "cn1.gradleVersion")
    private String cn1Version;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File source = sourceProject != null ? sourceProject
                : project != null && project.getBasedir() != null ? project.getBasedir() : new File(".");
        source = source.getAbsoluteFile();
        com.codename1.project.ProjectLayout layout = com.codename1.project.ProjectLayouts.detect(source);
        File root = layout == null ? source : layout.rootDir();
        File target = outputDir != null ? outputDir.getAbsoluteFile()
                : new File(root.getParentFile(), root.getName() + "-gradle");
        String version = cn1Version != null && cn1Version.length() > 0 ? cn1Version
                : com.codename1.maven.help.ToolingHelp.pluginVersion();
        try {
            new GradleConversion(MavenLog.of(getLog())).includeUntouchedBackend(includeBackend)
                    .convert(source, target, version);
        } catch (com.codename1.build.BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
        getLog().info("Next: cd " + target + " && ./gradlew run");
    }
}
