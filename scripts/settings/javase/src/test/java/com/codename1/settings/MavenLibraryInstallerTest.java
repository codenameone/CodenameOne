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
import com.codename1.settings.project.ProjectBinding;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class MavenLibraryInstallerTest {
    @TempDir Path root;

    private ProjectBinding project() throws Exception {
        Path common = Files.createDirectories(root.resolve("app with spaces/common"));
        Files.writeString(common.resolve("pom.xml"),
                "<project><modelVersion>4.0.0</modelVersion><dependencies/></project>");
        Path home = Files.createDirectories(root.resolve("maven with spaces"));
        Files.createDirectories(home.resolve("bin"));
        Files.createDirectories(home.resolve("boot"));
        Files.writeString(home.resolve("bin/m2.conf"), "");
        Files.writeString(home.resolve("boot/plexus-classworlds-test.jar"), "");
        return ProjectBinding.parse("projectDir=" + common + "\npom=" + common.resolve("pom.xml")
                + "\nmultimoduleRoot=" + root + "\nmavenHome=" + home);
    }

    private Path wrapper(ProjectBinding binding) throws Exception {
        Path wrapper = Path.of(binding.projectDir()).getParent().resolve("cn1libs/Example/pom.xml");
        Files.createDirectories(wrapper.getParent());
        Files.writeString(wrapper, "<project><parent><groupId>parent</groupId><artifactId>parent</artifactId>"
                + "<version>0</version></parent><groupId>example</groupId><artifactId>example-lib</artifactId>"
                + "<version>1</version><packaging>pom</packaging></project>");
        Files.writeString(wrapper.getParent().getParent().resolve("pom.xml"),
                "<project><profiles><profile><id>Example.cn1lib-cn1lib</id>"
                + "<activation><file><exists>${basedir}/Example/pom.xml</exists></file></activation>"
                + "<modules><module>Example</module></modules></profile></profiles></project>");
        return wrapper;
    }

    @Test void restoresExactCommonPomAfterConverterEditAndFailure() throws Exception {
        for (int scenario = 0; scenario < 3; scenario++) {
            final int failure = scenario;
            ProjectBinding binding = project();
            Path pom = Path.of(binding.pom());
            byte[] original = ("<project>\r\n<!-- café -->\r\n<dependencies>"
                    + "<!-- INJECT DEPENDENCIES --></dependencies></project>\r\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(pom, original);
            Path archive = root.resolve("Example.cn1lib");
            Files.writeString(archive, "archive");
            Path wrapper = Path.of(binding.projectDir()).getParent().resolve("cn1libs/Example/pom.xml");
            Files.deleteIfExists(wrapper);
            MavenLibraryInstaller installer = new MavenLibraryInstaller() {
                @Override void runMaven(List<String> command, File common) throws IOException, InterruptedException {
                    try {
                        Path generated = wrapper(binding);
                        Files.writeString(pom, "<project><dependencies><dependency><groupId>example</groupId>"
                                + "<artifactId>example-lib</artifactId><version>1</version>"
                                + "</dependency></dependencies></project>");
                        Files.delete(generated.getParent().getParent().resolve("pom.xml"));
                    } catch (Exception ex) { throw new IOException(ex); }
                    if (failure == 0) throw new IOException("Failed to register module");
                    if (failure == 1) throw new InterruptedException("Timed out while registering module");
                    // Even exit zero must roll back if wrapper validation fails.
                }
            };
            assertThrows(Exception.class, () -> installer.install(binding, archive.toString()));
            assertArrayEquals(original, Files.readAllBytes(pom));
            assertTrue(Files.isRegularFile(wrapper), "Preserve wrapper for recovery");
        }
    }

    @Test void retainsLaunchingMavenOptionsWithoutShellSplitting() throws Exception {
        ProjectBinding original = project();
        List<String> options = List.of("--settings", root.resolve("alternate settings.xml").toString(),
                "--global-settings", root.resolve("global settings.xml").toString(),
                "--offline", "--no-snapshot-updates", "-Pprivate", "-P!public", "-Dmaven.repo.local=" + root.resolve("local repo"),
                "-Dcustom=spaces & equals=and\nnewlines Ω ", "-Dfile=old.cn1lib");
        StringBuilder content = new StringBuilder("projectDir=" + original.projectDir()
                + "\nmavenHome=" + original.mavenHome() + "\n");
        for (String option : options) {
            content.append("mavenArgument=").append(java.util.Base64.getEncoder().encodeToString(
                    option.getBytes(java.nio.charset.StandardCharsets.UTF_8))).append('\n');
        }
        ProjectBinding binding = ProjectBinding.parse(content.toString());
        assertEquals(options, binding.mavenArguments());
        File archive = root.resolve("Example.cn1lib").toFile();
        List<String> command = new MavenLibraryInstaller().command(binding, archive);
        int first = command.indexOf("--non-recursive") + 1;
        assertEquals(options, command.subList(first, first + options.size()));
        assertEquals("-Dfile=" + archive, command.get(command.size() - 1));
    }

    @Test void partialConversionCannotBecomeSuccessOnRetry() throws Exception {
        ProjectBinding binding = project();
        Path archive = root.resolve("Example.cn1lib");
        Files.writeString(archive, "archive");
        String commonBefore = Files.readString(Path.of(binding.pom()));
        MavenLibraryInstaller installer = new MavenLibraryInstaller() {
            @Override void runMaven(List<String> command, File common) throws IOException {
                try {
                    Path generated = wrapper(binding);
                    Files.delete(generated.getParent().getParent().resolve("pom.xml"));
                } catch (Exception ex) { throw new IOException(ex); }
                throw new IOException("Conversion interrupted after writing wrapper");
            }
        };
        assertThrows(IOException.class, () -> installer.install(binding, archive.toString()));
        Path generated = Path.of(binding.projectDir()).getParent().resolve("cn1libs/Example/pom.xml");
        String wrapperBefore = Files.readString(generated);
        IOException retry = assertThrows(IOException.class, () -> installer.install(binding, archive.toString()));
        assertTrue(retry.getMessage().contains("not registered as module Example"));
        assertEquals(commonBefore, Files.readString(Path.of(binding.pom())));
        assertEquals(wrapperBefore, Files.readString(generated));
        Files.writeString(generated.getParent().getParent().resolve("pom.xml"),
                "<project><modules><module>Other</module></modules>"
                + "<!-- <module>Example</module> --></project>");
        assertThrows(IOException.class, () -> installer.dependency(binding, "Example.cn1lib"));
        Files.writeString(generated.getParent().getParent().resolve("pom.xml"),
                "<project><modules><module>Example</module></modules></project>");
        installer.install(binding, archive.toString());
        assertTrue(Files.readString(Path.of(binding.pom())).contains("<artifactId>example-lib</artifactId>"));
        assertEquals(wrapperBefore, Files.readString(generated));
    }

    @Test void downloadedFileAloneIsNotInstalled() throws Exception {
        ProjectBinding binding = project();
        Path archive = Path.of(binding.projectDir()).getParent().resolve("cn1libs/Example.cn1lib");
        Files.createDirectories(archive.getParent());
        Files.writeString(archive, "downloaded archive");
        assertNull(new MavenLibraryInstaller().dependency(binding, "Example.cn1lib"));
    }

    @Test void usesWrapperCoordinatesAndActualCommonParent() throws Exception {
        ProjectBinding binding = project();
        wrapper(binding);
        assertEquals("example:example-lib:1:pom",
                new MavenLibraryInstaller().dependency(binding, "Example.cn1lib").coordinates());
    }

    @Test void reinstallsWithoutOverwritingWrapperOrRequiringArchive() throws Exception {
        ProjectBinding binding = project();
        Path wrapper = wrapper(binding);
        String original = Files.readString(wrapper);
        MavenLibraryInstaller installer = new MavenLibraryInstaller() {
            @Override void runMaven(List<String> command, File common) { fail("Existing wrapper must be reused"); }
        };
        String archive = wrapper.getParent().getParent().resolve("Example.cn1lib").toString();
        installer.install(binding, archive);
        String updated = Files.readString(Path.of(binding.pom()));
        assertTrue(PomEditor.containsDependency(updated, installer.dependency(binding, "Example.cn1lib")));
        installer.install(binding, archive);
        assertEquals(updated, Files.readString(Path.of(binding.pom())), "reinstall is idempotent");
        assertEquals(original, Files.readString(wrapper));
    }

    @Test void successfulConversionAddsDependencyWithoutInjectionComment() throws Exception {
        ProjectBinding binding = project();
        Path archive = root.resolve("Example.cn1lib");
        Files.writeString(archive, "archive");
        MavenLibraryInstaller installer = new MavenLibraryInstaller() {
            @Override void runMaven(List<String> command, File common) throws IOException {
                assertEquals(Path.of(binding.projectDir()), common.toPath());
                try { wrapper(binding); } catch (Exception ex) { throw new IOException(ex); }
            }
        };
        installer.install(binding, archive.toString());
        assertTrue(Files.readString(Path.of(binding.pom())).contains("<artifactId>example-lib</artifactId>"));
    }

    @Test void failedConversionDoesNotAddDependency() throws Exception {
        ProjectBinding binding = project();
        Path archive = root.resolve("Example.cn1lib");
        Files.writeString(archive, "archive");
        String before = Files.readString(Path.of(binding.pom()));
        MavenLibraryInstaller installer = new MavenLibraryInstaller() {
            @Override void runMaven(List<String> command, File common) throws IOException {
                throw new IOException("Maven failed");
            }
        };
        assertThrows(IOException.class, () -> installer.install(binding, archive.toString()));
        assertEquals(before, Files.readString(Path.of(binding.pom())));
    }

    @Test void successfulExitWithoutWrapperIsStillAFailure() throws Exception {
        ProjectBinding binding = project();
        Path archive = root.resolve("Example.cn1lib");
        Files.writeString(archive, "archive");
        MavenLibraryInstaller installer = new MavenLibraryInstaller() {
            @Override void runMaven(List<String> command, File common) { }
        };
        assertThrows(IOException.class, () -> installer.install(binding, archive.toString()));
    }

    @Test void mavenLaunchKeepsPathsAsSeparateArgumentsAndDoesNotUseAShell() throws Exception {
        ProjectBinding binding = project();
        File archive = root.resolve("lib with spaces & punctuation.cn1lib").toFile();
        List<String> command = new MavenLibraryInstaller().command(binding, archive);
        assertTrue(command.get(0).endsWith("java") || command.get(0).endsWith("java.exe"));
        assertTrue(command.contains("-Dfile=" + archive));
        assertTrue(command.contains("-Dmaven.home=" + new File(binding.mavenHome()).getCanonicalPath()));
        assertTrue(command.contains("-Dmaven.multiModuleProjectDirectory="
                + new File(binding.projectDir()).getCanonicalFile().getParent()));
        assertTrue(command.contains("--non-recursive"));
        assertEquals("cn1:install-cn1lib", command.get(command.size() - 2));
    }

    @Test void reportsNonzeroMavenProcessExit() throws Exception {
        ProjectBinding binding = project();
        String java = new MavenLibraryInstaller().command(binding, root.resolve("Example.cn1lib").toFile()).get(0);
        IOException error = assertThrows(IOException.class, () -> new MavenLibraryInstaller().runMaven(
                List.of(java, "-invalid-settings-test-option"), new File(binding.projectDir())));
        assertTrue(error.getMessage().contains("exit"));
        assertTrue(error.getMessage().contains("settings.log"));
    }

    @Test void reusesDoctypePomsWithoutLoadingExternalSubsets() throws Exception {
        ProjectBinding binding = project();
        Path wrapper = wrapper(binding);
        Path aggregator = wrapper.getParent().getParent().resolve("pom.xml");
        String wrapperXml = Files.readString(wrapper);
        String aggregatorXml = Files.readString(aggregator);
        String missingDtd = root.resolve("must-not-read.dtd").toUri().toString();
        MavenLibraryInstaller installer = new MavenLibraryInstaller() {
            @Override void runMaven(List<String> command, File common) { fail("Reuse the existing wrapper"); }
        };
        for (String declaration : List.of("<!DOCTYPE project>",
                "<!DOCTYPE project SYSTEM '" + missingDtd + "'>",
                "<!DOCTYPE project PUBLIC '-//Example//DTD project//EN' '" + missingDtd + "'>",
                "<!DOCTYPE project [<!ENTITY % remote SYSTEM '" + missingDtd + "'>%remote;]>")) {
            Files.writeString(wrapper, declaration + wrapperXml);
            Files.writeString(aggregator, declaration + aggregatorXml);
            installer.install(binding, root.resolve("Example.cn1lib").toString());
            assertEquals("example:example-lib:1:pom", installer.dependency(binding, "Example.cn1lib").coordinates());
            assertEquals(declaration + wrapperXml, Files.readString(wrapper));
            assertEquals(declaration + aggregatorXml, Files.readString(aggregator));
        }
    }

    @Test void acceptsDoctypeAfterSuccessfulConversion() throws Exception {
        ProjectBinding binding = project();
        Path archive = root.resolve("Example.cn1lib");
        Files.writeString(archive, "archive");
        MavenLibraryInstaller installer = new MavenLibraryInstaller() {
            @Override void runMaven(List<String> command, File common) throws IOException {
                try {
                    Path generated = wrapper(binding);
                    Path aggregator = generated.getParent().getParent().resolve("pom.xml");
                    Files.writeString(aggregator, "<!DOCTYPE project>" + Files.readString(aggregator));
                } catch (Exception ex) { throw new IOException(ex); }
            }
        };
        installer.install(binding, archive.toString());
        assertTrue(PomEditor.containsDependency(Files.readString(Path.of(binding.pom())),
                installer.dependency(binding, "Example.cn1lib")));
    }

    @Test void doesNotExpandExternalModuleEntityInAggregator() throws Exception {
        ProjectBinding binding = project();
        Path generated = wrapper(binding);
        Path entity = root.resolve("module.txt");
        Files.writeString(entity, "Example");
        Files.writeString(generated.getParent().getParent().resolve("pom.xml"),
                "<!DOCTYPE project [<!ENTITY module SYSTEM '" + entity.toUri() + "'>]>"
                + "<project><modules><module>&module;</module></modules></project>");
        assertThrows(IOException.class, () -> new MavenLibraryInstaller().dependency(binding, "Example.cn1lib"));
    }

    @Test void rejectsPathsAndXmlExternalEntitiesInWrapper() throws Exception {
        ProjectBinding binding = project();
        MavenLibraryInstaller installer = new MavenLibraryInstaller();
        assertThrows(IOException.class, () -> installer.dependency(binding, "../Example.cn1lib"));
        Path wrapper = wrapper(binding);
        Path entity = root.resolve("group.txt");
        Files.writeString(entity, "example");
        Files.writeString(wrapper, "<!DOCTYPE project [<!ENTITY x SYSTEM '" + entity.toUri() + "'>]>"
                + "<project><groupId>&x;</groupId><artifactId>example-lib</artifactId><version>1</version></project>");
        IOException failure = assertThrows(IOException.class, () -> installer.dependency(binding, "Example.cn1lib"));
        assertTrue(failure.getMessage().contains("incomplete Maven coordinates"));
    }
}
