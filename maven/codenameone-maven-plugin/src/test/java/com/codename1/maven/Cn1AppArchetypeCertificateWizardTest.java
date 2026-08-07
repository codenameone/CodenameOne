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
                "codenameone:buildIosOnDeviceDebug");

        // The proxy bridges the device's wire protocol to JDWP.
        assertContains("tools/netbeans/nbactions.xml", "CUSTOM-Start iOS Debug Proxy");
        assertContains("tools/netbeans/nbactions.xml", "<goal>cn1:ios-on-device-debugging</goal>");
        assertContains(".idea/runConfigurations/CN1_Debug_Proxy.xml",
                "codenameone:ios-on-device-debugging");
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
