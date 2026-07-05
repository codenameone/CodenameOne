package com.codename1.certificatewizard;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CertificateWizardDesktopIdentityTest {
    @Test
    void launcherIsPublicEntryPointInsteadOfStub() throws Exception {
        String pom = read("pom.xml");
        assertTrue(pom.contains("<mainClass>com.codename1.certificatewizard.CertificateWizardLauncher</mainClass>"));
        assertTrue(read("src/desktop/java/com/codename1/certificatewizard/CertificateWizardLauncher.java")
                .contains("CertificateWizardStub.main(args);"));
    }

    @Test
    void desktopIdentityUsesReadableAppName() throws Exception {
        String stub = read("src/desktop/java/com/codename1/certificatewizard/CertificateWizardStub.java");
        assertTrue(stub.contains("APP_DISPLAY_NAME = \"Certificate Wizard\""));
        assertTrue(stub.contains("apple.awt.application.name"));
        assertTrue(stub.contains("com.apple.mrj.application.apple.menu.about.name"));
        assertTrue(stub.contains("sun.awt.application.name"));
        assertTrue(stub.contains("sun.awt.X11.XWMClass"));
        assertTrue(stub.contains("Taskbar.getTaskbar()"));
        assertTrue(stub.contains("Desktop.getDesktop()"));
        assertTrue(stub.contains("com.apple.eawt.Application"));
        assertTrue(stub.contains("setDockIconImage"));
    }

    @Test
    void highResolutionIconAndModuleExportArePackagedForDesktopTaskbars() throws Exception {
        assertNotNull(getClass().getResource("/icon.png"));
        String pom = read("pom.xml");
        assertTrue(pom.contains("<Application-Name>Certificate Wizard</Application-Name>"));
        assertTrue(pom.contains("<Add-Exports>java.desktop/com.apple.eawt.event java.desktop/com.apple.eawt</Add-Exports>"));
        assertEquals(512, javax.imageio.ImageIO.read(getClass().getResource("/icon.png")).getWidth());
    }

    private static String read(String relativePath) throws Exception {
        Path base = Paths.get(System.getProperty("user.dir"));
        return new String(Files.readAllBytes(base.resolve(relativePath)), StandardCharsets.UTF_8);
    }
}
