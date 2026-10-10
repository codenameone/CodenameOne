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

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which modules of the build the compliance check takes in beside the
/// project's own classes: the plain jar modules it depends on, which nothing
/// else checks, and not the Codename One modules, which check themselves.
class BytecodeComplianceSiblingModulesTest {

    @Test
    void onlyPlainJarModulesWithCompiledClassesAreChecked(@TempDir File tmp) throws Exception {
        MavenProject app = new MavenProject();
        app.setGroupId("com.example");
        app.setArtifactId("app-common");
        app.setVersion("1.0");

        MavenProject shared = sibling(tmp, "shared", "jar", true);
        MavenProject uncompiled = sibling(tmp, "uncompiled", "jar", false);
        MavenProject parent = sibling(tmp, "parent", "pom", true);
        MavenProject application = sibling(tmp, "common", "jar", true);
        assertTrue(new File(application.getBasedir(), "codenameone_settings.properties")
                .createNewFile());
        MavenProject library = sibling(tmp, "lib", "jar", true);
        assertTrue(new File(library.getBasedir(), "codenameone_library_appended.properties")
                .createNewFile());
        for (MavenProject sibling : new MavenProject[] {shared, uncompiled, parent, application, library}) {
            app.addProjectReference(sibling);
        }

        assertEquals(Collections.singletonList(new File(shared.getBuild().getOutputDirectory())),
                plainSiblingModules(app));
    }

    @Test
    void aProjectWithNoSiblingsChecksOnlyItself() throws Exception {
        MavenProject alone = new MavenProject();
        alone.setArtifactId("alone");
        assertTrue(plainSiblingModules(alone).isEmpty());
    }

    private static MavenProject sibling(File tmp, String artifactId, String packaging,
            boolean compiled) throws Exception {
        File basedir = new File(tmp, artifactId);
        File classes = new File(basedir, "target/classes");
        assertTrue(compiled ? classes.mkdirs() : basedir.mkdirs());
        MavenProject project = new MavenProject();
        project.setGroupId("com.example");
        project.setArtifactId(artifactId);
        project.setVersion("1.0");
        project.setPackaging(packaging);
        project.setBuild(new org.apache.maven.model.Build());
        project.setFile(new File(basedir, "pom.xml"));
        project.getBuild().setOutputDirectory(classes.getPath());
        return project;
    }

    @SuppressWarnings("unchecked")
    private static List<File> plainSiblingModules(MavenProject project) throws Exception {
        BytecodeComplianceMojo mojo = new BytecodeComplianceMojo();
        Field field = AbstractCN1Mojo.class.getDeclaredField("project");
        field.setAccessible(true);
        field.set(mojo, project);
        Method method = BytecodeComplianceMojo.class.getDeclaredMethod("plainSiblingModules");
        method.setAccessible(true);
        return (List<File>) method.invoke(mojo);
    }
}
