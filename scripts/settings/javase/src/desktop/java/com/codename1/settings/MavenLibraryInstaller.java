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

import com.codename1.settings.extensions.LegacyMavenInstaller;
import com.codename1.settings.extensions.MavenDependency;
import com.codename1.settings.extensions.PomEditor;
import com.codename1.settings.project.ProjectBinding;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/// Runs the existing Maven converter off the EDT instead of treating a downloaded
/// archive as an installed dependency. Uses Java directly to avoid shell quoting
/// of Maven batch files and project/library paths on Windows.
public class MavenLibraryInstaller implements LegacyMavenInstaller {
    @Override
    public void install(ProjectBinding binding, String libraryPath) throws Exception {
        File library = new File(libraryPath);
        File common = new File(binding.projectDir());
        File pom = new File(binding.pom());
        byte[] original = Files.readAllBytes(pom.toPath());
        try {
            MavenDependency dependency = dependency(binding, library.getName());
            if (dependency == null) {
                if (!library.isFile()) {
                    throw new IOException("Library download is missing: " + library);
                }
                runMaven(command(binding, library), common);
                dependency = dependency(binding, library.getName());
                if (dependency == null) {
                    throw new IOException("Maven did not create the library wrapper. See settings.log.");
                }
            }
            // The converter may already have injected a dependency. For POMs
            // without its marker (and reinstalls), add the direct dependency.
            String converted = Files.readString(pom.toPath(), StandardCharsets.UTF_8);
            String updated = PomEditor.addDependency(converted, dependency);
            if (!converted.equals(updated)) {
                Files.writeString(pom.toPath(), updated, StandardCharsets.UTF_8);
            }
        } catch (Exception failure) {
            // The converter edits common before registering the wrapper. A
            // failure or missing registration must not leave an unresolved
            // dependency behind. runMaven has reaped its child before returning.
            try {
                if (!Files.exists(pom.toPath()) || !Arrays.equals(original, Files.readAllBytes(pom.toPath()))) {
                    Files.write(pom.toPath(), original);
                }
            } catch (IOException restoreFailure) {
                failure.addSuppressed(restoreFailure);
            }
            throw failure;
        }
    }

    @Override
    public MavenDependency dependency(ProjectBinding binding, String fileName) throws Exception {
        if (fileName == null || !fileName.endsWith(".cn1lib") || fileName.contains("/")
                || fileName.contains("\\") || fileName.length() == ".cn1lib".length()) {
            throw new IOException("Invalid cn1lib filename: " + fileName);
        }
        // This is the layout required by cn1:install-cn1lib, including when
        // Maven was launched with a larger multi-module reactor as its root.
        File common = new File(binding.projectDir()).getCanonicalFile();
        if (!"common".equals(common.getName())) {
            throw new IOException("Legacy Maven libraries require a common/ project directory.");
        }
        File wrapper = new File(common.getParentFile(), "cn1libs/"
                + fileName.substring(0, fileName.length() - ".cn1lib".length()) + "/pom.xml");
        if (!wrapper.isFile()) {
            return null;
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        Element project = factory.newDocumentBuilder().parse(wrapper).getDocumentElement();
        MavenDependency dependency = new MavenDependency(value(project, "groupId"),
                value(project, "artifactId"), value(project, "version"), "pom");
        if (!dependency.isValid()) {
            throw new IOException("Library wrapper has incomplete Maven coordinates: " + wrapper);
        }
        File aggregator = new File(wrapper.getParentFile().getParentFile(), "pom.xml");
        String module = wrapper.getParentFile().getName();
        if (!aggregator.isFile() || !registered(
                factory.newDocumentBuilder().parse(aggregator).getDocumentElement(), module)) {
            throw new IOException("Incomplete library installation: " + wrapper
                    + " is not registered as module " + module + " in " + aggregator
                    + ". Restore the module registration before reinstalling. The wrapper was preserved.");
        }
        return dependency;
    }

    private static boolean registered(Element project, String module) {
        if (hasModule(child(project, "modules"), module)) return true;
        Element profiles = child(project, "profiles");
        if (profiles != null) {
            for (Node node = profiles.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element && "profile".equals(node.getNodeName())
                        && hasModule(child((Element) node, "modules"), module)) return true;
            }
        }
        return false;
    }

