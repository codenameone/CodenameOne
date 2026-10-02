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

import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The minimal Maven layout: an application whose platform modules are absent, built by
 * {@code common}. See {@link HostedPlatforms}.
 */
class HostedPlatformsTest {
    private static final String[] PLATFORMS = {"javase", "android", "ios", "javascript", "win", "linux"};

    @TempDir
    File tmp;

    private File app() throws IOException {
        File root = new File(tmp, "app");
        write(new File(root, "pom.xml"), "<project/>");
        write(new File(root, "common/pom.xml"), "<project/>");
        write(new File(root, "common/codenameone_settings.properties"), "");
        return root;
    }

    private static void write(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void commonHostsAPlatformExactlyWhenItHasNoPom() throws IOException {
        File root = app();
        File common = new File(root, "common");
        assertTrue(HostedPlatforms.shouldHost(common, common, "android"));
        // Native sources without a pom are still hosted by common.
        new File(root, "android/src/main/java").mkdirs();
        assertTrue(HostedPlatforms.shouldHost(common, common, "android"));
        write(new File(root, "android/pom.xml"), "<project/>");
        assertFalse(HostedPlatforms.shouldHost(common, common, "android"),
                "a module of its own does the work; common must not build it twice");
        assertTrue(HostedPlatforms.shouldHost(common, common, "ios"));
    }

    @Test
    void onlyCommonEverHosts() throws IOException {
        File root = app();
        File common = new File(root, "common");
        assertFalse(HostedPlatforms.shouldHost(root, common, "android"), "the root aggregator");
        assertFalse(HostedPlatforms.shouldHost(new File(root, "javase"), common, "javase"));
        assertFalse(HostedPlatforms.shouldHost(common, null, "android"), "not inside an application");
    }

    @Test
    void theArgumentFileQuotesAndEscapes() {
        String args = HostedPlatforms.classpathArgFile(Arrays.asList(
                "C:\\Users\\Jo Smith\\.m2\\core.jar", "/tmp/a \"b\"/classes"));
        assertEquals("-classpath\n\"C:\\\\Users\\\\Jo Smith\\\\.m2\\\\core.jar" + File.pathSeparator
                + "/tmp/a \\\"b\\\"/classes\"\n", args);
    }

    @Test
    void anUpdateBelowTheHostedGoalsIsRefusedForAProjectThatUsesThem() {
        String hosted = "<goals><goal>compile-javase-natives</goal></goals>";
        assertTrue(UpdateCodenameOneMojo.tooOldForHostedPlatforms("7.0.274", hosted));
        assertFalse(UpdateCodenameOneMojo.tooOldForHostedPlatforms("7.0.275", hosted));
        assertFalse(UpdateCodenameOneMojo.tooOldForHostedPlatforms("7.1.0", hosted));
        assertFalse(UpdateCodenameOneMojo.tooOldForHostedPlatforms("7.0.200-SNAPSHOT", hosted),
                "a development build is not refused");
        assertFalse(UpdateCodenameOneMojo.tooOldForHostedPlatforms("7.0.200", "<goal>css</goal>"),
                "a project without the goals can move to any version");
        assertTrue(UpdateCodenameOneMojo.tooOldForHostedPlatforms("7.0.100",
                "<goal>hosted-platform</goal>"));
    }

    @Test
    void theDesktopJarKeepsTheJavaseModulesName() {
        assertEquals("myapp-javase-1.0", JavaSEExecutableJarMojo.javaseFinalName("myapp-common", "1.0"));
        assertEquals("other-javase-1.0", JavaSEExecutableJarMojo.javaseFinalName("other", "1.0"));
    }

    @Test
    void aHostedBuildUploadsWhatTheModuleWouldHavePackaged() throws IOException {
        File root = app();
        ProjectLayout layout = ProjectLayouts.detect(new File(root, "common"));
        File natives = new File(root, "common/target/cn1-javase/classes");
        assertEquals(Collections.singletonList(natives.getAbsolutePath()),
                CN1BuildMojo.hostedUploadElements(layout, "javase", natives));
        List<String> android = CN1BuildMojo.hostedUploadElements(layout, "android", natives);
        assertEquals(2, android.size());
        assertTrue(android.get(0).endsWith("android" + File.separator + "src" + File.separator + "main"
                + File.separator + "java"), android.get(0));
        assertTrue(android.get(1).endsWith("android" + File.separator + "src" + File.separator + "main"
                + File.separator + "resources"), android.get(1));
        assertTrue(CN1BuildMojo.hostedUploadElements(layout, "ios", natives).get(0)
                .endsWith("objectivec"));
        assertTrue(CN1BuildMojo.hostedUploadElements(layout, "nosuch", natives).isEmpty());
    }

    @Test
    void nativesRecompileWhenAnythingTheyReadChanges() throws Exception {
        File src = new File(tmp, "HelloImpl.java");
        write(src, "class HelloImpl {}");
        File cp = new File(tmp, "classes");
        cp.mkdirs();
        File resources = new File(tmp, "resources");
        List<File> sources = Collections.singletonList(src);
        String inputs = CompileJavaSENativesMojo.describeInputs(sources,
                Collections.singletonList(cp.getAbsolutePath()), new String[] {null, "1.8", "1.8"});
        File stamp = new File(tmp, "inputs.txt");
        assertFalse(CompileJavaSENativesMojo.isUpToDate(stamp, inputs, sources, resources), "never compiled");
        write(stamp, inputs);
        long now = System.currentTimeMillis();
        stamp.setLastModified(now);
        src.setLastModified(now - 10000);
        cp.setLastModified(now - 10000);
        assertTrue(CompileJavaSENativesMojo.isUpToDate(stamp, inputs, sources, resources));
        src.setLastModified(now + 10000);
        assertFalse(CompileJavaSENativesMojo.isUpToDate(stamp, inputs, sources, resources), "an edited source");
        src.setLastModified(now - 10000);
        String moreSources = CompileJavaSENativesMojo.describeInputs(Arrays.asList(src, new File(tmp, "B.java")),
                Collections.singletonList(cp.getAbsolutePath()), new String[] {null, "1.8", "1.8"});
        assertFalse(CompileJavaSENativesMojo.isUpToDate(stamp, moreSources, sources, resources), "a new source");
        File appClass = new File(cp, "App.class");
        write(appClass, "x");
        appClass.setLastModified(now + 10000);
        assertFalse(CompileJavaSENativesMojo.isUpToDate(stamp, inputs, sources, resources),
                "the application classes the natives link against changed");
    }

    /**
     * The fragment both generators put in common/pom.xml. Each hosted goal must carry
     * {@code <hostedPlatform>} and every other plugin's execution must read the skip property
     * one of them sets, or a project with the module would run that work twice; each
     * build profile must switch on for its own platform only while that module is absent.
     */
    @Test
    void theHostedProfilesStepAsideForAModule() throws Exception {
        File fragment = new File("../../scripts/initializr/common/src/main/resources/common-hosted-platform-profiles.xml");
        String xml = new String(Files.readAllBytes(fragment.toPath()), StandardCharsets.UTF_8);
        Element profiles = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(("<profiles>" + xml + "</profiles>").getBytes(StandardCharsets.UTF_8)))
                .getDocumentElement();
        Set<String> ids = new HashSet<String>();
        Set<String> hostProfiles = new HashSet<String>();
        for (Element profile : children(profiles, "profile")) {
            String id = text(profile, "id");
            assertTrue(ids.add(id), "duplicate profile " + id);
            Element activation = child(profile, "activation");
            if (id.startsWith("cn1-host-")) {
                String platform = id.substring("cn1-host-".length());
                hostProfiles.add(platform);
                assertEquals(platform, text(child(activation, "property"), "value"), id);
                assertEquals("${basedir}/../" + platform + "/pom.xml", text(child(activation, "file"), "missing"), id);
            }
            Element build = child(profile, "build");
            Element plugins = build == null ? null : child(build, "plugins");
            if (plugins == null) {
                continue;
            }
            for (Element plugin : children(plugins, "plugin")) {
                Element executions = child(plugin, "executions");
                if (executions == null) {
                    continue;
                }
                boolean ours = "codenameone-maven-plugin".equals(text(plugin, "artifactId"));
                for (Element execution : children(executions, "execution")) {
                    Element config = child(execution, "configuration");
                    assertNotNull(config, id + "/" + text(execution, "id") + " has no configuration");
                    if (ours) {
                        assertNotNull(text(config, "hostedPlatform"), id + "/" + text(execution, "id")
                                + " runs even when the platform has a module of its own");
                    } else {
                        String skip = text(config, "skip");
                        assertTrue(skip != null && skip.matches("\\$\\{cn1\\.hosted\\.[a-z]+\\.skip}"),
                                id + "/" + text(execution, "id") + " must skip when a module does the work");
                    }
                }
            }
        }
        assertEquals(new HashSet<String>(Arrays.asList(PLATFORMS)), hostProfiles);
        assertTrue(ids.contains("simulator"));

        // The archetype replaces its own simulator profile with this fragment; a second
        // profile of that id fails every build.
        String common = new String(Files.readAllBytes(new File(
                "../cn1app-archetype/src/main/resources/archetype-resources/common/pom.xml").toPath()),
                StandardCharsets.UTF_8);
        assertTrue(common.contains("<!-- @CN1_HOSTED_PLATFORM_PROFILES@ -->"));
        assertFalse(common.contains("<id>simulator</id>"));
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> out = new ArrayList<Element>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n instanceof Element && name.equals(((Element) n).getTagName())) {
                out.add((Element) n);
            }
        }
        return out;
    }

    private static Element child(Element parent, String name) {
        List<Element> c = children(parent, name);
        return c.isEmpty() ? null : c.get(0);
    }

    private static String text(Element parent, String name) {
        Element c = child(parent, name);
        return c == null ? null : c.getTextContent().trim();
    }
}
