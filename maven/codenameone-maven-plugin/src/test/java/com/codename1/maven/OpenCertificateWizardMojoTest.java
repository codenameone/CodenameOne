package com.codename1.maven;

import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpenCertificateWizardMojoTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void extractsPackagedIconForDesktopLaunchers() throws Exception {
        File jar = tmp.newFile("wizard.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry("icon.png"));
            out.write("fake-png".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        File runtimeDir = tmp.newFolder("runtime");
        File extracted = new OpenCertificateWizardMojo().extractWizardIcon(jar, runtimeDir);

        assertTrue(extracted.isFile());
        assertEquals("certificate-wizard-icon.png", extracted.getName());
        assertEquals("fake-png", new String(Files.readAllBytes(extracted.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void macOsDetectionIsBasedOnOsName() {
        String old = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Mac OS X");
            assertTrue(OpenCertificateWizardMojo.isMacOs());
            System.setProperty("os.name", "Linux");
            assertFalse(OpenCertificateWizardMojo.isMacOs());
        } finally {
            if (old == null) {
                System.clearProperty("os.name");
            } else {
                System.setProperty("os.name", old);
            }
        }
    }

    @Test
    public void java9DetectionHandlesModernAndLegacyVersionStrings() {
        String old = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "17");
            assertTrue(OpenCertificateWizardMojo.isJava9OrNewer());
            System.setProperty("java.specification.version", "1.8");
            assertFalse(OpenCertificateWizardMojo.isJava9OrNewer());
        } finally {
            if (old == null) {
                System.clearProperty("java.specification.version");
            } else {
                System.setProperty("java.specification.version", old);
            }
        }
    }

    @Test
    public void desktopIdentityArgsIncludeCrossPlatformAppNameAndModuleExports() throws Exception {
        File jar = tmp.newFile("wizard.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry("icon.png"));
            out.write("fake-png".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        File runtimeDir = tmp.newFolder("runtime");

        List<String> args = new OpenCertificateWizardMojo().desktopIdentityArgs(jar, runtimeDir);

        assertTrue(args.contains("-Dapple.awt.application.name=Certificate Wizard"));
        assertTrue(args.contains("-Dcom.apple.mrj.application.apple.menu.about.name=Certificate Wizard"));
        assertTrue(args.contains("-Dsun.awt.application.name=Certificate Wizard"));
        assertTrue(args.contains("-Dsun.awt.X11.XWMClass=CertificateWizard"));
        if (OpenCertificateWizardMojo.isJava9OrNewer()) {
            assertTrue(args.contains("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"));
            assertTrue(args.contains("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED"));
        }
    }

    @Test
    public void namedJavaLauncherUsesCertificateWizardProcessName() throws Exception {
        File runtimeDir = tmp.newFolder("runtime");
        File launcher = new OpenCertificateWizardMojo().namedJavaLauncher(runtimeDir);

        if (OpenCertificateWizardMojo.isWindows()) {
            assertEquals("CertificateWizard.exe", launcher.getName());
        } else {
            assertEquals("Certificate Wizard", launcher.getName());
            assertTrue(Files.isSymbolicLink(launcher.toPath()) || launcher.exists());
        }
    }

    @Test
    public void certificateWizardCanLaunchFromProjectRootWithCommonModule() throws Exception {
        File root = tmp.newFolder("cn1app");
        File common = new File(root, "common");
        assertTrue(common.mkdirs());
        Files.write(new File(root, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "codenameone_settings.properties").toPath(),
                "codename1.packageName=com.example.app\n".getBytes(StandardCharsets.UTF_8));

        OpenCertificateWizardMojo mojo = new OpenCertificateWizardMojo();
        mojo.project = projectAt(root);

        assertTrue(mojo.isCN1ProjectDir());
        assertEquals(common.getCanonicalFile(), mojo.getCN1ProjectDir().getCanonicalFile());
    }

    @Test
    public void certificateWizardCanStillLaunchFromCommonModule() throws Exception {
        File common = tmp.newFolder("common");
        Files.write(new File(common, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "codenameone_settings.properties").toPath(),
                "codename1.packageName=com.example.app\n".getBytes(StandardCharsets.UTF_8));

        OpenCertificateWizardMojo mojo = new OpenCertificateWizardMojo();
        mojo.project = projectAt(common);

        assertTrue(mojo.isCN1ProjectDir());
        assertEquals(common.getCanonicalFile(), mojo.getCN1ProjectDir().getCanonicalFile());
    }

    private MavenProject projectAt(File basedir) {
        MavenProject project = new MavenProject();
        project.setFile(new File(basedir, "pom.xml"));
        project.addCompileSourceRoot(new File(basedir, "src/main/java").getAbsolutePath());
        return project;
    }
}
