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

public class OpenSettingsMojoTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void extractsPackagedIconForDesktopLaunchers() throws Exception {
        File jar = jarWithIcon("settings.jar");
        File runtimeDir = tmp.newFolder("runtime");

        File extracted = new OpenSettingsMojo().extractSettingsIcon(jar, runtimeDir);

        assertTrue(extracted.isFile());
        assertEquals("settings-icon.png", extracted.getName());
        assertEquals("fake-png", new String(Files.readAllBytes(extracted.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    public void desktopIdentityArgsUseSettingsName() throws Exception {
        File runtimeDir = tmp.newFolder("runtime");
        List<String> args = new OpenSettingsMojo().desktopIdentityArgs(jarWithIcon("settings.jar"), runtimeDir);

        assertTrue(args.contains("-Dapple.awt.application.name=Codename One Settings"));
        assertTrue(args.contains("-Dcom.apple.mrj.application.apple.menu.about.name=Codename One Settings"));
        assertTrue(args.contains("-Dsun.awt.application.name=Codename One Settings"));
        assertTrue(args.contains("-Dsun.awt.X11.XWMClass=CodenameOneSettings"));
        if (OpenSettingsMojo.isJava9OrNewer()) {
            assertTrue(args.contains("--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"));
            assertTrue(args.contains("--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED"));
        }
    }

    @Test
    public void namedJavaLauncherUsesSettingsProcessName() throws Exception {
        File launcher = new OpenSettingsMojo().namedJavaLauncher(tmp.newFolder("runtime"));

        if (OpenSettingsMojo.isWindows()) {
            assertEquals("CodenameOneSettings.exe", launcher.getName());
        } else {
            assertEquals("Codename One Settings", launcher.getName());
            assertTrue(Files.isSymbolicLink(launcher.toPath()) || launcher.exists());
        }
    }

    @Test
    public void settingsCanLaunchFromProjectRootWithCommonModule() throws Exception {
        File root = tmp.newFolder("cn1app");
        File common = new File(root, "common");
        assertTrue(common.mkdirs());
        Files.write(new File(root, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "codenameone_settings.properties").toPath(),
                "codename1.packageName=com.example.app\n".getBytes(StandardCharsets.UTF_8));

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(root);

        assertTrue(mojo.isCN1ProjectDir());
        assertEquals(common.getCanonicalFile(), mojo.getCN1ProjectDir().getCanonicalFile());
        assertEquals(root.getCanonicalFile(), mojo.multimoduleRoot(common).getCanonicalFile());
    }

    @Test
    public void settingsCanLaunchFromCommonModule() throws Exception {
        File common = tmp.newFolder("common");
        Files.write(new File(common, "pom.xml").toPath(), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(common, "codenameone_settings.properties").toPath(),
                "codename1.packageName=com.example.app\n".getBytes(StandardCharsets.UTF_8));

        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(common);

        assertTrue(mojo.isCN1ProjectDir());
        assertEquals(common.getCanonicalFile(), mojo.getCN1ProjectDir().getCanonicalFile());
    }

    @Test
    public void bindingContainsProjectFilesAndMultimoduleRoot() throws Exception {
        File root = tmp.newFolder("project");
        File common = new File(root, "common");
        assertTrue(common.mkdirs());
        File input = tmp.newFile("settings.input");

        new OpenSettingsMojo().writeBinding(input, common);

        String binding = new String(Files.readAllBytes(input.toPath()), StandardCharsets.UTF_8);
        assertTrue(binding.contains("projectDir=" + common.getAbsolutePath()));
        assertTrue(binding.contains("settings=" + new File(common, "codenameone_settings.properties").getAbsolutePath()));
        assertTrue(binding.contains("pom=" + new File(common, "pom.xml").getAbsolutePath()));
        assertTrue(binding.contains("multimoduleRoot=" + root.getAbsolutePath()));
    }

    @Test
    public void pluginVersionUsesCodenameOneVersionInsteadOfApplicationVersion() {
        OpenSettingsMojo mojo = new OpenSettingsMojo();
        mojo.project = projectAt(new File("."));
        mojo.project.setVersion("1.0-SNAPSHOT");
        mojo.project.getProperties().setProperty("cn1.version", "7.0.258");

        assertEquals("7.0.258", mojo.pluginVersion());

        mojo.project.getProperties().setProperty("cn1.plugin.version", "7.0.259");
        assertEquals("7.0.259", mojo.pluginVersion());
    }

    @Test
    public void platformDetectionHandlesMacWindowsAndJavaVersions() {
        String os = System.getProperty("os.name");
        String java = System.getProperty("java.specification.version");
        try {
            System.setProperty("os.name", "Mac OS X");
            assertTrue(OpenSettingsMojo.isMacOs());
            assertFalse(OpenSettingsMojo.isWindows());
            System.setProperty("os.name", "Windows 11");
            assertTrue(OpenSettingsMojo.isWindows());
            System.setProperty("java.specification.version", "17");
            assertTrue(OpenSettingsMojo.isJava9OrNewer());
            System.setProperty("java.specification.version", "1.8");
            assertFalse(OpenSettingsMojo.isJava9OrNewer());
        } finally {
            restoreProperty("os.name", os);
            restoreProperty("java.specification.version", java);
        }
    }

    private File jarWithIcon(String name) throws Exception {
        File jar = tmp.newFile(name);
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry("icon.png"));
            out.write("fake-png".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
        return jar;
    }

    private MavenProject projectAt(File basedir) {
        MavenProject project = new MavenProject();
        project.setFile(new File(basedir, "pom.xml"));
        project.addCompileSourceRoot(new File(basedir, "src/main/java").getAbsolutePath());
        return project;
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
