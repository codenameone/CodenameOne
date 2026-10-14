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

import com.codename1.build.BuildFailureException;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.*;

public class MainClassSourceMappingTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private MavenProject module(String name) throws Exception {
        File dir = tmp.newFolder(name);
        MavenProject project = new MavenProject();
        project.setFile(new File(dir, "pom.xml"));
        project.setBuild(new Build());
        project.getBuild().setDirectory(new File(dir, "target").getPath());
        project.getBuild().setOutputDirectory(new File(dir, "target/classes").getPath());
        project.getBuild().setFinalName(name);
        File source = new File(dir, "custom-sources");
        assertTrue(source.mkdirs());
        project.addCompileSourceRoot(source.getPath());
        project.getProperties().setProperty("project.build.sourceEncoding", "UTF-8");
        return project;
    }

    @Test
    public void commonModuleSourcesAreUsedForDirectoriesAndPackagedJars() throws Exception {
        MavenProject common = module("common");
        MavenProject android = module("android");
        CN1BuildMojo mojo = new CN1BuildMojo();
        mojo.project = android;
        Field reactor = AbstractCN1Mojo.class.getDeclaredField("reactorProjects");
        reactor.setAccessible(true);
        reactor.set(mojo, Arrays.asList(android, common));

        File source = new File(common.getCompileSourceRoots().get(0), "MyApp.java");
        Files.write(source.toPath(), "package example; public class MyApp {}".getBytes(StandardCharsets.UTF_8));
        File classes = new File(common.getBuild().getOutputDirectory());
        assertTrue(classes.mkdirs());
        assertEquals(0, javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "-d", classes.getPath(), source.getPath()));
        File jar = new File(common.getBuild().getDirectory(), "common.jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            out.putNextEntry(new JarEntry("example/MyApp.class"));
            out.write(Files.readAllBytes(new File(classes, "example/MyApp.class").toPath()));
            out.closeEntry();
        }
        Properties settings = new Properties();
        settings.setProperty("codename1.packageName", "example");
        settings.setProperty("codename1.mainName", "MyApp");
        AnnotationBuildHints.Sources sources = mojo.mainClassSources();
        for (File input : Arrays.asList(classes, jar)) {
            assertTrue(sources.rootsFor(input).contains(source.getParent()));
            assertEquals("UTF-8", sources.encodingFor(input));
            MainClassPreflight.validate(jar, settings, Collections.singletonList(input), sources);
        }
        Files.delete(source.toPath());
        for (File input : Arrays.asList(classes, jar)) {
            try {
                MainClassPreflight.validate(jar, settings, Collections.singletonList(input), sources);
                fail("The orphan in the common module must be rejected: " + input);
            } catch (BuildFailureException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().contains("compiled class is stale"));
            }
        }
        File binaryDependency = tmp.newFile("installed-library.jar");
        assertTrue(sources.rootsFor(binaryDependency).isEmpty());
    }
}
