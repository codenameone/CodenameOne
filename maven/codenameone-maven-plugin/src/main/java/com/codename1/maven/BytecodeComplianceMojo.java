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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks the compiled application against the Codename One Java runtime API; see
 * {@link BytecodeCompliance}, which holds the check and is shared with the Gradle plugin.
 */
@Mojo(name = "bytecode-compliance", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.TEST)
public class BytecodeComplianceMojo extends AbstractCN1Mojo {

    /// Where the port report of a desktop application is written: what works
    /// unchanged, what to change by source file, and what becomes of each
    /// dependency. Written whether the check passes or fails; the default is
    /// `target/codenameone/desktop-port-report.md`.
    @Parameter(property = "cn1.desktop.report")
    private java.io.File desktopReport;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        BytecodeCompliance check = new BytecodeCompliance(projectHost()) {
            @Override
            protected long sourcesModificationTime() throws IOException {
                // The common module's own sources, as the check always compared.
                return getSourcesModificationTime(true);
            }

            @Override
            protected void beforeCheck() {
                copyKotlinIncrementalCompileOutputToOutputDir();
            }
        };
        check.portReport(desktopReport);
        check.checkedDependencyRoots(plainSiblingModules());
        try {
            check.execute();
        } catch (com.codename1.build.BuildFailureException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        } catch (com.codename1.build.BuildExecutionException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex.getCause() == null ? ex : ex.getCause());
        }
    }

    /// The class directories of the plain jar modules of this build that the
    /// project depends on -- a `shared` module of DTOs and contracts. A Codename
    /// One module (an application or a cn1lib) is left out: it runs this check
    /// itself, on its own classes.
    ///
    /// Only what the application ships: a module on its compile or runtime
    /// class path. The project's references list every module it names,
    /// whatever the scope, and one named with `<scope>test</scope>` is a
    /// fixture that is free to use the whole JDK -- checked here, it failed the
    /// application's build over classes that never reach a device.
    private List<File> plainSiblingModules() {
        List<File> roots = new ArrayList<File>();
        if (project.getProjectReferences() == null) {
            return roots;
        }
        Set<String> shipped = new HashSet<String>();
        if (project.getArtifacts() != null) {
            for (Artifact artifact : project.getArtifacts()) {
                String scope = artifact.getScope();
                if (scope == null || Artifact.SCOPE_COMPILE.equals(scope)
                        || Artifact.SCOPE_RUNTIME.equals(scope)) {
                    shipped.add(artifact.getGroupId() + ":" + artifact.getArtifactId());
                }
            }
        }
        for (MavenProject sibling : project.getProjectReferences().values()) {
            if (sibling == null || !"jar".equals(sibling.getPackaging()) || sibling.getBasedir() == null) {
                continue;
            }
            if (!shipped.contains(sibling.getGroupId() + ":" + sibling.getArtifactId())) {
                continue;
            }
            if (new File(sibling.getBasedir(), "codenameone_settings.properties").exists()
                    || new File(sibling.getBasedir(), "codenameone_library_appended.properties").exists()) {
                continue;
            }
            File classes = new File(sibling.getBuild().getOutputDirectory());
            if (classes.isDirectory()) {
                roots.add(classes);
            }
        }
        return roots;
    }
}
