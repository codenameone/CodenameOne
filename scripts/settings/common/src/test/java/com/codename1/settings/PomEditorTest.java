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
package com.codename1.settings;

import com.codename1.settings.extensions.MavenDependency;
import com.codename1.settings.extensions.PomEditor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PomEditorTest {
    @Test
    public void ignoresPluginManagedProfileAndCommentDependencies() throws Exception {
        MavenDependency dep = new MavenDependency("example", "target", "1");
        String dependency = "<dependency><groupId>example</groupId><artifactId>target</artifactId></dependency>";
        String[] otherScopes = {
            "<build><plugins><plugin><dependencies/></plugin></plugins></build>",
            "<build><plugins><plugin><dependencies>" + dependency + "</dependencies></plugin></plugins></build>",
            "<dependencyManagement><dependencies>" + dependency + "</dependencies></dependencyManagement>",
            "<profiles><profile><dependencies>" + dependency + "</dependencies></profile></profiles>",
            "<!-- <dependencies>" + dependency + "</dependencies> -->",
            "<properties><example><![CDATA[<dependencies/>]]></example></properties>"
        };
        for (String other : otherScopes) {
            for (String projectDeps : new String[]{"", "<dependencies/>", "<dependencies />",
                    "<dependencies></dependencies>"}) {
                String pom = "<?xml version=\"1.0\"?><project note=\"a > b\">" + other + projectDeps + "</project>";
                assertFalse(PomEditor.containsDependency(pom, dep));
                assertEquals(pom, PomEditor.removeDependency(pom, dep));
                String installed = PomEditor.addDependency(pom, dep);
                assertTrue(installed.contains(other), "Unrelated XML must remain byte-for-byte intact");
                assertTrue(PomEditor.containsDependency(installed, dep));
                org.w3c.dom.Document document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(installed)));
                assertEquals("1", javax.xml.xpath.XPathFactory.newInstance().newXPath().evaluate(
                        "count(/project/dependencies/dependency[artifactId='target'])", document));
                assertEquals(installed, PomEditor.addDependency(installed, dep));
                String removed = PomEditor.removeDependency(installed, dep);
                assertTrue(removed.contains(other));
                assertFalse(PomEditor.containsDependency(removed, dep));
            }
        }
    }

    @Test
    public void removingInstalledLegacyDependencyPreservesCompactPom() {
        String pom = "<project><dependencies></dependencies></project>";
        MavenDependency dependency = new MavenDependency("example", "app-CN1JSON", "1", "pom");
        String installed = PomEditor.addDependency(pom, dependency);
        assertEquals(pom, PomEditor.removeDependency(installed, dependency));
    }

    @Test
    public void addsDependencyToExistingDependenciesBlock() {
        String pom = "<project>\n    <dependencies>\n    </dependencies>\n</project>\n";
        MavenDependency dep = new MavenDependency("com.codenameone", "cn1-demo", "1.0");
        String updated = PomEditor.addDependency(pom, dep);
        assertTrue(updated.contains("<groupId>com.codenameone</groupId>"));
        assertTrue(updated.contains("<artifactId>cn1-demo</artifactId>"));
        assertEquals(updated, PomEditor.addDependency(updated, dep));
    }

    @Test
    public void removesOnlyTheSelectedDependency() {
        String pom = "<project>\n  <dependencies>\n"
                + "    <dependency>\n      <groupId>com.example</groupId>\n      <artifactId>keep</artifactId>\n    </dependency>\n"
                + "    <dependency>\n      <groupId>com.codenameone</groupId>\n      <artifactId>remove</artifactId>\n    </dependency>\n"
                + "  </dependencies>\n</project>\n";
        MavenDependency dep = new MavenDependency("com.codenameone", "remove", "1.0");
        String updated = PomEditor.removeDependency(pom, dep);
        assertTrue(updated.contains("<artifactId>keep</artifactId>"));
        assertTrue(!updated.contains("<artifactId>remove</artifactId>"));
        assertEquals(updated, PomEditor.removeDependency(updated, dep));
    }

    @Test
    public void dependencyCoordinatesMustAppearInTheSameBlock() {
        String pom = "<project><dependencies>"
                + "<dependency><groupId>com.example</groupId><artifactId>one</artifactId></dependency>"
                + "<dependency><groupId>com.other</groupId><artifactId>target</artifactId></dependency>"
                + "</dependencies></project>";

        assertFalse(PomEditor.containsDependency(pom,
                new MavenDependency("com.example", "target", "1.0")));
    }

    @Test
    public void addsToProjectDependenciesInsteadOfDependencyManagementOrProfiles() {
        String pom = "<project>\n"
                + "  <dependencyManagement><dependencies><dependency>"
                + "<groupId>com.managed</groupId><artifactId>managed</artifactId>"
                + "</dependency></dependencies></dependencyManagement>\n"
                + "  <dependencies>\n  </dependencies>\n"
                + "  <profiles><profile><dependencies>\n  </dependencies></profile></profiles>\n"
                + "</project>\n";
        MavenDependency dep = new MavenDependency("com.codenameone", "cn1-demo", "1.0");

        String updated = PomEditor.addDependency(pom, dep);

        int inserted = updated.indexOf("<artifactId>cn1-demo</artifactId>");
        assertTrue(inserted > updated.indexOf("</dependencyManagement>"));
        assertTrue(inserted < updated.indexOf("<profiles>"));
    }
}
