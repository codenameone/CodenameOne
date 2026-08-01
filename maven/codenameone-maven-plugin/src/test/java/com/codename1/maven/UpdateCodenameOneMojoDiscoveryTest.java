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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * cn1:update must never offer a version the project cannot resolve. Discovery moved to
 * the Codename One repository so releases made after Maven Central stops receiving them
 * are visible, but a project whose pom does not declare that repository would then be
 * handed a version its next build cannot fetch.
 */
public class UpdateCodenameOneMojoDiscoveryTest {

    private static final String R2_URL =
            "https://repo.codenameone.com/maven2/com/codenameone/codenameone-maven-plugin/maven-metadata.xml";

    private static ArtifactRepository repo(String id, String url) {
        MavenArtifactRepository r = new MavenArtifactRepository();
        r.setId(id);
        r.setUrl(url);
        return r;
    }

    private static boolean canResolve(List<ArtifactRepository> dependencyRepos,
                                      List<ArtifactRepository> pluginRepos) {
        return UpdateCodenameOneMojo.canResolveFrom(R2_URL, dependencyRepos, pluginRepos);
    }

    private static List<ArtifactRepository> central() {
        return new ArrayList<ArtifactRepository>(Arrays.asList(
                repo("central", "https://repo.maven.apache.org/maven2")));
    }

    private static List<ArtifactRepository> centralAndCodenameOne() {
        return new ArrayList<ArtifactRepository>(Arrays.asList(
                repo("central", "https://repo.maven.apache.org/maven2"),
                repo("codenameone", "https://repo.codenameone.com/maven2")));
    }

    @Test
    public void refusesWhenTheProjectDeclaresOnlyCentral() {
        assertFalse("A project that cannot reach the Codename One repository must not be "
                        + "offered versions that only exist there",
                canResolve(central(), central()));
    }

    @Test
    public void acceptsWhenBothRepositoryKindsDeclareIt() {
        assertTrue(canResolve(centralAndCodenameOne(), centralAndCodenameOne()));
    }

    @Test
    public void refusesWhenOnlyDependenciesDeclareIt() {
        // The framework artifacts resolve from <repositories> but the plugin itself
        // resolves from <pluginRepositories>, so half the configuration is unusable.
        assertFalse(canResolve(centralAndCodenameOne(), central()));
    }

    @Test
    public void refusesWhenOnlyPluginsDeclareIt() {
        assertFalse(canResolve(central(), centralAndCodenameOne()));
    }
}
