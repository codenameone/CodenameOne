package com.codename1.maven;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompileCSSMojoTest {

    @Test
    void addsLocalizationDirectoryToDesignerInvocation(@TempDir Path tempDir) throws Exception {
        Path projectDir = setupProject(tempDir, "l10n");
        TestCompileCSSMojo mojo = createMojo(projectDir);

        mojo.executeImpl();

        List<String> args = mojo.getRecordingJava().getCommandLineArguments();
        assertTrue(args.contains("-l"), "Expected -l argument when localization directory exists");
        int index = args.indexOf("-l");
        assertTrue(index >= 0 && index + 1 < args.size(), "Expected localization directory argument after -l");
        assertEquals(projectDir.resolve("src/main/l10n").toFile().getAbsolutePath(), args.get(index + 1));
    }

    @Test
    void skipsLocalizationArgumentWhenDirectoryMissing(@TempDir Path tempDir) throws Exception {
        Path projectDir = setupProject(tempDir, null);
        TestCompileCSSMojo mojo = createMojo(projectDir);

        mojo.executeImpl();

        List<String> args = mojo.getRecordingJava().getCommandLineArguments();
        assertFalse(args.contains("-l"), "Did not expect -l argument without localization directory");
    }

    @Test
    void addsLocalizationArgumentWhenDirectoryIsEmpty(@TempDir Path tempDir) throws Exception {
        Path projectDir = setupProject(tempDir, null);
        Files.createDirectories(projectDir.resolve("src/main/l10n"));
        TestCompileCSSMojo mojo = createMojo(projectDir);

        mojo.executeImpl();

        List<String> args = mojo.getRecordingJava().getCommandLineArguments();
        assertTrue(args.contains("-l"), "Expected -l argument when localization directory exists even if empty");
        int index = args.indexOf("-l");
        assertTrue(index >= 0 && index + 1 < args.size(), "Expected localization directory argument after -l");
        assertEquals(projectDir.resolve("src/main/l10n").toFile().getAbsolutePath(), args.get(index + 1));
    }

    @Test
    void addsI18nDirectoryToDesignerInvocation(@TempDir Path tempDir) throws Exception {
        Path projectDir = setupProject(tempDir, "i18n");
        TestCompileCSSMojo mojo = createMojo(projectDir);

        mojo.executeImpl();

        List<String> args = mojo.getRecordingJava().getCommandLineArguments();
        assertTrue(args.contains("-l"), "Expected -l argument when i18n directory exists");
        int index = args.indexOf("-l");
        assertTrue(index >= 0 && index + 1 < args.size(), "Expected localization directory argument after -l");
        assertEquals(projectDir.resolve("src/main/i18n").toFile().getAbsolutePath(), args.get(index + 1));
    }

    /// Reproduces the staleness bug where edits to a `.properties` localization bundle do not
    /// trigger CSS re-compilation. `getCSSSourcesModificationTime` only walks `src/main/css`, but
    /// the CSS compiler also reads the l10n directory (it bakes the resource bundle into the same
    /// `theme.res`), so a newer l10n file with an older CSS file must still recompile.
    @Test
    void recompilesWhenLocalizationFileNewerThanThemeRes(@TempDir Path tempDir) throws Exception {
        Path projectDir = setupMultiModuleCommonProject(tempDir, "l10n");
        Path themeRes = projectDir.resolve("target/classes/theme.res");
        Path l10nFile = projectDir.resolve("src/main/l10n/Messages.properties");

        Files.createDirectories(themeRes.getParent());
        Files.write(themeRes, new byte[]{0x42});

        long base = System.currentTimeMillis() - 60_000L;
        ageAllInputs(projectDir, base);
        assertTrue(themeRes.toFile().setLastModified(base + 5_000L));
        assertTrue(l10nFile.toFile().setLastModified(base + 10_000L));

        TestCompileCSSMojo mojo = createMojo(projectDir);
        mojo.executeImpl();

        assertNotNull(mojo.getRecordingJava(),
                "CSS compiler should have been invoked because the l10n file is newer than theme.res");
    }

    /// Sanity-check: when only the merged-output theme.res is newer than every CSS/l10n input,
    /// the mojo correctly skips the subprocess. This guards against an over-eager fix that just
    /// disables the up-to-date check entirely.
    @Test
    void skipsCompilationWhenAllInputsOlderThanThemeRes(@TempDir Path tempDir) throws Exception {
        Path projectDir = setupMultiModuleCommonProject(tempDir, "l10n");
        Path themeRes = projectDir.resolve("target/classes/theme.res");

        Files.createDirectories(themeRes.getParent());
        Files.write(themeRes, new byte[]{0x42});

        long base = System.currentTimeMillis() - 60_000L;
        ageAllInputs(projectDir, base);
        assertTrue(themeRes.toFile().setLastModified(base + 10_000L));

        TestCompileCSSMojo mojo = createMojo(projectDir);
        mojo.executeImpl();

        assertNull(mojo.getRecordingJava(),
                "CSS compiler should be skipped when neither CSS nor l10n changed since the last build");
    }

    /// Companion to the l10n test: CSS edits already invalidate the cache today, but lock the
    /// behavior down so it does not regress alongside the l10n fix.
    @Test
    void recompilesWhenCssFileNewerThanThemeRes(@TempDir Path tempDir) throws Exception {
        Path projectDir = setupMultiModuleCommonProject(tempDir, null);
        Path themeRes = projectDir.resolve("target/classes/theme.res");
        Path cssFile = projectDir.resolve("src/main/css/theme.css");

        Files.createDirectories(themeRes.getParent());
        Files.write(themeRes, new byte[]{0x42});

        long base = System.currentTimeMillis() - 60_000L;
        ageAllInputs(projectDir, base);
        assertTrue(themeRes.toFile().setLastModified(base));
        assertTrue(cssFile.toFile().setLastModified(base + 10_000L));

        TestCompileCSSMojo mojo = createMojo(projectDir);
        mojo.executeImpl();

        assertNotNull(mojo.getRecordingJava(),
                "CSS compiler should have been invoked because the CSS file is newer than theme.res");
    }

    /// `getCSSSourcesModificationTime` walks `src/main/css` recursively (directory mtimes count too)
    /// and also stats the project pom and codenameone_settings. Files just created by the test
    /// helper land with `now` as their mtime, which would always win the comparison and force
    /// compilation regardless of the CSS/l10n setup we want to assert against. Walk every file
    /// and directory under the project and age them to a known baseline so each test can drive
    /// the modtime ordering it cares about.
    private static void ageAllInputs(Path projectDir, long baseMillis) throws IOException {
        Files.walk(projectDir).forEach(p -> {
            // The target/ directory is set up per-test (theme.res gets an explicit mtime later),
            // so leave it alone here.
            if (p.startsWith(projectDir.resolve("target"))) {
                return;
            }
            assertTrue(p.toFile().setLastModified(baseMillis),
                    "Failed to set mtime on " + p);
        });
    }

    private TestCompileCSSMojo createMojo(Path projectDir) throws IOException {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(projectDir.resolve("pom.xml").toFile());
        mavenProject.addCompileSourceRoot(projectDir.resolve("src/main/java").toString());
        Build build = new Build();
        build.setDirectory(projectDir.resolve("target").toString());
        build.setOutputDirectory(projectDir.resolve("target/classes").toString());
        mavenProject.setBuild(build);
        mavenProject.setArtifacts(new HashSet<>());

        TestCompileCSSMojo mojo = new TestCompileCSSMojo(projectDir.resolve("designer.jar").toFile());
        mojo.project = mavenProject;
        mojo.antProject = new Project();
        mojo.antProject.setBaseDir(projectDir.toFile());
        mojo.antProject.init();
        mojo.pluginArtifacts = new ArrayList<>();
        mojo.properties = new Properties();
        mojo.properties.setProperty("codename1.cssTheme", "true");
        mojo.setLog(new SystemStreamLog());

        return mojo;
    }

    private Path setupProject(Path tempDir, String localizationDirName) throws IOException {
        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Path cssDir = Files.createDirectories(projectDir.resolve("src/main/css"));
        Files.write(cssDir.resolve("theme.css"), Arrays.asList("/* test css */"));
        Files.write(projectDir.resolve("codenameone_settings.properties"), Arrays.asList("codename1.cssTheme=true"));
        Files.createDirectories(projectDir.resolve("target/classes"));
        Files.createFile(projectDir.resolve("designer.jar"));
        Files.write(projectDir.resolve("pom.xml"), Arrays.asList(
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"",
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
                "  <modelVersion>4.0.0</modelVersion>",
                "  <groupId>com.codename1</groupId>",
                "  <artifactId>test-project</artifactId>",
                "  <version>1.0-SNAPSHOT</version>",
                "</project>"
        ));

        if (localizationDirName != null) {
            Path localizationDir = Files.createDirectories(projectDir.resolve("src/main").resolve(localizationDirName));
            Files.write(localizationDir.resolve("Messages.properties"), Arrays.asList("greeting=Hello"));
        }

        return projectDir;
    }

    /// Sets up the canonical multi-module Codename One layout: a parent dir containing a `common/`
    /// child that owns the CSS and (optionally) the localization. `getCSSSourcesModificationTime`
    /// derives `root = projectBaseDir.getParentFile()` and then looks for `root/common/src/main/css`,
    /// so naming the basedir literally "common" is what makes that lookup resolve back to this
    /// project's CSS directory. The single-module helper above puts CSS at `project/src/main/css`,
    /// which leaves `getCSSSourcesModificationTime` returning zero and silently masks the staleness
    /// bug we want to test.
    private Path setupMultiModuleCommonProject(Path tempDir, String localizationDirName) throws IOException {
        Path parentDir = tempDir.resolve("parent");
        Files.createDirectories(parentDir);
        Path commonDir = parentDir.resolve("common");
        Files.createDirectories(commonDir);
        Files.createDirectories(commonDir.resolve("src/main/java"));
        Path cssDir = Files.createDirectories(commonDir.resolve("src/main/css"));
        Files.write(cssDir.resolve("theme.css"), Arrays.asList("/* test css */"));
        Files.write(commonDir.resolve("codenameone_settings.properties"), Arrays.asList("codename1.cssTheme=true"));
        Files.createDirectories(commonDir.resolve("target/classes"));
        Files.createFile(commonDir.resolve("designer.jar"));
        Files.write(commonDir.resolve("pom.xml"), Arrays.asList(
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"",
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
                "  <modelVersion>4.0.0</modelVersion>",
                "  <groupId>com.codename1</groupId>",
                "  <artifactId>test-common</artifactId>",
                "  <version>1.0-SNAPSHOT</version>",
                "</project>"
        ));

        if (localizationDirName != null) {
            Path localizationDir = Files.createDirectories(commonDir.resolve("src/main").resolve(localizationDirName));
            Files.write(localizationDir.resolve("Messages.properties"), Arrays.asList("greeting=Hello"));
        }

        return commonDir;
    }

    private static class TestCompileCSSMojo extends CompileCSSMojo {
        private final File designerJar;
        private RecordingJava recordingJava;

        private TestCompileCSSMojo(File designerJar) {
            this.designerJar = designerJar;
        }

        @Override
        public Java createJava() {
            recordingJava = new RecordingJava();
            recordingJava.setProject(antProject);
            return recordingJava;
        }

        @Override
        protected void setupCef() {
            // Skip CEF setup during tests.
        }

        @Override
        protected File getDesignerJar() {
            return designerJar;
        }

        RecordingJava getRecordingJava() {
            return recordingJava;
        }
    }

    private static class RecordingJava extends Java {
        private List<String> commandLineArguments = new ArrayList<>();

        @Override
        public int executeJava() {
            commandLineArguments = Arrays.asList(getCommandLine().getCommandline());
            return 0;
        }

        List<String> getCommandLineArguments() {
            return commandLineArguments;
        }
    }
}
