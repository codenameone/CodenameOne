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

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Cn1AppArchetypeCertificateWizardTest {

    @Test
    void certificateWizardBindingsAreIncludedInPackagedProjectTemplates() throws Exception {
        assertContains("tools/netbeans/nbactions.xml", "CUSTOM-Open Certificate Wizard");
        assertContains("tools/netbeans/nbactions.xml", "<goal>cn1:certificatewizard</goal>");
        assertContains("tools/eclipse/__mainName__ - Certificate Wizard.launch", "cn1:certificatewizard");
        assertContains(".idea/runConfigurations/CN1_Certificate_Wizard.xml", "cn1:certificatewizard");
        assertContains(".vscode/settings.json", "Tools > Certificate Wizard");
        assertContains(".vscode/settings.json", "cn1:certificatewizard");
        assertContains("mvnconfig.toml", "[certificate_wizard]");
        assertContains("mvnconfig.toml", "cn1:certificatewizard");
        assertContains("run.sh", "cn1:certificatewizard");
        assertContains("run.bat", "cn1:certificatewizard");
        assertContains("README.adoc", "mvn cn1:certificatewizard");
    }

    @Test
    void localJavaScriptBindingsAreIncludedInPackagedProjectTemplates() throws Exception {
        assertContains("javascript/pom.xml", "<codename1.defaultBuildTarget>local-javascript</codename1.defaultBuildTarget>");
        assertContains("mvnconfig.toml", "[javascript_local]");
        assertContains("mvnconfig.toml", "-Dcodename1.buildTarget=local-javascript");
        assertContains("build.sh", "function javascript_cloud");
        assertContains("build.sh", "-Dcodename1.buildTarget=local-javascript");
        assertContains("build.bat", ":javascript_cloud");
        assertContains("build.bat", "-Dcodename1.buildTarget^=local-javascript");
        assertContains("tools/netbeans/nb-configuration.xml", "<configuration id=\"Local JavaScript App\"");
        assertContains("tools/netbeans/nb-configuration.xml", "<property name=\"codename1.buildTarget\">local-javascript</property>");
        assertContains("tools/eclipse/__mainName__ - Build Javascript Locally.launch", "codename1.buildTarget=local-javascript");
        assertContains(".idea/runConfigurations/CN1_JavaScript_Local_Build.xml", "value=\"local-javascript\"");
        assertContains(".vscode/settings.json", "Local > JavaScript Build");
        assertContains(".vscode/settings.json", "-Dcodename1.buildTarget=local-javascript");
    }

    /**
     * A generated project must offer both halves of iOS on-device debugging
     * from the IDE: the build that turns the debug listener on, and the proxy
     * the IDE attaches through.
     *
     * NetBeans had neither, so the two goals had to be typed by hand and the
     * build one silently did nothing unless the developer had already edited
     * codenameone_settings.properties (issue #5333). IntelliJ shipped the
     * proxy and the attach config but not the build.
     */
    @Test
    void iosOnDeviceDebugBindingsAreIncludedInPackagedProjectTemplates() throws Exception {
        // The build goal forces the onDeviceDebug hint on for one build.
        assertContains("tools/netbeans/nbactions.xml", "CUSTOM-Build for iOS On-Device Debug");
        assertContains("tools/netbeans/nbactions.xml", "<goal>cn1:buildIosOnDeviceDebug</goal>");
        assertContains(".idea/runConfigurations/CN1_iOS_OnDeviceDebug.xml",
                "cn1:buildIosOnDeviceDebug");

        // The proxy bridges the device's wire protocol to JDWP.
        assertContains("tools/netbeans/nbactions.xml", "CUSTOM-Start iOS Debug Proxy");
        assertContains("tools/netbeans/nbactions.xml", "<goal>cn1:ios-on-device-debugging</goal>");
        assertContains(".idea/runConfigurations/CN1_Debug_Proxy.xml",
                "cn1:ios-on-device-debugging");
    }

    /**
     * Generated run configurations must use the goal prefix the plugin
     * actually registers.
     *
     * The descriptor sets {@code <goalPrefix>cn1</goalPrefix>}, so a
     * configuration written against the artifact-derived {@code codenameone}
     * prefix cannot resolve the plugin and the action fails to start. The
     * NetBeans actions and the certificate-wizard configuration were already
     * on {@code cn1}; the on-device-debug ones were not.
     */
    @Test
    void generatedRunConfigurationsUseTheRegisteredGoalPrefix() throws Exception {
        String descriptor = pluginDescriptor();
        assertTrue(descriptor.contains("<goalPrefix>cn1</goalPrefix>"),
                "this test pins the prefix the plugin registers; update it if that changes");

        File dir = new File("../cn1app-archetype/src/main/resources/archetype-resources/.idea/runConfigurations");
        File[] configs = dir.listFiles();
        assertTrue(configs != null && configs.length > 0, "expected run configurations at " + dir);
        for (File config : configs) {
            String body = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);
            assertFalse(body.contains("codenameone:"),
                    config.getName() + " uses the unregistered 'codenameone' prefix,"
                            + " so the action cannot resolve the plugin");
        }
    }

    /**
     * A generated Java 17 project must hand an agent the skill through all three
     * discovery conventions, and through the same paths the Initializr uses:
     * {@code AGENTS.md} at the root, the skill itself under
     * {@code .agent-skills/codename-one/}, and a thin redirect stub at
     * {@code .claude/skills/codename-one/SKILL.md}.
     *
     * This module shipped the full skill under {@code .claude/} alone (issue #5699),
     * which left an archetype-generated project with no root pointer and no
     * vendor-neutral copy -- so Codex and anything else that does not happen to know
     * Claude Code's directory layout never found it, while the identical project
     * downloaded from the Initializr was fine.
     *
     * All three come from one source directory, staged into the archetype JAR at build
     * time, so there is nothing under archetype-resources/ to read here. What is worth
     * pinning is that the wiring still names those paths, and that the two shared files
     * are still where the pom expects them.
     */
    @Test
    void agentSkillIsGeneratedInTheSameLayoutAsTheInitializr() throws Exception {
        File initializrResources =
                new File("../../scripts/initializr/common/src/main/resources");
        // Stored under a name that is NOT AGENTS.md on purpose: that name is one agents
        // look for by themselves, and a file called that inside this repository would be
        // read as instructions for the Codename One tree rather than for a generated app.
        assertTrue(new File(initializrResources, "agent-skill-agents-md.md").isFile(),
                "the archetype pom stages the AGENTS.md body from " + initializrResources);
        assertFalse(new File(initializrResources, "AGENTS.md").isFile(),
                "the AGENTS.md body must not be stored under the reserved name");
        assertTrue(new File(initializrResources, "agent-skill-claude-stub.md").isFile(),
                "the archetype pom stages the Claude stub from " + initializrResources);
        assertTrue(new File(initializrResources, "skill/SKILL.md").isFile(),
                "the archetype pom stages the skill itself from " + initializrResources);

        String pom = archetypeFile("pom.xml");
        assertTrue(pom.contains("archetype-resources/.agent-skills/codename-one"),
                "the skill must be staged under .agent-skills/, not only under .claude/");
        assertTrue(pom.contains("archetype-resources/AGENTS.md"),
                "the root AGENTS.md pointer must be staged into the archetype");
        assertTrue(pom.contains("archetype-resources/.claude/skills/codename-one/SKILL.md"),
                "the Claude stub must be staged at its single-file path");

        String metadata = archetypeFile("src/main/resources/META-INF/maven/archetype-metadata.xml");
        for (String declared : new String[] {
                "<directory>.agent-skills</directory>",
                "<directory>.claude</directory>",
                "<include>AGENTS.md</include>" }) {
            assertTrue(metadata.contains(declared),
                    "archetype-metadata.xml must extract " + declared);
        }

        // Java 8 projects get none of it: the skill's guidance is Java 17 throughout.
        String postGenerate = archetypeFile("src/main/resources/META-INF/archetype-post-generate.groovy");
        for (String stripped : new String[] { "\".claude\"", "\".agent-skills\"", "\"AGENTS.md\"" }) {
            assertTrue(postGenerate.contains(stripped),
                    "archetype-post-generate.groovy must strip " + stripped + " for Java 8");
        }
    }

    private static String archetypeFile(String path) throws Exception {
        File file = new File("../cn1app-archetype", path);
        assertTrue(file.isFile(), "Missing archetype file " + file.getAbsolutePath());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    private static String pluginDescriptor() throws Exception {
        File pom = new File("pom.xml");
        assertTrue(pom.isFile(), "expected the plugin pom at " + pom.getAbsolutePath());
        return new String(Files.readAllBytes(pom.toPath()), StandardCharsets.UTF_8);
    }

    private static void assertContains(String path, String expected) throws Exception {
        String content = archetypeResource("archetype-resources/" + path);
        assertTrue(content.contains(expected), path + " should contain " + expected);
    }

    private static String archetypeResource(String path) throws Exception {
        File file = new File("../cn1app-archetype/src/main/resources", path);
        assertTrue(file.isFile(), "Missing archetype resource " + file.getAbsolutePath());
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
