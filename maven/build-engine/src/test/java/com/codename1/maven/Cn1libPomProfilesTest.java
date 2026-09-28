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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Cn1libPomProfilesTest {
    /// Shaped exactly like a deployed cn1lib-archetype `lib/pom.xml`: nothing
    /// interpolated, `cn1lib.name` defined only in the parent.
    private static final String LIB_POM = "<project>\n"
            + "  <parent><groupId>com.acme</groupId><artifactId>maps</artifactId><version>1.2</version></parent>\n"
            + "  <groupId>com.acme</groupId><artifactId>maps-lib</artifactId><version>1.2</version>\n"
            + "  <packaging>pom</packaging>\n"
            + "  <dependencies>\n"
            + "    <dependency><groupId>${project.groupId}</groupId><artifactId>${cn1lib.name}-common</artifactId>"
            + "<version>${project.version}</version></dependency>\n"
            + "  </dependencies>\n"
            + "  <profiles>\n"
            + "    <profile><id>android</id><activation><property><name>codename1.platform</name>"
            + "<value>android</value></property></activation>\n"
            + "      <dependencies><dependency><groupId>${project.groupId}</groupId>"
            + "<artifactId>${cn1lib.name}-android</artifactId><version>${project.version}</version></dependency>"
            + "</dependencies></profile>\n"
            + "    <profile><id>ios</id><activation><property><name>codename1.platform</name>"
            + "<value>ios</value></property></activation>\n"
            + "      <dependencies><dependency><groupId>${project.groupId}</groupId>"
            + "<artifactId>${cn1lib.name}-ios</artifactId><version>${project.version}</version></dependency>"
            + "<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4</version>"
            + "<scope>test</scope></dependency></dependencies></profile>\n"
            + "    <profile><id>release</id><activation><activeByDefault>true</activeByDefault></activation>"
            + "</profile>\n"
            + "  </profiles>\n"
            + "</project>\n";

    private static final String PARENT_POM = "<project><groupId>com.acme</groupId><artifactId>maps</artifactId>"
            + "<version>1.2</version><properties><cn1lib.name>maps</cn1lib.name></properties></project>";

    @Test
    void readsEachPlatformProfileWithTheParentsProperties() {
        Map<String, List<Cn1libPomProfiles.Coordinate>> byPlatform = Cn1libPomProfiles.read(LIB_POM,
                (g, a, v) -> "com.acme".equals(g) && "maps".equals(a) && "1.2".equals(v) ? PARENT_POM : null);
        assertEquals(2, byPlatform.size());
        assertEquals("com.acme:maps-android:1.2", byPlatform.get("android").get(0).toNotation());
        List<Cn1libPomProfiles.Coordinate> ios = byPlatform.get("ios");
        assertEquals(1, ios.size(), "test-scoped dependencies are not part of the platform");
        assertEquals("com.acme:maps-ios:1.2", ios.get(0).toNotation());
    }

    @Test
    void anUnresolvablePropertyIsLeftVisibleRatherThanDropped() {
        Map<String, List<Cn1libPomProfiles.Coordinate>> byPlatform = Cn1libPomProfiles.read(LIB_POM, (g, a, v) -> null);
        assertEquals("${cn1lib.name}-android", byPlatform.get("android").get(0).artifactId);
    }

    @Test
    void notACn1libOrNotXml() {
        assertTrue(Cn1libPomProfiles.read("<project><groupId>a</groupId></project>", null).isEmpty());
        assertTrue(Cn1libPomProfiles.read("not xml", null).isEmpty());
        assertTrue(Cn1libPomProfiles.read(null, null).isEmpty());
    }

    @Test
    void classifierAndTypeBecomeGradleNotation() {
        String pom = "<project><groupId>g</groupId><artifactId>a</artifactId><version>1</version><profiles>"
                + "<profile><activation><property><name>codename1.platform</name><value>javase</value></property>"
                + "</activation><dependencies><dependency><groupId>g</groupId><artifactId>x</artifactId>"
                + "<version>${project.version}</version><classifier>natives</classifier><type>zip</type>"
                + "</dependency></dependencies></profile></profiles></project>";
        assertEquals("g:x:1:natives@zip", Cn1libPomProfiles.read(pom, null).get("javase").get(0).toNotation());
    }

    /// A cn1lib's common module names the cn1libs it uses as pom-type
    /// dependencies; that is how a consumer reaches their platform profiles.
    @Test
    void mainDependenciesExposeTheCn1libsALibraryUses() {
        String common = "<project><groupId>com.acme</groupId><artifactId>maps-common</artifactId>"
                + "<version>1.2</version><properties><geo.version>3.0</geo.version></properties><dependencies>"
                + "<dependency><groupId>com.acme</groupId><artifactId>geo-lib</artifactId>"
                + "<version>${geo.version}</version><type>pom</type></dependency>"
                + "<dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4</version>"
                + "<scope>test</scope></dependency>"
                + "</dependencies><profiles><profile><activation><property><name>codename1.platform</name>"
                + "<value>ios</value></property></activation><dependencies><dependency><groupId>x</groupId>"
                + "<artifactId>y</artifactId><version>1</version></dependency></dependencies></profile></profiles>"
                + "</project>";
        List<Cn1libPomProfiles.Coordinate> deps = Cn1libPomProfiles.dependencies(common, null);
        assertEquals(1, deps.size(), "test scope and profile dependencies are not main dependencies");
        assertEquals("com.acme:geo-lib:3.0@pom", deps.get(0).toNotation());
        assertTrue(Cn1libPomProfiles.dependencies("not xml", null).isEmpty());
    }

    /// A library built with Gradle is consumed through the same pom shape, so what
    /// the Gradle plugin writes must read back as the Maven archetype's does.
    @Test
    void theRenderedLibPomReadsBackPlatformByPlatform() {
        String pom = Cn1libPom.render("com.acme", "maps", "2.0",
                java.util.Arrays.asList("android", "ios", "javase"), true);
        Map<String, List<Cn1libPomProfiles.Coordinate>> byPlatform = Cn1libPomProfiles.read(pom, null);
        assertEquals(3, byPlatform.size());
        assertEquals("com.acme:maps-ios:2.0", byPlatform.get("ios").get(0).toNotation());
        assertTrue(pom.contains("<artifactId>maps-lib</artifactId>"));
        assertTrue(pom.contains("<classifier>cn1css</classifier>"));
    }
}
