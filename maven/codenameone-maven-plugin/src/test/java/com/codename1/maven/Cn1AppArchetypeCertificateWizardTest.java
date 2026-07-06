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