    private static boolean hasModule(Element modules, String name) {
        if (modules != null) {
            for (Node node = modules.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element && "module".equals(node.getNodeName())
                        && name.equals(node.getTextContent().trim())) return true;
            }
        }
        return false;
    }

    private static Element child(Element parent, String name) {
        if (parent != null) {
            for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element && name.equals(node.getNodeName())) return (Element) node;
            }
        }
        return null;
    }

    private static String value(Element project, String name) {
        // Only direct children: the generated wrapper also contains its parent's
        // coordinates and dependencies with unrelated artifact IDs.
        for (Node node = project.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && name.equals(node.getNodeName())) {
                return node.getTextContent().trim();
            }
        }
        return "";
    }

    List<String> command(ProjectBinding binding, File library) throws IOException {
        File home = mavenHome(binding);
        File[] boot = new File(home, "boot").listFiles((dir, name) ->
                name.startsWith("plexus-classworlds-") && name.endsWith(".jar"));
        if (boot == null || boot.length != 1) {
            throw new IOException("Cannot find Maven's launcher in " + home);
        }
        boolean windows = System.getProperty("os.name", "").startsWith("Windows");
        String java = new File(System.getProperty("java.home"), "bin/java" + (windows ? ".exe" : "")).getPath();
        File root = new File(binding.projectDir()).getCanonicalFile().getParentFile();
        List<String> command = new ArrayList<>(Arrays.asList(java, "-Duser.home=" + System.getProperty("user.home"),
                "-Dmaven.home=" + home,
                "-Dmaven.multiModuleProjectDirectory=" + root,
                "-Dclassworlds.conf=" + new File(home, "bin/m2.conf"),
                "-cp", boot[0].getPath(), "org.codehaus.plexus.classworlds.launcher.Launcher",
                "--batch-mode", "--non-recursive"));
        command.addAll(binding.mavenArguments());
        // The selected archive always wins over a -Dfile from the Settings invocation.
        command.add("cn1:install-cn1lib");
        command.add("-Dfile=" + library.getAbsolutePath());
        return command;
    }

    private File mavenHome(ProjectBinding binding) throws IOException {
        List<String> candidates = new ArrayList<>(Arrays.asList(binding.mavenHome(),
                System.getProperty("maven.home"), System.getenv("MAVEN_HOME"), System.getenv("M2_HOME")));
        String path = System.getenv("PATH");
        if (path != null) {
            for (String directory : path.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
                for (String name : new String[]{"mvn", "mvn.cmd"}) {
                    File executable = new File(directory, name);
                    if (executable.isFile()) {
                        candidates.add(executable.getCanonicalFile().getParentFile().getParent());
                    }
                }
            }
        }
        for (String candidate : candidates) {
            if (candidate != null && new File(candidate, "bin/m2.conf").isFile()) {
                return new File(candidate).getCanonicalFile();
            }
        }
        throw new IOException("Cannot locate Maven. Launch Settings with mvn cn1:settings, or set MAVEN_HOME.");
    }

    void runMaven(List<String> command, File common) throws IOException, InterruptedException {
        System.out.println("Running cn1:install-cn1lib in " + common);
        Process process = new ProcessBuilder(command).directory(common)
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start();
        try {
            process.getOutputStream().close();
            if (!process.waitFor(5, TimeUnit.MINUTES)) {
                throw new IOException("Maven library installation timed out. See settings.log.");
            }
            if (process.exitValue() != 0) {
                throw new IOException("Maven library installation failed (exit " + process.exitValue()
                        + "). See settings.log.");
            }
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                boolean interrupted = false;
                while (process.isAlive()) {
                    try {
                        process.waitFor();
                    } catch (InterruptedException ex) {
                        interrupted = true;
                    }
                }
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }
}
