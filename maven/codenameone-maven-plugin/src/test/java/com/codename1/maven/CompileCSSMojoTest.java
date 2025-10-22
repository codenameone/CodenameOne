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
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompileCSSMojoTest {

    @Test
    void addsLocalizationDirectoryToDesignerInvocation(@TempDir Path tempDir) throws Exception {
        Path projectDir = setupProject(tempDir, true);
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
        Path projectDir = setupProject(tempDir, false);
        TestCompileCSSMojo mojo = createMojo(projectDir);

        mojo.executeImpl();

        List<String> args = mojo.getRecordingJava().getCommandLineArguments();
        assertFalse(args.contains("-l"), "Did not expect -l argument without localization directory");
    }

    private TestCompileCSSMojo createMojo(Path projectDir) throws IOException {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setBasedir(projectDir.toFile());
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

    private Path setupProject(Path tempDir, boolean includeLocalization) throws IOException {
        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Path cssDir = Files.createDirectories(projectDir.resolve("src/main/css"));
        Files.write(cssDir.resolve("theme.css"), Arrays.asList("/* test css */"));
        Files.write(projectDir.resolve("codenameone_settings.properties"), Arrays.asList("codename1.cssTheme=true"));
        Files.createDirectories(projectDir.resolve("target/classes"));
        Files.createFile(projectDir.resolve("designer.jar"));

        if (includeLocalization) {
            Path localizationDir = Files.createDirectories(projectDir.resolve("src/main/l10n"));
            Files.write(localizationDir.resolve("Messages.properties"), Arrays.asList("greeting=Hello"));
        }

        return projectDir;
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
