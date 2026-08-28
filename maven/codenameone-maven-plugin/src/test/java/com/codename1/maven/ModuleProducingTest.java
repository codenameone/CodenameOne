/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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

import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Which reactor module produced a classpath element.
 *
 * <p>It decides where the sources of the classes in that element are, and so
 * whether an annotated class is live or a leftover. Answering with the RUNNING
 * module -- a platform module, in the generated layout -- gives the application's
 * own classes no backing source at all, and a misplaced annotation on one of
 * them then reads as stale and is never reported.</p>
 */
public class ModuleProducingTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** The output directory, which is what a reactor `compile` build hands over. */
    @Test
    public void anOutputDirectoryIsMatched() throws Exception {
        File classes = tmp.newFolder("classes");
        MavenProject owner = moduleWriting(classes.getAbsolutePath(), null, null);
        assertSame(owner, resolve(owner, classes));
    }

    /**
     * ...and the JAR, which is what a reactor `package` build hands over.
     *
     * <p>That is the ordinary shape for the generated layout, and matching
     * directories alone sent every class in the jar back to the running module's
     * roots, where it has none.</p>
     */
    @Test
    public void aPackagedJarIsMatchedToo() throws Exception {
        File target = tmp.newFolder("target");
        File jar = new File(target, "myapp-common-1.0.jar");
        MavenProject owner = moduleWriting(new File(target, "classes").getAbsolutePath(),
                target.getAbsolutePath(), "myapp-common-1.0");
        assertSame(owner, resolve(owner, jar));
    }

    /** Something no reactor module produced is nobody's. */
    @Test
    public void anUnrelatedJarMatchesNothing() throws Exception {
        File target = tmp.newFolder("target");
        MavenProject owner = moduleWriting(new File(target, "classes").getAbsolutePath(),
                target.getAbsolutePath(), "myapp-common-1.0");
        assertNull(resolve(owner, new File(target, "somebody-else-2.0.jar")));
    }

    private static MavenProject moduleWriting(String outputDirectory, String buildDirectory,
                                              String finalName) {
        MavenProject project = new MavenProject();
        Build build = new Build();
        build.setOutputDirectory(outputDirectory);
        if (buildDirectory != null) {
            build.setDirectory(buildDirectory);
        }
        if (finalName != null) {
            build.setFinalName(finalName);
        }
        project.setBuild(build);
        return project;
    }

    /** Runs moduleProducing with `owner` in the reactor beside a running module. */
    private static MavenProject resolve(MavenProject owner, File element) throws Exception {
        CN1BuildMojo mojo = new CN1BuildMojo();
        MavenProject running = new MavenProject();
        running.setBuild(new Build());
        Field reactor = field(mojo.getClass(), "reactorProjects");
        reactor.setAccessible(true);
        reactor.set(mojo, Arrays.asList(running, owner));
        return mojo.moduleProducing(element);
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException keepLooking) {
                // up the chain
            }
        }
        throw new NoSuchFieldException(name);
    }
}
